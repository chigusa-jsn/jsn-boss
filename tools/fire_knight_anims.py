"""Fire Knight animations (GeckoLib / Bedrock animation format).
Angle conventions (checked in the preview renderer):
  rotation X  : negative = raise a hanging limb forward, positive = lean torso forward
  rotation Y  : body twist
  rotation Z  : right_arm negative / left_arm positive = spread outward
Sword rest rotation is X -60 (blade forward-down). Sword X +60 lines the blade up with the arm.
"""
import json, os

OUT = "src/main/resources/assets/jsnboss/animations/entity"
Z = [0, 0, 0]
ONE = [1, 1, 1]

def flames(length, big=1.0, out=False):
    """flicker keys for every flame bone, offset in phase so they do not pulse together"""
    res = {}
    for k, name in enumerate(["plume", "right_flame", "left_flame", "cape_fire", "blade_fire"]):
        keys, steps = {}, max(4, round(length / 0.25))
        for i in range(steps + 1):
            t = round(length * i / steps, 3)
            w = [1.0, 1.18, 0.9, 1.12, 0.95, 1.22, 0.88][(i + k * 2) % 7]
            sx = 1 + (w - 1) * 0.4
            s = [round(sx * big, 3), round(w * big, 3), round(sx * big, 3)]
            if out:
                f = max(0.01, 1 - t / length)
                s = [round(v * f, 3) for v in s]
            keys[f"{t}"] = s
        if not out:
            keys[f"{round(length, 3)}"] = keys["0.0"]
        res[name] = {"scale": keys}
    return res

def merge(*parts):
    out = {}
    for p in parts:
        for bone, chans in p.items():
            out.setdefault(bone, {}).update(chans)
    return out

def keys(*pairs):
    return {f"{float(t)}": v for t, v in pairs}

A = {}

# ---------------------------------------------------------------- idle / walk
A["idle"] = dict(loop=True, length=2.0, bones=merge(flames(2.0), {
    "torso": {"rotation": keys((0, Z), (1, [1.5, 0, 0]), (2, Z))},
    "head": {"rotation": keys((0, Z), (1, [-2, 0, 0]), (2, Z))},
    "right_arm": {"rotation": keys((0, Z), (1, [2, 0, -1.5]), (2, Z))},
    "left_arm": {"rotation": keys((0, Z), (1, [2, 0, 1.5]), (2, Z))},
    "cape": {"rotation": keys((0, [3, 0, 0]), (1, [7, 0, 0]), (2, [3, 0, 0]))},
}))

A["walk"] = dict(loop=True, length=1.2, bones=merge(flames(1.2), {
    "root": {"position": keys((0, Z), (0.3, [0, -0.8, 0]), (0.6, Z), (0.9, [0, -0.8, 0]), (1.2, Z))},
    "right_leg": {"rotation": keys((0, [-25, 0, 0]), (0.6, [25, 0, 0]), (1.2, [-25, 0, 0]))},
    "left_leg": {"rotation": keys((0, [25, 0, 0]), (0.6, [-25, 0, 0]), (1.2, [25, 0, 0]))},
    "right_arm": {"rotation": keys((0, [12, 0, 0]), (0.6, [-12, 0, 0]), (1.2, [12, 0, 0]))},
    "left_arm": {"rotation": keys((0, [-14, 0, 0]), (0.6, [14, 0, 0]), (1.2, [-14, 0, 0]))},
    "torso": {"rotation": keys((0, [4, 3, 0]), (0.6, [4, -3, 0]), (1.2, [4, 3, 0]))},
    "cape": {"rotation": keys((0, [12, 0, 0]), (0.3, [16, 0, 0]), (0.6, [12, 0, 0]), (0.9, [16, 0, 0]), (1.2, [12, 0, 0]))},
}))

# ---------------------------------------------------------------- melee
def sweep(length, windup, hit, follow, name_flames=True):
    return dict(loop=False, length=length, bones=merge(flames(length), {
        "waist": {"rotation": keys((0, Z), (windup, [0, 20, 0]), (hit, [0, -20, 0]), (follow, [0, -25, 0]), (length, Z))},
        "torso": {"rotation": keys((0, Z), (windup, [6, 35, 0]), (hit, [8, -35, 0]), (follow, [8, -45, 0]), (length, Z))},
        "right_arm": {"rotation": keys((0, Z), (windup, [-75, 0, -20]), (hit, [-85, 0, 0]), (follow, [-70, 0, 10]), (length, Z))},
        "sword": {"rotation": keys((0, Z), (windup * 0.8, [60, 0, 0]), (follow, [60, 0, 0]), (length, Z))},
        "left_arm": {"rotation": keys((0, Z), (windup, [-25, 0, 20]), (hit, [-10, 0, 35]), (length, Z))},
        "right_leg": {"rotation": keys((0, Z), (windup, [-12, 0, 0]), (follow, [-12, 0, 0]), (length, Z))},
        "left_leg": {"rotation": keys((0, Z), (windup, [12, 0, 0]), (follow, [12, 0, 0]), (length, Z))},
        "cape": {"rotation": keys((0, [3, 0, 0]), (hit, [25, 0, -10]), (length, [3, 0, 0]))},
    }))

