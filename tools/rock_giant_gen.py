"""岩の巨人：モデル＋テクスチャ＋発光マスク。 python3 tools/rock_giant_gen.py
苔むした岩の巨人。肩と腕が大きく、頭は胸にめり込むように小さい（ゴリラのような体つき）。
背中から緑に光る結晶が突き出している。正面は -Z。身長は約4.5ブロック。
"""
import os, sys
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from geo_tools import Model, mirror, hexc

m = Model("rock_giant", 256, 256, seed=91)

# ---------------------------------------------------------------- model
m.bone("root", [0, 0, 0])

LEG = [
    ([-16, 0, -8], [14, 6, 15], "rock_dark", "foot"),
    ([-15, 5, -6], [12, 16, 12], "rock", "leg"),
    ([-16, 12, -7], [6, 7, 5], "rock", "knee_rock"),
]
m.bone("right_leg", [-9, 21, 0], parent="root", cubes=LEG)
m.bone("left_leg", [9, 21, 0], parent="root", cubes=mirror(LEG))

m.bone("waist", [0, 21, 0], parent="root", cubes=[
    ([-15, 18, -9], [30, 10, 18], "rock_dark", "hips"),
])

# 胴体：前かがみ。大きな胸の岩と、背中の苔、腹の割れ目
m.bone("torso", [0, 27, 2], parent="waist", rotation=[14, 0, 0], cubes=[
    ([-17, 26, -11], [34, 14, 21], "rock", "belly"),
    ([-21, 39, -13], [42, 17, 25], "rock", "chest"),
    ([-18, 54, -9], [36, 6, 20], "moss", "back_moss"),
    ([-12, 42, -15], [9, 8, 3], "rock", "chest_plate"),     # 胸の岩のでっぱり
    ([4, 44, -14.5], [8, 7, 2], "rock", "chest_plate"),
    ([-3, 30, -12.5], [5, 5, 2], "crystal", "core"),        # 腹の割れ目からのぞく結晶
])

# 肩の大岩（苔がのっている）
SHOULDER = [
    ([-33, 47, -11], [16, 14, 19], "rock", "shoulder"),
    ([-33, 60, -10], [15, 3, 17], "moss", "shoulder_moss"),
]
m.bone("right_shoulder", [-25, 54, 0], parent="torso", cubes=SHOULDER)
m.bone("left_shoulder", [25, 54, 0], parent="torso", cubes=mirror(SHOULDER))

# 小さな頭：胸の前にめり込むように低い位置
m.bone("head", [0, 51, -12], parent="torso", cubes=[
    ([-7, 46, -22], [14, 12, 12], "rock", "face"),
    ([-9, 55, -19], [3, 4, 4], "rock_dark", "horn"),
    ([6, 55, -19], [3, 4, 4], "rock_dark", "horn"),
    ([-7, 56, -20], [14, 3, 10], "rock_dark", "brow"),
    ([-6, 44, -22.5], [12, 4, 9], "rock_dark", "jaw"),
])

# 背中の結晶
CRYST = []
for i, (x, z, h, lean) in enumerate(((-11, 4, 12, -15), (-2, 7, 16, 0), (8, 3, 11, 18), (2, -2, 9, 8), (-15, -3, 8, -25))):
    m.bone(f"crystal{i}", [x + 2, 56, z + 2], parent="torso", rotation=[-20 + (i % 2) * 10, 0, lean], cubes=[
        ([x, 55, z], [4, h, 4], "crystal", "crystal"),
        ([x + 1, 55 + h, z + 1], [2, 3, 2], "crystal", "crystal_tip"),
    ])

# 腕：太い上腕、さらに太い前腕、地面近くまで届く大きなこぶし
UPPER = [([-34, 30, -6], [12, 20, 12], "rock", "upper_arm"),
         ([-36, 38, -4], [4, 7, 7], "rock_dark", "arm_rock")]
FORE = [
    ([-36, 12, -8], [16, 19, 16], "rock", "forearm"),
    ([-36, 22, -8.5], [6, 5, 5], "moss", "arm_moss"),
    ([-38, 15, -4], [3, 8, 8], "rock_dark", "arm_rock"),
]
FIST = [
    ([-37, -2, -9], [18, 15, 18], "rock_dark", "fist"),
    ([-38, 4, -8], [3, 6, 6], "rock_dark", "knuckle"),
]
m.bone("right_arm", [-28, 52, 0], parent="torso", cubes=UPPER)
m.bone("right_forearm", [-28, 31, 0], parent="right_arm", cubes=FORE)
m.bone("right_fist", [-28, 13, 0], parent="right_forearm", cubes=FIST)
m.bone("left_arm", [28, 52, 0], parent="torso", cubes=mirror(UPPER))
m.bone("left_forearm", [28, 31, 0], parent="left_arm", cubes=mirror(FORE))
m.bone("left_fist", [28, 13, 0], parent="left_forearm", cubes=mirror(FIST))

