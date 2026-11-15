# Contracts: settings, the toggle, the art sheet

## Server settings (`fourtwenty-builders-server.toml`, `[lifted]`)

| Key | Type | Default | Off means |
|---|---|---|---|
| `bodyFlight` | bool | true | 0.26.0's movement: only build-site trips fly; the walk-in and stranded rescues return |
| `liftedLook` | bool | true | everyone sees 0.26.0's look (MineColonies' citizen with the boombox, glow and dance) |

Sent to clients with the existing Lifted payload, so a client never draws the new look when the server has it off.

## Client setting (`fourtwenty-midnight-machine-client.toml`, `[midnightMachine]`)

| Key | Type | Default |
|---|---|---|
| `liftedLook` | bool | true |

## Pause menu

- Under ModularCraft's button, beside the Midnight Machine volume and mute: a toggle **"Lifted look: On / Off"** *(proposed, for the user to approve)*, tooltip "Show Lifted Builders in their Lifted look. Off shows them as MineColonies draws them."
- Only shown when the server has `liftedLook` on.

## Art sheet (one generated image)

- Panels for each model part's faces (head, neck, torso upper and lower, upper and lower arms and legs, hands, feet), on flat magenta (#FF00FF), at least 40 px apart, laid out as the prompt specifies once the silhouette is picked.
- Each panel paints the design with **separate flat colour zones** for hair, skin and the two clothing colours (the importer turns each zone into a tint mask), plus the fixed details (shading, seams, accessories) that aren't tinted.
- Imported sizes are multiples of 16 (the block-atlas mipmap rule found in spec 014).
