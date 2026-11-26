"""ダンジョンモブ 6回目：雷のセット（5体）。 python3 tools/dmob_thunder_gen.py
  thunder_lioness 雷鳴の牝獅子（中ボス）  volt_sentinel 雷の機兵（強）  storm_lizard 雷鳴の大トカゲ（強）
  spark_jelly 電気クラゲ（弱・半透明）     volt_beetle 雷虫（弱）
"""
import os, sys
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from geo_tools import Model, hexc
from dmob_fire_gen import B, L, glow_all

SPARK = ("#7ad8ff", "#e8f8ff", "#2a8ad0")


def eyes(m, rect, xs, y, color, w=1, h=1, glow=True):
    fx, fy, fw, fh = rect
    for x in xs:
        for d in range(w):
            for e in range(h):
                m.put(fx + x + d, fy + y + e, hexc(color), glow=glow)


def zigzag(m, rect, color="#8ae0ff", n=2):
    """稲妻もよう"""
    fx, fy, fw, fh = rect
    for _ in range(n):
        x, y = m.rng.randrange(fw), 0
        while y < fh:
            if 0 <= x < fw: m.put(fx + x, fy + y, hexc(color), glow=True)
            x += m.rng.choice((-1, 1)); y += 1


# ====================================================================== 雷鳴の牝獅子
def thunder_lioness():
    m = Model("thunder_lioness", 128, 128, seed=61)
    m.bone("root", [0, 0, 0])
    m.bone("body", [0, 12, 0], parent="root", cubes=[
        B([-4, 9, -8], [8, 8, 16], "fur", "torso"),
        B([-4, 9, -8], [8, 1, 16], "fur_light", "belly")])
    m.bone("head", [0, 15, -8], parent="body", cubes=[
        B([-3, 12, -14], [6, 6, 6], "fur", "head"),
        B([-2, 12, -16], [4, 3, 2], "fur_light", "muzzle"),
        B([-3, 18, -11], [2, 2, 1], "fur", "ear"), B([1, 18, -11], [2, 2, 1], "fur", "ear_l")])
    LEG_F = [B([-4, 0, -7], [3, 10, 3], "fur", "leg"), B([-4, 0, -8], [3, 2, 1], "fur_light", "paw")]
    LEG_B = [B([-4, 0, 4], [3, 10, 3], "fur", "leg"), B([-4, 0, 3], [3, 2, 1], "fur_light", "paw")]
    m.bone("front_right_leg", [-2.5, 10, -5.5], parent="root", cubes=LEG_F)
    m.bone("front_left_leg", [2.5, 10, -5.5], parent="root", cubes=L(LEG_F))
    m.bone("back_right_leg", [-2.5, 10, 5.5], parent="root", cubes=LEG_B)
    m.bone("back_left_leg", [2.5, 10, 5.5], parent="root", cubes=L(LEG_B))
    m.bone("tail", [0, 16, 8], parent="body", rotation=[-30, 0, 0], cubes=[
        B([-1, 15, 8], [2, 2, 11], "fur", "tail"), B([-1.5, 14.5, 18], [3, 3, 3], "spark", "tuft")])
    pal = {"fur": ("#c8a060", "#e4c080", "#7a5a2a"), "fur_light": ("#f0dcb0", "#fff4dc", "#c0a878"), "spark": SPARK}

    def det(m, mat, tag, face, rect):
        fx, fy, fw, fh = rect
        if mat == "spark": glow_all(m, rect)
        if tag in ("torso", "leg") and face in ("up", "east", "west"): zigzag(m, rect, n=2 if tag == "torso" else 1)
        if tag == "head" and face == "north":
            eyes(m, rect, (1, 4), 2, "#7ae0ff")
        if tag == "muzzle" and face == "north":
            m.put(fx + 1, fy, hexc("#3a2a1a")); m.put(fx + 2, fy, hexc("#3a2a1a"))
    m.paint(pal, det)
    m.save()


