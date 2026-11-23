"""堕ちた戦乙女のアニメーション。 python3 tools/fallen_valkyrie_anims.py
槍 spear は右の前腕の子。腕を前へ上げる（X マイナス）と槍の穂先は後ろを向くので、
突くときは spear を X -180 回して穂先を前へ向ける。翼は wing_* の Z で上下に羽ばたき、Y で開く。
マントのなびきは別のコントローラーの "cape" がずっと動かす（ほかのアニメでは cape* を触らない）。
"""
import json, math, os

OUT = "src/main/resources/assets/jsnboss/animations/entity"
Z = [0, 0, 0]


def keys(*pairs):
    return {f"{round(float(t), 3)}": v for t, v in pairs}


def merge(*parts):
    out = {}
    for p in parts:
        for b, ch in p.items():
            out.setdefault(b, {}).update(ch)
    return out


def wings(*pairs):
    """pairs = [(t, flap, spread)]。flap>0 で翼を振り上げ、spread>0 で横へ大きく開く"""
    return {"wing_right": {"rotation": keys(*[(t, [0, -sp, -fl]) for t, fl, sp in pairs])},
            "wing_left": {"rotation": keys(*[(t, [0, sp, fl]) for t, fl, sp in pairs])}}


def flap(length, n=1, amp=35, spread=20):
    ks = []
    for i in range(n * 4 + 1):
        t = length * i / (n * 4)
        ph = (i % 4)
        fl = (amp, 0, -amp * 0.8, 0)[ph]
        ks.append((t, fl, spread))
    return wings(*ks)


def legs(r, l=None):
    l = l or r
    return {"right_leg": {"rotation": keys(*r)}, "left_leg": {"rotation": keys(*l)}}


A = {}

# マントのなびき（ずっと）
A["cape"] = dict(loop=True, length=2.0, bones={
    **{f"cape{i}": {"rotation": keys((0, [3 * (i % 2), 0, 0]), (1.0, [8 - 3 * (i % 2), 0, 0]), (2.0, [3 * (i % 2), 0, 0]))} for i in range(3)},
    **{f"cape{i}_low": {"rotation": keys((0, [6, 0, 0]), (0.5 + i * 0.2, [14, 0, 2]), (1.5, [4, 0, -2]), (2.0, [6, 0, 0]))} for i in range(3)},
})

A["idle"] = dict(loop=True, length=3.0, bones=merge({
    "torso": {"rotation": keys((0, Z), (1.5, [-2, 0, 0]), (3.0, Z))},
    "head": {"rotation": keys((0, Z), (1.0, [0, 8, 0]), (2.0, [0, -8, 0]), (3.0, Z))},
}, wings((0, 0, 0), (1.5, 6, 4), (3.0, 0, 0))))

A["walk"] = dict(loop=True, length=1.2, bones=merge(legs([(0, [-20, 0, 0]), (0.6, [20, 0, 0]), (1.2, [-20, 0, 0])],
                                                          [(0, [20, 0, 0]), (0.6, [-20, 0, 0]), (1.2, [20, 0, 0])]), {
    "right_shin": {"rotation": keys((0, [10, 0, 0]), (0.3, [25, 0, 0]), (0.6, Z), (1.2, [10, 0, 0]))},
    "left_shin": {"rotation": keys((0, Z), (0.6, [10, 0, 0]), (0.9, [25, 0, 0]), (1.2, Z))},
    "left_arm": {"rotation": keys((0, [-15, 0, 0]), (0.6, [15, 0, 0]), (1.2, [-15, 0, 0]))},
    "root": {"position": keys((0, Z), (0.3, [0, 0.8, 0]), (0.6, Z), (0.9, [0, 0.8, 0]), (1.2, Z))},
}, wings((0, 0, 0), (0.6, 4, 0), (1.2, 0, 0))))

