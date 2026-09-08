package it.epicode.bacheche.security;

import it.epicode.bacheche.exceptions.NonAutenticatoException;
import it.epicode.bacheche.model.AppUser;
import it.epicode.bacheche.repository.AppUserRepository;
import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

/**
 * COME IL BACKEND RICONOSCE CHI STA CHIAMANDO (Passo 2 della consegna).
 *
 * Il client manda in ogni richiesta:  Authorization: Bearer <token>
 * Qui il token viene tradotto nell'utente, e il controller riceve direttamente
 * un AppUser gia' pronto.
 *
 * Se manca l'header, se non e' nel formato giusto, o se il token non e' nel
 * TokenStore -> NonAutenticatoException, che ExceptionsHandler trasforma in 401.
 *
 * Questa e' la versione "a mano" di quello che farebbe Spring Security:
 * l'abbiamo scritta noi perche' nell'esercizio non usiamo il suo starter.
 */
@Component
public class UtenteCorrenteResolver implements HandlerMethodArgumentResolver {

	private static final String PREFISSO = "Bearer ";

	private final TokenStore tokens;
	private final AppUserRepository utenti;

	// Iniezione dal costruttore (niente @Autowired sui campi): i campi possono
	// essere final e la classe non puo' esistere senza le sue dipendenze.
	public UtenteCorrenteResolver(TokenStore tokens, AppUserRepository utenti) {
		this.tokens = tokens;
		this.utenti = utenti;
	}

	/**
	 * Spring chiede: "questo parametro lo gestisci tu?".
	 * Rispondiamo di si' solo se ha la nostra annotazione ED e' un AppUser:
	 * cosi' non intercettiamo per sbaglio parametri di altri tipi.
	 */
	@Override
	public boolean supportsParameter(MethodParameter parameter) {
		return parameter.hasParameterAnnotation(UtenteCorrente.class)
				&& AppUser.class.equals(parameter.getParameterType());
	}

	/** Qui si costruisce il valore che il controller ricevera'. */
	@Override
	public Object resolveArgument(MethodParameter parameter,
			ModelAndViewContainer mavContainer,
			NativeWebRequest webRequest,
			WebDataBinderFactory binderFactory) {

		String header = webRequest.getHeader("Authorization");

		if (header == null || !header.startsWith(PREFISSO)) {
			throw new NonAutenticatoException("manca l'header Authorization: Bearer <token>");
		}

		// substring(7): togliamo "Bearer " e resta il token.
		String token = header.substring(PREFISSO.length()).trim();

		String username = tokens.utenteDi(token)
				.orElseThrow(() -> new NonAutenticatoException("token non valido o scaduto"));

		// Il token dice il nome; l'utente vero lo prendiamo comunque dal database.
		// Se qualcuno fosse stato cancellato, il token non deve continuare a valere.
		return utenti.findByUsername(username)
				.orElseThrow(() -> new NonAutenticatoException("l'utente del token non esiste piu'"));
	}
}
