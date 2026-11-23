"""マザークリーパー・第2形態「臨界」：体じゅうに白く光るひびが走り、火薬袋と目が黄白色に輝く（今にも爆発しそう）。"""
import json, random
import numpy as np
from PIL import Image

T = "src/main/resources/assets/jsnboss/textures/entity"
tex = np.array(Image.open(f"{T}/creeper_mother.png").convert("RGBA"))
glw = np.array(Image.open(f"{T}/creeper_mother_glowmask.png").convert("RGBA"))
rng = random.Random(8)
HOT = [np.array(c) for c in ([255, 170, 40], [255, 230, 120], [255, 255, 235])]

# 光っている所（火薬袋・目・火花）を黄白色に
for y, x in zip(*np.nonzero(glw[..., 3] > 0)):
    v = tex[y, x, :3].astype(float).mean() / 255
    c = HOT[0] if v < 0.5 else (HOT[1] if v < 0.8 else HOT[2])
    tex[y, x, :3] = c; glw[y, x, :3] = c


def glow(x, y, c):
    if tex[y, x, 3] == 0:
        return
    tex[y, x, :3] = c; glw[y, x, :3] = c; glw[y, x, 3] = 255


# 体の緑を少し明るく白っぽく（熱を帯びた感じ）し、白く光るひびを走らせる
for bone, f, mat in json.load(open("tools/creeper_mother_faces.json"))["flat"]:
    fx, fy, fw, fh = f[4]
    if mat not in ("skin", "skin_dark") or fw < 3 or fh < 3:
        continue
    reg = tex[fy:fy + fh, fx:fx + fw]
    a = reg[..., 3] > 0
    reg[..., :3][a] = (reg[..., :3][a] * 0.8 + np.array([200, 255, 180]) * 0.2).astype(np.uint8)
    if f[0] == "down":
        continue
    for _ in range(max(1, fw * fh // 70)):
        x, y = rng.randrange(fw), rng.randrange(fh)
        for _ in range(rng.randrange(4, 10)):
            if 0 <= x < fw and 0 <= y < fh:
                glow(fx + x, fy + y, HOT[2] if rng.random() < 0.4 else HOT[1])
            x += rng.choice((-1, 0, 1)); y += rng.choice((0, 1, 1))

Image.fromarray(tex).save(f"{T}/creeper_mother_phase2.png")
Image.fromarray(glw).save(f"{T}/creeper_mother_phase2_glowmask.png")
print("ok")
