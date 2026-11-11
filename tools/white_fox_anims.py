"""光の白狐のアニメーション。 python3 tools/white_fox_anims.py
角度の決まりは blade_dragon_anims.py と同じ（脚の X マイナス＝前へ、体の X マイナス＝頭を上げる）。
首はもともと X -35、頭は X +30、尻尾は各本 X +20〜48 / Y ±76 で扇状に開いている。
尻尾の Y を外側へ足すと扇がさらに開き、X を足すと持ち上がる。
"""
import json, math, os

OUT = "src/main/resources/assets/jsnboss/animations/entity"
Z = [0, 0, 0]
ONE = [1, 1, 1]
SPREAD = [-76, -57, -38, -19, 0, 19, 38, 57, 76]


def keys(*pairs):
    return {f"{round(float(t), 3)}": v for t, v in pairs}


def merge(*parts):
    out = {}
    for p in parts:
        for bone, ch in p.items():
            out.setdefault(bone, {}).update(ch)
    return out


def tails_sway(length, amp=6, lift=0.0, steps=8):
    """9本の尻尾をそれぞれ少しずつずらして揺らす（先の節ほど遅れる）"""
    out = {}
    for i in range(9):
        for seg, lag, a in (("", 0.0, 1.0), ("_mid", 0.12, 1.2), ("_tip", 0.24, 1.5)):
            ks = []
            for s in range(steps + 1):
                t = length * s / steps
                ph = 2 * math.pi * (t / length - lag) + i * 0.7
                ks.append((t, [round(lift + amp * 0.5 * a * math.sin(ph), 2), round(amp * a * math.cos(ph) * 0.6, 2), 0]))
            out[f"tail{i}{seg}"] = {"rotation": keys(*ks)}
    return out


def tails_fan(pairs):
    """扇の開き方を時間で変える：pairs = [(時間, 開き倍率の追加, 持ち上げ角)]"""
    out = {}
    for i, base in enumerate(SPREAD):
        out[f"tail{i}"] = {"rotation": keys(*[(t, [lift, round(base * k, 2), 0]) for t, k, lift in pairs])}
    return out


def gait(length, front, hind, pair=0.5, hind_phase=0.0, steps=4):
    def swing(amp, phase):
        return {"rotation": keys(*[(length * s / steps,
                                    [round(-amp * math.cos(2 * math.pi * (s / steps + phase)), 2), 0, 0]) for s in range(steps + 1)])}
    return {"front_right": swing(front, 0), "front_left": swing(front, pair),
            "hind_left": swing(hind, hind_phase), "hind_right": swing(hind, hind_phase + pair)}


A = {}

A["idle"] = dict(loop=True, length=3.0, bones=merge(tails_sway(3.0, 6), {
    "body": {"position": keys((0, Z), (1.5, [0, -0.5, 0]), (3, Z))},
    "neck": {"rotation": keys((0, Z), (1.0, [-3, 8, 0]), (2.0, [-2, -8, 0]), (3, Z))},
    "right_ear": {"rotation": keys((0, Z), (2.6, Z), (2.7, [0, 0, 15]), (2.8, Z), (3, Z))},
    "left_ear": {"rotation": keys((0, Z), (1.3, Z), (1.4, [0, 0, -15]), (1.5, Z), (3, Z))},
}))

A["walk"] = dict(loop=True, length=1.0, bones=merge(tails_sway(1.0, 4), gait(1.0, 20, 20), {
    "body": {"position": keys((0, Z), (0.25, [0, -0.6, 0]), (0.5, Z), (0.75, [0, -0.6, 0]), (1.0, Z))},
    "neck": {"rotation": keys((0, [4, 0, 0]), (0.5, [-2, 0, 0]), (1.0, [4, 0, 0]))},
}))

A["run"] = dict(loop=True, length=0.5, bones=merge(tails_sway(0.5, 3, lift=-12), gait(0.5, 45, 45, pair=0.08, hind_phase=0.5), {
    "body": {"rotation": keys((0, [-5, 0, 0]), (0.25, [6, 0, 0]), (0.5, [-5, 0, 0])),
             "position": keys((0, [0, 1, 0]), (0.25, [0, -1.5, 0]), (0.5, [0, 1, 0]))},
    "neck": {"rotation": keys((0, [18, 0, 0]), (0.25, [24, 0, 0]), (0.5, [18, 0, 0]))},
}))

