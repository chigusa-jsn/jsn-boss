"""ボスのモデル作成用の共通ツール。

使い方:
    m = Model("ice_witch", 256, 256, seed=3)
    m.bone("root", [0, 0, 0])
    m.bone("body", [0, 24, 0], parent="root", cubes=[(origin, size, material, tag), ...])
    m.paint(palette, details)   # details(m, mat, tag, face, rect) で顔などを描き込む
    m.save()

座標は Bedrock ジオメトリ（1 = 1px = 1/16 ブロック、Y が上、正面は -Z）。
"""
import json, os, random
import numpy as np
from PIL import Image

ASSETS = "src/main/resources/assets/jsnboss"
TOOLS = "tools"


def mirror(cubes):
    """右側(-X)のパーツを左側(+X)に反転コピー"""
    return [([-(o[0] + s[0]), o[1], o[2]], s, m, t) for (o, s, m, t) in cubes]


def hexc(h):
    h = h.lstrip("#")
    return np.array([int(h[i:i + 2], 16) for i in (0, 2, 4)], float)


def faces(o, s, u0, v0):
    """(name, A, S, T, (tu, tv, tw, th), normal) — Bedrock の box UV と同じ並び"""
    w, h, d = s
    x0, y0, z0 = o; x1, y1, z1 = x0 + w, y0 + h, z0 + d
    yield ("north", (x0, y1, z0), (w, 0, 0), (0, -h, 0), (u0 + d, v0 + d, w, h), (0, 0, -1))
    yield ("south", (x1, y1, z1), (-w, 0, 0), (0, -h, 0), (u0 + 2 * d + w, v0 + d, w, h), (0, 0, 1))
    yield ("west", (x0, y1, z1), (0, 0, -d), (0, -h, 0), (u0, v0 + d, d, h), (-1, 0, 0))
    yield ("east", (x1, y1, z0), (0, 0, d), (0, -h, 0), (u0 + d + w, v0 + d, d, h), (1, 0, 0))
    yield ("up", (x0, y1, z1), (w, 0, 0), (0, 0, -d), (u0 + d, v0, w, d), (0, 1, 0))
    yield ("down", (x0, y0, z1), (w, 0, 0), (0, 0, -d), (u0 + d + w, v0, w, d), (0, -1, 0))


