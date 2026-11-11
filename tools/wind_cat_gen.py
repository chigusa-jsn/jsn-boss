"""風の猫：モデル＋テクスチャ＋発光マスク。 python3 tools/wind_cat_gen.py
四つ足の大きな山猫。正面は -Z。全体を K 倍の大きさで作る。
"""
import os, sys
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from geo_tools import Model, mirror, hexc

K = 1.6
m = Model("wind_cat", 256, 256, seed=61)


def c(o, s, mat, tag):
    return ([round(v * K * 2) / 2 for v in o], [max(0, round(v * K)) if v == 0 else max(1, round(v * K)) for v in s], mat, tag)


def p(*xyz):
    return [round(v * K * 2) / 2 for v in xyz]


# ---------------------------------------------------------------- model
m.bone("root", [0, 0, 0])
m.bone("body", p(0, 15, 0), parent="root", cubes=[
    c([-5, 10, -11], [10, 9, 11], "fur", "chest"),
    c([-4.5, 10.5, 0], [9, 8, 9], "fur", "belly"),
    c([-5, 10, 9], [10, 9, 7], "fur", "hips"),
    c([-4, 9.5, -9], [8, 1, 18], "fur_white", "underside"),
])

# 首と頭：頬のふさふさ、耳の房
m.bone("neck", p(0, 17, -11), parent="body", rotation=[-20, 0, 0], cubes=[
    c([-3.5, 14, -16], [7, 7, 6], "fur", "neck"),
    c([-4.5, 13, -14], [9, 2, 6], "cloth", "scarf"),
])
m.bone("head", p(0, 18, -16), parent="neck", rotation=[20, 0, 0], cubes=[
    c([-4.5, 15, -23], [9, 7, 7], "fur", "skull"),
    c([-2.5, 15, -26], [5, 4, 3], "fur_white", "muzzle"),
    c([-1, 17.5, -26.5], [2, 1, 1], "nose", "nose"),
    c([-6, 14, -21], [2, 5, 4], "fur_white", "cheek_ruff"),
    c([4, 14, -21], [2, 5, 4], "fur_white", "cheek_ruff"),
])
EAR = [c([-4, 22, -19], [3, 4, 2], "fur", "ear"), c([-3, 26, -18.5], [1, 3, 1], "tuft", "tuft"),
       c([-3.5, 22.5, -19.5], [2, 3, 1], "ear_inner", "ear_inner")]
m.bone("right_ear", p(-2.5, 22, -18), parent="head", rotation=[-5, 0, 8], cubes=EAR)
m.bone("left_ear", p(2.5, 22, -18), parent="head", rotation=[-5, 0, -8], cubes=mirror(EAR))

# 首の布からなびく2本のリボン（風になびく）
for side, name in ((-1, "right_ribbon"), (1, "left_ribbon")):
    x = side * 4.6
    m.bone(name, p(x, 15, -12), parent="neck", rotation=[-8, side * 10, 0], cubes=[
        c([x, 13.5, -12], [0, 2, 16], "ribbon", "ribbon"),
    ])

# 脚
FRONT = [c([-4.5, 0, -8.5], [3.5, 10, 3.5], "fur", "front_leg"), c([-5, 0, -9.5], [4.5, 2, 4.5], "fur_white", "paw")]
HIND = [c([-5, 4, 9], [4, 8, 6], "fur", "thigh")]
HIND_LOW = [c([-4.5, 0, 12], [3.5, 5, 3.5], "fur", "shin"), c([-5, 0, 11], [4.5, 2, 4.5], "fur_white", "paw")]
m.bone("front_right", p(-3, 10, -7), parent="body", cubes=FRONT)
m.bone("front_left", p(3, 10, -7), parent="body", cubes=mirror(FRONT))
m.bone("hind_right", p(-3, 11, 12), parent="body", cubes=HIND)
m.bone("hind_right_low", p(-3, 5, 13), parent="hind_right", cubes=HIND_LOW)
m.bone("hind_left", p(3, 11, 12), parent="body", cubes=mirror(HIND))
m.bone("hind_left_low", p(3, 5, 13), parent="hind_left", cubes=mirror(HIND_LOW))

