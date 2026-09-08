package it.epicode.bacheche.payload;

import java.util.List;

/**
 * Quello che MessageService restituisce al controller dopo aver pubblicato.
 *
 * PERCHE' NON BASTA IL MESSAGGIO.
 * Dopo il salvataggio il controller deve mandare due cose diverse su due
 * destinazioni diverse: il messaggio sul feed pubblico, e una notifica personale
 * a ogni iscritto. Se il service restituisse solo il messaggio, il controller
 * dovrebbe ricalcolare l'elenco degli iscritti con altre query, ripetendo un
 * lavoro gia' fatto dentro la transazione.
 *
 * Cosi' invece il service dice: "ecco il messaggio, ed ecco a chi va consegnato".
 */
public record RisultatoPubblicazione(

		/** Il messaggio salvato: va sia nella risposta HTTP sia su /topic/feed/{nome}. */
		MessageDto messaggio,

		/** Una consegna per ogni iscritto diverso dall'autore. */
		List<Consegna> consegne) {

	/**
	 * Il destinatario e la notifica che gli spetta.
	 *
	 * La notifica NON e' la stessa per tutti: ognuno ha la sua riga sul database,
	 * con il suo id e il suo stato di lettura. Mandare a tutti lo stesso oggetto
	 * significherebbe che, cliccando su una notifica, si segnerebbe letta la riga
	 * di qualcun altro.
	 */
	public record Consegna(String destinatario, NotificationDto notifica) {
	}
}
