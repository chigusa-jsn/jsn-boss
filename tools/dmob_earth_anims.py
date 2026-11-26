"""ダンジョンモブ 大地のセットのアニメ。 python3 tools/dmob_earth_anims.py"""
import json, os, sys
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from dmob_fire_anims import keys, merge, walk, collapse, shrink, Z, OUT

ALL = {}


def anim(model, name, length, bones, loop=False):
    ALL.setdefault(model, {})[f"animation.{model}.{name}"] = {
        "loop": "hold_on_last_frame" if name == "death" else loop, "animation_length": length, "bones": bones}


# ------------------------------------------------------------------ 岩石のゴーレム
M = "boulder_golem"
anim(M, "idle", 3.0, {"body": {"rotation": keys((0, Z), (1.5, [2, 0, 0]), (3.0, Z))},
                      "right_arm": {"rotation": keys((0, Z), (1.5, [3, 0, -2]), (3.0, Z))},
                      "left_arm": {"rotation": keys((0, Z), (1.5, [3, 0, 2]), (3.0, Z))}}, loop=True)
anim(M, "walk", 1.6, merge(walk(1.6, leg=18, arm=12, bob=1.2), {"body": {"rotation": keys((0, [0, 0, 3]), (0.8, [0, 0, -3]), (1.6, [0, 0, 3]))}}), loop=True)
anim(M, "attack_throw", 1.6, {
    "right_arm": {"rotation": keys((0, Z), (0.6, [-180, 0, 10]), (0.8, [-60, 0, 0]), (1.6, Z))},
    "left_arm": {"rotation": keys((0, Z), (0.6, [-180, 0, -10]), (0.8, [-60, 0, 0]), (1.6, Z))},
    "body": {"rotation": keys((0, Z), (0.6, [-15, 0, 0]), (0.8, [20, 0, 0]), (1.6, Z))}})
anim(M, "attack_pillars", 1.6, {
    "right_arm": {"rotation": keys((0, Z), (0.6, [-160, 0, 0]), (0.8, [-20, 0, 0]), (1.3, [-20, 0, 0]), (1.6, Z))},
    "left_arm": {"rotation": keys((0, Z), (0.6, [-160, 0, 0]), (0.8, [-20, 0, 0]), (1.3, [-20, 0, 0]), (1.6, Z))},
    "body": {"rotation": keys((0, Z), (0.6, [-10, 0, 0]), (0.8, [35, 0, 0]), (1.3, [35, 0, 0]), (1.6, Z))}})
anim(M, "attack_quake", 1.4, {
    "right_leg": {"rotation": keys((0, Z), (0.5, [-50, 0, 0]), (0.65, Z), (1.4, Z))},
    "root": {"position": keys((0, Z), (0.5, [0, 2, 0]), (0.65, [0, -1, 0]), (0.9, Z))},
    "right_arm": {"rotation": keys((0, Z), (0.5, [0, 0, -40]), (0.65, Z))}, "left_arm": {"rotation": keys((0, Z), (0.5, [0, 0, 40]), (0.65, Z))}})
anim(M, "attack_punch", 1.0, {
    "right_arm": {"rotation": keys((0, Z), (0.35, [-30, 0, -30]), (0.5, [-90, 0, 0]), (0.8, [-80, 0, 0]), (1.0, Z))},
    "body": {"rotation": keys((0, Z), (0.35, [0, 25, 0]), (0.5, [5, -20, 0]), (1.0, Z))}})
anim(M, "death", 1.6, merge(collapse(1.2, -6, 40), {"right_arm": {"rotation": keys((0, Z), (1, [0, 0, -50]))},
                                                    "left_arm": {"rotation": keys((0, Z), (1, [0, 0, 50]))}}))

# ------------------------------------------------------------------ 岩甲羅の亀
M = "stone_tortoise"
LEGS = {"right_legs": {"rotation": keys((0, [10, 0, 0]), (0.5, [-10, 0, 0]), (1.0, [10, 0, 0]))},
        "left_legs": {"rotation": keys((0, [-10, 0, 0]), (0.5, [10, 0, 0]), (1.0, [-10, 0, 0]))}}
anim(M, "idle", 3.0, {"head": {"rotation": keys((0, Z), (1.5, [-10, 15, 0]), (3.0, Z))}}, loop=True)
anim(M, "walk", 1.0, merge(LEGS, {"shell": {"rotation": keys((0, [0, 0, 2]), (0.5, [0, 0, -2]), (1.0, [0, 0, 2]))}}), loop=True)
HIDE = lambda L0, L1: {b: {"scale": keys((0, [1, 1, 1]), (L0, [0.01, 0.01, 0.01]), (L1, [0.01, 0.01, 0.01]), (L1 + 0.2, [1, 1, 1]))}
                      for b in ("head", "right_legs", "left_legs", "tail")}
anim(M, "attack_spin", 2.4, merge(HIDE(0.3, 2.1), {
    "shell": {"rotation": keys(*[(0.3 + i * 0.225, [0, 90 * i, 0]) for i in range(9)]) | {"0.0": Z, "2.4": [0, 720, 0]},
              "position": keys((0, Z), (0.3, [0, -3, 0]), (2.1, [0, -3, 0]), (2.4, Z))}}))
