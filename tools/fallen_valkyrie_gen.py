"""堕ちた戦乙女（高精細）：モデル＋テクスチャ＋発光マスク。 python3 tools/fallen_valkyrie_gen.py
黒鋼に金の縁取りの全身鎧、翼のついた兜、羽根の刃でできた大きな翼、ボロボロのマント、光る長槍。
今までのボスより細かく作る：キューブごとに傾け、テクスチャは 1px あたり 2×2 ピクセル（density=2）。
正面は -Z。身長 約3.6ブロック（翼と槍を除く）。
"""
import math, os, sys
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from geo_tools import Model, mirror, hexc

m = Model("fallen_valkyrie", 512, 512, seed=21, density=2)


def B(o, s, mat, tag, rot=None, pivot=None):
    """キューブ。rot を渡すと pivot（省略時はキューブの中心）でそのキューブだけ傾ける"""
    x = {}
    if rot: x["rot"] = list(rot)
    if pivot: x["pivot"] = list(pivot)
    return (list(o), list(s), mat, tag, x) if x else (list(o), list(s), mat, tag)


# ================================================================ model
m.bone("root", [0, 0, 0])
m.bone("hips", [0, 26, 0], parent="root", cubes=[
    B([-5, 24, -3], [10, 4, 6], "steel", "pelvis"),
    B([-5.5, 25.5, -3.5], [11, 1.5, 7], "gold", "belt"),
    B([-1.5, 25, -4], [3, 2.5, 1], "gold", "buckle"),
    # 前垂れ（布）と腰の板（フォールド）を重ねる
    B([-3, 13, -3.6], [6, 12, 0.6], "cloth", "tabard_front", rot=[-6, 0, 0], pivot=[0, 25, -3.4]),
    B([-3, 13, 3.0], [6, 12, 0.6], "cloth", "tabard_back", rot=[8, 0, 0], pivot=[0, 25, 3.2]),
    B([-5.6, 21.5, -3.4], [3.6, 3.5, 6.8], "steel", "fauld", rot=[0, 0, 10], pivot=[-4, 25, 0]),
    B([2, 21.5, -3.4], [3.6, 3.5, 6.8], "steel", "fauld", rot=[0, 0, -10], pivot=[4, 25, 0]),
    B([-5.9, 18.5, -3.2], [3.4, 3.5, 6.4], "steel", "fauld2", rot=[0, 0, 14], pivot=[-4, 22, 0]),
    B([2.5, 18.5, -3.2], [3.4, 3.5, 6.4], "steel", "fauld2", rot=[0, 0, -14], pivot=[4, 22, 0]),
])

LEG_UP = [
    B([-4.3, 14, -1.8], [3.6, 11, 3.6], "body", "thigh"),
    B([-4.7, 15.5, -2.3], [4.4, 8, 4.6], "steel", "cuisse"),
    B([-4.8, 12, -2.8], [4.6, 3.5, 2], "gold", "knee", rot=[-10, 0, 0]),
    B([-4.2, 13.2, -3.4], [3.4, 1.2, 1], "steel", "knee_spike", rot=[-35, 0, 0]),
]
LEG_LOW = [
    B([-4.2, 2, -1.8], [3.4, 11, 3.6], "steel", "greave"),
    B([-3.1, 3, -2.4], [1.2, 9, 1], "gold", "greave_front"),
    B([-4.5, 0, -3.5], [4.0, 2.5, 6], "steel", "sabaton"),
    B([-3.5, 0, -5.4], [2, 1.6, 2.2], "steel", "toe", rot=[0, 0, 0]),
    B([-4.4, 8, 1.4], [3.8, 3, 0.8], "steel", "calf_plate", rot=[12, 0, 0]),
]
m.bone("right_leg", [-2.5, 25, 0], parent="hips", cubes=LEG_UP)
m.bone("right_shin", [-2.5, 13, 0], parent="right_leg", cubes=LEG_LOW)
m.bone("left_leg", [2.5, 25, 0], parent="hips", cubes=mirror(LEG_UP))
m.bone("left_shin", [2.5, 13, 0], parent="left_leg", cubes=mirror(LEG_LOW))

