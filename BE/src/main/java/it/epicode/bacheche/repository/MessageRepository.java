package it.epicode.bacheche.repository;

import it.epicode.bacheche.model.Message;
import it.epicode.bacheche.model.Topic;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface MessageRepository extends JpaRepository<Message, Long> {

	/**
	 * Lo storico di una bacheca, dal piu' recente.
	 *
	 * Pageable aggiunge da solo LIMIT e OFFSET: senza paginazione, una bacheca
	 * con diecimila messaggi li caricherebbe tutti in memoria per mostrarne venti.
	 *
	 * JOIN FETCH carica autore e topic nella stessa query. Senza, avremmo il
	 * problema N+1: una query per la pagina e poi una query per l'autore di ogni
	 * riga mentre costruiamo i DTO (e con open-in-view: false esploderebbe proprio,
	 * con LazyInitializationException).
	 */
	@Query(value = """
			SELECT m FROM Message m
			JOIN FETCH m.author
			JOIN FETCH m.topic
			WHERE m.topic = :topic
			ORDER BY m.createdAt DESC
			""",
			// countQuery: la query per il totale delle pagine. Va scritta a mano
			// perche' un COUNT con JOIN FETCH non e' valido.
			countQuery = "SELECT count(m) FROM Message m WHERE m.topic = :topic")
	Page<Message> storicoDi(Topic topic, Pageable pageable);
}
