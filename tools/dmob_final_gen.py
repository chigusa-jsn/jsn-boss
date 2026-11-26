"""ダンジョンモブ 11回目：残りの中ボス（4体）。 python3 tools/dmob_final_gen.py
  blade_wyvern 刃の飛竜  blackwing_attendant 黒翼の従者  foxfire_maiden 狐火の巫女  treant_warden 樹人の守り手
"""
import os, sys
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from geo_tools import Model, hexc
from dmob_fire_gen import B, L, glow_all


def eyes(m, rect, xs, y, color, w=1, h=1, glow=True):
    fx, fy, fw, fh = rect
    for x in xs:
        for d in range(w):
            for e in range(h):
                m.put(fx + x + d, fy + y + e, hexc(color), glow=glow)


# ====================================================================== 刃の飛竜
def blade_wyvern():
    m = Model("blade_wyvern", 128, 128, seed=111)
    m.bone("root", [0, 0, 0])
    m.bone("body", [0, 12, 0], parent="root", cubes=[
        B([-3, 9, -6], [6, 6, 12], "scale", "torso"),
        B([-2, 9, -6], [4, 1, 12], "belly", "belly"),
        B([-0.5 * 0, 15, -4], [1, 3, 2], "blade", "spine1", [-20, 0, 0]), B([0, 15, -1], [1, 4, 2], "blade", "spine2", [-20, 0, 0]),
        B([0, 15, 2], [1, 3, 2], "blade", "spine3", [-20, 0, 0])])
    m.bone("head", [0, 13, -6], parent="body", cubes=[
        B([-1.5, 11, -10], [3, 3, 5], "scale", "neck", [-20, 0, 0]),
        B([-2, 13, -15], [4, 4, 5], "scale", "head"), B([-1.5, 13, -18], [3, 2, 3], "scale", "snout"),
        B([-2, 17, -12], [1, 1, 4], "blade", "horn", [30, 0, 10]), B([1, 17, -12], [1, 1, 4], "blade", "horn_l", [30, 0, -10])])
    WING = [B([-16, 14, -4], [13, 1, 9], "membrane", "wing", None, None),
            B([-16, 14, -5], [13, 2, 1], "scale", "wing_arm"),
            B([-19, 13, -5], [3, 3, 1], "blade", "wing_claw"),
            B([-14, 13, 4], [2, 1, 3], "blade", "wb1"), B([-10, 13, 4], [2, 1, 3], "blade", "wb2"), B([-6, 13, 4], [2, 1, 3], "blade", "wb3")]
    m.bone("right_wing", [-3, 14, -4], parent="body", cubes=WING)
    m.bone("left_wing", [3, 14, -4], parent="body", cubes=L(WING))
    m.bone("tail", [0, 12, 6], parent="body", cubes=[
        B([-1, 11, 6], [2, 2, 10], "scale", "tail"), B([-0.5 * 0, 11, 16], [1, 2, 4], "blade", "tail_blade")])
    LEG = [B([-3, 4, -1], [2, 6, 2], "scale", "leg"), B([-3, 4, -2], [2, 1, 1], "blade", "talon")]
    m.bone("right_leg", [-2, 10, 0], parent="body", cubes=LEG)
    m.bone("left_leg", [2, 10, 0], parent="body", cubes=L(LEG))
    pal = {"scale": ("#3a3e4a", "#565c6a", "#1e2028"), "belly": ("#6a6e7a", "#8a8e9a", "#3a3e48"),
           "membrane": ("#2a2232", "#3e3448", "#140e1a"), "blade": ("#c0c4cc", "#ffffff", "#6a6e78")}

    def det(m, mat, tag, face, rect):
        fx, fy, fw, fh = rect
        if mat == "blade" and face in ("east", "west", "up"):
            for k in range(fw): m.put(fx + k, fy, hexc("#e080ff"), glow=True)
        if mat == "membrane" and face in ("up", "down"):
            for j in range(0, fh, 3):
                for k in range(fw): m.put(fx + k, fy + j, hexc("#4a3a58"))
        if tag == "head" and face in ("east", "west"): eyes(m, rect, (1,), 1, "#e060ff", 2)
    m.paint(pal, det)
    m.save()


