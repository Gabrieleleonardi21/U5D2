package it.epicode.bacheche.payload;

/**
 * Una bacheca come la vede l'elenco: GET /api/topics.
 *
 * "iscritti" e "iscritto" non esistono sull'entita' Topic: sono informazioni
 * che nascono dalle iscrizioni e dipendono da CHI sta guardando. E' esattamente
 * il motivo per cui esiste un DTO e non restituiamo l'entita' cosi' com'e'.
 */
public record TopicView(
		String name,
		String title,
		String description,
		/** Quante persone sono iscritte: uguale per tutti. */
		long iscritti,
		/** Se sono iscritto IO: cambia da utente a utente, e decide cosa mostra il frontend. */
		boolean iscritto) {
}
