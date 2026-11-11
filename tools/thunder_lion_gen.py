"""雷の獅子：モデル＋テクスチャ＋発光マスク。 python3 tools/thunder_lion_gen.py
四つ足の大きなライオン。正面は -Z。全体を K 倍の大きさで作る。
"""
import os, sys
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from geo_tools import Model, mirror, hexc

K = 1.7
m = Model("thunder_lion", 256, 256, seed=71)


def c(o, s, mat, tag):
    return ([round(v * K * 2) / 2 for v in o], [max(1, round(v * K)) for v in s], mat, tag)


def p(*xyz):
    return [round(v * K * 2) / 2 for v in xyz]


# ---------------------------------------------------------------- model
m.bone("root", [0, 0, 0])
m.bone("body", p(0, 14, 0), parent="root", cubes=[
    c([-6, 9, -12], [12, 11, 12], "fur", "chest"),
    c([-5.5, 9.5, 0], [11, 9, 10], "fur", "belly"),
    c([-5.5, 9.5, 10], [11, 10, 7], "fur", "hips"),
    c([-4.5, 8.5, -10], [9, 1, 20], "fur_light", "underside"),
])

m.bone("neck", p(0, 17, -12), parent="body", rotation=[-15, 0, 0], cubes=[
    c([-4.5, 13, -17], [9, 9, 7], "mane", "neck"),
])
m.bone("head", p(0, 18, -17), parent="neck", rotation=[15, 0, 0], cubes=[
    c([-4.5, 14, -24], [9, 8, 8], "fur", "skull"),
    c([-3, 14, -27], [6, 5, 3], "fur_light", "muzzle"),
    c([-1.5, 17.5, -27.5], [3, 2, 1], "nose", "nose"),
    c([-2.5, 12.5, -26], [5, 2, 5], "fur_light", "jaw"),
    c([-4.5, 22, -20], [2, 2, 1], "fur", "ear"),
    c([2.5, 22, -20], [2, 2, 1], "fur", "ear"),
])
# 大きなたてがみ（顔のまわり）
m.bone("mane", p(0, 18, -19), parent="head", cubes=[
    c([-7, 11, -21], [14, 14, 7], "mane", "mane"),
    c([-5, 9, -21], [10, 3, 6], "mane", "mane_chin"),
])
# たてがみから放射状に突き出す稲妻のトゲ（先が光る）
BOLT = 0
for ring, (angles, lean, z, r0, length) in enumerate((
        ([0, 30, -30, 60, -60, 90, -90, 120, -120, 145, -145], -18, -19, 24, 6),
        ([15, -15, 45, -45, 75, -75, 105, -105, 135, -135], -40, -16, 22, 5))):
    for ang in angles:
        m.bone(f"bolt{BOLT}", p(0, 18, z + 1), parent="mane", rotation=[lean, 0, ang], cubes=[
            c([-1, r0, z], [2, 3, 2], "mane", "spike"),
            c([-0.5, r0 + 3, z + 0.5], [1, length // 2, 1], "bolt", "spike_mid"),
            c([0, r0 + 3 + length // 2, z + 0.5], [1, length - length // 2, 1], "bolt", "spike_tip"),
            c([-1, r0 + 3 + length // 2 - 1, z + 0.5], [1, 1, 1], "bolt", "spike_kink"),
        ])
        BOLT += 1

# 体の周りに浮かぶジグザグの電撃（アニメーションで明滅させる）
def arc(x, y, z, n, dz):
    """ジグザグに折れ曲がる電撃（小さな光るブロックを互い違いに並べる）"""
    out = []
    for i in range(n):
        out.append(c([x + (0.6 if i % 2 else -0.6), y + i * 1.2, z + i * dz], [1, 1.3, 1], "bolt", "arc"))
    return out

ARCS = [((-7.5, 16, -8), 5, 0.6), ((7, 17, -5), 5, -0.5), ((-7, 15, 6), 4, 0.8), ((7, 16, 9), 4, -0.6),
        ((-2, 21, 2), 4, 1.0), ((2, 21, 10), 3, -1.0)]
for i, ((x, y, z), n, dz) in enumerate(ARCS):
    m.bone(f"arc{i}", p(x, y, z), parent="body", cubes=arc(x, y, z, n, dz))

# 脚
FRONT = [c([-5.5, 0, -10], [4, 9, 4], "fur", "front_leg"), c([-6, 0, -11], [5, 2, 5], "fur_light", "paw")]
HIND = [c([-6, 4, 9], [5, 8, 7], "fur", "thigh")]
HIND_LOW = [c([-5.5, 0, 12], [4, 5, 4], "fur", "shin"), c([-6, 0, 11], [5, 2, 5], "fur_light", "paw")]
m.bone("front_right", p(-3.5, 9, -8), parent="body", cubes=FRONT)
m.bone("front_left", p(3.5, 9, -8), parent="body", cubes=mirror(FRONT))
m.bone("hind_right", p(-3.5, 10, 12), parent="body", cubes=HIND)
m.bone("hind_right_low", p(-3.5, 5, 13), parent="hind_right", cubes=HIND_LOW)
m.bone("hind_left", p(3.5, 10, 12), parent="body", cubes=mirror(HIND))
m.bone("hind_left_low", p(3.5, 5, 13), parent="hind_left", cubes=mirror(HIND_LOW))

# 尻尾：下へ垂れてから先が上がる。先のふさに雷
m.bone("tail1", p(0, 15, 17), parent="body", rotation=[-25, 0, 0], cubes=[c([-1, 14, 17], [2, 2, 9], "fur", "tail")])
m.bone("tail2", p(0, 15, 26), parent="tail1", rotation=[35, 0, 0], cubes=[c([-1, 14, 26], [2, 2, 8], "fur", "tail")])
m.bone("tail_tuft", p(0, 15, 34), parent="tail2", rotation=[15, 0, 0], cubes=[
    c([-2, 13, 34], [4, 4, 4], "mane", "tuft"),
    c([-0.5, 17, 35], [1, 2, 1], "bolt", "tuft_bolt"),
    c([-0.5, 12, 36], [1, 1, 2], "bolt", "tuft_bolt"),
])

# ---------------------------------------------------------------- texture
PAL = {
    "fur": ("#c8923a", "#e2b25e", "#7a5216"),
    "fur_light": ("#f1d79a", "#fbe9bf", "#b89a5a"),
    "mane": ("#161b30", "#243052", "#0a0d18"),
    "nose": ("#3a2620", "#4e352c", "#1a100c"),
    "bolt": ("#7fe3ff", "#ffffff", "#2fa8e0"),
}
ELEC = [hexc("#2fa8e0"), hexc("#7fe3ff"), hexc("#e8fbff")]


def zigzag(m, fx, fy, fw, fh, x0, length):
    """稲妻のジグザグ線（光る）"""
    x, y = x0, 0
    for s in range(length):
        if 0 <= x < fw and 0 <= y < fh:
            m.put(fx + x, fy + y, ELEC[2] if s % 3 == 0 else ELEC[1], glow=True)
        y += 1
        x += 1 if (s // 2) % 2 == 0 else -1


def details(m, mat, tag, face, rect):
    fx, fy, fw, fh = rect
    r = m.rng
    if mat == "fur" and face not in ("down",):
        for _ in range(max(1, fw * fh // 12)):
            m.put(fx + r.randrange(fw), fy + r.randrange(fh), hexc("#c48f32"))
    # 肩と腰の横に光る稲妻のライン
    if tag in ("chest", "hips", "belly") and face in ("west", "east"):
        zigzag(m, fx, fy, fw, fh, fw // 4, fh)
        zigzag(m, fx, fy, fw, fh, fw // 2, fh - 2)
        zigzag(m, fx, fy, fw, fh, 3 * fw // 4, fh - 4)
    if tag in ("front_leg", "thigh", "shin", "tail") and face in ("west", "east", "north", "south"):
        zigzag(m, fx, fy, fw, fh, fw // 2, fh)
    if tag in ("chest", "belly", "hips") and face == "up":
        for j in range(fh):                               # 背骨にそって走る稲妻
            m.put(fx + fw // 2 + (1 if (j // 2) % 2 else -1), fy + j, ELEC[1] if j % 3 else ELEC[2], glow=True)
    if tag == "belly" and face == "up":
        for j in range(1, fh, 5):
            for k in range(fw // 2 - 1, fw // 2 + 1):
                m.put(fx + k, fy + j, ELEC[1], glow=True)
    # たてがみ：毛の流れと、ところどころ走る電気
    if mat == "mane" and face not in ("down",):
        for k in range(0, fw, 2):
            for j in range(fh):
                if r.random() < 0.5:
                    m.put(fx + k, fy + j, hexc("#0f1324"))
        for _ in range(max(1, fw * fh // 14)):           # たてがみを走る青い電気
            x0, y0 = r.randrange(fw), r.randrange(fh)
            for s_ in range(3):
                if 0 <= x0 < fw and 0 <= y0 + s_ < fh:
                    m.put(fx + x0, fy + y0 + s_, ELEC[1] if s_ != 1 else ELEC[2], glow=True)
                x0 += r.choice((-1, 1))
    # 顔：青く光る目、鼻筋
    if tag == "skull" and face == "north":
        ye = fh // 2 - 1
        for k in (1, 2, fw - 3, fw - 2):
            m.put(fx + k, fy + ye, ELEC[1], glow=True)
        m.put(fx + 2, fy + ye, ELEC[2], glow=True)
        m.put(fx + fw - 3, fy + ye, ELEC[2], glow=True)
        for j in range(ye + 1, fh):
            m.put(fx + fw // 2, fy + j, hexc("#c48f32"))
    if tag == "jaw" and face == "north":
        for k in range(1, fw - 1, 2):
            m.put(fx + k, fy, hexc("#ffffff"))
    if mat == "bolt":
        for j in range(fh):
            for k in range(fw):
                m.put(fx + k, fy + j, ELEC[(j + k) % 3], glow=True)


m.paint(PAL, details, glow_mats={"bolt"}, outline_skip={"fur", "fur_light", "mane", "bolt"},
        soft={"mats": {"fur", "fur_light", "mane"}, "y": (0, 45), "dark": 0.72})
m.save()
