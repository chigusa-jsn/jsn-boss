"""鋼鉄の機甲蜘蛛のアニメーション。 python3 tools/iron_spider_anims.py
脚：右の上脚 leg_r* は Z マイナスで先が持ち上がり、Y プラスで前へ振れる（左はどちらも逆）。
ノコギリの回転は別のコントローラーの "saws" がずっと回し続ける（ほかのアニメでは saw_right/left を触らない）。
"""
import json, os

OUT = "src/main/resources/assets/jsnboss/animations/entity"
Z = [0, 0, 0]
ONE = [1, 1, 1]


def keys(*pairs):
    return {f"{round(float(t), 3)}": v for t, v in pairs}


def merge(*parts):
    out = {}
    for p in parts:
        for b, ch in p.items():
            out.setdefault(b, {}).update(ch)
    return out


def leg(side, i, pairs):
    """pairs = [(t, lift, swing)]。lift>0 で脚を上げ、swing>0 で前へ"""
    s = 1 if side == "r" else -1
    return {f"leg_{side}{i}": {"rotation": keys(*[(t, [0, s * sw, -s * lf]) for t, lf, sw in pairs])}}


def all_legs(pairs):
    out = {}
    for side in "rl":
        for i in range(3):
            out.update(leg(side, i, pairs))
    return out


def stacks(*pairs):
    return {f"stack{i}": {"scale": keys(*pairs)} for i in range(2)}


A = {}

A["saws"] = dict(loop=True, length=0.4, bones={
    "saw_right": {"rotation": keys((0, Z), (0.1, [-90, 0, 0]), (0.2, [-180, 0, 0]), (0.3, [-270, 0, 0]), (0.4, [-360, 0, 0]))},
    "saw_left": {"rotation": keys((0, Z), (0.1, [-90, 0, 0]), (0.2, [-180, 0, 0]), (0.3, [-270, 0, 0]), (0.4, [-360, 0, 0]))},
})

A["idle"] = dict(loop=True, length=2.0, bones=merge({
    "body": {"position": keys((0, Z), (1.0, [0, -1, 0]), (2.0, Z))},
    "head": {"rotation": keys((0, Z), (0.6, [0, 15, 0]), (1.0, [0, 15, 0]), (1.5, [0, -15, 0]), (2.0, Z))},
}, stacks((0, ONE), (1.0, [1, 1.1, 1]), (2.0, ONE))))

# 三脚歩行：r0・l1・r2 と l0・r1・l2 が交互に上がる
def walk_legs(length):
    a = [(0, 25, 18), (length * 0.25, 0, 0), (length * 0.5, 0, -18), (length * 0.75, 0, 0), (length, 25, 18)]
    b = [(0, 0, -18), (length * 0.25, 0, 0), (length * 0.5, 25, 18), (length * 0.75, 0, 0), (length, 0, -18)]
    out = {}
    for side, i, ks in (("r", 0, a), ("l", 1, a), ("r", 2, a), ("l", 0, b), ("r", 1, b), ("l", 2, b)):
        out.update(leg(side, i, ks))
    return out


A["walk"] = dict(loop=True, length=0.8, bones=merge(walk_legs(0.8), {
    "body": {"position": keys((0, Z), (0.2, [0, 1, 0]), (0.4, Z), (0.6, [0, 1, 0]), (0.8, Z)),
             "rotation": keys((0, [0, 0, 2]), (0.4, [0, 0, -2]), (0.8, [0, 0, 2]))},
}))

# 回転ノコギリ：両腕を開いて、0.6〜0.9 秒に前を挟むようになぎ払う
A["saw_sweep"] = dict(loop=False, length=1.5, bones={
    "saw_arm_right": {"rotation": keys((0, Z), (0.45, [0, -45, 0]), (0.6, [0, -45, 0]), (0.9, [0, 35, 0]), (1.5, Z))},
    "saw_arm_left": {"rotation": keys((0, Z), (0.45, [0, 45, 0]), (0.6, [0, 45, 0]), (0.9, [0, -35, 0]), (1.5, Z))},
    "body": {"rotation": keys((0, Z), (0.45, [-5, 0, 0]), (0.75, [8, 0, 0]), (1.5, Z)),
             "position": keys((0, Z), (0.75, [0, 0, -3]), (1.5, Z))},
})