# 空中に浮かんでいる間（ゆっくり羽ばたき、脚は少したれる）
A["hover"] = dict(loop=True, length=1.2, bones=merge(flap(1.2, 1, 30, 25), legs([(0, [10, 0, 5]), (0.6, [14, 0, 5]), (1.2, [10, 0, 5])],
                                                                                  [(0, [16, 0, -5]), (0.6, [12, 0, -5]), (1.2, [16, 0, -5])]), {
    "right_shin": {"rotation": keys((0, [20, 0, 0]), (1.2, [20, 0, 0]))},
    "left_shin": {"rotation": keys((0, [28, 0, 0]), (1.2, [28, 0, 0]))},
    "root": {"position": keys((0, Z), (0.3, [0, -1.5, 0]), (0.9, [0, 1.5, 0]), (1.2, Z))},
    "torso": {"rotation": keys((0, [6, 0, 0]), (1.2, [6, 0, 0]))},
}))

# 槍の連突き：0.45 / 0.85 / 1.25 秒
def thrusts(times):
    arm, spear, tor = [(0, Z)], [(0, Z)], [(0, Z)]
    for t in times:
        arm += [(t - 0.2, [-70, 0, 10]), (t, [-92, 0, 0]), (t + 0.12, [-80, 0, 5])]
        spear += [(t - 0.2, [-180, 0, 0]), (t, [-180, 0, 0]), (t + 0.12, [-180, 0, 0])]
        tor += [(t - 0.2, [0, 25, 0]), (t, [8, -15, 0]), (t + 0.12, [5, -5, 0])]
    end = times[-1] + 0.45
    arm.append((end, Z)); spear.append((end, Z)); tor.append((end, Z))
    return {"right_arm": {"rotation": keys(*arm)}, "spear": {"rotation": keys(*spear)}, "torso": {"rotation": keys(*tor)},
            "right_forearm": {"rotation": keys((0, Z), (times[0] - 0.2, [-20, 0, 0]), (times[-1] + 0.12, [0, 0, 0]), (end, Z))}}


A["thrust"] = dict(loop=False, length=1.7, bones=merge(thrusts([0.45, 0.85, 1.25]),
                                                        legs([(0, Z), (0.3, [-25, 0, 0]), (1.3, [-25, 0, 0]), (1.7, Z)],
                                                             [(0, Z), (0.3, [15, 0, 0]), (1.3, [15, 0, 0]), (1.7, Z)])))

# なぎ払い：槍を横に構えて、0.7 秒に体ごと一回転
A["sweep"] = dict(loop=False, length=1.6, bones=merge({
    "right_arm": {"rotation": keys((0, Z), (0.45, [-80, 0, -60]), (0.7, [-85, 0, -70]), (1.1, [-80, 0, -60]), (1.6, Z))},
    "spear": {"rotation": keys((0, Z), (0.45, [-90, 0, 0]), (1.1, [-90, 0, 0]), (1.6, Z))},
    "root": {"rotation": keys((0, Z), (0.45, [0, 40, 0]), (0.7, [0, -120, 0]), (0.95, [0, -320, 0]), (0.96, [0, 40, 0]), (1.6, Z))},
}, wings((0, 0, 0), (0.45, 10, 30), (1.1, 10, 30), (1.6, 0, 0))))

# 飛翔：しゃがんで 0.5 秒に跳び上がり、羽ばたいて上昇
A["takeoff"] = dict(loop=False, length=1.5, bones=merge(flap(1.0, 2, 45, 30), legs([(0, Z), (0.4, [-40, 0, 0]), (0.5, [20, 0, 0]), (1.5, [12, 0, 5])],
                                                                                    [(0, Z), (0.4, [-40, 0, 0]), (0.5, [20, 0, 0]), (1.5, [16, 0, -5])]), {
    "root": {"position": keys((0, Z), (0.4, [0, -6, 0]), (0.5, [0, 2, 0]), (1.5, Z))},
    "right_shin": {"rotation": keys((0, Z), (0.4, [70, 0, 0]), (0.5, [10, 0, 0]), (1.5, [20, 0, 0]))},
    "left_shin": {"rotation": keys((0, Z), (0.4, [70, 0, 0]), (0.5, [10, 0, 0]), (1.5, [28, 0, 0]))},
}))

