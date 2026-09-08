import { useEffect, useState } from 'react'
import { Bacheca } from './components/Bacheca'
import { Campanella } from './components/Campanella'
import { ElencoBacheche } from './components/ElencoBacheche'
import { Login } from './components/Login'
import { attiva, disattiva } from './services/stompClient'
import type { Sessione } from './types/tipi'

/**
 * Il componente che tiene insieme le quattro schermate.
 *
 * La sessione (username + token) vive QUI, perche' e' l'unico punto da cui
 * scende sia verso le chiamate REST sia verso il canale. Il token non lo
 * salviamo in localStorage: il TokenStore del backend vive in memoria e al
 * riavvio del server tutti i token diventano invalidi, quindi ricaricare la
 * pagina e ritrovarsi "loggati" con un token morto sarebbe peggio che rifare
 * il login (e' scritto anche nel README).
 */
export function App() {
  const [sessione, setSessione] = useState<Sessione | null>(null)
  const [bachecaAperta, setBachecaAperta] = useState<string | null>(null)

  /**
   * Il canale si accende DOPO il login, quando il token esiste: e' il token
   * a dare un'identita' alla connessione, e senza identita' le notifiche
   * personali non arriverebbero.
   */
  useEffect(() => {
    if (!sessione) return
    attiva(sessione.token)
    return () => disattiva()
  }, [sessione])

  if (!sessione) {
    return (
      <main>
        <Login onEntrato={setSessione} />
      </main>
    )
  }

  return (
    <>
      <header>
        <h1>Bacheche</h1>
        <div className="spalla">
          <Campanella token={sessione.token} />
          <span className="nota">{sessione.username}</span>
          <button type="button" onClick={() => setSessione(null)}>
            Esci
          </button>
        </div>
      </header>

      <main>
        {bachecaAperta === null ? (
          <ElencoBacheche token={sessione.token} onApri={setBachecaAperta} />
        ) : (
          <Bacheca
            nome={bachecaAperta}
            token={sessione.token}
            onIndietro={() => setBachecaAperta(null)}
          />
        )}
      </main>
    </>
  )
}
