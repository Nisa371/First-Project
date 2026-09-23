export interface Recognition {
  lang: string; interimResults: boolean; continuous: boolean
  onresult: ((event: { resultIndex: number; results: ArrayLike<ArrayLike<{ transcript: string }> & { isFinal: boolean }> }) => void) | null
  onerror: ((event: { error: string }) => void) | null
  onend: (() => void) | null
  start(): void; stop(): void; abort(): void
}
export function recognitionConstructor() {
  const win = window as Window & { SpeechRecognition?: new () => Recognition; webkitSpeechRecognition?: new () => Recognition }
  return window.isSecureContext ? win.SpeechRecognition ?? win.webkitSpeechRecognition : undefined
}
export type VoiceField = HTMLInputElement | HTMLTextAreaElement
export function supportedField(target: EventTarget | null): target is VoiceField {
  return (target instanceof HTMLTextAreaElement || (target instanceof HTMLInputElement && ['text', 'search'].includes(target.type)))
    && !target.matches(':disabled') && !target.readOnly && !target.closest('[inert]') && target.dataset.voice !== 'off'
}
export function insertTranscript(field: VoiceField, transcript: string) {
  if (!supportedField(field) || !field.isConnected || !transcript.trim()) return
  const start = field.selectionStart ?? field.value.length, end = field.selectionEnd ?? start
  const before = field.value.slice(0, start), after = field.value.slice(end)
  const words = transcript.trim().replace(/\s+/g, ' ')
  let addition = `${before && !/\s$/.test(before) ? ' ' : ''}${words}${after && !/^\s/.test(after) ? ' ' : ''}`
  if (field.maxLength >= 0) addition = addition.slice(0, Math.max(0, field.maxLength - before.length - after.length))
  const value = before + addition + after
  // Use the native setter, bypassing React's value tracker, then deliver a bubbling input event.
  const prototype = field instanceof HTMLTextAreaElement ? HTMLTextAreaElement.prototype : HTMLInputElement.prototype
  Object.getOwnPropertyDescriptor(prototype, 'value')?.set?.call(field, value)
  field.dispatchEvent(new Event('input', { bubbles: true }))
  field.setSelectionRange(start + addition.length, start + addition.length)
}
export const voiceErrors: Record<string, string> = {
  'not-allowed': 'মাইক্রোফোন ব্যবহারের অনুমতি পাওয়া যায়নি। ব্রাউজারের অনুমতি ঠিক করে আবার চেষ্টা করুন।',
  'service-not-allowed': 'ব্রাউজারের ভয়েস সেবা চালু নেই। লিখে এগিয়ে যান।',
  'audio-capture': 'মাইক্রোফোন পাওয়া যায়নি। লিখে এগিয়ে যান।',
  'network': 'ভয়েস সংযোগে সমস্যা। লিখে এগিয়ে যান।',
  'no-speech': 'কথা শোনা যায়নি। আবার চেষ্টা করুন অথবা লিখুন।',
  'language-not-supported': 'এই ব্রাউজারে বাংলা ভয়েস নেই। লিখে এগিয়ে যান।',
}
