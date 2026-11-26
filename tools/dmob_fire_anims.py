"""ダンジョンモブ 炎のセットのアニメ。 python3 tools/dmob_fire_anims.py
腕・脚：X がマイナスで前に上がる。Z は右腕マイナス／左腕プラスで外に開く。"""
import json, os

OUT = "src/main/resources/assets/jsnboss/animations/entity"
Z = [0, 0, 0]


def keys(*pairs):
    return {f"{float(t)}": v for t, v in pairs}


def merge(*parts):
    out = {}
    for p in parts:
        for bone, ch in p.items():
            out.setdefault(bone, {}).update(ch)
    return out


def flicker(bones, length, amp=0.18):
    res = {}
    for k, b in enumerate(bones):
        ks, steps = {}, max(4, round(length / 0.2))
        for i in range(steps + 1):
            t = round(length * i / steps, 3)
            w = [1.0, 1 + amp, 1 - amp / 2, 1 + amp * 0.7, 1 - amp / 3][(i + k * 2) % 5]
            ks[f"{t}"] = [round(1 + (w - 1) * 0.4, 3), round(w, 3), round(1 + (w - 1) * 0.4, 3)]
        ks[f"{round(length, 3)}"] = ks["0.0"]
        res[b] = {"scale": ks}
    return res


def walk(length, legs=("right_leg", "left_leg"), arms=("right_arm", "left_arm"), leg=25, arm=14, bob=0.6):
    h = length / 2
    out = {"root": {"position": keys((0, Z), (h / 2, [0, -bob, 0]), (h, Z), (h * 1.5, [0, -bob, 0]), (length, Z))}}
    if legs:
        out[legs[0]] = {"rotation": keys((0, [-leg, 0, 0]), (h, [leg, 0, 0]), (length, [-leg, 0, 0]))}
        out[legs[1]] = {"rotation": keys((0, [leg, 0, 0]), (h, [-leg, 0, 0]), (length, [leg, 0, 0]))}
    if arms:
        out[arms[0]] = {"rotation": keys((0, [arm, 0, 0]), (h, [-arm, 0, 0]), (length, [arm, 0, 0]))}
        out[arms[1]] = {"rotation": keys((0, [-arm, 0, 0]), (h, [arm, 0, 0]), (length, [-arm, 0, 0]))}
    return out


def collapse(length=1.0, sink=-6, tilt=80):
    """倒れて沈む死亡アニメ"""
    return {"root": {"rotation": keys((0, Z), (length * 0.5, [tilt * 0.3, 0, 0]), (length, [tilt, 0, 0])),
                     "position": keys((0, Z), (length, [0, sink, 0]))}}


def shrink(bone, length=1.0):
    return {bone: {"scale": keys((0, [1, 1, 1]), (length * 0.6, [1.15, 1.15, 1.15]), (length, [0.01, 0.01, 0.01]))}}


ALL = {}


def anim(model, name, length, bones, loop=False):
    ALL.setdefault(model, {})[f"animation.{model}.{name}"] = {
        "loop": "hold_on_last_frame" if name == "death" else loop, "animation_length": length, "bones": bones}


# ------------------------------------------------------------------ 炎の従騎士
M = "flame_squire"
FL = flicker(["plume", "cape"], 2.0, 0.05)
anim(M, "idle", 2.0, merge({"body": {"rotation": keys((0, Z), (1, [1.5, 0, 0]), (2, Z))},
                            "cape": {"rotation": keys((0, [8, 0, 0]), (1, [12, 0, 0]), (2, [8, 0, 0]))},
                            "shield": {"rotation": keys((0, Z), (1, [0, 0, 2]), (2, Z))}}), loop=True)
anim(M, "walk", 1.0, merge(walk(1.0, arm=10), {"cape": {"rotation": keys((0, [14, 0, 0]), (0.5, [20, 0, 0]), (1, [14, 0, 0]))}}), loop=True)
anim(M, "attack_slash", 1.0, {
    "right_arm": {"rotation": keys((0, Z), (0.4, [-110, 30, -20]), (0.55, [-40, -40, 10]), (0.8, [-20, -30, 0]), (1.0, Z))},
    "body": {"rotation": keys((0, Z), (0.4, [0, 25, 0]), (0.55, [8, -30, 0]), (1.0, Z))}})
anim(M, "attack_bash", 1.6, {
    "left_arm": {"rotation": keys((0, Z), (0.4, [-70, -20, 10]), (1.2, [-70, -20, 10]), (1.6, Z))},
    "body": {"rotation": keys((0, Z), (0.5, [18, 0, 0]), (1.2, [18, 0, 0]), (1.6, Z))},
    "right_leg": {"rotation": keys((0, Z), (0.5, [30, 0, 0]), (0.7, [-30, 0, 0]), (0.9, [30, 0, 0]), (1.1, [-30, 0, 0]), (1.6, Z))},
    "left_leg": {"rotation": keys((0, Z), (0.5, [-30, 0, 0]), (0.7, [30, 0, 0]), (0.9, [-30, 0, 0]), (1.1, [30, 0, 0]), (1.6, Z))}})
