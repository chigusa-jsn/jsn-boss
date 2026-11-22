"""虚空の監視者のアニメーション。 python3 tools/ender_sentinel_anims.py
角度の決まり：X マイナス＝腕を前に上げる。翼はもともと X -20、Z ±30（アニメの値は足し算）。
光の輪とドローンは別のコントローラーの "ambient" がずっと動かす（ほかのアニメでは触らない）。
伸びる腕は right_forearm / left_forearm の scale Y で表す。
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
        for b, ch in p.items():
            out.setdefault(b, {}).update(ch)
    return out


def both(rk, part="arm"):
    lk = [(t, [v[0], -v[1], -v[2]]) for t, v in rk]
    return {f"right_{part}": {"rotation": keys(*rk)}, f"left_{part}": {"rotation": keys(*lk)}}


def fins(*pairs):
    """翼を開く（Z を外へ）"""
    return {"right_fin": {"rotation": keys(*[(t, [0, 0, a]) for t, a in pairs])},
            "left_fin": {"rotation": keys(*[(t, [0, 0, -a]) for t, a in pairs])}}


A = {}

# 光の輪の回転とドローンの上下（ずっと流れ続ける）
A["ambient"] = dict(loop=True, length=4.0, bones={
    "halo": {"rotation": keys(*[(4.0 * i / 8, [0, 360 * i / 8, 0]) for i in range(9)])},
    "drone_right": {"position": keys((0, Z), (1.0, [0, 1.5, 0]), (2.0, Z), (3.0, [0, -1.5, 0]), (4.0, Z)),
                    "rotation": keys(*[(4.0 * i / 4, [0, -90 * i, 0]) for i in range(5)])},
    "drone_left": {"position": keys((0, [0, 1.5, 0]), (1.0, Z), (2.0, [0, -1.5, 0]), (3.0, Z), (4.0, [0, 1.5, 0])),
                   "rotation": keys(*[(4.0 * i / 4, [0, 90 * i, 0]) for i in range(5)])},
})

A["idle"] = dict(loop=True, length=3.0, bones=merge({
    "torso": {"rotation": keys((0, Z), (1.5, [-2, 0, 0]), (3.0, Z))},
    "head": {"rotation": keys((0, Z), (0.8, [0, 10, 3]), (1.6, [0, -10, -3]), (2.4, [0, 0, 0]), (3.0, Z))},
}, both([(0, Z), (1.5, [-3, 0, -3]), (3.0, Z)]), fins((0, 0), (1.5, 6), (3.0, 0))))

# エンダーマンらしい、腕をだらりと下げた長い歩幅
A["walk"] = dict(loop=True, length=1.6, bones=merge({
    "right_leg": {"rotation": keys((0, [-22, 0, 0]), (0.8, [22, 0, 0]), (1.6, [-22, 0, 0]))},
    "left_leg": {"rotation": keys((0, [22, 0, 0]), (0.8, [-22, 0, 0]), (1.6, [22, 0, 0]))},
    "right_arm": {"rotation": keys((0, [10, 0, 0]), (0.8, [-10, 0, 0]), (1.6, [10, 0, 0]))},
    "left_arm": {"rotation": keys((0, [-10, 0, 0]), (0.8, [10, 0, 0]), (1.6, [-10, 0, 0]))},
    "root": {"position": keys((0, Z), (0.4, [0, 1, 0]), (0.8, Z), (1.2, [0, 1, 0]), (1.6, Z))},
    "head": {"rotation": keys((0, [0, 0, 3]), (0.8, [0, 0, -3]), (1.6, [0, 0, 3]))},
}))

# テレポート斬り：0.3 秒に消えて、0.6 秒に右の爪、1.2 秒に左の爪（第2形態）
A["slash"] = dict(loop=False, length=1.6, bones=merge({
    "root": {"scale": keys((0, ONE), (0.25, [1, 1, 1]), (0.3, [0.1, 1.3, 0.1]), (0.35, ONE), (0.85, ONE), (0.9, [0.1, 1.3, 0.1]), (0.95, ONE))},
    "torso": {"rotation": keys((0, Z), (0.45, [10, 30, 0]), (0.6, [15, -30, 0]), (1.05, [10, -30, 0]), (1.2, [15, 30, 0]), (1.6, Z))},
    "right_arm": {"rotation": keys((0, Z), (0.45, [-120, 0, -40]), (0.6, [-60, 0, 30]), (0.9, [-20, 0, 10]), (1.6, Z))},
    "left_arm": {"rotation": keys((0, Z), (0.6, [10, 0, 10]), (1.05, [-120, 0, 40]), (1.2, [-60, 0, -30]), (1.6, Z))},
    "right_leg": {"rotation": keys((0, Z), (0.6, [-20, 0, 0]), (1.6, Z))},
}, fins((0, 0), (0.6, 15), (1.2, 15), (1.6, 0))))

# 虚空レーザー：1.0 秒ためて、1.0〜2.5 秒にバイザーから光線（体ごと回してなぎ払う）
A["void_laser"] = dict(loop=False, length=3.0, bones=merge({
    "head": {"rotation": keys((0, Z), (0.8, [-15, 0, 0]), (1.0, [5, 0, 0]), (2.5, [5, 0, 0]), (3.0, Z)),
             "scale": keys((0, ONE), (0.9, [1.1, 1.1, 1.1]), (1.0, ONE))},
    "torso": {"rotation": keys((0, Z), (0.8, [-12, 0, 0]), (1.0, [8, 0, 0]), (2.5, [8, 0, 0]), (3.0, Z))},
}, both([(0, Z), (0.8, [-30, 0, -50]), (1.0, [-20, 0, -60]), (2.5, [-20, 0, -60]), (3.0, Z)]),
   fins((0, 0), (0.8, 25), (2.5, 25), (3.0, 0))))

# ドローン射撃：右手を前に向けて指示を出す
A["drones"] = dict(loop=False, length=2.0, bones={
    "right_arm": {"rotation": keys((0, Z), (0.4, [-90, 15, 0]), (1.8, [-90, -15, 0]), (2.0, Z))},
    "torso": {"rotation": keys((0, Z), (0.4, [0, 10, 0]), (1.8, [0, -10, 0]), (2.0, Z))},
    "head": {"rotation": keys((0, Z), (0.4, [0, -10, 0]), (1.8, [0, 10, 0]), (2.0, Z))},
})

# 空間の裂け目：左手を天にかざして 0.7 秒に握る
A["rift"] = dict(loop=False, length=1.5, bones=merge({
    "left_arm": {"rotation": keys((0, Z), (0.5, [-170, 0, -10]), (0.7, [-160, 0, 0]), (1.1, [-160, 0, 0]), (1.5, Z))},
    "left_forearm": {"rotation": keys((0, Z), (0.5, [-10, 0, 0]), (0.7, [-40, 0, 0]), (1.1, [-40, 0, 0]), (1.5, Z))},
    "head": {"rotation": keys((0, Z), (0.5, [-25, 0, 0]), (1.1, [-25, 0, 0]), (1.5, Z))},
}, fins((0, 0), (0.7, 20), (1.1, 20), (1.5, 0))))

# 伸びる腕（第1形態）：右腕を引いて、0.6 秒に前腕が一気に伸びる
A["reach"] = dict(loop=False, length=1.7, bones={
    "right_arm": {"rotation": keys((0, Z), (0.45, [-60, 0, -25]), (0.6, [-88, 0, 0]), (1.1, [-88, 0, 0]), (1.4, [-40, 0, 0]), (1.7, Z))},
    "right_forearm": {"scale": keys((0, ONE), (0.5, [1, 0.8, 1]), (0.6, [1, 3.6, 1]), (0.95, [1, 3.6, 1]), (1.15, ONE), (1.7, ONE))},
    "torso": {"rotation": keys((0, Z), (0.45, [-5, 25, 0]), (0.6, [10, -10, 0]), (1.1, [10, -10, 0]), (1.7, Z))},
    "left_arm": {"rotation": keys((0, Z), (0.6, [15, 0, 15]), (1.7, Z))},
})

# 伸びる腕（第2形態）：両腕を伸ばして、左右から挟むようになぎ払う（0.6〜0.9 秒）
A["reach_sweep"] = dict(loop=False, length=1.8, bones={
    "right_arm": {"rotation": keys((0, Z), (0.45, [-80, 0, -80]), (0.6, [-88, 0, -60]), (0.9, [-88, 0, 15]), (1.3, [-60, 0, 0]), (1.8, Z))},
    "left_arm": {"rotation": keys((0, Z), (0.45, [-80, 0, 80]), (0.6, [-88, 0, 60]), (0.9, [-88, 0, -15]), (1.3, [-60, 0, 0]), (1.8, Z))},
    "right_forearm": {"scale": keys((0, ONE), (0.5, ONE), (0.6, [1, 3.4, 1]), (1.0, [1, 3.4, 1]), (1.3, ONE), (1.8, ONE))},
    "left_forearm": {"scale": keys((0, ONE), (0.5, ONE), (0.6, [1, 3.4, 1]), (1.0, [1, 3.4, 1]), (1.3, ONE), (1.8, ONE))},
    "torso": {"rotation": keys((0, Z), (0.45, [-10, 0, 0]), (0.9, [10, 0, 0]), (1.8, Z))},
})

# 重力異常（第2形態）：両手のひらを上に向けて持ち上げる（0.7 秒）
A["gravity"] = dict(loop=False, length=1.5, bones=merge({
    "root": {"position": keys((0, Z), (0.7, [0, 4, 0]), (1.1, [0, 4, 0]), (1.5, Z))},
    "head": {"rotation": keys((0, Z), (0.7, [-20, 0, 0]), (1.5, Z))},
}, both([(0, Z), (0.4, [-40, 0, -30]), (0.7, [-150, 0, -30]), (1.1, [-150, 0, -30]), (1.5, Z)]),
   fins((0, 0), (0.7, 30), (1.1, 30), (1.5, 0))))

# 虚空の崩壊（第2形態）：両手を前で合わせて黒い穴を作り、3.5 秒まで支えてから広げる
A["collapse"] = dict(loop=False, length=3.5, bones=merge({
    "torso": {"rotation": keys((0, Z), (0.8, [10, 0, 0]), (3.0, [10, 0, 0]), (3.2, [-15, 0, 0]), (3.5, Z))},
    "head": {"rotation": keys((0, Z), (0.8, [10, 0, 0]), (3.0, [10, 0, 0]), (3.5, Z))},
}, both([(0, Z), (0.8, [-80, 0, 40]), (3.0, [-80, 0, 40]), (3.2, [-100, 0, -70]), (3.5, Z)]),
   fins((0, 0), (0.8, 10), (3.0, 10), (3.2, 40), (3.5, 0))))

A["phase_change"] = dict(loop=False, length=3.0, bones=merge({
    "root": {"position": keys((0, Z), (0.8, [0, 2, 0]), (1.5, [0, 6, 0]), (2.4, [0, 6, 0]), (3.0, Z))},
    "torso": {"rotation": keys((0, Z), (0.8, [25, 0, 0]), (1.5, [-20, 0, 0]), (2.4, [-20, 0, 0]), (3.0, Z))},
    "head": {"rotation": keys((0, Z), (0.8, [25, 0, 0]), (1.5, [-25, 0, 0]), (2.4, [-25, 0, 0]), (3.0, Z)),
             "scale": keys((0, ONE), (1.4, ONE), (1.5, [1.2, 1.2, 1.2]), (1.7, ONE))},
}, both([(0, Z), (0.8, [10, 0, 20]), (1.5, [-30, 0, -80]), (2.4, [-30, 0, -85]), (3.0, Z)]),
   fins((0, 0), (0.8, -10), (1.5, 45), (2.4, 45), (3.0, 0))))

# 倒れる：ひざから崩れ、体が縦に引き伸ばされるように消えていく
A["death"] = dict(loop="hold_on_last_frame", length=3.0, bones=merge({
    "root": {"position": keys((0, Z), (0.8, [0, -26, 0]), (3.0, [0, -27, 0]))},
    "right_leg": {"rotation": keys((0, Z), (0.8, [75, 0, 0]), (3.0, [78, 0, 0]))},
    "left_leg": {"rotation": keys((0, Z), (0.8, [75, 0, 0]), (3.0, [78, 0, 0]))},
    "torso": {"rotation": keys((0, Z), (0.8, [-15, 0, 0]), (2.0, [35, 0, 0]), (3.0, [40, 0, 0]))},
    "head": {"rotation": keys((0, Z), (0.8, [-30, 0, 0]), (3.0, [-35, 0, 10]))},
}, both([(0, Z), (0.8, [-20, 0, -40]), (2.0, [-10, 0, -60]), (3.0, [-10, 0, -60])]), fins((0, 0), (1.0, 40), (3.0, -20))))


def to_json():
    return {"format_version": "1.8.0", "animations": {
        f"animation.ender_sentinel.{n}": {"loop": a["loop"], "animation_length": a["length"], "bones": a["bones"]}
        for n, a in A.items()}}


if __name__ == "__main__":
    os.makedirs(OUT, exist_ok=True)
    with open(f"{OUT}/ender_sentinel.animation.json", "w") as f:
        json.dump(to_json(), f, indent=1)
    print(len(A), "animations")
