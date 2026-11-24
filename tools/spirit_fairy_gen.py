"""大詠唱で呼ばれる妖精：モデル＋属性ごとのテクスチャ。 python3 tools/spirit_fairy_gen.py
羽の生えた小さな妖精（高さ約0.9ブロック）。色だけ属性ごとに変えたテクスチャを作る（炎・氷・雷・木…8種）。
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
    B([-1.2, 6, -0.8], [2.4, 3, 1.6], "dress", "bodice"),
    B([-1.9, 3.2, -1.3], [3.8, 3, 2.6], "dress", "skirt"),
    B([-2.3, 2.4, -1.7], [4.6, 1.2, 3.4], "accent", "hem"),
    B([-0.4, 7.6, -0.95], [0.8, 0.8, 0.4], "gem", "gem"),
])
ARM = [B([-2.0, 5.6, -0.4], [0.8, 3.2, 0.8], "skin", "arm")]
m.bone("right_arm", [-1.6, 8.6, 0], parent="body", cubes=ARM)
m.bone("left_arm", [1.6, 8.6, 0], parent="body", cubes=mirror(ARM))
m.bone("head", [0, 9, 0], parent="body", cubes=[
    B([-1.8, 9, -1.8], [3.6, 3.6, 3.6], "skin", "face"),
    B([-2.0, 11.4, -2.0], [4.0, 1.6, 4.0], "hair", "hair_cap"),
    B([-2.0, 8.4, 0.4], [4.0, 3.4, 1.8], "hair", "hair_back"),
    B([-2.05, 10.6, -2.05], [4.1, 0.9, 0.6], "hair", "bangs"),
    B([-0.5, 13.0, -0.5], [1.0, 1.0, 1.0], "gem", "tiara", rot=[0, 45, 0]),
])
WING = [
    B([-6.4, 7.6, 1.0], [5.4, 3.2, 0.2], "wing", "wing_up", rot=[0, 0, -28], pivot=[-1, 8.4, 1.1]),
    B([-4.4, 5.2, 1.0], [3.6, 2.2, 0.2], "wing", "wing_low", rot=[0, 0, 22], pivot=[-1, 7.4, 1.1]),
]
m.bone("wing_right", [-0.8, 8, 1.0], parent="body", rotation=[0, 20, 0], cubes=WING)
m.bone("wing_left", [0.8, 8, 1.0], parent="body", rotation=[0, -20, 0],
       cubes=[c[:3] + (c[3] + "_l",) + c[4:] for c in mirror(WING)])

PAL = {
    "skin": ("#f4e0d2", "#fff0e6", "#c8a898"),
    "hair": ("#dcdcdc", "#ffffff", "#9a9a9a"),
    "dress": ("#e8e8e8", "#ffffff", "#a8a8a8"),
    "accent": ("#c8c8c8", "#f0f0f0", "#888888"),
    "gem": ("#ffffff", "#ffffff", "#d0d0d0"),
    "wing": ("#ffffff", "#ffffff", "#e0e0e0"),
}


def details(m, mat, tag, face, rect):
    fx, fy, fw, fh = rect
    if mat == "wing":
        for j in range(fh):
            for k in range(fw):
                edge = j in (0, fh - 1) or k in (0, fw - 1)
                m.tex[fy + j, fx + k] = (255, 255, 255, 230 if edge else 130)
                m.glow[fy + j, fx + k] = (255, 255, 255, 255) if edge or (j + k) % 5 == 0 else (0, 0, 0, 0)
    if mat == "gem":
        for j in range(fh):
            for k in range(fw):
                m.glow[fy + j, fx + k] = (255, 255, 255, 255)
    if tag == "face" and face == "north":
        ey = fh - 4
        for ex in (fw // 2 - 3, fw // 2 + 2):
            m.put(fx + ex, fy + ey, hexc("#ffffff"), glow=True)
            m.put(fx + ex, fy + ey + 1, hexc("#ffffff"), glow=True)


m.paint(PAL, details, glow_mats={"gem", "wing"}, outline_skip={"wing", "gem", "skin", "hair", "dress"})
m.save()

# 属性ごとに色をのせる（白・灰の部分にだけ色がつく。肌はそのまま）
TINT = {"fire": "#ff6a30", "ice": "#7ad8ff", "wind": "#8affb8", "thunder": "#ffe040",
        "earth": "#d09a5a", "dark": "#a050ff", "light": "#fff0a0", "wood": "#60e070"}
base = np.array(Image.open(f"{ASSETS}/textures/entity/spirit_fairy.png").convert("RGBA")).astype(float)
gl = np.array(Image.open(f"{ASSETS}/textures/entity/spirit_fairy_glowmask.png").convert("RGBA")).astype(float)
rgb = base[..., :3]
gray = (np.abs(rgb[..., 0] - rgb[..., 1]) < 12) & (np.abs(rgb[..., 1] - rgb[..., 2]) < 12)
for name, h in TINT.items():
    c = hexc(h)[:3].astype(float) / 255
    out = base.copy()
    lum = rgb.mean(axis=2, keepdims=True) / 255
    tinted = np.clip(lum * c * 255 * 1.15 + lum * 40, 0, 255)
    out[..., :3] = np.where(gray[..., None], tinted, rgb)
    Image.fromarray(out.astype(np.uint8)).save(f"{ASSETS}/textures/entity/spirit_fairy_{name}.png")
    g = gl.copy()
    gt = np.clip(g[..., :3] / 255 * (c * 0.6 + 0.4) * 255, 0, 255)
    g[..., :3] = gt
    Image.fromarray(g.astype(np.uint8)).save(f"{ASSETS}/textures/entity/spirit_fairy_{name}_glowmask.png")
print("tinted", len(TINT))
