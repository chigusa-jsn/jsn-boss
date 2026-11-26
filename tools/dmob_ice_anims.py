"""ダンジョンモブ 氷のセットのアニメ。 python3 tools/dmob_ice_anims.py"""
import json, os, sys
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from dmob_fire_anims import keys, merge, walk, collapse, shrink, Z, OUT

ALL = {}


def anim(model, name, length, bones, loop=False):
    ALL.setdefault(model, {})[f"animation.{model}.{name}"] = {
        "loop": "hold_on_last_frame" if name == "death" else loop, "animation_length": length, "bones": bones}


def quad_walk(length, deg=28, bob=0.6):
    h = length / 2
    a, b = [-deg, 0, 0], [deg, 0, 0]
    return {"front_right_leg": {"rotation": keys((0, a), (h, b), (length, a))},
            "back_left_leg": {"rotation": keys((0, a), (h, b), (length, a))},
            "front_left_leg": {"rotation": keys((0, b), (h, a), (length, b))},
            "back_right_leg": {"rotation": keys((0, b), (h, a), (length, b))},
            "body": {"position": keys((0, Z), (h / 2, [0, -bob, 0]), (h, Z), (h * 1.5, [0, -bob, 0]), (length, Z))}}


SPIN4 = lambda bone: {bone: {"rotation": keys((0, Z), (1, [0, 90, 0]), (2, [0, 180, 0]), (3, [0, 270, 0]), (4, [0, 360, 0]))}}

# ------------------------------------------------------------------ 霜の侍女
M = "frost_maiden"
BOB = {"body": {"position": keys((0, Z), (1, [0, 0.8, 0]), (2, Z), (3, [0, 0.8, 0]), (4, Z))}}
anim(M, "idle", 4.0, merge(SPIN4("crystals"), BOB, {
    "right_arm": {"rotation": keys((0, [0, 0, -6]), (2, [0, 0, -10]), (4, [0, 0, -6]))},
    "left_arm": {"rotation": keys((0, [0, 0, 6]), (2, [0, 0, 10]), (4, [0, 0, 6]))}}), loop=True)
anim(M, "walk", 4.0, merge(SPIN4("crystals"), BOB, {"body": {"rotation": keys((0, [6, 0, 0]), (4, [6, 0, 0]))}}), loop=True)
anim(M, "attack_needles", 1.2, merge(SPIN4("crystals"), {
    "right_arm": {"rotation": keys((0, Z), (0.45, [-60, 40, -40]), (0.6, [-90, -30, 10]), (1.2, Z))},
    "body": {"rotation": keys((0, Z), (0.45, [0, 20, 0]), (0.6, [0, -20, 0]), (1.2, Z))}}))
anim(M, "attack_circle", 1.5, merge(SPIN4("crystals"), {
    "right_arm": {"rotation": keys((0, Z), (0.5, [-160, 0, -20]), (1.1, [-160, 0, -20]), (1.5, Z))},
    "left_arm": {"rotation": keys((0, Z), (0.5, [-160, 0, 20]), (1.1, [-160, 0, 20]), (1.5, Z))},
    "crystals": {"scale": keys((0, [1, 1, 1]), (0.5, [1.6, 1.6, 1.6]), (1.1, [1.6, 1.6, 1.6]), (1.5, [1, 1, 1]))}}))
anim(M, "attack_blink", 0.8, {"root": {"scale": keys((0, [1, 1, 1]), (0.35, [0.05, 1.4, 0.05]), (0.45, [0.05, 1.4, 0.05]), (0.8, [1, 1, 1]))}})
anim(M, "attack_spikes", 1.5, {
    "right_arm": {"rotation": keys((0, Z), (0.5, [-120, 0, -30]), (0.7, [-30, 0, -40]), (1.5, Z))},
    "left_arm": {"rotation": keys((0, Z), (0.5, [-120, 0, 30]), (0.7, [-30, 0, 40]), (1.5, Z))},
    "body": {"rotation": keys((0, Z), (0.5, [-8, 0, 0]), (0.7, [15, 0, 0]), (1.5, Z))}})
anim(M, "death", 2.0, merge(shrink("crystals", 0.8), {"body": {"rotation": keys((0, Z), (1.0, [20, 0, 0])),
                                                              "position": keys((0, Z), (1.6, [0, -6, 0]))}}, shrink("root", 2.0)))

# ------------------------------------------------------------------ 群れの長狼
M = "pack_alpha"
TAIL = {"tail": {"rotation": keys((0, [0, -10, 0]), (0.5, [0, 10, 0]), (1.0, [0, -10, 0]))}}
anim(M, "idle", 2.0, merge({"body": {"scale": keys((0, [1, 1, 1]), (1, [1, 1.03, 1]), (2, [1, 1, 1]))},
                            "head": {"rotation": keys((0, Z), (1, [4, 0, 0]), (2, Z))},
                            "chain": {"rotation": keys((0, Z), (1, [6, 0, 0]), (2, Z))}}), loop=True)
