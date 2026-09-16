import { useEffect, useRef, useState } from 'react'
import { Field } from '../marketplace/shared'
interface Recognition {
  lang: string; interimResults: boolean; continuous: boolean
  onresult: ((e: { results: ArrayLike<ArrayLike<{ transcript: string }>> }) => void) | null
  onerror: ((e: { error: string }) => void) | null
  onend: (() => void) | null
  start(): void; stop(): void; abort(): void
}
type SpeechWindow = Window & { SpeechRecognition?: new () => Recognition; webkitSpeechRecognition?: new () => Recognition }
const errors: Record<string, string> = {
  'not-allowed': 'মাইক্রোফোনের অনুমতি নেই। অনুমতি দিন অথবা লিখুন। Microphone permission was denied.',
  'service-not-allowed': 'ভয়েস সেবা চালু নেই। নিচে লিখে এগিয়ে যান। Voice service is unavailable.',
  'audio-capture': 'মাইক্রোফোন পাওয়া যায়নি। নিচে লিখুন। No microphone was found.',
  'network': 'সংযোগে সমস্যা। আবার চেষ্টা করুন অথবা লিখুন। Check your connection.',
  'no-speech': 'কথা শোনা যায়নি। আবার বলুন অথবা লিখুন। No speech detected.',
  'language-not-supported': 'এই ব্রাউজারে বাংলা ভয়েস নেই। নিচে লিখুন। Bangla voice is unavailable.',
}
export function VoiceInput({ onApply, disabled = false }: { onApply: (text: string) => void; disabled?: boolean }) {
  const speech = useRef<Recognition | null>(null)
  const [listening, setListening] = useState(false), [transcript, setTranscript] = useState(''), [error, setError] = useState('')
  const win = window as SpeechWindow, Constructor = win.SpeechRecognition ?? win.webkitSpeechRecognition
  const supported = Boolean(Constructor) && window.isSecureContext
  useEffect(() => () => { const r = speech.current; if (r) { r.onresult = null; r.onerror = null; r.onend = null; r.abort() } }, [])
  function start() {
    if (!Constructor || speech.current) return
    setError(''); setTranscript('')
    try {
      const r = new Constructor(); speech.current = r
      r.lang = 'bn-BD'; r.interimResults = true; r.continuous = false
      r.onresult = e => setTranscript(Array.from(e.results).map(result => result[0]?.transcript ?? '').join(' ').slice(0, 2000))
      r.onerror = e => { setError(errors[e.error] ?? 'ভয়েস কাজ করছে না। আবার চেষ্টা করুন অথবা লিখুন। Voice input failed.'); setListening(false) }
      r.onend = () => { setListening(false); speech.current = null }
      setListening(true); r.start()
    } catch { speech.current = null; setListening(false); setError('ভয়েস চালু হয়নি। লিখে এগিয়ে যান। Could not start voice input.') }
  }
  return <section className="rounded-2xl border border-teal-200 bg-teal-50 p-5" aria-label="Bangla voice assistance">
    <div className="flex flex-wrap items-center justify-between gap-3"><h3 className="font-bold text-teal-950">বাংলায় বলুন · Speak in Bangla</h3><span className="text-xs font-semibold text-teal-800">ঐচ্ছিক · Optional</span></div>
    <p className="my-3 text-sm leading-relaxed text-teal-900">আপনার কাজের অভিজ্ঞতা বলুন, তারপর লেখাটি দেখে যোগ করুন। Describe your experience, then review the text.</p>
    <p className="mb-4 text-xs leading-relaxed text-teal-800">আমরা অডিও সংরক্ষণ করি না। ব্রাউজার ভয়েস প্রক্রিয়ার জন্য অডিও পাঠাতে পারে। We do not store audio; your browser may process it online.</p>
    {supported ? <button className={`button-secondary w-full ${listening ? 'ring-2 ring-teal-500' : ''}`} type="button" disabled={disabled} onClick={() => listening ? speech.current?.stop() : start()}><svg aria-hidden="true" className="mr-2 h-5 w-5 shrink-0" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8"><rect x="9" y="2" width="6" height="12" rx="3" /><path d="M5 10v2a7 7 0 0 0 14 0v-2M12 19v3m-4 0h8" /></svg>{listening ? 'শুনছি… থামুন · Stop listening' : 'কথা বলুন · Start microphone'}</button> : <p role="status" className="rounded-xl bg-white p-3 text-sm">এই ব্রাউজারে ভয়েস চালু নেই। নিচে লিখে সম্পূর্ণ করতে পারবেন। Voice is unavailable; type below to continue.</p>}
    <div role="status" aria-live="polite" className="mt-3 text-sm text-teal-900">{listening ? 'শুনছি… ধীরে ও স্পষ্ট করে বলুন। Listening…' : 'নিজে লিখুন · You can always type in the experience field below.'}</div>
    {error && <p role="alert" className="mt-3 text-sm text-rose-800">{error}</p>}
    {transcript && <div className="mt-4 space-y-3"><Field name="voice-preview" label="শুনে লেখা · Edit transcript" value={transcript} onChange={setTranscript} multiline maxLength={2000} /><button type="button" disabled={disabled || listening || !transcript.trim()} className="button-primary w-full" onClick={() => { onApply(transcript.trim()); setTranscript('') }}>অভিজ্ঞতায় যোগ করুন · Use this text</button></div>}
  </section>
}
