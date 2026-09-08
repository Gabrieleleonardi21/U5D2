package it.epicode.bacheche.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import lombok.Getter;

/**
 * L'iscrizione di un utente a un topic. E' la tabella che porta il peso del progetto:
 * risponde alle due domande centrali della consegna.
 *  1. Chi puo' scrivere in un topic?   -> chi ha una riga qui.
 *  2. A chi va consegnata la notifica? -> a tutte le righe di quel topic, tranne l'autore.
 */
@Entity
@Table(
		name = "subscriptions",
		// IL VINCOLO PIU' IMPORTANTE DEL PROGETTO.
		// La stessa persona non puo' risultare iscritta due volte allo stesso topic.
		// Non basta controllare "esiste gia'?" nel service: se due clic arrivano
		// nello stesso istante, entrambi i controlli passano e nascono due righe.
		// Il database invece non sbaglia: il secondo INSERT viola il vincolo e noi
		// traduciamo quell'errore in un 409 (vedi ExceptionsHandler).
		uniqueConstraints = @UniqueConstraint(
				name = "uk_subscription_user_topic",
				columnNames = {"user_id", "topic_id"}))
@Getter
public class Subscription {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	/**
	 * @ManyToOne: molte iscrizioni puntano allo stesso utente.
	 * FetchType.LAZY: l'utente collegato viene caricato solo se qualcuno lo chiede.
	 * Il default di @ManyToOne sarebbe EAGER, cioe' una JOIN a ogni lettura anche
	 * quando dell'utente non ci importa nulla: con tante iscrizioni si paga caro.
	 */
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id", nullable = false)
	private AppUser user;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "topic_id", nullable = false)
	private Topic topic;

	@Column(name = "subscribed_at", nullable = false, updatable = false)
	private Instant subscribedAt;

	protected Subscription() {
	}

	public Subscription(AppUser user, Topic topic) {
		this.user = user;
		this.topic = topic;
		this.subscribedAt = Instant.now();
	}
}
