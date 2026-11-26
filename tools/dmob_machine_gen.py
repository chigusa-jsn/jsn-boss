"""ダンジョンモブ 10回目：機械のセット（5体）。 python3 tools/dmob_machine_gen.py
  recon_drone 機甲の偵察機（中ボス）  cog_tank 歯車の戦車（強）  steam_automaton 蒸気の自動人形（強）
  clockwork_mouse ぜんまいねずみ（弱）  mini_drone 小型ドローン（弱）
"""
import os, sys
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from geo_tools import Model, hexc
from dmob_fire_gen import B, L, glow_all

GUN = ("#4a4e56", "#6a7078", "#262a30")
DARK = ("#2a2c32", "#40444c", "#141518")
BRASS = ("#c89a3a", "#e8c060", "#7a5a1a")
RED = ("#ff3a2a", "#ffb0a0", "#a01a10")


def rivets(m, rect, color="#8a8e96"):
    fx, fy, fw, fh = rect
    for k in (0, fw - 1):
        for j in (0, fh - 1):
            if fw > 2 and fh > 2: m.put(fx + k, fy + j, hexc(color))


def gear(m, rect):
    fx, fy, fw, fh = rect
    cx, cy = fw / 2 - 0.5, fh / 2 - 0.5
    for j in range(fh):
        for k in range(fw):
            d = ((k - cx) ** 2 + (j - cy) ** 2) ** 0.5
            if d < 1.0: m.put(fx + k, fy + j, hexc("#2a1e0a"))
            elif abs(d - min(fw, fh) / 2 + 0.8) < 0.6 and (k + j) % 2: m.put(fx + k, fy + j, hexc("#7a5a1a"))


