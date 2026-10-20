import json, math, sys
TOL = float(sys.argv[1]) if len(sys.argv) > 1 else 0.1

# ---------- Natural Earth I-projectie ----------
def ne_raw(lam, phi):
    p2 = phi * phi; p4 = p2 * p2
    x = lam * (0.8707 - 0.131979 * p2 + p4 * (-0.013791 + p4 * (0.003971 * p2 - 0.001529 * p4)))
    y = phi * (1.007226 + p2 * (0.015085 + p4 * (-0.044475 + 0.028874 * p2 - 0.005916 * p4)))
    return x, y
K = 1000 / (math.pi * 0.8707)
YMAX = ne_raw(0, math.pi / 2)[1] * K
OX, OY = 1000.0, YMAX
W, H = 2000.0, 2 * YMAX
def pr(lon, lat):
    x, y = ne_raw(math.radians(lon), math.radians(lat))
    return (OX + x * K, OY - y * K)

# ---------- hulpfuncties ----------
def area(r): return abs(sum(r[i-1][0]*r[i][1]-r[i][0]*r[i-1][1] for i in range(len(r))))/2
def dp(pts, tol):
    if len(pts) < 3: return pts
    keep = [False]*len(pts); keep[0] = keep[-1] = True; st = [(0, len(pts)-1)]
    while st:
        a, b = st.pop(); ax, ay = pts[a]; bx, by = pts[b]; dx, dy = bx-ax, by-ay; L = math.hypot(dx, dy) or 1e-9
        md = 0; mi = -1
        for i in range(a+1, b):
            dd = abs(dy*(pts[i][0]-ax)-dx*(pts[i][1]-ay))/L
            if dd > md: md = dd; mi = i
        if md > tol: keep[mi] = True; st += [(a, mi), (mi, b)]
    return [p for p, k in zip(pts, keep) if k]
def simp(r, tol):
    h = len(r)//2
    out = dp(r[:h+1], tol)[:-1] + dp(r[h:]+[r[0]], tol)[:-1]
    return out if len(out) >= 3 else r
def ringstr(r): return 'M' + 'L'.join(f'{x:.2f},{y:.2f}'.rstrip('0').rstrip('.') if False else f'{x:.1f},{y:.1f}' for x, y in r) + 'Z'
def lonc(ring): return sum(c[0] for c in ring)/len(ring)

# ---------- bronnen ----------
ne = json.load(open('data/ne_admin_0_countries.geojson'))['features']
mc = {c['cca3']: c for c in json.load(open('data/countries.json'))}
places = [f['properties'] for f in json.load(open('data/ne_places.geojson'))['features'] if f['properties'].get('adm0cap') == 1]

MERGE = {'ALD': 'FIN', 'CYN': 'CYP', 'SOL': 'SOM', 'KAS': 'IND'}
EXTRA = {'USA', 'GBR', 'NZL', 'NLD', 'FRA', 'FIN', 'DNK', 'CHN', 'AUS', 'KOS', 'ISR', 'PSX', 'SAH', 'GRL'}
MC_CODE = {'KOS': 'UNK', 'PSX': 'PSE', 'SAH': 'ESH', 'SDS': 'SSD'}
OUT_CODE = {'PSX': 'PSE', 'SAH': 'ESH', 'SDS': 'SSD'}  # app-codes = ISO, behalve Kosovo (KOS)
EUROPE = set('ISL NOR SWE FIN DNK EST LVA LTU IRL GBR NLD BEL LUX DEU POL CZE SVK AUT CHE LIE FRA MCO AND ESP PRT ITA SMR VAT MLT SVN HRV BIH SRB MNE KOS ALB MKD GRC BGR ROU MDA UKR BLR RUS HUN CYP TUR GEO ARM AZE'.split())
CONT = {'Europe': 'EU', 'Asia': 'AS', 'Africa': 'AF', 'North America': 'NA', 'South America': 'SA', 'Oceania': 'OC'}
SEVEN = {'MDV': 'AS', 'MUS': 'AF', 'SYC': 'AF'}

