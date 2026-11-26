"""ダンジョンモブ 5回目：風のセット（5体）。 python3 tools/dmob_wind_gen.py
  whirl_lynx 旋風の山猫（中ボス）  gale_eagle 疾風の鷲（強）  tornado_spirit 竜巻の精（強・半透明）
  wind_puff 風のわたぼうし（弱）   gale_weasel 疾風のイタチ（弱）
"""
import os, sys
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from geo_tools import Model, hexc
from dmob_fire_gen import B, L, glow_all

MINT = ("#8af0c8", "#e0fff2", "#3ab08a")


def eyes(m, rect, xs, y, color, w=1, h=1, glow=True):
    fx, fy, fw, fh = rect
    for x in xs:
        for d in range(w):
            for e in range(h):
                m.put(fx + x + d, fy + y + e, hexc(color), glow=glow)


def stripes(m, rect, color, every=3):
    fx, fy, fw, fh = rect
    for k in range(0, fw, every):
        for j in range(fh):
            if (j + k) % 4 < 2: m.put(fx + k, fy + j, hexc(color))


# ====================================================================== 旋風の山猫
def whirl_lynx():
    m = Model("whirl_lynx", 128, 128, seed=51)
    m.bone("root", [0, 0, 0])
    m.bone("body", [0, 12, 0], parent="root", cubes=[
        B([-4, 9, -7], [8, 8, 15], "fur", "torso"),
        B([-5, 10, -9], [10, 8, 3], "fur_light", "chest"),
    ])
    m.bone("head", [0, 15, -8], parent="body", cubes=[
        B([-4, 12, -14], [8, 7, 6], "fur", "head"),
        B([-2, 12, -16], [4, 3, 2], "fur_light", "muzzle"),
        B([-6, 12, -12], [2, 5, 3], "fur_light", "ruff"), B([4, 12, -12], [2, 5, 3], "fur_light", "ruff_l"),
        B([-4, 19, -11], [2, 3, 1], "fur", "ear"), B([2, 19, -11], [2, 3, 1], "fur", "ear_l"),
        B([-4, 22, -11], [1, 2, 1], "tuft", "tuft"), B([3, 22, -11], [1, 2, 1], "tuft", "tuft_l"),
    ])
    LEG_F = [B([-4, 0, -6], [3, 10, 3], "fur", "leg"), B([-4, 0, -7], [3, 2, 1], "fur_light", "paw")]
    LEG_B = [B([-4, 0, 4], [3, 10, 3], "fur", "leg"), B([-4, 0, 3], [3, 2, 1], "fur_light", "paw")]
    m.bone("front_right_leg", [-2.5, 10, -4.5], parent="root", cubes=LEG_F)
    m.bone("front_left_leg", [2.5, 10, -4.5], parent="root", cubes=L(LEG_F))
    m.bone("back_right_leg", [-2.5, 10, 5.5], parent="root", cubes=LEG_B)
    m.bone("back_left_leg", [2.5, 10, 5.5], parent="root", cubes=L(LEG_B))
    m.bone("tail", [0, 16, 8], parent="body", rotation=[-40, 0, 0], cubes=[
        B([-1, 15, 8], [2, 2, 8], "fur", "tail"), B([-1.5, 14.5, 15], [3, 3, 3], "wind", "tail_tip")])
    m.bone("wind", [0, 13, 0], parent="root", cubes=[
        B([-7, 14, -6], [1, 1, 10], "wind", "ribbon1", [0, 20, 0]), B([6, 11, -3], [1, 1, 10], "wind", "ribbon2", [0, -20, 0]),
        B([-3, 20, 2], [8, 1, 1], "wind", "ribbon3", [0, 0, 15])])
    pal = {"fur": ("#a8c4b4", "#cce4d6", "#5a7a6a"), "fur_light": ("#e4f2ea", "#ffffff", "#a8c0b2"),
           "tuft": ("#2a3a32", "#44564c", "#141e18"), "wind": MINT}

    def det(m, mat, tag, face, rect):
        fx, fy, fw, fh = rect
        if mat == "wind": glow_all(m, rect)
        if tag in ("torso", "leg", "tail") and face in ("up", "east", "west"): stripes(m, rect, "#5a7a6a")
        if tag == "head" and face == "north":
            eyes(m, rect, (1, 5), 2, "#60ffb0", 2)
            for x in (1, 6): m.put(fx + x, fy + 3, hexc("#1a2a22"))
    m.paint(pal, det)
    m.save()


