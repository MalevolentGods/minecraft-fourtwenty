# Quickstart: checking the Kush Suit

## Automated

- `./gradlew test`: `HueFamiliesTest` (every colour in exactly one family or none; the ability list's colours match their pieces) and `GearRulesTest` (costs, capacities, charge steps, flight safety decisions).
- `./gradlew runGameTestServer -PcompatRunDir=.compat/runs/beds-dev`:
  - sober wearers get no powers and spend no BI;
  - each armour power works while high and charged;
  - flight ends safely on every end path, with Slow Falling and no damage on landing;
  - a station charges hung and worn gear, and overloads safely on too high a tier;
  - a Dab Cell tops up gear;
  - the Blazer modes break, fell and place correctly, and do nothing in protected spots.

## In game (local server)

1. Craft and charge a full suit at a Re-Up Station.
2. Smoke, put it on: night vision and HUD, a high jump, no knockback, stomp.
3. Fly 100 blocks up and let the high run out: you float down unharmed.
4. Fill each piece's terpene slots with the right colours and check each ability; a wrong-colour terpene is refused.
5. The Blazer: shoot a mob, bore a tunnel, fell a tree, paint a wall; try each in spawn protection.
6. Sober: the suit still protects you, but no powers work.
