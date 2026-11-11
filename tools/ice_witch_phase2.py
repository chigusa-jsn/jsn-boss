"""氷の魔女・第2形態（吹雪）のテクスチャ：水色の氷の光をオーロラっぽい紫に変える。"""
import colorsys
import numpy as np
from PIL import Image

T = "src/main/resources/assets/jsnboss/textures/entity"
for src, dst in [("ice_witch.png", "ice_witch_phase2.png"), ("ice_witch_glowmask.png", "ice_witch_phase2_glowmask.png")]:
    im = np.array(Image.open(f"{T}/{src}").convert("RGBA")).astype(float)
    for y, x in zip(*np.nonzero(im[..., 3] > 0)):
        r, g, b = im[y, x, :3] / 255
        h, l, s = colorsys.rgb_to_hls(r, g, b)
        if 0.47 < h < 0.6 and s > 0.6:             # 鮮やかな水色〜空色だけ（肌や布は変えない）
            h = (h + 0.2) % 1.0                     # → すみれ色
            l = min(1.0, l * 1.05)
            im[y, x, :3] = np.array(colorsys.hls_to_rgb(h, l, s)) * 255
    Image.fromarray(im.astype(np.uint8)).save(f"{T}/{dst}")
print("ok")
