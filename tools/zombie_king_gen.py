"""ゾンビの王：モデル＋テクスチャ＋発光マスク。 python3 tools/zombie_king_gen.py"""
import os, sys
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from geo_tools import Model, mirror, hexc

m = Model("zombie_king", 256, 256, seed=21)

# ---------------------------------------------------------------- model
m.bone("root", [0, 0, 0])

LEG = [
    ([-9, 0, -4.5], [8, 4, 9], "iron", "boot"),
    ([-8.5, 4, -3.5], [7, 14, 7], "skin", "shin"),
]
m.bone("right_leg", [-5, 18, 0], parent="root", cubes=LEG)
m.bone("left_leg", [5, 18, 0], parent="root", cubes=mirror(LEG))

# 腰：ボロボロのローブのすそと金のベルト
m.bone("waist", [0, 18, 0], parent="root", cubes=[
    ([-11, 7, -6], [22, 12, 12], "robe", "skirt"),
    ([-11.5, 18, -6.5], [23, 3, 13], "gold", "belt"),
    ([-2, 18, -7], [4, 3, 1], "gem", "buckle"),
])

# 胴体：前かがみ（猫背）。前を開けたローブから肋骨がのぞく
m.bone("torso", [0, 21, 0], parent="waist", rotation=[15, 0, 0], cubes=[
    ([-10.5, 21, -5.5], [21, 9, 11], "skin", "belly"),
    ([-11.5, 27, -6.2], [23, 4, 12], "robe", "chest_low"),
    ([-12, 30, -7], [24, 12, 14], "robe", "chest"),
    ([-6, 30, -7.5], [12, 11, 1], "skin", "ribs"),
    ([-14, 41, -8], [28, 5, 16], "fur", "mantle"),
    ([-15, 42, -6], [3, 3, 12], "fur", "mantle_side"),
    ([12, 42, -6], [3, 3, 12], "fur", "mantle_side"),
])

m.bone("head", [0, 45, -2], parent="torso", cubes=[
    ([-6, 45, -9], [12, 11, 12], "skin", "face"),
    ([-5, 42, -9.5], [10, 3, 9], "bone", "jaw"),
])
CROWN_SPIKE = [([-7, 59, -8], [2, 4, 2], "gold", "crown_spike"),
               ([-1, 59, -8.5], [2, 5, 2], "gold", "crown_spike"),
               ([-7, 59, 1], [2, 3, 2], "gold", "crown_spike")]
m.bone("crown", [0, 56, -3], parent="head", cubes=[
    ([-7, 56, -10], [14, 3, 13], "gold", "crown"),
    ([-1, 57, -10.5], [2, 2, 1], "gem", "crown_gem"),
    ([-5, 57, -10.5], [1, 1, 1], "gem", "crown_gem"),
    ([4, 57, -10.5], [1, 1, 1], "gem", "crown_gem"),
] + CROWN_SPIKE + mirror(CROWN_SPIKE[:1] + CROWN_SPIKE[2:]))

# マント：背中から地面近くまで、すそが破れている
m.bone("cape", [0, 44, 7], parent="torso", cubes=[
    ([-12, 12, 7], [24, 32, 1], "cape", "cape"),
])

ARM = [
    ([-19.5, 26, -3.5], [7, 15, 7], "skin", "upper_arm"),
    ([-21, 38, -5.5], [9, 6, 11], "gold", "pauldron"),
    ([-20, 44, -2], [2, 3, 2], "gold", "pauldron_spike"),
]
HAND = [
    ([-19.5, 13, -3.5], [7, 15, 7], "skin", "forearm"),
    ([-20, 18, -4.5], [8, 4, 9], "bandage", "wrap"),
    ([-20, 7, -4], [8, 7, 8], "skin", "fist"),
    ([-19, 5, -4], [2, 2, 2], "bone", "claw"),
    ([-16, 5, -4], [2, 2, 2], "bone", "claw"),
]
m.bone("right_arm", [-15.5, 40, 0], parent="torso", cubes=ARM)
m.bone("right_hand", [-16, 28, 0], parent="right_arm", cubes=HAND)
m.bone("left_arm", [15.5, 40, 0], parent="torso", cubes=mirror(ARM))
m.bone("left_hand", [16, 28, 0], parent="left_arm", cubes=mirror(HAND))

# 王笏：重い金の頭がついた大きな杖。ふだんは先を前の地面につけている
m.bone("scepter", [-16, 10, 0], parent="right_hand", rotation=[-70, 0, 0], cubes=[
    ([-17, -12, -1], [2, 24, 2], "iron", "shaft"),
    ([-17.5, 12, -1.5], [3, 3, 3], "gold", "pommel"),
    ([-19.5, -19, -3.5], [7, 7, 7], "gold", "head_gold"),
    ([-17, -21, -1], [2, 2, 2], "gold", "head_tip"),
    ([-20.5, -17, -1], [1, 3, 2], "gold", "head_spike"),
    ([-12.5, -17, -1], [1, 3, 2], "gold", "head_spike"),
    ([-17.5, -17, -4], [3, 3, 1], "gem", "scepter_gem"),
])

