package it.epicode.bacheche.payload;

import jakarta.validation.constraints.NotBlank;

/**
 * Il corpo di POST /api/auth/login: stesso corpo della registrazione.
 *
 * Qui NON ripetiamo i vincoli di lunghezza: se le regole cambiassero
 * (per esempio password piu' lunghe), chi si e' registrato prima deve
 * comunque poter entrare. Al login basta che i campi ci siano.
 */
public record LoginRequest(

		@NotBlank(message = "lo username e' obbligatorio")
		String username,

		@NotBlank(message = "la password e' obbligatoria")
		String password) {
}
