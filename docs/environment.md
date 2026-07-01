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
| `MYSQL_ROOT_PASSWORD` | ✅ | — | MySQL root password |
| `MYSQL_DATABASE` | ✅ | `Cinemate` | MySQL database name |
| `MONGO_INITDB_DATABASE` | ✅ | `Cinemate2` | MongoDB initial database |

---

### Backend (`backend/.env.prod`)

Loaded via `env_file: ./backend/.env.prod` in `compose.yaml`.

#### Database

| Variable | Required | Default | Description |
|---|---|---|---|
| `DB_URL` | ✅ | — | JDBC URL for MySQL (e.g. `jdbc:mysql://mysql:3306/Cinemate?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC`) |
| `DB_USERNAME` | ✅ | — | MySQL username |
| `DB_PASSWORD` | ✅ | — | MySQL password (must match `MYSQL_ROOT_PASSWORD`) |
| `JPA_DDL_AUTO` | ✅ | `validate` | JPA schema strategy. Use `validate` in prod, `update` in dev. **Never use `create` in prod.** |
| `JPA_SHOW_SQL` | ❌ | `false` | Log every SQL query. Disable in prod. |
| `MONGODB_URI` | ✅ | — | MongoDB connection string (e.g. `mongodb://mongodb:27017/Cinemate2`) |

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
| `HATE_MODEL_URL` | ✅ | `http://hate-api:8000/api/hate/v1/analyze` | Internal URL of the hate-speech analyze endpoint |
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

### 6. Hate-Speech Model (no API key needed)

The `hate-api` service downloads the `facebook/roberta-hate-speech-dynabench-r4-target` model from HuggingFace **at Docker image build time** and stores it in the `hate-api-models` Docker volume. No account or API key is required.

> [!NOTE]
> The first `docker compose up --build` will take **5–10 minutes** and require a stable internet connection to download the ~500 MB model. Subsequent runs are fast as the volume is cached.

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
│   WATCHPARTY_KEY, HATE_MODEL_URL ... │  │   (watch-party service)   │
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
