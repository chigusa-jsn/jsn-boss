"""フェンリル：モデル＋テクスチャ＋発光マスク。 python3 tools/fenrir_gen.py
銀色の巨大な狼（第2形態は金色・赤い目。tools/fenrir_phase2.sh）。首と前脚に千切れた鎖の枷、首まわりと背中に逆立つ毛、氷のように光る目と肩のルーン。
正面は -Z。全体を K 倍の大きさで作る（肩の高さ 約2.5ブロック）。
"""
import os, sys
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from geo_tools import Model, mirror, hexc

K = 1.9
# GOLD=1 で実行すると、第2形態（金色の毛・赤い目とルーン）の色で塗る（形と UV は同じ）
GOLD = os.environ.get("GOLD") == "1"
m = Model("fenrir", 256, 256, seed=44)


def c(o, s, mat, tag):
    return ([round(v * K * 2) / 2 for v in o], [max(1, round(v * K)) for v in s], mat, tag)


def p(*xyz):
    return [round(v * K * 2) / 2 for v in xyz]


# ---------------------------------------------------------------- model
m.bone("root", [0, 0, 0])
m.bone("body", p(0, 15, 0), parent="root", cubes=[
    c([-5, 10, -12], [10, 10, 12], "fur", "chest"),
    c([-4, 12.5, 0], [8, 7, 10], "fur", "belly"),
    c([-4.5, 11.5, 10], [9, 8.5, 7], "fur", "hips"),
    c([-4, 9, -11], [8, 2.5, 9], "fur_light", "underside"),
    c([-5.5, 12, -12.5], [11, 9, 5], "fur_light", "chest_fluff"),
])

# 背中に逆立つ毛（後ろへ流れるトゲ）
for i, (z, h) in enumerate(((-10, 4), (-6, 4.5), (-2, 4), (2, 3.5), (6, 3), (10, 2.5))):
    m.bone(f"ridge{i}", p(0, 20, z), parent="body", rotation=[-35, 0, 0], cubes=[
        c([-1.5, 19.5, z - 1], [3, h, 2], "fur_dark", "ridge")])

m.bone("neck", p(0, 18, -11), parent="body", rotation=[-20, 0, 0], cubes=[
    c([-4, 14, -17], [8, 8, 7], "fur", "neck"),
    c([-6, 13, -15.5], [12, 11, 6], "fur_light", "mane"),             # 首まわりのたっぷりした毛
    c([-4.5, 14.5, -16.5], [9, 2, 6], "chain", "collar"),            # 首輪（鎖の枷）
])
# 首まわりの毛のえり（放射状に逆立つ）
for i, ang in enumerate((-60, -30, 0, 30, 60, -95, 95)):
    m.bone(f"ruff{i}", p(0, 19, -13), parent="neck", rotation=[-50, 0, ang], cubes=[
        c([-2, 22, -14], [4, 5, 3], "fur_light" if i % 2 else "fur", "ruff")])

m.bone("head", p(0, 19, -17), parent="neck", rotation=[20, 0, 0], cubes=[
    c([-4.5, 15, -24], [9, 7.5, 7.5], "fur", "skull"),
    c([-5.5, 14.5, -21], [2, 5, 3], "fur_light", "cheek"),
    c([3.5, 14.5, -21], [2, 5, 3], "fur_light", "cheek"),
    c([-2.5, 15, -30], [5, 4, 6], "fur", "snout"),
    c([-1.5, 18, -30.5], [3, 1.5, 1], "nose", "nose"),
    c([-2.5, 13, -29], [5, 2, 5], "fur_light", "jaw"),
    c([-2, 14.5, -29.5], [1, 1.5, 1], "fang", "fang"),
    c([1, 14.5, -29.5], [1, 1.5, 1], "fang", "fang"),
])
EAR = [c([-4.5, 22, -20], [3, 2, 1.6], "fur", "ear"), c([-4, 24, -20], [2, 1.5, 1.4], "fur", "ear"),
       c([-3.5, 25.5, -20], [1, 1, 1.2], "fur_dark", "ear_tip")]
