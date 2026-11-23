"""ユグドラシル（高精細）：モデル＋テクスチャ＋発光マスク。 python3 tools/yggdrasil_gen.py
動かない巨大な世界樹のボス。ねじれた太い幹に古の顔（光る目と口のうろ）、幹に刻まれた光るルーン、
地面をはう8本の根、左右に腕のように動く大枝、広がる枝と葉のかたまり、垂れ下がるツタと光る苔。
キューブごとに傾け、テクスチャは 1px あたり 2×2 ピクセル。正面は -Z。高さ 約6ブロック、幅 約6ブロック。
"""
import math, os, sys, random
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from geo_tools import Model, mirror, hexc

m = Model("yggdrasil", 1024, 1024, seed=88, density=2)
R = random.Random(5)


def B(o, s, mat, tag, rot=None, pivot=None):
    x = {}
    if rot: x["rot"] = list(rot)
    if pivot: x["pivot"] = list(pivot)
    return (list(o), list(s), mat, tag, x) if x else (list(o), list(s), mat, tag)


# ================================================================ model
m.bone("root", [0, 0, 0])

# 幹：根元が広がり、上へいくほど細く、少しねじれる
TRUNK = [
    B([-15, 0, -15], [30, 8, 30], "bark", "flare"),
    B([-12, 7, -12], [24, 20, 24], "bark", "trunk_low"),
    B([-10.5, 26, -10.5], [21, 18, 21], "bark", "trunk_mid", rot=[0, 8, 0]),
    B([-9, 43, -9], [18, 16, 18], "bark", "trunk_high", rot=[0, 16, 0]),
    B([-7, 58, -7], [14, 8, 14], "bark", "crown_base", rot=[0, 22, 0]),
]
# 幹のまわりを巻く樹皮のうね（ねじれた筋）
for i in range(10):
    a = 360 * i / 10
    r = 11.5
    x, z = math.sin(math.radians(a)) * r, -math.cos(math.radians(a)) * r
    TRUNK.append(B([x - 1.6, 4, z - 1.6], [3.2, 40, 3.2], "bark_dark", "ridge", rot=[0, -a, 6], pivot=[x, 4, z]))
# 苔のかたまり
for (x, y, z, w, h) in ((-12.5, 14, -6, 3, 8), (10, 22, -8, 3, 7), (-9, 33, 8, 3, 9), (8, 38, 6, 3, 6), (-3, 52, -9.5, 7, 3)):
    TRUNK.append(B([x, y, z], [w, h, 3] if abs(x) > 8 else [w, h, 2], "moss", "moss"))
m.bone("trunk", [0, 0, 0], parent="root", cubes=TRUNK)

# 古の顔：眉のでっぱり、光る目、口のうろ、苔のひげ
m.bone("face", [0, 36, -11], parent="trunk", cubes=[
    B([-9, 38, -13.2], [8, 3, 3], "bark_dark", "brow", rot=[0, 0, -12]),
    B([1, 38, -13.2], [8, 3, 3], "bark_dark", "brow", rot=[0, 0, 12]),
    B([-7, 34.5, -12.2], [4.5, 3, 1.4], "glow", "eye"),
    B([2.5, 34.5, -12.2], [4.5, 3, 1.4], "glow", "eye"),
    B([-1.5, 28, -13.6], [3, 7, 3], "bark", "nose", rot=[-8, 0, 0]),
    B([-5.5, 21, -12.4], [11, 5, 1.6], "hollow", "mouth"),
    B([-6, 14, -12.6], [12, 6, 1.2], "moss", "beard"),
])
# 幹のルーン（光る）
m.bone("runes", [0, 0, 0], parent="trunk", cubes=[
    B([-12.3, 10, -2], [0.6, 14, 4], "glow", "rune"), B([11.7, 12, -1], [0.6, 12, 3], "glow", "rune"),
    B([-2, 9, 11.7], [4, 16, 0.6], "glow", "rune"), B([-10.8, 30, 3], [0.6, 10, 3], "glow", "rune"),
])