anim(M, "walk", 0.8, merge(quad_walk(0.8, 30), TAIL, {"chain": {"rotation": keys((0, [10, 0, 0]), (0.4, [-10, 0, 0]), (0.8, [10, 0, 0]))}}), loop=True)
anim(M, "attack_bite", 0.8, {
    "head": {"rotation": keys((0, Z), (0.3, [-20, 0, 0]), (0.4, [25, 0, 0]), (0.8, Z))},
    "body": {"position": keys((0, Z), (0.3, [0, 0, 2]), (0.4, [0, 0, -3]), (0.8, Z))}})
anim(M, "attack_combo", 1.5, {
    "head": {"rotation": keys((0, Z), (0.3, [-20, 20, 0]), (0.4, [25, 0, 0]), (0.7, [-20, -20, 0]), (0.8, [25, 0, 0]),
                          (1.1, [-25, 0, 0]), (1.2, [30, 0, 0]), (1.5, Z))},
    "body": {"position": keys((0, Z), (0.4, [0, 0, -2]), (0.8, [0, 0, -3]), (1.2, [0, 0, -4]), (1.5, Z))}})
anim(M, "attack_pounce", 1.5, {
    "body": {"rotation": keys((0, Z), (0.45, [10, 0, 0]), (0.55, [-20, 0, 0]), (1.0, [10, 0, 0]), (1.5, Z))},
    "front_right_leg": {"rotation": keys((0, Z), (0.5, [-70, 0, 0]), (1.1, [-70, 0, 0]), (1.5, Z))},
    "front_left_leg": {"rotation": keys((0, Z), (0.5, [-70, 0, 0]), (1.1, [-70, 0, 0]), (1.5, Z))},
    "back_right_leg": {"rotation": keys((0, Z), (0.5, [50, 0, 0]), (1.1, [50, 0, 0]), (1.5, Z))},
    "back_left_leg": {"rotation": keys((0, Z), (0.5, [50, 0, 0]), (1.1, [50, 0, 0]), (1.5, Z))}})
anim(M, "attack_howl", 1.5, {
    "head": {"rotation": keys((0, Z), (0.4, [-55, 0, 0]), (1.2, [-55, 0, 0]), (1.5, Z))},
    "body": {"rotation": keys((0, Z), (0.4, [-15, 0, 0]), (1.2, [-15, 0, 0]), (1.5, Z))}})
anim(M, "attack_chain", 1.2, {
    "body": {"rotation": keys((0, Z), (0.45, [0, 40, 0]), (0.6, [0, -70, 0]), (0.9, [0, -40, 0]), (1.2, Z))},
    "chain": {"rotation": keys((0, Z), (0.45, [0, 0, 40]), (0.6, [-60, 0, -70]), (1.2, Z))}})
anim(M, "death", 1.6, {"root": {"rotation": keys((0, Z), (0.8, [0, 0, 90])), "position": keys((0, Z), (0.8, [0, -2, 0]))},
                       "head": {"rotation": keys((0, Z), (1.2, [20, 0, 0]))}})

# ------------------------------------------------------------------ 氷像の騎士
M = "frost_knight"
anim(M, "idle", 2.4, {"body": {"rotation": keys((0, Z), (1.2, [1.5, 0, 0]), (2.4, Z))},
                      "shield": {"rotation": keys((0, Z), (1.2, [0, 0, 2]), (2.4, Z))}}, loop=True)
anim(M, "walk", 1.3, walk(1.3, leg=20, arm=8, bob=0.8), loop=True)
anim(M, "attack_slash", 1.0, {
    "right_arm": {"rotation": keys((0, Z), (0.4, [-130, 20, -20]), (0.55, [-40, -30, 10]), (1.0, Z))},
    "body": {"rotation": keys((0, Z), (0.4, [0, 20, 0]), (0.55, [10, -25, 0]), (1.0, Z))}})
anim(M, "attack_guard", 2.5, {
    "left_arm": {"rotation": keys((0, Z), (0.25, [-75, -35, 0]), (2.2, [-75, -35, 0]), (2.5, Z))},
    "body": {"rotation": keys((0, Z), (0.25, [8, -15, 0]), (2.2, [8, -15, 0]), (2.5, Z))},
    "root": {"position": keys((0, Z), (0.25, [0, -1, 0]), (2.2, [0, -1, 0]), (2.5, Z))}})
anim(M, "death", 1.6, merge(collapse(1.2, -6, 70), {"head": {"position": keys((0, Z), (1.2, [4, -16, -2]))}}))

# ------------------------------------------------------------------ 氷牙の白熊
M = "frostfang_bear"
anim(M, "idle", 2.4, {"body": {"scale": keys((0, [1, 1, 1]), (1.2, [1, 1.03, 1]), (2.4, [1, 1, 1]))},
                      "head": {"rotation": keys((0, Z), (1.2, [0, 8, 0]), (2.4, Z))}}, loop=True)
