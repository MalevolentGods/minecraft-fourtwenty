---
title: Kief Caps and Spliff Coils
icon: fourtwenty:kief_cap
terms: [capacitor, capacitance, Garcia, inductor, inductance, Marley, time constant, Slow Burner, ramp, charge]
---
A Resinator does the same thing from one moment to the next. These two parts don't: what they do depends on what happened a moment ago. That's what makes timers, filters and radios possible.

# The Kief Cap
A **Kief Cap** is a capacitor: two plates, close together but never touching, that store charge between them. Its size, **capacitance**, is measured in **Garcias (Ga)**; the real unit is the **farad**. A Kief Cap holds charge in proportion to the push across it:

**charge = Ga × Kb**

Charge comes in Ignus-seconds (the real world's **coulomb**): one Ignus flowing for one second. Steady flow can't get through a Kief Cap, because its plates don't touch. But it fills up, and once it's full it keeps its push even after the supply has gone.

# Filling Takes Time
Fill a Kief Cap through a Resinator and it fills quickly at first, then slower and slower, because the fuller it gets, the less push is left over to fill it. How fast is set by one number, the **time constant**:

**time constant (seconds) = Φ × Ga**

After one time constant it's at about **63%** of the push; after five, it's as good as full. A 1,000 Φ Resinator and a 1 mGa Kief Cap, at their starting values, make exactly **one second**.

# Try It
Wire the + rail to a 1,000 Φ Resinator, then a 1 mGa Kief Cap, then the − rail. Read the Kief Cap: about 2.7 Kb after one second, nearly the full 4.2 after five. Step the Resinator to 10 kΦ and the whole thing takes ten times as long.

# A Steady Fill
Swap the Resinator for a **Slow Burner** and the Kief Cap fills at a steady rate instead, in a straight line: **Kb per second = Ig ÷ Ga**. A Slow Burner at 1 mIg into a 1 mGa Kief Cap climbs **1 Kb every second**, until the push runs out. A steady ramp like this is how real circuits measure time.

# The Spliff Coil
A **Spliff Coil** is an inductor: a coil of wire that stores energy in a magnetic field and fights any change in the flow through it. Its size, **inductance**, is measured in **Marleys (Ma)**; the real unit is the **henry**. Steady flow passes through it as if it were a plain Wick. Change the flow and it pushes back:

**push = Ma × change in flow (Ig per second)**

Cut the flow through a coil suddenly and it throws up a huge push for an instant, trying to keep the flow going. That's why real circuits put a diode across every coil, to give that flow somewhere harmless to go; you'll meet the diode in [One-Hitters, Zeners and FETs](entry:electronics/semiconductors). With a Resinator, a coil's time constant is **Ma ÷ Φ** seconds.
