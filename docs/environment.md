# Cinemate — Environment Variable Reference

This document describes every environment variable used by Cinemate, how to obtain external API credentials, and how to apply them for local development vs. production Docker Compose.

---

## Quick Start (Local Dev)

```bash
# 1. Copy the root template and fill in secrets
cp .env.example .env

# 2. Copy the backend template
cp backend/.env.example backend/.env.prod

# 3. The frontend already has .env.development with localhost defaults — no changes needed for local dev.

# 4. Start everything
docker compose up --build
```

> [!IMPORTANT]
> **Never commit `.env` or `backend/.env.prod`** — they are listed in `.gitignore`. Only `.env.example` files are safe to commit.

---

## Variables by Service

### Root Compose (`.env`)

These are consumed directly by `compose.yaml` and passed into the relevant containers.

| Variable | Required | Default | Description |
|---|---|---|---|
| `POSTGRES_PASSWORD` | ✅ | — | PostgreSQL password |
| `POSTGRES_DB` | ✅ | `Cinemate` | PostgreSQL database name |
| `POSTGRES_USER` | ✅ | `cinemate` | PostgreSQL username |

---

### Backend (`backend/.env.prod`)

Loaded via `env_file: ./backend/.env.prod` in `compose.yaml`.

#### Database

| Variable | Required | Default | Description |
|---|---|---|---|
| `DB_URL` | ✅ | — | JDBC URL for PostgreSQL (e.g. `jdbc:postgresql://postgres:5432/Cinemate`) |
| `DB_USERNAME` | ✅ | — | PostgreSQL username (matches `POSTGRES_USER`) |
| `DB_PASSWORD` | ✅ | — | PostgreSQL password (must match `POSTGRES_PASSWORD`) |
| `JPA_SHOW_SQL` | ❌ | `false` | Log every SQL query. Disable in prod. |

> Schema is owned by **Flyway** migrations (`ddl-auto=validate`, fixed). There is no
> `JPA_DDL_AUTO` knob or `MONGODB_URI` anymore — the app runs on a single PostgreSQL database.

#### JWT

| Variable | Required | Description |
|---|---|---|
| `JWT_SECRET` | ✅ | Random secret used to sign JWTs. Must be ≥ 64 characters for HS512. |

**Generate a secure secret:**
```bash
openssl rand -hex 64
```

#### Google OAuth2

| Variable | Required | Description |
|---|---|---|
| `GOOGLE_CLIENT_ID` | ✅ | OAuth 2.0 Client ID from Google Cloud Console |
| `GOOGLE_CLIENT_SECRET` | ✅ | OAuth 2.0 Client Secret |
| `BASE_URL` | ✅ | Publicly reachable URL of the **backend** (e.g. `http://localhost:8080` or `https://api.cinemate.example.com`) |
| `FRONTEND_URL` | ✅ | Publicly reachable URL of the **frontend** (e.g. `http://localhost:5173`). The backend redirects here after login. |
| `CORS_ALLOWED_ORIGINS` | ✅ | Comma-separated list of origins allowed via CORS (e.g. `http://localhost:5173,https://cinemate.example.com`) |

#### SendGrid

| Variable | Required | Description |
|---|---|---|
| `SENDGRID_API_KEY` | ✅ | SendGrid API key (begins with `SG.`) |
| `SENDGRID_FROM_EMAIL` | ✅ | Verified sender email address (must be verified in SendGrid) |

#### Internal Services

| Variable | Required | Default | Description |
|---|---|---|---|
| `KAFKA_BOOTSTRAP_SERVERS` | ❌ | `kafka:9092` | Internal Kafka broker for the moderation pipeline (outbox → `moderation.requests`, verdicts ← `moderation.verdicts`). No auth — internal-only. |
| `WATCHPARTY_SERVICE_URL` | ✅ | `http://watch-party:8081` | Internal HTTP URL of the watch-party microservice |
| `WATCHPARTY_WS_URL` | ❌ | `ws://watch-party:8081/ws` | WebSocket URL (currently unused in backend code) |
| `WATCHPARTY_KEY` | ✅ | — | Shared secret for internal backend → watch-party auth |

**Generate a WATCHPARTY_KEY:**
```bash
openssl rand -hex 32
```

---

### Watch-Party Microservice (set in `compose.yaml`)

These are passed directly in the `compose.yaml` `environment:` block, not via a `.env.prod` file.

| Variable | Value in Compose | Description |
|---|---|---|
| `SPRING_PROFILES_ACTIVE` | `prod` | Spring profile |
| `SPRING_REDIS_HOST` | `redis` | Docker service name for Redis |
| `SPRING_REDIS_PORT` | `6379` | Redis port |
| `CORS_ALLOWED_ORIGINS` | from root `.env` | Origins allowed to open the `/ws` WebSocket (SEC-NEW-02) — same value the backend uses for CORS |

---

### Frontend (`frontend/.env.development` / `frontend/.env.production`)

> [!IMPORTANT]
> Vite environment variables are **baked into the static bundle at build time**. They are not runtime secrets — they will be visible in the compiled JS bundle. Do not put secrets here.

| Variable | Description |
|---|---|
| `VITE_API_BASE_URL` | Base URL of the backend REST API (no trailing slash) |
| `VITE_API_WATCH_PARTY_BASE_URL` | Base URL of the watch-party WebSocket microservice |

For local dev, `.env.development` has `http://localhost:8080` and `http://localhost:8081` — these work out of the box.

