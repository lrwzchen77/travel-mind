# SQL Initialization

This directory stores MySQL initialization scripts for Travel Mind.

- `001_create_database.sql` creates the `travelmind` database used by the Spring Boot dev profile.
- `002_phase2_crud_schema.sql` creates Phase 2 resource, trip history, favorite, note, and AI record tables.
- `003_phase2_seed_data.sql` inserts all 34 provincial-level capitals or administrative seats, plus demo attractions, hotels, restaurants, tags, profile data, trip history, favorites, notes, and AI records.
- `004_identity_and_roles.sql` creates the BCrypt account and role table used by both portals.
- `005_community_and_assistant.sql` adds public travel inspirations, inspiration bags, and persisted AI conversations.
- `006_seed_community_posts.sql` adds 50 public community posts across food, stay, play, route, and tip topics.
- `007_trip_expenses.sql` creates user-owned actual trip expenses for budget comparison.
- `008_community_interactions.sql` adds likes and first-level comments to approved public community posts.
- `009_trip_memory.sql` adds private trip memories, timeline items, and evidence-backed generation versions.

Example:

```bash
mysql -uroot -p < sql/001_create_database.sql
mysql -uroot -p travelmind < sql/002_phase2_crud_schema.sql
mysql -uroot -p travelmind < sql/003_phase2_seed_data.sql
mysql -uroot -p travelmind < sql/004_identity_and_roles.sql
mysql -uroot -p travelmind < sql/005_community_and_assistant.sql
mysql -uroot -p travelmind < sql/006_seed_community_posts.sql
mysql -uroot -p travelmind < sql/007_trip_expenses.sql
mysql -uroot -p travelmind < sql/008_community_interactions.sql
mysql -uroot -p travelmind < sql/009_trip_memory.sql
```

The Spring `dev` profile also creates `004` automatically and provisions local demo accounts. Production deployments must run the migration and provision their own password hashes.
