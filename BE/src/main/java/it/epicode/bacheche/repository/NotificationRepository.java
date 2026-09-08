package it.epicode.bacheche.repository;

import it.epicode.bacheche.model.AppUser;
import it.epicode.bacheche.model.Notification;
import java.time.Instant;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

	/**
	 * Le notifiche di UNA persona, dalla piu' recente.
	 * Il destinatario non arriva mai come parametro della richiesta: arriva dal
	 * token. Cosi' non esiste modo di leggere le notifiche di qualcun altro.
	 *
	 * Il JOIN FETCH tira su messaggio, autore e topic: servono tutti per costruire
	 * la riga della campanella ("mario ha scritto su java: ...").
	 */
	@Query(value = """
			SELECT n FROM Notification n
			JOIN FETCH n.message m
			JOIN FETCH m.author
			JOIN FETCH m.topic
			WHERE n.recipient = :destinatario
			ORDER BY n.createdAt DESC
			""",
			countQuery = "SELECT count(n) FROM Notification n WHERE n.recipient = :destinatario")
	Page<Notification> mieDi(AppUser destinatario, Pageable pageable);

	/** Il numero del badge. Lo chiediamo al database, non lo teniamo da parte. */
	long countByRecipientAndReadAtIsNull(AppUser recipient);

	/**
	 * "Segna tutte come lette" con UNA sola UPDATE che tocca N righe,
	 * invece di N cicli di lettura + salvataggio. Su cento notifiche
	 * e' cento volte piu' veloce, e si vede nei log con show-sql attivo.
	 *
	 * @Modifying avvisa Spring Data che questa query scrive: senza, verrebbe
	 * eseguita come una SELECT e fallirebbe.
	 * La query e' in JPQL: lavora sulle classi (Notification, n.readAt),
	 * non sulle tabelle.
	 */
	@Modifying
	@Query("UPDATE Notification n SET n.readAt = :ora WHERE n.recipient = :destinatario AND n.readAt IS NULL")
	int segnaTutteLette(AppUser destinatario, Instant ora);
}
