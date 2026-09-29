import { useTheme } from './useTheme'
import { useIsTrade } from '../trade/useTradeText'
export function ThemeToggle({ trade: tradeOverride, bilingual = false }: { trade?: boolean; bilingual?: boolean } = {}) {
  const { theme, toggle } = useTheme(), tradeUser = useIsTrade()
  const trade = tradeOverride ?? tradeUser
  const englishLabel = theme === 'dark' ? 'Light theme' : 'Dark theme'
  const banglaLabel = theme === 'dark' ? 'হালকা থিম' : 'ডার্ক থিম'
  const label = trade ? (theme === 'dark' ? 'হালকা থিম' : 'গাঢ় থিম') : (theme === 'dark' ? 'Light theme' : 'Dark theme')
  return <button type="button" className="button-secondary gap-2" onClick={toggle} aria-label={bilingual ? `${englishLabel}, ${banglaLabel}` : label} title={bilingual ? `${englishLabel}, ${banglaLabel}` : label}><span aria-hidden="true">{theme === 'dark' ? '☀' : '☾'}</span><span>{bilingual ? <><span className="block">{englishLabel}</span><span lang="bn" className="block text-xs font-normal leading-snug opacity-80">{banglaLabel}</span></> : label}</span></button>
}
