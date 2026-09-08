package it.epicode.bacheche.web;

import it.epicode.bacheche.model.AppUser;
import it.epicode.bacheche.payload.TopicView;
import it.epicode.bacheche.security.UtenteCorrente;
import it.epicode.bacheche.service.TopicService;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * PASSO 3 - Elenco delle bacheche e iscrizioni.
 *
 * Il parametro @UtenteCorrente e' quello che rende protetti questi endpoint:
 * se il token manca o non vale, il resolver lancia l'eccezione e il metodo
 * non parte nemmeno.
 */
@RestController
@RequestMapping("/api")
public class TopicController {

	private final TopicService topicService;

	public TopicController(TopicService topicService) {
		this.topicService = topicService;
	}

	/**
	 * GET /api/topics -> [{ name, title, description, iscritti, iscritto }]
	 * L'elenco e' uguale per tutti tranne il campo "iscritto", che dipende dal token.
	 */
	@GetMapping("/topics")
	public List<TopicView> elenco(@UtenteCorrente AppUser utente) {
		return topicService.elenco(utente);
	}

	/** GET /api/me/subscriptions -> ["java", "spring"] */
	@GetMapping("/me/subscriptions")
	public List<String> mieIscrizioni(@UtenteCorrente AppUser utente) {
		return topicService.mieIscrizioni(utente);
	}

	/**
	 * POST /api/topics/{nome}/subscription -> 201 (iscrizione creata).
	 * 404 se il topic non esiste, 409 se ero gia' iscritto.
	 *
	 * L'iscrizione e' una risorsa che nasce, quindi POST e 201. Non restituiamo
	 * un corpo: il frontend ricarica l'elenco e ha gia' tutto quello che gli serve.
	 */
	@PostMapping("/topics/{nome}/subscription")
	@ResponseStatus(HttpStatus.CREATED)
	public void iscrivi(@PathVariable String nome, @UtenteCorrente AppUser utente) {
		topicService.iscrivi(nome, utente);
	}

	/**
	 * DELETE /api/topics/{nome}/subscription -> 204 (iscrizione rimossa).
	 *
	 * 204 = "fatto, e non ho niente da dirti". E' la risposta giusta per una
	 * cancellazione andata a buon fine: un corpo vuoto con 200 sarebbe piu' ambiguo.
	 */
	@DeleteMapping("/topics/{nome}/subscription")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void disiscrivi(@PathVariable String nome, @UtenteCorrente AppUser utente) {
		topicService.disiscrivi(nome, utente);
	}
}
