package it.epicode.bacheche.security;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotazione da mettere su un parametro di un metodo di controller:
 *
 *     public MessageDto pubblica(@UtenteCorrente AppUser utente, ...)
 *
 * Chi la legge e' UtenteCorrenteResolver, che traduce l'header Authorization
 * nell'utente che sta chiamando. Il vantaggio si vede nei controller: nessuno
 * di loro tocca gli header, e nessun endpoint puo' "dimenticarsi" di controllare
 * il token, perche' senza utente non riceve nemmeno il parametro.
 *
 * RUNTIME: l'annotazione deve essere visibile mentre il programma gira, non solo
 * al compilatore, altrimenti Spring non la vedrebbe.
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface UtenteCorrente {
}
