"""マザークリーパー（クリーパーのボス）：モデル＋テクスチャ＋発光マスク。 python3 tools/creeper_mother_gen.py
大きなクリーパー。体は苔とツタに覆われ、背中にTNTの束が埋まり、わき腹には緑に光る火薬袋がふくらんでいる。
正面は -Z。全体を K 倍の大きさで作る（身長 約4ブロック）。
"""
import os, sys
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from geo_tools import Model, mirror, hexc

K = 2.5
m = Model("creeper_mother", 256, 256, seed=12)


def c(o, s, mat, tag):
    return ([round(v * K * 2) / 2 for v in o], [max(1, round(v * K)) for v in s], mat, tag)


def p(*xyz):
    return [round(v * K * 2) / 2 for v in xyz]


# ---------------------------------------------------------------- model
m.bone("root", [0, 0, 0])

LEG = [c([-4, 0, -2], [4, 6, 4], "skin_dark", "leg"), c([-4.3, 0, -2.3], [4.6, 1.5, 4.6], "skin_dark", "foot")]
m.bone("leg_front_right", p(-2, 6, -3), parent="root", cubes=[([v[0][0], v[0][1], v[0][2] - 3 * K], v[1], v[2], v[3]) for v in LEG])
m.bone("leg_front_left", p(2, 6, -3), parent="root", cubes=mirror([([v[0][0], v[0][1], v[0][2] - 3 * K], v[1], v[2], v[3]) for v in LEG]))
m.bone("leg_back_right", p(-2, 6, 3), parent="root", cubes=[([v[0][0], v[0][1], v[0][2] + 3 * K], v[1], v[2], v[3]) for v in LEG])
m.bone("leg_back_left", p(2, 6, 3), parent="root", cubes=mirror([([v[0][0], v[0][1], v[0][2] + 3 * K], v[1], v[2], v[3]) for v in LEG]))

m.bone("body", p(0, 6, 0), parent="root", cubes=[
    c([-4, 6, -2.5], [8, 12, 5], "skin", "body"),
    c([-4.5, 15, -3], [9, 3, 6], "moss", "collar_moss"),          # 首まわりの苔
])

# 背中の TNT の束（導火線の先が光る）
TNT = []
for i, (x, y, rot) in enumerate(((-3.6, 11, [15, 10, 12]), (0.4, 12.5, [-10, -15, -8]), (-1.6, 7.5, [8, 20, -15]))):
    m.bone(f"tnt{i}", p(x + 1.5, y + 1.5, 3.6), parent="body", rotation=rot, cubes=[
        c([x, y, 2.1], [3, 3, 3], "tnt", "tnt"),
        c([x + 1.2, y + 3, 3.3], [0.6, 1.0, 0.6], "fuse", "fuse"),
        c([x + 1.2, y + 4, 3.3], [0.6, 0.6, 0.6], "spark", "spark"),
    ])
m.bone("tnt_strap", p(0, 10, 3), parent="body", cubes=[c([-4.2, 10.5, 2.4], [8.4, 0.8, 0.8], "vine", "strap")])

# わき腹にふくらんだ火薬袋（アニメーションで脈打つ）
SAC = [c([-5.4, 9, -1.5], [1.6, 4, 3], "sac", "sac"), c([-5.0, 12.5, -1], [1.2, 1.5, 2], "sac", "sac_top")]
m.bone("sac_right", p(-4.5, 11, 0), parent="body", cubes=SAC)
m.bone("sac_left", p(4.5, 11, 0), parent="body", cubes=mirror(SAC))
m.bone("sac_front", p(1.5, 9, -2.5), parent="body", cubes=[c([0.5, 7.5, -3.3], [2.2, 2.5, 1], "sac", "sac")])

# 体に絡むツタ（垂れ下がる）
VINES = []
for x, z, top, ln in ((-3.5, -2.6, 16, 7), (-1, -2.6, 15, 4), (2.5, -2.6, 16, 8), (3.9, 0, 16, 9), (-3.9, 1, 16, 6), (0.5, 2.6, 15, 5)):
    dx = 0.25 if abs(x) > 3.8 else 0
    VINES.append(c([x - (0.3 if x < 0 else 0) + (dx if x > 0 else -dx), top - ln, z - 0.2 if z < 0 else z], [0.6, ln, 0.6], "vine", "vine"))
m.bone("vines", p(0, 16, 0), parent="body", cubes=VINES)

# 頭：大きな四角い頭。てっぺんに苔とツタ、小さな花
m.bone("head", p(0, 18, 0), parent="body", cubes=[
    c([-4.5, 18, -4.5], [9, 9, 9], "skin", "head"),
    c([-4.8, 26, -4.8], [9.6, 1.4, 9.6], "moss", "head_moss"),
    c([-2.5, 27.2, -1], [1, 1, 1], "flower", "flower"),
    c([1.5, 27.2, 1], [1, 1, 1], "flower", "flower"),
    c([-4.8, 21, -4.8], [0.6, 5, 0.6], "vine", "vine"),
    c([4.2, 20, 1], [0.6, 6, 0.6], "vine", "vine"),
    c([-1, 22, 4.2], [0.6, 5, 0.6], "vine", "vine"),
])

