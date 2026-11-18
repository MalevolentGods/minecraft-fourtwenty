# Typed NBT read/write: every value is (tag_type, value); compounds are dicts of name -> (type, value).
import gzip, struct, io
END,BYTE,SHORT,INT,LONG,FLOAT,DOUBLE,BARR,STR,LIST,COMP,IARR,LARR=range(13)
F={BYTE:'>b',SHORT:'>h',INT:'>i',LONG:'>q',FLOAT:'>f',DOUBLE:'>d'}
def _r(s,t):
    u=lambda f:struct.unpack(f,s.read(struct.calcsize(f)))[0]
    if t in F: return u(F[t])
    if t==BARR: return s.read(u('>i'))
    if t==STR: return s.read(u('>H')).decode('utf-8')
    if t==LIST:
        et=u('>b'); n=u('>i'); return (et,[_r(s,et) for _ in range(n)])
    if t==COMP:
        d={}
        while True:
            et=s.read(1)[0]
            if et==0: return d
            k=s.read(u('>H')).decode('utf-8'); d[k]=(et,_r(s,et))
    if t==IARR: n=u('>i'); return list(struct.unpack(f'>{n}i',s.read(4*n)))
    if t==LARR: n=u('>i'); return list(struct.unpack(f'>{n}q',s.read(8*n)))
    raise ValueError(t)
def _w(s,t,v):
    p=lambda f,x:s.write(struct.pack(f,x))
    if t in F: p(F[t],v)
    elif t==BARR: p('>i',len(v)); s.write(v)
    elif t==STR: b=v.encode('utf-8'); p('>H',len(b)); s.write(b)
    elif t==LIST:
        et,items=v; p('>b',et if items else (et or END)); p('>i',len(items))
        for i in items: _w(s,et,i)
    elif t==COMP:
        for k,(et,ev) in v.items():
            s.write(bytes([et])); b=k.encode(); p('>H',len(b)); s.write(b); _w(s,et,ev)
        s.write(b'\x00')
    elif t==IARR: p('>i',len(v)); s.write(struct.pack(f'>{len(v)}i',*v))
    elif t==LARR: p('>i',len(v)); s.write(struct.pack(f'>{len(v)}q',*v))
def load(path):
    b=open(path,'rb').read(); gz=b[:2]==b'\x1f\x8b'
    if gz: b=gzip.decompress(b)
    s=io.BytesIO(b); t=s.read(1)[0]; n=s.read(struct.unpack('>H',s.read(2))[0]).decode()
    return n,_r(s,t),gz
def save(path,name,root,gz=True):
    s=io.BytesIO(); s.write(bytes([COMP])); b=name.encode(); s.write(struct.pack('>H',len(b))); s.write(b); _w(s,COMP,root)
    data=s.getvalue(); open(path,'wb').write(gzip.compress(data) if gz else data)
def show(t,v,ind=0):
    if t==COMP: return '{'+', '.join(f'{k}:{show(et,ev)}' for k,(et,ev) in v.items())+'}'
    if t==LIST: return f'L{v[0]}['+', '.join(show(v[0],i) for i in v[1][:4])+(', …' if len(v[1])>4 else '')+']'
    return f'{v!r}<{t}>'
