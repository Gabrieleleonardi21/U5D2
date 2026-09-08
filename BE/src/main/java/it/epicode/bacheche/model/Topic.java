package it.epicode.bacheche.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;

/**
 * Una bacheca a tema (java, spring, frontend).
 *
 * I tre topic li inserisce data.sql all'avvio: non esiste un endpoint per crearli,
 * perche' la consegna non lo chiede (e' fra i bonus).
 */
@Entity
@Table(name = "topics")
@Getter
public class Topic {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	/**
	 * Il nome corto in minuscolo: e' quello che compare negli URL
	 * (/api/topics/java/messages) e nelle destinazioni STOMP (/topic/feed/java).
	 * Unico, perche' e' di fatto la chiave con cui il mondo esterno lo nomina.
	 */
	@Column(nullable = false, unique = true, length = 30)
	private String name;

	/** Il titolo leggibile mostrato nell'elenco delle bacheche. */
	@Column(nullable = false, length = 100)
	private String title;

	@Column(length = 300)
	private String description;

	protected Topic() {
	}

	public Topic(String name, String title, String description) {
		this.name = name;
		this.title = title;
		this.description = description;
	}
}
