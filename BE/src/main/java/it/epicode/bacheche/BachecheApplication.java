package it.epicode.bacheche;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Punto di ingresso dell'applicazione.
 *
 * @SpringBootApplication e' tre annotazioni in una:
 *  - @Configuration        questa classe puo' definire bean;
 *  - @EnableAutoConfiguration  Spring guarda cosa c'e' nel classpath (JPA, WebSocket,
 *    Postgres...) e configura da solo quello che serve;
 *  - @ComponentScan        cerca @Component/@Service/@RestController a partire da
 *    QUESTO package e da tutti i sottopackage.
 *
 * L'ultimo punto e' il motivo per cui questa classe sta nella radice di
 * it.epicode.bacheche: se la spostassimo piu' in basso, meta' del progetto
 * non verrebbe piu' trovata e i bean risulterebbero "misteriosamente" assenti.
 */
@SpringBootApplication
public class BachecheApplication {

	public static void main(String[] args) {
		SpringApplication.run(BachecheApplication.class, args);
	}
}
