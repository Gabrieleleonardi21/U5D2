import { useState } from 'react'
import { login, registra } from '../services/api'
import type { Sessione } from '../types/tipi'

type Props = {
  onEntrato: (sessione: Sessione) => void
}

/**
 * Prima schermata: registrazione e login.
 *
 * I due casi condividono il form perche' il backend chiede lo stesso corpo e
 * risponde la stessa cosa ({ username, token }). Cambia solo l'endpoint.
 */
export function Login({ onEntrato }: Props) {
  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [errore, setErrore] = useState<string | null>(null)
  const [inCorso, setInCorso] = useState(false)

  async function invia(nuovoUtente: boolean) {
    setErrore(null)
    setInCorso(true)
    try {
      const risposta = nuovoUtente
        ? await registra(username, password)
        : await login(username, password)

      // Il token risale ad App: e' li' che vive la sessione, perche' serve
      // sia alle chiamate REST sia all'attivazione del canale.
      onEntrato({ username: risposta.username, token: risposta.token })
    } catch (e) {
      // Qui arrivano i messaggi veri del backend: "il nome utente 'mario'
      // e' gia' preso" (409) oppure "credenziali non valide" (401).
      setErrore(e instanceof Error ? e.message : 'errore imprevisto')
    } finally {
      setInCorso(false)
    }
  }

  return (
    <section className="riquadro stretto">
      <h1>Bacheche</h1>
      <p className="nota">Registrati o entra per iscriverti alle bacheche e scrivere.</p>

      <label>
        Nome utente
        <input value={username} onChange={(e) => setUsername(e.target.value)} autoComplete="username" />
      </label>

      <label>
        Password
        <input
          type="password"
          value={password}
          onChange={(e) => setPassword(e.target.value)}
          autoComplete="current-password"
        />
      </label>

      <div className="bottoni">
        {/* disabled durante la chiamata: evita il doppio invio, che in registrazione
            produrrebbe un 409 confuso invece di un secondo utente. */}
        <button type="button" onClick={() => invia(false)} disabled={inCorso}>
          Entra
        </button>
        <button type="button" onClick={() => invia(true)} disabled={inCorso}>
          Registrati
        </button>
      </div>

      {errore && <p className="errore">{errore}</p>}
    </section>
  )
}