# ====================================================================== 機甲の偵察機
def recon_drone():
    m = Model("recon_drone", 128, 128, seed=101)
    m.bone("root", [0, 0, 0])
    m.bone("body", [0, 14, 0], parent="root", cubes=[
        B([-6, 10, -6], [12, 9, 12], "gun", "hull"),
        B([-5, 9, -5], [10, 1, 10], "dark", "belly"),
        B([-2, 12, -7], [4, 4, 1], "lens", "lens"),
        B([-1, 19, -1], [1, 4, 1], "dark", "antenna"), B([-1.5, 23, -1.5], [2, 1, 2], "lens", "antenna_tip"),
        B([-8, 11, -3], [2, 3, 6], "dark", "gun_r"), B([6, 11, -3], [2, 3, 6], "dark", "gun_l"),
        B([-8, 12, -5], [1, 1, 2], "dark", "barrel_r"), B([7, 12, -5], [1, 1, 2], "dark", "barrel_l")])
    arms = []
    for i, (x, z) in enumerate(((-11, -11), (6, -11), (-11, 6), (6, 6))):
        arms.append(B([x + 2, 17, z + 2], [1, 1, 1], "dark", f"arm{i}"))
    m.bone("arms", [0, 17, 0], parent="body", cubes=[
        B([-10, 17, -1], [20, 1, 2], "dark", "beam_x"), B([-1, 17, -10], [2, 1, 20], "dark", "beam_z")])
    for i, (x, z) in enumerate(((-10, -1), (10, -1), (-1, -10), (-1, 10))):
        m.bone(f"rotor{i}", [x + 0.5, 18.5, z + 1], parent="arms", cubes=[
            B([x - 3, 18, z + 0.5], [7, 1, 1], "blade", f"rotor{i}a"), B([x + 0.5 - 0.5, 18, z - 2.5], [1, 1, 7], "blade", f"rotor{i}b")])
    pal = {"gun": GUN, "dark": DARK, "lens": RED, "blade": ("#9aa0a8", "#c8ccd4", "#5a6068")}

    def det(m, mat, tag, face, rect):
        fx, fy, fw, fh = rect
        if mat == "lens": glow_all(m, rect)
        if tag == "lens" and face == "north":
            m.put(fx + 1, fy + 1, hexc("#ffffff"), glow=True)
        if mat == "gun" and face in ("north", "south", "east", "west"):
            rivets(m, rect)
            for k in range(fw): m.put(fx + k, fy + fh // 2, hexc("#3a3e44"))
        if mat == "gun" and face == "up":
            for k in range(fw): m.put(fx + k, fy + fh // 2, hexc("#ffb020"))
    m.paint(pal, det)
    m.save()


# ====================================================================== 歯車の戦車
def cog_tank():
    m = Model("cog_tank", 128, 128, seed=102)
    m.bone("root", [0, 0, 0])
    m.bone("hull", [0, 4, 0], parent="root", cubes=[
        B([-6, 2, -8], [12, 5, 16], "brass", "hull"),
        B([-8, 0, -8], [2, 4, 16], "tread", "tread_r"), B([6, 0, -8], [2, 4, 16], "tread", "tread_l"),
        B([-9, 1, -5], [1, 3, 3], "brass", "cog_r1"), B([-9, 1, 2], [1, 3, 3], "brass", "cog_r2"),
        B([8, 1, -5], [1, 3, 3], "brass", "cog_l1"), B([8, 1, 2], [1, 3, 3], "brass", "cog_l2"),
        B([2, 7, 4], [2, 5, 2], "dark", "chimney")])
    m.bone("turret", [0, 9, 0], parent="hull", cubes=[
        B([-4, 7, -4], [8, 4, 8], "brass", "turret"), B([-1, 8, -14], [2, 2, 10], "dark", "barrel"),
        B([-1.5, 7.5, -15], [3, 3, 1], "dark", "muzzle"), B([-1, 11, -1], [2, 1, 2], "lens", "lamp")])
    pal = {"brass": BRASS, "tread": DARK, "dark": DARK, "lens": RED}

    def det(m, mat, tag, face, rect):
        fx, fy, fw, fh = rect
        if mat == "lens": glow_all(m, rect)
        if mat == "brass" and face in ("north", "south", "east", "west"): rivets(m, rect, "#7a5a1a")
        if tag.startswith("cog") and face in ("east", "west"): gear(m, rect)
        if mat == "tread" and face in ("east", "west", "up"):
            for k in range(0, fw if face != "up" else fh, 2):
                for j in range(fh if face != "up" else fw):
                    if face == "up": m.put(fx + j, fy + k, hexc("#0a0b0e"))
                    else: m.put(fx + k, fy + j, hexc("#0a0b0e"))
    m.paint(pal, det)
    m.save()


# ====================================================================== 蒸気の自動人形
def steam_automaton():
    m = Model("steam_automaton", 128, 128, seed=103)
    m.bone("root", [0, 0, 0])
    LEG = [B([-5, 0, -2], [4, 12, 4], "brass", "leg"), B([-5, 0, -3], [4, 2, 5], "dark", "foot"),
           B([-4, 4, -3], [2, 4, 1], "dark", "piston")]
    m.bone("right_leg", [-3, 12, 0], parent="root", cubes=LEG)
    m.bone("left_leg", [3, 12, 0], parent="root", cubes=L(LEG))
    m.bone("body", [0, 12, 0], parent="root", cubes=[
        B([-6, 12, -4], [12, 12, 8], "brass", "boiler"),
        B([-3, 14, -5], [6, 5, 1], "fire", "furnace"),
        B([-2, 24, 2], [4, 3, 4], "dark", "stack_base"), B([-1, 27, 3], [2, 7, 2], "dark", "smokestack")])
    m.bone("head", [0, 24, -1], parent="body", cubes=[
        B([-3, 24, -4], [6, 5, 5], "brass", "head"), B([-3, 26, -5], [6, 1, 1], "fire", "visor")])
    ARM = [B([-10, 12, -2], [4, 12, 4], "brass", "arm"), B([-10, 9, -3], [4, 4, 5], "dark", "fist"),
           B([-11, 20, -3], [6, 4, 6], "dark", "shoulder"), B([-9, 14, -3], [2, 5, 1], "dark", "arm_piston")]
    m.bone("right_arm", [-8, 23, 0], parent="body", cubes=ARM)
    m.bone("left_arm", [8, 23, 0], parent="body", cubes=L(ARM))
    pal = {"brass": BRASS, "dark": DARK, "fire": ("#ff8a20", "#ffe080", "#c04010")}

    def det(m, mat, tag, face, rect):
        fx, fy, fw, fh = rect
        if mat == "fire": glow_all(m, rect)
        if tag == "furnace" and face == "north":
            for k in range(0, fw, 2):
                for j in range(fh): m.put(fx + k, fy + j, hexc("#3a1a08"))
        if mat == "brass" and face in ("north", "south", "east", "west"):
            rivets(m, rect, "#7a5a1a")
            for _ in range(fw * fh // 25): m.put(fx + m.rng.randrange(fw), fy + m.rng.randrange(fh), hexc("#6aa890"))
    m.paint(pal, det)
    m.save()


# ====================================================================== ぜんまいねずみ
def clockwork_mouse():
    m = Model("clockwork_mouse", 64, 64, seed=104)
    m.bone("root", [0, 0, 0])
    m.bone("body", [0, 1, 0], parent="root", cubes=[
        B([-2, 1, -3], [4, 3, 6], "tin", "body"),
        B([-1.5, 1, -5], [3, 2, 2], "tin", "nose"), B([-0.5 * 0, 2, -6], [1, 1, 1], "lens", "nose_tip"),
        B([-2, 4, -2], [1, 2, 1], "tin", "ear"), B([1, 4, -2], [1, 2, 1], "tin", "ear_l"),
        B([-0.5 * 0, 1, 3], [1, 1, 4], "dark", "tail")])
    m.bone("key", [0, 4, 0.5], parent="body", cubes=[
        B([0, 4, 0], [1, 2, 1], "brass", "key_shaft"), B([-1, 6, 0], [3, 2, 1], "brass", "key_wing")])
    m.bone("wheels", [0, 0.5, 0], parent="root", cubes=[
        B([-2.5, 0, -2], [1, 1, 1], "dark", "w1"), B([1.5, 0, -2], [1, 1, 1], "dark", "w2"),
        B([-2.5, 0, 1], [1, 1, 1], "dark", "w3"), B([1.5, 0, 1], [1, 1, 1], "dark", "w4")])
    pal = {"tin": ("#a8acb4", "#d0d4dc", "#6a6e76"), "dark": DARK, "brass": BRASS, "lens": RED}

    def det(m, mat, tag, face, rect):
        fx, fy, fw, fh = rect
        if mat == "lens": glow_all(m, rect)
        if tag == "body" and face == "north":
            m.put(fx, fy + 1, hexc("#ff3a2a"), glow=True); m.put(fx + fw - 1, fy + 1, hexc("#ff3a2a"), glow=True)
        if tag == "body" and face in ("east", "west"):
            for k in range(fw): m.put(fx + k, fy + 1, hexc("#6a6e76"))
    m.paint(pal, det)
    m.save()


# ====================================================================== 小型ドローン
def mini_drone():
    m = Model("mini_drone", 64, 64, seed=105)
    m.bone("root", [0, 0, 0])
    m.bone("body", [0, 8, 0], parent="root", cubes=[
        B([-3, 6, -3], [6, 4, 6], "gun", "hull"), B([-1, 7, -4], [2, 2, 1], "lens", "lens"),
        B([-0.5 * 0, 4, -1], [1, 2, 1], "dark", "gun")])
    m.bone("rotor", [0, 10.5, 0], parent="body", cubes=[
        B([-5, 10, 0], [10, 1, 1], "blade", "blade_a"), B([0, 10, -5], [1, 1, 10], "blade", "blade_b"),
        B([-0.5 * 0, 10, 0], [1, 1, 1], "dark", "hub")])
    pal = {"gun": GUN, "dark": DARK, "lens": RED, "blade": ("#9aa0a8", "#c8ccd4", "#5a6068")}

    def det(m, mat, tag, face, rect):
        fx, fy, fw, fh = rect
        if mat == "lens": glow_all(m, rect)
        if mat == "gun" and face in ("north", "south", "east", "west"): rivets(m, rect)
    m.paint(pal, det)
    m.save()


if __name__ == "__main__":
    for f in (recon_drone, cog_tank, steam_automaton, clockwork_mouse, mini_drone):
        f()
