import { useEffect, useState } from 'react'
import { apiFailure } from '../../services/api'
export function useLoad<T>(loader: () => Promise<T>) {
  const [revision, setRevision] = useState(0)
  const [result, setResult] = useState<{ loader: typeof loader; revision: number; data: T | null; error: string } | null>(null)
  const loading = result?.loader !== loader || result?.revision !== revision
  useEffect(() => {
    let active = true
    loader().then(data => { if (active) setResult({ loader, revision, data, error: '' }) })
      .catch(e => { if (active) setResult({ loader, revision, data: null, error: apiFailure(e).message }) })
    return () => { active = false }
  }, [loader, revision])
  return { data: loading ? null : result?.data ?? null, error: loading ? '' : result?.error ?? '', loading,
    setData: (data: T) => setResult({ loader, revision, data, error: '' }), reload: () => setRevision(n => n + 1) }
}
export const words = (value: string) => value.toLowerCase().replaceAll('_', ' ')
