package it.epicode.bacheche.payload;

/**
 * La risposta di registrazione e login.
 *
 * Il token e' opaco: e' solo un identificativo casuale, non contiene dati.
 * Il frontend lo tiene e lo rimanda in due posti diversi:
 *  - nell'header Authorization di ogni chiamata REST;
 *  - nell'header del frame CONNECT quando apre il canale WebSocket.
 *
 * Restituiamo anche lo username perche' il frontend deve sapere chi e'
 * (per esempio per riconoscere i propri messaggi) senza decodificare niente.
 */
public record AuthResponse(String username, String token) {
}
