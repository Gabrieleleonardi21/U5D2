// I tipi ricalcano i record del backend: se cambia un DTO in Java, si cambia qui.
// Tenerli in un file solo evita di riscrivere la stessa forma in ogni componente.

export type Sessione = {
  username: string
  token: string
}

export type TopicView = {
  name: string
  title: string
  description: string
  iscritti: number
  /** Se sono iscritto IO: decide se mostrare il bottone o il form di scrittura. */
  iscritto: boolean
}

export type MessageDto = {
  id: number
  topic: string
  autore: string
  text: string
  createdAt: string // ISO in UTC
}

export type NotificationDto = {
  id: number
  topic: string
  autore: string
  anteprima: string
  createdAt: string
  letta: boolean
}

/** La forma della Page di Spring Data: a noi interessa quasi sempre solo content. */
export type Pagina<T> = {
  content: T[]
  totalElements: number
  number: number
}
