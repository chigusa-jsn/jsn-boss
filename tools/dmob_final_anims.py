"""ダンジョンモブ 残りの中ボス4体のアニメ。 python3 tools/dmob_final_anims.py"""
import json, os, sys
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from dmob_fire_anims import keys, merge, walk, collapse, shrink, Z, OUT

ALL = {}


def anim(model, name, length, bones, loop=False):
    ALL.setdefault(model, {})[f"animation.{model}.{name}"] = {
        "loop": "hold_on_last_frame" if name == "death" else loop, "animation_length": length, "bones": bones}


def flap(r, l, length, amp=30):
    h = length / 2
    return {r: {"rotation": keys((0, [0, 0, amp]), (h, [0, 0, -amp]), (length, [0, 0, amp]))},
            l: {"rotation": keys((0, [0, 0, -amp]), (h, [0, 0, amp]), (length, [0, 0, -amp]))}}


# ------------------------------------------------------------------ 刃の飛竜
M = "blade_wyvern"
FL = flap("right_wing", "left_wing", 0.8, 28)
anim(M, "idle", 0.8, merge(FL, {"body": {"position": keys((0, Z), (0.4, [0, 1.2, 0]), (0.8, Z))},
                                "tail": {"rotation": keys((0, [0, -10, 0]), (0.4, [0, 10, 0]), (0.8, [0, -10, 0]))}}), loop=True)
anim(M, "attack_dive", 1.4, {"right_wing": {"rotation": keys((0, Z), (0.3, [0, -50, 40]), (1.1, [0, -50, 40]), (1.4, Z))},
                             "left_wing": {"rotation": keys((0, Z), (0.3, [0, 50, -40]), (1.1, [0, 50, -40]), (1.4, Z))},
                             "body": {"rotation": keys((0, Z), (0.3, [35, 0, 0]), (1.1, [35, 0, 0]), (1.4, Z))}})
anim(M, "attack_swords", 1.2, merge(flap("right_wing", "left_wing", 0.3, 45),
                                    {"body": {"rotation": keys((0, Z), (0.3, [-20, 0, 0]), (0.9, [-20, 0, 0]), (1.2, Z))},
                                     "head": {"rotation": keys((0, Z), (0.4, [-25, 0, 0]), (0.6, [10, 0, 0]), (1.2, Z))}}))
anim(M, "attack_tail", 1.0, merge(FL, {"body": {"rotation": keys((0, Z), (0.5, [0, 360, 0]), (1.0, [0, 360, 0]))},
                                       "tail": {"rotation": keys((0, Z), (0.25, [0, 50, 0]), (0.5, [0, -30, 0]), (1.0, Z))}}))
anim(M, "attack_rain", 1.4, merge(flap("right_wing", "left_wing", 0.35, 40),
                                  {"head": {"rotation": keys((0, Z), (0.5, [-50, 0, 0]), (1.0, [-50, 0, 0]), (1.4, Z))},
                                   "body": {"rotation": keys((0, Z), (0.5, [-30, 0, 0]), (1.0, [-30, 0, 0]), (1.4, Z))}}))
anim(M, "death", 1.4, {"root": {"rotation": keys((0, Z), (1.4, [70, 0, 160])), "position": keys((0, Z), (1.4, [0, -12, 0]))}})

# ------------------------------------------------------------------ 黒翼の従者
M = "blackwing_attendant"
WF = {"right_wing": {"rotation": keys((0, [0, 0, 10]), (0.6, [0, -20, -10]), (1.2, [0, 0, 10]))},
      "left_wing": {"rotation": keys((0, [0, 0, -10]), (0.6, [0, 20, 10]), (1.2, [0, 0, -10]))}}
HOV = {"body": {"position": keys((0, Z), (0.6, [0, 1, 0]), (1.2, Z))},
       "right_leg": {"rotation": keys((0, [8, 0, 0]), (1.2, [8, 0, 0]))}, "left_leg": {"rotation": keys((0, [14, 0, 0]), (1.2, [14, 0, 0]))}}
anim(M, "idle", 1.2, merge(WF, HOV, {"right_arm": {"rotation": keys((0, [-20, 0, 0]), (1.2, [-20, 0, 0]))}}), loop=True)
anim(M, "attack_feathers", 1.2, merge(HOV, {
    "right_wing": {"rotation": keys((0, Z), (0.4, [0, 60, 20]), (0.6, [0, -60, -20]), (1.2, Z))},
    "left_wing": {"rotation": keys((0, Z), (0.4, [0, -60, -20]), (0.6, [0, 60, 20]), (1.2, Z))}}))
anim(M, "attack_dive", 1.6, merge(WF, {
    "right_arm": {"rotation": keys((0, Z), (0.4, [-150, 0, 0]), (0.6, [-80, 0, 0]), (1.3, [-80, 0, 0]), (1.6, Z))},
    "spear": {"rotation": keys((0, Z), (0.6, [0, 0, 0]), (1.6, Z))},
    "body": {"rotation": keys((0, Z), (0.4, [-15, 0, 0]), (0.6, [40, 0, 0]), (1.3, [40, 0, 0]), (1.6, Z))}}))
anim(M, "attack_swords", 1.4, merge(WF, HOV, {
    "left_arm": {"rotation": keys((0, Z), (0.5, [-170, 0, 10]), (1.0, [-170, 0, 10]), (1.4, Z))}}))
