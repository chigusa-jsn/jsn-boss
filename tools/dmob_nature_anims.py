"""ダンジョンモブ 自然のセットのアニメ。 python3 tools/dmob_nature_anims.py"""
import json, os, sys
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from dmob_fire_anims import keys, merge, walk, collapse, shrink, Z, OUT
from dmob_ice_anims import quad_walk

ALL = {}


def anim(model, name, length, bones, loop=False):
    ALL.setdefault(model, {})[f"animation.{model}.{name}"] = {
        "loop": "hold_on_last_frame" if name == "death" else loop, "animation_length": length, "bones": bones}


# ------------------------------------------------------------------ 胞子の苗床
M = "spore_nursery"
SWAY = {"right_vine": {"rotation": keys((0, [0, 10, 0]), (1.5, [0, -10, 5]), (3, [0, 10, 0]))},
        "left_vine": {"rotation": keys((0, [0, -10, 0]), (1.5, [0, 10, -5]), (3, [0, -10, 0]))},
        "pods": {"position": keys((0, Z), (1.5, [0, 0.8, 0]), (3, Z))}}
anim(M, "idle", 3.0, merge(SWAY, {"bulb": {"scale": keys((0, [1, 1, 1]), (1.5, [1.05, 1.05, 1.05]), (3, [1, 1, 1]))},
                                  "stem": {"rotation": keys((0, [0, 0, -2]), (1.5, [0, 0, 2]), (3, [0, 0, -2]))}}), loop=True)
anim(M, "attack_birth", 1.5, merge(SWAY, {"bulb": {"scale": keys((0, [1, 1, 1]), (0.6, [1.25, 0.85, 1.25]), (0.9, [0.9, 1.2, 0.9]), (1.5, [1, 1, 1]))}}))
anim(M, "attack_spores", 1.5, merge(SWAY, {"bulb": {"scale": keys((0, [1, 1, 1]), (0.4, [0.85, 0.85, 0.85]), (0.55, [1.3, 1.3, 1.3]), (1.5, [1, 1, 1]))},
                                           "pods": {"scale": keys((0, [1, 1, 1]), (0.55, [1.6, 1.6, 1.6]), (1.5, [1, 1, 1]))}}))
anim(M, "attack_vines", 1.3, {"right_vine": {"rotation": keys((0, Z), (0.4, [0, 70, -40]), (0.6, [0, -40, 10]), (1.3, Z))},
                              "left_vine": {"rotation": keys((0, Z), (0.4, [0, -70, 40]), (0.6, [0, 40, -10]), (1.3, Z))},
                              "stem": {"rotation": keys((0, Z), (0.4, [-10, 0, 0]), (0.6, [15, 0, 0]), (1.3, Z))}})
anim(M, "attack_seeds", 1.3, merge(SWAY, {"bulb": {"rotation": keys((0, Z), (0.2, [0, 0, 8]), (0.4, [0, 0, -8]), (0.6, [0, 0, 8]), (0.8, Z)),
                                                   "scale": keys((0, [1, 1, 1]), (0.6, [1.1, 1.1, 1.1]), (1.3, [1, 1, 1]))}}))
anim(M, "death", 2.0, {"stem": {"rotation": keys((0, Z), (1.5, [70, 0, 0]))}, "bulb": {"scale": keys((0, [1, 1, 1]), (2.0, [0.6, 0.4, 0.6]))}})

# ------------------------------------------------------------------ 毒花の魔物
M = "venus_snapper"
anim(M, "idle", 2.4, {"top_jaw": {"rotation": keys((0, Z), (0.6, [12, 0, 0]), (0.8, Z), (2.4, Z))},
                      "stem": {"rotation": keys((0, [0, 0, -4]), (1.2, [0, 0, 4]), (2.4, [0, 0, -4]))}}, loop=True)
anim(M, "attack_bite", 0.9, {"stem": {"rotation": keys((0, Z), (0.3, [-20, 0, 0]), (0.45, [40, 0, 0]), (0.9, Z))},
                             "top_jaw": {"rotation": keys((0, Z), (0.3, [-35, 0, 0]), (0.45, [30, 0, 0]), (0.9, Z))}})
anim(M, "attack_vine", 1.3, {"stem": {"rotation": keys((0, Z), (0.4, [30, 0, 0]), (0.7, [-25, 0, 0]), (1.3, Z))},
                             "top_jaw": {"rotation": keys((0, Z), (0.4, [-40, 0, 0]), (0.7, [30, 0, 0]), (1.3, Z))}})
anim(M, "attack_spit", 1.1, {"top_jaw": {"rotation": keys((0, Z), (0.4, [-45, 0, 0]), (0.6, [-45, 0, 0]), (1.1, Z))},
                             "head": {"scale": keys((0, [1, 1, 1]), (0.35, [1.15, 0.9, 1.15]), (0.5, [0.9, 1.1, 0.9]), (1.1, [1, 1, 1]))}})
