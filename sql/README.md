# Database Migrations

The schema is managed by Flyway from `app/src/main/resources/db/migration`.
Development-only seed data is stored in `app/src/main/resources/db/dev` and is enabled only by the `dev` profile.

- Fresh databases are migrated automatically when the Java application starts.
- Existing pre-Flyway databases are baselined at version 13 and receive version 14 onward.
- Production does not load demo users or community seed data.

Create the database once, then start the application:

```bash
mysql -uroot -p -e "CREATE DATABASE travelmind CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci"
mvn -pl app -am spring-boot:run
```

The `dev` profile provisions local demo identities after migration. On the first production startup, `TRAVELMIND_BOOTSTRAP_ADMIN_*` creates the initial administrator only when no active administrator exists; later accounts are managed through the administrator API.
