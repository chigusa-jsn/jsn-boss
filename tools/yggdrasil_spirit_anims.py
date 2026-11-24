"""ユグドラシルの精霊と妖精のアニメーション。 python3 tools/yggdrasil_spirit_anims.py
腕を前へ上げるのは X マイナス。杖 staff・剣 sword は右の前腕の子。
翼は wing_right [0, -開き, -羽ばたき]、wing_left [0, 開き, 羽ばたき]。
光輪 halo は別のコントローラーの "halo" がずっと回す（ほかのアニメでは halo を触らない）。
"""
import json, os

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


def rot(bone, *pairs):
    return {bone: {"rotation": keys(*pairs)}}


def pos(bone, *pairs):
    return {bone: {"position": keys(*pairs)}}


def wings(*pairs):
    """pairs = [(t, 羽ばたき, 開き)]"""
    return {"wing_right": {"rotation": keys(*[(t, [0, -sp, -fl]) for t, fl, sp in pairs])},
            "wing_left": {"rotation": keys(*[(t, [0, sp, fl]) for t, fl, sp in pairs])}}


def bob(length, amp=1.5):
    return pos("root", (0, Z), (length / 2, [0, amp, 0]), (length, Z))


def hair(length, amp=6):
    return merge(rot("hair_back", (0, Z), (length / 2, [amp, 0, 0]), (length, Z)),
                 rot("hair_back_low", (0, Z), (length / 2, [amp * 1.4, 0, 2]), (length, Z)),
                 rot("bow_tails", (0, Z), (length / 2, [amp, 0, 0]), (length, Z)))


A = {}

# ---------------------------------------------------------------- ずっと
A["halo"] = dict(loop=True, length=8.0, bones=rot("halo", (0, Z), (4.0, [0, 0, 180]), (8.0, [0, 0, 360])))

# 第4形態：杖を持って浮かぶ
A["idle"] = dict(loop=True, length=3.0, bones=merge(bob(3.0), hair(3.0), {
    "torso": {"rotation": keys((0, Z), (1.5, [-2, 0, 0]), (3.0, Z))},
    "head": {"rotation": keys((0, Z), (1.0, [0, 6, 0]), (2.0, [0, -6, 0]), (3.0, Z))},
    "right_arm": {"rotation": keys((0, [-14, 0, 4]), (1.5, [-18, 0, 4]), (3.0, [-14, 0, 4]))},
    "left_arm": {"rotation": keys((0, [-8, 0, -14]), (1.5, [-12, 0, -18]), (3.0, [-8, 0, -14]))},
    "left_forearm": {"rotation": keys((0, [-20, 0, 0]), (3.0, [-20, 0, 0]))},
}))

# 第5形態：剣を持ち、翼をゆっくり開閉
A["idle_sword"] = dict(loop=True, length=2.4, bones=merge(bob(2.4, 2.0), hair(2.4, 8), wings((0, 0, 0), (1.2, 6, 8), (2.4, 0, 0)), {
    "torso": {"rotation": keys((0, [4, 0, 0]), (1.2, [2, 0, 0]), (2.4, [4, 0, 0]))},
    "right_arm": {"rotation": keys((0, [-28, 0, 16]), (1.2, [-32, 0, 18]), (2.4, [-28, 0, 16]))},
    "right_forearm": {"rotation": keys((0, [-20, 0, 0]), (2.4, [-20, 0, 0]))},
    "left_arm": {"rotation": keys((0, [-10, 0, -20]), (2.4, [-10, 0, -20]))},
}))

# ---------------------------------------------------------------- 第4形態の魔法
# 魔法（片手）：0.5 秒で杖を突き出す
A["cast"] = dict(loop=False, length=1.0, bones=merge(hair(1.0), {
    "right_arm": {"rotation": keys((0, [-14, 0, 4]), (0.35, [-150, 0, 10]), (0.5, [-95, 0, 0]), (0.8, [-60, 0, 4]), (1.0, [-14, 0, 4]))},
    "torso": {"rotation": keys((0, Z), (0.35, [-8, 0, 0]), (0.5, [6, 0, 0]), (1.0, Z))},
    "left_arm": {"rotation": keys((0, [-8, 0, -14]), (0.4, [-40, 0, -50]), (1.0, [-8, 0, -14]))},
}))

