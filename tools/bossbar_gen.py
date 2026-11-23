"""ボス専用ボスバーのテクスチャを作る。

1 枚 512x64（GUI では 240x32 で表示。2 倍解像度）。
  - フレーム       : x 0..480,  y 0..64   （GUI 240x32）
      バーの窓     : GUI (36,13)-(234,20)  198x7  … 透明にしておき、下にバーを描く
      残りバーの玉 : GUI (40 + i*8, 25) 6x6
  - 満タン色の帯   : x 484..500, y 0..14
  - 空の帯         : x 484..500, y 16..30
  - 残りバーの玉   : x 484..496, y 32..44

使い方:  python3 bossbar_gen.py [boss ...]
出力:    src/main/resources/assets/jsnboss/textures/gui/bossbar/<boss>_<stage>.png
"""
import math, os, random, sys
from PIL import Image, ImageDraw, ImageFilter

OUT = os.path.join(os.path.dirname(__file__), "..", "src/main/resources/assets/jsnboss/textures/gui/bossbar")
S = 8                      # 作業キャンバスは GUI の 8 倍。最後に 1/4 に縮めて 2 倍解像度にする
W, H = 240, 32

PLATE = (31, 8, 238, 25)   # 板
WIN = (36, 13, 234, 20)    # バーの窓
EMB = (17, 16, 15.5)       # 紋章の中心と半径


def hexc(h, a=255):
    h = h.lstrip("#")
    return (int(h[0:2], 16), int(h[2:4], 16), int(h[4:6], 16), a)


def mix(a, b, t):
    return tuple(int(a[i] + (b[i] - a[i]) * t) for i in range(4))


class Canvas:
    def __init__(self):
        self.img = Image.new("RGBA", (W * S, H * S), (0, 0, 0, 0))

    # ---------------------------------------------------------------- 基本
    def _pts(self, pts):
        return [(x * S, y * S) for x, y in pts]

    def layer(self):
        return Image.new("RGBA", self.img.size, (0, 0, 0, 0))

    def paste(self, lay):
        self.img = Image.alpha_composite(self.img, lay)

    def poly(self, pts, fill, outline=None, width=0.0):
        lay = self.layer(); d = ImageDraw.Draw(lay)
        d.polygon(self._pts(pts), fill=fill)
        if outline:
            d.line(self._pts(pts + [pts[0]]), fill=outline, width=max(1, int(width * S)), joint="curve")
        self.paste(lay)

    def grad_mask(self, mask, top, bot, y0, y1):
        g = Image.new("RGBA", self.img.size)
        gd = ImageDraw.Draw(g)
        for y in range(int(y0 * S), int(y1 * S) + 1):
            t = (y - y0 * S) / max(1, (y1 - y0) * S)
            gd.line([(0, y), (g.width, y)], fill=mix(top, bot, min(1, max(0, t))))
        lay = self.layer(); lay.paste(g, (0, 0), mask)
        self.paste(lay)

    def grad_poly(self, pts, top, bot):
        m = Image.new("L", self.img.size, 0)
        ImageDraw.Draw(m).polygon(self._pts(pts), fill=255)
        ys = [p[1] for p in pts]
        self.grad_mask(m, top, bot, min(ys), max(ys))

    def rrect(self, box, r, fill=None, outline=None, width=0.0, grad=None):
        x0, y0, x1, y1 = box
        if grad:
            m = Image.new("L", self.img.size, 0)
            ImageDraw.Draw(m).rounded_rectangle([x0 * S, y0 * S, x1 * S, y1 * S], radius=r * S, fill=255)
            self.grad_mask(m, grad[0], grad[1], y0, y1)
        lay = self.layer(); d = ImageDraw.Draw(lay)
        d.rounded_rectangle([x0 * S, y0 * S, x1 * S, y1 * S], radius=r * S, fill=fill,
                            outline=outline, width=max(1, int(width * S)) if outline else 0)
        self.paste(lay)

    def circle(self, cx, cy, r, fill=None, outline=None, width=0.0, grad=None):
        if grad:
            m = Image.new("L", self.img.size, 0)
            ImageDraw.Draw(m).ellipse([(cx - r) * S, (cy - r) * S, (cx + r) * S, (cy + r) * S], fill=255)
            self.grad_mask(m, grad[0], grad[1], cy - r, cy + r)
        lay = self.layer(); d = ImageDraw.Draw(lay)
        d.ellipse([(cx - r) * S, (cy - r) * S, (cx + r) * S, (cy + r) * S], fill=fill,
                  outline=outline, width=max(1, int(width * S)) if outline else 0)
        self.paste(lay)

    def line(self, pts, color, width):
        lay = self.layer(); d = ImageDraw.Draw(lay)
        d.line(self._pts(pts), fill=color, width=max(1, int(width * S)), joint="curve")
        for x, y in pts:   # 端を丸く
            r = width * S / 2
            d.ellipse([x * S - r, y * S - r, x * S + r, y * S + r], fill=color)
        self.paste(lay)

    def glow(self, draw_fn, color, blur=2.0, strength=1.0):
        """draw_fn(d) で描いた形をぼかして光にする（下に敷く）"""
        lay = self.layer(); d = ImageDraw.Draw(lay)
        draw_fn(d, color)
        lay = lay.filter(ImageFilter.GaussianBlur(blur * S))
        if strength != 1.0:
            a = lay.getchannel("A").point(lambda v: min(255, int(v * strength)))
            lay.putalpha(a)
        self.paste(lay)

    def clear(self, box):
        x0, y0, x1, y1 = box
        a = self.img.getchannel("A")
        ImageDraw.Draw(a).rectangle([x0 * S, y0 * S, x1 * S - 1, y1 * S - 1], fill=0)
        self.img.putalpha(a)

    def finish(self):
        return self.img.resize((W * 2, H * 2), Image.LANCZOS)


# ============================================================ 共通の土台
def base_plate(c, p, plate_fn=None):
    x0, y0, x1, y1 = PLATE
    # 影
    c.glow(lambda d, col: d.rounded_rectangle([(x0) * S, (y0 + 1) * S, (x1 + 1) * S, (y1 + 1.5) * S], radius=3 * S, fill=col),
           (0, 0, 0, 170), blur=1.2)
    if plate_fn:
        plate_fn(c, p)
    else:
        c.rrect((x0, y0, x1, y1), 3, grad=(p["light"], p["dark"]))
        c.rrect((x0, y0, x1, y1), 3, outline=p["edge"], width=0.6)
        c.line([(x0 + 2.5, y0 + 1.0), (x1 - 2.5, y0 + 1.0)], mix(p["light"], (255, 255, 255, 255), 0.5), 0.4)
    # 窓のくぼみ
    wx0, wy0, wx1, wy1 = WIN
    c.rrect((wx0 - 1.2, wy0 - 1.2, wx1 + 1.2, wy1 + 1.2), 1.2, fill=p["edge"])
    c.line([(wx0 - 0.6, wy1 + 0.9), (wx1 + 0.6, wy1 + 0.9)], mix(p["light"], (255, 255, 255, 255), 0.3), 0.35)


def base_emblem(c, p, ring_fn=None):
    cx, cy, r = EMB
    c.glow(lambda d, col: d.ellipse([(cx - r - 1) * S, (cy - r - 1) * S, (cx + r + 1) * S, (cy + r + 1) * S], fill=col),
           p["glow"], blur=1.6, strength=0.8)
    if ring_fn:
        ring_fn(c, p)
    else:
        c.circle(cx, cy, r, grad=(p["light"], p["dark"]))
        c.circle(cx, cy, r, outline=p["edge"], width=0.6)
        c.circle(cx, cy, r - 2.2, outline=mix(p["dark"], (0, 0, 0, 255), 0.4), width=0.5)
    c.circle(cx, cy, r - 2.8, grad=(p["bg2"], p["bg"]))
    c.circle(cx, cy, r - 2.8, outline=p["edge"], width=0.4)


def finish_window(c):
    c.clear(WIN)


def pip_orb(bd, p):
    bd.ellipse([1, 1, 46, 46], fill=p["edge"])
    for i in range(18):
        t = i / 17
        r = 19 - i
        bd.ellipse([24 - r, 22 - r + i * 0.3, 24 + r, 22 + r + i * 0.3], fill=mix(p["fill_bot"], p["fill_hi"], t))
    bd.ellipse([13, 9, 23, 17], fill=(255, 255, 255, 200))


def pip_poly(bd, p, pts, hi=None):
    """48x48 の中に多角形の玉（縁取り＋グラデ）"""
    bd.polygon(pts, fill=p["edge"])
    cx = sum(x for x, _ in pts) / len(pts); cy = sum(y for _, y in pts) / len(pts)
    for i in range(10):
        k = 1 - i * 0.075 - 0.12
        q = [(cx + (x - cx) * k, cy + (y - cy) * k - i * 0.4) for x, y in pts]
        bd.polygon(q, fill=mix(p["fill_bot"], p["fill_hi"], i / 9))
    if hi:
        bd.ellipse(hi, fill=(255, 255, 255, 190))


