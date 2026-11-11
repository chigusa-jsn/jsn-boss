"""氷の魔女のアニメーション。 python3 tools/ice_witch_anims.py
角度の決まりは fire_knight_anims.py と同じ（X マイナス＝腕を前に上げる）。
右腕はもともと Z -20、杖は Z +20 回っている（モデル側の初期値）。
"""
import json, os

OUT = "src/main/resources/assets/jsnboss/animations/entity"
Z = [0, 0, 0]
ONE = [1, 1, 1]


def keys(*pairs):
    return {f"{float(t)}": v for t, v in pairs}


def spin(length, turns_of_120=1):
    """周りの結晶をぐるっと回す（3つが120度ずつ並ぶので、120度の倍数ならつなぎ目が見えない）"""
    return {"orbit": {"rotation": keys((0, Z), (length, [0, 120 * turns_of_120, 0]))}}


def merge(*parts):
    out = {}
    for p in parts:
        for bone, ch in p.items():
            out.setdefault(bone, {}).update(ch)
    return out


A = {}

A["idle"] = dict(loop=True, length=3.0, bones=merge(spin(3.0), {
    "body": {"position": keys((0, Z), (1.5, [0, 1.5, 0]), (3, Z))},
    "robe": {"rotation": keys((0, [2, 0, 0]), (1.5, [-2, 0, 0]), (3, [2, 0, 0]))},
    "head": {"rotation": keys((0, Z), (1.5, [3, 4, 0]), (3, Z))},
    "left_arm": {"rotation": keys((0, [-10, 0, 12]), (1.5, [-14, 0, 16]), (3, [-10, 0, 12]))},
    "hat": {"rotation": keys((0, Z), (1.5, [-2, 0, 2]), (3, Z))},
}))

A["walk"] = dict(loop=True, length=1.5, bones=merge(spin(1.5), {
    "body": {"rotation": keys((0, [8, 0, 0]), (1.5, [8, 0, 0])),
             "position": keys((0, Z), (0.75, [0, 1, 0]), (1.5, Z))},
    "robe": {"rotation": keys((0, [10, 0, 0]), (0.75, [14, 0, 0]), (1.5, [10, 0, 0]))},
    "left_arm": {"rotation": keys((0, [15, 0, 15]), (0.75, [20, 0, 18]), (1.5, [15, 0, 15]))},
    "hat": {"rotation": keys((0, [-6, 0, 0]), (1.5, [-6, 0, 0]))},
}))

# 氷の槍：杖を前に向けて 0.6 / 0.8 / 1.0 秒に発射
A["cast_spear"] = dict(loop=False, length=1.6, bones=merge(spin(1.6), {
    "right_arm": {"rotation": keys((0, Z), (0.45, [-82, 0, 18]), (0.6, [-74, 0, 18]), (0.7, [-82, 0, 18]),
                                    (0.8, [-74, 0, 18]), (0.9, [-82, 0, 18]), (1.0, [-74, 0, 18]), (1.25, [-80, 0, 18]), (1.6, Z))},
    "left_arm": {"rotation": keys((0, Z), (0.45, [-45, 0, 25]), (1.25, [-45, 0, 25]), (1.6, Z))},
    "torso": {"rotation": keys((0, Z), (0.45, [0, 12, 0]), (1.25, [0, 12, 0]), (1.6, Z))},
    "robe": {"rotation": keys((0, Z), (0.6, [6, 0, 0]), (1.0, [6, 0, 0]), (1.6, Z))},
}))

# 氷のトゲ：杖を振り上げて 0.7 秒に地面へ突く
A["cast_spikes"] = dict(loop=False, length=1.4, bones=merge(spin(1.4), {
    "right_arm": {"rotation": keys((0, Z), (0.5, [-165, 0, 20]), (0.7, [-40, 0, 20]), (1.0, [-40, 0, 20]), (1.4, Z))},
    "left_arm": {"rotation": keys((0, Z), (0.5, [-120, 0, 30]), (0.7, [-20, 0, 30]), (1.4, Z))},
    "body": {"position": keys((0, Z), (0.5, [0, 3, 0]), (0.7, [0, -1, 0]), (1.0, [0, -1, 0]), (1.4, Z))},
    "torso": {"rotation": keys((0, Z), (0.5, [-10, 0, 0]), (0.7, [15, 0, 0]), (1.0, [15, 0, 0]), (1.4, Z))},
    "hat": {"rotation": keys((0, Z), (0.7, [8, 0, 0]), (1.4, Z))},
}))

