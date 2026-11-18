# Contract: Payloads and commands

All payloads are sent only to connections that have the channel (`hasChannel`).

| Payload | Direction | Content | When |
|---|---|---|---|
| `fourtwenty:handbook_progress` | server → owner | full or diff: ticks (key, state, time), counts (key, n), earned (id, time) | login (full); after any change (diff) |
| `fourtwenty:handbook_notice` | server → owner | requirement key or earned id | a tick or an earning (the client shows the notice and sound) |
| `fourtwenty:handbook_sign` | client → server | requirement key | the player signs an honour requirement (validated: honour, open, prerequisites) |
| `fourtwenty:handbook_sash` | server → trackers | wearer entity id, earned badge ids in order | start tracking a wearer; a wearer earns a badge; the sash put on or taken off |

## Commands (op level 2)

- `/fourtwenty handbook reset <player> [all|rank <id>|badge <id>]`: back to open (FR-027).
- `/fourtwenty handbook tick <player> <requirement key>`: for testing (game tests, balance runs).
- `/fourtwenty handbook show <player>`: ranks, badges and open requirements in chat.
- Client, dev only: `/fourtwenty handbook export`: item icons for the PDF (research R6).

## Config

- Server: `handbookAnnounceInChat` (bool, default true), `handbookCatchUp` (bool, default true).
- Client: notice duration, notice sound on/off.
