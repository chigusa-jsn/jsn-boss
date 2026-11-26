"""紋章システムのアイテム画像（16x16）：核・欠片・紋章 I/II/III を15テーマぶん作る。
python3 tools/emblem_gen.py [preview.png]
"""
import os, sys
from PIL import Image

OUT = os.path.join(os.path.dirname(__file__), '..', 'src/main/resources/assets/jsnboss/textures/item')

def hx(s):
    s = s.lstrip('#'); return tuple(int(s[i:i+2], 16) for i in (0, 2, 4)) + (255,)

# key: (メイン, 明るい, 暗い, 模様)
THEMES = {
    'crimson':      ('#FF7A1C', '#FFD36B', '#7A220C', 'flame'),
    'frostbloom':   ('#7FE3FF', '#F0FDFF', '#24508C', 'snow'),
    'corpse_king':  ('#8CC152', '#E6C55A', '#3A2240', 'skull'),
    'abyss':        ('#B06BFF', '#E8D0FF', '#2A1744', 'eye'),
    'shadow_blade': ('#FF3048', '#FFB0B8', '#22232C', 'blade'),
    'foxfire':      ('#F4F1EA', '#FFE7A0', '#C8321E', 'fox'),
    'gale':         ('#3AC69A', '#C4F6E4', '#174A3C', 'wind'),
    'thunder':      ('#FFE14A', '#FFFBD8', '#8A5E1E', 'bolt'),
    'boulder':      ('#9A9284', '#5FF0B0', '#45403A', 'rock'),
    'void':         ('#C45CFF', '#F2D4FF', '#140F1E', 'void'),
    'verdant':      ('#5CC045', '#B6F09A', '#1C4616', 'bomb'),
    'chain_wolf':   ('#B4BAC2', '#F0BC38', '#40444C', 'fang'),
    'mecha':        ('#8A9099', '#FF9A2A', '#33383F', 'gear'),
    'fallen':       ('#9FF4FF', '#FFFFFF', '#353A46', 'wing'),
    'star_tree':    ('#3CB878', '#8CFFD8', '#4A3524', 'tree'),
}

# 7x7 の模様（# = 明るい色、+ = メインの色）
GLYPHS = {
    'flame':   ["...#...", "..##...", "..###..", ".##+#..", ".#+++#.", ".#+++#.", "..###.."],
    'snow':    ["...#...", ".#.#.#.", "..###..", "###+###", "..###..", ".#.#.#.", "...#..."],
    'skull':   [".#####.", "#######", "#..#..#", "#######", ".##.##.", "..#+#..", "..#.#.."],
    'eye':     ["..###..", ".#+++#.", "#++#++#", "#+###+#", "#++#++#", ".#+++#.", "..###.."],
    'blade':   ["...#...", "..###..", "..#+#..", "..#+#..", ".#####.", "...+...", "..+++.."],
    'fox':     ["#.....#", "##...##", "#+###+#", "#######", ".#.#.#.", "..###..", "...#..."],
    'wind':    ["..####.", ".#....#", "......#", "######.", ".......", "#####..", ".....#."],
    'bolt':    ["...##..", "..##...", ".##....", ".####..", "...##..", "..##...", ".#....."],
    'rock':    ["...#...", "..###..", "..#+#..", ".##+##.", ".#####.", "##+####", "#######"],
    'void':    ["...#...", "..#+#..", ".#+#+#.", "#+###+#", ".#+#+#.", "..#+#..", "...#..."],
    'bomb':    ["....#.#", ".....#.", "..###..", ".#####.", "##+####", "#######", ".#####."],
    'fang':    ["#######", ".#...#.", ".#...#.", ".......", ".#...#.", ".#...#.", "#######"],
    'gear':    ["..#.#..", ".#####.", "##+++##", ".#+.+#.", "##+++##", ".#####.", "..#.#.."],
    'wing':    ["....###", "..#####", ".######", "####+..", "###+...", "##+....", "#......"],
    'tree':    ["..###..", ".#####.", "#######", ".##+##.", "...+...", "...+...", "..+++.."],
}

BRONZE = (hx('#B5713A'), hx('#E8A86A'), hx('#5E3418'))
SILVER = (hx('#AEB6C2'), hx('#F2F6FA'), hx('#59606C'))
GOLD = (hx('#E8B830'), hx('#FFF2A0'), hx('#7A5410'))
CLEAR = (0, 0, 0, 0)


def blank():
    return Image.new('RGBA', (16, 16), CLEAR)


def mix(a, b, t):
    return tuple(int(a[i] + (b[i] - a[i]) * t) for i in range(3)) + (255,)


def disk(img, cx, cy, r, col):
    for y in range(16):
        for x in range(16):
            if (x + .5 - cx) ** 2 + (y + .5 - cy) ** 2 <= r * r:
                img.putpixel((x, y), col)


def outline(img, col):
    """透明でない部分のまわりを1ピクセル縁取る"""
    src = img.copy()
    for y in range(16):
        for x in range(16):
            if src.getpixel((x, y))[3]:
                continue
            for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                nx, ny = x + dx, y + dy
                if 0 <= nx < 16 and 0 <= ny < 16 and src.getpixel((nx, ny))[3]:
                    img.putpixel((x, y), col)
                    break