# ====================================================================== 黒翼の従者
def blackwing_attendant():
    m = Model("blackwing_attendant", 128, 128, seed=112)
    m.bone("root", [0, 0, 0])
    LEG = [B([-3, 0, -1], [3, 12, 3], "armor", "leg"), B([-3, 5, -2], [3, 2, 1], "gold", "knee")]
    m.bone("right_leg", [-1.5, 12, 0], parent="root", cubes=LEG)
    m.bone("left_leg", [1.5, 12, 0], parent="root", cubes=L(LEG))
    m.bone("body", [0, 12, 0], parent="root", cubes=[
        B([-3, 12, -2], [6, 11, 4], "armor", "torso"),
        B([-4, 9, -3], [8, 5, 6], "cloth", "skirt"),
        B([-3, 19, -3], [6, 3, 1], "gold", "breast"),
        B([-4, 22, -3], [8, 2, 5], "armor", "pauldrons")])
    m.bone("head", [0, 24, 0], parent="body", cubes=[
        B([-3, 24, -3], [6, 7, 6], "skin", "head"),
        B([-3, 29, -3], [6, 2, 6], "hair", "hair_top"), B([-3, 20, 2], [6, 10, 2], "hair", "hair_back"),
        B([-4, 27, -3], [1, 2, 5], "gold", "wing_helm"), B([3, 27, -3], [1, 2, 5], "gold", "wing_helm_l")])
    ARM = [B([-5, 13, -1], [2, 10, 3], "armor", "arm")]
    m.bone("right_arm", [-4, 22, 0], parent="body", cubes=ARM)
    m.bone("spear", [-4, 13, 0], parent="right_arm", cubes=[
        B([-4.5, 13, -14], [1, 1, 26], "dark", "shaft"), B([-5, 12.5, -18], [2, 2, 4], "gold", "spearhead")])
    m.bone("left_arm", [4, 22, 0], parent="body", cubes=L(ARM))
    WING = [B([-14, 16, 2], [11, 12, 1], "feather", "wing", [0, 25, 0], [-3, 22, 2]),
            B([-18, 10, 2], [6, 10, 1], "feather", "wing_low", [0, 25, -15], [-3, 22, 2])]
    m.bone("right_wing", [-3, 22, 2], parent="body", cubes=WING)
    m.bone("left_wing", [3, 22, 2], parent="body", cubes=L(WING))
    pal = {"armor": ("#2a2630", "#423c4a", "#141218"), "gold": ("#d4a84a", "#ffe08a", "#8a6420"),
           "cloth": ("#3a1a2a", "#5a2a40", "#200a14"), "skin": ("#e8d4c8", "#fff0e8", "#b8a094"),
           "hair": ("#d8d8e4", "#ffffff", "#9898a8"), "dark": ("#1a1820", "#2a2834", "#0a0a10"),
           "feather": ("#1a1620", "#2e2836", "#08060a")}

    def det(m, mat, tag, face, rect):
        fx, fy, fw, fh = rect
        if mat == "feather" and face in ("north", "south"):
            for j in range(0, fh, 2):
                for k in range(fw): m.put(fx + k, fy + j, hexc("#2e2836"))
            for k in range(fw): m.put(fx + k, fy + fh - 1, hexc("#a060ff"), glow=True)
        if tag == "head" and face == "north":
            for k in range(fw): m.put(fx + k, fy, hexc("#d8d8e4"))
            eyes(m, rect, (1, 4), 3, "#ff4a6a")
    m.paint(pal, det)
    m.save()


# ====================================================================== 狐火の巫女
def foxfire_maiden():
    m = Model("foxfire_maiden", 128, 128, seed=113)
    m.bone("root", [0, 0, 0])
    m.bone("body", [0, 12, 0], parent="root", cubes=[
        B([-4, 0, -3], [8, 12, 6], "hakama", "hakama"),
        B([-3, 12, -2], [6, 9, 4], "kimono", "torso"),
        B([-4, 12, -3], [8, 2, 6], "hakama", "obi"),
        B([-1, 18, -3], [2, 3, 1], "hakama", "collar")])
    m.bone("tails", [0, 10, 3], parent="body", cubes=[
        B([-3, 8, 3], [3, 3, 9], "fur", "tail1", [-30, -20, 0], [0, 10, 3]), B([0, 8, 3], [3, 3, 9], "fur", "tail2", [-30, 20, 0], [0, 10, 3]),
        B([-2.5, 8.5, 11], [2, 2, 2], "fire", "tail1_tip", [-30, -20, 0], [0, 10, 3]), B([0.5, 8.5, 11], [2, 2, 2], "fire", "tail2_tip", [-30, 20, 0], [0, 10, 3])])
    m.bone("head", [0, 21, 0], parent="body", cubes=[
        B([-3, 21, -3], [6, 6, 6], "skin", "head"),
        B([-3, 25, -3], [6, 2, 6], "hair", "hair_top"), B([-3, 16, 2], [6, 10, 2], "hair", "hair_back"),
        B([-3, 27, -1], [2, 3, 1], "fur", "ear"), B([1, 27, -1], [2, 3, 1], "fur", "ear_l"),
        B([-3, 22, -4], [6, 4, 1], "mask", "mask")])
    ARM = [B([-6, 13, -2], [3, 8, 4], "kimono", "sleeve"), B([-6, 11, -2], [3, 3, 4], "kimono", "sleeve_drape")]
    m.bone("right_arm", [-4, 20, 0], parent="body", cubes=ARM)
    m.bone("gohei", [-5, 12, 0], parent="right_arm", cubes=[
        B([-5, 11, -8], [1, 1, 8], "wood", "stick"), B([-5.5, 10, -9], [2, 3, 1], "paper", "paper")])
    m.bone("left_arm", [4, 20, 0], parent="body", cubes=L(ARM))
    m.bone("flames", [0, 18, 0], parent="root", cubes=[
        B([-9, 22, 0], [2, 2, 2], "fire", "f1"), B([7, 24, 1], [2, 2, 2], "fire", "f2"), B([-1, 28, 6], [2, 2, 2], "fire", "f3")])
    pal = {"hakama": ("#c02a2a", "#e04a40", "#7a1414"), "kimono": ("#f4f0ec", "#ffffff", "#c0bcb8"),
           "skin": ("#f2ddd2", "#fff0e8", "#d0b0a4"), "hair": ("#f4f4f8", "#ffffff", "#b8b8c4"),
           "fur": ("#f4ecdc", "#ffffff", "#c0b8a4"), "mask": ("#ffffff", "#ffffff", "#d0d0d0"),
           "wood": ("#8a6a44", "#aa8a5a", "#5a4228"), "paper": ("#ffffff", "#ffffff", "#e0e0e0"),
           "fire": ("#7ab8ff", "#e0f0ff", "#3a70d0")}

    def det(m, mat, tag, face, rect):
        fx, fy, fw, fh = rect
        if mat == "fire": glow_all(m, rect)
        if tag == "mask" and face == "north":                          # 狐のお面（赤い隈取り）
            for (x, y) in ((1, 1), (4, 1), (0, 2), (5, 2)): m.put(fx + x, fy + y, hexc("#d02a2a"))
            m.put(fx + 1, fy + 2, hexc("#1a1010")); m.put(fx + 4, fy + 2, hexc("#1a1010"))
            m.put(fx + 2, fy + 3, hexc("#d02a2a")); m.put(fx + 3, fy + 3, hexc("#d02a2a"))
    m.paint(pal, det)
    m.save()


