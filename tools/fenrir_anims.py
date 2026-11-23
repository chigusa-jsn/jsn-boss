"""フェンリルのアニメーション。 python3 tools/fenrir_anims.py
角度の決まりは thunder_lion_anims.py と同じ。首 X -20、頭 X +20、尻尾 X -30/+20/+15 が初期値（アニメの値は足し算）。
"""
import json, math, os

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


def tail(length, amp=12):
    return merge(wave("tail1", length, amp * 0.6, 1, 0.0), wave("tail2", length, amp, 1, 0.9),
                 wave("tail3", length, amp * 1.3, 1, 1.8))


def chains(length, amp=10):
    """垂れた鎖のゆれ"""
    return merge(wave("chain_neck", length, amp, 0, 0.5), wave("chain_front", length, amp, 0, 1.3))


def gait(length, front, hind, pair=0.5, hind_phase=0.0, steps=4):
    def swing(amp, phase):
        return {"rotation": keys(*[(length * s / steps,
                                    [round(-amp * math.cos(2 * math.pi * (s / steps + phase)), 2), 0, 0]) for s in range(steps + 1)])}
    return {"front_right": swing(front, 0), "front_left": swing(front, pair),
            "hind_left": swing(hind, hind_phase), "hind_right": swing(hind, hind_phase + pair)}


def legs(f, h):
    return {"front_right": {"rotation": keys(*f)}, "front_left": {"rotation": keys(*f)},
            "hind_right": {"rotation": keys(*h)}, "hind_left": {"rotation": keys(*h)}}


def ears(*pairs):
    return {"ear_right": {"rotation": keys(*[(t, [a, 0, 0]) for t, a in pairs])},
            "ear_left": {"rotation": keys(*[(t, [a, 0, 0]) for t, a in pairs])}}


def leap(t0, t1, up=6.0):
    """t0 から t1 まで跳ぶ（体が上下し、脚を前後に伸ばす）"""
    mid = (t0 + t1) / 2
    return {"root": {"position": [(t0 - 0.15, [0, -2, 0]), (t0, Z), (mid, [0, up, 0]), (t1, Z), (t1 + 0.1, [0, -1.5, 0]), (t1 + 0.3, Z)]},
            "body": {"rotation": [(t0 - 0.15, [8, 0, 0]), (t0, [-15, 0, 0]), (mid, [-5, 0, 0]), (t1, [12, 0, 0]), (t1 + 0.3, Z)]},
            "front_right": {"rotation": [(t0 - 0.15, [20, 0, 0]), (mid, [-70, 0, 0]), (t1, [-20, 0, 0]), (t1 + 0.3, Z)]},
            "front_left": {"rotation": [(t0 - 0.15, [20, 0, 0]), (mid, [-70, 0, 0]), (t1, [-20, 0, 0]), (t1 + 0.3, Z)]},
            "hind_right": {"rotation": [(t0 - 0.15, [-25, 0, 0]), (mid, [55, 0, 0]), (t1, [10, 0, 0]), (t1 + 0.3, Z)]},
            "hind_left": {"rotation": [(t0 - 0.15, [-25, 0, 0]), (mid, [55, 0, 0]), (t1, [10, 0, 0]), (t1 + 0.3, Z)]}}


def leaps(*spans, end=None, up=6.0):
    out = {}
    for t0, t1 in spans:
        for bone, ch in leap(t0, t1, up).items():
            for k, v in ch.items():
                out.setdefault(bone, {}).setdefault(k, []).extend(v)
    res = {}
    for bone, ch in out.items():
        res[bone] = {}
        for k, v in ch.items():
            v = [(0, Z)] + v + ([(end, Z)] if end else [])
            res[bone][k] = keys(*v)
    return res


A = {}

A["idle"] = dict(loop=True, length=2.4, bones=merge(tail(2.4, 8), chains(2.4, 6), {
    "body": {"position": keys((0, Z), (1.2, [0, -0.5, 0]), (2.4, Z))},
    "head": {"rotation": keys((0, Z), (0.8, [0, 10, 0]), (1.6, [0, -10, 0]), (2.4, Z))},
}, ears((0, 0), (1.0, 0), (1.1, -15), (1.25, 0), (2.4, 0))))

A["walk"] = dict(loop=True, length=1.1, bones=merge(tail(1.1, 10), chains(1.1, 15), gait(1.1, 20, 20), {
    "body": {"position": keys((0, Z), (0.275, [0, -0.7, 0]), (0.55, Z), (0.825, [0, -0.7, 0]), (1.1, Z))},
    "head": {"rotation": keys((0, Z), (0.275, [4, 0, 0]), (0.55, Z), (0.825, [4, 0, 0]), (1.1, Z))},
}))

