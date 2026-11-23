"""亜種モブのテクスチャ（バニラのモデルと同じ UV 配置で、手続きで描く）。 python3 tools/variant_textures.py [preview.png]
textures/entity/variant/<名前>.png に保存する。引数にファイル名を渡すと、8体を並べたプレビューも描く。
"""
import os, sys, random
import numpy as np
from PIL import Image
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from geo_tools import faces, hexc

OUT = "src/main/resources/assets/jsnboss/textures/entity/variant"

# ---------------------------------------------------------------- バニラのモデルの箱（位置はプレビュー用のおおよそ）
# (名前, 原点, 大きさ, UV)
BLAZE = [("head", (-4, 18, -4), (8, 8, 8), (0, 0))] + \
        [(f"rod{i}", (int(round(np.cos(i * 1.57 + 0.4) * 8)) - 1, 10 + (i // 4) * -4, int(round(np.sin(i * 1.57 + 0.4) * 8)) - 1),
          (2, 8, 2), (0, 16)) for i in range(4)] + \
        [(f"rod{i + 4}", (int(round(np.cos(i * 1.57 + 1.2) * 6)) - 1, 4, int(round(np.sin(i * 1.57 + 1.2) * 6)) - 1), (2, 8, 2), (0, 16)) for i in range(4)] + \
        [(f"rod{i + 8}", (int(round(np.cos(i * 1.57) * 3)) - 1, -3, int(round(np.sin(i * 1.57) * 3)) - 1), (2, 8, 2), (0, 16)) for i in range(4)]
ZOMBIE = [("head", (-4, 24, -4), (8, 8, 8), (0, 0)), ("body", (-4, 12, -2), (8, 12, 4), (16, 16)),
          ("arm_r", (-8, 12, -2), (4, 12, 4), (40, 16)), ("arm_l", (4, 12, -2), (4, 12, 4), (32, 48)),
          ("leg_r", (-4, 0, -2), (4, 12, 4), (0, 16)), ("leg_l", (0, 0, -2), (4, 12, 4), (16, 48))]
SKELETON = [("head", (-4, 24, -4), (8, 8, 8), (0, 0)), ("body", (-4, 12, -2), (8, 12, 4), (16, 16)),
            ("arm", (-6, 12, -1), (2, 12, 2), (40, 16)), ("arm", (4, 12, -1), (2, 12, 2), (40, 16)),
            ("leg", (-3, 0, -1), (2, 12, 2), (0, 16)), ("leg", (1, 0, -1), (2, 12, 2), (0, 16))]
CREEPER = [("head", (-4, 18, -4), (8, 8, 8), (0, 0)), ("body", (-4, 6, -2), (8, 12, 4), (16, 16))] + \
          [("leg", (x, 0, z), (4, 6, 4), (0, 16)) for x, z in ((-4, -6), (0, -6), (-4, 2), (0, 2))]
SPIDER = [("head", (-4, 5, -14), (8, 8, 8), (32, 4)), ("neck", (-3, 6, -6), (6, 6, 6), (0, 0)),
          ("abdomen", (-5, 5, 0), (10, 8, 12), (0, 12))] + \
         [("leg", (-17 if i % 2 == 0 else 1, 7, -5 + (i // 2) * 3), (16, 2, 2), (18, 0)) for i in range(8)]

MODELS = {"blaze": (BLAZE, 64, 32), "zombie": (ZOMBIE, 64, 64), "skeleton": (SKELETON, 64, 32),
          "creeper": (CREEPER, 64, 32), "spider": (SPIDER, 64, 32)}


class Tex:
    def __init__(self, w, h, seed):
        self.img = np.zeros((h, w, 4), np.uint8)
        self.r = random.Random(seed)

    def put(self, x, y, c):
        c = np.clip(np.array(c, float), 0, 255)
        self.img[y, x, :3] = c
        self.img[y, x, 3] = 255

    def fill(self, rect, base, var=8):
        fx, fy, fw, fh = rect
        b = hexc(base)
        for j in range(fh):
            for k in range(fw):
                self.put(fx + k, fy + j, b + self.r.uniform(-var, var))

    def speckle(self, rect, cols, rate):
        fx, fy, fw, fh = rect
        for j in range(fh):
            for k in range(fw):
                if self.r.random() < rate:
                    self.put(fx + k, fy + j, hexc(self.r.choice(cols)))


def each_face(boxes):
    """(箱の名前, 面の名前, rect) を重複なしで返す（同じ UV を共有する箱は1回だけ）"""
    seen = set()
    for name, o, s, uv in boxes:
        for f in faces(o, s, uv[0], uv[1]):
            key = (f[4], f[0])
            if key in seen:
                continue
            seen.add(key)
            yield name, f[0], f[4]


# ---------------------------------------------------------------- 描き方
def face_rect(head_north, x, y, w, h):
    fx, fy, _, _ = head_north
    return (fx + x, fy + y, w, h)


def paint_blaze(t, boxes, body, light, dark, eye, rod_cols, sparkle):
    for name, face, rect in each_face(boxes):
        fx, fy, fw, fh = rect
        if name == "head":
            t.fill(rect, body, 10)
            t.speckle(rect, [light, dark], 0.25)
            if face == "north":
                for k in range(1, 7):                       # 目と口（バニラのブレイズに似た配置）
                    if k in (1, 2, 5, 6):
                        t.put(fx + k, fy + 3, hexc(dark))
                        t.put(fx + k, fy + 4, hexc(eye))
                for k in range(2, 6):
                    t.put(fx + k, fy + 6, hexc(dark))
            if face == "up":
                t.speckle(rect, [light], 0.4)
        else:
            for j in range(fh):
                for k in range(fw):
                    t.put(fx + k, fy + j, hexc(rod_cols[(j // 2 + k) % len(rod_cols)]))
            t.speckle(rect, [sparkle], 0.15)


def paint_humanoid(t, boxes, skin, skin_dark, shirt, pants, eye, extra=None):
    for name, face, rect in each_face(boxes):
        fx, fy, fw, fh = rect
        if name == "head":
            t.fill(rect, skin, 8)
            t.speckle(rect, [skin_dark], 0.2)
            if face == "north":
                for x in (1, 2, 5, 6):
                    t.put(fx + x, fy + 4, hexc("#101010"))
                t.put(fx + 2, fy + 4, hexc(eye)); t.put(fx + 5, fy + 4, hexc(eye))
                for x in range(2, 6):
                    t.put(fx + x, fy + 6, hexc(skin_dark))
        elif name == "body":
            t.fill(rect, shirt, 8)
        elif name.startswith("arm"):
            t.fill(rect, skin, 8)
            if fh > 4:
                t.fill((fx, fy, fw, min(fh, 4)), shirt, 8) if face not in ("up", "down") else None
        else:
            t.fill(rect, pants, 8)
        if extra:
            extra(t, name, face, rect)


# ---------------------------------------------------------------- 8体
def soul_blaze():
    boxes, w, h = MODELS["blaze"]
    t = Tex(w, h, 1)
    paint_blaze(t, boxes, "#2a5d9a", "#7fd0ff", "#0c1e3a", "#c8f4ff",
                ["#3fa0e0", "#7fd8ff", "#c8f4ff", "#2a6fb8"], "#ffffff")
    return t, "blaze"


def frost_blaze():
    boxes, w, h = MODELS["blaze"]
    t = Tex(w, h, 2)
    paint_blaze(t, boxes, "#cfeeff", "#ffffff", "#2a5a8a", "#3aa0ff",
                ["#e8faff", "#a8e0ff", "#ffffff", "#78c4f0"], "#ffffff")
    return t, "blaze"


def tomb_zombie():
    boxes, w, h = MODELS["zombie"]
    t = Tex(w, h, 3)

    def wraps(t, name, face, rect):
        fx, fy, fw, fh = rect
        # 全身の包帯：横の帯と、ところどころのすき間（黒ずんだ肌）
        for j in range(fh):
            for k in range(fw):
                band = (j + (k // 3)) % 3
                c = ("#d8c8a0", "#c4b088", "#b09a72")[band]
                t.put(fx + k, fy + j, hexc(c) + t.r.uniform(-6, 6))
        for _ in range(max(1, fw * fh // 14)):
            t.put(fx + t.r.randrange(fw), fy + t.r.randrange(fh), hexc(t.r.choice(("#3a2e20", "#5a4a32"))))
        if name == "head" and face == "north":
            for x in range(1, 7):
                t.put(fx + x, fy + 4, hexc("#1a1208"))
            t.put(fx + 2, fy + 4, hexc("#ffd24a")); t.put(fx + 5, fy + 4, hexc("#ffd24a"))
        if name == "body" and face == "north":                    # 胸の金の首飾り
            for x in range(2, 6):
                t.put(fx + x, fy + 1, hexc("#d4a020"))
            t.put(fx + 3, fy + 2, hexc("#3ac8c8")); t.put(fx + 4, fy + 2, hexc("#d4a020"))
    paint_humanoid(t, boxes, "#c4b088", "#8a7656", "#c4b088", "#b09a72", "#ffd24a", wraps)
    return t, "zombie"


def thunder_zombie():
    boxes, w, h = MODELS["zombie"]
    t = Tex(w, h, 4)

    def veins(t, name, face, rect):
        fx, fy, fw, fh = rect
        if face in ("down",):
            return
        if name in ("head", "arm_r", "arm_l") or (name == "body" and face in ("east", "west")):
            x = t.r.randrange(fw)
            for j in range(fh):                                    # 光る電気の血管
                if 0 <= x < fw and t.r.random() < 0.8:
                    t.put(fx + x, fy + j, hexc("#8fe8ff" if j % 3 else "#e8fcff"))
                x += t.r.choice((-1, 0, 1))
        if name == "body" and face == "north":
            for j in range(fh):                                    # 破れたシャツの稲妻模様
                x = 3 + (1 if (j // 2) % 2 else -1)
                t.put(fx + x, fy + j, hexc("#ffe14a"))
    paint_humanoid(t, boxes, "#4f7a8a", "#2e4a56", "#243a6a", "#1c2a44", "#8fe8ff", veins)
    return t, "zombie"


def fire_skeleton():
    boxes, w, h = MODELS["skeleton"]
    t = Tex(w, h, 5)
    for name, face, rect in each_face(boxes):
        fx, fy, fw, fh = rect
        t.fill(rect, "#2a2420", 8)                                 # 焦げた骨
        t.speckle(rect, ["#3c342c", "#1a1612"], 0.3)
        for _ in range(max(1, fw * fh // 18)):                      # 赤く光る残り火のひび
            x, y = t.r.randrange(fw), t.r.randrange(fh)
            for _ in range(3):
                if 0 <= x < fw and 0 <= y < fh:
                    t.put(fx + x, fy + y, hexc(t.r.choice(("#ff6a1a", "#ffaa3a"))))
                x += t.r.choice((-1, 0, 1)); y += 1
        if name == "head" and face == "north":
            for x, y in ((1, 3), (2, 3), (1, 4), (2, 4), (5, 3), (6, 3), (5, 4), (6, 4)):
                t.put(fx + x, fy + y, hexc("#ff8a1a"))
            for x in range(2, 6):
                t.put(fx + x, fy + 6, hexc("#0c0806"))
            t.put(fx + 3, fy + 5, hexc("#0c0806")); t.put(fx + 4, fy + 5, hexc("#0c0806"))
    return t, "skeleton"


def poison_skeleton():
    boxes, w, h = MODELS["skeleton"]
    t = Tex(w, h, 6)
    for name, face, rect in each_face(boxes):
        fx, fy, fw, fh = rect
        t.fill(rect, "#b8c8a0", 8)                                 # 薄緑の骨
        t.speckle(rect, ["#8aa070", "#d0dcc0"], 0.25)
        if face not in ("up", "down"):
            for k in range(fw):                                     # 上から垂れる紫緑の毒
                for j in range(t.r.choice((0, 0, 1, 2, 3))):
                    t.put(fx + k, fy + j, hexc(t.r.choice(("#7a3aa0", "#5ab83a", "#4a2a70"))))
        if name == "head" and face == "north":
            for x, y in ((1, 3), (2, 3), (1, 4), (2, 4), (5, 3), (6, 3), (5, 4), (6, 4)):
                t.put(fx + x, fy + y, hexc("#7aff4a"))
            for x in range(2, 6):
                t.put(fx + x, fy + 6, hexc("#20301a"))
    return t, "skeleton"


def frost_creeper():
    boxes, w, h = MODELS["creeper"]
    t = Tex(w, h, 7)
    for name, face, rect in each_face(boxes):
        fx, fy, fw, fh = rect
        t.fill(rect, "#a8d8f0", 6)
        t.speckle(rect, ["#ffffff", "#78b8e0", "#c8ecff", "#5a9ac8"], 0.45)   # 氷の迷彩
        if name == "head" and face == "north":
            for x, y in ((1, 2), (2, 2), (1, 3), (2, 3), (5, 2), (6, 2), (5, 3), (6, 3),
                         (3, 4), (4, 4), (2, 5), (3, 5), (4, 5), (5, 5), (2, 6), (5, 6), (2, 7), (5, 7)):
                t.put(fx + x, fy + y, hexc("#1a3a6a"))
            t.put(fx + 1, fy + 3, hexc("#5ac8ff")); t.put(fx + 6, fy + 3, hexc("#5ac8ff"))
        if face == "up":
            t.speckle(rect, ["#ffffff"], 0.5)                       # 頭と背中に積もった雪
    return t, "creeper"


def magma_spider():
    boxes, w, h = MODELS["spider"]
    t = Tex(w, h, 8)
    for name, face, rect in each_face(boxes):
        fx, fy, fw, fh = rect
        t.fill(rect, "#2a1c18", 8)                                 # 黒い玄武岩の体
        t.speckle(rect, ["#3a2a22", "#1a100c"], 0.3)
        n = max(1, fw * fh // 14)
        for _ in range(n):                                          # 溶岩のひび
            x, y = t.r.randrange(fw), t.r.randrange(fh)
            for _ in range(4):
                if 0 <= x < fw and 0 <= y < fh:
                    t.put(fx + x, fy + y, hexc(t.r.choice(("#ff5a0a", "#ffa21a", "#ffd25a"))))
                x += t.r.choice((-1, 0, 1)); y += t.r.choice((-1, 0, 1))
        if name == "head" and face == "north":                      # 赤い目（光る目は SpiderEyesLayer が描く）
            for x, y in ((1, 3), (2, 3), (5, 3), (6, 3), (3, 2), (4, 2)):
                t.put(fx + x, fy + y, hexc("#ff3a1a"))
    return t, "spider"


VARIANTS = {"soul_blaze": soul_blaze, "frost_blaze": frost_blaze, "tomb_zombie": tomb_zombie, "thunder_zombie": thunder_zombie,
            "fire_skeleton": fire_skeleton, "poison_skeleton": poison_skeleton, "frost_creeper": frost_creeper,
            "magma_spider": magma_spider}


def preview(out):
    import render_preview as R
    ims = []
    for name, fn in VARIANTS.items():
        t, model = fn()
        boxes, w, h = MODELS[model]
        R.tex = t.img.astype(float)
        R.glw = np.zeros_like(R.tex)
        R.data = {"bones": [{"name": "root", "pivot": [0, 0, 0]}],
                  "flat": [("root", list(f), "x") for _, o, s, uv in boxes for f in faces(o, s, uv[0], uv[1])]}
        R.bones = {"root": R.data["bones"][0]}
        ims.append(R.to_image(*R.render(-30, 12, 5, bounds=(-22, 22, -36, 6))[:3]))
    W = sum(i.width for i in ims[:4]); H = ims[0].height * 2
    sheet = Image.new("RGB", (W, H), (20, 18, 24))
    for i, im in enumerate(ims):
        sheet.paste(im, ((i % 4) * ims[0].width, (i // 4) * ims[0].height))
    sheet.save(out)


if __name__ == "__main__":
    os.makedirs(OUT, exist_ok=True)
    for name, fn in VARIANTS.items():
        t, _ = fn()
        Image.fromarray(t.img).save(f"{OUT}/{name}.png")
        print("saved", name)
    if len(sys.argv) > 1:
        preview(sys.argv[1])