# 凍てつく大地：両腕を広げて 0.9 秒に冷気を放つ
A["cast_field"] = dict(loop=False, length=1.6, bones=merge(spin(1.6, 2), {
    "right_arm": {"rotation": keys((0, Z), (0.6, [-30, 0, -50]), (0.9, [-50, 0, -40]), (1.2, [-50, 0, -40]), (1.6, Z))},
    "left_arm": {"rotation": keys((0, Z), (0.6, [-30, 0, 70]), (0.9, [-50, 0, 60]), (1.2, [-50, 0, 60]), (1.6, Z))},
    "head": {"rotation": keys((0, Z), (0.6, [-15, 0, 0]), (0.9, [10, 0, 0]), (1.6, Z))},
    "body": {"position": keys((0, Z), (0.6, [0, 2, 0]), (0.9, [0, 0, 0]), (1.6, Z))},
    "orbit": {"scale": keys((0, ONE), (0.6, [1.3, 1, 1.3]), (0.9, [1.8, 1, 1.8]), (1.6, ONE))},
}))

# 氷の爆発（近づかれたとき）：0.6 秒に杖を地面に叩きつける
A["nova"] = dict(loop=False, length=1.2, bones=merge(spin(1.2, 2), {
    "right_arm": {"rotation": keys((0, Z), (0.4, [-170, 0, 20]), (0.6, [-20, 0, 20]), (0.85, [-20, 0, 20]), (1.2, Z))},
    "left_arm": {"rotation": keys((0, Z), (0.4, [-170, 0, -20]), (0.6, [-20, 0, 40]), (1.2, Z))},
    "body": {"position": keys((0, Z), (0.4, [0, 2, 0]), (0.6, [0, -2, 0]), (0.85, [0, -2, 0]), (1.2, Z))},
    "torso": {"rotation": keys((0, Z), (0.4, [-8, 0, 0]), (0.6, [20, 0, 0]), (0.85, [20, 0, 0]), (1.2, Z))},
}))

# テレポート：0.5 秒で細く消えて、移動先でつぶれた形から戻る
A["teleport"] = dict(loop=False, length=1.0, bones=merge(spin(1.0, 2), {
    "root": {"scale": keys((0, ONE), (0.3, [0.6, 1.3, 0.6]), (0.5, [0.05, 1.8, 0.05]), (0.55, [0.05, 0.05, 0.05]),
                           (0.8, [1.3, 0.7, 1.3]), (1.0, ONE))},
    "right_arm": {"rotation": keys((0, Z), (0.3, [-20, 0, -30]), (0.8, [-20, 0, -30]), (1.0, Z))},
    "left_arm": {"rotation": keys((0, Z), (0.3, [-20, 0, 50]), (0.8, [-20, 0, 50]), (1.0, Z))},
}))

# 雹（第2形態）：杖を空に掲げる 0.8〜1.6 秒
A["cast_hail"] = dict(loop=False, length=2.0, bones=merge(spin(2.0, 3), {
    "right_arm": {"rotation": keys((0, Z), (0.8, [-175, 0, 15]), (1.6, [-178, 0, 15]), (2.0, Z))},
    "left_arm": {"rotation": keys((0, Z), (0.8, [-160, 0, -10]), (1.6, [-165, 0, -10]), (2.0, Z))},
    "head": {"rotation": keys((0, Z), (0.8, [-25, 0, 0]), (1.6, [-25, 0, 0]), (2.0, Z))},
    "body": {"position": keys((0, Z), (0.8, [0, 3, 0]), (1.6, [0, 3, 0]), (2.0, Z))},
    "hat": {"rotation": keys((0, Z), (0.8, [-10, 0, 0]), (2.0, Z))},
}))

# 分身（第2形態）
A["summon_clones"] = dict(loop=False, length=1.4, bones=merge(spin(1.4, 2), {
    "right_arm": {"rotation": keys((0, Z), (0.5, [-60, 0, -40]), (0.7, [-90, 0, 0]), (1.0, [-90, 0, 0]), (1.4, Z))},
    "left_arm": {"rotation": keys((0, Z), (0.5, [-60, 0, 60]), (0.7, [-90, 0, 0]), (1.0, [-90, 0, 0]), (1.4, Z))},
    "body": {"position": keys((0, Z), (0.7, [0, 2, 0]), (1.4, Z))},
    "orbit": {"scale": keys((0, ONE), (0.5, [0.4, 1, 0.4]), (0.7, [2.0, 1.4, 2.0]), (1.4, ONE))},
}))

