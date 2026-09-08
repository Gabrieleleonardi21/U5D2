-- I tre topic di partenza (Passo 3 della consegna).
-- Non serve nessuna schermata di amministrazione: le bacheche sono fisse.
--
-- ON CONFLICT DO NOTHING: data.sql gira a ogni avvio. Senza questa clausola,
-- al secondo avvio il vincolo unico su name farebbe fallire l'inserimento
-- e l'applicazione non partirebbe.
INSERT INTO topics (name, title, description) VALUES
    ('java',     'Java',     'Tutto quello che gira sulla JVM: linguaggio, librerie, JDK.'),
    ('spring',   'Spring',   'Spring Boot, Data, Security e il resto dell''ecosistema.'),
    ('frontend', 'Frontend', 'React, TypeScript, CSS e la vita nel browser.')
ON CONFLICT (name) DO NOTHING;
