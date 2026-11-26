"""ダンジョンモブ 2回目：氷のセット（6体）。 python3 tools/dmob_ice_gen.py
  frost_maiden 霜の侍女（中ボス）  pack_alpha 群れの長狼（中ボス）
  frost_knight 氷像の騎士（強）    frostfang_bear 氷牙の白熊（強）
  frost_sprite 氷の妖精（弱）      icicle_crawler つらら虫（弱）
"""
import math, os, sys
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from geo_tools import Model, hexc
from dmob_fire_gen import B, L, glow_all

ICE = ("#9ad8f4", "#e4f8ff", "#4a90c0")
ICE_GLOW = ("#bfeaff", "#ffffff", "#6ab8e8")


def ice_face(m, rect, face, glow=False):
    """氷：ななめの白い筋と、ふちの明るさ"""
    fx, fy, fw, fh = rect
    for j in range(fh):
        for k in range(fw):
            if (k + j) % 5 == 0 and m.rng.random() < 0.7:
                m.put(fx + k, fy + j, hexc("#f4fcff"), glow=glow)
            elif glow:
                c = m.tex[fy + j, fx + k]
                m.glow[fy + j, fx + k] = (int(c[0]), int(c[1]), int(c[2]), 255)


def eyes(m, rect, xs, y, color, w=1):
    fx, fy, fw, fh = rect
    for x in xs:
        for d in range(w):
            m.put(fx + x + d, fy + y, hexc(color), glow=True)


