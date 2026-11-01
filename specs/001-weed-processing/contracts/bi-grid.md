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
- A push is capped by the source's tier rate.
- A Wick burns out when the power through it in one second passes twice a Hookah of its tier, across all sources.
- A sink below the source's tier overloads instead of receiving power: it stops and smokes, and never breaks.
- Every powered machine MUST ask `MachinePower` whether it may run. Machines MUST NOT test highness themselves.
- `MachinePower.spendOnce(level, pos, cost)` serves machines with no block entity.
