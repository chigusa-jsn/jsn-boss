"""ダンジョンモブ 闇のセットのアニメ。 python3 tools/dmob_dark_anims.py"""
import json, os, sys
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from dmob_fire_anims import keys, merge, walk, collapse, shrink, Z, OUT

ALL = {}


def anim(model, name, length, bones, loop=False):
    ALL.setdefault(model, {})[f"animation.{model}.{name}"] = {
        "loop": "hold_on_last_frame" if name == "death" else loop, "animation_length": length, "bones": bones}


SPIN = {"orbs": {"rotation": keys((0, Z), (1, [0, 90, 0]), (2, [0, 180, 0]), (3, [0, 270, 0]), (4, [0, 360, 0]))}}

# ------------------------------------------------------------------ 影の弟子
M = "shadow_apprentice"
anim(M, "idle", 4.0, merge(SPIN, {"body": {"position": keys((0, Z), (2, [0, 0.6, 0]), (4, Z))},
                                  "left_arm": {"rotation": keys((0, [-40, 0, 0]), (2, [-44, 0, 0]), (4, [-40, 0, 0]))}}), loop=True)
anim(M, "walk", 4.0, merge(SPIN, {"body": {"rotation": keys((0, [8, 0, 0]), (4, [8, 0, 0])),
                                           "position": keys((0, Z), (2, [0, 0.6, 0]), (4, Z))},
                                  "left_arm": {"rotation": keys((0, [-40, 0, 0]), (4, [-40, 0, 0]))}}), loop=True)
anim(M, "attack_orbs", 1.2, merge(SPIN, {
    "right_arm": {"rotation": keys((0, Z), (0.4, [-30, 0, -30]), (0.55, [-95, 0, 0]), (1.0, [-90, 0, 0]), (1.2, Z))},
    "left_arm": {"rotation": keys((0, [-40, 0, 0]), (1.2, [-40, 0, 0]))}}))
anim(M, "attack_backstab", 1.2, {
    "root": {"scale": keys((0, [1, 1, 1]), (0.3, [0.05, 1.3, 0.05]), (0.45, [0.05, 1.3, 0.05]), (0.6, [1, 1, 1]))},
    "right_arm": {"rotation": keys((0.6, Z), (0.75, [-120, 0, -30]), (0.9, [-30, 0, 20]), (1.2, Z))}})
anim(M, "attack_flash", 1.4, {
    "right_arm": {"rotation": keys((0, Z), (0.5, [-170, 0, -15]), (1.0, [-170, 0, -15]), (1.4, Z))},
    "left_arm": {"rotation": keys((0, [-40, 0, 0]), (0.5, [-170, 0, 15]), (1.0, [-170, 0, 15]), (1.4, [-40, 0, 0]))},
    "orbs": {"scale": keys((0, [1, 1, 1]), (0.6, [2.5, 2.5, 2.5]), (0.8, [0.5, 0.5, 0.5]), (1.4, [1, 1, 1]))}})
anim(M, "attack_summon", 1.5, {
    "left_arm": {"rotation": keys((0, [-40, 0, 0]), (0.5, [-120, 0, 0]), (1.1, [-120, 0, 0]), (1.5, [-40, 0, 0]))},
    "book": {"scale": keys((0, [1, 1, 1]), (0.5, [1.4, 1.4, 1.4]), (1.1, [1.4, 1.4, 1.4]), (1.5, [1, 1, 1]))}})
anim(M, "death", 1.6, merge(shrink("orbs", 0.6), {"body": {"position": keys((0, Z), (1.6, [0, -10, 0]))}}, shrink("root", 1.6)))

# ------------------------------------------------------------------ 影の暗殺者
M = "shadow_assassin"
anim(M, "idle", 2.0, {"body": {"rotation": keys((0, [6, 0, 0]), (1, [8, 0, 0]), (2, [6, 0, 0]))},
                      "right_arm": {"rotation": keys((0, [-20, 0, -10]), (1, [-24, 0, -12]), (2, [-20, 0, -10]))},
                      "left_arm": {"rotation": keys((0, [-20, 0, 10]), (1, [-24, 0, 12]), (2, [-20, 0, 10]))},
                      "scarf_tail": {"rotation": keys((0, Z), (1, [10, 0, 5]), (2, Z))}}, loop=True)
anim(M, "walk", 0.6, merge(walk(0.6, leg=40, arm=0, bob=0.8), {
    "body": {"rotation": keys((0, [20, 0, 0]), (0.6, [20, 0, 0]))},
    "right_arm": {"rotation": keys((0, [30, 0, -10]), (0.6, [30, 0, -10]))}, "left_arm": {"rotation": keys((0, [30, 0, 10]), (0.6, [30, 0, 10]))},
    "scarf_tail": {"rotation": keys((0, [40, 0, 0]), (0.3, [50, 0, 5]), (0.6, [40, 0, 0]))}}), loop=True)
anim(M, "attack_slash", 1.0, {
    "right_arm": {"rotation": keys((0, Z), (0.25, [-120, 30, -30]), (0.35, [-40, -30, 10]), (1.0, Z))},
    "left_arm": {"rotation": keys((0, Z), (0.45, [-120, -30, 30]), (0.55, [-40, 30, -10]), (1.0, Z))},
    "body": {"rotation": keys((0, Z), (0.25, [0, 25, 0]), (0.35, [10, -20, 0]), (0.55, [10, 20, 0]), (1.0, Z))}})
