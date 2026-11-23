"""ユグドラシル・第3形態「神樹」：葉が白金と淡い桃色の花の冠になり、樹皮は白っぽい銀に、
目とルーンと苔の実は金色に光る。幹に金の筋が走る。冠の花（blossoms の骨）はこの形態でだけ表示される。"""
import json, random
import numpy as np
from PIL import Image

T = "src/main/resources/assets/jsnboss/textures/entity"
tex = np.array(Image.open(f"{T}/yggdrasil.png").convert("RGBA"))
glw = np.array(Image.open(f"{T}/yggdrasil_glowmask.png").convert("RGBA"))
rng = random.Random(12)
GOLD = [np.array(c) for c in ([210, 160, 40], [255, 220, 110], [255, 250, 220])]
PETAL = [np.array(c) for c in ([255, 196, 226], [250, 170, 210], [255, 214, 234], [246, 222, 150], [236, 150, 196])]

faces = json.load(open("tools/yggdrasil_faces.json"))["flat"]
blossom_px = set()
for bone, f, mat in faces:
    if mat == "blossom":
        fx, fy, fw, fh = f[4]
        for j in range(fh):
            for k in range(fw):
                blossom_px.add((fy + j, fx + k))

for y, x in zip(*np.nonzero(glw[..., 3] > 0)):
    if (y, x) in blossom_px:
        continue
    v = tex[y, x, :3].astype(float).mean() / 255
    c = GOLD[0] if v < 0.5 else (GOLD[1] if v < 0.8 else GOLD[2])
    tex[y, x, :3] = c; glw[y, x, :3] = c


def glow(x, y, c):
    if tex[y, x, 3] == 0:
        return
    tex[y, x, :3] = c; glw[y, x, :3] = c; glw[y, x, 3] = 255


for bone, f, mat in faces:
    fx, fy, fw, fh = f[4]
    if fw < 1 or fh < 1:
        continue
    reg = tex[fy:fy + fh, fx:fx + fw]
    solid = (reg[..., 3] > 0) & (glw[fy:fy + fh, fx:fx + fw, 3] == 0)
    if mat == "leaves":
        for j in range(fh):
            for k in range(fw):
                if solid[j, k]:
                    tex[fy + j, fx + k, :3] = PETAL[rng.randrange(5)] if rng.random() < 0.9 else np.array([200, 230, 170])
        for _ in range(max(1, fw * fh // 60)):
            glow(fx + rng.randrange(fw), fy + rng.randrange(fh), GOLD[2])
    elif mat in ("bark", "bark_dark"):
        # 白っぽい銀の樹皮（元の明暗は残す）
        lum = reg[..., :3].astype(float).mean(-1, keepdims=True) / 110.0
        newc = np.clip(np.array([240, 226, 196]) * (0.55 + 0.55 * lum), 0, 255)
        reg[..., :3][solid] = newc[solid].astype(np.uint8)
        if f[0] in ("north", "south", "east", "west") and fw >= 6 and fh >= 6:
            for _ in range(max(1, fw * fh // 400)):          # 金の筋
                x, y = rng.randrange(fw), rng.randrange(fh)
                for _ in range(rng.randrange(8, 20)):
                    if 0 <= x < fw and 0 <= y < fh:
                        glow(fx + x, fy + y, GOLD[1] if rng.random() < 0.7 else GOLD[2])
                    x += rng.choice((-1, 0, 1)); y += rng.choice((0, 1, 1))
    elif mat in ("moss", "vine"):
        for j in range(fh):
            for k in range(fw):
                if solid[j, k]:
                    tex[fy + j, fx + k, :3] = rng.choice(([150, 210, 120], [180, 230, 150], [255, 220, 236])) if rng.random() < 0.85 \
                        else np.array([255, 250, 200])

Image.fromarray(tex).save(f"{T}/yggdrasil_phase3.png")
Image.fromarray(glw).save(f"{T}/yggdrasil_phase3_glowmask.png")
print("ok")
