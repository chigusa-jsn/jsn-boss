"""ダンジョンモブ 4回目：闇のセット（5体）。 python3 tools/dmob_dark_gen.py
  shadow_apprentice 影の弟子（中ボス）  shadow_assassin 影の暗殺者（強）  grimoire 魔導書の魔物（強）
  shade_imp 影の小人（弱）              dark_eye 闇の目玉（弱）
"""
import math, os, sys
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from geo_tools import Model, hexc
from dmob_fire_gen import B, L, glow_all

PURPLE = "#c060ff"


def eyes(m, rect, xs, y, color, w=1, h=1):
    fx, fy, fw, fh = rect
    for x in xs:
        for d in range(w):
            for e in range(h):
                m.put(fx + x + d, fy + y + e, hexc(color), glow=True)


def runes(m, rect, n=4, color="#a050ff"):
    fx, fy, fw, fh = rect
    for _ in range(n):
        x, y = m.rng.randrange(fw), m.rng.randrange(fh)
        m.put(fx + x, fy + y, hexc(color), glow=True)


# ====================================================================== 影の弟子
def shadow_apprentice():
    m = Model("shadow_apprentice", 128, 128, seed=41)
    m.bone("root", [0, 0, 0])
    m.bone("body", [0, 12, 0], parent="root", cubes=[
        B([-5, 0, -4], [10, 8, 8], "robe", "robe_low"),
        B([-4, 8, -3], [8, 14, 6], "robe", "robe_up"),
        B([-4, 12, -4], [8, 2, 8], "trim", "sash"),
        B([-1, 14, -4], [2, 8, 1], "trim", "front_trim"),
    ])
    m.bone("head", [0, 22, 0], parent="body", cubes=[
        B([-4, 22, -4], [8, 8, 8], "shadow", "face"),
        B([-5, 28, -5], [10, 3, 10], "robe", "hood_top"),
        B([-5, 21, 3], [10, 10, 2], "robe", "hood_back"),
        B([-5, 21, -5], [1, 8, 8], "robe", "hood_side"), B([4, 21, -5], [1, 8, 8], "robe", "hood_side_l"),
        B([-1, 31, -1], [2, 3, 4], "robe", "hood_tip", [-35, 0, 0]),
    ])
    ARM = [B([-7, 11, -2], [3, 11, 4], "robe", "sleeve"), B([-7, 10, -2], [3, 2, 4], "shadow", "hand")]
    m.bone("right_arm", [-5, 21, 0], parent="body", cubes=ARM)
    m.bone("left_arm", [5, 21, 0], parent="body", cubes=L(ARM))
    m.bone("book", [6, 11, -3], parent="left_arm", cubes=[
        B([5, 9, -7], [4, 6, 4], "book", "book"), B([5.5, 9.5, -7.5], [3, 5, 1], "page", "pages")])
    m.bone("orbs", [0, 16, 0], parent="root", cubes=[
        B([-9, 26, 2], [2, 2, 2], "orb", "orb1"), B([7, 24, 3], [2, 2, 2], "orb", "orb2")])
    pal = {"robe": ("#3a2a52", "#58447a", "#1e142e"), "trim": ("#c8a040", "#ffd870", "#7a5a1a"),
           "shadow": ("#0e0a14", "#1a1424", "#06040a"), "book": ("#5a1e6a", "#7a2e8a", "#2e0a3a"),
           "page": ("#e8dcc0", "#fff4dc", "#b8a888"), "orb": ("#a050ff", "#e0b0ff", "#5a1aa0")}

    def det(m, mat, tag, face, rect):
        fx, fy, fw, fh = rect
        if mat == "orb": glow_all(m, rect)
        if tag == "face" and face == "north": eyes(m, rect, (1, 5), 4, PURPLE, 2)
        if tag == "robe_low" and face in ("north", "east", "west", "south"): runes(m, rect, 5)
        if tag == "book" and face == "east": runes(m, rect, 3, "#ffd870")
    m.paint(pal, det)
    m.save()


