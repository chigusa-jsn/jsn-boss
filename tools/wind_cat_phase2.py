"""風の猫・第2形態：体が半分風になる。毛を半透明で淡く、翡翠の縞とリボンを強く光らせる。
半透明はゲーム側で entityTranslucent の描き方にして表示する（WindCatRenderer）。"""
import numpy as np
from PIL import Image

T = "src/main/resources/assets/jsnboss/textures/entity"
tex = np.array(Image.open(f"{T}/wind_cat.png").convert("RGBA")).astype(int)
glw = np.array(Image.open(f"{T}/wind_cat_glowmask.png").convert("RGBA")).astype(int)
JADE = np.array([0x3a, 0xa6, 0x88])
MINT = np.array([0x8c, 0xff, 0xdc])

vis = tex[..., 3] > 0
orig = tex.copy()
RIB_COLORS = [np.array([0x6f, 0xe0, 0xc2]), np.array([0xc9, 0xff, 0xf0])]
ribbon_px = vis & np.any([np.all(orig[..., :3] == c_, axis=-1) for c_ in RIB_COLORS], axis=0)
jade = vis & np.all(tex[..., :3] == JADE, axis=-1)
ribbon = ribbon_px | (vis & (glw[..., 3] > 0))
grayish = vis & ~jade & ~ribbon & (np.abs(tex[..., 0] - tex[..., 1]) < 30) & (tex[..., 1] > 120)

# 毛：ミント寄りに淡くして半透明に
tex[grayish, :3] = (tex[grayish, :3] * 0.6 + np.array([210, 255, 240]) * 0.4).astype(int)
tex[grayish, 3] = 150
# 縞：光る
tex[jade, :3] = MINT; glw[jade, :3] = MINT; glw[jade, 3] = 255
# リボン：全部光る
glw[ribbon_px, :3] = orig[ribbon_px, :3]; glw[ribbon_px, 3] = 255
Image.fromarray(tex.clip(0, 255).astype(np.uint8)).save(f"{T}/wind_cat_phase2.png")
Image.fromarray(glw.clip(0, 255).astype(np.uint8)).save(f"{T}/wind_cat_phase2_glowmask.png")
print("ok", int(grayish.sum()), int(jade.sum()))
