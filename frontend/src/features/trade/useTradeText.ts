import { useAuth } from '../auth/useAuth'
import { tradeBn } from './tradeBn'
// Call only for system UI; employer/candidate prose is rendered unchanged by its page.
export function tradeText(text: string, trade: boolean): string {
  if (!trade) return text
  if (tradeBn[text]) return tradeBn[text]
  let match = /^Replacement #(\d+) · (.+) · (.+)$/.exec(text)
  if (match) return `বিকল্প কর্মীর অনুরোধ #${match[1]} · ${match[2]} · ${tradeBn[match[3]] ?? match[3]}`
  match = /^Your placement in (.+) is now active\.$/.exec(text)
  if (match) return `${match[1]} কাজে আপনার নিয়োগ শুরু হয়েছে।`
  match = /^Placement #(\d+) is (.+)\.$/.exec(text)
  if (match) return `নিয়োগ #${match[1]}: ${tradeBn[match[2]] ?? match[2]}।`
  match = /^You have been referred to (.+)\. Open Training to see the program\.$/.exec(text)
  if (match) return `${match[1]} প্রশিক্ষণের পরামর্শ দেওয়া হয়েছে। প্রশিক্ষণের পাতায় বিস্তারিত দেখুন।`
  match = /^Your account status is now (.+)\.$/.exec(text)
  if (match) return `আপনার অ্যাকাউন্টের অবস্থা: ${tradeBn[match[1].toLowerCase()] ?? match[1]}।`
  match = /^(CONSULTATION|INTERVIEW) · (.+)$/.exec(text)
  if (match) return `${tradeBn[match[1].toLowerCase()]} · ${new Date(match[2]).toLocaleString('bn-BD')}`
  return text.replace(/\b(\d+) years?\b/g, '$1 বছর').replace(/\b(\d+) months?\b/g, '$1 মাস')
}
export function useIsTrade() { const { user } = useAuth(); return user?.role === 'CANDIDATE' && user.candidateType === 'TRADE' }
export function useTradeText() { const trade = useIsTrade(); return (text: string | undefined) => tradeText(text ?? '', trade) }
export function useTradeError() {
  const trade = useIsTrade()
  return (message: string) => {
    if (!trade || !message) return message
    if (tradeBn[message]) return tradeBn[message]
    if (/^[^A-Za-z]*[\u0980-\u09ff]/.test(message)) return message.replace(/\s+[A-Za-z].*$/, '')
    return 'কাজটি সম্পূর্ণ হয়নি। তথ্য ও সংযোগ দেখে আবার চেষ্টা করুন। প্রয়োজন হলে প্ল্যাটফর্মের সঙ্গে যোগাযোগ করুন।'
  }
}
