import { useId, useState, type InputHTMLAttributes } from 'react'
import { useTradeText } from '../features/trade/useTradeText'

type Props = Omit<InputHTMLAttributes<HTMLInputElement>, 'type' | 'value' | 'defaultValue' | 'multiple'>

export function LocalizedFileInput({ id, className, onChange, ...props }: Props) {
  const generatedId = useId()
  const inputId = id ?? generatedId
  const tr = useTradeText()
  const [filename, setFilename] = useState('')
  return <div className={className}>
    <input {...props} id={inputId} type="file" className="peer sr-only" aria-describedby={`${inputId}-filename`} onChange={event => {
      onChange?.(event)
      setFilename(event.target.files?.[0]?.name ?? '')
    }} />
    <label htmlFor={inputId} className="button-secondary cursor-pointer peer-focus-visible:outline-2 peer-focus-visible:outline-offset-2 peer-focus-visible:outline-indigo-600 peer-disabled:opacity-50">{tr('Choose file')}</label>
    <span id={`${inputId}-filename`} className="ml-3 break-all text-sm" aria-live="polite">{filename || tr('No file selected')}</span>
  </div>
}
