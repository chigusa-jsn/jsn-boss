"""ダンジョンモブ 9回目：自然のセット（5体）。 python3 tools/dmob_nature_gen.py
  spore_nursery 胞子の苗床（中ボス）  venus_snapper 毒花の魔物（強）  thorn_stag 茨の大鹿（強）
  mushroom_soldier キノコ兵（弱）      seed_squirrel 種まきリス（弱）
"""
import os, sys
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from geo_tools import Model, hexc
from dmob_fire_gen import B, L, glow_all

LEAF = ("#4a8a3a", "#6aaa52", "#2a5a22")
STEM = ("#5a7a3a", "#7a9a52", "#3a5226")


def eyes(m, rect, xs, y, color, w=1, h=1, glow=True):
    fx, fy, fw, fh = rect
    for x in xs:
        for d in range(w):
            for e in range(h):
                m.put(fx + x + d, fy + y + e, hexc(color), glow=glow)


def veins(m, rect, color):
    fx, fy, fw, fh = rect
    for j in range(fh): m.put(fx + fw // 2, fy + j, hexc(color))
    for k in range(0, fw, 3):
        if k != fw // 2: m.put(fx + k, fy + (k * 2) % max(1, fh), hexc(color))


# ====================================================================== 胞子の苗床
def spore_nursery():
    m = Model("spore_nursery", 128, 128, seed=91)
    m.bone("root", [0, 0, 0])
    leaves = []
    for i, (x, z, rz, rx) in enumerate(((-12, -2, 0, 0), (2, -2, 0, 0), (-2, -12, 0, 0), (-2, 2, 0, 0))):
        w, d = (10, 4) if i < 2 else (4, 10)
        leaves.append(B([x, 0, z], [w, 1, d], "leaf", f"leaf{i}"))
    m.bone("leaves", [0, 0, 0], parent="root", cubes=leaves)
    m.bone("stem", [0, 1, 0], parent="root", cubes=[B([-2, 1, -2], [4, 12, 4], "stem", "stem")])
    m.bone("bulb", [0, 14, 0], parent="stem", cubes=[
        B([-7, 13, -7], [14, 12, 14], "bulb", "bulb"),
        B([-5, 25, -5], [10, 3, 10], "bulb", "bulb_top"),
        B([-1, 28, -1], [2, 3, 2], "pod", "tip")])
    pods = [B([x, y, z], [3, 3, 3], "pod", f"pod{i}") for i, (x, y, z) in enumerate(((-9, 18, -2), (7, 16, 1), (-2, 20, 7), (0, 15, -9)))]
    m.bone("pods", [0, 14, 0], parent="bulb", cubes=pods)
    VINE = [B([-14, 1, -1], [6, 1, 1], "stem", "vine", [0, 0, 15]), B([-16, 2, -1], [2, 2, 2], "pod", "vine_tip")]
    m.bone("right_vine", [-8, 2, 0], parent="root", cubes=VINE)
    m.bone("left_vine", [8, 2, 0], parent="root", cubes=L(VINE))
    pal = {"leaf": LEAF, "stem": STEM, "bulb": ("#5aa04a", "#7ac062", "#2e6024"), "pod": ("#8ae06a", "#c0ff90", "#4a9a3a")}

    def det(m, mat, tag, face, rect):
        fx, fy, fw, fh = rect
        if mat == "pod": glow_all(m, rect)
        if mat == "leaf" and face == "up": veins(m, rect, "#2a5a22")
        if tag == "bulb" and face in ("north", "south", "east", "west"):
            for _ in range(10):                                   # クリーパーっぽいまだら
                m.put(fx + m.rng.randrange(fw), fy + m.rng.randrange(fh), hexc("#2e6024"))
            if face == "north":
                for (x, y) in ((3, 3), (4, 3), (3, 4), (4, 4), (9, 3), (10, 3), (9, 4), (10, 4)):
                    m.put(fx + x, fy + y, hexc("#102a0c"))
                for (x, y) in ((6, 6), (7, 6), (5, 7), (6, 7), (7, 7), (8, 7), (5, 8), (8, 8)):
                    m.put(fx + x, fy + y, hexc("#102a0c"))
    m.paint(pal, det)
    m.save()


# ====================================================================== 毒花の魔物
def venus_snapper():
    m = Model("venus_snapper", 128, 64, seed=92)
    m.bone("root", [0, 0, 0])
    m.bone("leaves", [0, 0, 0], parent="root", cubes=[
        B([-9, 0, -2], [7, 1, 4], "leaf", "leaf1"), B([2, 0, -2], [7, 1, 4], "leaf", "leaf2"), B([-2, 0, 2], [4, 1, 7], "leaf", "leaf3")])
    m.bone("stem", [0, 1, 0], parent="root", rotation=[10, 0, 0], cubes=[B([-1, 1, -1], [3, 14, 3], "stem", "stem")])
    m.bone("head", [0, 16, 0], parent="stem", cubes=[
        B([-5, 14, -8], [10, 3, 10], "trap", "jaw_low"),
        B([-5, 17, -9], [10, 1, 1], "tooth", "teeth_low")])
    m.bone("top_jaw", [0, 17, 2], parent="head", rotation=[-30, 0, 0], cubes=[
        B([-5, 17, -8], [10, 3, 10], "trap", "jaw_up"), B([-5, 16, -9], [10, 1, 1], "tooth", "teeth_up")])
    pal = {"leaf": LEAF, "stem": STEM, "trap": ("#7ac04a", "#a0e070", "#3a7024"), "tooth": ("#f0f0d0", "#ffffff", "#b8b8a0")}

    def det(m, mat, tag, face, rect):
        fx, fy, fw, fh = rect
        if mat == "trap":
            if (tag == "jaw_low" and face == "up") or (tag == "jaw_up" and face == "down"):
                for j in range(fh):
                    for k in range(fw): m.put(fx + k, fy + j, hexc("#c03040") if (j + k) % 3 else hexc("#e05060"))
            elif face != "down" and face != "up":
                for k in range(fw):
                    if k % 2 == 0: m.put(fx + k, fy, hexc("#c03040"))
        if mat == "tooth":
            for k in range(fw):
                if k % 2: m.put(fx + k, fy, (0, 0, 0), a=0) if face == "north" else None
        if mat == "leaf" and face == "up": veins(m, rect, "#2a5a22")
    m.paint(pal, det)
    m.save()


# ====================================================================== 茨の大鹿
def thorn_stag():
    m = Model("thorn_stag", 128, 128, seed=93)
    m.bone("root", [0, 0, 0])
    m.bone("body", [0, 14, 0], parent="root", cubes=[
        B([-4, 12, -7], [8, 8, 14], "fur", "torso"),
        B([-4, 12, -7], [8, 1, 14], "fur_light", "belly")])
    antler = [B([-2, 33, -9], [1, 6, 1], "thorn", "a1", [0, 0, 20]), B([-5, 37, -9], [1, 4, 1], "thorn", "a2", [0, 0, 45]),
              B([-3, 38, -9], [1, 3, 1], "thorn", "a3", [0, 0, -10]), B([-4, 35, -9], [2, 1, 1], "leaf", "a_leaf")]
    m.bone("head", [0, 22, -7], parent="body", cubes=[
        B([-1.5, 20, -9], [3, 8, 3], "fur", "neck", [25, 0, 0]),
        B([-2, 27, -14], [4, 4, 6], "fur", "head"), B([-1, 27, -16], [2, 2, 2], "fur_light", "muzzle"),
        B([-3, 30, -10], [1, 2, 1], "fur", "ear"), B([2, 30, -10], [1, 2, 1], "fur", "ear_l")] + antler + L(antler))
    LEG_F = [B([-4, 0, -6], [2, 14, 2], "fur", "leg"), B([-4, 0, -6], [2, 2, 2], "hoof", "hoof")]
    LEG_B = [B([-4, 0, 4], [2, 14, 2], "fur", "leg"), B([-4, 0, 4], [2, 2, 2], "hoof", "hoof")]
    m.bone("front_right_leg", [-3, 14, -5], parent="root", cubes=LEG_F)
    m.bone("front_left_leg", [3, 14, -5], parent="root", cubes=L(LEG_F))
    m.bone("back_right_leg", [-3, 14, 5], parent="root", cubes=LEG_B)
    m.bone("back_left_leg", [3, 14, 5], parent="root", cubes=L(LEG_B))
    m.bone("tail", [0, 19, 7], parent="body", cubes=[B([-1, 17, 7], [2, 3, 1], "fur_light", "tail")])
    pal = {"fur": ("#7a6a4a", "#9a8a62", "#4a3e28"), "fur_light": ("#d8ccb0", "#f0e8d0", "#a09880"),
           "thorn": ("#4a3a2a", "#6a5a44", "#2a1e14"), "leaf": LEAF, "hoof": ("#2a2420", "#4a4038", "#141010")}

    def det(m, mat, tag, face, rect):
        fx, fy, fw, fh = rect
        if tag == "torso" and face in ("up", "east", "west"):
            for _ in range(8): m.put(fx + m.rng.randrange(fw), fy + m.rng.randrange(fh), hexc("#5a8a3a"))   # 苔
        if tag == "head" and face == "north": eyes(m, rect, (0, 3), 1, "#a0ff60")
    m.paint(pal, det)
    m.save()


# ====================================================================== キノコ兵
def mushroom_soldier():
    m = Model("mushroom_soldier", 64, 64, seed=94)
    m.bone("root", [0, 0, 0])
    LEG = [B([-2, 0, -1], [2, 4, 2], "stalk", "leg")]
    m.bone("right_leg", [-1, 4, 0], parent="root", cubes=LEG)
    m.bone("left_leg", [1, 4, 0], parent="root", cubes=L(LEG))
    m.bone("body", [0, 4, 0], parent="root", cubes=[
        B([-2, 4, -2], [4, 6, 4], "stalk", "torso"),
        B([-5, 10, -5], [10, 3, 10], "cap", "cap"), B([-3, 13, -3], [6, 2, 6], "cap", "cap_top")])
    ARM = [B([-4, 5, -1], [2, 4, 2], "stalk", "arm")]
    m.bone("right_arm", [-3, 9, 0], parent="body", cubes=ARM)
    m.bone("spear", [-3, 5, 0], parent="right_arm", cubes=[
        B([-3.5, 5, -6], [1, 1, 8], "wood", "shaft"), B([-3.5, 5, -8], [1, 1, 2], "tip", "tip")])
    m.bone("left_arm", [3, 9, 0], parent="body", cubes=L(ARM))
    pal = {"stalk": ("#e8dcc0", "#fff4dc", "#b0a488"), "cap": ("#c02a2a", "#e04a40", "#7a1414"),
           "wood": ("#6a4a2a", "#8a6a44", "#3a2614"), "tip": ("#8a8a8a", "#c0c0c0", "#4a4a4a")}

    def det(m, mat, tag, face, rect):
        fx, fy, fw, fh = rect
        if mat == "cap" and face in ("up", "north", "south", "east", "west"):
            for _ in range(fw * fh // 10 + 1):
                x, y = m.rng.randrange(fw), m.rng.randrange(fh)
                m.put(fx + x, fy + y, hexc("#f4f0e8"))
        if tag == "torso" and face == "north":
            eyes(m, rect, (0, 3), 3, "#1a1410", glow=False)
            m.put(fx + 1, fy + 4, hexc("#3a2a20")); m.put(fx + 2, fy + 4, hexc("#3a2a20"))
    m.paint(pal, det)
    m.save()


# ====================================================================== 種まきリス
def seed_squirrel():
    m = Model("seed_squirrel", 64, 64, seed=95)
    m.bone("root", [0, 0, 0])
    m.bone("body", [0, 3, 0], parent="root", cubes=[
        B([-2, 1, -2], [4, 5, 4], "fur", "torso"),
        B([-1, 1, -3], [2, 3, 1], "fur_light", "belly")])
    m.bone("head", [0, 6, -1], parent="body", cubes=[
        B([-2, 6, -3], [4, 4, 4], "fur", "head"),
        B([-2, 10, -1], [1, 2, 1], "fur", "ear"), B([1, 10, -1], [1, 2, 1], "fur", "ear_l")])
    m.bone("tail", [0, 3, 2], parent="body", rotation=[-20, 0, 0], cubes=[
        B([-2, 2, 2], [4, 9, 3], "fur", "tail"), B([-2, 10, 0], [4, 3, 3], "fur", "tail_tip")])
    m.bone("acorn", [0, 4, -3], parent="body", cubes=[
        B([-1, 3, -5], [2, 2, 2], "acorn", "nut"), B([-1, 5, -5], [2, 1, 2], "cap", "nut_cap")])
    LEG = [B([-2, 0, -1], [1, 1, 2], "fur", "foot")]
    m.bone("right_leg", [-1.5, 1, 0], parent="root", cubes=LEG)
    m.bone("left_leg", [1.5, 1, 0], parent="root", cubes=L(LEG))
    pal = {"fur": ("#b06a3a", "#d08a52", "#6a3a1a"), "fur_light": ("#f0dcc0", "#fff0e0", "#c0a888"),
           "acorn": ("#a06a3a", "#c08a52", "#603a1a"), "cap": ("#5a3e26", "#7a5636", "#3a2414")}

    def det(m, mat, tag, face, rect):
        fx, fy, fw, fh = rect
        if tag == "head" and face == "north": eyes(m, rect, (0, 3), 1, "#1a1008", glow=False)
    m.paint(pal, det)
    m.save()


if __name__ == "__main__":
    for f in (spore_nursery, venus_snapper, thorn_stag, mushroom_soldier, seed_squirrel):
        f()