# 大きな魔法（両手）：0.8 秒で杖を高く掲げ、1.0 秒で振り下ろす
A["cast_big"] = dict(loop=False, length=1.6, bones=merge(hair(1.6, 10), pos("root", (0, Z), (0.8, [0, 4, 0]), (1.0, [0, 1, 0]), (1.6, Z)), {
    "right_arm": {"rotation": keys((0, [-14, 0, 4]), (0.8, [-175, 0, 0]), (1.0, [-80, 0, 0]), (1.6, [-14, 0, 4]))},
    "left_arm": {"rotation": keys((0, [-8, 0, -14]), (0.8, [-165, 0, -10]), (1.0, [-70, 0, -10]), (1.6, [-8, 0, -14]))},
    "torso": {"rotation": keys((0, Z), (0.8, [-12, 0, 0]), (1.0, [10, 0, 0]), (1.6, Z))},
    "head": {"rotation": keys((0, Z), (0.8, [-20, 0, 0]), (1.0, [5, 0, 0]), (1.6, Z))},
}))

# 杖の魔弾（ふつうの攻撃）
A["bolt"] = dict(loop=False, length=0.6, bones={
    "right_arm": {"rotation": keys((0, [-14, 0, 4]), (0.2, [-110, 0, -10]), (0.3, [-85, 0, 0]), (0.6, [-14, 0, 4]))},
})

# ---------------------------------------------------------------- 大詠唱（ずっと）
A["chant"] = dict(loop=True, length=2.0, bones=merge(hair(2.0, 12), {
    "root": {"position": keys((0, [0, 6, 0]), (1.0, [0, 7.5, 0]), (2.0, [0, 6, 0]))},
    "right_arm": {"rotation": keys((0, [-170, 0, 8]), (1.0, [-175, 0, 12]), (2.0, [-170, 0, 8]))},
    "left_arm": {"rotation": keys((0, [-150, 0, -30]), (1.0, [-155, 0, -35]), (2.0, [-150, 0, -30]))},
    "head": {"rotation": keys((0, [-25, 0, 0]), (2.0, [-25, 0, 0]))},
    "torso": {"rotation": keys((0, [-8, 0, 0]), (2.0, [-8, 0, 0]))},
}))
# 第5形態の大詠唱：剣を地面に突き立て、両手で柄を握る
A["chant_sword"] = dict(loop=True, length=2.0, bones=merge(hair(2.0, 12), wings((0, -10, 20), (1.0, -4, 26), (2.0, -10, 20)), {
    "root": {"position": keys((0, [0, 1, 0]), (1.0, [0, 2, 0]), (2.0, [0, 1, 0]))},
    "right_arm": {"rotation": keys((0, [-55, 0, -18]), (2.0, [-55, 0, -18]))},
    "left_arm": {"rotation": keys((0, [-55, 0, 18]), (2.0, [-55, 0, 18]))},
    "right_forearm": {"rotation": keys((0, [-10, 0, 0]), (2.0, [-10, 0, 0]))},
    "sword": {"rotation": keys((0, [150, 0, 0]), (2.0, [150, 0, 0]))},
    "head": {"rotation": keys((0, [18, 0, 0]), (2.0, [18, 0, 0]))},
}))

# 結界が割れて動けない（ずっと）
A["stun"] = dict(loop=True, length=2.0, bones=merge(wings((0, -30, -15), (2.0, -30, -15)), {
    "root": {"position": keys((0, [0, -6, 0]), (1.0, [0, -6.5, 0]), (2.0, [0, -6, 0]))},
    "torso": {"rotation": keys((0, [28, 0, 0]), (1.0, [31, 0, 0]), (2.0, [28, 0, 0]))},
    "head": {"rotation": keys((0, [30, 0, 10]), (2.0, [30, 0, 10]))},
    "right_arm": {"rotation": keys((0, [10, 0, 10]), (2.0, [10, 0, 10]))},
    "left_arm": {"rotation": keys((0, [10, 0, -10]), (2.0, [10, 0, -10]))},
}))

