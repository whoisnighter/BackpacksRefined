"""Draw the tier backpacks' textures: the leather backpack's own textures with metal laid over them.

The leather is never replaced, only trimmed: a rim round the lid and the foot, brackets on the corners
and a latch (netherite also gets guards down every vertical edge), so every tier still reads as the
same leather backpack. The trim is drawn on the faces of the two models by name, in face coordinates
(x right, y down, 0,0 the face's top left as seen from outside), so it fits the placed backpack
(block/backpack.png, also what every inventory draws) and the worn one (entity/backpack.png). The flat sprite (item/backpack.png) is only the creative tab's icon,
which is the leather backpack, so the tiers need none.

    python tools/scripts/tiers/draw_tiers.py [tier ...]
    python tools/scripts/tiers/draw_tiers.py install [tier ...]

The first draws each tier (every tier when none is named) into tools/scripts/tiers/out/<tier>/:
block.png, entity.png and preview.png, the placed backpack from the front and the back, the worn one
and both textures, scaled up; out/sheet.png puts the leather and every tier drawn side by side.
install writes the tiers the mod has over their textures, textures/block/<tier>_backpack.png and
textures/entity/<tier>_backpack.png. Needs Pillow and NumPy.
"""
import json, math, sys
from pathlib import Path
import numpy as np
from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parents[3]
ASSETS = ROOT / 'src/main/resources/assets/backpacksrefined'
OUT = Path(__file__).resolve().parent / 'out'

# Metal palettes, in the mod's tier order, light to dark: highlight, light, mid, dark, shadow, then wear: a second mid for the
# odd worn spot. Taken from the vanilla textures: the five steps are that material's chestplate item,
# wear a colour of its ingot or gem that the chestplate lacks. Netherite sits a step darker than its
# chestplate, its light to shadow from the ingot's darker greys, so it reads heavier on the leather.
METALS = {
    'copper': [(0xfd, 0xd4, 0xcb), (0xfc, 0x99, 0x82), (0xd6, 0x6d, 0x48), (0x9c, 0x4e, 0x31), (0x80, 0x39, 0x21),
               (0xc1, 0x5a, 0x36)],
    'iron': [(0xff, 0xff, 0xff), (0xd8, 0xd8, 0xd8), (0xc6, 0xc6, 0xc6), (0x96, 0x96, 0x96), (0x6b, 0x6b, 0x6b),
             (0xa8, 0xa8, 0xa8)],
    'gold': [(0xff, 0xff, 0xff), (0xfd, 0xff, 0x76), (0xea, 0xee, 0x57), (0xe9, 0xb1, 0x15), (0xdc, 0x96, 0x13),
             (0xfa, 0xd6, 0x4a)],
    'diamond': [(0xff, 0xff, 0xff), (0xa1, 0xfb, 0xe8), (0x4a, 0xed, 0xd9), (0x20, 0xc5, 0xb5), (0x1a, 0xaa, 0xa7),
                (0x2c, 0xe0, 0xd8)],
    'netherite': [(0x76, 0x6a, 0x76), (0x4d, 0x49, 0x4d), (0x3b, 0x39, 0x3b), (0x27, 0x1c, 0x1d), (0x17, 0x11, 0x11),
                  (0x31, 0x29, 0x2a)],
}
# Latches in their own palette where the trim's is too dark for one: netherite's in its chestplate's.
LATCHES = {
    'netherite': [(0x76, 0x6a, 0x76), (0x5d, 0x56, 0x5d), (0x4d, 0x49, 0x4d), (0x37, 0x35, 0x37), (0x24, 0x1f, 0x20),
                  (0x3c, 0x32, 0x32)],
}


# --- faces -------------------------------------------------------------------------------------------

class Face:
    """One face's rectangle in a texture, drawn on in face coordinates."""

    def __init__(self, img, x0, y0, x1, y1, hidden_top=0):
        self.img, self.x0, self.y0 = img, x0, y0
        self.w, self.h = x1 - x0, y1 - y0
        self.top = hidden_top  # rows the lid covers

    def get(self, x, y):
        return self.img.getpixel((self.x0 + x, self.y0 + y))

    def put(self, x, y, c):
        if 0 <= x < self.w and 0 <= y < self.h:
            self.img.putpixel((self.x0 + x, self.y0 + y), tuple(c[:3]) + (255,))


