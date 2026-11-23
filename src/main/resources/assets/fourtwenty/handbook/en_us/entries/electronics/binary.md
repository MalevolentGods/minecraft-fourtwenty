---
title: Counting on Two Fingers
icon: fourtwenty:jar_nug
terms: [binary, bit, byte, base 2, carry]
---
You count in tens because you have ten fingers. A computer has two states, **off** and **on**, so it counts in **twos**. Each on-or-off switch is a **bit**, and a row of eight is a **byte**.

# How It Works
In ordinary counting, each place is worth ten times the one to its right: ones, tens, hundreds. In binary, each place is worth **twice** the one to its right: 1, 2, 4, 8, 16, 32, 64, 128.

Write a 1 where you want that place counted, and add them up. So 1011 is 8 + 0 + 2 + 1 = **11**.

{table}
| Number | Binary |
| 0 | 0000 |
| 1 | 0001 |
| 2 | 0010 |
| 3 | 0011 |
| 4 | 0100 |
| 5 | 0101 |
| 6 | 0110 |
| 7 | 0111 |
| 8 | 1000 |
{/table}

# Eight Bits
A byte, eight bits, counts from 0 (00000000) to **255** (11111111). The mod's **Eighth Ribbon** carries a byte in nine wires: eight for the bits, and a ninth, the **carry**, for when the answer runs past 255.

# Adding
Binary adds just like you learned at school, except you carry whenever you reach **2**, not 10. 1 + 1 = 10 (that's two: a one and a zero). The last circuit in this section adds two bits for you.
