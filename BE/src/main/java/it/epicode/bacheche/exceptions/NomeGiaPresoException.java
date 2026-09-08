package it.epicode.bacheche.exceptions;

/** 409 - lo username scelto in registrazione esiste gia'. */
public class NomeGiaPresoException extends RuntimeException {

	public NomeGiaPresoException(String username) {
		super("il nome utente '" + username + "' e' gia' preso");
	}
}
