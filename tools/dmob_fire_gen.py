"""ダンジョンモブ 1回目：炎のセット（5体）のモデルとテクスチャ。 python3 tools/dmob_fire_gen.py
  flame_squire  炎の従騎士（中ボス）   lava_armor  溶岩の甲冑兵（強）   flame_eye  炎の魔眼（強）
  ember_imp     燃える小鬼（弱）       cinder_bat  火の粉コウモリ（弱）
"""
import math, os, sys
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from geo_tools import Model, mirror, hexc


def B(o, s, mat, tag="", rot=None, pivot=None):
    x = {}
    if rot: x["rot"] = list(rot)
    if pivot: x["pivot"] = list(pivot)
    return (list(o), list(s), mat, tag, x) if x else (list(o), list(s), mat, tag)


def L(cubes):     # 左側（+X）用に反転して、タグに _l を付ける
    return [c[:3] + (c[3] + "_l",) + c[4:] for c in mirror(cubes)]


FLAME = ("#ff8a2a", "#ffe08a", "#d0401a")
EMBER = ("#ff5a1a", "#ffb050", "#a02a10")


def glow_all(m, rect):
    fx, fy, fw, fh = rect
    for j in range(fh):
        for k in range(fw):
            c = m.tex[fy + j, fx + k]
            if c[3]:
                m.glow[fy + j, fx + k] = (int(c[0]), int(c[1]), int(c[2]), 255)


def flame_face(m, rect, face):
    """炎のパーツ：下が赤、上が黄色。先が揺らぐ"""
    fx, fy, fw, fh = rect
    for j in range(fh):
        t = j / max(1, fh - 1)
        for k in range(fw):
            c = hexc("#ffe27a") * (1 - t) + hexc("#ff4a14") * t
            if face == "up": c = hexc("#fff0a0")
            m.put(fx + k, fy + j, c + m.rng.uniform(-10, 10), glow=True)


