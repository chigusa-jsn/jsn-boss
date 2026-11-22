"""岩の巨人のアニメーション。 python3 tools/rock_giant_anims.py
角度の決まり：X マイナス＝腕を前に上げる、X プラス＝体を前に倒す。胴はもともと X +14（前かがみ）。
結晶 crystal0..4 は scale で光の強まりを表す。
"""
import json, os

OUT = "src/main/resources/assets/jsnboss/animations/entity"
Z = [0, 0, 0]
ONE = [1, 1, 1]


def keys(*pairs):
    return {f"{round(float(t), 3)}": v for t, v in pairs}


def crystals(*pairs):
    return {f"crystal{i}": {"scale": keys(*pairs)} for i in range(5)}


def both(rk, lk=None, part="arm"):
    """右腕のキーから左腕のキーを作る（Z を反転）"""
    lk = lk or [(t, [v[0], -v[1], -v[2]]) for t, v in rk]
    return {f"right_{part}": {"rotation": keys(*rk)}, f"left_{part}": {"rotation": keys(*lk)}}


def merge(*parts):
    out = {}
    for p in parts:
        for b, ch in p.items():
            out.setdefault(b, {}).update(ch)
    return out


A = {}

A["idle"] = dict(loop=True, length=3.0, bones=merge(crystals((0, ONE), (1.5, [1.06, 1.08, 1.06]), (3.0, ONE)), {
    "torso": {"rotation": keys((0, Z), (1.5, [-2, 0, 0]), (3.0, Z))},
    "head": {"rotation": keys((0, Z), (1.0, [0, 6, 0]), (2.0, [0, -6, 0]), (3.0, Z))},
}, both([(0, [0, 0, 0]), (1.5, [-3, 0, -3]), (3.0, Z)])))

# 重い足取り。こぶしを交互に前へ
A["walk"] = dict(loop=True, length=2.0, bones=merge({
    "right_leg": {"rotation": keys((0, [-18, 0, 0]), (1.0, [18, 0, 0]), (2.0, [-18, 0, 0]))},
    "left_leg": {"rotation": keys((0, [18, 0, 0]), (1.0, [-18, 0, 0]), (2.0, [18, 0, 0]))},
    "root": {"position": keys((0, Z), (0.5, [0, -1.5, 0]), (1.0, Z), (1.5, [0, -1.5, 0]), (2.0, Z))},
    "waist": {"rotation": keys((0, [0, 5, 3]), (1.0, [0, -5, -3]), (2.0, [0, 5, 3]))},
    "head": {"rotation": keys((0, [0, -4, 0]), (1.0, [0, 4, 0]), (2.0, [0, -4, 0]))},
}, both([(0, [14, 0, 0]), (1.0, [-14, 0, 0]), (2.0, [14, 0, 0])],
        [(0, [-14, 0, 0]), (1.0, [14, 0, 0]), (2.0, [-14, 0, 0])])))

# 両こぶし叩きつけ：1.05 秒に前の地面を叩く
A["slam"] = dict(loop=False, length=2.0, bones=merge({
    "torso": {"rotation": keys((0, Z), (0.8, [-18, 0, 0]), (1.05, [32, 0, 0]), (1.5, [32, 0, 0]), (2.0, Z))},
    "root": {"position": keys((0, Z), (0.8, [0, 2, 0]), (1.05, [0, -4, 0]), (1.5, [0, -4, 0]), (2.0, Z))},
    "head": {"rotation": keys((0, Z), (0.8, [-15, 0, 0]), (1.05, [10, 0, 0]), (2.0, Z))},
    "right_leg": {"rotation": keys((0, Z), (0.8, [-8, 0, 0]), (1.5, [-12, 0, 0]), (2.0, Z))},
}, both([(0, Z), (0.8, [-170, 0, -15]), (1.05, [-55, 0, 0]), (1.5, [-55, 0, 0]), (2.0, Z)]),
   both([(0, Z), (0.8, [-20, 0, 0]), (1.05, [-10, 0, 0]), (2.0, Z)], part="forearm")))

