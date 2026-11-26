"""ダンジョンモブ 風のセットのアニメ。 python3 tools/dmob_wind_anims.py"""
import json, os, sys
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from dmob_fire_anims import keys, merge, walk, collapse, shrink, Z, OUT
from dmob_ice_anims import quad_walk

ALL = {}


def anim(model, name, length, bones, loop=False):
    ALL.setdefault(model, {})[f"animation.{model}.{name}"] = {
        "loop": "hold_on_last_frame" if name == "death" else loop, "animation_length": length, "bones": bones}


def spin(bone, length, turns=1):
    return {bone: {"rotation": keys(*[(length * i / 4, [0, 90 * i * turns, 0]) for i in range(5)])}}


# ------------------------------------------------------------------ 旋風の山猫
M = "whirl_lynx"
WIND = spin("wind", 2.0)
TAIL = {"tail": {"rotation": keys((0, [0, -15, 0]), (0.5, [0, 15, 0]), (1.0, [0, -15, 0]))}}
anim(M, "idle", 2.0, merge(WIND, TAIL, {"body": {"scale": keys((0, [1, 1, 1]), (1, [1, 1.03, 1]), (2, [1, 1, 1]))}}), loop=True)
anim(M, "walk", 1.0, merge(spin("wind", 1.0), quad_walk(1.0, 32)), loop=True)
anim(M, "attack_claws", 1.2, merge(WIND, {
    "front_right_leg": {"rotation": keys((0, Z), (0.2, [-80, 0, 0]), (0.3, [10, 0, 0]), (0.6, [-80, 0, 0]), (0.7, [10, 0, 0]), (1.2, Z))},
    "front_left_leg": {"rotation": keys((0, Z), (0.4, [-80, 0, 0]), (0.5, [10, 0, 0]), (0.8, [-80, 0, 0]), (0.9, [10, 0, 0]), (1.2, Z))},
    "body": {"rotation": keys((0, Z), (0.2, [-10, 0, 0]), (1.0, [-10, 0, 0]), (1.2, Z))}}))
anim(M, "attack_pounce", 1.5, merge(WIND, {
    "body": {"rotation": keys((0, Z), (0.45, [10, 0, 0]), (0.55, [-20, 0, 0]), (1.0, [10, 0, 0]), (1.5, Z))},
    "front_right_leg": {"rotation": keys((0, Z), (0.5, [-70, 0, 0]), (1.1, [-70, 0, 0]), (1.5, Z))},
    "front_left_leg": {"rotation": keys((0, Z), (0.5, [-70, 0, 0]), (1.1, [-70, 0, 0]), (1.5, Z))},
    "back_right_leg": {"rotation": keys((0, Z), (0.5, [50, 0, 0]), (1.1, [50, 0, 0]), (1.5, Z))},
    "back_left_leg": {"rotation": keys((0, Z), (0.5, [50, 0, 0]), (1.1, [50, 0, 0]), (1.5, Z))}}))
anim(M, "attack_tornado", 1.4, merge(spin("wind", 1.4, 3), {
    "head": {"rotation": keys((0, Z), (0.5, [-40, 0, 0]), (1.0, [-40, 0, 0]), (1.4, Z))},
    "body": {"rotation": keys((0, Z), (0.5, [-15, 0, 0]), (1.0, [-15, 0, 0]), (1.4, Z))}}))
anim(M, "attack_blades", 1.0, merge(WIND, {
    "body": {"rotation": keys((0, Z), (0.35, [0, 40, 0]), (0.5, [0, -50, 0]), (1.0, Z))},
    "tail": {"rotation": keys((0, Z), (0.35, [0, -60, 0]), (0.5, [0, 70, 0]), (1.0, Z))}}))
anim(M, "death", 1.6, merge(shrink("wind", 0.6), {"root": {"rotation": keys((0, Z), (0.8, [0, 0, 90])), "position": keys((0, Z), (0.8, [0, -2, 0]))}}))

# ------------------------------------------------------------------ 疾風の鷲
M = "gale_eagle"
FLAP = {"right_wing": {"rotation": keys((0, [0, 0, 25]), (0.25, [0, 0, -30]), (0.5, [0, 0, 25]))},
        "left_wing": {"rotation": keys((0, [0, 0, -25]), (0.25, [0, 0, 30]), (0.5, [0, 0, -25]))},
        "body": {"position": keys((0, Z), (0.25, [0, 1, 0]), (0.5, Z))}}
anim(M, "idle", 0.5, FLAP, loop=True)
anim(M, "attack_dive", 1.4, {
    "right_wing": {"rotation": keys((0, Z), (0.3, [0, -40, 60]), (1.0, [0, -40, 60]), (1.4, Z))},
    "left_wing": {"rotation": keys((0, Z), (0.3, [0, 40, -60]), (1.0, [0, 40, -60]), (1.4, Z))},
    "body": {"rotation": keys((0, Z), (0.3, [40, 0, 0]), (1.0, [40, 0, 0]), (1.4, Z))}})
anim(M, "attack_gust", 1.2, {
    "right_wing": {"rotation": keys((0, Z), (0.3, [0, 0, 60]), (0.45, [-30, 0, -50]), (0.7, [0, 0, 60]), (0.85, [-30, 0, -50]), (1.2, Z))},
    "left_wing": {"rotation": keys((0, Z), (0.3, [0, 0, -60]), (0.45, [-30, 0, 50]), (0.7, [0, 0, -60]), (0.85, [-30, 0, 50]), (1.2, Z))},
    "body": {"rotation": keys((0, Z), (0.3, [-25, 0, 0]), (1.0, [-25, 0, 0]), (1.2, Z))}})
