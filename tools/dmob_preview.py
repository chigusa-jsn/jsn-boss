"""ダンジョンモブのデザイン確認用シート。 python3 tools/dmob_preview.py out.png name:ラベル ...
各モブを「ななめ前（プレイヤーと並べて大きさ比較）」と「うしろ」の2方向で並べる"""
import sys, os
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import render_preview as rp
from PIL import Image, ImageDraw, ImageFont

FONT = "/usr/share/fonts/opentype/noto/NotoSansCJK-Black.ttc"
out, items = sys.argv[1], [a.split(":", 1) for a in sys.argv[2:]]
S = 6
cards = []
for name, label in items:
    rp.load(name)
    rp.POSE.clear()
    views = []
    for yaw, pitch, extra in ((-35, 14, rp.player(-26)), (150, 10, ())):
        im, gl, cv, _, _ = rp.render(yaw, pitch, S, extra)
        views.append(rp.to_image(im, gl, cv))
    h = max(v.height for v in views)
    w = sum(v.width for v in views) + 10
    card = Image.new("RGB", (max(w, 260), h + 46), (20, 18, 24))
    x = 0
    for v in views:
        card.paste(v, (x, 40 + h - v.height)); x += v.width + 10
    ImageDraw.Draw(card).text((8, 4), label, font=ImageFont.truetype(FONT, 26), fill=(255, 230, 200))
    cards.append(card)
W = sum(c.width for c in cards) + 16 * (len(cards) + 1)
H = max(c.height for c in cards) + 32
sheet = Image.new("RGB", (W, H), (12, 10, 14))
x = 16
for c in cards:
    sheet.paste(c, (x, 16 + H - 32 - c.height)); x += c.width + 16
if sheet.width > 3000:
    sheet = sheet.resize((3000, int(sheet.height * 3000 / sheet.width)), Image.LANCZOS)
sheet.save(out); print(sheet.size)
