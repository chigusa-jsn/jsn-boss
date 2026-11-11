"""闇の魔導士のアニメーション。 python3 tools/dark_mage_anims.py
角度の決まりは fire_knight_anims.py と同じ。右腕はもともと Z -15、杖は Z +15、左腕は X -20 / Z +15。
"""
import json, os

OUT = "src/main/resources/assets/jsnboss/animations/entity"
Z = [0, 0, 0]
ONE = [1, 1, 1]


def keys(*pairs):
    return {f"{float(t)}": v for t, v in pairs}


def spin(length, turns_of_120=1):
    return {"orbit": {"rotation": keys((0, Z), (length, [0, 120 * turns_of_120, 0]))}}


def book(length):
    """魔導書がふわふわ浮く"""
    return {"grimoire": {"position": keys((0, Z), (length / 2, [0, 1.5, 0]), (length, Z)),
                         "rotation": keys((0, Z), (length / 2, [5, 0, -5]), (length, Z))}}


def merge(*parts):
    out = {}
    for p in parts:
        for bone, ch in p.items():
            out.setdefault(bone, {}).update(ch)
    return out


A = {}

A["idle"] = dict(loop=True, length=3.0, bones=merge(spin(3.0), book(3.0), {
    "body": {"position": keys((0, Z), (1.5, [0, 1.5, 0]), (3, Z))},
    "robe": {"rotation": keys((0, [3, 0, 2]), (1.5, [-3, 0, -2]), (3, [3, 0, 2]))},
    "head": {"rotation": keys((0, Z), (1.5, [4, -5, 0]), (3, Z))},
}))

A["walk"] = dict(loop=True, length=1.2, bones=merge(spin(1.2), book(1.2), {
    "body": {"rotation": keys((0, [12, 0, 0]), (1.2, [12, 0, 0])),
             "position": keys((0, Z), (0.6, [0, 1, 0]), (1.2, Z))},
    "robe": {"rotation": keys((0, [18, 0, 0]), (0.6, [24, 0, 0]), (1.2, [18, 0, 0]))},
    "head": {"rotation": keys((0, [-8, 0, 0]), (1.2, [-8, 0, 0]))},
}))

# 闇の弾：杖を突き出して 0.6 / 0.8 / 1.0 秒
A["cast_orbs"] = dict(loop=False, length=1.4, bones=merge(spin(1.4, 2), {
    "right_arm": {"rotation": keys((0, Z), (0.45, [-85, 0, 15]), (0.6, [-78, 0, 15]), (0.7, [-85, 0, 15]),
                                    (0.8, [-78, 0, 15]), (0.9, [-85, 0, 15]), (1.0, [-78, 0, 15]), (1.4, Z))},
    "left_arm": {"rotation": keys((0, Z), (0.45, [-40, 0, 10]), (1.0, [-40, 0, 10]), (1.4, Z))},
    "grimoire": {"position": keys((0, Z), (0.45, [-2, 4, -3]), (1.0, [-2, 4, -3]), (1.4, Z))},
    "torso": {"rotation": keys((0, Z), (0.45, [0, 15, 0]), (1.0, [0, 15, 0]), (1.4, Z))},
}))

# 背後に回って斬る：0.3 秒で消える → 0.35 秒に現れる → 0.7 秒に杖で斬る
A["blink"] = dict(loop=False, length=1.2, bones=merge(spin(1.2, 2), {
    "root": {"scale": keys((0, ONE), (0.25, [0.4, 1.4, 0.4]), (0.3, [0.02, 0.02, 0.02]), (0.35, [0.02, 0.02, 0.02]),
                           (0.5, [1.2, 0.9, 1.2]), (0.6, ONE), (1.2, ONE))},
    "right_arm": {"rotation": keys((0, Z), (0.55, [-80, 0, -50]), (0.7, [-85, 0, 50]), (0.85, [-80, 0, 55]), (1.2, Z))},
    "staff": {"rotation": keys((0, Z), (0.5, [80, 0, 0]), (0.9, [80, 0, 0]), (1.2, Z))},
    "torso": {"rotation": keys((0, Z), (0.55, [0, 40, 0]), (0.7, [10, -40, 0]), (0.85, [10, -45, 0]), (1.2, Z))},
}))

