#!/usr/bin/env python3
"""Render README recipe cards from shipped recipes and textures (Python 3.11+).

Install Pillow==12.3.0, then run from any directory:
  python3 tools/generate_recipe_cards.py --minecraft-jar /path/to/1.21.1.jar
Add --check to compare generated PNG bytes with the committed cards without writing.
The client JAR must be the vanilla Minecraft 1.21.1 client, not a server/mod JAR.
Vanilla textures are Mojang assets; FourTwenty textures come from this repository.
Only the composed documentation cards are saved, never the client JAR or raw assets.
"""

import argparse
from collections import Counter
from io import BytesIO
import json
from pathlib import Path
import re
from zipfile import ZipFile

from PIL import Image, ImageDraw, ImageFont

ROOT = Path(__file__).resolve().parents[1]
RESOURCES = ROOT / "src/main/resources"
RECIPES = RESOURCES / "data/fourtwenty/recipe"
OUTPUT = ROOT / "docs/images/recipes"
SCALE = 2
INK = "#293c30"
MUTED = "#596558"


class Assets:
    """Resolve shipped names, item textures, and cube model faces without Minecraft."""

    def __init__(self, jar):
        self.jar = jar
        self.names = json.loads(jar.read("assets/minecraft/lang/en_us.json"))
        self.names.update(json.loads((RESOURCES / "assets/fourtwenty/lang/en_us.json").read_text()))

    def read(self, identifier, folder, suffix):
        namespace, name = identifier.split(":")
        path = f"assets/{namespace}/{folder}/{name}.{suffix}"
        return (RESOURCES / path).read_bytes() if namespace == "fourtwenty" else self.jar.read(path)

    def name(self, identifier):
        namespace, name = identifier.split(":")
        return self.names.get(f"item.{namespace}.{name}") or self.names[f"block.{namespace}.{name}"]

    def texture(self, identifier):
        return Image.open(BytesIO(self.read(identifier, "textures", "png"))).convert("RGBA")

    def icon(self, identifier):
        model = json.loads(self.read(identifier.replace(":", ":item/"), "models", "json"))
        textures = model.get("textures", {})
        if "layer0" in textures:
            return self.texture(textures["layer0"]).resize((64, 64), Image.Resampling.NEAREST)
        # These recipes use cube_all, cube_bottom_top, and orientable block models.
        block = json.loads(self.read(model["parent"], "models", "json"))
        textures = block["textures"]
        if "all" in textures:
            top = front = side = textures["all"]
        else:
            top = textures["top"]
            side = textures["side"]
            front = textures.get("front", side)
        icon = Image.new("RGBA", (64, 64))
        self.face(icon, self.texture(top), (32, 2), (28, 14), (-28, 14), 1.0)
        self.face(icon, self.texture(front), (4, 16), (28, 14), (0, 32), 0.85)
        self.face(icon, self.texture(side), (32, 30), (28, -14), (0, 32), 0.65)
        return icon

    @staticmethod
    def face(canvas, texture, origin, horizontal, vertical, shade):
        """Project texture pixels onto a cube face, retaining crisp texel edges."""
        draw = ImageDraw.Draw(canvas)
        width, height = texture.size

        def point(x, y):
            return (round(origin[0] + horizontal[0] * x / width + vertical[0] * y / height),
                    round(origin[1] + horizontal[1] * x / width + vertical[1] * y / height))

        for y in range(height):
            for x in range(width):
                r, g, b, a = texture.getpixel((x, y))
                if a:
                    draw.polygon([point(x, y), point(x + 1, y), point(x + 1, y + 1), point(x, y + 1)],
                                 fill=(round(r * shade), round(g * shade), round(b * shade), a))


def ingredient_id(ingredient):
    """Keep tag identity for captions while selecting a representative icon."""
    if "item" in ingredient:
        return ingredient["item"]
    if ingredient == {"tag": "minecraft:planks"}:
        return "#minecraft:planks"
    raise ValueError(f"Unsupported ingredient: {ingredient}")


def layout(recipe):
    """Preserve shaped slots, and arrange shapeless ingredients only as an example."""
    if recipe["type"] == "minecraft:crafting_shaped":
        rows = recipe["pattern"]
        if len(rows) > 3 or any(len(row) > 3 for row in rows):
            raise ValueError("Recipe does not fit a crafting grid")
        return [None if symbol == " " else ingredient_id(recipe["key"][symbol])
                for row in rows + [""] * (3 - len(rows)) for symbol in row.ljust(3)]
    if recipe["type"] == "minecraft:crafting_shapeless":
        items = [ingredient_id(item) for item in recipe["ingredients"]]
        if len(items) > 9:
            raise ValueError("Recipe does not fit a crafting grid")
        return items + [None] * (9 - len(items))
    raise ValueError(f"Unsupported recipe type: {recipe['type']}")


