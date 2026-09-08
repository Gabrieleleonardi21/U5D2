package it.epicode.bacheche.repository;

import it.epicode.bacheche.model.Topic;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TopicRepository extends JpaRepository<Topic, Long> {

	/** I topic si cercano per nome, perche' e' il nome che viaggia nell'URL. */
	Optional<Topic> findByName(String name);

	/** Ordine stabile nell'elenco: senza ORDER BY il database non promette nulla. */
	List<Topic> findAllByOrderByNameAsc();
}
