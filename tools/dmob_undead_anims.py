"""ダンジョンモブ アンデッドのセットのアニメ。 python3 tools/dmob_undead_anims.py"""
import json, os, sys
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from dmob_fire_anims import keys, merge, walk, collapse, shrink, Z, OUT

ALL = {}


def anim(model, name, length, bones, loop=False):
    ALL.setdefault(model, {})[f"animation.{model}.{name}"] = {
        "loop": "hold_on_last_frame" if name == "death" else loop, "animation_length": length, "bones": bones}


# ------------------------------------------------------------------ 腐肉の近衛
M = "rotting_guard"
anim(M, "idle", 2.4, {"body": {"rotation": keys((0, [3, 0, 0]), (1.2, [5, 0, -2]), (2.4, [3, 0, 0]))},
                      "head": {"rotation": keys((0, Z), (1.2, [0, 0, 8]), (2.4, Z))},
                      "left_arm": {"rotation": keys((0, [-10, 0, 0]), (1.2, [-14, 0, 0]), (2.4, [-10, 0, 0]))}}, loop=True)
anim(M, "walk", 1.4, merge(walk(1.4, leg=22, arm=6, bob=1.0), {"body": {"rotation": keys((0, [6, 0, 3]), (0.7, [6, 0, -3]), (1.4, [6, 0, 3]))},
                                                             "left_arm": {"rotation": keys((0, [-70, 0, 0]), (0.7, [-80, 0, 0]), (1.4, [-70, 0, 0]))}}), loop=True)
anim(M, "attack_sweep", 1.3, {
    "right_arm": {"rotation": keys((0, Z), (0.5, [-80, 70, -40]), (0.7, [-80, -60, 20]), (1.3, Z))},
    "halberd": {"rotation": keys((0, Z), (0.5, [80, 0, 0]), (0.7, [80, 0, 0]), (1.3, Z))},
    "body": {"rotation": keys((0, Z), (0.5, [0, 35, 0]), (0.7, [5, -40, 0]), (1.3, Z))}})
anim(M, "attack_slam", 1.6, {
    "right_arm": {"rotation": keys((0, Z), (0.6, [-170, 0, 0]), (0.8, [-40, 0, 0]), (1.6, Z))},
    "left_arm": {"rotation": keys((0, Z), (0.6, [-170, 0, 0]), (0.8, [-40, 0, 0]), (1.6, Z))},
    "halberd": {"rotation": keys((0, Z), (0.6, [20, 0, 0]), (0.8, [100, 0, 0]), (1.6, Z))},
    "body": {"rotation": keys((0, Z), (0.6, [-15, 0, 0]), (0.8, [25, 0, 0]), (1.6, Z))}})
anim(M, "attack_thrust", 1.0, {
    "right_arm": {"rotation": keys((0, Z), (0.35, [-60, 0, 0]), (0.5, [-85, 0, 0]), (1.0, Z))},
    "halberd": {"rotation": keys((0, Z), (0.35, [90, 0, 0]), (0.75, [90, 0, 0]), (1.0, Z)),
                "position": keys((0, Z), (0.35, [0, 0, 4]), (0.5, [0, 0, -4]), (0.75, Z))},
    "body": {"rotation": keys((0, Z), (0.35, [-5, 10, 0]), (0.5, [12, -5, 0]), (1.0, Z))}})
anim(M, "attack_summon", 1.5, {
    "left_arm": {"rotation": keys((0, Z), (0.5, [-160, 0, 20]), (1.1, [-160, 0, 20]), (1.5, Z))},
    "head": {"rotation": keys((0, Z), (0.5, [-30, 0, 0]), (1.1, [-30, 0, 0]), (1.5, Z))}})
anim(M, "death", 2.0, collapse(1.5, -4, 90))

# ------------------------------------------------------------------ 墓守の大鎌
M = "grave_reaper"
HOVER = {"body": {"position": keys((0, Z), (1, [0, 0.8, 0]), (2, Z))}}
anim(M, "idle", 2.0, merge(HOVER, {"head": {"rotation": keys((0, Z), (1, [0, 0, 6]), (2, Z))}}), loop=True)
anim(M, "walk", 2.0, merge(HOVER, {"body": {"rotation": keys((0, [10, 0, 0]), (2, [10, 0, 0]))}}), loop=True)
anim(M, "attack_reap", 1.4, {
    "right_arm": {"rotation": keys((0, Z), (0.6, [-90, 90, -60]), (0.8, [-90, -70, 30]), (1.4, Z))},
    "scythe": {"rotation": keys((0, Z), (0.6, [70, 0, 0]), (0.8, [70, 0, 0]), (1.4, Z))},
    "body": {"rotation": keys((0, Z), (0.6, [0, 50, 0]), (0.8, [0, -50, 0]), (1.4, Z))}})
anim(M, "attack_hook", 1.3, {
    "right_arm": {"rotation": keys((0, Z), (0.4, [-150, 0, -10]), (0.6, [-60, 0, 0]), (0.9, [-20, 0, 0]), (1.3, Z))},
    "scythe": {"rotation": keys((0, Z), (0.4, [30, 0, 0]), (0.6, [110, 0, 0]), (0.9, [40, 0, 0]), (1.3, Z))},
    "body": {"rotation": keys((0, Z), (0.4, [-10, 0, 0]), (0.6, [20, 0, 0]), (0.9, [-10, 0, 0]), (1.3, Z))}})
