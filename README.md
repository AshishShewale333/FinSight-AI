# FinSight AI

FinSight AI is a Java 17 / Spring Boot modular monolith for Indian equity portfolio tracking and document-grounded financial research. The backend is paired with a responsive React + TypeScript dashboard. Market values, news and AI outputs are provider-backed; missing data remains explicitly unavailable rather than being fabricated.

## Project layout

```text
FinSight-AI/
├── backend/     Java 17 / Spring Boot application
├── frontend/    React + TypeScript dashboard
├── outputs/     Architecture roadmap and Postman collection
├── compose.yaml Local MySQL, Redis, Kafka, and Qdrant services
└── .env         Local configuration (create from .env.example)
```

## Architecture

```mermaid
flowchart LR
  Web[React SPA] -->|JSON + JWT| API[Spring Boot API]
  API --> Auth[Authentication / User]
  API --> Core[Company / Market / Portfolio / Analytics]
  API --> Research[AI / Documents / News / Notifications]
  Auth --> MySQL[(MySQL 8.4)]
  Core --> MySQL
  Research --> MySQL
  API --> Redis[(Redis cache and rate limiter)]
  API --> Outbox[Transactional outbox]
  Outbox --> Kafka[(Kafka)]
  Kafka --> Workers[Idempotent consumers / DLT]
  API --> Qdrant[(Qdrant vectors)]
  API --> Providers[Market, news and LLM adapters]
```

### Modules

`authentication` and `user` manage BCrypt credentials, JWT access tokens, rotating opaque refresh tokens, and user profiles. `company` and `marketdata` provide company search, quotes, and history through replaceable provider interfaces. `portfolio` and `transaction` enforce ownership and apply weighted-average cost basis and realized P&L rules. `analytics` calculates returns, XIRR, allocation, concentration, volatility, and drawdown in Java. `document` stores owned PDF metadata in MySQL and vectors in Qdrant. `aiassistant` provides session chat, grounded answers, and asynchronous research reports. `news` and `notification` cover provider news summaries and price/portfolio events. `common` contains API envelopes, error handling, correlation IDs, and cache/scheduling setup.

## Data model

```mermaid
erDiagram
  USERS ||--o{ PORTFOLIOS : owns
  USERS ||--o{ REFRESH_TOKENS : authenticates
  USERS ||--o{ CHAT_SESSIONS : owns
  USERS ||--o{ FINANCIAL_DOCUMENTS : uploads
  PORTFOLIOS ||--o{ PORTFOLIO_TRANSACTIONS : records
  PORTFOLIOS ||--o{ PORTFOLIO_HOLDINGS : contains
  COMPANIES ||--o{ STOCK_QUOTES : priced_by
  COMPANIES ||--o{ HISTORICAL_STOCK_PRICES : history
  COMPANIES ||--o{ PORTFOLIO_TRANSACTIONS : traded
  CHAT_SESSIONS ||--o{ CHAT_MESSAGES : contains
  FINANCIAL_DOCUMENTS ||--o{ DOCUMENT_CHUNKS : splits
  USERS ||--o{ PRICE_ALERTS : configures
  USERS ||--o{ NOTIFICATION_EVENTS : receives
  OUTBOX_EVENTS ||--o{ PROCESSED_EVENTS : delivers
```

Flyway owns schema evolution (`backend/src/main/resources/db/migration`). MySQL is the metadata and relational source of truth; Qdrant only stores searchable vector passages and each retrieval is constrained by user/document metadata and checked against MySQL ownership/status.

### Stock Explorer directory import

The Stock Explorer directory is stored in its own `stock_explorer_listings` table and does not feed portfolio transaction or quote endpoints. To load NSE's downloadable equity list, download `EQUITY_L.csv` from the [NSE securities page](https://www.nseindia.com/market-data/securities-available-for-trading) yourself and place it at `backend/data/nse/EQUITY_L.csv` (or set `NSE_EQUITY_CSV_PATH` in `.env`). On backend startup, the importer validates the expected CSV columns, upserts by `(exchange_code, symbol)`, and marks omitted listings inactive. It runs at most once per India calendar day. It reads the local file only; it does not automatically fetch NSE's website.

## Async events

```mermaid
flowchart LR
  Action[User/API transaction] --> DB[(MySQL transaction + outbox)]
  DB --> Relay[Outbox relay]
  Relay --> Kafka[(Kafka)]
  Kafka --> Consumer[Validated idempotent consumer]
  Consumer --> Work[Analytics / alerts / summaries / reports]
  Consumer -->|retry exhaustion| DLT[Dead-letter topic]
```

