# Data Model: Lifted Body

All state is in memory (server flight state, client render caches); nothing new is saved. After a restart a Builder loads on the ground, as now.

## Errand (server, per Builder)

| Field | Meaning |
|---|---|
| destination | the block MineColonies asked her to walk to |
| purpose | job (build, hut, racks) or break (restaurant, bed, hospital); for logs and the badge |
| reached | within the arrival distance of the destination |
| best, since | closest she's been to the destination and when (the 30 s progress rule) |

**Lifecycle:** created when MineColonies asks her to walk while she's Lifted → flown in legs → reached (lands; the navigator reports done) **or** no progress for 30 s (recalled) **or** her high ends / she's switched off (flies home or lands, as spec 013). A new walk request replaces the errand.

## Doorway (server, per Builder)

| Field | Meaning |
|---|---|
| position | the door's lower block, or the gate |
| openedByHer | she opened it (so she closes it) |
| passedAt | when she was last within 2 blocks |

**Rules:** opened as she comes within 1.5 blocks on her route; closed when she's 2 blocks past and nothing stands in it; only wooden doors and fence gates (FR-004).

## Route cell (pure, `FlightPlanner`)

Gains a `doorway` flag: a passable door or gate cell, at a small extra cost.

## Lifted look (client, per Builder)

| Field | Meaning |
|---|---|
| skin | her MineColonies skin texture |
| palette | hair, skin, cloth 1, cloth 2 colours (`SkinPalette`) |
| texture | the composed Lifted texture for that palette (cached per skin) |
| pose | current blend of cruise / hover / smoke / dance, with weights easing over 0.3-0.5 s |

## Settings

| Where | Setting | Default |
|---|---|---|
| `fourtwenty-builders-server.toml` `[lifted]` | `bodyFlight` | true |
| same | `liftedLook` | true |
| `fourtwenty-midnight-machine-client.toml` | `liftedLook` (the player's own) | true |
