import { useCallback, useEffect, useState } from 'react'
import { contaNonLette, notifiche as caricaNotifiche, segnaTutteLette } from '../services/api'
import { ascolta } from '../services/stompClient'
import type { NotificationDto } from '../types/tipi'

type Props = {
  token: string
}

/** La destinazione personale: /user e' un prefisso virtuale, il broker lo riscrive. */
const DESTINAZIONE_PERSONALE = '/user/queue/notifications'

/**
 * La campanella: il contatore delle non lette e il pannello che le elenca.
 *
 * Le due sorgenti convivono, e servono a due cose diverse:
 *  - il canale accende il contatore MENTRE si e' collegati;
 *  - la chiamata REST lo rimette a posto AL RIENTRO, perche' i frame di quando
 *    si era offline sono andati persi e solo il database li ricorda.
 */
export function Campanella({ token }: Props) {
  const [nonLette, setNonLette] = useState(0)
  const [elenco, setElenco] = useState<NotificationDto[]>([])
  const [aperta, setAperta] = useState(false)

  // useCallback: la funzione non viene ricreata a ogni render, cosi' puo' stare
  // fra le dipendenze degli effetti senza farli ripartire di continuo.
  const ricaricaContatore = useCallback(async () => {
    try {
      setNonLette(await contaNonLette(token))
    } catch (e) {
      console.error('contatore non aggiornato:', e)
    }
  }, [token])

  // Al montaggio: il numero viene dal database. E' questo che fa comparire
  // il badge anche se la notifica era arrivata mentre la finestra era chiusa.
  useEffect(() => {
    void ricaricaContatore()
  }, [ricaricaContatore])

  // L'ascolto della coda personale.
  useEffect(() => {
    return ascolta<NotificationDto>(DESTINAZIONE_PERSONALE, (notifica) => {
      setNonLette((n) => n + 1)
      // Aggiorniamo anche l'elenco, cosi' aprendo il pannello si trova gia' la riga
      // senza aspettare una nuova chiamata.
      setElenco((precedenti) => [notifica, ...precedenti])
    })
    // Il return di ascolta e' la funzione di pulizia: React la chiama allo
    // smontaggio. Senza, dopo un logout continueremmo ad ascoltare.
  }, [])

  async function apriPannello() {
    setAperta((era) => !era)
    if (!aperta) {
      try {
        setElenco(await caricaNotifiche(token))
      } catch (e) {
        console.error('notifiche non caricate:', e)
      }
    }
  }

  async function tutteLette() {
    await segnaTutteLette(token)
    setNonLette(0)
    setElenco((precedenti) => precedenti.map((n) => ({ ...n, letta: true })))
  }

  return (
    <div className="campanella">
      <button type="button" onClick={apriPannello} aria-label="notifiche">
        🔔
        {nonLette > 0 && <span className="badge">{nonLette}</span>}
      </button>

      {aperta && (
        <div className="pannello">
          <div className="pannello-testa">
            <strong>Notifiche</strong>
            <button type="button" onClick={tutteLette} disabled={nonLette === 0}>
              Segna tutte lette
            </button>
          </div>

          {elenco.length === 0 ? (
            <p className="nota">Nessuna notifica. Iscriviti a una bacheca per riceverne.</p>
          ) : (
            <ul>
              {elenco.map((n) => (
                <li key={n.id} className={n.letta ? 'letta' : 'da-leggere'}>
                  <strong>{n.autore}</strong> su <code>{n.topic}</code>
                  <div>{n.anteprima}</div>
                </li>
              ))}
            </ul>
          )}
        </div>
      )}
    </div>
  )
}
