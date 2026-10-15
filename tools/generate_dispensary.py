#!/usr/bin/env python3
"""Author the Phase 2 timber/stone prototype in Structurize v1 NBT, without external libraries."""
import gzip
import json
import struct
from collections import deque
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
RES = ROOT / 'src/main/resources'
PACK = RES / 'blueprints/fourtwenty/dispensary'
def s(value): return (8, value)
def i(value): return (3, value)
def compound(value): return (10, value)
def listing(kind, value): return (9, (kind, value))
def position(x, y, z): return compound(dict(x=i(x), y=i(y), z=i(z)))
def string(value):
    data = value.encode('utf-8')
    return struct.pack('>H', len(data)) + data

def payload(kind, value):
    if kind in (1, 2, 3): return struct.pack({1: '>b', 2: '>h', 3: '>i'}[kind], value)
    if kind == 8: return string(value)
    if kind == 9:
        subtype, values = value
        return bytes([subtype]) + struct.pack('>i', len(values)) + b''.join(payload(subtype, v) for v in values)
    if kind == 10:
        return b''.join(bytes([v[0]]) + string(k) + payload(*v) for k, v in value.items()) + b'\0'
    if kind == 11:
        return struct.pack('>i', len(value)) + b''.join(struct.pack('>i', v if v < 2**31 else v - 2**32) for v in value)
    raise ValueError(kind)

def generate():
    size = (9, 7, 11)
    anchor = (4, 1, 3)
    blocks = {}
    def put(x, y, z, name, **props):
        state = {'Name': s(name if ':' in name else 'minecraft:' + name)}
        if props: state['Properties'] = compound({k: s(v) for k, v in props.items()})
        blocks[x, y, z] = state
    for x in range(9):
        for z in range(11): put(x, 0, z, 'cobblestone')
    for y in range(1, 4):
        for x in range(1, 8):
            for z in range(1, 10):
                if x not in (1, 7) and z not in (1, 9): continue
                if z == 1 and x == 4 and y < 3: continue
                wood = 'spruce_log' if x in (1, 7) and z in (1, 9) else 'spruce_planks'
                if y == 2 and x in (1, 7) and z in (3, 4, 6, 7): wood = 'glass'
                put(x, y, z, wood)
    for x in range(9):
        for z in range(11): put(x, 4 + min(x, 8-x)//2, z, 'spruce_planks')
    put(*anchor, 'fourtwenty:dispensary', facing='north')
    put(2, 1, 7, 'crafting_table')
    put(2, 1, 8, 'barrel', facing='up', open='false')
    for z in (5, 7): put(6, 1, z, 'spruce_stairs', facing='east', half='bottom', shape='straight', waterlogged='false')
    for x in (2, 6): put(x, 3, 2, 'lantern', hanging='true', waterlogged='false')
    # Prove that the entrance reaches hut, preparation, storage and both seat approaches.
    reachable = {(4, 0)}
    queue = deque(reachable)
    while queue:
        x, z = queue.popleft()
        for nx, nz in ((x-1,z),(x+1,z),(x,z-1),(x,z+1)):
            if (nx,nz) not in reachable and (nx,0,nz) in blocks and (nx,1,nz) not in blocks and (nx,2,nz) not in blocks:
                reachable.add((nx,nz)); queue.append((nx,nz))
    approaches = [(4,2), (3,7), (3,8), (5,5), (5,7)]
    assert all(pos in reachable for pos in approaches)
    tags = []
    for pos, tag in [((3,1,7),'work'), ((3,1,6),'stand_in'), ((6,1,5),'sit_in'), ((6,1,7),'sit_in')]:
        tags.append({'tagPos': position(*(pos[n]-anchor[n] for n in range(3))), 'tagNameList': listing(10,[{'tagName':s(tag)}])})
    provider = {'schematicName':s('dispensary1'), 'corner1':position(-4,-1,-3), 'corner2':position(4,5,7),
                'posTagMap':listing(10,tags), 'pack':s('FourTwenty'), 'path':s('dispensary/dispensary1.blueprint')}
    tiles = [{'id':s('fourtwenty:dispensary'), 'x':i(4),'y':i(1),'z':i(3), 'registryName':s('fourtwenty:dispensary'),
              'blueprintDataProvider':compound(provider)},
             {'id':s('minecraft:barrel'),'x':i(2),'y':i(1),'z':i(8)}]
    palette = [{'Name':s('minecraft:air')}]
    indices = {json.dumps(palette[0],sort_keys=True):0}
    cells = []
    for y in range(size[1]):
        for z in range(size[2]):
            for x in range(size[0]):
                state = blocks.get((x,y,z),palette[0]); key=json.dumps(state,sort_keys=True)
                if key not in indices: indices[key]=len(palette); palette.append(state)
                cells.append(indices[key])
    packed = [(cells[n]<<16) | (cells[n+1] if n+1<len(cells) else 0) for n in range(0,len(cells),2)]
    root = {'version':(1,1), 'size_x':(2,size[0]),'size_y':(2,size[1]),'size_z':(2,size[2]),
            'palette':listing(10,palette),'blocks':(11,packed),'tile_entities':listing(10,tiles),'entities':listing(10,[]),
            'required_mods':listing(8,['fourtwenty','minecolonies','structurize']), 'name':s('dispensary1'),
            'architects':listing(8,['FourTwenty contributors']), 'mcversion':i(3955),
            'optional_data':compound({'structurize':compound({'primary_offset':position(*anchor)})})}
    path = PACK/'dispensary/dispensary1.blueprint'
    path.parent.mkdir(parents=True,exist_ok=True)
    path.write_bytes(gzip.compress(b'\x0a\x00\x00'+payload(10,root),mtime=0))
    (PACK/'pack.json').write_text(json.dumps({'version':1,'pack-format':1,'name':'FourTwenty',
        'desc':'A rustic dispensary and preparation room. Phase 2 level-one prototype.',
        'authors':['FourTwenty contributors'],'mods':['fourtwenty','minecolonies','structurize'],'icon':'dispensary.png'},indent=2)+'\n')
    (PACK/'dispensary.png').write_bytes((RES/'assets/fourtwenty/textures/item/weed_bud.png').read_bytes())
    print(json.dumps({'size':size,'anchor':anchor,'indoorSeats':2,'reachableApproaches':approaches}))

if __name__ == '__main__': generate()
