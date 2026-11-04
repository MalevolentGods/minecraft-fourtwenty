# Data model: Kush Suit, Blazer and Charging

## GearState (data component on each piece and the Dab Cell)

| Field | Meaning |
|---|---|
| charge | BI held, 0 to the piece's capacity |
| terps | up to 3 terpene stacks (4 on the Blazer), each of the piece's hue family |
| mode | Blazer only: the selected mode (beam, bore, timber, paint) |

## Pieces

| Piece | Slot | Capacity | Family | Built-in powers |
|---|---|---|---|---|
| Hotbox Helm | head | 100,000 | yellows | night vision, HUD |
| Puff Plate | chest | 400,000 | greens | Puff Pack flight (heaviest armour) |
| Couchlock Greaves | legs | 200,000 | purples | no knockback, stomp |
| Moon Boots | feet | 100,000 | blues | high jump, no fall damage |
| Blazer | hand (tool) | 1,000,000 | reds and oranges | Blaze Beam |

## Ability (from `abilities.json`)

`{ "piece": "moon_boots", "colour": "lav_lav_lim", "ability": "water_walk", "cost": 5, "per": "tick" }`. `per` is `tick` (while running) or `use` (each time).

## FlightLease (per player, server)

`granted` (whether flight was lent), `source` (Puff Plate or Golden God). It ends with Slow Falling if the player is in the air.

## Re-Up Station (block entity, per tier)

| Field | Meaning |
|---|---|
| intake | small BI buffer fed by Wicks (overloads above its tier) |
| hung | one gear item or Dab Cell being charged |
| rate | tier wire rate: 7, 14, 28 or 56 BI/t |

The pad charges the worn pieces and carried Dab Cell of any player standing on the block, shared out of the same rate.

## States of a piece

- **Powered:** high, and charge at least the cost.
- **Idle:** otherwise. Armour protection always applies.
- **Charging:** on a station, a pad or from a Dab Cell; independent of being high.
