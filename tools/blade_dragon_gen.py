"""影刃竜：モデル＋テクスチャ＋発光マスク。 python3 tools/blade_dragon_gen.py
四つ足：翼が前脚を兼ねる。体は Z 方向に長く、正面は -Z。
"""
import os, sys
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from geo_tools import Model, mirror, hexc

m = Model("blade_dragon", 256, 256, seed=41)


def blade(x, y, z, length=6, width=2, tag="blade"):
    """上向きの刃（根元 x,y,z）。先が細くなる2段"""
    return [([x - 0.5, y, z - width / 2], [1, length - 2, width], "blade", tag),
            ([x - 0.5, y + length - 2, z - width / 2 + 0.5], [1, 2, max(1, width - 1)], "blade", tag)]


# ---------------------------------------------------------------- model
m.bone("root", [0, 0, 0])
m.bone("body", [0, 20, 0], parent="root", cubes=[
    ([-7, 14, -16], [14, 13, 14], "hide", "chest"),
    ([-6.5, 13, -2], [13, 12, 14], "hide", "belly"),
    ([-6, 14, 12], [12, 11, 8], "hide", "hips"),
    ([-5, 12, -14], [10, 2, 24], "underside", "belly_plate"),
])

# 背中の刃：根元ごとに後ろへ倒す
for i, z in enumerate([-14, -9, -4, 1, 6, 11, 16]):
    h = 7 if i < 4 else 5
    top = 27 if z < 12 else 25
    m.bone(f"spine{i}", [0, top, z], parent="body", rotation=[-40, 0, 0], cubes=blade(0, top, z, h, 3))
# 肩の刃（左右）
for side in (-1, 1):
    for i, z in enumerate([-13, -8]):
        x = side * 6.5
        m.bone(f"shoulder_blade_{'r' if side < 0 else 'l'}{i}", [x, 26, z], parent="body",
               rotation=[-35, 0, side * 30], cubes=blade(x, 26, z, 5, 2))

# 首と頭（首は斜め上へ、頭は水平に戻す）
m.bone("neck", [0, 24, -16], parent="body", rotation=[-25, 0, 0], cubes=[
    ([-4, 20, -26], [8, 8, 11], "hide", "neck"),
    ([-0.5, 28, -24], [1, 3, 2], "blade", "neck_blade"),
    ([-0.5, 28, -20], [1, 3, 2], "blade", "neck_blade"),
])
m.bone("head", [0, 24, -26], parent="neck", rotation=[22, 0, 0], cubes=[
    ([-5, 20, -37], [10, 9, 11], "hide", "skull"),
    ([-3.5, 20, -43], [7, 6, 6], "hide", "snout"),
    ([-3.5, 17, -42], [7, 3, 12], "hide", "jaw"),
    ([-3, 20, -42.5], [6, 1, 1], "fang", "teeth"),
])
# 耳のような長い刃（後ろへ流れる）
for side in (-1, 1):
    x = side * 4
    m.bone(f"ear_{'r' if side < 0 else 'l'}", [x, 28, -30], parent="head", rotation=[-60, 0, side * 20],
           cubes=blade(x, 28, -30, 9, 3))

# 前脚（翼）：上腕・前腕・たたんだ翼膜・翼のふちの刃
WING = [
    ([-12, 12, -12.5], [5, 14, 5], "hide", "wing_upper"),
    ([-13.5, 12, -9], [1, 13, 15], "membrane", "membrane"),
]
FORE = [
    ([-12, 0, -12.5], [5, 13, 5], "hide", "forearm"),
    ([-12.5, 0, -15], [6, 2, 3], "fang", "claw"),
    ([-13, 3, -12], [1, 8, 4], "blade", "fore_blade"),
]
m.bone("right_wing", [-7, 24, -10], parent="body", cubes=WING)
m.bone("right_fore", [-9.5, 13, -10], parent="right_wing", cubes=FORE)
m.bone("left_wing", [7, 24, -10], parent="body", cubes=mirror(WING))
m.bone("left_fore", [9.5, 13, -10], parent="left_wing", cubes=mirror(FORE))
# 翼のふちから外向き・後ろ向きに並ぶ刃
for side, name in ((-1, "right"), (1, "left")):
    cubes = []
    for i, y in enumerate([25, 22, 19, 16]):
        length = 9 - i
        x0 = side * 12 - (length if side < 0 else 0)
        cubes.append(([x0, y, -11], [length, 1, 2], "blade", "wing_blade"))
    m.bone(f"{name}_wing_blades", [side * 12, 22, -10], parent=f"{name}_wing",
           rotation=[0, 35 * -side, side * 20], cubes=cubes)

# 後ろ脚
LEG = [([-11, 9, 10], [6, 11, 9], "hide", "thigh")]
SHIN = [
    ([-10.5, 0, 15], [5, 10, 5], "hide", "shin"),
    ([-11, 0, 12], [6, 2, 6], "hide", "foot"),
    ([-11, 0, 11], [2, 1, 1], "fang", "toe"),
    ([-7, 0, 11], [2, 1, 1], "fang", "toe"),
]
m.bone("right_leg", [-6, 18, 14], parent="body", cubes=LEG)
m.bone("right_shin", [-8, 10, 17], parent="right_leg", cubes=SHIN)
m.bone("left_leg", [6, 18, 14], parent="body", cubes=mirror(LEG))
m.bone("left_shin", [8, 10, 17], parent="left_leg", cubes=mirror(SHIN))