# ---------------------------------------------------------------- 登場・変身・死亡
# 神樹の幹から現れる（3秒）
A["emerge"] = dict(loop=False, length=3.0, bones=merge(hair(3.0, 10), {
    "root": {"position": keys((0, [0, -40, 0]), (1.6, [0, -8, 0]), (2.4, [0, 2, 0]), (3.0, Z)),
             "scale": keys((0, [0.3, 0.3, 0.3]), (1.6, [0.9, 0.9, 0.9]), (3.0, [1, 1, 1]))},
    "torso": {"rotation": keys((0, [30, 0, 0]), (1.6, [20, 0, 0]), (2.4, [-10, 0, 0]), (3.0, Z))},
    "head": {"rotation": keys((0, [40, 0, 0]), (2.0, [20, 0, 0]), (2.6, [-20, 0, 0]), (3.0, Z))},
    "right_arm": {"rotation": keys((0, [10, 0, 10]), (2.0, [10, 0, 10]), (2.6, [-40, 0, 70]), (3.0, [-14, 0, 4]))},
    "left_arm": {"rotation": keys((0, [10, 0, -10]), (2.0, [10, 0, -10]), (2.6, [-40, 0, -70]), (3.0, [-8, 0, -14]))},
}))

# 第5形態へ（4秒）：くるりと回り、2.0 秒で杖が剣に、翼が開く
A["transform"] = dict(loop=False, length=4.0, bones=merge(hair(4.0, 14), {
    "root": {"rotation": keys((0, Z), (1.8, [0, 360, 0]), (2.2, [0, 720, 0]), (4.0, [0, 720, 0])),
             "position": keys((0, Z), (1.8, [0, 8, 0]), (3.0, [0, 6, 0]), (4.0, Z))},
    "right_arm": {"rotation": keys((0, [-14, 0, 4]), (1.6, [-175, 0, 0]), (2.4, [-175, 0, 0]), (3.2, [-40, 0, 60]), (4.0, [-28, 0, 16]))},
    "left_arm": {"rotation": keys((0, [-8, 0, -14]), (1.6, [-40, 0, -70]), (3.2, [-40, 0, -70]), (4.0, [-10, 0, -20]))},
    "wings": {"scale": keys((0, [0.01, 0.01, 0.01]), (2.0, [0.01, 0.01, 0.01]), (2.8, [1.15, 1.15, 1.15]), (3.2, [1, 1, 1]))},
    "sword": {"scale": keys((0, [0.01, 0.01, 0.01]), (2.0, [0.01, 0.01, 0.01]), (2.4, [1, 1, 1]))},
    "head": {"rotation": keys((0, Z), (1.6, [-25, 0, 0]), (3.0, [-10, 0, 0]), (4.0, Z))},
}))

A["death"] = dict(loop=False, length=3.0, bones=merge(wings((0, 0, 0), (1.5, -40, -20), (3.0, -60, -30)), {
    "root": {"position": keys((0, Z), (1.0, [0, 4, 0]), (3.0, [0, -10, 0])),
             "scale": keys((0, [1, 1, 1]), (2.0, [1, 1, 1]), (3.0, [0.6, 0.6, 0.6]))},
    "torso": {"rotation": keys((0, Z), (1.0, [-20, 0, 0]), (3.0, [-35, 0, 0]))},
    "head": {"rotation": keys((0, Z), (1.0, [-35, 0, 0]), (3.0, [-40, 0, 0]))},
    "right_arm": {"rotation": keys((0, Z), (1.0, [-20, 0, 60]), (3.0, [0, 0, 40]))},
    "left_arm": {"rotation": keys((0, Z), (1.0, [-20, 0, -60]), (3.0, [0, 0, -40]))},
}))

# ---------------------------------------------------------------- 第5形態の剣技
SWORD_IDLE_R = [-28, 0, 16]

# 円舞斬（1.4秒）：0.6〜0.9 秒で一回転して周りを斬る
A["slash_spin"] = dict(loop=False, length=1.4, bones=merge(wings((0, 0, 0), (0.5, 10, 20), (0.9, -10, 30), (1.4, 0, 0)), {
    "root": {"rotation": keys((0, Z), (0.6, [0, -30, 0]), (0.9, [0, 360, 0]), (1.4, [0, 360, 0]))},
    "right_arm": {"rotation": keys((0, SWORD_IDLE_R), (0.5, [-80, 0, 70]), (0.9, [-90, 0, 90]), (1.4, SWORD_IDLE_R))},
    "sword": {"rotation": keys((0, Z), (0.5, [80, 0, 0]), (0.9, [90, 0, 0]), (1.4, Z))},
    "torso": {"rotation": keys((0, Z), (0.5, [0, 25, 0]), (0.9, [0, -20, 0]), (1.4, Z))},
}))