A["attack_sweep"] = sweep(1.5, 0.5, 0.75, 0.95)          # 横なぎ  hit 0.75s
A["attack_wave_h"] = sweep(1.2, 0.4, 0.55, 0.7)          # 横の飛ぶ斬撃  release 0.55s

A["attack_wave_v"] = dict(loop=False, length=1.3, bones=merge(flames(1.3), {   # 縦の飛ぶ斬撃 release 0.65s
    "torso": {"rotation": keys((0, Z), (0.45, [-12, 0, 0]), (0.65, [22, 0, 0]), (0.85, [22, 0, 0]), (1.3, Z))},
    "right_arm": {"rotation": keys((0, Z), (0.45, [-170, 0, 0]), (0.65, [-65, 0, 0]), (0.85, [-60, 0, 0]), (1.3, Z))},
    "left_arm": {"rotation": keys((0, Z), (0.45, [-160, 0, 0]), (0.65, [-60, 0, 0]), (1.3, Z))},
    "sword": {"rotation": keys((0, Z), (0.4, [60, 0, 0]), (0.9, [60, 0, 0]), (1.3, Z))},
    "root": {"position": keys((0, Z), (0.65, [0, -2, 0]), (0.85, [0, -2, 0]), (1.3, Z))},
    "cape": {"rotation": keys((0, [3, 0, 0]), (0.65, [30, 0, 0]), (1.3, [3, 0, 0]))},
}))

A["attack_dash"] = dict(loop=False, length=1.6, bones=merge(flames(1.6, 1.1), {   # 突進斬り dash 0.6-0.9, hit 0.9
    "torso": {"rotation": keys((0, Z), (0.5, [22, 15, 0]), (0.85, [25, 10, 0]), (0.95, [15, -20, 0]), (1.2, [15, -20, 0]), (1.6, Z))},
    "right_arm": {"rotation": keys((0, Z), (0.5, [35, 0, -15]), (0.85, [40, 0, -10]), (0.95, [-100, 0, 0]), (1.2, [-95, 0, 0]), (1.6, Z))},
    "sword": {"rotation": keys((0, Z), (0.5, [60, 0, 0]), (1.2, [60, 0, 0]), (1.6, Z))},
    "left_arm": {"rotation": keys((0, Z), (0.5, [-30, 0, 25]), (0.95, [20, 0, 20]), (1.6, Z))},
    "right_leg": {"rotation": keys((0, Z), (0.5, [20, 0, 0]), (0.95, [-30, 0, 0]), (1.2, [-30, 0, 0]), (1.6, Z))},
    "left_leg": {"rotation": keys((0, Z), (0.5, [-30, 0, 0]), (0.95, [25, 0, 0]), (1.2, [25, 0, 0]), (1.6, Z))},
    "root": {"position": keys((0, Z), (0.5, [0, -2, 0]), (0.95, [0, -1, 0]), (1.6, Z))},
    "cape": {"rotation": keys((0, [3, 0, 0]), (0.6, [10, 0, 0]), (0.9, [45, 0, 0]), (1.6, [3, 0, 0]))},
}))

