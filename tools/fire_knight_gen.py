"""Fire Knight: GeckoLib geo.json + texture + glowmask generator.
Coordinates: Bedrock geometry units (1 unit = 1 px = 1/16 block), Y up, front = -Z.
"""
import json, os, random
import numpy as np
from PIL import Image

random.seed(7)
OUT = "src/main/resources/assets/jsnboss"
TEX_W, TEX_H = 256, 256

# ---------------------------------------------------------------- model
# cube: (origin, size, material, tag)
def mirror(cubes):
    out = []
    for (o, s, m, tag) in cubes:
        out.append(([-(o[0] + s[0]), o[1], o[2]], s, m, tag))
    return out

def flame_cross(cx, y, cz, w, h, tag="flame"):
    """two crossed zero-thickness planes, like vanilla fire"""
    return [([cx - w / 2, y, cz], [w, h, 0], "flame", tag),
            ([cx, y, cz - w / 2], [0, h, w], "flame", tag)]

R_LEG = [
    ([-8.5, 0, -6.5], [7, 3, 9], "iron", "sabaton"),
    ([-8, 0, -8], [6, 2, 2], "iron", "toe"),
    ([-8.5, 3, -4], [7, 7, 7], "iron_crack", "greave"),
    ([-9, 9, -4.5], [8, 2, 8], "iron", "greave_rim"),
    ([-8.5, 10, -5.5], [7, 4, 2], "iron", "knee_cop"),
    ([-6, 11, -6.5], [2, 2, 1], "trim", "knee_point"),
    ([-8, 11, -3.5], [7, 9, 7], "iron", "thigh"),
]
R_ARM = [
    ([-16.5, 27, -3], [6, 10, 6], "iron", "upper_arm"),
    ([-17, 25, -3.5], [7, 3, 7], "iron", "couter"),
    ([-14.5, 25.5, 3.5], [2, 2, 2], "horn", "elbow_spike"),
    # layered pauldron (bell shape, steps outward/down)
    ([-19, 42, -5], [9, 2, 10], "trim", "pauldron_cap"),
    ([-20, 39, -6], [10, 3, 12], "iron_crack", "pauldron1"),
    ([-21, 36, -6.5], [9, 3, 13], "iron", "pauldron2"),
    ([-21.5, 33, -6], [7, 3, 12], "iron", "pauldron3"),
    ([-16, 44, -1], [2, 3, 2], "horn", "spike"),
]
R_HAND = [
    ([-16.5, 20, -3], [6, 6, 6], "iron_crack", "vambrace"),
    ([-17.5, 17, -4], [8, 3, 8], "iron", "cuff"),
    ([-16.5, 13, -3], [6, 4, 6], "iron", "fist"),
]
HEAD = [
    ([-4.5, 41, -4.5], [9, 9, 9], "helmet", "helm"),
    ([-5, 46, -5.5], [10, 2, 2], "iron", "brow"),
    ([-4.5, 41, -5.5], [9, 3, 1], "iron", "faceplate"),
    ([-3.5, 50, -3.5], [7, 2, 7], "iron", "dome"),
    ([-0.5, 50, -5], [1, 3, 10], "trim", "crest"),
    ([-5.5, 41, -4], [1, 6, 7], "iron", "cheek"),
    ([-7, 46, -2], [2, 3, 3], "horn", "horn1"),
    ([-10, 47, -1.5], [3, 3, 3], "horn", "horn2"),
    ([-11.5, 50, -1], [3, 4, 2], "horn", "horn3"),
    ([-12, 54, -0.5], [2, 2, 1], "horn", "horn4"),
    ([-12.5, 56, -0.5], [1, 1, 1], "horn_tip", "horn5"),
]
HEAD += mirror(HEAD[5:])