m.bone("torso", [0, 29, 0], parent="hips", cubes=[
    B([-4, 28, -2.5], [8, 5, 5], "body", "waist"),
    B([-5.5, 33, -3.2], [11, 10, 6.4], "steel", "chest"),
    B([-5.8, 37, -3.6], [11.6, 6, 1], "steel", "breastplate", rot=[-8, 0, 0], pivot=[0, 43, -3.4]),
    B([-0.8, 33.5, -4.0], [1.6, 9, 1], "gold", "ridge"),
    B([-4.8, 30.5, -3.1], [9.6, 2.6, 6.2], "steel", "abs1"),
    B([-4.4, 28.5, -2.9], [8.8, 2.4, 5.8], "steel", "abs2"),
    B([-3.5, 42, -2.6], [7, 2.5, 5.2], "gold", "gorget"),
    B([-5.2, 33.5, 2.6], [10.4, 9, 1], "steel", "backplate", rot=[6, 0, 0]),
    B([-1.5, 38, -4.3], [3, 3, 1], "glow", "heart_rune"),          # 胸の光るルーン
])

# 肩当て：3枚の板を少しずつ傾けて重ねる
PAULDRON = [
    B([-10, 41.5, -3.5], [6.5, 2.2, 7], "steel", "pauldron1", rot=[0, 0, 18], pivot=[-5, 43, 0]),
    B([-10.8, 39.5, -3.3], [6, 2.2, 6.6], "gold", "pauldron2", rot=[0, 0, 28], pivot=[-6, 41, 0]),
    B([-11.3, 37.6, -3.1], [5.4, 2.0, 6.2], "steel", "pauldron3", rot=[0, 0, 38], pivot=[-7, 39, 0]),
    B([-9.5, 43.2, -1], [1.4, 3, 2], "gold", "pauldron_spike", rot=[0, 0, 30], pivot=[-8.8, 43.2, 0]),
]
m.bone("right_pauldron", [-6, 42, 0], parent="torso", cubes=PAULDRON)
m.bone("left_pauldron", [6, 42, 0], parent="torso", cubes=mirror(PAULDRON))

UPPER = [B([-8, 32, -1.6], [3.2, 9.5, 3.2], "body", "upper_arm"),
         B([-8.3, 33, -1.9], [3.8, 4, 3.8], "steel", "rerebrace")]
FORE = [
    B([-8, 22.5, -1.6], [3.2, 9.5, 3.2], "body", "forearm"),
    B([-8.5, 23, -2.1], [4.2, 7, 4.2], "steel", "vambrace"),
    B([-8.6, 29.5, -2.2], [4.4, 1.2, 4.4], "gold", "vambrace_rim"),
    B([-8.3, 30.4, -2.6], [3.6, 3, 1.4], "steel", "elbow", rot=[20, 0, 0]),
    B([-8.2, 19.5, -2.0], [3.6, 3.2, 4], "steel", "gauntlet"),
    B([-8.0, 18.2, -1.8], [3.2, 1.4, 3.6], "body", "fingers"),
]
m.bone("right_arm", [-6.4, 41, 0], parent="torso", cubes=UPPER)
m.bone("right_forearm", [-6.4, 32, 0], parent="right_arm", cubes=FORE)
m.bone("left_arm", [6.4, 41, 0], parent="torso", cubes=mirror(UPPER))
m.bone("left_forearm", [6.4, 32, 0], parent="left_arm", cubes=mirror(FORE))

# 長槍（右手に持つ。いつもは縦に立てて持つ）
SPEAR = [
    B([-7.1, -6, -0.7], [1.4, 64, 1.4], "shaft", "shaft"),
    B([-7.2, 18, -0.8], [1.6, 4, 1.6], "gold", "grip"),
    B([-7.3, 52, -0.9], [1.8, 2, 1.8], "gold", "socket"),
    B([-10.4, 54, -0.6], [8, 1.4, 1.2], "gold", "guard"),
    B([-10.6, 54.6, -0.5], [2.2, 3.2, 1], "gold", "guard_wing", rot=[0, 0, 30], pivot=[-9.5, 55, 0]),
    B([-5.8, 54.6, -0.5], [2.2, 3.2, 1], "gold", "guard_wing", rot=[0, 0, -30], pivot=[-4.7, 55, 0]),
    B([-7.6, 55.4, -0.4], [2.4, 11, 0.8], "blade", "spearhead"),
    B([-7.0, 66.4, -0.4], [1.2, 2.4, 0.8], "blade", "spear_tip"),
    B([-7.0, -7.5, -0.6], [1.2, 1.6, 1.2], "gold", "butt"),
]
m.bone("spear", [-6.4, 20.5, 0], parent="right_forearm", cubes=SPEAR)