# Nederlandse namen van hoofdsteden die afwijken van het Engels (mledoze geeft Engels)
CAP_NL = {
 'Copenhagen':'Kopenhagen','Brussels':'Brussel','Berlin':'Berlijn','Warsaw':'Warschau','Prague':'Praag','Vienna':'Wenen','Paris':'Parijs',
 'Lisbon':'Lissabon','Rome':'Rome','Athens':'Athene','Bucharest':'Boekarest','Budapest':'Boedapest','Belgrade':'Belgrado','Moscow':'Moskou',
 'London':'Londen','Kyiv':'Kyiv','Chișinău':'Chisinau','Yerevan':'Jerevan','Baku':'Bakoe','Luxembourg':'Luxemburg','City of San Marino':'San Marino',
 'Vatican City':'Vaticaanstad','Beijing':'Peking','Mexico City':'Mexico-Stad','Cairo':'Caïro','Algiers':'Algiers','Tripoli':'Tripoli',
 'Kuwait City':'Koeweit-Stad','Guatemala City':'Guatemala-Stad','Panama City':'Panama-Stad','Singapore':'Singapore','Tehran':'Teheran',
 'Baghdad':'Bagdad','Damascus':'Damascus','Jerusalem':'Jeruzalem','Kathmandu':'Kathmandu','New Delhi':'New Delhi','Tokyo':'Tokio',
 'Seoul':'Seoel','Pyongyang':'Pyongyang','Hanoi':'Hanoi','Bangkok':'Bangkok','Riyadh':'Riyad','Sana\'a':'Sanaa','Muscat':'Masqat',
 'Ulan Bator':'Ulaanbaatar','Taipei':'Taipei','Havana':'Havana','Bern':'Bern','Reykjavik':'Reykjavik','Tbilisi':'Tbilisi',
 'Mogadishu':'Mogadishu','Addis Ababa':'Addis Abeba','Ottawa':'Ottawa','Washington D.C.':'Washington','Brasília':'Brasília',
 'Bogotá':'Bogota','Tunis':'Tunis','Nicosia':'Nicosia','Vaduz':'Vaduz','Kingston':'Kingston','Port of Spain':'Port of Spain',
 'Ramallah':'Ramallah','Amsterdam':'Amsterdam','Andorra la Vella':'Andorra la Vella','Monaco':'Monaco','Valletta':'Valletta',
 'El Aaiún':'El Aaiún','Nuuk':'Nuuk','Pristina':'Pristina','Abu Dhabi':'Abu Dhabi','Dhaka':'Dhaka','Kabul':'Kabul','Islamabad':'Islamabad',
}
CAP_FIX = {'ISR': 'Jeruzalem', 'PSE': 'Ramallah', 'BOL': 'Sucre', 'ZAF': 'Pretoria', 'NLD': 'Amsterdam', 'ESH': 'El Aaiún', 'KOS': 'Pristina'}
NAME_FIX = {'KOS': 'Kosovo', 'ESH': 'Westelijke Sahara', 'PSE': 'Palestina', 'GRL': 'Groenland', 'TWN': 'Taiwan', 'BLR': 'Belarus',
            'CZE': 'Tsjechië', 'MKD': 'Noord-Macedonië', 'SWZ': 'Eswatini', 'CIV': 'Ivoorkust', 'COD': 'Congo-Kinshasa', 'COG': 'Congo-Brazzaville',
            'MMR': 'Myanmar', 'TLS': 'Oost-Timor', 'CPV': 'Kaapverdië', 'KOR': 'Zuid-Korea', 'PRK': 'Noord-Korea', 'USA': 'Verenigde Staten',
            'GBR': 'Verenigd Koninkrijk', 'ARE': 'Verenigde Arabische Emiraten', 'BIH': 'Bosnië en Herzegovina', 'TUR': 'Turkije'}

NAME_EN = {'KOS': 'Kosovo', 'ESH': 'Western Sahara', 'PSE': 'Palestine', 'COD': 'DR Congo', 'COG': 'Republic of the Congo',
           'MKD': 'North Macedonia', 'CIV': 'Ivory Coast', 'TLS': 'East Timor', 'CZE': 'Czechia', 'SWZ': 'Eswatini', 'VAT': 'Vatican City',
           'FSM': 'Micronesia', 'BHS': 'Bahamas', 'GMB': 'Gambia', 'TUR': 'Turkey'}
CAP_EN_FIX = {'ISR': 'Jerusalem', 'PSE': 'Ramallah', 'BOL': 'Sucre', 'ZAF': 'Pretoria', 'NLD': 'Amsterdam', 'ESH': 'El Aaiún',
              'KOS': 'Pristina', 'SMR': 'San Marino', 'USA': 'Washington, D.C.'}
MANUAL_CLL = {'SSD': [4.85, 31.58], 'NRU': [-0.547, 166.921], 'ESH': [27.153, -13.203], 'PSE': [31.903, 35.204], 'GRL': [64.181, -51.721]}
# ---------- landen verzamelen ----------
groups = {}
bg = []
for f in ne:
    p = f['properties']; a3 = p['ADM0_A3']
    g = f['geometry']; polys = g['coordinates'] if g['type'] == 'MultiPolygon' else [g['coordinates']]
    tgt = MERGE.get(a3, a3)
    keep = p['TYPE'] in ('Sovereign country', 'Sovereignty') or a3 in EXTRA or a3 in MERGE
    if tgt == 'ATA' or not keep:
        bg.append((a3, polys)); continue
    e = groups.setdefault(tgt, {'polys': [], 'props': None})
    e['polys'] += polys
    if a3 == tgt: e['props'] = p