# ---------------------------------------------------------------- texture
PAL = {
    "skin": ("#5d7a52", "#7f9c6e", "#2f3f2a"),
    "robe": ("#4a1d4f", "#6e2f74", "#220b25"),
    "cape": ("#5e1518", "#7d2125", "#2a0809"),
    "gold": ("#c9a227", "#f1d36a", "#6e5410"),
    "fur": ("#4b3b2e", "#6b5643", "#231a12"),
    "iron": ("#3a3a40", "#5a5a63", "#1b1b1f"),
    "bone": ("#d9d2b5", "#f2ecd4", "#8e876c"),
    "bandage": ("#a59a7c", "#c8bd9c", "#5f5644"),
    "gem": ("#ff3b3b", "#ffb0a0", "#a0101a"),
}
ROT = [hexc("#3d5236"), hexc("#4a3a2a"), hexc("#6b2a22")]
GEM_GLOW = [hexc("#ff3b3b"), hexc("#ff8a6a")]


def details(m, mat, tag, face, rect):
    fx, fy, fw, fh = rect
    r = m.rng
    if mat == "skin" and face not in ("up", "down"):
        for _ in range(max(1, fw * fh // 18)):          # 腐った斑点と傷
            x, y = r.randrange(fw), r.randrange(fh)
            m.put(fx + x, fy + y, r.choice(ROT))
            if r.random() < 0.3 and x + 1 < fw:
                m.put(fx + x + 1, fy + y, hexc("#d9d2b5"))  # のぞく骨
    if tag == "ribs" and face == "north":
        for j in range(1, fh - 1, 2):                    # 肋骨
            for k in range(1, fw - 1):
                if k != fw // 2 and k != fw // 2 - 1:
                    m.put(fx + k, fy + j, hexc("#d9d2b5"))
            m.put(fx + fw // 2 - 1, fy + j, hexc("#e8e2c8"))
        for j in range(0, fh, 2):
            m.put(fx + fw // 2, fy + j, hexc("#cfc8aa"))
    if tag == "face" and face == "north":
        # 落ちくぼんだ目に赤い光、鼻の穴、しわ
        for k in (2, 3, 8, 9):
            m.put(fx + k, fy + 4, hexc("#1a1210"))
            m.put(fx + k, fy + 5, hexc("#1a1210"))
        for k in (2, 9):
            m.put(fx + k + (1 if k == 2 else -1), fy + 5, GEM_GLOW[0], glow=True)
            m.put(fx + k + (1 if k == 2 else -1), fy + 4, GEM_GLOW[1], glow=True)
        m.put(fx + 5, fy + 7, hexc("#1a1210")); m.put(fx + 6, fy + 7, hexc("#1a1210"))
        for k in range(2, 10):
            if r.random() < 0.4:
                m.put(fx + k, fy + 2, ROT[0])
    if tag == "jaw" and face == "north":
        for k in range(1, fw - 1):                       # 歯
            m.put(fx + k, fy, hexc("#f2ecd4") if k % 2 else hexc("#3a2a20"))
    if mat in ("robe", "cape") and face not in ("up", "down"):
        for k in range(fw):                              # 金の縁取り
            m.put(fx + k, fy, hexc("#c9a227") if k % 3 else hexc("#f1d36a"))
        if tag in ("skirt", "cape"):
            for k in range(fw):                          # 破れたすそ
                rag = r.choice([0, 0, 1, 2, 3, 4])
                for j in range(fh - rag, fh):
                    m.clear(fx + k, fy + j)
            for _ in range(fw * fh // 25):              # 虫食いの穴
                m.clear(fx + r.randrange(fw), fy + 2 + r.randrange(max(1, fh - 6)))
    if mat == "gold" and face not in ("down",):
        for _ in range(fw * fh // 10):                   # くすんだ金
            m.put(fx + r.randrange(fw), fy + r.randrange(fh), hexc("#8a6d1c"))
    if tag == "crown" and face in ("north", "south", "east", "west"):
        for k in range(1, fw - 1, 3):                    # 王冠の小さな宝石
            m.put(fx + k, fy + 1, GEM_GLOW[0], glow=True)
    if mat == "gem":
        for j in range(fh):
            for k in range(fw):
                m.put(fx + k, fy + j, GEM_GLOW[(j + k) % 2], glow=True)
    if mat == "fur" and face not in ("down",):
        for k in range(fw):                              # 毛並み
            if k % 2 == 0:
                for j in range(fh):
                    if r.random() < 0.5:
                        m.put(fx + k, fy + j, hexc("#3a2d22"))
    if mat == "bandage" and face not in ("up", "down"):
        for j in range(0, fh, 2):
            for k in range(fw):
                m.put(fx + k, fy + j, hexc("#8a7f62"))


m.paint(PAL, details, glow_mats={"gem"}, outline_skip={"gem", "cape", "robe", "fur", "bandage"},
        soft={"mats": {"skin", "robe", "cape", "fur"}, "y": (0, 58), "dark": 0.62})
m.save()
