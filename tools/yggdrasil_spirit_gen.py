"""ユグドラシルの精霊（高精細）：モデル＋テクスチャ＋発光マスク。 python3 tools/yggdrasil_spirit_gen.py
第3形態の神樹が倒れると、幹の中から現れる人型の精霊（第4・第5形態）。
緑の長い髪に葉と白い花、枝の冠と背中の光輪、白と淡い緑の重ね着のドレス（裾に金の根の模様）、
世界樹の枝が絡み合った杖（先に緑の結晶）。足は見えず、ドレスの裾ごと浮いている。
正面は -Z。身長 約3.6ブロック（杖・光輪を除く）。
"""
import math, os, sys
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from geo_tools import Model, mirror, hexc
import numpy as np

m = Model("yggdrasil_spirit", 512, 512, seed=33, density=2)


def B(o, s, mat, tag, rot=None, pivot=None):
    x = {}
    if rot: x["rot"] = list(rot)
    if pivot: x["pivot"] = list(pivot)
    return (list(o), list(s), mat, tag, x) if x else (list(o), list(s), mat, tag)


# ================================================================ model
m.bone("root", [0, 0, 0])
m.bone("hips", [0, 30, 0], parent="root", cubes=[
    B([-3.4, 29, -2.4], [6.8, 3, 4.8], "dress", "hip_core"),
    B([-3.8, 31.5, -2.8], [7.6, 1.4, 5.6], "gold", "sash"),
    B([-1.2, 30.6, -3.3], [2.4, 2.4, 1], "crystal", "sash_gem"),
])


def skirt_ring(name, n, top, h, r, tilt, w, mat, tag, phase=0.0, thick=0.6):
    """ドレスの段：パネルを輪に並べ、外へ広げる（パネル1枚ごとに骨を作り、骨で向きを回す）"""
    for k in range(n):
        yaw = 360.0 * (k + phase) / n
        m.bone(f"{name}{k}", [0, top, 0], parent="hips", rotation=[0, yaw, 0], cubes=[
            B([-w / 2, top - h, -r - thick], [w, h, thick], mat, tag, rot=[-tilt, 0, 0], pivot=[0, top, -r])])


# 外側の白いオーバースカート（前が少し短い感じは段の重なりで出す）
skirt_ring("skirt_a", 12, 31, 15, 4.4, 24, 4.6, "dress", "skirt_a")
# その下の淡い緑の長いスカート
skirt_ring("skirt_b", 14, 28.5, 22, 5.6, 19, 5.4, "green", "skirt_b", phase=0.5)
# いちばん下：白いフリルの裾
skirt_ring("skirt_c", 16, 12, 9, 11.2, 9, 5.0, "dress", "skirt_c", phase=0.25)

# 裾からほどけて垂れる葉
LEAVES = []
for k in range(10):
    a = 2 * math.pi * (k + 0.3) / 10
    r = 13.6
    x, z = math.sin(a) * r, -math.cos(a) * r
    LEAVES.append(B([x - 0.9, 1.2 - (k % 3) * 0.8, z - 0.25], [1.8, 3.2, 0.5], "leaf", "hem_leaf",
                    rot=[0, -math.degrees(a), 18 if k % 2 else -18], pivot=[x, 4, z]))
m.bone("hem_leaves", [0, 4, 0], parent="hips", cubes=LEAVES)

