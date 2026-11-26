---
title: Ohm's Law at Work
icon: fourtwenty:inspoweedometer
terms: [Ohm's law, worked examples, power formula, burning out, Wick rating, Resinator rating, machine load]
---
[Volts, Amps and Ohms](entry:electronics/theory) gave you the rule: **push = flow × squeeze**, or in Weedcraft's units, **Kb = Ig × Φ**. This page puts it to work. Every number here can be checked in the world with an Inspoweedometer, and you should check them, because the people who wrote this page were high.

# Three Ways Round
{table}
| To find | Use |
| push (Kb) | flow × squeeze |
| flow (Ig) | push ÷ squeeze |
| squeeze (Φ) | push ÷ flow |
{/table}

Add the power rule, **W = Kb × Ig**, and Ohm's law gives you two more you'll use all the time: **W = Ig² × Φ** and **W = Kb² ÷ Φ**.

# What Flows Through a Wick?
A Hookah makes 420 W at Puff's 4.2 Kb, so it sends 420 ÷ 4.2 = **100 Ig** down its Wicks. A Hemp Wick is rated for 200 Ig, which is where "a Wick carries about two Hookahs" comes from. Put three Hookahs' worth through it and 300 Ig tries to squeeze through a Wick built for 200. You know how that ends: smoke, ash, and a walk to fetch another Wick.

# What Does a Machine Take?
A Hookah runs four machines of its tier, so a Puff machine takes 420 ÷ 4 = **105 W**, which at 4.2 Kb is 105 ÷ 4.2 = **25 Ig**. Four of them draw exactly one Hookah's 100 Ig. Seen from the Wick, a running machine looks like a squeeze of 4.2² ÷ 105 = **0.168 Φ**.

# Will This Resinator Survive?
A Resinator is rated for **10 W**; past that, it burns out. Put one straight across the Puff rails and it has to take W = Kb² ÷ Φ:

{table}
| Resinator | Flow | Heat | Survives? |
| 1 Φ | 4.2 Ig | 17.6 W | no |
| 2.2 Φ | 1.9 Ig | 8.0 W | yes, warmly |
| 1,000 Φ | 4.2 mIg | 0.018 W | easily |
{/table}

So on Puff, nothing under 2.2 Φ goes straight across the rails. Do this sum before you place a Resinator and you'll burn out far fewer of them.

# Try It
Put a 1,000 Φ Resinator between the rails and read it: 4.2 Kb across it, 4.2 mIg through it. Step it up to 2,200 Φ and the flow drops to 1.9 mIg. Georg Ohm worked this rule out in 1827 with a thermocouple and a great deal of patience. You just did it in a field.