# ====================================================================== 雷の機兵
def volt_sentinel():
    m = Model("volt_sentinel", 128, 128, seed=62)
    m.bone("root", [0, 0, 0])
    LEG = [B([-5, 0, -2], [4, 13, 5], "copper", "leg"), B([-5, 0, -3], [4, 3, 6], "copper_dark", "foot"),
           B([-5, 6, -3], [4, 2, 1], "spark", "knee")]
    m.bone("right_leg", [-3, 13, 0], parent="root", cubes=LEG)
    m.bone("left_leg", [3, 13, 0], parent="root", cubes=L(LEG))
    m.bone("body", [0, 13, 0], parent="root", cubes=[
        B([-6, 13, -3], [12, 12, 6], "copper", "torso"),
        B([-2, 17, -4], [4, 4, 1], "spark", "core"),
        B([-7, 13, -4], [14, 2, 8], "copper_dark", "waist")])
    m.bone("head", [0, 25, 0], parent="body", cubes=[
        B([-3, 25, -3], [6, 6, 6], "copper", "head"),
        B([0, 31, 0], [1, 6, 1], "copper_dark", "antenna"),
        B([-1, 37, -1], [2, 2, 2], "spark", "antenna_ball")])
    ARM = [B([-10, 12, -2], [4, 13, 4], "copper", "arm"), B([-11, 22, -3], [5, 4, 6], "copper_dark", "shoulder"),
           B([-10, 15, -3], [4, 1, 1], "spark", "coil1"), B([-10, 18, -3], [4, 1, 1], "spark", "coil2"),
           B([-11, 10, -3], [6, 3, 6], "copper_dark", "fist")]
    m.bone("right_arm", [-8, 24, 0], parent="body", cubes=ARM)
    m.bone("left_arm", [8, 24, 0], parent="body", cubes=L(ARM))
    pal = {"copper": ("#c87a4a", "#e8a06a", "#7a4424"), "copper_dark": ("#6a4a3a", "#8a6a54", "#3a261c"), "spark": SPARK}

    def det(m, mat, tag, face, rect):
        fx, fy, fw, fh = rect
        if mat == "spark": glow_all(m, rect)
        if mat == "copper":
            for _ in range(fw * fh // 16): m.put(fx + m.rng.randrange(fw), fy + m.rng.randrange(fh), hexc("#5aa890"))   # 緑青
        if tag == "head" and face == "north":
            for k in range(1, fw - 1): m.put(fx + k, fy + 2, hexc("#7ad8ff"), glow=True)
    m.paint(pal, det)
    m.save()


# ====================================================================== 雷鳴の大トカゲ
def storm_lizard():
    m = Model("storm_lizard", 128, 64, seed=63)
    m.bone("root", [0, 0, 0])
    crest = [B([-0.5 * 0, 9, -6 + i * 3], [1, 2 + (i % 2), 2], "spark", f"crest{i}") for i in range(5)]
    m.bone("body", [0, 6, 0], parent="root", cubes=[
        B([-4, 3, -8], [8, 6, 16], "scale", "torso"),
        B([-4, 3, -8], [8, 1, 16], "belly", "belly")] + crest)
    m.bone("head", [0, 6, -8], parent="body", cubes=[
        B([-3, 4, -14], [6, 4, 6], "scale", "head"), B([-2, 3, -17], [4, 2, 3], "scale", "snout"),
        B([-3, 3, -14], [6, 1, 6], "belly", "jaw")])
    LEG = [B([-7, 0, -7], [3, 4, 3], "scale", "leg_f"), B([-7, 0, 4], [3, 4, 3], "scale", "leg_b")]
    m.bone("right_legs", [-4, 4, 0], parent="root", cubes=LEG)
    m.bone("left_legs", [4, 4, 0], parent="root", cubes=L(LEG))
    m.bone("tail", [0, 6, 8], parent="body", cubes=[
        B([-2, 4, 8], [4, 3, 8], "scale", "tail1"), B([-1, 4, 16], [2, 2, 8], "scale", "tail2"),
        B([-0.5 * 0, 6, 10], [1, 2, 2], "spark", "tail_crest")])
    pal = {"scale": ("#3a4a62", "#566a88", "#1e2836"), "belly": ("#8a9aaa", "#aabaca", "#5a6a7a"), "spark": SPARK}

    def det(m, mat, tag, face, rect):
        fx, fy, fw, fh = rect
        if mat == "spark": glow_all(m, rect)
        if mat == "scale" and face == "up":
            for j in range(0, fh, 2):
                for k in range(j % 4 // 2, fw, 2): m.put(fx + k, fy + j, hexc("#2a3648"))
        if tag == "torso" and face in ("east", "west"): zigzag(m, rect, n=2)
        if tag == "head" and face in ("east", "west"): eyes(m, rect, (2,), 1, "#ffe040")
    m.paint(pal, det)
    m.save()


# ====================================================================== 電気クラゲ
def spark_jelly():
    m = Model("spark_jelly", 64, 64, seed=64)
    m.bone("root", [0, 0, 0])
    m.bone("bell", [0, 10, 0], parent="root", cubes=[
        B([-4, 9, -4], [8, 5, 8], "jelly", "bell"), B([-3, 14, -3], [6, 2, 6], "jelly", "bell_top"),
        B([-2, 10, -2], [4, 3, 4], "core", "core")])
    tent = []
    for i, (x, z) in enumerate(((-3, -3), (2, -3), (-3, 2), (2, 2), (-1, 0))):
        tent.append(B([x, 1 + (i % 2) * 2, z], [1, 8 - (i % 2) * 2, 1], "tentacle", f"tentacle{i}"))
    m.bone("tentacles", [0, 9, 0], parent="bell", cubes=tent)
    pal = {"jelly": ("#7ac0ff", "#d0ecff", "#3a80d0"), "core": ("#e8f8ff", "#ffffff", "#9ad0ff"), "tentacle": SPARK}

    def det(m, mat, tag, face, rect):
        fx, fy, fw, fh = rect
        for j in range(fh):
            for k in range(fw):
                c = m.tex[fy + j, fx + k]
                if c[3]:
                    a = 150 if mat == "jelly" else 230
                    m.tex[fy + j, fx + k] = (int(c[0]), int(c[1]), int(c[2]), a)
                    m.glow[fy + j, fx + k] = (int(c[0]), int(c[1]), int(c[2]), 130 if mat == "jelly" else 255)
        if tag == "bell" and face == "north": eyes(m, rect, (2, 5), 2, "#0a2a4a", glow=False)
    m.paint(pal, det)
    m.save()


# ====================================================================== 雷虫
def volt_beetle():
    m = Model("volt_beetle", 64, 64, seed=65)
    m.bone("root", [0, 0, 0])
    m.bone("body", [0, 2, 0], parent="root", cubes=[
        B([-3, 1, -4], [6, 4, 8], "shell", "shell"),
        B([-2, 1, -6], [4, 3, 2], "shell_dark", "head"),
        B([0, 3, -9], [1, 1, 3], "shell_dark", "horn"), B([-0.5 * 0, 4, -10], [1, 2, 1], "spark", "horn_tip")])
    LEG = [B([-5, 0, -3], [2, 1, 1], "shell_dark", "leg1"), B([-5, 0, 0], [2, 1, 1], "shell_dark", "leg2"),
           B([-5, 0, 3], [2, 1, 1], "shell_dark", "leg3")]
    m.bone("right_legs", [-3, 1, 0], parent="root", cubes=LEG)
    m.bone("left_legs", [3, 1, 0], parent="root", cubes=L(LEG))
    pal = {"shell": ("#2a3a5a", "#4a5a80", "#141e30"), "shell_dark": ("#1a2234", "#2a3448", "#0a0e18"), "spark": SPARK}

    def det(m, mat, tag, face, rect):
        fx, fy, fw, fh = rect
        if mat == "spark": glow_all(m, rect)
        if tag == "shell" and face == "up":
            for j in range(fh): m.put(fx + fw // 2, fy + j, hexc("#7ad8ff"), glow=True)
        if tag == "head" and face == "north": eyes(m, rect, (0, 3), 1, "#7ad8ff")
    m.paint(pal, det)
    m.save()


if __name__ == "__main__":
    for f in (thunder_lioness, volt_sentinel, storm_lizard, spark_jelly, volt_beetle):
        f()
