"""ダンジョンモブ 虚空のセットのアニメ。 python3 tools/dmob_void_anims.py"""
import json, os, sys
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from dmob_fire_anims import keys, merge, walk, collapse, shrink, Z, OUT

ALL = {}


def anim(model, name, length, bones, loop=False):
    ALL.setdefault(model, {})[f"animation.{model}.{name}"] = {
        "loop": "hold_on_last_frame" if name == "death" else loop, "animation_length": length, "bones": bones}


BLINK = lambda t0, t1, t2: {"root": {"scale": keys((0, [1, 1, 1]), (t0, [0.05, 1.3, 0.05]), (t1, [0.05, 1.3, 0.05]), (t2, [1, 1, 1]))}}

# ------------------------------------------------------------------ 虚空の斥候
M = "void_scout"
anim(M, "idle", 2.4, {"head": {"rotation": keys((0, Z), (1.2, [0, 0, 15]), (2.4, Z))},
                      "right_arm": {"rotation": keys((0, [0, 0, -4]), (1.2, [5, 0, -8]), (2.4, [0, 0, -4]))},
                      "left_arm": {"rotation": keys((0, [0, 0, 4]), (1.2, [5, 0, 8]), (2.4, [0, 0, 4]))}}, loop=True)
anim(M, "walk", 1.6, walk(1.6, leg=22, arm=10, bob=0.8), loop=True)
anim(M, "attack_blink", 1.2, merge(BLINK(0.25, 0.4, 0.5), {
    "right_arm": {"rotation": keys((0.5, Z), (0.6, [-140, 0, -30]), (0.75, [-20, 0, 20]), (1.2, Z))},
    "left_arm": {"rotation": keys((0.5, Z), (0.65, [-140, 0, 30]), (0.8, [-20, 0, -20]), (1.2, Z))}}))
anim(M, "attack_levitate", 1.4, {
    "right_arm": {"rotation": keys((0, Z), (0.4, [-90, 0, 0]), (0.6, [-140, 0, 0]), (1.1, [-140, 0, 0]), (1.4, Z))},
    "head": {"rotation": keys((0, Z), (0.4, [-15, 0, 0]), (1.1, [-15, 0, 0]), (1.4, Z))}})
anim(M, "attack_pull", 1.3, {
    "right_arm": {"rotation": keys((0, Z), (0.4, [-95, 0, 0]), (0.6, [-30, 0, 0]), (1.3, Z))},
    "left_arm": {"rotation": keys((0, Z), (0.4, [-95, 0, 0]), (0.6, [-30, 0, 0]), (1.3, Z))},
    "body": {"rotation": keys((0, Z), (0.4, [15, 0, 0]), (0.6, [-10, 0, 0]), (1.3, Z))}})
anim(M, "attack_barrage", 1.4, {
    "right_arm": {"rotation": keys((0, Z), (0.35, [-90, 0, -40]), (1.1, [-90, 0, -40]), (1.4, Z))},
    "left_arm": {"rotation": keys((0, Z), (0.35, [-90, 0, 40]), (1.1, [-90, 0, 40]), (1.4, Z))}})
anim(M, "death", 1.6, merge(collapse(1.0, -4, 80), shrink("root", 1.6)))

# ------------------------------------------------------------------ 虚空の狩人
M = "void_stalker"
anim(M, "idle", 1.6, {"body": {"rotation": keys((0, Z), (0.8, [4, 0, 0]), (1.6, Z))},
                      "head": {"rotation": keys((0, Z), (0.4, [0, 20, 0]), (1.2, [0, -20, 0]), (1.6, Z))}}, loop=True)
anim(M, "walk", 0.6, merge(walk(0.6, leg=40, arm=30, bob=0.8), {"body": {"rotation": keys((0, [15, 0, 0]), (0.6, [15, 0, 0]))}}), loop=True)
anim(M, "attack_strike", 1.0, {
    "right_arm": {"rotation": keys((0, Z), (0.25, [-130, 0, -20]), (0.35, [-20, 0, 10]), (1.0, Z))},
    "left_arm": {"rotation": keys((0, Z), (0.45, [-130, 0, 20]), (0.55, [-20, 0, -10]), (1.0, Z))}})