The API persists user-facing actions without requiring Kafka. The outbox relay publishes events after commit. Consumers record processed event IDs and use retry/backoff with dead-letter handling. Topics include `stock-price-update-topic`, `portfolio-transaction-topic`, `financial-news-topic`, `ai-report-request-topic`, and `notification-topic`; failed events route to a DLT.

For local Kafka, start Docker Desktop and run `docker compose up -d kafka-init` from the project root. Compose starts Kafka, waits for its health check, and creates the application topics (including dead-letter topics). The backend connects from the host through `localhost:29092`.

### Live financial headlines

Set `FINANCIAL_NEWS_PROVIDER=gdelt-rss` and `FINANCIAL_NEWS_SYNC_ENABLED=true` in the local `.env` to fetch India-focused headlines from GDELT and Google News RSS. The first sync runs shortly after backend startup; later syncs use `NEWS_SYNC_FIXED_DELAY` (one hour by default). FinSight stores headline metadata and story links, not full publisher articles. Each story links to its source, and the news screen credits the GDELT Project. Automatic AI summaries stay disabled by default, so headline ingestion does not depend on Kafka or an OpenAI key. Use `FINANCIAL_NEWS_PROVIDER=mock` for the clearly labeled practice scenarios instead.

## RAG flow

```mermaid
flowchart LR
  PDF[Owned PDF upload] --> Extract[PDFBox text and page extraction]
  Extract --> Split[Page-aware chunks]
  Split --> MySQL[(Document and chunk metadata)]
  Split --> Embed[Spring AI embeddings]
  Embed --> Qdrant[(Qdrant vector store)]
  Question[User question] --> Retrieve[Owner-filtered similarity search]
  Qdrant --> Retrieve
  Retrieve --> Verify[MySQL ownership and READY check]
  Verify --> Prompt[Grounded prompt and guardrails]
  Prompt --> LLM[Configured Spring AI chat model]
  LLM --> Answer[Answer + citations + missing information]
```

RAG is enabled only when a chat provider and embedding provider are configured. Retrieved passages are treated as untrusted evidence, not instructions. The assistant reports missing evidence and includes source document/page references when available. Outputs are informational and are not investment advice.

## APIs

All application routes use `/api/v1` and return `{success,message,data,timestamp,correlationId}` envelopes. OpenAPI UI is available at `/swagger-ui.html`.

| Area | Routes |
|---|---|
| Authentication | `POST /auth/register`, `/auth/login`, `/auth/refresh`, `/auth/logout`; `GET /users/me` |
| Companies and market | `GET /companies?query=&page=&size=`, `GET /companies/{id}`, `GET /companies/compare?ids=`, `GET /companies/{id}/quotes/latest`, `GET /companies/{id}/prices` |
| Portfolios | `GET/POST /portfolios`, `GET/DELETE /portfolios/{id}`, `GET /portfolios/{id}/holdings`, `GET/POST /portfolios/{id}/transactions`, `GET /portfolios/{id}/analytics` |
| Assistant | `POST /ai/chat`, `POST/GET /ai/chat/sessions`, `GET /ai/chat/sessions/{id}/messages`, `POST /ai/reports`, `GET /ai/reports/{id}` |
| Documents | `POST/GET /documents`, `GET/DELETE /documents/{id}` |
| News and notifications | `GET /news`, `GET /news/{id}`, `POST /news/{id}/summarize`, `GET /notifications`, `PATCH /notifications/{id}/read`, `GET/POST /alerts`, `DELETE /alerts/{id}` |

An importable Postman collection is at [`outputs/FinSight-AI.postman_collection.json`](outputs/FinSight-AI.postman_collection.json).

## Technology

Java 17, Spring Boot 3.5, Spring AI 1.1, Spring Security, Spring Data JPA, Spring Batch, MySQL 8.4, Flyway, Redis, Kafka, Qdrant, React 19, TypeScript, Tailwind CSS, Axios, Recharts, Maven, Docker Compose, JUnit 5, Mockito, and Springdoc OpenAPI.

## Local setup

Requirements: JDK 17, Maven 3.6.3+, Docker Compose v2, and Node.js 20+ with npm.

