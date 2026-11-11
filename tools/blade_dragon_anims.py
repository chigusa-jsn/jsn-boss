"""影刃竜のアニメーション。 python3 tools/blade_dragon_anims.py
角度の決まり（fire_knight_anims.py と同じ）：
  前脚（翼）/後ろ脚の X マイナス＝前へ振り出す、X プラス＝後ろへ
  体の X マイナス＝頭を上げる（後ろ足で立ち上がる方向）、X プラス＝前のめり
  首はもともと X -25、頭は X +22、尻尾は各節 X +6〜8 回っている
  右前脚の Z プラス＝外側へ開く（左はマイナス）
"""
import json, math, os

OUT = "src/main/resources/assets/jsnboss/animations/entity"
Z = [0, 0, 0]
ONE = [1, 1, 1]
TAIL = ["tail1", "tail2", "tail3", "tail_tip"]


def keys(*pairs):
    return {f"{round(float(t), 3)}": v for t, v in pairs}


def merge(*parts):
    out = {}
    for p in parts:
        for bone, ch in p.items():
            out.setdefault(bone, {}).update(ch)
    return out


def tail_wave(length, amp_y=8, amp_x=0, lag=0.12, steps=8, base_x=0):
    """尻尾を根元から先へ遅れて波打たせる"""
    out = {}
    for i, b in enumerate(TAIL):
        ks = []
        for s in range(steps + 1):
            t = length * s / steps
            ph = 2 * math.pi * (t / length) - i * lag * 2 * math.pi
            ks.append((t, [round(base_x + amp_x * math.sin(ph), 2), round(amp_y * (1 + 0.3 * i) * math.sin(ph), 2), 0]))
        out[b] = {"rotation": keys(*ks)}
    return out


def tail_pose(t_pairs):
    """[(時間, [x,y,z]), ...] を尻尾の全節に同じだけ足す"""
    return {b: {"rotation": keys(*t_pairs)} for b in TAIL}


def gait(length, front, hind, phase_front_pair=0.5, phase_hind=0.0, steps=4):
    """前脚(翼)と後ろ脚を振る。trot: 右前と左後ろが同じ動き"""
    def swing(amp, phase):
        ks = []
        for s in range(steps + 1):
            t = length * s / steps
            ks.append((t, [round(-amp * math.cos(2 * math.pi * (t / length + phase)), 2), 0, 0]))
        return {"rotation": keys(*ks)}
    return {"right_wing": swing(front, 0), "left_wing": swing(front, phase_front_pair),
            "left_leg": swing(hind, phase_hind), "right_leg": swing(hind, phase_hind + phase_front_pair)}


A = {}

A["idle"] = dict(loop=True, length=3.0, bones=merge(tail_wave(3.0, 6), {
    "body": {"position": keys((0, Z), (1.5, [0, -0.6, 0]), (3, Z))},
    "neck": {"rotation": keys((0, Z), (1.0, [-4, 6, 0]), (2.0, [-2, -6, 0]), (3, Z))},
    "head": {"rotation": keys((0, Z), (1.5, [4, 0, 0]), (3, Z))},
}))

# 歩き：右前と左後ろ、左前と右後ろが同時に出る
A["walk"] = dict(loop=True, length=1.0, bones=merge(tail_wave(1.0, 5), gait(1.0, 18, 18), {
    "body": {"position": keys((0, Z), (0.25, [0, -0.8, 0]), (0.5, Z), (0.75, [0, -0.8, 0]), (1.0, Z))},
    "neck": {"rotation": keys((0, [3, 0, 0]), (0.5, [-3, 0, 0]), (1.0, [3, 0, 0]))},
}))

# 走り：前脚2本・後ろ脚2本がそろって跳ねる（ネコ科の全力疾走）
A["run"] = dict(loop=True, length=0.6, bones=merge(tail_wave(0.6, 4, 6), gait(0.6, 40, 40, phase_front_pair=0.08, phase_hind=0.5), {
    "body": {"rotation": keys((0, [-6, 0, 0]), (0.3, [6, 0, 0]), (0.6, [-6, 0, 0])),
             "position": keys((0, [0, 1, 0]), (0.3, [0, -1.5, 0]), (0.6, [0, 1, 0]))},
    "neck": {"rotation": keys((0, [10, 0, 0]), (0.3, [16, 0, 0]), (0.6, [10, 0, 0]))},
}))

