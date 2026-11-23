"""ゲーム内と同じ並べ方でボスバーを描いてみる（GUI スケール 3 相当）"""
import os, sys
from PIL import Image, ImageDraw, ImageFont

D = os.path.join(os.path.dirname(__file__), "..", "src/main/resources/assets/jsnboss/textures/gui/bossbar")
FONT = ImageFont.truetype("/usr/share/fonts/opentype/noto/NotoSansCJK-Bold.ttc", 24)
G = 3   # GUI スケール


def bar(img, boss, stage, total, prog, trail, name, y):
    tex = Image.open(os.path.join(D, f"{boss}_{stage + 1}.png")).convert("RGBA")
    x0 = img.width // (2 * G) - 120
    wx, wy, ww, wh = x0 + 36, y + 13, 198, 7

    def blit(u, v, uw, vh, x, yy, w, h):
        if w <= 0:
            return
        part = tex.crop((u, v, u + uw, v + vh)).resize((w * G, h * G), Image.NEAREST)
        img.alpha_composite(part, (x * G, yy * G))

    blit(484, 16, 16, 14, wx, wy, ww, wh)
    fw, tw = round(ww * prog), round(ww * trail)
    d = ImageDraw.Draw(img)
    if tw > fw:
        d.rectangle([(wx + fw) * G, wy * G, (wx + tw) * G - 1, (wy + wh) * G - 1], fill=(255, 242, 176, 255))
    blit(484, 0, 16, 14, wx, wy, fw, wh)
    blit(0, 0, 480, 64, x0, y, 240, 32)
    left = total - 1 - stage
    for i in range(left):
        blit(484, 32, 12, 12, x0 + 40 + i * 8, y + 25, 6, 6)
    tw_ = d.textlength(name, font=FONT)
    tx = (wx + ww // 2) * G - tw_ / 2
    d.text((tx + 3, y * G - 5 + 3), name, font=FONT, fill=(60, 60, 60, 255))
    d.text((tx, y * G - 5), name, font=FONT, fill=(255, 255, 255, 255))


def background(w, h):
    img = Image.new("RGBA", (w, h))
    d = ImageDraw.Draw(img)
    for y in range(h):
        t = y / h
        d.line([(0, y), (w, y)], fill=(int(110 + 40 * t), int(150 + 30 * t), int(205 - 20 * t), 255))
    d.rectangle([0, int(h * 0.82), w, h], fill=(86, 130, 60, 255))
    return img


def main(rows, out):
    w = 300 * G
    h = (len(rows) * 36 + 8) * G
    img = background(w, h)
    y = 4
    for r in rows:
        bar(img, *r, y=y)
        y += 36
    img.save(out)


if __name__ == "__main__":
    rows = [
        ("fire_knight", 0, 2, 0.72, 0.85, "炎の騎士"),
        ("fire_knight", 1, 2, 0.40, 0.40, "炎の騎士"),
        ("ender_sentinel", 0, 2, 0.95, 0.95, "虚空の監視者"),
        ("ender_sentinel", 1, 2, 0.55, 0.70, "虚空の監視者"),
        ("yggdrasil", 0, 3, 0.88, 0.88, "ユグドラシル"),
        ("yggdrasil", 1, 3, 0.50, 0.62, "ユグドラシル"),
        ("yggdrasil", 2, 3, 0.80, 0.80, "ユグドラシル"),
    ]
    main(rows, sys.argv[1] if len(sys.argv) > 1 else "/tmp/bossbar_preview.png")
