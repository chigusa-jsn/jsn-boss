"""ダンジョンモブ 雷のセットのアニメ。 python3 tools/dmob_thunder_anims.py"""
import json, os, sys
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from dmob_fire_anims import keys, merge, walk, collapse, shrink, Z, OUT
from dmob_ice_anims import quad_walk

ALL = {}


def anim(model, name, length, bones, loop=False):
    ALL.setdefault(model, {})[f"animation.{model}.{name}"] = {
        "loop": "hold_on_last_frame" if name == "death" else loop, "animation_length": length, "bones": bones}


# ------------------------------------------------------------------ 雷鳴の牝獅子
M = "thunder_lioness"
TAIL = {"tail": {"rotation": keys((0, [0, -15, 0]), (0.6, [0, 15, 0]), (1.2, [0, -15, 0]))}}
anim(M, "idle", 1.2, merge(TAIL, {"body": {"scale": keys((0, [1, 1, 1]), (0.6, [1, 1.03, 1]), (1.2, [1, 1, 1]))}}), loop=True)
anim(M, "walk", 0.7, merge(quad_walk(0.7, 32), {"tail": {"rotation": keys((0, [10, 0, 0]), (0.7, [10, 0, 0]))}}), loop=True)
anim(M, "attack_marks", 1.4, merge(TAIL, {
    "head": {"rotation": keys((0, Z), (0.5, [-45, 0, 0]), (1.0, [-45, 0, 0]), (1.4, Z))},
    "body": {"rotation": keys((0, Z), (0.5, [-12, 0, 0]), (1.0, [-12, 0, 0]), (1.4, Z))}}))
anim(M, "attack_tackle", 1.6, {
    "body": {"rotation": keys((0, Z), (0.5, [12, 0, 0]), (0.6, [-8, 0, 0]), (1.3, [-8, 0, 0]), (1.6, Z))},
    "head": {"rotation": keys((0, Z), (0.5, [20, 0, 0]), (1.3, [20, 0, 0]), (1.6, Z))},
    "front_right_leg": {"rotation": keys((0.6, Z), (0.75, [-50, 0, 0]), (0.9, [40, 0, 0]), (1.05, [-50, 0, 0]), (1.2, [40, 0, 0]), (1.3, Z))},
    "front_left_leg": {"rotation": keys((0.6, Z), (0.75, [-50, 0, 0]), (0.9, [40, 0, 0]), (1.05, [-50, 0, 0]), (1.2, [40, 0, 0]), (1.3, Z))},
    "back_right_leg": {"rotation": keys((0.6, Z), (0.75, [40, 0, 0]), (0.9, [-40, 0, 0]), (1.05, [40, 0, 0]), (1.2, [-40, 0, 0]), (1.3, Z))},
    "back_left_leg": {"rotation": keys((0.6, Z), (0.75, [40, 0, 0]), (0.9, [-40, 0, 0]), (1.05, [40, 0, 0]), (1.2, [-40, 0, 0]), (1.3, Z))}})
anim(M, "attack_roar", 1.5, {
    "head": {"rotation": keys((0, Z), (0.4, [15, 0, 0]), (0.6, [-25, 0, 0]), (1.1, [-25, 0, 0]), (1.5, Z)),
             "scale": keys((0, [1, 1, 1]), (0.6, [1.15, 1.15, 1.15]), (1.1, [1.15, 1.15, 1.15]), (1.5, [1, 1, 1]))},
    "body": {"rotation": keys((0, Z), (0.4, [10, 0, 0]), (0.6, [-6, 0, 0]), (1.5, Z))}})
anim(M, "attack_bolt", 1.2, {
    "head": {"rotation": keys((0, Z), (0.4, [30, 0, 0]), (0.6, [10, 0, 0]), (1.2, Z))},
    "front_right_leg": {"rotation": keys((0, Z), (0.4, [-60, 0, 0]), (0.5, [10, 0, 0]), (1.2, Z))},
    "front_left_leg": {"rotation": keys((0, Z), (0.4, [-60, 0, 0]), (0.5, [10, 0, 0]), (1.2, Z))}})
anim(M, "death", 1.6, {"root": {"rotation": keys((0, Z), (0.8, [0, 0, 90])), "position": keys((0, Z), (0.8, [0, -2, 0]))}})

# ------------------------------------------------------------------ 雷の機兵
M = "volt_sentinel"
anim(M, "idle", 2.0, {"head": {"rotation": keys((0, Z), (0.5, [0, 20, 0]), (1.0, Z), (1.5, [0, -20, 0]), (2.0, Z))},
                      "body": {"position": keys((0, Z), (1, [0, -0.4, 0]), (2, Z))}}, loop=True)
anim(M, "walk", 1.3, walk(1.3, leg=20, arm=10, bob=0.6), loop=True)
anim(M, "attack_chain", 1.4, {
    "right_arm": {"rotation": keys((0, Z), (0.5, [-170, 0, -10]), (1.0, [-170, 0, -10]), (1.4, Z))},
    "left_arm": {"rotation": keys((0, Z), (0.5, [-90, 0, 10]), (0.7, [-80, 0, 0]), (1.0, [-90, 0, 0]), (1.4, Z))}})
