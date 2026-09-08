package it.epicode.bacheche.exceptions;

/**
 * 403 - "so chi sei, ma qui non puoi".
 *
 * E' la regola centrale della consegna: chi non e' iscritto puo' leggere
 * ma non scrivere. Confondere 401 e 403 e' l'errore piu' frequente:
 *   401 = non so chi sei      (manca o e' invalido il token)
 *   403 = so chi sei, ma no   (token valido, permesso assente)
 */
public class NonIscrittoException extends RuntimeException {

	public NonIscrittoException(String topic) {
		super("non sei iscritto al topic '" + topic + "': puoi leggere ma non scrivere");
	}
}
