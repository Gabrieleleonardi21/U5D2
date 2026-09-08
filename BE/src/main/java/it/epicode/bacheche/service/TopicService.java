package it.epicode.bacheche.service;

import it.epicode.bacheche.exceptions.ConflittoException;
import it.epicode.bacheche.exceptions.NonTrovatoException;
import it.epicode.bacheche.model.AppUser;
import it.epicode.bacheche.model.Subscription;
import it.epicode.bacheche.model.Topic;
import it.epicode.bacheche.payload.TopicView;
import it.epicode.bacheche.repository.SubscriptionRepository;
import it.epicode.bacheche.repository.TopicRepository;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * PASSO 3 - TOPIC E ISCRIZIONI.
 */
@Service
public class TopicService {

	private final TopicRepository topics;
	private final SubscriptionRepository iscrizioni;

	public TopicService(TopicRepository topics, SubscriptionRepository iscrizioni) {
		this.topics = topics;
		this.iscrizioni = iscrizioni;
	}

	/**
	 * GET /api/topics -> l'elenco delle bacheche, gia' personalizzato per chi guarda.
	 *
	 * Il campo "iscritto" lo calcoliamo QUI con UNA sola query sulle iscrizioni
	 * dell'utente corrente, non con una chiamata per ogni topic.
	 * Con tre topic la differenza non si vede; l'abitudine di fare query dentro
	 * un ciclo invece si paga appena l'elenco cresce (problema N+1).
	 */
	@Transactional(readOnly = true)
	public List<TopicView> elenco(AppUser utente) {

		// Set e non List: dentro il ciclo facciamo tanti contains() quanti sono i
		// topic, e su un Set costano tempo costante invece di scorrere la lista.
		Set<String> iscrittoA = Set.copyOf(iscrizioni.nomiTopicIscritti(utente));

		return topics.findAllByOrderByNameAsc().stream()
				.map(topic -> new TopicView(
						topic.getName(),
						topic.getTitle(),
						topic.getDescription(),
						// Il conteggio degli iscritti: Spring Data deriva la query
						// dal nome del metodo, non c'e' niente da scrivere a mano.
						iscrizioni.countByTopic(topic),
						iscrittoA.contains(topic.getName())))
				.toList();
	}

	/** GET /api/me/subscriptions -> i nomi dei topic a cui sono iscritto. */
	@Transactional(readOnly = true)
	public List<String> mieIscrizioni(AppUser utente) {
		return iscrizioni.nomiTopicIscritti(utente);
	}

	/**
	 * POST /api/topics/{nome}/subscription -> 201.
	 * 404 se il topic non esiste, 409 se l'iscrizione c'e' gia'.
	 */
	@Transactional
	public void iscrivi(String nomeTopic, AppUser utente) {

		Topic topic = trova(nomeTopic);

		if (iscrizioni.existsByUserAndTopic(utente, topic)) {
			throw new ConflittoException("sei gia' iscritto al topic '" + nomeTopic + "'");
		}

		// Se due clic arrivano nello stesso istante, entrambi superano l'if qui sopra:
		// il secondo save viola il vincolo unico (user_id, topic_id) e diventa 409
		// grazie all'handler su DataIntegrityViolationException. Il risultato per
		// l'utente e' identico, e sul database non nasce comunque una seconda riga.
		iscrizioni.save(new Subscription(utente, topic));
	}

	/**
	 * DELETE /api/topics/{nome}/subscription -> 204.
	 *
	 * deleteByUserAndTopic restituisce quante righe ha tolto: se sono zero
	 * l'iscrizione non c'era, e rispondere 204 sarebbe una bugia.
	 */
	@Transactional
	public void disiscrivi(String nomeTopic, AppUser utente) {

		Topic topic = trova(nomeTopic);

		long rimosse = iscrizioni.deleteByUserAndTopic(utente, topic);

		if (rimosse == 0) {
			throw new NonTrovatoException("non risulti iscritto al topic '" + nomeTopic + "'");
		}
	}

	/**
	 * Il topic si cerca sempre per nome, e sempre allo stesso modo.
	 * Il metodo privato evita di ripetere lo stesso orElseThrow in tre punti:
	 * se un giorno il messaggio del 404 cambia, cambia in un posto solo.
	 */
	private Topic trova(String nome) {
		return topics.findByName(nome)
				.orElseThrow(() -> new NonTrovatoException("il topic '" + nome + "' non esiste"));
	}
}