def block_faces(img):
    """The placed backpack's faces in its 64 by 64 texture: south is the front with the clasp."""
    f = lambda *r, **k: Face(img, *r, **k)
    return {
        'base': {'south': f(26, 8, 36, 19, hidden_top=3), 'north': f(8, 8, 18, 19, hidden_top=3),
                 'east': f(0, 8, 8, 19, hidden_top=3), 'west': f(18, 8, 26, 19, hidden_top=3),
                 'down': f(18, 0, 28, 8)},
        'lid': {'south': f(29, 28, 40, 33), 'north': f(9, 28, 20, 33), 'east': f(0, 28, 9, 33),
                'west': f(20, 28, 29, 33), 'up': f(9, 19, 20, 28)},
    }


def entity_faces(img):
    """The worn backpack's faces: box UV at texOffs(0, 0) 8x9x6 and texOffs(0, 15) 9x4x7."""
    f = lambda *r, **k: Face(img, *r, **k)
    return {
        'base': {'south': f(20, 6, 28, 15, hidden_top=3), 'north': f(6, 6, 14, 15, hidden_top=3),
                 'east': f(0, 6, 6, 15, hidden_top=3), 'west': f(14, 6, 20, 15, hidden_top=3),
                 'down': f(14, 0, 22, 6)},
        'lid': {'south': f(23, 22, 32, 26), 'north': f(7, 22, 16, 26), 'east': f(0, 22, 7, 26),
                'west': f(16, 22, 23, 26), 'up': f(7, 15, 16, 22)},
    }


# --- metal -------------------------------------------------------------------------------------------

def lum(c):
    return (0.3 * c[0] + 0.59 * c[1] + 0.11 * c[2]) / 255


class Metal:
    def __init__(self, pal, latch=None):
        self.p = pal
        self.latch = Metal(latch) if latch else self

    def tone(self, i, under):
        """Palette step i; a mid over the leather's darkest spots is worn. Only the darkest: wearing
        it wherever the leather was darker than average looked checkered."""
        return self.p[5] if i == 2 and lum(under) < 0.29 else self.p[i]

    def plate(self, f, x0, y0, x1, y1):
        """A bevelled plate over [x0, x1) by [y0, y1): lit top and left, shaded bottom and right."""
        for y in range(y0, y1):
            for x in range(x0, x1):
                if x == x1 - 1 or y == y1 - 1:
                    i = 3
                elif x == x0 or y == y0:
                    i = 1
                else:
                    i = 2
                f.put(x, y, self.tone(i, f.get(x, y)))

    def strip(self, f, y):
        """A one pixel rim along row y, lit from the left."""
        for x in range(f.w):
            i = 1 if x == 0 else 3 if x == f.w - 1 else 2
            f.put(x, y, self.tone(i, f.get(x, y)))

    def column(self, f, x, y0, y1):
        """A one pixel guard down column x over [y0, y1), lit from the top."""
        for y in range(y0, y1):
            f.put(x, y, self.tone(1 if y == y0 else 3 if y == y1 - 1 else 2, f.get(x, y)))

    def dot(self, f, x, y, i):
        f.put(x, y, self.p[i])


def center(w, n):
    return (w - n) // 2


# --- trim ----------------------------------------------------------------------------------------------
# The trim draws on one model's faces; m is the Metal.

