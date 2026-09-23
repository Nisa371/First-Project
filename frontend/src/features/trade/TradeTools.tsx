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
  const [open, setOpen] = useState(false), [question, setQuestion] = useState(''), [busy, setBusy] = useState(false)
  const [conversation, setConversation] = useState<Conversation>({ failureCode: null, messages: [] })
  const [error, setError] = useState(''), [loading, setLoading] = useState(false)
  const trigger = useRef<HTMLButtonElement>(null), input = useRef<HTMLTextAreaElement>(null), pending = useRef(false)
  const mounted = useRef(true)
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
    return () => { mounted.current = false; document.removeEventListener('focusin', focus); dispose() }
  }, [])
  useEffect(() => { dispose(); target.current = null }, [pathname])
  useEffect(() => {
    if (!open) return
    input.current?.focus()
    let active = true
    api.get<Conversation>('/trade/assistant/conversation').then(({ data }) => { if (mounted.current && active) setConversation(data) })
      .catch(() => { if (mounted.current && active) setError('কথোপকথন আনা যায়নি। বন্ধ করে আবার খুলুন।') })
      .finally(() => { if (mounted.current && active) setLoading(false) })
    return () => { active = false }
  }, [open])
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
    if (pending.current || !question.trim()) return
    pending.current = true; setBusy(true); setError('')
    try {
      const { data } = await api.post<Conversation>('/trade/assistant/messages', { message: question.trim() })
      if (!mounted.current) return
      setConversation(data)
      if (data.failureCode) setError(data.failureCode === 'AI_PROVIDER_NOT_CONFIGURED' ? 'AI সহকারী এখনো কনফিগার করা হয়নি। অন্য সব কাজ স্বাভাবিকভাবে করতে পারবেন।' : 'সহকারী এখন উত্তর দিতে পারছে না। পরে চেষ্টা করুন।')
      else setQuestion('')
    } catch { if (mounted.current) setError('বার্তা পাঠানো যায়নি। সংযোগ দেখে আবার চেষ্টা করুন।') }
    finally { pending.current = false; if (mounted.current) setBusy(false) }
  }
  function close() { dispose(); setOpen(false); trigger.current?.focus() }
  return <aside lang="bn" aria-label="বাংলা সহায়তা" className="fixed bottom-3 right-3 z-40 flex max-w-[calc(100vw-1.5rem)] flex-col items-end gap-2 print:hidden">
    {open && <section role="dialog" aria-labelledby="trade-assistant-title" onKeyDown={e => { if (e.key === 'Escape') close() }} className="flex max-h-[65dvh] w-96 max-w-full flex-col overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-xl">
      <header className="flex items-center justify-between bg-slate-900 p-4 text-white"><h2 id="trade-assistant-title" className="font-bold">বাংলা সহকারী</h2><button type="button" className="min-h-11 rounded-lg px-3 focus-visible:ring-2" onClick={close} aria-label="সহকারী বন্ধ করুন">বন্ধ করুন ×</button></header>
      <div className="min-h-0 flex-1 space-y-3 overflow-y-auto p-4" aria-live="polite" aria-busy={loading || busy}>
        {loading ? <p role="status">কথোপকথন আসছে…</p> : conversation.messages.length ? conversation.messages.map((message, i) => <div key={i} className={`rounded-xl p-3 ${message.role === 'USER' ? 'ml-4 bg-indigo-50' : 'mr-4 bg-slate-50'}`}><p className="mb-1 text-xs font-semibold">{message.role === 'USER' ? 'আপনি' : 'সহকারী'}</p><p className="whitespace-pre-wrap break-words text-sm leading-relaxed">{message.content}</p></div>) : <><p className="text-sm text-slate-600">চাকরি, প্রোফাইল বা ভেরিফিকেশন নিয়ে প্রশ্ন লিখুন। সাম্প্রতিক কথোপকথন সংরক্ষিত থাকে।</p>{starters.map(text => <button type="button" key={text} className="block w-full rounded-xl border border-slate-200 p-3 text-left text-sm hover:bg-slate-50" onClick={() => { setQuestion(text); input.current?.focus() }}>{text}</button>)}</>}
        {busy && <p role="status">উত্তরের অপেক্ষায়…</p>}{error && <p role="alert" className="rounded-xl bg-amber-50 p-3 text-sm text-amber-900">{error}</p>}
      </div>
      <form className="space-y-2 border-t border-slate-200 p-4" onSubmit={e => { e.preventDefault(); void send() }}><label htmlFor="trade-question" className="field-label">আপনার প্রশ্ন</label><textarea ref={input} id="trade-question" rows={2} maxLength={2000} disabled={busy} value={question} onChange={e => setQuestion(e.target.value)} className="form-input" placeholder="এখানে লিখুন বা মাইক্রোফোনে বলুন" /><button disabled={loading || busy || !question.trim()} className="button-primary w-full">{busy ? 'পাঠানো হচ্ছে…' : 'পাঠান'}</button></form>
    </section>}
    <div className="max-w-xs rounded-xl border border-slate-200 bg-white p-2 text-xs text-slate-600 shadow-sm" role="status">{!Constructor ? 'এই ব্রাউজারে ভয়েস নেই। লিখে এগিয়ে যান।' : voiceStatus || (listening ? 'শুনছি… ছেড়ে দিন বা থামান।' : 'লেখার ঘর বেছে মাইক ধরে বলুন। কিবোর্ডে একবার চাপ দিয়ে চালু/বন্ধ করুন।')}</div>
    <div className="flex gap-2"><button ref={trigger} type="button" aria-expanded={open} aria-label="বাংলা সহকারী খুলুন" className="button-primary shadow-lg" onClick={() => open ? close() : (setLoading(true), setError(''), setOpen(true))}>সহকারী</button>
      <button title="ব্রাউজার অনলাইনে ভয়েস প্রক্রিয়া করতে পারে। আমরা অডিও সংরক্ষণ করি না।" data-trade-mic="true" type="button" disabled={!Constructor} aria-label={listening ? 'শুনছি… মাইক্রোফোন থামান' : 'কথা বলে লিখুন'} aria-pressed={listening} className={`button-secondary touch-none shadow-lg ${listening ? 'ring-2 ring-teal-600' : ''}`}
        onPointerDown={e => { if (e.button !== 0) return; e.preventDefault(); e.currentTarget.setPointerCapture(e.pointerId); start() }} onPointerUp={stop} onPointerCancel={dispose} onLostPointerCapture={stop}
        onClick={e => { if (e.detail === 0) { if (speech.current) stop(); else start() } }} 
        >{listening ? '● শুনছি…' : '🎙 কথা বলে লিখুন'}</button></div>
  </aside>
}
