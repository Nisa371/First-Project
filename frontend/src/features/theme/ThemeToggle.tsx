import { useTheme } from './useTheme'
import { useIsTrade } from '../trade/useTradeText'
export function ThemeToggle() {
  const { theme, toggle } = useTheme(), trade = useIsTrade()
  const label = trade ? (theme === 'dark' ? 'হালকা থিম' : 'গাঢ় থিম') : (theme === 'dark' ? 'Light theme' : 'Dark theme')
  return <button type="button" className="button-secondary gap-2" onClick={toggle} aria-label={label} title={label}><span aria-hidden="true">{theme === 'dark' ? '☀' : '☾'}</span><span>{label}</span></button>
}
