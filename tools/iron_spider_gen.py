"""鋼鉄の機甲蜘蛛（機械系ボス）：モデル＋テクスチャ＋発光マスク。 python3 tools/iron_spider_gen.py
6本脚の蜘蛛型の機械。鋼鉄の胴体に銅の配管とオレンジに光る配線、ドーム型の頭に赤く光るひとつ目、
両肩にミサイル砲台、前に2枚の回転ノコギリ。正面は -Z。幅 約4ブロック、高さ 約2.7ブロック。
"""
import os, sys
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from geo_tools import Model, mirror, hexc

m = Model("iron_spider", 256, 256, seed=7)

# ---------------------------------------------------------------- model
m.bone("root", [0, 0, 0])
m.bone("body", [0, 26, 0], parent="root", cubes=[
    ([-12, 20, -14], [24, 10, 26], "steel", "chassis"),
    ([-10, 30, -12], [20, 3, 22], "steel_dark", "top_plate"),
    ([-6, 17, -8], [12, 3, 14], "glow", "engine"),             # 腹のエンジン（光る）
    ([-13, 22, -10], [1, 6, 18], "copper", "side_pipe"),
    ([12, 22, -10], [1, 6, 18], "copper", "side_pipe"),
    ([-8, 21, 12], [16, 7, 4], "steel_dark", "rear_vent"),
])
# 背中の排気筒（蒸気が出る）
for i, x in enumerate((-6, 3)):
    m.bone(f"stack{i}", [x + 1.5, 33, 8], parent="body", cubes=[([x, 33, 6.5], [3, 6, 3], "copper", "stack")])

# ドーム型の頭とひとつ目
m.bone("head", [0, 31, -8], parent="body", cubes=[
    ([-6, 30, -15], [12, 8, 10], "steel", "dome"),
    ([-3, 32, -15.6], [6, 4, 1], "steel_dark", "eye_frame"),
    ([-2, 33, -16], [4, 2, 1], "eye", "eye"),
    ([4, 38, -9], [1, 5, 1], "steel_dark", "antenna"),
    ([4, 43, -9], [1, 1, 1], "eye", "antenna_tip"),
])

# 肩のミサイル砲台（ポッドに発射口が並ぶ）
POD = [([-17, 32, -5], [7, 6, 10], "steel_dark", "pod"),
       ([-16.5, 33, -5.6], [6, 4, 1], "glow", "pod_front"),
       ([-15, 29, -2], [3, 3, 4], "steel", "pod_mount")]
m.bone("pod_right", [-13.5, 31, 0], parent="body", cubes=POD)
m.bone("pod_left", [13.5, 31, 0], parent="body", cubes=mirror(POD))

# 前の回転ノコギリ（腕＋円盤）
ARM = [([-12, 22, -24], [3, 3, 11], "steel_dark", "saw_arm")]
SAW = [([-11.5, 16.5, -31.5], [1, 13, 13], "saw", "saw"),
       ([-12.5, 21, -27], [3, 4, 4], "steel", "saw_hub")]
m.bone("saw_arm_right", [-10.5, 23.5, -14], parent="body", cubes=ARM)
m.bone("saw_right", [-11, 23, -25], parent="saw_arm_right", cubes=SAW)
m.bone("saw_arm_left", [10.5, 23.5, -14], parent="body", cubes=mirror(ARM))
m.bone("saw_left", [11, 23, -25], parent="saw_arm_left", cubes=mirror(SAW))

# 6本の脚：胴から横へ伸びる上脚と、地面へ下りる下脚
for i, (z, yaw) in enumerate(((-8, 30), (2, 0), (11, -30))):
    UP = [([-28, 24, z - 1.5], [16, 3, 3], "steel", "leg_up"), ([-20, 23.5, z - 2], [2, 4, 4], "copper", "joint")]
    LOW = [([-29.5, 2, z - 1.5], [3, 23, 3], "steel_dark", "leg_low"), ([-30, 0, z - 2], [4, 3, 4], "steel", "foot"),
           ([-30, 22, z - 2], [4, 4, 4], "copper", "knee")]
    m.bone(f"leg_r{i}", [-12, 25.5, z], parent="body", rotation=[0, yaw, -30], cubes=UP)
    m.bone(f"leg_r{i}_low", [-28, 25, z], parent=f"leg_r{i}", rotation=[0, 0, 22], cubes=LOW)
    m.bone(f"leg_l{i}", [12, 25.5, z], parent="body", rotation=[0, -yaw, 30], cubes=mirror(UP))
    m.bone(f"leg_l{i}_low", [28, 25, z], parent=f"leg_l{i}", rotation=[0, 0, -22], cubes=mirror(LOW))

