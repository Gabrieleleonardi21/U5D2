package it.epicode.bacheche.config;

import it.epicode.bacheche.security.UtenteCorrenteResolver;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Configurazione della parte REST: CORS e il resolver dell'utente corrente.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

	private final UtenteCorrenteResolver utenteCorrenteResolver;
	private final String allowedOrigin;

	public WebConfig(UtenteCorrenteResolver utenteCorrenteResolver,
			@Value("${app.cors.allowed-origin}") String allowedOrigin) {
		this.utenteCorrenteResolver = utenteCorrenteResolver;
		this.allowedOrigin = allowedOrigin;
	}

	/**
	 * Senza questa registrazione, @UtenteCorrente sarebbe un'annotazione
	 * che non fa niente: Spring proverebbe a costruire un AppUser dal corpo
	 * della richiesta e fallirebbe.
	 */
	@Override
	public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
		resolvers.add(utenteCorrenteResolver);
	}

	/**
	 * CORS: il browser blocca le chiamate fetch verso un'origine diversa
	 * (5173 -> 8080) se il server non dichiara di accettarle.
	 *
	 * allowedHeaders("*") e' necessario perche' mandiamo Authorization, che non
	 * e' fra gli header "semplici": senza, la preflight OPTIONS fallirebbe e in
	 * console si leggerebbe un errore CORS anche se il backend e' perfetto.
	 *
	 * Nota: questo vale SOLO per le chiamate REST. L'endpoint /ws ha la sua
	 * autorizzazione separata, dentro WebSocketConfig.
	 */
	@Override
	public void addCorsMappings(CorsRegistry registry) {
		registry.addMapping("/api/**")
				.allowedOrigins(allowedOrigin)
				.allowedMethods("GET", "POST", "PATCH", "DELETE", "OPTIONS")
				.allowedHeaders("*");
	}
}