A["attack_combo"] = dict(loop=False, length=2.4, bones=merge(flames(2.4, 1.15), {   # 3連撃 hits 0.5 / 1.0 / 1.7
    "torso": {"rotation": keys((0, Z), (0.3, [6, 35, 0]), (0.5, [8, -35, 0]), (0.75, [8, -45, 0]), (1.0, [8, 35, 0]),
                               (1.3, [-12, 0, 0]), (1.7, [25, 0, 0]), (2.0, [25, 0, 0]), (2.4, Z))},
    "right_arm": {"rotation": keys((0, Z), (0.3, [-75, 0, -20]), (0.5, [-85, 0, 0]), (0.75, [-80, 0, 15]), (1.0, [-85, 0, -15]),
                                   (1.3, [-170, 0, 0]), (1.7, [-35, 0, 0]), (2.0, [-30, 0, 0]), (2.4, Z))},
    "left_arm": {"rotation": keys((0, Z), (0.5, [-15, 0, 30]), (1.3, [-160, 0, 0]), (1.7, [-35, 0, 0]), (2.4, Z))},
    "sword": {"rotation": keys((0, Z), (0.25, [60, 0, 0]), (2.0, [60, 0, 0]), (2.4, Z))},
    "root": {"position": keys((0, Z), (1.3, Z), (1.7, [0, -2.5, 0]), (2.0, [0, -2.5, 0]), (2.4, Z))},
    "right_leg": {"rotation": keys((0, Z), (0.3, [-12, 0, 0]), (2.0, [-12, 0, 0]), (2.4, Z))},
    "left_leg": {"rotation": keys((0, Z), (0.3, [12, 0, 0]), (2.0, [12, 0, 0]), (2.4, Z))},
    "cape": {"rotation": keys((0, [3, 0, 0]), (0.5, [25, 0, -10]), (1.0, [25, 0, 10]), (1.7, [35, 0, 0]), (2.4, [3, 0, 0]))},
}))

A["attack_leap"] = dict(loop=False, length=2.2, bones=merge(flames(2.2, 1.2), {   # 跳躍叩きつけ jump 0.4, impact 1.2
    "root": {"position": keys((0, Z), (0.35, [0, -3, 0]), (0.45, [0, 1, 0]), (1.15, Z), (1.2, [0, -4, 0]), (1.6, [0, -4, 0]), (2.2, Z))},
    "torso": {"rotation": keys((0, Z), (0.35, [18, 0, 0]), (0.5, [-12, 0, 0]), (1.1, [-15, 0, 0]), (1.2, [30, 0, 0]), (1.6, [30, 0, 0]), (2.2, Z))},
    "right_arm": {"rotation": keys((0, Z), (0.35, [25, 0, 0]), (0.6, [-170, 0, 0]), (1.1, [-175, 0, 0]), (1.2, [-40, 0, 0]), (1.6, [-40, 0, 0]), (2.2, Z))},
    "left_arm": {"rotation": keys((0, Z), (0.35, [25, 0, 0]), (0.6, [-165, 0, 0]), (1.1, [-170, 0, 0]), (1.2, [-40, 0, 0]), (1.6, [-40, 0, 0]), (2.2, Z))},
    "sword": {"rotation": keys((0, Z), (0.5, [60, 0, 0]), (1.1, [60, 0, 0]), (1.2, [20, 0, 0]), (1.6, [20, 0, 0]), (2.2, Z))},
    "right_leg": {"rotation": keys((0, Z), (0.35, [-15, 0, 0]), (0.6, [20, 0, 0]), (1.2, [-20, 0, 0]), (1.6, [-20, 0, 0]), (2.2, Z))},
    "left_leg": {"rotation": keys((0, Z), (0.35, [-15, 0, 0]), (0.6, [30, 0, 0]), (1.2, [15, 0, 0]), (1.6, [15, 0, 0]), (2.2, Z))},
    "cape": {"rotation": keys((0, [3, 0, 0]), (0.6, [-20, 0, 0]), (1.1, [-25, 0, 0]), (1.2, [50, 0, 0]), (2.2, [3, 0, 0]))},
}))

# ---------------------------------------------------------------- magic
A["attack_pillar"] = dict(loop=False, length=1.8, bones=merge(flames(1.8, 1.25), {   # 火柱 sword planted 0.9
    "torso": {"rotation": keys((0, Z), (0.6, [-10, 0, 0]), (0.9, [25, 0, 0]), (1.4, [25, 0, 0]), (1.8, Z))},
    "right_arm": {"rotation": keys((0, Z), (0.6, [-170, 0, 0]), (0.9, [-70, 0, 0]), (1.4, [-70, 0, 0]), (1.8, Z))},
    "left_arm": {"rotation": keys((0, Z), (0.6, [-160, 0, 0]), (0.9, [-70, 0, 0]), (1.4, [-70, 0, 0]), (1.8, Z))},
    "sword": {"rotation": keys((0, Z), (0.6, [60, 0, 0]), (0.9, [85, 0, 0]), (1.4, [85, 0, 0]), (1.8, Z))},
    "root": {"position": keys((0, Z), (0.9, [0, -3, 0]), (1.4, [0, -3, 0]), (1.8, Z))},
    "head": {"rotation": keys((0, Z), (0.6, [-15, 0, 0]), (0.9, [5, 0, 0]), (1.8, Z))},
    "cape": {"rotation": keys((0, [3, 0, 0]), (0.9, [20, 0, 0]), (1.8, [3, 0, 0]))},
}))