anim(M, "attack_ring", 1.7, {
    "right_arm": {"rotation": keys((0, Z), (0.5, [-170, 0, 10]), (0.75, [-30, 0, 0]), (1.7, Z))},
    "sword": {"rotation": keys((0, Z), (0.5, [60, 0, 0]), (0.75, [90, 0, 0]), (1.4, [90, 0, 0]), (1.7, Z))},
    "body": {"rotation": keys((0, Z), (0.5, [-10, 0, 0]), (0.75, [25, 0, 0]), (1.4, [25, 0, 0]), (1.7, Z))},
    "root": {"position": keys((0, Z), (0.75, [0, -2, 0]), (1.4, [0, -2, 0]), (1.7, Z))}})
anim(M, "attack_wave", 1.2, {
    "right_arm": {"rotation": keys((0, Z), (0.5, [-60, 60, -60]), (0.65, [-80, -50, 20]), (1.2, Z))},
    "body": {"rotation": keys((0, Z), (0.5, [0, 35, 0]), (0.65, [5, -35, 0]), (1.2, Z))}})
anim(M, "death", 2.0, merge(collapse(1.4, -4, 85), {"right_arm": {"rotation": keys((0, Z), (1.0, [-40, 0, -20]))}}))

# ------------------------------------------------------------------ 溶岩の甲冑兵
M = "lava_armor"
anim(M, "idle", 2.4, {"body": {"rotation": keys((0, Z), (1.2, [2, 0, 0]), (2.4, Z))},
                      "head": {"rotation": keys((0, Z), (1.2, [0, 6, 0]), (2.4, Z))},
                      "right_arm": {"rotation": keys((0, Z), (1.2, [0, 0, -3]), (2.4, Z))},
                      "left_arm": {"rotation": keys((0, Z), (1.2, [0, 0, 3]), (2.4, Z))}}, loop=True)
anim(M, "walk", 1.4, walk(1.4, leg=20, arm=10, bob=1.0), loop=True)
anim(M, "attack_punch", 1.0, {
    "right_arm": {"rotation": keys((0, Z), (0.35, [-20, 0, -30]), (0.5, [-95, 0, 0]), (0.8, [-80, 0, 0]), (1.0, Z))},
    "body": {"rotation": keys((0, Z), (0.35, [0, 30, 0]), (0.5, [10, -20, 0]), (1.0, Z))}})
anim(M, "attack_charge", 2.2, {
    "body": {"rotation": keys((0, Z), (0.6, [-10, 0, 0]), (0.7, [30, 0, 0]), (1.7, [30, 0, 0]), (2.2, Z))},
    "right_arm": {"rotation": keys((0, Z), (0.6, [20, 0, -20]), (0.7, [-40, 0, 10]), (1.7, [-40, 0, 10]), (2.2, Z))},
    "left_arm": {"rotation": keys((0, Z), (0.6, [20, 0, 20]), (0.7, [-40, 0, -10]), (1.7, [-40, 0, -10]), (2.2, Z))},
    "right_leg": {"rotation": keys((0.7, Z), (0.9, [-35, 0, 0]), (1.1, [35, 0, 0]), (1.3, [-35, 0, 0]), (1.5, [35, 0, 0]), (1.7, Z))},
    "left_leg": {"rotation": keys((0.7, Z), (0.9, [35, 0, 0]), (1.1, [-35, 0, 0]), (1.3, [35, 0, 0]), (1.5, [-35, 0, 0]), (1.7, Z))}})
anim(M, "death", 1.6, merge(collapse(1.2, -8, 30), {
    "head": {"position": keys((0, Z), (0.6, [0, 2, 0]), (1.2, [3, -14, -4])), "rotation": keys((0, Z), (1.2, [60, 30, 40]))},
    "right_arm": {"rotation": keys((0, Z), (1.0, [0, 0, -50]))}, "left_arm": {"rotation": keys((0, Z), (1.0, [0, 0, 50]))}}))

