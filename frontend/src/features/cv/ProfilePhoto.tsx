import { useTradeText } from '../trade/useTradeText'
import { useEffect, useState } from 'react'
import { api, apiFailure } from '../../services/api'
import type { Profile } from '../marketplace/api'
import { Feedback } from '../marketplace/shared'
import { photoUrl } from './model'

export function Avatar({ url, name }: { url: string | null; name: string }) {
  const tr = useTradeText()
  const [failedUrl, setFailedUrl] = useState<string | null>(null)
  return url && failedUrl !== url ? <img className="size-24 shrink-0 rounded-2xl border border-slate-200 object-cover" src={photoUrl(url)} alt={`${name} · ${tr('Profile photo')}`} onError={() => setFailedUrl(url)} /> : <div role="img" aria-label={`${name}: ${tr('No profile photo')}`} className="grid size-24 shrink-0 place-items-center rounded-2xl bg-indigo-50 text-3xl font-bold text-indigo-700">{name.trim().slice(0, 1).toUpperCase() || '?'}</div>
}
export function PhotoUpload({ profile, onSaved }: { profile: Profile; onSaved: (profile: Profile) => void }) {
  const tr = useTradeText()

  const [file, setFile] = useState<File | null>(null), [preview, setPreview] = useState('')
  const [busy, setBusy] = useState(false), [error, setError] = useState(''), [success, setSuccess] = useState('')
  useEffect(() => () => { if (preview) URL.revokeObjectURL(preview) }, [preview])
  async function upload() {
    if (!file) return
    setBusy(true); setError(''); setSuccess('')
    const data = new FormData(); data.append('file', file)
    try { const updated = (await api.post<Profile>('/candidates/me/photo', data)).data; onSaved({ ...updated, profilePhotoUrl: `${updated.profilePhotoUrl}?v=${Date.now()}` }); setFile(null); setPreview(''); setSuccess(tr("Your profile photo has been saved.")) }
    catch (e) { setError(apiFailure(e).message) } finally { setBusy(false) }
  }
  return <section className="surface space-y-4"><h2 className="text-xl font-bold">{tr("Profile photo · প্রোফাইল ছবি")}</h2><Avatar url={profile.profilePhotoUrl} name={profile.fullName} />{!profile.profilePhotoUrl && <p className="rounded-xl bg-amber-50 p-3 text-sm text-amber-900">{tr("Profile photo required · ছবি যোগ করুন। Complete your profile by uploading your photo.")}</p>}<p className="text-sm text-slate-600">{tr("JPEG or PNG, up to 5 MB and 16 megapixels. This photo is visible on your profile and applications.")}</p><Feedback error={error} success={success} /><form className="space-y-4" onSubmit={e => { e.preventDefault(); void upload() }}><label className="field-label" htmlFor="profile-photo">{profile.profilePhotoUrl ? tr("Change Photo") : tr("Upload Photo")}</label><input id="profile-photo" className="block w-full min-w-0 text-sm file:mr-3 file:rounded-lg file:border-0 file:bg-slate-100 file:p-3" type="file" accept="image/jpeg,image/png" disabled={busy} onChange={e => { setError(''); setSuccess(''); const selected = e.target.files?.[0] ?? null; if (selected && (selected.size > 5 * 1024 * 1024 || !['image/jpeg', 'image/png'].includes(selected.type))) { setError(tr("Choose a JPEG or PNG up to 5 MB.")); setFile(null); setPreview(''); e.target.value = ''; return } setFile(selected); setPreview(selected ? URL.createObjectURL(selected) : '') }} />{file && preview && <div><p className="mb-2 text-sm">{tr("Selected photo preview")}</p><img alt={tr("Selected photo preview")} src={preview} className="size-28 rounded-2xl object-cover" /></div>}<button className="button-primary w-full" disabled={busy || !file}>{busy ? tr("Uploading…") : tr("Save photo")}</button></form></section>
}