A["attack_fireball"] = dict(loop=False, length=1.4, bones=merge(flames(1.4, 1.2), {   # 火の玉 cast 0.8
    "torso": {"rotation": keys((0, Z), (0.5, [-8, 10, 0]), (0.8, [8, -5, 0]), (1.0, [8, -5, 0]), (1.4, Z))},
    "right_arm": {"rotation": keys((0, Z), (0.5, [-165, 0, 0]), (0.8, [-95, 0, 0]), (1.0, [-90, 0, 0]), (1.4, Z))},
    "sword": {"rotation": keys((0, Z), (0.4, [60, 0, 0]), (1.0, [60, 0, 0]), (1.4, Z))},
    "left_arm": {"rotation": keys((0, Z), (0.5, [-20, 0, 25]), (0.8, [-70, 0, 0]), (1.4, Z))},
    "blade_fire": {"scale": keys((0, ONE), (0.5, [1.6, 1.4, 1.6]), (0.8, [2.0, 1.5, 2.0]), (0.9, ONE), (1.4, ONE))},
}))

# ---------------------------------------------------------------- phase / death
A["phase_change"] = dict(loop=False, length=2.5, bones=merge({
    "torso": {"rotation": keys((0, Z), (0.6, [25, 0, 0]), (1.0, [-20, 0, 0]), (2.0, [-20, 0, 0]), (2.5, Z))},
    "head": {"rotation": keys((0, Z), (0.6, [20, 0, 0]), (1.0, [-25, 0, 0]), (2.0, [-25, 0, 0]), (2.5, Z))},
    "right_arm": {"rotation": keys((0, Z), (0.6, [10, 0, 10]), (1.0, [-30, 0, -70]), (2.0, [-30, 0, -70]), (2.5, Z))},
    "left_arm": {"rotation": keys((0, Z), (0.6, [10, 0, -10]), (1.0, [-30, 0, 70]), (2.0, [-30, 0, 70]), (2.5, Z))},
    "root": {"position": keys((0, Z), (0.6, [0, -3, 0]), (1.0, Z), (2.5, Z))},
    "cape": {"rotation": keys((0, [3, 0, 0]), (1.0, [55, 0, 0]), (2.0, [45, 0, 0]), (2.5, [3, 0, 0]))},
}, {n: {"scale": keys((0, ONE), (0.6, [0.6, 0.5, 0.6]), (1.0, [1.8, 2.2, 1.8]), (1.5, [1.6, 1.9, 1.6]),
                      (2.0, [1.8, 2.2, 1.8]), (2.5, [1.3, 1.4, 1.3]))}
    for n in ["plume", "right_flame", "left_flame", "cape_fire", "blade_fire"]}))

A["death"] = dict(loop="hold_on_last_frame", length=3.0, bones=merge(flames(3.0, out=True), {
    "torso": {"rotation": keys((0, Z), (0.5, [-15, 10, 0]), (1.4, [30, 0, 0]), (3.0, [38, 0, 0]))},
    "head": {"rotation": keys((0, Z), (0.5, [-20, 0, 0]), (1.4, [25, 0, 0]), (3.0, [35, 0, 0]))},
    "right_arm": {"rotation": keys((0, Z), (0.5, [10, 0, -20]), (1.4, [-55, 0, 0]), (3.0, [-55, 0, 0]))},
    "sword": {"rotation": keys((0, Z), (1.4, [70, 0, 0]), (3.0, [70, 0, 0]))},
    "left_arm": {"rotation": keys((0, Z), (0.5, [0, 0, 25]), (1.4, [-20, 0, 10]), (3.0, [5, 0, 5]))},
    "root": {"position": keys((0, Z), (0.5, [0, -1, 0]), (1.4, [0, -7, 0]), (3.0, [0, -8, 0]))},
    "right_leg": {"rotation": keys((0, Z), (1.4, [-70, 0, 0]), (3.0, [-75, 0, 0]))},
    "left_leg": {"rotation": keys((0, Z), (1.4, [60, 0, 0]), (3.0, [65, 0, 0]))},
    "cape": {"rotation": keys((0, [3, 0, 0]), (1.4, [-10, 0, 0]), (3.0, [0, 0, 0]))},
}))

def to_json():
    anims = {}
    for name, a in A.items():
        anims[f"animation.fire_knight.{name}"] = {"loop": a["loop"], "animation_length": a["length"], "bones": a["bones"]}
    return {"format_version": "1.8.0", "animations": anims}

if __name__ == "__main__":
    os.makedirs(OUT, exist_ok=True)
    with open(f"{OUT}/fire_knight.animation.json", "w") as f:
        json.dump(to_json(), f, indent=1)
    print(len(A), "animations")
