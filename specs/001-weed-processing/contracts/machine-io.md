# Contract: Machine Item Input and Output

Applies to T2-T4 machines, the Hookahs' fuel slot, and any hopper or pipe mod using the NeoForge item capability.

| Side | Accepts | Gives |
|---|---|---|
| Top and sides | Valid inputs for this machine, into any input slot | Nothing |
| Bottom | Nothing | Anything in output slots |

- TRIM machines accept `rough_bud` only. CURE machines accept `trimmed_bud` only.
- Hookahs accept `rough_bud`, `trimmed_bud` and `weed_bud` from top and sides, and give nothing.
- Different strains never stack, and a machine processes each slot's strain separately.
- A machine with no output room stops and loses nothing.
- Breaking a machine drops all its contents.
- T1 machines expose no item capability.
