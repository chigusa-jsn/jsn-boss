"""ユグドラシル精霊・第5形態「星樹の魔法剣士」(オリジナル曲)  D minor, 168 BPM, ループ"""
import sys, numpy as np
from synth import *

BPM = 168
S16 = 60 / BPM / 4           # 16分音符の秒数
BAR = S16 * 16

# ---- 和音（ルート音名, 構成音 MIDI） ----
CH = {
    'Dm': [50, 53, 57], 'Bb': [46, 50, 53], 'C': [48, 52, 55], 'Am': [45, 48, 52],
    'Gm': [43, 46, 50], 'A': [45, 49, 52], 'F': [41, 45, 48], 'Asus': [45, 50, 52],
}
INTRO = ['Dm', 'Dm', 'Bb', 'C']
A_ = ['Dm', 'Bb', 'C', 'Am', 'Dm', 'Bb', 'Gm', 'A']
B_ = ['Bb', 'C', 'Dm', 'Dm', 'Bb', 'C', 'A', 'A']
C_ = ['Gm', 'A', 'Dm', 'Bb', 'Gm', 'Asus', 'A', 'A']
SECTIONS = [('intro', INTRO), ('A', A_), ('A2', A_), ('B', B_), ('C', C_)]

# ---- メロディ（MIDI, 16分の長さ）  None=休符 ----
def n(name):
    names = {'C':0,'C#':1,'D':2,'Eb':3,'E':4,'F':5,'F#':6,'G':7,'Ab':8,'A':9,'Bb':10,'B':11}
    p, o = name[:-1], int(name[-1]); return 12 * (o + 1) + names[p]
def mel(s):
    out = []
    for tok in s.split():
        p, l = tok.split(':'); out.append((None if p == 'r' else n(p), int(l)))
    return out
MEL_A = mel("""D5:6 E5:2 F5:4 A5:4  G5:6 F5:2 D5:8  E5:6 F5:2 G5:4 C6:4  A5:12 G5:2 E5:2
F5:6 E5:2 D5:4 A4:4  Bb4:4 D5:4 F5:4 Bb5:4  A5:6 G5:2 F5:4 E5:4  E5:8 C#5:4 A4:4""")
MEL_A2 = mel("""D5:6 E5:2 F5:4 A5:4  G5:6 F5:2 D5:8  E5:6 F5:2 G5:4 C6:4  A5:12 C6:2 E6:2
F6:6 E6:2 D6:4 A5:4  Bb5:4 A5:4 G5:4 F5:4  G5:6 F5:2 E5:4 G5:4  A5:12 r:4""")
MEL_B = mel("""F5:2 F5:2 Bb5:4 A5:4 F5:4  G5:2 G5:2 C6:4 Bb5:4 G5:4  A5:6 D6:2 C6:4 A5:4  D6:12 C6:2 A5:2
Bb5:4 A5:4 G5:4 F5:4  G5:4 F5:4 E5:4 C5:4  C#6:8 A5:8  E6:12 r:4""")
MEL_C = mel("""Bb5:8 A5:8  C#6:8 E5:8  D6:8 F5:8  D6:8 F6:8
G5:4 Bb5:4 D6:4 G6:4  A6:16  E6:8 C#6:8  A5:4 r:12""")

def lead_note(freq, dur, duty=0.25, vib=0.25):
    nn = int(dur * SR)
    ph = phase(freq, nn + int(0.25 * SR), vib=vib)
    env = env_adsr(nn, 0.008, 0.12, 0.75, 0.22)
    m = min(len(ph), len(env))
    w = additive_pulse(ph[:m], freq, duty) * 0.65 + additive_saw(ph[:m] * 1.003, freq, 24) * 0.35
    return w * env[:m]