1. Copy `.env.example` to `.env`, set local database passwords, and replace `JWT_SECRET` with a Base64-encoded random key (at least 32 decoded bytes). PowerShell: `[Convert]::ToBase64String([Security.Cryptography.RandomNumberGenerator]::GetBytes(32))`.
2. Start local infrastructure: `docker compose up -d`.
3. Export the app variables from `.env` into the shell. Compose reads `.env`; Spring Boot does not. For PowerShell, assign `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, and `JWT_SECRET` in `$env:`.
4. From the project root, run the API with `cd backend; mvn spring-boot:run`. Flyway applies migrations; Hibernate validates rather than creating schema.
5. Run the UI in another terminal from the project root: `cd frontend; npm ci; npm run dev`. Default API base is `http://localhost:8080/api/v1`; override with `VITE_API_BASE_URL` if needed.
6. Open `http://localhost:5173`, API docs at `http://localhost:8080/swagger-ui.html`, and health at `http://localhost:8080/actuator/health`.

### Environment variables

| Variable | Purpose |
|---|---|
| `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` | MySQL JDBC connection |
| `MYSQL_DATABASE`, `MYSQL_USER`, `MYSQL_PASSWORD`, `MYSQL_ROOT_PASSWORD` | Compose MySQL initialization |
| `JWT_SECRET`, `JWT_ISSUER`, `JWT_ACCESS_TTL`, `JWT_REFRESH_TTL` | JWT signing and token lifetimes |
| `SERVER_PORT`, `DB_POOL_SIZE` | API port and connection pool |
| `REDIS_HOST`, `REDIS_PORT` | Redis service |
| `KAFKA_BOOTSTRAP_SERVERS` | Kafka broker |
| `QDRANT_HOST`, `QDRANT_PORT` | Qdrant endpoint |
| `OPENAI_API_KEY`, `FINSIGHT_AI_ENABLED` | Optional OpenAI-compatible assistant provider |
| `SPRING_AI_MODEL_CHAT`, `SPRING_AI_MODEL_EMBEDDING` | Select `openai` to enable chat and embeddings; defaults disable external AI |
| `OPENAI_CHAT_MODEL`, `OPENAI_EMBEDDING_MODEL` | Configurable model names |
| `FINSIGHT_MARKET_MOCK_ENABLED` | Explicitly labeled local mock quotes; off by default |
| `FINSIGHT_MARKET_DATA_SYNC_ENABLED`, `FINSIGHT_NEWS_SYNC_ENABLED` | Optional scheduled provider ingestion |
| `FINSIGHT_DOCUMENT_STORAGE` | Local document storage directory |

Keep real credentials in an environment/secret manager and out of source control. The browser demo stores short-lived tokens in `sessionStorage`; production should move refresh tokens to an HttpOnly, Secure, SameSite cookie with an explicit CSRF policy.

## Sample requests

Register and login:

```http
POST /api/v1/auth/register
Content-Type: application/json

{"email":"person@example.com","password":"a-long-unique-passphrase","displayName":"Person"}
```

Create a portfolio with the returned access token:

```http
POST /api/v1/portfolios
Authorization: Bearer <accessToken>
Content-Type: application/json

{"name":"Long term","baseCurrency":"INR"}
```

Ask the assistant:

```http
POST /api/v1/ai/chat/sessions
Authorization: Bearer <accessToken>
Content-Type: application/json

{"title":"Annual report research","documentId":"<owned-document-id>"}
```

Use the returned session ID to ask a question:

```http
POST /api/v1/ai/chat
Authorization: Bearer <accessToken>
Content-Type: application/json

{"sessionId":"<session-id>","question":"What are the major risks mentioned in the report?"}
```

## Testing and build

```bash
cd backend && mvn verify
cd ../frontend && npm ci && npm run build
```

The project includes unit tests for JWT token issuance/validation and financial calculations. Integration tests against MySQL/Kafka remain a follow-up and require a working Docker daemon. Frontend output is generated in `frontend/dist`.

## Deployment

Build a Java 17 runtime artifact from `backend/` with `mvn clean package` and serve `frontend/dist` from a static web server or CDN. Deploy MySQL, Redis, Kafka, and Qdrant as managed services or secured private infrastructure. Provide secrets through the deployment secret manager, terminate TLS at the ingress, restrict actuator exposure, configure backups and retention, and set provider sync toggles deliberately. Compose is for local development, not production deployment.

## Calculation assumptions and limitations

Transactions use weighted-average cost basis. Unrealized P&L requires a current quote; missing prices leave valuation incomplete. XIRR uses dated cash flows and is unavailable when cash-flow roots are absent or ambiguous. CAGR requires a positive beginning/end valuation over a positive period. Volatility and maximum drawdown need sufficient dated price history and use the available stored observations; short or incomplete histories are not annualized as authoritative. Financial metrics, quotes, news, and model output depend on configured providers and their freshness/coverage.