# 薙ぎ払い：右のこぶしで周りを払う（0.85 秒）
A["sweep"] = dict(loop=False, length=1.8, bones={
    "waist": {"rotation": keys((0, Z), (0.6, [0, 35, 0]), (0.85, [0, -30, 0]), (1.1, [0, -40, 0]), (1.8, Z))},
    "torso": {"rotation": keys((0, Z), (0.6, [0, 25, 0]), (0.85, [8, -30, 0]), (1.1, [8, -35, 0]), (1.8, Z))},
    "right_arm": {"rotation": keys((0, Z), (0.6, [-40, 0, -70]), (0.85, [-75, -20, -40]), (1.1, [-70, -30, -20]), (1.8, Z))},
    "left_arm": {"rotation": keys((0, Z), (0.6, [10, 0, 20]), (0.85, [-30, 0, 30]), (1.8, Z))},
    "right_forearm": {"rotation": keys((0, Z), (0.6, [-25, 0, 0]), (1.1, [-10, 0, 0]), (1.8, Z))},
})

# 岩の柱：右こぶしを地面に突き刺す（0.8 秒）
A["pillars"] = dict(loop=False, length=1.8, bones=merge(crystals((0, ONE), (0.8, [1.2, 1.2, 1.2]), (1.8, ONE)), {
    "torso": {"rotation": keys((0, Z), (0.55, [-10, 15, 0]), (0.8, [30, -5, 0]), (1.3, [30, -5, 0]), (1.8, Z))},
    "root": {"position": keys((0, Z), (0.8, [0, -3, 0]), (1.3, [0, -3, 0]), (1.8, Z))},
    "right_arm": {"rotation": keys((0, Z), (0.55, [-150, 0, -10]), (0.8, [-40, 0, 5]), (1.3, [-40, 0, 5]), (1.8, Z))},
    "left_arm": {"rotation": keys((0, Z), (0.55, [-20, 0, 25]), (0.8, [5, 0, 20]), (1.8, Z))},
    "left_leg": {"rotation": keys((0, Z), (0.8, [-15, 0, 0]), (1.3, [-15, 0, 0]), (1.8, Z))},
}))

# 大岩投げ：0.6 秒に地面から岩をつかみ、1.2 秒に頭上へ持ち上げ、1.5 秒に投げる
A["throw"] = dict(loop=False, length=2.4, bones=merge({
    "torso": {"rotation": keys((0, Z), (0.6, [38, 0, 0]), (1.2, [-22, 0, 0]), (1.5, [25, 0, 0]), (1.9, [20, 0, 0]), (2.4, Z))},
    "root": {"position": keys((0, Z), (0.6, [0, -4, 0]), (1.2, [0, 1, 0]), (1.5, [0, -1, 0]), (2.4, Z))},
    "head": {"rotation": keys((0, Z), (1.2, [-20, 0, 0]), (1.5, [5, 0, 0]), (2.4, Z))},
    "right_leg": {"rotation": keys((0, Z), (0.6, [-25, 0, 0]), (1.2, Z), (1.5, [-15, 0, 0]), (2.4, Z))},
    "left_leg": {"rotation": keys((0, Z), (0.6, [10, 0, 0]), (1.5, [15, 0, 0]), (2.4, Z))},
}, both([(0, Z), (0.6, [-35, 0, 12]), (1.2, [-175, 0, 12]), (1.5, [-65, 0, 5]), (1.9, [-50, 0, 0]), (2.4, Z)]),
   both([(0, Z), (0.6, [-10, 0, 0]), (1.2, [-30, 0, 0]), (1.5, [-5, 0, 0]), (2.4, Z)], part="forearm")))

# 地割れ：右足を上げて 0.8 秒に踏みつけ
A["fissure"] = dict(loop=False, length=1.6, bones=merge({
    "right_leg": {"rotation": keys((0, Z), (0.55, [-45, 0, 0]), (0.8, [5, 0, 0]), (1.6, Z))},
    "left_leg": {"rotation": keys((0, Z), (0.55, [8, 0, 0]), (0.8, Z), (1.6, Z))},
    "root": {"position": keys((0, Z), (0.55, [0, 2, 0]), (0.8, [0, -2.5, 0]), (1.1, [0, -2, 0]), (1.6, Z))},
    "torso": {"rotation": keys((0, Z), (0.55, [-10, 0, 0]), (0.8, [12, 0, 0]), (1.6, Z))},
}, both([(0, Z), (0.55, [-30, 0, -40]), (0.8, [-10, 0, -20]), (1.6, Z)])))

# 落石（第2形態）：両腕を広げて天に吠え、1.0 秒に胸を叩く
A["rockfall"] = dict(loop=False, length=2.2, bones=merge(crystals((0, ONE), (1.0, [1.3, 1.3, 1.3]), (1.8, [1.25, 1.25, 1.25]), (2.2, ONE)), {
    "torso": {"rotation": keys((0, Z), (0.7, [-25, 0, 0]), (1.0, [-20, 0, 0]), (1.8, [-20, 0, 0]), (2.2, Z))},
    "head": {"rotation": keys((0, Z), (0.7, [-30, 0, 0]), (1.8, [-30, 0, 0]), (2.2, Z))},
}, both([(0, Z), (0.7, [-130, 0, -50]), (1.0, [-80, 0, 30]), (1.2, [-130, 0, -50]), (1.8, [-130, 0, -50]), (2.2, Z)])))

