"""虚空の監視者（エンダーマンのボス、未来風）：モデル＋テクスチャ＋発光マスク。 python3 tools/ender_sentinel_gen.py
エンダーマンのように細長い体。黒いボディに紫に光る回路、目の代わりに光るバイザー、頭の上に浮かぶ光の輪、
背中の光の翼、両肩の横に浮かぶ小さなドローン2機。正面は -Z。全体を K 倍の大きさで作る。
"""
import math, os, sys
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from geo_tools import Model, mirror, hexc

K = 1.4
m = Model("ender_sentinel", 256, 256, seed=33)


def c(o, s, mat, tag):
    return ([round(v * K * 2) / 2 for v in o], [max(1, round(v * K)) for v in s], mat, tag)


def p(*xyz):
    return [round(v * K * 2) / 2 for v in xyz]


def cm(cubes):
    """c() で作ったパーツを左右反転"""
    return mirror(cubes)


# ---------------------------------------------------------------- model
m.bone("root", [0, 0, 0])

LEG = [
    c([-3.5, 2, -1.5], [3, 28, 3], "body", "leg"),
    c([-4, 13, -2.2], [4, 5, 1], "armor", "knee"),
    c([-4, 20, -2], [4, 8, 4], "armor", "thigh_plate"),
    c([-4, 0, -3], [4, 2, 5], "armor", "foot"),
]
m.bone("right_leg", p(-2, 30, 0), parent="root", cubes=LEG)
m.bone("left_leg", p(2, 30, 0), parent="root", cubes=cm(LEG))

m.bone("hips", p(0, 30, 0), parent="root", cubes=[c([-4.5, 28, -2], [9, 4, 4], "armor", "hips")])

m.bone("torso", p(0, 32, 0), parent="hips", cubes=[
    c([-4.5, 32, -2.5], [9, 12, 5], "body", "chest"),
    c([-5, 37, -3.2], [10, 6, 1], "armor", "chest_plate"),
    c([-1, 38.5, -3.8], [2, 2, 1], "glow", "core"),
    c([-3, 32, -3], [6, 3, 1], "armor", "abs_plate"),
])
SHOULDER = [c([-8, 41, -3], [4, 3, 6], "armor", "shoulder"),
            c([-8.5, 44, -1], [2, 3, 2], "armor", "shoulder_spike")]
m.bone("right_shoulder", p(-6, 42, 0), parent="torso", cubes=SHOULDER)
m.bone("left_shoulder", p(6, 42, 0), parent="torso", cubes=cm(SHOULDER))

m.bone("head", p(0, 44, 0), parent="torso", cubes=[
    c([-4, 44, -4], [8, 8, 8], "body", "head"),
    c([-4.5, 47.5, -4.6], [9, 1.2, 1], "glow", "visor"),
    c([-5, 46, -2], [1, 5, 4], "armor", "ear_fin"),
    c([4, 46, -2], [1, 5, 4], "armor", "ear_fin"),
    c([-4.5, 51.5, -4.5], [9, 1, 9], "armor", "head_cap"),
])

# 頭の上に浮かぶ光の輪（小さな光るブロックを円に並べる。アニメーションで回す）
HALO = []
for i in range(14):
    a = 2 * math.pi * i / 14
    HALO.append(c([math.cos(a) * 5 - 0.6, 55.5, math.sin(a) * 5 - 0.6], [1.2, 0.8, 1.2], "glow", "halo"))
m.bone("halo", p(0, 56, 0), parent="head", cubes=HALO)

# 背中の光の翼（細い板に光る縁）
FIN = [c([-2.5, 40, 2.5], [1, 20, 1], "armor", "fin_bar"),
       c([-2.5, 43, 3.5], [1, 16, 3], "body", "fin"),
       c([-2.5, 44, 6.5], [1, 14, 1], "glow", "fin_edge")]
m.bone("right_fin", p(-2, 41, 3), parent="torso", rotation=[-20, 0, 30], cubes=FIN)
m.bone("left_fin", p(2, 41, 3), parent="torso", rotation=[-20, 0, -30], cubes=cm(FIN))

# 長い腕（上腕と前腕に分ける）。指先が光る
UPPER = [c([-7.5, 29, -1.25], [2.5, 13, 2.5], "body", "upper_arm")]
FORE = [
    c([-7.5, 12, -1.25], [2.5, 17, 2.5], "body", "forearm"),
    c([-8, 15, -1.75], [3.5, 9, 3.5], "armor", "bracer"),
    c([-8, 8, -1.5], [3, 4, 3], "armor", "hand"),
    c([-7.5, 6, -1.5], [1, 2, 1], "glow", "claw"),
    c([-5.5, 6, -1.5], [1, 2, 1], "glow", "claw"),
    c([-7.5, 6, 0.5], [1, 2, 1], "glow", "claw"),
]
m.bone("right_arm", p(-6.25, 42, 0), parent="torso", cubes=UPPER)
m.bone("right_forearm", p(-6.25, 29, 0), parent="right_arm", cubes=FORE)
m.bone("left_arm", p(6.25, 42, 0), parent="torso", cubes=cm(UPPER))
m.bone("left_forearm", p(6.25, 29, 0), parent="left_arm", cubes=cm(FORE))