m.bone("torso", [0, 32, 0], parent="hips", cubes=[
    B([-2.8, 32, -2.0], [5.6, 4, 4.0], "dress", "waist"),
    B([-3.6, 35.5, -2.4], [7.2, 6, 4.8], "dress", "bodice"),
    B([-2.2, 32.4, -2.5], [4.4, 8.4, 0.6], "green", "bodice_front"),         # 前の緑の布（金の縁取り）
    B([-3.8, 39.5, -2.7], [7.6, 2.4, 5.4], "dress", "bust"),
    B([-3.4, 41.6, -2.2], [6.8, 1.0, 4.4], "skin", "collarbone"),
    B([-1.1, 42.4, -1.1], [2.2, 2.6, 2.2], "skin", "neck"),
    B([-0.8, 38.6, -3.25], [1.6, 1.6, 0.6], "crystal", "pendant"),
    # 肩のフリル（肩を出したドレス。葉の形の飾りを左右に）
    B([-5.6, 40.4, -2.4], [3.2, 1.4, 4.8], "dress", "frill", rot=[0, 0, 20], pivot=[-3.6, 41, 0]),
    B([2.4, 40.4, -2.4], [3.2, 1.4, 4.8], "dress", "frill", rot=[0, 0, -20], pivot=[3.6, 41, 0]),
    B([-5.9, 39.4, -2.2], [2.6, 1.2, 4.4], "green", "frill2", rot=[0, 0, 32], pivot=[-4.4, 40, 0]),
    B([3.3, 39.4, -2.2], [2.6, 1.2, 4.4], "green", "frill2", rot=[0, 0, -32], pivot=[4.4, 40, 0]),
    # 背中の大きなリボン（葉の形）
    B([-4.6, 33.5, 2.4], [4.2, 3, 0.8], "green", "bow", rot=[0, 0, -20], pivot=[0, 35, 2.6]),
    B([0.4, 33.5, 2.4], [4.2, 3, 0.8], "green", "bow", rot=[0, 0, 20], pivot=[0, 35, 2.6]),
    B([-0.8, 34.2, 2.5], [1.6, 1.6, 1], "gold", "bow_knot"),
])
m.bone("bow_tails", [0, 34, 2.8], parent="torso", cubes=[
    B([-2.2, 22, 2.6], [1.8, 12, 0.5], "green", "bow_tail", rot=[8, 0, 8], pivot=[-1.3, 34, 2.8]),
    B([0.4, 23, 2.6], [1.8, 11, 0.5], "green", "bow_tail", rot=[8, 0, -8], pivot=[1.3, 34, 2.8]),
])

# 腕：細い腕に、ひじから広がる袖
UPPER = [
    B([-5.6, 35, -1.0], [2.0, 6.5, 2.0], "skin", "upper_arm"),
    B([-5.9, 39.0, -1.3], [2.6, 2.4, 2.6], "dress", "sleeve_cuff"),
]
FORE = [
    B([-5.5, 27.5, -0.9], [1.8, 7.5, 1.8], "skin", "forearm"),
    B([-6.9, 26.2, -2.3], [4.4, 7.6, 4.6], "dress", "sleeve", rot=[0, 0, 8], pivot=[-4.6, 34, 0]),
    B([-6.7, 26.0, -2.1], [4.0, 1.0, 4.2], "gold", "sleeve_rim", rot=[0, 0, 8], pivot=[-4.6, 34, 0]),
    B([-5.6, 25.0, -1.1], [2.0, 2.6, 2.2], "skin", "hand"),
]
m.bone("right_arm", [-4.6, 41, 0], parent="torso", cubes=UPPER)
m.bone("right_forearm", [-4.6, 35, 0], parent="right_arm", cubes=FORE)
m.bone("left_arm", [4.6, 41, 0], parent="torso", cubes=mirror(UPPER))
m.bone("left_forearm", [4.6, 35, 0], parent="left_arm", cubes=mirror(FORE))

# 世界樹の杖：2本の枝がねじれ合った柄、先端は枝が開いて緑の結晶を包む
SX, SZ = -4.6, -0.1           # 手の位置
STAFF = []
for i in range(8):           # ねじれた柄（2本を少しずつ回しながら積む）
    y = 9.2 + i * 5.6
    for side in (0, 1):
        a = i * 0.9 + side * math.pi
        dx, dz = math.cos(a) * 0.45, math.sin(a) * 0.45
        STAFF.append(B([SX - 0.55 + dx, y, SZ - 0.55 + dz], [1.1, 6.0, 1.1], "wood", "staff",
                       rot=[0, i * 25 + side * 90, 0]))
