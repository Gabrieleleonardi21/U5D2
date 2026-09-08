package it.epicode.bacheche.exceptions;

import it.epicode.bacheche.payload.ErroreDto;
import java.util.stream.Collectors;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Il traduttore da eccezioni a codici HTTP.
 *
 * @RestControllerAdvice: questa classe vede le eccezioni lanciate da TUTTI
 * i controller. Il vantaggio e' che nei service possiamo lanciare l'eccezione
 * che descrive il PROBLEMA (non sei iscritto) senza sapere nulla di HTTP,
 * e nei controller non c'e' un solo try/catch.
 *
 * La tabella dei codici della consegna vive qui, in un posto solo.
 */
@RestControllerAdvice
public class ExceptionsHandler {

	/** 401: manca il token, o non e' valido. */
	@ExceptionHandler(NonAutenticatoException.class)
	@ResponseStatus(HttpStatus.UNAUTHORIZED)
	public ErroreDto nonAutenticato(NonAutenticatoException ex) {
		return ErroreDto.di(401, ex.getMessage());
	}

	/** 401: login con credenziali sbagliate. */
	@ExceptionHandler(CredenzialiErrateException.class)
	@ResponseStatus(HttpStatus.UNAUTHORIZED)
	public ErroreDto credenziali(CredenzialiErrateException ex) {
		return ErroreDto.di(401, ex.getMessage());
	}

	/** 403: autenticato, ma senza il permesso di scrivere in quel topic. */
	@ExceptionHandler(NonIscrittoException.class)
	@ResponseStatus(HttpStatus.FORBIDDEN)
	public ErroreDto nonIscritto(NonIscrittoException ex) {
		return ErroreDto.di(403, ex.getMessage());
	}

	/** 404: topic o iscrizione inesistenti. */
	@ExceptionHandler(NonTrovatoException.class)
	@ResponseStatus(HttpStatus.NOT_FOUND)
	public ErroreDto nonTrovato(NonTrovatoException ex) {
		return ErroreDto.di(404, ex.getMessage());
	}

	/** 409: nome utente gia' preso, iscrizione doppia. */
	@ExceptionHandler({NomeGiaPresoException.class, ConflittoException.class})
	@ResponseStatus(HttpStatus.CONFLICT)
	public ErroreDto conflitto(RuntimeException ex) {
		return ErroreDto.di(409, ex.getMessage());
	}

	/**
	 * 409 anche qui, ma per la strada di sotto.
	 *
	 * Questa eccezione arriva dal DATABASE quando salta un vincolo unico:
	 * due clic su "Iscriviti" nello stesso istante, oppure due registrazioni
	 * simultanee con lo stesso nome. I controlli exists() nei service passano
	 * entrambi, e a fermare il secondo INSERT e' il vincolo.
	 *
	 * Senza questo handler quel caso diventerebbe un 500: il server "si rompe"
	 * per una situazione che invece abbiamo previsto e gestito.
	 */
	@ExceptionHandler(DataIntegrityViolationException.class)
	@ResponseStatus(HttpStatus.CONFLICT)
	public ErroreDto vincoloViolato(DataIntegrityViolationException ex) {
		return ErroreDto.di(409, "operazione gia' effettuata");
	}

	/**
	 * 400: e' fallita la validazione di @Valid.
	 *
	 * Raccogliamo tutti i messaggi, non solo il primo: se mancano username e
	 * password, l'utente deve saperlo in un colpo solo e non uno alla volta.
	 */
	@ExceptionHandler(MethodArgumentNotValidException.class)
	@ResponseStatus(HttpStatus.BAD_REQUEST)
	public ErroreDto validazione(MethodArgumentNotValidException ex) {
		String messaggi = ex.getBindingResult().getFieldErrors().stream()
				.map(errore -> errore.getField() + ": " + errore.getDefaultMessage())
				.collect(Collectors.joining("; "));
		return ErroreDto.di(400, messaggi);
	}
}
