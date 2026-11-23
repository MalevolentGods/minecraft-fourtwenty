---
title: Circuit 4: A One-Bit Memory
icon: fourtwenty:latch_nug
terms: [Latch Nug, latch, memory]
---
**The goal:** a light you can turn on with one switch and off with another, and that **stays** as you left it. Circuits that remember are the start of every computer.

**You'll need:** your [workbench](entry:electronics/board), a **Latch Nug** and two **Toggle Joints** (or Puff Buttons).

1. Place the **Latch Nug** like the AND gate in [Circuit 3](entry:electronics/circuit_lock): power on top and bottom, a switch on each side.
2. The **left** switch is **Set**, the **right** is **Reset**.
3. Flick **Set** on and then off again.
4. Flick **Reset** on and then off again.

**What happens:** after Set, the Nug stays lit even though you let go of the switch: it **remembers**. Only Reset turns it off, and it stays off.

**The real thing:** an **SR latch**: one bit of memory. A row of eight of them is a byte.
