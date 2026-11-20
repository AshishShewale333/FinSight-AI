import axios from 'axios'
import type { Envelope, TokenResponse } from './types'

const baseURL = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080/api/v1'
export const tokens = {
  get access() { return sessionStorage.getItem('finsight.access') },
  get refresh() { return sessionStorage.getItem('finsight.refresh') },
  set(value: TokenResponse) { sessionStorage.setItem('finsight.access', value.accessToken); sessionStorage.setItem('finsight.refresh', value.refreshToken) },
  clear() { sessionStorage.removeItem('finsight.access'); sessionStorage.removeItem('finsight.refresh') },
}

export const api = axios.create({ baseURL, timeout: 20000 })
api.interceptors.request.use(config => {
  const access = tokens.access
  if (access) config.headers.Authorization = `Bearer ${access}`
  return config
})
let refreshPromise: Promise<void> | null = null
api.interceptors.response.use(response => response, async error => {
  const original = error.config
  if (error.response?.status !== 401 || !original || original._retried || !tokens.refresh || original.url?.includes('/auth/')) throw error
  original._retried = true
  refreshPromise ??= axios.post<Envelope<TokenResponse>>(`${baseURL}/auth/refresh`, { refreshToken: tokens.refresh })
    .then(response => { tokens.set(response.data.data) })
    .catch(refreshError => { tokens.clear(); window.dispatchEvent(new Event('finsight:logout')); throw refreshError })
    .finally(() => { refreshPromise = null })
  await refreshPromise
  original.headers.Authorization = `Bearer ${tokens.access}`
  return api(original)
})

export async function request<T>(promise: Promise<{ data: Envelope<T> }>): Promise<T> {
  return (await promise).data.data
}
export function errorMessage(error: unknown): string {
  if (axios.isAxiosError(error)) return error.response?.data?.message ?? error.message
  return error instanceof Error ? error.message : 'Request failed'
}
