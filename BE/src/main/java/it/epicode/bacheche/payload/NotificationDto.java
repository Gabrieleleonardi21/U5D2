package it.epicode.bacheche.payload;

import it.epicode.bacheche.model.Notification;
import java.time.Instant;

/**
 * La notifica come la vede la campanella.
 *
 * Contiene il "riassunto" che la consegna chiede: bacheca, autore e anteprima
 * del testo. L'anteprima e' tagliata: nella campanella non serve il messaggio
 * intero, e mandare 500 caratteri per riga rende il pannello illeggibile.
 *
 * letta: qui diventa un booleano, mentre sul database e' una data.
 * Al frontend serve solo sapere se accendere la riga; il "quando" resta al backend.
 */
public record NotificationDto(
		Long id,
		String topic,
		String autore,
		String anteprima,
		Instant createdAt,
		boolean letta) {

	private static final int LUNGHEZZA_ANTEPRIMA = 80;

	public static NotificationDto da(Notification n) {
		var messaggio = n.getMessage();
		return new NotificationDto(
				n.getId(),
				messaggio.getTopic().getName(),
				messaggio.getAuthor().getUsername(),
				anteprimaDi(messaggio.getText()),
				n.getCreatedAt(),
				// La conversione data -> booleano: null significa "non letta".
				n.getReadAt() != null);
	}

	private static String anteprimaDi(String testo) {
		if (testo.length() <= LUNGHEZZA_ANTEPRIMA) {
			return testo;
		}
		return testo.substring(0, LUNGHEZZA_ANTEPRIMA) + "...";
	}
}
