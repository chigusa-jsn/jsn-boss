"""ユグドラシル・第2形態「怒れる世界樹」：葉が燃えるような紅葉（赤・橙・黄）になり、
目とルーンと苔の実が怒りの赤橙に光り、幹に赤橙のひびが走る。"""
import json, random
import numpy as np
from PIL import Image

T = "src/main/resources/assets/jsnboss/textures/entity"
tex = np.array(Image.open(f"{T}/yggdrasil.png").convert("RGBA"))
glw = np.array(Image.open(f"{T}/yggdrasil_glowmask.png").convert("RGBA"))
rng = random.Random(9)
EMBER = [np.array(c) for c in ([190, 50, 10], [255, 120, 30], [255, 220, 140])]
AUTUMN = [np.array(c) for c in ([150, 30, 20], [200, 60, 20], [230, 110, 30], [240, 170, 40], [120, 25, 15])]

for y, x in zip(*np.nonzero(glw[..., 3] > 0)):
    v = tex[y, x, :3].astype(float).mean() / 255
    c = EMBER[0] if v < 0.5 else (EMBER[1] if v < 0.8 else EMBER[2])
    tex[y, x, :3] = c; glw[y, x, :3] = c

for bone, f, mat in json.load(open("tools/yggdrasil_faces.json"))["flat"]:
    fx, fy, fw, fh = f[4]
    if fw < 2 or fh < 2:
        continue
    if mat == "leaves":
        for j in range(fh):
            for k in range(fw):
                p = tex[fy + j, fx + k]
                if p[3] and glw[fy + j, fx + k, 3] == 0:
                    v = p[:3].astype(float).mean() / 100.0       # 元の明るさで色を選ぶ
                    tex[fy + j, fx + k, :3] = AUTUMN[min(4, int(v * 3)) if rng.random() < 0.85 else rng.randrange(5)]
    if mat in ("bark", "bark_dark") and f[0] in ("north", "south", "east", "west") and fw >= 6 and fh >= 6:
        for _ in range(max(1, fw * fh // 500)):
            x, y = rng.randrange(fw), rng.randrange(fh)
            for _ in range(rng.randrange(6, 16)):
                if 0 <= x < fw and 0 <= y < fh and tex[fy + y, fx + x, 3]:
                    c = EMBER[1] if rng.random() < 0.7 else EMBER[2]
                    tex[fy + y, fx + x, :3] = c; glw[fy + y, fx + x, :3] = c; glw[fy + y, fx + x, 3] = 255
                x += rng.choice((-1, 0, 1)); y += rng.choice((0, 1, 1))

Image.fromarray(tex).save(f"{T}/yggdrasil_phase2.png")
Image.fromarray(glw).save(f"{T}/yggdrasil_phase2_glowmask.png")
print("ok")
