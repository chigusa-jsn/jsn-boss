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