STAFF.append(B([SX - 0.9, 24.2, SZ - 0.9], [1.8, 3.4, 1.8], "gold", "grip"))
STAFF.append(B([SX - 0.8, 53.6, SZ - 0.8], [1.6, 1.2, 1.6], "gold", "staff_ring"))
# 先端の枝（4本が外へ開いて、上で閉じる）
for k in range(4):
    a = k * 90 + 45
    STAFF.append(B([SX - 0.35, 54.5, SZ - 3.4], [0.7, 6.5, 0.7], "wood", "staff_branch",
                   rot=[-22, a, 0], pivot=[SX, 54.5, SZ]))
    STAFF.append(B([SX - 0.3, 60.4, SZ - 3.0], [0.6, 4.5, 0.6], "wood", "staff_branch2",
                   rot=[24, a, 0], pivot=[SX, 60.6, SZ - 2.6]))
    STAFF.append(B([SX - 1.0, 58.5, SZ - 3.6], [2.0, 1.0, 0.4], "leaf", "staff_leaf",
                   rot=[-10, a + 20, 30], pivot=[SX, 59, SZ - 3.4]))
m.bone("staff", [SX, 25.5, SZ], parent="right_forearm", cubes=STAFF)
m.bone("staff_crystal", [SX, 60.5, SZ], parent="staff", cubes=[
    B([SX - 1.3, 58.4, SZ - 1.3], [2.6, 4.2, 2.6], "crystal", "staff_crystal", rot=[0, 45, 0]),
    B([SX - 0.8, 62.4, SZ - 0.8], [1.6, 1.6, 1.6], "crystal", "staff_crystal_tip", rot=[0, 45, 0]),
    B([SX - 0.8, 57.2, SZ - 0.8], [1.6, 1.4, 1.6], "crystal", "staff_crystal_tip", rot=[0, 45, 0]),
])

# 第5形態の剣：杖の結晶が伸びて刃になった、光の結晶の片手剣（第4形態では隠す）
HX, HY, HZ = SX, 25.5, SZ
SWORD = [
    B([HX - 0.6, HY - 3.0, HZ - 0.6], [1.2, 5.6, 1.2], "wood", "hilt"),
    B([HX - 0.9, HY - 4.2, HZ - 0.9], [1.8, 1.4, 1.8], "crystal", "pommel", rot=[0, 45, 0]),
    B([HX - 3.2, HY + 2.6, HZ - 0.8], [6.4, 1.0, 1.6], "gold", "guard"),
    B([HX - 0.9, HY + 2.3, HZ - 1.0], [1.8, 1.6, 2.0], "crystal", "guard_gem"),
    # 鍔の左右に広がる葉
    B([HX - 6.2, HY + 2.8, HZ - 0.3], [3.4, 1.4, 0.6], "leaf", "guard_leaf", rot=[0, 0, 25], pivot=[HX - 3, HY + 3, HZ]),
    B([HX + 2.8, HY + 2.8, HZ - 0.3], [3.4, 1.4, 0.6], "leaf", "guard_leaf", rot=[0, 0, -25], pivot=[HX + 3, HY + 3, HZ]),
    # 刃（光の結晶）と、根元に絡む枝
    B([HX - 1.9, HY + 3.6, HZ - 0.4], [3.8, 20, 0.8], "blade", "blade"),
    B([HX - 0.4, HY + 3.6, HZ - 0.55], [0.8, 19, 1.1], "crystal", "blade_core"),
    B([HX - 1.35, HY + 22.3, HZ - 0.4], [2.7, 2.7, 0.8], "blade", "blade_tip", rot=[0, 0, 45], pivot=[HX, HY + 23.6, HZ]),
    B([HX - 1.6, HY + 3.4, HZ - 0.7], [0.6, 4.5, 0.6], "wood", "blade_vine", rot=[0, 0, -14], pivot=[HX, HY + 3.4, HZ]),
    B([HX + 1.0, HY + 3.4, HZ - 0.7], [0.6, 4.0, 0.6], "wood", "blade_vine", rot=[0, 0, 14], pivot=[HX, HY + 3.4, HZ]),
]
m.bone("sword", [HX, HY, HZ], parent="right_forearm", rotation=[30, 0, 0], cubes=SWORD)

