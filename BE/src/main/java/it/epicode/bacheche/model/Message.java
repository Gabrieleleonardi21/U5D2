package it.epicode.bacheche.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;

/**
 * Un messaggio pubblicato su una bacheca.
 *
 * Il messaggio e' il dato che RESTA: il frame WebSocket serve a consegnarlo
 * subito a chi e' collegato, non a conservarlo. Chi apre la pagina dopo
 * lo ritrova perche' e' qui, non perche' era passato dal canale.
 */
@Entity
@Table(
		name = "messages",
		// La lettura piu' frequente e' "lo storico di questo topic, dal piu' recente".
		// Senza indice il database dovrebbe leggere tutta la tabella e poi ordinarla;
		// con l'indice su (topic_id, created_at) le righe sono gia' nell'ordine giusto.
		indexes = @Index(name = "idx_messages_topic_created", columnList = "topic_id, created_at"))
@Getter
public class Message {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "topic_id", nullable = false)
	private Topic topic;

	/** L'autore. Serve anche per escluderlo dai destinatari delle notifiche. */
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "author_id", nullable = false)
	private AppUser author;

	/**
	 * 500 caratteri come da consegna. Il limite e' scritto due volte:
	 * qui (cosi' lo garantisce il database) e nel payload con @Size
	 * (cosi' l'utente riceve un 400 chiaro invece di un errore SQL).
	 */
	@Column(nullable = false, length = 500)
	private String text;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	protected Message() {
	}

	public Message(Topic topic, AppUser author, String text) {
		this.topic = topic;
		this.author = author;
		this.text = text;
		this.createdAt = Instant.now();
	}
}
