"""Maakt app/src/main/assets/flags/flagdata.json: per vlag de kleuren (gemeten), vormen (flag_tags.py)
en een paar 'interessante' uitsneden voor de puzzel Vlag van dichtbij."""
import json, os, sys
from PIL import Image
sys.path.insert(0, os.path.dirname(__file__))
from flag_tags import TAGS

import colorsys

def nearest(c):
    """Kleurnaam op tint/verzadiging/helderheid (zoals mensen kleuren benoemen)."""
    h, sat, v = colorsys.rgb_to_hsv(*(x / 255 for x in c))
    h *= 360
    if v < 0.16 or (v < 0.3 and sat < 0.35): return 'black'
    if sat < 0.18: return 'white' if v > 0.6 else 'black'
    if h < 14 or h >= 300: return 'red'
    if h < 38: return 'orange' if v > 0.55 else 'red'
    if h < 70: return 'yellow'
    if h < 165: return 'green'
    if h < 195: return 'green' if v < 0.5 else 'blue'  # donker teal = groen (Sri Lanka), helder cyaan = blauw
    if h < 265: return 'blue'
    return 'red'

# Correcties na controle met het oog: schaduwen/randen in emblemen die als losse kleur geteld werden,
# en donkerblauw dat als zwart werd gezien.
DROP = {'AD': ['orange'], 'KZ': ['green'], 'SE': ['green'], 'KG': ['orange'], 'MK': ['orange'], 'ME': ['orange'],
        'MD': ['orange', 'black'], 'ER': ['orange'], 'UY': ['red'], 'KH': ['black'], 'TM': ['orange'], 'SM': ['black']}
AS_BLUE = {'TH', 'NP'}

root = os.path.join(os.path.dirname(__file__), '..', 'app', 'src', 'main', 'assets', 'flags')
tags = {l.split()[0]: l.split()[1:] for l in TAGS.strip().splitlines()}
out = {}
for f in sorted(os.listdir(root)):
    if not f.endswith('.webp'): continue
    code = f[:2].upper()
    im = Image.open(os.path.join(root, f)).convert('RGB')
    small = im.resize((96, 64), Image.NEAREST)
    px = list(small.get_flattened_data()) if hasattr(small, "get_flattened_data") else list(small.getdata())
    counts = {}
    for c in px: counts[nearest(c)] = counts.get(nearest(c), 0) + 1
    colors = sorted([n for n, k in counts.items() if k / len(px) >= 0.015], key=lambda n: -counts[n])
    # Uitsneden: vierkant van 30% van de hoogte; score = aantal kleuren + randen; de 4 beste, niet te dicht bij elkaar.
    W, H = small.size
    grid = [[nearest(small.getpixel((x, y))) for x in range(W)] for y in range(H)]
    win = int(H * 0.3)
    cands = []
    for cy in range(win // 2, H - win // 2, 3):
        for cx in range(win // 2, W - win // 2, 3):
            cols, edges = set(), 0
            for y in range(cy - win // 2, cy + win // 2):
                for x in range(cx - win // 2, cx + win // 2):
                    cols.add(grid[y][x])
                    if x + 1 < W and grid[y][x] != grid[y][x + 1]: edges += 1
                    if y + 1 < H and grid[y][x] != grid[y + 1][x]: edges += 1
            cands.append((len(cols) * 40 + min(edges, 120), cx / W, cy / H))
    cands.sort(reverse=True)
    picked = []
    for sc, x, y in cands:
        if all(abs(x - a) + abs(y - b) > 0.25 for a, b in picked): picked.append((round(x, 3), round(y, 3)))
        if len(picked) == 4: break
    colors = [c for c in colors if c not in DROP.get(code, [])]
    if code in AS_BLUE: colors = list(dict.fromkeys(['blue' if c == 'black' else c for c in colors]))
    out[code] = {'c': colors, 't': tags.get(code, []), 'z': picked, 'r': round(im.size[0] / im.size[1], 3)}
missing = [c for c in out if c not in tags]
print('vlaggen', len(out), 'zonder tags', missing)
json.dump(out, open(os.path.join(root, 'flagdata.json'), 'w'), separators=(',', ':'))