m.bone("ear_right", p(-3, 22, -19), parent="head", rotation=[-15, 0, 22], cubes=EAR)
m.bone("ear_left", p(3, 22, -19), parent="head", rotation=[-15, 0, -22], cubes=mirror(EAR))

# 首輪から垂れる千切れた鎖
CHAIN = [c([-0.5, 14 - i * 1.5, -17.5 + (i % 2) * 0.3], [1, 1.5, 0.6 + (i % 2) * 0.4], "chain", "link") for i in range(5)]
m.bone("chain_neck", p(0, 14.5, -17), parent="neck", cubes=CHAIN)

# 脚：前脚に鎖の枷と、垂れた鎖
FRONT = [c([-5, 0, -11], [4, 11, 4], "fur", "front_leg"), c([-5.5, 0, -12], [5, 2, 5], "fur_dark", "paw"),
         c([-5.5, 3, -11.5], [5, 1.5, 5], "chain", "shackle")]
FCHAIN = [c([-3.5, 3 - i * 1.4, -12 - i * 0.6], [1, 1.4, 0.8], "chain", "link") for i in range(3)]
HIND = [c([-5.5, 5, 9], [5, 8, 7], "fur", "thigh")]
HIND_LOW = [c([-5, 0, 13], [4, 6, 4], "fur", "shin"), c([-5.5, 0, 12], [5, 2, 5], "fur_dark", "paw")]
m.bone("front_right", p(-3, 11, -9), parent="body", cubes=FRONT)
m.bone("chain_front", p(-3, 3, -11), parent="front_right", cubes=FCHAIN)
m.bone("front_left", p(3, 11, -9), parent="body", cubes=mirror(FRONT))
m.bone("hind_right", p(-3, 11, 12), parent="body", cubes=HIND)
m.bone("hind_right_low", p(-3, 6, 14), parent="hind_right", cubes=HIND_LOW)
m.bone("hind_left", p(3, 11, 12), parent="body", cubes=mirror(HIND))
m.bone("hind_left_low", p(3, 6, 14), parent="hind_left", cubes=mirror(HIND_LOW))

# ふさふさの長い尻尾
m.bone("tail1", p(0, 17, 16), parent="body", rotation=[-30, 0, 0], cubes=[c([-1.5, 15.5, 16], [3, 3, 8], "fur", "tail")])
m.bone("tail2", p(0, 17, 24), parent="tail1", rotation=[20, 0, 0], cubes=[c([-2.2, 15, 24], [4.4, 4.4, 8], "fur", "tail_bush")])
m.bone("tail3", p(0, 17, 32), parent="tail2", rotation=[15, 0, 0], cubes=[c([-1.8, 15.3, 32], [3.6, 3.6, 5], "fur_light", "tail_tip")])

# ---------------------------------------------------------------- texture
SILVER_PAL = {
    "fur": ("#8f949c", "#b4b8c0", "#5a5e66"),
    "fur_light": ("#c9ccd2", "#e4e6ea", "#8e9198"),
    "fur_dark": ("#4a4e57", "#62666f", "#2a2d33"),
}
GOLD_PAL = {
    "fur": ("#f0bc38", "#ffd966", "#b07a10"),
    "fur_light": ("#ffe49a", "#fff4cc", "#d0a850"),
    "fur_dark": ("#b07a18", "#c88e26", "#6e4806"),
}
PAL = {
    **(GOLD_PAL if GOLD else SILVER_PAL),
    "nose": ("#1c1d22", "#2c2d33", "#0c0c10"),
    "fang": ("#f2efe4", "#ffffff", "#bdb8a8"),
    "chain": ("#5c5f66", "#8a8e96", "#2e3035"),
}
ICE = ([hexc("#a8141c"), hexc("#ff3a3a"), hexc("#ffc8b8")] if GOLD
       else [hexc("#3aa8d8"), hexc("#8fe8ff"), hexc("#e8fcff")])


