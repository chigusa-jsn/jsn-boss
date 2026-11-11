"""雷の獅子のアニメーション。 python3 tools/thunder_lion_anims.py
角度の決まりは wind_cat_anims.py と同じ。首 X -15、頭 X +15、尻尾 X -25/+35/+15 が初期値（アニメの値は足し算）。
arc0..arc5 は体の周りの電撃。scale を 0 と 1 で切り替えて明滅させる。
"""
import json, math, os, random

OUT = "src/main/resources/assets/jsnboss/animations/entity"
Z = [0, 0, 0]


def keys(*pairs):
    return {f"{round(float(t), 3)}": v for t, v in pairs}


def merge(*parts):
    out = {}
    for p in parts:
        for bone, ch in p.items():
            out.setdefault(bone, {}).update(ch)
    return out


def wave(bone, length, amp, axis=1, phase=0.0, steps=8):
    ks = []
    for s in range(steps + 1):
        v = [0, 0, 0]
        v[axis] = round(amp * math.sin(2 * math.pi * s / steps + phase), 2)
        ks.append((length * s / steps, v))
    return {bone: {"rotation": keys(*ks)}}


def flicker(length, rate=0.1, on=0.6, seed=1):
    """体の周りの電撃をパチパチ明滅させる"""
    r = random.Random(seed)
    out = {}
    n = max(2, int(round(length / rate)))
    for i in range(6):
        ks = []
        for s in range(n + 1):
            v = 1 if (r.random() < on and s < n) else 0
            if s == n:
                v = ks[0][1][0]
            ks.append((length * s / n, [v, v, v]))
        out[f"arc{i}"] = {"scale": keys(*ks)}
    return out


def tail(length, amp=10):
    return merge(wave("tail1", length, amp * 0.6, 1, 0.0), wave("tail2", length, amp, 1, 0.9),
                 wave("tail_tuft", length, amp * 1.3, 1, 1.8))


def gait(length, front, hind, pair=0.5, hind_phase=0.0, steps=4):
    def swing(amp, phase):
        return {"rotation": keys(*[(length * s / steps,
                                    [round(-amp * math.cos(2 * math.pi * (s / steps + phase)), 2), 0, 0]) for s in range(steps + 1)])}
    return {"front_right": swing(front, 0), "front_left": swing(front, pair),
            "hind_left": swing(hind, hind_phase), "hind_right": swing(hind, hind_phase + pair)}


def legs(t_keys):
    """四本の脚に同じ回転キーを入れる（前脚と後ろ脚は別）"""
    f, h = t_keys
    return {"front_right": {"rotation": keys(*f)}, "front_left": {"rotation": keys(*f)},
            "hind_right": {"rotation": keys(*h)}, "hind_left": {"rotation": keys(*h)}}


A = {}

A["idle"] = dict(loop=True, length=2.4, bones=merge(flicker(2.4, 0.12, 0.55, 1), tail(2.4, 8), {
    "body": {"position": keys((0, Z), (1.2, [0, -0.5, 0]), (2.4, Z))},
    "head": {"rotation": keys((0, Z), (0.8, [0, 8, 0]), (1.6, [0, -8, 0]), (2.4, Z))},
    "mane": {"scale": keys((0, [1, 1, 1]), (1.2, [1.04, 1.04, 1.04]), (2.4, [1, 1, 1]))},
}))

A["walk"] = dict(loop=True, length=1.2, bones=merge(flicker(1.2, 0.1, 0.6, 2), tail(1.2, 10), gait(1.2, 18, 18), {
    "body": {"position": keys((0, Z), (0.3, [0, -0.7, 0]), (0.6, Z), (0.9, [0, -0.7, 0]), (1.2, Z))},
    "head": {"rotation": keys((0, Z), (0.3, [3, 0, 0]), (0.6, Z), (0.9, [3, 0, 0]), (1.2, Z))},
}))

