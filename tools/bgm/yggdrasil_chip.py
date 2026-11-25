"""ユグドラシル（第1〜3形態）「古樹の目覚め」ピコピコ版  E phrygian/minor, 112 BPM, ループ"""
import sys, numpy as np
from synth import *
from yggdrasil import BPM, S16, BAR, CH, SECTIONS, MEL_A, MEL_B

def render():
    rng = np.random.default_rng(11)
    bars = sum(len(c) for _, c in SECTIONS)
    tr = Track(bars * BAR); t0 = 0.0
    for sec, chords in SECTIONS:
        for bi, cname in enumerate(chords):
            bt = t0 + bi * BAR; notes = CH[cname]; root = notes[0]
            for m in notes:                                   # 重い和音（こもらせて低めに）
                for pan in (-0.6, 0.6):
                    tr.add('pad', bt, chip_pad(mtof(m + 12), BAR, rng, attack=0.3, cutoff=1300), pan, 0.04)
            for i in range(8):                                # うなるベース（8分）
                f = mtof(root - 12 if root >= 40 else root)
                acc = (1.0 if i % 2 == 0 else 0.6) * (0.5 if sec == 'intro' and bi < 2 else 1)
                tr.add('bass', bt + i * S16 * 2, chip_bass(f, S16 * 2 * 0.95, 700), 0, 0.5 * acc)
            # アルペジオ（8分でゆっくり、暗い響き）
            tones = [notes[0] + 24, notes[1] + 24, notes[2] + 24, notes[1] + 24]
            for i in range(8):
                if sec == 'intro' and bi < 2 and i % 2: continue
                tr.add('arp', bt + i * S16 * 2, chip_arp(mtof(tones[i % 4]), S16 * 2, 0.125, 0.15), -0.5 if i % 2 else 0.5, 0.14)
            # ドラム（太鼓っぽく低いキックとタム）
            pat = {'intro': [0], 'A': [0, 6, 8, 11], 'B': [0, 3, 6, 8, 10, 11, 14], 'bridge': [0, 8]}[sec]
            if sec == 'intro' and bi >= 2: pat = [0, 8]
            for i in pat:
                big = i in (0, 8)
                tr.add('drums', bt + i * S16, chip_kick(rng, 38 if big else 55, 90, 6 if big else 9), 0, 0.9 if big else 0.5)
            if sec in ('A', 'B'):
                for i in (4, 12): tr.add('drums', bt + i * S16, chip_snare(rng, 150), 0.1, 0.42)
                for i in range(0, 16, 2): tr.add('drums', bt + i * S16, chip_hat(rng), -0.35, 0.08)
            if bi == len(chords) - 1:                         # タムのロール
                for i in range(8, 16):
                    tr.add('drums', bt + i * S16, chip_tom(rng, 220 - 12 * (i - 8)), (-0.4 if i % 2 else 0.4), 0.3 + 0.05 * (i - 8))
            if bi == 0 and sec != 'intro':
                tr.add('drums', bt, chip_crash(rng), 0.4, 0.2)
            if sec in ('intro', 'bridge'):                    # 鐘のかわりに高いピコピコの一音
                tr.add('arp', bt, chip_lead(mtof(notes[0] + 36), S16 * 6, 0.5, 0.0, 0.002, 0.6), -0.3, 0.12)
        m = {'A': MEL_A, 'B': MEL_B}.get(sec)
        if m:
            pos = 0
            for p, l in m:
                if p is not None:
                    tr.add('lead', t0 + pos * S16, chip_lead(mtof(p), l * S16 * 0.95, 0.25, 0.25), 0.0, 0.26)
                    tr.add('lead', t0 + pos * S16, chip_lead(mtof(p - 12), l * S16 * 0.95, 0.5, 0.15), 0.0, 0.14)
                pos += l
        t0 += len(chords) * BAR
    return master(tr, sends={'lead': 0.35, 'pad': 0.5, 'arp': 0.4, 'drums': 0.12},
                  levels={'lead': 1.5, 'pad': 1.2, 'bass': 0.55, 'arp': 1.3, 'drums': 0.62},
                  ir_sec=2.6, ir_decay=2.4, wet_gain=0.6, delay=('lead', S16 * 3))

if __name__ == '__main__':
    out = sys.argv[1] if len(sys.argv) > 1 else 'yggdrasil_chip.wav'
    mix = render(); write_wav(out, mix); print(out, len(mix) / SR)
