package it.epicode.bacheche.exceptions;

/**
 * 409 - la richiesta e' valida ma va contro lo stato attuale:
 * ti stai iscrivendo a un topic a cui sei gia' iscritto.
 *
 * Non e' un 400: il corpo e' corretto. Non e' un 403: hai il permesso.
 * E' semplicemente gia' successo.
 */
public class ConflittoException extends RuntimeException {

	public ConflittoException(String messaggio) {
		super(messaggio);
	}
}
