"""Render a Minecraft block model JSON with headless Blender, for checking models by eye.

    blender -b --python tools/render_block_model.py -- <model.json> <out_prefix> [--size 512] [--views fl,fr,bl,br,f,top] [--fit 1]

Builds every element (from/to, rotation, per-face texture and UV) in Blender, shades each face
the way Minecraft does (top 1.0, north/south 0.8, east/west 0.6, bottom 0.5, as unlit emission),
honours texture alpha, and renders orthographic views to <out_prefix>_<view>.png. Model texture
references (#name -> namespace:path) resolve under src/main/resources/assets. Parents aren't
followed: render a model that has its own elements.
"""
import json
import math
import os
import sys

import bpy
from mathutils import Matrix, Vector

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
ASSETS = os.path.join(ROOT, 'src', 'main', 'resources', 'assets')
SHADE = {'up': 1.0, 'down': 0.5, 'north': 0.8, 'south': 0.8, 'east': 0.6, 'west': 0.6}
# For each face, seen from outside: the world directions of the texture's right and up.
AXES = {'north': ((-1, 0, 0), (0, 1, 0)), 'south': ((1, 0, 0), (0, 1, 0)), 'east': ((0, 0, -1), (0, 1, 0)),
        'west': ((0, 0, 1), (0, 1, 0)), 'up': ((1, 0, 0), (0, 0, -1)), 'down': ((1, 0, 0), (0, 0, 1))}
NORMAL = {'north': (0, 0, -1), 'south': (0, 0, 1), 'east': (1, 0, 0), 'west': (-1, 0, 0), 'up': (0, 1, 0), 'down': (0, -1, 0)}


def args():
    a = sys.argv[sys.argv.index('--') + 1:]
    opts = {'size': 512, 'views': 'fl,fr,bl,br,f,top'}
    pos = []
    i = 0
    while i < len(a):
        if a[i].startswith('--'):
            opts[a[i][2:]] = a[i + 1]
            i += 2
        else:
            pos.append(a[i])
            i += 1
    return pos[0], pos[1], int(opts['size']), opts['views'].split(','), 'fit' in opts and opts['fit'] != '0'


def texture_path(ref, textures, depth=0):
    while ref.startswith('#') and depth < 8:
        ref = textures[ref[1:]]
        depth += 1
    ns, _, path = ref.partition(':') if ':' in ref else ('minecraft', '', ref)
    return os.path.join(ASSETS, ns, 'textures', path + '.png')


MATERIALS = {}


def material(path, face, emissive):
    key = (path, face, emissive)
    if key in MATERIALS:
        return MATERIALS[key]
    m = bpy.data.materials.new(os.path.basename(path) + '_' + face)
    m.use_nodes = True
    m.blend_method = 'BLEND' if hasattr(m, 'blend_method') else None
    nt = m.node_tree
    for n in list(nt.nodes):
        nt.nodes.remove(n)
    out = nt.nodes.new('ShaderNodeOutputMaterial')
    tex = nt.nodes.new('ShaderNodeTexImage')
    tex.image = bpy.data.images.load(path, check_existing=True)
    tex.interpolation = 'Closest'
    emit = nt.nodes.new('ShaderNodeEmission')
    emit.inputs['Strength'].default_value = 1.0 if emissive else SHADE[face]
    transparent = nt.nodes.new('ShaderNodeBsdfTransparent')
    mix = nt.nodes.new('ShaderNodeMixShader')
    nt.links.new(tex.outputs['Color'], emit.inputs['Color'])
    nt.links.new(tex.outputs['Alpha'], mix.inputs['Fac'])
    nt.links.new(transparent.outputs[0], mix.inputs[1])
    nt.links.new(emit.outputs[0], mix.inputs[2])
    nt.links.new(mix.outputs[0], out.inputs['Surface'])
    MATERIALS[key] = m
    return m


