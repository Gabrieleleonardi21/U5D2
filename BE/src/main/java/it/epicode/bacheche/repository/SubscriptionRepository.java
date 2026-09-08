package it.epicode.bacheche.repository;

import it.epicode.bacheche.model.AppUser;
import it.epicode.bacheche.model.Subscription;
import it.epicode.bacheche.model.Topic;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {

	/**
	 * La domanda "questa persona puo' scrivere qui?". La fa MessageService
	 * prima di ogni pubblicazione: se risponde false -> 403.
	 */
	boolean existsByUserAndTopic(AppUser user, Topic topic);

	/**
	 * Il conteggio degli iscritti mostrato nell'elenco.
	 * Spring Data deriva la query dal nome: SELECT count(*) ... WHERE topic_id = ?
	 * Non teniamo una colonna contatore sul topic: andrebbe aggiornata a mano
	 * a ogni iscrizione e prima o poi qualcuno se ne dimentica; da quel momento mente.
	 */
	long countByTopic(Topic topic);

	/**
	 * La cancellazione dell'iscrizione. Restituisce quante righe ha tolto:
	 * 0 significa "non eri iscritto" e ci serve per rispondere 404 invece di
	 * fingere che sia andato tutto bene.
	 *
	 * Chi chiama deve essere @Transactional: una delete derivata senza transazione
	 * aperta fallisce con TransactionRequiredException.
	 */
	long deleteByUserAndTopic(AppUser user, Topic topic);

	/**
	 * I nomi dei topic a cui l'utente e' iscritto, in UNA sola query.
	 *
	 * E' il campo "iscritto" dell'elenco: il frontend deve sapere se mostrare
	 * il bottone Iscriviti o il form di scrittura. L'alternativa sbagliata sarebbe
	 * chiamare existsByUserAndTopic dentro un ciclo, cioe' una query per topic.
	 */
	@Query("SELECT s.topic.name FROM Subscription s WHERE s.user = :user")
	List<String> nomiTopicIscritti(AppUser user);

	/**
	 * I destinatari delle notifiche: tutti gli iscritti al topic tranne l'autore.
	 *
	 * L'esclusione dell'autore si fa QUI, in SQL, non in Java dopo aver caricato
	 * tutti: cosi' la lista che arriva e' gia' quella giusta e non si rischia
	 * di dimenticare il filtro in un ramo del codice.
	 */
	@Query("SELECT s.user FROM Subscription s WHERE s.topic = :topic AND s.user <> :autore")
	List<AppUser> iscrittiDiversiDa(Topic topic, AppUser autore);
}
