package it.epicode.bacheche.web;

import it.epicode.bacheche.model.AppUser;
import it.epicode.bacheche.payload.MessageDto;
import it.epicode.bacheche.payload.MessaggioRequest;
import it.epicode.bacheche.security.UtenteCorrente;
import it.epicode.bacheche.service.MessageService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * PASSO 4 + PASSO 5 - Pubblicazione e consegna.
 *
 * Qui si vede il punto piu' importante del progetto: il messaggio arriva con una
 * POST REST (che puo' fallire con 403 o 404 e dirlo al client), e solo DOPO che
 * il salvataggio e' andato a buon fine partono i frame sul canale.
 */
@RestController
@RequestMapping("/api/topics/{nome}/messages")
public class MessageController {

	/** Il pennello per scrivere sul canale: e' Spring a fornirlo, gia' pronto. */
	private final SimpMessagingTemplate messaging;
	private final MessageService messageService;

	public MessageController(SimpMessagingTemplate messaging, MessageService messageService) {
		this.messaging = messaging;
		this.messageService = messageService;
	}

	/**
	 * POST /api/topics/{nome}/messages -> 201, il messaggio creato.
	 * 400 testo vuoto o troppo lungo, 401 senza token, 403 se non sei iscritto,
	 * 404 se il topic non esiste.
	 */
	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public MessageDto pubblica(@PathVariable String nome,
			@RequestBody @Valid MessaggioRequest corpo,
			@UtenteCorrente AppUser utente) {

		// 1. Il salvataggio, dentro la sua transazione. Se la regola sugli iscritti
		//    non e' rispettata, qui vola l'eccezione e sotto non si arriva mai:
		//    nessun frame parte per un messaggio che non e' stato salvato.
		var esito = messageService.pubblica(nome, utente, corpo.text());

		// Da qui in poi la transazione ha gia' fatto commit: quello che consegniamo
		// esiste davvero sul database. Un frame inviato non si puo' richiamare indietro,
		// quindi l'ordine di queste due fasi non e' un dettaglio.

		// 2. LA PRIMA DESTINAZIONE: il feed pubblico della bacheca.
		//    Lo riceve chiunque abbia quella pagina aperta, anche chi non e' iscritto:
		//    la lettura e' libera, e' la scrittura a essere riservata.
		messaging.convertAndSend("/topic/feed/" + nome, esito.messaggio());

		// 3. LA SECONDA DESTINAZIONE: una notifica personale per ogni iscritto.
		//    convertAndSendToUser scrive su /user/queue/notifications, e Spring
		//    traduce quel /user nella coda privata delle sessioni di quella persona.
		//    Il nome usato qui deve essere lo stesso messo come Principal dal
		//    StompAuthInterceptor, altrimenti la consegna finisce nel vuoto.
		//
		//    Se nessuna sessione corrisponde (utente offline), il messaggio viene
		//    scartato senza eccezioni e senza log: la riga salvata su notifications
		//    resta l'unica garanzia, ed e' per questo che la salviamo.
		for (var consegna : esito.consegne()) {
			messaging.convertAndSendToUser(
					consegna.destinatario(),
					"/queue/notifications",
					consegna.notifica());
		}

		// 4. La risposta HTTP a chi ha scritto. Chi pubblica riceve il messaggio
		//    due volte (qui e dal feed): nel frontend si gestisce controllando l'id
		//    prima di appendere, oppure ignorando i frame dei propri messaggi.
		return esito.messaggio();
	}

	/**
	 * GET /api/topics/{nome}/messages?page=0&size=20 -> lo storico, dal piu' recente.
	 *
	 * I due parametri hanno un default, cosi' la chiamata senza query string funziona.
	 * PageRequest.of(page, size) costruisce la richiesta di pagina; l'ordinamento
	 * e' gia' scritto nella query del repository.
	 */
	@GetMapping
	public Page<MessageDto> storico(@PathVariable String nome,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size,
			// Il parametro sembra inutilizzato, e invece fa una cosa sola ma importante:
			// obbliga la richiesta ad avere un token valido, altrimenti il resolver
			// lancia l'eccezione e questo metodo non parte. La lettura resta libera
			// per tutti gli utenti registrati, iscritti o no.
			@UtenteCorrente AppUser utente) {

		return messageService.storico(nome, PageRequest.of(page, size));
	}
}
