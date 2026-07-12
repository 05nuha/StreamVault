# StreamVault

A streaming-platform web application built with Jakarta Servlets (WAR deployment on Tomcat 10+), MySQL for relational data, and MongoDB for analytics. Users can register, subscribe to plans, browse/filter the content catalog, and track watch history; admins and content managers get role-specific dashboards.

See `HOW_IT_WORKS.md` and `CODE_EXPLAINED.md` for a guided tour of the codebase.

## Stack

- **Backend:** Java 11+, Jakarta Servlet 5 (Tomcat 10+)
- **Databases:** MySQL (users, subscriptions, catalog) + MongoDB (analytics)
- **Security:** BCrypt password hashing (cost 12), PreparedStatements everywhere, HTML-escaped output, session-id rotation on login
- **Build:** Maven (`mvn package` → `target/streamvault.war`)

## Setup

1. **MySQL** — run the scripts in `sql/`:
   ```bash
   mysql -u root -p < sql/setup.sql
   mysql -u root -p < sql/grants.sql
   ```
2. **MongoDB** — seed analytics data:
   ```bash
   mongosh < seed_mongo.js
   ```
   Connection defaults to `mongodb://localhost:27017`; override with `MONGO_URI` / `MONGO_DB` env vars.
3. **Database credentials** — copy the template and fill in real values:
   ```bash
   cp src/main/resources/db.properties.example src/main/resources/db.properties
   ```
   `db.properties` is gitignored, so credentials never reach version control.
4. **Build & deploy:**
   ```bash
   mvn package
   cp target/streamvault.war $CATALINA_HOME/webapps/
   ```
   Then open `http://localhost:8080/streamvault/`.

## Tests

```bash
mvn test
```

Unit tests cover the server-side input validation used by the auth flow.

## Security notes (2026-07 hardening pass)

- **Session fixation:** the session id is now rotated on successful login.
- **Atomic registration:** the user + subscription inserts run in one transaction; failures (e.g. duplicate email) roll back and surface as `register.html?error=taken` instead of silently pretending success.
- **Single-query auth:** credential check and role lookup were merged into one query (`AuthService.authenticate`).
- **Server-side validation:** email shape, password length (min 8), and name are validated in `ValidationUtil` before any DB work.
- **Thread-safe Mongo client:** lazy init now uses double-checked locking, and the URI is env-configurable.
- Compiler target raised from Java 8 to Java 11 (`maven.compiler.release`), matching the Jakarta EE 9 / Tomcat 10 baseline.