def build(model):
    textures = model.get('textures', {})
    verts, faces, uvs, mats = [], [], [], []
    mat_index = {}
    for e in model['elements']:
        f, t = Vector(e['from']), Vector(e['to'])
        rot = None
        if 'rotation' in e:
            r = e['rotation']
            axis = {'x': 'X', 'y': 'Y', 'z': 'Z'}[r['axis']]
            o = Vector(r['origin'])
            rot = (o, Matrix.Rotation(math.radians(r['angle']), 4, axis))
        emissive = 'neoforge_data' in e
        for face, spec in e['faces'].items():
            right, up = (Vector(v) for v in AXES[face])
            n = Vector(NORMAL[face])
            # The face's four corners (MC coordinates), from the box and the face's normal.
            corners = []
            for su, sv in ((0, 0), (1, 0), (1, 1), (0, 1)):     # bottom-left, bottom-right, top-right, top-left
                p = Vector(((f.x if n.x <= 0 else t.x) if n.x else 0, (f.y if n.y <= 0 else t.y) if n.y else 0,
                            (f.z if n.z <= 0 else t.z) if n.z else 0))
                for axis_i in range(3):
                    if n[axis_i] == 0:
                        lo, hi = f[axis_i], t[axis_i]
                        along_right, along_up = right[axis_i], up[axis_i]
                        if along_right:
                            p[axis_i] = lo if (su == 0) == (along_right > 0) else hi
                        elif along_up:
                            p[axis_i] = lo if (sv == 0) == (along_up > 0) else hi
                corners.append(p)
            u0, v0, u1, v1 = spec.get('uv', [0, 0, 16, 16])
            face_uv = [(u0, v1), (u1, v1), (u1, v0), (u0, v0)]
            k = (spec.get('rotation', 0) // 90) % 4                 # the face's texture, turned clockwise
            face_uv = face_uv[k:] + face_uv[:k]
            base = len(verts)
            for p in corners:
                if rot:
                    p = rot[1] @ (p - rot[0]) + rot[0]
                verts.append((p.x / 16, -p.z / 16, p.y / 16))      # MC (x, y, z) -> Blender (x, -z, y)
            faces.append((base, base + 1, base + 2, base + 3))
            uvs.append([(u / 16, 1 - v / 16) for u, v in face_uv])
            key = (texture_path(spec['texture'], textures), face, emissive)
            if key not in mat_index:
                mat_index[key] = len(mat_index)
            mats.append(mat_index[key])
    mesh = bpy.data.meshes.new('model')
    mesh.from_pydata(verts, [], faces)
    mesh.update()
    layer = mesh.uv_layers.new()
    for poly, coords in zip(mesh.polygons, uvs):
        for li, uv in zip(poly.loop_indices, coords):
            layer.data[li].uv = uv
    obj = bpy.data.objects.new('model', mesh)
    bpy.context.scene.collection.objects.link(obj)
    for (path, face, emissive), i in sorted(mat_index.items(), key=lambda kv: kv[1]):
        obj.data.materials.append(material(path, face, emissive))
    for poly, mi in zip(mesh.polygons, mats):
        poly.material_index = mi
    return obj


VIEWS = {'fl': (-45, 30), 'fr': (45, 30), 'bl': (-135, 30), 'br': (135, 30), 'f': (0, 8), 'top': (-30, 70)}


def render(out_prefix, size, views, frame=None):
    scene = bpy.context.scene
    scene.render.engine = 'CYCLES'
    scene.cycles.samples = 8
    scene.cycles.use_denoising = False
    scene.render.resolution_x = scene.render.resolution_y = size
    scene.render.film_transparent = True
    scene.view_settings.view_transform = 'Standard'
    scene.world = bpy.data.worlds.new('w')
    cam_data = bpy.data.cameras.new('cam')
    cam_data.type = 'ORTHO'
    cam_data.ortho_scale = frame[1] if frame else 1.75
    cam = bpy.data.objects.new('cam', cam_data)
    scene.collection.objects.link(cam)
    scene.camera = cam
    centre = frame[0] if frame else Vector((0.5, -0.5, 0.55))
    for v in views:
        yaw, pitch = VIEWS[v]
        # yaw 0 looks at the front (MC north, Blender +y side... front faces -z in MC = +y in Blender).
        d = Vector((math.sin(math.radians(yaw)) * math.cos(math.radians(pitch)),
                    math.cos(math.radians(yaw)) * math.cos(math.radians(pitch)),
                    math.sin(math.radians(pitch))))
        cam.location = centre + d * (4 + (frame[1] if frame else 0))
        cam.rotation_euler = (-d).to_track_quat('-Z', 'Y').to_euler()
        scene.render.filepath = f'{out_prefix}_{v}.png'
        bpy.ops.render.render(write_still=True)


def main():
    model_path, out_prefix, size, views, fit = args()
    for o in list(bpy.data.objects):
        bpy.data.objects.remove(o)
    with open(model_path, encoding='utf-8') as fh:
        model = json.load(fh)
    if 'children' in model:                                # a NeoForge composite: draw every part
        model = dict(model, elements=[e for c in model['children'].values() for e in c.get('elements', [])])
    build(model)
    frame = None
    if fit:                                                # --fit 1: frame the whole model (a scene of several blocks)
        lo = [min(e['from'][i] for e in model['elements']) / 16 for i in range(3)]
        hi = [max(e['to'][i] for e in model['elements']) / 16 for i in range(3)]
        centre = Vector(((lo[0] + hi[0]) / 2, -(lo[2] + hi[2]) / 2, (lo[1] + hi[1]) / 2))
        frame = (centre, max(h - l for l, h in zip(lo, hi)) * 1.35)
    render(out_prefix, size, views, frame)


main()
