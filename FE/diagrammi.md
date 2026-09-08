# Diagrammi del progetto

Tutti i collegamenti del progetto in sei diagrammi Mermaid.
GitHub li renderizza da solo; per modificarli si puo' incollare il singolo blocco
su <https://mermaid.live>.

---

## 1. Il modello dei dati (Passo 1)

Le cinque entita' e i vincoli. Le due che portano il peso del progetto sono
`subscriptions` (dice chi riceve e chi puo' scrivere) e `notifications`
(conserva quello che il canale ha gia' consegnato).

```mermaid
erDiagram
    USERS ||--o{ SUBSCRIPTIONS : "si iscrive"
    TOPICS ||--o{ SUBSCRIPTIONS : "ha iscritti"
    USERS ||--o{ MESSAGES : "scrive"
    TOPICS ||--o{ MESSAGES : "raccoglie"
    USERS ||--o{ NOTIFICATIONS : "e destinatario"
    MESSAGES ||--o{ NOTIFICATIONS : "genera una riga per iscritto"

    USERS {
        bigint id PK "IDENTITY"
        varchar username UK "unico, 60 caratteri, e anche il Principal STOMP"
        varchar password_hash "BCrypt, mai in chiaro"
        timestamp created_at "updatable false"
    }

    TOPICS {
        bigint id PK
        varchar name UK "minuscolo, va nell URL e in /topic/feed/{name}"
        varchar title "titolo leggibile"
        varchar description
    }

    SUBSCRIPTIONS {
        bigint id PK
        bigint user_id FK "ManyToOne LAZY"
        bigint topic_id FK "ManyToOne LAZY"
        timestamp subscribed_at
        constraint uk_user_topic "UNIQUE user_id topic_id - traduce il doppio clic in 409"
    }

    MESSAGES {
        bigint id PK
        bigint topic_id FK
        bigint author_id FK
        varchar text "max 500"
        timestamp created_at
        index idx_topic_created "topic_id created_at - lo storico dal piu recente"
    }

    NOTIFICATIONS {
        bigint id PK
        bigint recipient_id FK "una riga PER DESTINATARIO"
        bigint message_id FK
        timestamp created_at
        timestamp read_at "null = non letta"
        index idx_recipient_read "recipient_id read_at - il contatore del badge"
    }
```

---

## 2. L'architettura: chi chiama chi

Le frecce continue sono chiamate diritte, quelle tratteggiate sono consegne
sul canale WebSocket. Da notare che il canale parte dal **controller**, non dal
service: cosi' i frame escono solo dopo il commit.

```mermaid
flowchart TB
    subgraph FE["Frontend - React + TypeScript"]
        APP["App.tsx<br/>tiene la sessione"]
        LOGIN["Login.tsx"]
        ELENCO["ElencoBacheche.tsx"]
        BACHECA["Bacheca.tsx"]
        CAMP["Campanella.tsx"]
        API["api.ts<br/>fetch + header Authorization"]
        STOMP["stompClient.ts<br/>UN SOLO Client"]
    end

    subgraph WEB["Backend - web"]
        AUTHC["AuthController"]
        TOPICC["TopicController"]
        MSGC["MessageController"]
        NOTIFC["NotificationController"]
    end

    subgraph SEC["security + config"]
        RESOLVER["UtenteCorrenteResolver<br/>Bearer token to AppUser"]
        TOKENS["TokenStore<br/>mappa in memoria"]
        WSCONF["WebSocketConfig<br/>broker /topic /queue"]
        INTERC["StompAuthInterceptor<br/>CONNECT to Principal"]
    end

    subgraph SRV["service - qui vivono le regole"]
        AUTHS["AuthService<br/>BCrypt, 409, 401"]
        TOPICS["TopicService<br/>404, 409"]
        MSGS["MessageService<br/>403 se non iscritto"]
        NOTIFS["NotificationService"]
    end

    subgraph REPO["repository + database"]
        REPOS["AppUser / Topic / Subscription<br/>Message / Notification Repository"]
        DB[("PostgreSQL")]
    end

    BROKER["SimpMessagingTemplate<br/>broker in memoria"]

    LOGIN --> API
    ELENCO --> API
    BACHECA --> API
    CAMP --> API
    APP --> STOMP
    BACHECA --> STOMP
    CAMP --> STOMP

    API -->|"HTTP + Bearer"| AUTHC
    API -->|"HTTP + Bearer"| TOPICC
    API -->|"HTTP + Bearer"| MSGC
    API -->|"HTTP + Bearer"| NOTIFC
    STOMP -->|"CONNECT + SUBSCRIBE"| INTERC

    TOPICC -.->|"@UtenteCorrente"| RESOLVER
    MSGC -.->|"@UtenteCorrente"| RESOLVER
    NOTIFC -.->|"@UtenteCorrente"| RESOLVER
    RESOLVER --> TOKENS
    INTERC --> TOKENS
    AUTHS --> TOKENS
    INTERC --> WSCONF

    AUTHC --> AUTHS
    TOPICC --> TOPICS
    MSGC --> MSGS
    NOTIFC --> NOTIFS

    AUTHS --> REPOS
    TOPICS --> REPOS
    MSGS --> REPOS
    NOTIFS --> REPOS
    REPOS --> DB

    MSGC ==>|"dopo il commit"| BROKER
    BROKER -.->|"/topic/feed/{nome}"| BACHECA
    BROKER -.->|"/user/queue/notifications"| CAMP
```

---

## 3. Registrazione, login e le due strade del token

Lo stesso token serve due volte: nell'header `Authorization` delle chiamate REST
e nell'header del frame `CONNECT`. E' il pezzo che rende possibile una notifica
indirizzata a una sola persona.

```mermaid
sequenceDiagram
    autonumber
    participant B as Browser
    participant AC as AuthController
    participant AS as AuthService
    participant TS as TokenStore
    participant DB as PostgreSQL
    participant WS as StompAuthInterceptor

    B->>AC: POST /api/auth/register {username, password}
    AC->>AS: registra(richiesta)
    AS->>DB: existsByUsername
    alt nome gia preso
        AS-->>B: 409 il nome utente e gia preso
    else nome libero
        AS->>AS: encoder.encode(password) con BCrypt
        AS->>DB: INSERT users (password_hash)
        AS->>TS: crea(username) - UUID casuale
        AS-->>B: 201 {username, token}
    end

    Note over B: il token resta in memoria nel componente App

    B->>AC: POST /api/auth/login
    AC->>AS: login(richiesta)
    AS->>DB: findByUsername
    AS->>AS: encoder.matches(password, hash)
    Note right of AS: mai ricalcolare l hash e confrontare le stringhe:<br/>il sale sarebbe diverso e non coinciderebbero mai
    AS-->>B: 200 {username, token} oppure 401 credenziali non valide

    B->>WS: CONNECT con header Authorization Bearer token
    WS->>TS: utenteDi(token)
    TS-->>WS: username
    WS->>WS: accessor.setUser(() -> username)
    Note right of WS: da qui convertAndSendToUser sa dove consegnare.<br/>Senza questo passaggio la sessione resta anonima<br/>e i messaggi personali spariscono in silenzio
```

---

## 4. La pubblicazione di un messaggio (il cuore della consegna)

Una transazione che scrive due tabelle, e solo dopo il commit i due frame.

```mermaid
sequenceDiagram
    autonumber
    participant M as Browser di mario
    participant MC as MessageController
    participant MS as MessageService
    participant DB as PostgreSQL
    participant BR as Broker STOMP
    participant L as Browser di lucia

    M->>MC: POST /api/topics/java/messages {text}
    MC->>MC: @Valid - testo vuoto o oltre 500 caratteri = 400
    MC->>MC: @UtenteCorrente - token assente o invalido = 401
    MC->>MS: pubblica("java", mario, testo)

    rect rgb(238, 244, 255)
        Note over MS,DB: una sola transazione
        MS->>DB: findByName("java")
        alt topic inesistente
            MS-->>M: 404
        end
        MS->>DB: existsByUserAndTopic(mario, java)
        alt mario non e iscritto
            MS-->>M: 403 so chi sei, ma qui non puoi
        end
        MS->>DB: INSERT messages
        MS->>DB: SELECT iscritti diversi da mario
        MS->>DB: INSERT notifications - una riga per ogni iscritto
    end

    Note over MS,DB: COMMIT: da qui in poi il messaggio esiste davvero

    MS-->>MC: RisultatoPubblicazione {messaggio, consegne}
    MC->>BR: convertAndSend("/topic/feed/java", messaggio)
    BR-->>L: MESSAGE sul feed - lo riceve chiunque abbia la pagina aperta
    MC->>BR: convertAndSendToUser("lucia", "/queue/notifications", notifica)
    BR-->>L: MESSAGE sulla coda privata - accende la campanella
    MC-->>M: 201 il messaggio creato

    Note over BR,L: se lucia e offline il frame viene scartato senza errori:<br/>la riga in notifications resta l unica garanzia
```

---

## 5. Chi era offline

Il caso che spiega perche' le notifiche si salvano su una tabella invece di
esistere solo come frame.

```mermaid
sequenceDiagram
    autonumber
    participant L as Browser di lucia
    participant BR as Broker STOMP
    participant NC as NotificationController
    participant DB as PostgreSQL

    Note over L: lucia chiude la finestra

    BR--xL: convertAndSendToUser non trova sessioni:<br/>nessuna eccezione, nessun log
    Note over DB: la riga in notifications resta li

    Note over L: lucia rientra
    L->>BR: CONNECT + SUBSCRIBE /user/queue/notifications
    Note over BR: il broker non conserva nulla del passato:<br/>da qui in poi ricevera i messaggi nuovi

    L->>NC: GET /api/notifications/unread-count
    NC->>DB: count where recipient = lucia and read_at is null
    DB-->>NC: 3
    NC-->>L: {"count": 3} - il badge si accende comunque

    L->>NC: POST /api/notifications/read-all
    NC->>DB: UNA sola UPDATE su tutte le righe non lette
    NC-->>L: 204
```

---

## 6. I codici di errore, in un colpo d'occhio

Confondere 401 e 403 e' l'errore piu' frequente della consegna:
**401 = non so chi sei**, **403 = so chi sei, ma qui non puoi**.

```mermaid
flowchart TD
    A["Arriva una richiesta su /api"] --> B{"Header Authorization<br/>presente e valido?"}
    B -->|no| C["401<br/>NonAutenticatoException"]
    B -->|si| D{"Il corpo passa @Valid?"}
    D -->|no| E["400<br/>MethodArgumentNotValidException"]
    D -->|si| F{"La risorsa dell URL esiste?"}
    F -->|no| G["404<br/>NonTrovatoException"]
    F -->|si| H{"E una scrittura su un topic?"}
    H -->|no| L{"L operazione e gia stata fatta?"}
    H -->|si| I{"Sono iscritto al topic?"}
    I -->|no| J["403<br/>NonIscrittoException"]
    I -->|si| L
    L -->|si| M["409<br/>Conflitto o vincolo unico del database"]
    L -->|no| N["200 / 201 / 204"]
```
