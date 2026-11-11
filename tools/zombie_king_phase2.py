"""ゾンビの王・第2形態「不死の王」：赤く光る目と宝石を、毒々しい緑の光に変える。"""
import colorsys
import numpy as np
from PIL import Image

T = "src/main/resources/assets/jsnboss/textures/entity"
for src, dst in [("zombie_king.png", "zombie_king_phase2.png"), ("zombie_king_glowmask.png", "zombie_king_phase2_glowmask.png")]:
    im = np.array(Image.open(f"{T}/{src}").convert("RGBA")).astype(float)
    glow = np.array(Image.open(f"{T}/zombie_king_glowmask.png").convert("RGBA"))[..., 3] > 0
    for y, x in zip(*np.nonzero(glow & (im[..., 3] > 0))):
        r, g, b = im[y, x, :3] / 255
        h, l, s = colorsys.rgb_to_hls(r, g, b)
        h = 0.3                                   # 赤 → 黄緑
        l = min(1.0, l * 1.1)
        im[y, x, :3] = np.array(colorsys.hls_to_rgb(h, l, s)) * 255
    Image.fromarray(im.astype(np.uint8)).save(f"{T}/{dst}")
print("ok")

# 肋骨のすき間から緑の光が漏れる（第2形態だけ）
import json
faces = json.load(open("tools/zombie_king_faces.json"))["flat"]
rect = next(f[4] for b, f, mat in faces if b == "torso" and f[0] == "north" and mat == "skin" and f[4][2:] == [12, 11])
fx, fy, fw, fh = rect
tex = np.array(Image.open(f"{T}/zombie_king_phase2.png").convert("RGBA"))
glw = np.array(Image.open(f"{T}/zombie_king_phase2_glowmask.png").convert("RGBA"))
for j in range(2, fh - 1, 2):
    for k in range(1, fw - 1):
        if k in (fw // 2 - 1, fw // 2):
            continue                                   # 背骨はそのまま
        center = 1 - abs(k - (fw - 1) / 2) / (fw / 2)  # 真ん中ほど明るい
        c = np.array([90, 200, 60]) + center * np.array([110, 55, 60])
        tex[fy + j, fx + k, :3] = c; tex[fy + j, fx + k, 3] = 255
        glw[fy + j, fx + k, :3] = c; glw[fy + j, fx + k, 3] = 255
Image.fromarray(tex).save(f"{T}/zombie_king_phase2.png")
Image.fromarray(glw).save(f"{T}/zombie_king_phase2_glowmask.png")
print("ribs glow", rect)
