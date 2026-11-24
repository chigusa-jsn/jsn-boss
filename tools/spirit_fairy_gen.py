"""大詠唱で呼ばれる妖精：モデル＋属性ごとのテクスチャ。 python3 tools/spirit_fairy_gen.py
細身の小さな妖精（高さ約1ブロック）。ふわっとした髪、蝶のような半透明の羽。
色は属性ごとに変えたテクスチャを作る（髪・服の白〜灰の部分にだけ色がのる）。
頭の小物も属性ごと（モデルで、その属性のもの以外を隠す）：
  炎＝頭の炎、氷＝氷の冠、風＝羽根の髪飾り、雷＝稲妻の髪飾り、土＝琥珀のサークレット、
  闇＝小さなコウモリの羽のカチューシャ、光＝頭の上の光輪、木＝葉っぱの帽子
"""
import os, sys
import numpy as np
from PIL import Image
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from geo_tools import Model, mirror, hexc, ASSETS

m = Model("spirit_fairy", 128, 128, seed=5, density=2)


def B(o, s, mat, tag, rot=None, pivot=None):
    x = {}
    if rot: x["rot"] = list(rot)
    if pivot: x["pivot"] = list(pivot)
    return (list(o), list(s), mat, tag, x) if x else (list(o), list(s), mat, tag)


m.bone("root", [0, 0, 0])
m.bone("body", [0, 6, 0], parent="root", cubes=[
    B([-0.9, 6.0, -0.6], [1.8, 2.8, 1.2], "dress", "bodice"),
    B([-1.2, 4.6, -0.85], [2.4, 1.6, 1.7], "dress", "skirt_top"),
    B([-1.7, 3.2, -1.25], [3.4, 1.5, 2.5], "dress", "skirt_low"),
    B([-1.9, 2.8, -1.4], [3.8, 0.5, 2.8], "accent", "hem"),
    B([-1.0, 6.6, -0.75], [2.0, 0.5, 0.3], "accent", "ribbon"),
    B([-0.35, 7.6, -0.8], [0.7, 0.7, 0.3], "gem", "gem"),
    # 細い脚（スカートの下からちょこんと）
    B([-0.7, 0.6, -0.3], [0.6, 2.4, 0.6], "skin", "leg"),
    B([0.1, 0.9, -0.3], [0.6, 2.1, 0.6], "skin", "leg"),
    B([-0.75, 0.3, -0.45], [0.7, 0.5, 0.85], "accent", "shoe"),
    B([0.05, 0.6, -0.45], [0.7, 0.5, 0.85], "accent", "shoe"),
])
ARM = [B([-1.55, 5.8, -0.3], [0.6, 2.9, 0.6], "skin", "arm"),
       B([-1.65, 8.0, -0.4], [0.8, 0.7, 0.8], "dress", "puff")]
m.bone("right_arm", [-1.2, 8.6, 0], parent="body", cubes=ARM)
m.bone("left_arm", [1.2, 8.6, 0], parent="body", cubes=mirror(ARM))

# 頭：顔は大きめ。髪はふわっと（大きめの髪、横のふくらみ、後ろに流れる髪、アホ毛）
m.bone("head", [0, 9, 0], parent="body", cubes=[
    B([-1.7, 9.0, -1.7], [3.4, 3.2, 3.4], "skin", "face"),
    B([-2.1, 11.3, -2.1], [4.2, 1.6, 4.2], "hair", "hair_cap"),
    B([-1.95, 12.6, -1.6], [3.9, 0.7, 3.4], "hair", "hair_top"),
    B([-1.95, 10.5, -2.15], [3.9, 1.0, 0.6], "hair", "bangs"),
    B([-2.6, 8.6, -1.4], [1.1, 3.4, 2.8], "hair", "puff_side"),
    B([1.5, 8.6, -1.4], [1.1, 3.4, 2.8], "hair", "puff_side"),
    B([-2.2, 7.8, 0.5], [4.4, 4.4, 1.8], "hair", "hair_back"),
    B([-1.9, 6.4, 1.1], [3.8, 1.8, 1.3], "hair", "hair_back_low", rot=[10, 0, 0], pivot=[0, 8, 1.2]),
    B([-2.9, 9.2, 0.2], [1.2, 1.6, 1.4], "hair", "puff_curl", rot=[0, 0, 20], pivot=[-2.4, 10, 0.8]),
    B([1.7, 9.2, 0.2], [1.2, 1.6, 1.4], "hair", "puff_curl", rot=[0, 0, -20], pivot=[2.4, 10, 0.8]),
    B([-0.2, 13.1, -0.6], [0.4, 1.4, 0.4], "hair", "ahoge", rot=[-25, 0, 15], pivot=[0, 13.2, -0.4]),
])

