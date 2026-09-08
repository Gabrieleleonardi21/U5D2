package it.epicode.bacheche.repository;

import it.epicode.bacheche.model.AppUser;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Il repository e' il punto in cui si parla con il database.
 * E' un'interfaccia senza implementazione: Spring Data legge i nomi dei metodi
 * all'avvio e genera lui il codice. Da JpaRepository arrivano gratis
 * save, findById, findAll, delete...
 */
public interface AppUserRepository extends JpaRepository<AppUser, Long> {

	/**
	 * "find By Username" diventa SELECT * FROM users WHERE username = ?
	 * Optional invece di null: chi chiama e' costretto a decidere cosa fare
	 * quando l'utente non c'e' (nel login: 401).
	 */
	Optional<AppUser> findByUsername(String username);

	/**
	 * Usato in registrazione per rispondere 409 con un messaggio sensato.
	 * Nota: e' un controllo "gentile", non una garanzia. La garanzia vera e'
	 * il vincolo unique sulla colonna, perche' due registrazioni simultanee
	 * possono superare entrambe questo exists.
	 */
	boolean existsByUsername(String username);
}
