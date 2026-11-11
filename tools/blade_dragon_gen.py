"""影刃竜：モデル＋テクスチャ＋発光マスク。 python3 tools/blade_dragon_gen.py
四つ足：翼が前脚を兼ねる。体は Z 方向に長く、正面は -Z。
全身からいろいろな剣が生えている。剣は根元（鍔）を体にうめて、刀身が外を向く。
"""
import os, sys
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from geo_tools import Model, mirror, hexc

m = Model("blade_dragon", 256, 256, seed=41)

# ---------------------------------------------------------------- 剣のかたち（根元 x,y,z から +Y 方向へ伸びる）
def sword(kind, x, y, z, L):
    if kind == "great":        # 大剣：幅広くて長い、鉄色
        return [([x - 0.5, y, z - 3], [1, 2, 6], "darkiron", "guard"),
                ([x - 0.5, y + 2, z - 1.5], [1, L, 3], "darkiron_blade", "great"),
                ([x - 0.5, y + 2 + L, z - 1], [1, 2, 2], "darkiron_blade", "great"),
                ([x - 0.5, y + 4 + L, z - 0.5], [1, 1, 1], "darkiron_blade", "great")]
    if kind == "broad":        # ブロードソード：金の十字鍔
        return [([x - 0.5, y, z - 2.5], [1, 1, 5], "gold", "guard"),
                ([x - 0.5, y + 1, z - 1.5], [1, L, 3], "steel", "broad"),
                ([x - 0.5, y + 1 + L, z - 1], [1, 2, 2], "steel", "broad"),
                ([x - 0.5, y + 3 + L, z - 0.5], [1, 1, 1], "steel", "broad")]
    if kind == "katana":       # 刀：細長く、丸い鍔、先が少し反る
        return [([x - 0.5, y, z - 1.5], [1, 1, 3], "tsuba", "guard"),
                ([x - 0.5, y + 1, z - 0.5], [1, L, 1], "bright", "katana"),
                ([x - 0.5, y + 1 + L, z], [1, 2, 1], "bright", "katana")]
    if kind == "long":         # ロングソード：まっすぐな両刃
        return [([x - 0.5, y, z - 2], [1, 1, 4], "steel", "guard"),
                ([x - 0.5, y + 1, z - 1], [1, L, 2], "steel", "long"),
                ([x - 0.5, y + 1 + L, z - 0.5], [1, 2, 1], "steel", "long")]
    if kind == "serrated":     # ノコギリ刃
        cubes = [([x - 0.5, y, z - 2], [1, 1, 4], "darkiron", "guard"),
                 ([x - 0.5, y + 1, z - 1], [1, L, 2], "steel", "serrated"),
                 ([x - 0.5, y + 1 + L, z - 0.5], [1, 2, 1], "steel", "serrated")]
        for i in range(2, L, 3):
            cubes.append(([x - 0.5, y + 1 + i, z + 1], [1, 1, 1], "steel", "tooth"))
        return cubes
    if kind == "cursed":       # 呪いの剣：黒い刀身、赤く光る刃
        return [([x - 0.5, y, z - 2], [1, 1, 4], "darkiron", "guard"),
                ([x - 0.5, y + 1, z - 1], [1, L, 2], "cursed", "cursed"),
                ([x - 0.5, y + 1 + L, z - 0.5], [1, 2, 1], "cursed", "cursed")]
    raise ValueError(kind)


_n = [0]
def plant(parent, kind, x, y, z, L, rot):
    """parent の (x,y,z) に剣を1本生やす。rot = [前後の倒れ(X), ひねり(Y), 左右の倒れ(Z)]"""
    _n[0] += 1
    m.bone(f"sword{_n[0]}", [x, y, z], parent=parent, rotation=rot, cubes=sword(kind, x, y, z, L))


# ---------------------------------------------------------------- body
m.bone("root", [0, 0, 0])
m.bone("body", [0, 20, 0], parent="root", cubes=[
    ([-7, 14, -16], [14, 13, 14], "hide", "chest"),
    ([-6.5, 13, -2], [13, 12, 14], "hide", "belly"),
    ([-6, 14, 12], [12, 11, 8], "hide", "hips"),
    ([-5, 12, -14], [10, 2, 24], "underside", "belly_plate"),
])
# 背中：大小いろいろな剣が後ろへ流れるように並ぶ
SPINE = [("great", -14, 14), ("broad", -9, 13), ("katana", -4, 15), ("cursed", 1, 12),
         ("serrated", 6, 11), ("long", 11, 10), ("katana", 16, 9)]