countries = {}
for a3, e in groups.items():
    p = e['props']; code = OUT_CODE.get(a3, a3); m = mc.get(MC_CODE.get(a3, a3))
    polys = e['polys']
    # antimeridiaan: houd de delen aan de kant van het hoofdgebied
    main = max(polys, key=lambda pl: area(pl[0]))
    mlon = 170 if a3 == 'KIR' else lonc(main[0])
    def shift(ring):
        c = lonc(ring)
        if mlon > 0 and c < -150: return [[x+360, y] for x, y in ring]
        if mlon < 0 and c > 150: return [[x-360, y] for x, y in ring]
        return ring
    out = []; parts = []
    for pl in polys:
        rings = [[pr(x, y) for x, y in shift(r)] for r in pl]
        A = area(rings[0])
        parts.append((A, rings))
    Amax = max(a for a, _ in parts)
    for A, rings in parts:
        if A < 0.02 and A < Amax * 0.5: continue   # piepkleine eilandjes weglaten
        for r in rings:
            s = simp(r, TOL)
            if len(s) >= 3: out.append(s)
    # kader van het 'hoofdgebied': grote delen + delen dichtbij het grootste deel
    mr = max(parts, key=lambda t: t[0])[1][0]
    mx0, mx1 = min(x for x, _ in mr), max(x for x, _ in mr); my0, my1 = min(y for _, y in mr), max(y for _, y in mr)
    span = max(mx1-mx0, my1-my0)
    bx0, by0, bx1, by1 = mx0, my0, mx1, my1
    for A, rings in parts:
        r = rings[0]; x0 = min(x for x, _ in r); x1 = max(x for x, _ in r); y0 = min(y for _, y in r); y1 = max(y for _, y in r)
        cx, cy = (x0+x1)/2, (y0+y1)/2
        near = mx0 - span*0.6 < cx < mx1 + span*0.6 and my0 - span*0.6 < cy < my1 + span*0.6
        if (A >= Amax*0.15 and code not in ('FRA','NLD')) or near:
            bx0, by0, bx1, by1 = min(bx0, x0), min(by0, y0), max(bx1, x1), max(by1, y1)
    name = NAME_FIX.get(code) or (m and m['translations'].get('nld', {}).get('common')) or p['NAME']
    cap_en = (m['capital'][0] if m and m.get('capital') else None)
    cap = CAP_FIX.get(code) or (CAP_NL.get(cap_en, cap_en) if cap_en else None)
    # hoofdstad-coördinaten uit Natural Earth populated places
    cands = [q for q in places if q['adm0_a3'] in (a3, p['SOV_A3'] if p else a3)]
    cll = None
    if cands:
        want = (cap_en or '').lower()
        best = next((q for q in cands if q['name'].lower() == want or (q.get('namealt') or '').lower() == want or q['nameascii'].lower() == want), None)
        best = best or next((q for q in cands if q['adm0_a3'] == a3), cands[0])
        cll = [round(best['latitude'], 3), round(best['longitude'], 3)]
    elif code in MANUAL_CLL:
        cll = MANUAL_CLL[code]
    elif m and m.get('capitalInfo', {}).get('latlng'):
        cll = [round(v, 3) for v in m['capitalInfo']['latlng']]
    cont = set()
    if p['CONTINENT'] in CONT: cont.add(CONT[p['CONTINENT']])
    if a3 in SEVEN: cont.add(SEVEN[a3])
    if code in EUROPE: cont.add('EU')
    if code in ('TUR', 'CYP', 'GEO', 'ARM', 'AZE', 'RUS'): cont.add('AS'); 
    if code in ('TUR', 'CYP', 'GEO', 'ARM', 'AZE'): pass
    if code == 'RUS': cont |= {'EU', 'AS'}
    nb = []
    if m:
        for b in m.get('borders', []):
            b = {'UNK': 'KOS'}.get(b, b)
            nb.append(b)
    lbl = pr(p['LABEL_X'], p['LABEL_Y'])
    name_en = NAME_EN.get(code) or (m and m['name']['common']) or p['NAME']
    cap_en_out = CAP_EN_FIX.get(code) or cap_en
    countries[code] = {
        'n': name, 'ne': name_en, 'ce': cap_en_out, 'a2': (m['cca2'] if m else p['ISO_A2_EH']).upper().replace('XK', 'XK'),
        'cap': cap, 'cll': cll, 'pop': int(p['POP_EST']), 'area': (m['area'] if m else None),
        'cont': sorted(cont), 'nb': nb, 'lbl': [round(lbl[0], 1), round(lbl[1], 1)],
        'b': [round(bx0, 1), round(by0, 1), round(bx1, 1), round(by1, 1)],
        'd': ''.join(ringstr(r) for r in out),
        'tld': (m['tld'][0] if m and m.get('tld') else None),
        'sov': p['TYPE'] in ('Sovereign country', 'Sovereignty') or code in ('USA','GBR','NZL','NLD','FRA','FIN','DNK','CHN','AUS','ISR'),
    }