# 肩の横に浮かぶドローン
DRONE = [c([-14.5, 45, -1.5], [3, 3, 3], "armor", "drone"),
         c([-14, 45.5, -2.2], [2, 2, 1], "glow", "drone_eye"),
         c([-15.5, 46, -0.5], [1, 1, 1], "glow", "drone_tip"),
         c([-11.5, 46, -0.5], [1, 1, 1], "glow", "drone_tip")]
m.bone("drone_right", p(-13, 46.5, 0), parent="root", cubes=DRONE)
m.bone("drone_left", p(13, 46.5, 0), parent="root", cubes=cm(DRONE))

# ---------------------------------------------------------------- texture
PAL = {
    "body": ("#14101e", "#221a33", "#07050c"),
    "armor": ("#2a2538", "#3d3652", "#120f1a"),
    "glow": ("#c45cff", "#f2c8ff", "#8a2fd6"),
}
GLOW = [hexc("#8a2fd6"), hexc("#c45cff"), hexc("#f2c8ff")]


def circuit(m, fx, fy, fw, fh, n):
    """回路の線：まっすぐ進んで直角に曲がり、端に小さな点（端子）"""
    r = m.rng
    for _ in range(n):
        x, y = r.randrange(fw), r.randrange(fh)
        horiz = r.random() < 0.5
        for s in range(r.randrange(4, 12)):
            if 0 <= x < fw and 0 <= y < fh:
                m.put(fx + x, fy + y, GLOW[1], glow=True)
            if r.random() < 0.18:
                horiz = not horiz
            x, y = (x + 1, y) if horiz else (x, y + 1)
        if 0 <= x < fw and 0 <= y < fh:
            m.put(fx + x, fy + y, GLOW[2], glow=True)


def details(m, mat, tag, face, rect):
    fx, fy, fw, fh = rect
    r = m.rng
    if mat == "body" and face != "down":
        for _ in range(fw * fh // 10):                     # 宇宙のような細かい粒
            m.put(fx + r.randrange(fw), fy + r.randrange(fh), hexc(r.choice(("#1d1530", "#0b0814", "#2a1d44"))))
        if tag in ("leg", "forearm", "upper_arm") and face in ("north", "south", "east", "west"):
            x = fw // 2                                     # 手足にそって走る光の線
            for j in range(fh):
                if j % 7 != 3:
                    m.put(fx + x, fy + j, GLOW[1], glow=True)
        if tag == "chest" and face in ("north", "south"):
            circuit(m, fx, fy, fw, fh, 4)
        if tag == "head" and face in ("east", "west", "south", "up"):
            circuit(m, fx, fy, fw, fh, 2)
        if tag == "fin" and face in ("east", "west"):
            for j in range(0, fh, 3):                       # 翼の光の筋
                for k in range(fw):
                    if (k + j) % 4 == 0:
                        m.put(fx + k, fy + j, GLOW[1], glow=True)
    if mat == "armor" and face != "down":
        # 金属の板：上の縁が光り、パネルの継ぎ目
        if face != "up" and tag in ("chest_plate", "shoulder", "knee", "bracer", "head_cap", "hips", "foot"):
            for k in range(fw):
                m.put(fx + k, fy, GLOW[1], glow=True)
        if fw >= 6 and fh >= 4:
            for j in range(2, fh - 1):
                m.put(fx + fw // 2, fy + j, hexc("#100d18"))
            m.put(fx + 1, fy + fh - 2, GLOW[2], glow=True)
            m.put(fx + fw - 2, fy + fh - 2, GLOW[2], glow=True)
    if mat == "glow":
        for j in range(fh):
            for k in range(fw):
                if tag == "visor":                          # バイザー：真ん中ほど白く光る横一文字
                    col = GLOW[2] if abs(k - fw / 2) < fw / 4 else GLOW[1]
                elif tag in ("core", "drone_eye") and 0 < k < fw - 1 and 0 < j < fh - 1:
                    col = GLOW[2]
                else:
                    col = GLOW[(j + k) % 2 + 1]
                m.put(fx + k, fy + j, col, glow=True)


m.paint(PAL, details, glow_mats={"glow"}, outline_skip={"body", "glow"})
m.save()
