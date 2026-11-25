"""ユグドラシル（第1〜3形態）「古樹の目覚め」(オリジナル曲)  E phrygian/minor, 112 BPM, ループ"""
import sys, numpy as np
from synth import *

BPM = 112
S16 = 60 / BPM / 4; BAR = S16 * 16
CH = {'Em': [40, 43, 47], 'F': [41, 45, 48], 'D': [38, 42, 45], 'C': [36, 40, 43],
      'Am': [45, 48, 52], 'G': [43, 47, 50], 'B': [47, 51, 54]}
INTRO = ['Em', 'F', 'Em', 'F']
A_ = ['Em', 'F', 'Em', 'D', 'C', 'D', 'Em', 'Em']
B_ = ['Am', 'Em', 'F', 'G', 'Am', 'B', 'Em', 'Em']
BR = ['C', 'B', 'C', 'B']
SECTIONS = [('intro', INTRO), ('A', A_), ('B', B_), ('bridge', BR)]
MEL_A = mel("""E4:8 G4:4 B4:4  A4:6 G4:2 F4:8  E4:4 G4:4 B4:4 E5:4  D5:12 C5:2 B4:2
C5:8 B4:4 A4:4  B4:6 A4:2 F#4:8  G4:8 F4:4 E4:4  E4:16""")
MEL_B = mel("""E5:6 D5:2 C5:4 E5:4  B4:12 G4:4  A4:4 C5:4 F5:8  D5:6 B4:2 G4:8
A4:4 C5:4 E5:4 A5:4  F#5:8 D#5:8  E5:16  B4:8 r:8""")

def render():
    rng = np.random.default_rng(11)
    bars = sum(len(c) for _, c in SECTIONS)
    tr = Track(bars * BAR); t0 = 0.0
    for sec, chords in SECTIONS:
        for bi, cname in enumerate(chords):
            bt = t0 + bi * BAR; notes = CH[cname]; root = notes[0]
            # 聖歌隊（あー）
            for m in notes:
                tr.add('choir', bt, choir(mtof(m + 12), BAR * 1.02, rng, attack=0.5), rng.uniform(-0.4, 0.4), 0.16)
            # 低いうなり（8分で脈打つベース）
            for i in range(8):
                f = mtof(root - 12 if root >= 40 else root)
                nn = int(S16 * 2 * SR * 0.95); ph = phase(f, nn + 3000)
                env = env_adsr(nn, 0.01, 0.15, 0.5, 0.05)[:len(ph)]
                w = lowpass(naive_saw(ph[:len(env)]) + naive_saw(ph[:len(env)] * 1.006), 380) * env
                acc = 1.0 if i % 2 == 0 else 0.6
                if sec == 'intro' and bi < 2: acc *= 0.5
                tr.add('bass', bt + i * S16 * 2, w, 0, 0.5 * acc)
            # 鐘（小節頭と3拍目）
            for i, m in ((0, notes[0] + 36), (8, notes[2] + 24)):
                if sec == 'bridge' or sec == 'intro' or i == 0:
                    tr.add('bell', bt + i * S16, bell(mtof(m), 1.0), 0.45 if i else -0.45, 0.16)
            # 和太鼓
            pat = {'intro': [0], 'A': [0, 6, 8, 11], 'B': [0, 3, 6, 8, 10, 11, 14], 'bridge': [0, 8]}[sec]
            if sec == 'intro' and bi >= 2: pat = [0, 8]
            for i in pat:
                big = i in (0, 8)
                tr.add('drums', bt + i * S16, taiko(rng, 1.0 if big else 1.35), 0.0 if big else 0.3, 0.9 if big else 0.5)
            if sec in ('A', 'B'):   # 締め太鼓っぽい高い音
                for i in (4, 12):
                    tr.add('drums', bt + i * S16, frame_drum(rng) , -0.3, 0.35)
            last = bi == len(chords) - 1
            if last:                 # ロール
                for i in range(8, 16):
                    tr.add('drums', bt + i * S16, taiko(rng, 1.6), (-0.4 if i % 2 else 0.4), 0.25 + 0.05 * (i - 8))
            if bi == 0 and sec != 'intro':
                nn = int(2.5 * SR); tt = np.arange(nn) / SR
                gong = sum(np.sin(2 * np.pi * f * tt) * np.exp(-tt * d) for f, d in ((98, 1.2), (147.3, 1.6), (211, 2.2), (293, 3)))
                tr.add('drums', bt, gong * np.minimum(1, tt / 0.01) * 0.25, 0.1, 0.5)
        m = {'A': MEL_A, 'B': MEL_B}.get(sec)
        if m:
            pos = 0
            for p, l in m:
                if p is not None:
                    tr.add('lead', t0 + pos * S16, horn(mtof(p), l * S16 * 0.97), -0.05, 0.42)
                    tr.add('lead', t0 + pos * S16, horn(mtof(p - 12), l * S16 * 0.97), 0.05, 0.22)
                pos += l
        t0 += len(chords) * BAR
    return master(tr, sends={'choir': 0.6, 'lead': 0.35, 'bell': 0.7, 'drums': 0.25},
                  levels={'choir': 1.5, 'bass': 0.5, 'bell': 1.9, 'drums': 0.7, 'lead': 0.95},
                  ir_sec=3.2, ir_decay=2.0, wet_gain=0.7)

if __name__ == '__main__':
    out = sys.argv[1] if len(sys.argv) > 1 else 'yggdrasil.wav'
    mix = render(); write_wav(out, mix); print(out, len(mix) / SR)
