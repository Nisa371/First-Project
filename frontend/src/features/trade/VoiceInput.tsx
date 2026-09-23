import { useTradeText } from './useTradeText'
import { useEffect, useRef, useState } from 'react'
import { Field } from '../marketplace/shared'
import { recognitionConstructor, type Recognition } from './voice'
import { useIsTrade } from './useTradeText'
const errors: Record<string, string> = {
  'not-allowed': 'মাইক্রোফোনের অনুমতি নেই। অনুমতি দিন অথবা লিখুন। Microphone permission was denied.',
  'service-not-allowed': 'ভয়েস সেবা চালু নেই। নিচে লিখে এগিয়ে যান। Voice service is unavailable.',
  'audio-capture': 'মাইক্রোফোন পাওয়া যায়নি। নিচে লিখুন। No microphone was found.',
  'network': 'সংযোগে সমস্যা। আবার চেষ্টা করুন অথবা লিখুন। Check your connection.',
  'no-speech': 'কথা শোনা যায়নি। আবার বলুন অথবা লিখুন। No speech detected.',
  'language-not-supported': 'এই ব্রাউজারে বাংলা ভয়েস নেই। নিচে লিখুন। Bangla voice is unavailable.',
}
export function VoiceInput({ onApply, disabled = false, language = 'bn-BD', mode = 'experience', maxLength = 2000 }: { onApply: (text: string) => void; disabled?: boolean; language?: string; mode?: 'experience' | 'assessment'; maxLength?: number }) {
  const tr = useTradeText()
  const isTrade = useIsTrade()

  const speech = useRef<Recognition | null>(null)
  const [listening, setListening] = useState(false), [transcript, setTranscript] = useState(''), [error, setError] = useState('')
  const Constructor = recognitionConstructor()
  const supported = Boolean(Constructor) && window.isSecureContext
  useEffect(() => () => { const r = speech.current; if (r) { r.onresult = null; r.onerror = null; r.onend = null; r.abort() } }, [])
  function start() {
    if (!Constructor || speech.current) return
    setError(''); setTranscript('')
    try {
      const r = new Constructor(); speech.current = r
      r.lang = language; r.interimResults = true; r.continuous = false
      r.onresult = e => setTranscript(Array.from(e.results).map(result => result[0]?.transcript ?? '').join(' ').slice(0, maxLength))
      r.onerror = e => { setError(errors[e.error] ?? tr("ভয়েস কাজ করছে না। আবার চেষ্টা করুন অথবা লিখুন। Voice input failed.")); setListening(false) }
      r.onend = () => { setListening(false); speech.current = null }
      setListening(true); r.start()
    } catch { speech.current = null; setListening(false); setError(tr("ভয়েস চালু হয়নি। লিখে এগিয়ে যান। Could not start voice input.")) }
  }
  if (isTrade) return <p className="rounded-xl bg-teal-50 p-4 text-sm text-teal-900">লেখার ঘরে চাপ দিয়ে নিচের মাইক্রোফোন ধরে বাংলায় বলুন। চাইলে নিজেও লিখতে পারবেন। আমরা অডিও সংরক্ষণ করি না; ব্রাউজার অনলাইনে ভয়েস প্রক্রিয়া করতে পারে।</p>
  return <section className="rounded-2xl border border-teal-200 bg-teal-50 p-5" aria-label={tr("Optional voice input")}>
    <div className="flex flex-wrap items-center justify-between gap-3"><h3 className="font-bold text-teal-950">{mode === 'assessment' ? tr("Dictate your answer") : tr("বাংলায় বলুন · Speak in Bangla")}</h3><span className="text-xs font-semibold text-teal-800">{tr("ঐচ্ছিক · Optional")}</span></div>
    <p className="my-3 text-sm leading-relaxed text-teal-900">{mode === 'assessment' ? tr("Speak your answer, review the transcript, then use the text in your answer below.") : tr("আপনার কাজের অভিজ্ঞতা বলুন, তারপর লেখাটি দেখে যোগ করুন। Describe your experience, then review the text.")}</p>
    <p className="mb-4 text-xs leading-relaxed text-teal-800">{tr("আমরা অডিও সংরক্ষণ করি না। ব্রাউজার ভয়েস প্রক্রিয়ার জন্য অডিও পাঠাতে পারে। We do not store audio; your browser may process it online.")}</p>
    {supported ? <button className={`button-secondary w-full ${listening ? 'ring-2 ring-teal-500' : ''}`} type="button" disabled={disabled} onClick={() => listening ? speech.current?.stop() : start()}><svg aria-hidden="true" className="mr-2 h-5 w-5 shrink-0" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8"><rect x="9" y="2" width="6" height="12" rx="3" /><path d="M5 10v2a7 7 0 0 0 14 0v-2M12 19v3m-4 0h8" /></svg>{listening ? tr("শুনছি… থামুন · Stop listening") : tr("কথা বলুন · Start microphone")}</button> : <p role="status" className="rounded-xl bg-white p-3 text-sm">{tr("এই ব্রাউজারে ভয়েস চালু নেই। নিচে লিখে সম্পূর্ণ করতে পারবেন। Voice is unavailable; type below to continue.")}</p>}
    <div role="status" aria-live="polite" className="mt-3 text-sm text-teal-900">{listening ? tr("শুনছি… ধীরে ও স্পষ্ট করে বলুন। Listening…") : tr("নিজে লিখুন · You can always type in the field below.")}</div>
    {error && <p role="alert" className="mt-3 text-sm text-rose-800">{error}</p>}
    {transcript && <div className="mt-4 space-y-3"><Field name="voice-preview" label={tr("শুনে লেখা · Edit transcript")} value={transcript} onChange={setTranscript} multiline maxLength={maxLength} /><button type="button" disabled={disabled || listening || !transcript.trim()} className="button-primary w-full" onClick={() => { onApply(transcript.trim()); setTranscript('') }}>{mode === 'assessment' ? tr("Use this text in my answer") : tr("অভিজ্ঞতায় যোগ করুন · Use this text")}</button></div>}
  </section>
}
