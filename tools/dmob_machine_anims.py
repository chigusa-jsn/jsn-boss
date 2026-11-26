"""ダンジョンモブ 機械のセットのアニメ。 python3 tools/dmob_machine_anims.py"""
import json, os, sys
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from dmob_fire_anims import keys, merge, walk, collapse, shrink, Z, OUT

ALL = {}


def anim(model, name, length, bones, loop=False):
    ALL.setdefault(model, {})[f"animation.{model}.{name}"] = {
        "loop": "hold_on_last_frame" if name == "death" else loop, "animation_length": length, "bones": bones}


def spin(bone, length, axis=1, turns=1):
    def v(i):
        r = [0, 0, 0]; r[axis] = 90 * i * turns; return r
    return {bone: {"rotation": keys(*[(length * i / 4, v(i)) for i in range(5)])}}


# ------------------------------------------------------------------ 機甲の偵察機
M = "recon_drone"
ROT = merge(*[spin(f"rotor{i}", 0.2, 1, 1 if i % 2 else -1) for i in range(4)])
BOB = {"body": {"position": keys((0, Z), (0.6, [0, 1, 0]), (1.2, Z))}}
anim(M, "idle", 1.2, merge(ROT, BOB, merge(*[spin(f"rotor{i}", 1.2, 1, 6 if i % 2 else -6) for i in range(4)])), loop=True)
anim(M, "attack_volley", 1.4, merge(*[spin(f"rotor{i}", 1.4, 1, 7) for i in range(4)],
                                     {"body": {"position": keys((0, Z), (0.45, [0, 0, 1]), (0.5, Z), (0.6, [0, 0, 1]), (0.65, Z),
                                                                (0.75, [0, 0, 1]), (0.8, Z), (0.9, [0, 0, 1]), (0.95, Z), (1.4, Z))}}))
anim(M, "attack_mines", 1.2, merge(*[spin(f"rotor{i}", 1.2, 1, 6) for i in range(4)],
                                    {"body": {"rotation": keys((0, Z), (0.4, [20, 0, 0]), (0.6, [-10, 0, 0]), (1.2, Z))}}))
anim(M, "attack_spotlight", 1.6, merge(*[spin(f"rotor{i}", 1.6, 1, 8) for i in range(4)],
                                       {"body": {"scale": keys((0, [1, 1, 1]), (0.4, [1.1, 1.1, 1.1]), (1.3, [1.1, 1.1, 1.1]), (1.6, [1, 1, 1]))}}))
anim(M, "attack_ram", 1.4, merge(*[spin(f"rotor{i}", 1.4, 1, 10) for i in range(4)],
                                  {"body": {"rotation": keys((0, Z), (0.3, [-20, 0, 0]), (0.5, [35, 0, 0]), (1.1, [35, 0, 0]), (1.4, Z))}}))
anim(M, "death", 1.4, {"root": {"rotation": keys((0, Z), (1.4, [60, 360, 30])), "position": keys((0, Z), (1.4, [0, -14, 0]))}})

# ------------------------------------------------------------------ 歯車の戦車
M = "cog_tank"
anim(M, "idle", 4.0, {"turret": {"rotation": keys((0, Z), (1, [0, 25, 0]), (2, Z), (3, [0, -25, 0]), (4, Z))}}, loop=True)
anim(M, "walk", 0.6, {"hull": {"position": keys((0, Z), (0.15, [0, 0.4, 0]), (0.3, Z), (0.45, [0, 0.4, 0]), (0.6, Z))}}, loop=True)
anim(M, "attack_cannon", 1.4, {"turret": {"position": keys((0, Z), (0.55, Z), (0.6, [0, 0, 2]), (1.0, Z))},
                               "hull": {"rotation": keys((0, Z), (0.55, Z), (0.6, [-5, 0, 0]), (0.9, Z))}})
anim(M, "attack_ram", 1.6, {"hull": {"rotation": keys((0, Z), (0.4, [8, 0, 0]), (1.3, [8, 0, 0]), (1.6, Z))}})
anim(M, "death", 1.4, {"turret": {"position": keys((0, Z), (0.4, [0, 8, 0]), (1.0, [3, 2, 5])), "rotation": keys((0, Z), (1.0, [40, 0, 60]))},
                       "hull": {"rotation": keys((0, Z), (0.4, [-10, 0, 0]), (1.0, Z))}})

