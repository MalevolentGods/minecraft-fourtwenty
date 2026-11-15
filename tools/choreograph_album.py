"""Bespoke dance routines for the Midnight Machine album (spec 014 T031).

    python tools/choreograph_album.py [--force] [--only track3]

For each track, from the beat map in album.json and the VU bands in trackN.bands (both written by
tools/beatmap_album.py), this finds the song's sections and drafts a routine she dances along with it:

- **Bars:** four of her dance beats each (every other beat on the double-time tracks above 140 BPM, as DanceRules
  dances them). Each bar gets the mean of its six band levels and its overall energy.
- **Sections:** a boundary where the four bars before and the four after differ most (novelty), at least
  MIN_SECTION_BARS apart. Each section is labelled by its energy within the track and its trend: intro (a quiet
  start), outro (a quiet end), drop (loud), breakdown (quiet), build (rising), otherwise verse.
- **Moves:** each label takes the move the track's own palette names for it (PALETTES below: the flavour comes from
  the track's title). Man and Machine swaps between a robot move and a human one section by section.
- **Flourishes:** short showy moves on the section starts that lift and on the loudest accents, at least
  FLOURISH_GAP_MS apart and about one per FLOURISH_EVERY_MS, taking turns through the track's flourish set.

Writes assets/fourtwenty/midnight_machine/trackN.routine.json. A routine is meant to be hand-tuned afterwards, so an
existing file is left alone unless --force. Move and flourish names must be ones DanceMoves knows (DanceMovesTest
checks every routine shipped).
"""
import argparse
import json
from pathlib import Path

import numpy as np

ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / 'src/main/resources/assets/fourtwenty/midnight_machine'
BANDS = 6
BAND_MS = 50
DOUBLE_TIME_BPM = 140
MIN_SECTION_BARS = 6
FLOURISH_GAP_MS = 8000
FLOURISH_EVERY_MS = 20000

# Each track's moves by section label, and its flourishes. Original moves from ordinary dance vocabulary only.
PALETTES = {
    'track1': {  # Rise and Peak: arms climb through the builds, a big reach and spin at the peak
        'intro': 'groove', 'verse': 'step_touch', 'build': 'reach_climb', 'drop': 'lean_pump',
        'breakdown': 'float', 'outro': 'wave_overhead',
        'flourishes': ['big_spin_point', 'dip_and_rise'],
    },
    'track2': {  # Smoke and Mirrors: hazy, smooth, mirrored
        'intro': 'float', 'verse': 'mirror', 'build': 'shoulder_shimmy', 'drop': 'mirror',
        'breakdown': 'float', 'outro': 'float',
        'flourishes': ['slow_turn_open', 'dip_and_rise'],
    },
    'track3': {  # Listen and Learn: nods, a cupped ear, conducting the band
        'intro': 'cupped_ear', 'verse': 'conduct', 'build': 'air_synth', 'drop': 'lean_pump',
        'breakdown': 'cupped_ear', 'outro': 'conduct',
        'flourishes': ['ear_cup_freeze', 'big_spin_point'],
    },
    'track4': {  # Awake and Adapt: sharp robot pops that loosen up
        'intro': 'robot_pop', 'verse': 'robot_pop', 'build': 'shoulder_shimmy', 'drop': 'step_touch',
        'breakdown': 'robot_pop', 'outro': 'groove',
        'flourishes': ['double_pop', 'jump_kick'],
    },
    'track5': {  # Beauty and Light: wide open arms, slow graceful turns
        'intro': 'float', 'verse': 'wave_overhead', 'build': 'reach_climb', 'drop': 'wave_overhead',
        'breakdown': 'float', 'outro': 'float',
        'flourishes': ['slow_turn_open', 'big_spin_point'],
    },
    'track6': {  # Far and Away: floaty, gliding, a hand shading the eyes toward the horizon
        'intro': 'horizon', 'verse': 'float', 'build': 'step_touch', 'drop': 'windmill',
        'breakdown': 'horizon', 'outro': 'horizon',
        'flourishes': ['dip_and_rise', 'slow_turn_open'],
    },
    'track7': {  # Man and Machine: robot and human halves, section by section
        'intro': 'robot_pop', 'verse': 'air_synth', 'build': 'shoulder_shimmy', 'drop': 'robot_pop',
        'breakdown': 'groove', 'outro': 'robot_pop',
        'human': ['air_synth', 'step_touch', 'groove'], 'machine': ['robot_pop'],
        'flourishes': ['double_pop', 'big_spin_point'],
    },
}


def bars_of(track):
    beats = np.array(track['beats_ms'], dtype=float)
    step = 2 if track['bpm'] > DOUBLE_TIME_BPM else 1
    dance_beats = beats[::step]
    return dance_beats[::4]   # bar starts, ms


