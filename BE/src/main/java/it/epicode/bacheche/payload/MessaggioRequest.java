package it.epicode.bacheche.payload;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Il corpo di POST /api/topics/{name}/messages.
 *
 * @NotBlank rifiuta sia "" sia "   ": un messaggio di soli spazi e' vuoto
 * per chi lo legge, e deve valere 400 come da consegna.
 * Il 500 e' lo stesso limite dichiarato sulla colonna dell'entita'.
 */
public record MessaggioRequest(

		@NotBlank(message = "il testo non puo' essere vuoto")
		@Size(max = 500, message = "il testo non puo' superare i 500 caratteri")
		String text) {
}