# ====================================================================== 疾風の鷲
def gale_eagle():
    m = Model("gale_eagle", 128, 64, seed=52)
    m.bone("root", [0, 0, 0])
    m.bone("body", [0, 10, 0], parent="root", cubes=[
        B([-3, 7, -4], [6, 6, 10], "feather", "torso"),
        B([-2, 9, 6], [4, 1, 6], "white", "tail"),
        B([-2, 4, -1], [1, 3, 1], "beak", "leg"), B([1, 4, -1], [1, 3, 1], "beak", "leg_l"),
    ])
    m.bone("head", [0, 12, -4], parent="body", cubes=[
        B([-2, 11, -8], [4, 5, 4], "white", "head"),
        B([-1, 12, -10], [2, 2, 2], "beak", "beak"),
    ])
    WING = [B([-14, 11, -3], [11, 1, 8], "feather", "wing", [0, 0, 0], None),
            B([-18, 11, -2], [4, 1, 6], "wind", "wing_tip")]
    m.bone("right_wing", [-3, 12, 0], parent="body", cubes=WING)
    m.bone("left_wing", [3, 12, 0], parent="body", cubes=L(WING))
    pal = {"feather": ("#6a4a32", "#8a6a4a", "#3a2618"), "white": ("#f0ece4", "#ffffff", "#b8b0a4"),
           "beak": ("#e8b830", "#ffe070", "#a07810"), "wind": MINT}

    def det(m, mat, tag, face, rect):
        fx, fy, fw, fh = rect
        if mat == "wind": glow_all(m, rect)
        if tag == "wing" and face in ("up", "down"):
            for k in range(0, fw, 2):
                for j in range(fh - 2, fh): m.put(fx + k, fy + j, hexc("#3a2618"))
        if tag == "head" and face == "north": eyes(m, rect, (0, 3), 1, "#ffb020")
    m.paint(pal, det)
    m.save()


