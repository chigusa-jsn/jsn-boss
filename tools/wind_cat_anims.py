"""風の猫のアニメーション。 python3 tools/wind_cat_anims.py
角度の決まりは white_fox_anims.py と同じ。尻尾は各節 X +30/+18/+12、首 X -20、頭 X +20 が初期値。
リボンは Y を振るとはためく。
"""
import json, math, os

OUT = "src/main/resources/assets/jsnboss/animations/entity"
Z = [0, 0, 0]
ONE = [1, 1, 1]


def keys(*pairs):
    return {f"{round(float(t), 3)}": v for t, v in pairs}


def merge(*parts):
    out = {}
    for p in parts:
        for bone, ch in p.items():
            out.setdefault(bone, {}).update(ch)
    return out


def wave(bone, length, amp, axis=1, phase=0.0, steps=8, base=(0, 0, 0)):
    ks = []
    for s in range(steps + 1):
        t = length * s / steps
        v = list(base)
        v[axis] = round(base[axis] + amp * math.sin(2 * math.pi * (s / steps) + phase), 2)
        ks.append((t, v))
    return {bone: {"rotation": keys(*ks)}}


def flutter(length, amp=14, fast=2):
    """リボンと尻尾が風になびく（短い周期で波打つ）"""
    parts = []
    for i in range(fast):
        pass
    out = merge(
        wave("right_ribbon", length / fast if fast > 1 else length, amp, 1, 0.0),
        wave("left_ribbon", length / fast if fast > 1 else length, amp, 1, 1.5),
        wave("tail1", length, 8, 1, 0.0), wave("tail2", length, 12, 1, 0.8), wave("tail3", length, 16, 1, 1.6))
    # 周期が短いものはループの長さに合わせて繰り返す
    if fast > 1:
        for b in ("right_ribbon", "left_ribbon"):
            ks = []
            n = 8 * fast
            for s in range(n + 1):
                t = length * s / n
                ks.append((t, [0, round(amp * math.sin(2 * math.pi * fast * s / n + (0 if b[0] == "r" else 1.5)), 2), 0]))
            out[b] = {"rotation": keys(*ks)}
    return out


def gait(length, front, hind, pair=0.5, hind_phase=0.0, steps=4):
    def swing(amp, phase):
        return {"rotation": keys(*[(length * s / steps,
                                    [round(-amp * math.cos(2 * math.pi * (s / steps + phase)), 2), 0, 0]) for s in range(steps + 1)])}
    return {"front_right": swing(front, 0), "front_left": swing(front, pair),
            "hind_left": swing(hind, hind_phase), "hind_right": swing(hind, hind_phase + pair)}


A = {}

A["idle"] = dict(loop=True, length=2.0, bones=merge(flutter(2.0, 10, 2), {
    "body": {"position": keys((0, Z), (1.0, [0, -0.4, 0]), (2.0, Z))},
    "head": {"rotation": keys((0, Z), (0.7, [0, 10, 0]), (1.4, [0, -6, 0]), (2.0, Z))},
    "right_ear": {"rotation": keys((0, Z), (1.7, Z), (1.8, [0, 0, 12]), (1.9, Z), (2.0, Z))},
}))

A["walk"] = dict(loop=True, length=1.0, bones=merge(flutter(1.0, 14, 2), gait(1.0, 20, 20), {
    "body": {"position": keys((0, Z), (0.25, [0, -0.6, 0]), (0.5, Z), (0.75, [0, -0.6, 0]), (1.0, Z))},
}))

A["run"] = dict(loop=True, length=0.5, bones=merge(flutter(0.5, 22, 2), gait(0.5, 48, 48, pair=0.08, hind_phase=0.5), {
    "body": {"rotation": keys((0, [-6, 0, 0]), (0.25, [6, 0, 0]), (0.5, [-6, 0, 0])),
             "position": keys((0, [0, 1, 0]), (0.25, [0, -1.5, 0]), (0.5, [0, 1, 0]))},
    "neck": {"rotation": keys((0, [14, 0, 0]), (0.25, [20, 0, 0]), (0.5, [14, 0, 0]))},
    "tail1": {"rotation": keys((0, [-25, 0, 0]), (0.5, [-25, 0, 0]))},
}))