def render():
    bars = sum(len(c) for _, c in SECTIONS)
    tr = Track(bars * BAR)
    rng = np.random.default_rng(7)
    t0 = 0.0
    for sec, chords in SECTIONS:
        for bi, cname in enumerate(chords):
            bt = t0 + bi * BAR
            notes = CH[cname]; root = notes[0]
            # --- パッド（ふくらみのある和音） ---
            for k, m in enumerate(notes + [notes[0] + 12]):
                f = mtof(m + 12); nn = int(BAR * SR)
                env = env_adsr(nn, 0.25 if sec in ('intro', 'C') else 0.05, 0.3, 0.8, 0.4)
                for det, pan in ((-0.08, -0.7), (0.0, 0.0), (0.08, 0.7)):
                    ph = phase(f * 2 ** (det / 12), len(env))
                    w = lowpass(naive_saw(ph + rng.random() * 6.28), 2200 if sec != 'C' else 1500) * env
                    tr.add('pad', bt, w, pan, 0.045)
            # --- ベース（8分のオクターブ跳躍） ---
            if sec != 'intro' or bi >= 2:
                for i in range(8):
                    m = root - 12 + (12 if i % 2 else 0)
                    if sec == 'C' and bi < 4: m = root - 12
                    f = mtof(m); nn = int(S16 * 2 * SR * 0.9)
                    ph = phase(f, nn + 2000)
                    env = env_adsr(nn, 0.004, 0.08, 0.6, 0.04)[:len(ph)]
                    w = lowpass(additive_pulse(ph[:len(env)], f, 0.5, 16) * 0.6 + additive_saw(ph[:len(env)], f, 20) * 0.6, 1400) * env
                    tr.add('bass', bt + i * S16 * 2, w, 0, 0.42)
            # --- アルペジオ（16分、左右に振る） ---
            pat = [0, 1, 2, 3, 2, 1, 0, 1, 2, 3, 2, 1, 0, 1, 2, 3]
            tones = [notes[0] + 24, notes[1] + 24, notes[2] + 24, notes[0] + 36]
            for i, p in enumerate(pat):
                f = mtof(tones[p]); nn = int(S16 * SR)
                ph = phase(f, nn + 3000)
                env = env_adsr(nn, 0.002, 0.07, 0.25, 0.05)[:len(ph)]
                w = additive_pulse(ph[:len(env)], f, 0.125, 14) * env
                g = 0.10 if sec in ('A', 'A2', 'B') else 0.14
                tr.add('arp', bt + i * S16, w, -0.5 if i % 2 else 0.5, g)
            # --- ドラム ---
            for i in range(16):
                st = bt + i * S16
                kick = (i in (0, 8)) or (i in (6, 14) and sec != 'intro') or (sec == 'B' and i % 4 == 0)
                if sec == 'intro' and bi < 2: kick = i == 0
                if kick:
                    nn = int(0.25 * SR); tt = np.arange(nn) / SR
                    f = 45 + 110 * np.exp(-tt * 30)
                    w = np.sin(2 * np.pi * np.cumsum(f) / SR) * np.exp(-tt * 9)
                    w[:60] += rng.standard_normal(60) * 0.3
                    tr.add('drums', st, w, 0, 0.75)
                snare = i in (4, 12) and not (sec == 'intro' and bi < 2)
                last_bar = bi == len(chords) - 1
                if last_bar and i >= 12 and sec != 'intro': snare = True   # フィル
                if sec == 'intro' and bi == 3 and i >= 8 and i % 2 == 0: snare = True
                if snare:
                    nn = int(0.22 * SR); tt = np.arange(nn) / SR
                    w = highpass(rng.standard_normal(nn), 1200) * np.exp(-tt * 16) * 0.7
                    w += np.sin(2 * np.pi * 185 * tt) * np.exp(-tt * 25) * 0.5
                    tr.add('drums', st, w, 0.1, 0.42 if not (last_bar and i >= 12) else 0.3 + 0.04 * (i - 12))
                if i % 2 == 0 or sec == 'B':
                    if not (sec == 'intro' and bi < 2):
                        nn = int(0.05 * SR); tt = np.arange(nn) / SR
                        w = highpass(rng.standard_normal(nn), 7000) * np.exp(-tt * 70)
                        tr.add('drums', st, w, -0.35, 0.16 if i % 4 == 2 else 0.1)
            if bi == 0 and sec != 'intro':   # クラッシュ
                nn = int(1.6 * SR); tt = np.arange(nn) / SR
                w = highpass(rng.standard_normal(nn), 4000) * np.exp(-tt * 2.5)
                tr.add('drums', bt, w, 0.4, 0.18)
        # --- メロディ ---
        m = {'A': MEL_A, 'A2': MEL_A2, 'B': MEL_B, 'C': MEL_C}.get(sec)
        if m:
            pos = 0
            for p, l in m:
                if p is not None:
                    f = mtof(p)
                    w = lead_note(f, l * S16 * 0.95, duty=0.25 if sec != 'C' else 0.5)
                    tr.add('lead', t0 + pos * S16, w, 0.0, 0.24)
                    if sec in ('A2', 'B'):        # 1オクターブ下でユニゾン
                        w2 = lead_note(f / 2, l * S16 * 0.95, duty=0.5, vib=0.15)
                        tr.add('lead2', t0 + pos * S16, w2, 0.0, 0.12)
                pos += l
        t0 += len(chords) * BAR

    # ---- ミックス ----
    N = tr.n
    def get(k): return tr.buses.get(k, np.zeros((N + SR * 4, 2)))
    lead = (get('lead') + get('lead2')) * 1.5
    # ディレイ（付点8分、左右ピンポン）
    d = int(S16 * 3 * SR); dl = np.zeros_like(lead)
    for k, g in ((1, 0.32), (2, 0.18), (3, 0.1)):
        ch = k % 2
        dl[d * k:, ch] += lead[:-d * k, 0 if ch == 1 else 1] * g if d * k < len(lead) else 0
    dry = lead + dl * 0.8 + get('pad') * 1.2 + get('bass') * 0.55 + get('arp') * 1.3 + get('drums') * 0.62
    ir = reverb_ir()
    send = lead * 0.35 + get('pad') * 0.5 + get('arp') * 0.4 + get('drums') * 0.12
    wet = np.stack([fftconvolve(send[:, c], ir[:, c]) for c in range(2)], axis=1)
    full = np.zeros((max(len(dry), len(wet)), 2))
    full[:len(dry)] += dry; full[:len(wet)] += wet * 0.55
    mix = fold_loop(full, N)
    mix = np.tanh(mix * 1.1)
    mix /= np.abs(mix).max() / 0.92
    return mix

if __name__ == '__main__':
    import wave
    mix = render()
    out = sys.argv[1] if len(sys.argv) > 1 else 'spirit_p5.wav'
    pcm = (mix * 32767).astype('<i2')
    with wave.open(out, 'wb') as w:
        w.setnchannels(2); w.setsampwidth(2); w.setframerate(SR); w.writeframes(pcm.tobytes())
    print(out, len(mix) / SR, 's')