# 属性の小物（ひとつだけ見せる）
FX = {
    "acc_fire": [   # 頭の炎
        B([-0.9, 13.0, -0.5], [1.8, 1.4, 1.2], "fire", "flame"),
        B([-0.6, 14.2, -0.35], [1.2, 1.2, 0.9], "fire", "flame"),
        B([-0.35, 15.2, -0.2], [0.7, 1.0, 0.6], "fire", "flame_tip"),
        B([0.6, 13.6, -0.2], [0.6, 1.0, 0.5], "fire", "flame_tip", rot=[0, 0, -20], pivot=[0.8, 13.6, 0]),
    ],
    "acc_ice": [    # 氷の冠
        B([-1.9, 12.8, -1.9], [3.8, 0.5, 3.8], "ice", "crown_band"),
        B([-0.35, 13.2, -2.0], [0.7, 1.8, 0.5], "ice", "crown_spike", rot=[-10, 0, 0], pivot=[0, 13.2, -1.8]),
        B([-1.5, 13.2, -1.9], [0.6, 1.2, 0.5], "ice", "crown_spike", rot=[-10, 0, 15], pivot=[-1.2, 13.2, -1.8]),
        B([0.9, 13.2, -1.9], [0.6, 1.2, 0.5], "ice", "crown_spike", rot=[-10, 0, -15], pivot=[1.2, 13.2, -1.8]),
        B([-1.95, 13.2, -0.3], [0.5, 1.0, 0.6], "ice", "crown_spike"),
        B([1.45, 13.2, -0.3], [0.5, 1.0, 0.6], "ice", "crown_spike"),
    ],
    "acc_wind": [   # 羽根の髪飾り
        B([-3.0, 11.6, -0.6], [0.5, 3.6, 1.4], "feather", "feather", rot=[0, 0, 25], pivot=[-2.6, 11.6, 0]),
        B([-3.5, 11.4, 0.2], [0.5, 3.0, 1.2], "feather", "feather", rot=[0, 0, 45], pivot=[-2.6, 11.4, 0.5]),
        B([-2.7, 11.0, -0.5], [0.8, 0.8, 0.8], "windgem", "pin"),
    ],
    "acc_thunder": [    # 稲妻の髪飾り
        B([1.5, 12.6, -0.5], [0.5, 1.4, 0.5], "bolt", "bolt", rot=[0, 0, -25], pivot=[1.7, 12.6, -0.3]),
        B([1.9, 13.6, -0.5], [0.5, 1.4, 0.5], "bolt", "bolt", rot=[0, 0, 25], pivot=[2.1, 13.6, -0.3]),
        B([1.5, 14.6, -0.5], [0.5, 1.2, 0.5], "bolt", "bolt", rot=[0, 0, -25], pivot=[1.7, 14.6, -0.3]),
    ],
    "acc_earth": [      # 琥珀のサークレット
        B([-2.15, 11.0, -2.15], [4.3, 0.5, 4.3], "stone", "circlet"),
        B([-0.5, 10.9, -2.4], [1.0, 1.0, 0.5], "amber", "amber"),
        B([-2.3, 10.8, -0.5], [0.5, 0.9, 1.0], "amber", "amber"),
        B([1.8, 10.8, -0.5], [0.5, 0.9, 1.0], "amber", "amber"),
    ],
    "acc_dark": [       # 小さなコウモリの羽のカチューシャ
        B([-2.0, 12.6, -0.3], [4.0, 0.4, 0.6], "darkband", "band"),
        B([-3.4, 12.8, -0.2], [1.8, 1.4, 0.3], "batwing", "batwing", rot=[0, 0, 20], pivot=[-1.8, 13, 0]),
        B([1.6, 12.8, -0.2], [1.8, 1.4, 0.3], "batwing", "batwing", rot=[0, 0, -20], pivot=[1.8, 13, 0]),
    ],
    "acc_light": [      # 頭の上の光輪
        B([-1.6, 15.0, -1.6], [3.2, 0.3, 0.4], "halo", "halo"),
        B([-1.6, 15.0, 1.2], [3.2, 0.3, 0.4], "halo", "halo"),
        B([-1.6, 15.0, -1.2], [0.4, 0.3, 2.4], "halo", "halo"),
        B([1.2, 15.0, -1.2], [0.4, 0.3, 2.4], "halo", "halo"),
    ],
    "acc_wood": [       # 葉っぱの帽子
        B([-2.3, 12.6, -2.3], [4.6, 0.6, 4.6], "leaf", "hat_brim"),
        B([-1.5, 13.2, -1.5], [3.0, 1.2, 3.0], "leaf", "hat_top"),
        B([-0.15, 14.4, -0.15], [0.3, 0.9, 0.3], "stem", "stem", rot=[0, 0, 20], pivot=[0, 14.4, 0]),
        B([0.1, 14.9, -0.4], [1.2, 0.3, 0.8], "leaf", "hat_leaf", rot=[0, 30, -15], pivot=[0.2, 15, 0]),
        B([1.4, 12.8, -2.4], [0.8, 0.8, 0.5], "flower", "hat_flower"),
    ],
}
for name, cubes in FX.items():
    m.bone(name, [0, 12, 0], parent="head", cubes=cubes)

