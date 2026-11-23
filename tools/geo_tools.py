"""ボスのモデル作成用の共通ツール。

使い方:
    m = Model("ice_witch", 256, 256, seed=3)
    m.bone("root", [0, 0, 0])
    m.bone("body", [0, 24, 0], parent="root", cubes=[(origin, size, material, tag), ...])
    m.paint(palette, details)   # details(m, mat, tag, face, rect) で顔などを描き込む
    m.save()

座標は Bedrock ジオメトリ（1 = 1px = 1/16 ブロック、Y が上、正面は -Z）。

細かいモデル用の追加機能:
  - キューブごとの傾き：キューブの5番目に {"rot": [x, y, z], "pivot": [...]} を付ける（pivot を省くとキューブの中心）
  - density=2 などにすると、1px あたり density×density 個のテクスチャのピクセルを使う（面ごとの UV で書き出す）
"""
import json, math, os, random
import numpy as np
from PIL import Image

ASSETS = "src/main/resources/assets/jsnboss"
TOOLS = "tools"


def mirror(cubes):
    """右側(-X)のパーツを左側(+X)に反転コピー（傾きも左右反転）"""
    out = []
    for c in cubes:
        o, s, m, t = c[:4]
        n = ([-(o[0] + s[0]), o[1], o[2]], s, m, t)
        if len(c) > 4 and c[4]:
            x = dict(c[4])
            if "rot" in x:
                x["rot"] = [x["rot"][0], -x["rot"][1], -x["rot"][2]]
            if "pivot" in x:
                x["pivot"] = [-x["pivot"][0], x["pivot"][1], x["pivot"][2]]
            n = n + (x,)
        out.append(n)
    return out


def rot(rx, ry, rz):
    """render_preview.py と同じ回転（Bedrock / GeckoLib の骨とキューブの回転）"""
    ax, ay, az = math.radians(-rx), math.radians(ry), math.radians(rz)
    Rx = np.array([[1, 0, 0], [0, math.cos(ax), -math.sin(ax)], [0, math.sin(ax), math.cos(ax)]])
    Ry = np.array([[math.cos(ay), 0, math.sin(ay)], [0, 1, 0], [-math.sin(ay), 0, math.cos(ay)]])
    Rz = np.array([[math.cos(az), -math.sin(az), 0], [math.sin(az), math.cos(az), 0], [0, 0, 1]])
    return Rz @ Ry @ Rx


def cube_extra(c):
    return c[4] if len(c) > 4 and c[4] else {}


def cube_pivot(c):
    o, s = c[0], c[1]
    x = cube_extra(c)
    return x.get("pivot", [o[0] + s[0] / 2, o[1] + s[1] / 2, o[2] + s[2] / 2])


def hexc(h):
    h = h.lstrip("#")
    return np.array([int(h[i:i + 2], 16) for i in (0, 2, 4)], float)


def tdims(s, D):
    """テクスチャ上の大きさ（density 倍、最低1ピクセル）"""
    if D == 1:
        return s
    return [max(1, int(round(v * D))) for v in s]


def faces(o, s, u0, v0, D=1):
    """(name, A, S, T, (tu, tv, tw, th), normal) — Bedrock の box UV と同じ並び"""
    w, h, d = s
    W, H, Dd = tdims(s, D)
    x0, y0, z0 = o; x1, y1, z1 = x0 + w, y0 + h, z0 + d
    yield ("north", (x0, y1, z0), (w, 0, 0), (0, -h, 0), (u0 + Dd, v0 + Dd, W, H), (0, 0, -1))
    yield ("south", (x1, y1, z1), (-w, 0, 0), (0, -h, 0), (u0 + 2 * Dd + W, v0 + Dd, W, H), (0, 0, 1))
    yield ("west", (x0, y1, z1), (0, 0, -d), (0, -h, 0), (u0, v0 + Dd, Dd, H), (-1, 0, 0))
    yield ("east", (x1, y1, z0), (0, 0, d), (0, -h, 0), (u0 + Dd + W, v0 + Dd, Dd, H), (1, 0, 0))
    yield ("up", (x0, y1, z1), (w, 0, 0), (0, 0, -d), (u0 + Dd, v0, W, Dd), (0, 1, 0))
    yield ("down", (x0, y0, z1), (w, 0, 0), (0, 0, -d), (u0 + Dd + W, v0, W, Dd), (0, -1, 0))


def rotated_face(f, c):
    """キューブの傾きを面（A, S, T, 法線）に反映する（プレビュー用。骨の座標系の中で）"""
    x = cube_extra(c)
    if "rot" not in x:
        return f
    R = rot(*x["rot"])
    p = np.array(cube_pivot(c), float)
    name, A, S, T, rect, n = f
    A2 = R @ (np.array(A, float) - p) + p
    return (name, tuple(A2.tolist()), tuple((R @ np.array(S, float)).tolist()),
            tuple((R @ np.array(T, float)).tolist()), rect, tuple((R @ np.array(n, float)).tolist()))


class Model:
    def __init__(self, name, tw=256, th=256, seed=1, density=1):
        self.name, self.tw, self.th, self.D = name, tw, th, density
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
        dims = [tdims(c[1], self.D) for _, c in self.all]
        order = sorted(range(len(self.all)), key=lambda i: -(dims[i][2] + dims[i][1]))
        self.uv, x, y, sh = {}, 0, 0, 0
        for i in order:
            w, h, d = dims[i]
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
        for i, (b, cube) in enumerate(self.all):
            o, s, mat, tag = cube[:4]
            u0, v0 = self.uv[i]
            for f0 in faces(o, s, u0, v0, self.D):
                f = rotated_face(f0, cube)
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
                    if self.D > 1 and mat not in outline_skip:
                        # 細かいモデル：金属のような上から下へのグラデーションと、ふちの明暗
                        for j in range(fh):
                            for k in range(fw):
                                g = 1.08 - 0.22 * (j / max(1, fh - 1)) if fname not in ("up", "down") else (1.1 if fname == "up" else 0.75)
                                c = base * g + self.rng.uniform(-4, 4)
                                if j == 0 and fname not in ("up", "down"):
                                    c = hi
                                if (k == 0 or k == fw - 1 or j == fh - 1) and fw > 2 and fh > 2:
                                    c = lo
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
            cubes = []
            for i, (bb, c) in enumerate(self.all):
                if bb is not b:
                    continue
                gc = {"origin": [num(v) for v in c[0]], "size": [num(v) for v in c[1]]}
                if self.D == 1:
                    gc["uv"] = list(self.uv[i])
                else:
                    u0, v0 = self.uv[i]
                    fu = {}
                    for f in faces(c[0], c[1], u0, v0, self.D):
                        fx, fy, fw, fh = f[4]
                        if f[0] == "down":      # box UV と同じく、下の面は上下反転
                            fu["down"] = {"uv": [fx, fy + fh], "uv_size": [fw, -fh]}
                        else:
                            fu[f[0]] = {"uv": [fx, fy], "uv_size": [fw, fh]}
                    gc["uv"] = fu
                x = cube_extra(c)
                if "rot" in x:
                    gc["rotation"] = list(x["rot"])
                    gc["pivot"] = [num(v) for v in cube_pivot(c)]
                cubes.append(gc)
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
            json.dump({"bones": [{k: v for k, v in b.items() if k != "cubes"} for b in self.bones],
                       "flat": [(n, list(f), m) for n, f, m in self.flat]}, fp)
        print(self.name, "cubes:", len(self.all))
