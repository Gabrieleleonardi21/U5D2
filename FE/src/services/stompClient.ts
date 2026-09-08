import { Client, type StompSubscription } from '@stomp/stompjs'

/**
 * IL CANALE, LATO BROWSER.
 *
 * UN SOLO Client PER TUTTA L'APPLICAZIONE.
 * Un Client corrisponde a una connessione. Crearne uno per componente
 * significa aprire tante connessioni quanti sono i componenti montati:
 * e' l'errore piu' comune, e si nota solo quando le notifiche arrivano
 * in doppio o in triplo. Qui il Client e' una variabile di modulo:
 * esiste una volta sola, come la connessione che rappresenta.
 */
const client = new Client({
  // ws:// e non http://, perche' il backend registra l'endpoint SENZA SockJS.
  // Se in WebSocketConfig si aggiungesse .withSockJS(), questo indirizzo dovrebbe
  // diventare http://localhost:8080/ws e servirebbe un webSocketFactory.
  brokerURL: 'ws://localhost:8080/ws',

  // Se la connessione cade (backend riavviato, rete assente) il client riprova
  // da solo ogni 5 secondi. E' anche il motivo per cui le sottoscrizioni vanno
  // riaperte in onConnect: dopo una riconnessione il broker non ricorda nulla.
  reconnectDelay: 5000,
})

/**
 * I gestori registrati dai componenti, raggruppati per destinazione.
 * Li teniamo noi perche' devono sopravvivere alle riconnessioni: la subscribe
 * vera si rifa' ogni volta che il client torna connesso.
 */
const gestori = new Map<string, Set<(corpo: unknown) => void>>()
const aperte = new Map<string, StompSubscription>()

/** Apre la subscribe verso il broker, se non c'e' gia' e se siamo connessi. */
function apri(destinazione: string) {
  if (aperte.has(destinazione) || !client.connected) return

  const sub = client.subscribe(destinazione, (frame) => {
    // frame.body e' SEMPRE una stringa: il JSON.parse tocca a noi.
    // Dimenticarlo produce oggetti che in pagina diventano [object Object].
    const corpo = JSON.parse(frame.body)
    gestori.get(destinazione)?.forEach((gestore) => gestore(corpo))
  })

  aperte.set(destinazione, sub)
}

/**
 * Le subscribe vanno QUI, dentro onConnect, non subito dopo activate().
 * activate() non e' istantaneo: chiamare subscribe() subito dopo significa
 * chiamarla su un client non ancora connesso, e il frame SUBSCRIBE non parte.
 * Lo stesso vale dopo ogni riconnessione automatica: onConnect scatta di nuovo
 * e qui riapriamo tutto quello che era registrato.
 */
client.onConnect = () => {
  gestori.forEach((_, destinazione) => apri(destinazione))
}

/** Utile in fase di studio: gli errori del broker altrimenti restano invisibili. */
client.onStompError = (frame) => {
  console.error('Errore STOMP:', frame.headers['message'], frame.body)
}

/**
 * Accende il canale dopo il login, quando il token esiste.
 *
 * Il token va negli header del frame CONNECT: e' li' che il backend
 * (StompAuthInterceptor) lo legge per dare un'identita' alla sessione.
 * Senza, la connessione si apre lo stesso ma le notifiche personali
 * vengono scartate in silenzio.
 */
export function attiva(token: string) {
  if (client.active) return
  client.connectHeaders = { Authorization: `Bearer ${token}` }
  client.activate()
}

/** Al logout: chiude la connessione e dimentica le sottoscrizioni. */
export function disattiva() {
  aperte.forEach((sub) => sub.unsubscribe())
  aperte.clear()
  gestori.clear()
  void client.deactivate()
}

/**
 * L'unico modo in cui i componenti parlano con il canale.
 *
 * Restituisce la funzione di pulizia da usare nel return di useEffect:
 * senza, cambiando bacheca resterebbe attivo l'ascolto di quella precedente
 * e i messaggi finirebbero nella pagina sbagliata.
 */
export function ascolta<T>(destinazione: string, gestore: (corpo: T) => void): () => void {
  const perDestinazione = gestori.get(destinazione) ?? new Set()
  perDestinazione.add(gestore as (corpo: unknown) => void)
  gestori.set(destinazione, perDestinazione)

  // Se il client e' gia' connesso apriamo subito; altrimenti ci pensa onConnect.
  apri(destinazione)

  return () => {
    perDestinazione.delete(gestore as (corpo: unknown) => void)

    // La subscribe verso il broker si chiude solo quando NESSUNO ascolta piu'
    // quella destinazione: due componenti sulla stessa destinazione devono
    // poter convivere senza spegnersi a vicenda.
    if (perDestinazione.size === 0) {
      aperte.get(destinazione)?.unsubscribe()
      aperte.delete(destinazione)
      gestori.delete(destinazione)
    }
  }
}
