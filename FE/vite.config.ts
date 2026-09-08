import react from '@vitejs/plugin-react'
import { defineConfig } from 'vite'

// Configurazione minima: il server di sviluppo gira sulla 5173, che e' l'indirizzo
// dichiarato nel backend (app.cors.allowed-origin e setAllowedOrigins su /ws).
// Se cambi porta qui, devi cambiarla anche la', altrimenti l'handshake risponde 403.
export default defineConfig({
  plugins: [react()],
  server: { port: 5173 },
})