# 長く風に流れる尻尾（3節）
m.bone("tail1", p(0, 16, 16), parent="body", rotation=[30, 0, 0], cubes=[c([-1.5, 14.5, 16], [3, 3, 8], "fur", "tail")])
m.bone("tail2", p(0, 16, 24), parent="tail1", rotation=[18, 0, 0], cubes=[c([-2, 14, 24], [4, 4, 8], "fur", "tail")])
m.bone("tail3", p(0, 16, 32), parent="tail2", rotation=[12, 0, 0], cubes=[
    c([-1.5, 14.5, 32], [3, 3, 6], "fur", "tail_tip"),
    c([0, 13.5, 33], [0, 5, 6], "ribbon", "tail_wind"),
])

# ---------------------------------------------------------------- texture
PAL = {
    "fur": ("#cdd6cf", "#e6ede7", "#8e9a92"),
    "fur_white": ("#f1f4f0", "#ffffff", "#c0c8c1"),
    "tuft": ("#2a302c", "#3c443f", "#141816"),
    "ear_inner": ("#e9c8c4", "#f6dcd8", "#b89490"),
    "nose": ("#b07a7c", "#c99496", "#7a5052"),
    "cloth": ("#2f9e84", "#55c9ab", "#17584a"),
    "ribbon": ("#6fe0c2", "#c9fff0", "#2f9e84"),
}
JADE = hexc("#3aa688")
EYE = [hexc("#4dff9c"), hexc("#c8ffe0")]
MINT = [hexc("#7cf0cf"), hexc("#c9fff0"), hexc("#ffffff")]


def details(m, mat, tag, face, rect):
    fx, fy, fw, fh = rect
    r = m.rng
    # 翡翠色の縞（背中と脚の外側）
    if mat == "fur" and tag not in ("skull", "ear", "tail_tip"):
        if face in ("west", "east"):                      # 体の横：縦に走る縞
            for k in range(1, fw - 1, 4):
                for j in range(fh - 1):
                    if r.random() < 0.9:
                        m.put(fx + k, fy + j, JADE)
                        if j < fh // 2:
                            m.put(fx + k + 1, fy + j, JADE)
        elif face == "up":                                # 背中：横切る縞
            for j in range(1, fh - 1, 4):
                for k in range(fw):
                    if r.random() < 0.9:
                        m.put(fx + k, fy + j, JADE)
        elif face in ("north", "south") and tag in ("tail", "front_leg", "shin", "thigh"):
            for j in range(1, fh - 1, 3):                 # 尻尾と脚：輪の縞
                for k in range(fw):
                    m.put(fx + k, fy + j, JADE)
    if mat == "fur" and face not in ("down",):
        for _ in range(max(1, fw * fh // 12)):
            m.put(fx + r.randrange(fw), fy + r.randrange(fh), hexc("#bcc7bf"))
    # 顔：エメラルドの目、額の縞
    if tag == "skull" and face == "north":
        ye = fh // 2
        for k in (1, 2, fw - 3, fw - 2):
            m.put(fx + k, fy + ye, EYE[0], glow=True)
        m.put(fx + 2, fy + ye - 1, hexc("#1e2420"))
        m.put(fx + fw - 3, fy + ye - 1, hexc("#1e2420"))
        m.put(fx + 2, fy + ye, EYE[1], glow=True)
        m.put(fx + fw - 3, fy + ye, EYE[1], glow=True)
        for j in range(0, ye - 1):                         # 額の3本線
            for k in (fw // 2 - 2, fw // 2, fw // 2 + 2):
                if 0 <= k < fw:
                    m.put(fx + k, fy + j, JADE)
    if tag == "muzzle" and face == "north":
        for k in range(fw):
            if k in (0, fw - 1):
                m.put(fx + k, fy + fh - 1, hexc("#9aa59e"))
    # 布とリボン：風の紋様、リボンは淡く光る
    if mat == "cloth" and face not in ("down",):
        for k in range(fw):
            if k % 4 == 1:
                m.put(fx + k, fy, hexc("#c9fff0"))
    if mat == "ribbon":
        for j in range(fh):
            for k in range(fw):
                v = MINT[0] if (k + j) % 6 else MINT[1]
                if k >= fw - 2 and r.random() < 0.5:
                    m.clear(fx + k, fy + j)                # 先がほどける
                else:
                    m.put(fx + k, fy + j, v, glow=(k + j) % 3 == 0)
    if tag == "tail_tip" and face not in ("down",):
        for k in range(fw):
            m.put(fx + k, fy + fh - 1, hexc("#2a302c"))


m.paint(PAL, details, glow_mats=set(), outline_skip={"fur", "fur_white", "ribbon", "cloth"},
        soft={"mats": {"fur", "fur_white"}, "y": (0, 40), "dark": 0.82})
m.save()
