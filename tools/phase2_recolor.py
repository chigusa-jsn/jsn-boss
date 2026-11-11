"""Make the phase-2 (blue soul fire) texture + glowmask from the phase-1 ones.
Every glowing pixel is remapped by brightness onto a blue fire gradient."""
import numpy as np
from PIL import Image

T = "src/main/resources/assets/jsnboss/textures/entity"
tex = np.array(Image.open(f"{T}/fire_knight.png").convert("RGBA")).astype(float)
glw = np.array(Image.open(f"{T}/fire_knight_glowmask.png").convert("RGBA")).astype(float)

STOPS = [(0, (6, 22, 70)), (70, (10, 60, 200)), (130, (25, 140, 255)),
         (185, (80, 220, 255)), (225, (170, 245, 255)), (255, (235, 255, 255))]

def blue(lum):
    for (a, ca), (b, cb) in zip(STOPS, STOPS[1:]):
        if lum <= b:
            f = (lum - a) / (b - a)
            return [ca[i] + (cb[i] - ca[i]) * f for i in range(3)]
    return list(STOPS[-1][1])

mask = glw[..., 3] > 0
for y, x in zip(*np.nonzero(mask)):
    r, g, b = glw[y, x, :3]
    lum = 0.25 * r + 0.6 * g + 0.15 * b          # weight green so yellow cores stay brightest
    lum = min(255, lum * 1.25 + 20)
    c = blue(lum)
    tex[y, x, :3] = c
    glw[y, x, :3] = c

Image.fromarray(tex.astype(np.uint8)).save(f"{T}/fire_knight_phase2.png")
Image.fromarray(glw.astype(np.uint8)).save(f"{T}/fire_knight_phase2_glowmask.png")
print("recolored", int(mask.sum()), "pixels")
