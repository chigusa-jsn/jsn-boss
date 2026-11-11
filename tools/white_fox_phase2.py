"""光の白狐・第2形態：9本の尻尾すべてを金白に輝かせ、朱の隈取りと目も光らせる。"""
import json, random
import numpy as np
from PIL import Image

T = "src/main/resources/assets/jsnboss/textures/entity"
tex = np.array(Image.open(f"{T}/white_fox.png").convert("RGBA"))
glw = np.array(Image.open(f"{T}/white_fox_glowmask.png").convert("RGBA"))
rng = random.Random(5)
GOLD = [np.array(c) for c in ([255, 236, 170], [255, 246, 214], [255, 255, 255], [255, 214, 110])]

# 尻尾の全パーツ（根元・中ほど・先）の面を光らせる。根元ほど少し控えめ
for bone, f, mat in json.load(open("tools/white_fox_faces.json"))["flat"]:
    if not bone.startswith("tail") or bone == "tails":
        continue
    fx, fy, fw, fh = f[4]
    strength = 0.55 if bone.endswith(("0", "1", "2", "3", "4", "5", "6", "7", "8")) else 0.85
    for j in range(fh):
        for k in range(fw):
            if tex[fy + j, fx + k, 3] == 0 or rng.random() > strength:
                continue
            c = GOLD[rng.randrange(len(GOLD))]
            tex[fy + j, fx + k, :3] = c
            glw[fy + j, fx + k, :3] = c; glw[fy + j, fx + k, 3] = 255

# 朱の隈取りを光らせる
v = np.all(tex[..., :3] == [224, 64, 42], axis=-1) & (tex[..., 3] > 0)
tex[v, :3] = [255, 90, 60]; glw[v, :3] = [255, 90, 60]; glw[v, 3] = 255
Image.fromarray(tex).save(f"{T}/white_fox_phase2.png")
Image.fromarray(glw).save(f"{T}/white_fox_phase2_glowmask.png")
print("ok")
