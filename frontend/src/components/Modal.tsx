import { useEffect, useRef, type ReactNode } from 'react'
export function Modal({ title, children, close }: { title: string; children: ReactNode; close: () => void }) {
  const ref = useRef<HTMLDialogElement>(null)
  useEffect(() => { const d = ref.current; d?.showModal(); return () => d?.close() }, [])
  return <dialog ref={ref} onCancel={e => { e.preventDefault(); close() }} aria-labelledby="modal-title" className="m-auto w-[calc(100%_-_2rem)] max-w-lg rounded-2xl border-0 bg-white p-6 shadow-xl backdrop:bg-slate-950/60 sm:p-8"><h2 id="modal-title" className="mb-4 text-2xl font-bold">{title}</h2>{children}</dialog>
}
