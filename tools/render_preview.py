"""Orthographic preview renderer for the generated geo + texture."""
import json, math, sys
import numpy as np
from PIL import Image, ImageFilter, ImageDraw

SCR = "tools"
A = "src/main/resources/assets/jsnboss"
tex = glw = data = bones = None

def load(name="fire_knight", texture=None):
    """name のモデル（tools/<name>_faces.json）とテクスチャを読み込む"""
    global tex, glw, data, bones
    texture = texture or name
    tex = np.array(Image.open(f"{A}/textures/entity/{texture}.png").convert("RGBA")).astype(float)
    glw = np.array(Image.open(f"{A}/textures/entity/{texture}_glowmask.png").convert("RGBA")).astype(float)
    data = json.load(open(f"{SCR}/{name}_faces.json"))
    bones = {b["name"]: b for b in data["bones"]}

load()

def rot(rx, ry, rz):
    ax, ay, az = math.radians(-rx), math.radians(ry), math.radians(rz)
    Rx = np.array([[1, 0, 0], [0, math.cos(ax), -math.sin(ax)], [0, math.sin(ax), math.cos(ax)]])
    Ry = np.array([[math.cos(ay), 0, math.sin(ay)], [0, 1, 0], [-math.sin(ay), 0, math.cos(ay)]])
    Rz = np.array([[math.cos(az), -math.sin(az), 0], [math.sin(az), math.cos(az), 0], [0, 0, 1]])
    return Rz @ Ry @ Rx

POSE = {}  # bone -> {"rotation": [..], "position": [..], "scale": [..]} (animation offsets)

def bone_xf(name):
    b = bones[name]
    M, t = np.eye(3), np.zeros(3)
    if "parent" in b:
        M, t = bone_xf(b["parent"])
    ps = POSE.get(name, {})
    r0 = b.get("rotation", [0, 0, 0]); ra = ps.get("rotation", [0, 0, 0])
    R = rot(*[r0[i] + ra[i] for i in range(3)]) @ np.diag(ps.get("scale", [1, 1, 1]))
    p = np.array(b["pivot"], float)
    off = np.array(ps.get("position", [0, 0, 0]), float) * [-1, 1, 1]
    # local: x -> R(x - p) + p + off ; then parent
    return M @ R, M @ (p - R @ p + off) + t

FLIP = np.diag([-1.0, 1, 1])  # bedrock geo -> in-game (mirrored X)

def render(yaw, pitch, scale, extra=(), bounds=None):
    cy, sy = math.cos(math.radians(yaw)), math.sin(math.radians(yaw))
    cp, sp = math.cos(math.radians(pitch)), math.sin(math.radians(pitch))
    Yr = np.array([[cy, 0, sy], [0, 1, 0], [-sy, 0, cy]])
    Xr = np.array([[1, 0, 0], [0, cp, -sp], [0, sp, cp]])
    V = Xr @ Yr
    quads = []
    for bname, f, mat in data["flat"]:
        M, t = bone_xf(bname)
        _, Apt, S, T, rect, n = f
        Aw = FLIP @ (M @ np.array(Apt, float) + t)
        Sw, Tw, nw = FLIP @ M @ np.array(S, float), FLIP @ M @ np.array(T, float), FLIP @ M @ np.array(n, float)
        quads.append((V @ Aw, V @ Sw, V @ Tw, V @ nw, nw, rect, None))
    for (Apt, S, T, n, col) in extra:
        quads.append((V @ np.array(Apt, float), V @ np.array(S, float), V @ np.array(T, float),
                      V @ np.array(n, float), np.array(n, float), None, col))
    pts = np.array([q[0] for q in quads] + [q[0] + q[1] + q[2] for q in quads])
    if bounds:
        minx, maxx, miny, maxy = bounds
    else:
        minx, maxx = (-pts[:, 0]).min() - 6, (-pts[:, 0]).max() + 6
        miny, maxy = (-pts[:, 1]).min() - 6, (-pts[:, 1]).max() + 4
    W, H = int((maxx - minx) * scale), int((maxy - miny) * scale)
    img = np.zeros((H, W, 3)); zb = np.full((H, W), np.inf); gl = np.zeros((H, W, 3))
    L = np.array([-0.45, 0.8, -0.4]); L /= np.linalg.norm(L)
    for (Av, Sv, Tv, nv, nworld, rect, col) in quads:
        if nv[2] > 0.02:  # back-facing (camera looks +Z)
            continue
        to2 = lambda v: np.array([-v[0], -v[1]])
        a2 = (to2(Av) - [minx, miny]) * scale
        s2, t2 = to2(Sv) * scale, to2(Tv) * scale
        Mx = np.array([s2, t2]).T
        if abs(np.linalg.det(Mx)) < 1e-6:
            continue
        inv = np.linalg.inv(Mx)
        corners = np.array([a2, a2 + s2, a2 + t2, a2 + s2 + t2])
        x0, y0 = np.floor(corners.min(0)).astype(int); x1, y1 = np.ceil(corners.max(0)).astype(int)
        x0, y0 = max(x0, 0), max(y0, 0); x1, y1 = min(x1, W), min(y1, H)
        if x0 >= x1 or y0 >= y1:
            continue
        gx, gy = np.meshgrid(np.arange(x0, x1) + 0.5, np.arange(y0, y1) + 0.5)
        d = np.stack([gx - a2[0], gy - a2[1]], -1) @ inv.T
        s, t = d[..., 0], d[..., 1]
        m = (s >= 0) & (s < 1) & (t >= 0) & (t < 1)
        z = Av[2] + s * Sv[2] + t * Tv[2]
        shade = 0.5 + 0.5 * max(0.0, float(nworld @ L) / max(1e-9, np.linalg.norm(nworld)))
        if rect is not None:
            tu, tv, tw, th = rect
            u = np.clip((tu + np.floor(s * tw)).astype(int), 0, tex.shape[1] - 1)
            v = np.clip((tv + np.floor(t * th)).astype(int), 0, tex.shape[0] - 1)
            texel = tex[v, u]; gtex = glw[v, u]
            m &= texel[..., 3] > 0
            colr = texel[..., :3] * shade
            isg = gtex[..., 3] > 0
            colr[isg] = gtex[..., :3][isg]
        else:
            colr = np.broadcast_to(np.array(col, float) * shade, s.shape + (3,)).copy(); isg = np.zeros_like(m)
        sub = zb[y0:y1, x0:x1]
        upd = m & (z < sub)
        sub[upd] = z[upd]
        img[y0:y1, x0:x1][upd] = colr[upd]
        g = gl[y0:y1, x0:x1]; g[upd] = 0
        gsel = upd & isg
        g[gsel] = colr[gsel]
    cover = np.isfinite(zb)
    return img, gl, cover, (minx, miny), scale

