package it.epicode.bacheche.payload;

import it.epicode.bacheche.model.Message;
import java.time.Instant;

/**
 * Il messaggio come esce dal backend: sia nelle risposte REST sia nel frame
 * inviato su /topic/feed/{nome}.
 *
 * E' importante che la forma sia la STESSA nei due casi: il frontend appende
 * in fondo alla lista un oggetto identico a quelli che ha letto dallo storico,
 * senza doppie conversioni e senza campi che compaiono solo a volte.
 *
 * Nota cosa NON c'e': l'hash della password dell'autore, l'id interno del topic,
 * le altre iscrizioni. Restituire l'entita' li' avrebbe portati fuori tutti.
 */
public record MessageDto(
		Long id,
		String topic,
		String autore,
		String text,
		Instant createdAt) {

	/**
	 * Il metodo di conversione sta qui, accanto alla forma dei dati, cosi' non
	 * viene riscritto in ogni controller.
	 *
	 * Attenzione: legge m.getAuthor() e m.getTopic(), che sono LAZY. Va chiamato
	 * mentre la sessione e' aperta (dentro il service) oppure su entita' caricate
	 * con JOIN FETCH, come fanno le query dei repository.
	 */
	public static MessageDto da(Message m) {
		return new MessageDto(
				m.getId(),
				m.getTopic().getName(),
				m.getAuthor().getUsername(),
				m.getText(),
				m.getCreatedAt());
	}
}