anim(M, "death", 1.6, merge({"right_wing": {"rotation": keys((0, Z), (1.0, [0, 0, 70]))}, "left_wing": {"rotation": keys((0, Z), (1.0, [0, 0, -70]))}},
                            collapse(1.4, -4, 85)))

# ------------------------------------------------------------------ 狐火の巫女
M = "foxfire_maiden"
FLAMES = {"flames": {"rotation": keys((0, Z), (1, [0, 90, 0]), (2, [0, 180, 0]), (3, [0, 270, 0]), (4, [0, 360, 0]))},
          "tails": {"rotation": keys((0, [0, -8, 0]), (2, [0, 8, 0]), (4, [0, -8, 0]))}}
anim(M, "idle", 4.0, merge(FLAMES, {"body": {"position": keys((0, Z), (2, [0, 0.4, 0]), (4, Z))},
                                    "right_arm": {"rotation": keys((0, [-30, 0, 0]), (4, [-30, 0, 0]))}}), loop=True)
anim(M, "walk", 4.0, merge(FLAMES, {"body": {"rotation": keys((0, [6, 0, 0]), (4, [6, 0, 0]))}, "right_arm": {"rotation": keys((0, [-30, 0, 0]), (4, [-30, 0, 0]))}}), loop=True)
anim(M, "attack_foxfire", 1.2, merge(FLAMES, {"right_arm": {"rotation": keys((0, [-30, 0, 0]), (0.4, [-140, 0, -20]), (0.6, [-70, 0, 20]), (1.2, [-30, 0, 0]))}}))
anim(M, "attack_pillar", 1.4, merge(FLAMES, {
    "right_arm": {"rotation": keys((0, [-30, 0, 0]), (0.5, [-170, 0, -10]), (1.0, [-170, 0, -10]), (1.4, [-30, 0, 0]))},
    "left_arm": {"rotation": keys((0, Z), (0.5, [-170, 0, 10]), (1.0, [-170, 0, 10]), (1.4, Z))}}))
anim(M, "attack_charm", 1.0, merge(FLAMES, {"left_arm": {"rotation": keys((0, Z), (0.35, [-60, 40, 0]), (0.5, [-90, -30, 0]), (1.0, Z))}}))
anim(M, "attack_blink", 0.8, {"root": {"scale": keys((0, [1, 1, 1]), (0.25, [0.05, 1.3, 0.05]), (0.4, [0.05, 1.3, 0.05]), (0.55, [1, 1, 1]))}})
anim(M, "death", 1.6, merge(shrink("flames", 0.6), {"body": {"rotation": keys((0, Z), (1.0, [15, 0, 0])), "position": keys((0, Z), (1.6, [0, -6, 0]))}},
                            shrink("root", 1.6)))

# ------------------------------------------------------------------ 樹人の守り手
M = "treant_warden"
SWAY = {"body": {"rotation": keys((0, [0, 0, -2]), (2, [0, 0, 2]), (4, [0, 0, -2]))},
        "sapling": {"rotation": keys((0, [0, 0, 6]), (2, [0, 0, -6]), (4, [0, 0, 6]))}}
anim(M, "idle", 4.0, SWAY, loop=True)
anim(M, "walk", 1.8, merge(walk(1.8, leg=16, arm=10, bob=1.0), {"sapling": {"rotation": keys((0, [0, 0, 8]), (0.9, [0, 0, -8]), (1.8, [0, 0, 8]))}}), loop=True)
anim(M, "attack_roots", 1.6, {
    "right_arm": {"rotation": keys((0, Z), (0.6, [-160, 0, 0]), (0.8, [-20, 0, 0]), (1.3, [-20, 0, 0]), (1.6, Z))},
    "left_arm": {"rotation": keys((0, Z), (0.6, [-160, 0, 0]), (0.8, [-20, 0, 0]), (1.3, [-20, 0, 0]), (1.6, Z))},
    "body": {"rotation": keys((0, Z), (0.6, [-10, 0, 0]), (0.8, [25, 0, 0]), (1.3, [25, 0, 0]), (1.6, Z))}})
anim(M, "attack_heal", 2.0, merge(SWAY, {
    "right_arm": {"rotation": keys((0, Z), (0.5, [-150, 0, -30]), (1.6, [-150, 0, -30]), (2.0, Z))},
    "left_arm": {"rotation": keys((0, Z), (0.5, [-150, 0, 30]), (1.6, [-150, 0, 30]), (2.0, Z))},
    "sapling": {"scale": keys((0, [1, 1, 1]), (0.8, [1.8, 1.8, 1.8]), (1.6, [1.8, 1.8, 1.8]), (2.0, [1, 1, 1]))}}))
anim(M, "attack_sweep", 1.3, {
    "right_arm": {"rotation": keys((0, Z), (0.5, [-80, 70, -50]), (0.7, [-80, -70, 30]), (1.3, Z))},
    "body": {"rotation": keys((0, Z), (0.5, [0, 40, 0]), (0.7, [0, -45, 0]), (1.3, Z))}})
anim(M, "death", 2.0, merge(collapse(1.6, -2, 90), {"sapling": {"scale": keys((0, [1, 1, 1]), (1.0, [0.01, 0.01, 0.01]))}}))

if __name__ == "__main__":
    os.makedirs(OUT, exist_ok=True)
    for model, anims in ALL.items():
        with open(f"{OUT}/{model}.animation.json", "w") as f:
            json.dump({"format_version": "1.8.0", "animations": anims}, f, indent=1)
        print(model, len(anims))
