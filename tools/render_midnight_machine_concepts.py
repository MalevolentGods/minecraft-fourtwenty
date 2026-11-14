"""Midnight Machine boombox concepts (spec 014 T015), rendered in Blender for the user to pick from.

    "C:\\Program Files\\Blender Foundation\\Blender 5.1\\blender.exe" -b -P tools/render_midnight_machine_concepts.py -- <album dir> <out dir>

Four designs, built only from cuboids (so the chosen one can become a Minecraft JSON model), each worn on the back of a
citizen-sized stand-in and rendered from four angles over a synthwave grid. Units are Minecraft pixels (1/16 block).
The album art (Art.png) goes on each design's front panel. Palette from the album art.
"""
import math
import os
import sys

import bpy

ARGS = sys.argv[sys.argv.index('--') + 1:] if '--' in sys.argv else []
ALBUM = ARGS[0] if ARGS else r'C:\Users\Ignus\OneDrive\Desktop\ATLemental - Midnight Machine'
OUT = ARGS[1] if len(ARGS) > 1 else os.path.join(os.environ.get('TEMP', '.'), 'mm_concepts')
os.makedirs(OUT, exist_ok=True)
PX = 1 / 16

# The album's palette (sampled from Art.png / MM2 / MM3)
MIDNIGHT = '#140119'
PURPLE = '#2a024e'
PLUM = '#5a0251'
MAGENTA = '#c30b59'
PINK = '#f34659'
HOT = '#ed2e65'
ORANGE = '#e16b57'
GOLD = '#f0cb73'
CREAM = '#fbfbe1'
TEAL = '#406b73'
GRAPHITE = '#231121'


def rgb(hexs):
    h = hexs.lstrip('#')
    lin = []
    for i in (0, 2, 4):
        c = int(h[i:i + 2], 16) / 255
        lin.append(c / 12.92 if c <= 0.04045 else ((c + 0.055) / 1.055) ** 2.4)
    return (*lin, 1)


MATS = {}


def mat(name, colour, emit=0.0, rough=0.5, metal=0.0, image=None):
    key = (name, colour, emit, image)
    if key in MATS:
        return MATS[key]
    m = bpy.data.materials.new(name)
    m.use_nodes = True
    bsdf = m.node_tree.nodes['Principled BSDF']
    bsdf.inputs['Base Color'].default_value = rgb(colour)
    bsdf.inputs['Roughness'].default_value = rough
    bsdf.inputs['Metallic'].default_value = metal
    if emit > 0:
        bsdf.inputs['Emission Color'].default_value = rgb(colour)
        bsdf.inputs['Emission Strength'].default_value = emit
    if image:
        tex = m.node_tree.nodes.new('ShaderNodeTexImage')
        tex.image = bpy.data.images.load(image, check_existing=True)
        tex.interpolation = 'Closest'
        m.node_tree.links.new(tex.outputs['Color'], bsdf.inputs['Base Color'])
        m.node_tree.links.new(tex.outputs['Color'], bsdf.inputs['Emission Color'])
        bsdf.inputs['Emission Strength'].default_value = emit or 1.2
    MATS[key] = m
    return m


def box(x0, y0, z0, x1, y1, z1, material, parent=None, name='part'):
    """A cuboid from pixel corners (x right, y up, z toward the viewer behind her)."""
    bpy.ops.mesh.primitive_cube_add(size=1)
    o = bpy.context.active_object
    o.name = name
    o.scale = ((x1 - x0) * PX, (z1 - z0) * PX, (y1 - y0) * PX)
    o.location = ((x0 + x1) / 2 * PX, -(z0 + z1) / 2 * PX, (y0 + y1) / 2 * PX)
    o.data.materials.append(material)
    if parent:
        o.parent = parent
    return o


def art_panel(x0, y0, x1, y1, z, parent, emit=1.4):
    """The album art on a thin plate facing the viewer (on her back, facing out)."""
    bpy.ops.mesh.primitive_plane_add(size=1)
    o = bpy.context.active_object
    o.scale = ((x1 - x0) * PX, (y1 - y0) * PX, 1)
    o.rotation_euler = (math.radians(90), 0, 0)
    o.location = ((x0 + x1) / 2 * PX, -z * PX - 0.001, (y0 + y1) / 2 * PX)
    o.data.materials.append(mat('art', CREAM, emit, image=os.path.join(ALBUM, 'Art.png')))
    o.parent = parent
    return o