class Model:
    def __init__(self, name, tw=256, th=256, seed=1):
        self.name, self.tw, self.th = name, tw, th
        self.bones = []
        self.tex = np.zeros((th, tw, 4), np.uint8)
        self.glow = np.zeros((th, tw, 4), np.uint8)
        self.rng = random.Random(seed)

    # ------------------------------------------------------------ structure
    def bone(self, name, pivot, parent=None, cubes=(), rotation=None):
        b = dict(name=name, pivot=list(pivot), cubes=list(cubes))
        if parent: b["parent"] = parent
        if rotation: b["rotation"] = list(rotation)
        self.bones.append(b)
        return b

    def _pack(self):
        self.all = [(b, c) for b in self.bones for c in b["cubes"]]
        order = sorted(range(len(self.all)), key=lambda i: -(self.all[i][1][1][2] + self.all[i][1][1][1]))
        self.uv, x, y, sh = {}, 0, 0, 0
        for i in order:
            w, h, d = self.all[i][1][1]
            bw, bh = 2 * (w + d), d + h
            if x + bw > self.tw:
                x, y, sh = 0, y + sh + 1, 0
            self.uv[i] = (x, y)
            x += bw + 1
            sh = max(sh, bh)
        assert y + sh <= self.th, f"texture overflow ({y + sh} > {self.th})"

    # ------------------------------------------------------------ painting
    def put(self, x, y, col, a=255, glow=False):
        self.tex[y, x, :3] = np.clip(col, 0, 255); self.tex[y, x, 3] = a
        if glow:
            self.glow[y, x, :3] = np.clip(col, 0, 255); self.glow[y, x, 3] = 255

    def clear(self, x, y):
        self.tex[y, x] = 0; self.glow[y, x] = 0

    def paint(self, palette, details=None, glow_mats=(), outline_skip=(), soft=None):
        """palette: mat -> (base, highlight, outline)。details(m, mat, tag, face, rect) で描き足す
        soft = {"mats": {...}, "y": (下端, 上端), "dark": 0.7}: その素材はふちの線を描かず、
        体全体の高さで上が明るく下が暗いなめらかな陰影にする（パーツの区切りが目立たない）"""
        self._pack()
        self.flat = []
        for i, (b, (o, s, mat, tag)) in enumerate(self.all):
            u0, v0 = self.uv[i]
            for f in faces(o, s, u0, v0):
                fname, rect = f[0], f[4]
                fx, fy, fw, fh = rect
                if fw and fh and mat in palette:
                    base, hi, lo = (hexc(c) for c in palette[mat])
                    if soft and mat in soft["mats"]:
                        y0, y1 = soft["y"]
                        for j in range(fh):
                            if fname == "up":
                                wy = o[1] + s[1]
                            elif fname == "down":
                                wy = o[1]
                            else:
                                wy = o[1] + s[1] - (j + 0.5) * s[1] / fh
                            f_ = min(1.0, max(0.0, (wy - y0) / (y1 - y0)))
                            shade = soft.get("dark", 0.7) + (1 - soft.get("dark", 0.7)) * f_
                            for k in range(fw):
                                c = base * shade + self.rng.uniform(-4, 4)
                                if fname == "down":
                                    c = c * 0.8
                                self.put(fx + k, fy + j, c, glow=mat in glow_mats)
                        if details:
                            details(self, mat, tag, fname, rect)
                        self.flat.append((b["name"], f, mat))
                        continue
                    for j in range(fh):
                        for k in range(fw):
                            c = base + self.rng.uniform(-6, 6)
                            if j == 0 and fname not in ("up", "down"):
                                c = hi
                            if (k == 0 or k == fw - 1 or j == fh - 1) and mat not in outline_skip and fw > 2:
                                c = lo
                            self.put(fx + k, fy + j, c, glow=mat in glow_mats)
                if fw and fh and details:
                    details(self, mat, tag, fname, rect)
                self.flat.append((b["name"], f, mat))

    # ------------------------------------------------------------ output
    def save(self):
        os.makedirs(f"{ASSETS}/textures/entity", exist_ok=True)
        os.makedirs(f"{ASSETS}/geo/entity", exist_ok=True)
        Image.fromarray(self.tex).save(f"{ASSETS}/textures/entity/{self.name}.png")
        Image.fromarray(self.glow).save(f"{ASSETS}/textures/entity/{self.name}_glowmask.png")

        def num(v):
            return int(v) if float(v).is_integer() else v

        geo_bones = []
        for b in self.bones:
            gb = {"name": b["name"], "pivot": [num(v) for v in b["pivot"]]}
            if "parent" in b: gb["parent"] = b["parent"]
            if "rotation" in b: gb["rotation"] = b["rotation"]
            cubes = [{"origin": [num(v) for v in c[0]], "size": list(c[1]), "uv": list(self.uv[i])}
                     for i, (bb, c) in enumerate(self.all) if bb is b]
            if cubes: gb["cubes"] = cubes
            geo_bones.append(gb)
        geo = {"format_version": "1.12.0", "minecraft:geometry": [{
            "description": {"identifier": f"geometry.{self.name}", "texture_width": self.tw,
                            "texture_height": self.th, "visible_bounds_width": 6,
                            "visible_bounds_height": 5, "visible_bounds_offset": [0, 2, 0]},
            "bones": geo_bones}]}
        with open(f"{ASSETS}/geo/entity/{self.name}.geo.json", "w") as fp:
            json.dump(geo, fp, indent=1)
        with open(f"{TOOLS}/{self.name}_faces.json", "w") as fp:
            json.dump({"bones": self.bones, "flat": [(n, list(f), m) for n, f, m in self.flat]}, fp)
        print(self.name, "cubes:", len(self.all))
