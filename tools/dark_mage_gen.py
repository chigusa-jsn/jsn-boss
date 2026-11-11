"""闇の魔導士：モデル＋テクスチャ＋発光マスク。 python3 tools/dark_mage_gen.py"""
import math, os, sys
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from geo_tools import Model, mirror, hexc

m = Model("dark_mage", 256, 256, seed=31)

# ---------------------------------------------------------------- model
m.bone("root", [0, 0, 0])
m.bone("body", [0, 20, 0], parent="root")

# 下に行くほど細くなり、黒いもやにほどけていくローブ（足はない）
m.bone("robe", [0, 20, 0], parent="body", cubes=[
    ([-5, 15, -3.5], [10, 6, 7], "robe", "robe1"),
    ([-4.5, 10, -3], [9, 5, 6], "robe", "robe2"),
    ([-3.5, 6, -2.5], [7, 4, 5], "wisp", "robe3"),
    ([-2.5, 3, -2], [5, 3, 4], "wisp", "robe4"),
    ([-4.5, 4, -1], [2, 4, 2], "wisp", "tendril"),
    ([2.5, 5, -2], [2, 3, 2], "wisp", "tendril"),
    ([-1, 0, 0.5], [2, 3, 2], "wisp", "tendril"),
    ([1, 1, -2.5], [1, 2, 1], "wisp", "tendril"),
])

m.bone("torso", [0, 21, 0], parent="body", cubes=[
    ([-4.5, 21, -3], [9, 8, 6], "robe", "chest"),
    ([-5, 20, -3.5], [10, 2, 7], "trim", "sash"),
    ([-1, 23, -3.3], [2, 5, 1], "rune", "chest_rune"),
    ([-6, 27, -4], [12, 3, 8], "mantle", "mantle"),
    ([-7, 28, -3], [2, 2, 6], "mantle", "mantle_edge"),
    ([5, 28, -3], [2, 2, 6], "mantle", "mantle_edge"),
])

# 先のとがったフード。顔は真っ暗で目だけ光る
m.bone("head", [0, 30, 0], parent="torso", cubes=[
    ([-4.5, 30, -4.5], [9, 9, 9], "hood", "hood"),
    ([-3.5, 38, -2], [7, 2, 6], "hood", "hood_top"),
    ([-2.5, 39, 0.5], [5, 2, 4], "hood", "hood_tip1"),
    ([-1.5, 40, 2.5], [3, 2, 3], "hood", "hood_tip2"),
    ([-0.5, 41, 4.5], [1, 1, 2], "hood", "hood_tip3"),
])

ARM = [
    ([-7, 22, -1.5], [3, 7, 3], "robe", "upper_arm"),
    ([-8, 17, -2.5], [5, 5, 5], "sleeve", "sleeve"),
    ([-7, 14, -1], [2, 3, 2], "bone", "hand"),
    ([-7.5, 13, -1.5], [1, 2, 1], "bone", "finger"),
    ([-5.5, 13, -1.5], [1, 2, 1], "bone", "finger"),
]
m.bone("right_arm", [-5.5, 28, 0], parent="torso", cubes=ARM, rotation=[0, 0, -15])
m.bone("left_arm", [5.5, 28, 0], parent="torso", cubes=mirror(ARM), rotation=[-20, 0, 15])

# 杖：先に闇の宝玉
m.bone("staff", [-6, 15, 0], parent="right_arm", rotation=[0, 0, 15], cubes=[
    ([-7, -6, -1], [2, 32, 2], "wood", "shaft"),
    ([-8.5, 25, -1], [1, 5, 2], "bone", "claw"),
    ([-3.5, 25, -1], [1, 5, 2], "bone", "claw"),
    ([-7.5, 27, -1.5], [3, 3, 3], "orb", "orb"),
    ([-6.5, 30, -0.5], [1, 1, 1], "rune", "orb_spark"),
])

# 浮かぶ魔導書（開いたページが光る）
m.bone("grimoire", [11, 23, -3], parent="root", rotation=[-25, 0, 10], cubes=[
    ([7, 22, -6], [8, 1, 6], "cover", "cover"),
    ([7.5, 23, -5.5], [7, 1, 5], "pages", "pages"),
])

# 周りを回る闇の玉
orbit = []
for k, (ang, y) in enumerate([(60, 24), (180, 30), (300, 20)]):
    a = math.radians(ang)
    x, z = round(10 * math.cos(a)), round(10 * math.sin(a))
    orbit.append(([x - 1, y, z - 1], [2, 2, 2], "orb", f"orbit{k}"))
