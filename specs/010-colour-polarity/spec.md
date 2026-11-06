# Feature Specification: Colour polarity, lamp-style Lit Bits, one family of Wick recipes

**Feature Branch**: `010-colour-polarity`
**Created**: 2026-11-06
**Status**: Built
**Input**: User, 2026-11-06: "make our machines so any side can be positive or negative and get rid of the +/- symbols on our blocks. now that we have the double cables it doesn't really make much sense anyway. lets also model the Lit Bits off of our four twenty lamps. just use the exact same - full color with the glow." Then: red is + and black is −, each with a simple recipe; use Natural Hemp in the Sticky Finger's recipe; no daisy in the Daisy Wick's recipe: every kind of Wick in a tier is the same materials in different arrangements, easy to craft, tiered by materials (crafting balance for the whole mod comes later).

This amends specs/007-polarity-circuits (terminal faces) and specs/009-twin-daisy-wicks (recipes).

## Requirements

- **FR-001**: A powered block MUST have no + or − face. A red single Wick on any of its faces MUST join its + terminal, a black one its −. Single Wicks of other colours MUST NOT plug into blocks; they join only Wicks of their own colour.
- **FR-002**: Blocks touching each other MUST NOT connect. Series wiring is the Daisy Wick's.
- **FR-003**: The Hit Box keeps its two windings by face: front and back the high-voltage winding, left and right the low; top and bottom connect nothing. Red and black set + and − on each.
- **FR-004**: The + and − studs MUST no longer be drawn. Fittings are drawn on every face a red or black Wick, or a cable, plugs into.
- **FR-005**: A Lit Bit MUST look like a Four Twenty lamp: dark, the white lamp's unlit face; lit, the lamp's lit face in its colour, full bright, with the lamps' halo in its colour, fainter as it dims.
- **FR-006**: Recipes, per tier: the tier's Wick as before; eight of them round a red or black dye make eight red or black Wicks; a red and a black side by side make a Twin Wick, one above the other a Daisy Wick. A dyed Wick or cable keeps its colour when broken, places in it, and shows it in the inventory.
- **FR-007**: The Sticky Finger's recipe uses Natural Hemp instead of string.

## Consequences

- Reversed wiring can no longer happen with single Wicks (red only meets +, black only −), and cables orient themselves; the "wired backwards" status stays in the solver but isn't reachable by normal wiring.
- Builds on red and black rails keep working whatever way their blocks face. Builds whose rails are undyed, or another colour, need red and black.
- Game tests: `Ladder` lays red and black rails; tests that relied on fixed faces (series by touching blocks, wired backwards) were replaced by Daisy and "no black Wick" tests.