# かまいたち：前脚を振りかぶって 0.5 秒に引っかく（風の刃が飛ぶ）。第2形態は左右交互に
A["kamaitachi"] = dict(loop=False, length=1.0, bones=merge(flutter(1.0, 20, 2), {
    "body": {"rotation": keys((0, Z), (0.35, [-15, 15, 0]), (0.5, [5, -15, 0]), (0.75, [-10, -15, 0]), (1.0, Z)),
             "position": keys((0, Z), (0.35, [0, 2, 0]), (0.5, Z), (1.0, Z))},
    "front_right": {"rotation": keys((0, Z), (0.35, [-110, 0, 30]), (0.5, [-30, 0, -10]), (0.75, [-20, 0, -5]), (1.0, Z))},
    "front_left": {"rotation": keys((0, Z), (0.35, [-20, 0, 0]), (0.6, [-100, 0, -30]), (0.75, [-30, 0, 10]), (1.0, Z))},
    "neck": {"rotation": keys((0, Z), (0.35, [-10, 0, 0]), (0.5, [10, 0, 0]), (1.0, Z))},
}))

# 竜巻：後ろ脚で立ち上がって身をひねり 0.9 秒に竜巻を生む
A["tornado"] = dict(loop=False, length=1.6, bones=merge(flutter(1.6, 30, 4), {
    "root": {"rotation": keys((0, Z), (0.9, [0, 360, 0]), (0.91, Z), (1.6, Z))},
    "body": {"rotation": keys((0, Z), (0.5, [-30, 0, 0]), (0.9, [-20, 0, 0]), (1.2, Z), (1.6, Z)),
             "position": keys((0, Z), (0.5, [0, 4, 0]), (0.9, [0, 3, 0]), (1.2, Z), (1.6, Z))},
    "front_right": {"rotation": keys((0, Z), (0.5, [-60, 0, 20]), (1.0, [-60, 0, 20]), (1.4, Z))},
    "front_left": {"rotation": keys((0, Z), (0.5, [-60, 0, -20]), (1.0, [-60, 0, -20]), (1.4, Z))},
}))

# 突風：踏ん張って 0.7 秒に吠え、前方へ強い風を吹きつける
A["gust"] = dict(loop=False, length=1.6, bones=merge(flutter(1.6, 35, 4), {
    "body": {"rotation": keys((0, Z), (0.5, [8, 0, 0]), (0.7, [-6, 0, 0]), (1.3, [-6, 0, 0]), (1.6, Z)),
             "position": keys((0, Z), (0.5, [0, -2, 0]), (0.7, Z), (1.6, Z))},
    "neck": {"rotation": keys((0, Z), (0.5, [15, 0, 0]), (0.7, [5, 0, 0]), (1.3, [5, 0, 0]), (1.6, Z))},
    "head": {"rotation": keys((0, Z), (0.7, [-10, 0, 0]), (1.3, [-10, 0, 0]), (1.6, Z))},
    "front_right": {"rotation": keys((0, Z), (0.5, [-25, 0, 0]), (1.3, [-25, 0, 0]), (1.6, Z))},
    "front_left": {"rotation": keys((0, Z), (0.5, [-25, 0, 0]), (1.3, [-25, 0, 0]), (1.6, Z))},
    "hind_right": {"rotation": keys((0, Z), (0.5, [25, 0, 0]), (1.3, [25, 0, 0]), (1.6, Z))},
    "hind_left": {"rotation": keys((0, Z), (0.5, [25, 0, 0]), (1.3, [25, 0, 0]), (1.6, Z))},
    "right_ear": {"rotation": keys((0, Z), (0.7, [30, 0, 20]), (1.3, [30, 0, 20]), (1.6, Z))},
    "left_ear": {"rotation": keys((0, Z), (0.7, [30, 0, -20]), (1.3, [30, 0, -20]), (1.6, Z))},
}))

# 疾風の突進：低くためて 0.5 秒から一直線に駆け抜ける体勢
A["charge"] = dict(loop=False, length=1.6, bones=merge(flutter(1.6, 30, 4), {
    "body": {"rotation": keys((0, Z), (0.45, [10, 0, 0]), (0.55, [0, 0, 0]), (1.2, [0, 0, 0]), (1.6, Z)),
             "position": keys((0, Z), (0.45, [0, -4, 0]), (0.55, [0, -1, 0]), (1.2, [0, -1, 0]), (1.6, Z))},
    "front_right": {"rotation": keys((0, Z), (0.45, [20, 0, 0]), (0.55, [-75, 0, 0]), (1.2, [-75, 0, 0]), (1.6, Z))},
    "front_left": {"rotation": keys((0, Z), (0.45, [20, 0, 0]), (0.55, [-75, 0, 0]), (1.2, [-75, 0, 0]), (1.6, Z))},
    "hind_right": {"rotation": keys((0, Z), (0.45, [-20, 0, 0]), (0.55, [70, 0, 0]), (1.2, [70, 0, 0]), (1.6, Z))},
    "hind_left": {"rotation": keys((0, Z), (0.45, [-20, 0, 0]), (0.55, [70, 0, 0]), (1.2, [70, 0, 0]), (1.6, Z))},
    "neck": {"rotation": keys((0, Z), (0.45, [20, 0, 0]), (0.55, [15, 0, 0]), (1.2, [15, 0, 0]), (1.6, Z))},
    "tail1": {"rotation": keys((0, Z), (0.55, [-35, 0, 0]), (1.2, [-35, 0, 0]), (1.6, Z))},
}))

