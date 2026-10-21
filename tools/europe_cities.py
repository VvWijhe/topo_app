import sys, json, re; sys.path.insert(0, 'scripts')
from dbf import read_dbf
EU = 'ISL NOR SWE FIN DNK EST LVA LTU IRL GBR NLD BEL LUX DEU POL CZE SVK AUT CHE LIE FRA MCO AND ESP PRT ITA SMR VAT MLT SVN HRV BIH SRB MNE KOS ALB MKD GRC BGR ROU MDA UKR BLR RUS HUN CYP TUR GEO ARM AZE'.split()
NE_CODE = {'KOS': 'KOS'}
world = json.load(open('world.json'))['c']
rows = [r for r in read_dbf('data/places10.dbf') if r['adm0_a3'] in EU and r['pop_max']]
# Nederlandse namen die afwijken
NL = {'Saint Petersburg':'Sint-Petersburg','Plzeň':'Pilsen','Limassol':'Limassol','Ganja':'Gəncə','Gomel':'Homel','Mogilev':'Mahiljow','Nakhchivan':'Nachitsjevan',
      'Patras':'Patras','Aarhus':'Aarhus','Antwerp':'Antwerpen','Ghent':'Gent','Bruges':'Brugge','Málaga':'Málaga','Liverpool':'Liverpool','Munich':'München','Cologne':'Keulen','Milan':'Milaan','Naples':'Napels','Turin':'Turijn','Venice':'Venetië','Genoa':'Genua','Antwerp':'Antwerpen',
      'Ghent':'Gent','Bruges':'Brugge','Liège':'Luik','Seville':'Sevilla','Saint Petersburg':'Sint-Petersburg','St. Petersburg':'Sint-Petersburg','Gothenburg':'Göteborg',
      'Geneva':'Genève','Zurich':'Zürich','Basel':'Bazel','Kraków':'Krakau','Krakow':'Krakau','Aachen':'Aken','Hanover':'Hannover','Nuremberg':'Neurenberg',
      'Nürnberg':'Neurenberg','Odessa':'Odesa','The Hague':'Den Haag','Thessaloniki':'Thessaloniki','Florence':'Florence','Lisbon':'Lissabon','Porto':'Porto',
      'Bologna':'Bologna','Palermo':'Palermo','Rotterdam':'Rotterdam','Marseille':'Marseille','Nizhny Novgorod':'Nizjni Novgorod','Kharkiv':'Charkiv',
      'Rostov':'Rostov aan de Don','Gdansk':'Gdańsk','Wroclaw':'Wrocław','Poznan':'Poznań','Lodz':'Łódź','Brno':'Brno','Izmir':'İzmir','Kazan':'Kazan',
      'Bremen':'Bremen','Dusseldorf':'Düsseldorf','Düsseldorf':'Düsseldorf','Mainz':'Mainz','Strasbourg':'Straatsburg','Lille':'Rijsel'}
EN_FIX = {'St.  Petersburg':'Saint Petersburg','Pizen':'Plzeň','Lemosos':'Limassol','Ganca':'Ganja','Sumqayt':'Sumqayit','Homyel':'Gomel','Mahilyow':'Mogilev',
          'Naxcivan':'Nakhchivan','Patra':'Patras','Århus':'Aarhus','Antwerpen':'Antwerp','Gent':'Ghent','Brugge':'Bruges','Constanța':'Constanța','Volgograd':'Volgograd','Kiev':'Kyiv','Kharkov':'Kharkiv','Odessa':'Odesa','Dnipropetrovsk':'Dnipro','Lvov':'Lviv','Saint Petersburg':'Saint Petersburg','St. Petersburg':'Saint Petersburg',
          'Gdansk':'Gdańsk','Wroclaw':'Wrocław','Poznan':'Poznań','Lodz':'Łódź','Krakow':'Kraków','Dusseldorf':'Düsseldorf','Nurnberg':'Nuremberg','Nürnberg':'Nuremberg'}