# ====================================================================== 竜巻の精
def tornado_spirit():
    m = Model("tornado_spirit", 128, 128, seed=53)
    m.bone("root", [0, 0, 0])
    m.bone("tornado", [0, 6, 0], parent="root", cubes=[
        B([-2, 0, -2], [4, 3, 4], "storm", "t1"), B([-3, 3, -3], [6, 3, 6], "storm", "t2"),
        B([-4, 6, -4], [8, 3, 8], "storm", "t3"), B([-5, 9, -5], [10, 3, 10], "storm", "t4")])
    m.bone("body", [0, 12, 0], parent="root", cubes=[
        B([-3, 12, -2], [6, 8, 4], "spirit", "torso"),
    ])
    m.bone("head", [0, 20, 0], parent="body", cubes=[
        B([-3, 20, -3], [6, 6, 6], "spirit", "head"),
        B([-3, 24, -3], [6, 2, 6], "hair", "hair_top"), B([-3, 20, 3], [6, 6, 2], "hair", "hair_back"),
        B([-2, 22, 5], [4, 2, 5], "hair", "hair_flow", [20, 0, 0])])
    ARM = [B([-5, 12, -1], [2, 8, 2], "spirit", "arm"), B([-6, 11, -2], [3, 2, 3], "storm", "cuff")]
    m.bone("right_arm", [-4, 19, 0], parent="body", cubes=ARM)
    m.bone("left_arm", [4, 19, 0], parent="body", cubes=L(ARM))
    pal = {"storm": ("#a8f0d8", "#e8fff6", "#5ac0a0"), "spirit": ("#7ad8b8", "#b4f4dc", "#3a987a"),
           "hair": ("#d8fff0", "#ffffff", "#8ad0b8")}

    def det(m, mat, tag, face, rect):
        fx, fy, fw, fh = rect
        a = 140 if mat == "storm" else 210
        for j in range(fh):
            for k in range(fw):
                c = m.tex[fy + j, fx + k]
                if c[3]:
                    m.tex[fy + j, fx + k] = (int(c[0]), int(c[1]), int(c[2]), a)
                    m.glow[fy + j, fx + k] = (int(c[0]), int(c[1]), int(c[2]), 110 if mat == "storm" else 60)
        if mat == "storm" and face in ("north", "south", "east", "west"):
            for k in range(fw):                                      # うずの線
                m.put(fx + k, fy + (k // 2) % fh, hexc("#ffffff"), a=200, glow=True)
        if tag == "head" and face == "north":
            for x in (1, 4):
                m.put(fx + x, fy + 3, hexc("#ffffff"), a=255, glow=True)
    m.paint(pal, det)
    m.save()


# ====================================================================== 風のわたぼうし
def wind_puff():
    m = Model("wind_puff", 64, 64, seed=54)
    m.bone("root", [0, 0, 0])
    m.bone("body", [0, 6, 0], parent="root", cubes=[
        B([-4, 2, -4], [8, 8, 8], "fluff", "ball"),
        B([-5, 4, -2], [1, 4, 4], "fluff", "tuft_side"), B([4, 4, -2], [1, 4, 4], "fluff", "tuft_side_l"),
        B([-2, 10, -2], [4, 1, 4], "fluff", "tuft_top"),
        B([0, 11, 0], [1, 2, 1], "stem", "stem"), B([0, 13, -1], [2, 1, 2], "leaf", "leaf"),
    ])
    pal = {"fluff": ("#f4fbf8", "#ffffff", "#c8dcd4"), "stem": ("#5a8a4a", "#7aaa62", "#3a5a2a"), "leaf": ("#7ad070", "#a8f090", "#4a9040")}

    def det(m, mat, tag, face, rect):
        fx, fy, fw, fh = rect
        if mat == "fluff":
            for _ in range(fw * fh // 5): m.put(fx + m.rng.randrange(fw), fy + m.rng.randrange(fh), hexc("#e0ece8"))
        if tag == "ball" and face == "north":
            eyes(m, rect, (2, 5), 3, "#2a3a34", 1, 2, glow=False)
            m.put(fx + 3, fy + 6, hexc("#e08090")); m.put(fx + 4, fy + 6, hexc("#e08090"))
            m.put(fx + 1, fy + 5, hexc("#f0b0b8")); m.put(fx + 6, fy + 5, hexc("#f0b0b8"))
    m.paint(pal, det)
    m.save()


# ====================================================================== 疾風のイタチ
def gale_weasel():
    m = Model("gale_weasel", 64, 64, seed=55)
    m.bone("root", [0, 0, 0])
    m.bone("body", [0, 4, 0], parent="root", cubes=[
        B([-2, 2, -5], [4, 4, 10], "fur", "torso"),
        B([-2, 2, -5], [4, 1, 10], "fur_light", "belly")])
    m.bone("head", [0, 5, -5], parent="body", cubes=[
        B([-2, 3, -9], [4, 4, 4], "fur", "head"), B([-1, 3, -10], [2, 2, 1], "fur_light", "snout"),
        B([-2, 7, -7], [1, 1, 1], "fur", "ear"), B([1, 7, -7], [1, 1, 1], "fur", "ear_l")])
    LEG = [B([-2, 0, -4], [1, 2, 1], "fur", "leg_f"), B([-2, 0, 3], [1, 2, 1], "fur", "leg_b")]
    m.bone("right_legs", [-1.5, 2, 0], parent="root", cubes=LEG)
    m.bone("left_legs", [1.5, 2, 0], parent="root", cubes=L(LEG))
    m.bone("tail", [0, 5, 5], parent="body", rotation=[-20, 0, 0], cubes=[
        B([-1, 4, 5], [2, 2, 6], "fur", "tail"), B([-0.5, 5, 10], [1, 5, 4], "wind", "sickle")])
    pal = {"fur": ("#a87a4a", "#c89a62", "#6a4a28"), "fur_light": ("#f0e0c8", "#fff4e4", "#c0a888"), "wind": MINT}

    def det(m, mat, tag, face, rect):
        fx, fy, fw, fh = rect
        if mat == "wind": glow_all(m, rect)
        if tag == "head" and face == "north": eyes(m, rect, (0, 3), 1, "#60ffb0")
    m.paint(pal, det)
    m.save()


if __name__ == "__main__":
    for f in (whirl_lynx, gale_eagle, tornado_spirit, wind_puff, gale_weasel):
        f()
