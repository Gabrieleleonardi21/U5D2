package it.epicode.bacheche.security;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

/**
 * L'elenco dei token attivi: token -> nome utente.
 *
 * IL TOKEN E' OPACO. Non contiene dati (a differenza di un JWT): e' solo un
 * numero casuale che qui dentro e' associato a un nome. Chi lo intercetta non
 * puo' leggerci niente, ma chi lo possiede e' quella persona, quindi va trattato
 * come una password.
 *
 * VIVE IN MEMORIA. E' una scelta consapevole dell'esercizio, e va scritta nel
 * README: al riavvio del backend la mappa si svuota e tutti i token diventano
 * invalidi (le chiamate successive rispondono 401 e il frontend deve rifare
 * il login). In produzione questa mappa sarebbe un JWT firmato, oppure Redis.
 *
 * ConcurrentHashMap e non HashMap: le richieste HTTP arrivano su thread diversi
 * e i frame WebSocket su altri ancora. Una HashMap scritta da piu' thread puo'
 * corrompersi in modi difficili da riprodurre.
 */
@Component
public class TokenStore {

	private final Map<String, String> utentePerToken = new ConcurrentHashMap<>();

	/**
	 * Crea un token nuovo per questo utente.
	 *
	 * UUID.randomUUID() usa un generatore crittograficamente sicuro: un token
	 * prevedibile (per esempio un contatore, o il nome utente codificato)
	 * permetterebbe di indovinare la sessione di qualcun altro.
	 *
	 * Ogni login produce un token diverso e i vecchi restano validi: e' il
	 * comportamento che rende possibile la prova con due browser aperti.
	 */
	public String crea(String username) {
		String token = UUID.randomUUID().toString();
		utentePerToken.put(token, username);
		return token;
	}

	/**
	 * Da token a nome utente. Optional.empty() significa "token sconosciuto":
	 * chi chiama decide, e in questo progetto decide sempre 401.
	 */
	public Optional<String> utenteDi(String token) {
		if (token == null || token.isBlank()) {
			return Optional.empty();
		}
		return Optional.ofNullable(utentePerToken.get(token));
	}

	/** Il logout: il token smette di valere. Non e' richiesto dalla consegna, ma costa una riga. */
	public void rimuovi(String token) {
		utentePerToken.remove(token);
	}
}
