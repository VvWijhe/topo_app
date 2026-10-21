import struct
def read_dbf(path):
    f = open(path, 'rb'); hdr = f.read(32)
    n, hlen, rlen = struct.unpack('<IHH', hdr[4:12])
    fields = []
    while True:
        d = f.read(32)
        if d[0] == 0x0D: break
        name = d[:11].split(b'\0')[0].decode(); typ = chr(d[11]); ln = d[16]
        fields.append((name, typ, ln))
    f.seek(hlen)
    for _ in range(n):
        rec = f.read(rlen)
        if rec[0:1] == b'*': continue
        pos = 1; row = {}
        for name, typ, ln in fields:
            raw = rec[pos:pos+ln]; pos += ln
            s = raw.decode('utf-8', 'replace').strip()
            if typ in 'NF':
                try: row[name] = float(s) if s else None
                except: row[name] = None
            else: row[name] = s
        yield row
