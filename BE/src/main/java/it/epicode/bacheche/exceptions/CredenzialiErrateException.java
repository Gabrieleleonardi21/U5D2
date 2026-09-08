package it.epicode.bacheche.exceptions;

/**
 * 401 al login.
 *
 * Il messaggio e' volutamente vago e uguale nei due casi (utente inesistente
 * o password sbagliata). Se rispondessimo "utente inesistente", chiunque
 * potrebbe scoprire quali username esistono provandoli uno per uno.
 */
public class CredenzialiErrateException extends RuntimeException {

	public CredenzialiErrateException() {
		super("credenziali non valide");
	}
}
