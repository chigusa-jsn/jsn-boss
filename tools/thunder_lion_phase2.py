"""雷の獅子・第2形態：全身の毛に稲妻が走り、たてがみ全体が青白く帯電する。"""
import json, random
import numpy as np
from PIL import Image

T = "src/main/resources/assets/jsnboss/textures/entity"
tex = np.array(Image.open(f"{T}/thunder_lion.png").convert("RGBA"))
glw = np.array(Image.open(f"{T}/thunder_lion_glowmask.png").convert("RGBA"))
rng = random.Random(9)
ELEC = [np.array(c) for c in ([47, 168, 224], [127, 227, 255], [232, 251, 255])]


def glow(x, y, c):
    if tex[y, x, 3] == 0:
        return
    tex[y, x, :3] = c
    glw[y, x, :3] = c; glw[y, x, 3] = 255


def bolt(fx, fy, fw, fh):
    """面の上から下へ、折れ曲がりながら走る稲妻"""
    x, y = rng.randrange(fw), 0
    while y < fh:
        if 0 <= x < fw:
            glow(fx + x, fy + y, ELEC[2] if rng.random() < 0.35 else ELEC[1])
        y += 1
        x += rng.choice((-1, 0, 1, 1)) if (y // 3) % 2 else rng.choice((-1, -1, 0, 1))


for bone, f, mat in json.load(open("tools/thunder_lion_faces.json"))["flat"]:
    fx, fy, fw, fh = f[4]
    if fw <= 0 or fh <= 0:
        continue
    if mat in ("fur", "fur_light") and bone != "head":
        for _ in range(max(1, fw * fh // 130)):
            bolt(fx, fy, fw, fh)
    elif mat == "mane":
        # たてがみ：黒地に青い電気がびっしり
        for j in range(fh):
            for k in range(fw):
                if rng.random() < 0.28:
                    glow(fx + k, fy + j, ELEC[rng.randrange(3)])

Image.fromarray(tex).save(f"{T}/thunder_lion_phase2.png")
Image.fromarray(glw).save(f"{T}/thunder_lion_phase2_glowmask.png")
print("ok")