A["run"] = dict(loop=True, length=0.5, bones=merge(chains(0.5, 25), gait(0.5, 50, 50, pair=0.1, hind_phase=0.5), {
    "body": {"rotation": keys((0, [-6, 0, 0]), (0.25, [6, 0, 0]), (0.5, [-6, 0, 0])),
             "position": keys((0, [0, 1.5, 0]), (0.25, [0, -1.5, 0]), (0.5, [0, 1.5, 0]))},
    "neck": {"rotation": keys((0, [12, 0, 0]), (0.25, [18, 0, 0]), (0.5, [12, 0, 0]))},
    "tail1": {"rotation": keys((0, [25, 0, 0]), (0.25, [30, 0, 0]), (0.5, [25, 0, 0]))},
}, ears((0, -30), (0.5, -30))))

# 噛みつき：首を引いて 0.5 秒にがぶり
A["bite"] = dict(loop=False, length=1.2, bones=merge(ears((0, 0), (0.35, -25), (0.8, -25), (1.2, 0)), {
    "neck": {"rotation": keys((0, Z), (0.35, [-20, 0, 0]), (0.5, [30, 0, 0]), (0.8, [25, 0, 0]), (1.2, Z))},
    "head": {"rotation": keys((0, Z), (0.35, [-25, 0, 0]), (0.5, [5, 0, 0]), (1.2, Z))},
    "body": {"position": keys((0, Z), (0.35, [0, 0, 3]), (0.5, [0, -1, -4]), (0.8, [0, 0, -3]), (1.2, Z))},
}, legs([(0, Z), (0.35, [15, 0, 0]), (0.5, [-25, 0, 0]), (1.2, Z)], [(0, Z), (0.35, [-10, 0, 0]), (0.5, [15, 0, 0]), (1.2, Z)])))

# 飛びかかり：0.5 秒に跳んで 1.0 秒に着地
A["pounce"] = dict(loop=False, length=1.6, bones=merge(ears((0, 0), (0.4, -30), (1.3, -30), (1.6, 0)), leaps((0.5, 1.0), end=1.6, up=10)))

# 爪の連撃：右・左・右（0.4 / 0.8 / 1.2 秒）
A["claws"] = dict(loop=False, length=1.6, bones=merge(ears((0, 0), (0.2, -25), (1.3, -25), (1.6, 0)), {
    "body": {"rotation": keys((0, Z), (0.25, [-15, 15, 0]), (0.4, [8, -10, 0]), (0.65, [-15, -15, 0]), (0.8, [8, 10, 0]),
                              (1.05, [-15, 15, 0]), (1.2, [8, -10, 0]), (1.6, Z))},
    "front_right": {"rotation": keys((0, Z), (0.25, [-110, 0, 20]), (0.4, [-10, 0, -10]), (0.8, [0, 0, 0]),
                                     (1.05, [-110, 0, 20]), (1.2, [-10, 0, -10]), (1.6, Z))},
    "front_left": {"rotation": keys((0, Z), (0.4, Z), (0.65, [-110, 0, -20]), (0.8, [-10, 0, 10]), (1.6, Z))},
    "neck": {"rotation": keys((0, Z), (0.4, [10, 0, 0]), (1.2, [10, 0, 0]), (1.6, Z))},
}))

# 氷の息：0.6 秒ためて、0.6〜2.2 秒に吐き続ける（首を左右に振る）
A["frost_breath"] = dict(loop=False, length=2.6, bones=merge(ears((0, 0), (0.6, -20), (2.2, -20), (2.6, 0)), {
    "neck": {"rotation": keys((0, Z), (0.5, [-20, 0, 0]), (0.6, [15, -20, 0]), (1.4, [15, 20, 0]), (2.2, [15, -15, 0]), (2.6, Z))},
    "head": {"rotation": keys((0, Z), (0.5, [-20, 0, 0]), (0.6, [-5, 0, 0]), (2.2, [-5, 0, 0]), (2.6, Z))},
    "body": {"rotation": keys((0, Z), (0.5, [-6, 0, 0]), (0.6, [4, 0, 0]), (2.2, [4, 0, 0]), (2.6, Z))},
}, legs([(0, Z), (0.6, [-15, 0, 0]), (2.2, [-15, 0, 0]), (2.6, Z)], [(0, Z), (0.6, [15, 0, 0]), (2.2, [15, 0, 0]), (2.6, Z)])))

