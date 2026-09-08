package it.epicode.bacheche.payload;

import java.time.Instant;

/**
 * La forma unica di tutte le risposte di errore, prodotta da ExceptionsHandler.
 *
 * Avere una forma sola vuol dire che il frontend puo' scrivere una funzione
 * sola per mostrare gli errori, invece di indovinare ogni volta dove sta il messaggio.
 */
public record ErroreDto(
		int status,
		String messaggio,
		Instant timestamp) {

	public static ErroreDto di(int status, String messaggio) {
		return new ErroreDto(status, messaggio, Instant.now());
	}
}
