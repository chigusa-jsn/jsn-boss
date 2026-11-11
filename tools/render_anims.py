"""Render every animation into one looping GIF grid (and single-animation GIFs)."""
import os, sys, math
import numpy as np
from PIL import Image, ImageDraw, ImageFont
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import render_preview as R
import importlib
FA = importlib.import_module(sys.argv[2] if len(sys.argv) > 2 else "fire_knight_anims")
if len(sys.argv) > 3:
    R.load(sys.argv[3])

JP = {"idle": "待機", "walk": "歩き", "attack_sweep": "横なぎ", "attack_dash": "突進斬り",
      "attack_pillar": "火柱", "attack_fireball": "火の玉", "attack_wave_v": "飛ぶ斬撃(縦)",
      "attack_wave_h": "飛ぶ斬撃(横)", "attack_leap": "跳躍叩きつけ", "attack_combo": "3連撃",
      "phase_change": "第2形態へ", "death": "死亡",
      "cast_spear": "氷の槍", "cast_spikes": "氷のトゲ", "cast_field": "凍てつく大地", "nova": "氷の爆発",
      "teleport": "テレポート", "cast_hail": "雹", "summon_clones": "分身", "absolute_zero": "絶対零度",
      "smash": "振り下ろし", "sweep": "薙ぎ払い", "pound": "衝撃波", "summon": "手下召喚", "breath": "腐毒のブレス",
      "hands": "死者の手", "roar": "第2形態へ", "revive": "復活",
      "cast_orbs": "闇の弾", "blink": "背後斬り", "drain": "命を吸う鎖", "beam": "闇の光線", "abyss": "深淵の詠唱",
      "stagger": "ふらつき", "burst": "闇の爆発"}

def sample(chan, t):
    ks = sorted((float(k), v) for k, v in chan.items())
    if t <= ks[0][0]: return ks[0][1]
    for (t0, v0), (t1, v1) in zip(ks, ks[1:]):
        if t <= t1:
            f = (t - t0) / (t1 - t0) if t1 > t0 else 1
            return [a + (b - a) * f for a, b in zip(v0, v1)]
    return ks[-1][1]

def pose_at(anim, t):
    pose = {}
    for bone, chans in anim["bones"].items():
        pose[bone] = {c: sample(k, t) for c, k in chans.items()}
    return pose

FONT = None
for fp in ["/usr/share/fonts/opentype/noto/NotoSansCJK-Bold.ttc", "/usr/share/fonts/truetype/noto/NotoSansCJK-Bold.ttc",
           "/usr/share/fonts/opentype/ipafont-gothic/ipagp.ttf", "/usr/share/fonts/truetype/fonts-japanese-gothic.ttf"]:
    try:
        FONT = ImageFont.truetype(fp, 18); break
    except Exception:
        pass

def frame(name, t, yaw, pitch, scale, bounds):
    R.POSE.clear(); R.POSE.update(pose_at(FA.A[name], t))
    im, gl, cv, _, _ = R.render(yaw, pitch, scale, bounds=bounds)
    return R.to_image(im, gl, cv)

if __name__ == "__main__":
    names = list(FA.A.keys())
    fps, scale, yaw, pitch = 10, 3, -40, 12
    bounds = (-55, 55, -78, 18)
    total = max(a["length"] for a in FA.A.values()) + 0.6
    nfr = int(total * fps)
    cols = 4; rows = math.ceil(len(names) / cols)
    cw, ch = int(110 * scale), int(96 * scale) + 26
    frames = []
    for fi in range(nfr):
        t = fi / fps
        sheet = Image.new("RGB", (cols * cw, rows * ch), (16, 14, 20))
        d = ImageDraw.Draw(sheet)
        for i, n in enumerate(names):
            a = FA.A[n]
            tt = t % a["length"] if a["loop"] is True else min(t, a["length"])
            img = frame(n, tt, yaw, pitch, scale, bounds)
            x, y = (i % cols) * cw, (i // cols) * ch
            sheet.paste(img, (x, y + 26))
            d.text((x + 8, y + 3), f"{JP.get(n, n)}  {tt:.1f}s", fill=(255, 210, 150), font=FONT)
        frames.append(sheet)
        print(fi, end=" ", flush=True)
    out = sys.argv[1]
    frames[0].save(out, save_all=True, append_images=frames[1:], duration=1000 // fps, loop=0, optimize=True)
    print("\nsaved", out, frames[0].size)