anim(M, "attack_vanish", 2.5, {
    "root": {"scale": keys((0, [1, 1, 1]), (0.3, [0.05, 1.2, 0.05]), (1.8, [0.05, 1.2, 0.05]), (1.9, [1, 1, 1]))},
    "right_arm": {"rotation": keys((1.9, [-150, 0, -20]), (2.05, [-30, 0, 10]), (2.5, Z))},
    "left_arm": {"rotation": keys((1.9, [-150, 0, 20]), (2.05, [-30, 0, -10]), (2.5, Z))}})
anim(M, "death", 1.4, merge(collapse(1.0, -3, -85), shrink("root", 1.4)))

# ------------------------------------------------------------------ 魔導書の魔物
M = "grimoire"
FLOAT = {"book": {"position": keys((0, Z), (1, [0, 1, 0]), (2, Z)), "rotation": keys((0, [0, 0, -3]), (1, [0, 0, 3]), (2, [0, 0, -3]))}}
anim(M, "idle", 2.0, merge(FLOAT, {"lid": {"rotation": keys((0, Z), (0.3, [25, 0, 0]), (0.5, Z), (1.3, Z), (1.5, [20, 0, 0]), (1.7, Z), (2.0, Z))}}), loop=True)
anim(M, "attack_orbs", 1.4, {
    "lid": {"rotation": keys((0, Z), (0.4, [-30, 0, 0]), (1.0, [-30, 0, 0]), (1.4, Z))},
    "book": {"rotation": keys((0, Z), (0.4, [-10, 0, 0]), (1.0, [-10, 0, 0]), (1.4, Z))},
    "eye": {"scale": keys((0, [1, 1, 1]), (0.4, [1.6, 1.6, 1.6]), (1.0, [1.6, 1.6, 1.6]), (1.4, [1, 1, 1]))}})
anim(M, "attack_bite", 0.8, {
    "lid": {"rotation": keys((0, Z), (0.25, [-35, 0, 0]), (0.4, [35, 0, 0]), (0.8, Z))},
    "book": {"position": keys((0, Z), (0.25, [0, 0, 3]), (0.4, [0, 0, -5]), (0.8, Z))}})
anim(M, "attack_summon", 1.4, {
    "lid": {"rotation": keys((0, Z), (0.4, [-40, 0, 0]), (1.0, [-40, 0, 0]), (1.4, Z))},
    "book": {"rotation": keys((0, Z), (0.4, [0, 180, 0]), (1.0, [0, 360, 0]), (1.4, [0, 360, 0]))}})
anim(M, "death", 1.2, {"lid": {"rotation": keys((0, Z), (0.3, [35, 0, 0]))},
                       "book": {"rotation": keys((0, Z), (1.2, [0, 0, 180])), "position": keys((0, Z), (1.2, [0, -8, 0]))}})

# ------------------------------------------------------------------ 影の小人
M = "shade_imp"
anim(M, "idle", 1.2, {"head": {"rotation": keys((0, [0, 0, -6]), (0.6, [0, 0, 6]), (1.2, [0, 0, -6]))},
                      "body": {"rotation": keys((0, [6, 0, 0]), (1.2, [6, 0, 0]))}}, loop=True)
anim(M, "walk", 0.4, merge(walk(0.4, leg=45, arm=45, bob=0.6), {"body": {"rotation": keys((0, [20, 0, 0]), (0.4, [20, 0, 0]))}}), loop=True)
anim(M, "attack_scratch", 0.6, {
    "right_arm": {"rotation": keys((0, Z), (0.2, [-150, 0, -20]), (0.35, [-20, 0, 10]), (0.6, Z))},
    "left_arm": {"rotation": keys((0, Z), (0.25, [-150, 0, 20]), (0.4, [-20, 0, -10]), (0.6, Z))}})
anim(M, "death", 1.0, merge(shrink("root", 1.0), {"root": {"position": keys((0, Z), (1.0, [0, -3, 0]))}}))

# ------------------------------------------------------------------ 闇の目玉
M = "dark_eye"
FLAP = {"right_wing": {"rotation": keys((0, [0, 0, 30]), (0.2, [0, 0, -30]), (0.4, [0, 0, 30]))},
        "left_wing": {"rotation": keys((0, [0, 0, -30]), (0.2, [0, 0, 30]), (0.4, [0, 0, -30]))},
        "body": {"position": keys((0, Z), (0.2, [0, 0.8, 0]), (0.4, Z))}}
anim(M, "idle", 0.4, FLAP, loop=True)
anim(M, "attack_gaze", 1.5, merge(FLAP, {"body": {"scale": keys((0, [1, 1, 1]), (0.4, [1.3, 1.3, 1.3]), (1.2, [1.3, 1.3, 1.3]), (1.5, [1, 1, 1]))}}))
anim(M, "death", 1.0, merge({"root": {"rotation": keys((0, Z), (1.0, [0, 0, 160])), "position": keys((0, Z), (1.0, [0, -6, 0]))}},
                            shrink("body", 1.0)))

if __name__ == "__main__":
    os.makedirs(OUT, exist_ok=True)
    for model, anims in ALL.items():
        with open(f"{OUT}/{model}.animation.json", "w") as f:
            json.dump({"format_version": "1.8.0", "animations": anims}, f, indent=1)
        print(model, len(anims))