def glyph(img, name, ox, oy, light, main):
    for j, row in enumerate(GLYPHS[name]):
        for i, c in enumerate(row):
            if c == '#':
                img.putpixel((ox + i, oy + j), light)
            elif c == '+':
                img.putpixel((ox + i, oy + j), main)


def core(t):
    main, light, dark, _ = [hx(c) if c.startswith('#') else c for c in t]
    img = blank()
    # 光る球
    for y in range(16):
        for x in range(16):
            d = ((x + .5 - 8) ** 2 + (y + .5 - 8) ** 2) ** .5
            if d <= 5.6:
                hl = ((x + .5 - 6.3) ** 2 + (y + .5 - 6.3) ** 2) ** .5
                c = mix(light, main, min(1, hl / 3.2)) if hl < 4.2 else mix(main, dark, min(1, (d - 3.4) / 2.4))
                img.putpixel((x, y), c)
    img.putpixel((6, 5), (255, 255, 255, 255))
    img.putpixel((5, 6), (255, 255, 255, 255))
    # 金の留め具（上下左右）
    g, gl, gd = GOLD
    for (x, y) in ((8, 1), (7, 1), (8, 14), (7, 14), (1, 7), (1, 8), (14, 7), (14, 8)):
        img.putpixel((x, y), g)
    for (x, y) in ((7, 2), (8, 2), (2, 7), (2, 8)):
        img.putpixel((x, y), gl)
    for (x, y) in ((7, 13), (8, 13), (13, 7), (13, 8)):
        img.putpixel((x, y), gd)
    outline(img, hx('#18141C'))
    return img


SHARD = ["...........#....", "..........##....", ".........#+#....", "........#++#....", ".......#++##....",
         "......#+++#.....", ".....#++#+#.....", "....#++#++#.....", "....#+#++#......", "...#++#+#.......",
         "...#+++#........", "..#++++#........", "..#+++#.........", "..####..........", "................",
         "................"]


def shard(t):
    main, light, dark, _ = [hx(c) if c.startswith('#') else c for c in t]
    img = blank()
    for y, row in enumerate(SHARD):
        for x, c in enumerate(row):
            if c == '#':
                img.putpixel((x, y + 1), dark)
            elif c == '+':
                img.putpixel((x, y + 1), main)
    # 光の筋
    for (x, y) in ((10, 4), (9, 5), (8, 6), (7, 7), (6, 8), (5, 10), (4, 12)):
        if img.getpixel((x, y))[3]:
            img.putpixel((x, y), light)
    img.putpixel((13, 3), light); img.putpixel((12, 2), mix(light, CLEAR[:3] + (255,), 0) )
    outline(img, hx('#18141C'))
    return img


def emblem(t, rank):
    main, light, dark, gname = [hx(c) if c.startswith('#') else c for c in t]
    metal, mlight, mdark = (BRONZE, SILVER, GOLD)[rank - 1]
    img = blank()
    # III は後ろに翼のような飾り
    if rank == 3:
        for (x, y) in ((0, 5), (0, 6), (1, 4), (1, 5), (1, 6), (1, 7), (2, 5), (2, 8), (1, 9), (0, 10),
                       (15, 5), (15, 6), (14, 4), (14, 5), (14, 6), (14, 7), (13, 5), (13, 8), (14, 9), (15, 10)):
            img.putpixel((x, y), mlight if y < 6 else metal)
    # II 以上は上下左右にとげ
    if rank >= 2:
        for (x, y) in ((7, 0), (8, 0), (7, 15), (8, 15), (0, 7), (0, 8), (15, 7), (15, 8)):
            img.putpixel((x, y), mdark if y >= 8 else metal)
    disk(img, 8, 8, 7.0, mdark)
    disk(img, 8, 8, 6.2, metal)
    # 縁の明るい側
    for y in range(16):
        for x in range(16):
            d = ((x + .5 - 8) ** 2 + (y + .5 - 8) ** 2) ** .5
            if 5.2 < d <= 6.2 and x + y < 14:
                img.putpixel((x, y), mlight)
    disk(img, 8, 8, 5.0, dark)
    glyph(img, gname, 5, 5, light, main)
    if rank == 3:
        for (x, y) in ((3, 3), (12, 3), (3, 12), (12, 12)):
            img.putpixel((x, y), mlight)
    outline(img, hx('#18141C'))
    return img


def main():
    os.makedirs(OUT, exist_ok=True)
    sheet = Image.new('RGBA', (5 * 18 + 2, len(THEMES) * 18 + 2), (52, 48, 60, 255))
    for row, (key, t) in enumerate(THEMES.items()):
        imgs = [core(t), shard(t), emblem(t, 1), emblem(t, 2), emblem(t, 3)]
        names = [f'{key}_core', f'{key}_shard', f'{key}_emblem_1', f'{key}_emblem_2', f'{key}_emblem_3']
        for col, (im, n) in enumerate(zip(imgs, names)):
            im.save(os.path.join(OUT, n + '.png'))
            sheet.alpha_composite(im, (2 + col * 18, 2 + row * 18))
    if len(sys.argv) > 1:
        sheet.resize((sheet.width * 6, sheet.height * 6), Image.NEAREST).save(sys.argv[1])


if __name__ == '__main__':
    main()
