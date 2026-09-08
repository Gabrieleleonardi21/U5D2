package it.epicode.bacheche.exceptions;

/**
 * 401 - "non so chi sei".
 * Manca l'header Authorization, oppure il token non e' fra quelli validi
 * (per esempio perche' il backend e' stato riavviato: il TokenStore vive in memoria).
 */
public class NonAutenticatoException extends RuntimeException {

	public NonAutenticatoException(String messaggio) {
		super(messaggio);
	}
}