# 怒りの連打（第2形態）：右・左・両手の3回（0.7 / 1.4 / 2.3 秒）
A["fury"] = dict(loop=False, length=3.0, bones={
    "torso": {"rotation": keys((0, Z), (0.5, [-10, 20, 0]), (0.7, [30, -10, 0]), (1.2, [-10, -20, 0]), (1.4, [30, 10, 0]),
                               (2.0, [-20, 0, 0]), (2.3, [35, 0, 0]), (2.7, [35, 0, 0]), (3.0, Z))},
    "root": {"position": keys((0, Z), (0.7, [0, -3, 0]), (1.0, Z), (1.4, [0, -3, 0]), (2.0, [0, 1, 0]), (2.3, [0, -4, 0]), (2.7, [0, -4, 0]), (3.0, Z))},
    "right_arm": {"rotation": keys((0, Z), (0.5, [-165, 0, -15]), (0.7, [-45, 0, 0]), (1.4, [-30, 0, 0]),
                                   (2.0, [-170, 0, -15]), (2.3, [-55, 0, 0]), (2.7, [-55, 0, 0]), (3.0, Z))},
    "left_arm": {"rotation": keys((0, Z), (0.7, [-20, 0, 10]), (1.2, [-165, 0, 15]), (1.4, [-45, 0, 0]),
                                  (2.0, [-170, 0, 15]), (2.3, [-55, 0, 0]), (2.7, [-55, 0, 0]), (3.0, Z))},
})

A["phase_change"] = dict(loop=False, length=3.0, bones=merge(
    crystals((0, ONE), (0.9, [0.8, 0.8, 0.8]), (1.6, [1.4, 1.4, 1.4]), (2.4, [1.3, 1.3, 1.3]), (3.0, ONE)), {
    "torso": {"rotation": keys((0, Z), (0.9, [30, 0, 0]), (1.6, [-25, 0, 0]), (2.4, [-25, 0, 0]), (3.0, Z))},
    "root": {"position": keys((0, Z), (0.9, [0, -4, 0]), (1.6, [0, 1, 0]), (2.4, [0, 1, 0]), (3.0, Z))},
    "head": {"rotation": keys((0, Z), (0.9, [20, 0, 0]), (1.6, [-30, 0, 0]), (2.4, [-30, 0, 0]), (3.0, Z))},
}, both([(0, Z), (0.9, [-20, 0, 10]), (1.6, [-110, 0, -60]), (2.4, [-115, 0, -60]), (3.0, Z)])))

# 倒れる：ひざをついて前へ崩れ落ちる
A["death"] = dict(loop="hold_on_last_frame", length=3.0, bones=merge(
    crystals((0, ONE), (1.0, ONE), (1.6, [0.3, 0.3, 0.3]), (3.0, [0.3, 0.3, 0.3])), {
    "root": {"position": keys((0, Z), (0.8, [0, -6, 0]), (1.8, [0, -14, -4]), (3.0, [0, -16, -6]))},
    "right_leg": {"rotation": keys((0, Z), (0.8, [-70, 0, 0]), (3.0, [-80, 0, 0]))},
    "left_leg": {"rotation": keys((0, Z), (0.8, [-70, 0, 0]), (3.0, [-80, 0, 0]))},
    "torso": {"rotation": keys((0, Z), (0.8, [-10, 0, 0]), (1.8, [55, 0, 5]), (3.0, [60, 0, 5]))},
    "head": {"rotation": keys((0, Z), (1.8, [20, 0, 10]), (3.0, [25, 0, 10]))},
}, both([(0, Z), (0.8, [-30, 0, 10]), (1.8, [-80, 0, 20]), (3.0, [-85, 0, 25])])))


def to_json():
    return {"format_version": "1.8.0", "animations": {
        f"animation.rock_giant.{n}": {"loop": a["loop"], "animation_length": a["length"], "bones": a["bones"]}
        for n, a in A.items()}}


if __name__ == "__main__":
    os.makedirs(OUT, exist_ok=True)
    with open(f"{OUT}/rock_giant.animation.json", "w") as f:
        json.dump(to_json(), f, indent=1)
    print(len(A), "animations")