def strips(p, pip_fn=None):
    """満タン色の帯・空の帯・玉（512x64 の右端に置く）"""
    img = Image.new("RGBA", (28, 64), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    for y in range(14):                         # 満タン
        if y < 2:
            col = mix(p["fill_hi"], p["fill_top"], y / 2)
        else:
            col = mix(p["fill_top"], p["fill_bot"], (y - 2) / 11)
        if y == 13:
            col = mix(p["fill_bot"], (0, 0, 0, 255), 0.35)
        d.line([(0, y), (15, y)], fill=col)
    for y in range(14):                         # 空
        col = mix(p["empty"], (0, 0, 0, 255), 0.1 + 0.3 * y / 13)
        if y < 2:
            col = mix(col, (0, 0, 0, 255), 0.4)
        d.line([(0, 16 + y), (15, 16 + y)], fill=col)
    # 玉（4 倍で描いて縮める）
    big = Image.new("RGBA", (48, 48), (0, 0, 0, 0)); bd = ImageDraw.Draw(big)
    (pip_fn or pip_orb)(bd, p)
    img.alpha_composite(big.resize((12, 12), Image.LANCZOS), (0, 32))
    return img


def build(boss, stage, p, emblem_fn, orn_back=None, orn_front=None, plate_fn=None, ring_fn=None, pip_fn=None):
    c = Canvas()
    if orn_back:
        orn_back(c, p, stage)
    base_plate(c, p, plate_fn)
    finish_window(c)
    base_emblem(c, p, ring_fn)
    emblem_fn(c, p, stage)
    if orn_front:
        orn_front(c, p, stage)
    finish_window(c)
    frame = c.finish()
    out = Image.new("RGBA", (512, 64), (0, 0, 0, 0))
    out.paste(frame, (0, 0))
    out.paste(strips(p, pip_fn), (484, 0))
    os.makedirs(OUT, exist_ok=True)
    out.save(os.path.join(OUT, f"{boss}_{stage + 1}.png"))
    return out


def pal(dark, light, edge, glow, bg, bg2, accent, accent2, fill_top, fill_bot, fill_hi, empty="#1a1418"):
    return {k: hexc(v) if isinstance(v, str) else v for k, v in dict(
        dark=dark, light=light, edge=edge, glow=glow, bg=bg, bg2=bg2, accent=accent, accent2=accent2,
        fill_top=fill_top, fill_bot=fill_bot, fill_hi=fill_hi, empty=empty).items()}


# ============================================================ 形の部品
def flame_pts(cx, by, w, h, lean=0.0, n=20):
    """下が丸く上がとがった炎（by が底、h が高さ）"""
    right, left = [], []
    for i in range(n + 1):
        t = i / n                                   # 0=底 1=先
        hw = w * 0.5 * (math.sin(math.pi * min(1, 0.15 + t * 0.95)) ** 0.8) * (1 - t) ** 0.35
        x = cx + lean * t * t
        y = by - t * h
        right.append((x + hw, y)); left.append((x - hw, y))
    bottom = [(cx + math.cos(a) * w * 0.42, by + math.sin(a) * w * 0.3) for a in [math.pi * k / 10 for k in range(11)]]
    return right[::-1][:-1] + [(cx + lean, by - h)] + left[1:] + bottom[::-1]


def star_pts(cx, cy, r1, r2, n, rot=0.0):
    pts = []
    for i in range(n * 2):
        r = r1 if i % 2 == 0 else r2
        a = rot + i * math.pi / n - math.pi / 2
        pts.append((cx + math.cos(a) * r, cy + math.sin(a) * r))
    return pts


def leaf_pts(x, y, ang, L, w):
    ca, sa = math.cos(ang), math.sin(ang)
    pts = []
    for i in range(13):
        t = i / 12
        px = t * L; py = math.sin(t * math.pi) * w
        pts.append((x + px * ca - py * sa, y + px * sa + py * ca))
    for i in range(12, -1, -1):
        t = i / 12
        px = t * L; py = -math.sin(t * math.pi) * w
        pts.append((x + px * ca - py * sa, y + px * sa + py * ca))
    return pts


# ============================================================ 炎の騎士
def fk_pal(stage):
    if stage == 0:
        return pal("#3a1c14", "#8a5a3c", "#120806", (255, 110, 30, 200), "#2a0c06", "#5a1a0a",
                   "#ffb03a", "#ff5a1a", "#ff7a2a", "#c8200c", "#ffd890")
    return pal("#141a34", "#4a5a8a", "#060812", (60, 150, 255, 210), "#060c26", "#122a5a",
               "#9ad8ff", "#3a7aff", "#5ab0ff", "#1838c8", "#d8f2ff")


def fk_flames(c, p, stage, spots):
    rnd = random.Random(7 + stage)
    for (x, by, h) in spots:
        w = h * 0.55
        lean = rnd.uniform(-1.2, 1.2)
        pts = flame_pts(x, by, w, h, lean)
        c.glow(lambda d, col, pts=pts: d.polygon([(a * S, b * S) for a, b in pts], fill=col), p["glow"], blur=1.0)
        c.grad_poly(pts, p["fill_hi"], p["accent2"])
        inner = flame_pts(x + lean * 0.1, by, w * 0.5, h * 0.55, lean * 0.6)
        c.grad_poly(inner, (255, 255, 255, 230), p["accent"])


def fk_back(c, p, stage):
    # 紋章の後ろで燃える炎の冠
    cx, cy, r = EMB
    # 右端の炎
    fk_flames(c, p, stage, [(226, 11, 8), (232, 12, 11), (237.5, 14, 8)])


def fk_emblem(c, p, stage):
    cx, cy, _ = EMB
    pts = flame_pts(cx, cy + 7, 15, 19, 0.0)
    c.glow(lambda d, col: d.polygon([(a * S, b * S) for a, b in pts], fill=col), p["glow"], blur=1.0)
    c.grad_poly(pts, p["fill_hi"], p["accent2"])
    c.grad_poly(flame_pts(cx, cy + 7, 9, 12, 0.0), (255, 255, 255, 200), p["accent"])
    # 剣
    blade = mix(p["light"], (255, 255, 255, 255), 0.75)
    c.poly([(cx - 1.1, cy - 8.5), (cx, cy - 10.5), (cx + 1.1, cy - 8.5), (cx + 1.1, cy + 4.6), (cx - 1.1, cy + 4.6)],
           blade, outline=p["edge"], width=0.35)
    c.line([(cx, cy - 9), (cx, cy + 4)], mix(blade, p["dark"], 0.3), 0.3)
    c.rrect((cx - 4.8, cy + 4.4, cx + 4.8, cy + 6.0), 0.6, fill=p["dark"], outline=p["edge"], width=0.3)
    c.rrect((cx - 0.8, cy + 6, cx + 0.8, cy + 10), 0.4, fill=p["dark"], outline=p["edge"], width=0.2)
    c.circle(cx, cy + 10.4, 1.0, fill=p["accent"], outline=p["edge"], width=0.2)


def fk_front(c, p, stage):
    spots = [(40, 10.5, 6), (45, 10.5, 8), (50, 10.5, 5), (56, 10.5, 6.5), (62, 10.5, 4),
             (200, 10.5, 4), (206, 10.5, 6.5), (212, 10.5, 5), (218, 10.5, 7)]
    if stage == 1:
        spots += [(68, 10.5, 3.5), (194, 10.5, 3.5)]
        spots = [(x, y, h * 1.2) for x, y, h in spots]
    fk_flames(c, p, stage + 10, spots)
    c.poly([(232, 9), (240, 16.5), (232, 24)], p["dark"], outline=p["edge"], width=0.5)
    c.poly([(233.5, 12), (237.5, 16.5), (233.5, 21)], p["accent2"])
    if stage == 1:
        rnd = random.Random(3)
        for _ in range(14):
            x, y = rnd.uniform(60, 230), rnd.uniform(25.5, 30)
            c.circle(x, y, rnd.uniform(0.3, 0.6), fill=p["fill_hi"])


# ============================================================ 虚空の監視者（未来的）
def es_pal(stage):
    if stage == 0:
        return pal("#14101e", "#3e3456", "#05040a", (200, 80, 255, 210), "#0a0614", "#22103a",
                   "#e080ff", "#a040ff", "#c860ff", "#6a18c8", "#f4d0ff", empty="#100c18")
    return pal("#0c1620", "#2e4a5a", "#03070a", (40, 230, 255, 220), "#04121a", "#0a2e3a",
               "#80ffff", "#20c8ff", "#40e8ff", "#0878c8", "#e0ffff", empty="#0a1418")


def es_plate(c, p):
    x0, y0, x1, y1 = PLATE
    pts = [(x0 + 2, y0), (x1 - 5, y0), (x1, y0 + 5), (x1, y1), (x0 + 6, y1), (x0, y1 - 4), (x0, y0 + 2)]
    c.grad_poly(pts, p["light"], p["dark"])
    c.poly(pts, None, outline=p["edge"], width=0.6)
    # 回路の線
    col = mix(p["accent"], (0, 0, 0, 0), 0.35)
    for (a, b) in [([(60, 9.6), (90, 9.6), (93, 11.2), (130, 11.2)], None),
                   ([(150, 23.4), (180, 23.4), (183, 21.8), (225, 21.8)], None),
                   ([(196, 9.6), (228, 9.6), (231, 11.5)], None)]:
        c.line(a, col, 0.35)
    for (x, y) in [(130, 11.2), (60, 9.6), (225, 21.8), (150, 23.4)]:
        c.circle(x, y, 0.6, fill=p["accent"])


def es_ring(c, p):
    cx, cy, r = EMB
    pts = [(cx + math.cos(i * math.pi / 3 + math.pi / 6) * r, cy + math.sin(i * math.pi / 3 + math.pi / 6) * r) for i in range(6)]
    c.grad_poly(pts, p["light"], p["dark"])
    c.poly(pts, None, outline=p["edge"], width=0.6)


def es_emblem(c, p, stage):
    cx, cy, r = EMB
    # 目
    c.glow(lambda d, col: d.ellipse([(cx - 7) * S, (cy - 4) * S, (cx + 7) * S, (cy + 4) * S], fill=col), p["glow"], blur=1.0)
    eye = []
    for i in range(25):
        t = i / 24 * math.pi
        eye.append((cx - math.cos(t) * 8, cy - math.sin(t) * 3.6))
    for i in range(25):
        t = i / 24 * math.pi
        eye.append((cx + math.cos(t) * 8, cy + math.sin(t) * 3.6))
    c.poly(eye, p["accent2"])
    c.circle(cx, cy, 2.6, fill=p["fill_hi"])
    c.circle(cx, cy, 1.2, fill=p["edge"])
    # まわりの輪（データリング）
    for i in range(12):
        a = i * math.pi / 6
        if i % 3 == 0:
            continue
        c.line([(cx + math.cos(a) * 9.5, cy + math.sin(a) * 9.5), (cx + math.cos(a + 0.35) * 9.5, cy + math.sin(a + 0.35) * 9.5)],
               p["accent"], 0.6)


def es_back(c, p, stage):
    # 伸びるアーム（右端の上下にはさむ形）
    ext = 2.5 if stage == 1 else 0
    for sgn in (-1, 1):
        def Y(v): return 16.5 + sgn * v
        pts = [(192 - ext * 4, Y(7.5)), (202 - ext * 4, Y(13.5 + ext * 0.3)), (239, Y(13.5 + ext * 0.3)), (239, Y(10)), (205 - ext * 4, Y(10)), (198 - ext * 4, Y(6))]
        c.glow(lambda d, col, pts=pts: d.polygon([(a * S, b * S) for a, b in pts], fill=col), p["glow"], blur=0.8, strength=0.6)
        c.grad_poly(pts, p["light"], p["dark"])
        c.poly(pts, None, outline=p["edge"], width=0.4)
        c.line([(204 - ext * 4, Y(11.8 + ext * 0.15)), (237, Y(11.8 + ext * 0.15))], p["accent"], 0.45)


def es_front(c, p, stage):
    c.line([(235.5, 11), (235.5, 22)], p["accent"], 0.5)
    # 紋章のまわりのホロ輪
    cx, cy, r = EMB
    for k in range(3):
        a0 = k * 2 * math.pi / 3 + 0.3
        pts = [(cx + math.cos(a0 + i * 0.07) * (r + 1.6), cy + math.sin(a0 + i * 0.07) * (r + 1.6)) for i in range(14)]
        c.line(pts, mix(p["accent"], (0, 0, 0, 0), 0.2), 0.45)


# ============================================================ ユグドラシル
def yg_pal(stage):
    if stage == 0:
        return pal("#2a1c10", "#6a4a2a", "#0e0804", (120, 255, 120, 170), "#0c1a08", "#1e3a12",
                   "#8aff6a", "#3ac82a", "#6ae04a", "#1a8a1a", "#dcffb0")
    if stage == 1:
        return pal("#2a1410", "#6a3a26", "#0e0604", (255, 90, 50, 180), "#1a0806", "#3a120a",
                   "#ffa050", "#d83a1a", "#ff6a3a", "#a01a0a", "#ffd8a0")
    return pal("#3a2a14", "#c8a86a", "#1a1006", (255, 230, 120, 220), "#2a2008", "#5a4a18",
               "#fff0a0", "#ffc83a", "#ffe070", "#d89a10", "#fffbe0", empty="#1e1a10")


def yg_plate(c, p):
    x0, y0, x1, y1 = PLATE
    # 木の皮のような板：上下の縁を少し波打たせる
    rnd = random.Random(11)
    top = [(x, y0 + rnd.uniform(-0.4, 0.5)) for x in [x0 + 3 + i * 4 for i in range(int((x1 - x0 - 6) / 4) + 1)]]
    bot = [(x, y1 + rnd.uniform(-0.5, 0.4)) for x in [x1 - 3 - i * 4 for i in range(int((x1 - x0 - 6) / 4) + 1)]]
    pts = [(x0, y0 + 3)] + top + [(x1, y0 + 3), (x1, y1 - 3)] + bot + [(x0, y1 - 3)]
    c.grad_poly(pts, p["light"], p["dark"])
    c.poly(pts, None, outline=p["edge"], width=0.6)
    # 木目
    for i in range(16):
        x = x0 + 8 + i * 13 + rnd.uniform(-3, 3)
        y = rnd.choice([y0 + 2.0, y1 - 2.0])
        c.line([(x, y), (x + rnd.uniform(5, 9), y + rnd.uniform(-0.4, 0.4))], mix(p["dark"], (0, 0, 0, 255), 0.3), 0.35)


def yg_ring(c, p):
    cx, cy, r = EMB
    c.circle(cx, cy, r, grad=(p["light"], p["dark"]))
    c.circle(cx, cy, r, outline=p["edge"], width=0.6)
    # 根が絡んだ輪
    for i in range(10):
        a0 = i * math.pi / 5
        pts = [(cx + math.cos(a0 + k * 0.08) * (r - 1.2 + math.sin(k) * 0.6), cy + math.sin(a0 + k * 0.08) * (r - 1.2 + math.sin(k) * 0.6)) for k in range(8)]
        c.line(pts, mix(p["dark"], (0, 0, 0, 255), 0.3), 0.4)


def yg_emblem(c, p, stage):
    cx, cy, r = EMB
    trunk = mix(p["light"], (255, 255, 255, 255), 0.15)
    # 幹と根
    c.poly([(cx - 1.4, cy + 7.5), (cx - 0.9, cy - 1), (cx + 0.9, cy - 1), (cx + 1.4, cy + 7.5)], trunk)
    for sgn in (-1, 1):
        c.line([(cx + sgn * 1, cy + 6.5), (cx + sgn * 4, cy + 8.5), (cx + sgn * 6.5, cy + 8.8)], trunk, 0.7)
        c.line([(cx, cy + 1), (cx + sgn * 3.5, cy - 2.5), (cx + sgn * 5, cy - 5)], trunk, 0.7)
    # 葉の冠
    crown = p["accent2"]
    c.glow(lambda d, col: d.ellipse([(cx - 9) * S, (cy - 11) * S, (cx + 9) * S, (cy - 1) * S], fill=col), p["glow"], blur=1.2, strength=0.8)
    for (dx, dy, rr) in [(-5, -5, 3.5), (5, -5, 3.5), (0, -7.5, 4.2), (-2.5, -3, 3), (2.5, -3, 3), (-7, -2.5, 2.2), (7, -2.5, 2.2)]:
        c.circle(cx + dx, cy + dy, rr, grad=(p["accent"], crown))
    if stage == 2:   # 花
        for (dx, dy) in [(-5, -6), (4, -8), (6, -3), (-1, -4), (-6.5, -2)]:
            for k in range(5):
                a = k * math.pi * 2 / 5
                c.circle(cx + dx + math.cos(a) * 0.8, cy + dy + math.sin(a) * 0.8, 0.7, fill=(255, 230, 240, 255))
            c.circle(cx + dx, cy + dy, 0.45, fill=(255, 200, 60, 255))


def yg_back(c, p, stage):
    rnd = random.Random(5)
    bark = mix(p["dark"], p["light"], 0.4)
    # 板に巻きつく枝（上下）
    branch1 = [(30, 10), (45, 6.5), (62, 7.5), (78, 5.5)]
    branch2 = [(200, 26.5), (214, 28.5), (228, 26.5), (238, 22)]
    branch3 = [(222, 8.5), (230, 4.5), (238, 6)]
    for br in (branch1, branch2, branch3):
        c.line(br, p["edge"], 1.6)
        c.line(br, bark, 1.0)
    leaves = [(47, 6.4, -2.4), (60, 7.2, -0.6), (72, 6.0, -2.8), (79, 5.4, -0.3), (212, 28.4, 0.6), (225, 27.0, 1.2),
              (233, 25, 0.2), (229, 4.6, -2.0), (236, 5.8, -0.8)]
    for (x, y, a) in leaves:
        lp = leaf_pts(x, y, a, 4.4, 1.6)
        c.poly(lp, p["accent2"], outline=p["edge"], width=0.25)
        c.line([(x, y), (x + math.cos(a) * 3.4, y + math.sin(a) * 3.4)], p["accent"], 0.25)
    if stage == 2:
        for (x, y) in [(52, 5.8), (66, 6.2), (218, 29), (232, 4)]:
            for k in range(5):
                a = k * math.pi * 2 / 5
                c.circle(x + math.cos(a) * 1.0, y + math.sin(a) * 1.0, 0.85, fill=(255, 225, 236, 255))
            c.circle(x, y, 0.55, fill=(255, 200, 60, 255))


def yg_front(c, p, stage):
    # 窓の上を少しだけまたぐツタ
    vine = [(100, 25.5), (104, 23.5), (108, 25.8), (113, 24.2)]
    c.line(vine, p["accent2"], 0.5)
    if stage >= 1:
        c.glow(lambda d, col: d.rectangle([WIN[0] * S, (WIN[1] - 1) * S, WIN[2] * S, (WIN[3] + 1) * S], fill=col),
               mix(p["glow"], (0, 0, 0, 0), 0.4), blur=1.4, strength=0.6)



# ============================================================ 汎用の部品
def spikes_top(c, p, xs, base, h, w=2.2, col=None, tip=None):
    for x in xs:
        pts = [(x - w / 2, base), (x, base - h), (x + w / 2, base)]
        c.grad_poly(pts, tip or mix(p["light"], (255, 255, 255, 255), 0.4), col or p["dark"])
        c.poly(pts, None, outline=p["edge"], width=0.3)


def bolt_pts(x, y, h, w):
    return [(x + w * 0.2, y), (x - w * 0.5, y + h * 0.55), (x, y + h * 0.5), (x - w * 0.3, y + h),
            (x + w * 0.5, y + h * 0.4), (x, y + h * 0.45)]


def rivets(c, p, xs, ys):
    for x in xs:
        for y in ys:
            c.circle(x, y, 0.75, grad=(mix(p["light"], (255, 255, 255, 255), 0.5), p["dark"]))
            c.circle(x, y, 0.75, outline=p["edge"], width=0.2)


def glow_poly(c, pts, col, blur=1.0, strength=1.0):
    c.glow(lambda d, cc, pts=pts: d.polygon([(a * S, b * S) for a, b in pts], fill=cc), col, blur=blur, strength=strength)


# ============================================================ 氷の魔女
def iw_pal(stage):
    if stage == 0:
        return pal("#2a3e5a", "#a8c8e8", "#0a1424", (120, 200, 255, 200), "#0a1a30", "#1e4a78",
                   "#e8f8ff", "#7ac8ff", "#8ad8ff", "#2a7ad8", "#f0ffff", empty="#0e1620")
    return pal("#2e2048", "#a898d8", "#0e0820", (190, 120, 255, 210), "#140a2a", "#3a2068",
               "#f4e8ff", "#b07aff", "#c08aff", "#6a2ad8", "#f8f0ff", empty="#140e20")


def snowflake(c, cx, cy, r, col, w):
    for k in range(6):
        a = k * math.pi / 3 - math.pi / 2
        ex, ey = cx + math.cos(a) * r, cy + math.sin(a) * r
        c.line([(cx, cy), (ex, ey)], col, w)
        for f in (0.45, 0.7):
            mx, my = cx + math.cos(a) * r * f, cy + math.sin(a) * r * f
            for sg in (-1, 1):
                b = a + sg * 0.75
                c.line([(mx, my), (mx + math.cos(b) * r * 0.3, my + math.sin(b) * r * 0.3)], col, w * 0.8)


def iw_emblem(c, p, stage):
    cx, cy, _ = EMB
    c.glow(lambda d, col: d.ellipse([(cx - 9) * S, (cy - 9) * S, (cx + 9) * S, (cy + 9) * S], fill=col), p["glow"], blur=1.2)
    snowflake(c, cx, cy, 9.5, p["edge"], 1.6)
    snowflake(c, cx, cy, 9.5, p["accent"], 0.9)
    c.poly(star_pts(cx, cy, 2.6, 1.2, 6), (255, 255, 255, 255))


def crystal(c, p, x, base, h, w, ang=0.0, down=False):
    sg = 1 if down else -1
    ca, sa = math.cos(ang), math.sin(ang)
    loc = [(-w / 2, 0), (-w / 2, sg * h * 0.7), (0, sg * h), (w / 2, sg * h * 0.7), (w / 2, 0)]
    pts = [(x + px * ca - py * sa, base + px * sa + py * ca) for px, py in loc]
    glow_poly(c, pts, p["glow"], blur=0.7, strength=0.7)
    c.grad_poly(pts, (255, 255, 255, 240) if not down else p["accent2"], p["accent2"] if not down else (255, 255, 255, 240))
    c.poly(pts, None, outline=p["edge"], width=0.25)
    c.line([(x, base), (x - sa * sg * h * 0.85, base + ca * sg * h * 0.85)], (255, 255, 255, 160), 0.25)


def iw_back(c, p, stage):
    for (x, h, w, a) in [(226, 9, 3.2, 0.25), (231, 12, 3.6, 0.05), (236, 8, 2.8, -0.3), (40, 6, 2.4, 0.2), (45, 8, 2.8, 0)]:
        crystal(c, p, x, 10.5, h * (1.15 if stage else 1), w, a)


def iw_front(c, p, stage):
    rnd = random.Random(21)
    for x in range(108, 232, 7):
        h = rnd.uniform(2.0, 5.5) * (1.2 if stage else 1)
        crystal(c, p, x + rnd.uniform(-1.5, 1.5), 24.6, h, rnd.uniform(1.4, 2.2), 0, down=True)
    if stage:
        for _ in range(12):
            snowflake(c, rnd.uniform(70, 230), rnd.uniform(1, 6), 0.9, (230, 220, 255, 220), 0.25)


def pip_ice(bd, p):
    pip_poly(bd, p, [(24, 1), (40, 14), (34, 46), (14, 46), (8, 14)], hi=[17, 9, 25, 19])


# ============================================================ ゾンビキング（3本）
def zk_pal(stage):
    if stage == 0:
        return pal("#3a2a10", "#b8903a", "#140c04", (255, 70, 50, 190), "#200806", "#4a1410",
                   "#ffd04a", "#c82a1a", "#e8402a", "#8a1010", "#ffb0a0")
    if stage == 1:
        return pal("#2e2a12", "#8a8a3a", "#0e0c04", (120, 255, 60, 200), "#0c1806", "#1e3a10",
                   "#e8e04a", "#5ac82a", "#7ae040", "#2a8a10", "#e0ffb0")
    return pal("#16180e", "#4a5038", "#060804", (160, 255, 40, 230), "#060a04", "#142008",
               "#c8ff4a", "#7aff1a", "#a8ff3a", "#3aa010", "#f0ffc0", empty="#0c100a")


def crown_pts(cx, by, w, h):
    return [(cx - w / 2, by), (cx - w / 2, by - h * 0.55), (cx - w / 4, by - h * 0.3), (cx - w / 8, by - h),
            (cx, by - h * 0.4), (cx + w / 8, by - h), (cx + w / 4, by - h * 0.3), (cx + w / 2, by - h * 0.55), (cx + w / 2, by)]


def zk_emblem(c, p, stage):
    cx, cy, _ = EMB
    # どくろ
    bone = (226, 216, 190, 255) if stage < 2 else (190, 210, 160, 255)
    c.circle(cx, cy + 1.5, 6.2, fill=bone, outline=p["edge"], width=0.35)
    c.rrect((cx - 3.6, cy + 5, cx + 3.6, cy + 9), 1.0, fill=bone, outline=p["edge"], width=0.35)
    eye = p["accent2"] if stage else (30, 10, 10, 255)
    for sg in (-1, 1):
        c.circle(cx + sg * 2.4, cy + 2.2, 1.6, fill=eye)
    c.poly([(cx, cy + 4.2), (cx - 0.8, cy + 5.6), (cx + 0.8, cy + 5.6)], (40, 20, 20, 255))
    for k in range(-2, 3):
        c.line([(cx + k * 1.3, cy + 6.6), (cx + k * 1.3, cy + 8.6)], p["edge"], 0.25)
    # 王冠
    pts = crown_pts(cx, cy - 3.2, 13, 7.5)
    glow_poly(c, pts, p["glow"], blur=0.8, strength=0.7)
    c.grad_poly(pts, p["accent"], mix(p["accent"], p["dark"], 0.6))
    c.poly(pts, None, outline=p["edge"], width=0.35)
    for x in (cx - 3.5, cx, cx + 3.5):
        c.circle(x, cy - 4.6, 0.8, fill=p["accent2"])
    if stage == 2:   # ひび
        c.line([(cx + 1, cy - 2), (cx + 2.5, cy + 0.5), (cx + 1.6, cy + 2.6)], p["edge"], 0.35)


def zk_back(c, p, stage):
    # 板の両端に王冠の飾り
    for cx in (52, 220):
        pts = crown_pts(cx, 9.5, 14, 6.5)
        c.grad_poly(pts, p["accent"], mix(p["accent"], p["dark"], 0.6))
        c.poly(pts, None, outline=p["edge"], width=0.35)
        c.circle(cx, 6.6, 0.9, fill=p["accent2"])


def zk_front(c, p, stage):
    rivets(c, p, [40, 100, 160, 228], [10.2, 23])
    if stage >= 1:   # 垂れる毒
        rnd = random.Random(4 + stage)
        for x in [rnd.uniform(110, 230) for _ in range(6 + stage * 4)]:
            L = rnd.uniform(1.5, 4.5)
            c.line([(x, 24.5), (x, 24.5 + L)], p["fill_top"], 0.7)
            c.circle(x, 24.8 + L, 0.6, fill=p["fill_hi"])


def pip_skull(bd, p):
    bd.ellipse([4, 2, 44, 38], fill=p["edge"]); bd.rectangle([12, 28, 36, 46], fill=p["edge"])
    bd.ellipse([8, 6, 40, 34], fill=p["fill_top"]); bd.rectangle([15, 28, 33, 42], fill=p["fill_top"])
    bd.ellipse([12, 16, 21, 25], fill=p["edge"]); bd.ellipse([27, 16, 36, 25], fill=p["edge"])
    bd.ellipse([13, 8, 22, 14], fill=(255, 255, 255, 170))


# ============================================================ 闇の魔導士
def dm_pal(stage):
    if stage == 0:
        return pal("#1e1228", "#5a3a78", "#08040e", (170, 60, 255, 200), "#0c0614", "#2a1048",
                   "#e0a0ff", "#9a3aff", "#a84aff", "#4a1898", "#f0d0ff", empty="#120c18")
    return pal("#280c14", "#783048", "#0e0406", (255, 40, 80, 210), "#140408", "#480c1e",
               "#ffa0b0", "#ff2a5a", "#ff3a5a", "#980a2a", "#ffd0d8", empty="#180a0e")


def dm_emblem(c, p, stage):
    cx, cy, _ = EMB
    # 魔法陣
    c.circle(cx, cy, 10.5, outline=p["accent2"], width=0.4)
    c.circle(cx, cy, 8.2, outline=mix(p["accent2"], (0, 0, 0, 0), 0.3), width=0.3)
    for k in range(8):
        a = k * math.pi / 4
        c.circle(cx + math.cos(a) * 9.35, cy + math.sin(a) * 9.35, 0.55, fill=p["accent"])
    tri = lambda rot: [(cx + math.cos(rot + k * 2 * math.pi / 3) * 8.2, cy + math.sin(rot + k * 2 * math.pi / 3) * 8.2) for k in range(3)]
    for rot in (-math.pi / 2, math.pi / 2):
        t = tri(rot)
        c.line(t + [t[0]], mix(p["accent2"], (0, 0, 0, 0), 0.25), 0.35)
    # 三日月と目
    c.glow(lambda d, col: d.ellipse([(cx - 5) * S, (cy - 5) * S, (cx + 5) * S, (cy + 5) * S], fill=col), p["glow"], blur=1.0)
    eye = [(cx - math.cos(i / 16 * math.pi) * 5.5, cy - math.sin(i / 16 * math.pi) * 2.6) for i in range(17)] + \
          [(cx + math.cos(i / 16 * math.pi) * 5.5, cy + math.sin(i / 16 * math.pi) * 2.6) for i in range(17)]
    c.poly(eye, p["bg"], outline=p["accent"], width=0.4)
    c.circle(cx, cy, 1.9, fill=p["accent"])
    c.poly([(cx - 0.4, cy - 1.7), (cx + 0.4, cy - 1.7), (cx + 0.4, cy + 1.7), (cx - 0.4, cy + 1.7)], p["edge"])


RUNES = [[(0, 0), (1, 1), (0, 2)], [(0, 0), (0, 2), (1, 1)], [(0, 2), (1, 0), (1, 2)], [(0, 0), (1, 0), (0, 2), (1, 2)],
         [(0.5, 0), (0.5, 2), (0, 1), (1, 1)], [(0, 0), (1, 1), (1, 2)]]


def dm_front(c, p, stage):
    rnd = random.Random(9)
    for i, x in enumerate(range(44, 232, 9)):
        if 80 < x < 190 and i % 2:
            continue
        r = RUNES[rnd.randrange(len(RUNES))]
        pts = [(x + px * 1.4, 22.2 + py * 0.0) for px, py in r]
    # 板の下を漂う鬼火
    for (x, y, rr) in [(120, 27.5, 1.0), (150, 28.8, 0.7), (186, 27.2, 1.2), (214, 28.6, 0.8), (232, 27, 1.0)]:
        c.glow(lambda d, col, x=x, y=y, rr=rr: d.ellipse([(x - rr * 2) * S, (y - rr * 2) * S, (x + rr * 2) * S, (y + rr * 2) * S], fill=col), p["glow"], blur=0.8)
        c.circle(x, y, rr * 0.6, fill=p["fill_hi"])


def dm_plate(c, p):
    x0, y0, x1, y1 = PLATE
    c.rrect((x0, y0, x1, y1), 3, grad=(p["light"], p["dark"]))
    c.rrect((x0, y0, x1, y1), 3, outline=p["edge"], width=0.6)
    # 板に刻まれたルーン
    rnd = random.Random(2)
    col = mix(p["accent"], (0, 0, 0, 0), 0.4)
    for x in range(42, 232, 8):
        r = RUNES[rnd.randrange(len(RUNES))]
        c.line([(x + px * 1.6, 9.4 + py * 0.9) for px, py in r], col, 0.3)
        r = RUNES[rnd.randrange(len(RUNES))]
        c.line([(x + 3 + px * 1.6, 21.6 + py * 0.9) for px, py in r], col, 0.3)
    # 右端の宝玉
    c.glow(lambda d, col: d.ellipse([233 * S, 13 * S, 240 * S, 20 * S], fill=col), p["glow"], blur=1.0)
    c.poly([(236.5, 11.5), (239.5, 16.5), (236.5, 21.5), (233.5, 16.5)], p["accent2"], outline=p["edge"], width=0.3)


def pip_gem(bd, p):
    pip_poly(bd, p, [(24, 1), (44, 24), (24, 47), (4, 24)], hi=[16, 12, 23, 20])


# ============================================================ 剣竜
def bd_pal(stage):
    if stage == 0:
        return pal("#3a3e48", "#d8dce4", "#101218", (200, 230, 255, 180), "#141820", "#3a4250",
                   "#ffffff", "#9ab0c8", "#e0e8f4", "#7a8aa8", "#ffffff", empty="#12141a")
    return pal("#3a1418", "#b06068", "#120406", (255, 50, 50, 210), "#1a0406", "#4a0c10",
               "#ffd0d0", "#e01a2a", "#ff3a3a", "#900a14", "#ffd0c8", empty="#180a0c")


def sword_pts(x, base, h, w, ang=0.0):
    loc = [(-w / 2, 0), (-w / 2, -h * 0.82), (0, -h), (w / 2, -h * 0.82), (w / 2, 0)]
    ca, sa = math.cos(ang), math.sin(ang)
    return [(x + px * ca - py * sa, base + px * sa + py * ca) for px, py in loc]


def bd_emblem(c, p, stage):
    cx, cy, _ = EMB
    # 交差する剣
    for ang in (-0.6, 0.6):
        pts = sword_pts(cx - math.sin(ang) * -7, cy + 7.5, 17, 2.0, ang)
        c.grad_poly(pts, (255, 255, 255, 255), p["accent2"])
        c.poly(pts, None, outline=p["edge"], width=0.3)
    # 竜の目
    c.glow(lambda d, col: d.ellipse([(cx - 6) * S, (cy - 4) * S, (cx + 6) * S, (cy + 4) * S], fill=col), p["glow"], blur=1.0)
    eye = [(cx - math.cos(i / 16 * math.pi) * 6.5, cy - math.sin(i / 16 * math.pi) * 3.2) for i in range(17)] + \
          [(cx + math.cos(i / 16 * math.pi) * 6.5, cy + math.sin(i / 16 * math.pi) * 3.2) for i in range(17)]
    c.poly(eye, (255, 200, 60, 255) if stage == 0 else (255, 80, 40, 255), outline=p["edge"], width=0.4)
    c.poly([(cx, cy - 2.8), (cx + 0.8, cy), (cx, cy + 2.8), (cx - 0.8, cy)], p["edge"])


def bd_back(c, p, stage):
    rnd = random.Random(13)
    xs = [38, 42, 46, 50, 54, 58] + [204, 209, 214, 219, 224, 229, 234]
    if stage:
        xs += [62, 66, 199]
    for x in xs:
        h = rnd.uniform(5, 10) * (1.25 if stage else 1)
        pts = sword_pts(x, 11, h, 1.7, rnd.uniform(-0.25, 0.25))
        c.grad_poly(pts, (255, 255, 255, 255), p["accent2"])
        c.poly(pts, None, outline=p["edge"], width=0.25)


def bd_front(c, p, stage):
    c.poly([(231, 9.5), (240, 16.5), (231, 23.5)], p["light"], outline=p["edge"], width=0.4)
    c.line([(232.5, 12), (237.5, 16.5), (232.5, 21)], p["accent2"], 0.4)


def pip_blade(bd, p):
    pip_poly(bd, p, [(24, 0), (34, 14), (30, 47), (18, 47), (14, 14)], hi=[19, 8, 24, 20])


# ============================================================ 白狐
def wf_pal(stage):
    if stage == 0:
        return pal("#4a3010", "#e8b850", "#180e02", (255, 200, 60, 200), "#2a1404", "#6a3a08",
                   "#fff0a0", "#ff9a2a", "#ffc84a", "#d86a10", "#fff8d0")
    return pal("#5a5a6a", "#f4f4fa", "#1a1a24", (200, 230, 255, 220), "#283048", "#5a6a98",
               "#ffffff", "#8ad0ff", "#e8f4ff", "#8ab0e0", "#ffffff", empty="#141820")


def wf_emblem(c, p, stage):
    cx, cy, _ = EMB
    fur = (250, 248, 240, 255)
    head = [(cx - 8, cy - 9), (cx - 4, cy - 3.5), (cx + 4, cy - 3.5), (cx + 8, cy - 9), (cx + 7.5, cy + 0.5),
            (cx + 2.5, cy + 5.5), (cx, cy + 9), (cx - 2.5, cy + 5.5), (cx - 7.5, cy + 0.5)]
    glow_poly(c, head, p["glow"], blur=1.0)
    c.poly(head, fur, outline=p["edge"], width=0.4)
    for sg in (-1, 1):   # 耳の中・隈取り
        c.poly([(cx + sg * 7, cy - 7.5), (cx + sg * 4.6, cy - 3.8), (cx + sg * 6.8, cy - 2.5)], p["accent2"])
        c.poly([(cx + sg * 1.6, cy - 0.5), (cx + sg * 5.6, cy - 1.6), (cx + sg * 3.2, cy + 1.2)], p["accent2"])
        c.line([(cx + sg * 2, cy + 0.0), (cx + sg * 4.6, cy - 1.0)], p["edge"], 0.45)
    c.poly([(cx - 0.8, cy - 3.2), (cx + 0.8, cy - 3.2), (cx, cy - 1)], p["accent2"])
    c.circle(cx, cy + 8.2, 0.9, fill=p["edge"])


def foxfire(c, p, x, y, r):
    pts = flame_pts(x, y, r * 2, r * 3.2, 0)
    glow_poly(c, pts, p["glow"], blur=1.0)
    c.grad_poly(pts, (255, 255, 255, 255), p["accent2"])


def wf_back(c, p, stage):
    pass


def fox_tail(c, p, root, ang, L, w, bend):
    pts = []
    for i in range(15):
        t = i / 14
        a = ang + bend * t * t
        pts.append((root[0] + math.cos(a) * L * t, root[1] + math.sin(a) * L * t))
    # 太さが変わる尾：根元は細く、中ほどで太く、先がとがる
    left, right = [], []
    for i, (x, y) in enumerate(pts):
        t = i / 14
        ww = w * (0.35 + math.sin(min(1, t * 1.1) * math.pi) * 0.9) * (1 - t * 0.3)
        if i < 14:
            dx, dy = pts[i + 1][0] - x, pts[i + 1][1] - y
        n = math.hypot(dx, dy) or 1
        nx, ny = -dy / n, dx / n
        left.append((x + nx * ww, y + ny * ww)); right.append((x - nx * ww, y - ny * ww))
    poly = left + [pts[-1]] + right[::-1]
    glow_poly(c, poly, p["glow"], blur=0.6, strength=0.5)
    c.poly(poly, (250, 248, 240, 255), outline=p["edge"], width=0.3)
    tip = left[10:] + [pts[-1]] + right[10:][::-1]
    c.poly(tip, p["accent2"])


def wf_tails(c, p, stage):
    n = 3 if stage == 0 else 5
    angs = [-1.9, -1.45, -1.0, -2.3, -0.6][:n]
    for k, a in enumerate(angs):
        fox_tail(c, p, (233, 13), a - 0.5, 13 + (k % 2) * 2, 1.9, 0.8)


def wf_front(c, p, stage):
    wf_tails(c, p, stage)
    for (x, y, r) in [(42, 8.5, 1.3), (52, 6.5, 1.0), (62, 8.0, 1.2), (198, 7, 1.0), (212, 25.5, 1.0)] + ([(74, 6.5, 0.9), (188, 6.0, 0.9)] if stage else []):
        foxfire(c, p, x, y, r)


def pip_foxfire(bd, p):
    pts = [(24 + math.cos(a) * 15, 30 + math.sin(a) * 15) for a in [math.pi * k / 10 for k in range(11)]]
    pts = pts[::-1] + [(10, 26), (14, 14), (24, 1), (34, 14), (38, 26)]
    pts = [(38, 26)] + [(24 + math.cos(a) * 15, 30 + math.sin(a) * 15) for a in [math.pi * k / 10 for k in range(11)]] + [(10, 26), (16, 12), (24, 1), (32, 12)]
    pip_poly(bd, p, pts, hi=[18, 20, 25, 28])


# ============================================================ 風の猫
def wc_pal(stage):
    if stage == 0:
        return pal("#1e3a28", "#6ab88a", "#08140c", (100, 255, 170, 180), "#08180e", "#18402a",
                   "#d8ffe8", "#3ae08a", "#5ae8a0", "#18a058", "#e8fff0", empty="#0c1610")
    return pal("#1e2a48", "#6a8ad8", "#080c1a", (100, 190, 255, 200), "#080e22", "#182a5a",
               "#e0f0ff", "#4aa0ff", "#5ab8ff", "#1860d8", "#f0f8ff", empty="#0c1020")


def swirl(c, cx, cy, r, turns, col, w, a0=0.0):
    pts = []
    for i in range(40):
        t = i / 39
        a = a0 + t * turns * 2 * math.pi
        rr = r * (1 - t * 0.8)
        pts.append((cx + math.cos(a) * rr, cy + math.sin(a) * rr))
    c.line(pts, col, w)


def wc_emblem(c, p, stage):
    cx, cy, _ = EMB
    head = [(cx - 8, cy - 8.5), (cx - 5, cy - 4), (cx + 5, cy - 4), (cx + 8, cy - 8.5), (cx + 8.2, cy + 1),
            (cx + 5, cy + 6.5), (cx, cy + 8), (cx - 5, cy + 6.5), (cx - 8.2, cy + 1)]
    c.poly(head, p["dark"], outline=p["edge"], width=0.4)
    swirl(c, cx, cy + 1.5, 6.5, 1.2, p["accent2"], 0.7, 0.0)
    for sg in (-1, 1):
        eye = [(cx + sg * 3.3 + math.cos(a) * 1.8, cy + 0.2 + math.sin(a) * 1.2 * (1 if math.sin(a) > 0 else 0.5)) for a in [k * math.pi / 8 for k in range(17)]]
        c.poly(eye, p["fill_hi"])
        c.line([(cx + sg * 3.3, cy - 0.5), (cx + sg * 3.3, cy + 1.2)], p["edge"], 0.4)
    c.poly([(cx - 0.8, cy + 3), (cx + 0.8, cy + 3), (cx, cy + 4)], p["accent"])


def wc_front(c, p, stage):
    for (x, y, r, a0) in [(42, 6, 3.2, 0.5), (210, 5.5, 3.6, 1.2), (226, 28, 3.0, 2.5), (130, 28.5, 2.4, 3.5)] + ([(60, 5, 2.4, 0), (190, 28.8, 2.6, 2)] if stage else []):
        swirl(c, x, y, r * 1.3, 0.9, p["edge"], 1.1, a0)
        swirl(c, x, y, r * 1.3, 0.9, p["accent"], 0.6, a0)
    rnd = random.Random(31)
    for _ in range(8 + stage * 6):   # 風の筋
        x = rnd.uniform(70, 230); y = rnd.choice([rnd.uniform(4.5, 7.5), rnd.uniform(26.5, 30)])
        if 80 < x < 190 and y < 8:
            continue
        L = rnd.uniform(5, 12)
        c.line([(x, y), (x + L, y - 0.3)], mix(p["fill_hi"], (0, 0, 0, 0), 0.3), 0.3)


def pip_wind(bd, p):
    pip_orb(bd, p)
    pts = [(24 + math.cos(a) * (14 - i * 0.3), 24 + math.sin(a) * (14 - i * 0.3)) for i, a in enumerate([k * 0.25 for k in range(30)])]
    bd.line(pts, fill=p["edge"], width=3)


# ============================================================ 雷獅子
def tl_pal(stage):
    if stage == 0:
        return pal("#4a3a10", "#d8b040", "#181002", (255, 230, 60, 210), "#281c04", "#5a4008",
                   "#fff8a0", "#ffd02a", "#ffe04a", "#d89a10", "#fffad0")
    return pal("#142048", "#4a70d8", "#040818", (80, 200, 255, 220), "#040c28", "#10286a",
               "#e0f8ff", "#3ac8ff", "#4ad8ff", "#1870e0", "#f0ffff", empty="#0a0e1c")


def tl_ring(c, p):
    cx, cy, r = EMB
    mane = star_pts(cx, cy, r + 0.5, r - 3, 14)
    c.grad_poly(mane, p["light"], p["dark"])
    c.poly(mane, None, outline=p["edge"], width=0.5)
    c.circle(cx, cy, r - 2.2, grad=(p["light"], p["dark"]))
    c.circle(cx, cy, r - 2.2, outline=p["edge"], width=0.4)


def tl_emblem(c, p, stage):
    cx, cy, _ = EMB
    pts = bolt_pts(cx + 0.5, cy - 9, 18, 9)
    glow_poly(c, pts, p["glow"], blur=1.2)
    c.grad_poly(pts, (255, 255, 255, 255), p["accent2"])
    c.poly(pts, None, outline=p["edge"], width=0.35)


def tl_back(c, p, stage):
    for (x, y, h, w) in [(40, 1, 9, 4.5), (48, 3, 7, 3.6), (222, 1, 9, 4.5), (231, 0.5, 11, 5)] + ([(56, 3, 6, 3), (213, 3, 7, 3.5)] if stage else []):
        pts = bolt_pts(x, y, h, w)
        glow_poly(c, pts, p["glow"], blur=0.8)
        c.grad_poly(pts, (255, 255, 255, 255), p["accent2"])


def tl_front(c, p, stage):
    rnd = random.Random(17 + stage)
    for _ in range(3 + stage * 3):   # バチバチ
        x = rnd.uniform(100, 230); y = 25.5
        pts = [(x, y)]
        for k in range(4):
            x += rnd.uniform(1.5, 3); y = 25.5 + rnd.uniform(0.5, 3.5)
            pts.append((x, y))
        c.line(pts, p["fill_hi"], 0.35)
    rivets(c, p, [40, 228], [10.3, 22.8])


def pip_bolt(bd, p):
    pip_poly(bd, p, [(30, 0), (8, 26), (22, 26), (16, 47), (40, 18), (26, 18)])


# ============================================================ 岩の巨人
def rg_pal(stage):
    if stage == 0:
        return pal("#3a3a34", "#8a8a7a", "#121210", (120, 200, 80, 150), "#1a1e14", "#3a4428",
                   "#9ad85a", "#5a9a3a", "#7ac84a", "#3a7a1a", "#d8f8a0", empty="#141612")
    return pal("#2e2420", "#6a5a50", "#0e0806", (255, 110, 30, 220), "#1a0a04", "#4a1a08",
               "#ffd060", "#ff6a1a", "#ff8a2a", "#c82a08", "#ffe0a0", empty="#160e0a")


def rg_plate(c, p):
    x0, y0, x1, y1 = PLATE
    rnd = random.Random(8)
    top = [(x, y0 + rnd.uniform(-1.2, 0.8)) for x in range(x0 + 4, x1 - 3, 5)]
    bot = [(x, y1 + rnd.uniform(-0.8, 1.2)) for x in range(x1 - 4, x0 + 3, -5)]
    pts = [(x0 - 1, y0 + 3)] + top + [(x1 + 1, y0 + 2), (x1 + 2, y1 - 2)] + bot + [(x0, y1 - 2)]
    c.grad_poly(pts, p["light"], p["dark"])
    c.poly(pts, None, outline=p["edge"], width=0.6)
    # 石の割れ目（第2形態は溶岩が光る）
    for i in range(14):
        x = x0 + 10 + i * 14.5 + rnd.uniform(-3, 3)
        top_side = i % 2 == 0
        ya, yb = (y0 - 0.5, y0 + 3.2) if top_side else (y1 + 0.5, y1 - 3.2)
        crack = [(x, ya), (x + rnd.uniform(-1.5, 1.5), (ya + yb) / 2), (x + rnd.uniform(-1, 2), yb)]
        c.line(crack, p["edge"], 0.45)
        c.line(crack, mix(p["accent2"], (0, 0, 0, 0), 0.4), 0.2)
    # 苔・岩のかたまり
    for (x, y, r) in [(34, 9, 2.6), (60, 8, 2.0), (210, 8.5, 2.2), (232, 9.5, 3.0), (236, 22, 2.6), (120, 25, 1.8)]:
        q = [(x + math.cos(a) * r * rnd.uniform(0.75, 1.1), y + math.sin(a) * r * rnd.uniform(0.6, 0.9)) for a in [k * math.pi / 4 for k in range(8)]]
        c.grad_poly(q, mix(p["light"], (255, 255, 255, 255), 0.15), p["dark"])
        c.poly(q, None, outline=p["edge"], width=0.35)
        c.poly([(qx, qy) for qx, qy in q[4:8]], mix(p["accent2"], (0, 0, 0, 0), 0.2))


def rg_ring(c, p):
    cx, cy, r = EMB
    rnd = random.Random(3)
    pts = [(cx + math.cos(a) * r * rnd.uniform(0.92, 1.05), cy + math.sin(a) * r * rnd.uniform(0.92, 1.05)) for a in [k * math.pi / 7 for k in range(14)]]
    c.grad_poly(pts, p["light"], p["dark"])
    c.poly(pts, None, outline=p["edge"], width=0.6)


def rg_emblem(c, p, stage):
    cx, cy, _ = EMB
    # 岩の拳
    fist = [(cx - 6.5, cy - 3), (cx - 5.5, cy - 7), (cx + 5.5, cy - 7.5), (cx + 7, cy - 2), (cx + 6, cy + 4),
            (cx + 2, cy + 8.5), (cx - 3.5, cy + 8.5), (cx - 7, cy + 3)]
    c.grad_poly(fist, mix(p["light"], (255, 255, 255, 255), 0.2), p["dark"])
    c.poly(fist, None, outline=p["edge"], width=0.45)
    for k in range(3):
        x = cx - 3 + k * 3.2
        c.line([(x, cy - 7), (x - 0.3, cy - 2)], p["edge"], 0.4)
    c.line([(cx - 6, cy - 2), (cx + 6.5, cy - 1.5)], p["edge"], 0.4)
    c.line([(cx - 6.5, cy + 1), (cx - 2, cy + 2.5), (cx - 1, cy + 5)], p["edge"], 0.4)
    # 割れ目の光
    glow_poly(c, [(cx - 2, cy + 2), (cx + 3, cy + 1), (cx + 1, cy + 5)], p["glow"], blur=0.8)
    c.line([(cx - 1, cy - 1.5), (cx + 2.2, cy + 1.5), (cx + 1, cy + 5)], p["accent2"], 0.35)


def pip_rock(bd, p):
    pip_poly(bd, p, [(10, 4), (32, 1), (46, 16), (42, 40), (20, 47), (2, 30)], hi=[14, 8, 22, 15])


# ============================================================ マザークリーパー
def cm_pal(stage):
    if stage == 0:
        return pal("#1e3a1a", "#5aa84a", "#081406", (90, 255, 70, 180), "#0a1a08", "#1e4a18",
                   "#c8ff9a", "#3ac82a", "#5ad84a", "#1a8a10", "#e0ffd0", empty="#0e160c")
    return pal("#3a3a12", "#b8b84a", "#141404", (255, 240, 60, 210), "#1e1e04", "#4a4a08",
               "#fffff0", "#ffe02a", "#fff04a", "#c8b010", "#fffce0", empty="#16160a")


def cm_plate(c, p):
    x0, y0, x1, y1 = PLATE
    c.rrect((x0, y0, x1, y1), 1.2, fill=p["dark"])
    # クリーパーの肌：ドット
    rnd = random.Random(42)
    for y in range(int(y0), int(y1)):
        for x in range(int(x0), int(x1)):
            t = rnd.random()
            col = p["light"] if t < 0.45 else mix(p["light"], p["dark"], 0.35) if t < 0.8 else mix(p["light"], (255, 255, 255, 255), 0.25)
            c.poly([(x, y), (x + 1, y), (x + 1, y + 1), (x, y + 1)], col)
    c.rrect((x0, y0, x1, y1), 1.2, outline=p["edge"], width=0.6)


def cm_ring(c, p):
    cx, cy, r = EMB
    c.rrect((cx - r + 1, cy - r + 1, cx + r - 1, cy + r - 1), 2.5, grad=(p["light"], p["dark"]))
    c.rrect((cx - r + 1, cy - r + 1, cx + r - 1, cy + r - 1), 2.5, outline=p["edge"], width=0.6)


def cm_emblem(c, p, stage):
    cx, cy, _ = EMB
    face = (20, 26, 20, 255) if stage == 0 else (50, 30, 6, 255)
    skin = mix(p["light"], (255, 255, 255, 255), 0.1)
    c.rrect((cx - 9.5, cy - 9.5, cx + 9.5, cy + 9.5), 1.5, fill=skin, outline=p["edge"], width=0.4)
    rnd = random.Random(1)
    for yy in range(-9, 9, 2):
        for xx in range(-9, 9, 2):
            if rnd.random() < 0.4:
                c.poly([(cx + xx, cy + yy), (cx + xx + 2, cy + yy), (cx + xx + 2, cy + yy + 2), (cx + xx, cy + yy + 2)],
                       mix(skin, p["dark"], rnd.uniform(0.15, 0.35)))
    u = 3.0
    blocks = [(-2, -2), (-1, -2), (1, -2), (2, -2), (-2, -1), (-1, -1), (1, -1), (2, -1),
              (0, 0), (-1, 1), (0, 1), (1, 1), (-1, 2), (1, 2), (-1, 0) if False else (0, 0)]
    for bx, by in set(blocks):
        x, y = cx + (bx - 0.5) * u, cy + (by - 0.5) * u
        c.poly([(x, y), (x + u, y), (x + u, y + u), (x, y + u)], face)
    if stage:
        for sg in (-1, 1):
            ex, ey = cx + sg * 4.5, cy - 6.0
            c.glow(lambda d, col, ex=ex, ey=ey: d.ellipse([(ex - 2) * S, (ey - 2) * S, (ex + 2) * S, (ey + 2) * S], fill=col), (255, 60, 30, 255), blur=0.7)
            c.circle(ex, ey, 0.9, fill=(255, 120, 80, 255))


def cm_front(c, p, stage):
    # 右端の TNT の導火線と火花
    c.rrect((229, 10, 239, 23), 0.6, fill=(200, 40, 30, 255), outline=p["edge"], width=0.4)
    c.rrect((229, 14.6, 239, 18.4), 0, fill=(240, 236, 220, 255))
    c.line([(230.5, 16.5), (237.5, 16.5)], (40, 30, 30, 255), 0.6)
    c.line([(234, 10), (235, 7), (237, 5.5)], (80, 70, 60, 255), 0.5)
    spark = star_pts(237.2, 5.2, 2.0 + stage * 0.6, 0.7, 5)
    glow_poly(c, spark, (255, 220, 80, 255), blur=0.8)
    c.poly(spark, (255, 250, 200, 255))
    # 小さなクリーパーの顔（子どもたち）
    if stage:
        for x in (110, 150, 190):
            c.rrect((x - 2, 25.5, x + 2, 29.5), 0.3, fill=p["light"], outline=p["edge"], width=0.25)
            for dx in (-1, 0.4):
                c.poly([(x + dx, 26.4), (x + dx + 0.7, 26.4), (x + dx + 0.7, 27.1), (x + dx, 27.1)], p["edge"])
            c.poly([(x - 0.4, 27.2), (x + 0.4, 27.2), (x + 0.4, 28.6), (x - 0.4, 28.6)], p["edge"])


def pip_creeper(bd, p):
    bd.rectangle([2, 2, 45, 45], fill=p["edge"]); bd.rectangle([6, 6, 41, 41], fill=p["fill_top"])
    for (x, y, w, h) in [(11, 12, 9, 9), (28, 12, 9, 9), (20, 21, 8, 8), (16, 26, 16, 8), (16, 34, 5, 5), (27, 34, 5, 5)]:
        bd.rectangle([x, y, x + w, y + h], fill=p["edge"])


# ============================================================ フェンリル
def fe_pal(stage):
    if stage == 0:
        return pal("#3a4048", "#c8d0dc", "#10141a", (180, 220, 255, 180), "#141a24", "#344050",
                   "#ffffff", "#8ac0e8", "#d0e4f8", "#6a8ab0", "#ffffff", empty="#12161c")
    return pal("#4a3410", "#e8c050", "#181002", (255, 70, 40, 210), "#200a04", "#5a2a08",
               "#fff0b0", "#ff3a2a", "#ffc84a", "#c88a10", "#fff8d8", empty="#18120a")


def fe_emblem(c, p, stage):
    cx, cy, _ = EMB
    fur = mix(p["light"], (255, 255, 255, 255), 0.35)
    head = [(cx - 7.5, cy - 10), (cx - 3.5, cy - 4.5), (cx + 3.5, cy - 4.5), (cx + 7.5, cy - 10), (cx + 8, cy - 1),
            (cx + 9.5, cy + 3), (cx + 4, cy + 3.5), (cx + 2.2, cy + 9), (cx - 2.2, cy + 9), (cx - 4, cy + 3.5), (cx - 9.5, cy + 3), (cx - 8, cy - 1)]
    c.grad_poly(head, fur, p["dark"])
    c.poly(head, None, outline=p["edge"], width=0.4)
    eye = (120, 220, 255, 255) if stage == 0 else (255, 40, 30, 255)
    for sg in (-1, 1):
        pts = [(cx + sg * 1.6, cy - 0.5), (cx + sg * 5.5, cy - 2.2), (cx + sg * 4.6, cy + 0.6)]
        c.glow(lambda d, col, pts=pts: d.polygon([(a * S, b * S) for a, b in pts], fill=col), eye, blur=0.6)
        c.poly(pts, eye)
        c.poly([(cx + sg * 6.2, cy - 8), (cx + sg * 4.3, cy - 4.8), (cx + sg * 6.6, cy - 4.2)], p["dark"])
    c.poly([(cx - 1.4, cy + 6.8), (cx + 1.4, cy + 6.8), (cx, cy + 8.4)], p["edge"])
    # 額の模様
    c.line([(cx, cy - 4), (cx, cy - 1)], p["accent2"], 0.5)


def fe_front(c, p, stage):
    # 板の下の牙
    for x in range(112, 232, 10):
        pts = [(x - 1.2, 24.5), (x, 24.5 + 3.5 + (x % 20 == 12) * 1.5), (x + 1.2, 24.5)]
        c.poly(pts, (245, 245, 235, 255), outline=p["edge"], width=0.25)
    # 爪あと
    for i in range(3):
        x = 214 + i * 4.5
        c.line([(x, 2.5 + i * 0.6), (x + 5, 9.5)], p["edge"], 1.1)
        c.line([(x, 2.5 + i * 0.6), (x + 5, 9.5)], p["accent2"] if stage else mix(p["light"], (255, 255, 255, 255), 0.4), 0.5)
    if stage:
        rnd = random.Random(6)
        for _ in range(10):
            x, y = rnd.uniform(40, 80), rnd.uniform(2, 7)
            c.circle(x, y, rnd.uniform(0.3, 0.6), fill=p["fill_hi"])


def pip_fang(bd, p):
    pip_poly(bd, p, [(8, 2), (40, 2), (36, 18), (24, 47), (12, 18)], hi=[14, 6, 22, 13])


# ============================================================ 鋼鉄の機甲蜘蛛
def is_pal(stage):
    if stage == 0:
        return pal("#3a3a3e", "#9a9aa4", "#101014", (255, 210, 40, 180), "#18181c", "#3a3a44",
                   "#ffd83a", "#ffb01a", "#ffd02a", "#c88a08", "#fff4c0", empty="#121214")
    return pal("#3a2a2a", "#9a7a78", "#120808", (255, 60, 30, 220), "#1c0808", "#441414",
               "#ff8a4a", "#ff2a1a", "#ff4a2a", "#a81208", "#ffc8a8", empty="#160c0c")


def gear_pts(cx, cy, r, teeth, depth):
    pts = []
    for k in range(teeth * 4):
        a = k * 2 * math.pi / (teeth * 4)
        rr = r if (k % 4) in (1, 2) else r - depth
        pts.append((cx + math.cos(a) * rr, cy + math.sin(a) * rr))
    return pts


def is_ring(c, p):
    cx, cy, r = EMB
    g = gear_pts(cx, cy, r + 0.3, 12, 2.2)
    c.grad_poly(g, p["light"], p["dark"])
    c.poly(g, None, outline=p["edge"], width=0.5)


def is_plate(c, p):
    x0, y0, x1, y1 = PLATE
    pts = [(x0, y0), (x1 - 4, y0), (x1, y0 + 4), (x1, y1), (x0, y1)]
    c.grad_poly(pts, p["light"], p["dark"])
    c.poly(pts, None, outline=p["edge"], width=0.6)
    # 警告のしま
    for x0s in (196,):
        lay = Canvas(); lay.img = Image.new("RGBA", c.img.size, (0, 0, 0, 0))
    for k in range(9):
        x = 198 + k * 4.2
        c.poly([(x, 22), (x + 2, 22), (x + 3.4, 24.4), (x + 1.4, 24.4)], p["accent"])
        c.poly([(x, 8.6), (x + 2, 8.6), (x + 3.4, 11), (x + 1.4, 11)], p["accent"])
    rivets(c, p, [44, 70, 96, 122, 148, 174], [10.2, 23.0])


def is_emblem(c, p, stage):
    cx, cy, _ = EMB
    # 機械の蜘蛛
    for sg in (-1, 1):
        for k, (ax, ay, bx, by) in enumerate([(4, -4, 8, -8), (5, -1, 9.5, -3), (5, 1.5, 9.5, 4), (4, 4, 8, 8.5)]):
            c.line([(cx + sg * 1.5, cy + ay * 0.3), (cx + sg * ax, cy + ay), (cx + sg * bx, cy + by)], p["edge"], 1.1)
            c.line([(cx + sg * 1.5, cy + ay * 0.3), (cx + sg * ax, cy + ay), (cx + sg * bx, cy + by)], p["light"], 0.5)
    c.circle(cx, cy + 2.5, 4.0, grad=(p["light"], p["dark"]))
    c.circle(cx, cy + 2.5, 4.0, outline=p["edge"], width=0.35)
    c.circle(cx, cy - 2.5, 2.6, grad=(p["light"], p["dark"]))
    c.circle(cx, cy - 2.5, 2.6, outline=p["edge"], width=0.35)
    for dx in (-1.1, 1.1):
        c.glow(lambda d, col, dx=dx: d.ellipse([(cx + dx - 1) * S, (cy - 3.5) * S, (cx + dx + 1) * S, (cy - 1.5) * S], fill=col), p["glow"], blur=0.5)
        c.circle(cx + dx, cy - 2.6, 0.6, fill=p["accent2"])
    c.line([(cx - 2, cy + 2.5), (cx + 2, cy + 2.5)], p["accent"], 0.5)


def is_back(c, p, stage):
    # 右端から出る機械の脚
    for (ys, ye, xm) in [(9, 1, 226), (24, 31.5, 226), (10, 2.5, 234), (23, 30.5, 234)]:
        c.line([(220, ys), (xm, (ys + ye) / 2 - (3 if ye < ys else -3)), (xm + 5, ye)], p["edge"], 1.6)
        c.line([(220, ys), (xm, (ys + ye) / 2 - (3 if ye < ys else -3)), (xm + 5, ye)], p["light"], 0.8)
        c.circle(xm, (ys + ye) / 2 - (3 if ye < ys else -3), 0.8, fill=p["accent"])


def is_front(c, p, stage):
    if stage:   # 回る丸のこ
        for (x, y) in [(40, 5.5), (234, 16.5)]:
            g = gear_pts(x, y, 3.6, 10, 1.2)
            glow_poly(c, g, p["glow"], blur=0.6, strength=0.6)
            c.grad_poly(g, (230, 230, 240, 255), (120, 120, 130, 255))
            c.poly(g, None, outline=p["edge"], width=0.25)
            c.circle(x, y, 1.0, fill=p["edge"])


def pip_gear(bd, p):
    pts = []
    for k in range(32):
        a = k * 2 * math.pi / 32
        rr = 23 if (k % 4) in (1, 2) else 17
        pts.append((24 + math.cos(a) * rr, 24 + math.sin(a) * rr))
    pip_poly(bd, p, pts)
    bd.ellipse([18, 18, 30, 30], fill=p["edge"])


# ============================================================ 堕ちた戦乙女
def fv_pal(stage):
    if stage == 0:
        return pal("#4a4a58", "#e8e8f4", "#14141c", (220, 230, 255, 200), "#1a1e30", "#3a4468",
                   "#fff4c0", "#c8d8ff", "#e8eeff", "#8a9ad8", "#ffffff", empty="#12141c")
    return pal("#2a1424", "#8a5078", "#0c040a", (255, 90, 190, 220), "#14040e", "#3a0c2a",
               "#ffd0f0", "#ff4ab0", "#ff6ac0", "#b01a78", "#ffe0f4", empty="#160a12")


def wing(c, p, root, side, scale, stage):
    """紋章の後ろから広がる翼（side=-1 左, 1 右）"""
    rx, ry = root
    feathers = 7
    col_a = (255, 255, 255, 255) if stage == 0 else (60, 30, 50, 255)
    col_b = p["accent2"]
    for k in range(feathers):
        t = k / (feathers - 1)
        ang = -math.pi / 2 + side * (0.55 + t * 1.0)
        L = scale * (14 - t * 5)
        tip = (rx + math.cos(ang) * L, ry + math.sin(ang) * L)
        w = 1.8
        nx, ny = -math.sin(ang) * w, math.cos(ang) * w
        pts = [(rx + nx, ry + ny), (tip[0] + nx * 0.3, tip[1] + ny * 0.3), (tip[0] + math.cos(ang) * 1.5, tip[1] + math.sin(ang) * 1.5),
               (tip[0] - nx * 0.3, tip[1] - ny * 0.3), (rx - nx, ry - ny)]
        c.grad_poly(pts, col_a, col_b)
        c.poly(pts, None, outline=p["edge"], width=0.25)


def fv_back(c, p, stage):
    cx, cy, _ = EMB
    # 紋章の後ろから左右に広がる翼（左は紋章の陰、右は板の上へ）
    wing(c, p, (cx + 6, cy - 4), 1, 0.85 if stage else 0.75, stage)
    wing(c, p, (234, 11), 1, 0.45, stage)
    wing(c, p, (226, 11), -1, 0.55, stage)


def fv_wings_front(c, p, stage):
    wing(c, p, (29, 11), 1, 0.95 if stage else 0.85, stage)


def fv_emblem(c, p, stage):
    cx, cy, _ = EMB
    # 槍と羽根の兜
    c.line([(cx, cy - 11), (cx, cy + 10)], p["edge"], 1.4)
    c.line([(cx, cy - 11), (cx, cy + 10)], mix(p["light"], (255, 255, 255, 255), 0.4), 0.7)
    head = [(cx, cy - 12.5), (cx + 2, cy - 8.5), (cx, cy - 6.5), (cx - 2, cy - 8.5)]
    glow_poly(c, head, p["glow"], blur=0.7)
    c.poly(head, p["accent"], outline=p["edge"], width=0.3)
    helm = [(cx - 5, cy + 4.5), (cx - 5.5, cy - 1), (cx - 3, cy - 4.5), (cx + 3, cy - 4.5), (cx + 5.5, cy - 1), (cx + 5, cy + 4.5),
            (cx + 1.6, cy + 6.5), (cx - 1.6, cy + 6.5)]
    c.grad_poly(helm, mix(p["light"], (255, 255, 255, 255), 0.3), p["dark"])
    c.poly(helm, None, outline=p["edge"], width=0.35)
    c.poly([(cx - 3.8, cy + 0.2), (cx + 3.8, cy + 0.2), (cx + 3.2, cy + 1.6), (cx - 3.2, cy + 1.6)], p["edge"])
    c.line([(cx - 2.8, cy + 0.9), (cx + 2.8, cy + 0.9)], p["accent2"], 0.4)
    for sg in (-1, 1):   # 兜の羽
        c.poly([(cx + sg * 5, cy - 1.5), (cx + sg * 9, cy - 5.5), (cx + sg * 8, cy - 1), (cx + sg * 5.2, cy + 1)],
               (255, 255, 255, 255) if stage == 0 else p["accent2"], outline=p["edge"], width=0.25)


def fv_front(c, p, stage):
    fv_wings_front(c, p, stage)
    rnd = random.Random(12)
    for _ in range(5 + stage * 4):   # 舞う羽根
        x, y = rnd.uniform(110, 225), rnd.uniform(26, 30.5)
        lp = leaf_pts(x, y, rnd.uniform(-0.6, 0.6), 3.2, 0.8)
        c.poly(lp, (255, 255, 255, 230) if stage == 0 else p["accent"], outline=p["edge"], width=0.2)


def pip_feather(bd, p):
    pts = [(24 + math.sin(t / 12 * math.pi) * 12 * (1 if i == 0 else -1), 2 + t / 12 * 44) for i in (0, 1) for t in (range(13) if i == 0 else range(12, -1, -1))]
    pip_poly(bd, p, pts, hi=[18, 10, 24, 22])


# ============================================================ 既存3体の玉
def pip_ember(bd, p):
    pts = [(24, 0), (34, 16), (40, 28), (36, 40), (24, 47), (12, 40), (8, 28), (14, 16)]
    pip_poly(bd, p, pts, hi=[17, 24, 24, 33])


def pip_core(bd, p):
    pip_poly(bd, p, [(24 + math.cos(k * math.pi / 3 + math.pi / 6) * 23, 24 + math.sin(k * math.pi / 3 + math.pi / 6) * 23) for k in range(6)])
    bd.ellipse([17, 17, 31, 31], fill=p["edge"]); bd.ellipse([20, 20, 28, 28], fill=(255, 255, 255, 255))


def pip_seed(bd, p):
    pts = [(24 + math.cos(a) * 16, 28 + math.sin(a) * 18) for a in [k * math.pi / 10 for k in range(21)]]
    pts = [(x, y) for x, y in pts]
    pip_poly(bd, p, pts, hi=[15, 16, 23, 26])
    bd.polygon([(24, 12), (36, 1), (38, 8), (28, 14)], fill=p["edge"])
    bd.polygon([(26, 11), (35, 4), (36, 8), (28, 12)], fill=(140, 220, 90, 255))

BOSSES = {
    "fire_knight": (2, fk_pal, dict(emblem_fn=fk_emblem, orn_back=fk_back, orn_front=fk_front, pip_fn=pip_ember)),
    "ice_witch": (2, iw_pal, dict(emblem_fn=iw_emblem, orn_back=iw_back, orn_front=iw_front, pip_fn=pip_ice)),
    "zombie_king": (3, zk_pal, dict(emblem_fn=zk_emblem, orn_back=zk_back, orn_front=zk_front, pip_fn=pip_skull)),
    "dark_mage": (2, dm_pal, dict(emblem_fn=dm_emblem, orn_front=dm_front, plate_fn=dm_plate, pip_fn=pip_gem)),
    "blade_dragon": (2, bd_pal, dict(emblem_fn=bd_emblem, orn_back=bd_back, orn_front=bd_front, pip_fn=pip_blade)),
    "white_fox": (2, wf_pal, dict(emblem_fn=wf_emblem, orn_back=wf_back, orn_front=wf_front, pip_fn=pip_foxfire)),
    "wind_cat": (2, wc_pal, dict(emblem_fn=wc_emblem, orn_front=wc_front, pip_fn=pip_wind)),
    "thunder_lion": (2, tl_pal, dict(emblem_fn=tl_emblem, orn_back=tl_back, orn_front=tl_front, ring_fn=tl_ring, pip_fn=pip_bolt)),
    "rock_giant": (2, rg_pal, dict(emblem_fn=rg_emblem, plate_fn=rg_plate, ring_fn=rg_ring, pip_fn=pip_rock)),
    "ender_sentinel": (2, es_pal, dict(emblem_fn=es_emblem, orn_back=es_back, orn_front=es_front, plate_fn=es_plate, ring_fn=es_ring, pip_fn=pip_core)),
    "creeper_mother": (2, cm_pal, dict(emblem_fn=cm_emblem, orn_front=cm_front, plate_fn=cm_plate, ring_fn=cm_ring, pip_fn=pip_creeper)),
    "fenrir": (2, fe_pal, dict(emblem_fn=fe_emblem, orn_front=fe_front, pip_fn=pip_fang)),
    "iron_spider": (2, is_pal, dict(emblem_fn=is_emblem, orn_back=is_back, orn_front=is_front, plate_fn=is_plate, ring_fn=is_ring, pip_fn=pip_gear)),
    "fallen_valkyrie": (2, fv_pal, dict(emblem_fn=fv_emblem, orn_back=fv_back, orn_front=fv_front, pip_fn=pip_feather)),
    "yggdrasil": (3, yg_pal, dict(emblem_fn=yg_emblem, orn_back=yg_back, orn_front=yg_front, plate_fn=yg_plate, ring_fn=yg_ring, pip_fn=pip_seed)),
}


def main(names):
    for name in names or BOSSES:
        n, palf, kw = BOSSES[name]
        for s in range(n):
            build(name, s, palf(s), **kw)
            print("wrote", name, s + 1)


if __name__ == "__main__":
    main(sys.argv[1:])
