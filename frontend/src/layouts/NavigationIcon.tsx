import type { NavigationIcon as IconName } from './navigation'
const paths: Record<IconName, string> = {
  home: 'm3 10 9-7 9 7v10a1 1 0 0 1-1 1h-5v-7H9v7H4a1 1 0 0 1-1-1z',
  user: 'M20 21v-2a6 6 0 0 0-6-6h-4a6 6 0 0 0-6 6v2 M16 6a4 4 0 1 1-8 0 4 4 0 0 1 8 0',
  file: 'M14 2H5v20h14V7z M14 2v6h5 M8 12h8 M8 16h8',
  jobs: 'M8 6V3h8v3 M3 7h18v14H3z M3 12l9 3 9-3 M12 12v5',
  search: 'M21 21l-6-6 M17 10a7 7 0 1 1-14 0 7 7 0 0 1 14 0',
  clipboard: 'M9 4H5v18h14V4h-4 M9 2h6v4H9z M8 11h8 M8 16h6',
  shield: 'M12 2 3 6v6c0 5 9 10 9 10s9-5 9-10V6z m-5 10 3 3 6-6',
  calendar: 'M3 5h18v16H3z M7 2v6 M17 2v6 M3 11h18 M7 15h2 M13 15h2',
  users: 'M16 21v-2a5 5 0 0 0-5-5H7a5 5 0 0 0-5 5v2 M13 6a4 4 0 1 1-8 0 4 4 0 0 1 8 0 M17 3a4 4 0 0 1 0 8 M22 21v-2a5 5 0 0 0-4-5',
  refresh: 'M20 7a9 9 0 0 0-15-2L2 8 M2 2v6h6 M4 17a9 9 0 0 0 15 2l3-3 M22 22v-6h-6',
  training: 'm2 9 10-6 10 6-10 6z M6 12v6l6 3 6-3v-6 M22 9v9',
  bell: 'M18 8a6 6 0 0 0-12 0c0 8-3 8-3 10h18c0-2-3-2-3-10 M10 21h4',
  settings: 'M9 3h6l1 3 3 1 2 5-2 5-3 1-1 3H9l-1-3-3-1-2-5 2-5 3-1z M15 12a3 3 0 1 1-6 0 3 3 0 0 1 6 0',
  payment: 'M2 5h20v14H2z M2 10h20 M6 15h4',
  skills: 'm14 6 4 4 4-4a7 7 0 0 1-9 9l-6 6-4-4 6-6a7 7 0 0 1 9-9z',
  menu: 'M4 6h16 M4 12h16 M4 18h16', close: 'm6 6 12 12 M18 6 6 18', left: 'm14 6-6 6 6 6', right: 'm10 6 6 6-6 6',
}
export function NavigationIcon({ name }: { name: IconName }) {
  return <svg aria-hidden="true" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.7" strokeLinecap="round" strokeLinejoin="round" className="shrink-0"><path d={paths[name]} /></svg>
}