anim(M, "death", 1.6, {"stem": {"rotation": keys((0, Z), (1.2, [80, 0, 0]))}, "top_jaw": {"rotation": keys((0, Z), (0.6, [30, 0, 0]))}})

# ------------------------------------------------------------------ 茨の大鹿
M = "thorn_stag"
anim(M, "idle", 3.0, {"head": {"rotation": keys((0, Z), (1.0, [15, 10, 0]), (2.0, [15, -10, 0]), (3.0, Z))},
                      "tail": {"rotation": keys((0, Z), (0.2, [-30, 0, 0]), (0.4, Z), (3.0, Z))}}, loop=True)
anim(M, "walk", 0.9, quad_walk(0.9, 28, 0.8), loop=True)
anim(M, "attack_charge", 1.8, merge(quad_walk(0.3, 40), {"head": {"rotation": keys((0, Z), (0.4, [40, 0, 0]), (1.5, [40, 0, 0]), (1.8, Z))}}))
anim(M, "attack_thorns", 1.8, {
    "root": {"rotation": keys((0, Z), (0.4, [-35, 0, 0]), (0.55, Z), (0.9, [-25, 0, 0]), (1.05, Z), (1.8, Z)),
             "position": keys((0, Z), (0.4, [0, 0, 4]), (0.55, Z), (0.9, [0, 0, 3]), (1.05, Z))},
    "front_right_leg": {"rotation": keys((0, Z), (0.4, [-60, 0, 0]), (0.55, Z), (0.9, [-50, 0, 0]), (1.05, Z))},
    "front_left_leg": {"rotation": keys((0, Z), (0.4, [-60, 0, 0]), (0.55, Z), (0.9, [-50, 0, 0]), (1.05, Z))}})
anim(M, "attack_gore", 1.0, {"head": {"rotation": keys((0, Z), (0.35, [40, 0, 0]), (0.5, [-30, 0, 0]), (1.0, Z))},
                             "body": {"position": keys((0, Z), (0.35, [0, 0, 1]), (0.5, [0, 0, -3]), (1.0, Z))}})
anim(M, "death", 1.6, {"root": {"rotation": keys((0, Z), (0.8, [0, 0, 90])), "position": keys((0, Z), (0.8, [0, -2, 0]))}})

# ------------------------------------------------------------------ キノコ兵
M = "mushroom_soldier"
anim(M, "idle", 1.6, {"body": {"rotation": keys((0, [0, 0, -3]), (0.8, [0, 0, 3]), (1.6, [0, 0, -3]))}}, loop=True)
anim(M, "walk", 0.6, walk(0.6, leg=35, arm=20, bob=0.5), loop=True)
anim(M, "attack_poke", 0.8, {"right_arm": {"rotation": keys((0, Z), (0.25, [-60, 0, 0]), (0.4, [-90, 0, 0]), (0.8, Z))},
                             "spear": {"position": keys((0, Z), (0.25, [0, 0, 2]), (0.4, [0, 0, -3]), (0.8, Z))}})
anim(M, "attack_spore", 1.0, {"body": {"scale": keys((0, [1, 1, 1]), (0.3, [1.2, 0.85, 1.2]), (0.45, [0.9, 1.15, 0.9]), (1.0, [1, 1, 1]))}})
anim(M, "death", 1.0, merge(collapse(0.6, -1, -90), shrink("root", 1.0)))

# ------------------------------------------------------------------ 種まきリス
M = "seed_squirrel"
anim(M, "idle", 1.6, {"tail": {"rotation": keys((0, Z), (0.2, [15, 0, 0]), (0.4, Z), (1.6, Z))},
                      "head": {"rotation": keys((0, Z), (0.6, [0, 25, 0]), (1.2, [0, -25, 0]), (1.6, Z))}}, loop=True)
anim(M, "walk", 0.4, {"root": {"position": keys((0, Z), (0.2, [0, 1.5, 0]), (0.4, Z))},
                      "tail": {"rotation": keys((0, [10, 0, 0]), (0.2, [-10, 0, 0]), (0.4, [10, 0, 0]))}}, loop=True)
anim(M, "attack_throw", 0.8, {"body": {"rotation": keys((0, Z), (0.25, [-20, 0, 0]), (0.4, [20, 0, 0]), (0.8, Z))},
                              "acorn": {"scale": keys((0, [1, 1, 1]), (0.4, [1, 1, 1]), (0.45, [0.01, 0.01, 0.01]), (0.75, [0.01, 0.01, 0.01]), (0.8, [1, 1, 1]))}})
anim(M, "death", 0.8, {"root": {"rotation": keys((0, Z), (0.5, [0, 0, 180])), "position": keys((0, Z), (0.5, [0, 3, 0]))}})

if __name__ == "__main__":
    os.makedirs(OUT, exist_ok=True)
    for model, anims in ALL.items():
        with open(f"{OUT}/{model}.animation.json", "w") as f:
            json.dump({"format_version": "1.8.0", "animations": anims}, f, indent=1)
        print(model, len(anims))