# ====================================================================== 影の暗殺者
def shadow_assassin():
    m = Model("shadow_assassin", 128, 128, seed=42)
    m.bone("root", [0, 0, 0])
    LEG = [B([-3, 0, -1], [3, 13, 3], "cloth", "leg"), B([-3, 0, -2], [3, 2, 4], "leather", "boot")]
    m.bone("right_leg", [-1.5, 13, 0], parent="root", cubes=LEG)
    m.bone("left_leg", [1.5, 13, 0], parent="root", cubes=L(LEG))
    m.bone("body", [0, 13, 0], parent="root", cubes=[
        B([-3, 13, -2], [6, 11, 4], "cloth", "torso"),
        B([-3, 14, -3], [6, 2, 1], "leather", "belt"),
        B([-4, 22, -3], [8, 2, 6], "scarf", "scarf"),
    ])
    m.bone("scarf_tail", [2, 23, 3], parent="body", rotation=[20, 0, 0], cubes=[B([1, 12, 3], [2, 11, 1], "scarf", "scarf_tail")])
    m.bone("head", [0, 24, 0], parent="body", cubes=[
        B([-3, 24, -3], [6, 7, 6], "cloth", "head"),
        B([-3, 24, -4], [6, 3, 1], "scarf", "mask"),
    ])
    ARM = [B([-6, 13, -1], [3, 11, 3], "cloth", "arm"), B([-6, 13, -2], [3, 3, 4], "leather", "bracer")]
    DAG = [B([-5.5, 12, -9], [1, 1, 7], "blade", "dagger"), B([-6, 11, -3], [2, 3, 1], "leather", "hilt")]
    m.bone("right_arm", [-4.5, 23, 0], parent="body", cubes=ARM)
    m.bone("right_dagger", [-5, 12, -1], parent="right_arm", cubes=DAG)
    m.bone("left_arm", [4.5, 23, 0], parent="body", cubes=L(ARM))
    m.bone("left_dagger", [5, 12, -1], parent="left_arm", cubes=L(DAG))
    pal = {"cloth": ("#1e1a26", "#322c3e", "#0e0c14"), "leather": ("#3a2a2a", "#54403e", "#1e1414"),
           "scarf": ("#5a1a3a", "#7a2a52", "#30081e"), "blade": ("#8a7aa8", "#d8c8ff", "#4a3a6a")}

    def det(m, mat, tag, face, rect):
        fx, fy, fw, fh = rect
        if tag == "head" and face == "north": eyes(m, rect, (1, 4), 3, PURPLE)
        if mat == "blade" and face in ("east", "west", "up"):
            for k in range(fw): m.put(fx + k, fy, hexc("#c080ff"), glow=True)
    m.paint(pal, det)
    m.save()