for i, (kind, z, L) in enumerate(SPINE):
    y = 26 if z < 12 else 24
    plant("body", kind, 0, y, z, L, [-42, 0, (6 if i % 2 else -6)])
# 肩：左右に斜めに2本ずつ
for side in (-1, 1):
    for kind, z, L in (("long", -13, 11), ("katana", -8, 12)):
        plant("body", kind, side * 6.5, 25, z, L, [-30, 0, -side * 40])

# 首と頭
m.bone("neck", [0, 24, -16], parent="body", rotation=[-25, 0, 0], cubes=[
    ([-4, 20, -26], [8, 8, 11], "hide", "neck"),
])
plant("neck", "katana", 0, 27, -23, 6, [-50, 0, 0])
plant("neck", "long", 0, 27, -19, 7, [-50, 0, 0])
m.bone("head", [0, 24, -26], parent="neck", rotation=[22, 0, 0], cubes=[
    ([-5, 20, -37], [10, 9, 11], "hide", "skull"),
    ([-3.5, 20, -43], [7, 6, 6], "hide", "snout"),
    ([-3.5, 17, -42], [7, 3, 12], "hide", "jaw"),
    ([-3, 20, -42.5], [6, 1, 1], "fang", "teeth"),
])
# 耳のように後ろへ流れる2本の刀
for side in (-1, 1):
    plant("head", "katana", side * 4, 28, -30, 13, [-62, 0, -side * 18])

# 前脚（翼）
WING = [
    ([-12, 12, -12.5], [5, 14, 5], "hide", "wing_upper"),
    ([-13.5, 12, -9], [1, 13, 15], "membrane", "membrane"),
]
FORE = [
    ([-12, 0, -12.5], [5, 13, 5], "hide", "forearm"),
    ([-12.5, 0, -15], [6, 2, 3], "fang", "claw"),
]
m.bone("right_wing", [-7, 24, -10], parent="body", cubes=WING)
m.bone("right_fore", [-9.5, 13, -10], parent="right_wing", cubes=FORE)
m.bone("left_wing", [7, 24, -10], parent="body", cubes=mirror(WING))
m.bone("left_fore", [9.5, 13, -10], parent="left_wing", cubes=mirror(FORE))
# 翼のふち：外向きに扇状に開いた長い剣（いちばん長い）
for side, name in ((-1, "right"), (1, "left")):
    for (kind, y, L, back) in (("great", 25, 17, -15), ("cursed", 22, 15, -25), ("long", 19, 13, -35), ("serrated", 16, 11, -45)):
        plant(f"{name}_wing", kind, side * 12, y, -11, L, [back, 0, -side * 78])
    plant(f"{name}_fore", "broad", side * 12.5, 9, -10, 12, [-55, 0, -side * 12])

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
for side, name in ((-1, "right_leg"), (1, "left_leg")):
    plant(name, "serrated", side * 11, 17, 13, 10, [-45, 0, -side * 25])

# 尻尾：上と左右に剣、先は1本の大剣
m.bone("tail1", [0, 21, 20], parent="body", rotation=[8, 0, 0], cubes=[([-4, 17, 20], [8, 8, 10], "hide", "tail")])
m.bone("tail2", [0, 21, 30], parent="tail1", rotation=[6, 0, 0], cubes=[([-3, 18, 30], [6, 6, 10], "hide", "tail")])
m.bone("tail3", [0, 21, 40], parent="tail2", rotation=[6, 0, 0], cubes=[([-2.5, 18.5, 40], [5, 5, 10], "hide", "tail")])
m.bone("tail_tip", [0, 21, 50], parent="tail3", rotation=[6, 0, 0], cubes=[([-2, 19, 50], [4, 4, 4], "hide", "tail_end")])
for name, top, half, z, kinds in (("tail1", 25, 4, 24, ("long", "katana")), ("tail2", 24, 3, 34, ("serrated", "long")),
                                  ("tail3", 23.5, 2.5, 44, ("katana", "cursed"))):
    plant(name, kinds[0], 0, top, z, 8, [-50, 0, 0])
    for side in (-1, 1):
        plant(name, kinds[1], side * half, 21, z + 2, 7, [-35, 0, -side * 75])