# 鎖の鞭：その場でぐるっと一回転（0.4〜1.0 秒）、鎖が外へ振り出される
A["chain_whip"] = dict(loop=False, length=1.5, bones={
    "root": {"rotation": keys((0, Z), (0.35, [0, 20, 0]), (0.7, [0, -170, 0]), (1.0, [0, -340, 0]), (1.01, [0, 20, 0]), (1.5, Z))},
    "chain_neck": {"rotation": keys((0, Z), (0.4, [-80, 0, 0]), (1.0, [-80, 0, 0]), (1.3, Z))},
    "chain_front": {"rotation": keys((0, Z), (0.4, [-90, 0, 0]), (1.0, [-90, 0, 0]), (1.3, Z))},
    "front_right": {"rotation": keys((0, Z), (0.4, [-50, 0, 30]), (1.0, [-50, 0, 30]), (1.5, Z))},
    "body": {"rotation": keys((0, Z), (0.4, [0, 0, -10]), (1.0, [0, 0, -10]), (1.5, Z))},
})

# 遠吠え：空を見上げて 0.7〜2.0 秒に吠える
A["wolf_howl"] = dict(loop=False, length=2.4, bones=merge(tail(2.4, 20), ears((0, 0), (0.7, 10), (2.0, 10), (2.4, 0)), {
    "body": {"rotation": keys((0, Z), (0.7, [-15, 0, 0]), (2.0, [-15, 0, 0]), (2.4, Z))},
    "neck": {"rotation": keys((0, Z), (0.7, [-40, 0, 0]), (2.0, [-40, 0, 0]), (2.4, Z))},
    "head": {"rotation": keys((0, Z), (0.7, [-30, 0, 0]), (1.3, [-35, 0, 0]), (2.0, [-30, 0, 0]), (2.4, Z))},
}, legs([(0, Z), (0.7, [-10, 0, 0]), (2.0, [-10, 0, 0]), (2.4, Z)], [(0, Z), (0.7, [15, 0, 0]), (2.0, [15, 0, 0]), (2.4, Z)])))

# 月喰い（第2形態）：低く構えて、3回とびかかる（0.6 / 1.4 / 2.2 秒に跳び出す）
A["eclipse"] = dict(loop=False, length=3.0, bones=merge(ears((0, -35), (3.0, -35)), leaps((0.6, 0.9), (1.4, 1.7), (2.2, 2.5), end=3.0, up=5)))

A["phase_change"] = dict(loop=False, length=3.0, bones=merge(tail(3.0, 25), chains(3.0, 30), {
    "body": {"rotation": keys((0, Z), (0.8, [10, 0, 0]), (1.4, [-30, 0, 0]), (2.4, [-30, 0, 0]), (3.0, Z)),
             "position": keys((0, Z), (0.8, [0, -3, 0]), (1.4, [0, 5, 0]), (2.4, [0, 5, 0]), (3.0, Z))},
    "neck": {"rotation": keys((0, Z), (0.8, [20, 0, 0]), (1.4, [-35, 0, 0]), (2.4, [-35, 0, 0]), (3.0, Z))},
    "head": {"rotation": keys((0, Z), (1.4, [-30, 0, 0]), (2.4, [-30, 0, 0]), (3.0, Z))},
}, legs([(0, Z), (0.8, [20, 0, 0]), (1.4, [-60, 0, 0]), (2.4, [-60, 0, 0]), (3.0, Z)],
        [(0, Z), (0.8, [-20, 0, 0]), (1.4, [40, 0, 0]), (2.4, [40, 0, 0]), (3.0, Z)])))

A["death"] = dict(loop="hold_on_last_frame", length=3.0, bones={
    "body": {"rotation": keys((0, Z), (0.6, [-12, 0, 0]), (1.6, [5, 0, 80]), (3.0, [5, 0, 85])),
             "position": keys((0, Z), (0.6, [0, 1, 0]), (1.6, [0, -9, 0]), (3.0, [0, -10, 0]))},
    "neck": {"rotation": keys((0, Z), (0.6, [-25, 0, 0]), (1.6, [30, 0, 15]), (3.0, [35, 0, 20]))},
    "front_right": {"rotation": keys((0, Z), (1.6, [-30, 0, 30]), (3.0, [-35, 0, 35]))},
    "hind_right": {"rotation": keys((0, Z), (1.6, [30, 0, 25]), (3.0, [35, 0, 25]))},
    "tail1": {"rotation": keys((0, Z), (1.6, [20, 20, 0]), (3.0, [25, 25, 0]))},
    "ear_right": {"rotation": keys((0, Z), (1.6, [-40, 0, 0]), (3.0, [-40, 0, 0]))},
    "ear_left": {"rotation": keys((0, Z), (1.6, [-40, 0, 0]), (3.0, [-40, 0, 0]))},
})


def to_json():
    return {"format_version": "1.8.0", "animations": {
        f"animation.fenrir.{n}": {"loop": a["loop"], "animation_length": a["length"], "bones": a["bones"]}
        for n, a in A.items()}}


if __name__ == "__main__":
    os.makedirs(OUT, exist_ok=True)
    with open(f"{OUT}/fenrir.animation.json", "w") as f:
        json.dump(to_json(), f, indent=1)
    print(len(A), "animations")
