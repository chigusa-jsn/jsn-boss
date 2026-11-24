"""精霊の「床の層」のテクスチャ・モデル・ブロックステートを作る。 python3 tools/field_layer_gen.py"""
import json, os, random
from PIL import Image, ImageDraw

A = os.path.join(os.path.dirname(__file__), "..", "src/main/resources/assets/jsnboss")
KINDS = {   # 床の色（地, 模様, 明るい所）
    "fire": ("#5a1406", "#ff5a1a", "#ffd060"),
    "ice": ("#7ab8d8", "#d8f4ff", "#ffffff"),
    "wind": ("#4aa078", "#a8ffc8", "#ffffff"),
    "thunder": ("#1a2a5a", "#ffe040", "#ffffff"),
    "earth": ("#5a3e22", "#8a6a44", "#c8a070"),
    "dark": ("#140a24", "#5a20a0", "#b070ff"),
    "light": ("#c8b060", "#fff6c0", "#ffffff"),
    "wood": ("#3a8a3a", "#ffc8dc", "#ffffff"),
    "sanctuary": ("#e8f4ff", "#ffffff", "#fff8d0"),
}


def hx(h, a=255):
    h = h.lstrip("#"); return (int(h[0:2], 16), int(h[2:4], 16), int(h[4:6], 16), a)


def field(kind, base, mid, hi):
    r = random.Random(kind)
    im = Image.new("RGBA", (16, 16), hx(base, 235)); d = ImageDraw.Draw(im)
    if kind == "fire":            # 溶岩のひび
        for _ in range(5):
            x, y = r.randrange(16), r.randrange(16)
            for _ in range(7):
                d.point((x % 16, y % 16), fill=hx(mid)); x += r.choice((-1, 0, 1)); y += r.choice((0, 1))
        for _ in range(6): d.point((r.randrange(16), r.randrange(16)), fill=hx(hi))
    elif kind == "ice":           # 氷のひび
        for _ in range(3):
            x, y = r.randrange(16), r.randrange(16)
            d.line([(x, y), (x + r.randint(-6, 6), y + r.randint(-6, 6))], fill=hx(mid), width=1)
        for _ in range(8): d.point((r.randrange(16), r.randrange(16)), fill=hx(hi, 255))
        im.putalpha(Image.new("L", (16, 16), 200))
    elif kind == "thunder":       # 稲妻の筋
        for _ in range(2):
            x, y = r.randrange(16), 0
            while y < 16:
                nx = x + r.choice((-2, -1, 1, 2)); d.line([(x, y), (nx, y + 3)], fill=hx(mid)); x, y = nx, y + 3
        for _ in range(4): d.point((r.randrange(16), r.randrange(16)), fill=hx(hi))
    elif kind == "earth":         # ひび割れ
        for _ in range(4):
            x, y = r.randrange(16), r.randrange(16)
            d.line([(x, y), (x + r.randint(-5, 5), y + r.randint(-5, 5))], fill=hx("#2a1a0a"))
        for _ in range(10): d.point((r.randrange(16), r.randrange(16)), fill=hx(mid))
    elif kind == "dark":          # 闇の霧
        for _ in range(30): d.point((r.randrange(16), r.randrange(16)), fill=hx(mid, 220))
        for _ in range(5): d.point((r.randrange(16), r.randrange(16)), fill=hx(hi))
        im.putalpha(Image.new("L", (16, 16), 210))
    elif kind == "wood":          # 草と花
        for _ in range(40): d.point((r.randrange(16), r.randrange(16)), fill=hx("#5ac85a"))
        for _ in range(5):
            x, y = r.randrange(1, 15), r.randrange(1, 15)
            for dx, dy in ((0, -1), (0, 1), (-1, 0), (1, 0)): d.point((x + dx, y + dy), fill=hx(mid))
            d.point((x, y), fill=hx("#ffd860"))
    elif kind == "sanctuary":     # 聖域：白い光と金の縁
        im = Image.new("RGBA", (16, 16), hx(base, 170)); d = ImageDraw.Draw(im)
        d.rectangle([0, 0, 15, 15], outline=hx("#ffe890", 230))
        for _ in range(6): d.point((r.randrange(16), r.randrange(16)), fill=hx(hi))
    else:                         # 風・光（床は残らないが予告用に作っておく）
        for _ in range(12): d.point((r.randrange(16), r.randrange(16)), fill=hx(mid))
    return im


def warn(kind, base, mid, hi):
    """予告：属性の色で光る。縁を強く、真ん中は少し透ける"""
    im = Image.new("RGBA", (16, 16), (0, 0, 0, 0)); d = ImageDraw.Draw(im)
    c = hx(mid if kind not in ("ice", "light", "sanctuary") else hi)
    if kind == "ice": c = hx("#8ae0ff")
    if kind == "light": c = hx("#fff0a0")
    if kind == "earth": c = hx("#e0a050")
    if kind == "dark": c = hx("#b060ff")
    if kind == "wood": c = hx("#70ff80")
    d.rectangle([0, 0, 15, 15], fill=(c[0], c[1], c[2], 110))
    d.rectangle([0, 0, 15, 15], outline=(c[0], c[1], c[2], 240))
    d.rectangle([1, 1, 14, 14], outline=(min(255, c[0] + 60), min(255, c[1] + 60), min(255, c[2] + 60), 160))
    for i in range(0, 16, 4):     # 斜めの縞
        d.line([(i, 0), (0, i)], fill=(255, 255, 255, 60))
        d.line([(15, i + 1), (i + 1, 15)], fill=(255, 255, 255, 60))
    return im


os.makedirs(f"{A}/textures/block/field", exist_ok=True)
os.makedirs(f"{A}/models/block/field", exist_ok=True)
os.makedirs(f"{A}/blockstates", exist_ok=True)

template = {
    "ambientocclusion": False,
    "render_type": "minecraft:translucent",
    "textures": {"particle": "#top"},
    "elements": [{
        "from": [0, 0, 0], "to": [16, 0.5, 16],
        "neoforge_data": {"block_light": 15, "sky_light": 15},
        "faces": {
            "up": {"uv": [0, 0, 16, 16], "texture": "#top"},
            "down": {"uv": [0, 0, 16, 16], "texture": "#top", "cullface": "down"},
        },
    }],
}
json.dump(template, open(f"{A}/models/block/field/template.json", "w"), indent=2)

variants = {}
for kind, (b, m, h) in KINDS.items():
    field(kind, b, m, h).save(f"{A}/textures/block/field/{kind}.png")
    warn(kind, b, m, h).save(f"{A}/textures/block/field/{kind}_warn.png")
    for w in (False, True):
        name = f"{kind}_warn" if w else kind
        json.dump({"parent": "jsnboss:block/field/template", "textures": {"top": f"jsnboss:block/field/{name}"}},
                  open(f"{A}/models/block/field/{name}.json", "w"), indent=2)
        variants[f"kind={kind},warn={'true' if w else 'false'}"] = {"model": f"jsnboss:block/field/{name}"}
json.dump({"variants": variants}, open(f"{A}/blockstates/field_layer.json", "w"), indent=2)
print("ok", len(variants))