# 狐火：尻尾を大きく開いて、0.6 / 0.8 / 1.0 秒に光の玉を放つ
A["foxfire"] = dict(loop=False, length=1.4, bones=merge(
    tails_fan([(0, 0, 0), (0.4, 0.3, 15), (1.0, 0.3, 15), (1.4, 0, 0)]), {
    "neck": {"rotation": keys((0, Z), (0.4, [-15, 0, 0]), (0.6, [-8, 0, 0]), (0.8, [-15, 0, 0]), (1.0, [-8, 0, 0]), (1.4, Z))},
    "body": {"rotation": keys((0, Z), (0.4, [-6, 0, 0]), (1.0, [-6, 0, 0]), (1.4, Z))},
}))

# 光の柱：前脚を振り上げて 0.7 秒に地面を踏む
A["pillar"] = dict(loop=False, length=1.4, bones=merge(tails_fan([(0, 0, 0), (0.5, 0.15, 20), (0.7, 0.2, -5), (1.4, 0, 0)]), {
    "body": {"rotation": keys((0, Z), (0.5, [-25, 0, 0]), (0.7, [6, 0, 0]), (1.0, [6, 0, 0]), (1.4, Z)),
             "position": keys((0, Z), (0.5, [0, 4, 0]), (0.7, [0, -1, 0]), (1.4, Z))},
    "front_right": {"rotation": keys((0, Z), (0.5, [-70, 0, 0]), (0.7, [-10, 0, 0]), (1.4, Z))},
    "front_left": {"rotation": keys((0, Z), (0.5, [-60, 0, 0]), (0.7, [-10, 0, 0]), (1.4, Z))},
    "neck": {"rotation": keys((0, Z), (0.5, [-15, 0, 0]), (0.7, [12, 0, 0]), (1.4, Z))},
}))

# 閃光：尻尾を一気に全開にして 0.8 秒にまぶしく光る
A["flash"] = dict(loop=False, length=1.4, bones=merge(tails_fan([(0, 0, 0), (0.6, -0.4, -10), (0.8, 0.45, 30), (1.1, 0.4, 25), (1.4, 0, 0)]), {
    "body": {"rotation": keys((0, Z), (0.6, [6, 0, 0]), (0.8, [-12, 0, 0]), (1.4, Z))},
    "neck": {"rotation": keys((0, Z), (0.6, [15, 0, 0]), (0.8, [-20, 0, 0]), (1.4, Z))},
}))

# 幻（分身）：くるりと回って 0.7 秒に分かれる
A["illusion"] = dict(loop=False, length=1.2, bones=merge(tails_fan([(0, 0, 0), (0.7, 0.35, 25), (1.2, 0, 0)]), {
    "root": {"rotation": keys((0, Z), (0.7, [0, 360, 0]), (0.71, Z), (1.2, Z)),
             "scale": keys((0, ONE), (0.6, [0.9, 1.1, 0.9]), (0.7, [1.15, 0.9, 1.15]), (0.85, ONE), (1.2, ONE))},
}))

# 白い光線：頭を低く構えて 1.0 秒ためて、1.0〜2.4 秒撃ち続ける
A["laser"] = dict(loop=False, length=2.8, bones=merge(tails_fan([(0, 0, 0), (1.0, 0.25, 20), (2.4, 0.25, 20), (2.8, 0, 0)]), {
    "body": {"rotation": keys((0, Z), (1.0, [8, 0, 0]), (2.4, [8, 0, 0]), (2.8, Z)),
             "position": keys((0, Z), (1.0, [0, -2, 0]), (2.4, [0, -2, 0]), (2.8, Z))},
    "neck": {"rotation": keys((0, Z), (1.0, [25, 0, 0]), (1.1, [20, 0, 0]), (2.4, [20, 0, 0]), (2.8, Z))},
    "front_right": {"rotation": keys((0, Z), (1.0, [-20, 0, 0]), (2.4, [-20, 0, 0]), (2.8, Z))},
    "front_left": {"rotation": keys((0, Z), (1.0, [-20, 0, 0]), (2.4, [-20, 0, 0]), (2.8, Z))},
    "hind_right": {"rotation": keys((0, Z), (1.0, [20, 0, 0]), (2.4, [20, 0, 0]), (2.8, Z))},
    "hind_left": {"rotation": keys((0, Z), (1.0, [20, 0, 0]), (2.4, [20, 0, 0]), (2.8, Z))},
}))