# 飛びかかり：0.5 秒までためて、0.5〜0.9 秒で跳ぶ、0.9 秒に着地して斬る
A["pounce"] = dict(loop=False, length=1.4, bones=merge(
    tail_pose([(0, Z), (0.5, [-15, 0, 0]), (0.7, [20, 0, 0]), (0.9, [-10, 0, 0]), (1.4, Z)]), {
    "body": {"rotation": keys((0, Z), (0.5, [8, 0, 0]), (0.7, [-18, 0, 0]), (0.9, [10, 0, 0]), (1.4, Z)),
             "position": keys((0, Z), (0.5, [0, -4, 0]), (0.7, [0, 2, 0]), (0.9, [0, -3, 0]), (1.4, Z))},
    "right_wing": {"rotation": keys((0, Z), (0.5, [20, 0, 0]), (0.7, [-75, 0, 10]), (0.9, [-30, 0, 0]), (1.4, Z))},
    "left_wing": {"rotation": keys((0, Z), (0.5, [20, 0, 0]), (0.7, [-75, 0, -10]), (0.9, [-30, 0, 0]), (1.4, Z))},
    "right_leg": {"rotation": keys((0, Z), (0.5, [-20, 0, 0]), (0.7, [55, 0, 0]), (0.9, [10, 0, 0]), (1.4, Z))},
    "left_leg": {"rotation": keys((0, Z), (0.5, [-20, 0, 0]), (0.7, [55, 0, 0]), (0.9, [10, 0, 0]), (1.4, Z))},
    "neck": {"rotation": keys((0, Z), (0.5, [10, 0, 0]), (0.7, [-10, 0, 0]), (0.9, [15, 0, 0]), (1.4, Z))},
}))

# 翼の剣で斬る：右の翼を外へ振りかぶって 0.6 秒に内側へなぎ払う
A["wing_slash"] = dict(loop=False, length=1.2, bones=merge(tail_pose([(0, Z), (0.6, [0, -20, 0]), (1.2, Z)]), {
    "body": {"rotation": keys((0, Z), (0.4, [-12, 20, 0]), (0.6, [-6, -25, 0]), (0.8, [-6, -25, 0]), (1.2, Z))},
    "right_wing": {"rotation": keys((0, Z), (0.4, [-50, 0, 55]), (0.6, [-80, 0, -15]), (0.8, [-75, 0, -20]), (1.2, Z))},
    "left_wing": {"rotation": keys((0, Z), (0.4, [-10, 0, -15]), (1.2, Z))},
    "neck": {"rotation": keys((0, Z), (0.4, [-10, 15, 0]), (0.6, [5, -10, 0]), (1.2, Z))},
}))

# 回転斬り：0.6〜1.1 秒で尻尾ごと一回転（0.85 秒に周り全部に当たる）
A["tail_spin"] = dict(loop=False, length=1.6, bones=merge(
    tail_pose([(0, Z), (0.6, [-10, 0, 0]), (0.85, [-5, 0, 0]), (1.1, Z), (1.6, Z)]), {
    "root": {"rotation": keys((0, Z), (0.6, [0, -20, 0]), (1.1, [0, 360, 0]), (1.11, Z), (1.6, Z))},
    "body": {"position": keys((0, Z), (0.6, [0, -3, 0]), (1.1, [0, -2, 0]), (1.6, Z))},
    "right_wing": {"rotation": keys((0, Z), (0.6, [0, 0, 40]), (1.1, [0, 0, 40]), (1.6, Z))},
    "left_wing": {"rotation": keys((0, Z), (0.6, [0, 0, -40]), (1.1, [0, 0, -40]), (1.6, Z))},
}))