# 兜：細いスリットのバイザーが光り、両側に翼の飾り、頭頂にとさか
m.bone("head", [0, 44, 0], parent="torso", cubes=[
    B([-3.6, 44, -3.6], [7.2, 7.6, 7.2], "steel", "helm"),
    B([-3.9, 44.5, -4.1], [7.8, 3.2, 1], "steel", "faceplate", rot=[6, 0, 0]),
    B([-3.0, 47.6, -4.2], [6, 0.8, 1], "glow", "visor"),
    B([-0.6, 44.2, -4.4], [1.2, 3.2, 1], "gold", "nose_guard"),
    B([-0.7, 51, -4.2], [1.4, 2.6, 8.8], "gold", "crest", rot=[-5, 0, 0]),
    B([-3.8, 50.8, -3.8], [7.6, 1, 7.6], "gold", "helm_rim"),
])
HWING = [B([-6.5 - i * 0.4, 47 + i * 0.8, -1 + i * 0.6], [3 + i * 0.5, 1.2, 1], "gold" if i % 2 else "steel", "helm_feather",
           rot=[0, 0, -40 - i * 14], pivot=[-3.6, 47.5, 0]) for i in range(4)]
m.bone("helm_wing_right", [-3.6, 47.5, 0], parent="head", cubes=HWING)
m.bone("helm_wing_left", [3.6, 47.5, 0], parent="head", cubes=mirror(HWING))

# 羽根の刃でできた大きな翼（扇形に並べる）
FEATHERS = []
# 上の羽根ほど長く、下へいくほど短く垂れる。根元は「翼の骨」で覆う
for i in range(8):
    L = 27 - i * 2.0
    ang = -28 + i * 13                    # 上向き → 下向き
    FEATHERS.append(B([-4 - L, 40.2, 4.4 + i * 0.2], [L, 2.2, 0.6], "feather", "feather", rot=[0, 0, ang], pivot=[-4, 41.3, 4.6]))
for i in range(7):                        # 内側の短い羽根（2段目）で隙間を埋める
    L = 15 - i * 1.0
    ang = -22 + i * 13 + 6
    FEATHERS.append(B([-4 - L, 40.4, 3.9 + i * 0.15], [L, 2.0, 0.5], "feather", "covert", rot=[0, 0, ang], pivot=[-4, 41.3, 4.6]))
FEATHERS.append(B([-14, 40.0, 4.0], [11, 3.2, 1.4], "steel", "wing_bone", rot=[0, 0, -20], pivot=[-4, 41.3, 4.6]))
FEATHERS.append(B([-9, 38.6, 4.1], [6, 3, 1.3], "gold", "wing_bone2", rot=[0, 0, 15], pivot=[-4, 41.3, 4.6]))
m.bone("wing_right", [-2.5, 41, 3.8], parent="torso", rotation=[0, 35, 0], cubes=FEATHERS)
LEFT = [c[:3] + (c[3] + "_l",) + c[4:] if c[3] in ("feather", "covert") else c for c in mirror(FEATHERS)]
m.bone("wing_left", [2.5, 41, 3.8], parent="torso", rotation=[0, -35, 0], cubes=LEFT)

# ボロボロのマント：3本の帯を上下2段に分ける（アニメーションでなびく）
for i, x in enumerate((-5, -1.6, 1.8)):
    w = 3.4
    m.bone(f"cape{i}", [x + w / 2, 43, 3.9], parent="torso", rotation=[6, 0, 0], cubes=[
        B([x, 30, 3.6], [w, 13, 0.6], "cape", "cape")])
    m.bone(f"cape{i}_low", [x + w / 2, 30, 3.9], parent=f"cape{i}", rotation=[4, 0, 0], cubes=[
        B([x, 14 + (i % 2) * 2, 3.6], [w, 16 - (i % 2) * 2, 0.6], "cape", "cape_low")])

# ================================================================ texture
PAL = {
    "steel": ("#3a3f4a", "#6a7282", "#16181d"),
    "body": ("#1e2128", "#2c3039", "#0c0d10"),
    "gold": ("#c9a24a", "#f2d68a", "#6e5420"),
    "cloth": ("#3a1d2a", "#5a2c40", "#1a0c12"),
    "cape": ("#2a1420", "#46223a", "#12080e"),
    "shaft": ("#2a2018", "#40302a", "#120c08"),
    "feather": ("#4a5160", "#8a93a6", "#1c1f26"),
    "blade": ("#bff6ff", "#ffffff", "#5ac8e0"),
    "glow": ("#9ff4ff", "#ffffff", "#4ab8d8"),
}
GLOW = [hexc("#4ab8d8"), hexc("#9ff4ff"), hexc("#f0ffff")]