# 頭
m.bone("head", [0, 44.5, 0], parent="torso", cubes=[
    B([-3.2, 44.5, -3.2], [6.4, 6.6, 6.4], "skin", "face"),
    B([-3.5, 48.6, -3.5], [7.0, 3.0, 7.0], "hair", "hair_cap"),
    B([-3.5, 44.5, 0.2], [7.0, 4.4, 3.3], "hair", "hair_back_head"),
    B([-3.45, 48.0, -3.55], [6.9, 1.4, 1.0], "hair", "bangs"),                 # 前髪
    B([-3.4, 46.6, -3.55], [1.6, 2.0, 1.0], "hair", "bangs_side"),
    B([1.8, 46.6, -3.55], [1.6, 2.0, 1.0], "hair", "bangs_side"),
    B([-0.6, 46.9, -3.6], [1.2, 1.6, 0.8], "hair", "bangs_mid", rot=[0, 0, 12]),
    # 横に流れる長い房（顔の両横から胸まで）
    B([-4.1, 37.5, -2.4], [1.4, 11.5, 2.2], "hair", "side_lock", rot=[6, 0, -4], pivot=[-3.4, 49, -1.3]),
    B([2.7, 37.5, -2.4], [1.4, 11.5, 2.2], "hair", "side_lock", rot=[6, 0, 4], pivot=[3.4, 49, -1.3]),
    # 耳（少しとがった精霊の耳）
    B([-4.2, 46.2, -0.8], [1.2, 2.4, 1.2], "skin", "ear", rot=[0, 0, 30], pivot=[-3.4, 46.8, -0.2]),
    B([3.0, 46.2, -0.8], [1.2, 2.4, 1.2], "skin", "ear", rot=[0, 0, -30], pivot=[3.4, 46.8, -0.2]),
])
# 後ろ髪（2段に分けてなびかせる）
m.bone("hair_back", [0, 50, 3.0], parent="head", rotation=[6, 0, 0], cubes=[
    B([-4.0, 36, 2.4], [8.0, 14, 1.6], "hair", "hair_long"),
    B([-4.4, 38, 1.8], [1.6, 11, 1.4], "hair", "hair_strand", rot=[0, 0, -6], pivot=[-3.6, 50, 2.5]),
    B([2.8, 38, 1.8], [1.6, 11, 1.4], "hair", "hair_strand", rot=[0, 0, 6], pivot=[3.6, 50, 2.5]),
])
m.bone("hair_back_low", [0, 36, 3.4], parent="hair_back", rotation=[8, 0, 0], cubes=[
    B([-3.6, 22, 2.4], [7.2, 14.4, 1.4], "hair", "hair_low"),
    B([-3.9, 20, 2.5], [2.2, 4, 1.2], "hair", "hair_tip", rot=[0, 0, 8], pivot=[-2.8, 22, 3]),
    B([-1.1, 19, 2.5], [2.2, 4, 1.2], "hair", "hair_tip"),
    B([1.7, 20, 2.5], [2.2, 4, 1.2], "hair", "hair_tip", rot=[0, 0, -8], pivot=[2.8, 22, 3]),
])

# 髪の葉と白い花
FLOWERS = [
    B([-4.1, 49.5, -2.0], [1.6, 1.6, 1.6], "flower", "flower", rot=[0, 0, 45]),
    B([-4.0, 47.4, 0.8], [1.2, 1.2, 1.2], "flower", "flower", rot=[0, 0, 45]),
    B([-4.3, 48.0, -0.8], [0.6, 2.6, 1.4], "leaf", "hair_leaf", rot=[30, 0, -20]),
    B([-4.2, 50.2, 0.6], [0.6, 2.2, 1.2], "leaf", "hair_leaf", rot=[-30, 0, -30]),
    B([2.7, 50.0, 1.4], [1.3, 1.3, 1.3], "flower", "flower", rot=[0, 0, 45]),
    B([3.6, 49.0, -0.4], [0.6, 2.4, 1.2], "leaf", "hair_leaf", rot=[20, 0, 25]),
]
m.bone("hair_flowers", [0, 48, 0], parent="head", cubes=FLOWERS)

# 枝の冠：細い枝が上へ伸び、正面に小さな結晶
CROWN = [B([-3.6, 51.0, -3.6], [7.2, 0.8, 7.2], "wood", "crown_band")]
for k, (x, z, h, rz, rx) in enumerate([(-2.8, -3.4, 4.0, 22, -10), (-1.2, -3.6, 3.0, 8, -12), (1.2, -3.6, 3.0, -8, -12),
                                         (2.8, -3.4, 4.0, -22, -10), (-3.4, 0, 3.4, 30, 0), (3.4, 0, 3.4, -30, 0),
                                         (-2.4, 3.2, 3.0, 20, 12), (2.4, 3.2, 3.0, -20, 12)]):
    CROWN.append(B([x - 0.3, 51.6, z - 0.3], [0.6, h, 0.6], "wood", "crown_twig", rot=[rx, 0, rz], pivot=[x, 51.6, z]))
    if k < 4:
        CROWN.append(B([x - 0.6, 51.6 + h * 0.6, z - 0.5], [1.4, 0.8, 0.4], "leaf", "crown_leaf", rot=[rx, 0, rz + (40 if x < 0 else -40)], pivot=[x, 51.6, z]))
