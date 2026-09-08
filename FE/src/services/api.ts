import type { MessageDto, NotificationDto, Pagina, TopicView } from '../types/tipi'

// L'unico file che sa dove abita il backend.
const API = 'http://localhost:8080/api'

export type AuthResponse = { username: string; token: string }

/**
 * Tutte le chiamate passano da qui, per tre motivi:
 *  1. l'header Authorization si scrive in un posto solo (dimenticarlo su un
 *     endpoint significherebbe un 401 apparentemente inspiegabile);
 *  2. fetch() NON considera errore una risposta 403 o 404: il controllo su
 *     res.ok tocca a noi, e farlo qui vuol dire non dimenticarlo mai;
 *  3. il messaggio di errore del backend (il campo "messaggio" di ErroreDto)
 *     viene estratto e rilanciato, cosi' l'interfaccia puo' mostrare
 *     "non sei iscritto" invece di un generico "errore".
 */
async function chiedi(percorso: string, token: string | null, init?: RequestInit): Promise<Response> {
  const headers: Record<string, string> = { 'Content-Type': 'application/json' }
  if (token) headers.Authorization = `Bearer ${token}`

  const res = await fetch(`${API}${percorso}`, { ...init, headers: { ...headers, ...init?.headers } })

  if (!res.ok) {
    // Il corpo dell'errore potrebbe non essere JSON (per esempio se il backend e' spento):
    // in quel caso ripieghiamo sul codice di stato.
    const corpo = await res.json().catch(() => null)
    throw new Error(corpo?.messaggio ?? `HTTP ${res.status}`)
  }
  return res
}

// --- Autenticazione (le uniche chiamate senza token) ------------------------

export async function registra(username: string, password: string): Promise<AuthResponse> {
  const res = await chiedi('/auth/register', null, {
    method: 'POST',
    body: JSON.stringify({ username, password }),
  })
  return res.json()
}

export async function login(username: string, password: string): Promise<AuthResponse> {
  const res = await chiedi('/auth/login', null, {
    method: 'POST',
    body: JSON.stringify({ username, password }),
  })
  return res.json()
}

// --- Bacheche e iscrizioni --------------------------------------------------

export async function elencoTopic(token: string): Promise<TopicView[]> {
  const res = await chiedi('/topics', token)
  return res.json()
}

export async function iscriviti(nome: string, token: string): Promise<void> {
  await chiedi(`/topics/${nome}/subscription`, token, { method: 'POST' })
}

export async function disiscriviti(nome: string, token: string): Promise<void> {
  await chiedi(`/topics/${nome}/subscription`, token, { method: 'DELETE' })
}

// --- Messaggi ---------------------------------------------------------------

/**
 * Lo storico arriva SEMPRE da qui, anche a canale aperto: il canale porta solo
 * quello che succede da adesso in poi, non quello che c'era prima.
 */
export async function storico(nome: string, token: string): Promise<MessageDto[]> {
  const res = await chiedi(`/topics/${nome}/messages?page=0&size=20`, token)
  const pagina: Pagina<MessageDto> = await res.json()
  // Il backend restituisce dal piu' recente; in pagina li mostriamo dal piu' vecchio,
  // come in una chat, quindi invertiamo qui una volta sola.
  return pagina.content.slice().reverse()
}

export async function pubblica(nome: string, testo: string, token: string): Promise<MessageDto> {
  const res = await chiedi(`/topics/${nome}/messages`, token, {
    method: 'POST',
    body: JSON.stringify({ text: testo }),
  })
  return res.json()
}

// --- Campanella -------------------------------------------------------------

export async function notifiche(token: string): Promise<NotificationDto[]> {
  const res = await chiedi('/notifications?page=0&size=20', token)
  const pagina: Pagina<NotificationDto> = await res.json()
  return pagina.content
}

/** Il numero del badge al rientro: viene dal database, non dal canale. */
export async function contaNonLette(token: string): Promise<number> {
  const res = await chiedi('/notifications/unread-count', token)
  const corpo: { count: number } = await res.json()
  return corpo.count
}

export async function segnaTutteLette(token: string): Promise<void> {
  await chiedi('/notifications/read-all', token, { method: 'POST' })
}
