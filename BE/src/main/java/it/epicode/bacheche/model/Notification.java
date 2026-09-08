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
import lombok.Setter;

/**
 * La notifica: "qualcuno ha scritto su una bacheca a cui sei iscritto".
 *
 * UNA RIGA PER DESTINATARIO, non una per messaggio.
 * Sembra uno spreco, ma e' il cuore della consegna: e' questo che permette
 * a chi era offline di ritrovare la notifica al rientro. Il frame STOMP,
 * se non c'e' nessuno collegato, viene scartato in silenzio e sparisce per sempre;
 * la riga sul database resta e la si rilegge con GET /api/notifications.
 *
 * Se salvassimo una sola riga per messaggio non sapremmo chi l'ha gia' letta.
 */
@Entity
@Table(
		name = "notifications",
		// La query piu' frequente dell'applicazione e' il contatore della campanella:
		// "quante non lette ha questa persona". Filtra sempre su recipient e read_at.
		indexes = @Index(name = "idx_notifications_recipient_read", columnList = "recipient_id, read_at"))
@Getter
public class Notification {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	/** A chi e' destinata: e' sempre un iscritto al topic, e non e' mai l'autore. */
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "recipient_id", nullable = false)
	private AppUser recipient;

	/** Il messaggio a cui si riferisce: da qui ricaviamo topic, autore e anteprima. */
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "message_id", nullable = false)
	private Message message;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	/**
	 * Non un boolean "letta si'/no" ma la data in cui e' stata letta:
	 * null = non ancora letta. Costa uguale e dice una cosa in piu' (QUANDO).
	 *
	 * E' l'unico campo mutabile della classe, quindi e' l'unico che ha il setter:
	 * un oggetto che nasce corretto e non puo' essere rovinato da fuori
	 * e' un oggetto su cui non si aprono bug.
	 */
	@Setter
	@Column(name = "read_at")
	private Instant readAt;

	protected Notification() {
	}

	public Notification(AppUser recipient, Message message) {
		this.recipient = recipient;
		this.message = message;
		this.createdAt = Instant.now();
		// readAt resta null: appena creata, la notifica non e' letta.
	}
}