# ---------------------------------------------------------------- texture
PAL = {
    "steel": ("#7a8089", "#a2a8b0", "#454950"),
    "steel_dark": ("#454a52", "#5c626b", "#24272c"),
    "copper": ("#c4703c", "#e8955a", "#7a3e1c"),
    "glow": ("#ff9a2a", "#ffd27a", "#d0600c"),
    "eye": ("#ff2a2a", "#ffb0a0", "#a00a0a"),
    "saw": ("#b8bec6", "#e8ecf0", "#6a7078"),
}
GLOW = [hexc("#d0600c"), hexc("#ff9a2a"), hexc("#ffe0a0")]


def details(m, mat, tag, face, rect):
    fx, fy, fw, fh = rect
    r = m.rng
    if mat in ("steel", "steel_dark") and face != "down":
        # 鉄板：パネルの継ぎ目とリベット、ところどころの傷
        step = 6 if fw > 10 else max(3, fw // 2)
        for k in range(step, fw, step):
            for j in range(fh):
                m.put(fx + k, fy + j, hexc("#2c2f35" if mat == "steel" else "#1a1c20"))
        for k in range(1, fw - 1, step):
            for j in (1, fh - 2):
                if 0 <= j < fh:
                    m.put(fx + k, fy + j, hexc("#c8ccd2"))
        for _ in range(fw * fh // 30):
            m.put(fx + r.randrange(fw), fy + r.randrange(fh), hexc(r.choice(("#8a5a3a", "#5a5e66", "#b0b5bc"))))
        # 胴体の横と上を走る、オレンジに光る配線
        if tag in ("chassis", "top_plate") and face in ("north", "south", "east", "west", "up"):
            y = fh // 2
            x = 0
            while x < fw:
                ln = r.randrange(3, 8)
                for d in range(ln):
                    if x + d < fw:
                        m.put(fx + x + d, fy + y, GLOW[1], glow=True)
                x += ln
                if x < fw:
                    ny = max(1, min(fh - 2, y + r.choice((-2, 2))))
                    for yy in range(min(y, ny), max(y, ny) + 1):
                        m.put(fx + x, fy + yy, GLOW[1], glow=True)
                    y = ny
            m.put(fx + fw - 1, fy + y, GLOW[2], glow=True)
    if mat == "copper" and face != "down":
        for j in range(fh):
            for k in range(fw):
                if r.random() < 0.18:
                    m.put(fx + k, fy + j, hexc(r.choice(("#4fa38a", "#3e8c74"))))   # 緑青
                elif (j % 4) == 0:
                    m.put(fx + k, fy + j, hexc("#8a4a24"))
    if mat == "glow":
        for j in range(fh):
            for k in range(fw):
                col = GLOW[2] if (j + k) % 4 == 0 else GLOW[1]
                if tag == "pod_front" and (k % 2 == 1 and j % 2 == 1):
                    col = hexc("#1a1a1a")                                         # ミサイルの発射口
                    m.put(fx + k, fy + j, col)
                    continue
                m.put(fx + k, fy + j, col, glow=True)
    if mat == "eye":
        for j in range(fh):
            for k in range(fw):
                m.put(fx + k, fy + j, hexc("#ffe0d8") if abs(k - fw / 2 + 0.5) < 1 else hexc("#ff2a2a"), glow=True)
    if mat == "saw" and face in ("east", "west"):
        # ノコギリの円盤：外周にギザギザの歯、真ん中は暗い
        cx, cy = (fw - 1) / 2, (fh - 1) / 2
        for j in range(fh):
            for k in range(fw):
                d = ((k - cx) ** 2 + (j - cy) ** 2) ** 0.5
                if d > cx + 0.6:
                    m.clear(fx + k, fy + j)
                elif d > cx - 0.8:
                    m.put(fx + k, fy + j, hexc("#e8ecf0") if (k + j) % 2 else hexc("#8a9098"))
                elif d > cx - 2.5:
                    m.put(fx + k, fy + j, hexc("#a2a8b0"))


m.paint(PAL, details, glow_mats={"glow", "eye"}, outline_skip={"glow", "eye", "saw"})
m.save()