CROWN.append(B([-0.7, 51.4, -3.9], [1.4, 1.8, 0.6], "crystal", "crown_gem", rot=[0, 0, 45]))
m.bone("crown", [0, 51, 0], parent="head", cubes=CROWN)

# 背中の光輪（枝の輪に光る粒。アニメーションでゆっくり回る）
HALO = []
for k in range(18):
    a = 2 * math.pi * k / 18
    x, y = math.cos(a) * 8.5, 49 + math.sin(a) * 8.5
    HALO.append(B([x - 1.0, y - 0.35, 7.2], [2.0, 0.7, 0.5], "halo", "halo", rot=[0, 0, math.degrees(a) + 90]))
    if k % 3 == 0:
        HALO.append(B([x - 0.6, y - 0.6, 7.0], [1.2, 1.2, 0.6], "crystal", "halo_gem", rot=[0, 0, 45]))
m.bone("halo", [0, 49, 7.4], parent="torso", cubes=HALO)

# 第5形態の光の翼（半透明の羽根の板を3段の扇形に重ねる。第4形態では隠す）
WING = []
for i in range(14):                        # 風切り羽：上から足元まで放射状に広がる光の刃
    t = i / 13
    ang = -72 + t * 150                        # 上 -72° 〜 下 +78°
    L = 56 - abs(t - 0.3) * 40                 # 斜め上がいちばん長く、下へいくほど短い
    WING.append(B([-3 - L, 39.2, 3.6 + i * 0.1], [L, 6.0, 0.4], "lightwing", "primary", rot=[0, 0, ang], pivot=[-3, 40.8, 3.8]))
for i in range(10):                        # 次列風切り
    L = 33 - i * 1.4
    ang = -60 + i * 14
    WING.append(B([-3 - L, 39.3, 3.1 + i * 0.1], [L, 5.6, 0.4], "lightwing", "secondary", rot=[0, 0, ang], pivot=[-3, 40.8, 3.3]))
for i in range(6):                         # 雨覆い（根元の短い羽）
    L = 16 - i * 0.9
    ang = -30 + i * 12
    WING.append(B([-3 - L, 39.6, 2.7 + i * 0.08], [L, 4.6, 0.4], "lightwing", "covert", rot=[0, 0, ang], pivot=[-3, 40.8, 2.9]))
WING.append(B([-9, 39.6, 2.6], [7, 2.2, 1.0], "crystal", "wing_root", rot=[0, 0, -18], pivot=[-3, 40.8, 3]))
m.bone("wings", [0, 40, 3], parent="torso")
m.bone("wing_right", [-2.0, 40.8, 3.2], parent="wings", rotation=[-6, 22, 0], cubes=WING)
LW = [c[:3] + (c[3] + "_l",) + c[4:] if c[2] == "lightwing" else c for c in mirror(WING)]
m.bone("wing_left", [2.0, 40.8, 3.2], parent="wings", rotation=[-6, -22, 0], cubes=LW)

# ================================================================ texture
PAL = {
    "skin": ("#f4e0d2", "#fff0e6", "#c8a898"),
    "hair": ("#3cb878", "#8af0b4", "#1d6e46"),
    "dress": ("#eef4ee", "#ffffff", "#aec4b8"),
    "green": ("#8edcb0", "#c8f8dc", "#3e9a6c"),
    "gold": ("#d8b860", "#fff0b0", "#8a6e28"),
    "wood": ("#6a4a2c", "#9a7448", "#36240f"),
    "leaf": ("#4cc070", "#9af0a8", "#1e7a3a"),
    "flower": ("#fff6fa", "#ffffff", "#f0b8cc"),
    "crystal": ("#6affb0", "#e0ffee", "#20c070"),
    "halo": ("#c8ffb0", "#ffffff", "#7ad890"),
    "lightwing": ("#b8ffd8", "#ffffff", "#60e0a0"),
    "blade": ("#a8ffd0", "#ffffff", "#40d090"),
}
GLOW = [hexc("#30d080"), hexc("#8affc0"), hexc("#f0fff6")]
GOLD = hexc("#d8b860")
GOLD_DK = hexc("#a8862e")


