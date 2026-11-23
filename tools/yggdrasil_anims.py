"""ユグドラシルのアニメーション。 python3 tools/yggdrasil_anims.py
大枝の腕：arm_* の Z で上下（右は Z マイナスで振り上げ、左は逆）、forearm_* の X で前後に振る。
根：root*_tip の X マイナスで先が持ち上がる。
"""
import json, math, os

OUT = "src/main/resources/assets/jsnboss/animations/entity"
Z = [0, 0, 0]
ONE = [1, 1, 1]


def keys(*pairs):
    return {f"{round(float(t), 3)}": v for t, v in pairs}


def merge(*parts):
    out = {}
    for p in parts:
        for b, ch in p.items():
            out.setdefault(b, {}).update(ch)
    return out


def arms(right, left=None):
    """right = [(t, raise, swing)]。raise>0 で腕を振り上げ、swing>0 で前へ振り出す"""
    left = left or right
    return {"arm_right": {"rotation": keys(*[(t, [0, 0, -r]) for t, r, s in right])},
            "forearm_right": {"rotation": keys(*[(t, [-s, 0, 0]) for t, r, s in right])},
            "arm_left": {"rotation": keys(*[(t, [0, 0, r]) for t, r, s in left])},
            "forearm_left": {"rotation": keys(*[(t, [-s, 0, 0]) for t, r, s in left])}}


def roots_wave(t0, step, up=25, hold=0.25):
    out = {}
    for i in range(8):
        t = t0 + i * step
        out[f"root{i}_tip"] = {"rotation": keys((0, Z), (t, Z), (t + 0.1, [-up, 0, 0]), (t + 0.1 + hold, [-up, 0, 0]), (t + 0.3 + hold, Z))}
    return out


def canopy_shake(t0, t1, amp=3):
    ks, t, i = [(0, Z), (t0, Z)], t0, 0
    while t < t1:
        t += 0.1; i += 1
        ks.append((t, [amp if i % 2 else -amp, 0, (amp if i % 3 else -amp) * 0.7]))
    ks.append((t1 + 0.2, Z))
    return {"canopy": {"rotation": keys(*ks)}}


A = {}

A["idle"] = dict(loop=True, length=4.0, bones=merge({
    "canopy": {"rotation": keys((0, Z), (2.0, [1.5, 0, 1.5]), (4.0, Z))},
    "vines": {"rotation": keys((0, Z), (1.0, [2, 0, -2]), (3.0, [-2, 0, 2]), (4.0, Z))},
    "face": {"scale": keys((0, ONE), (2.0, [1.02, 1.02, 1.02]), (4.0, ONE))},
}, arms([(0, 0, 0), (2.0, 4, 4), (4.0, 0, 0)], [(0, 0, 0), (2.0, -3, 6), (4.0, 0, 0)])))

# 枝の叩きつけ：両腕を振り上げ、0.9 秒に前へ叩きつける
A["slam"] = dict(loop=False, length=2.0, bones=merge(arms([(0, 0, 0), (0.7, 35, 50), (0.9, -5, 85), (1.5, -5, 85), (2.0, 0, 0)]), {
    "trunk": {"rotation": keys((0, Z), (0.7, [-5, 0, 0]), (0.9, [6, 0, 0]), (1.5, [6, 0, 0]), (2.0, Z))},
}, canopy_shake(0.9, 1.3, 3)))

# 根の突き上げ：両腕の爪を地面へ突き刺す（0.8 秒）
A["root_spikes"] = dict(loop=False, length=1.8, bones=merge(arms([(0, 0, 0), (0.5, 25, 20), (0.8, -20, 30), (1.4, -20, 30), (1.8, 0, 0)]), {
    "hand_right": {"rotation": keys((0, Z), (0.8, [0, 0, -20]), (1.4, [0, 0, -20]), (1.8, Z))},
    "hand_left": {"rotation": keys((0, Z), (0.8, [0, 0, 20]), (1.4, [0, 0, 20]), (1.8, Z))},
    "face": {"scale": keys((0, ONE), (0.8, [1.06, 1.06, 1.06]), (1.4, ONE))},
}))