for c in countries.values():
    c['nb'] = [b for b in c['nb'] if b in countries]

bgd = []
for a3, polys in bg:
    for pl in polys:
        rings = [[pr(x, y) for x, y in r] for r in pl]
        if area(rings[0]) < 0.3: continue
        bgd.append(''.join(ringstr(simp(r, TOL*2)) for r in rings))

lakes = []
for f in json.load(open('data/ne_lakes.geojson'))['features']:
    g = f['geometry']; polys = g['coordinates'] if g['type'] == 'MultiPolygon' else [g['coordinates']]
    for pl in polys:
        r = [pr(x, y) for x, y in pl[0]]
        if area(r) >= 2: lakes.append(ringstr(simp(r, TOL)))

# ---------- Europese rivieren (als lijnen) ----------
RIV = {'Rijn':['Rhein','Rhin','Rhine','Waal'],'Donau':['Danube','Donau','Bratul Sulina'],'Elbe':['Elbe'],'Oder':['Oder'],'Wisła':['Vistula'],
       'Dnjepr':['Dnipro','Dnepre'],'Dnjestr':['Dniester'],'Don':['Don'],'Wolga':['Volga'],'Loire':['Loire'],'Seine':['Seine'],'Rhône':['Rhône'],
       'Garonne':['Garonne'],'Po':['Po'],'Taag':['Tajo','Tejo'],'Ebro':['Ebro'],'Douro':['Duero','Douro'],'Theems':['Thames'],'Daugava':['Daugava'],
       'Tisza':['Tisza','Tisa']}
inv = {n: k for k, v in RIV.items() for n in v}
rivers = {}
for f in json.load(open('data/ne_rivers_lake_centerlines.geojson'))['features']:
    n = f['properties'].get('name')
    if n not in inv: continue
    g = f['geometry']; ls = g['coordinates'] if g['type'] == 'MultiLineString' else [g['coordinates']]
    for l in ls:
        if not any(-12 < x < 60 and 34 < y < 70 for x, y in l): continue
        if n == 'Don' and min(x for x, _ in l) > 45: continue
        r = dp([pr(x, y) for x, y in l], TOL)
        rivers[inv[n]] = rivers.get(inv[n], '') + 'M' + 'L'.join(f'{x:.1f},{y:.1f}' for x, y in r)

def bbox_ll(lon0, lon1, lat0, lat1):
    pts = [pr(lo, la) for lo in [lon0 + (lon1-lon0)*i/20 for i in range(21)] for la in [lat0 + (lat1-lat0)*j/20 for j in range(21)]]
    return [round(min(x for x, _ in pts), 1), round(min(y for _, y in pts), 1), round(max(x for x, _ in pts), 1), round(max(y for _, y in pts), 1)]
CONTS = {
    'EU': ['Europa', bbox_ll(-25, 45, 34, 71.5), 'Europe'],
    'AS': ['Azië', bbox_ll(25, 150, -11, 78), 'Asia'],
    'AF': ['Afrika', bbox_ll(-26, 58, -36, 38), 'Africa'],
    'NA': ['Noord-Amerika', bbox_ll(-170, -12, 7, 84), 'North America'],
    'SA': ['Zuid-Amerika', bbox_ll(-92, -32, -56, 13), 'South America'],
    'OC': ['Oceanië', bbox_ll(110, 200, -48, 3), 'Oceania'],
}
CTR = {'EU': (50, 15), 'AS': (45, 90), 'AF': (5, 20), 'NA': (48, -100), 'SA': (-15, -60), 'OC': (-25, 140)}
out = {
    'vb': [W, round(H, 1)], 'proj': {'k': K, 'ox': OX, 'oy': OY},
    'c': countries, 'bg': bgd, 'lakes': lakes, 'riv': rivers,
    'cont': {k: {'n': v[0], 'ne': v[2], 'v': v[1], 'c': [round(x, 1) for x in pr(CTR[k][1], CTR[k][0])]} for k, v in CONTS.items()},
}
s = json.dumps(out, ensure_ascii=False, separators=(',', ':'))
open('world.json', 'w').write(s)
print('countries', len(countries), 'bytes', len(s), 'points', s.count('L') + s.count('M'))
from collections import Counter
print(Counter(c for v in countries.values() for c in v['cont']))
print([k for k, v in countries.items() if not v['cont']], [k for k, v in countries.items() if not v['cll']], [k for k, v in countries.items() if not v['cap']])
