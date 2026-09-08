package it.epicode.bacheche.config;

import it.epicode.bacheche.security.TokenStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;

/**
 * DARE UN'IDENTITA' ALLA CONNESSIONE.
 *
 * Senza questo passaggio le notifiche personali non raggiungono nessuno,
 * e il backend non segnala nulla: e' il guasto piu' silenzioso del progetto.
 *
 * Ogni messaggio in arrivo dai client passa da preSend. A noi ne interessa uno
 * solo: il primo, il frame CONNECT. Da li' leggiamo l'header Authorization,
 * lo traduciamo in un nome utente con il TokenStore, e quel nome diventa il
 * Principal della sessione.
 *
 * Da quel momento convertAndSendToUser("lucia", ...) sa dove consegnare, perche'
 * il broker risolve le destinazioni /user in base a questo nome. Deve quindi
 * essere lo STESSO username che sta sul database: se qui mettessimo l'email o
 * l'id, il messaggio partirebbe verso una coda che nessuno ascolta.
 */
@Component
public class StompAuthInterceptor implements ChannelInterceptor {

	private static final Logger log = LoggerFactory.getLogger(StompAuthInterceptor.class);
	private static final String PREFISSO = "Bearer ";

	private final TokenStore tokens;

	public StompAuthInterceptor(TokenStore tokens) {
		this.tokens = tokens;
	}

	@Override
	public Message<?> preSend(Message<?> message, MessageChannel channel) {

		// LA LENTE GIUSTA PER LEGGERE IL FRAME.
		// MessageHeaderAccessor.getAccessor(...) restituisce l'accessor VERO, quello
		// agganciato al messaggio. StompHeaderAccessor.wrap(...) ne creerebbe una
		// COPIA: setUser lavorerebbe sulla copia, non avrebbe effetto, e nessuno
		// segnalerebbe l'errore. E' il secondo dettaglio che fa perdere piu' tempo.
		StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

		if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {

			// Il token viaggia in un header del frame CONNECT, non nella query string
			// dell'URL: un token nell'indirizzo finisce nei log di ogni proxy
			// e nella cronologia del browser.
			String header = accessor.getFirstNativeHeader("Authorization");

			if (header == null || !header.startsWith(PREFISSO)) {
				// Sessione senza Principal: il broadcast su /topic arrivera' comunque,
				// ma i messaggi personali verranno scartati in silenzio.
				// Per questo il warning c'e': e' l'unica traccia del problema.
				log.warn("CONNECT senza header Authorization: sessione anonima, "
						+ "le notifiche personali verranno scartate senza errori");
			} else {
				String token = header.substring(PREFISSO.length()).trim();

				tokens.utenteDi(token).ifPresentOrElse(
						// setUser vuole un java.security.Principal: un'interfaccia con
						// un solo metodo, getName(). Questa lambda e' l'implementazione
						// piu' piccola possibile.
						username -> {
							accessor.setUser(() -> username);
							log.debug("CONNECT riconosciuto: sessione di {}", username);
						},
						() -> log.warn("CONNECT con token non valido: sessione anonima"));
			}
		}

		// Il messaggio prosegue in ogni caso. Restituire null lo fermerebbe qui:
		// si potrebbe fare per rifiutare le connessioni senza token, ma il client
		// vedrebbe solo una connessione chiusa senza sapere perche'.
		return message;
	}
}