def details(m, mat, tag, face, rect):
    fx, fy, fw, fh = rect
    r = m.rng
    if mat == "steel" and face not in ("down",):
        # 金属の光沢：斜めのハイライト線と、細かい傷
        for k in range(fw):
            j = int((k * 0.6) % max(1, fh))
            if fh > 3 and k % 7 < 2:
                m.put(fx + k, fy + j, hexc("#7c8596"))
        for _ in range(fw * fh // 60):
            x, y = r.randrange(fw), r.randrange(fh)
            for d in range(r.randrange(2, 4)):
                if x + d < fw:
                    m.put(fx + x + d, fy + y, hexc("#22252c"))
        # 大きな板には金の縁取り（上の段）と、彫られたルーン
        if tag in ("chest", "breastplate", "backplate", "cuisse", "greave", "helm") and face in ("north", "south", "east", "west"):
            for k in range(fw):
                m.put(fx + k, fy + 1, hexc("#c9a24a"))
            if tag in ("breastplate", "cuisse", "greave") and fw >= 6 and face == "north":
                cx = fw // 2
                for j in range(3, fh - 2, 2):
                    m.put(fx + cx, fy + j, GLOW[0], glow=True)
                    if j % 4 == 3 and 1 <= cx - 2 and cx + 2 < fw:
                        m.put(fx + cx - 2, fy + j, GLOW[0], glow=True)
                        m.put(fx + cx + 2, fy + j, GLOW[0], glow=True)
    if mat == "gold" and face != "down":
        for k in range(fw):
            if (k + fy) % 5 == 0:
                for j in range(fh):
                    m.put(fx + k, fy + j, hexc("#a8822e"))
        if fh > 2:
            for k in range(0, fw, 3):
                m.put(fx + k, fy + fh // 2, hexc("#fff0b8"))
    if mat in ("cloth", "cape") and face in ("north", "south", "east", "west"):
        # 布：縦のひだと、ボロボロに破れた裾（下のほうをギザギザに抜く）
        for k in range(0, fw, 3):
            for j in range(fh):
                m.put(fx + k, fy + j, hexc("#1c0c14" if mat == "cape" else "#2a1420"))
        if tag in ("cape_low", "tabard_front", "tabard_back"):
            for k in range(fw):
                cut = r.choice((0, 1, 2, 3, 5, 7)) if face in ("north", "south") else r.choice((0, 1, 2))
                for j in range(fh - cut, fh):
                    m.clear(fx + k, fy + j)
            # 裾のほつれた金糸
            for k in range(0, fw, 4):
                j = fh - 9
                if 0 <= j < fh:
                    m.put(fx + k, fy + j, hexc("#c9a24a"))
        if tag == "tabard_front" and face == "north":
            cx = fw // 2                                  # 前垂れの紋章（翼）
            for d in range(-4, 5):
                y = 4 + abs(d) // 2
                if 0 <= cx + d < fw and y < fh:
                    m.put(fx + cx + d, fy + y, hexc("#c9a24a"))
            for j in range(3, 9):
                m.put(fx + cx, fy + j, hexc("#c9a24a"))
    if mat == "feather":
        # 羽根の刃：縁が明るく、根元から先へ細い筋
        for k in range(fw):
            m.put(fx + k, fy, hexc("#b8c2d4"))
            if fh > 2:
                m.put(fx + k, fy + fh - 1, hexc("#c9a24a"))
            if k % 3 == 0 and fh > 3:
                m.put(fx + k, fy + fh // 2, hexc("#2a2e38"))
        if face in ("north", "south", "up") and tag in ("feather", "feather_l"):   # 刃先（外側の端）だけ光る
            left = tag.endswith("_l")
            outer_at_start = (face != "south") != left
            for j in range(fh):
                for k in range(min(3, fw)):
                    kk = k if outer_at_start else fw - 1 - k
                    m.put(fx + kk, fy + j, GLOW[1] if k < 2 else GLOW[0], glow=True)
    if mat in ("glow", "blade"):
        for j in range(fh):
            for k in range(fw):
                c = GLOW[2] if (abs(k - (fw - 1) / 2) < 1 or (j + k) % 5 == 0) else GLOW[1]
                m.put(fx + k, fy + j, c, glow=True)
    if mat == "shaft" and face != "down":
        for j in range(0, fh, 6):                         # 柄に巻いた金の帯
            for k in range(fw):
                m.put(fx + k, fy + j, hexc("#c9a24a"))
    if mat == "body" and face != "down":
        for _ in range(fw * fh // 8):                     # 鎖かたびらの細かい目
            x, y = r.randrange(fw), r.randrange(fh)
            m.put(fx + x, fy + y, hexc(r.choice(("#3a3e48", "#14161a"))))


m.paint(PAL, details, glow_mats={"glow", "blade"}, outline_skip={"glow", "blade", "cloth", "cape", "body"})
m.save()
