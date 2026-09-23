import { createContext, useContext } from 'react'
export type Theme = 'light' | 'dark'
export const themeKey = 'verified-career-theme'
export function initialTheme(): Theme {
  try { const saved = localStorage.getItem(themeKey); if (saved === 'light' || saved === 'dark') return saved } catch { /* Storage may be unavailable. */ }
  return window.matchMedia?.('(prefers-color-scheme: dark)').matches ? 'dark' : 'light'
}
export function apply(theme: Theme) { document.documentElement.dataset.theme = theme; document.documentElement.style.colorScheme = theme }
export const ThemeContext = createContext<{ theme: Theme; toggle: () => void } | null>(null)
export function useTheme() { const value = useContext(ThemeContext); if (!value) throw new Error('ThemeProvider is required'); return value }