# 跳躍（近づかれたとき）：後ろへ大きく跳んで距離をとる
A["leap"] = dict(loop=False, length=1.0, bones=merge(tails_fan([(0, 0, 0), (0.3, -0.2, -15), (0.6, 0.1, 25), (1.0, 0, 0)]), {
    "body": {"rotation": keys((0, Z), (0.25, [10, 0, 0]), (0.5, [-20, 0, 0]), (0.8, [5, 0, 0]), (1.0, Z)),
             "position": keys((0, Z), (0.25, [0, -3, 0]), (0.5, Z), (1.0, Z))},
    "front_right": {"rotation": keys((0, Z), (0.5, [-50, 0, 0]), (0.8, [-10, 0, 0]), (1.0, Z))},
    "front_left": {"rotation": keys((0, Z), (0.5, [-50, 0, 0]), (0.8, [-10, 0, 0]), (1.0, Z))},
    "hind_right": {"rotation": keys((0, Z), (0.25, [-20, 0, 0]), (0.5, [45, 0, 0]), (1.0, Z))},
    "hind_left": {"rotation": keys((0, Z), (0.25, [-20, 0, 0]), (0.5, [45, 0, 0]), (1.0, Z))},
}))

# 光の剣の雨（第2形態）：後ろ脚で立ち上がり、天へ吠える 0.8〜2.0 秒
A["sword_rain"] = dict(loop=False, length=2.4, bones=merge(tails_fan([(0, 0, 0), (0.8, 0.45, 35), (2.0, 0.45, 35), (2.4, 0, 0)]), {
    "body": {"rotation": keys((0, Z), (0.8, [-30, 0, 0]), (2.0, [-30, 0, 0]), (2.4, Z)),
             "position": keys((0, Z), (0.8, [0, 4, 0]), (2.0, [0, 4, 0]), (2.4, Z))},
    "neck": {"rotation": keys((0, Z), (0.8, [-10, 0, 0]), (2.0, [-10, 0, 0]), (2.4, Z))},
    "head": {"rotation": keys((0, Z), (0.8, [-30, 0, 0]), (2.0, [-30, 0, 0]), (2.4, Z))},
    "front_right": {"rotation": keys((0, Z), (0.8, [-60, 0, 10]), (2.0, [-60, 0, 10]), (2.4, Z))},
    "front_left": {"rotation": keys((0, Z), (0.8, [-60, 0, -10]), (2.0, [-60, 0, -10]), (2.4, Z))},
}))

A["phase_change"] = dict(loop=False, length=3.0, bones=merge(tails_fan([(0, 0, 0), (0.8, -0.5, -10), (1.4, 0.55, 40), (2.4, 0.5, 35), (3.0, 0, 0)]), {
    "body": {"rotation": keys((0, Z), (0.8, [10, 0, 0]), (1.4, [-30, 0, 0]), (2.4, [-30, 0, 0]), (3.0, Z)),
             "position": keys((0, Z), (0.8, [0, -3, 0]), (1.4, [0, 5, 0]), (2.4, [0, 5, 0]), (3.0, Z))},
    "neck": {"rotation": keys((0, Z), (0.8, [20, 0, 0]), (1.4, [-25, 0, 0]), (2.4, [-25, 0, 0]), (3.0, Z))},
    "head": {"rotation": keys((0, Z), (1.4, [-25, 0, 0]), (2.4, [-25, 0, 0]), (3.0, Z))},
}))

A["death"] = dict(loop="hold_on_last_frame", length=3.0, bones=merge(tails_fan([(0, 0, 0), (1.5, -0.3, -30), (3.0, -0.35, -40)]), {
    "body": {"rotation": keys((0, Z), (0.6, [-10, 0, 0]), (1.6, [5, 0, 30]), (3.0, [5, 0, 35])),
             "position": keys((0, Z), (0.6, [0, 1, 0]), (1.6, [0, -9, 0]), (3.0, [0, -10, 0]))},
    "neck": {"rotation": keys((0, Z), (0.6, [-20, 0, 0]), (1.6, [35, 0, 15]), (3.0, [40, 0, 20]))},
    "front_right": {"rotation": keys((0, Z), (1.6, [-30, 0, 30]), (3.0, [-35, 0, 35]))},
    "front_left": {"rotation": keys((0, Z), (1.6, [20, 0, 0]), (3.0, [20, 0, 0]))},
    "hind_right": {"rotation": keys((0, Z), (1.6, [30, 0, 25]), (3.0, [35, 0, 25]))},
    "hind_left": {"rotation": keys((0, Z), (1.6, [-20, 0, 0]), (3.0, [-20, 0, 0]))},
}))


def to_json():
    return {"format_version": "1.8.0", "animations": {
        f"animation.white_fox.{n}": {"loop": a["loop"], "animation_length": a["length"], "bones": a["bones"]}
        for n, a in A.items()}}


if __name__ == "__main__":
    os.makedirs(OUT, exist_ok=True)
    with open(f"{OUT}/white_fox.animation.json", "w") as f:
        json.dump(to_json(), f, indent=1)
    print(len(A), "animations")
