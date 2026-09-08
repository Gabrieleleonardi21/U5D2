package it.epicode.bacheche.web;

import it.epicode.bacheche.payload.AuthResponse;
import it.epicode.bacheche.payload.LoginRequest;
import it.epicode.bacheche.payload.RegistrazioneRequest;
import it.epicode.bacheche.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * I due soli endpoint che NON richiedono un token: qui il token si ottiene.
 *
 * Si riconoscono dal fatto che nessun metodo ha il parametro @UtenteCorrente:
 * in questo progetto e' quel parametro a rendere protetto un endpoint.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

	private final AuthService authService;

	public AuthController(AuthService authService) {
		this.authService = authService;
	}

	/**
	 * @Valid attiva le annotazioni del payload: se username o password mancano,
	 * il metodo non viene nemmeno eseguito e la risposta e' 400.
	 * 409 se il nome e' gia' preso (lo decide il service).
	 */
	@PostMapping("/register")
	@ResponseStatus(HttpStatus.CREATED) // 201: e' nata una risorsa nuova.
	public AuthResponse registra(@RequestBody @Valid RegistrazioneRequest richiesta) {
		return authService.registra(richiesta);
	}

	/**
	 * 200 e non 201: il login non crea nessuna risorsa, restituisce solo un token.
	 * 401 se le credenziali non combaciano.
	 */
	@PostMapping("/login")
	public AuthResponse login(@RequestBody @Valid LoginRequest richiesta) {
		return authService.login(richiesta);
	}
}