# 剣を飛ばす：翼を大きく広げて 0.7 秒に前へ振る
A["sword_volley"] = dict(loop=False, length=1.4, bones=merge(tail_pose([(0, Z), (0.5, [15, 0, 0]), (1.4, Z)]), {
    "body": {"rotation": keys((0, Z), (0.5, [-18, 0, 0]), (0.7, [5, 0, 0]), (1.0, [5, 0, 0]), (1.4, Z)),
             "position": keys((0, Z), (0.5, [0, 3, 0]), (0.7, Z), (1.4, Z))},
    "right_wing": {"rotation": keys((0, Z), (0.5, [-40, 0, 65]), (0.7, [-70, 0, 10]), (1.0, [-70, 0, 10]), (1.4, Z))},
    "left_wing": {"rotation": keys((0, Z), (0.5, [-40, 0, -65]), (0.7, [-70, 0, -10]), (1.0, [-70, 0, -10]), (1.4, Z))},
    "neck": {"rotation": keys((0, Z), (0.5, [-15, 0, 0]), (0.7, [10, 0, 0]), (1.4, Z))},
}))

# 刃の道：後ろ足で立ち上がって 0.8 秒に前脚を地面へ叩きつける
A["blade_path"] = dict(loop=False, length=1.6, bones=merge(tail_pose([(0, Z), (0.6, [-20, 0, 0]), (0.8, [10, 0, 0]), (1.6, Z)]), {
    "body": {"rotation": keys((0, Z), (0.6, [-30, 0, 0]), (0.8, [10, 0, 0]), (1.1, [10, 0, 0]), (1.6, Z)),
             "position": keys((0, Z), (0.6, [0, 5, 0]), (0.8, [0, -2, 0]), (1.1, [0, -2, 0]), (1.6, Z))},
    "right_wing": {"rotation": keys((0, Z), (0.6, [-80, 0, 20]), (0.8, [-20, 0, 0]), (1.1, [-20, 0, 0]), (1.6, Z))},
    "left_wing": {"rotation": keys((0, Z), (0.6, [-80, 0, -20]), (0.8, [-20, 0, 0]), (1.1, [-20, 0, 0]), (1.6, Z))},
    "neck": {"rotation": keys((0, Z), (0.6, [-20, 0, 0]), (0.8, [20, 0, 0]), (1.6, Z))},
}))

# 影に溶けて消える（0.5 秒で平たい影になる）
A["vanish"] = dict(loop=False, length=0.6, bones={
    "root": {"scale": keys((0, ONE), (0.3, [1.1, 0.5, 1.1]), (0.5, [1.3, 0.02, 1.3]), (0.6, [1.3, 0.02, 1.3]))},
})
# 影から現れる
A["emerge"] = dict(loop=False, length=0.6, bones={
    "root": {"scale": keys((0, [1.3, 0.02, 1.3]), (0.3, [0.9, 1.2, 0.9]), (0.45, [1.05, 0.95, 1.05]), (0.6, ONE))},
    "neck": {"rotation": keys((0, [15, 0, 0]), (0.3, [-15, 0, 0]), (0.6, Z))},
})

# 第2形態へ：立ち上がって吠え、翼の剣を広げる（1.4 秒に目と刃が赤く）
A["roar"] = dict(loop=False, length=3.0, bones=merge(tail_wave(3.0, 15, 0, steps=12), {
    "body": {"rotation": keys((0, Z), (0.8, [8, 0, 0]), (1.4, [-35, 0, 0]), (2.4, [-35, 0, 0]), (3.0, Z)),
             "position": keys((0, Z), (0.8, [0, -3, 0]), (1.4, [0, 6, 0]), (2.4, [0, 6, 0]), (3.0, Z))},
    "neck": {"rotation": keys((0, Z), (0.8, [20, 0, 0]), (1.4, [-30, 0, 0]), (2.4, [-30, 0, 0]), (3.0, Z))},
    "head": {"rotation": keys((0, Z), (1.4, [-25, 0, 0]), (2.4, [-25, 0, 0]), (3.0, Z))},
    "right_wing": {"rotation": keys((0, Z), (0.8, [10, 0, 0]), (1.4, [-50, 0, 75]), (2.4, [-50, 0, 75]), (3.0, Z))},
    "left_wing": {"rotation": keys((0, Z), (0.8, [10, 0, 0]), (1.4, [-50, 0, -75]), (2.4, [-50, 0, -75]), (3.0, Z))},
}))

