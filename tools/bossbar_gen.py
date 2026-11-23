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


def strips(p):
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
    bd.ellipse([1, 1, 46, 46], fill=p["edge"])
    for i in range(18):
        t = i / 17
        r = 19 - i
        bd.ellipse([24 - r, 22 - r + i * 0.3, 24 + r, 22 + r + i * 0.3], fill=mix(p["fill_bot"], p["fill_hi"], t))
    bd.ellipse([13, 9, 23, 17], fill=(255, 255, 255, 200))
    img.alpha_composite(big.resize((12, 12), Image.LANCZOS), (0, 32))
    return img


def build(boss, stage, p, emblem_fn, orn_back=None, orn_front=None, plate_fn=None, ring_fn=None):
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
    out.paste(strips(p), (484, 0))
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


BOSSES = {
    "fire_knight": (2, fk_pal, dict(emblem_fn=fk_emblem, orn_back=fk_back, orn_front=fk_front)),
    "ender_sentinel": (2, es_pal, dict(emblem_fn=es_emblem, orn_back=es_back, orn_front=es_front, plate_fn=es_plate, ring_fn=es_ring)),
    "yggdrasil": (3, yg_pal, dict(emblem_fn=yg_emblem, orn_back=yg_back, orn_front=yg_front, plate_fn=yg_plate, ring_fn=yg_ring)),
}


def main(names):
    for name in names or BOSSES:
        n, palf, kw = BOSSES[name]
        for s in range(n):
            build(name, s, palf(s), **kw)
            print("wrote", name, s + 1)


if __name__ == "__main__":
    main(sys.argv[1:])
