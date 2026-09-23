import { useState, type ReactNode } from 'react'
import { ThemeContext, initialTheme, apply, themeKey, type Theme } from './useTheme'
export function ThemeProvider({ children }: { children: ReactNode }) {
  const [theme, setTheme] = useState<Theme>(() => { const value = initialTheme(); apply(value); return value })
  function toggle() { const next = theme === 'light' ? 'dark' : 'light'; apply(next); setTheme(next); try { localStorage.setItem(themeKey, next) } catch { /* Keep in-memory preference. */ } }
  return <ThemeContext.Provider value={{ theme, toggle }}>{children}</ThemeContext.Provider>
}
