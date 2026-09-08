import { useEffect, useState } from 'react'
import { disiscriviti, elencoTopic, iscriviti } from '../services/api'
import type { TopicView } from '../types/tipi'

type Props = {
  token: string
  onApri: (nome: string) => void
}

/**
 * Seconda schermata: l'elenco delle bacheche con titolo, numero di iscritti
 * e il bottone per iscriversi o disiscriversi.
 *
 * Il campo "iscritto" arriva gia' calcolato dal backend: il frontend non deve
 * incrociare due elenchi per capire dove si trova.
 */
export function ElencoBacheche({ token, onApri }: Props) {
  const [topic, setTopic] = useState<TopicView[]>([])
  const [errore, setErrore] = useState<string | null>(null)

  async function ricarica() {
    try {
      setTopic(await elencoTopic(token))
      setErrore(null)
    } catch (e) {
      setErrore(e instanceof Error ? e.message : 'errore imprevisto')
    }
  }

  useEffect(() => {
    void ricarica()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [token])

  async function cambiaIscrizione(t: TopicView) {
    try {
      if (t.iscritto) {
        await disiscriviti(t.name, token)
      } else {
        await iscriviti(t.name, token)
      }
      // Ricarichiamo dal backend invece di aggiustare lo stato a mano:
      // cosi' anche il contatore degli iscritti resta quello vero, non una stima.
      await ricarica()
    } catch (e) {
      setErrore(e instanceof Error ? e.message : 'errore imprevisto')
    }
  }

  return (
    <section>
      <h2>Bacheche</h2>
      {errore && <p className="errore">{errore}</p>}

      <ul className="elenco">
        {topic.map((t) => (
          <li key={t.name} className="riquadro">
            <div>
              <button type="button" className="titolo" onClick={() => onApri(t.name)}>
                {t.title}
              </button>
              <p className="nota">{t.description}</p>
              <p className="nota">
                {t.iscritti} {t.iscritti === 1 ? 'iscritto' : 'iscritti'}
              </p>
            </div>

            <button type="button" onClick={() => cambiaIscrizione(t)}>
              {t.iscritto ? 'Disiscriviti' : 'Iscriviti'}
            </button>
          </li>
        ))}
      </ul>
    </section>
  )
}
