"""影刃竜・第2形態：金の目を赤く、全身の剣の刃先を赤く光らせ、翼膜の血管も光らせる。"""
import numpy as np
from PIL import Image

T = "src/main/resources/assets/jsnboss/textures/entity"
tex = np.array(Image.open(f"{T}/blade_dragon.png").convert("RGBA"))
glw = np.array(Image.open(f"{T}/blade_dragon_glowmask.png").convert("RGBA"))

def rgb(h):
    return np.array([int(h[i:i + 2], 16) for i in (1, 3, 5)])

RULES = [
    # (元の色, 新しい色, 光らせる)
    ("#ffcc33", "#ff2030", True),    # 目
    ("#fff0a0", "#ff9090", True),
    ("#f4f8ff", "#ff3a4a", True),    # 剣の刃先
    ("#ffffff", "#ff6070", True),    # 刀の刃紋
    ("#dfe6ef", "#e0303e", True),
    ("#3a141c", "#a01828", True),    # 翼膜の血管
]
for src, dst, glow in RULES:
    mask = np.all(tex[..., :3] == rgb(src), axis=-1) & (tex[..., 3] > 0)
    tex[mask, :3] = rgb(dst)
    if glow:
        glw[mask, :3] = rgb(dst); glw[mask, 3] = 255
# 呪いの剣はもっと明るく
cursed = np.all(glw[..., :3] == rgb("#ff3048"), axis=-1) & (glw[..., 3] > 0)
tex[cursed, :3] = rgb("#ff6a7a"); glw[cursed, :3] = rgb("#ff6a7a")
Image.fromarray(tex).save(f"{T}/blade_dragon_phase2.png")
Image.fromarray(glw).save(f"{T}/blade_dragon_phase2_glowmask.png")
print("ok")