# 蝶のような羽（上の大きな羽と下の小さな羽）
WING = [
    B([-6.0, 8.0, 0.9], [5.0, 4.2, 0.2], "wing", "wing_up", rot=[0, 0, -22], pivot=[-1, 8.6, 1.0]),
    B([-4.4, 4.8, 0.9], [3.6, 3.0, 0.2], "wing", "wing_low", rot=[0, 0, 26], pivot=[-1, 7.6, 1.0]),
]
m.bone("wing_right", [-0.6, 8, 0.9], parent="body", rotation=[0, 20, 0], cubes=WING)
m.bone("wing_left", [0.6, 8, 0.9], parent="body", rotation=[0, -20, 0],
       cubes=[c[:3] + (c[3] + "_l",) + c[4:] for c in mirror(WING)])

PAL = {
    "skin": ("#f6e4d6", "#fff2e8", "#d4b4a4"),
    "hair": ("#e0e0e0", "#ffffff", "#9a9a9a"),
    "dress": ("#e8e8e8", "#ffffff", "#a8a8a8"),
    "accent": ("#c8c8c8", "#f0f0f0", "#888888"),
    "gem": ("#ffffff", "#ffffff", "#d0d0d0"),
    "wing": ("#ffffff", "#ffffff", "#e0e0e0"),
    # 小物（色つき：属性の色づけの影響を受けない）
    "fire": ("#ff7a2a", "#ffe080", "#d03a10"),
    "ice": ("#a8e8ff", "#ffffff", "#5ab0e0"),
    "feather": ("#c8ffe0", "#ffffff", "#7ad8a8"),
    "windgem": ("#5ae0a0", "#c8ffe8", "#2a9a6a"),
    "bolt": ("#ffe040", "#fffbd0", "#c8a010"),
    "stone": ("#9a8a70", "#c8b898", "#5a4a38"),
    "amber": ("#ffa830", "#ffe0a0", "#c86a10"),
    "darkband": ("#3a2a4a", "#5a4a6a", "#1a1024"),
    "batwing": ("#4a2a6a", "#8a5ab0", "#24103a"),
    "halo": ("#fff4b0", "#ffffff", "#e8c860"),
    "leaf": ("#5ac85a", "#9af09a", "#2a8a3a"),
    "stem": ("#8a6a3a", "#b08a50", "#5a4020"),
    "flower": ("#ffd0e0", "#ffffff", "#e898b8"),
}
GLOW = {"gem", "wing", "fire", "ice", "bolt", "halo", "windgem", "amber", "feather"}


