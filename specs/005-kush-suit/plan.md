# Implementation Plan: Kush Suit, Blazer and Charging

**Branch**: `005-kush-suit` | **Spec**: [spec.md](spec.md) | **Research**: [research.md](research.md) | **Data model**: [data-model.md](data-model.md) | **Quickstart**: [quickstart.md](quickstart.md)

## Technical Context

Java 21, NeoForge 21.1.218, Minecraft 1.21.1. JUnit in `src/test`, game tests in `src/gametest`. Art comes from `tools/generate_*.py`: armour layer textures, item icons, Re-Up Station models and the Dab Cell. No MineColonies code. New code lives in a `gear` package, with pure rules in `domain/GearRules` and `domain/HueFamilies`.

## Constitution Check

- **Nothing needs MineColonies:** yes.
- **Gear works only while high AND holding BI:** one shared check, `Gear.powered(player, stack)`, used by every power, with nothing reimplemented per piece. Armour protection always applies, as the user decided.
- **Flying can't kill you:** a shared `FlightLease`, generalised from Golden God's lent flight. Every way flight can end (sober, empty, Plate off, death, logout, dimension change) gives Slow Falling until the wearer lands. Moon Boots get no such rule.
- **Failure is safe:** a Re-Up Station on too high a tier overloads and stops, through the existing `BIBuffer` overload.
- **Approved names:** all from the plan (Hotbox Helm, Puff Plate, Puff Pack, Couchlock Greaves, Moon Boots, Blazer, Blaze Beam, Bore Toke, Timber Torch, Paint Puff, Re-Up Station, Dab Cell).
- **Pure rules in `domain/`, numbers in config:** capacities, costs, charge rates, stomp radius and beam damage are all config values. The ability list is data, so adding abilities takes no new code.
- **Payloads only to connections with the channel:** the Blazer's mode switch and beam effect are the only payloads, guarded by `hasChannel`.

Gates pass.

## Key decisions

(Details and alternatives in [research.md](research.md).)

- **BI on items:** one data component, `GearState` (charge, terpenes, Blazer mode), on all five pieces and the Dab Cell. Charge is an int, since 1M fits.
- **Costs fit the real BI economy.** Capacities stay as planned. Costs make a full piece last a good while: flight about 100 BI/t, so about 3 minutes from a full Plate; night vision about 2 BI/t. Charging runs at the station tier's wire rate (Puff 7, Toke 14, Rip 28, Blaze 56 BI/t), so gear is mid-to-late game.
- **Hue families from the colour code:** a final terpene's first two letters are its hue row, and each family takes set rows plus Minecraft colours (research R3). Greys, creams and browns belong to no family.
- **Abilities are data:** `data/fourtwenty/gear/abilities.json` lists each piece, colour, ability id and cost, read from the mod's own resources on both sides. Each ability id is a small Java handler; adding a colour for an existing ability is data only.
- **Terpene slots:** three per armour piece, four on the Blazer, edited in a small screen opened by sneak-using the piece in hand.
- **Powers run on the player tick:**
  - **Helm:** night vision refresh, plus a client HUD layer.
  - **Plate:** the flight lease.
  - **Greaves:** knockback cancelled; a crouch-landing stomp.
  - **Boots:** Jump Boost while powered; fall damage cancelled.
  - **Blazer:** a held tool; right-click fires, sneak-right-click in the air cycles unlocked modes.
- **Block changes** go through `ServerPlayerGameMode.destroyBlock` (Bore Toke, Timber Torch) and a place-with-events helper (Paint Puff), so protection, claims and drops behave like mining by hand.
- **Re-Up Station:** a tiered BI block in the shape of the Construction bench. It has a small Wick-fed intake, one hanging slot that charges the item in it, and a pad that charges a standing player's worn pieces and any Dab Cell they carry.
- **Dab Cell:** a 400,000 BI handheld battery. Carried, it tops up worn gear each second.

## Build order

1. **Rules:** `HueFamilies`, `GearRules` (costs, capacities, charge steps, stomp maths, flight safety decisions) with JUnit tests; config.
2. **Items and state:** the five pieces as armour and a tool, `GearState`, tooltips, the shared `Gear.powered` check, the terpene screen, `abilities.json`.
3. **Armour powers and the HUD:** Helm, Greaves, Boots, then the Plate with `FlightLease`. Game tests for each, flight safety above all.
4. **Charging:** Re-Up Station tiers and the Dab Cell, with game tests for hanging, pad and Dab Cell charging, and overload.
5. **The Blazer:** modes, Blaze Beam, Bore Toke, Timber Torch, Paint Puff, the protection checks; game tests.
6. **Art:** armour layers, icons, station models, beam particles.
7. **Docs** (README, CHANGELOG), then the in-game walkthrough on the local server.

## Risks

- **Flight feel:** lent creative flight (`mayfly`) is simple and safe, but floaty. A thrust-style jetpack is possible later; the safety rule stays the same either way.
- **Balance:** capacities against the BI economy. All costs are in config and get tuned in playtests.
- **Art volume:** five armour and tool textures and four station tiers. Generated first, and replaceable with your art later.