anim(M, "attack_discharge", 1.6, {
    "right_arm": {"rotation": keys((0, Z), (0.5, [0, 0, -90]), (1.2, [0, 0, -90]), (1.6, Z))},
    "left_arm": {"rotation": keys((0, Z), (0.5, [0, 0, 90]), (1.2, [0, 0, 90]), (1.6, Z))},
    "body": {"scale": keys((0, [1, 1, 1]), (0.6, [1.08, 1.08, 1.08]), (0.7, [0.96, 0.96, 0.96]), (0.8, [1.08, 1.08, 1.08]),
                           (0.9, [0.96, 0.96, 0.96]), (1.2, [1, 1, 1]))}})
anim(M, "death", 1.6, merge(collapse(1.2, -4, 80), {"head": {"rotation": keys((0, Z), (0.6, [0, 0, 30]))}}))

# ------------------------------------------------------------------ 雷鳴の大トカゲ
M = "storm_lizard"
WIG = {"body": {"rotation": keys((0, [0, 8, 0]), (0.3, [0, -8, 0]), (0.6, [0, 8, 0]))},
       "tail": {"rotation": keys((0, [0, -15, 0]), (0.3, [0, 15, 0]), (0.6, [0, -15, 0]))},
       "right_legs": {"rotation": keys((0, [0, 20, 0]), (0.3, [0, -20, 0]), (0.6, [0, 20, 0]))},
       "left_legs": {"rotation": keys((0, [0, 20, 0]), (0.3, [0, -20, 0]), (0.6, [0, 20, 0]))}}
anim(M, "idle", 2.4, {"head": {"rotation": keys((0, Z), (1.2, [-10, 10, 0]), (2.4, Z))},
                      "tail": {"rotation": keys((0, Z), (1.2, [0, 15, 0]), (2.4, Z))}}, loop=True)
anim(M, "walk", 0.6, WIG, loop=True)
anim(M, "attack_charge", 1.6, merge(WIG, {"head": {"rotation": keys((0, Z), (0.4, [-20, 0, 0]), (1.4, [-20, 0, 0]), (1.6, Z))}}))
anim(M, "attack_bolt", 1.4, {
    "body": {"rotation": keys((0, Z), (0.5, [-35, 0, 0]), (0.9, [-35, 0, 0]), (1.4, Z))},
    "head": {"rotation": keys((0, Z), (0.5, [-30, 0, 0]), (0.9, [-30, 0, 0]), (1.4, Z))}})
anim(M, "death", 1.4, {"root": {"rotation": keys((0, Z), (0.7, [0, 0, 180])), "position": keys((0, Z), (0.7, [0, 6, 0]))}})

# ------------------------------------------------------------------ 電気クラゲ
M = "spark_jelly"
anim(M, "idle", 1.6, {"bell": {"scale": keys((0, [1, 1, 1]), (0.4, [1.12, 0.88, 1.12]), (0.8, [1, 1, 1]), (1.6, [1, 1, 1])),
                               "position": keys((0, Z), (0.4, [0, -0.4, 0]), (0.8, [0, 1, 0]), (1.6, Z))},
                      "tentacles": {"rotation": keys((0, [4, 0, 0]), (0.8, [-4, 0, 4]), (1.6, [4, 0, 0]))}}, loop=True)
anim(M, "attack_zap", 0.8, {"bell": {"scale": keys((0, [1, 1, 1]), (0.2, [1.3, 0.8, 1.3]), (0.35, [0.9, 1.15, 0.9]), (0.8, [1, 1, 1]))},
                            "tentacles": {"scale": keys((0, [1, 1, 1]), (0.25, [1.4, 1.2, 1.4]), (0.8, [1, 1, 1]))}})
anim(M, "death", 1.0, merge(shrink("root", 1.0), {"root": {"position": keys((0, Z), (1.0, [0, -6, 0]))}}))

# ------------------------------------------------------------------ 雷虫
M = "volt_beetle"
LEGS = {"right_legs": {"rotation": keys((0, [0, 15, 0]), (0.15, [0, -15, 0]), (0.3, [0, 15, 0]))},
        "left_legs": {"rotation": keys((0, [0, 15, 0]), (0.15, [0, -15, 0]), (0.3, [0, 15, 0]))}}
anim(M, "idle", 2.0, {"body": {"scale": keys((0, [1, 1, 1]), (1, [1, 1.05, 1]), (2, [1, 1, 1]))}}, loop=True)
anim(M, "walk", 0.3, LEGS, loop=True)
anim(M, "attack_ram", 1.0, merge(LEGS, {"body": {"rotation": keys((0, Z), (0.3, [-20, 0, 0]), (0.4, [10, 0, 0]), (0.7, Z))}}))
anim(M, "death", 1.0, {"root": {"rotation": keys((0, Z), (0.6, [0, 0, 180])), "position": keys((0, Z), (0.6, [0, 4, 0]))}})

if __name__ == "__main__":
    os.makedirs(OUT, exist_ok=True)
    for model, anims in ALL.items():
        with open(f"{OUT}/{model}.animation.json", "w") as f:
            json.dump({"format_version": "1.8.0", "animations": anims}, f, indent=1)
        print(model, len(anims))
