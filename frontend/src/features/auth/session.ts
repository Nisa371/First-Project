const key = 'verified-career-session'
// Tab-scoped persistence; credentials and profile data are never stored.
export const session = {
  token: () => sessionStorage.getItem(key),
  save: (token: string) => sessionStorage.setItem(key, token),
  clear: () => sessionStorage.removeItem(key),
}
