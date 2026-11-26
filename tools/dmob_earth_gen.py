"""ダンジョンモブ 7回目：大地のセット（5体）。 python3 tools/dmob_earth_gen.py
  boulder_golem 岩石のゴーレム（中ボス）  stone_tortoise 岩甲羅の亀（強）  clay_brute 粘土の巨兵（強）
  pebble_golem 小石ゴーレム（弱）          burrow_mole もぐら（弱）
"""
import os, sys
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from geo_tools import Model, hexc
from dmob_fire_gen import B, L, glow_all

STONE = ("#8a8a84", "#aaaaa2", "#55554f")
MOSS = ("#5a8a3a", "#7aaa52", "#3a5a22")
AMBER = ("#ffb040", "#ffe090", "#c07010")


def eyes(m, rect, xs, y, color, w=1, h=1, glow=True):
    fx, fy, fw, fh = rect
    for x in xs:
        for d in range(w):
            for e in range(h):
                m.put(fx + x + d, fy + y + e, hexc(color), glow=glow)


def cracks(m, rect, color, n=2):
    fx, fy, fw, fh = rect
    for _ in range(n):
        x, y = m.rng.randrange(fw), m.rng.randrange(fh)
        for _ in range(4):
            if 0 <= x < fw and 0 <= y < fh: m.put(fx + x, fy + y, hexc(color))
            x += m.rng.choice((-1, 0, 1)); y += 1


def moss_top(m, rect, face):
    fx, fy, fw, fh = rect
    if face == "up":
        for j in range(fh):
            for k in range(fw):
                if m.rng.random() < 0.75: m.put(fx + k, fy + j, hexc(MOSS[0]) + m.rng.uniform(-10, 10))
    elif face in ("north", "south", "east", "west"):
        for k in range(fw):
            for j in range(m.rng.randrange(1, 3)): m.put(fx + k, fy + j, hexc(MOSS[0]))


