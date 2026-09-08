package it.epicode.bacheche.service;

import it.epicode.bacheche.exceptions.NonTrovatoException;
import it.epicode.bacheche.model.AppUser;
import it.epicode.bacheche.model.Notification;
import it.epicode.bacheche.payload.NotificationDto;
import it.epicode.bacheche.repository.NotificationRepository;
import java.time.Instant;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * IL CONTATORE, E CHI ERA OFFLINE.
 *
 * Tutti i metodi partono dall'utente autenticato, mai da un id passato nella
 * richiesta: le notifiche di un altro utente non devono essere raggiungibili
 * nemmeno indicandone l'identificativo. La query parte dal token.
 */
@Service
public class NotificationService {

	private final NotificationRepository notifiche;

	public NotificationService(NotificationRepository notifiche) {
		this.notifiche = notifiche;
	}

	/**
	 * GET /api/notifications -> le mie, dalla piu' recente.
	 *
	 * E' la risposta alla domanda "cosa mi sono perso mentre ero offline":
	 * il frame STOMP di allora e' andato perduto, la riga sul database no.
	 */
	@Transactional(readOnly = true)
	public Page<NotificationDto> mie(AppUser utente, Pageable pagina) {
		return notifiche.mieDi(utente, pagina).map(NotificationDto::da);
	}

	/** GET /api/notifications/unread-count -> il numero del badge. */
	@Transactional(readOnly = true)
	public long nonLette(AppUser utente) {
		return notifiche.countByRecipientAndReadAtIsNull(utente);
	}

	/**
	 * POST /api/notifications/read-all -> 204.
	 *
	 * Una sola UPDATE che tocca tutte le righe non lette, invece di caricarle
	 * una per una e salvarle una per una. Con show-sql attivo si vede nel
	 * terminale: una riga di log, non cento.
	 */
	@Transactional
	public void segnaTutteLette(AppUser utente) {
		notifiche.segnaTutteLette(utente, Instant.now());
	}

	/**
	 * PATCH /api/notifications/{id}/read -> 204. (Bonus della consegna.)
	 *
	 * Il controllo sul destinatario e' la parte importante: senza, chiunque
	 * potrebbe segnare lette le notifiche di un altro provando gli id a caso.
	 * Rispondiamo 404 e non 403 di proposito: una notifica che non e' tua
	 * e' come se non esistesse, e cosi' non riveliamo che quell'id esiste.
	 */
	@Transactional
	public void segnaLetta(Long id, AppUser utente) {

		Notification notifica = notifiche.findById(id)
				.filter(n -> n.getRecipient().getId().equals(utente.getId()))
				.orElseThrow(() -> new NonTrovatoException("notifica non trovata"));

		// Solo se non era gia' letta: altrimenti sposteremmo in avanti la data
		// della prima lettura a ogni clic.
		if (notifica.getReadAt() == null) {
			notifica.setReadAt(Instant.now());
			// Nessun save(): l'oggetto e' "managed" dentro la transazione, e Hibernate
			// scrive da solo la UPDATE al commit (dirty checking).
		}
	}
}