# 8本の根：幹から外へ、地面をはうように伸びる（先は別の骨で動かせる）。少し地面にもぐる
for i in range(8):
    a = 360 * i / 8 + 22.5
    L1, L2 = 15 + R.uniform(-2, 3), 11 + R.uniform(-2, 3)
    th = 6.5 - (i % 2)
    m.bone(f"root{i}", [0, 0, 0], parent="root", rotation=[0, -a, 0], cubes=[
        B([-th / 2, -1, -12 - L1], [th, th, L1 + 2], "bark", "root"),
        B([-th / 2 - 0.4, th - 2, -12 - L1 * 0.8], [th + 0.8, 1.6, L1 * 0.5], "moss", "root_moss"),
    ])
    m.bone(f"root{i}_tip", [0, 0, -12 - L1], parent=f"root{i}", cubes=[
        B([-th / 2 + 1, -1, -12 - L1 - L2], [th - 2, th - 2.5, L2 + 1], "bark", "root_tip"),
        B([-th / 2 + 1.8, -1, -12 - L1 - L2 - 4], [th - 3.6, th - 4, 4.5], "bark_dark", "root_end"),
    ])

# 腕のように動く大枝（左右）：肩から横へ伸び、ひじから下へ垂れ、先が爪のような小枝
ARM = [B([-34, 50, -3.5], [25, 7, 7], "bark", "arm_branch"),
       B([-24, 57, -1.5], [2.5, 7, 2.5], "bark_dark", "twig"),
       B([-30, 49, -4], [9, 2.5, 8], "moss", "arm_moss")]
FORE = [B([-34.5, 27, -3.5], [6, 24, 6], "bark", "fore_branch"),
        B([-35, 40, -4], [7, 3, 7], "moss", "fore_moss")]
HAND = [B([-35, 15, -3.5], [2.2, 12, 2.2], "bark_dark", "claw"),
        B([-32.5, 15, -3.5], [2.2, 12, 2.2], "bark_dark", "claw"),
        B([-33.7, 15, 1], [2.2, 12, 2.2], "bark_dark", "claw")]
m.bone("arm_right", [-10, 53.5, 0], parent="trunk", rotation=[0, 0, -8], cubes=ARM)
m.bone("forearm_right", [-31.5, 51, 0], parent="arm_right", cubes=FORE)
m.bone("hand_right", [-31.5, 27, 0], parent="forearm_right", cubes=HAND)
m.bone("arm_left", [10, 53.5, 0], parent="trunk", rotation=[0, 0, 8], cubes=mirror(ARM))
m.bone("forearm_left", [31.5, 51, 0], parent="arm_left", cubes=mirror(FORE))
m.bone("hand_left", [31.5, 27, 0], parent="forearm_left", cubes=mirror(HAND))

# 上の枝と葉の冠
CROWN = []
for i in range(7):
    a = 360 * i / 7 + 10
    L = 18 + (i % 3) * 3
    m.bone(f"branch{i}", [0, 62, 0], parent="trunk", rotation=[40 + (i % 2) * 12, -a, 0], cubes=[
        B([-2, 60, -2 - L], [4, 4, L + 2], "bark", "upper_branch")])
LEAVES = []
for i in range(40):
    a = R.uniform(0, 2 * math.pi)
    rr = 32 * math.sqrt(R.random())                  # 中心ほど密に
    y = 64 + (1 - rr / 32) * 20 + R.uniform(-3, 4)   # 中心が高いドーム型
    w = R.uniform(7, 13)
    h = R.uniform(5, 8)
    x, z = math.cos(a) * rr, math.sin(a) * rr
    LEAVES.append(B([x - w / 2, y, z - w / 2], [w, h, w * R.uniform(0.8, 1.1)], "leaves", "leaves",
                    rot=[R.uniform(-15, 15), R.uniform(0, 90), R.uniform(-15, 15)]))
m.bone("crown", [0, 60, 0], parent="trunk", cubes=CROWN)
m.bone("canopy", [0, 70, 0], parent="trunk", cubes=LEAVES)

# 垂れ下がるツタと、光る苔の実（ランタンのように光る）
VINES = []
for i in range(14):
    a = 2 * math.pi * i / 14 + R.uniform(-0.2, 0.2)
    rr = R.uniform(14, 28)
    ln = R.uniform(10, 22)
    x, z = math.cos(a) * rr, math.sin(a) * rr
    top = 70 - rr * 0.2
    VINES.append(B([x - 0.6, top - ln, z - 0.6], [1.2, ln, 1.2], "vine", "vine"))
    if i % 2 == 0:
        VINES.append(B([x - 1.2, top - ln - 2.2, z - 1.2], [2.4, 2.4, 2.4], "glow", "lantern"))
m.bone("vines", [0, 70, 0], parent="canopy", cubes=VINES)