anim(M, "death", 1.0, merge({"root": {"rotation": keys((0, Z), (1.0, [60, 0, 180])), "position": keys((0, Z), (1.0, [0, -6, 0]))}}))

# ------------------------------------------------------------------ 竜巻の精
M = "tornado_spirit"
TOR = {"tornado": {"rotation": keys((0, Z), (0.25, [0, 90, 0]), (0.5, [0, 180, 0]), (0.75, [0, 270, 0]), (1.0, [0, 360, 0]))}}
anim(M, "idle", 1.0, merge(TOR, {"body": {"position": keys((0, Z), (0.5, [0, 0.6, 0]), (1.0, Z))},
                                 "right_arm": {"rotation": keys((0, [0, 0, -15]), (1.0, [0, 0, -15]))},
                                 "left_arm": {"rotation": keys((0, [0, 0, 15]), (1.0, [0, 0, 15]))}}), loop=True)
anim(M, "attack_pull", 2.0, merge({"tornado": {"rotation": keys(*[(i * 0.25, [0, 180 * i, 0]) for i in range(9)]),
                                               "scale": keys((0, [1, 1, 1]), (0.4, [1.4, 1, 1.4]), (1.6, [1.4, 1, 1.4]), (2.0, [1, 1, 1]))}},
                                  {"right_arm": {"rotation": keys((0, Z), (0.4, [-90, 30, 0]), (1.6, [-90, 30, 0]), (2.0, Z))},
                                   "left_arm": {"rotation": keys((0, Z), (0.4, [-90, -30, 0]), (1.6, [-90, -30, 0]), (2.0, Z))}}))
anim(M, "attack_blades", 1.2, merge(TOR, {
    "right_arm": {"rotation": keys((0, Z), (0.4, [-60, 0, -80]), (0.6, [-60, 0, 30]), (1.2, Z))},
    "left_arm": {"rotation": keys((0, Z), (0.4, [-60, 0, 80]), (0.6, [-60, 0, -30]), (1.2, Z))},
    "body": {"rotation": keys((0, Z), (0.4, [0, -30, 0]), (0.6, [0, 30, 0]), (1.2, Z))}}))
anim(M, "death", 1.4, merge(shrink("tornado", 1.0), shrink("root", 1.4), {"body": {"position": keys((0, Z), (1.4, [0, 6, 0]))}}))

# ------------------------------------------------------------------ 風のわたぼうし
M = "wind_puff"
anim(M, "idle", 1.6, {"body": {"position": keys((0, Z), (0.8, [0, 1.2, 0]), (1.6, Z)),
                               "rotation": keys((0, [0, 0, -6]), (0.8, [0, 0, 6]), (1.6, [0, 0, -6]))}}, loop=True)
anim(M, "attack_bump", 0.6, {"body": {"scale": keys((0, [1, 1, 1]), (0.2, [1.3, 0.75, 1.3]), (0.3, [0.85, 1.25, 0.85]), (0.6, [1, 1, 1])),
                                      "position": keys((0, Z), (0.2, [0, 0, 2]), (0.3, [0, 0, -3]), (0.6, Z))}})
anim(M, "death", 0.6, {"body": {"scale": keys((0, [1, 1, 1]), (0.3, [1.5, 1.5, 1.5]), (0.6, [0.01, 0.01, 0.01]))}})

# ------------------------------------------------------------------ 疾風のイタチ
M = "gale_weasel"
LEGS = {"right_legs": {"rotation": keys((0, [25, 0, 0]), (0.15, [-25, 0, 0]), (0.3, [25, 0, 0]))},
        "left_legs": {"rotation": keys((0, [-25, 0, 0]), (0.15, [25, 0, 0]), (0.3, [-25, 0, 0]))}}
anim(M, "idle", 1.6, {"head": {"rotation": keys((0, Z), (0.4, [-15, 20, 0]), (0.8, [-15, -20, 0]), (1.2, [0, 0, 0]), (1.6, Z))},
                      "tail": {"rotation": keys((0, Z), (0.8, [0, 20, 0]), (1.6, Z))}}, loop=True)
anim(M, "walk", 0.3, merge(LEGS, {"body": {"position": keys((0, Z), (0.15, [0, 0.6, 0]), (0.3, Z))}}), loop=True)
anim(M, "attack_cut", 0.8, merge(LEGS, {
    "tail": {"rotation": keys((0, Z), (0.2, [-60, 0, 0]), (0.4, [60, 0, 0]), (0.8, Z))},
    "body": {"rotation": keys((0, Z), (0.2, [10, 0, 0]), (0.4, [-15, 0, 0]), (0.8, Z))}}))
anim(M, "death", 0.8, {"root": {"rotation": keys((0, Z), (0.5, [0, 0, 180])), "position": keys((0, Z), (0.5, [0, 3, 0]))}})

if __name__ == "__main__":
    os.makedirs(OUT, exist_ok=True)
    for model, anims in ALL.items():
        with open(f"{OUT}/{model}.animation.json", "w") as f:
            json.dump({"format_version": "1.8.0", "animations": anims}, f, indent=1)
        print(model, len(anims))
