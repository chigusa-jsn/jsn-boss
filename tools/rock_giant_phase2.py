"""岩の巨人・第2形態：緑の結晶と割れ目の光がマグマのオレンジに変わり、全身の岩にマグマの筋が走る。"""
import json, random
import numpy as np
from PIL import Image

T = "src/main/resources/assets/jsnboss/textures/entity"
tex = np.array(Image.open(f"{T}/rock_giant.png").convert("RGBA"))
glw = np.array(Image.open(f"{T}/rock_giant_glowmask.png").convert("RGBA"))
rng = random.Random(4)
LAVA = [np.array(c) for c in ([200, 60, 10], [255, 130, 20], [255, 210, 90])]

# 光っている所（結晶・目・割れ目）を明るさに合わせてマグマ色に
lit = glw[..., 3] > 0
for y, x in zip(*np.nonzero(lit)):
    v = tex[y, x, :3].astype(float).mean() / 255
    c = LAVA[0] if v < 0.55 else (LAVA[1] if v < 0.8 else LAVA[2])
    tex[y, x, :3] = c; glw[y, x, :3] = c


def glow(x, y, c):
    if tex[y, x, 3] == 0:
        return
    tex[y, x, :3] = c; glw[y, x, :3] = c; glw[y, x, 3] = 255


# 岩の面にマグマの筋（ひびの線をなぞるように、下へ折れ曲がりながら）
for bone, f, mat in json.load(open("tools/rock_giant_faces.json"))["flat"]:
    fx, fy, fw, fh = f[4]
    if mat not in ("rock", "rock_dark") or f[0] in ("up", "down") or fw < 4 or fh < 4:
        continue
    for _ in range(max(1, fw * fh // 220)):
        x, y = rng.randrange(fw), rng.randrange(max(1, fh // 3))
        for _ in range(rng.randrange(5, 14)):
            if 0 <= x < fw and 0 <= y < fh:
                glow(fx + x, fy + y, LAVA[1] if rng.random() < 0.7 else LAVA[2])
            x += rng.choice((-1, 0, 1)); y += 1
    # 元の暗いひびもうっすら赤く光らせる
    for j in range(fh):
        for k in range(fw):
            p = tex[fy + j, fx + k]
            if p[3] and p[:3].max() < 50 and rng.random() < 0.35:
                glow(fx + k, fy + j, LAVA[0])

Image.fromarray(tex).save(f"{T}/rock_giant_phase2.png")
Image.fromarray(glw).save(f"{T}/rock_giant_phase2_glowmask.png")
print("ok")