# ------------------------------------------------------------------ 炎の魔眼
M = "flame_eye"
SPIN = {"ring": {"rotation": keys((0, Z), (1, [0, 0, 90]), (2, [0, 0, 180]), (3, [0, 0, 270]), (4, [0, 0, 360]))}}
BOB = {"eye": {"position": keys((0, Z), (1, [0, 1.2, 0]), (2, Z), (3, [0, 1.2, 0]), (4, Z))}}
TAIL = flicker(["tail"], 4.0, 0.25)
anim(M, "idle", 4.0, merge(SPIN, BOB, TAIL), loop=True)
anim(M, "attack_volley", 1.5, {
    "eye": {"scale": keys((0, [1, 1, 1]), (0.4, [1.12, 0.9, 1.12]), (0.5, [0.95, 1.08, 0.95]), (0.75, [1.1, 0.92, 1.1]),
                          (1.0, [0.95, 1.06, 0.95]), (1.5, [1, 1, 1]))},
    "ring": {"rotation": keys((0, Z), (0.5, [0, 0, 240]), (1.0, [0, 0, 480]), (1.5, [0, 0, 720]))}})
anim(M, "attack_glare", 2.2, merge(TAIL, {
    "eye": {"scale": keys((0, [1, 1, 1]), (0.5, [1.2, 1.2, 1.2]), (1.9, [1.2, 1.2, 1.2]), (2.2, [1, 1, 1])),
            "rotation": keys((0, Z), (0.5, [-8, 0, 0]), (2.2, Z))},
    "ring": {"scale": keys((0, [1, 1, 1]), (0.5, [1.3, 1.3, 1.3]), (1.9, [1.3, 1.3, 1.3]), (2.2, [1, 1, 1])),
             "rotation": keys((0, Z), (2.2, [0, 0, 720]))}}))
anim(M, "death", 1.2, merge(shrink("ring", 0.6), {"eye": {"rotation": keys((0, Z), (1.2, [0, 0, 120])),
                                                          "position": keys((0, Z), (1.2, [0, -10, 0]))}}, shrink("root", 1.2)))

# ------------------------------------------------------------------ 燃える小鬼
M = "ember_imp"
anim(M, "idle", 1.2, merge(flicker(["head"], 1.2, 0.0), {
    "body": {"rotation": keys((0, Z), (0.6, [4, 0, 0]), (1.2, Z))},
    "tail": {"rotation": keys((0, [-30, 0, 0]), (0.6, [-30, 25, 0]), (1.2, [-30, 0, 0]))},
    "head": {"rotation": keys((0, Z), (0.3, [0, 0, 6]), (0.9, [0, 0, -6]), (1.2, Z))}}), loop=True)
anim(M, "walk", 0.5, merge(walk(0.5, leg=40, arm=40, bob=0.8), {"body": {"rotation": keys((0, [15, 0, 0]), (0.5, [15, 0, 0]))},
                                                                "tail": {"rotation": keys((0, [-30, 20, 0]), (0.25, [-30, -20, 0]), (0.5, [-30, 20, 0]))}}), loop=True)
anim(M, "attack_scratch", 0.6, {
    "right_arm": {"rotation": keys((0, Z), (0.2, [-150, 0, -20]), (0.35, [-20, 0, 10]), (0.6, Z))},
    "left_arm": {"rotation": keys((0, Z), (0.25, [-150, 0, 20]), (0.4, [-20, 0, -10]), (0.6, Z))},
    "body": {"rotation": keys((0, Z), (0.2, [-10, 0, 0]), (0.35, [25, 0, 0]), (0.6, Z))}})
anim(M, "death", 1.0, merge(collapse(0.6, -2, -90), shrink("root", 1.0)))

# ------------------------------------------------------------------ 火の粉コウモリ
M = "cinder_bat"
FLAP = {"right_wing": {"rotation": keys((0, [0, 0, 30]), (0.15, [0, 0, -35]), (0.3, [0, 0, 30]))},
        "left_wing": {"rotation": keys((0, [0, 0, -30]), (0.15, [0, 0, 35]), (0.3, [0, 0, -30]))},
        "body": {"position": keys((0, Z), (0.15, [0, 1, 0]), (0.3, Z))}}
anim(M, "idle", 0.3, FLAP, loop=True)
anim(M, "attack_bite", 0.5, merge(FLAP, {"body": {"rotation": keys((0, Z), (0.15, [-30, 0, 0]), (0.3, [40, 0, 0]), (0.5, Z))}}))
anim(M, "death", 1.0, merge({"right_wing": {"rotation": keys((0, Z), (0.6, [0, 0, 60]))},
                             "left_wing": {"rotation": keys((0, Z), (0.6, [0, 0, -60]))},
                             "root": {"rotation": keys((0, Z), (1.0, [180, 0, 0])), "position": keys((0, Z), (1.0, [0, -8, 0]))}},
                            shrink("body", 1.0)))

if __name__ == "__main__":
    os.makedirs(OUT, exist_ok=True)
    for model, anims in ALL.items():
        with open(f"{OUT}/{model}.animation.json", "w") as f:
            json.dump({"format_version": "1.8.0", "animations": anims}, f, indent=1)
        print(model, len(anims))
