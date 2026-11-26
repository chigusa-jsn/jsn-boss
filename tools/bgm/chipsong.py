"""ピコピコ曲の共通エンジン。曲ごとの設定（コード・メロディ・パターン）を渡すと、ループするステレオ音声を作る。"""
import numpy as np
from synth import *

DRUMS = {   # 1小節16ステップ（6/8・3/4は12ステップ）: (kick, snare, hat間隔, ハットの強拍)
    'none':   ([], [], 0),
    'light':  ([0], [], 4),
    'half':   ([0, 10], [8], 2),
    'rock':   ([0, 6, 8], [4, 12], 2),
    'drive':  ([0, 4, 8, 12], [4, 12], 1),
    'gallop': ([0, 3, 8, 11], [4, 12], 2),
    'break':  ([0, 3, 6, 8, 10, 14], [4, 12], 1),
    'waltz':  ([0], [4, 8], 2),
    'waltz2': ([0, 6], [4, 8], 1),
    'six8':   ([0, 6], [6], 2),
    'march':  ([0, 8], [4, 12, 14], 2),
    'gallop8': ([0, 4, 6, 10], [3, 9], 2),   # 6/8 で駆ける
}

def bass_notes(kind, root, steps):
    """(step, midi, 長さ16分) のリスト"""
    b = root - 12 if root >= 40 else root
    if kind == 'oct8':   return [(i, b + (12 if (i // 2) % 2 else 0), 2) for i in range(0, steps, 2)]
    if kind == 'root8':  return [(i, b, 2) for i in range(0, steps, 2)]
    if kind == 'gallop': return [(i + o, b + (12 if i % 8 == 4 else 0), 1 if o < 2 else 2) for i in range(0, steps, 4) for o in (0, 1, 2)][:steps]
    if kind == 'riff16': return [(i, b + (12 if i % 3 == 2 else 0), 1) for i in range(steps)]
    if kind == 'whole':  return [(0, b, steps)]
    if kind == 'half':   return [(0, b, steps // 2), (steps // 2, b + 7 if steps == 16 else b, steps // 2)]
    if kind == 'waltz':  return [(0, b, 4), (4, b + 12, 2), (8, b + 7, 2)]
    if kind == 'dotted': return [(0, b, 6), (6, b + 12, 4), (10, b, 6)][: (3 if steps == 16 else 2)]
    if kind == 'pump':   return [(i, b + (12 if i % 4 == 2 else 0), 1) for i in range(0, steps, 1) if i % 4 != 3]
    return []

def render(song, seed=1):
    rng = np.random.default_rng(seed)
    S16 = 60 / song['bpm'] / 4
    steps = song.get('steps', 16)
    BAR = S16 * steps
    CH = song['chords']
    bars = sum(len(s['chords']) for s in song['sections'])
    tr = Track(bars * BAR); t0 = 0.0
    for sec in song['sections']:
        chords = sec['chords']
        for bi, cname in enumerate(chords):
            bt = t0 + bi * BAR; notes = CH[cname]; root = notes[0]
            last = bi == len(chords) - 1
            # パッド
            pad = sec.get('pad', song.get('pad', 'triad'))
            if pad:
                ms = [root + 12, root + 19, root + 24] if pad == 'power' else [m + 12 for m in notes]
                for m in ms:
                    for pan in (-0.6, 0.6):
                        tr.add('pad', bt, chip_pad(mtof(m), BAR, rng, attack=sec.get('pad_attack', 0.05),
                                                    cutoff=song.get('pad_cutoff', 2200)), pan, 0.035)
            # ベース
            for st, m, ln in bass_notes(sec.get('bass', 'oct8'), root, steps):
                tr.add('bass', bt + st * S16, chip_bass(mtof(m), S16 * ln * 0.9, song.get('bass_cutoff', 1400)), 0,
                       0.42 if st % 4 == 0 else 0.32)
            # アルペジオ
            arp = sec.get('arp', song.get('arp'))
            if arp:
                pat, rate, oct_ = arp
                tones = [notes[0] + 12 * oct_, notes[1] + 12 * oct_, notes[2] + 12 * oct_, notes[0] + 12 * (oct_ + 1)]
                for k, i in enumerate(range(0, steps, rate)):
                    tr.add('arp', bt + i * S16, chip_arp(mtof(tones[pat[k % len(pat)]]), S16 * rate, song.get('arp_duty', 0.125)),
                           -0.5 if k % 2 else 0.5, song.get('arp_gain', 0.1))
            # ドラム
            kick, snare, hat = DRUMS[sec.get('drums', 'rock')]
            for i in kick: tr.add('drums', bt + i * S16, chip_kick(rng, 42, 120, 8), 0, 0.85)
            for i in snare: tr.add('drums', bt + i * S16, chip_snare(rng, song.get('snare_tone', 185)), 0.1, 0.45)
            if hat:
                for i in range(0, steps, hat):
                    tr.add('drums', bt + i * S16, chip_hat(rng), -0.35, 0.13 if i % 4 == 2 else 0.07)
            if last and sec.get('fill', True):
                h = steps // 2
                if sec.get('fill') == 'snare':
                    for i in range(h, steps): tr.add('drums', bt + i * S16, chip_snare(rng), 0.1, 0.15 + 0.03 * (i - h))
                else:
                    for i in range(h, steps):
                        tr.add('drums', bt + i * S16, chip_tom(rng, 240 - 14 * (i - h)), (-0.4 if i % 2 else 0.4), 0.35 + 0.04 * (i - h))
            if bi == 0 and sec.get('crash', True): tr.add('drums', bt, chip_crash(rng), 0.4, 0.2)
        # メロディ
        if sec.get('mel'):
            pos = 0
            duty = sec.get('duty', song.get('duty', 0.25))
            for p, l in mel(sec['mel']):
                if p is not None:
                    p += sec.get('transpose', 0)
                    tr.add('lead', t0 + pos * S16, chip_lead(mtof(p), l * S16 * 0.95, duty, song.get('vib', 0.25)), 0.0, 0.25)
                    if sec.get('double'):
                        tr.add('lead', t0 + pos * S16, chip_lead(mtof(p + sec['double']), l * S16 * 0.95, 0.5, 0.15), 0.0, 0.12)
                pos += l
            assert pos == len(chords) * steps, (song.get('name'), sec.get('id'), pos, len(chords) * steps)
        t0 += len(chords) * BAR
    return master(tr, sends={'lead': 0.35, 'pad': 0.45, 'arp': 0.4, 'drums': 0.1},
                  levels={'lead': 1.5, 'pad': 1.2, 'bass': 0.55, 'arp': 1.3, 'drums': 0.62},
                  ir_sec=2.3, ir_decay=2.6, wet_gain=song.get('wet', 0.55), delay=('lead', S16 * 3))
