package it.epicode.bacheche.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;

/**
 * L'utente registrato.
 *
 * PERCHE' LA CLASSE SI CHIAMA AppUser E LA TABELLA users
 * "user" e' una parola riservata in Postgres (SELECT user restituisce l'utente
 * di connessione). Se lasciassimo che Hibernate chiamasse la tabella "user",
 * ogni query andrebbe messa fra virgolette. Chiamiamo la tabella "users" e
 * la classe AppUser, cosi' non serve nessun trucco.
 */
@Entity
@Table(name = "users")
@Getter // Lombok genera i getter. I setter NON li generiamo: dopo la creazione
        // nessuno deve poter cambiare username o hash da fuori.
public class AppUser {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	// IDENTITY: l'id lo genera il database al momento dell'INSERT.
	// Non lo scegliamo noi, quindi non ha senso nemmeno poterlo impostare.
	private Long id;

	/**
	 * Il nome utente e' anche l'identita' che viaggia sul canale WebSocket:
	 * e' il nome che l'interceptor mette come Principal e che
	 * convertAndSendToUser usa per trovare la coda privata.
	 * Deve essere unico, altrimenti due persone riceverebbero le stesse notifiche.
	 */
	@Column(nullable = false, unique = true, length = 60)
	private String username;

	/**
	 * L'hash BCrypt della password, mai la password.
	 * 100 caratteri: BCrypt ne produce 60, teniamo margine per un eventuale
	 * cambio di algoritmo senza dover migrare la colonna.
	 */
	@Column(name = "password_hash", nullable = false, length = 100)
	private String passwordHash;

	/**
	 * updatable = false: la data di creazione si scrive una volta sola.
	 * Se per errore qualcuno provasse a modificarla, Hibernate ignorerebbe la modifica.
	 */
	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	/**
	 * Costruttore vuoto per Hibernate: gli serve per ricostruire l'oggetto quando
	 * legge una riga. E' protected perche' serve a lui, non a noi.
	 */
	protected AppUser() {
	}

	/**
	 * Il costruttore che usiamo noi: chiede tutto quello che serve perche'
	 * l'utente sia valido, cosi' non e' possibile crearne uno a meta'.
	 * La data la mette l'oggetto stesso: non e' un dato che arriva da fuori.
	 */
	public AppUser(String username, String passwordHash) {
		this.username = username;
		this.passwordHash = passwordHash;
		this.createdAt = Instant.now();
	}
}
