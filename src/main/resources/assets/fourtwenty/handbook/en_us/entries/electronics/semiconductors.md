---
title: One-Hitters, Zeners and FETs
icon: fourtwenty:sativa_fet
terms: [semiconductor, diode, One-Hitter, Zener, Zen Hitter, regulator, transistor, MOSFET, Sativa FET, Indica FET, CMOS, op-amp, Hype Amp, gain]
---
Everything so far has been straightforward: double the push, double the flow. The parts on this page aren't like that. They're made of **semiconductors**, materials that conduct only under the right conditions, and they're the reason a computer now fits in a pocket instead of filling a building.

# The One-Hitter: One Way Only
A **One-Hitter** is a **diode**: a one-way valve. Push it the right way, into its plain end and out of its banded end, and once the push across it passes about **0.65 Kb**, it conducts. Push it backwards and nothing gets through. Use one to stop flow going where it shouldn't.

That 0.65 Kb isn't free. Every One-Hitter in a path costs about 0.65 Kb of push, and on a 4.2 Kb rail that's 15% gone at each one.

# The Zen Hitter: Holding Steady
A **Zen Hitter** is a **Zener diode**, built to be pushed backwards. Wired banded end to +, it holds the push across it at its rated voltage, whatever the push above it does, within reason. Add a Resinator in series to limit the flow and you have a **regulator**. A 3.3 Kb Zen Hitter under a 1,000 Φ Resinator on a 4.2 Kb rail holds about 3.3 Kb; the Resinator takes the other 0.9 Kb, and 0.9 mIg flows.

# The FET: a Switch You Steer
A **Sativa FET** is a **transistor**, an N-channel MOSFET, with three pins: **source** at its back, **drain** in front, **gate** on its left. Raise the gate more than about **1 Kb** above the source and the path from drain to source opens; drop it below that and the path closes. The gate itself takes almost no flow at all: with nothing but push, you steer a flow.

An **Indica FET** (P-channel) is the mirror image: it opens when its gate is pulled more than 1 Kb *below* its source. Sativa up, Indica down, just like the strains.

# Two FETs Make a NOT
Put an Indica FET between the + rail and an output, a Sativa FET between the output and the − rail, and join their gates as the input. Input high: the Sativa opens, the Indica closes, and the output is pulled down to 0. Input low: the other way round, and the output goes up to 1. That's a **NOT gate** made of two transistors. The design is called **CMOS**, and it's how almost every logic gate in the real world is built, billions at a time, in every phone and computer.

# The Hype Amp: Turning It Up
A **Hype Amp** is an **op-amp**: it takes the difference between its two inputs, multiplies it by **100,000**, and puts that on its output, as far as its supply allows. That's far too much to use raw, so you tame it with two Resinators: feed part of the output back to its − input, and the gain becomes their ratio. With 10 kΦ from the output to −, and 1,000 Φ from − to the − rail, a small signal on + comes out **11 times** bigger: 1 + 10,000 ÷ 1,000.
