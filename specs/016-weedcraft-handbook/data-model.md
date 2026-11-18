# Data Model: The Handbook

## Book (assets, client)

- **Book** (`assets/fourtwenty/handbook/book.json`): title key, author ("Simon Stalwart"), cover image, inside-cover text key (DON'T PANIC), chapters in order.
- **Chapter**: id, title key, icon item, `needs` (optional mod id), entry ids in order.
- **Entry** (`<lang>/entries/<chapter>/<id>.md`): id (= file name), title, icon, `terms` (index/glossary terms it explains), `needs` (mod id), body (markup). Valid only if every reference resolves.
- **Page element** (parsed from markup): Heading(level, text) · Paragraph(spans) · List(items) · Steps(numbered items) · Item(id, caption) · Recipe(id) · Image(path, caption, size) · Table(rows) · Truth(gate) · Link span(entry id) · Spoiler span(terpene code) · PageBreak.
- **Glossary term**: term key, one-line meaning, entry id. Built from entries' `terms` plus `glossary.md`.
- **Index line**: term → (entry id, page), built at layout time.
- **Part translation**: part item id → real-world term (from `tools/generate_real_terms.py`'s table, exported to `assets/fourtwenty/handbook/real_terms.json`), plus the units table.
- **Bookmark** (client config): world key → entry id + page.

## Scouting (data, server)

- **Rank** (`data/fourtwenty/handbook/ranks/<id>.json`): id, order 1-6, name key, emblem texture, strain grade it follows, requirements.
- **Stoner Badge** (`data/fourtwenty/handbook/badges/<id>.json`): id, name key, patch texture, chapter it covers, `needs` (mod id), requirements.
- **Requirement**: number ("1", "2a"), text key, kind (`tracked` | `honour`), for tracked: `deed` (from the deed list), optional `subject` filter (item/tag/strain/family/grade/terpene/block/circuit id), `count` (default 1), `entry` (the entry that teaches it).

## Player progress (player persisted NBT `fourtwenty_handbook`, server)

- `ticks`: requirement key (`rank:tenderbud/2a`, `badge:munchies/3`) → state (`done` | `honour`) + game time.
- `counts`: requirement key → progress count (for counted requirements).
- `earned`: rank/badge id → game time; `rank`: highest earned rank order.
- `catchUp`: bool (the one-time old-world catch-up ran).

### State transitions

- Requirement: open → (deed matches, count reached) → done; open → (player signs) → honour. Never back, except by the reset command.
- Rank n: locked (rank n-1 not earned) → open → earned (all requirements done/honour). Ranks earn strictly in order; a requirement of a later rank may tick early and counts once that rank opens.
- Stoner Badge: open → earned (all done/honour); any order.
- Reset (op command): all, or one rank/badge, back to open (and dependent later ranks lose earned state).

## Sync (server → client)

- **ProgressPayload** (to the owner): full on login, diffs after (ticks, counts, earned).
- **SashPayload** (to players tracking the wearer): wearer entity id → earned badge ids in order.
- **SignPayload** (client → server): requirement key to sign on honour (server validates it's an honour requirement and open).
