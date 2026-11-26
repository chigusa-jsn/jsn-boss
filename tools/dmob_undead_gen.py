"""ダンジョンモブ 3回目：アンデッドのセット（5体）。 python3 tools/dmob_undead_gen.py
  rotting_guard 腐肉の近衛（中ボス）  grave_reaper 墓守の大鎌（強）  bone_juggernaut 骨の重装兵（強）
  crawling_bones 這う骨（弱）          grave_wisp 墓場の魂（弱・半透明）
"""
import os, sys
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from geo_tools import Model, hexc
from dmob_fire_gen import B, L

BONE = ("#d8d0b8", "#f0eadc", "#9a927c")
SOUL = "#7af0a0"


def eyes(m, rect, xs, y, color, w=1, h=1):
    fx, fy, fw, fh = rect
    for x in xs:
        for d in range(w):
            for e in range(h):
                m.put(fx + x + d, fy + y + e, hexc(color), glow=True)


def bone_detail(m, rect, face, pal_dark):
    fx, fy, fw, fh = rect
    for _ in range(max(1, fw * fh // 14)):
        m.put(fx + m.rng.randrange(fw), fy + m.rng.randrange(fh), hexc(pal_dark))


def skull_face(m, rect, glow=SOUL):
    fx, fy, fw, fh = rect
    for x in (1, fw - 3):                                   # 目のくぼみと光
        for d in range(2):
            for e in range(2):
                m.put(fx + x + d, fy + 2 + e, hexc("#1a1612"))
        m.put(fx + x + (1 if x == 1 else 0), fy + 3, hexc(glow), glow=True)
    m.put(fx + fw // 2 - 1, fy + 5, hexc("#2a2420")); m.put(fx + fw // 2, fy + 5, hexc("#2a2420"))   # 鼻
    for k in range(1, fw - 1):                              # 歯
        m.put(fx + k, fy + fh - 2, hexc("#2a2420") if k % 2 else hexc("#f0eadc"))


# ====================================================================== 腐肉の近衛
def rotting_guard():
    m = Model("rotting_guard", 128, 128, seed=31)
    m.bone("root", [0, 0, 0])
    LEG = [B([-4, 0, -2], [4, 12, 4], "skin", "leg"), B([-4, 3, -3], [4, 6, 1], "rust", "greave")]
    m.bone("right_leg", [-2, 12, 0], parent="root", cubes=LEG)
    m.bone("left_leg", [2, 12, 0], parent="root", cubes=L(LEG))
    m.bone("body", [0, 12, 0], parent="root", cubes=[
        B([-5, 12, -3], [10, 12, 6], "skin", "torso"),
        B([-5, 16, -4], [10, 8, 8], "rust", "chest"),
        B([-6, 12, -4], [12, 3, 8], "leather", "belt"),
        B([-5, 9, -4], [10, 4, 1], "cloth", "tabard"),
    ])
    m.bone("head", [0, 24, 0], parent="body", cubes=[
        B([-4, 24, -4], [8, 8, 8], "skin", "head"),
        B([-5, 29, -5], [10, 4, 10], "rust", "helm"),
        B([-1, 33, -1], [2, 2, 2], "rust", "helm_knob"),
    ])
    ARM = [B([-8, 11, -2], [3, 13, 4], "skin", "arm"), B([-9, 20, -3], [5, 5, 6], "rust", "pauldron")]
    m.bone("right_arm", [-6, 23, 0], parent="body", cubes=ARM)
    m.bone("halberd", [-7, 12, 0], parent="right_arm", cubes=[
        B([-7, 0, -1], [1, 34, 1], "wood", "pole"),
        B([-7.5, 27, -6], [2, 6, 5], "steel", "axe"),
        B([-7, 33, -1], [1, 4, 1], "steel", "spike"),
        B([-7.5, 28, 0], [2, 3, 2], "steel", "back_hook")])
    m.bone("left_arm", [6, 23, 0], parent="body", cubes=L(ARM))
    pal = {"skin": ("#6a8a5a", "#8aaa72", "#3a5a32"), "rust": ("#7a4a2a", "#9a6a40", "#4a2a14"),
           "leather": ("#4a3424", "#6a4a34", "#2a1a10"), "cloth": ("#5a1e1e", "#7a2e2a", "#3a0e0e"),
           "wood": ("#5a3e26", "#7a5636", "#3a2414"), "steel": ("#7a7a80", "#a8a8b0", "#4a4a50")}

    def det(m, mat, tag, face, rect):
        fx, fy, fw, fh = rect
        if mat == "rust": bone_detail(m, rect, face, "#c47a3a")
        if mat == "skin":
            for _ in range(fw * fh // 10):                      # 腐った斑点
                m.put(fx + m.rng.randrange(fw), fy + m.rng.randrange(fh), hexc("#4a5a3a"))
        if tag == "head" and face == "north":
            eyes(m, rect, (1, 5), 3, "#e0ff60", 2)
            for k in range(2, 6): m.put(fx + k, fy + 6, hexc("#2a1a14"))
        if tag == "axe" and face in ("east", "west"):
            for j in range(fh): m.put(fx, fy + j, hexc("#d0d0d8"))
    m.paint(pal, det)
    m.save()


# ====================================================================== 墓守の大鎌
def grave_reaper():
    m = Model("grave_reaper", 128, 128, seed=32)
    m.bone("root", [0, 0, 0])
    m.bone("body", [0, 12, 0], parent="root", cubes=[
        B([-5, 0, -4], [10, 10, 8], "robe", "robe_low"),
        B([-4, 10, -3], [8, 12, 6], "robe", "robe_up"),
        B([-2, 12, -4], [4, 8, 1], "bone", "ribs"),
        B([-5, 20, -3], [10, 2, 6], "robe", "shoulders"),
    ])
    m.bone("head", [0, 22, 0], parent="body", cubes=[
        B([-3, 22, -3], [6, 7, 6], "bone", "skull"),
        B([-5, 29, -5], [10, 2, 10], "robe", "hood_top"),
        B([-5, 21, 3], [10, 10, 2], "robe", "hood_back"),
        B([-5, 21, -5], [1, 8, 8], "robe", "hood_side"), B([4, 21, -5], [1, 8, 8], "robe", "hood_side_l"),
        B([-1, 31, -2], [2, 3, 3], "robe", "hood_tip", [-30, 0, 0]),
    ])
    ARM = [B([-6, 9, -1], [2, 12, 2], "bone", "arm"), B([-7, 15, -2], [4, 6, 4], "robe", "sleeve")]
    m.bone("right_arm", [-5, 21, 0], parent="body", cubes=ARM)
    m.bone("scythe", [-5, 10, 0], parent="right_arm", cubes=[
        B([-6, -2, 0], [1, 34, 1], "wood", "pole"),
        B([-6, 29, -14], [1, 3, 14], "blade", "blade"),
        B([-6, 26, -16], [1, 4, 3], "blade", "blade_tip"),
        B([-6.5, 29, 0], [2, 3, 2], "iron", "collar")])
    m.bone("left_arm", [5, 21, 0], parent="body", cubes=L(ARM))
    pal = {"robe": ("#2a2630", "#423c4a", "#141218"), "bone": BONE, "wood": ("#3a2a1e", "#5a4230", "#1e140c"),
           "blade": ("#9aa0a8", "#e0e4ea", "#5a6068"), "iron": ("#4a4a52", "#6a6a74", "#2a2a30")}

    def det(m, mat, tag, face, rect):
        fx, fy, fw, fh = rect
        if mat == "robe" and face in ("north", "south", "east", "west") and tag == "robe_low":
            for k in range(fw):                                 # ぼろぼろのすそ
                if m.rng.random() < 0.4: m.put(fx + k, fy + fh - 1, (0, 0, 0), a=0)
        if tag == "skull" and face == "north": skull_face(m, rect)
        if tag == "ribs" and face == "north":
            for j in range(1, fh, 2): m.put(fx + 0, fy + j, hexc("#1a1612")); m.put(fx + fw - 1, fy + j, hexc("#1a1612"))
        if mat == "blade" and face in ("east", "west", "down"):
            for k in range(fw): m.put(fx + k, fy + fh - 1, hexc(SOUL), glow=True)
    m.paint(pal, det)
    m.save()


# ====================================================================== 骨の重装兵
def bone_juggernaut():
    m = Model("bone_juggernaut", 128, 128, seed=33)
    m.bone("root", [0, 0, 0])
    LEG = [B([-5, 0, -2], [4, 13, 4], "bone", "leg"), B([-5, 0, -3], [4, 3, 5], "iron", "boot")]
    m.bone("right_leg", [-3, 13, 0], parent="root", cubes=LEG)
    m.bone("left_leg", [3, 13, 0], parent="root", cubes=L(LEG))
    m.bone("body", [0, 13, 0], parent="root", cubes=[
        B([-6, 14, -3], [12, 11, 6], "bone", "ribcage"),
        B([-5, 12, -3], [10, 3, 6], "bone", "pelvis"),
        B([-7, 20, -4], [14, 5, 8], "iron", "breastplate"),
    ])
    m.bone("head", [0, 25, 0], parent="body", cubes=[
        B([-4, 25, -4], [8, 8, 8], "bone", "skull"),
        B([-5, 30, -5], [10, 4, 10], "iron", "helm"),
        B([-7, 32, -1], [2, 4, 2], "horn", "horn", [0, 0, 30]), B([5, 32, -1], [2, 4, 2], "horn", "horn_l", [0, 0, -30]),
    ])
    ARM = [B([-10, 12, -2], [4, 13, 4], "bone", "arm"), B([-11, 21, -3], [6, 5, 6], "iron", "pauldron")]
    m.bone("right_arm", [-8, 24, 0], parent="body", cubes=ARM)
    m.bone("mace", [-8, 12, 0], parent="right_arm", cubes=[
        B([-9, 11, -11], [2, 2, 9], "wood", "handle"),
        B([-10, 10, -15], [4, 4, 4], "iron", "mace_head"),
        B([-9, 14, -14], [2, 1, 2], "iron", "spike_up"), B([-11, 11, -14], [1, 2, 2], "iron", "spike_side")])
    m.bone("left_arm", [8, 24, 0], parent="body", cubes=L(ARM))
    m.bone("shield", [11, 16, 0], parent="left_arm", cubes=[
        B([11, 3, -8], [2, 22, 15], "iron", "shield"), B([13, 10, -3], [1, 8, 6], "bone", "emblem")])
    pal = {"bone": BONE, "iron": ("#3a3a42", "#5a5a64", "#1e1e24"), "horn": ("#2a2420", "#4a4038", "#141010"),
           "wood": ("#3a2a1e", "#5a4230", "#1e140c")}

    def det(m, mat, tag, face, rect):
        fx, fy, fw, fh = rect
        if tag == "skull" and face == "north": skull_face(m, rect, "#ff5a3a")
        if tag == "ribcage" and face in ("north", "south"):
            for j in range(1, fh, 2):
                for k in range(1, fw - 1): m.put(fx + k, fy + j, hexc("#2a2420"))
            for j in range(fh): m.put(fx + fw // 2, fy + j, hexc(BONE[0]))
        if tag == "emblem" and face == "east": skull_face(m, rect, "#ff5a3a")
        if mat == "iron": bone_detail(m, rect, face, "#6a5040")
    m.paint(pal, det)
    m.save()


# ====================================================================== 這う骨
def crawling_bones():
    m = Model("crawling_bones", 64, 64, seed=34)
    m.bone("root", [0, 0, 0])
    m.bone("body", [0, 2, 0], parent="root", cubes=[
        B([-4, 1, -3], [8, 5, 8], "bone", "ribcage"),
        B([-1, 2, 5], [2, 2, 4], "bone", "spine"),
    ])
    m.bone("head", [0, 4, -3], parent="body", cubes=[B([-3, 1, -9], [6, 6, 6], "bone", "skull")])
    ARM = [B([-6, 1, -12], [2, 2, 10], "bone", "arm"), B([-6.5, 0, -14], [3, 1, 3], "bone", "hand")]
    m.bone("right_arm", [-5, 2, -3], parent="body", cubes=ARM)
    m.bone("left_arm", [5, 2, -3], parent="body", cubes=L(ARM))
    pal = {"bone": BONE}

    def det(m, mat, tag, face, rect):
        fx, fy, fw, fh = rect
        if tag == "skull" and face == "north": skull_face(m, rect)
        if tag == "ribcage" and face in ("up", "east", "west"):
            for j in range(1, fh, 2):
                for k in range(fw): m.put(fx + k, fy + j, hexc("#2a2420"))
    m.paint(pal, det)
    m.save()


# ====================================================================== 墓場の魂
def grave_wisp():
    m = Model("grave_wisp", 64, 64, seed=35)
    m.bone("root", [0, 0, 0])
    m.bone("body", [0, 10, 0], parent="root", cubes=[
        B([-4, 6, -4], [8, 10, 8], "ghost", "head"),
        B([-3, 3, -3], [6, 3, 6], "ghost", "tail1"),
        B([-2, 1, -2], [4, 2, 4], "ghost", "tail2"),
        B([-1, 0, -1], [2, 1, 2], "ghost", "tail3"),
    ])
    ARM = [B([-6, 8, -1], [2, 6, 2], "ghost", "arm")]
    m.bone("right_arm", [-4, 14, 0], parent="body", cubes=ARM)
    m.bone("left_arm", [4, 14, 0], parent="body", cubes=L(ARM))
    pal = {"ghost": ("#a8f0e0", "#e4fff8", "#5ab0a0")}

    def det(m, mat, tag, face, rect):
        fx, fy, fw, fh = rect
        for j in range(fh):                                     # 半透明（下ほど薄い）
            for k in range(fw):
                c = m.tex[fy + j, fx + k]
                if c[3]:
                    a = 150 if not tag.startswith("tail") else 110
                    m.tex[fy + j, fx + k] = (int(c[0]), int(c[1]), int(c[2]), a)
                    m.glow[fy + j, fx + k] = (int(c[0]), int(c[1]), int(c[2]), 90)
        if tag == "head" and face == "north":
            for x in (1, 5):
                for d in range(2):
                    for e in range(3):
                        m.put(fx + x + d, fy + 3 + e, hexc("#0a1a18"), a=240); m.glow[fy + 3 + e, fx + x + d] = (0, 0, 0, 0)
            for k in range(3, 5):
                for e in range(2):
                    m.put(fx + k, fy + 7 + e, hexc("#0a1a18"), a=240); m.glow[fy + 7 + e, fx + k] = (0, 0, 0, 0)
    m.paint(pal, det)
    m.save()


if __name__ == "__main__":
    for f in (rotting_guard, grave_reaper, bone_juggernaut, crawling_bones, grave_wisp):
        f()
