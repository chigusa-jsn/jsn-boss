"""テーマ亜種（森・べたべた・海・空・黄金・残響・鉱石・光る × ゾンビ・スケルトン・クモ・スライム・ブレイズ・クリーパー）のテクスチャ。
python3 tools/themed_textures.py [preview.png]
すでに手描きしてある組み合わせ（moss_zombie など）は variant_textures.py の方を使うので、ここでは作らない。
保存先：textures/entity/variant/<テーマ>_<モブ>.png（鉱石は <テーマ>_<モブ>_<段階>.png）
"""
import os, sys
import numpy as np
from PIL import Image
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from geo_tools import hexc
from variant_textures import MODELS, Tex, each_face, OUT

BASES = ["zombie", "skeleton", "spider", "slime", "blaze", "creeper"]
EXISTING = {("moss", "zombie"), ("honey", "spider"), ("sea", "zombie"), ("sky", "skeleton"), ("gold", "zombie"),
            ("sculk", "skeleton"), ("ore", "zombie"), ("glow", "slime")}
TIERS = {"stone": ("#7a7a7a", ["#2a2a2a", "#3a3a3a"]), "iron": ("#8a8a8a", ["#d8b090", "#c89a78"]),
         "gold": ("#8a8478", ["#ffd83a", "#fff07a"]), "diamond": ("#6a7078", ["#5af0e8", "#a8fff8"])}

# テーマの色：地, 暗い色, 明るい色, 目
THEMES = {
    "moss": ("#5a7a4a", "#2a5a1e", "#8ae05a", "#c8ff6a"),
    "honey": ("#4a3218", "#2a1a08", "#ffc840", "#ffd040"),
    "sea": ("#4aa08e", "#2a6a6a", "#a8f0e0", "#a8fff0"),
    "sky": ("#c8e0f8", "#8ab8e8", "#ffffff", "#5ac8ff"),
    "gold": ("#e8b830", "#a87818", "#ffe878", "#ff3a2a"),
    "sculk": ("#1a2a30", "#0a1418", "#2ac8c8", "#5af0e8"),
    "glow": ("#a8e83a", "#6aa81a", "#f4ffb0", "#ffffff"),
}