def root_pattern(m, rect, seed, strength=1.0, glow_from=None):
    """パネルの下から上へ枝分かれする金の根の模様"""
    fx, fy, fw, fh = rect
    r = m.rng
    for start in range(1):
        x = r.uniform(1, fw - 2)
        y = fh - 1
        width = 2 if fw > 6 else 1
        while y > fh * (0.25 + r.random() * 0.25):
            for d in range(width):
                xx = int(x) + d
                if 0 <= xx < fw:
                    glow = glow_from is not None and y > fh * glow_from
                    m.put(fx + xx, fy + int(y), GLOW[0] if glow else GOLD, glow=glow)
            x += r.choice((-1, 0, 0, 1)) * 0.8
            x = min(fw - 2, max(1, x))
            y -= 1
            if r.random() < 0.08 and width > 1:
                width = 1
            if r.random() < 0.035:                      # 枝分かれ
                bx, by = x, y
                dirx = r.choice((-1, 1))
                for _ in range(r.randrange(3, 7)):
                    bx += dirx * 0.9; by -= 0.7
                    if 0 <= int(bx) < fw and 0 <= int(by) < fh:
                        m.put(fx + int(bx), fy + int(by), GOLD)


TINT_RECTS = []   # 属性の色に変える場所（光の翼・結晶の剣）