def box(o, s, col):
    """flat-colored cube in game space (already mirrored), for the player reference"""
    x0, y0, z0 = o; w, h, d = s; x1, y1, z1 = x0 + w, y0 + h, z0 + d
    return [((x0, y1, z0), (w, 0, 0), (0, -h, 0), (0, 0, -1), col),
            ((x1, y1, z1), (-w, 0, 0), (0, -h, 0), (0, 0, 1), col),
            ((x0, y1, z1), (0, 0, -d), (0, -h, 0), (-1, 0, 0), col),
            ((x1, y1, z0), (0, 0, d), (0, -h, 0), (1, 0, 0), col),
            ((x0, y1, z1), (w, 0, 0), (0, 0, -d), (0, 1, 0), col)]

def player(ox):
    c1, c2, c3 = (95, 140, 170), (70, 95, 160), (205, 160, 120)
    q = []
    q += box((ox - 4, 0, -2), (4, 12, 4), c2) + box((ox, 0, -2), (4, 12, 4), c2)
    q += box((ox - 4, 12, -2), (8, 12, 4), c1)
    q += box((ox - 8, 12, -2), (4, 12, 4), c3) + box((ox + 4, 12, -2), (4, 12, 4), c3)
    q += box((ox - 4, 24, -4), (8, 8, 8), c3)
    return q

def to_image(img, gl, cover, bg_top=(28, 24, 34), bg_bot=(64, 40, 34)):
    H, W, _ = img.shape
    grad = np.linspace(0, 1, H)[:, None, None]
    bg = np.array(bg_top) * (1 - grad) + np.array(bg_bot) * grad
    out = np.where(cover[..., None], img, np.broadcast_to(bg, img.shape))
    bloom = np.array(Image.fromarray(np.clip(gl, 0, 255).astype(np.uint8)).filter(ImageFilter.GaussianBlur(6))).astype(float)
    out = np.clip(out + bloom * 0.7, 0, 255)
    return Image.fromarray(out.astype(np.uint8))

if __name__ == "__main__":
    if len(sys.argv) > 2:
        load(sys.argv[2])
    S = 7
    views = [(0, 6, "正面"), (-35, 14, "ななめ前"), (155, 10, "うしろ"), (90, 6, "よこ")]
    imgs = []
    for yaw, pitch, label in views:
        extra = player(-34) if label == "ななめ前" else ()
        im, gl, cv, _, _ = render(yaw, pitch, S, extra)
        imgs.append(to_image(im, gl, cv))
    Hm = max(i.height for i in imgs)
    sheet = Image.new("RGB", (sum(i.width for i in imgs) + 20 * (len(imgs) + 1), Hm + 40), (20, 18, 24))
    x = 20
    for i in imgs:
        sheet.paste(i, (x, 20 + Hm - i.height)); x += i.width + 20
    sheet.save(sys.argv[1] if len(sys.argv) > 1 else f"{SCR}/preview.png")
    print(sheet.size)