# ====================================================================== 霜の侍女
def frost_maiden():
    m = Model("frost_maiden", 128, 128, seed=21)
    m.bone("root", [0, 0, 0])
    m.bone("body", [0, 14, 0], parent="root", cubes=[
        B([-7, 2, -6], [14, 4, 12], "dress", "hem"),
        B([-6, 6, -5], [12, 4, 10], "dress", "skirt2"),
        B([-5, 10, -4], [10, 4, 8], "dress", "skirt1"),
        B([-7, 2, -6], [14, 1, 12], "trim", "hem_trim"),
        B([-3, 14, -2], [6, 8, 4], "dress", "torso"),
        B([-3, 18, -3], [6, 3, 1], "trim", "apron"),
        B([-4, 21, -2], [8, 1, 4], "trim", "collar"),
    ])
    m.bone("head", [0, 22, 0], parent="body", cubes=[
        B([-4, 22, -4], [8, 8, 8], "skin", "head"),
        B([-4, 28, -4], [8, 3, 8], "hair", "hair_top"),
        B([-4, 17, 2], [8, 11, 3], "hair", "hair_back"),
        B([-5, 23, -3], [1, 6, 6], "hair", "hair_side"), B([4, 23, -3], [1, 6, 6], "hair", "hair_side_l"),
        B([-3, 31, -2], [1, 2, 1], "crystal", "tiara1"), B([-1, 31, -3], [2, 4, 1], "crystal", "tiara2"),
        B([2, 31, -2], [1, 2, 1], "crystal", "tiara3"),
    ])
    ARM = [B([-5, 13, -1], [2, 9, 2], "dress", "sleeve"), B([-6, 12, -2], [3, 2, 3], "trim", "cuff")]
    m.bone("right_arm", [-4, 21, 0], parent="body", cubes=ARM)
    m.bone("left_arm", [4, 21, 0], parent="body", cubes=L(ARM))
    m.bone("crystals", [0, 20, 0], parent="root", cubes=[
        B([-10, 20, -1], [2, 4, 2], "crystal", "orbit1", [0, 0, 45]), B([8, 22, 2], [2, 4, 2], "crystal", "orbit2", [0, 0, 45]),
        B([-1, 24, 8], [2, 4, 2], "crystal", "orbit3", [0, 0, 45])])
    pal = {"dress": ("#5aa0d8", "#8ac8f0", "#2a6098"), "trim": ("#e8f6ff", "#ffffff", "#a8c8e0"),
           "skin": ("#f2ddd2", "#fff0e8", "#d0b0a4"), "hair": ("#dceeff", "#ffffff", "#9ab8d8"), "crystal": ICE_GLOW}

    def det(m, mat, tag, face, rect):
        fx, fy, fw, fh = rect
        if mat == "crystal": ice_face(m, rect, face, glow=True)
        if mat == "dress" and face in ("north", "south", "east", "west") and tag in ("hem", "skirt2"):
            for k in range(0, fw, 3):                          # 雪の結晶もよう
                m.put(fx + k, fy + fh // 2, hexc("#e8f6ff"))
        if tag == "head" and face == "north":
            for k in range(fw):
                m.put(fx + k, fy, hexc("#dceeff")); m.put(fx + k, fy + 1, hexc("#dceeff"))   # 前髪
            eyes(m, rect, (1, 5), 4, "#40b8ff", 2)
            m.put(fx + 3, fy + 6, hexc("#c08080")); m.put(fx + 4, fy + 6, hexc("#c08080"))
    m.paint(pal, det)
    m.save()


# ====================================================================== 群れの長狼
def pack_alpha():
    m = Model("pack_alpha", 128, 128, seed=22)
    m.bone("root", [0, 0, 0])
    m.bone("body", [0, 14, 0], parent="root", cubes=[
        B([-5, 10, -8], [10, 9, 18], "fur", "torso"),
        B([-6, 11, -12], [12, 11, 7], "fur_light", "mane"),
        B([-6, 14, -13], [12, 2, 1], "iron", "collar"),
        B([-1.5, 18, -6], [3, 5, 3], "ice", "spike1", [-25, 0, 0]), B([-1.5, 18, -1], [3, 6, 3], "ice", "spike2", [-25, 0, 0]),
        B([-1.5, 18, 4], [3, 4, 3], "ice", "spike3", [-25, 0, 0]), B([-4, 19, -10], [2, 4, 2], "ice", "spike4", [-20, 0, -25]),
        B([2, 19, -10], [2, 4, 2], "ice", "spike5", [-20, 0, 25]),
    ])
    m.bone("chain", [0, 14, -12], parent="body", cubes=[
        B([-3, 7, -14], [2, 7, 2], "iron", "chain1"), B([2, 6, -14], [2, 8, 2], "iron", "chain2"),
        B([-1, 9, -14], [2, 5, 2], "iron", "chain3")])
    m.bone("head", [0, 17, -12], parent="body", cubes=[
        B([-4, 13, -19], [8, 8, 7], "fur", "head"),
        B([-2, 13, -23], [4, 4, 4], "fur_light", "snout"),
        B([-1, 16, -24], [2, 1, 1], "nose", "nose"),
        B([-4, 21, -15], [2, 3, 1], "fur", "ear"), B([2, 21, -15], [2, 3, 1], "fur", "ear_l"),
    ])
    LEG_F = [B([-5, 0, -7], [4, 11, 4], "fur", "leg"), B([-5, 0, -8], [4, 2, 1], "claw", "claw")]
    LEG_B = [B([-5, 0, 5], [4, 11, 4], "fur", "leg"), B([-5, 0, 4], [4, 2, 1], "claw", "claw")]
    m.bone("front_right_leg", [-3.5, 11, -5.5], parent="root", cubes=LEG_F)
    m.bone("front_left_leg", [3.5, 11, -5.5], parent="root", cubes=L(LEG_F))
    m.bone("back_right_leg", [-3.5, 11, 6.5], parent="root", cubes=LEG_B)
    m.bone("back_left_leg", [3.5, 11, 6.5], parent="root", cubes=L(LEG_B))
    m.bone("tail", [0, 17, 10], parent="body", rotation=[-35, 0, 0], cubes=[B([-1.5, 15, 10], [3, 3, 10], "fur_light", "tail")])
    pal = {"fur": ("#9aa2ac", "#c4ccd6", "#5a626c"), "fur_light": ("#d8dee6", "#f4f8fc", "#9aa4b0"),
           "iron": ("#4a4a52", "#7a7a84", "#2a2a30"), "nose": ("#1a1a1e", "#2a2a2e", "#0a0a0c"),
           "claw": ("#2a2a2e", "#4a4a4e", "#141418"), "ice": ICE_GLOW}

    def det(m, mat, tag, face, rect):
        fx, fy, fw, fh = rect
        if mat == "ice": ice_face(m, rect, face, glow=True)
        if mat in ("fur", "fur_light") and face != "down":
            for _ in range(fw * fh // 6):                      # 毛並み
                m.put(fx + m.rng.randrange(fw), fy + m.rng.randrange(fh), hexc(pal[mat][0]) * 0.85)
        if tag == "head" and face == "north":
            eyes(m, rect, (1, 6), 2, "#7ad8ff")
        if tag == "snout" and face == "north":
            for k in range(fw):
                if k % 2 == 0: m.put(fx + k, fy + fh - 1, hexc("#ffffff"))   # 牙
    m.paint(pal, det)
    m.save()


# ====================================================================== 氷像の騎士
def frost_knight():
    m = Model("frost_knight", 128, 128, seed=23)
    m.bone("root", [0, 0, 0])
    LEG = [B([-5, 0, -2], [5, 13, 5], "ice", "leg"), B([-5, 0, -3], [5, 3, 6], "ice_dark", "boot")]
    m.bone("right_leg", [-2.5, 13, 0], parent="root", cubes=LEG)
    m.bone("left_leg", [2.5, 13, 0], parent="root", cubes=L(LEG))
    m.bone("body", [0, 13, 0], parent="root", cubes=[
        B([-6, 13, -3], [12, 13, 6], "ice", "torso"),
        B([-6, 13, -4], [12, 3, 8], "ice_dark", "belt"),
        B([-2, 18, -4], [4, 6, 1], "crystal", "heart"),
    ])
    m.bone("head", [0, 26, 0], parent="body", cubes=[
        B([-4, 26, -4], [8, 9, 8], "ice", "helm"),
        B([-1, 35, -3], [2, 3, 7], "crystal", "crest"),
        B([-5, 30, -5], [10, 1, 10], "ice_dark", "helm_rim"),
    ])
    ARM = [B([-10, 13, -2], [4, 12, 5], "ice", "arm"), B([-11, 22, -3], [6, 5, 7], "ice_dark", "pauldron"),
           B([-11, 27, -1], [2, 4, 2], "crystal", "shoulder_crystal", [0, 0, 20]), B([-10, 11, -3], [4, 3, 6], "ice_dark", "gauntlet")]
    m.bone("right_arm", [-8, 25, 0], parent="body", cubes=ARM)
    m.bone("sword", [-8, 12, 0], parent="right_arm", cubes=[
        B([-9, 11, -3], [2, 2, 4], "ice_dark", "grip"), B([-10, 10, -5], [4, 4, 1], "ice_dark", "guard"),
        B([-9, 11, -22], [2, 3, 17], "crystal", "blade")])
    m.bone("left_arm", [8, 25, 0], parent="body", cubes=L(ARM))
    m.bone("shield", [11, 16, 0], parent="left_arm", cubes=[
        B([11, 6, -8], [2, 18, 14], "crystal", "shield"), B([13, 12, -3], [1, 6, 4], "ice_dark", "shield_boss")])
    pal = {"ice": ICE, "ice_dark": ("#5a9ac4", "#8ac0e4", "#2a5a84"), "crystal": ICE_GLOW}

    def det(m, mat, tag, face, rect):
        fx, fy, fw, fh = rect
        if mat in ("ice", "ice_dark"): ice_face(m, rect, face)
        if mat == "crystal": ice_face(m, rect, face, glow=True)
        if tag == "helm" and face == "north":
            for k in range(1, fw - 1): m.put(fx + k, fy + 3, hexc("#1a3a5a"))
            eyes(m, rect, (2, 5), 3, "#a8f0ff")
    m.paint(pal, det)
    m.save()


# ====================================================================== 氷牙の白熊
def frostfang_bear():
    m = Model("frostfang_bear", 128, 128, seed=24)
    m.bone("root", [0, 0, 0])
    m.bone("body", [0, 13, 0], parent="root", cubes=[
        B([-7, 8, -10], [14, 12, 21], "fur", "torso"),
        B([-6, 10, -12], [12, 10, 3], "fur", "chest"),
    ] + [B([-1 + (i % 2) * 2 - 1, 19, -7 + i * 4], [2, 4 + (i % 3), 2], "ice", f"spike{i}", [-25, (i % 2) * 30 - 15, 0]) for i in range(5)])
    m.bone("head", [0, 16, -12], parent="body", cubes=[
        B([-4, 11, -18], [8, 8, 7], "fur", "head"),
        B([-2, 11, -21], [4, 4, 3], "fur", "snout"),
        B([-1, 14, -22], [2, 1, 1], "nose", "nose"),
        B([-4, 19, -14], [2, 2, 1], "fur", "ear"), B([2, 19, -14], [2, 2, 1], "fur", "ear_l"),
        B([-2, 10, -21], [1, 2, 1], "ice", "fang"), B([1, 10, -21], [1, 2, 1], "ice", "fang_l"),
    ])
    LEG_F = [B([-7, 0, -9], [5, 10, 5], "fur", "leg"), B([-7, 0, -10], [5, 2, 1], "claw", "claw")]
    LEG_B = [B([-7, 0, 5], [5, 10, 5], "fur", "leg"), B([-7, 0, 4], [5, 2, 1], "claw", "claw")]
    m.bone("front_right_leg", [-4.5, 10, -6.5], parent="root", cubes=LEG_F)
    m.bone("front_left_leg", [4.5, 10, -6.5], parent="root", cubes=L(LEG_F))
    m.bone("back_right_leg", [-4.5, 10, 7.5], parent="root", cubes=LEG_B)
    m.bone("back_left_leg", [4.5, 10, 7.5], parent="root", cubes=L(LEG_B))
    pal = {"fur": ("#e8eef2", "#ffffff", "#b4c0c8"), "nose": ("#1a1a1e", "#2a2a2e", "#0a0a0c"),
           "claw": ("#3a3a40", "#5a5a60", "#1a1a1e"), "ice": ICE_GLOW}

    def det(m, mat, tag, face, rect):
        fx, fy, fw, fh = rect
        if mat == "ice": ice_face(m, rect, face, glow=True)
        if mat == "fur" and face != "down":
            for _ in range(fw * fh // 6):
                m.put(fx + m.rng.randrange(fw), fy + m.rng.randrange(fh), hexc(pal[mat][0]) * 0.9)
        if tag == "head" and face == "north":
            eyes(m, rect, (1, 6), 3, "#2a2a2e")
            m.put(fx + 1, fy + 2, hexc("#7ad8ff"), glow=True); m.put(fx + 6, fy + 2, hexc("#7ad8ff"), glow=True)
    m.paint(pal, det)
    m.save()


# ====================================================================== 氷の妖精
def frost_sprite():
    m = Model("frost_sprite", 64, 64, seed=25)
    m.bone("root", [0, 0, 0])
    m.bone("body", [0, 10, 0], parent="root", cubes=[
        B([-2, 8, -1], [4, 5, 3], "dress", "torso"),
        B([-3, 5, -2], [6, 3, 5], "dress", "skirt"),
        B([-1, 2, -1], [1, 3, 1], "skin", "leg"), B([0, 2, -1], [1, 3, 1], "skin", "leg_l"),
    ])
    m.bone("head", [0, 13, 0], parent="body", cubes=[
        B([-3, 13, -3], [6, 6, 6], "skin", "head"),
        B([-3, 17, -3], [6, 3, 6], "hair", "hair"), B([-3, 13, 2], [6, 5, 2], "hair", "hair_back"),
        B([-1, 20, -1], [2, 2, 2], "crystal", "crown"),
    ])
    WING = [B([-8, 9, 1], [6, 7, 1], "wing", "wing_up", [0, 0, -15], [-2, 11, 1]),
            B([-6, 5, 1], [4, 5, 1], "wing", "wing_low", [0, 0, 20], [-2, 9, 1])]
    m.bone("right_wing", [-2, 11, 1], parent="body", cubes=WING)
    m.bone("left_wing", [2, 11, 1], parent="body", cubes=L(WING))
    m.bone("snowball", [-3, 9, -2], parent="body", cubes=[B([-5, 7, -4], [3, 3, 3], "snow", "snowball")])
    pal = {"dress": ("#f4fbff", "#ffffff", "#b8d4e8"), "skin": ("#f4e0d8", "#fff4ee", "#d4b4a8"),
           "hair": ("#9ad8f4", "#d4f0ff", "#5a98c4"), "crystal": ICE_GLOW, "wing": ("#bfeaff", "#ffffff", "#7ac0e8"),
           "snow": ("#ffffff", "#ffffff", "#d0e4f0")}

    def det(m, mat, tag, face, rect):
        fx, fy, fw, fh = rect
        if mat in ("crystal", "wing"): ice_face(m, rect, face, glow=True)
        if tag == "head" and face == "north":
            for k in range(fw): m.put(fx + k, fy, hexc("#9ad8f4"))
            eyes(m, rect, (1, 4), 3, "#2a80d0")
    m.paint(pal, det)
    m.save()


# ====================================================================== つらら虫
def icicle_crawler():
    m = Model("icicle_crawler", 64, 64, seed=26)
    m.bone("root", [0, 0, 0])
    spikes = []
    for i, (z, h) in enumerate(((-4, 5), (-1, 7), (2, 6), (5, 4))):
        spikes.append(B([-3, 5, z], [2, h, 2], "ice", f"icicle{i}a", [15, 0, -12]))
        spikes.append(B([1, 5, z], [2, h - 1, 2], "ice", f"icicle{i}b", [15, 0, 12]))
    m.bone("body", [0, 3, 0], parent="root", cubes=[
        B([-4, 1, -6], [8, 4, 13], "shell", "shell"),
        B([-3, 1, -9], [6, 3, 3], "shell_dark", "head"),
        B([-2, 4, -9], [1, 1, 3], "shell_dark", "antenna", [-30, 15, 0]), B([1, 4, -9], [1, 1, 3], "shell_dark", "antenna_l", [-30, -15, 0]),
    ] + spikes)
    LEG = [B([-6, 0, -4], [2, 2, 1], "shell_dark", "leg1"), B([-6, 0, 0], [2, 2, 1], "shell_dark", "leg2"),
           B([-6, 0, 4], [2, 2, 1], "shell_dark", "leg3")]
    m.bone("right_legs", [-4, 1, 0], parent="root", cubes=LEG)
    m.bone("left_legs", [4, 1, 0], parent="root", cubes=L(LEG))
    pal = {"shell": ("#4a6a84", "#6a8ca8", "#2a4258"), "shell_dark": ("#2a3a4a", "#4a5a6a", "#141e28"), "ice": ICE_GLOW}

    def det(m, mat, tag, face, rect):
        fx, fy, fw, fh = rect
        if mat == "ice": ice_face(m, rect, face, glow=True)
        if tag == "shell" and face in ("up", "east", "west"):
            for j in range(0, fh, 3):                          # 節の線
                for k in range(fw): m.put(fx + k, fy + j, hexc(pal[mat][0]) * 0.7)
        if tag == "head" and face == "north":
            eyes(m, rect, (1, 4), 1, "#a8f0ff")
    m.paint(pal, det)
    m.save()


if __name__ == "__main__":
    for f in (frost_maiden, pack_alpha, frost_knight, frostfang_bear, frost_sprite, icicle_crawler):
        f()
