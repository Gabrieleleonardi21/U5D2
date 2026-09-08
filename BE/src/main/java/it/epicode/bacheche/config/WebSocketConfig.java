package it.epicode.bacheche.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * PASSO 5 - IL CANALE.
 *
 * Serve una sola classe di configurazione: dichiara dove si apre la connessione
 * e quali destinazioni riconosce il broker. Il resto lo fa Spring.
 *
 * COS'E' STOMP: un protocollo di messaggi che gira DENTRO la WebSocket.
 * La WebSocket resta un tubo che trasporta stringhe; STOMP e' l'accordo su come
 * sono fatte quelle stringhe (frame CONNECT, SUBSCRIBE, SEND, MESSAGE...).
 * Grazie a quell'accordo esiste una libreria che le legge al posto nostro,
 * sia lato server sia lato browser.
 *
 * @EnableWebSocketMessageBroker, non @EnableWebSocket: non registriamo un handler
 * scritto da noi, registriamo un endpoint STOMP e lasciamo smistare a un broker.
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

	private final String allowedOrigin;
	private final StompAuthInterceptor authInterceptor;

	public WebSocketConfig(@Value("${app.cors.allowed-origin}") String allowedOrigin,
			StompAuthInterceptor authInterceptor) {
		this.allowedOrigin = allowedOrigin;
		this.authInterceptor = authInterceptor;
	}

	/**
	 * L'indirizzo del canale: uno solo, /ws. E' quello che il browser apre
	 * con new Client({ brokerURL: 'ws://localhost:8080/ws' }).
	 *
	 * setAllowedOrigins va SEMPRE dichiarato. Se manca, l'handshake risponde 403
	 * e in console del browser si legge soltanto "WebSocket connection failed",
	 * che sembra un problema di rete e non lo e'. E' l'errore che fa perdere
	 * piu' tempo di tutti.
	 *
	 * Qui NON usiamo withSockJS(): il client della consegna si collega con
	 * ws:// diretto. Se aggiungessimo SockJS, l'indirizzo lato client dovrebbe
	 * diventare http://localhost:8080/ws e il Client STOMP andrebbe configurato
	 * con webSocketFactory: sono due mondi che non si mescolano a meta'.
	 */
	@Override
	public void registerStompEndpoints(StompEndpointRegistry registry) {
		registry.addEndpoint("/ws").setAllowedOrigins(allowedOrigin);
	}

	/**
	 * Le due destinazioni del progetto, piu' il prefisso che le rende private.
	 */
	@Override
	public void configureMessageBroker(MessageBrokerRegistry registry) {
		// (1) Il broker "semplice": in memoria, dentro questo processo.
		// Tutto cio' che parte verso /topic o /queue lo smista lui.
		// Convenzione: /topic = a molti (chi si iscrive riceve), /queue = personale.
		//   /topic/feed/{nome}     il feed pubblico di una bacheca
		//   /queue/notifications   il riassunto che accende la campanella
		// Con piu' server questa riga diventerebbe enableStompBrokerRelay
		// verso un broker vero (RabbitMQ, ActiveMQ), perche' una mappa in memoria
		// non e' condivisa fra due istanze.
		registry.enableSimpleBroker("/topic", "/queue");

		// (2) Il prefisso che rende privata una destinazione.
		// Il server pubblica per l'utente "lucia" su /queue/notifications e Spring
		// riscrive quella destinazione in una coda unica per le sessioni di lucia.
		// Il client si iscrive a /user/queue/notifications e riceve solo la sua roba:
		// nessun altro puo' iscriversi alla coda di lucia.
		registry.setUserDestinationPrefix("/user");

		// COSA MANCA DI PROPOSITO: setApplicationDestinationPrefixes("/app").
		// In questo progetto non esiste nessun @MessageMapping. I messaggi arrivano
		// al server con una POST REST, perche' la richiesta deve poter FALLIRE
		// con un 403 o un 404 e il client deve saperlo. Su un frame STOMP un errore
		// non ha dove tornare: il canale serve a consegnare, non a chiedere.
	}

	/**
	 * Tutto quello che arriva dai client passa da questo canale: qui infiliamo
	 * l'interceptor che da' un'identita' alla connessione.
	 */
	@Override
	public void configureClientInboundChannel(ChannelRegistration registration) {
		registration.interceptors(authInterceptor);
	}
}
