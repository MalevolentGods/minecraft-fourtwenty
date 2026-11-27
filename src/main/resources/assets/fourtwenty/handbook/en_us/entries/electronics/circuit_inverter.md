---
title: Circuit 2: An Inverter
icon: fourtwenty:not_nug
terms: [NOT Nug, inverter]
---
**The goal:** a light that's on while a switch is **off**, and off while it's on. A **NOT** gate flips its input.

{image:scenes/circuit_inverter|The inverter on its board|wide}

**You'll need:** your [workbench](entry:electronics/board), a **NOT Nug** and a **Toggle Joint**.

1. Place the **NOT Nug** beside the board with its front facing away from the switch: input at its back, output in front.
2. Give it power: a red Wick from the + rail to its **top**, and a black Wick from the − rail to its **bottom**.
3. Put a **Toggle Joint** at the Nug's back, so it touches its input. Feed the switch's other side with a red Wick from the + rail.
4. Watch the Nug's face.

**What happens:** switch open, the input is 0, so the output is 1: the Nug glows. Close the switch and the input is 1, so the output is 0: dark.

{truth:NOT}

**The real thing:** an **inverter**, the simplest gate in every chip.
