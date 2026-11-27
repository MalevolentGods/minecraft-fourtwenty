---
title: Circuit 6: Adding in Binary
icon: fourtwenty:stacker_nug
terms: [half adder, XOR Nug, adder, binary addition]
---
**The goal:** add two one-bit numbers, A and B, and show the answer in binary on two lights. This is a **half adder**: the first step on the road to a calculator.

{image:scenes/circuit_adder|The half adder, two switches in and sum and carry out|wide}

**You'll need:** your [workbench](entry:electronics/board), an **XOR Nug** and an **AND Nug**, and two **Toggle Joints**.

Recall [Counting on Two Fingers](entry:electronics/binary): 1 + 1 = 10 in binary. So the answer needs two lights: the **sum** (the ones place) and the **carry** (the twos place).

- The **XOR** gate is on when the inputs differ: that's the **sum**.
- The **AND** gate is on when both inputs are on: that's the **carry**.

1. Place the **XOR Nug** and the **AND Nug** one above the other, both with their fronts the same way, each with power on top and bottom.
2. Run a **Hemp Thread** down the left side of both gates, and another down the right side. These carry A and B to both at once.
3. Put a **Toggle Joint** on each thread: switch **A** on the left, switch **B** on the right, each fed from the + rail.
4. Flip the switches through all four cases.

{table}
| A | B | Carry | Sum | Result |
| 0 | 0 | 0 | 0 | 00 = 0 |
| 1 | 0 | 0 | 1 | 01 = 1 |
| 0 | 1 | 0 | 1 | 01 = 1 |
| 1 | 1 | 1 | 0 | 10 = 2 |
{/table}

**The real thing:** a **half adder**. Chain these into a **full adder**, take eight of those, and you have the **Stacker Nug**: a byte adder, the thing inside every calculator.

# Where Next?
You can now build circuits from the book. The next steps are yours: a **Numb Nug** to show a number, a **Weedweeno** to run a light show, a **Dankoder** to read a keypad. In the back of the book, the **translation table** will tell you the real name of every part you meet. And if you want to know *why* any of this works, the pages after this one go deeper, starting with [Ohm's Law at Work](entry:electronics/ohms_law).