# 命を吸う鎖：左手を伸ばして 3 秒間吸い続ける
A["drain"] = dict(loop=False, length=3.0, bones=merge(spin(3.0, 3), {
    "left_arm": {"rotation": keys((0, Z), (0.4, [-75, 0, -10]), (1.5, [-70, 0, -12]), (2.6, [-75, 0, -10]), (3.0, Z))},
    "grimoire": {"position": keys((0, Z), (0.4, [-3, 6, -4]), (2.6, [-3, 6, -4]), (3.0, Z)),
                 "rotation": keys((0, Z), (0.4, [20, 0, 0]), (2.6, [20, 0, 0]), (3.0, Z))},
    "head": {"rotation": keys((0, Z), (0.4, [-10, 0, 0]), (1.0, [-8, 3, 0]), (1.6, [-10, -3, 0]), (2.6, [-10, 0, 0]), (3.0, Z))},
    "torso": {"rotation": keys((0, Z), (0.4, [-8, 0, 0]), (2.6, [-8, 0, 0]), (3.0, Z))},
}))

# 闇の爆発（近づかれたとき）：0.5 秒
A["burst"] = dict(loop=False, length=1.0, bones=merge(spin(1.0, 2), {
    "right_arm": {"rotation": keys((0, Z), (0.35, [-20, 0, 30]), (0.5, [-30, 0, -70]), (1.0, Z))},
    "left_arm": {"rotation": keys((0, Z), (0.35, [-10, 0, -30]), (0.5, [-20, 0, 70]), (1.0, Z))},
    "torso": {"rotation": keys((0, Z), (0.35, [15, 0, 0]), (0.5, [-15, 0, 0]), (1.0, Z))},
    "orbit": {"scale": keys((0, ONE), (0.35, [0.5, 1, 0.5]), (0.5, [2.0, 1, 2.0]), (1.0, ONE))},
}))

# 闇の光線（第2形態）：1.0 秒溜めて、2.6 秒まで左から右へなぎ払う
A["beam"] = dict(loop=False, length=3.4, bones=merge(spin(3.4, 3), {
    "right_arm": {"rotation": keys((0, Z), (0.6, [-90, 0, 15]), (2.6, [-90, 0, 15]), (3.4, Z))},
    "left_arm": {"rotation": keys((0, Z), (0.6, [-60, 0, -20]), (2.6, [-60, 0, -20]), (3.4, Z))},
    "torso": {"rotation": keys((0, Z), (0.6, [0, 60, 0]), (1.0, [0, 60, 0]), (2.6, [0, -60, 0]), (3.0, [0, -60, 0]), (3.4, Z))},
    "head": {"rotation": keys((0, Z), (1.0, [5, 0, 0]), (2.6, [5, 0, 0]), (3.4, Z))},
    "robe": {"rotation": keys((0, Z), (1.0, [0, 0, -8]), (2.6, [0, 0, 8]), (3.4, Z))},
}))

# 深淵の詠唱（第2形態の大技）：6 秒溜めて爆発
A["abyss"] = dict(loop=False, length=6.0, bones=merge(spin(6.0, 9), {
    "root": {"position": keys((0, Z), (1.0, [0, 10, 0]), (5.6, [0, 12, 0]), (6.0, Z))},
    "right_arm": {"rotation": keys((0, Z), (1.0, [-160, 0, 0]), (5.6, [-165, 0, 0]), (5.8, [-40, 0, -60]), (6.0, Z))},
    "left_arm": {"rotation": keys((0, Z), (1.0, [-150, 0, 0]), (5.6, [-155, 0, 0]), (5.8, [-40, 0, 60]), (6.0, Z))},
    "head": {"rotation": keys((0, Z), (1.0, [-35, 0, 0]), (3.0, [-35, 5, 0]), (5.0, [-35, -5, 0]), (5.8, [10, 0, 0]), (6.0, Z))},
    "orbit": {"scale": keys((0, ONE), (1.0, [1.8, 1.4, 1.8]), (5.6, [3.0, 2.0, 3.0]), (5.8, [0.2, 1, 0.2]), (6.0, ONE))},
    "grimoire": {"position": keys((0, Z), (1.0, [-11, 16, 3]), (5.6, [-11, 18, 3]), (6.0, Z)),
                 "rotation": keys((0, Z), (1.0, [25, 0, -10]), (5.6, [25, 0, -10]), (6.0, Z))},
}))