def render(recipe, assets):
    """Compose one self-contained card with slots, result, and counted legend."""
    card = Image.new("RGB", (960 * SCALE, 440 * SCALE), "#f4f1e8")
    draw = ImageDraw.Draw(card)

    def box(rect, color):
        draw.rectangle(tuple(v * SCALE for v in rect), fill=color)

    def text(x, y, value, size=22, color=INK):
        font = ImageFont.load_default(size=size * SCALE)
        # Pillow's bundled font lacks multiplication signs and em dashes.
        # Draw those two symbols geometrically instead of depending on system fonts.
        parts = re.split(r"([×—])", value)
        widths = [size * SCALE * (0.7 if part == "×" else 1.0) if part in ("×", "—")
                  else draw.textlength(part, font=font) for part in parts]
        if x * SCALE + sum(widths) > 932 * SCALE:
            raise ValueError(f"Card label too wide: {value}")
        cursor, top, unit = x * SCALE, y * SCALE, size * SCALE
        for part, width in zip(parts, widths):
            if part == "×":
                for start, end in [(0.35, 0.8), (0.8, 0.35)]:
                    draw.line((cursor + unit * 0.12, top + unit * start,
                               cursor + unit * 0.57, top + unit * end), fill=color, width=SCALE * 2)
            elif part == "—":
                draw.line((cursor, top + unit * 0.6, cursor + width, top + unit * 0.6), fill=color, width=SCALE * 2)
            else:
                draw.text((cursor, top), part, font=font, fill=color)
            cursor += width

    def icon(item, x, y, size=64):
        identifier = "minecraft:oak_planks" if item == "#minecraft:planks" else item
        sprite = assets.icon(identifier).resize((size * SCALE, size * SCALE), Image.Resampling.NEAREST)
        card.paste(sprite, (x * SCALE, y * SCALE), sprite)

    def slot(x, y, item):
        box((x, y, x + 74, y + 74), "#fdfcf8")
        box((x, y, x + 72, y + 72), "#65665c")
        box((x + 3, y + 3, x + 72, y + 72), "#aaa99d")
        if item:
            icon(item, x + 6, y + 6)

    result = recipe["result"]
    slots = layout(recipe)
    counts = Counter(item for item in slots if item)
    if len(counts) > 5:
        raise ValueError("Card legend supports up to five distinct ingredients")
    conditions = recipe.get("neoforge:conditions", [])
    if conditions and conditions != [{"type": "neoforge:mod_loaded", "modid": "minecolonies"}]:
        raise ValueError(f"Unsupported recipe conditions: {conditions}")
    shapeless = recipe["type"] == "minecraft:crafting_shapeless"
    box((0, 0, 960, 7), "#537447")
    text(30, 22, "FOURTWENTY  /  CRAFTING", 16, MUTED)
    text(30, 50, assets.name(result["id"]), 36)
    if conditions:
        text(640, 60, "Requires MineColonies", 23, "#526c3f")
    text(30, 101, "Shapeless — ingredients can go anywhere" if shapeless else "Shaped recipe", 22, MUTED)
    for i, item in enumerate(slots):
        slot(30 + (i % 3) * 78, 145 + (i // 3) * 78, item)
    draw.polygon([(x * SCALE, y * SCALE) for x, y in
                  [(282, 249), (307, 249), (307, 237), (329, 260), (307, 283), (307, 271), (282, 271)]],
                 fill="#6f7d60")
    text(348, 182, "OUTPUT", 16, MUTED)
    slot(346, 223, result["id"])
    text(355, 312, f"× {result.get('count', 1)}", 25)
    box((450, 145, 451, 375), "#d3d5c8")
    text(478, 145, "INGREDIENTS", 16, MUTED)
    for i, (item, count) in enumerate(counts.items()):
        y = 179 + i * 40
        icon(item, 478, y, 32)
        name = "Any planks" if item == "#minecraft:planks" else assets.name(item)
        text(525, y + 3, f"{count} × {name}", 22)
    note = "Any planks accepted; oak shown." if "#minecraft:planks" in counts else "Each occupied slot uses one ingredient."
    text(30, 400, note, 18, MUTED)
    output = BytesIO()
    card.save(output, format="PNG", optimize=True)
    return output.getvalue()


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--minecraft-jar", required=True, type=Path)
    parser.add_argument("--check", action="store_true", help="Check committed cards without writing")
    args = parser.parse_args()
    with ZipFile(args.minecraft_jar) as jar:
        if json.loads(jar.read("version.json"))["id"] != "1.21.1":
            parser.error("Expected the vanilla Minecraft 1.21.1 client JAR")
        assets = Assets(jar)
        cards = {path.stem + ".png": render(json.loads(path.read_text()), assets)
                 for path in sorted(RECIPES.glob("*.json"))}
    if args.check:
        stale = [name for name, data in cards.items() if not (OUTPUT / name).exists() or (OUTPUT / name).read_bytes() != data]
        extra = sorted(path.name for path in OUTPUT.glob("*.png") if path.name not in cards)
        if stale or extra:
            parser.exit(1, f"Outdated/missing cards: {stale}; unexpected cards: {extra}\n")
        print(f"Verified {len(cards)} recipe cards.")
    else:
        OUTPUT.mkdir(parents=True, exist_ok=True)
        for name, data in cards.items():
            (OUTPUT / name).write_bytes(data)
        print(f"Generated {len(cards)} recipe cards in {OUTPUT}")


if __name__ == "__main__":
    main()