def clasp(m, F):
    """A latch: a plate on the lid's front edge and a hasp on the pocket below it."""
    m = m.latch
    lid, base = F['lid']['south'], F['base']['south']
    n = 3 if lid.w % 2 else 2
    x = center(lid.w, n)
    m.plate(lid, x, lid.h - 2, x + n, lid.h)
    m.dot(lid, x + n // 2, lid.h - 1, 4)  # keyhole
    bx = center(base.w, 2)
    for y in range(base.top - 1, base.top + 2):
        base.put(bx, y, m.tone(1, base.get(bx, y)))
        base.put(bx + 1, y, m.tone(3, base.get(bx + 1, y)))


def trim(m, F):
    """The trim: a rim round the lid and the base's foot, L brackets on the corners."""
    for f in F['lid'].values():
        if f is F['lid']['up']:
            continue
        m.strip(f, f.h - 1)
    up = F['lid']['up']
    for cx, cy, dx, dy in ((0, 0, 1, 1), (up.w - 1, 0, -1, 1), (0, up.h - 1, 1, -1), (up.w - 1, up.h - 1, -1, -1)):
        m.dot(up, cx, cy, 1); m.dot(up, cx + dx, cy, 2); m.dot(up, cx, cy + dy, 2)
    for name in ('south', 'north', 'east', 'west'):
        f = F['base'][name]
        m.strip(f, f.h - 1)
        for x in (0, f.w - 1):
            m.column(f, x, f.h - 3, f.h)
    d = F['base']['down']
    for x, y in ((0, 0), (d.w - 2, 0), (0, d.h - 2), (d.w - 2, d.h - 2)):
        m.plate(d, x, y, x + 2, y + 2)
    clasp(m, F)


def reinforced(m, F):
    """The trim, and guards down every vertical edge of the base and the lid."""
    trim(m, F)
    for part in ('base', 'lid'):
        for name in ('south', 'north', 'east', 'west'):
            f = F[part][name]
            for x in (0, f.w - 1):
                m.column(f, x, f.top, f.h)


# Every tier wears the trim but netherite, the last, which gets the heavier one.
STYLES = {'netherite': reinforced}


# --- preview -----------------------------------------------------------------------------------------

SHADE = {'up': 1.0, 'south': 0.86, 'north': 0.86, 'east': 0.68, 'west': 0.68, 'down': 0.5}
NORMAL = {'north': (0, 0, -1), 'south': (0, 0, 1), 'east': (1, 0, 0), 'west': (-1, 0, 0),
          'up': (0, 1, 0), 'down': (0, -1, 0)}


def corners(f, a, b):
    x0, y0, z0 = a; x1, y1, z1 = b
    return {
        'north': ((x1, y1, z0), (x0, y1, z0), (x1, y0, z0)),
        'south': ((x0, y1, z1), (x1, y1, z1), (x0, y0, z1)),
        'east': ((x1, y1, z1), (x1, y1, z0), (x1, y0, z1)),
        'west': ((x0, y1, z0), (x0, y1, z1), (x0, y0, z0)),
        'up': ((x0, y1, z0), (x1, y1, z0), (x0, y1, z1)),
        'down': ((x0, y0, z1), (x1, y0, z1), (x0, y0, z0)),
    }[f]


def render(elements, tex, uv_size, yaw, pitch=25, size=320):
    """Elements as in a block model (uv in uv_size units), drawn the way render_logo.py does."""
    T = np.asarray(tex.convert('RGBA')).astype(np.float32)
    TH, TW = T.shape[:2]
    yr, pr = math.radians(yaw), math.radians(pitch)
    cy, sy, cp, sp = math.cos(yr), math.sin(yr), math.cos(pr), math.sin(pr)

    def view(p, pivot=True):
        x, y, z = p
        if pivot:  # turn about the block's centre
            x, y, z = x - 8, y - 6, z - 8
        x, z = x * cy + z * sy, -x * sy + z * cy
        y, z = y * cp - z * sp, y * sp + z * cp
        return np.array([x, -y, z])

    faces = []
    for e in elements:
        for f, uv in e['faces'].items():
            o, sa, ta = (view(c) for c in corners(f, e['from'], e['to']))
            if view(NORMAL[f], False)[2] <= 0:
                continue
            S, Tv = sa - o, ta - o
            if abs(S[0] * Tv[1] - S[1] * Tv[0]) < 1e-6:
                continue
            faces.append((f, o, S, Tv, uv))
    N = size * 3
    img = np.zeros((N, N, 4), np.float32)
    zb = np.full((N, N), -1e9, np.float32)
    gy, gx = np.mgrid[0:N, 0:N].astype(np.float32) + 0.5
    scale, off = N / 20, N / 2
    for f, o, S, Tv, uv in faces:
        O = o[:2] * scale + off; A = S[:2] * scale; B = Tv[:2] * scale
        det = A[0] * B[1] - A[1] * B[0]
        px, py = gx - O[0], gy - O[1]
        s = (px * B[1] - py * B[0]) / det
        t = (A[0] * py - A[1] * px) / det
        msk = (s >= 0) & (s < 1) & (t >= 0) & (t < 1)
        depth = o[2] + s * S[2] + t * Tv[2]
        u = uv[0] + s * (uv[2] - uv[0]); v = uv[1] + t * (uv[3] - uv[1])
        ti = np.clip((u * TW / uv_size).astype(int), 0, TW - 1)
        tj = np.clip((v * TH / uv_size).astype(int), 0, TH - 1)
        col = T[tj, ti]
        msk &= (col[..., 3] > 0) & (depth > zb)
        zb[msk] = depth[msk]
        c = col.copy(); c[..., :3] *= SHADE[f]
        img[msk] = c[msk]
    return Image.fromarray(img.clip(0, 255).astype(np.uint8), 'RGBA').resize((size, size), Image.LANCZOS)


def block_elements():
    d = json.load(open(ASSETS / 'models/block/backpack.json'))
    return [{'from': e['from'], 'to': e['to'], 'faces': {k: v['uv'] for k, v in e['faces'].items()}}
            for e in d['elements']]


def entity_elements():
    """The worn base and lid, placed y up with the outside (south) toward +z, centred like the block."""
    def box(x0, y0, z0, w, h, d, u, v):
        return {'from': [x0, y0, z0], 'to': [x0 + w, y0 + h, z0 + d], 'faces': {
            'up': [u + d, v, u + d + w, v + d], 'down': [u + d + w, v, u + d + 2 * w, v + d],
            'east': [u, v + d, u + d, v + d + h], 'north': [u + d, v + d, u + d + w, v + d + h],
            'west': [u + d + w, v + d, u + 2 * d + w, v + d + h],
            'south': [u + 2 * d + w, v + d, u + 2 * d + 2 * w, v + d + h]}}
    return [box(4, 0, 5, 8, 9, 6, 0, 0), box(3.5, 6, 4.5, 9, 4, 7, 0, 15)]


def preview(block, entity, label):
    be, ee = block_elements(), entity_elements()
    tiles = [render(be, block, 16, 35), render(be, block, 16, 215), render(ee, entity, 64, 35),
             block.resize((320, 320), Image.NEAREST), entity.resize((320, 320), Image.NEAREST)]
    W = sum(t.width for t in tiles) + 20 * (len(tiles) + 1)
    sheet = Image.new('RGBA', (W, 380), (46, 40, 38, 255))
    x = 20
    for i, t in enumerate(tiles):
        if i >= 3:  # the textures, on a lighter ground
            sheet.paste((70, 62, 58, 255), (x, 40, x + t.width, 40 + t.height))
        sheet.alpha_composite(t, (x, 40 + (320 - t.height) // 2))
        x += t.width + 20
    ImageDraw.Draw(sheet).text((20, 12), label, fill=(235, 225, 215, 255))
    return sheet


# --- main --------------------------------------------------------------------------------------------

def leather():
    load = lambda k: Image.open(ASSETS / f'textures/{k}/backpack.png').convert('RGBA')
    return load('block'), load('entity')


def draw(tier):
    m = Metal(METALS[tier], LATCHES.get(tier))
    block, entity = leather()
    style = STYLES.get(tier, trim)
    style(m, block_faces(block))
    style(m, entity_faces(entity))
    return block, entity


def main():
    args = sys.argv[1:]
    if args[:1] == ['install']:
        for tier in args[1:] or list(METALS):
            if not (ASSETS / f'models/block/{tier}_backpack.json').exists():  # drawn to try only
                print('skipped', tier, '(the mod has no such tier)')
                continue
            for kind, img in zip(('block', 'entity'), draw(tier)):
                path = ASSETS / f'textures/{kind}/{tier}_backpack.png'
                img.save(path)
                print('wrote', path.relative_to(ROOT))
        return
    rows = [preview(*leather(), 'leather')]
    for tier in args or list(METALS):
        block, entity = draw(tier)
        d = OUT / tier
        d.mkdir(parents=True, exist_ok=True)
        block.save(d / 'block.png'); entity.save(d / 'entity.png')
        p = preview(block, entity, tier)
        p.save(d / 'preview.png')
        rows.append(p)
        print('wrote', d.relative_to(ROOT))
    sheet = Image.new('RGBA', (rows[0].width, sum(r.height for r in rows)))
    for i, r in enumerate(rows):
        sheet.paste(r, (0, i * r.height))
    sheet.save(OUT / 'sheet.png')


if __name__ == '__main__':
    main()