m.bone("orbit", [0, 25, 0], parent="root", cubes=orbit)

# ---------------------------------------------------------------- texture
PAL = {
    "robe": ("#1b1426", "#2e2240", "#0c0812"),
    "sleeve": ("#1b1426", "#2e2240", "#0c0812"),
    "mantle": ("#2a1d3d", "#3d2b58", "#120c1c"),
    "hood": ("#1d1529", "#30243f", "#0b0710"),
    "wisp": ("#120c1a", "#1f1530", "#07050b"),
    "trim": ("#6c3fa8", "#9b6be0", "#2f1a4c"),
    "rune": ("#b06bff", "#e6c8ff", "#7a3fd0"),
    "bone": ("#cfc7b0", "#ebe4cf", "#7e7763"),
    "wood": ("#2a1f1a", "#3d2e26", "#120d0a"),
    "orb": ("#3a0f5c", "#b06bff", "#14051f"),
    "cover": ("#3b1e2a", "#5a2f40", "#1b0c12"),
    "pages": ("#e8dcc0", "#fff6e0", "#a89a7c"),
}
GLOW = [hexc("#7a3fd0"), hexc("#b06bff"), hexc("#e6c8ff")]
VOID = hexc("#050308")


def details(m, mat, tag, face, rect):
    fx, fy, fw, fh = rect
    r = m.rng
    # フードの顔：真っ暗な中に光る目
    if tag == "hood" and face == "north":
        for j in range(1, fh):
            for k in range(1, fw - 1):
                m.put(fx + k, fy + j, VOID)
        for k in (2, 6):
            m.put(fx + k, fy + 4, GLOW[2], glow=True)
            m.put(fx + k, fy + 5, GLOW[1], glow=True)
        m.put(fx + 4, fy + 7, hexc("#160c22"))
    # ルーン：ローブのすそ・マントの縁に光る紋様
    if tag in ("robe1", "mantle") and face not in ("up", "down"):
        row = fh - 2 if tag == "robe1" else fh - 1
        for k in range(1, fw - 1):
            if (k + (1 if face in ("east", "west") else 0)) % 3 != 0:
                m.put(fx + k, fy + row, GLOW[1], glow=True)
    if tag == "chest_rune" or mat == "rune":
        for j in range(fh):
            for k in range(fw):
                m.put(fx + k, fy + j, GLOW[(j + k) % 3], glow=True)
    if tag == "sleeve" and face not in ("up", "down"):
        for k in range(fw):
            m.put(fx + k, fy + fh - 1, GLOW[0], glow=True)
    # もや：ところどころ透けて、紫の光がにじむ
    if mat == "wisp" and face not in ("up",):
        for j in range(fh):
            for k in range(fw):
                p = r.random()
                if j >= fh - 2 and p < 0.35:
                    m.clear(fx + k, fy + j)
                elif p < 0.12:
                    m.put(fx + k, fy + j, GLOW[0], glow=True)
    # 宝玉：中心が明るい
    if mat == "orb":
        for j in range(fh):
            for k in range(fw):
                d = abs(k - (fw - 1) / 2) + abs(j - (fh - 1) / 2)
                c = GLOW[2] if d < 0.6 else GLOW[1] if d < 1.6 else GLOW[0]
                m.put(fx + k, fy + j, c, glow=True)
    # 魔導書のページ：光る文字
    if mat == "pages" and face == "up":
        for j in range(fh):
            for k in range(fw):
                if k == fw // 2:
                    m.put(fx + k, fy + j, hexc("#a89a7c"))
                elif j % 2 == 0 and r.random() < 0.6:
                    m.put(fx + k, fy + j, GLOW[1], glow=True)
    if mat == "cover" and face == "down":
        m.put(fx + fw // 2, fy + fh // 2, GLOW[1], glow=True)
    if mat == "bone" and tag == "hand" and face not in ("up", "down"):
        for j in range(0, fh, 2):
            m.put(fx + (j % fw), fy + j, hexc("#9e9680"))


m.paint(PAL, details, glow_mats={"rune", "orb"}, outline_skip={"rune", "orb", "pages", "wisp"},
        soft={"mats": {"robe", "sleeve", "mantle", "hood", "wisp"}, "y": (0, 42), "dark": 0.55})
m.save()