# 根のなぎ払い：根の先が順に持ち上がって地面を払う（0.6 秒から）
A["root_sweep"] = dict(loop=False, length=1.8, bones=merge(roots_wave(0.5, 0.06, 30), {
    "trunk": {"rotation": keys((0, Z), (0.4, [0, -6, 0]), (0.9, [0, 6, 0]), (1.8, Z))},
}))

# 毒の花粉：冠を大きく揺らす（0.8 秒〜）
A["pollen"] = dict(loop=False, length=1.8, bones=merge(canopy_shake(0.4, 1.4, 5), arms([(0, 0, 0), (0.5, 20, -10), (1.4, 20, -10), (1.8, 0, 0)])))

# 種の爆弾：腕で冠をはたいて種を飛ばす（0.8 秒）
A["seeds"] = dict(loop=False, length=1.6, bones=merge(arms([(0, 0, 0), (0.5, 60, -20), (0.8, 30, 40), (1.6, 0, 0)],
                                                           [(0, 0, 0), (0.6, 60, -20), (0.9, 30, 40), (1.6, 0, 0)]), canopy_shake(0.75, 1.1, 4)))

# 番兵召喚：根が震え、顔のうろが開く（1.0 秒）
A["summon"] = dict(loop=False, length=1.8, bones=merge(roots_wave(0.3, 0.03, 12, 0.6), {
    "face": {"scale": keys((0, ONE), (0.8, [1.1, 1.1, 1.1]), (1.2, [1.1, 1.1, 1.1]), (1.8, ONE))},
}, arms([(0, 0, 0), (0.8, 30, 0), (1.3, 30, 0), (1.8, 0, 0)])))

# 大地の脈動（第2形態）：腕を大きく掲げて、1.0 秒に地面を打つ
A["pulse"] = dict(loop=False, length=2.2, bones=merge(arms([(0, 0, 0), (0.8, 70, 10), (1.0, -25, 40), (1.6, -25, 40), (2.2, 0, 0)]),
                                                       roots_wave(1.0, 0.0, 20, 0.6), canopy_shake(1.0, 1.6, 5), {
    "trunk": {"rotation": keys((0, Z), (0.8, [-6, 0, 0]), (1.0, [5, 0, 0]), (1.6, [5, 0, 0]), (2.2, Z))},
}))

A["phase_change"] = dict(loop=False, length=3.0, bones=merge(arms([(0, 0, 0), (0.8, -20, 0), (1.5, 80, 20), (2.4, 80, 20), (3.0, 0, 0)]),
                                                             canopy_shake(0.3, 2.4, 5), roots_wave(1.5, 0.02, 25, 0.8), {
    "face": {"scale": keys((0, ONE), (1.5, [1.15, 1.15, 1.15]), (2.4, [1.12, 1.12, 1.12]), (3.0, ONE))},
}))

# 倒れる：根が浮き、幹が後ろへゆっくり傾いて倒れる
A["death"] = dict(loop="hold_on_last_frame", length=3.0, bones=merge(roots_wave(0.0, 0.05, 35, 3.0), arms([(0, 0, 0), (1.0, -30, 10), (3.0, -40, 20)]), {
    "trunk": {"rotation": keys((0, Z), (0.6, [-5, 0, 0]), (1.4, [20, 0, 0]), (2.4, [70, 0, 0]), (2.6, [64, 0, 0]), (3.0, [66, 0, 0]))},
    "canopy": {"rotation": keys((0, Z), (2.4, [8, 0, 0]), (2.6, [-4, 0, 0]), (3.0, Z))},
}))


def to_json():
    return {"format_version": "1.8.0", "animations": {
        f"animation.yggdrasil.{n}": {"loop": a["loop"], "animation_length": a["length"], "bones": a["bones"]}
        for n, a in A.items()}}


if __name__ == "__main__":
    os.makedirs(OUT, exist_ok=True)
    with open(f"{OUT}/yggdrasil.animation.json", "w") as f:
        json.dump(to_json(), f, indent=1)
    print(len(A), "animations")
