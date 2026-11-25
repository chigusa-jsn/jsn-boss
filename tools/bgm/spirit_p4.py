"""ユグドラシル精霊・第4形態「精霊の大詠唱」(オリジナル曲)  B minor, 6/8 (付点4分=72), ループ"""
import sys, numpy as np
from synth import *

S16 = 60 / 72 / 6           # 6/8 の16分音符
BAR = S16 * 12
CH = {'Bm': [47, 50, 54], 'G': [43, 47, 50], 'D': [50, 54, 57], 'A': [45, 49, 52],
      'Em': [40, 43, 47], 'F#': [42, 46, 49], 'F#m': [42, 45, 49]}
INTRO = ['Bm', 'G', 'Bm', 'F#']
A_ = ['Bm', 'G', 'D', 'A', 'Bm', 'G', 'Em', 'F#']
B_ = ['G', 'A', 'F#m', 'Bm', 'G', 'A', 'F#', 'F#']
SECTIONS = [('intro', INTRO), ('A', A_), ('B', B_), ('A2', A_)]
MEL_A = mel("""F#5:4 E5:2 D5:4 C#5:2  B4:6 D5:6  A4:4 F#5:2 E5:4 D5:2  E5:12
F#5:4 G5:2 A5:4 F#5:2  G5:4 F#5:2 E5:4 D5:2  E5:4 D5:2 C#5:4 B4:2  C#5:6 A#4:6""")
MEL_B = mel("""D6:6 B5:6  C#6:6 A5:6  A5:4 F#5:2 C#6:4 A5:2  B5:12
B5:4 A5:2 G5:4 F#5:2  E5:4 F#5:2 G5:4 A5:2  A#5:6 C#6:6  F#5:6 r:6""")
COUNTER = mel("""D6:2 F#6:2 B6:2 F#6:2 D6:2 B5:2  B5:2 D6:2 G6:2 D6:2 B5:2 G5:2  A5:2 D6:2 F#6:2 D6:2 A5:2 F#5:2  A5:2 C#6:2 E6:2 C#6:2 A5:2 E5:2
D6:2 F#6:2 B6:2 F#6:2 D6:2 B5:2  B5:2 D6:2 G6:2 D6:2 B5:2 G5:2  G5:2 B5:2 E6:2 B5:2 G5:2 E5:2  A#5:2 C#6:2 F#6:2 C#6:2 A#5:2 F#5:2""")

def render():
    rng = np.random.default_rng(23)
    bars = sum(len(c) for _, c in SECTIONS)
    tr = Track(bars * BAR); t0 = 0.0
    for sec, chords in SECTIONS:
        for bi, cname in enumerate(chords):
            bt = t0 + bi * BAR; notes = CH[cname]; root = notes[0]
            for m in notes:
                tr.add('choir', bt, choir(mtof(m + 12), BAR * 1.02, rng, attack=0.35), rng.uniform(-0.5, 0.5), 0.14)
            # ハープ（上下に流れるアルペジオ）
            seq = [root - 12, notes[1] - 12 + 12 * (notes[1] < root), notes[2], root + 12, notes[1] + 12, notes[2] + 12]
            seq = seq + seq[::-1][1:-1] + [root]
            for i in range(6):
                p = seq[i] if bi % 2 == 0 else seq[(i + 5) % len(seq)]
                tr.add('harp', bt + i * S16 * 2, pluck(mtof(p), S16 * 2, 0.8), -0.5 + i * 0.2, 0.3)
            # ベース（付点4分）
            if sec != 'intro' or bi >= 2:
                for i in (0, 6):
                    f = mtof(root - 12 if root > 45 else root); nn = int(S16 * 5.5 * SR)
                    ph = phase(f, nn + 6000); env = env_adsr(nn, 0.01, 0.3, 0.6, 0.15)[:len(ph)]
                    w = lowpass(additive_saw(ph[:len(env)], f, 16), 600) * env
                    tr.add('bass', bt + i * S16, w, 0, 0.45)
            # パーカッション
            if sec != 'intro':
                for i in (0, 6):
                    tr.add('drums', bt + i * S16, frame_drum(rng), 0, 0.55 if i == 0 else 0.4)
                if sec == 'B':
                    tr.add('drums', bt + 10 * S16, frame_drum(rng), 0.2, 0.3)
                for i in range(0, 12, 2):
                    tr.add('drums', bt + i * S16, shaker(rng), 0.45, 0.1 if i % 6 else 0.16)
                if bi % 2 == 0:
                    tr.add('bell', bt, bell(mtof(root + 48), 1.0, decay=1.4, index=1.2, ratio=4.0), 0.6, 0.06)
            else:
                tr.add('bell', bt, bell(mtof(notes[2] + 36), 1.2, decay=1.2, index=1.5), (-0.5 if bi % 2 else 0.5), 0.16)
            if bi == len(chords) - 1 and sec != 'intro':     # キラキラ上昇
                for i in range(6, 12):
                    tr.add('bell', bt + i * S16, bell(mtof(notes[i % 3] + 36 + 12 * (i >= 9)), 0.4, decay=3, index=1.0, ratio=4), -0.6 + 0.2 * (i - 6), 0.1)
        m = {'A': MEL_A, 'B': MEL_B, 'A2': MEL_A}.get(sec)
        if m:
            pos = 0
            for p, l in m:
                if p is not None:
                    up = 12 if sec == 'A2' and p < 76 else 0
                    tr.add('lead', t0 + pos * S16, flute(mtof(p + up), l * S16 * 0.95, rng), 0.0, 0.5)
                pos += l
        if sec in ('A2',):
            pos = 0
            for p, l in COUNTER:
                tr.add('bell', t0 + pos * S16, bell(mtof(p), 0.3, decay=3.5, index=1.0, ratio=4.0), 0.5 if (pos // 2) % 2 else -0.5, 0.07)
                pos += l
        t0 += len(chords) * BAR
    return master(tr, sends={'choir': 0.6, 'lead': 0.4, 'harp': 0.45, 'bell': 0.8, 'drums': 0.2},
                  levels={'choir': 1.5, 'harp': 1.0, 'bass': 0.6, 'drums': 0.8, 'bell': 1.6, 'lead': 1.0},
                  ir_sec=3.0, ir_decay=2.2, wet_gain=0.65, delay=('lead', S16 * 3))

if __name__ == '__main__':
    out = sys.argv[1] if len(sys.argv) > 1 else 'spirit_p4.wav'
    mix = render(); write_wav(out, mix); print(out, len(mix) / SR)
