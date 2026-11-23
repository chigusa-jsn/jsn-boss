"""鋼鉄の機甲蜘蛛・第2形態「暴走」：オレンジの光がすべて真っ赤になり、装甲に焦げ跡がつく。"""
import json, random
import numpy as np
from PIL import Image

T = "src/main/resources/assets/jsnboss/textures/entity"
tex = np.array(Image.open(f"{T}/iron_spider.png").convert("RGBA"))
glw = np.array(Image.open(f"{T}/iron_spider_glowmask.png").convert("RGBA"))
rng = random.Random(3)
RED = [np.array(c) for c in ([170, 10, 10], [255, 40, 30], [255, 200, 180])]

for y, x in zip(*np.nonzero(glw[..., 3] > 0)):
    v = tex[y, x, :3].astype(float).mean() / 255
    c = RED[0] if v < 0.45 else (RED[1] if v < 0.78 else RED[2])
    tex[y, x, :3] = c; glw[y, x, :3] = c

# 装甲の焦げ跡（黒っぽいまだら）
for bone, f, mat in json.load(open("tools/iron_spider_faces.json"))["flat"]:
    fx, fy, fw, fh = f[4]
    if mat not in ("steel", "steel_dark") or fw < 3 or fh < 3:
        continue
    for _ in range(max(1, fw * fh // 40)):
        cx, cy = rng.randrange(fw), rng.randrange(fh)
        for _ in range(6):
            x, y = cx + rng.randrange(-1, 2), cy + rng.randrange(-1, 2)
            if 0 <= x < fw and 0 <= y < fh and glw[fy + y, fx + x, 3] == 0 and tex[fy + y, fx + x, 3]:
                tex[fy + y, fx + x, :3] = rng.choice(([30, 26, 24], [52, 40, 34]))

Image.fromarray(tex).save(f"{T}/iron_spider_phase2.png")
Image.fromarray(glw).save(f"{T}/iron_spider_phase2_glowmask.png")
print("ok")