# 急降下突き：空中で槍を下に構え、0.6〜1.1 秒に突っ込み、着地
A["dive"] = dict(loop=False, length=1.8, bones=merge(wings((0, 30, 25), (0.5, 40, 30), (0.6, -60, -10), (1.1, -60, -10), (1.3, 10, 20), (1.8, 0, 0)), {
    "right_arm": {"rotation": keys((0, Z), (0.5, [-160, 0, 0]), (0.6, [-110, 0, 0]), (1.3, [-110, 0, 0]), (1.8, Z))},
    "spear": {"rotation": keys((0, Z), (0.5, [-180, 0, 0]), (1.3, [-180, 0, 0]), (1.8, Z))},
    "torso": {"rotation": keys((0, Z), (0.5, [-15, 0, 0]), (0.6, [35, 0, 0]), (1.3, [30, 0, 0]), (1.8, Z))},
    "root": {"position": keys((0, Z), (1.1, Z), (1.2, [0, -4, 0]), (1.8, Z))},
}, legs([(0, Z), (0.6, [30, 0, 0]), (1.1, [30, 0, 0]), (1.2, [-40, 0, 0]), (1.8, Z)],
        [(0, Z), (0.6, [40, 0, 0]), (1.1, [40, 0, 0]), (1.2, [20, 0, 0]), (1.8, Z)])))

# 地より出ずる槍：槍を逆手に持ち上げ、0.7 秒に地面へ突き立てる
A["ground_spears"] = dict(loop=False, length=1.8, bones=merge({
    "right_arm": {"rotation": keys((0, Z), (0.5, [-170, 0, 10]), (0.7, [-60, 0, 0]), (1.3, [-60, 0, 0]), (1.8, Z))},
    "left_arm": {"rotation": keys((0, Z), (0.5, [-150, 0, -10]), (0.7, [-60, 0, 0]), (1.3, [-60, 0, 0]), (1.8, Z))},
    "spear": {"rotation": keys((0, Z), (0.5, [-180, 0, 0]), (1.3, [-180, 0, 0]), (1.8, Z))},
    "torso": {"rotation": keys((0, Z), (0.5, [-12, 0, 0]), (0.7, [28, 0, 0]), (1.3, [28, 0, 0]), (1.8, Z))},
    "root": {"position": keys((0, Z), (0.7, [0, -5, 0]), (1.3, [0, -5, 0]), (1.8, Z))},
}, legs([(0, Z), (0.7, [-50, 0, 0]), (1.3, [-50, 0, 0]), (1.8, Z)], [(0, Z), (0.7, [30, 0, 0]), (1.3, [30, 0, 0]), (1.8, Z)]),
   wings((0, 0, 0), (0.5, 30, 30), (0.7, -10, 30), (1.3, -10, 30), (1.8, 0, 0))))

# 槍の雨（空中）：左手を天にかざし、0.8 秒に振り下ろす
A["spear_rain"] = dict(loop=False, length=1.8, bones=merge(flap(1.8, 2, 30, 25), {
    "left_arm": {"rotation": keys((0, Z), (0.5, [-175, 0, -10]), (0.8, [-60, 0, 0]), (1.4, [-50, 0, 0]), (1.8, Z))},
    "head": {"rotation": keys((0, Z), (0.5, [-25, 0, 0]), (0.8, [15, 0, 0]), (1.8, Z))},
}))

# 羽根の刃：翼を大きく前へ振り抜く（0.7 秒）
A["feathers"] = dict(loop=False, length=1.4, bones=merge(wings((0, 0, 0), (0.5, 40, 40), (0.7, -30, -30), (1.0, -20, -20), (1.4, 0, 0)), {
    "torso": {"rotation": keys((0, Z), (0.5, [-15, 0, 0]), (0.7, [12, 0, 0]), (1.4, Z))},
    "right_arm": {"rotation": keys((0, Z), (0.5, [-20, 0, -30]), (0.7, [-10, 0, -10]), (1.4, Z))},
}))