A["run"] = dict(loop=True, length=0.6, bones=merge(flicker(0.6, 0.05, 0.75, 3), gait(0.6, 45, 45, pair=0.1, hind_phase=0.5), {
    "body": {"rotation": keys((0, [-5, 0, 0]), (0.3, [5, 0, 0]), (0.6, [-5, 0, 0])),
             "position": keys((0, [0, 1, 0]), (0.3, [0, -1.5, 0]), (0.6, [0, 1, 0]))},
    "neck": {"rotation": keys((0, [10, 0, 0]), (0.3, [16, 0, 0]), (0.6, [10, 0, 0]))},
    "tail1": {"rotation": keys((0, [20, 0, 0]), (0.3, [26, 0, 0]), (0.6, [20, 0, 0]))},
}))

# 突進：低く身構えて 0.6 秒から一直線に突っ込む（頭を下げてたてがみから）
A["rush"] = dict(loop=False, length=1.8, bones=merge(flicker(1.8, 0.05, 0.85, 4), {
    "body": {"rotation": keys((0, Z), (0.5, [8, 0, 0]), (0.6, [3, 0, 0]), (1.4, [3, 0, 0]), (1.8, Z)),
             "position": keys((0, Z), (0.5, [0, -4, 0]), (0.6, [0, -1, 0]), (1.4, [0, -1, 0]), (1.8, Z))},
    "neck": {"rotation": keys((0, Z), (0.5, [15, 0, 0]), (0.6, [25, 0, 0]), (1.4, [25, 0, 0]), (1.8, Z))},
    "head": {"rotation": keys((0, Z), (0.6, [10, 0, 0]), (1.4, [10, 0, 0]), (1.8, Z))},
    "tail1": {"rotation": keys((0, Z), (0.6, [30, 0, 0]), (1.4, [30, 0, 0]), (1.8, Z))},
}, legs(([(0, Z), (0.5, [25, 0, 0]), (0.6, [-70, 0, 0]), (0.8, [40, 0, 0]), (1.0, [-70, 0, 0]), (1.2, [40, 0, 0]), (1.4, [-30, 0, 0]), (1.8, Z)],
         [(0, Z), (0.5, [-25, 0, 0]), (0.6, [60, 0, 0]), (0.8, [-40, 0, 0]), (1.0, [60, 0, 0]), (1.2, [-40, 0, 0]), (1.4, [20, 0, 0]), (1.8, Z)]))))

# 落雷：後ろ脚で立ち上がって天を仰ぎ、0.9 秒に前脚を叩きつける（雷が落ちる）
A["lightning_call"] = dict(loop=False, length=2.0, bones=merge(flicker(2.0, 0.05, 0.9, 5), tail(2.0, 20), {
    "body": {"rotation": keys((0, Z), (0.6, [-35, 0, 0]), (0.9, [5, 0, 0]), (1.4, [5, 0, 0]), (2.0, Z)),
             "position": keys((0, Z), (0.6, [0, 5, 0]), (0.9, [0, -1, 0]), (1.4, Z), (2.0, Z))},
    "neck": {"rotation": keys((0, Z), (0.6, [-12, 0, 0]), (0.9, [5, 0, 0]), (2.0, Z))},
    "head": {"rotation": keys((0, Z), (0.6, [-10, 0, 0]), (0.9, [0, 0, 0]), (2.0, Z))},
    "mane": {"scale": keys((0, [1, 1, 1]), (0.6, [1.1, 1.1, 1.1]), (0.9, [1.05, 1.05, 1.05]), (2.0, [1, 1, 1]))},
}, legs(([(0, Z), (0.6, [-60, 0, 0]), (0.9, [-10, 0, 0]), (1.4, [-10, 0, 0]), (2.0, Z)],
         [(0, Z), (0.6, [35, 0, 0]), (0.9, [5, 0, 0]), (2.0, Z)]))))

