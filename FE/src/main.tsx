import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { App } from './App'
import './index.css'

/**
 * NOTA SU StrictMode.
 * In sviluppo React monta ogni componente due volte di proposito, per far emergere
 * gli effetti che non si puliscono. Lo lasciamo attivo apposta: se una subscribe
 * non venisse annullata nella funzione di pulizia, qui si vedrebbero i messaggi
 * arrivare in doppio. In produzione il doppio montaggio non avviene.
 */
createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <App />
  </StrictMode>,
)