# 詠唱を止められてふらつく：3 秒
A["stagger"] = dict(loop=False, length=3.0, bones=merge(spin(3.0), {
    "root": {"position": keys((0, [0, 12, 0]), (0.5, [0, -2, 0]), (2.6, [0, -2, 0]), (3.0, Z))},
    "torso": {"rotation": keys((0, Z), (0.5, [30, 0, 10]), (1.5, [35, 0, -8]), (2.6, [30, 0, 5]), (3.0, Z))},
    "head": {"rotation": keys((0, Z), (0.5, [30, 0, 15]), (1.5, [35, 0, -15]), (2.6, [30, 0, 10]), (3.0, Z))},
    "right_arm": {"rotation": keys((0, Z), (0.5, [10, 0, 0]), (2.6, [10, 0, 0]), (3.0, Z))},
    "left_arm": {"rotation": keys((0, Z), (0.5, [30, 0, -10]), (2.6, [30, 0, -10]), (3.0, Z))},
    "grimoire": {"position": keys((0, Z), (0.5, [0, -12, 0]), (2.6, [0, -14, 0]), (3.0, Z))},
}))

A["phase_change"] = dict(loop=False, length=3.0, bones=merge(spin(3.0, 6), {
    "root": {"position": keys((0, Z), (1.0, [0, 6, 0]), (2.2, [0, 6, 0]), (3.0, Z))},
    "right_arm": {"rotation": keys((0, Z), (1.0, [-20, 0, 30]), (1.4, [-30, 0, -90]), (2.2, [-30, 0, -90]), (3.0, Z))},
    "left_arm": {"rotation": keys((0, Z), (1.0, [-10, 0, -30]), (1.4, [-20, 0, 90]), (2.2, [-20, 0, 90]), (3.0, Z))},
    "head": {"rotation": keys((0, Z), (1.0, [20, 0, 0]), (1.4, [-30, 0, 0]), (2.2, [-30, 0, 0]), (3.0, Z))},
    "torso": {"rotation": keys((0, Z), (1.0, [20, 0, 0]), (1.4, [-15, 0, 0]), (2.2, [-15, 0, 0]), (3.0, Z))},
    "orbit": {"scale": keys((0, ONE), (1.0, [0.4, 1, 0.4]), (1.4, [2.6, 1.5, 2.6]), (2.2, [2.0, 1.3, 2.0]), (3.0, ONE))},
}))

# 死亡：うずくまって、黒いもやになって溶けるように消える
A["death"] = dict(loop="hold_on_last_frame", length=3.0, bones={
    "torso": {"rotation": keys((0, Z), (0.5, [-25, 0, 0]), (1.4, [35, 0, 0]), (3.0, [40, 0, 0]))},
    "head": {"rotation": keys((0, Z), (0.5, [-35, 0, 0]), (1.4, [30, 0, 0]), (3.0, [35, 0, 0]))},
    "right_arm": {"rotation": keys((0, Z), (0.5, [-40, 0, -40]), (1.4, [10, 0, 0]), (3.0, [10, 0, 0]))},
    "staff": {"rotation": keys((0, Z), (1.4, [0, 0, 50]), (3.0, [0, 0, 80]))},
    "left_arm": {"rotation": keys((0, Z), (0.5, [-40, 0, 40]), (1.4, [10, 0, 0]), (3.0, [10, 0, 0]))},
    "grimoire": {"position": keys((0, Z), (1.0, [0, -18, 0]), (3.0, [0, -20, 0])),
                 "rotation": keys((0, Z), (1.0, [65, 0, 0]), (3.0, [65, 0, 0]))},
    "orbit": {"position": keys((0, Z), (1.0, [0, -20, 0]), (3.0, [0, -22, 0])),
              "scale": keys((0, ONE), (1.0, [1.2, 0.5, 1.2]), (3.0, [0.1, 0.1, 0.1]))},
    "body": {"position": keys((0, Z), (1.4, [0, -6, 0]), (3.0, [0, -8, 0])),
             "scale": keys((0, ONE), (1.6, ONE), (3.0, [0.2, 0.05, 0.2]))},
})


def to_json():
    return {"format_version": "1.8.0", "animations": {
        f"animation.dark_mage.{n}": {"loop": a["loop"], "animation_length": a["length"], "bones": a["bones"]}
        for n, a in A.items()}}


if __name__ == "__main__":
    os.makedirs(OUT, exist_ok=True)
    with open(f"{OUT}/dark_mage.animation.json", "w") as f:
        json.dump(to_json(), f, indent=1)
    print(len(A), "animations")