# 咆哮：踏ん張って 0.7 秒に吠える（半径5の衝撃）
A["howl"] = dict(loop=False, length=1.8, bones=merge(flicker(1.8, 0.05, 0.9, 6), {
    "body": {"rotation": keys((0, Z), (0.5, [6, 0, 0]), (0.7, [-8, 0, 0]), (1.4, [-8, 0, 0]), (1.8, Z)),
             "position": keys((0, Z), (0.5, [0, -2, 0]), (0.7, Z), (1.8, Z))},
    "neck": {"rotation": keys((0, Z), (0.5, [15, 0, 0]), (0.7, [-5, 0, 0]), (1.4, [-5, 0, 0]), (1.8, Z))},
    "head": {"rotation": keys((0, Z), (0.5, [10, 0, 0]), (0.7, [-10, 0, 0]), (1.4, [-10, 0, 0]), (1.8, Z))},
    "mane": {"scale": keys((0, [1, 1, 1]), (0.7, [1.12, 1.12, 1.12]), (1.4, [1.1, 1.1, 1.1]), (1.8, [1, 1, 1]))},
    "tail1": {"rotation": keys((0, Z), (0.7, [35, 0, 0]), (1.4, [35, 0, 0]), (1.8, Z))},
}, legs(([(0, Z), (0.5, [-20, 0, 0]), (1.4, [-20, 0, 0]), (1.8, Z)], [(0, Z), (0.5, [20, 0, 0]), (1.4, [20, 0, 0]), (1.8, Z)]))))

# 地を走る雷：右前脚を振り上げて 0.6 秒に地面を引っかく（3方向に雷が走る）
A["ground_bolt"] = dict(loop=False, length=1.4, bones=merge(flicker(1.4, 0.05, 0.8, 7), tail(1.4, 14), {
    "body": {"rotation": keys((0, Z), (0.45, [-12, 10, 0]), (0.6, [8, -5, 0]), (1.0, [4, 0, 0]), (1.4, Z)),
             "position": keys((0, Z), (0.45, [0, 2, 0]), (0.6, [0, -1, 0]), (1.4, Z))},
    "front_right": {"rotation": keys((0, Z), (0.45, [-100, 0, 20]), (0.6, [10, 0, 0]), (1.0, [5, 0, 0]), (1.4, Z))},
    "front_left": {"rotation": keys((0, Z), (0.45, [-15, 0, 0]), (0.6, [5, 0, 0]), (1.4, Z))},
    "neck": {"rotation": keys((0, Z), (0.45, [-10, 0, 0]), (0.6, [15, 0, 0]), (1.4, Z))},
}))

# 雷のリング：高く跳んで 0.9 秒に着地、地面に雷の輪が広がる
A["ring"] = dict(loop=False, length=1.8, bones=merge(flicker(1.8, 0.05, 0.9, 8), {
    "root": {"position": keys((0, Z), (0.3, [0, -2, 0]), (0.6, [0, 14, 0]), (0.9, Z), (1.0, [0, -1.5, 0]), (1.3, Z), (1.8, Z))},
    "body": {"rotation": keys((0, Z), (0.3, [8, 0, 0]), (0.6, [-15, 0, 0]), (0.9, [10, 0, 0]), (1.3, Z), (1.8, Z))},
    "mane": {"scale": keys((0, [1, 1, 1]), (0.9, [1.1, 1.1, 1.1]), (1.3, [1, 1, 1]))},
}, legs(([(0, Z), (0.3, [20, 0, 0]), (0.6, [-50, 0, 0]), (0.9, [-10, 0, 0]), (1.3, Z), (1.8, Z)],
         [(0, Z), (0.3, [-20, 0, 0]), (0.6, [45, 0, 0]), (0.9, [10, 0, 0]), (1.3, Z), (1.8, Z)]))))

