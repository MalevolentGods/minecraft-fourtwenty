# Contract: Book markup

Read by `domain/handbook/Markup` (Java) and `tools/handbook_markup.py` (Python). Both run `src/test/resources/handbook/markup-samples/*.md` and must produce the same element list (`*.expected.json`).

## File

`assets/fourtwenty/handbook/<lang>/entries/<chapter>/<entry-id>.md`

```
---
title: Your First Grow
icon: fourtwenty:weed_seeds
terms: [starter seeds, farmland, light]
needs: minecolonies          # optional; hide when the mod is absent
---
# Heading            (one level only below the entry title: "#")
Text with **bold**, *italic* and {c:lime}coloured{/c} words.
A [link to Trimming](entry:processing/trim_tray).

- a list item
1. a numbered step (Steps keep their own numbering across page breaks)

{item:fourtwenty:weed_seeds|Your four starter seeds}
{recipe:fourtwenty:trim_tray}
{image:scenes/first_grow|A first grow by a window|wide}
{table}
| Tier | Power |
| Puff | 16 Kb |
{/table}
{truth:AND}
{spoiler:terpene:myr_lim_lav}text shown only once discovered{/spoiler}
{page}
```

## Rules

- Unknown tags, unclosed spans, and references that don't resolve (entry, item, recipe, image, terpene code) are **errors** at build time (`check_handbook.py` and a Gradle task) and are skipped with a log line at run time.
- Colours: the mod's palette names only (`lime`, `myr`, `lav`, `pin`, `cream`, `gold`, `red`, `gray`).
- `{truth:GATE}`: NOT, AND, OR, NAND, NOR, XOR, XNOR, generated (not hand-written).
- `{table:parts}` renders the translation table from `real_terms.json`; `{table:units}` the units table.
- Lines wrap to the page; elements never split except paragraphs, lists and steps.

## Special files

- `book.json`: chapters (id, title key, icon, needs, entries in order); cover image; inside-cover key.
- `glossary.md`: `term: one-line meaning -> entry:id` per line.
- `front/oath.md`, `front/law.md`, `front/motto.md`, `front/slogan.md`: the scouting front matter.
