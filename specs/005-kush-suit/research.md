# Research: Kush Suit, Blazer and Charging

## R1. The "powered" rule

- **Decision:** `Gear.powered(player, stack) = StrainEffects.isHigh(player) && charge(stack) >= cost`. Every power asks it before acting, and spends only when it acts.
- **Rationale:** the constitution requires both conditions and one rule. Armour protection sits outside it (the user's call), as a normal armour material.

## R2. Flight safety

- **Decision:** a `FlightLease` generalising `GoldenGod`'s lent flight. It sets `mayfly` only if the player didn't already have it, and on every end path takes flight back and gives Slow Falling until landing. The end paths are lost high, empty Plate, Plate removed, death, logout, dimension change and server stop. A login check takes back any flight lent before a crash.
- **Rationale:** that code is already tested in production for Golden God. One lease shared by the Plate and Golden God means one safety path.
- **Alternatives:** a velocity-thrust jetpack. It feels better but needs client input packets and its own safety; kept for later.

## R3. Hue families

- **Decision:** by the final terpene's first two letters (its hue row), plus the matching Minecraft colours:

| Family | Rows | Minecraft colours |
|---|---|---|
| Reds and oranges (Blazer) | `lim_pin`, `lim_lav` | red, orange |
| Yellows (Helm) | `lim_lim` | yellow |
| Greens (Plate) | `pin_lim`, `pin_pin`, `pin_myr` | green, lime |
| Blues (Boots) | `lav_pin`, `lav_lav`, `pin_lav` | blue, light blue, cyan |
| Purples (Greaves) | `lav_myr`, `lav_lim` | purple, magenta, pink |

  Rows `myr_*` (greys and creams) and `lim_myr` (browns) belong to no family.
- **Rationale:** these rows are the palette's hues as drawn (`TerpenePalette.FINALS`). An explicit table is testable and easy to change.

## R4. Ability data

- **Decision:** `data/fourtwenty/gear/abilities.json` holds a list of `{piece, colour, ability, cost}`, read from the mod's resources at startup on both sides, with a JUnit test that every entry's colour is in its piece's family. Each ability id maps to a Java handler.
- **Starting set (FR-013):**

| Piece | Abilities |
|---|---|
| Helm | ore sense, mob sense, zoom |
| Plate | hover, toughness, fast flight |
| Greaves | big stomp, resistance, slow fall |
| Boots | higher jump, water walking, speed |
| Blazer | Bore Toke, Timber Torch, Paint Puff, beam power |

## R5. Costs and charging (config defaults)

| Piece | Capacity | Cost |
|---|---|---|
| Helm | 100k | night vision 2 BI/t; senses 4 BI/t; zoom 2 BI/t |
| Plate | 400k | flight 100 BI/t (about 3.3 minutes); hover +20 BI/t |
| Greaves | 200k | 200 per knockback cancelled; stomp 2,000 |
| Boots | 100k | 200 per boosted jump; 500 per fall cancelled; water walking 5 BI/t |
| Blazer | 1M | beam 500 per shot; Bore Toke 300 per block; Timber Torch 100 per log; Paint Puff 50 per block |
| Dab Cell | 400k | moves up to 100 BI/t into worn gear |

Re-Up Station charge rate is the tier's wire rate: Puff 7, Toke 14, Rip 28, Blaze 56 BI/t.

## R6. Block-changing modes and protection

- **Decision:** Bore Toke and Timber Torch break through `ServerPlayerGameMode.destroyBlock`, which fires the break event (claims, spawn protection) and drops items. Timber Torch is capped at 256 logs. Paint Puff places with `BlockEvent.EntityPlaceEvent` and `mayInteract` checks. Bore Toke skips unbreakable blocks and blocks with an inventory.
