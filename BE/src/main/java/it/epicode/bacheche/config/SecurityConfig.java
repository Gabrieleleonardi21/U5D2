package it.epicode.bacheche.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * L'unico pezzo di Spring Security che usiamo: il cifratore delle password.
 *
 * BCrypt fa due cose che un hash normale (SHA-256 e simili) non fa:
 *  - e' LENTO di proposito, quindi provare milioni di password al secondo
 *    contro il nostro database costa troppo tempo per essere pratico;
 *  - inserisce un "sale" casuale dentro l'hash, quindi due utenti con la stessa
 *    password hanno hash diversi e non si puo' capire che coincidono.
 *
 * Il sale sta dentro la stringa prodotta: per questo non serve una colonna in piu',
 * e per questo il confronto si fa con encoder.matches(chiaro, hash) e MAI
 * ricalcolando l'hash e confrontando le stringhe (verrebbe sempre diverso).
 *
 * Lo dichiariamo come @Bean per poterlo iniettare nei service: un solo oggetto
 * per tutta l'applicazione, invece di uno nuovo a ogni chiamata.
 */
@Configuration
public class SecurityConfig {

	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}
}