anim(M, "walk", 1.2, quad_walk(1.2, 22, 0.8), loop=True)
anim(M, "attack_maul", 1.2, {
    "root": {"rotation": keys((0, Z), (0.5, [-40, 0, 0]), (0.7, [8, 0, 0]), (1.2, Z)),
             "position": keys((0, Z), (0.5, [0, 2, 4]), (0.7, Z))},
    "front_right_leg": {"rotation": keys((0, Z), (0.5, [-110, 0, 0]), (0.7, [-20, 0, 0]), (1.2, Z))},
    "front_left_leg": {"rotation": keys((0, Z), (0.5, [-110, 0, 0]), (0.7, [-20, 0, 0]), (1.2, Z))},
    "head": {"rotation": keys((0, Z), (0.5, [25, 0, 0]), (0.7, [-10, 0, 0]), (1.2, Z))}})
anim(M, "attack_spikes", 1.3, {
    "body": {"rotation": keys((0, Z), (0.5, [12, 0, 0]), (0.6, [-6, 0, 0]), (1.3, Z)),
             "scale": keys((0, [1, 1, 1]), (0.5, [1.08, 0.92, 1.08]), (0.6, [0.95, 1.08, 0.95]), (1.3, [1, 1, 1]))},
    "head": {"rotation": keys((0, Z), (0.5, [20, 0, 0]), (1.3, Z))}})
anim(M, "death", 1.6, {"root": {"rotation": keys((0, Z), (0.9, [0, 0, -90])), "position": keys((0, Z), (0.9, [0, -2, 0]))}})

# ------------------------------------------------------------------ 氷の妖精
M = "frost_sprite"
FLUT = {"right_wing": {"rotation": keys((0, [0, 25, 0]), (0.1, [0, -20, 0]), (0.2, [0, 25, 0]))},
        "left_wing": {"rotation": keys((0, [0, -25, 0]), (0.1, [0, 20, 0]), (0.2, [0, -25, 0]))}}
anim(M, "idle", 0.8, merge(FLUT, {"body": {"position": keys((0, Z), (0.4, [0, 0.8, 0]), (0.8, Z))}},
                           {"right_wing": FLUT["right_wing"]}), loop=True)
anim(M, "attack_throw", 0.8, merge(FLUT, {
    "snowball": {"rotation": keys((0, Z), (0.3, [-60, 0, 0]), (0.4, [60, 0, 0]), (0.8, Z)),
                 "scale": keys((0, [1, 1, 1]), (0.4, [1, 1, 1]), (0.45, [0.01, 0.01, 0.01]), (0.75, [0.01, 0.01, 0.01]), (0.8, [1, 1, 1]))},
    "body": {"rotation": keys((0, Z), (0.3, [-15, 0, 0]), (0.4, [15, 0, 0]), (0.8, Z))}}))
anim(M, "death", 1.0, merge(shrink("root", 1.0), {"root": {"position": keys((0, Z), (1.0, [0, -6, 0]))}}))

# ------------------------------------------------------------------ つらら虫
M = "icicle_crawler"
LEGS = {"right_legs": {"rotation": keys((0, [0, 15, 0]), (0.2, [0, -15, 0]), (0.4, [0, 15, 0]))},
        "left_legs": {"rotation": keys((0, [0, 15, 0]), (0.2, [0, -15, 0]), (0.4, [0, 15, 0]))}}
anim(M, "idle", 2.0, {"body": {"scale": keys((0, [1, 1, 1]), (1, [1, 1.05, 1]), (2, [1, 1, 1]))}}, loop=True)
anim(M, "walk", 0.4, merge(LEGS, {"body": {"rotation": keys((0, [0, 3, 0]), (0.2, [0, -3, 0]), (0.4, [0, 3, 0]))}}), loop=True)
anim(M, "attack_shoot", 1.0, {"body": {"rotation": keys((0, Z), (0.4, [-25, 0, 0]), (0.5, [10, 0, 0]), (1.0, Z)),
                                       "position": keys((0, Z), (0.4, [0, 1, 0]), (0.5, Z))}})
anim(M, "attack_bite", 0.6, {"body": {"position": keys((0, Z), (0.2, [0, 0, 1]), (0.3, [0, 0, -2]), (0.6, Z))}})
anim(M, "death", 1.0, {"root": {"rotation": keys((0, Z), (0.6, [0, 0, 180])), "position": keys((0, Z), (0.6, [0, 4, 0]))}})

if __name__ == "__main__":
    os.makedirs(OUT, exist_ok=True)
    for model, anims in ALL.items():
        with open(f"{OUT}/{model}.animation.json", "w") as f:
            json.dump({"format_version": "1.8.0", "animations": anims}, f, indent=1)
        print(model, len(anims))
