"""氷の魔女：モデル＋テクスチャ＋発光マスク。 python3 tools/ice_witch_gen.py"""
import math, os, sys
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from geo_tools import Model, mirror, hexc

m = Model("ice_witch", 256, 256, seed=11)


def shard(x, y, z, h=6, mat="crystal", tag="shard"):
    """縦長のひし形っぽい氷の結晶（中心 x,z、下端 y）"""
    return [([x - 1, y, z - 1], [2, h, 2], mat, tag),
            ([x - 1.5, y + h // 3, z - 1.5], [3, max(2, h // 3), 3], mat, tag)]


# ---------------------------------------------------------------- model
m.bone("root", [0, 0, 0])
m.bone("body", [0, 22, 0], parent="root")

# 3段に広がるローブ。すそには氷のつららが下がる
m.bone("robe", [0, 22, 0], parent="body", cubes=[
    ([-4.5, 18, -3], [9, 4, 6], "robe", "skirt1"),
    ([-5, 14, -3.5], [10, 4, 7], "robe", "skirt1"),
    ([-5.5, 10, -4], [11, 4, 8], "robe", "skirt2"),
    ([-6, 6, -4.5], [12, 4, 9], "robe", "skirt2"),
    ([-7, 3, -5.5], [14, 4, 11], "robe_hem", "skirt3"),
    ([-6, 1, -5], [2, 3, 2], "crystal", "icicle"),
    ([-1, 2, -6], [2, 2, 2], "crystal", "icicle"),
    ([4, 1, -4], [2, 3, 2], "crystal", "icicle"),
    ([-4, 1, 3], [2, 3, 2], "crystal", "icicle"),
    ([3, 2, 3.5], [2, 2, 2], "crystal", "icicle"),
    ([-7.5, 2, -1], [2, 2, 2], "crystal", "icicle"),
    ([5.5, 1, 0], [2, 3, 2], "crystal", "icicle"),
])

SHOULDER = [
    ([-7, 28, -2], [3, 3, 4], "crystal", "shoulder"),
    ([-8, 30, -1], [2, 4, 2], "crystal", "shoulder_spike"),
    ([-6.5, 31, -2.5], [1, 3, 1], "crystal", "shoulder_spike"),
]
m.bone("torso", [0, 22, 0], parent="body", cubes=[
    ([-4.5, 21, -3], [9, 2, 6], "silver", "sash"),
    ([-4, 23, -2.5], [8, 7, 5], "robe", "bodice"),
    ([-1, 26, -3], [2, 2, 1], "crystal_glow", "brooch"),
    ([-5, 29, -3], [10, 2, 6], "fur", "collar"),
] + SHOULDER + mirror(SHOULDER))

m.bone("head", [0, 31, 0], parent="torso", cubes=[
    ([-3.5, 31, -3.5], [7, 7, 7], "skin", "face"),
    ([-4, 27, 2.5], [8, 9, 2], "hair", "hair_mid"),
    ([-3, 20, 3], [6, 7, 2], "hair", "hair_back"),
    ([-4, 31, -3], [1, 6, 6], "hair", "hair_side"),
    ([3, 31, -3], [1, 6, 6], "hair", "hair_side"),
])
m.bone("hat", [0, 37, 0], parent="head", cubes=[
    ([-7, 37, -7], [14, 1, 14], "hat", "brim"),
    ([-4.5, 38, -4.5], [9, 4, 9], "hat", "cone1"),
    ([-5, 38, -5], [10, 1, 10], "silver", "band"),
    ([-1, 38, -5.5], [2, 2, 1], "crystal_glow", "band_gem"),
    ([-3.5, 42, -3], [7, 3, 7], "hat", "cone2"),
    ([-2.5, 45, -1.5], [5, 3, 5], "hat", "cone3"),
    ([-1.5, 48, 0.5], [3, 2, 3], "hat", "cone4"),
    ([-1, 49, 2.5], [2, 2, 2], "crystal_glow", "hat_tip"),
    ([4, 38, -6], [2, 3, 2], "crystal", "brim_ice"),
    ([-6, 38, 2], [2, 2, 2], "crystal", "brim_ice"),
])

ARM = [
    ([-7, 23, -1.5], [3, 8, 3], "robe", "upper_arm"),
    ([-8, 17, -2.5], [5, 6, 5], "sleeve", "sleeve"),
    ([-6.5, 15, -1], [2, 2, 2], "skin", "hand"),
]
# 右腕は少し外に開いて、ローブの外で杖を持つ（杖は逆に回してまっすぐ立てる）
m.bone("right_arm", [-5.5, 30, 0], parent="torso", cubes=ARM, rotation=[0, 0, -20])
m.bone("left_arm", [5.5, 30, 0], parent="torso", cubes=mirror(ARM))

# 氷の杖（右手）。先端に光る大きな結晶と、それを抱える銀の爪
m.bone("staff", [-5.5, 16, 0], parent="right_arm", rotation=[0, 0, 20], cubes=[
    ([-6.5, 0, -1], [2, 36, 2], "staff", "shaft"),
    ([-6, -1, -0.5], [1, 1, 1], "silver", "ferrule"),
    ([-7.5, 35, -2], [4, 2, 4], "silver", "claw_base"),
    ([-9, 36, -1], [1, 5, 2], "silver", "claw"),
    ([-2, 36, -1], [1, 5, 2], "silver", "claw"),
    ([-7, 37, -1.5], [3, 8, 3], "crystal_glow", "staff_gem"),
    ([-6.5, 45, -1], [2, 2, 2], "crystal_glow", "staff_gem"),
])

# 周りをゆっくり回る3つの氷の結晶
orbit = []
for k, (ang, y) in enumerate([(30, 27), (150, 31), (270, 23)]):
    a = math.radians(ang)
    orbit += shard(round(12 * math.cos(a)), y, round(12 * math.sin(a)), 7, "crystal_glow", f"orbit{k}")
m.bone("orbit", [0, 28, 0], parent="root", cubes=orbit)

# ---------------------------------------------------------------- texture
PAL = {
    "robe": ("#1d2b53", "#3a5a9a", "#0e1530"),
    "robe_hem": ("#1d2b53", "#3a5a9a", "#0e1530"),
    "sleeve": ("#1d2b53", "#3a5a9a", "#0e1530"),
    "fur": ("#e3ebf4", "#ffffff", "#aab8ca"),
    "silver": ("#b6c4d8", "#eef4ff", "#5f6d86"),
    "skin": ("#cddbeb", "#e6eef8", "#9fb0c8"),
    "hair": ("#dce6f1", "#ffffff", "#9fb1c7"),
    "hat": ("#17223f", "#2f4778", "#0a0f20"),
    "staff": ("#cfe6f5", "#ffffff", "#6f97b8"),
    "crystal": ("#7fd8fb", "#e6fbff", "#2f8fc6"),
    "crystal_glow": ("#7fe3ff", "#f0fdff", "#36a9e6"),
}
ICE = [hexc("#3fb6f0"), hexc("#8fe6ff"), hexc("#dffaff")]
FROST = hexc("#cfe3f7")


def details(m, mat, tag, face, rect):
    fx, fy, fw, fh = rect
    r = m.rng
    # 結晶：斜めのきらめきと、明るい部分は発光
    if mat in ("crystal", "crystal_glow"):
        for j in range(fh):
            for k in range(fw):
                if (k + j) % 4 == 0:
                    m.put(fx + k, fy + j, ICE[2], glow=True)
                elif mat == "crystal" and (k + j) % 4 == 2:
                    m.put(fx + k, fy + j, ICE[1], glow=True)
    # ローブのすそ：光る雪の結晶模様の帯と、白い霜の縁
    if tag == "skirt3" and face not in ("up", "down"):
        for k in range(1, fw - 1):
            if k % 3 == 1:
                m.put(fx + k, fy + 2, ICE[1], glow=True)
                m.put(fx + k, fy + 1, ICE[0], glow=True)
                m.put(fx + k, fy + 3, ICE[0], glow=True)
            elif k % 3 == 2:
                m.put(fx + k, fy + 2, ICE[0], glow=True)
        for k in range(fw):
            m.put(fx + k, fy + fh - 1, FROST)
            if r.random() < 0.5:
                m.put(fx + k, fy + fh - 2, FROST)
    if tag in ("skirt1", "skirt2", "skirt3") and face == "north":
        mid = fw // 2
        for j in range(fh):                             # 前の合わせから見える明るい内側のドレス
            for k in (mid - 1, mid):
                if tag != "skirt3" or j < 1 or j == fh - 2:
                    m.put(fx + k, fy + j, hexc("#4f7fc0") + r.uniform(-6, 6))
    if tag == "skirt2" and face not in ("up", "down"):
        for k in range(fw):                             # すそに銀のライン
            m.put(fx + k, fy + fh - 1, hexc("#8fa3c4"))
    if tag == "sleeve" and face not in ("up", "down"):
        for k in range(fw):                             # 袖口のファー
            m.put(fx + k, fy + fh - 1, hexc("#e3ebf4"))
            m.put(fx + k, fy + fh - 2, hexc("#ffffff") if k % 2 else hexc("#cdd8e6"))
    if tag == "bodice" and face == "north":
        for j in range(1, fh - 1):                      # 胸元の銀の編み上げ
            m.put(fx + fw // 2 - 1 + (j % 2), fy + j, hexc("#b6c4d8"))
    # 顔：光る水色の目、凍った涙のあと、前髪
    if tag == "face":
        if face == "north":
            for k in range(fw):
                m.put(fx + k, fy, hexc("#dce6f1"))
                if k in (0, fw - 1):
                    for j in range(1, 4):
                        m.put(fx + k, fy + j, hexc("#dce6f1"))
            for k in (1, 2, 4, 5):
                m.put(fx + k, fy + 3, hexc("#5ff0ff") if k in (2, 4) else hexc("#1a6fa8"), glow=k in (2, 4))
            m.put(fx + 2, fy + 4, ICE[0], glow=True)
            m.put(fx + 4, fy + 4, ICE[0], glow=True)
            m.put(fx + 3, fy + 5, hexc("#7a8fb0"))
        elif face in ("up", "south", "west", "east"):
            for j in range(fh):
                for k in range(fw):
                    c = hexc("#dce6f1") + r.uniform(-8, 8)
                    if face in ("west", "east") and j > 4 and k < 2:
                        c = hexc("#cddbeb")
                    m.put(fx + k, fy + j, c)
    if mat == "hair" and face not in ("up", "down"):
        for k in range(fw):                             # 髪の流れ
            if k % 2 == 0:
                for j in range(fh):
                    if r.random() < 0.6:
                        m.put(fx + k, fy + j, hexc("#c3d1e2"))
        if tag == "hair_back":
            for k in range(fw):                         # 毛先が凍っている
                m.put(fx + k, fy + fh - 1, ICE[1], glow=True)
    # 帽子：ふちに霜がつく
    if mat == "hat" and face not in ("down",):
        for k in range(fw):
            if r.random() < 0.55:
                m.put(fx + k, fy, FROST)
            if face != "up" and r.random() < 0.15:
                m.put(fx + k, fy + r.randrange(fh), FROST)
    if mat == "staff" and face not in ("up", "down"):
        for j in range(0, fh, 5):                       # 杖の巻き模様（光る）
            for k in range(fw):
                m.put(fx + k, fy + j, ICE[1], glow=True)


m.paint(PAL, details, glow_mats={"crystal_glow"}, outline_skip={"crystal", "crystal_glow", "hair"})
m.save()
