"""光の白狐：モデル＋テクスチャ＋発光マスク。 python3 tools/white_fox_gen.py
四つ足。正面は -Z。全体を K 倍の大きさで作る（基本の寸法は普通の狐を大きくしたくらい）。
"""
import os, sys
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from geo_tools import Model, mirror, hexc

K = 1.5
m = Model("white_fox", 256, 256, seed=51)


def c(o, s, mat, tag):
    """寸法を K 倍する（大きさは整数に丸める）"""
    return ([round(v * K * 2) / 2 for v in o], [max(1, round(v * K)) for v in s], mat, tag)


def p(*xyz):
    return [round(v * K * 2) / 2 for v in xyz]


# ---------------------------------------------------------------- model
m.bone("root", [0, 0, 0])
m.bone("body", p(0, 18, 0), parent="root", cubes=[
    c([-5, 13, -12], [10, 9, 12], "fur", "chest"),
    c([-4.5, 13.5, 0], [9, 8, 10], "fur", "belly"),
    c([-5, 13, 10], [10, 9, 7], "fur", "hips"),
    c([-4, 12.5, -10], [8, 1, 20], "fur_light", "underside"),
])

# 首：ふわふわの襟、しめ縄と金の鈴
m.bone("neck", p(0, 21, -12), parent="body", rotation=[-35, 0, 0], cubes=[
    c([-3.5, 18, -19], [7, 7, 8], "fur", "neck"),
    c([-5, 15.5, -15], [10, 9, 5], "fur_light", "ruff"),
    c([-5.5, 19, -16.5], [11, 2, 6], "rope", "shimenawa"),
    c([-1, 15, -17.5], [2, 2, 2], "gold", "bell"),
])
m.bone("head", p(0, 22, -19), parent="neck", rotation=[30, 0, 0], cubes=[
    c([-4, 19, -26], [8, 7, 7], "fur", "skull"),
    c([-2, 19, -32], [4, 3.5, 6], "fur", "snout"),
    c([-1, 21, -32.5], [2, 1, 1], "nose", "nose"),
    c([-5, 18.5, -23], [1, 4, 3], "fur_light", "cheek"),
    c([4, 18.5, -23], [1, 4, 3], "fur_light", "cheek"),
])
EAR = [c([-4.5, 26, -23], [4, 2, 2], "fur", "ear"), c([-4, 28, -23], [3, 2, 2], "fur", "ear"),
       c([-3.5, 30, -23], [2, 1, 2], "fur", "ear_tip"), c([-4, 26.5, -23.5], [3, 2, 1], "ear_inner", "ear_inner")]
m.bone("right_ear", p(-2.5, 26, -22), parent="head", rotation=[-10, 0, 20], cubes=EAR)
m.bone("left_ear", p(2.5, 26, -22), parent="head", rotation=[-10, 0, -20], cubes=mirror(EAR))

# 脚：細く長い。前脚と後ろ脚
FRONT = [c([-5, 0, -10], [3.5, 13, 3.5], "fur", "front_leg"), c([-5.5, 0, -11], [4.5, 2, 4.5], "fur_light", "paw")]
HIND = [c([-5, 6, 10], [4, 8, 6], "fur", "thigh")]
HIND_LOW = [c([-5, 0, 13], [3.5, 7, 3.5], "fur", "shin"), c([-5.5, 0, 12], [4.5, 2, 4.5], "fur_light", "paw")]
m.bone("front_right", p(-3, 13, -8), parent="body", cubes=FRONT)
m.bone("front_left", p(3, 13, -8), parent="body", cubes=mirror(FRONT))
m.bone("hind_right", p(-3, 14, 13), parent="body", cubes=HIND)
m.bone("hind_right_low", p(-3, 7, 14), parent="hind_right", cubes=HIND_LOW)
m.bone("hind_left", p(3, 14, 13), parent="body", cubes=mirror(HIND))
m.bone("hind_left_low", p(3, 7, 14), parent="hind_left", cubes=mirror(HIND_LOW))

# 9本の尻尾：扇状に広げて持ち上げる。根元・中ほど・光る先端の3段
m.bone("tails", p(0, 20, 16), parent="body")
for i, ry in enumerate([-76, -57, -38, -19, 0, 19, 38, 57, 76]):
    rx = 48 - abs(ry) * 0.35
    m.bone(f"tail{i}", p(0, 20, 16), parent="tails", rotation=[rx, ry, 0], cubes=[
        c([-2, 18, 16], [4, 4, 7], "fur", f"tail_base"),
    ])
    m.bone(f"tail{i}_mid", p(0, 20, 22), parent=f"tail{i}", rotation=[18, 0, 0], cubes=[
        c([-3, 17, 22], [6, 6, 8], "fur", "tail_mid"),
    ])
    m.bone(f"tail{i}_tip", p(0, 20, 29), parent=f"tail{i}_mid", rotation=[20, 0, 0], cubes=[
        c([-2.5, 17.5, 29], [5, 5, 4], "fur_tip", "tail_tip"),
        c([-1.5, 18.5, 32.5], [3, 3, 2], "light", "tail_light"),
    ])