# 五方閃（1.4秒）：0.8 秒で剣を振り下ろす
A["slash_lines"] = dict(loop=False, length=1.4, bones=merge(wings((0, 0, 0), (0.6, 20, 10), (0.8, -15, 30), (1.4, 0, 0)), {
    "right_arm": {"rotation": keys((0, SWORD_IDLE_R), (0.6, [-190, 0, 10]), (0.8, [-50, 0, 0]), (1.0, [-30, 0, 0]), (1.4, SWORD_IDLE_R))},
    "left_arm": {"rotation": keys((0, [-10, 0, -20]), (0.6, [-160, 0, -10]), (0.8, [-60, 0, -10]), (1.4, [-10, 0, -20]))},
    "torso": {"rotation": keys((0, Z), (0.6, [-15, 0, 0]), (0.8, [22, 0, 0]), (1.4, Z))},
}))

# 星降り（1.6秒）：0.6 秒で剣を天へ突き上げる
A["star_fall"] = dict(loop=False, length=1.6, bones=merge(wings((0, 0, 0), (0.6, 25, 30), (1.6, 0, 0)), pos("root", (0, Z), (0.6, [0, 5, 0]), (1.6, Z)), {
    "right_arm": {"rotation": keys((0, SWORD_IDLE_R), (0.6, [-180, 0, 0]), (1.1, [-180, 0, 0]), (1.6, SWORD_IDLE_R))},
    "right_forearm": {"rotation": keys((0, [-20, 0, 0]), (0.6, Z), (1.6, [-20, 0, 0]))},
    "sword": {"rotation": keys((0, Z), (0.6, [-30, 0, 0]), (1.1, [-30, 0, 0]), (1.6, Z))},
    "head": {"rotation": keys((0, Z), (0.6, [-30, 0, 0]), (1.6, Z))},
}))

# 波紋斬（1.6秒）：0.7 秒で剣を地面に突き立てる
A["ripple"] = dict(loop=False, length=1.6, bones=merge(wings((0, 0, 0), (0.5, 20, 10), (0.7, -20, 30), (1.6, 0, 0)), pos("root", (0, Z), (0.5, [0, 4, 0]), (0.7, [0, -3, 0]), (1.6, Z)), {
    "right_arm": {"rotation": keys((0, SWORD_IDLE_R), (0.5, [-150, 0, 0]), (0.7, [-50, 0, -10]), (1.3, [-50, 0, -10]), (1.6, SWORD_IDLE_R))},
    "left_arm": {"rotation": keys((0, [-10, 0, -20]), (0.7, [-50, 0, 10]), (1.3, [-50, 0, 10]), (1.6, [-10, 0, -20]))},
    "sword": {"rotation": keys((0, Z), (0.5, [0, 0, 0]), (0.7, [150, 0, 0]), (1.3, [150, 0, 0]), (1.6, Z))},
    "torso": {"rotation": keys((0, Z), (0.7, [25, 0, 0]), (1.3, [25, 0, 0]), (1.6, Z))},
}))

# 十字閃・市松斬（1.6秒）：0.6 秒と 1.1 秒に斜めに斬る
A["cross"] = dict(loop=False, length=1.6, bones=merge(wings((0, 0, 0), (0.6, -15, 25), (1.1, -15, 25), (1.6, 0, 0)), {
    "right_arm": {"rotation": keys((0, SWORD_IDLE_R), (0.45, [-160, 0, 60]), (0.6, [-40, 0, -50]), (0.95, [-160, 0, -40]), (1.1, [-40, 0, 60]), (1.6, SWORD_IDLE_R))},
    "torso": {"rotation": keys((0, Z), (0.45, [0, 30, 0]), (0.6, [10, -30, 0]), (0.95, [0, -30, 0]), (1.1, [10, 30, 0]), (1.6, Z))},
}))