def pattern(t, theme, rect, face, tier=None):
    """テーマの模様を1面に描く"""
    fx, fy, fw, fh = rect
    r = t.r
    if theme == "ore":
        base, spots = TIERS[tier]
        t.fill(rect, base, 10)
        t.speckle(rect, ["#5a5a5a", "#9a9a9a"], 0.25)
        for _ in range(max(1, fw * fh // 9)):
            x, y = r.randrange(fw), r.randrange(fh)
            for dx, dy in ((0, 0), (1, 0), (0, 1)):
                if x + dx < fw and y + dy < fh:
                    t.put(fx + x + dx, fy + y + dy, hexc(r.choice(spots)))
        return
    base, dark, light, _ = THEMES[theme]
    t.fill(rect, base, 8)
    if theme == "moss":
        t.speckle(rect, ["#3e7a2a", "#5a9a3a", dark], 0.35)
        if face not in ("up", "down"):
            for _ in range(max(1, fw // 3)):
                x = r.randrange(fw)
                for j in range(r.randrange(max(1, fh // 2), fh + 1)):
                    if j < fh:
                        t.put(fx + x, fy + j, hexc("#2e6a22" if j % 3 else "#6ac84a"))
        if face == "up":
            t.speckle(rect, [light, "#6ac84a"], 0.6)
    elif theme == "honey":
        t.speckle(rect, ["#5a3e1e", dark], 0.3)
        for _ in range(max(1, fw * fh // 10)):
            x, y = r.randrange(fw), r.randrange(fh)
            for j in range(r.randrange(1, 4)):
                if y + j < fh:
                    t.put(fx + x, fy + y + j, hexc(r.choice(("#ffb020", "#ffc840", "#e89010"))))
        t.speckle(rect, ["#7ad85a", "#9af07a"], 0.06)
        if face == "up":
            t.speckle(rect, ["#ffc840", "#ffe080"], 0.35)
    elif theme == "sea":
        for j in range(fh):
            for k in range(fw):
                if j % 4 == 0 or (k % 4 == 0 and (j // 4 + k // 4) % 2 == 0):
                    t.put(fx + k, fy + j, hexc(dark))
                elif r.random() < 0.12:
                    t.put(fx + k, fy + j, hexc("#8af0d8"))
    elif theme == "sky":
        for j in range(fh):
            for k in range(fw):
                v = j / max(1, fh - 1)
                t.put(fx + k, fy + j, hexc("#e8f4ff") * (1 - v) + hexc(dark) * v + r.uniform(-6, 6))
        t.speckle(rect, ["#ffffff"], 0.2)
    elif theme == "gold":
        for j in range(fh):
            for k in range(fw):
                c = hexc(base) if (j + k) % 5 else hexc(light)
                if (j * 3 + k) % 11 == 0:
                    c = hexc(dark)
                t.put(fx + k, fy + j, c + r.uniform(-8, 8))
        t.speckle(rect, ["#fffbd0"], 0.06)
    elif theme == "sculk":
        t.speckle(rect, [dark, "#24363e"], 0.3)
        for _ in range(max(1, fw * fh // 12)):
            x, y = r.randrange(fw), r.randrange(fh)
            for _ in range(4):
                if 0 <= x < fw and 0 <= y < fh:
                    t.put(fx + x, fy + y, hexc(r.choice(("#2ac8c8", "#5af0e8", "#1a8a8a"))))
                x += r.choice((-1, 0, 1)); y += r.choice((-1, 0, 1))
    elif theme == "glow":
        for j in range(fh):
            for k in range(fw):
                t.put(fx + k, fy + j, hexc(base if (j + k) % 3 else light) + r.uniform(-6, 6))
        t.speckle(rect, ["#ffffff", "#ffff9a"], 0.12)


def eye_col(theme, tier):
    return TIERS[tier][1][-1] if theme == "ore" else THEMES[theme][3]


def dark_col(theme, tier):
    return "#3a3a3a" if theme == "ore" else THEMES[theme][1]


def paint(theme, base, tier=None):
    boxes, w, h = MODELS[base]
    t = Tex(w, h, hash((theme, base, tier)) & 0xffff)
    eye, dark = eye_col(theme, tier), dark_col(theme, tier)
    for name, face, rect in each_face(boxes):
        fx, fy, fw, fh = rect
        if base == "slime":
            if name == "outer":
                pattern(t, theme, rect, face, tier)
                t.img[fy:fy + fh, fx:fx + fw, 3] = 150                     # 外側は半透明
            elif name == "inner":
                pattern(t, theme, rect, face, tier)
            elif name.startswith("eye"):
                t.fill(rect, "#101010", 2)
                if face == "north":
                    t.put(fx, fy, hexc(eye))
            else:
                t.fill(rect, "#202020", 2)
            continue
        pattern(t, theme, rect, face, tier)
        if base == "blaze" and name != "head":                             # ブレイズの棒：縞
            for j in range(0, fh, 3):
                for k in range(fw):
                    t.put(fx + k, fy + j, hexc(eye_col(theme, tier)) * 0.6 + hexc(dark) * 0.4)
        if name != "head" or face != "north":
            continue
        # 顔
        if base in ("zombie", "skeleton"):
            for x in (1, 2, 5, 6):
                t.put(fx + x, fy + 4, hexc("#101010"))
            t.put(fx + 2, fy + 4, hexc(eye)); t.put(fx + 5, fy + 4, hexc(eye))
            if base == "skeleton":
                for x, y in ((1, 3), (2, 3), (5, 3), (6, 3)):
                    t.put(fx + x, fy + y, hexc("#101010"))
            for x in range(2, 6):
                t.put(fx + x, fy + 6, hexc(dark))
        elif base == "creeper":
            for x, y in ((1, 2), (2, 2), (1, 3), (2, 3), (5, 2), (6, 2), (5, 3), (6, 3),
                         (3, 4), (4, 4), (2, 5), (3, 5), (4, 5), (5, 5), (2, 6), (5, 6), (2, 7), (5, 7)):
                t.put(fx + x, fy + y, hexc("#101010"))
            t.put(fx + 1, fy + 3, hexc(eye)); t.put(fx + 6, fy + 3, hexc(eye))
        elif base == "spider":
            for x, y in ((1, 3), (2, 3), (5, 3), (6, 3), (3, 2), (4, 2)):
                t.put(fx + x, fy + y, hexc(eye))
        elif base == "blaze":
            for k in (1, 2, 5, 6):
                t.put(fx + k, fy + 3, hexc("#101010"))
                t.put(fx + k, fy + 4, hexc(eye))
            for k in range(2, 6):
                t.put(fx + k, fy + 6, hexc("#101010"))
    return t


def jobs():
    for theme in list(THEMES) + ["ore"]:
        for base in BASES:
            if (theme, base) in EXISTING:
                continue
            if theme == "ore":
                for tier in TIERS:
                    yield f"ore_{base}_{tier}", theme, base, tier
            else:
                yield f"{theme}_{base}", theme, base, None


def preview(out):
    import render_preview as R
    from geo_tools import faces
    ims = []
    for name, theme, base, tier in jobs():
        if tier not in (None, "diamond"):
            continue
        t = paint(theme, base, tier)
        boxes, w, h = MODELS[base]
        R.tex = t.img.astype(float)
        R.glw = np.zeros_like(R.tex)
        R.data = {"bones": [{"name": "root", "pivot": [0, 0, 0]}],
                  "flat": [("root", list(f), "x") for _, o, s, uv in boxes for f in faces(o, s, uv[0], uv[1])]}
        R.bones = {"root": R.data["bones"][0]}
        ims.append(R.to_image(*R.render(-30, 12, 3, bounds=(-22, 22, -36, 6))[:3]))
    cols = 8
    rows = (len(ims) + cols - 1) // cols
    sheet = Image.new("RGB", (ims[0].width * cols, ims[0].height * rows), (20, 18, 24))
    for i, im in enumerate(ims):
        sheet.paste(im, ((i % cols) * ims[0].width, (i // cols) * ims[0].height))
    sheet.save(out)


if __name__ == "__main__":
    os.makedirs(OUT, exist_ok=True)
    n = 0
    for name, theme, base, tier in jobs():
        Image.fromarray(paint(theme, base, tier).img).save(f"{OUT}/{name}.png")
        n += 1
    print("saved", n)
    if len(sys.argv) > 1:
        preview(sys.argv[1])