# 絶対零度（第2形態の大技）：浮き上がって 3.2 秒に全方位へ冷気
A["absolute_zero"] = dict(loop=False, length=4.0, bones=merge(spin(4.0, 6), {
    "root": {"position": keys((0, Z), (0.8, [0, 16, 0]), (3.5, [0, 16, 0]), (4.0, Z))},
    "right_arm": {"rotation": keys((0, Z), (0.8, [-10, 0, -70]), (3.0, [-15, 0, -75]), (3.2, [-100, 0, 0]), (3.5, [-100, 0, 0]), (4.0, Z))},
    "left_arm": {"rotation": keys((0, Z), (0.8, [-10, 0, 90]), (3.0, [-15, 0, 95]), (3.2, [-100, 0, 0]), (3.5, [-100, 0, 0]), (4.0, Z))},
    "head": {"rotation": keys((0, Z), (0.8, [-30, 0, 0]), (3.0, [-30, 0, 0]), (3.2, [10, 0, 0]), (4.0, Z))},
    "robe": {"rotation": keys((0, Z), (0.8, [15, 0, 0]), (3.2, [-10, 0, 0]), (4.0, Z))},
    "orbit": {"scale": keys((0, ONE), (0.8, [1.5, 1, 1.5]), (3.0, [2.2, 1.5, 2.2]), (3.2, [0.3, 1, 0.3]), (3.5, [2.5, 1, 2.5]), (4.0, ONE))},
}))

A["phase_change"] = dict(loop=False, length=3.0, bones=merge(spin(3.0, 6), {
    "root": {"position": keys((0, Z), (1.0, [0, 8, 0]), (2.2, [0, 8, 0]), (3.0, Z))},
    "right_arm": {"rotation": keys((0, Z), (1.0, [-20, 0, -80]), (1.4, [-30, 0, -100]), (2.2, [-30, 0, -100]), (3.0, Z))},
    "left_arm": {"rotation": keys((0, Z), (1.0, [-20, 0, 100]), (1.4, [-30, 0, 120]), (2.2, [-30, 0, 120]), (3.0, Z))},
    "head": {"rotation": keys((0, Z), (1.0, [15, 0, 0]), (1.4, [-35, 0, 0]), (2.2, [-35, 0, 0]), (3.0, Z))},
    "orbit": {"scale": keys((0, ONE), (1.0, [0.5, 1, 0.5]), (1.4, [2.4, 1.6, 2.4]), (2.2, [2.0, 1.4, 2.0]), (3.0, ONE))},
    "robe": {"rotation": keys((0, Z), (1.4, [20, 0, 0]), (3.0, Z))},
}))

A["death"] = dict(loop="hold_on_last_frame", length=3.0, bones={
    "body": {"position": keys((0, Z), (1.5, [0, -4, 0]), (3.0, [0, -5, 0]))},
    "torso": {"rotation": keys((0, Z), (0.5, [-15, 0, 0]), (1.5, [35, 0, 0]), (3.0, [40, 0, 0]))},
    "head": {"rotation": keys((0, Z), (0.5, [-25, 0, 0]), (1.5, [30, 0, 0]), (3.0, [35, 0, 0]))},
    "hat": {"rotation": keys((0, Z), (1.5, [-30, 0, 20]), (3.0, [-60, 0, 30])),
            "position": keys((0, Z), (1.5, [0, 2, 3]), (3.0, [-3, -18, 6]))},
    "right_arm": {"rotation": keys((0, Z), (0.5, [-40, 0, -30]), (1.5, [10, 0, 0]), (3.0, [15, 0, 0]))},
    "staff": {"rotation": keys((0, Z), (1.5, [0, 0, 60]), (3.0, [0, 0, 85]))},
    "left_arm": {"rotation": keys((0, Z), (0.5, [-40, 0, 40]), (1.5, [10, 0, 5]), (3.0, [15, 0, 5]))},
    "orbit": {"position": keys((0, Z), (1.2, [0, -24, 0]), (3.0, [0, -26, 0])),
              "scale": keys((0, ONE), (1.2, [1.2, 1, 1.2]), (3.0, [1.3, 0.3, 1.3]))},
    "robe": {"rotation": keys((0, Z), (1.5, [-20, 0, 0]), (3.0, [-15, 0, 0]))},
})


def to_json():
    return {"format_version": "1.8.0", "animations": {
        f"animation.ice_witch.{n}": {"loop": a["loop"], "animation_length": a["length"], "bones": a["bones"]}
        for n, a in A.items()}}


if __name__ == "__main__":
    os.makedirs(OUT, exist_ok=True)
    with open(f"{OUT}/ice_witch.animation.json", "w") as f:
        json.dump(to_json(), f, indent=1)
    print(len(A), "animations")