# ---------------------------------------------------------------- texture
PAL = {
    "skin": ("#4fa83a", "#6cc451", "#2c6e22"),
    "skin_dark": ("#3a7f2c", "#4f9a3a", "#21521a"),
    "moss": ("#3e6b24", "#56882f", "#24401a"),
    "vine": ("#2f6a1e", "#3f8226", "#1b4212"),
    "tnt": ("#c23a2a", "#e05a44", "#7a1e14"),
    "fuse": ("#5a4a3a", "#7a6a55", "#2e241a"),
    "spark": ("#ffd25a", "#ffffff", "#ff8a1a"),
    "strap": ("#5a3e26", "#74522f", "#2e1e10"),
    "sac": ("#7fff6a", "#d8ffcc", "#3fcf3a"),
    "flower": ("#ff6a9a", "#ffd0e0", "#c03a6a"),
}
SKIN = ["#4fa83a", "#5bb845", "#3f9030", "#6cc451", "#2f7a26", "#86d46a"]
GLOW = [hexc("#3fcf3a"), hexc("#7fff6a"), hexc("#d8ffcc")]


def details(m, mat, tag, face, rect):
    fx, fy, fw, fh = rect
    r = m.rng
    if mat in ("skin", "skin_dark"):
        # クリーパーらしい緑のまだら（ところどころ灰色のピクセル）
        for j in range(fh):
            for k in range(fw):
                if r.random() < 0.4:
                    m.put(fx + k, fy + j, hexc(r.choice(SKIN if r.random() < 0.985 else ["#b8c8b0"])))
        if face == "up" or tag in ("body",) and face in ("north", "south", "east", "west"):
            for _ in range(fw * fh // 10):                  # 苔のかたまり
                m.put(fx + r.randrange(fw), fy + r.randrange(max(1, fh // 3)), hexc(r.choice(("#3e6b24", "#56882f"))))
    if tag == "head" and face == "north":
        # 顔：目と口（目の奥がうっすら緑に光る）
        u = fw / 8.0
        def rect_(x0, y0, w, h, col, glow=False):
            for j in range(int(y0 * u), int((y0 + h) * u)):
                for k in range(int(x0 * u), int((x0 + w) * u)):
                    m.put(fx + k, fy + j, col, glow=glow)
        rect_(1, 2, 2, 2, hexc("#0c120a"))
        rect_(5, 2, 2, 2, hexc("#0c120a"))
        rect_(1.5, 2.5, 1, 1, GLOW[1], True)
        rect_(5.5, 2.5, 1, 1, GLOW[1], True)
        rect_(3, 4, 2, 3, hexc("#0c120a"))
        rect_(2, 5, 1, 3, hexc("#0c120a"))
        rect_(5, 5, 1, 3, hexc("#0c120a"))
    if mat == "moss" and face != "down":
        for _ in range(fw * fh // 2):
            m.put(fx + r.randrange(fw), fy + r.randrange(fh), hexc(r.choice(("#2f5a1c", "#56882f", "#6a9a3a", "#3e6b24"))))
        if face != "up":
            for k in range(fw):
                for j in range(fh - r.choice((0, 1, 1, 2)), fh):
                    m.clear(fx + k, fy + j)
    if mat == "vine":
        for j in range(fh):
            for k in range(fw):
                if r.random() < 0.35:
                    m.put(fx + k, fy + j, hexc(r.choice(("#4f9a2e", "#1f5212"))))
    if mat == "tnt" and face in ("north", "south", "east", "west"):
        # TNT：赤の真ん中に白い帯と黒い文字のような線
        for j in range(fh):
            for k in range(fw):
                if k % 3 == 1:
                    m.put(fx + k, fy + j, hexc("#a82e20"))
        mid = fh // 2
        for k in range(fw):
            for j in range(mid - 1, mid + 2):
                m.put(fx + k, fy + j, hexc("#f0f0f0"))
            if 0 < k < fw - 1 and k % 2 == 1:
                m.put(fx + k, fy + mid, hexc("#202020"))
    if mat == "tnt" and face == "up":
        for j in range(fh):
            for k in range(fw):
                m.put(fx + k, fy + j, hexc("#b8b8b8" if (j + k) % 3 else "#7a7a7a"))
    if mat in ("sac", "spark"):
        for j in range(fh):
            for k in range(fw):
                if mat == "sac":
                    col = GLOW[2] if r.random() < 0.15 else (GLOW[1] if r.random() < 0.7 else GLOW[0])
                else:
                    col = hexc("#ffffff") if (j + k) % 2 else hexc("#ffd25a")
                m.put(fx + k, fy + j, col, glow=True)
    if mat == "flower" and face == "up":
        m.put(fx + fw // 2, fy + fh // 2, hexc("#ffe85a"))


m.paint(PAL, details, glow_mats={"sac", "spark"}, outline_skip={"skin", "skin_dark", "moss", "vine", "sac", "spark"},
        soft={"mats": {"skin", "skin_dark"}, "y": (0, 70), "dark": 0.75})
m.save()
