# Database Design

## Database

Default database name: `travelmind`. Scripts are stored in `sql/`.

- `001_create_database.sql`: database creation.
- `002_phase2_crud_schema.sql`: core table schema.
- `003_phase2_seed_data.sql`: demo data.

## Core Tables

| Table | Purpose |
| --- | --- |
| `tm_user` | Demo users and profile fields. |
| `tm_user_preference` | Budget, travel style, preferred city/tags, transport, hotel, and diet preferences. |
| `tm_city` | City resource library. |
| `tm_map_poi` | Shared attraction, hotel, and restaurant source: all fetched AMap/OSM records plus administrator `manual` supplements. |
| `tm_attraction` / `tm_hotel` / `tm_restaurant` | Legacy Phase 2 demo tables retained for migration compatibility; discovery and admin CRUD no longer read them. |
| `tm_travel_tag` | Shared travel tags. |
| `tm_favorite` | User favorites for attractions, trips, or other target types. |
| `tm_travel_note` | User travel notes and content snippets. |
| `tm_trip_plan` | Saved trip plan header and raw plan JSON. |
| `tm_trip_day` | Per-day saved trip plan data. |
| `tm_trip_item` | Attractions, meals, hotels, and schedule items inside a trip. |
| `tm_ai_analysis_record` | AI request summaries, result summaries, raw JSON, and success/failure status. |

## Design Rules

- All business tables use logical delete through `deleted`.
- CRUD resources expose common `status`, `create_time`, and `update_time` semantics where useful.
- Java backend is the only component writing AI analysis records.
- Raw trip and AI JSON are stored for traceability and frontend display.
- POI provenance is immutable: external rows keep their provider/source ID, while administrator additions use `source=manual`.