# 槍の牢獄（第2形態）：槍を天へ掲げ、1.0 秒に石突きで地面を打つ
A["prison"] = dict(loop=False, length=2.0, bones=merge({
    "right_arm": {"rotation": keys((0, Z), (0.7, [-175, 0, 0]), (1.0, [-20, 0, 0]), (1.6, [-20, 0, 0]), (2.0, Z))},
    "head": {"rotation": keys((0, Z), (0.7, [-25, 0, 0]), (1.0, [10, 0, 0]), (2.0, Z))},
    "root": {"position": keys((0, Z), (0.7, [0, 3, 0]), (1.0, [0, -2, 0]), (1.4, Z))},
}, wings((0, 0, 0), (0.7, 45, 40), (1.0, -15, 40), (1.6, -15, 40), (2.0, 0, 0))))

A["phase_change"] = dict(loop=False, length=3.0, bones=merge({
    "root": {"position": keys((0, Z), (0.8, [0, -4, 0]), (1.5, [0, 8, 0]), (2.4, [0, 8, 0]), (3.0, Z))},
    "torso": {"rotation": keys((0, Z), (0.8, [30, 0, 0]), (1.5, [-25, 0, 0]), (2.4, [-25, 0, 0]), (3.0, Z))},
    "head": {"rotation": keys((0, Z), (0.8, [20, 0, 0]), (1.5, [-30, 0, 0]), (2.4, [-30, 0, 0]), (3.0, Z))},
    "right_arm": {"rotation": keys((0, Z), (0.8, [10, 0, 20]), (1.5, [-30, 0, -70]), (2.4, [-30, 0, -70]), (3.0, Z))},
    "left_arm": {"rotation": keys((0, Z), (0.8, [10, 0, -20]), (1.5, [-30, 0, 70]), (2.4, [-30, 0, 70]), (3.0, Z))},
}, wings((0, 0, 0), (0.8, -40, -20), (1.5, 50, 45), (2.4, 50, 45), (3.0, 0, 0)),
   legs([(0, Z), (0.8, [-30, 0, 0]), (1.5, [10, 0, 0]), (3.0, Z)])))

# 倒れる：ひざをつき、槍を地面に突き立てたまま、翼がだらりと垂れる
A["death"] = dict(loop="hold_on_last_frame", length=3.0, bones=merge({
    "root": {"position": keys((0, Z), (1.0, [0, -10, 0]), (3.0, [0, -10, 0]))},
    "right_leg": {"rotation": keys((0, Z), (1.0, [-80, 0, 0]), (3.0, [-80, 0, 0]))},
    "right_shin": {"rotation": keys((0, Z), (1.0, [80, 0, 0]), (3.0, [80, 0, 0]))},
    "left_leg": {"rotation": keys((0, Z), (1.0, [5, 0, 0]), (3.0, [5, 0, 0]))},
    "left_shin": {"rotation": keys((0, Z), (1.0, [85, 0, 0]), (3.0, [85, 0, 0]))},
    "torso": {"rotation": keys((0, Z), (1.0, [20, 0, 0]), (2.0, [30, 0, 0]), (3.0, [30, 0, 0]))},
    "head": {"rotation": keys((0, Z), (2.0, [35, 0, 0]), (3.0, [35, 0, 0]))},
    "right_arm": {"rotation": keys((0, Z), (1.0, [-50, 0, 0]), (3.0, [-50, 0, 0]))},
    "spear": {"rotation": keys((0, Z), (1.0, [-180, 0, 0]), (3.0, [-180, 0, 0]))},
}, wings((0, 0, 0), (1.0, -20, 0), (2.0, -60, -15), (3.0, -60, -15))))


def to_json():
    return {"format_version": "1.8.0", "animations": {
        f"animation.fallen_valkyrie.{n}": {"loop": a["loop"], "animation_length": a["length"], "bones": a["bones"]}
        for n, a in A.items()}}


if __name__ == "__main__":
    os.makedirs(OUT, exist_ok=True)
    with open(f"{OUT}/fallen_valkyrie.animation.json", "w") as f:
        json.dump(to_json(), f, indent=1)
    print(len(A), "animations")