SADDLE = ("#c48a1c", "#d09622") if GOLD else ("#5f636b", "#6b6f78")


def details(m, mat, tag, face, rect):
    fx, fy, fw, fh = rect
    r = m.rng
    if mat in ("fur", "fur_light", "fur_dark") and face != "down":
        # 毛並み：ところどころに短い毛の筋（細かいノイズは入れない）
        if GOLD:
            dark = {"fur": "#d49e26", "fur_light": "#f0cf78", "fur_dark": "#94640e"}[mat]
            lite = {"fur": "#ffe07a", "fur_light": "#fff8de", "fur_dark": "#d29a30"}[mat]
        else:
            dark = {"fur": "#7e838b", "fur_light": "#b3b6bd", "fur_dark": "#3e424a"}[mat]
            lite = {"fur": "#a4a8b0", "fur_light": "#dde0e5", "fur_dark": "#575b64"}[mat]
        for _ in range(fw * fh // 9):
            x, y = r.randrange(fw), r.randrange(fh)
            col = hexc(dark if r.random() < 0.6 else lite)
            for d in range(r.randrange(2, 4)):
                if y + d < fh:
                    m.put(fx + x, fy + y + d, col)
        # 背中側は濃い灰色（鞍のような模様）
        if mat == "fur" and tag in ("chest", "belly", "hips", "neck", "skull", "tail", "tail_bush"):
            if face == "up":
                for j in range(fh):
                    for k in range(fw):
                        if r.random() < 0.7:
                            m.put(fx + k, fy + j, hexc(r.choice(SADDLE)))
            elif face in ("west", "east"):
                for k in range(fw):
                    for j in range(r.choice((2, 3, 3, 4))):
                        m.put(fx + k, fy + j, hexc(r.choice(SADDLE)))
    # 肩と腰のルーン（氷のように光る）
    if tag in ("chest", "hips") and face in ("west", "east"):
        cx, cy = fw // 2, fh // 2
        for d in range(-2, 3):
            m.put(fx + cx + d, fy + cy, ICE[1], glow=True)
            m.put(fx + cx, fy + cy + d, ICE[1], glow=True)
        for d in (-2, 2):
            m.put(fx + cx + d, fy + cy - 2, ICE[0], glow=True)
            m.put(fx + cx + d, fy + cy + 2, ICE[0], glow=True)
        m.put(fx + cx, fy + cy, ICE[2], glow=True)
    # 顔：細く鋭い、氷のように光る目
    if tag == "skull" and face == "north":
        ye = fh // 2 - 1
        for k in (1, 2, fw - 3, fw - 2):
            m.put(fx + k, fy + ye, ICE[1], glow=True)
        m.put(fx + 2, fy + ye, ICE[2], glow=True)
        m.put(fx + fw - 3, fy + ye, ICE[2], glow=True)
        for k in (0, 1, 2, fw - 3, fw - 2, fw - 1):         # 眉間のしわ（暗く）
            m.put(fx + k, fy + ye - 1, hexc("#6a4810" if GOLD else "#3a3d44"))
    if tag == "snout" and face == "up":
        for j in range(fh):
            m.put(fx + fw // 2, fy + j, hexc("#a07424" if GOLD else "#6a6e77"))
    if tag == "jaw" and face == "north":
        for k in range(fw):
            m.put(fx + k, fy, hexc("#f2efe4") if k % 2 == 0 else hexc("#2a1a1a"))
    # 鎖：輪の模様
    if mat == "chain" and face != "down":
        for k in range(fw):
            for j in range(fh):
                if (k % 3 == 0) or (j % 2 == 0 and k % 3 == 1):
                    m.put(fx + k, fy + j, hexc("#3a3c42"))
                elif r.random() < 0.2:
                    m.put(fx + k, fy + j, hexc("#a8acb4"))


m.paint(PAL, details, glow_mats=set(), outline_skip={"fur", "fur_light", "fur_dark", "chain"},
        soft={"mats": {"fur", "fur_light", "fur_dark"}, "y": (0, 50), "dark": 0.7})
m.save()
