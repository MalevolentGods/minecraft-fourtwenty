# Contract: Boundless Inspiration Grid

Block capability `fourtwenty:bi`, available to future Four Twenty machines.

```java
interface BINode {
    int tier();                                 // 1 Puff, 2 Toke, 3 Rip
    long stored();
    long capacity();
    long receive(long amount);
    long extract(long amount);
    boolean canReceive();
    boolean canExtract();
}
```

- Sources push through Wicks; sinks don't pull.
- A push is capped by the source's tier rate and by the lowest Wick on its route.
- A Wick or sink below the source's tier burns out instead of receiving power.
- Every powered machine MUST ask `MachinePower` whether it may run. Machines MUST NOT test highness themselves.
- `MachinePower.spendOnce(level, pos, cost)` serves machines with no block entity.