# ミサイル連射：砲台を上に向け、0.6〜1.6 秒に撃ち続ける（反動で揺れる）
def recoil(t0, t1, step):
    ks, t, i = [(0, Z), (t0 - 0.1, [-35, 0, 0])], t0, 0
    while t <= t1:
        ks += [(t, [-28, 0, 0]), (t + step / 2, [-35, 0, 0])]
        t += step
    ks += [(t1 + 0.3, Z)]
    return keys(*ks)


A["missiles"] = dict(loop=False, length=2.2, bones={
    "pod_right": {"rotation": recoil(0.6, 1.6, 0.2)},
    "pod_left": {"rotation": recoil(0.7, 1.7, 0.2)},
    "body": {"rotation": keys((0, Z), (0.5, [-4, 0, 0]), (1.8, [-4, 0, 0]), (2.2, Z))},
})

# 火炎放射：頭を下げ、0.6〜2.2 秒に左右へ振りながら炎を吐く
A["flamethrower"] = dict(loop=False, length=2.6, bones={
    "head": {"rotation": keys((0, Z), (0.5, [12, 0, 0]), (0.6, [12, -25, 0]), (1.4, [12, 25, 0]), (2.2, [12, -15, 0]), (2.6, Z))},
    "body": {"rotation": keys((0, Z), (0.5, [6, 0, 0]), (2.2, [6, 0, 0]), (2.6, Z))},
})

# 主砲：2 秒かけて狙いを定め（体を低く）、2.0 秒に撃つ（大きく反動）
A["main_cannon"] = dict(loop=False, length=2.8, bones=merge(all_legs([(0, 0, 0), (0.5, 10, 0), (2.0, 10, 0), (2.1, 4, 0), (2.8, 0, 0)]), {
    "body": {"position": keys((0, Z), (0.5, [0, -4, 0]), (2.0, [0, -4, 0]), (2.1, [0, -2, 5]), (2.8, Z)),
             "rotation": keys((0, Z), (0.5, [5, 0, 0]), (2.0, [5, 0, 0]), (2.1, [-12, 0, 0]), (2.8, Z))},
    "head": {"scale": keys((0, ONE), (1.8, [1.06, 1.06, 1.06]), (2.0, [1.1, 1.1, 1.1]), (2.1, ONE))},
}))

# ジャンプ踏みつけ：0.5 秒に跳び上がり、1.0 秒に着地
A["jump_slam"] = dict(loop=False, length=1.8, bones=merge(all_legs([(0, 0, 0), (0.4, 15, 0), (0.6, -20, 0), (0.95, -10, 0), (1.0, 18, 0), (1.4, 6, 0), (1.8, 0, 0)]), {
    "root": {"position": keys((0, Z), (0.4, [0, -6, 0]), (0.75, [0, 22, 0]), (1.0, Z), (1.1, [0, -4, 0]), (1.5, Z))},
}))

# 蒸気噴射：体を震わせ、排気筒がふくらむ（0.5 秒）
A["steam"] = dict(loop=False, length=1.4, bones=merge(stacks((0, ONE), (0.4, [1.3, 1.5, 1.3]), (0.5, [1.1, 0.8, 1.1]), (1.0, ONE)), {
    "body": {"rotation": keys((0, Z), (0.3, [0, 0, 2]), (0.35, [0, 0, -2]), (0.4, [0, 0, 2]), (0.45, [0, 0, -2]), (0.5, Z)),
             "position": keys((0, Z), (0.4, [0, -2, 0]), (0.5, [0, 1, 0]), (1.0, Z))},
}))