# 急降下（第2形態）：0.6 秒で飛び立ち、羽ばたいて浮かび、2.2〜2.6 秒で頭から突っ込む
FLAP = [(t, [-30, 0, 70 if (i % 2 == 0) else 25]) for i, t in enumerate([0.6, 0.8, 1.0, 1.2, 1.4, 1.6, 1.8, 2.0, 2.2])]
A["dive"] = dict(loop=False, length=3.0, bones=merge(tail_pose([(0, Z), (0.6, [-15, 0, 0]), (2.2, [-10, 0, 0]), (2.6, [25, 0, 0]), (3.0, Z)]), {
    "body": {"rotation": keys((0, Z), (0.5, [10, 0, 0]), (0.8, [-20, 0, 0]), (2.2, [-10, 0, 0]), (2.4, [35, 0, 0]), (2.6, [10, 0, 0]), (3.0, Z)),
             "position": keys((0, Z), (0.5, [0, -4, 0]), (0.8, Z), (2.6, [0, -3, 0]), (3.0, Z))},
    "right_wing": {"rotation": keys((0, Z), (0.5, [20, 0, 0]), *FLAP, (2.4, [-60, 0, 20]), (2.6, [-30, 0, 0]), (3.0, Z))},
    "left_wing": {"rotation": keys((0, Z), (0.5, [20, 0, 0]), *[(t, [v[0], 0, -v[2]]) for t, v in FLAP],
                                   (2.4, [-60, 0, -20]), (2.6, [-30, 0, 0]), (3.0, Z))},
    "right_leg": {"rotation": keys((0, Z), (0.8, [40, 0, 0]), (2.2, [40, 0, 0]), (2.6, [-10, 0, 0]), (3.0, Z))},
    "left_leg": {"rotation": keys((0, Z), (0.8, [40, 0, 0]), (2.2, [40, 0, 0]), (2.6, [-10, 0, 0]), (3.0, Z))},
    "neck": {"rotation": keys((0, Z), (0.8, [-10, 0, 0]), (2.2, [10, 0, 0]), (2.4, [25, 0, 0]), (3.0, Z))},
}))

A["death"] = dict(loop="hold_on_last_frame", length=3.0, bones=merge(
    tail_pose([(0, Z), (0.8, [-10, 15, 0]), (2.0, [-8, 20, 0]), (3.0, [-8, 20, 0])]), {
    "body": {"rotation": keys((0, Z), (0.6, [-15, 0, 0]), (1.6, [5, 0, 25]), (3.0, [5, 0, 30])),
             "position": keys((0, Z), (0.6, [0, 2, 0]), (1.6, [0, -9, 0]), (3.0, [0, -10, 0]))},
    "neck": {"rotation": keys((0, Z), (0.6, [-25, 0, 0]), (1.6, [30, 0, 15]), (3.0, [35, 0, 20]))},
    "head": {"rotation": keys((0, Z), (1.6, [10, 0, 0]), (3.0, [15, 0, 0]))},
    "right_wing": {"rotation": keys((0, Z), (0.6, [-30, 0, 40]), (1.6, [-20, 0, 70]), (3.0, [-20, 0, 75]))},
    "left_wing": {"rotation": keys((0, Z), (0.6, [-30, 0, -40]), (1.6, [30, 0, -10]), (3.0, [30, 0, -10]))},
    "right_leg": {"rotation": keys((0, Z), (1.6, [40, 0, 20]), (3.0, [45, 0, 20]))},
    "left_leg": {"rotation": keys((0, Z), (1.6, [-30, 0, 0]), (3.0, [-30, 0, 0]))},
}))


def to_json():
    return {"format_version": "1.8.0", "animations": {
        f"animation.blade_dragon.{n}": {"loop": a["loop"], "animation_length": a["length"], "bones": a["bones"]}
        for n, a in A.items()}}


if __name__ == "__main__":
    os.makedirs(OUT, exist_ok=True)
    with open(f"{OUT}/blade_dragon.animation.json", "w") as f:
        json.dump(to_json(), f, indent=1)
    print(len(A), "animations")