BONES = [
    dict(name="root", pivot=[0, 0, 0], cubes=[]),
    dict(name="right_leg", parent="root", pivot=[-4.5, 20, 0], cubes=R_LEG),
    dict(name="left_leg", parent="root", pivot=[4.5, 20, 0], cubes=mirror(R_LEG)),
    dict(name="waist", parent="root", pivot=[0, 20, 0], cubes=[
        ([-9, 20, -5], [18, 3, 10], "trim", "belt"),
        ([-2, 20, -5.5], [4, 3, 1], "core", "buckle"),
        ([-8.5, 16, -6], [8, 5, 1], "iron", "tasset"),
        ([-8, 12, -6.5], [7, 5, 1], "iron_crack", "tasset"),
        ([-10, 14, -4], [1, 7, 8], "iron", "tasset"),
        ([-8, 13, 5], [16, 8, 1], "iron", "tasset"),
    ] + mirror([
        ([-8.5, 16, -6], [8, 5, 1], "iron", "tasset"),
        ([-8, 12, -6.5], [7, 5, 1], "iron_crack", "tasset"),
        ([-10, 14, -4], [1, 7, 8], "iron", "tasset"),
    ])),
    dict(name="torso", parent="waist", pivot=[0, 23, 0], cubes=[
        ([-7, 23, -4.5], [14, 3, 9], "iron", "lame"),
        ([-7.5, 25.5, -5], [15, 3, 10], "iron", "lame"),
        ([-8, 28, -5], [16, 3, 10], "iron", "lame"),
        ([-10, 30, -5.5], [20, 9, 11], "chest", "chest"),
        ([-9, 38, -5], [18, 3, 10], "iron", "collar"),
        ([-9, 34, -6.5], [6, 4, 1], "iron", "pec"),
        ([3, 34, -6.5], [6, 4, 1], "iron", "pec"),
        ([-1, 30, -6.5], [2, 3, 1], "iron", "keel"),
        ([-2.5, 33, -7], [5, 5, 1], "core", "core"),
        ([-4, 39, -4], [8, 3, 8], "iron", "gorget"),
    ]),
    dict(name="head", parent="torso", pivot=[0, 40, 0], cubes=HEAD),
    dict(name="right_arm", parent="torso", pivot=[-13.5, 37, 0], cubes=R_ARM),
    dict(name="right_hand", parent="right_arm", pivot=[-13.5, 26, 0], cubes=R_HAND),
    dict(name="sword", parent="right_hand", pivot=[-13.5, 15, 0], rotation=[-60, 0, 0], cubes=[
        ([-14.5, 11, -1], [2, 10, 2], "grip", "grip"),
        ([-15, 21, -1.5], [3, 3, 3], "trim", "pommel"),
        ([-15, 9, -6], [3, 2, 12], "trim", "guard"),
        ([-14, 9, -7.5], [2, 3, 2], "horn", "guard_claw"),
        ([-14, 9, 5.5], [2, 3, 2], "horn", "guard_claw"),
        ([-14.5, 3, -3.5], [2, 6, 7], "blade", "blade_base"),
        ([-14.5, -15, -3], [2, 18, 6], "blade", "blade"),
        ([-14.5, -18, -2], [2, 3, 4], "blade", "tip1"),
        ([-14.5, -19, -1], [2, 1, 2], "blade", "tip2"),
    ]),
    dict(name="blade_fire", parent="sword", pivot=[-13.5, -4, 0], cubes=[
        ([-13.5, -16, -5], [0, 24, 10], "flame_blade", "blade_fire"),
    ]),
    dict(name="plume", parent="head", pivot=[0, 52, 0.5], cubes=flame_cross(0, 52, 0.5, 10, 10, "plume")),
    dict(name="right_flame", parent="right_arm", pivot=[-15, 44, 0], cubes=flame_cross(-15, 44, 0, 8, 7)),
    dict(name="left_arm", parent="torso", pivot=[13.5, 37, 0], cubes=mirror(R_ARM)),
    dict(name="left_hand", parent="left_arm", pivot=[13.5, 26, 0], cubes=mirror(R_HAND)),
    dict(name="left_flame", parent="left_arm", pivot=[15, 44, 0], cubes=mirror(flame_cross(-15, 44, 0, 8, 7))),
    dict(name="cape", parent="torso", pivot=[0, 39, 5.5], cubes=[
        ([-8, 13, 5.5], [16, 26, 1], "cape", "cape"),
    ]),
    dict(name="cape_fire", parent="cape", pivot=[0, 10, 6.6], cubes=[
        ([-8, 10, 6.6], [16, 7, 0], "flame", "cape_fire"),
    ]),
]