For production, update `frontend/.env.production` with your real domain before running `docker compose up --build`.

---

## How to Get Each API Key

### 1. Google OAuth2 Client ID & Secret

Used for "Sign in with Google".

1. Go to [Google Cloud Console](https://console.cloud.google.com/) → **APIs & Services** → **Credentials**.
2. Click **Create Credentials** → **OAuth 2.0 Client ID**.
3. Choose **Application type: Web application**.
4. Set **Authorized redirect URIs**:
   - Local dev: `http://localhost:8080/login/oauth2/code/google`
   - Production: `https://api.yourbackenddomain.com/login/oauth2/code/google`
5. Click **Create**. Copy the **Client ID** and **Client Secret**.
6. Paste them into `.env` and `backend/.env.prod`:
   ```
   GOOGLE_CLIENT_ID=xxxxxxxx.apps.googleusercontent.com
   GOOGLE_CLIENT_SECRET=GOCSPX-xxxxxxxx
   ```
7. Also make sure to add `BASE_URL` (backend public URL) and `FRONTEND_URL` (frontend public URL) so OAuth redirects land in the right place.

> [!TIP]
> Google OAuth requires the **redirect URI** registered in the console to exactly match what the backend sends. The backend computes it as: `${BASE_URL}/login/oauth2/code/google`.

---

### 2. SendGrid API Key

Used to send email verification codes and password reset emails.

1. Create a free account at [sendgrid.com](https://sendgrid.com/) (free tier: 100 emails/day).
2. Go to **Settings** → **API Keys** → **Create API Key**.
3. Choose **Restricted Access** and grant **Mail Send** → **Full Access**.
4. Copy the key (it starts with `SG.`) — **you can only see it once**.
5. Go to **Settings** → **Sender Authentication** → **Single Sender Verification**.
6. Verify the email address you want to send from (e.g. `no-reply@yourdomain.com`).
7. Paste into your env files:
   ```
   SENDGRID_API_KEY=SG.xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
   SENDGRID_FROM_EMAIL=no-reply@yourdomain.com
   ```

> [!NOTE]
> The `SENDGRID_FROM_EMAIL` must be a **verified sender** in SendGrid or emails will silently fail.

---

### 3. JWT Secret

Not an external service — you generate this yourself.

```bash
# Generate a cryptographically secure 64-byte hex string (128 chars)
openssl rand -hex 64
```

Paste the result as `JWT_SECRET`. This secret must:
- Be at least 64 characters (required by HS512).
- Be kept secret — anyone with this value can forge valid JWTs.
- Never change in production (all issued tokens would become invalid).

---

### 4. MySQL Root Password

Set `MYSQL_ROOT_PASSWORD` to any strong password. A suggested generator:
```bash
openssl rand -base64 24
```

Use the same value for `DB_PASSWORD` in the backend env.

---

### 5. Watch-Party Internal Key (`WATCHPARTY_KEY`)

Not an external service — you generate this yourself.

```bash
openssl rand -hex 32
```

> [!NOTE]
> The internal API key enforcement between the backend and watch-party service is currently **commented out** in `WatchPartyService.java`. Setting this variable now will make it easy to enable once the code is uncommented (`TD-01` in the tech debt backlog).

---

### 6. Content-Moderation Model (no API key needed)

The `moderation-worker` service bakes the `minuva/MiniLMv2-toxic-jigsaw-lite` ONNX model (pinned HuggingFace revision) into its image **at Docker image build time**. No account or API key is required. Thresholds are configured via `TOXIC_THRESHOLD` / `SEVERE_TOXIC_THRESHOLD` (default `0.5` each); either label crossing its threshold flags the content.

> [!NOTE]
> The first `docker compose up --build` downloads the model at build time and may take a few minutes on a stable connection. Subsequent runs reuse the cached image layer.

---

## Environment Variable Flow Diagram

```
┌──────────────────────────────────────────────────────────────────────┐
│ .env (root — loaded by Docker Compose)                               │
│   MYSQL_ROOT_PASSWORD, MYSQL_DATABASE,                               │
│   FRONTEND_URL, CORS_ALLOWED_ORIGINS, ...                            │
└──────────────────────────────────────────────────────────────────────┘
         │ passed to containers via compose.yaml
         ▼
┌──────────────────────────────────────┐  ┌──────────────────────────┐
│ backend/.env.prod                    │  │ compose.yaml environment: │
│   DB_URL, DB_PASSWORD, JWT_SECRET,   │  │   SPRING_REDIS_HOST,      │
│   GOOGLE_CLIENT_ID, SENDGRID_API_KEY │  │   SPRING_PROFILES_ACTIVE  │
│   WATCHPARTY_KEY, KAFKA_BOOTSTRAP... │  │   (watch-party service)   │
└──────────────────────────────────────┘  └──────────────────────────┘
         │ env_file: ./backend/.env.prod
         ▼
┌──────────────────────────────────────┐
│ Spring application.properties        │
│   ${DB_URL}, ${JWT_SECRET},          │
│   ${app.frontend.url},               │
│   ${app.cors.allowed-origins} ...    │
└──────────────────────────────────────┘

┌──────────────────────────────────────┐
│ frontend/.env.production             │
│   VITE_API_BASE_URL                  │  ← baked into JS bundle at build time
│   VITE_API_WATCH_PARTY_BASE_URL      │
└──────────────────────────────────────┘
```