# 雷雲（第2形態）：空を見上げて長く吠え、雷雲を呼ぶ
A["storm"] = dict(loop=False, length=2.2, bones=merge(flicker(2.2, 0.04, 0.95, 9), tail(2.2, 18), {
    "body": {"rotation": keys((0, Z), (0.6, [-20, 0, 0]), (1.8, [-20, 0, 0]), (2.2, Z)),
             "position": keys((0, Z), (0.6, [0, 3, 0]), (1.8, [0, 3, 0]), (2.2, Z))},
    "neck": {"rotation": keys((0, Z), (0.6, [-15, 0, 0]), (1.8, [-15, 0, 0]), (2.2, Z))},
    "head": {"rotation": keys((0, Z), (0.6, [-12, 0, 0]), (0.8, [-16, 0, 0]), (1.8, [-16, 0, 0]), (2.2, Z))},
    "mane": {"scale": keys((0, [1, 1, 1]), (0.8, [1.12, 1.12, 1.12]), (1.8, [1.12, 1.12, 1.12]), (2.2, [1, 1, 1]))},
}, legs(([(0, Z), (0.6, [-25, 0, 0]), (1.8, [-25, 0, 0]), (2.2, Z)], [(0, Z), (0.6, [20, 0, 0]), (1.8, [20, 0, 0]), (2.2, Z)]))))

A["phase_change"] = dict(loop=False, length=3.0, bones=merge(flicker(3.0, 0.04, 0.95, 10), tail(3.0, 25), {
    "body": {"rotation": keys((0, Z), (0.8, [10, 0, 0]), (1.4, [-40, 0, 0]), (2.4, [-40, 0, 0]), (3.0, Z)),
             "position": keys((0, Z), (0.8, [0, -3, 0]), (1.4, [0, 6, 0]), (2.4, [0, 6, 0]), (3.0, Z))},
    "neck": {"rotation": keys((0, Z), (0.8, [20, 0, 0]), (1.4, [-12, 0, 0]), (2.4, [-12, 0, 0]), (3.0, Z))},
    "head": {"rotation": keys((0, Z), (1.4, [-12, 0, 0]), (2.4, [-12, 0, 0]), (3.0, Z))},
    "mane": {"scale": keys((0, [1, 1, 1]), (0.8, [0.9, 0.9, 0.9]), (1.4, [1.1, 1.1, 1.1]), (2.4, [1.12, 1.12, 1.12]), (3.0, [1, 1, 1]))},
}, legs(([(0, Z), (0.8, [20, 0, 0]), (1.4, [-70, 0, 0]), (2.4, [-70, 0, 0]), (3.0, Z)],
         [(0, Z), (0.8, [-20, 0, 0]), (1.4, [40, 0, 0]), (2.4, [40, 0, 0]), (3.0, Z)]))))

A["death"] = dict(loop="hold_on_last_frame", length=3.0, bones={
    **{f"arc{i}": {"scale": keys((0, [1, 1, 1]), (0.8, [1, 1, 1]), (0.9, [0, 0, 0]), (3.0, [0, 0, 0]))} for i in range(6)},
    "body": {"rotation": keys((0, Z), (0.6, [-12, 0, 0]), (1.6, [5, 0, 30]), (3.0, [5, 0, 35])),
             "position": keys((0, Z), (0.6, [0, 1, 0]), (1.6, [0, -9, 0]), (3.0, [0, -10, 0]))},
    "neck": {"rotation": keys((0, Z), (0.6, [-25, 0, 0]), (1.6, [30, 0, 15]), (3.0, [35, 0, 20]))},
    "front_right": {"rotation": keys((0, Z), (1.6, [-30, 0, 30]), (3.0, [-35, 0, 35]))},
    "hind_right": {"rotation": keys((0, Z), (1.6, [30, 0, 25]), (3.0, [35, 0, 25]))},
    "tail1": {"rotation": keys((0, Z), (1.6, [20, 20, 0]), (3.0, [25, 25, 0]))},
})


def to_json():
    return {"format_version": "1.8.0", "animations": {
        f"animation.thunder_lion.{n}": {"loop": a["loop"], "animation_length": a["length"], "bones": a["bones"]}
        for n, a in A.items()}}


if __name__ == "__main__":
    os.makedirs(OUT, exist_ok=True)
    with open(f"{OUT}/thunder_lion.animation.json", "w") as f:
        json.dump(to_json(), f, indent=1)
    print(len(A), "animations")