anim(M, "attack_bite", 0.8, {"head": {"rotation": keys((0, Z), (0.3, [-20, 0, 0]), (0.4, [20, 0, 0]), (0.8, Z)),
                                      "position": keys((0, Z), (0.3, [0, 0, 1]), (0.4, [0, 0, -3]), (0.8, Z))}})
anim(M, "death", 1.4, {"root": {"rotation": keys((0, Z), (0.8, [0, 0, 180])), "position": keys((0, Z), (0.8, [0, 12, 0]))}})

# ------------------------------------------------------------------ 粘土の巨兵
M = "clay_brute"
anim(M, "idle", 2.4, {"body": {"rotation": keys((0, Z), (1.2, [3, 0, 0]), (2.4, Z))},
                      "head": {"rotation": keys((0, Z), (1.2, [0, 10, 0]), (2.4, Z))}}, loop=True)
anim(M, "walk", 1.4, walk(1.4, leg=20, arm=14, bob=1.0), loop=True)
anim(M, "attack_mud", 1.2, {
    "right_arm": {"rotation": keys((0, Z), (0.45, [-170, 0, -10]), (0.6, [-50, 0, 0]), (1.2, Z))},
    "body": {"rotation": keys((0, Z), (0.45, [-10, 20, 0]), (0.6, [10, -15, 0]), (1.2, Z))}})
anim(M, "attack_smash", 1.4, {
    "right_arm": {"rotation": keys((0, Z), (0.55, [-160, 0, 0]), (0.7, [-30, 0, 0]), (1.4, Z))},
    "left_arm": {"rotation": keys((0, Z), (0.55, [-160, 0, 0]), (0.7, [-30, 0, 0]), (1.4, Z))},
    "body": {"rotation": keys((0, Z), (0.55, [-15, 0, 0]), (0.7, [30, 0, 0]), (1.4, Z))}})
anim(M, "death", 1.6, merge(collapse(1.2, -10, 20), {"body": {"scale": keys((0, [1, 1, 1]), (1.4, [1.3, 0.3, 1.3]))}}))

# ------------------------------------------------------------------ 小石ゴーレム
M = "pebble_golem"
anim(M, "idle", 1.6, {"body": {"rotation": keys((0, [0, 0, -4]), (0.8, [0, 0, 4]), (1.6, [0, 0, -4]))}}, loop=True)
anim(M, "walk", 0.6, walk(0.6, leg=35, arm=30, bob=0.5), loop=True)
anim(M, "attack_throw", 1.0, {"right_arm": {"rotation": keys((0, Z), (0.4, [-160, 0, -10]), (0.55, [-40, 0, 0]), (1.0, Z))}})
anim(M, "death", 0.8, merge(shrink("body", 0.8), {"body": {"position": keys((0, Z), (0.8, [0, -3, 0]))}}))

# ------------------------------------------------------------------ もぐら
M = "burrow_mole"
PADDLE = {"right_claw": {"rotation": keys((0, [30, 0, 0]), (0.15, [-30, 0, 0]), (0.3, [30, 0, 0]))},
          "left_claw": {"rotation": keys((0, [-30, 0, 0]), (0.15, [30, 0, 0]), (0.3, [-30, 0, 0]))}}
anim(M, "idle", 2.0, {"body": {"rotation": keys((0, Z), (1, [-8, 10, 0]), (2, Z))}}, loop=True)
anim(M, "walk", 0.3, merge(PADDLE, {"body": {"position": keys((0, Z), (0.15, [0, 0.4, 0]), (0.3, Z))}}), loop=True)
anim(M, "attack_burrow", 2.5, merge(PADDLE, {"body": {"position": keys((0, Z), (0.4, [0, -8, 0]), (1.95, [0, -8, 0]), (2.05, [0, 3, 0]), (2.3, Z)),
                                                      "rotation": keys((0, Z), (0.4, [30, 0, 0]), (1.95, [30, 0, 0]), (2.05, [-50, 0, 0]), (2.5, Z))}}))
anim(M, "attack_scratch", 0.6, {"right_claw": {"rotation": keys((0, Z), (0.2, [-80, 0, 0]), (0.3, [20, 0, 0]), (0.6, Z))},
                                "left_claw": {"rotation": keys((0, Z), (0.25, [-80, 0, 0]), (0.35, [20, 0, 0]), (0.6, Z))}})
anim(M, "death", 1.0, {"root": {"rotation": keys((0, Z), (0.6, [0, 0, 180])), "position": keys((0, Z), (0.6, [0, 5, 0]))}})

if __name__ == "__main__":
    os.makedirs(OUT, exist_ok=True)
    for model, anims in ALL.items():
        with open(f"{OUT}/{model}.animation.json", "w") as f:
            json.dump({"format_version": "1.8.0", "animations": anims}, f, indent=1)
        print(model, len(anims))
