#!/usr/bin/env python3
"""Deterministic pixel assets: block-face tiles, a leaf sign, and native 128x64 citizen UVs."""
import struct
import zlib
from pathlib import Path
ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / 'src/main/resources/assets/fourtwenty'


def png(path, pixels):
    height, width = len(pixels), len(pixels[0])
    def chunk(name, data):
        return struct.pack('>I',len(data))+name+data+struct.pack('>I',zlib.crc32(name+data))
    raw = b''.join(b'\0'+bytes(c for p in row for c in p) for row in pixels)
    path.parent.mkdir(parents=True,exist_ok=True)
    path.write_bytes(b'\x89PNG\r\n\x1a\n'+chunk(b'IHDR',struct.pack('>IIBBBBB',width,height,8,6,0,0,0))+chunk(b'IDAT',zlib.compress(raw,9))+chunk(b'IEND',b''))


def canvas(w,h,color=(0,0,0,0)): return [[color for _ in range(w)] for _ in range(h)]
def rect(p,x,y,w,h,c):
    for yy in range(y,y+h):
        for xx in range(x,x+w): p[yy][xx]=(*c,255) if len(c)==3 else c


def leaf(p,ox=0,oy=0):
    pattern=['.......#.......','.......#.......','...#...#...#...','....#.###.#....','.#..#######..#.','..###########..','...#########...','.....#####.....','.......#.......','.......#.......']
    for y,row in enumerate(pattern):
        for x,v in enumerate(row):
            if v=='#': p[y+oy][x+ox]=(95+12*(y%2),139+10*(x%2),62,255)


def generate():
    side=canvas(16,16)
    for y in range(16):
        for x in range(16):
            if y>=12:
                shade=82+((x//4+y//2)%3)*13
                side[y][x]=(shade,shade+1,shade-2,255)
            else:
                shade=((x*7+y*3)%5)*3
                side[y][x]=(89+shade,61+shade,38+shade,255)
            if y in (0,11) or x in (0,15):side[y][x]=(50,36,26,255)
    front=[r[:] for r in side];rect(front,2,2,12,9,(216,195,146))
    # A compact leaf sign on the hut front; no resolution-dependent lettering.
    for x,y in [(7,3),(7,4),(5,4),(9,4),(6,5),(7,5),(8,5),(4,5),(10,5),(5,6),(6,6),(7,6),(8,6),(9,6),(6,7),(7,7),(8,7),(7,8)]:front[y][x]=(59,104,48,255)
    top=canvas(16,16)
    for y in range(16):
        for x in range(16):top[y][x]=(124+(x%4)*5,88+(x%4)*4,50+(x%4)*3,255) if y not in (0,15) else (62,43,28,255)
    rect(top,3,3,10,10,(207,187,139));rect(top,4,10,8,2,(227,218,192));rect(top,5,5,3,3,(72,110,47))
    for name,p in [('side',side),('front',front),('top',top)]:png(ASSETS/f'textures/block/dispensary_{name}.png',p)
    icon=canvas(16,16);rect(icon,0,0,16,16,(53,39,29));rect(icon,1,1,14,14,(216,195,146));leaf(icon,0,3)
    png(ASSETS/'textures/gui/dispensary.png',icon)
    png(ROOT/'src/main/resources/blueprints/fourtwenty/dispensary/dispensary.png',icon)
    # Native citizen models use 128x64, with vanilla body/head UVs in the upper left.
    skin=canvas(128,64);rect(skin,0,0,32,16,(177,122,86));rect(skin,0,0,32,10,(63,42,29))
    rect(skin,8,10,8,6,(189,137,96));rect(skin,9,11,2,1,(239,230,206));rect(skin,13,11,2,1,(239,230,206));rect(skin,10,11,1,1,(48,60,36));rect(skin,13,11,1,1,(48,60,36));rect(skin,11,14,3,1,(124,73,53))
    rect(skin,16,16,24,16,(206,195,165));rect(skin,0,16,16,16,(68,63,47));rect(skin,0,29,16,3,(45,34,28))
    rect(skin,16,48,16,16,(68,63,47));rect(skin,16,61,16,3,(45,34,28))
    rect(skin,32,48,16,16,(206,195,165));rect(skin,32,59,16,5,(189,137,96))
    rect(skin,40,16,16,16,(206,195,165));rect(skin,40,27,16,5,(189,137,96))
    rect(skin,20,22,8,10,(58,91,51));rect(skin,21,20,1,4,(58,91,51));rect(skin,26,20,1,4,(58,91,51));rect(skin,22,27,4,3,(88,119,65));rect(skin,16,25,24,1,(112,78,44));rect(skin,23,25,2,1,(210,170,80))
    # Female model hair extension. Extra male hat geometry stays transparent.
    rect(skin,46,0,18,9,(63,42,29))
    png(ASSETS/'textures/entity/budtender.png',skin)

if __name__=='__main__': generate()