anim(M, "death", 1.6, merge(shrink("scythe", 0.6), {"body": {"position": keys((0, Z), (1.6, [0, -14, 0]))}}, shrink("root", 1.6)))

# ------------------------------------------------------------------ 骨の重装兵
M = "bone_juggernaut"
anim(M, "idle", 2.4, {"body": {"rotation": keys((0, Z), (1.2, [2, 0, 0]), (2.4, Z))},
                      "head": {"rotation": keys((0, Z), (1.2, [0, 8, 0]), (2.4, Z))}}, loop=True)
anim(M, "walk", 1.4, merge(walk(1.4, leg=20, arm=6, bob=1.0), {"left_arm": {"rotation": keys((0, [-30, -20, 0]), (1.4, [-30, -20, 0]))}}), loop=True)
anim(M, "attack_smash", 1.4, {
    "right_arm": {"rotation": keys((0, Z), (0.6, [-160, 0, -20]), (0.8, [-30, 0, 0]), (1.4, Z))},
    "mace": {"rotation": keys((0, Z), (0.6, [-40, 0, 0]), (0.8, [70, 0, 0]), (1.4, Z))},
    "body": {"rotation": keys((0, Z), (0.6, [-12, 15, 0]), (0.8, [20, -10, 0]), (1.4, Z))}})
anim(M, "attack_guard", 3.0, {
    "left_arm": {"rotation": keys((0, Z), (0.3, [-75, -35, 0]), (2.7, [-75, -35, 0]), (3.0, Z))},
    "body": {"rotation": keys((0, Z), (0.3, [10, -15, 0]), (2.7, [10, -15, 0]), (3.0, Z))},
    "root": {"position": keys((0, Z), (0.3, [0, -1.5, 0]), (2.7, [0, -1.5, 0]), (3.0, Z))}})
anim(M, "death", 1.6, merge(collapse(1.0, -10, 10), {
    "head": {"position": keys((0, Z), (1.0, [3, -18, -4])), "rotation": keys((0, Z), (1.0, [40, 0, 70]))},
    "right_arm": {"rotation": keys((0, Z), (0.8, [0, 0, -60]))}, "left_arm": {"rotation": keys((0, Z), (0.8, [0, 0, 60]))}}))

# ------------------------------------------------------------------ 這う骨
M = "crawling_bones"
anim(M, "idle", 2.0, {"head": {"rotation": keys((0, Z), (1, [0, 15, 0]), (2, Z))}}, loop=True)
anim(M, "walk", 0.8, {"right_arm": {"rotation": keys((0, [20, 15, 0]), (0.4, [-30, 15, 0]), (0.8, [20, 15, 0]))},
                      "left_arm": {"rotation": keys((0, [-30, -15, 0]), (0.4, [20, -15, 0]), (0.8, [-30, -15, 0]))},
                      "body": {"rotation": keys((0, [0, 5, 0]), (0.4, [0, -5, 0]), (0.8, [0, 5, 0]))}}, loop=True)
anim(M, "attack_grab", 0.8, {
    "right_arm": {"rotation": keys((0, Z), (0.3, [-40, -20, 0]), (0.45, [10, 20, 0]), (0.8, Z))},
    "left_arm": {"rotation": keys((0, Z), (0.3, [-40, 20, 0]), (0.45, [10, -20, 0]), (0.8, Z))},
    "head": {"rotation": keys((0, Z), (0.3, [-25, 0, 0]), (0.45, [15, 0, 0]), (0.8, Z))}})
anim(M, "death", 1.0, merge(shrink("head", 0.6), {"body": {"position": keys((0, Z), (1.0, [0, -3, 0]))}}))

# ------------------------------------------------------------------ 墓場の魂
M = "grave_wisp"
anim(M, "idle", 2.0, {"body": {"position": keys((0, Z), (1, [0, 1.2, 0]), (2, Z)),
                               "rotation": keys((0, [0, 0, -4]), (1, [0, 0, 4]), (2, [0, 0, -4]))},
                      "right_arm": {"rotation": keys((0, [-20, 0, -10]), (1, [-30, 0, -20]), (2, [-20, 0, -10]))},
                      "left_arm": {"rotation": keys((0, [-20, 0, 10]), (1, [-30, 0, 20]), (2, [-20, 0, 10]))}}, loop=True)
anim(M, "attack_touch", 0.8, {
    "right_arm": {"rotation": keys((0, Z), (0.3, [-100, 0, -10]), (0.5, [-70, 0, 0]), (0.8, Z))},
    "left_arm": {"rotation": keys((0, Z), (0.3, [-100, 0, 10]), (0.5, [-70, 0, 0]), (0.8, Z))},
    "body": {"scale": keys((0, [1, 1, 1]), (0.3, [1.15, 1.15, 1.15]), (0.8, [1, 1, 1]))}})
anim(M, "death", 1.2, merge(shrink("root", 1.2), {"body": {"position": keys((0, Z), (1.2, [0, 8, 0]))}}))

if __name__ == "__main__":
    os.makedirs(OUT, exist_ok=True)
    for model, anims in ALL.items():
        with open(f"{OUT}/{model}.animation.json", "w") as f:
            json.dump({"format_version": "1.8.0", "animations": anims}, f, indent=1)
        print(model, len(anims))
