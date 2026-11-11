"""闇の魔導士・第2形態「深淵」：紫の光とルーンを、血のような深紅に変える。"""
import colorsys
import numpy as np
from PIL import Image

T = "src/main/resources/assets/jsnboss/textures/entity"
for src, dst in [("dark_mage.png", "dark_mage_phase2.png"), ("dark_mage_glowmask.png", "dark_mage_phase2_glowmask.png")]:
    im = np.array(Image.open(f"{T}/{src}").convert("RGBA")).astype(float)
    for y, x in zip(*np.nonzero(im[..., 3] > 0)):
        r, g, b = im[y, x, :3] / 255
        h, l, s = colorsys.rgb_to_hls(r, g, b)
        if 0.68 < h < 0.85 and s > 0.4 and l > 0.15:   # 明るい紫だけ（暗いローブはそのまま）
            h = 0.985                                    # → 深紅
            s = min(1.0, s * 1.1)
            im[y, x, :3] = np.array(colorsys.hls_to_rgb(h, l, s)) * 255
    Image.fromarray(im.astype(np.uint8)).save(f"{T}/{dst}")
print("ok")