# ---------------------------------------------------------------- texture
PAL = {
    "fur": ("#fbfaf6", "#ffffff", "#d0ccc2"),
    "fur_light": ("#fbfaf6", "#ffffff", "#d8d3c8"),
    "fur_tip": ("#fff6dc", "#ffffff", "#e8d9a8"),
    "ear_inner": ("#f2b8c0", "#ffd8de", "#c8808c"),
    "nose": ("#2a2226", "#3e3438", "#120e10"),
    "rope": ("#f4f0e4", "#ffffff", "#b8b0a0"),
    "gold": ("#d8a830", "#ffe27a", "#7a5a10"),
    "light": ("#ffe9a0", "#ffffff", "#ffd060"),
}
VERMILION = hexc("#e0402a")
EYE = [hexc("#ffc23a"), hexc("#fff3b0")]
GLOW = [hexc("#ffe08a"), hexc("#fff6d0"), hexc("#ffffff")]


def details(m, mat, tag, face, rect):
    fx, fy, fw, fh = rect
    r = m.rng
    # 毛並み：うっすら灰色の筋
    if mat in ("fur", "fur_light") and face not in ("down",):
        for _ in range(max(1, fw * fh // 10)):
            m.put(fx + r.randrange(fw), fy + r.randrange(fh), hexc("#e2ded4"))
    # 顔：金色の目、朱の隈取り（目尻から上へ、額に宝珠の印）
    if tag == "skull" and face == "north":
        y_eye = fh // 2
        for k in (1, 2, fw - 3, fw - 2):
            m.put(fx + k, fy + y_eye, EYE[0], glow=True)
        m.put(fx + 2, fy + y_eye, EYE[1], glow=True)
        m.put(fx + fw - 3, fy + y_eye, EYE[1], glow=True)
        for k, j in ((0, y_eye - 1), (0, y_eye - 2), (1, y_eye - 3), (fw - 1, y_eye - 1), (fw - 1, y_eye - 2), (fw - 2, y_eye - 3)):
            m.put(fx + k, fy + j, VERMILION)
        mid = fw // 2
        m.put(fx + mid - 1, fy + 1, VERMILION, glow=True)
        m.put(fx + mid, fy + 1, VERMILION, glow=True)
        m.put(fx + mid - 1, fy + 2, hexc("#ff7a50"), glow=True)
        m.put(fx + mid, fy + 2, hexc("#ff7a50"), glow=True)
    if tag == "skull" and face in ("west", "east"):
        for k in range(fw):                                    # 目尻から後ろへ伸びる朱の線
            if k % 2 == 0:
                m.put(fx + k, fy + fh // 2 - 1, VERMILION)
    if tag == "snout" and face == "north":
        m.put(fx + fw // 2 - 1, fy + fh - 1, hexc("#c8c0b4"))
    # 耳の先は朱色
    if tag == "ear_tip" and face not in ("down",):
        for j in range(fh):
            for k in range(fw):
                m.put(fx + k, fy + j, VERMILION)
    # しめ縄：赤白のねじり模様
    if mat == "rope" and face not in ("up", "down"):
        for j in range(fh):
            for k in range(fw):
                m.put(fx + k, fy + j, hexc("#d8382a") if (k + j) % 4 < 2 else hexc("#f8f4e8"))
    if tag == "bell" and face == "north":
        m.put(fx + fw // 2, fy + fh - 1, hexc("#3a2a08"))
    # 尻尾の先：毛先ほど金色に光る
    if mat == "fur_tip":
        for j in range(fh):
            for k in range(fw):
                if r.random() < 0.45:
                    m.put(fx + k, fy + j, GLOW[r.randrange(2)], glow=True)
    if mat == "light":
        for j in range(fh):
            for k in range(fw):
                m.put(fx + k, fy + j, GLOW[(j + k) % 3], glow=True)
    if tag == "tail_mid" and face not in ("up", "down"):
        for k in range(fw):                                    # 中ほどから先へ金がかる
            if r.random() < 0.3:
                m.put(fx + k, fy + fh - 1 - r.randrange(2), hexc("#f6e6b8"))


m.paint(PAL, details, glow_mats={"light"}, outline_skip={"fur", "fur_light", "fur_tip", "light", "rope"},
        soft={"mats": {"fur", "fur_light"}, "y": (0, 45), "dark": 0.9})
m.save()