def cone(cx, cy, r, z, parent, ring=PINK, inner=GRAPHITE, emit=2.5):
    """A speaker cone: stepped squares (Minecraft can't do circles) with a glowing ring."""
    box(cx - r, cy - r, z, cx + r, cy + r, z + 0.5, mat('ring', ring, emit), parent)
    box(cx - r + 1, cy - r + 1, z + 0.5, cx + r - 1, cy + r - 1, z + 1, mat('cone', inner, 0, rough=0.8), parent)
    box(cx - 1, cy - 1, z + 1, cx + 1, cy + 1, z + 1.5, mat('dust', MAGENTA, 0.6), parent)


def citizen():
    """A citizen-sized stand-in (Minecraft player proportions), facing away from the camera's front view."""
    skin = mat('skin', '#8d5a3b', 0, rough=0.9)
    shirt = mat('shirt', TEAL, 0, rough=0.9)
    pants = mat('pants', '#2b2f45', 0, rough=0.9)
    box(-4, 24, -4, 4, 32, 4, skin, name='head')
    box(-4, 12, -2, 4, 24, 2, shirt, name='body')
    box(-8, 12, -2, -4, 24, 2, shirt, name='arm_r')
    box(4, 12, -2, 8, 24, 2, shirt, name='arm_l')
    box(-4, 0, -2, 0, 12, 2, pants, name='leg_r')
    box(0, 0, -2, 4, 12, 2, pants, name='leg_l')


def straps(parent, top=24, width=2):
    s = mat('strap', GRAPHITE, 0, rough=0.7)
    for x in (-3, 1):
        box(x, top - 1, -2.2, x + width, top, 2.2, s, parent)   # over the shoulder
        box(x, 13, -2.6, x + width, top, -2.2, s, parent)        # down the chest


def design_a():
    """Retro Twin-Cone: the classic, twin cones, handle, cassette door, magenta trim."""
    root = bpy.data.objects.new('A', None); bpy.context.collection.objects.link(root)
    shell, trim = mat('shell', MIDNIGHT, 0, rough=0.35, metal=0.4), mat('trim', HOT, 2.6)
    box(-6, 13, 2, 6, 21, 6, shell, root)
    for (x0, x1) in ((-6, 6),):
        box(x0, 21, 2, x1, 21.5, 6, trim, root); box(x0, 12.5, 2, x1, 13, 6, trim, root)
    box(-6.5, 12.5, 2, -6, 21.5, 6, trim, root); box(6, 12.5, 2, 6.5, 21.5, 6, trim, root)
    cone(-3.5, 17, 2.5, 6, root); cone(3.5, 17, 2.5, 6, root)
    art_panel(-1.5, 14.5, 1.5, 19.5, 6.6, root, 2.0)
    box(-1.5, 13.5, 6, 1.5, 14.5, 6.5, mat('door', GRAPHITE, 0.0), root)
    box(-1.2, 13.7, 6.5, 1.2, 14.3, 6.6, mat('reel', GOLD, 2.6), root)
    for i, x in enumerate((-5, -4, 4, 5)):
        box(x - 0.35, 21.6, 4.5, x + 0.35, 22.1, 5.5, mat(f'btn{i}', (CREAM, HOT, GOLD, PINK)[i], 4.0), root)
    h = mat('handle', GRAPHITE, 0, metal=0.6, rough=0.3)
    box(-5, 21.5, 3.5, -4, 24, 4.5, h, root); box(4, 21.5, 3.5, 5, 24, 4.5, h, root); box(-5, 24, 3.5, 5, 25, 4.5, h, root)
    straps(root)
    return root


def design_b():
    """Synth Machine: a tall rack, glowing VU meter bars, knobs, antenna."""
    root = bpy.data.objects.new('B', None); bpy.context.collection.objects.link(root)
    shell = mat('shell', MIDNIGHT, 0, rough=0.3, metal=0.5)
    box(-5, 12, 2, 5, 25, 6, shell, root)
    art_panel(-3.5, 19, 3.5, 24.5, 6.1, root, 1.6)
    bars = [GOLD, ORANGE, PINK, HOT, MAGENTA, PLUM]
    for i, c in enumerate(bars):
        h = [3, 5, 4, 6, 3, 5][i]
        box(-4.5 + i * 1.5, 13, 6, -3.7 + i * 1.5, 13 + h, 6.5, mat(f'vu{i}', c, 3.0), root)
    for x in (-3, 0, 3):
        box(x - 0.5, 18, 6, x + 0.5, 18.8, 6.8, mat('knob', CREAM, 1.0), root)
    trim = mat('trim', HOT, 2.6)
    box(-5.4, 12, 2, -5, 25, 6, trim, root); box(5, 12, 2, 5.4, 25, 6, trim, root)
    box(3.5, 25, 3.5, 4, 31, 4, mat('antenna', CREAM, 0, metal=0.8), root)
    box(3.25, 31, 3.25, 4.25, 32, 4.25, mat('tip', PINK, 4.0), root)
    straps(root)
    return root


