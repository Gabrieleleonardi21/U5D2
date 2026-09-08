package it.epicode.bacheche.service;

import it.epicode.bacheche.exceptions.CredenzialiErrateException;
import it.epicode.bacheche.exceptions.NomeGiaPresoException;
import it.epicode.bacheche.model.AppUser;
import it.epicode.bacheche.payload.AuthResponse;
import it.epicode.bacheche.payload.LoginRequest;
import it.epicode.bacheche.payload.RegistrazioneRequest;
import it.epicode.bacheche.repository.AppUserRepository;
import it.epicode.bacheche.security.TokenStore;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * PASSO 2 - REGISTRAZIONE E LOGIN.
 *
 * La password non viene mai salvata in chiaro e non esce mai dal backend:
 * entra, diventa un hash, e da quel momento esiste solo l'hash.
 */
@Service
public class AuthService {

	private final AppUserRepository utenti;
	private final PasswordEncoder encoder;
	private final TokenStore tokens;

	public AuthService(AppUserRepository utenti, PasswordEncoder encoder, TokenStore tokens) {
		this.utenti = utenti;
		this.encoder = encoder;
		this.tokens = tokens;
	}

	/**
	 * POST /api/auth/register -> 201 { username, token }
	 *
	 * Chi si registra e' subito dentro: restituiamo il token senza obbligarlo
	 * a rifare il login. E' la stessa risposta del login, cosi' il frontend
	 * tratta i due casi con una funzione sola.
	 */
	@Transactional
	public AuthResponse registra(RegistrazioneRequest richiesta) {

		// Controllo "gentile": serve a dare un messaggio chiaro (409 nome preso).
		// La garanzia vera resta il vincolo unique sulla colonna: due registrazioni
		// simultanee possono superare entrambe questo if, e in quel caso il secondo
		// INSERT viene fermato dal database e ExceptionsHandler lo traduce in 409.
		if (utenti.existsByUsername(richiesta.username())) {
			throw new NomeGiaPresoException(richiesta.username());
		}

		// encode() applica BCrypt: dentro la stringa prodotta ci sono l'algoritmo,
		// il costo e il sale casuale. Due utenti con la stessa password ottengono
		// hash diversi.
		String hash = encoder.encode(richiesta.password());

		AppUser nuovo = utenti.save(new AppUser(richiesta.username(), hash));

		return new AuthResponse(nuovo.getUsername(), tokens.crea(nuovo.getUsername()));
	}

	/**
	 * POST /api/auth/login -> 200 { username, token }
	 *
	 * readOnly = true: dichiara a Hibernate che qui non si scrive.
	 * Evita il "dirty checking" a fine transazione e rende esplicita l'intenzione.
	 */
	@Transactional(readOnly = true)
	public AuthResponse login(LoginRequest richiesta) {

		AppUser utente = utenti.findByUsername(richiesta.username())
				// Stessa eccezione del caso "password sbagliata", di proposito:
				// due messaggi diversi permetterebbero di scoprire quali username
				// esistono provandoli uno a uno.
				.orElseThrow(CredenzialiErrateException::new);

		// matches() ricava il sale dall'hash salvato e ricalcola con quello.
		// Confrontare encoder.encode(password) con l'hash NON funzionerebbe mai:
		// il sale nuovo sarebbe diverso e le due stringhe non coinciderebbero.
		if (!encoder.matches(richiesta.password(), utente.getPasswordHash())) {
			throw new CredenzialiErrateException();
		}

		// Un token nuovo a ogni login. I precedenti restano validi: e' quello che
		// permette di tenere due schede aperte con lo stesso utente durante la prova.
		return new AuthResponse(utente.getUsername(), tokens.crea(utente.getUsername()));
	}
}
