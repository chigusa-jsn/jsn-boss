"""虚空の監視者・第2形態：出力全開。紫の光がすべて白っぽいシアンに変わる。"""
import numpy as np
from PIL import Image

T = "src/main/resources/assets/jsnboss/textures/entity"
tex = np.array(Image.open(f"{T}/ender_sentinel.png").convert("RGBA"))
glw = np.array(Image.open(f"{T}/ender_sentinel_glowmask.png").convert("RGBA"))
CYAN = [np.array(c) for c in ([31, 159, 200], [79, 230, 255], [224, 253, 255])]

lit = glw[..., 3] > 0
for y, x in zip(*np.nonzero(lit)):
    v = tex[y, x, :3].astype(float).mean() / 255
    c = CYAN[0] if v < 0.45 else (CYAN[1] if v < 0.8 else CYAN[2])
    tex[y, x, :3] = c
    glw[y, x, :3] = c
Image.fromarray(tex).save(f"{T}/ender_sentinel_phase2.png")
Image.fromarray(glw).save(f"{T}/ender_sentinel_phase2_glowmask.png")
print("ok")