# 空中跳び：高く跳んで 0.6 秒に空中をもう一度蹴り、1.0 秒に急降下で踏みつける
A["air_jump"] = dict(loop=False, length=1.4, bones=merge(flutter(1.4, 30, 4), {
    "body": {"rotation": keys((0, Z), (0.25, [10, 0, 0]), (0.45, [-15, 0, 0]), (0.6, [10, 0, 0]), (0.75, [-10, 0, 0]),
                              (1.0, [25, 0, 0]), (1.1, Z), (1.4, Z)),
             "position": keys((0, Z), (0.25, [0, -3, 0]), (0.45, Z), (1.4, Z))},
    "front_right": {"rotation": keys((0, Z), (0.45, [-60, 0, 0]), (0.6, [30, 0, 0]), (0.75, [-50, 0, 0]), (1.0, [-80, 0, 0]), (1.1, Z), (1.4, Z))},
    "front_left": {"rotation": keys((0, Z), (0.45, [-60, 0, 0]), (0.6, [30, 0, 0]), (0.75, [-50, 0, 0]), (1.0, [-80, 0, 0]), (1.1, Z), (1.4, Z))},
    "hind_right": {"rotation": keys((0, Z), (0.25, [-20, 0, 0]), (0.45, [50, 0, 0]), (0.6, [-30, 0, 0]), (0.75, [50, 0, 0]), (1.1, Z), (1.4, Z))},
    "hind_left": {"rotation": keys((0, Z), (0.25, [-20, 0, 0]), (0.45, [50, 0, 0]), (0.6, [-30, 0, 0]), (0.75, [50, 0, 0]), (1.1, Z), (1.4, Z))},
}))

A["phase_change"] = dict(loop=False, length=3.0, bones=merge(flutter(3.0, 40, 6), {
    "root": {"rotation": keys((0, Z), (0.8, Z), (1.4, [0, 360, 0]), (1.41, Z), (3.0, Z))},
    "body": {"rotation": keys((0, Z), (0.8, [10, 0, 0]), (1.4, [-30, 0, 0]), (2.4, [-30, 0, 0]), (3.0, Z)),
             "position": keys((0, Z), (0.8, [0, -3, 0]), (1.4, [0, 5, 0]), (2.4, [0, 5, 0]), (3.0, Z))},
    "neck": {"rotation": keys((0, Z), (1.4, [-25, 0, 0]), (2.4, [-25, 0, 0]), (3.0, Z))},
    "head": {"rotation": keys((0, Z), (1.4, [-20, 0, 0]), (2.4, [-20, 0, 0]), (3.0, Z))},
}))

A["death"] = dict(loop="hold_on_last_frame", length=3.0, bones={
    "body": {"rotation": keys((0, Z), (0.6, [-10, 0, 0]), (1.6, [5, 0, 30]), (3.0, [5, 0, 35])),
             "position": keys((0, Z), (0.6, [0, 1, 0]), (1.6, [0, -8, 0]), (3.0, [0, -9, 0]))},
    "neck": {"rotation": keys((0, Z), (0.6, [-20, 0, 0]), (1.6, [30, 0, 15]), (3.0, [35, 0, 20]))},
    "front_right": {"rotation": keys((0, Z), (1.6, [-30, 0, 30]), (3.0, [-35, 0, 35]))},
    "hind_right": {"rotation": keys((0, Z), (1.6, [30, 0, 25]), (3.0, [35, 0, 25]))},
    "tail1": {"rotation": keys((0, Z), (1.6, [-25, 20, 0]), (3.0, [-30, 25, 0]))},
    "right_ribbon": {"rotation": keys((0, Z), (1.6, [20, 10, 0]), (3.0, [30, 10, 0]))},
    "left_ribbon": {"rotation": keys((0, Z), (1.6, [20, -10, 0]), (3.0, [30, -10, 0]))},
})


def to_json():
    return {"format_version": "1.8.0", "animations": {
        f"animation.wind_cat.{n}": {"loop": a["loop"], "animation_length": a["length"], "bones": a["bones"]}
        for n, a in A.items()}}


if __name__ == "__main__":
    os.makedirs(OUT, exist_ok=True)
    with open(f"{OUT}/wind_cat.animation.json", "w") as f:
        json.dump(to_json(), f, indent=1)
    print(len(A), "animations")
