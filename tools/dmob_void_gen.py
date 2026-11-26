"""ダンジョンモブ 8回目：虚空のセット（5体）。 python3 tools/dmob_void_gen.py
  void_scout 虚空の斥候（中ボス）  void_stalker 虚空の狩人（強）  rift_wraith 次元の歪み（強・半透明）
  void_shard 虚空のかけら（弱）    watcher_mote 監視の小目（弱）
"""
import math, os, sys
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from geo_tools import Model, hexc
from dmob_fire_gen import B, L, glow_all

VOID = ("#1a1220", "#2e2238", "#0a060e")
VGLOW = ("#c070ff", "#f0d0ff", "#7a30c0")


def eyes(m, rect, xs, y, color, w=1, h=1, glow=True):
    fx, fy, fw, fh = rect
    for x in xs:
        for d in range(w):
            for e in range(h):
                m.put(fx + x + d, fy + y + e, hexc(color), glow=glow)


def specks(m, rect, n):
    fx, fy, fw, fh = rect
    for _ in range(n):
        m.put(fx + m.rng.randrange(fw), fy + m.rng.randrange(fh), hexc("#c070ff"), glow=True)


# ====================================================================== 虚空の斥候
def void_scout():
    m = Model("void_scout", 128, 128, seed=81)
    m.bone("root", [0, 0, 0])
    LEG = [B([-3, 0, -1], [2, 18, 2], "void", "leg")]
    m.bone("right_leg", [-2, 18, 0], parent="root", cubes=LEG)
    m.bone("left_leg", [2, 18, 0], parent="root", cubes=L(LEG))
    m.bone("body", [0, 18, 0], parent="root", rotation=[8, 0, 0], cubes=[
        B([-3, 18, -2], [6, 11, 4], "void", "torso"),
        B([-4, 14, 1], [8, 15, 2], "cloak", "cloak"),
        B([-4, 26, -3], [8, 3, 6], "cloak", "collar")])
    m.bone("head", [0, 29, 0], parent="body", cubes=[
        B([-3, 29, -3], [6, 7, 6], "void", "head"),
        B([-3, 29, -4], [6, 7, 1], "mask", "mask"),
        B([-1, 36, -1], [2, 3, 2], "void", "horn", [-20, 0, 0])])
    ARM = [B([-5, 9, -1], [2, 19, 2], "void", "arm"),
           B([-5.5, 6, -2], [1, 4, 1], "claw", "claw1"), B([-4.5, 6, -2], [1, 4, 1], "claw", "claw2")]
    m.bone("right_arm", [-4, 28, 0], parent="body", cubes=ARM)
    m.bone("left_arm", [4, 28, 0], parent="body", cubes=L(ARM))
    pal = {"void": VOID, "cloak": ("#2a1a36", "#3e2a4e", "#140a1c"), "mask": ("#e8e0d4", "#ffffff", "#b0a89c"), "claw": VGLOW}

    def det(m, mat, tag, face, rect):
        fx, fy, fw, fh = rect
        if mat == "claw": glow_all(m, rect)
        if mat in ("void", "cloak"): specks(m, rect, max(1, fw * fh // 30))
        if tag == "mask" and face == "north":
            cx, cy = fw / 2 - 0.5, 3
            for j in range(fh):
                for k in range(fw):
                    d = math.hypot(k - cx, j - cy)
                    if d < 1.8: m.put(fx + k, fy + j, hexc("#c070ff") if d > 0.6 else hexc("#ffffff"), glow=True)
            for k in range(1, fw - 1): m.put(fx + k, fy + fh - 2, hexc("#2a1a30"))
        if tag == "cloak" and face in ("north", "south"):
            for k in range(fw):
                if m.rng.random() < 0.5: m.put(fx + k, fy + fh - 1, (0, 0, 0), a=0)
    m.paint(pal, det)
    m.save()


# ====================================================================== 虚空の狩人
def void_stalker():
    m = Model("void_stalker", 128, 128, seed=82)
    m.bone("root", [0, 0, 0])
    LEG = [B([-3, 0, -1], [2, 14, 3], "void", "leg"), B([-3, 0, -2], [2, 2, 1], "claw", "toe")]
    m.bone("right_leg", [-2, 14, 0], parent="root", cubes=LEG)
    m.bone("left_leg", [2, 14, 0], parent="root", cubes=L(LEG))
    m.bone("body", [0, 14, 0], parent="root", rotation=[15, 0, 0], cubes=[
        B([-3, 14, -2], [6, 10, 4], "void", "torso"),
        B([-1, 18, -3], [2, 4, 1], "claw", "rune")])
    m.bone("head", [0, 24, 0], parent="body", cubes=[
        B([-3, 24, -4], [6, 5, 7], "void", "head"),
        B([-2, 24, -6], [4, 3, 2], "void", "snout"),
        B([-3, 29, 0], [1, 4, 1], "void", "ear", [-30, 0, -10]), B([2, 29, 0], [1, 4, 1], "void", "ear_l", [-30, 0, 10])])
    ARM = [B([-5, 11, -1], [2, 13, 2], "void", "arm"),
           B([-5.5, 8, -3], [1, 5, 1], "claw", "c1"), B([-4.5, 8, -3], [1, 5, 1], "claw", "c2"), B([-5, 8, -1], [1, 5, 1], "claw", "c3")]
    m.bone("right_arm", [-4, 23, 0], parent="body", cubes=ARM)
    m.bone("left_arm", [4, 23, 0], parent="body", cubes=L(ARM))
    pal = {"void": ("#241a30", "#3a2c4a", "#100a16"), "claw": VGLOW}

    def det(m, mat, tag, face, rect):
        fx, fy, fw, fh = rect
        if mat == "claw": glow_all(m, rect)
        if mat == "void": specks(m, rect, max(1, fw * fh // 24))
        if tag == "head" and face == "north": eyes(m, rect, (1, 4), 2, "#e080ff")
    m.paint(pal, det)
    m.save()


# ====================================================================== 次元の歪み
def rift_wraith():
    m = Model("rift_wraith", 64, 64, seed=83)
    m.bone("root", [0, 0, 0])
    m.bone("rift", [0, 14, 0], parent="root", cubes=[
        B([-1, 4, -1], [2, 20, 2], "core", "slit"),
        B([-3, 8, -1], [6, 12, 1], "rift", "glow_in"),
        B([-5, 11, -1], [10, 6, 1], "rift", "glow_out")])
    HAND = [B([-8, 10, -2], [3, 8, 2], "wraith", "arm", [0, 0, -20]), B([-9, 7, -3], [3, 3, 3], "wraith", "hand")]
    m.bone("right_hand", [-4, 15, 0], parent="rift", cubes=HAND)
    m.bone("left_hand", [4, 15, 0], parent="rift", cubes=L(HAND))
    pal = {"core": ("#ffffff", "#ffffff", "#f0d0ff"), "rift": VGLOW, "wraith": ("#4a2a6a", "#6a4a8a", "#2a1440")}

    def det(m, mat, tag, face, rect):
        fx, fy, fw, fh = rect
        a = {"core": 255, "rift": 150, "wraith": 170}[mat]
        for j in range(fh):
            for k in range(fw):
                c = m.tex[fy + j, fx + k]
                if c[3]:
                    m.tex[fy + j, fx + k] = (int(c[0]), int(c[1]), int(c[2]), a)
                    m.glow[fy + j, fx + k] = (int(c[0]), int(c[1]), int(c[2]), 255 if mat != "wraith" else 80)
    m.paint(pal, det)
    m.save()


# ====================================================================== 虚空のかけら
def void_shard():
    m = Model("void_shard", 64, 64, seed=84)
    m.bone("root", [0, 0, 0])
    m.bone("crystal", [0, 8, 0], parent="root", cubes=[
        B([-2, 4, -2], [4, 9, 4], "crystal", "main", [0, 45, 0]),
        B([-1, 13, -1], [2, 3, 2], "crystal", "tip", [0, 45, 0]),
        B([-1, 2, -1], [2, 2, 2], "crystal", "bottom", [0, 45, 0]),
        B([2, 6, -1], [2, 5, 2], "crystal", "side", [0, 0, -20]), B([-4, 7, -1], [2, 4, 2], "crystal", "side_l", [0, 0, 20])])
    pal = {"crystal": ("#8a40d0", "#d0a0ff", "#4a1a80")}

    def det(m, mat, tag, face, rect):
        fx, fy, fw, fh = rect
        for j in range(fh):
            for k in range(fw):
                c = m.tex[fy + j, fx + k]
                if c[3]: m.glow[fy + j, fx + k] = (int(c[0]), int(c[1]), int(c[2]), 200)
        for j in range(fh):
            if (j + fx) % 3 == 0: m.put(fx + min(fw - 1, j % fw), fy + j, hexc("#f0d0ff"), glow=True)
    m.paint(pal, det)
    m.save()


# ====================================================================== 監視の小目
def watcher_mote():
    m = Model("watcher_mote", 64, 64, seed=85)
    m.bone("root", [0, 0, 0])
    m.bone("eye", [0, 10, 0], parent="root", cubes=[
        B([-3, 7, -3], [6, 6, 6], "shell", "shell"),
        B([-2, 8, -4], [4, 4, 1], "iris", "iris")])
    m.bone("ring", [0, 10, 0], parent="root", cubes=[
        B([-5, 9, -5], [10, 1, 1], "ring", "r1"), B([-5, 9, 4], [10, 1, 1], "ring", "r2"),
        B([-5, 9, -4], [1, 1, 8], "ring", "r3"), B([4, 9, -4], [1, 1, 8], "ring", "r4")])
    pal = {"shell": ("#2a2234", "#40344c", "#140e1a"), "iris": ("#c070ff", "#ffffff", "#7a30c0"), "ring": VGLOW}

    def det(m, mat, tag, face, rect):
        fx, fy, fw, fh = rect
        if mat in ("ring", "iris"): glow_all(m, rect)
        if tag == "iris" and face == "north":
            m.put(fx + 1, fy + 1, hexc("#0a0410")); m.put(fx + 2, fy + 1, hexc("#0a0410"))
            m.put(fx + 1, fy + 2, hexc("#0a0410")); m.put(fx + 2, fy + 2, hexc("#0a0410"))
        if mat == "shell": specks(m, rect, 2)
    m.paint(pal, det)
    m.save()


if __name__ == "__main__":
    for f in (void_scout, void_stalker, rift_wraith, void_shard, watcher_mote):
        f()