def design_c():
    """Street Bass: chunky, a big stepped bass port, two tweeters, grill bars, sunset stripes."""
    root = bpy.data.objects.new('C', None); bpy.context.collection.objects.link(root)
    shell = mat('shell', MIDNIGHT, 0, rough=0.5, metal=0.2)
    box(-7, 12, 2, 7, 22, 7, shell, root)
    for i, c in enumerate((GOLD, ORANGE, HOT, MAGENTA)):
        box(-7, 21.6 - i * 0.5, 7, 7, 22 - i * 0.5, 7.2, mat(f'stripe{i}', c, 2.6), root)
    # big bass port: stepped rings
    for r, c, e in ((3, PINK, 6.0), (2.2, GRAPHITE, 0), (1.5, MAGENTA, 4.0), (0.7, GRAPHITE, 0)):
        box(-r, 15 - r, 7 + (3 - r) * 0.3, r, 15 + r, 7.3 + (3 - r) * 0.3, mat(f'port{r}', c, e), root)
    for x in (-5.5, 5.5):
        cone(x, 15, 1.2, 7, root, ring=GOLD)
    art_panel(-2.5, 17.6, 2.5, 20.4, 7.35, root, 2.0)
    grill = mat('grill', CREAM, 0, metal=0.9, rough=0.2)
    for y in (12.5, 13.5):
        box(-6.5, y, 7.3, -3, y + 0.3, 7.6, grill, root); box(3, y, 7.3, 6.5, y + 0.3, 7.6, grill, root)
    box(-7, 22, 4, 7, 23, 5, mat('bar', GRAPHITE, 0, metal=0.6), root)
    straps(root)
    return root


def design_d():
    """Phoenix Deck: a slim neon deck, sunset stripes like the album's sun, glowing phoenix wings out the sides."""
    root = bpy.data.objects.new('D', None); bpy.context.collection.objects.link(root)
    shell = mat('shell', MIDNIGHT, 0, rough=0.3, metal=0.4)
    box(-5, 13, 2, 5, 22, 5, shell, root)
    art_panel(-4, 15, 4, 21.5, 5.1, root, 2.0)
    for i, c in enumerate((GOLD, ORANGE, PINK, HOT, MAGENTA)):
        box(-5, 13 + i * 0.4, 5, 5, 13.3 + i * 0.4, 5.3, mat(f'sun{i}', c, 3.0), root)
    # wings: rising feather blades, longest at the top, out both sides
    feathers = [(3, 21, 7), (2.5, 19.5, 6), (2, 18, 5), (1.5, 16.5, 4)]
    for side in (-1, 1):
        for i, (t, y, length) in enumerate(feathers):
            c = (GOLD, ORANGE, HOT, MAGENTA)[i]
            x0 = 5 if side > 0 else -5 - length
            x1 = 5 + length if side > 0 else -5
            lift = i * 0.0
            box(x0, y, 3, x1, y + t * 0.5, 4, mat(f'wing{i}', c, 3.0), root)
            tip = (x1 - 1, x1) if side > 0 else (x0, x0 + 1)
            box(tip[0], y + t * 0.5, 3, tip[1], y + t * 0.5 + 1.5, 4, mat(f'wingtip{i}', c, 3.0), root)
    box(-5.4, 22, 2, 5.4, 22.5, 5, mat('trim', HOT, 2.6), root)
    straps(root)
    return root