def details(m, mat, tag, face, rect):
    fx, fy, fw, fh = rect
    if mat in ("lightwing", "blade") or tag in ("wing_root", "blade_core", "pommel", "guard_gem"):
        TINT_RECTS.append(rect)
    r = m.rng
    side = face in ("north", "south", "east", "west")
    if mat == "dress" and side:
        # 布のひだ（縦の淡い影）
        for k in range(0, fw, 4):
            for j in range(fh):
                m.put(fx + k, fy + j, hexc("#d4e2da"))
        if tag in ("skirt_a", "skirt_c") and face == "north":
            for k in range(fw):                         # 裾の緑と金の二重線
                m.put(fx + k, fy + fh - 1, hexc("#3e9a6c"))
                m.put(fx + k, fy + fh - 2, hexc("#8edcb0"))
                m.put(fx + k, fy + fh - 4, GOLD)
            if tag == "skirt_a":                        # 白い布には葉の模様をひとつ
                cx = fw // 2
                for j in range(fh - 14, fh - 6):
                    w = int(2.2 * math.sin((j - (fh - 14)) / 8 * math.pi))
                    for d in range(-w, w + 1):
                        m.put(fx + cx + d, fy + j, hexc("#8edcb0"))
                    m.put(fx + cx, fy + j, GOLD)
        if tag == "skirt_c" and face in ("north", "south"):
            for k in range(fw):                         # 裾を葉っぱの形に（波形に抜く）
                cut = int(2.5 + 2.5 * math.sin(k / fw * math.pi * 2))
                for j in range(fh - cut, fh):
                    if j > fh - 1:
                        continue
                    m.clear(fx + k, fy + j)
        if tag == "bodice" and face == "north":
            for j in range(fh):
                m.put(fx + 1, fy + j, GOLD); m.put(fx + fw - 2, fy + j, GOLD)
        if tag in ("sleeve", "frill") and face in ("north", "south", "east", "west"):
            for k in range(fw):
                m.put(fx + k, fy + fh - 1, hexc("#3e9a6c"))
    if mat == "green" and side:
        for k in range(0, fw, 5):
            for j in range(fh):
                m.put(fx + k, fy + j, hexc("#6cc494"))
        if tag == "skirt_b" and face == "north":
            root_pattern(m, rect, r.random(), glow_from=0.9)
            for k in range(fw):
                m.put(fx + k, fy + fh - 1, GOLD)
        if tag == "bodice_front" and face == "north":
            for j in range(fh):                         # 金の縁取りと葉の模様
                m.put(fx, fy + j, GOLD); m.put(fx + fw - 1, fy + j, GOLD)
            cx = fw // 2
            for j in range(2, fh - 2, 3):
                for d in (-1, 0, 1):
                    m.put(fx + cx + d, fy + j + abs(d), GOLD)
    if mat == "gold" and face != "down":
        for k in range(0, fw, 3):
            m.put(fx + k, fy + fh // 2, hexc("#fff4c8"))
    if mat == "hair" and face != "down":
        # 髪の流れ：縦の明るい筋と暗い筋
        for k in range(fw):
            if k % 3 == 0:
                for j in range(fh):
                    m.put(fx + k, fy + j, hexc("#2a9a60"))
            elif k % 5 == 1:
                for j in range(0, fh, 2):
                    m.put(fx + k, fy + j, hexc("#7ae0a8"))
        if tag in ("hair_cap", "hair_back_head") and face in ("north", "up"):
            for k in range(fw):                         # 天使の輪（つや）
                j = fh // 2 if face == "north" else fh // 3
                m.put(fx + k, fy + j, hexc("#b8ffd8"))
        if tag in ("hair_tip", "hair_low", "side_lock") and face in ("north", "south", "east", "west"):
            for k in range(fw):                         # 毛先はとがらせる
                cut = (k * 7 + 3) % 4
                for j in range(fh - cut, fh):
                    m.clear(fx + k, fy + j)
    if mat == "skin" and tag == "face" and face == "north":
        # 顔：16x13 くらい。目（緑で光る）、まつげ、口
        cx = fw // 2
        ey = fh - 6                                    # 前髪の下に目がくるように
        for sgn in (-1, 1):
            ex = cx + sgn * 3 - (1 if sgn < 0 else 0)
            for d in range(-1, 3):
                m.put(fx + ex + d - 1 + (1 if sgn > 0 else 0), fy + ey - 1, hexc("#163a28"))   # まつげ
            for d in range(2):
                m.put(fx + ex + d - (0 if sgn > 0 else 0), fy + ey, GLOW[1], glow=True)
                m.put(fx + ex + d - (0 if sgn > 0 else 0), fy + ey + 1, GLOW[0], glow=True)
            m.put(fx + ex + (1 if sgn < 0 else 0), fy + ey, GLOW[2], glow=True)                   # 目の光
            m.put(fx + ex + sgn * 2 + (1 if sgn < 0 else 0), fy + ey + 2, hexc("#f4c4c0"))         # ほお
        m.put(fx + cx - 1, fy + ey + 3, hexc("#e0a0a0"))
        m.put(fx + cx, fy + ey + 3, hexc("#e0a0a0"))
    if mat == "wood" and face != "down":
        for j in range(fh):
            for k in range(fw):
                if (j + k * 3) % 5 == 0:
                    m.put(fx + k, fy + j, hexc("#4a321c"))
        if tag == "staff":
            for j in range(0, fh, 7):
                for k in range(fw):
                    m.put(fx + k, fy + j, GLOW[0], glow=True) if (j // 7) % 3 == 1 else None
    if mat == "leaf":
        for j in range(fh):
            m.put(fx + fw // 2, fy + j, hexc("#2e9a4a"))
    if mat == "flower" and face != "down":
        m.put(fx + fw // 2, fy + fh // 2, hexc("#ffd860"))
    if mat == "lightwing":
        # 光の羽根：半透明。根元はうすく、先へいくほど明るい。中心の軸と縁が強く光り、先はとがる
        long_side = face in ("north", "south", "up", "down")
        for j in range(fh):
            for k in range(fw):
                t = k / max(1, fw - 1)                    # 0 = 先（外側）, 1 = 根元
                if (face == "south") != tag.endswith("_l"):
                    t = 1 - t
                edge = j == 0 or j == fh - 1
                axis = abs(j - (fh - 1) / 2) < 0.8
                tip_w = (fh / 2) * min(1.0, (1 - t) * 0 + t * 6) if long_side else fh
                if long_side and abs(j - (fh - 1) / 2) > tip_w:
                    m.clear(fx + k, fy + j)                   # 羽先をとがらせる
                    continue
                glow = False
                if axis:
                    c, a, glow = hexc("#f0fff6"), 220, t < 0.85
                elif edge:
                    c, a, glow = hexc("#c8ffe0"), 190, t < 0.5
                else:
                    c = (hexc("#e8fff2") * (1 - t) + hexc("#5ad898") * t)
                    a = int(165 - 75 * t)
                    glow = t < 0.12                          # 羽先だけ光る
                m.tex[fy + j, fx + k, :3] = np.clip(c, 0, 255); m.tex[fy + j, fx + k, 3] = a
                if glow:
                    m.glow[fy + j, fx + k, :3] = np.clip(c * 0.8, 0, 255); m.glow[fy + j, fx + k, 3] = 255
    if mat == "blade":
        # 光の結晶の刃：少し透ける。縁が白く光る
        for j in range(fh):
            for k in range(fw):
                edge = k == 0 or k == fw - 1
                facet = (k < fw / 2) == ((j // 4) % 2 == 0)       # 結晶の面：明暗を交互に
                c = hexc("#f4fff8") if edge else (hexc("#9af4c8") if facet else hexc("#5ad8a0"))
                m.tex[fy + j, fx + k, :3] = c[:3]; m.tex[fy + j, fx + k, 3] = 240 if edge else 200
                if edge or (j + k) % 7 == 0:
                    m.glow[fy + j, fx + k, :3] = (c[:3] * 0.85).astype(int); m.glow[fy + j, fx + k, 3] = 255
    if mat in ("crystal", "halo"):
        for j in range(fh):
            for k in range(fw):
                c = GLOW[2] if (abs(k - (fw - 1) / 2) < 1 or (j + k) % 4 == 0) else GLOW[1]
                if mat == "halo":
                    c = hexc("#f0ffd0") if (j + k) % 3 else hexc("#c8ffb0")
                m.put(fx + k, fy + j, c, glow=True)


m.paint(PAL, details, glow_mats={"crystal", "halo"},
        outline_skip={"crystal", "halo", "dress", "green", "hair", "skin", "leaf", "flower"})
m.save()

# 第5形態：剣と翼を属性の色にしたテクスチャ（緑のままが木）
from geo_tools import ASSETS
from PIL import Image
TINT = {"fire": "#ff6a30", "ice": "#7ad8ff", "wind": "#c8fff0", "thunder": "#ffe040",
        "earth": "#e0a050", "dark": "#b060ff", "light": "#fff4b0", "wood": None}
base = np.array(Image.open(f"{ASSETS}/textures/entity/yggdrasil_spirit.png").convert("RGBA")).astype(float)
glw = np.array(Image.open(f"{ASSETS}/textures/entity/yggdrasil_spirit_glowmask.png").convert("RGBA")).astype(float)
mask = np.zeros(base.shape[:2], bool)
for fx, fy, fw, fh in TINT_RECTS:
    mask[fy:fy + fh, fx:fx + fw] = True


def recolor(arr, c):
    rgb = arr[..., :3] / 255
    lum = rgb.max(axis=2, keepdims=True)
    white = np.clip((rgb.min(axis=2, keepdims=True) - 0.55) / 0.45, 0, 1) ** 1.5   # 白い所は白いまま
    col = lum * c
    out = col * (1 - white) + rgb * white
    res = arr.copy()
    res[..., :3] = np.where(mask[..., None], np.clip(out * 255, 0, 255), arr[..., :3])
    return res


for name, h in TINT.items():
    if h is None:
        Image.fromarray(base.astype(np.uint8)).save(f"{ASSETS}/textures/entity/yggdrasil_spirit_{name}.png")
        Image.fromarray(glw.astype(np.uint8)).save(f"{ASSETS}/textures/entity/yggdrasil_spirit_{name}_glowmask.png")
        continue
    c = hexc(h)[:3].astype(float) / 255
    Image.fromarray(recolor(base, c).astype(np.uint8)).save(f"{ASSETS}/textures/entity/yggdrasil_spirit_{name}.png")
    Image.fromarray(recolor(glw, c).astype(np.uint8)).save(f"{ASSETS}/textures/entity/yggdrasil_spirit_{name}_glowmask.png")
print("element textures", len(TINT), "rects", len(TINT_RECTS))