def details(m, mat, tag, face, rect):
    fx, fy, fw, fh = rect
    if mat == "wing":
        # 蝶の羽：半透明で縁と模様が光る。角を丸く抜く
        for j in range(fh):
            for k in range(fw):
                corner = (j in (0, fh - 1)) and (k in (0, fw - 1))
                if corner:
                    m.tex[fy + j, fx + k] = (0, 0, 0, 0); m.glow[fy + j, fx + k] = (0, 0, 0, 0)
                    continue
                edge = j in (0, fh - 1) or k in (0, fw - 1)
                spot = (j - fh / 2) ** 2 + (k - fw / 2) ** 2 < (min(fw, fh) / 4) ** 2
                m.tex[fy + j, fx + k] = (255, 255, 255, 235 if edge else (200 if spot else 120))
                m.glow[fy + j, fx + k] = (255, 255, 255, 255) if (edge or spot) else (0, 0, 0, 0)
    elif mat in GLOW:
        for j in range(fh):
            for k in range(fw):
                c = m.tex[fy + j, fx + k]
                m.glow[fy + j, fx + k] = (int(c[0]), int(c[1]), int(c[2]), 255)
    if mat == "hair" and face != "down":
        for k in range(fw):        # ふわっとした髪の流れ
            if k % 3 == 1:
                for j in range(fh):
                    m.put(fx + k, fy + j, hexc("#c4c4c4"))
            if k % 4 == 0 and fh > 2:
                m.put(fx + k, fy + 1, hexc("#ffffff"))
    if tag == "face" and face == "north":
        ey = fh - 3
        for ex in (fw // 2 - 3, fw // 2 + 1):          # 大きめの目（2×2）
            m.put(fx + ex, fy + ey - 1, hexc("#ffffff"))                  # 目の光
            m.put(fx + ex + 1, fy + ey - 1, hexc("#3a4a8a"))
            m.put(fx + ex, fy + ey, hexc("#5a7ac8"))
            m.put(fx + ex + 1, fy + ey, hexc("#2a3466"))
            for dx in (-1, 0, 1, 2):
                m.put(fx + ex + dx, fy + ey - 2, hexc("#2a2440"))          # まつげ
        m.put(fx + fw // 2 - 4, fy + ey + 1, hexc("#f4b8b8"))
        m.put(fx + fw // 2 + 3, fy + ey + 1, hexc("#f4b8b8"))
        m.put(fx + fw // 2 - 1, fy + ey + 2, hexc("#e8a8a0"))


m.paint(PAL, details, glow_mats=GLOW, outline_skip=set(PAL))
m.save()

# 属性ごとに色をのせる（白・灰の部分にだけ色がつく。肌・小物はそのまま）
TINT = {"fire": "#ff6a30", "ice": "#7ad8ff", "wind": "#8affb8", "thunder": "#ffe040",
        "earth": "#d09a5a", "dark": "#a050ff", "light": "#fff0a0", "wood": "#60e070"}
base = np.array(Image.open(f"{ASSETS}/textures/entity/spirit_fairy.png").convert("RGBA")).astype(float)
gl = np.array(Image.open(f"{ASSETS}/textures/entity/spirit_fairy_glowmask.png").convert("RGBA")).astype(float)
rgb = base[..., :3]
gray = (np.abs(rgb[..., 0] - rgb[..., 1]) < 12) & (np.abs(rgb[..., 1] - rgb[..., 2]) < 12)
ggray = (np.abs(gl[..., 0] - gl[..., 1]) < 12) & (np.abs(gl[..., 1] - gl[..., 2]) < 12)
for name, h in TINT.items():
    c = hexc(h)[:3].astype(float) / 255
    out = base.copy()
    lum = rgb.mean(axis=2, keepdims=True) / 255
    tinted = np.clip(lum * c * 255 * 1.15 + lum * 40, 0, 255)
    out[..., :3] = np.where(gray[..., None], tinted, rgb)
    Image.fromarray(out.astype(np.uint8)).save(f"{ASSETS}/textures/entity/spirit_fairy_{name}.png")
    g = gl.copy()
    g[..., :3] = np.where(ggray[..., None], np.clip(g[..., :3] / 255 * (c * 0.6 + 0.4) * 255, 0, 255), g[..., :3])
    Image.fromarray(g.astype(np.uint8)).save(f"{ASSETS}/textures/entity/spirit_fairy_{name}_glowmask.png")
print("tinted", len(TINT))