# ====================================================================== 魔導書の魔物
def grimoire():
    m = Model("grimoire", 128, 64, seed=43)
    m.bone("root", [0, 0, 0])
    m.bone("book", [0, 10, 0], parent="root", cubes=[
        B([-7, 8, -6], [14, 1, 12], "cover", "cover_low"),
        B([-6, 9, -5], [12, 2, 10], "page", "pages_low"),
        B([-7, 8, 5], [14, 6, 1], "cover", "spine"),
        B([-6, 11, -6], [12, 1, 1], "tooth", "teeth_low"),
    ])
    m.bone("lid", [0, 12, 5], parent="book", rotation=[-35, 0, 0], cubes=[
        B([-6, 11, -5], [12, 2, 10], "page", "pages_up"),
        B([-7, 13, -6], [14, 1, 12], "cover", "cover_up"),
        B([-6, 10, -6], [12, 1, 1], "tooth", "teeth_up"),
        B([-2, 14, -2], [4, 1, 4], "gem", "clasp"),
    ])
    m.bone("eye", [0, 12, 2], parent="book", cubes=[B([-2, 11, 2], [4, 3, 1], "eye", "eye")])
    pal = {"cover": ("#4a1a2a", "#6a2a3e", "#260a14"), "page": ("#e0d4b8", "#fff4dc", "#a89878"), "gem": ("#a050ff", "#e0b0ff", "#5a1aa0"),
           "tooth": ("#f0eadc", "#ffffff", "#b8b0a0"), "eye": ("#ffd040", "#fff0a0", "#a07010")}

    def det(m, mat, tag, face, rect):
        fx, fy, fw, fh = rect
        if mat == "page": runes(m, rect, max(2, fw * fh // 12), "#8a40e0")
        if mat == "cover" and face in ("east", "west", "north", "south"):
            for k in range(fw): m.put(fx + k, fy, hexc("#c8a040")); m.put(fx + k, fy + fh - 1, hexc("#c8a040"))
        if mat == "gem": glow_all(m, rect)
        if mat == "eye":
            glow_all(m, rect)
            if face == "north":
                for j in range(fh): m.put(fx + fw // 2, fy + j, hexc("#1a0a04"))
        if mat == "tooth":
            for k in range(fw):
                if k % 2: m.put(fx + k, fy, (0, 0, 0), a=0) if face == "north" else None
    m.paint(pal, det)
    m.save()


# ====================================================================== 影の小人
def shade_imp():
    m = Model("shade_imp", 64, 64, seed=44)
    m.bone("root", [0, 0, 0])
    LEG = [B([-2, 0, -1], [2, 5, 2], "shadow", "leg")]
    m.bone("right_leg", [-1, 5, 0], parent="root", cubes=LEG)
    m.bone("left_leg", [1, 5, 0], parent="root", cubes=L(LEG))
    m.bone("body", [0, 5, 0], parent="root", cubes=[B([-2, 5, -1], [4, 5, 3], "shadow", "torso")])
    m.bone("head", [0, 10, 0], parent="body", cubes=[
        B([-3, 10, -3], [6, 6, 6], "shadow", "head"),
        B([-3, 16, -1], [1, 3, 1], "shadow", "ear", [0, 0, 25]), B([2, 16, -1], [1, 3, 1], "shadow", "ear_l", [0, 0, -25])])
    ARM = [B([-4, 4, -1], [2, 6, 2], "shadow", "arm"), B([-4, 3, -2], [2, 1, 2], "claw", "claw")]
    m.bone("right_arm", [-3, 9, 0], parent="body", cubes=ARM)
    m.bone("left_arm", [3, 9, 0], parent="body", cubes=L(ARM))
    pal = {"shadow": ("#16121e", "#2a2236", "#08060c"), "claw": ("#6a3aa0", "#a060e0", "#3a1a60")}

    def det(m, mat, tag, face, rect):
        fx, fy, fw, fh = rect
        if tag == "head" and face == "north":
            eyes(m, rect, (1, 4), 2, "#d080ff")
            for k in range(1, 5): m.put(fx + k, fy + 4, hexc("#c060ff"), glow=True)
        if mat == "claw": glow_all(m, rect)
    m.paint(pal, det)
    m.save()


# ====================================================================== 闇の目玉
def dark_eye():
    m = Model("dark_eye", 64, 64, seed=45)
    m.bone("root", [0, 0, 0])
    m.bone("body", [0, 10, 0], parent="root", cubes=[
        B([-4, 6, -4], [8, 8, 8], "eyeball", "eyeball"),
        B([-1, 4, -1], [2, 2, 2], "flesh", "nerve")])
    WING = [B([-12, 9, 0], [8, 6, 1], "wing", "wing", [0, 0, -15], [-4, 11, 0]),
            B([-15, 8, 0], [4, 4, 1], "wing", "wing_tip", [0, 0, -30], [-11, 11, 0])]
    m.bone("right_wing", [-4, 11, 0], parent="body", cubes=WING)
    m.bone("left_wing", [4, 11, 0], parent="body", cubes=L(WING))
    pal = {"eyeball": ("#2a1e2e", "#3a2a40", "#140e18"), "flesh": ("#6a2a4a", "#8a3a62", "#3a1428"),
           "wing": ("#2a1a30", "#422a4a", "#140a18")}

    def det(m, mat, tag, face, rect):
        fx, fy, fw, fh = rect
        if tag == "eyeball" and face == "north":
            cx, cy = fw / 2 - 0.5, fh / 2 - 0.5
            for j in range(fh):
                for k in range(fw):
                    d = math.hypot(k - cx, j - cy)
                    if d < 3.2: m.put(fx + k, fy + j, hexc("#a040ff") if d > 1.2 else hexc("#0a0410"), glow=d > 1.2)
        if mat == "wing" and face in ("north", "south"):
            for j in range(0, fh, 2): m.put(fx + fw // 2, fy + j, hexc("#0e0612"))
    m.paint(pal, det)
    m.save()


if __name__ == "__main__":
    for f in (shadow_apprentice, shadow_assassin, grimoire, shade_imp, dark_eye):
        f()