# ====================================================================== 炎の従騎士
def flame_squire():
    m = Model("flame_squire", 128, 128, seed=11)
    m.bone("root", [0, 0, 0])
    LEG = [B([-4, 0, -2], [4, 12, 4], "armor", "leg"), B([-4, 0, -3], [4, 3, 5], "steel", "boot"),
           B([-4, 7, -3], [4, 2, 1], "trim", "knee")]
    m.bone("right_leg", [-2, 12, 0], parent="root", cubes=LEG)
    m.bone("left_leg", [2, 12, 0], parent="root", cubes=L(LEG))
    m.bone("body", [0, 12, 0], parent="root", cubes=[
        B([-4, 12, -2], [8, 12, 4], "armor", "torso"),
        B([-4, 17, -3], [8, 6, 1], "trim", "chest"),
        B([-5, 12, -3], [10, 2, 6], "leather", "belt"),
        B([-1, 12, -4], [2, 2, 1], "flame_gem", "buckle"),
        B([-5, 9, -3], [10, 3, 1], "cape", "tabard"),
    ])
    m.bone("cape", [0, 23, 2], parent="body", rotation=[8, 0, 0], cubes=[
        B([-5, 4, 2], [10, 19, 1], "cape", "cape"),
        B([-5, 3, 2], [10, 2, 1], "flame", "cape_fire"),
    ])
    m.bone("head", [0, 24, 0], parent="body", cubes=[
        B([-4, 24, -4], [8, 8, 8], "armor", "helm"),
        B([-4, 28, -5], [8, 1, 1], "trim", "brow"),
        B([-1, 32, -3], [2, 2, 7], "trim", "crest"),
        B([-1, 33, -2], [2, 4, 6], "flame", "plume"),
        B([-1, 36, 0], [2, 3, 4], "flame", "plume_tip"),
    ])
    ARM = [B([-8, 12, -2], [4, 11, 4], "armor", "arm"), B([-9, 20, -3], [5, 4, 6], "armor", "pauldron"),
           B([-9, 23, -3], [5, 1, 6], "trim", "pauldron_rim"), B([-8, 11, -3], [4, 3, 5], "steel", "gauntlet")]
    m.bone("right_arm", [-5, 22, 0], parent="body", cubes=ARM)
    m.bone("sword", [-6, 12, -1], parent="right_arm", cubes=[
        B([-7, 11, -3], [2, 2, 4], "leather", "grip"),
        B([-8, 10, -5], [4, 4, 1], "trim", "guard"),
        B([-7, 11, -21], [2, 2, 16], "blade", "blade"),
        B([-6.5, 13, -20], [1, 1, 14], "flame", "blade_fire"),
    ])
    m.bone("left_arm", [5, 22, 0], parent="body", cubes=L(ARM))
    m.bone("shield", [8, 15, 0], parent="left_arm", cubes=[
        B([8, 8, -6], [1, 13, 11], "shield", "shield"),
        B([9, 12, -3], [1, 5, 5], "flame_gem", "emblem"),
        B([7.5, 20, -6], [2, 1, 11], "trim", "shield_rim"),
    ])
    pal = {"armor": ("#4a4652", "#6c6876", "#2a2830"), "steel": ("#5e5a66", "#8a8694", "#32303a"),
           "trim": ("#d4922e", "#ffd07a", "#8a5414"), "leather": ("#5a3a26", "#7a523a", "#3a2416"),
           "cape": ("#9a2216", "#c4402a", "#5a120a"), "blade": ("#c8c4cc", "#ffffff", "#7a7680"),
           "shield": ("#6a3020", "#8a4a30", "#3a160c"), "flame": FLAME, "flame_gem": ("#ff6a1a", "#ffd080", "#b03010")}

    def det(m, mat, tag, face, rect):
        fx, fy, fw, fh = rect
        if mat in ("flame",): flame_face(m, rect, face)
        if mat == "flame_gem": glow_all(m, rect)
        if tag == "helm" and face == "north":          # 横長の覗き穴と光る目
            for k in range(1, fw - 1):
                m.put(fx + k, fy + 3, hexc("#141016"))
            for ex in (2, 5):
                m.put(fx + ex, fy + 3, hexc("#ffb040"), glow=True)
            for j in range(4, 7):
                m.put(fx + fw // 2, fy + j, hexc("#2a2830"))      # 鼻当て
        if tag == "cape" and face in ("north", "south"):
            for k in range(fw):                                     # すそが焦げて燃える
                for j in range(fh - 3, fh):
                    if (k + j) % 3 == 0: m.put(fx + k, fy + j, hexc("#ff7a20"), glow=True)
        if tag == "blade" and face in ("east", "west", "up"):
            for k in range(fw):
                m.put(fx + k, fy, hexc("#ffb060"), glow=True)
    m.paint(pal, det)
    m.save()


# ====================================================================== 溶岩の甲冑兵
def lava_armor():
    m = Model("lava_armor", 128, 128, seed=12)
    m.bone("root", [0, 0, 0])
    LEG = [B([-6, 0, -3], [5, 13, 6], "iron", "leg"), B([-6, 0, -4], [5, 4, 7], "iron_dark", "boot"),
           B([-5, 13, -2], [3, 2, 4], "lava", "hip_joint"), B([-6, 7, -4], [5, 1, 1], "lava", "knee_crack")]
    m.bone("right_leg", [-3, 14, 0], parent="root", cubes=LEG)
    m.bone("left_leg", [3, 14, 0], parent="root", cubes=L(LEG))
    m.bone("body", [0, 14, 0], parent="root", cubes=[
        B([-7, 15, -4], [14, 14, 8], "iron", "torso"),
        B([-3, 19, -5], [6, 7, 1], "lava", "core"),
        B([-8, 14, -5], [16, 3, 10], "iron_dark", "waist"),
        B([-5, 29, -3], [10, 1, 6], "lava", "neck"),
    ])
    m.bone("head", [0, 30, 0], parent="body", cubes=[
        B([-4, 30, -5], [8, 8, 8], "iron", "helm"),
        B([-5, 34, -6], [10, 2, 10], "iron_dark", "helm_rim"),
        B([-1, 38, -4], [2, 3, 6], "iron_dark", "helm_ridge"),
    ])
    ARM = [B([-12, 14, -3], [5, 14, 6], "iron", "arm"), B([-13, 25, -4], [7, 6, 8], "iron_dark", "pauldron"),
           B([-11, 31, -1], [2, 4, 2], "iron_dark", "spike", [0, 0, 15]), B([-13, 11, -4], [7, 5, 8], "iron_dark", "fist"),
           B([-11, 19, -4], [3, 1, 1], "lava", "elbow_crack"), B([-8, 27, -2], [1, 3, 4], "lava", "shoulder_joint")]
    m.bone("right_arm", [-9, 28, 0], parent="body", cubes=ARM)
    m.bone("left_arm", [9, 28, 0], parent="body", cubes=L(ARM))
    pal = {"iron": ("#3c3634", "#5a5250", "#1e1a18"), "iron_dark": ("#2a2422", "#443c38", "#120e0c"),
           "lava": ("#ff6a10", "#ffd060", "#c02a08")}

    def det(m, mat, tag, face, rect):
        fx, fy, fw, fh = rect
        if mat == "lava":
            for j in range(fh):
                for k in range(fw):
                    c = hexc("#ffd060") if m.rng.random() < 0.25 else hexc("#ff5a10")
                    m.put(fx + k, fy + j, c, glow=True)
        if mat in ("iron", "iron_dark") and face != "down" and fw > 3 and fh > 3:
            for _ in range(max(1, fw * fh // 30)):            # 溶岩のひび
                x, y = m.rng.randrange(fw), m.rng.randrange(fh)
                for _ in range(3):
                    if 0 <= x < fw and 0 <= y < fh:
                        m.put(fx + x, fy + y, hexc("#ff5a10"), glow=True)
                    x += m.rng.choice((-1, 0, 1)); y += 1
        if tag == "helm" and face == "north":
            for k in range(1, fw - 1):
                m.put(fx + k, fy + 3, hexc("#ffb030"), glow=True)
            for j in range(4, fh - 1):                         # したたる溶岩
                m.put(fx + 2, fy + j, hexc("#ff6a10"), glow=True)
                if j < fh - 2: m.put(fx + fw - 3, fy + j, hexc("#ff6a10"), glow=True)
    m.paint(pal, det)
    m.save()


# ====================================================================== 炎の魔眼
def flame_eye():
    m = Model("flame_eye", 128, 64, seed=13)
    m.bone("root", [0, 0, 0])
    m.bone("eye", [0, 16, 0], parent="root", cubes=[
        B([-6, 10, -6], [12, 12, 12], "eyeball", "eyeball"),
        B([-7, 19, -7], [14, 4, 14], "shell", "lid_top"),
        B([-7, 9, -7], [14, 3, 14], "shell", "lid_bottom"),
        B([-2, 23, -2], [4, 3, 4], "shell", "horn"),
        B([-1, 26, -1], [2, 3, 2], "flame", "horn_fire"),
    ])
    ring = []                                               # 目の後ろで燃える炎の輪（後光のように縦に）
    for i in range(10):
        a = i * math.pi / 5
        x, y = round(12 * math.cos(a)) - 1, round(16 + 12 * math.sin(a)) - 1
        ring.append(B([x, y, 4], [3, 3, 3 + (i % 2) * 2], "flame", "ring"))
    m.bone("ring", [0, 16, 0], parent="root", cubes=ring)
    m.bone("tail", [0, 10, 0], parent="eye", cubes=[
        B([-3, 5, -3], [6, 5, 6], "flame", "tail1"), B([-2, 2, -2], [4, 3, 4], "flame", "tail2"),
        B([-1, 0, -1], [2, 2, 2], "flame", "tail3")])
    pal = {"eyeball": ("#f0e4d4", "#ffffff", "#c8a890"), "shell": ("#3a2420", "#5a3a30", "#1e100c"), "flame": FLAME}

    def det(m, mat, tag, face, rect):
        fx, fy, fw, fh = rect
        if mat == "flame": flame_face(m, rect, face)
        if mat == "eyeball":
            for _ in range(10):                                # 血管
                x, y = m.rng.randrange(fw), m.rng.randrange(fh)
                for _ in range(3):
                    if 0 <= x < fw and 0 <= y < fh: m.put(fx + x, fy + y, hexc("#d05a4a"))
                    x += m.rng.choice((-1, 1)); y += m.rng.choice((-1, 0, 1))
            if face == "north":                                # 燃える虹彩と縦長の瞳
                cx, cy = fw / 2 - 0.5, fh / 2 - 0.5
                for j in range(fh):
                    for k in range(fw):
                        d = math.hypot(k - cx, j - cy)
                        if d < 4.6:
                            c = hexc("#ffd040") if d < 2.5 else hexc("#ff6a10")
                            m.put(fx + k, fy + j, c, glow=True)
                        if abs(k - cx) < 0.6 and abs(j - cy) < 3.6:
                            m.put(fx + k, fy + j, hexc("#100808"))
        if mat == "shell" and face == "north" and tag == "lid_top":
            for k in range(fw):
                if k % 3 == 1: m.put(fx + k, fy + fh - 1, hexc("#d8c8a0"))   # まつげの牙
    m.paint(pal, det)
    m.save()


# ====================================================================== 燃える小鬼
def ember_imp():
    m = Model("ember_imp", 64, 64, seed=14)
    m.bone("root", [0, 0, 0])
    LEG = [B([-3, 0, -1], [2, 5, 2], "skin", "leg"), B([-3, 0, -2], [2, 1, 3], "claw", "foot")]
    m.bone("right_leg", [-2, 5, 0], parent="root", cubes=LEG)
    m.bone("left_leg", [2, 5, 0], parent="root", cubes=L(LEG))
    m.bone("body", [0, 5, 0], parent="root", cubes=[
        B([-3, 5, -2], [6, 6, 4], "skin", "torso"),
        B([-3, 5, -3], [6, 2, 5], "cloth", "loincloth"),
    ])
    m.bone("tail", [0, 6, 2], parent="body", rotation=[-30, 0, 0], cubes=[
        B([0, 6, 2], [1, 1, 6], "skin", "tail"), B([-1, 5, 7], [3, 3, 3], "flame", "tail_fire")])
    m.bone("head", [0, 11, 0], parent="body", cubes=[
        B([-4, 11, -4], [8, 7, 7], "skin", "head"),
        B([-5, 14, -1], [1, 2, 2], "skin", "ear"), B([4, 14, -1], [1, 2, 2], "skin", "ear_l"),
        B([-4, 17, -2], [1, 3, 1], "horn", "horn", [0, 0, 20]), B([3, 17, -2], [1, 3, 1], "horn", "horn_l", [0, 0, -20]),
        B([-3, 18, -3], [6, 2, 5], "flame", "hair"), B([-2, 20, -2], [4, 2, 3], "flame", "hair2"),
        B([-1, 22, -1], [2, 2, 2], "flame", "hair3"),
    ])
    ARM = [B([-5, 5, -1], [2, 6, 2], "skin", "arm"), B([-5, 4, -2], [2, 1, 3], "claw", "hand")]
    m.bone("right_arm", [-4, 10, 0], parent="body", cubes=ARM)
    m.bone("left_arm", [4, 10, 0], parent="body", cubes=L(ARM))
    pal = {"skin": ("#c8402a", "#e86a4a", "#8a2014"), "cloth": ("#3a2a22", "#5a4232", "#1e140e"),
           "horn": ("#3a2a26", "#5a4a40", "#1a100c"), "claw": ("#2a1e1a", "#4a3a30", "#140c08"), "flame": FLAME}

    def det(m, mat, tag, face, rect):
        fx, fy, fw, fh = rect
        if mat == "flame": flame_face(m, rect, face)
        if tag == "head" and face == "north":
            for ex in (1, 5):                                   # 黄色く光る目
                m.put(fx + ex, fy + 3, hexc("#ffe040"), glow=True)
                m.put(fx + ex + 1, fy + 3, hexc("#ffe040"), glow=True)
            for k in range(2, 6):                               # ギザギザの笑い
                m.put(fx + k, fy + 5, hexc("#2a0a06"))
                if k % 2 == 0: m.put(fx + k, fy + 5, hexc("#fff0d0"))
    m.paint(pal, det)
    m.save()


# ====================================================================== 火の粉コウモリ
def cinder_bat():
    m = Model("cinder_bat", 64, 64, seed=15)
    m.bone("root", [0, 0, 0])
    m.bone("body", [0, 10, 0], parent="root", cubes=[
        B([-2, 8, -2], [4, 5, 4], "fur", "torso"),
        B([-2, 13, -3], [4, 4, 4], "fur", "head"),
        B([-2, 17, -2], [1, 2, 1], "fur", "ear"), B([1, 17, -2], [1, 2, 1], "fur", "ear_l"),
        B([-1, 6, -1], [2, 2, 2], "ember", "belly_fire"),
    ])
    WING = [B([-11, 10, -2], [9, 2, 8], "wing", "wing_in", [0, 0, -25], [-2, 11, 0]),
            B([-17, 9, -1], [7, 2, 6], "wing", "wing_out", [0, 0, -10], [-11, 10, 0]),
            B([-17, 9, 5], [15, 2, 1], "ember", "wing_edge", [0, 0, -18], [-2, 10, 5])]
    m.bone("right_wing", [-2, 12, 0], parent="body", cubes=WING)
    m.bone("left_wing", [2, 12, 0], parent="body", cubes=L(WING))
    pal = {"fur": ("#3a2420", "#5a3a30", "#1e100c"), "wing": ("#5a2a1e", "#7a3a2a", "#2a120a"), "ember": EMBER}

    def det(m, mat, tag, face, rect):
        fx, fy, fw, fh = rect
        if mat == "ember":
            for j in range(fh):
                for k in range(fw):
                    m.put(fx + k, fy + j, hexc("#ffb040") if m.rng.random() < 0.4 else hexc("#ff4a10"), glow=True)
        if mat == "wing" and face in ("up", "down"):
            for k in range(fw):                                 # 燃えている縁と骨
                m.put(fx + k, fy + fh - 1, hexc("#ff7a20"), glow=True)
            for j in range(fh):
                if j % 3 == 0: m.put(fx + fw // 2, fy + j, hexc("#2a120a"))
        if tag == "head" and face == "north":
            m.put(fx + 0, fy + 1, hexc("#ffa020"), glow=True); m.put(fx + 3, fy + 1, hexc("#ffa020"), glow=True)
            m.put(fx + 1, fy + 3, hexc("#ffffff")); m.put(fx + 2, fy + 3, hexc("#ffffff"))   # 牙
    m.paint(pal, det)
    m.save()


if __name__ == "__main__":
    for f in (flame_squire, lava_armor, flame_eye, ember_imp, cinder_bat):
        f()
