"""Render a Blockbench model (.bbmodel) to the mod's logo, a 512 by 512 PNG.

The logo is Backpacks for Dummies' placed backpack, BackpackBlockModel.bbmodel, drawn with the texture
embedded in the model, at a three quarter view from the front (the clasp), the right side and above.

    python tools/scripts/logo/render_logo.py <Backpacks>/src/main/resources/assets/backpacks/BackpackBlockModel.bbmodel .github/assets/logo.png [yaw] [pitch]

Writes two files: the output on a dark warm background with a drop shadow, and beside it
<name>_transparent.png, the model alone. yaw turns the model about the vertical axis (35 degrees by
default, more shows more of the side), pitch tilts the view down onto the top (28 by default).

Faces are shaded the way Minecraft shades blocks: the top brightest, front and back less, the sides
least. The model is drawn at 2048 pixels with a depth buffer, then scaled down, which smooths its
edges. Copy the background version to src/main/resources/assets/backpacksrefined/icon.png, the icon
every manifest points at. Needs Pillow and NumPy.
"""
import base64, io, json, math, sys
import numpy as np
from PIL import Image, ImageFilter, ImageDraw

bb, out = sys.argv[1], sys.argv[2]
yaw = math.radians(float(sys.argv[3]) if len(sys.argv) > 3 else 35)
pitch = math.radians(float(sys.argv[4]) if len(sys.argv) > 4 else 28)

d = json.load(open(bb))
tex = Image.open(io.BytesIO(base64.b64decode(d['textures'][0]['source'].split(',', 1)[1]))).convert('RGBA')
T = np.asarray(tex).astype(np.float32)
TH, TW = T.shape[:2]
uvw, uvh = d['textures'][0].get('uv_width', TW), d['textures'][0].get('uv_height', TH)

# camera: rotate around Y by yaw, then tilt by pitch; look from +z toward -z
cy, sy, cp, sp = math.cos(yaw), math.sin(yaw), math.cos(pitch), math.sin(pitch)
def view(p):
    x, y, z = p
    x, z = x * cy + z * sy, -x * sy + z * cy
    y, z = y * cp - z * sp, y * sp + z * cp
    return np.array([x, -y, z])  # screen x, screen y (down), depth (bigger = nearer)

# face corner layout: origin, s axis end, t axis end (Minecraft/Blockbench convention)
def corners(f, a, b):
    x0, y0, z0 = a; x1, y1, z1 = b
    return {
        'north': ((x1, y1, z0), (x0, y1, z0), (x1, y0, z0)),
        'south': ((x0, y1, z1), (x1, y1, z1), (x0, y0, z1)),
        'east':  ((x1, y1, z1), (x1, y1, z0), (x1, y0, z1)),
        'west':  ((x0, y1, z0), (x0, y1, z1), (x0, y0, z0)),
        'up':    ((x0, y1, z0), (x1, y1, z0), (x0, y1, z1)),
        'down':  ((x0, y0, z1), (x1, y0, z1), (x0, y0, z0)),
    }[f]
shade = {'up': 1.0, 'south': 0.86, 'north': 0.86, 'east': 0.68, 'west': 0.68, 'down': 0.5}
normal = {'north': (0, 0, -1), 'south': (0, 0, 1), 'east': (1, 0, 0), 'west': (-1, 0, 0), 'up': (0, 1, 0), 'down': (0, -1, 0)}

faces = []
for e in d['elements']:
    for f, fd in e['faces'].items():
        o, sa, ta = (view(c) for c in corners(f, e['from'], e['to']))
        if view(normal[f])[2] <= 0:
            continue
        S, Tv = sa - o, ta - o
        if abs(S[0] * Tv[1] - S[1] * Tv[0]) < 1e-6:
            continue
        faces.append((f, o, S, Tv, fd['uv']))

pts = np.array([p for _, o, S, Tv, _ in faces for p in (o, o + S, o + Tv, o + S + Tv)])
mn, mx = pts[:, :2].min(0), pts[:, :2].max(0)
N = 2048
scale = N * 0.62 / (mx - mn).max()
off = N / 2 - (mn + mx) / 2 * scale

img = np.zeros((N, N, 4), np.float32)
zb = np.full((N, N), -1e9, np.float32)
gy, gx = np.mgrid[0:N, 0:N].astype(np.float32) + 0.5
for f, o, S, Tv, uv in faces:
    O = o[:2] * scale + off; A = S[:2] * scale; B = Tv[:2] * scale
    det = A[0] * B[1] - A[1] * B[0]
    px, py = gx - O[0], gy - O[1]
    s = (px * B[1] - py * B[0]) / det
    t = (A[0] * py - A[1] * px) / det
    m = (s >= 0) & (s < 1) & (t >= 0) & (t < 1)
    depth = o[2] + s * S[2] + t * Tv[2]
    u = uv[0] + s * (uv[2] - uv[0]); v = uv[1] + t * (uv[3] - uv[1])
    ti = np.clip((u * TW / uvw).astype(int), 0, TW - 1); tj = np.clip((v * TH / uvh).astype(int), 0, TH - 1)
    col = T[tj, ti]
    m &= (col[..., 3] > 0) & (depth > zb)
    zb[m] = depth[m]
    c = col.copy(); c[..., :3] *= shade[f]
    img[m] = c[m]

model = Image.fromarray(img.clip(0, 255).astype(np.uint8), 'RGBA')

# background: dark warm radial gradient + soft drop shadow, like the original logo
r = np.hypot(gx - N * 0.5, gy - N * 0.45) / (N * 0.75)
inner, outer = np.array([92, 52, 34]), np.array([18, 10, 8])
bg = (inner + (outer - inner) * np.clip(r, 0, 1)[..., None] ** 1.2).astype(np.uint8)
canvas = Image.fromarray(bg, 'RGB').convert('RGBA')
alpha = model.split()[3]
shadow = Image.new('RGBA', (N, N), (0, 0, 0, 0))
sh = ImageDraw.Draw(shadow)
by = int(np.nonzero(np.asarray(alpha).any(1))[0][-1])
sh.ellipse((N * 0.2, by - N * 0.09, N * 0.8, by + N * 0.01), fill=(0, 0, 0, 190))
canvas.alpha_composite(shadow.filter(ImageFilter.GaussianBlur(N * 0.03)))
canvas.alpha_composite(model)

canvas.convert('RGB').resize((512, 512), Image.LANCZOS).save(out)
model.resize((512, 512), Image.LANCZOS).save(out.replace('.png', '_transparent.png'))
