package it.epicode.bacheche.service;

import it.epicode.bacheche.exceptions.NonIscrittoException;
import it.epicode.bacheche.exceptions.NonTrovatoException;
import it.epicode.bacheche.model.AppUser;
import it.epicode.bacheche.model.Message;
import it.epicode.bacheche.model.Notification;
import it.epicode.bacheche.model.Topic;
import it.epicode.bacheche.payload.MessageDto;
import it.epicode.bacheche.payload.NotificationDto;
import it.epicode.bacheche.payload.RisultatoPubblicazione;
import it.epicode.bacheche.repository.MessageRepository;
import it.epicode.bacheche.repository.NotificationRepository;
import it.epicode.bacheche.repository.SubscriptionRepository;
import it.epicode.bacheche.repository.TopicRepository;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * PASSO 4 - I MESSAGGI E LA REGOLA SUGLI ISCRITTI.
 *
 * La regola vive QUI, nel service, dentro una transazione:
 * chi non e' iscritto non scrive, e riceve un 403.
 *
 * Perche' nel service e non nel controller: il controller sa parlare HTTP,
 * il service sa cosa significa "pubblicare". Se domani la pubblicazione
 * partisse anche da un job o da un import, la regola sarebbe ancora rispettata.
 */
@Service
public class MessageService {

	private final TopicRepository topics;
	private final MessageRepository messaggi;
	private final SubscriptionRepository iscrizioni;
	private final NotificationRepository notifiche;

	public MessageService(TopicRepository topics,
			MessageRepository messaggi,
			SubscriptionRepository iscrizioni,
			NotificationRepository notifiche) {
		this.topics = topics;
		this.messaggi = messaggi;
		this.iscrizioni = iscrizioni;
		this.notifiche = notifiche;
	}

	/**
	 * Pubblica un messaggio e prepara le notifiche.
	 *
	 * @Transactional: le due scritture (il messaggio e le N notifiche) stanno o
	 * cadono insieme. Se il salvataggio delle notifiche fallisse a meta', anche
	 * il messaggio verrebbe annullato: meglio "non pubblicato" che "pubblicato ma
	 * invisibile a meta' degli iscritti".
	 *
	 * Nota cosa NON c'e' qui dentro: nessun invio sul canale. I frame partono dal
	 * controller, DOPO che questo metodo e' finito e la transazione ha fatto commit.
	 * Il motivo e' semplice: un frame inviato non si puo' richiamare indietro,
	 * quindi si consegna solo cio' che sul database esiste davvero.
	 */
	@Transactional
	public RisultatoPubblicazione pubblica(String nomeTopic, AppUser autore, String testo) {

		Topic topic = topics.findByName(nomeTopic)
				.orElseThrow(() -> new NonTrovatoException("il topic '" + nomeTopic + "' non esiste"));

		// LA REGOLA. 403 e non 401: sappiamo benissimo chi e' (ha un token valido),
		// semplicemente qui non ha il permesso di scrivere.
		if (!iscrizioni.existsByUserAndTopic(autore, topic)) {
			throw new NonIscrittoException(nomeTopic);
		}

		Message salvato = messaggi.save(new Message(topic, autore, testo));

		// Una notifica per ogni iscritto, TRANNE l'autore: chi scrive non si notifica
		// da solo. L'esclusione la fa la query, non un if dimenticabile.
		List<AppUser> destinatari = iscrizioni.iscrittiDiversiDa(topic, autore);

		List<Notification> nuove = destinatari.stream()
				.map(destinatario -> new Notification(destinatario, salvato))
				.toList();

		// saveAll: un solo viaggio verso il database invece di N save separate.
		List<Notification> create = notifiche.saveAll(nuove);

		// Costruiamo i DTO adesso, mentre la sessione Hibernate e' ancora aperta:
		// con open-in-view: false, farlo nel controller darebbe LazyInitializationException
		// appena si prova a leggere topic o autore.
		List<RisultatoPubblicazione.Consegna> consegne = create.stream()
				.map(n -> new RisultatoPubblicazione.Consegna(
						n.getRecipient().getUsername(),
						NotificationDto.da(n)))
				.toList();

		return new RisultatoPubblicazione(MessageDto.da(salvato), consegne);
	}

	/**
	 * GET /api/topics/{nome}/messages -> lo storico, paginato, dal piu' recente.
	 *
	 * Lo storico si legge SEMPRE dal database, anche quando il canale e' aperto:
	 * il canale porta solo quello che succede da adesso in poi. Chi apre la pagina
	 * ora deve vedere anche i messaggi di ieri.
	 *
	 * La lettura non richiede l'iscrizione: la consegna dice che gli altri possono
	 * leggere, e ricevono un errore solo se provano a scrivere.
	 */
	@Transactional(readOnly = true)
	public Page<MessageDto> storico(String nomeTopic, Pageable pagina) {

		Topic topic = topics.findByName(nomeTopic)
				.orElseThrow(() -> new NonTrovatoException("il topic '" + nomeTopic + "' non esiste"));

		// Page.map trasforma il contenuto ma conserva i dati della paginazione
		// (numero di pagina, totale elementi): il frontend li riceve senza che
		// dobbiamo ricostruirli a mano.
		return messaggi.storicoDi(topic, pagina).map(MessageDto::da);
	}
}