# ====================================================================== 岩石のゴーレム
def boulder_golem():
    m = Model("boulder_golem", 128, 128, seed=71)
    m.bone("root", [0, 0, 0])
    LEG = [B([-6, 0, -3], [5, 11, 6], "stone", "leg")]
    m.bone("right_leg", [-3.5, 11, 0], parent="root", cubes=LEG)
    m.bone("left_leg", [3.5, 11, 0], parent="root", cubes=L(LEG))
    m.bone("body", [0, 11, 0], parent="root", cubes=[
        B([-7, 11, -4], [14, 13, 8], "stone", "torso"),
        B([-2, 16, -5], [4, 4, 1], "amber", "core")])
    m.bone("head", [0, 23, -1], parent="body", cubes=[B([-3, 22, -5], [6, 5, 6], "stone", "head")])
    ARM = [B([-12, 5, -3], [5, 18, 6], "stone", "arm"), B([-13, 2, -4], [7, 6, 8], "stone", "fist"),
           B([-12, 21, -2], [2, 4, 2], "amber", "crystal", [0, 0, 20]), B([-10, 22, 0], [2, 3, 2], "amber", "crystal2", [0, 0, -10])]
    m.bone("right_arm", [-9, 22, 0], parent="body", cubes=ARM)
    m.bone("left_arm", [9, 22, 0], parent="body", cubes=L(ARM))
    pal = {"stone": STONE, "amber": AMBER}

    def det(m, mat, tag, face, rect):
        fx, fy, fw, fh = rect
        if mat == "amber": glow_all(m, rect)
        if mat == "stone":
            cracks(m, rect, "#3a3a36", 1 + fw * fh // 60)
            if tag in ("torso", "arm", "head"): moss_top(m, rect, face)
        if tag == "head" and face == "north": eyes(m, rect, (1, 4), 2, "#ffb040")
    m.paint(pal, det)
    m.save()


# ====================================================================== 岩甲羅の亀
def stone_tortoise():
    m = Model("stone_tortoise", 128, 128, seed=72)
    m.bone("root", [0, 0, 0])
    m.bone("shell", [0, 4, 0], parent="root", cubes=[
        B([-7, 4, -8], [14, 7, 16], "stone", "shell"),
        B([-5, 11, -6], [10, 2, 12], "stone", "shell_top"),
        B([-7, 3, -8], [14, 1, 16], "under", "plastron"),
        B([-2, 13, -2], [4, 1, 4], "amber", "gem")])
    m.bone("head", [0, 6, -8], parent="shell", cubes=[
        B([-2, 4, -12], [4, 4, 5], "skin", "head"), B([-2, 4, -13], [4, 2, 1], "beak", "beak")])
    LEG = [B([-8, 0, -7], [4, 5, 4], "skin", "leg_f"), B([-8, 0, 3], [4, 5, 4], "skin", "leg_b")]
    m.bone("right_legs", [-6, 5, 0], parent="root", cubes=LEG)
    m.bone("left_legs", [6, 5, 0], parent="root", cubes=L(LEG))
    m.bone("tail", [0, 5, 8], parent="shell", cubes=[B([-1, 4, 8], [2, 2, 3], "skin", "tail")])
    pal = {"stone": STONE, "under": ("#c8b890", "#e0d4b0", "#8a7a5a"), "skin": ("#6a7a5a", "#8a9a72", "#3a4a2e"),
           "beak": ("#3a3a32", "#5a5a4e", "#1e1e18"), "amber": AMBER}

    def det(m, mat, tag, face, rect):
        fx, fy, fw, fh = rect
        if mat == "amber": glow_all(m, rect)
        if mat == "stone":
            if face == "up":
                for j in range(0, fh, 4):
                    for k in range(fw): m.put(fx + k, fy + j, hexc("#55554f"))
                for k in range(0, fw, 4):
                    for j in range(fh): m.put(fx + k, fy + j, hexc("#55554f"))
            moss_top(m, rect, face)
        if tag == "head" and face == "north": eyes(m, rect, (0, 3), 1, "#ffe040")
    m.paint(pal, det)
    m.save()


# ====================================================================== 粘土の巨兵
def clay_brute():
    m = Model("clay_brute", 128, 128, seed=73)
    m.bone("root", [0, 0, 0])
    LEG = [B([-5, 0, -3], [5, 12, 6], "clay", "leg")]
    m.bone("right_leg", [-3, 12, 0], parent="root", cubes=LEG)
    m.bone("left_leg", [3, 12, 0], parent="root", cubes=L(LEG))
    m.bone("body", [0, 12, 0], parent="root", rotation=[12, 0, 0], cubes=[
        B([-7, 12, -4], [14, 12, 9], "clay", "torso"),
        B([-6, 22, -3], [12, 4, 7], "clay_dark", "hump")])
    m.bone("head", [0, 23, -3], parent="body", cubes=[B([-3, 21, -8], [6, 6, 6], "clay", "head")])
    ARM = [B([-12, 8, -3], [5, 16, 6], "clay", "arm"), B([-13, 5, -4], [7, 5, 8], "clay_dark", "fist")]
    m.bone("right_arm", [-9, 23, 0], parent="body", cubes=ARM)
    m.bone("left_arm", [9, 23, 0], parent="body", cubes=L(ARM))
    pal = {"clay": ("#b0704a", "#cc8a60", "#6a3e24"), "clay_dark": ("#7a4a32", "#9a6248", "#42261a")}

    def det(m, mat, tag, face, rect):
        fx, fy, fw, fh = rect
        cracks(m, rect, "#4a2a18", 1 + fw * fh // 50)
        if face in ("north", "south", "east", "west"):
            for k in range(fw):                                   # 垂れる泥
                if m.rng.random() < 0.25:
                    for j in range(m.rng.randrange(1, 4)): m.put(fx + k, fy + fh - 1 - j, hexc("#5a3a24"))
        if tag == "head" and face == "north":
            for x in (1, 4):
                for e in range(2): m.put(fx + x, fy + 2 + e, hexc("#1a0e08"))
            m.put(fx + 1, fy + 2, hexc("#ffb040"), glow=True); m.put(fx + 4, fy + 2, hexc("#ffb040"), glow=True)
            for k in range(1, 5): m.put(fx + k, fy + 4, hexc("#2a160c"))
    m.paint(pal, det)
    m.save()


# ====================================================================== 小石ゴーレム
def pebble_golem():
    m = Model("pebble_golem", 64, 64, seed=74)
    m.bone("root", [0, 0, 0])
    LEG = [B([-3, 0, -1], [2, 3, 2], "stone", "leg")]
    m.bone("right_leg", [-2, 3, 0], parent="root", cubes=LEG)
    m.bone("left_leg", [2, 3, 0], parent="root", cubes=L(LEG))
    m.bone("body", [0, 3, 0], parent="root", cubes=[
        B([-3, 3, -3], [6, 6, 6], "stone", "torso"),
        B([-2, 9, -2], [4, 4, 4], "stone", "head"),
        B([-1, 13, -1], [2, 1, 2], "moss", "moss")])
    ARM = [B([-5, 4, -1], [2, 5, 2], "stone", "arm"), B([-6, 3, -2], [3, 2, 3], "stone", "hand")]
    m.bone("right_arm", [-4, 8, 0], parent="body", cubes=ARM)
    m.bone("left_arm", [4, 8, 0], parent="body", cubes=L(ARM))
    pal = {"stone": ("#9a968c", "#bab6aa", "#625e56"), "moss": MOSS}

    def det(m, mat, tag, face, rect):
        fx, fy, fw, fh = rect
        if mat == "stone": cracks(m, rect, "#4a4640", 1)
        if tag == "head" and face == "north": eyes(m, rect, (0, 3), 1, "#ffb040")
    m.paint(pal, det)
    m.save()


# ====================================================================== もぐら
def burrow_mole():
    m = Model("burrow_mole", 64, 64, seed=75)
    m.bone("root", [0, 0, 0])
    m.bone("body", [0, 2, 0], parent="root", cubes=[
        B([-3, 0, -4], [6, 5, 9], "fur", "torso"),
        B([-1, 1, -6], [2, 2, 2], "nose", "snout")])
    CLAW = [B([-5, 0, -5], [2, 2, 3], "claw", "claw")]
    m.bone("right_claw", [-3, 2, -4], parent="body", cubes=CLAW)
    m.bone("left_claw", [3, 2, -4], parent="body", cubes=L(CLAW))
    pal = {"fur": ("#3a3036", "#54484e", "#1e181c"), "nose": ("#e8a0a8", "#ffc8d0", "#a86870"), "claw": ("#e8dcc8", "#fff4e4", "#a89a84")}

    def det(m, mat, tag, face, rect):
        fx, fy, fw, fh = rect
        if tag == "torso" and face == "north":
            m.put(fx + 1, fy + 2, hexc("#0a0608")); m.put(fx + 4, fy + 2, hexc("#0a0608"))
    m.paint(pal, det)
    m.save()


if __name__ == "__main__":
    for f in (boulder_golem, stone_tortoise, clay_brute, pebble_golem, burrow_mole):
        f()
