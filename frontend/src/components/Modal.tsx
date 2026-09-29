import { useEffect, useRef, type ReactNode } from 'react'
export function Modal({ title, children, close, closeLabel }: { title: string; children: ReactNode; close: () => void; closeLabel?: string }) {
  const ref = useRef<HTMLDialogElement>(null)
  useEffect(() => { const d = ref.current; d?.showModal(); return () => d?.close() }, [])
  return <dialog ref={ref} onCancel={e => { e.preventDefault(); close() }} aria-labelledby="modal-title" className="m-auto w-[calc(100%_-_2rem)] max-w-lg rounded-2xl border-0 bg-white p-6 shadow-xl backdrop:bg-slate-950/60 sm:p-8"><div className="mb-4 flex items-start justify-between gap-4"><h2 id="modal-title" className="text-2xl font-bold">{title}</h2>{closeLabel && <button type="button" className="button-secondary shrink-0" aria-label={closeLabel} onClick={close}>×</button>}</div>{children}</dialog>
}
