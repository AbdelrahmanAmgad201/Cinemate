# Cinemate

**Cinemate** is a full-stack social platform for movies: discovery and reviews,
Reddit-style forums (forums → posts → threaded comments → votes), real-time
synchronized watch parties with live chat, and AI-based content moderation of
user-generated text.

It is built as a **hybrid monolith + microservices** system behind a single API
gateway, with all application data in one PostgreSQL database.

---

## Features

* Movie discovery, browsing, and reviews
* Reddit-style forums (posts, threaded comments, votes)
* Real-time watch parties with live chat
* Access + refresh token auth (RS256) and Google OAuth2
* Asynchronous AI content moderation (optimistic publish + transactional outbox)
* Dockerized multi-service deployment behind one gateway

---

## Tech Stack

| Layer            | Technology                                         |
| ---------------- | -------------------------------------------------- |
| Backend          | Java 21, Spring Boot, Spring Security              |
| API Gateway      | Spring Cloud Gateway (single entry point)          |
| Frontend         | React + Vite (served as static SPA via nginx)      |
| Database         | PostgreSQL 16 (JPA/Hibernate + Flyway)             |
| Realtime         | Redis 7 (watch-party state) + WebSocket / STOMP    |
| Messaging        | Kafka 3.9 (KRaft) — moderation pipeline            |
| ML inference     | ONNX Runtime + tokenizers (Python worker)          |
| Deployment       | Docker / Docker Compose                            |
| External APIs    | Wistia, SendGrid, Google OAuth                     |

---

## Architecture Overview

The browser only ever talks to the **gateway** (the single published port, `:8080`).
Everything else is internal to the `app-net` Docker network.

```
browser ─▶ gateway (Spring Cloud Gateway, :8080)
             ├── /            ─▶ frontend  (React SPA, nginx)
             ├── /api/**      ─▶ backend   (Spring Boot monolith)
             ├── /ws/**       ─▶ watch-party (Spring Boot microservice)
             └── /oauth2/**   ─▶ backend   (Google handshake)

backend      ─▶ PostgreSQL · Kafka (moderation)
watch-party  ─▶ Redis (durable party state)
moderation-worker ◀─ Kafka ─▶ backend   (Content-moderator, Python/ONNX)
```

### Services

- **backend/** — Spring Boot monolith: auth, users, movies, forums/posts/comments/votes,
  feed, admin/org, and the moderation producer + verdict consumer.
- **gateway/** — Spring Cloud Gateway: the single entry point. Serves the SPA, proxies
  `/api` and `/ws`, verifies access tokens (RS256, public key only), enforces the
  route/role matrix, injects trusted `X-User-*` identity headers, and rate-limits.
- **frontend/** — React + Vite SPA.
- **watch-party/** — Spring Boot microservice: WebSocket/STOMP fan-out for watch parties,
  Redis pub/sub for party state, authenticated to the backend via an internal API key.
- **Content-moderator/** — Python Kafka consumer that scores text with a quantized ONNX
  transformer and produces moderation verdicts.

### Persistence

All application data lives in **one PostgreSQL database** (schema owned by Flyway;
Hibernate runs `ddl-auto=validate`). Identity/catalog entities use `BIGINT` ids;
social content uses UUIDv7 ids. The moderation outbox commits in the same transaction
as the content it moderates. The only Redis instances are watch-party's durable party
state and the gateway's rate-limiter — the backend uses no Redis.

---

## Design Decisions

### Single entry point (gateway)

The gateway is the only service with a published host port. It verifies tokens at the
edge and forwards a trusted identity to the internal backend, which has no host port
and therefore cannot receive forged identity headers. One origin means no CORS and a
`SameSite=Lax` refresh cookie. See [`dev_docs/gateway.md`](dev_docs/gateway.md).

### Asynchronous content moderation

Moderation is off the request's critical path. Content is saved as `PENDING` and is
visible immediately; a transactional outbox relays a moderation request to Kafka, an
ONNX worker fleet scores it, and a verdict either approves or retroactively removes the
content. See [`dev_docs/moderation-architecture.md`](dev_docs/moderation-architecture.md).

### Shared Redis state for watch parties (horizontal scaling)

Party state (members, playback position, chat, status) lives in shared Redis rather
than in-process, so any watch-party instance can serve any request — stateless
instances, no sticky sessions.

---

## Running the Project

```bash
# 1. Copy the env templates and fill in secrets
cp .env.example .env
cp backend/.env.example backend/.env.prod

# 2. Build and start the full stack
docker compose up --build
```

Open the app at **http://localhost:8080** (the gateway).

See [`docs/environment.md`](docs/environment.md) for every environment variable and how
to obtain the external API credentials (Google OAuth2, SendGrid).

---

## Testing

* Backend unit tests (mocked): `cd backend && ./mvnw test`
* Integration tests use **Testcontainers** (a real `postgres:16` container + Flyway),
  so a reachable Docker daemon is required.

---

## Documentation

| Doc | Use for |
|---|---|
| [`docs/auth.md`](docs/auth.md) | Access/refresh token model + gateway contract |
| [`docs/environment.md`](docs/environment.md) | Env vars, secrets, deployment |
| [`dev_docs/gateway.md`](dev_docs/gateway.md) | Gateway design and topology |
| [`dev_docs/moderation-architecture.md`](dev_docs/moderation-architecture.md) | Moderation pipeline deep dive |
| [`dev_docs/postgres-consolidation.md`](dev_docs/postgres-consolidation.md) | Data-layer design rationale |
| [`dev_docs/TECH_DEBT.md`](dev_docs/TECH_DEBT.md) | Known open issues / backlog |

---

## Media Disclaimer

Cinemate does **not** host or distribute copyrighted movies. All movie posters,
metadata, and trailers are used strictly for educational, academic, and portfolio
demonstration purposes. All media content belongs to its respective copyright owners.
