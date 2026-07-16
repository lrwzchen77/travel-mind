# Dual-Portal Product Boundary

## Product Goal

Travel Mind is one travel platform with two products, not one mixed CRUD application:

- Consumer application (`/`): helps a traveler discover, plan, save, revisit, and improve a trip.
- Admin console (`/admin`): helps operations staff govern users, destination content, trip records, AI usage, and provider configuration.

Both products share domain services and MySQL tables. They do not share page layouts, navigation, API namespaces, or authorization rules.

## Business Ownership

| Capability | Consumer application | Admin console |
| --- | --- | --- |
| Cities, attractions, hotels, restaurants | Browse active cards and start planning | Create, edit, publish, unpublish, delete |
| Trip planning | Submit and follow own planning task | Inspect platform trip records |
| Trip history | View, copy, chat with, delete own trips | Search and govern all trips |
| Profile and preferences | Edit own human-readable form | Inspect and govern users |
| Favorites and notes | Create and manage own content | Moderate all records |
| AI | Use travel-oriented inspiration tools and view own footprint | Inspect AI records and validate raw tools |
| Provider settings | No access | Configure map, content, and model providers |

## Migration Matrix

| Previous feature | New destination |
| --- | --- |
| Generic `ResourceCrudView` for all resources | `/admin/resources/*` only |
| Cities, attractions, hotels, restaurants tables | Consumer discovery cards under original URLs |
| Profile JSON editor | Consumer profile and preference form |
| Raw AI lab | `/admin/ai-tools` |
| Consumer AI entry | Simplified travel-note insight workflow |
| Runtime settings | `/admin/settings` |
| Fixed user `1001` | Current authenticated user from Sa-Token |

## Route And API Contract

- Public data: `/api/public/**`
- Authenticated traveler data: `/api/user/**`, role `user`
- Administrator data: `/api/admin/**`, role `admin`
- Consumer login: `/login`
- Admin login: `/admin/login`

The frontend stores one Sa-Token session and clears it on `401`. Route guards reject the wrong portal role. The backend repeats those checks and remains authoritative.

## Delivered Expansion

Consumer:

- Public destination discovery with search and active-only data.
- Owned trip planning/history/detail boundaries.
- Owned favorites, notes, and AI footprint.
- Human-readable preference editing.
- Consumer-oriented AI inspiration analysis.

Admin:

- Operations dashboard with live resource counts.
- Resource, user, trip, note, tag, and AI-record maintenance.
- Provider configuration for maps, Xiaohongshu, and large models.
- Raw AI validation workspace.

## Next Product Backlog

1. Replace generic admin JSON editing with typed forms and reference selectors.
2. Add user registration, password recovery, phone verification, and risk controls.
3. Add destination detail pages, semantic search, and one-click favorites from discovery.
4. Add admin audit logs, moderation queues, provider health, and usage charts.
5. Split the frontend deployment only when independent release cadence or team ownership justifies it.

## Acceptance Gates

- Anonymous users can read public discovery but receive `401` on personal APIs.
- User tokens receive `403` on admin APIs; admin tokens receive `403` on user APIs.
- User IDs cannot be supplied to access another user's profile, task, trip, favorite, or note.
- Consumer pages expose no raw JSON editor, status toggle, delete-all control, or internal user ID.
- Admin pages are unavailable from consumer navigation and use an independent layout.
- `mvn test`, `npm test`, and `npm run build` pass before delivery.
