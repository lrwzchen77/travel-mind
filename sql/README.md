# SQL Initialization

This directory stores MySQL initialization scripts for Travel Mind.

- `001_create_database.sql` creates the `travelmind` database used by the Spring Boot dev profile.
- `002_phase2_crud_schema.sql` creates Phase 2 resource, trip history, favorite, note, and AI record tables.
- `003_phase2_seed_data.sql` inserts demo cities, attractions, hotels, restaurants, tags, profile data, trip history, favorites, notes, and AI records.

Example:

```bash
mysql -uroot -p < sql/001_create_database.sql
mysql -uroot -p travelmind < sql/002_phase2_crud_schema.sql
mysql -uroot -p travelmind < sql/003_phase2_seed_data.sql
```