# オーバーヒート：体が沈み、脚が開いて、ガタガタ震える（3 秒）
def jitter(t0, t1, a):
    ks, t, i = [(0, Z)], t0, 0
    while t < t1:
        ks.append((t, [0, 0, a if i % 2 else -a])); t += 0.1; i += 1
    ks.append((t1 + 0.2, Z))
    return keys(*ks)


A["overheat"] = dict(loop=False, length=3.0, bones=merge(all_legs([(0, 0, 0), (0.4, 16, 0), (2.6, 16, 0), (3.0, 0, 0)]),
                                                         stacks((0, ONE), (0.3, [1.2, 1.3, 1.2]), (2.6, [1.2, 1.3, 1.2]), (3.0, ONE)), {
    "body": {"position": keys((0, Z), (0.4, [0, -7, 0]), (2.6, [0, -7, 0]), (3.0, Z)), "rotation": jitter(0.4, 2.6, 1.5)},
    "head": {"rotation": keys((0, Z), (0.4, [20, 0, 0]), (2.6, [20, 0, 0]), (3.0, Z))},
}))

A["phase_change"] = dict(loop=False, length=3.0, bones=merge(all_legs([(0, 0, 0), (0.8, 12, 0), (1.4, -12, 0), (2.4, -12, 0), (3.0, 0, 0)]),
                                                             stacks((0, ONE), (1.4, [1.4, 1.6, 1.4]), (2.4, [1.3, 1.5, 1.3]), (3.0, ONE)), {
    "body": {"position": keys((0, Z), (0.8, [0, -5, 0]), (1.4, [0, 4, 0]), (2.4, [0, 4, 0]), (3.0, Z)), "rotation": jitter(0.2, 0.8, 2)},
    "head": {"rotation": keys((0, Z), (1.4, [-20, 0, 0]), (2.4, [-20, 0, 0]), (3.0, Z)),
             "scale": keys((0, ONE), (1.3, ONE), (1.4, [1.15, 1.15, 1.15]), (1.6, ONE))},
    "pod_right": {"rotation": keys((0, Z), (1.4, [-40, 0, 0]), (2.4, [-40, 0, 0]), (3.0, Z))},
    "pod_left": {"rotation": keys((0, Z), (1.4, [-40, 0, 0]), (2.4, [-40, 0, 0]), (3.0, Z))},
}))

# 停止：脚が崩れて胴が地面に落ち、頭がうなだれる
A["death"] = dict(loop="hold_on_last_frame", length=3.0, bones=merge(all_legs([(0, 0, 0), (0.6, -8, 0), (1.4, 35, 0), (3.0, 45, 0)]), {
    "body": {"position": keys((0, Z), (0.6, [0, 2, 0]), (1.4, [0, -16, 0]), (1.6, [0, -14, 0]), (3.0, [0, -15, 0])),
             "rotation": keys((0, Z), (1.4, [6, 0, -8]), (3.0, [6, 0, -8]))},
    "head": {"rotation": keys((0, Z), (1.6, [30, 0, 10]), (3.0, [30, 0, 10]))},
    "pod_right": {"rotation": keys((0, Z), (1.6, [20, 0, 0]), (3.0, [20, 0, 0]))},
    "saw_arm_left": {"rotation": keys((0, Z), (1.6, [25, -20, 0]), (3.0, [25, -20, 0]))},
}))


def to_json():
    return {"format_version": "1.8.0", "animations": {
        f"animation.iron_spider.{n}": {"loop": a["loop"], "animation_length": a["length"], "bones": a["bones"]}
        for n, a in A.items()}}


if __name__ == "__main__":
    os.makedirs(OUT, exist_ok=True)
    with open(f"{OUT}/iron_spider.animation.json", "w") as f:
        json.dump(to_json(), f, indent=1)
    print(len(A), "animations")
