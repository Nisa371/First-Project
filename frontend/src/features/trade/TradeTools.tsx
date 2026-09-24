import { useEffect, useRef, useState } from 'react'
import { useLocation } from 'react-router'
import { api } from '../../services/api'
import { recognitionConstructor, supportedField, insertTranscript, voiceErrors, type Recognition, type VoiceField } from './voice'
interface Conversation { failureCode: string | null; messages: { role: 'USER' | 'ASSISTANT'; content: string }[] }
const starters = ['কীভাবে চাকরির জন্য আবেদন করব?', 'ভেরিফিকেশনের জন্য কী লাগবে?', 'আমার প্রোফাইল কীভাবে সম্পূর্ণ করব?', 'সেশন কীভাবে বুক করব?']
export function TradeTools() {
  const { pathname } = useLocation()
  const target = useRef<VoiceField | null>(null), speech = useRef<Recognition | null>(null)
  const [listening, setListening] = useState(false), [voiceStatus, setVoiceStatus] = useState('')
  const [open, setOpen] = useState(false), [question, setQuestion] = useState(''), [isSending, setIsSending] = useState(false)
  const [conversation, setConversation] = useState<Conversation>({ failureCode: null, messages: [] })
  const [error, setError] = useState(''), [loading, setLoading] = useState(false)
  const trigger = useRef<HTMLButtonElement>(null), input = useRef<HTMLTextAreaElement>(null), pending = useRef<AbortController | null>(null)
  const mounted = useRef(true)
  const refresh = useRef<AbortController | null>(null)
  const [historyError, setHistoryError] = useState('')
  const [historyRetry, setHistoryRetry] = useState(0)
  const Constructor = recognitionConstructor()
  useEffect(() => {
    mounted.current = true
    function focus(event: FocusEvent) {
      const element = event.target
      if (element instanceof HTMLElement && element.dataset.tradeMic === 'true') return
      if (speech.current) dispose()
      if (!(element instanceof Element) || !element.closest('[data-trade-workspace="true"]')) { target.current = null; return }
      if (supportedField(element)) target.current = element
      else if (element instanceof HTMLInputElement || element instanceof HTMLTextAreaElement || element instanceof HTMLSelectElement) target.current = null
    }
    document.addEventListener('focusin', focus)
    return () => { mounted.current = false; pending.current?.abort(); pending.current = null; refresh.current?.abort(); refresh.current = null; document.removeEventListener('focusin', focus); dispose() }
  }, [])
  useEffect(() => { dispose(); target.current = null }, [pathname])
  useEffect(() => {
    if (!open) return
    input.current?.focus()
    // Reopening during a send must not race its authoritative conversation response.
    if (pending.current) { setLoading(false); return }
    const controller = new AbortController()
    refresh.current = controller
    setLoading(true); setHistoryError('')
    api.get<Conversation>('/trade/assistant/conversation', { signal: controller.signal })
      .then(({ data }) => { if (!controller.signal.aborted) setConversation(data) })
      .catch(() => { if (!controller.signal.aborted) setHistoryError('কথোপকথন আনা যায়নি। আবার চেষ্টা করুন।') })
      .finally(() => {
        if (refresh.current === controller) { refresh.current = null; setLoading(false) }
      })
    return () => { controller.abort(); if (refresh.current === controller) refresh.current = null }
  }, [open, historyRetry])

  function dispose() {
    const current = speech.current
    if (current) { current.onresult = null; current.onerror = null; current.onend = null; current.abort(); speech.current = null }
    if (mounted.current) setListening(false)
  }
  function start() {
    if (!Constructor || speech.current) return
    const field = target.current
    if (!field || !supportedField(field) || !field.isConnected) { setVoiceStatus('আগে যে ঘরে লিখতে চান, সেখানে চাপ দিন।'); return }
    try {
      const recognition = new Constructor(); speech.current = recognition
      recognition.lang = 'bn-BD'; recognition.interimResults = false; recognition.continuous = true
      recognition.onresult = event => {
        for (let i = event.resultIndex; i < event.results.length; i++) if (event.results[i].isFinal) insertTranscript(field, event.results[i][0].transcript)
      }
      recognition.onerror = event => { setVoiceStatus(voiceErrors[event.error] ?? 'ভয়েস কাজ করছে না। লিখে এগিয়ে যান।'); dispose() }
      recognition.onend = () => { speech.current = null; setListening(false) }
      setVoiceStatus(''); setListening(true); recognition.start()
    } catch { dispose(); setVoiceStatus('মাইক্রোফোন চালু হয়নি। লিখে এগিয়ে যান।') }
  }
  function stop() { try { speech.current?.stop() } catch { dispose() } }
  async function send() {
    if (!mounted.current || pending.current || isSending || loading || !question.trim()) return
    const controller = new AbortController()
    pending.current = controller
    refresh.current?.abort(); refresh.current = null
    setIsSending(true); setError(''); setHistoryError('')
    let data: Conversation
    try {
      // Gemini has its own bounded backend timeout. The shared 8s timeout can expire
      // before a successful reply is persisted. Allow 60s (backend default: 30s).
      // Axios owns and clears its timeout on settlement or signal cancellation.
      const response = await api.post<Conversation>('/trade/assistant/messages', { message: question.trim() }, { signal: controller.signal, timeout: 60000 })
      data = response.data
    } catch {
      if (mounted.current && pending.current === controller && !controller.signal.aborted)
        setError('বার্তা পাঠানো যায়নি। সংযোগ দেখে আবার চেষ্টা করুন।')
      return
    } finally {
      if (pending.current === controller) {
        pending.current = null
        if (mounted.current) setIsSending(false)
      }
    }
    if (!mounted.current || controller.signal.aborted) return
    // UI work is outside the network catch: a committed success cannot become a send failure.
    if (data.failureCode) {
      setError(data.failureCode === 'AI_PROVIDER_NOT_CONFIGURED' ? 'AI সহকারী এখনো কনফিগার করা হয়নি। অন্য সব কাজ স্বাভাবিকভাবে করতে পারবেন।' : 'সহকারী এখন উত্তর দিতে পারছে না। পরে চেষ্টা করুন।')
      return
    }
    setConversation(data)
    setQuestion('')
  }

  function close() { dispose(); setOpen(false); trigger.current?.focus() }
  return <aside lang="bn" aria-label="বাংলা সহায়তা" className="fixed bottom-3 right-3 z-40 flex max-w-[calc(100vw-1.5rem)] flex-col items-end gap-2 print:hidden">
    {open && <section role="dialog" aria-labelledby="trade-assistant-title" onKeyDown={e => { if (e.key === 'Escape') close() }} className="flex max-h-[65dvh] w-96 max-w-full flex-col overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-xl">
      <header className="flex items-center justify-between bg-slate-900 p-4 text-white"><h2 id="trade-assistant-title" className="font-bold">বাংলা সহকারী</h2><button type="button" className="min-h-11 rounded-lg px-3 focus-visible:ring-2" onClick={close} aria-label="সহকারী বন্ধ করুন">বন্ধ করুন ×</button></header>
      <div className="min-h-0 flex-1 space-y-3 overflow-y-auto p-4" aria-live="polite" aria-busy={loading || isSending}>
        {loading ? <p role="status">কথোপকথন আসছে…</p> : conversation.messages.length ? conversation.messages.map((message, i) => <div key={i} className={`rounded-xl p-3 ${message.role === 'USER' ? 'ml-4 bg-indigo-50' : 'mr-4 bg-slate-50'}`}><p className="mb-1 text-xs font-semibold">{message.role === 'USER' ? 'আপনি' : 'সহকারী'}</p><p className="whitespace-pre-wrap break-words text-sm leading-relaxed">{message.content}</p></div>) : <><p className="text-sm text-slate-600">চাকরি, প্রোফাইল বা ভেরিফিকেশন নিয়ে প্রশ্ন লিখুন। সাম্প্রতিক কথোপকথন সংরক্ষিত থাকে।</p>{starters.map(text => <button type="button" key={text} className="block w-full rounded-xl border border-slate-200 p-3 text-left text-sm hover:bg-slate-50" onClick={() => { setQuestion(text); input.current?.focus() }}>{text}</button>)}</>}
        {isSending && <p role="status">উত্তরের অপেক্ষায়…</p>}{historyError && <div role="alert" className="rounded-xl bg-amber-50 p-3 text-sm text-amber-900">{historyError}<button type="button" disabled={isSending || loading} className="button-secondary mt-2" onClick={() => setHistoryRetry(value => value + 1)}>আবার চেষ্টা করুন</button></div>}
        {error && <div role="alert" className="rounded-xl bg-amber-50 p-3 text-sm text-amber-900">{error}<button type="button" disabled={isSending || loading} className="button-secondary mt-2" onClick={() => void send()}>আবার চেষ্টা করুন</button></div>}
      </div>
      <form className="space-y-2 border-t border-slate-200 p-4" onSubmit={e => { e.preventDefault(); void send() }}><label htmlFor="trade-question" className="field-label">আপনার প্রশ্ন</label><textarea ref={input} id="trade-question" rows={2} maxLength={2000} disabled={isSending} value={question} onChange={e => setQuestion(e.target.value)} className="form-input" placeholder="এখানে লিখুন বা মাইক্রোফোনে বলুন" /><button type="submit" disabled={loading || isSending || !question.trim()} className="button-primary w-full">{isSending ? 'পাঠানো হচ্ছে…' : 'পাঠান'}</button></form>
    </section>}
    <div className="max-w-xs rounded-xl border border-slate-200 bg-white p-2 text-xs text-slate-600 shadow-sm" role="status">{!Constructor ? 'এই ব্রাউজারে ভয়েস নেই। লিখে এগিয়ে যান।' : voiceStatus || (listening ? 'শুনছি… ছেড়ে দিন বা থামান।' : 'লেখার ঘর বেছে মাইক ধরে বলুন। কিবোর্ডে একবার চাপ দিয়ে চালু/বন্ধ করুন।')}</div>
    <div className="flex gap-2"><button ref={trigger} type="button" aria-expanded={open} aria-label="বাংলা সহকারী খুলুন" className="button-primary shadow-lg" onClick={() => open ? close() : setOpen(true)}>সহকারী</button>
      <button title="ব্রাউজার অনলাইনে ভয়েস প্রক্রিয়া করতে পারে। আমরা অডিও সংরক্ষণ করি না।" data-trade-mic="true" type="button" disabled={!Constructor} aria-label={listening ? 'শুনছি… মাইক্রোফোন থামান' : 'কথা বলে লিখুন'} aria-pressed={listening} className={`button-secondary touch-none shadow-lg ${listening ? 'ring-2 ring-teal-600' : ''}`}
        onPointerDown={e => { if (e.button !== 0) return; e.preventDefault(); e.currentTarget.setPointerCapture(e.pointerId); start() }} onPointerUp={stop} onPointerCancel={dispose} onLostPointerCapture={stop}
        onClick={e => { if (e.detail === 0) { if (speech.current) stop(); else start() } }} 
        >{listening ? '● শুনছি…' : '🎙 কথা বলে লিখুন'}</button></div>
  </aside>
}