MUST = {
 'DEU': ['Hamburg','Munich','Cologne','Frankfurt','Stuttgart','Dresden'], 'ITA': ['Milan','Naples','Turin','Venice','Florence','Palermo'],
 'ESP': ['Barcelona','Valencia','Seville','Bilbao','Málaga'], 'FRA': ['Marseille','Lyon','Nice','Toulouse','Bordeaux','Lille'],
 'GBR': ['Manchester','Birmingham','Edinburgh','Glasgow','Liverpool','Belfast'], 'NLD': ['Rotterdam','The Hague','Utrecht'],
 'POL': ['Kraków','Gdansk','Wroclaw','Poznan'], 'RUS': ['St.  Petersburg','Nizhny Novgorod','Kazan','Volgograd','Kaliningrad','Murmansk'],
 'TUR': ['Istanbul','İzmir','Antalya','Bursa','Trabzon'], 'UKR': ['Kharkiv','Odessa','Lviv','Dnipro'], 'GRC': ['Thessaloniki','Patra'],
 'PRT': ['Porto','Faro'], 'CHE': ['Zürich','Geneva','Basel'], 'AUT': ['Salzburg','Graz','Innsbruck'], 'SWE': ['Göteborg','Malmö'],
 'NOR': ['Bergen','Trondheim'], 'DNK': ['Århus'], 'BEL': ['Antwerpen','Gent','Brugge'], 'CZE': ['Brno'], 'FIN': ['Tampere','Turku'],
 'IRL': ['Cork'], 'HRV': ['Split','Dubrovnik'], 'SRB': ['Novi Sad'], 'ROU': ['Cluj-Napoca','Constanța'], 'BGR': ['Plovdiv','Varna'],
 'HUN': ['Debrecen'], 'SVK': ['Košice'], 'CYP': ['Lemosos'], 'GEO': ['Batumi'],
}
def extra(code):
    p = world[code]['pop']
    if code == 'RUS': return 7
    if code in ('DEU', 'FRA', 'ITA', 'GBR'): return 6
    if code in ('TUR', 'ESP'): return 5
    if code in ('UKR', 'POL'): return 4
    if p < 1_000_000: return 1
    if p < 3_000_000: return 1
    if p < 10_000_000: return 2
    if p < 40_000_000: return 3
    return 5
out = []
for code in EU:
    rs = sorted([r for r in rows if r['adm0_a3'] == code], key=lambda r: -r['pop_max'])
    if code == 'RUS': rs = [r for r in rs if r['longitude'] < 60]
    if code == 'LUX': rs = [r for r in rs if r['name'] != 'Diekirch']
    w = world[code]
    cap_en, cap_nl = w['ce'], w['cap']
    caps = [r for r in rs if r['adm0cap'] == 1]
    # hoofdstad: neem de rij met de naam uit de wereldkaart, anders adm0cap
    cap = next((r for r in rs if r['name'] in (cap_en, cap_nl) or r['nameascii'] == cap_en), caps[0] if caps else None)
    picked = []
    if cap:
        picked.append((cap, True))
    others = [r for r in rs if r is not cap and r['name'] not in (cap_en, cap_nl)]
    must = [next((r for r in others if r['name'] == m or r['nameascii'] == m), None) for m in MUST.get(code, [])]
    missing = [m for m, r in zip(MUST.get(code, []), must) if r is None]
    if missing: print('MISSING', code, missing)
    must = [r for r in must if r is not None]
    rest = [r for r in others if r not in must]
    for r in (must + rest)[:max(extra(code), len(must))]: picked.append((r, False))
    for r, is_cap in picked:
        en = cap_en if is_cap else EN_FIX.get(r['name'], r['name'])
        nl = cap_nl if is_cap else NL.get(r['name'], NL.get(en, en))
        alts = sorted({a.strip() for x in [r['nameascii'], r['name'], r['namealt']] if x for a in x.split('|') if a.strip() and a.strip() not in (en, nl)})
        out.append(dict(nl=nl, en=en, lat=round(r['latitude'], 3), lon=round(r['longitude'], 3), code=code, cap=is_cap, pop=r['pop_max'], alt=alts,
                        cen=w['ne'], cnl=w['n']))
# handmatig: tweede stad waar de data er geen heeft
manual = [('Nikšić', 'Nikšić', 42.773, 18.944, 'MNE', 56970), ('Esch-sur-Alzette', 'Esch-sur-Alzette', 49.496, 5.981, 'LUX', 36000)]
for nl, en, lat, lon, code, pop in manual:
    w = world[code]; out.append(dict(nl=nl, en=en, lat=lat, lon=lon, code=code, cap=False, pop=pop, alt=['niksic'], cen=w['ne'], cnl=w['n']))
out.sort(key=lambda d: -d['pop'])
seen = set()
for d in out:
    k = d['nl']
    if k in seen: d['nl'] = k + ' (' + d['cnl'] + ')'
    seen.add(d['nl'])
print(len(out))
for d in out: print(d['code'], d['cap'], d['en'], '/', d['nl'], int(d['pop']), d['alt'][:3])
json.dump(out, open('europe_cities.json', 'w'), ensure_ascii=False)