# ================================================================ texture
PAL = {
    "bark": ("#4a3524", "#6a4c34", "#22170e"),
    "bark_dark": ("#33241a", "#4a3626", "#160f09"),
    "moss": ("#3d6b2a", "#5c8f3a", "#1f3a14"),
    "leaves": ("#3f7a34", "#6aa84a", "#1d3e18"),
    "vine": ("#2f5a22", "#45802e", "#183012"),
    "hollow": ("#0c0806", "#1a120c", "#000000"),
    "glow": ("#7affd0", "#e0fff4", "#2fbf8a"),
}
GLOW = [hexc("#2fbf8a"), hexc("#7affd0"), hexc("#e0fff4")]


def details(m, mat, tag, face, rect):
    fx, fy, fw, fh = rect
    r = m.rng
    if mat in ("bark", "bark_dark") and face != "down":
        # 樹皮：縦に走る深い溝と、ところどころの節
        dark = hexc("#1c130c" if mat == "bark" else "#0f0a06")
        x = 0
        while x < fw:
            x += r.randrange(2, 5)
            if x >= fw:
                break
            xx = x
            for j in range(fh):
                if 0 <= xx < fw:
                    m.put(fx + xx, fy + j, dark)
                if r.random() < 0.15:
                    xx += r.choice((-1, 1))
        for _ in range(max(1, fw * fh // 400)):
            cx, cy = r.randrange(fw), r.randrange(fh)
            for d in range(-2, 3):
                for e in range(-1, 2):
                    if 0 <= cx + d < fw and 0 <= cy + e < fh and abs(d) + abs(e) < 3:
                        m.put(fx + cx + d, fy + cy + e, hexc("#2a1c12"))
        for _ in range(fw * fh // 25):                    # 樹皮の明るい筋
            m.put(fx + r.randrange(fw), fy + r.randrange(fh), hexc("#7a5a3e" if mat == "bark" else "#54402e"))
        if face == "up":
            for _ in range(fw * fh // 6):
                m.put(fx + r.randrange(fw), fy + r.randrange(fh), hexc(r.choice(("#3d6b2a", "#5c8f3a"))))
    if mat in ("moss", "leaves", "vine") and face != "down":
        cols = {"moss": ("#2e5420", "#4f7f32", "#6a9a3c", "#3d6b2a"),
                "leaves": ("#2f6428", "#4f8f3a", "#76b450", "#3f7a34", "#24501f"),
                "vine": ("#24481a", "#3f7a2a", "#56923a")}[mat]
        for j in range(fh):
            for k in range(fw):
                if r.random() < 0.7:
                    m.put(fx + k, fy + j, hexc(r.choice(cols)))
        if mat == "leaves":
            # 葉のすき間（透かし）と、ふちのギザギザ、ところどころ光る胞子
            for _ in range(fw * fh // 14):
                m.clear(fx + r.randrange(fw), fy + r.randrange(fh))
            for k in range(fw):
                for j in range(r.choice((0, 0, 1, 2))):
                    m.clear(fx + k, fy + fh - 1 - j)
            for _ in range(max(1, fw * fh // 300)):
                m.put(fx + r.randrange(fw), fy + r.randrange(fh), GLOW[1], glow=True)
        if mat == "moss" and tag == "beard" and face == "north":
            for k in range(fw):
                for j in range(r.choice((0, 2, 4, 6)), 0, -1):
                    m.clear(fx + k, fy + fh - j)
    if mat == "glow":
        for j in range(fh):
            for k in range(fw):
                c = GLOW[2] if (tag == "eye" and abs(k - fw / 2) < fw / 5 and abs(j - fh / 2) < fh / 3) else \
                    (GLOW[1] if (j + k) % 3 else GLOW[0])
                m.put(fx + k, fy + j, c, glow=True)
        if tag == "rune":
            for j in range(fh):                           # ルーンの形：ところどころ途切れる
                for k in range(fw):
                    if (j // 3) % 3 == 2 and k % 2 == 0:
                        m.clear(fx + k, fy + j)
    if mat == "hollow" and face == "north":
        for k in range(fw):                               # 口のうろの奥がかすかに光る
            m.put(fx + k, fy + fh // 2, GLOW[0], glow=True)
        for k in range(0, fw, 3):                         # 牙のような木のささくれ
            m.put(fx + k, fy, hexc("#6a4c34"))
            m.put(fx + k, fy + fh - 1, hexc("#6a4c34"))


m.paint(PAL, details, glow_mats={"glow"}, outline_skip={"bark", "bark_dark", "moss", "leaves", "vine", "glow", "hollow"},
        soft={"mats": {"bark", "bark_dark"}, "y": (0, 70), "dark": 0.72})
m.save()