# 尻尾：4節。上に刃、先は刃の生えた棍棒
m.bone("tail1", [0, 21, 20], parent="body", rotation=[8, 0, 0], cubes=[
    ([-4, 17, 20], [8, 8, 10], "hide", "tail"),
] + blade(0, 25, 24, 4, 2))
m.bone("tail2", [0, 21, 30], parent="tail1", rotation=[6, 0, 0], cubes=[
    ([-3, 18, 30], [6, 6, 10], "hide", "tail"),
] + blade(0, 24, 34, 4, 2))
m.bone("tail3", [0, 21, 40], parent="tail2", rotation=[6, 0, 0], cubes=[
    ([-2.5, 18.5, 40], [5, 5, 10], "hide", "tail"),
] + blade(0, 23.5, 44, 3, 2))
m.bone("tail_tip", [0, 21, 50], parent="tail3", rotation=[6, 0, 0], cubes=[
    ([-3.5, 17.5, 50], [7, 7, 7], "hide", "tail_club"),
    ([-7, 20, 52], [4, 1, 2], "blade", "tail_spike"),
    ([3, 20, 52], [4, 1, 2], "blade", "tail_spike"),
    ([-0.5, 24.5, 52], [1, 4, 2], "blade", "tail_spike"),
    ([-0.5, 13.5, 52], [1, 4, 2], "blade", "tail_spike"),
    ([-1, 19, 57], [2, 3, 6], "blade", "tail_spike"),
])

# 尻尾の左右にも刃を並べる（外向き）
for name, half, z, y in (("tail1", 4, 24, 21), ("tail2", 3, 34, 21), ("tail3", 2.5, 44, 21)):
    for zz in (z - 2, z + 3):
        m.bone(f"{name}_side_{zz}", [0, y, zz], parent=name, rotation=[0, 0, 0], cubes=[
            ([-half - 4, y, zz], [4, 1, 2], "blade", "tail_side"),
            ([half, y, zz], [4, 1, 2], "blade", "tail_side"),
        ])
# 太ももの外側の刃
for side in (-1, 1):
    x = side * 11.5
    m.bone(f"thigh_blade_{'r' if side < 0 else 'l'}", [x, 18, 13], parent=f"{'right' if side < 0 else 'left'}_leg",
           rotation=[-45, 0, side * 15], cubes=blade(x, 18, 13, 6, 3))
# 前腕にもう一枚ずつ大きな刃
for side in (-1, 1):
    x = side * 13
    m.bone(f"fore_big_blade_{'r' if side < 0 else 'l'}", [x, 10, -9], parent=f"{'right' if side < 0 else 'left'}_fore",
           rotation=[-55, 0, side * 10], cubes=blade(x, 10, -9, 9, 3))

# ---------------------------------------------------------------- texture
PAL = {
    "hide": ("#17181f", "#2c2f3c", "#08080b"),
    "underside": ("#2a2c36", "#3c3f4c", "#121318"),
    "membrane": ("#17090d", "#2a1016", "#0a0406"),
    "blade": ("#8a909c", "#e2e7ef", "#3a3f48"),
    "fang": ("#d8d2c0", "#f4f0e2", "#8a8474"),
}
EYE = [hexc("#ffcc33"), hexc("#fff0a0")]
EDGE = hexc("#f4f8ff")


def details(m, mat, tag, face, rect):
    fx, fy, fw, fh = rect
    r = m.rng
    # 黒い鱗の照り
    if mat == "hide" and face not in ("down",):
        for _ in range(max(1, fw * fh // 14)):
            m.put(fx + r.randrange(fw), fy + r.randrange(fh), hexc("#262a38"))
        if face == "up":
            for k in range(0, fw, 2):
                for j in range(fh):
                    if (j + k // 2) % 3 == 0:
                        m.put(fx + k, fy + j, hexc("#1f2230"))
    # 目：細く鋭い金色の目（第2形態で赤に変える）
    if tag == "skull" and face in ("west", "east"):
        for k in range(1, 5):
            m.put(fx + k, fy + 3, EYE[0], glow=True)
        m.put(fx + 2, fy + 3, EYE[1], glow=True)
    if tag == "skull" and face == "north":
        m.put(fx + 1, fy + 3, EYE[0], glow=True)
        m.put(fx + fw - 2, fy + 3, EYE[0], glow=True)
    if tag == "jaw" and face in ("north", "west", "east"):
        for k in range(fw):
            if k % 2 == 0:
                m.put(fx + k, fy, hexc("#f4f0e2"))
    # 刃：刃先が白く光る（発光はさせない）
    if mat == "blade":
        for j in range(fh):
            m.put(fx, fy + j, EDGE)
        for k in range(fw):
            m.put(fx + k, fy, EDGE)
    # 翼膜：血管
    if mat == "membrane" and face in ("west", "east"):
        for k in range(0, fw, 4):
            for j in range(fh):
                if r.random() < 0.7:
                    m.put(fx + k, fy + j, hexc("#5a1e28"))


m.paint(PAL, details, glow_mats=set(), outline_skip={"membrane"},
        soft={"mats": {"hide", "underside", "membrane"}, "y": (0, 34), "dark": 0.55})
m.save()
