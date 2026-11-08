#!/usr/bin/env python3
"""The real electronics name of every electrical part, shown quietly under its name in its tooltip (tooltip.fourtwenty.real.<id>).

    python3 tools/generate_real_terms.py

Run it after tools/generate_components.py (which writes the same lang file). Keep it in step with any new electrical part.
"""
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
LANG = ROOT / 'src/main/resources/assets/fourtwenty/lang/en_us.json'

WIRE = 'Wire'
TWIN = 'Two-core cable (parallel)'
DAISY = 'Two-core cable (series, a daisy chain)'

REAL = {
    # power
    'weed_bong': 'Generator', 'double_hose': 'Generator', 'big_bertha': 'Generator', 'grand_hookah': 'Generator',
    'stash_jar': 'Battery', 'stash_box': 'Battery', 'mother_stash': 'Battery', 'the_vault': 'Battery',
    'hit_box': 'Transformer',
    'hemp_wick': WIRE, 'rolled_wick': WIRE, 'glass_wick': WIRE, 'gold_wick': WIRE,
    'hemp_twin_wick': TWIN, 'rolled_twin_wick': TWIN, 'glass_twin_wick': TWIN, 'gold_twin_wick': TWIN,
    'hemp_daisy_wick': DAISY, 'rolled_daisy_wick': DAISY, 'glass_daisy_wick': DAISY, 'gold_daisy_wick': DAISY,
    'dab_cell': 'Power bank',
    're_up_station_puff': 'Charger', 're_up_station_toke': 'Charger', 're_up_station_rip': 'Charger', 're_up_station_blaze': 'Charger',
    'bottomless_stash_puff': 'Ideal voltage source', 'bottomless_stash_toke': 'Ideal voltage source',
    'bottomless_stash_rip': 'Ideal voltage source', 'bottomless_stash_blaze': 'Ideal voltage source',
    'bench_rig': 'Bench power supply (variable DC)',
    'inspoweedometer': 'Multimeter · Kb volts, Ig amps, W watts, Φ ohms, Ga farads, Ma henries',
    # lights and programming
    'lit_bit': 'RGB LED',
    'weedweeno': 'Microcontroller',
    'bit_chit': 'EEPROM',
    'sticky_finger': 'Pick-and-place tool',
    'twister': 'Wrench',
    # components
    'hemp_thread': 'Signal wire',
    'resinator': 'Resistor',
    'kief_cap': 'Capacitor',
    'spliff_coil': 'Inductor',
    'one_hitter': 'Diode',
    'zen_hitter': 'Zener diode',
    'toggle_joint': 'Toggle switch',
    'kill_switch': 'Relay',
    'puff_button': 'Momentary push button',
    'slow_burner': 'Current regulator',
    'not_nug': 'NOT gate (inverter)',
    'buffer_nug': 'Buffer',
    'and_nug': 'AND gate',
    'or_nug': 'OR gate',
    'nand_nug': 'NAND gate',
    'nor_nug': 'NOR gate',
    'xor_nug': 'XOR gate',
    'xnor_nug': 'XNOR gate',
    'tri_state_nug': 'Tri-state buffer',
    'latch_nug': 'SR latch',
    'flip_flop_nug': 'D flip-flop',
    'ticker': 'Clock (square-wave oscillator)',
    'sativa_fet': 'N-channel MOSFET',
    'indica_fet': 'P-channel MOSFET',
    'hype_amp': 'Operational amplifier (op-amp)',
    'eighth_ribbon': 'Ribbon cable (8-bit bus)',
    'roach_clip': 'Bus tap',
    'rolling_tray': 'Backplane (PCB)',
    'skip_me_bro': 'Jumper (crossover)',
    'puff_pass_nug': 'Shift register, serial in, parallel out (74HC595)',
    'pack_nug': 'Shift register, parallel in, serial out (74HC165)',
    'sampler_nug': 'Multiplexer, 8 to 1',
    'dealer_nug': 'Demultiplexer, 1 to 8',
    'blend_nug': 'Bitwise logic unit',
    'stacker_nug': '8-bit adder / subtractor',
    'scale_nug': 'Magnitude comparator',
    'jar_nug': '8-bit register (74HC574)',
    'numb_nug': '7-segment display',
    'dankoder': 'Keypad encoder (74C922)',
}


def main():
    lang = json.loads(LANG.read_text(encoding='utf-8'))
    for k in [k for k in lang if k.startswith('tooltip.fourtwenty.real.')]:
        del lang[k]
    for item, real in REAL.items():
        lang[f'tooltip.fourtwenty.real.{item}'] = real
    out = '{\n' + ',\n'.join(f'  {json.dumps(k, ensure_ascii=False)}: {json.dumps(v, ensure_ascii=False)}' for k, v in lang.items()) + '\n}\n'
    LANG.write_text(out, encoding='utf-8')
    print(f'wrote {len(REAL)} real names')


if __name__ == '__main__':
    main()