plant("tail_tip", "great", 0, 21, 53, 16, [-90, 0, 0])

# ---------------------------------------------------------------- texture
PAL = {
    "hide": ("#17181f", "#2c2f3c", "#08080b"),
    "underside": ("#2a2c36", "#3c3f4c", "#121318"),
    "membrane": ("#17090d", "#2a1016", "#0a0406"),
    "fang": ("#d8d2c0", "#f4f0e2", "#8a8474"),
    "steel": ("#8f96a3", "#e2e7ef", "#3e434c"),
    "bright": ("#c9d1dc", "#ffffff", "#6a7380"),
    "darkiron": ("#3d4048", "#5e626c", "#1a1b1f"),
    "darkiron_blade": ("#4a4e58", "#8a909c", "#1e2025"),
    "gold": ("#b8902a", "#efcf6a", "#5e4710"),
    "tsuba": ("#2c2420", "#4a3d34", "#120e0b"),
    "cursed": ("#1b0a0e", "#3a1218", "#0a0305"),
}
EYE = [hexc("#ffcc33"), hexc("#fff0a0")]
EDGE = hexc("#f4f8ff")
RED = [hexc("#ff3048"), hexc("#ff8090")]


def details(m, mat, tag, face, rect):
    fx, fy, fw, fh = rect
    r = m.rng
    if mat == "hide" and face not in ("down",):
        for _ in range(max(1, fw * fh // 14)):
            m.put(fx + r.randrange(fw), fy + r.randrange(fh), hexc("#262a38"))
    if tag == "skull" and face in ("west", "east"):
        for k in range(1, 5):
            m.put(fx + k, fy + 3, EYE[0], glow=True)
        m.put(fx + 2, fy + 3, EYE[1], glow=True)
    if tag == "skull" and face == "north":
        m.put(fx + 1, fy + 3, EYE[0], glow=True)
        m.put(fx + fw - 2, fy + 3, EYE[0], glow=True)
    if tag == "jaw" and face in ("north", "west", "east"):
        for k in range(0, fw, 2):
            m.put(fx + k, fy, hexc("#f4f0e2"))
    if mat == "membrane" and face in ("west", "east"):
        for k in range(0, fw, 4):
            for j in range(fh):
                if r.random() < 0.6:
                    m.put(fx + k, fy + j, hexc("#3a141c"))
    # ---- 剣の描き込み（刀身の平らな面 = west/east）
    flat = face in ("west", "east")
    if tag in ("great", "broad", "long", "serrated") and flat:
        for j in range(fh):                               # 両側の刃先は白く、中央に樋（溝）
            m.put(fx, fy + j, EDGE)
            m.put(fx + fw - 1, fy + j, EDGE)
            if fw >= 3:
                m.put(fx + fw // 2, fy + j, hexc("#5a606c") if mat != "darkiron_blade" else hexc("#2a2c32"))
    if tag == "katana" and flat:
        for j in range(fh):                               # 刃紋（白く波打つ線）
            m.put(fx, fy + j, hexc("#ffffff") if j % 3 else hexc("#dfe6ef"))
    if tag == "cursed":
        for j in range(fh):                               # 赤く光る刃
            if flat:
                m.put(fx, fy + j, RED[0], glow=True)
                m.put(fx + fw - 1, fy + j, RED[1] if j % 2 else RED[0], glow=True)
            elif face == "north":
                m.put(fx, fy + j, RED[0], glow=True)
    if mat == "gold" and face not in ("down",):
        for k in range(fw):
            if (k + fx) % 2 == 0:
                m.put(fx + k, fy, hexc("#8a6a18"))
    if tag == "tooth":
        for j in range(fh):
            for k in range(fw):
                m.put(fx + k, fy + j, EDGE)


m.paint(PAL, details, glow_mats=set(), outline_skip={"membrane", "bright", "cursed"},
        soft={"mats": {"hide", "underside", "membrane"}, "y": (0, 34), "dark": 0.55})
m.save()
