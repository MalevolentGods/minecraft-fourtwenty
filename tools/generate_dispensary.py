#!/usr/bin/env python3
"""Author five deterministic timber/stone dispensaries in Structurize v1 NBT, without external libraries."""
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

FOOTPRINTS = [(9, 11), (11, 14), (13, 17), (15, 20), (17, 23)]


def design(level):
    """Build an anchor-relative shop; each expansion preserves the front door and hut."""
    width, depth = FOOTPRINTS[level - 1]
    height = 6 + width // 2
    size = (width, height, depth)
    anchor = (width // 2, 1, 3)
    blocks, tiles, markers, approaches = {}, [], [], []
    def put(x, y, z, name, **props):
        state = {'Name': s(name if ':' in name else 'minecraft:' + name)}
        if props: state['Properties'] = compound({k: s(v) for k, v in props.items()})
        blocks[x, y, z] = state
    for x in range(width):
        for z in range(depth):
            put(x, 0, z, 'stone_bricks' if x in (0, width-1) or z in (0, depth-1) else 'spruce_planks')
    # Timber frame and stone plinth, glazed walls, unobstructed covered entrance.
    for y in range(1, 5):
        for x in range(1, width-1):
            for z in range(1, depth-1):
                if x not in (1, width-2) and z not in (1, depth-2): continue
                if z == 1 and x == anchor[0] and y < 4: continue
                post = (x in (1, width-2) and (z-1) % 4 == 0) or (z in (1, depth-2) and x in (1, width-2))
                name = 'spruce_log' if post else 'stone_bricks' if y == 1 else 'glass' if y in (2,3) else 'spruce_planks'
                put(x,y,z,name, **({'axis':'y'} if post else {}))
    # Solid pitched roof and closed gables; no rain reaches indoor seating.
    for x in range(width):
        roof = 5 + min(x, width-1-x)
        for z in range(depth):
            put(x,roof,z,'spruce_slab',type='bottom',waterlogged='false') if x == width//2 else put(x,roof,z,'spruce_stairs',facing='east' if x < width//2 else 'west',half='bottom',shape='straight',waterlogged='false')
        if 1 <= x < width-1:
            for y in range(5,roof):
                for z in (1,depth-2):
                    put(x,y,z,'spruce_log',axis='y') if x == width//2 else put(x,y,z,'glass' if y in (6,7) and abs(x-width//2)==1 else 'spruce_planks')
    put(*anchor,'fourtwenty:dispensary',facing='north')
    approaches.append((anchor[0],2))
    # Keep the original level-one crafting table and barrel positions.
    put(2,1,7,'crafting_table'); approaches.append((3,7))
    storage = [(2,1,8), (2,1,6)] + [(2,1,z) for z in range(10,depth-2,2)]
    for x,y,z in storage:
        put(x,y,z,'barrel',facing='up',open='false') if z == 8 else put(x,y,z,'minecolonies:blockminecoloniesrack',facing='east',variant='blockrackempty')
        tiles.append({'id':s('minecraft:barrel' if z == 8 else 'minecolonies:tileentityrack'),'x':i(x),'y':i(y),'z':i(z)})
        approaches.append((x+1,z))
    markers += [((3,1,7),'work'), ((anchor[0],1,2),'stand_in')]
    # A window-side seat bank faces the lounge aisle; every seat has a clear approach.
    seats = [(width-3,1,5+(2 if level == 1 else 1)*n) for n in range(level*2)]
    for x,y,z in seats:
        put(x,y,z,'spruce_stairs',facing='east',half='bottom',shape='straight',waterlogged='false')
        markers.append(((x,y,z),'sit_in')); approaches.append((x-1,z))
    # Counter wings leave the central aisle open; preparation room grows with the shop.
    if level >= 2:
        for x in range(2,anchor[0]-1): put(x,1,3,'spruce_planks')
        put(2,1,5,'crafting_table'); approaches.append((3,5))
    if level >= 3:
        # Framed preparation/storage partition; wide openings keep the lounge connected.
        for z in range(5,depth-2):
            if z in (7,8,depth-4,depth-3): continue
            put(4,1,z,'spruce_planks')
            put(4,2,z,'glass')
            put(4,3,z,'spruce_planks')
        for z in range(7,depth-3,4):
            put(anchor[0],1,z,'spruce_fence',north='false',south='false',east='false',west='false',waterlogged='false')
            put(anchor[0],2,z,'spruce_pressure_plate',powered='false')
    for z in range(2,depth-1,4):
        for x in (2,width-3):
            put(x,4,z,'lantern',hanging='false',waterlogged='false')
            put(x,3,z,'spruce_fence',north='false',south='false',east='false',west='false',waterlogged='false')
    validate(size, anchor, blocks, markers, approaches)
    tags = [{'tagPos':position(*(pos[n]-anchor[n] for n in range(3))),
             'tagNameList':listing(10,[{'tagName':s(tag)}])} for pos,tag in markers]
    name = f'dispensary{level}'
    provider = {'schematicName':s(name), 'corner1':position(*(-v for v in anchor)),
                'corner2':position(*(size[n]-1-anchor[n] for n in range(3))),
                'posTagMap':listing(10,tags),'pack':s('FourTwenty'),'path':s(f'dispensary/{name}.blueprint')}
    tiles.insert(0,{'id':s('fourtwenty:dispensary'),'x':i(anchor[0]),'y':i(anchor[1]),'z':i(anchor[2]),
                    'registryName':s('fourtwenty:dispensary'),'blueprintDataProvider':compound(provider)})
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
    root = {'version':(1,1),'size_x':(2,size[0]),'size_y':(2,size[1]),'size_z':(2,size[2]),
            'palette':listing(10,palette),'blocks':(11,packed),'tile_entities':listing(10,tiles),'entities':listing(10,[]),
            'required_mods':listing(8,['fourtwenty','minecolonies','structurize']),'name':s(name),
            'architects':listing(8,['FourTwenty contributors']),'mcversion':i(3955),
            'optional_data':compound({'structurize':compound({'primary_offset':position(*anchor)})})}
    return root, blocks, markers, approaches


def validate(size, anchor, blocks, markers, approaches):
    """Reject blocked two-block-high routes, unroofed seats, and invalid marker positions."""
    reachable = {(anchor[0],0)}
    queue = deque(reachable)
    while queue:
        x,z = queue.popleft()
        for nx,nz in ((x-1,z),(x+1,z),(x,z-1),(x,z+1)):
            if (nx,nz) not in reachable and (nx,0,nz) in blocks and (nx,1,nz) not in blocks and (nx,2,nz) not in blocks:
                reachable.add((nx,nz)); queue.append((nx,nz))
    assert all(pos in reachable for pos in approaches), 'Blocked service approach'
    for (x,y,z), tag in markers:
        if tag == 'sit_in':
            assert (x,y,z) in blocks and (x,y+1,z) not in blocks
            assert any((x,roof,z) in blocks for roof in range(y+2,size[1])), 'Seat exposed to rain'
        else: assert (x,z) in reachable
    assert sum(state['Name'][1]=='fourtwenty:dispensary' for state in blocks.values()) == 1
    assert all(0 <= pos[n] < size[n] for pos in blocks for n in range(3))


def encoded(level):
    root, _, _, _ = design(level)
    return gzip.compress(b'\x0a\x00\x00'+payload(10,root),mtime=0)


def generate():
    for level in range(1,6):
        path = PACK/f'dispensary/dispensary{level}.blueprint'
        path.parent.mkdir(parents=True,exist_ok=True)
        path.write_bytes(encoded(level))
        print(json.dumps({'level':level,'footprint':FOOTPRINTS[level-1],'indoorSeats':level*2}))
    (PACK/'pack.json').write_text(json.dumps({'version':1,'pack-format':1,'name':'FourTwenty',
        'desc':'Five timber-and-stone dispensaries: a village shop growing into a sheltered lounge.',
        'authors':['FourTwenty contributors'],'mods':['fourtwenty','minecolonies','structurize'],'icon':'dispensary.png'},indent=2)+'\n')

if __name__ == '__main__': generate()
