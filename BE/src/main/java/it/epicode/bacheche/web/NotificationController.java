package it.epicode.bacheche.web;

import it.epicode.bacheche.model.AppUser;
import it.epicode.bacheche.payload.ContatoreDto;
import it.epicode.bacheche.payload.NotificationDto;
import it.epicode.bacheche.security.UtenteCorrente;
import it.epicode.bacheche.service.NotificationService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Gli endpoint della campanella.
 *
 * Nessuno di questi metodi accetta un "destinatario" come parametro:
 * il destinatario e' sempre e solo l'utente del token. E' la differenza fra
 * "le notifiche sono private" e "le notifiche sono private se il frontend
 * chiede quelle giuste".
 */
@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

	private final NotificationService notificationService;

	public NotificationController(NotificationService notificationService) {
		this.notificationService = notificationService;
	}

	/** GET /api/notifications?page=0&size=20 -> le mie, dalla piu' recente. */
	@GetMapping
	public Page<NotificationDto> mie(@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size,
			@UtenteCorrente AppUser utente) {
		return notificationService.mie(utente, PageRequest.of(page, size));
	}

	/**
	 * GET /api/notifications/unread-count -> { "count": 3 }
	 *
	 * E' la chiamata che risolve il caso "ero offline": al rientro il canale non
	 * ha niente da consegnare, ma questo numero viene dal database ed e' corretto.
	 */
	@GetMapping("/unread-count")
	public ContatoreDto nonLette(@UtenteCorrente AppUser utente) {
		return new ContatoreDto(notificationService.nonLette(utente));
	}

	/** POST /api/notifications/read-all -> 204. Una sola UPDATE, non N. */
	@PostMapping("/read-all")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void segnaTutteLette(@UtenteCorrente AppUser utente) {
		notificationService.segnaTutteLette(utente);
	}

	/**
	 * PATCH /api/notifications/{id}/read -> 204. (Bonus.)
	 *
	 * PATCH e non PUT: stiamo modificando un solo campo della notifica,
	 * non sostituendola per intero.
	 */
	@PatchMapping("/{id}/read")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void segnaLetta(@PathVariable Long id, @UtenteCorrente AppUser utente) {
		notificationService.segnaLetta(id, utente);
	}
}
