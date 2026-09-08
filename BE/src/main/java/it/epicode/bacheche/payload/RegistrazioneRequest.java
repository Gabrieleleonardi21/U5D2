package it.epicode.bacheche.payload;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Il corpo di POST /api/auth/register.
 *
 * E' un record: classe immutabile con costruttore, getter, equals e toString
 * generati dal compilatore. Per un payload e' perfetto, perche' un payload
 * si legge e basta, non si modifica.
 *
 * Le annotazioni di validazione fanno rispondere 400 PRIMA che il controller
 * venga eseguito: senza, un username vuoto arriverebbe fino al database e
 * l'errore sarebbe un 500 incomprensibile.
 */
public record RegistrazioneRequest(

		@NotBlank(message = "lo username e' obbligatorio")
		@Size(min = 3, max = 60, message = "lo username deve avere fra 3 e 60 caratteri")
		String username,

		// La password non ha un massimo stretto: BCrypt produce sempre 60 caratteri
		// qualunque sia la lunghezza in ingresso. Il minimo serve a non accettare "a".
		@NotBlank(message = "la password e' obbligatoria")
		@Size(min = 6, max = 72, message = "la password deve avere fra 6 e 72 caratteri")
		String password) {
}
