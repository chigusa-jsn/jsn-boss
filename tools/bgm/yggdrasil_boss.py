"""ユグドラシル（第1〜3形態）「古樹の目覚め」ボス版  E minor（半音の不穏さ入り）, 150 BPM, ループ"""
import sys, numpy as np
from synth import *

BPM = 150
S16 = 60 / BPM / 4; BAR = S16 * 16
CH = {'Em': [40, 43, 47], 'F': [41, 45, 48], 'C': [36, 40, 43], 'D': [38, 42, 45],
      'B': [35, 39, 42], 'Am': [45, 48, 52], 'G': [43, 47, 50]}
INTRO = ['Em', 'Em', 'Em', 'F']
A_ = ['Em', 'F', 'Em', 'F', 'C', 'D', 'B', 'B']
B_ = ['Am', 'F', 'Em', 'C', 'Am', 'F', 'B', 'B']
C_ = ['C', 'D', 'Em', 'Em', 'C', 'D', 'B', 'B']
SECTIONS = [('intro', INTRO), ('A', A_), ('B', B_), ('C', C_)]
MEL_A = mel("""E5:3 E5:3 B4:2 E5:4 G5:4  F5:6 E5:2 C5:8  E5:3 E5:3 B4:2 G5:4 B5:4  A5:6 G5:2 F5:8
E5:4 G5:4 C6:4 B5:4  A5:4 F#5:4 D5:4 A5:4  B5:12 A5:2 G5:2  F#5:8 D#5:8""")
MEL_B = mel("""A5:2 r:2 A5:2 B5:2 C6:4 E6:4  D6:6 C6:2 A5:8  B5:2 r:2 B5:2 C6:2 B5:4 G5:4  E5:6 G5:2 C6:8
A5:4 C6:4 E6:4 A6:4  F6:6 E6:2 C6:8  D#6:8 F#6:8  B6:12 r:4""")
MEL_C = mel("""G5:2 E5:2 C5:2 E5:2 G5:2 C6:2 E6:2 C6:2  A5:2 F#5:2 D5:2 F#5:2 A5:2 D6:2 F#6:2 D6:2
B5:2 G5:2 E5:2 G5:2 B5:2 E6:2 G6:2 E6:2  B6:8 G6:4 E6:4
E6:6 D6:2 C6:4 B5:4  A5:6 B5:2 C6:4 D6:4  D#6:16  B5:4 r:12""")
RIFF = [0, 0, 12, 0, 0, 12, 0, 0, 0, 0, 12, 0, 1, 0, 12, 0]

def render():
    rng = np.random.default_rng(31)
    bars = sum(len(c) for _, c in SECTIONS)
    tr = Track(bars * BAR); t0 = 0.0
    for sec, chords in SECTIONS:
        for bi, cname in enumerate(chords):
            bt = t0 + bi * BAR; notes = CH[cname]; root = notes[0]
            last = bi == len(chords) - 1
            # パワーコード（ルート・5度・オクターブ）
            if sec != 'intro' or bi == 3:
                for m in (root + 12, root + 19, root + 24):
                    for pan in (-0.6, 0.6):
                        tr.add('pad', bt, chip_pad(mtof(m), BAR, rng, attack=0.01, cutoff=2600), pan, 0.035)
            # 16分で刻むベースリフ（Emのときだけ半音上がる不穏な音）
            b = root - 12 if root >= 40 else root
            for i, o in enumerate(RIFF):
                if o == 1 and cname != 'Em': o = 0
                tr.add('bass', bt + i * S16, chip_bass(mtof(b + o), S16 * 0.9, 1600), 0, 0.42 if i % 4 == 0 else 0.32)
            # 16分アルペジオ
            if sec != 'intro':
                tones = [notes[0] + 24, notes[2] + 24, notes[1] + 36, notes[2] + 24]
                for i in range(16):
                    tr.add('arp', bt + i * S16, chip_arp(mtof(tones[i % 4]), S16), -0.5 if i % 2 else 0.5, 0.09)
            # ドラム：4つ打ち＋2・4拍スネア＋16分ハット
            kicks = [0, 4, 8, 12] if not (sec == 'intro' and bi < 2) else ([0, 8] if bi == 1 else [0])
            if sec == 'C' and bi in (2, 3): kicks = [0, 3, 4, 8, 10, 12, 14]
            for i in kicks: tr.add('drums', bt + i * S16, chip_kick(rng, 42, 120, 8), 0, 0.85)
            if not (sec == 'intro' and bi < 3):
                for i in (4, 12): tr.add('drums', bt + i * S16, chip_snare(rng), 0.1, 0.45)
            for i in range(16):
                if sec == 'intro' and bi == 0 and i % 4: continue
                tr.add('drums', bt + i * S16, chip_hat(rng), -0.35, 0.13 if i % 4 == 2 else 0.07)
            if last:                                          # フィル
                if sec == 'intro':
                    for i in range(0, 16, 1 if True else 2):
                        tr.add('drums', bt + i * S16, chip_snare(rng), 0.1, 0.12 + 0.025 * i)
                else:
                    for i in range(8, 16):
                        tr.add('drums', bt + i * S16, chip_tom(rng, 240 - 14 * (i - 8)), (-0.4 if i % 2 else 0.4), 0.35 + 0.04 * (i - 8))
            if bi == 0 and sec != 'intro': tr.add('drums', bt, chip_crash(rng), 0.4, 0.22)
            if bi == 4 and sec != 'intro': tr.add('drums', bt, chip_crash(rng), -0.4, 0.16)
            # イントロ：高い音の警報みたいな一音
            if sec == 'intro' and bi in (1, 2):
                for i in (0, 6, 12):
                    tr.add('arp', bt + i * S16, chip_lead(mtof(76 + (1 if i == 12 else 0)), S16 * 3, 0.5, 0.0, 0.002, 0.15), 0.3 if i else -0.3, 0.13)
        m = {'A': MEL_A, 'B': MEL_B, 'C': MEL_C}.get(sec)
        if m:
            pos = 0
            for p, l in m:
                if p is not None:
                    tr.add('lead', t0 + pos * S16, chip_lead(mtof(p), l * S16 * 0.95, 0.25), 0.0, 0.25)
                    if sec in ('B', 'C'):
                        tr.add('lead', t0 + pos * S16, chip_lead(mtof(p - 12), l * S16 * 0.95, 0.5, 0.15), 0.0, 0.12)
                pos += l
        t0 += len(chords) * BAR
    return master(tr, sends={'lead': 0.35, 'pad': 0.4, 'arp': 0.4, 'drums': 0.1},
                  levels={'lead': 1.5, 'pad': 1.2, 'bass': 0.55, 'arp': 1.3, 'drums': 0.62},
                  ir_sec=2.2, ir_decay=2.8, wet_gain=0.55, delay=('lead', S16 * 3))

if __name__ == '__main__':
    out = sys.argv[1] if len(sys.argv) > 1 else 'yggdrasil_boss.wav'
    mix = render(); write_wav(out, mix); print(out, len(mix) / SR)