# ====================================================================== 樹人の守り手
def treant_warden():
    m = Model("treant_warden", 128, 128, seed=114)
    m.bone("root", [0, 0, 0])
    LEG = [B([-5, 0, -3], [5, 12, 6], "bark", "leg"), B([-6, 0, -4], [7, 2, 8], "bark", "root_foot")]
    m.bone("right_leg", [-3, 12, 0], parent="root", cubes=LEG)
    m.bone("left_leg", [3, 12, 0], parent="root", cubes=L(LEG))
    m.bone("body", [0, 12, 0], parent="root", cubes=[
        B([-6, 12, -4], [12, 14, 8], "bark", "trunk"),
        B([-7, 24, -5], [14, 8, 10], "leaves", "crown"), B([-5, 32, -4], [10, 3, 8], "leaves", "crown_top")])
    m.bone("face", [0, 20, -4], parent="body", cubes=[B([-4, 17, -5], [8, 6, 1], "bark_dark", "face")])
    ARM = [B([-11, 8, -2], [4, 16, 4], "bark", "arm"), B([-13, 5, -3], [3, 5, 2], "bark", "twig1", [0, 0, 20]),
           B([-9, 4, -3], [2, 5, 2], "bark", "twig2", [0, 0, -15])]
    m.bone("right_arm", [-8, 24, 0], parent="body", cubes=ARM)
    m.bone("left_arm", [8, 24, 0], parent="body", cubes=L(ARM))
    m.bone("sapling", [-5, 32, 0], parent="body", cubes=[
        B([-7, 32, -1], [1, 4, 1], "bark", "sap_stem"), B([-8, 35, -2], [3, 2, 3], "leaves_light", "sap_leaves")])
    pal = {"bark": ("#6a4a2e", "#8a6844", "#3a2614"), "bark_dark": ("#3a2614", "#5a3e26", "#1e1208"),
           "leaves": ("#3a7a2a", "#5a9a42", "#1e4a14"), "leaves_light": ("#7ad060", "#a8f090", "#4a9a3a")}

    def det(m, mat, tag, face, rect):
        fx, fy, fw, fh = rect
        if mat == "bark" and face in ("north", "south", "east", "west"):
            for k in range(0, fw, 2):
                for j in range(fh):
                    if (j + k * 3) % 5 == 0: m.put(fx + k, fy + j, hexc("#3a2614"))
        if mat == "leaves":
            for _ in range(fw * fh // 6): m.put(fx + m.rng.randrange(fw), fy + m.rng.randrange(fh), hexc("#2a5a1e"))
            for _ in range(2): m.put(fx + m.rng.randrange(fw), fy + m.rng.randrange(fh), hexc("#ff6a8a"))   # 小さな花
        if tag == "face" and face == "north":
            eyes(m, rect, (1, 5), 1, "#8aff60", 2)
            for k in range(2, 6): m.put(fx + k, fy + 4, hexc("#0e0804"))
    m.paint(pal, det)
    m.save()


if __name__ == "__main__":
    for f in (blade_wyvern, blackwing_attendant, foxfire_maiden, treant_warden):
        f()
