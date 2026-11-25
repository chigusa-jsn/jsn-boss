"""ユグドラシル精霊・第4形態「精霊の大詠唱」ピコピコ版  B minor, 6/8, ループ"""
import sys, numpy as np
from synth import *
from spirit_p4 import S16, BAR, CH, SECTIONS, MEL_A, MEL_B, COUNTER

def render():
    rng = np.random.default_rng(23)
    bars = sum(len(c) for _, c in SECTIONS)
    tr = Track(bars * BAR); t0 = 0.0
    for sec, chords in SECTIONS:
        for bi, cname in enumerate(chords):
            bt = t0 + bi * BAR; notes = CH[cname]; root = notes[0]
            for m in notes:
                for pan in (-0.6, 0.6):
                    tr.add('pad', bt, chip_pad(mtof(m + 12), BAR, rng, attack=0.2, cutoff=1800), pan, 0.04)
            # 16分で上下に流れるアルペジオ（ハープのかわり）
            seq = [root, notes[1], notes[2], root + 12, notes[1] + 12, notes[2] + 12]
            seq = seq + seq[::-1]
            for i in range(12):
                tr.add('arp', bt + i * S16, chip_arp(mtof(seq[i] + 12), S16, 0.125, 0.3), -0.5 if i % 2 else 0.5, 0.12)
            if sec != 'intro' or bi >= 2:
                for i in (0, 6, 9) if sec == 'B' else (0, 6):
                    f = mtof(root - 12 if root > 45 else root)
                    tr.add('bass', bt + i * S16, chip_bass(f, S16 * (5.5 if i != 9 else 2.5)), 0, 0.42)
            if sec != 'intro':
                tr.add('drums', bt, chip_kick(rng), 0, 0.7)
                tr.add('drums', bt + 6 * S16, chip_snare(rng), 0.1, 0.38)
                if sec == 'B':
                    tr.add('drums', bt + 4 * S16, chip_kick(rng), 0, 0.45)
                    tr.add('drums', bt + 10 * S16, chip_kick(rng), 0, 0.45)
                for i in range(0, 12, 2):
                    tr.add('drums', bt + i * S16, chip_hat(rng), -0.35, 0.12 if i % 6 else 0.16)
                if bi == 0: tr.add('drums', bt, chip_crash(rng), 0.4, 0.18)
            if bi == len(chords) - 1 and sec != 'intro':
                for i in range(6, 12):
                    tr.add('drums', bt + i * S16, chip_snare(rng), 0.1, 0.18 + 0.04 * (i - 6))
        m = {'A': MEL_A, 'B': MEL_B, 'A2': MEL_A}.get(sec)
        if m:
            pos = 0
            for p, l in m:
                if p is not None:
                    up = 12 if sec == 'A2' and p < 76 else 0
                    tr.add('lead', t0 + pos * S16, chip_lead(mtof(p + up), l * S16 * 0.95, 0.25 if sec != 'B' else 0.5), 0.0, 0.25)
                    if sec == 'A2':
                        tr.add('lead', t0 + pos * S16, chip_lead(mtof(p + up - 12), l * S16 * 0.95, 0.5, 0.15), 0.0, 0.12)
                pos += l
        if sec == 'A2':
            pos = 0
            for p, l in COUNTER:
                tr.add('arp', t0 + pos * S16, chip_arp(mtof(p), S16 * 2, 0.25, 0.2), 0.5 if (pos // 2) % 2 else -0.5, 0.07)
                pos += l
        t0 += len(chords) * BAR
    return master(tr, sends={'lead': 0.35, 'pad': 0.5, 'arp': 0.4, 'drums': 0.12},
                  levels={'lead': 1.5, 'pad': 1.2, 'bass': 0.55, 'arp': 1.3, 'drums': 0.62},
                  ir_sec=2.4, ir_decay=2.6, wet_gain=0.6, delay=('lead', S16 * 3))

if __name__ == '__main__':
    out = sys.argv[1] if len(sys.argv) > 1 else 'spirit_p4_chip.wav'
    mix = render(); write_wav(out, mix); print(out, len(mix) / SR)