def features(levels, starts, end_ms):
    edges = list(starts) + [end_ms]
    out = []
    for a, b in zip(edges[:-1], edges[1:]):
        fa, fb = int(a // BAND_MS), max(int(a // BAND_MS) + 1, int(b // BAND_MS))
        chunk = levels[fa:fb]
        out.append(chunk.mean(axis=0) if len(chunk) else np.zeros(BANDS))
    return np.array(out)


def boundaries(feat):
    n = len(feat)
    w = 4
    novelty = np.zeros(n)
    for i in range(w, n - w):
        novelty[i] = np.linalg.norm(feat[i - w:i].mean(axis=0) - feat[i:i + w].mean(axis=0))
    cut = novelty.mean() + 0.5 * novelty.std()
    picked = []
    for i in np.argsort(-novelty):
        if novelty[i] < cut:
            break
        if i < MIN_SECTION_BARS or n - i < MIN_SECTION_BARS // 2:
            continue
        if all(abs(i - p) >= MIN_SECTION_BARS for p in picked):
            picked.append(int(i))
    return sorted(picked)


def label(i, count, energy_rank, trend):
    if i == 0 and energy_rank < 0.5:
        return 'intro'
    if i == count - 1 and energy_rank < 0.5:
        return 'outro'
    if energy_rank >= 0.7:
        return 'drop'
    if energy_rank <= 0.3:
        return 'breakdown'
    if trend > 0.08:
        return 'build'
    return 'verse'


def routine(track, levels):
    tid = track['id']
    palette = PALETTES[tid]
    end_ms = track['length_ms']
    starts = bars_of(track)
    feat = features(levels / 255.0, starts, end_ms)
    energy = feat.mean(axis=1)
    cuts = [0] + boundaries(feat) + [len(feat)]
    spans = [(a, b) for a, b in zip(cuts[:-1], cuts[1:]) if b > a]
    means = np.array([energy[a:b].mean() for a, b in spans])
    # rank among the track's own sections (0 quietest, 1 loudest): min-max let a quiet intro squash the rest to "drop"
    order = np.argsort(np.argsort(means))
    ranks = order / max(1, len(spans) - 1)
    sections = []
    for i, (a, b) in enumerate(spans):
        rank = float(ranks[i])
        q = max(1, (b - a) // 4)
        trend = energy[b - q:b].mean() - energy[a:a + q].mean()
        lab = label(i, len(spans), rank, trend)
        move = palette[lab]
        if tid == 'track7' and lab not in ('drop', 'intro', 'outro'):   # robot and human halves take turns
            move = palette['machine'][0] if i % 2 == 0 else palette['human'][i // 2 % len(palette['human'])]
        if sections and move == sections[-1]['move']:   # the same move twice running: take another from the set
            for alt in (palette['verse'], palette['build'], palette['drop'], palette['breakdown']):
                if alt != move:
                    move = alt
                    break
        start = 0 if i == 0 else int(starts[a])
        stop = end_ms if b >= len(starts) else int(starts[b])
        sections.append({'startMs': start, 'endMs': stop, 'label': lab, 'move': move,
                         'intensity': round(0.6 + 0.4 * float(rank), 2)})

    # flourishes: lifted section starts first, then the loudest accents
    candidates = []
    for prev, cur in zip(sections[:-1], sections[1:]):
        lift = means[sections.index(cur)] - means[sections.index(prev)]
        if lift > 0.05:
            candidates.append((2.0 + float(lift), cur['startMs']))
    for ms in track['accents_ms']:
        f = int(ms // BAND_MS)
        if f < len(levels):
            candidates.append((float(levels[f].mean() / 255.0), int(ms)))
    budget = max(1, int(end_ms // FLOURISH_EVERY_MS))
    chosen = []
    for score, ms in sorted(candidates, reverse=True):
        if len(chosen) >= budget:
            break
        if ms < 4000 or ms > end_ms - 4000:
            continue
        if all(abs(ms - c) >= FLOURISH_GAP_MS for c in chosen):
            chosen.append(ms)
    names = palette['flourishes']
    flourishes = [{'ms': ms, 'move': names[i % len(names)]} for i, ms in enumerate(sorted(chosen))]
    return {'track': tid, 'title': track['title'], 'sections': sections, 'flourishes': flourishes}


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument('--force', action='store_true', help='overwrite routines that already exist (hand edits are lost)')
    ap.add_argument('--only', help='one track id, e.g. track3')
    args = ap.parse_args()
    album = json.loads((OUT / 'album.json').read_text(encoding='utf-8'))
    for track in album['tracks']:
        tid = track['id']
        if args.only and tid != args.only:
            continue
        path = OUT / f'{tid}.routine.json'
        if path.exists() and not args.force:
            print(f'{tid}: {path.name} exists, left alone (--force to redo)')
            continue
        levels = np.frombuffer((OUT / f'{tid}.bands').read_bytes(), dtype=np.uint8).reshape(-1, BANDS).astype(float)
        r = routine(track, levels)
        path.write_text(json.dumps(r, indent=1) + '\n', encoding='utf-8')
        summary = ', '.join(f"{s['startMs'] // 1000}s {s['label']}:{s['move']}" for s in r['sections'])
        print(f"{tid} {track['title']}: {len(r['sections'])} sections ({summary}); {len(r['flourishes'])} flourishes")


if __name__ == '__main__':
    main()