# ------------------------------------------------------------------ 蒸気の自動人形
M = "steam_automaton"
anim(M, "idle", 2.0, {"body": {"position": keys((0, Z), (0.25, [0, -0.4, 0]), (0.5, Z), (2.0, Z))},
                      "head": {"rotation": keys((0, Z), (1.0, [0, 15, 0]), (2.0, Z))}}, loop=True)
anim(M, "walk", 1.2, walk(1.2, leg=20, arm=12, bob=0.8), loop=True)
anim(M, "attack_steam", 1.6, {"body": {"rotation": keys((0, Z), (0.4, [-10, 0, 0]), (0.6, [15, 0, 0]), (1.3, [15, 0, 0]), (1.6, Z))},
                              "right_arm": {"rotation": keys((0, Z), (0.5, [-80, 0, -30]), (1.3, [-80, 0, -30]), (1.6, Z))},
                              "left_arm": {"rotation": keys((0, Z), (0.5, [-80, 0, 30]), (1.3, [-80, 0, 30]), (1.6, Z))}})
P = lambda a, b: keys((0, Z), (a, [-90, 0, 0]), (b, Z))
anim(M, "attack_punches", 1.6, {"right_arm": {"rotation": keys((0, Z), (0.2, [-90, 0, 0]), (0.35, Z), (0.8, [-90, 0, 0]), (0.95, Z), (1.6, Z))},
                                "left_arm": {"rotation": keys((0, Z), (0.5, [-90, 0, 0]), (0.65, Z), (1.1, [-90, 0, 0]), (1.25, Z), (1.6, Z))},
                                "body": {"rotation": keys((0, Z), (0.2, [0, -15, 0]), (0.5, [0, 15, 0]), (0.8, [0, -15, 0]), (1.1, [0, 15, 0]), (1.6, Z))}})
anim(M, "death", 1.6, merge(collapse(1.2, -4, 85), {"head": {"rotation": keys((0, Z), (0.8, [0, 0, 40]))}}))

# ------------------------------------------------------------------ ぜんまいねずみ
M = "clockwork_mouse"
KEY = lambda L, n: spin("key", L, 2, n)
anim(M, "idle", 2.0, KEY(2.0, 1), loop=True)
anim(M, "walk", 0.5, merge(KEY(0.5, 1), {"body": {"position": keys((0, Z), (0.125, [0, 0.3, 0]), (0.25, Z), (0.375, [0, 0.3, 0]), (0.5, Z))}}), loop=True)
anim(M, "attack_boom", 1.0, merge(KEY(1.0, 4), {"body": {"scale": keys((0, [1, 1, 1]), (0.8, [1.5, 1.5, 1.5]), (1.0, [1.7, 1.7, 1.7]))}}))
anim(M, "death", 0.6, {"body": {"rotation": keys((0, Z), (0.4, [0, 0, 180])), "position": keys((0, Z), (0.4, [0, 2, 0]))}})

# ------------------------------------------------------------------ 小型ドローン
M = "mini_drone"
anim(M, "idle", 0.8, merge(spin("rotor", 0.8, 1, 6), {"body": {"position": keys((0, Z), (0.4, [0, 0.6, 0]), (0.8, Z))}}), loop=True)
anim(M, "attack_shoot", 0.8, merge(spin("rotor", 0.8, 1, 6), {"body": {"position": keys((0, Z), (0.35, Z), (0.4, [0, 0, 1]), (0.6, Z))}}))
anim(M, "death", 1.0, {"root": {"rotation": keys((0, Z), (1.0, [40, 360, 20])), "position": keys((0, Z), (1.0, [0, -8, 0]))}})

if __name__ == "__main__":
    os.makedirs(OUT, exist_ok=True)
    for model, anims in ALL.items():
        with open(f"{OUT}/{model}.animation.json", "w") as f:
            json.dump({"format_version": "1.8.0", "animations": anims}, f, indent=1)
        print(model, len(anims))