# 天翔斬（3.0秒）：舞い上がり（体ごと上へ動く）、1.8 秒で急降下して突き刺す
A["dive"] = dict(loop=False, length=3.0, bones=merge(wings((0, 0, 0), (0.6, 40, 20), (1.0, -20, 30), (1.6, 40, 30), (1.8, -40, 40), (2.4, -30, 30), (3.0, 0, 0)), {
    "right_arm": {"rotation": keys((0, SWORD_IDLE_R), (1.4, [-175, 0, 0]), (1.8, [-60, 0, 0]), (2.6, [-60, 0, 0]), (3.0, SWORD_IDLE_R))},
    "left_arm": {"rotation": keys((0, [-10, 0, -20]), (1.4, [-165, 0, 0]), (1.8, [-60, 0, 0]), (2.6, [-60, 0, 0]), (3.0, [-10, 0, -20]))},
    "sword": {"rotation": keys((0, Z), (1.4, Z), (1.8, [150, 0, 0]), (2.6, [150, 0, 0]), (3.0, Z))},
    "torso": {"rotation": keys((0, Z), (1.4, [-15, 0, 0]), (1.8, [30, 0, 0]), (2.6, [30, 0, 0]), (3.0, Z))},
}))

# 羽根の嵐（1.6秒）：0.7 秒で翼を大きく羽ばたかせる
A["feather_storm"] = dict(loop=False, length=1.6, bones=merge(wings((0, 0, 0), (0.5, 40, -10), (0.7, -45, 45), (1.0, -30, 40), (1.6, 0, 0)), pos("root", (0, Z), (0.5, [0, -2, 0]), (0.7, [0, 6, 0]), (1.6, Z)), {
    "torso": {"rotation": keys((0, Z), (0.5, [15, 0, 0]), (0.7, [-20, 0, 0]), (1.6, Z))},
    "left_arm": {"rotation": keys((0, [-10, 0, -20]), (0.7, [-40, 0, -80]), (1.6, [-10, 0, -20]))},
    "right_arm": {"rotation": keys((0, SWORD_IDLE_R), (0.7, [-40, 0, 80]), (1.6, SWORD_IDLE_R))},
}))

# 八属の剣舞（5.0秒）：0.6 秒ごとに8回斬りながら回る
def eight():
    arm, rootr, tor = [(0, SWORD_IDLE_R)], [(0, Z)], [(0, Z)]
    for i in range(8):
        t = 0.6 + i * 0.5
        side = 1 if i % 2 == 0 else -1
        arm += [(t - 0.2, [-150, 0, 60 * side]), (t, [-50, 0, -50 * side])]
        rootr += [(t, [0, 45 * (i + 1), 0])]
        tor += [(t - 0.2, [0, 25 * side, 0]), (t, [10, -25 * side, 0])]
    arm += [(5.0, SWORD_IDLE_R)]; rootr += [(5.0, [0, 360, 0])]; tor += [(5.0, Z)]
    return {"right_arm": {"rotation": keys(*arm)}, "root": {"rotation": keys(*rootr)}, "torso": {"rotation": keys(*tor)}}


A["eight_blade"] = dict(loop=False, length=5.0, bones=merge(eight(), wings((0, 0, 0), (0.6, -15, 25), (4.4, -15, 25), (5.0, 0, 0))))

# ---------------------------------------------------------------- 妖精
F = {}
F["idle"] = dict(loop=True, length=0.4, bones=merge({
    "wing_right": {"rotation": keys((0, [0, -25, 0]), (0.2, [0, 25, 0]), (0.4, [0, -25, 0]))},
    "wing_left": {"rotation": keys((0, [0, 25, 0]), (0.2, [0, -25, 0]), (0.4, [0, 25, 0]))},
    "root": {"position": keys((0, Z), (0.2, [0, 0.6, 0]), (0.4, Z))},
}))
F["cast"] = dict(loop=False, length=0.6, bones={
    "right_arm": {"rotation": keys((0, Z), (0.25, [-150, 0, 0]), (0.35, [-90, 0, 0]), (0.6, Z))},
    "left_arm": {"rotation": keys((0, Z), (0.25, [-150, 0, 0]), (0.35, [-90, 0, 0]), (0.6, Z))},
})


def to_json(name, anims):
    return {"format_version": "1.8.0", "animations": {
        f"animation.{name}.{n}": {"loop": ("hold_on_last_frame" if n == "death" else a["loop"]), "animation_length": a["length"], "bones": a["bones"]}
        for n, a in anims.items()}}


if __name__ == "__main__":
    os.makedirs(OUT, exist_ok=True)
    json.dump(to_json("yggdrasil_spirit", A), open(f"{OUT}/yggdrasil_spirit.animation.json", "w"), indent=1)
    json.dump(to_json("spirit_fairy", F), open(f"{OUT}/spirit_fairy.animation.json", "w"), indent=1)
    print(len(A), "+", len(F), "animations")