def bloom(scene):
    """A neon glow: the compositor's Glare (fog glow) over the render. Blender 5 keeps the tree on the scene."""
    try:
        tree = bpy.data.node_groups.new('mm_bloom', 'CompositorNodeTree')
        scene.compositing_node_group = tree
        rl = tree.nodes.new('CompositorNodeRLayers')
        glare = tree.nodes.new('CompositorNodeGlare')
        out = tree.nodes.new('NodeGroupOutput')
        tree.interface.new_socket('Image', in_out='OUTPUT', socket_type='NodeSocketColor')
        for k, v in (('glare_type', 'FOG_GLOW'), ('quality', 'HIGH'), ('threshold', 1.4), ('size', 6)):
            try:
                setattr(glare, k, v)
            except Exception:
                pass
        for name, value in (('Threshold', 1.4), ('Strength', 0.45), ('Size', 0.45)):
            if name in glare.inputs:
                glare.inputs[name].default_value = value
        tree.links.new(rl.outputs['Image'], glare.inputs['Image'])
        tree.links.new(glare.outputs['Image'], out.inputs[0])
    except Exception as e:   # older API: scene.node_tree
        try:
            scene.use_nodes = True
            nt = scene.node_tree
            rl = nt.nodes.get('Render Layers') or nt.nodes.new('CompositorNodeRLayers')
            comp = nt.nodes.get('Composite') or nt.nodes.new('CompositorNodeComposite')
            glare = nt.nodes.new('CompositorNodeGlare'); glare.glare_type = 'FOG_GLOW'; glare.threshold = 0.8; glare.size = 8
            nt.links.new(rl.outputs['Image'], glare.inputs['Image']); nt.links.new(glare.outputs['Image'], comp.inputs['Image'])
        except Exception as e2:
            print('no bloom:', e, e2)


def stage():
    for o in list(bpy.data.objects):
        bpy.data.objects.remove(o, do_unlink=True)
    scene = bpy.context.scene
    for engine in ('BLENDER_EEVEE', 'BLENDER_EEVEE_NEXT'):
        try:
            scene.render.engine = engine
            break
        except TypeError:
            continue
    scene.render.resolution_x, scene.render.resolution_y = 720, 720
    scene.view_settings.view_transform = 'Standard'
    scene.view_settings.look = 'None'
    bloom(scene)
    world = bpy.data.worlds.new('w'); scene.world = world; world.use_nodes = True
    world.node_tree.nodes['Background'].inputs['Color'].default_value = rgb(MIDNIGHT)
    world.node_tree.nodes['Background'].inputs['Strength'].default_value = 0.6
    # synthwave floor: dark plane and glowing grid lines
    box(-120, -1, -120, 120, 0, 120, mat('floor', MIDNIGHT, 0, rough=0.2, metal=0.3), name='floor')
    grid = mat('grid', MAGENTA, 1.6)
    for i in range(-120, 121, 16):
        box(i - 0.15, 0, -120, i + 0.15, 0.05, 120, grid, name='gx')
        box(-120, 0, i - 0.15, 120, 0.05, i + 0.15, grid, name='gz')
    bpy.ops.object.light_add(type='AREA', location=(2.5, -3, 4)); key = bpy.context.active_object
    key.data.energy = 220; key.data.color = (1, 1, 1); key.rotation_euler = (math.radians(50), 0, math.radians(35))
    bpy.ops.object.light_add(type='AREA', location=(-3, 3, 3)); rim = bpy.context.active_object
    rim.data.energy = 120; rim.data.color = rgb(MAGENTA)[:3]; rim.rotation_euler = (math.radians(-55), 0, math.radians(200))


def shoot(name, angles):
    scene = bpy.context.scene
    bpy.ops.object.camera_add(); cam = bpy.context.active_object; scene.camera = cam
    cam.data.lens = 55
    target = (0, 0, 18 * PX)
    files = []
    for label, az, el, dist in angles:
        x = target[0] + dist * math.cos(math.radians(el)) * math.sin(math.radians(az))
        y = target[1] - dist * math.cos(math.radians(el)) * math.cos(math.radians(az))
        z = target[2] + dist * math.sin(math.radians(el))
        cam.location = (x, y, z)
        d = (target[0] - x, target[1] - y, target[2] - z)
        cam.rotation_euler = (math.atan2(math.hypot(d[0], d[1]), -d[2]), 0, math.atan2(d[0], -d[1]) + math.pi)
        cam.rotation_euler[2] = math.atan2(-d[0], d[1])
        f = os.path.join(OUT, f'{name}_{label}.png')
        scene.render.filepath = f
        bpy.ops.render.render(write_still=True)
        files.append(f)
    return files


ANGLES = [('back', 0, 8, 3.0), ('three_quarter', 35, 15, 3.0), ('side', 70, 6, 3.0), ('hero', 20, -2, 2.2)]
for key, build in (('A_retro_twin_cone', design_a), ('B_synth_machine', design_b), ('C_street_bass', design_c), ('D_phoenix_deck', design_d)):
    stage()
    MATS.clear()
    citizen()
    build()
    shoot(key, ANGLES)
    print('rendered', key)
