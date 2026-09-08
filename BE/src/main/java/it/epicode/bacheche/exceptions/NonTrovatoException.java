package it.epicode.bacheche.exceptions;

/**
 * 404 - la risorsa indicata nell'URL non esiste: un topic che non c'e',
 * un'iscrizione da cancellare che non e' mai stata creata.
 *
 * Una sola classe per tutti i 404: il messaggio dice cosa manca, e non serve
 * una gerarchia di eccezioni per distinguere casi che il client tratta uguale.
 */
public class NonTrovatoException extends RuntimeException {

	public NonTrovatoException(String messaggio) {
		super(messaggio);
	}
}