# ---------------------------------------------------------------- UV packing (shelf)
all_cubes = [(b, c) for b in BONES for c in b["cubes"]]
order = sorted(range(len(all_cubes)), key=lambda i: -(all_cubes[i][1][1][2] + all_cubes[i][1][1][1]))
uv = {}
x = y = shelf_h = 0
for i in order:
    w, h, d = all_cubes[i][1][1]
    bw, bh = 2 * (w + d), d + h
    if x + bw > TEX_W:
        x, y, shelf_h = 0, y + shelf_h + 1, 0
    uv[i] = (x, y)
    x += bw + 1
    shelf_h = max(shelf_h, bh)
assert y + shelf_h <= TEX_H, ("texture overflow", y + shelf_h)
USED_H = y + shelf_h

# ---------------------------------------------------------------- face geometry
def faces(o, s, u0, v0):
    """yield (name, A, Svec, Tvec, (tu, tv, tw, th), outward normal)"""
    w, h, d = s
    x0, y0, z0 = o; x1, y1, z1 = x0 + w, y0 + h, z0 + d
    yield ("north", (x0, y1, z0), (w, 0, 0), (0, -h, 0), (u0 + d, v0 + d, w, h), (0, 0, -1))
    yield ("south", (x1, y1, z1), (-w, 0, 0), (0, -h, 0), (u0 + 2 * d + w, v0 + d, w, h), (0, 0, 1))
    yield ("west", (x0, y1, z1), (0, 0, -d), (0, -h, 0), (u0, v0 + d, d, h), (-1, 0, 0))
    yield ("east", (x1, y1, z0), (0, 0, d), (0, -h, 0), (u0 + d + w, v0 + d, d, h), (1, 0, 0))
    yield ("up", (x0, y1, z1), (w, 0, 0), (0, 0, -d), (u0 + d, v0, w, d), (0, 1, 0))
    yield ("down", (x0, y0, z1), (w, 0, 0), (0, 0, -d), (u0 + d + w, v0, w, d), (0, -1, 0))

# ---------------------------------------------------------------- painting
tex = np.zeros((TEX_H, TEX_W, 4), np.uint8)
glow = np.zeros((TEX_H, TEX_W, 4), np.uint8)

def hexc(h):
    h = h.lstrip("#"); return np.array([int(h[i:i + 2], 16) for i in (0, 2, 4)], float)

PAL = {
    "iron": ("#2b2a31", "#47454f", "#141317"),
    "helmet": ("#2b2a31", "#4d4a55", "#141317"),
    "chest": ("#2b2a31", "#4a4852", "#141317"),
    "iron_crack": ("#2b2a31", "#47454f", "#141317"),
    "trim": ("#6a3420", "#9a5530", "#2e140b"),
    "grip": ("#3a2416", "#563624", "#1c1009"),
    "horn": ("#221e1f", "#3d3536", "#0f0c0d"),
    "horn_tip": ("#ff7a1c", "#ffc24a", "#b33a08"),
    "cape": ("#7c1515", "#a12424", "#3a0808"),
    "blade": ("#1e1b26", "#3a3647", "#0c0a10"),
    "core": ("#ff9a26", "#fff07a", "#d14a0c"),
}
GLOW_MATS = {"horn_tip", "core"}
EMBER = [hexc("#ff5a0a"), hexc("#ff8e1f"), hexc("#ffc84a")]

def put(px, py, col, a=255, g=False):
    tex[py, px, :3] = np.clip(col, 0, 255); tex[py, px, 3] = a
    if g:
        glow[py, px, :3] = np.clip(col, 0, 255); glow[py, px, 3] = 255