anim(M, "attack_blink", 1.2, merge(BLINK(0.25, 0.4, 0.5), {
    "right_arm": {"rotation": keys((0.5, Z), (0.6, [-150, 0, -10]), (0.7, [-20, 0, 0]), (1.2, Z))},
    "left_arm": {"rotation": keys((0.5, Z), (0.6, [-150, 0, 10]), (0.7, [-20, 0, 0]), (1.2, Z))}}))
anim(M, "death", 1.4, merge(collapse(1.0, -3, 80), shrink("root", 1.4)))

# ------------------------------------------------------------------ 次元の歪み
M = "rift_wraith"
anim(M, "idle", 2.0, {"rift": {"scale": keys((0, [1, 1, 1]), (1, [1.15, 1.05, 1]), (2, [1, 1, 1])),
                               "position": keys((0, Z), (1, [0, 1, 0]), (2, Z))},
                      "right_hand": {"rotation": keys((0, [0, 0, -10]), (1, [0, 0, -25]), (2, [0, 0, -10]))},
                      "left_hand": {"rotation": keys((0, [0, 0, 10]), (1, [0, 0, 25]), (2, [0, 0, 10]))}}, loop=True)
anim(M, "attack_pull", 2.0, {
    "right_hand": {"rotation": keys((0, Z), (0.4, [-80, 0, 20]), (1.6, [-80, 0, 20]), (2.0, Z))},
    "left_hand": {"rotation": keys((0, Z), (0.4, [-80, 0, -20]), (1.6, [-80, 0, -20]), (2.0, Z))},
    "rift": {"scale": keys((0, [1, 1, 1]), (0.4, [1.5, 1.1, 1]), (1.6, [1.5, 1.1, 1]), (2.0, [1, 1, 1]))}})
anim(M, "attack_warp", 1.4, {"rift": {"scale": keys((0, [1, 1, 1]), (0.5, [2.2, 1.3, 1]), (0.8, [0.5, 1, 1]), (1.4, [1, 1, 1]))}})
anim(M, "death", 1.0, {"rift": {"scale": keys((0, [1, 1, 1]), (0.4, [2, 1.2, 1]), (1.0, [0.01, 1.5, 1]))}})

# ------------------------------------------------------------------ 虚空のかけら
M = "void_shard"
SPIN = {"crystal": {"rotation": keys((0, Z), (0.5, [0, 90, 0]), (1, [0, 180, 0]), (1.5, [0, 270, 0]), (2, [0, 360, 0])),
                    "position": keys((0, Z), (1, [0, 1, 0]), (2, Z))}}
anim(M, "idle", 2.0, SPIN, loop=True)
anim(M, "attack_ram", 0.8, merge(BLINK(0.15, 0.25, 0.35), {"crystal": {"rotation": keys((0, Z), (0.8, [0, 720, 0]))}}))
anim(M, "death", 0.6, {"crystal": {"scale": keys((0, [1, 1, 1]), (0.25, [1.5, 1.5, 1.5]), (0.6, [0.01, 0.01, 0.01]))}})

# ------------------------------------------------------------------ 監視の小目
M = "watcher_mote"
RING = {"ring": {"rotation": keys((0, Z), (0.75, [0, 90, 0]), (1.5, [0, 180, 0]), (2.25, [0, 270, 0]), (3, [0, 360, 0]))},
        "eye": {"position": keys((0, Z), (1.5, [0, 1, 0]), (3, Z))}}
anim(M, "idle", 3.0, RING, loop=True)
anim(M, "attack_beam", 1.6, merge(RING, {"eye": {"scale": keys((0, [1, 1, 1]), (0.4, [1.3, 1.3, 1.3]), (1.3, [1.3, 1.3, 1.3]), (1.6, [1, 1, 1]))}}))
anim(M, "death", 1.0, merge(shrink("ring", 0.5), {"eye": {"position": keys((0, Z), (1.0, [0, -8, 0]))}}, shrink("root", 1.0)))

if __name__ == "__main__":
    os.makedirs(OUT, exist_ok=True)
    for model, anims in ALL.items():
        with open(f"{OUT}/{model}.animation.json", "w") as f:
            json.dump({"format_version": "1.8.0", "animations": anims}, f, indent=1)
        print(model, len(anims))