# ---------------------------------------------------------------- texture
PAL = {
    "rock": ("#7a7468", "#958e80", "#4a463e"),
    "rock_dark": ("#5d584f", "#736d62", "#35322c"),
    "moss": ("#4f7a2e", "#6c9a3c", "#2f4a1a"),
    "crystal": ("#4fe0a0", "#c8ffe4", "#1f9a68"),
}
GLOW = [hexc("#2fbf82"), hexc("#6ff0b8"), hexc("#d8fff0")]
CRACK = hexc("#2a2722")


def crack(m, fx, fy, fw, fh, x, y, n):
    """岩のひび（ジグザグの暗い線）"""
    r = m.rng
    for _ in range(n):
        if 0 <= x < fw and 0 <= y < fh:
            m.put(fx + x, fy + y, CRACK)
        x += r.choice((-1, 0, 1))
        y += 1


def details(m, mat, tag, face, rect):
    fx, fy, fw, fh = rect
    r = m.rng
    if mat in ("rock", "rock_dark"):
        # 岩のまだら模様と小石
        for _ in range(fw * fh // 14):
            x, y = r.randrange(fw), r.randrange(fh)
            m.put(fx + x, fy + y, hexc(r.choice(("#6a645a", "#857e70", "#5a554c", "#8f8878"))))
        if face not in ("down",):
            for _ in range(max(1, fw * fh // 90)):
                crack(m, fx, fy, fw, fh, r.randrange(fw), r.randrange(max(1, fh // 2)), r.randrange(4, 10))
        # 上の面には苔がところどころ
        if face == "up":
            for _ in range(fw * fh // 4):
                m.put(fx + r.randrange(fw), fy + r.randrange(fh), hexc(r.choice(("#4f7a2e", "#5f8a36"))))
        elif face != "down" and tag not in ("face", "jaw", "fist", "knuckle"):
            for k in range(fw):                         # 苔が上から垂れる
                for j in range(r.choice((0, 0, 1, 1, 2, 3))):
                    m.put(fx + k, fy + j, hexc(r.choice(("#4f7a2e", "#6c9a3c"))))
    if mat == "moss" and face != "down":
        for _ in range(fw * fh // 3):
            m.put(fx + r.randrange(fw), fy + r.randrange(fh), hexc(r.choice(("#3f6a24", "#6c9a3c", "#7fae46", "#355a1e"))))
        if face not in ("up",):
            for k in range(fw):                         # すそがギザギザに垂れる
                for j in range(fh - r.choice((0, 0, 1)), fh):
                    m.clear(fx + k, fy + j)
    if tag == "face" and face == "north":
        # くぼんだ目の奥で光る緑の目
        ye = fh // 2 - 1
        for k in (2, 3, fw - 4, fw - 3):
            m.put(fx + k, fy + ye - 1, hexc("#24211c"))
            m.put(fx + k, fy + ye, GLOW[1], glow=True)
            m.put(fx + k, fy + ye + 1, hexc("#24211c"))
        m.put(fx + 3, fy + ye, GLOW[2], glow=True)
        m.put(fx + fw - 4, fy + ye, GLOW[2], glow=True)
    if tag == "jaw" and face == "north":
        for k in range(1, fw - 1):
            m.put(fx + k, fy + 1, hexc("#24211c"))
    if tag in ("chest", "belly") and face == "north":
        # 胸の割れ目から漏れる光（太めのジグザグ）
        for x0 in ((fw // 2 - 4, fw // 2 + 5) if tag == "chest" else (fw // 2,)):
            x = x0
            for j in range(fh):
                for dx in (0, 1):
                    if 0 <= x + dx < fw:
                        m.put(fx + x + dx, fy + j, GLOW[1] if dx else GLOW[0], glow=True)
                if j % 4 == 0:
                    m.put(fx + x, fy + j, GLOW[2], glow=True)
                x += r.choice((-1, 0, 0, 1))
                x = max(1, min(fw - 3, x))
    if mat == "crystal":
        for j in range(fh):
            for k in range(fw):
                m.put(fx + k, fy + j, GLOW[(j + k) % 3] if tag != "core" else GLOW[(k // 2 + j) % 3], glow=True)


m.paint(PAL, details, glow_mats={"crystal"}, outline_skip={"rock", "rock_dark", "moss", "crystal"},
        soft={"mats": {"rock", "rock_dark"}, "y": (0, 70), "dark": 0.7})
m.save()