def crack(fx, fy, fw, fh, n, length):
    for _ in range(n):
        cx, cy = random.randrange(fw), random.randrange(fh)
        for k in range(length):
            put(fx + cx, fy + cy, EMBER[min(2, k // 2) if k < 2 else random.choice([0, 1, 1, 2])], g=True)
            cx = max(0, min(fw - 1, cx + random.choice([-1, 0, 1])))
            cy = max(0, min(fh - 1, cy + random.choice([-1, 1, 1, 0])))

FLAME = [hexc("#fff6b8"), hexc("#ffd04a"), hexc("#ff8a1e"), hexc("#ff4a0c"), hexc("#b81f06")]

def flame_col(frac):
    return FLAME[min(4, int(frac * 5))]

def paint_flame(fx, fy, fw, fh):
    """flame tongues rising from the bottom row, transparent elsewhere"""
    phase = random.uniform(0, 6.28)
    for i in range(fw):
        x = (i + 0.5) / fw * 2 - 1
        body = (1 - abs(x)) ** 0.7
        tongue = 0.25 * (0.5 + 0.5 * np.sin(i * 1.9 + phase)) + random.uniform(-0.1, 0.12)
        hcol = fh * max(0.15, min(1.0, 0.25 + 0.65 * body + tongue))
        for j in range(fh):
            hb = fh - 1 - j
            if hb < hcol:
                f = hb / hcol
                if f > 0.7 and random.random() < 0.25:
                    continue                          # flickering gaps near the tips
                put(fx + i, fy + j, flame_col(f * 0.95 + (0.1 if abs(x) > 0.6 else 0)), g=True)
        # stray sparks above the flame
        if random.random() < 0.25:
            sj = int(fh - 1 - hcol - random.randint(1, 3))
            if 0 <= sj < fh:
                put(fx + i, fy + sj, FLAME[2], g=True)

def paint_blade_flame(fx, fy, fw, fh):
    """flames licking out from both edges of the blade (blade occupies the middle columns)"""
    for j in range(fh):
        for side in (0, 1):
            ln = (1 + (j * 7 + side * 3) % 3 // 2 + (1 if random.random() < 0.3 else 0)) if j > 1 else 0
            for k in range(ln):
                i = k if side == 0 else fw - 1 - k
                put(fx + i, fy + j, FLAME[3 - k] if k < 2 else FLAME[4], g=True)

def paint_face(mat, tag, fname, rect):
    fx, fy, fw, fh = rect
    if fw == 0 or fh == 0:
        return
    if mat == "flame":
        if fname not in ("up", "down"):
            paint_flame(fx, fy, fw, fh)
        return
    if mat == "flame_blade":
        if fname in ("east", "west"):
            paint_blade_flame(fx, fy, fw, fh)
        return
    base, hi, lo = (hexc(c) for c in PAL[mat])
    for j in range(fh):
        for i in range(fw):
            n = random.uniform(-7, 7)
            c = base + n
            if mat == "cape":
                c = base * (1 - 0.35 * j / max(1, fh - 1)) + n
            if mat == "horn_tip":
                c = hi * (1 - j / max(1, fh)) + base * (j / max(1, fh))
            if j == 0 and fname not in ("up", "down"):
                c = hi                               # top highlight
            if (i == 0 or i == fw - 1 or j == fh - 1) and mat not in ("cape", "horn_tip"):
                c = lo if fw > 2 else c              # edge outline
            put(fx + i, fy + j, c, g=mat in GLOW_MATS)

    # ---- details
    if mat == "iron_crack" and fname in ("north", "east", "west", "south"):
        crack(fx, fy, fw, fh, 2, 5)
    # molten seams: the lower rim of overlapping plates glows dimly
    if tag in ("lame", "tasset", "pauldron1", "pauldron2", "pauldron3", "collar", "greave_rim", "cuff") \
            and fname not in ("up", "down"):
        for i in range(1, fw - 1):
            r = random.random()
            if r < 0.45:
                put(fx + i, fy + fh - 1, hexc("#6e2207"), g=True)
            elif r < 0.55:
                put(fx + i, fy + fh - 1, EMBER[0], g=True)
    if tag == "helm" and fname == "north":
        for j in (4, 5):                             # visor slit with burning eyes
            for i in range(fw):
                put(fx + i, fy + j, hexc("#09080b"))
            for i in (1, 2, fw - 3, fw - 2):
                put(fx + i, fy + j, hexc("#ffe066") if j == 4 else hexc("#ff8a1e"), g=True)
    if tag == "brow" and fname == "down":
        for i in range(fw):                          # eye glow lighting the underside of the brow
            put(fx + i, fy + fh - 1, hexc("#7a2a0a"), g=True)
    if tag == "faceplate" and fname == "north":
        for i in (2, 4, 6):                          # breathing holes leaking embers
            put(fx + i, fy + 1, EMBER[0], g=True)
    if mat == "chest" and fname == "north":
        cx = fw // 2
        for j in range(fh):                         # cracks radiating from the core
            for side in (-1, 1):
                px = cx + side * (2 + j // 2 + (j % 3 == 0))
                if 0 < px < fw - 1 and j not in (0, fh - 1):
                    put(fx + px, fy + j, EMBER[j % 3], g=True)
        crack(fx, fy + 1, fw, fh - 2, 2, 4)
    if mat == "chest" and fname in ("east", "west", "south"):
        crack(fx, fy, fw, fh, 1, 5)
    if mat == "cape":
        for i in range(fw):                          # ragged, singed bottom edge
            rag = random.choice([0, 1, 1, 2, 3, 4])
            for j in range(fh - rag, fh):
                tex[fy + j, fx + i, 3] = 0
            if fh - rag - 1 >= 0 and random.random() < 0.6:
                put(fx + i, fy + fh - rag - 1, EMBER[random.randrange(2)], g=True)
    if mat == "blade" and fname in ("east", "west"):
        for j in range(fh):                          # steel edges + molten core line
            put(fx, fy + j, hexc("#8a8796"))
            put(fx + fw - 1, fy + j, hexc("#8a8796"))
            if fw >= 4:
                mid = fw // 2
                put(fx + mid, fy + j, EMBER[0] if j % 6 else EMBER[1], g=True)
    if mat == "grip" and fname not in ("up", "down"):
        for j in range(0, fh, 2):
            for i in range(fw):
                put(fx + i, fy + j, hexc("#1c1009"))

flat = []  # for renderer
for i, (bone, cube) in enumerate(all_cubes):
    o, s, mat, tag = cube
    u0, v0 = uv[i]
    for f in faces(o, s, u0, v0):
        paint_face(mat, tag, f[0], f[4])
        flat.append((bone["name"], f, mat))

os.makedirs(f"{OUT}/textures/entity", exist_ok=True)
os.makedirs(f"{OUT}/geo/entity", exist_ok=True)
Image.fromarray(tex).save(f"{OUT}/textures/entity/fire_knight.png")
Image.fromarray(glow).save(f"{OUT}/textures/entity/fire_knight_glowmask.png")

# ---------------------------------------------------------------- geo.json
def num(v):
    return int(v) if float(v).is_integer() else v

geo_bones = []
for b in BONES:
    gb = {"name": b["name"], "pivot": [num(v) for v in b["pivot"]]}
    if "parent" in b: gb["parent"] = b["parent"]
    if "rotation" in b: gb["rotation"] = b["rotation"]
    cubes = []
    for i, (bb, c) in enumerate(all_cubes):
        if bb is b:
            cubes.append({"origin": [num(v) for v in c[0]], "size": c[1], "uv": list(uv[i])})
    if cubes: gb["cubes"] = cubes
    geo_bones.append(gb)
geo = {"format_version": "1.12.0", "minecraft:geometry": [{
    "description": {"identifier": "geometry.fire_knight", "texture_width": TEX_W, "texture_height": TEX_H,
                    "visible_bounds_width": 6, "visible_bounds_height": 5, "visible_bounds_offset": [0, 2, 0]},
    "bones": geo_bones}]}
with open(f"{OUT}/geo/entity/fire_knight.geo.json", "w") as fp:
    json.dump(geo, fp, indent=1)

json.dump({"bones": BONES, "flat": [(n, list(f), m) for n, f, m in flat]},
          open("tools/fk_faces.json", "w"))
print("cubes", len(all_cubes), "texture rows used", USED_H)
