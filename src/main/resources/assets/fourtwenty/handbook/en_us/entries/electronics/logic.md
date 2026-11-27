---
title: Logic: Yes, No and Maybe Not
icon: fourtwenty:and_nug
terms: [Boolean logic, gate, truth table, NOT, AND, OR, NAND, NOR, XOR, XNOR, inverter]
---
A **logic gate** looks at one or two on-or-off inputs and decides whether to switch its output on. Write 1 for on and 0 for off, and a **truth table** shows every case.

# NOT: the Contrary One
Output the opposite of the input. Its real name is the **inverter**.

{truth:NOT}

# AND: Both or Nothing
On only when **both** inputs are on.

{truth:AND}

# OR: Either Will Do
On when **either** input is on, or both.

{truth:OR}

# XOR: One or the Other
On when the inputs **differ**: one on, one off, but not both.

{truth:XOR}

# The Negative Ones
**NAND** is an AND turned upside down: off only when both inputs are on. **NOR** is an OR turned upside down: on only when both are off. **XNOR** is an XOR turned upside down: on when the inputs match.

{truth:NAND}

{truth:NOR}

{truth:XNOR}

# Why Bother?
Out of just these, you can build anything a computer does. NAND alone is enough to build the lot, if you have a few thousand of them and nothing planned for the next year. We won't ask that of you.
