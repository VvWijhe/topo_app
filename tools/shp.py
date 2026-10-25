import struct
def read_polygons(path):
    """Minimal .shp reader for polygon shapes (type 5). Returns list of [rings], rings = [(x,y)...]."""
    f = open(path, 'rb'); f.seek(100); out = []
    while True:
        h = f.read(8)
        if len(h) < 8: break
        _, clen = struct.unpack('>ii', h)
        rec = f.read(clen * 2)
        st = struct.unpack('<i', rec[:4])[0]
        if st == 0: out.append([]); continue
        nparts, npts = struct.unpack('<ii', rec[36:44])
        parts = list(struct.unpack('<%di' % nparts, rec[44:44 + 4 * nparts]))
        off = 44 + 4 * nparts
        pts = [struct.unpack('<dd', rec[off + 16 * i: off + 16 * i + 16]) for i in range(npts)]
        parts.append(npts)
        out.append([pts[parts[i]:parts[i + 1]] for i in range(nparts)])
    return out
