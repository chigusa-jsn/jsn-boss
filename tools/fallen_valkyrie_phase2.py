"""堕ちた戦乙女・第2形態「完全なる堕天」：青白い光が赤紫に変わり、翼の羽根が欠けてボロボロになり、
鎧に赤紫のひびが走る。"""
import json, random
import numpy as np
from PIL import Image

T = "src/main/resources/assets/jsnboss/textures/entity"
tex = np.array(Image.open(f"{T}/fallen_valkyrie.png").convert("RGBA"))
glw = np.array(Image.open(f"{T}/fallen_valkyrie_glowmask.png").convert("RGBA"))
rng = random.Random(6)
RED = [np.array(c) for c in ([150, 10, 60], [255, 50, 110], [255, 200, 220])]

for y, x in zip(*np.nonzero(glw[..., 3] > 0)):
    v = tex[y, x, :3].astype(float).mean() / 255
    c = RED[0] if v < 0.55 else (RED[1] if v < 0.85 else RED[2])
    tex[y, x, :3] = c; glw[y, x, :3] = c


def glow(x, y, c):
    if tex[y, x, 3] == 0:
        return
    tex[y, x, :3] = c; glw[y, x, :3] = c; glw[y, x, 3] = 255


for bone, f, mat in json.load(open("tools/fallen_valkyrie_faces.json"))["flat"]:
    fx, fy, fw, fh = f[4]
    if fw < 2 or fh < 2:
        continue
    if mat == "feather" and f[0] in ("north", "south", "up", "down"):
        # 羽根の縁をところどころ欠けさせる（ギザギザ）
        for k in range(fw):
            if rng.random() < 0.25:
                for j in range(rng.randrange(1, max(2, fh // 2 + 1))):
                    tex[fy + j, fx + k] = 0; glw[fy + j, fx + k] = 0
    if mat == "steel" and f[0] in ("north", "south", "east", "west") and fw >= 4 and fh >= 4:
        for _ in range(max(1, fw * fh // 160)):            # 鎧の赤紫のひび
            x, y = rng.randrange(fw), rng.randrange(fh)
            for _ in range(rng.randrange(4, 10)):
                if 0 <= x < fw and 0 <= y < fh:
                    glow(fx + x, fy + y, RED[1] if rng.random() < 0.7 else RED[2])
                x += rng.choice((-1, 0, 1)); y += rng.choice((0, 1))

Image.fromarray(tex).save(f"{T}/fallen_valkyrie_phase2.png")
Image.fromarray(glw).save(f"{T}/fallen_valkyrie_phase2_glowmask.png")
print("ok")
