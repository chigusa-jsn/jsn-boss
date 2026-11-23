"""マザークリーパーのアニメーション。 python3 tools/creeper_mother_anims.py
火薬袋 sac_* は scale で脈打たせる。自爆は root の scale で体ごとふくらませる。
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


def sacs(*pairs):
    return {b: {"scale": keys(*pairs)} for b in ("sac_right", "sac_left", "sac_front")}


def legs4(front, back):
    """前脚と後ろ脚に同じ回転"""
    return {"leg_front_right": {"rotation": keys(*front)}, "leg_front_left": {"rotation": keys(*front)},
            "leg_back_right": {"rotation": keys(*back)}, "leg_back_left": {"rotation": keys(*back)}}


def shake(t0, t1, amp, step=0.05):
    ks, t, i = [], t0, 0
    while t < t1:
        ks.append((t, [amp if i % 2 else -amp, 0, (amp if i % 3 == 0 else -amp) * 0.6]))
        t += step; i += 1
    ks.append((t1, Z))
    return ks


A = {}

A["idle"] = dict(loop=True, length=2.0, bones=merge(sacs((0, ONE), (1.0, [1.15, 1.12, 1.15]), (2.0, ONE)), {
    "head": {"rotation": keys((0, Z), (1.0, [0, 6, 0]), (2.0, Z))},
    "body": {"position": keys((0, Z), (1.0, [0, 0.4, 0]), (2.0, Z))},
}))

# クリーパーらしい、対角の脚が一緒に動く歩き方
A["walk"] = dict(loop=True, length=1.0, bones=merge(sacs((0, ONE), (0.5, [1.1, 1.1, 1.1]), (1.0, ONE)), {
    "leg_front_right": {"rotation": keys((0, [-30, 0, 0]), (0.5, [30, 0, 0]), (1.0, [-30, 0, 0]))},
    "leg_back_left": {"rotation": keys((0, [-30, 0, 0]), (0.5, [30, 0, 0]), (1.0, [-30, 0, 0]))},
    "leg_front_left": {"rotation": keys((0, [30, 0, 0]), (0.5, [-30, 0, 0]), (1.0, [30, 0, 0]))},
    "leg_back_right": {"rotation": keys((0, [30, 0, 0]), (0.5, [-30, 0, 0]), (1.0, [30, 0, 0]))},
    "body": {"rotation": keys((0, [0, 0, 2]), (0.5, [0, 0, -2]), (1.0, [0, 0, 2]))},
}))

# 自爆：1.5 秒かけてふくらみ（震えながら）、1.5 秒に爆発してしぼむ
A["swell"] = dict(loop=False, length=2.2, bones=merge(sacs((0, ONE), (1.5, [1.6, 1.6, 1.6]), (1.6, [0.8, 0.8, 0.8]), (2.2, ONE)), {
    "root": {"scale": keys((0, ONE), (0.5, [1.08, 1.04, 1.08]), (1.0, [1.15, 1.08, 1.15]), (1.45, [1.3, 1.15, 1.3]),
                           (1.55, [0.9, 0.95, 0.9]), (1.8, [1.03, 1.0, 1.03]), (2.2, ONE))},
    "body": {"rotation": keys(*shake(0.3, 1.5, 3))},
    "head": {"rotation": keys((0, Z), (1.4, [-12, 0, 0]), (1.55, [5, 0, 0]), (2.2, Z))},
}))

# 子クリーパー召喚：頭を上げて体を震わせる（0.9 秒に産む）
A["spawn_kids"] = dict(loop=False, length=1.6, bones=merge(sacs((0, ONE), (0.8, [1.5, 1.5, 1.5]), (0.9, [0.7, 0.7, 0.7]), (1.6, ONE)), {
    "head": {"rotation": keys((0, Z), (0.6, [-25, 0, 0]), (0.9, [10, 0, 0]), (1.6, Z))},
    "body": {"rotation": keys(*shake(0.2, 0.9, 2.5))},
}))

# 地雷まき：体をぐるっとひねりながら背中から地雷をばらまく（0.7 秒）
A["mines"] = dict(loop=False, length=1.6, bones=merge({
    "body": {"rotation": keys((0, Z), (0.4, [-10, -40, 0]), (0.7, [5, 60, 0]), (1.0, [5, 40, 0]), (1.6, Z))},
    "head": {"rotation": keys((0, Z), (0.7, [0, -30, 0]), (1.6, Z))},
}, legs4([(0, Z), (0.4, [15, 0, 0]), (0.7, [-10, 0, 0]), (1.6, Z)], [(0, Z), (0.4, [-15, 0, 0]), (0.7, [10, 0, 0]), (1.6, Z)])))

# TNT 投げ：体を後ろにそらして、0.8 秒に前へ振って背中から放り出す
A["throw_tnt"] = dict(loop=False, length=1.6, bones=merge({
    "body": {"rotation": keys((0, Z), (0.6, [-25, 0, 0]), (0.8, [25, 0, 0]), (1.1, [15, 0, 0]), (1.6, Z))},
    "head": {"rotation": keys((0, Z), (0.6, [-15, 0, 0]), (0.8, [20, 0, 0]), (1.6, Z))},
    **{f"tnt{i}": {"scale": keys((0, ONE), (0.75, ONE), (0.8, [0.2, 0.2, 0.2]), (1.3, [0.2, 0.2, 0.2]), (1.6, ONE))} for i in range(3)},
}, legs4([(0, Z), (0.6, [-15, 0, 0]), (0.8, [20, 0, 0]), (1.6, Z)], [(0, Z), (0.6, [15, 0, 0]), (0.8, [-15, 0, 0]), (1.6, Z)])))

# 体当たり：前かがみで小刻みに走る（0.5〜1.4 秒）
A["tackle"] = dict(loop=False, length=1.8, bones=merge({
    "body": {"rotation": keys((0, Z), (0.4, [-10, 0, 0]), (0.5, [20, 0, 0]), (1.4, [20, 0, 0]), (1.8, Z))},
    "head": {"rotation": keys((0, Z), (0.5, [15, 0, 0]), (1.4, [15, 0, 0]), (1.8, Z))},
    "leg_front_right": {"rotation": keys((0, Z), (0.5, [-40, 0, 0]), (0.65, [40, 0, 0]), (0.8, [-40, 0, 0]), (0.95, [40, 0, 0]), (1.1, [-40, 0, 0]), (1.25, [40, 0, 0]), (1.4, Z))},
    "leg_back_left": {"rotation": keys((0, Z), (0.5, [-40, 0, 0]), (0.65, [40, 0, 0]), (0.8, [-40, 0, 0]), (0.95, [40, 0, 0]), (1.1, [-40, 0, 0]), (1.25, [40, 0, 0]), (1.4, Z))},
    "leg_front_left": {"rotation": keys((0, Z), (0.5, [40, 0, 0]), (0.65, [-40, 0, 0]), (0.8, [40, 0, 0]), (0.95, [-40, 0, 0]), (1.1, [40, 0, 0]), (1.25, [-40, 0, 0]), (1.4, Z))},
    "leg_back_right": {"rotation": keys((0, Z), (0.5, [40, 0, 0]), (0.65, [-40, 0, 0]), (0.8, [40, 0, 0]), (0.95, [-40, 0, 0]), (1.1, [40, 0, 0]), (1.25, [-40, 0, 0]), (1.4, Z))},
}))

# 連鎖爆発（第2形態）：飛び上がって 0.7 秒に着地
A["chain"] = dict(loop=False, length=1.6, bones=merge(sacs((0, ONE), (0.7, [1.4, 1.4, 1.4]), (1.6, ONE)), {
    "root": {"position": keys((0, Z), (0.25, [0, -2, 0]), (0.45, [0, 8, 0]), (0.7, Z), (0.8, [0, -1.5, 0]), (1.1, Z))},
    "body": {"rotation": keys((0, Z), (0.45, [-10, 0, 0]), (0.7, [10, 0, 0]), (1.1, Z))},
}, legs4([(0, Z), (0.45, [-30, 0, 0]), (0.7, Z)], [(0, Z), (0.45, [30, 0, 0]), (0.7, Z)])))

A["phase_change"] = dict(loop=False, length=3.0, bones=merge(sacs((0, ONE), (1.4, [1.8, 1.8, 1.8]), (1.6, [1.3, 1.3, 1.3]), (3.0, ONE)), {
    "root": {"scale": keys((0, ONE), (1.4, [1.2, 1.1, 1.2]), (1.6, ONE), (3.0, ONE))},
    "body": {"rotation": keys(*shake(0.3, 1.4, 4))},
    "head": {"rotation": keys((0, Z), (1.4, [-20, 0, 0]), (1.6, [-25, 0, 0]), (2.6, [-25, 0, 0]), (3.0, Z))},
}))

# 倒れる：横にどさっと倒れる
A["death"] = dict(loop="hold_on_last_frame", length=3.0, bones=merge(sacs((0, ONE), (0.8, [1.3, 1.3, 1.3]), (1.4, [0.4, 0.4, 0.4]), (3.0, [0.4, 0.4, 0.4])), {
    "root": {"rotation": keys((0, Z), (0.6, [0, 0, -8]), (1.4, [0, 0, 88]), (1.6, [0, 0, 82]), (3.0, [0, 0, 85])),
             "position": keys((0, Z), (1.4, [-4, 2, 0]), (3.0, [-4, 2, 0]))},
    "head": {"rotation": keys((0, Z), (1.4, [0, 0, 10]), (3.0, [0, 0, 15]))},
}))


def to_json():
    return {"format_version": "1.8.0", "animations": {
        f"animation.creeper_mother.{n}": {"loop": a["loop"], "animation_length": a["length"], "bones": a["bones"]}
        for n, a in A.items()}}


if __name__ == "__main__":
    os.makedirs(OUT, exist_ok=True)
    with open(f"{OUT}/creeper_mother.animation.json", "w") as f:
        json.dump(to_json(), f, indent=1)
    print(len(A), "animations")
