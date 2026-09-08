package it.epicode.bacheche.payload;

/**
 * La risposta di GET /api/notifications/unread-count: { "count": 3 }.
 *
 * Perche' un oggetto e non il numero nudo? Perche' un JSON che e' solo "3"
 * non e' estendibile: il giorno in cui servisse aggiungere, per esempio,
 * la data dell'ultima notifica, cambierebbe il tipo della risposta e il
 * frontend si romperebbe. Con un oggetto si aggiunge un campo e basta.
 */
public record ContatoreDto(long count) {
}
