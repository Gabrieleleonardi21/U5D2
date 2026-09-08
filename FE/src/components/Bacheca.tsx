import { useEffect, useState } from 'react'
import { elencoTopic, pubblica, storico } from '../services/api'
import { ascolta } from '../services/stompClient'
import type { MessageDto } from '../types/tipi'

type Props = {
  nome: string
  token: string
  onIndietro: () => void
}

/**
 * Terza schermata: la pagina di una bacheca.
 *
 * Due sorgenti per la stessa lista:
 *  - lo storico letto una volta dal database (quello che c'era prima);
 *  - il feed sul canale, che appende i messaggi nuovi senza ricaricare.
 */
export function Bacheca({ nome, token, onIndietro }: Props) {
  const [messaggi, setMessaggi] = useState<MessageDto[]>([])
  const [testo, setTesto] = useState('')
  const [iscritto, setIscritto] = useState(false)
  const [errore, setErrore] = useState<string | null>(null)

  // 1. Lo storico e lo stato dell'iscrizione, dal database.
  useEffect(() => {
    async function carica() {
      try {
        setMessaggi(await storico(nome, token))
        const topic = await elencoTopic(token)
        setIscritto(topic.find((t) => t.name === nome)?.iscritto ?? false)
      } catch (e) {
        setErrore(e instanceof Error ? e.message : 'errore imprevisto')
      }
    }
    void carica()
  }, [nome, token])

  // 2. Il feed dal vivo di QUESTA bacheca.
  useEffect(() => {
    return ascolta<MessageDto>(`/topic/feed/${nome}`, (messaggio) => {
      setMessaggi((precedenti) => {
        // Chi scrive riceve il proprio messaggio due volte: nella risposta HTTP
        // e da questo frame. Il controllo sull'id evita il duplicato in pagina.
        if (precedenti.some((m) => m.id === messaggio.id)) return precedenti
        return [...precedenti, messaggio]
      })
    })
    // La funzione restituita da ascolta viene chiamata quando cambia bacheca:
    // senza, resteremmo in ascolto anche del feed precedente.
  }, [nome])

  async function invia() {
    setErrore(null)
    try {
      const creato = await pubblica(nome, testo, token)
      setTesto('')
      // Lo aggiungiamo subito, senza aspettare il frame: l'interfaccia risponde
      // all'istante. Se il frame arriva dopo, il controllo sull'id lo scarta.
      setMessaggi((precedenti) =>
        precedenti.some((m) => m.id === creato.id) ? precedenti : [...precedenti, creato],
      )
    } catch (e) {
      // Qui compare il 403 del backend: "non sei iscritto al topic 'java'".
      setErrore(e instanceof Error ? e.message : 'errore imprevisto')
    }
  }

  return (
    <section>
      <button type="button" onClick={onIndietro}>
        ← Tutte le bacheche
      </button>

      <h2>{nome}</h2>

      <ul className="messaggi">
        {messaggi.map((m) => (
          <li key={m.id}>
            <strong>{m.autore}</strong>{' '}
            <small>{new Date(m.createdAt).toLocaleTimeString('it-IT')}</small>
            <div>{m.text}</div>
          </li>
        ))}
      </ul>

      {/* Il form e' disabilitato per chi non e' iscritto. E' solo una cortesia
          dell'interfaccia: la regola vera sta nel service, e chi provasse a
          chiamare l'endpoint a mano riceverebbe comunque un 403. */}
      <div className="scrittura">
        <textarea
          value={testo}
          onChange={(e) => setTesto(e.target.value)}
          maxLength={500}
          disabled={!iscritto}
          placeholder={iscritto ? 'Scrivi un messaggio' : 'Iscriviti alla bacheca per scrivere'}
        />
        <button type="button" onClick={invia} disabled={!iscritto || testo.trim() === ''}>
          Pubblica
        </button>
      </div>

      {errore && <p className="errore">{errore}</p>}
    </section>
  )
}
