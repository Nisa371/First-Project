import { useEffect, useRef, useState } from 'react'
import { useLocation } from 'react-router'
import { useIsTrade, useTradeText } from './useTradeText'
import { recognitionConstructor, supportedField, insertTranscript, voiceErrors, type Recognition, type VoiceField } from './voice'

// Shared Trade press-and-hold control. A supplied target prevents focus from selecting other fields.
export function HoldToTalk({ targetId, disabled = false }: { targetId?: string; disabled?: boolean }) {
  const { pathname } = useLocation(), trade = useIsTrade(), tr = useTradeText()
  const target = useRef<VoiceField | null>(null), speech = useRef<Recognition | null>(null)
  const [listening, setListening] = useState(false), [status, setStatus] = useState('')
  const Constructor = recognitionConstructor()
  function dispose() {
    const current = speech.current
    if (current) { current.onresult = null; current.onerror = null; current.onend = null; current.abort(); speech.current = null }
    setListening(false)
  }
  useEffect(() => {
    function focus(event: FocusEvent) {
      const element = event.target
      if (element instanceof HTMLElement && element.dataset.tradeMic === 'true') return
      if (speech.current) dispose()
      if (targetId) return
      if (!(element instanceof Element) || !element.closest('[data-trade-workspace="true"]')) { target.current = null; return }
      if (supportedField(element)) target.current = element
      else if (element instanceof HTMLInputElement || element instanceof HTMLTextAreaElement || element instanceof HTMLSelectElement) target.current = null
    }
    document.addEventListener('focusin', focus)
    return () => { document.removeEventListener('focusin', focus); dispose() }
  }, [targetId])
  useEffect(() => { dispose(); target.current = null }, [pathname, disabled])
  function start() {
    if (disabled || !Constructor || speech.current) return
    const field = targetId ? document.getElementById(targetId) : target.current
    if (!supportedField(field) || !field.isConnected) { setStatus(tr('Choose a text field first.')); return }
    try {
      const recognition = new Constructor(); speech.current = recognition
      recognition.lang = trade ? 'bn-BD' : 'en-US'; recognition.interimResults = false; recognition.continuous = true
      recognition.onresult = event => {
        for (let i = event.resultIndex; i < event.results.length; i++) if (event.results[i].isFinal) insertTranscript(field, event.results[i][0].transcript)
      }
      recognition.onerror = event => { setStatus(trade ? voiceErrors[event.error] ?? tr('Voice input failed. You can keep typing.') : event.error === 'not-allowed' ? 'Microphone permission was denied. You can keep typing.' : 'Voice input failed. You can keep typing.'); dispose() }
      recognition.onend = () => { speech.current = null; setListening(false) }
      setStatus(''); setListening(true); recognition.start()
    } catch { dispose(); setStatus(tr('Voice input failed. You can keep typing.')) }
  }
  function stop() { try { speech.current?.stop() } catch { dispose() } }
  return <div className={targetId ? 'fixed bottom-3 right-3 z-40 flex max-w-xs flex-col items-end gap-2 print:hidden' : 'flex max-w-xs flex-col items-end gap-2'}>
    <div className="rounded-xl border border-slate-200 bg-white p-2 text-xs text-slate-600 shadow-sm" role="status">{!Constructor ? tr('Voice is unavailable in this browser. You can keep typing.') : status || (listening ? tr('Listening… release to stop.') : targetId ? tr('Tap and hold to speak') : 'লেখার ঘর বেছে মাইক ধরে বলুন। কিবোর্ডে একবার চাপ দিয়ে চালু/বন্ধ করুন।')}</div>
    <button title={tr('Your browser may process audio online. We do not store audio.')} data-trade-mic="true" type="button" disabled={disabled || !Constructor} aria-label={tr('Tap and hold to speak')} aria-pressed={listening} className={`button-secondary touch-none shadow-lg ${listening ? 'ring-2 ring-teal-600' : ''}`}
      onPointerDown={e => { if (e.button !== 0) return; e.preventDefault(); e.currentTarget.setPointerCapture(e.pointerId); start() }} onPointerUp={stop} onPointerCancel={dispose} onLostPointerCapture={stop}
      onClick={e => { if (e.detail === 0) { if (speech.current) stop(); else start() } }}
    >{listening ? tr('Listening…') : `🎙 ${tr('Tap and hold to speak')}`}</button>
  </div>
}
