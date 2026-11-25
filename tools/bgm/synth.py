"""小さなシンセ。numpy で音を作って足し合わせる。"""
import numpy as np
from scipy.signal import butter, lfilter, fftconvolve

SR = 44100

def mtof(m): return 440.0 * 2 ** ((m - 69) / 12)

def env_adsr(n, a, d, s, r, sr=SR):
    a, d, r = int(a * sr), int(d * sr), int(r * sr)
    e = np.full(n + r, s, dtype=np.float64)
    a = max(1, min(a, n)); e[:a] = np.linspace(0, 1, a)
    dd = max(1, min(d, n - a));
    if n - a > 0: e[a:a + dd] = np.linspace(1, s, dd)
    e[n:] = np.linspace(e[n - 1] if n > 0 else s, 0, r)
    return e

def phase(freq, n, vib=0.0, vib_rate=5.5, vib_delay=0.15):
    t = np.arange(n) / SR
    f = np.full(n, freq)
    if vib:
        depth = np.clip((t - vib_delay) / 0.2, 0, 1) * vib
        f = f * 2 ** (depth * np.sin(2 * np.pi * vib_rate * t) / 12)
    return 2 * np.pi * np.cumsum(f) / SR

def additive_pulse(ph, freq, duty=0.25, maxh=24):
    out = np.zeros_like(ph)
    for k in range(1, maxh + 1):
        if k * freq > SR * 0.45: break
        a = np.sin(np.pi * k * duty) / k
        out += a * np.cos(k * ph)
    return out * 1.2

def additive_saw(ph, freq, maxh=40):
    out = np.zeros_like(ph)
    for k in range(1, maxh + 1):
        if k * freq > SR * 0.45: break
        out += ((-1) ** (k + 1)) * np.sin(k * ph) / k
    return out * 0.6

def tri(ph):
    return 2 / np.pi * np.arcsin(np.sin(ph))

def naive_saw(ph):
    return ((ph / (2 * np.pi)) % 1.0) * 2 - 1

def lowpass(x, fc, order=2):
    b, a = butter(order, fc / (SR / 2), 'low'); return lfilter(b, a, x)

def highpass(x, fc, order=2):
    b, a = butter(order, fc / (SR / 2), 'high'); return lfilter(b, a, x)

def reverb_ir(seconds=2.2, decay=3.0, seed=1):
    rng = np.random.default_rng(seed)
    n = int(seconds * SR); t = np.arange(n) / SR
    ir = np.zeros((n, 2))
    for c in range(2):
        noise = rng.standard_normal(n)
        noise = lowpass(noise, 6000)
        ir[:, c] = noise * np.exp(-decay * t)
    ir[:int(0.012 * SR)] = 0
    return ir / np.sqrt((ir ** 2).sum(axis=0))

class Track:
    def __init__(self, seconds):
        self.n = int(seconds * SR)
        self.buses = {}
    def bus(self, name):
        if name not in self.buses: self.buses[name] = np.zeros((self.n + SR * 4, 2))
        return self.buses[name]
    def add(self, name, start_s, mono, pan=0.0, gain=1.0):
        b = self.bus(name); i = int(start_s * SR)
        L = np.cos((pan + 1) * np.pi / 4); R = np.sin((pan + 1) * np.pi / 4)
        seg = mono[:max(0, b.shape[0] - i)]
        b[i:i + len(seg), 0] += seg * L * gain * 1.414
        b[i:i + len(seg), 1] += seg * R * gain * 1.414
    def add_st(self, name, start_s, st, gain=1.0):
        b = self.bus(name); i = int(start_s * SR)
        seg = st[:max(0, b.shape[0] - i)]
        b[i:i + len(seg)] += seg * gain

def fold_loop(x, n):
    """ループ用：終わりからはみ出た余韻を頭に足し戻す"""
    out = x[:n].copy(); tail = x[n:]
    k = 0
    while k < len(tail):
        seg = tail[k:k + n]; out[:len(seg)] += seg; k += n
    return out

# ---------------------------------------------------------------- 楽器
NOTE = {'C':0,'C#':1,'Db':1,'D':2,'D#':3,'Eb':3,'E':4,'F':5,'F#':6,'Gb':6,'G':7,'G#':8,'Ab':8,'A':9,'A#':10,'Bb':10,'B':11}
def nn_(name):
    p, o = name[:-1], int(name[-1]); return 12 * (o + 1) + NOTE[p]
def mel(s):
    out = []
    for tok in s.split():
        p, l = tok.split(':'); out.append((None if p == 'r' else nn_(p), int(l)))
    return out

def bandpass(x, lo, hi, order=2):
    b, a = butter(order, [lo / (SR / 2), hi / (SR / 2)], 'band'); return lfilter(b, a, x)

def pluck(freq, dur, bright=1.0):
    n = int((dur + 0.6) * SR); t = np.arange(n) / SR
    ph = 2 * np.pi * freq * t; out = np.zeros(n)
    for k in range(1, 14):
        if k * freq > SR * 0.45: break
        out += np.sin(k * ph) / k ** (1.6 - 0.4 * bright) * np.exp(-t * (1.2 + 0.9 * k))
    out *= np.minimum(1, t / 0.002)
    return out * 0.7

def bell(freq, dur, decay=2.2, index=2.5, ratio=3.5):
    n = int((dur + 1.5) * SR); t = np.arange(n) / SR
    I = index * np.exp(-t * 3)
    out = np.sin(2 * np.pi * freq * t + I * np.sin(2 * np.pi * freq * ratio * t))
    out += 0.3 * np.sin(2 * np.pi * freq * 2.76 * t) * np.exp(-t * 5)
    return out * np.exp(-t * decay) * np.minimum(1, t / 0.002) * 0.6

def flute(freq, dur, rng):
    nn = int(dur * SR)
    env = env_adsr(nn, 0.06, 0.1, 0.85, 0.18)
    ph = phase(freq, len(env), vib=0.18, vib_rate=5.0, vib_delay=0.25)
    w = tri(ph) * 0.8 + np.sin(2 * ph) * 0.25 + np.sin(3 * ph) * 0.06
    breath = bandpass(rng.standard_normal(len(env)), min(freq * 1.5, 9000), min(freq * 3, 15000)) * 0.12
    return (w + breath) * env

def horn(freq, dur):
    nn = int(dur * SR)
    env = env_adsr(nn, 0.07, 0.2, 0.8, 0.25)
    ph = phase(freq, len(env), vib=0.12, vib_rate=4.8, vib_delay=0.3)
    w = additive_saw(ph, freq, 18)
    return lowpass(w, 1500) * env * 1.3

def choir(freq, dur, rng, attack=0.4):
    nn = int(dur * SR)
    env = env_adsr(nn, attack, 0.3, 0.9, 0.6)
    out = np.zeros(len(env))
    for det in (-0.1, 0.0, 0.1):
        ph = phase(freq * 2 ** (det / 12), len(env), vib=0.08, vib_rate=4.5 + det * 5, vib_delay=0.0)
        out += naive_saw(ph + rng.random() * 6.28)
    out = bandpass(out, 600, 900) * 1.0 + bandpass(out, 1000, 1250) * 0.5 + lowpass(out, 400) * 0.4
    return out * env * 0.6

def taiko(rng, pitch=1.0):
    n = int(0.7 * SR); t = np.arange(n) / SR
    f = (55 + 45 * np.exp(-t * 18)) * pitch
    w = np.sin(2 * np.pi * np.cumsum(f) / SR) * np.exp(-t * 5)
    w += lowpass(rng.standard_normal(n), 700) * np.exp(-t * 25) * 0.6
    return w

def frame_drum(rng):
    n = int(0.3 * SR); t = np.arange(n) / SR
    f = 95 + 40 * np.exp(-t * 30)
    w = np.sin(2 * np.pi * np.cumsum(f) / SR) * np.exp(-t * 12)
    w += bandpass(rng.standard_normal(n), 300, 2000) * np.exp(-t * 35) * 0.25
    return w

def shaker(rng, length=0.07):
    n = int(length * SR); t = np.arange(n) / SR
    e = np.minimum(1, t / 0.015) * np.exp(-t * 45)
    return highpass(rng.standard_normal(n), 6000) * e

def master(tr, sends, levels, ir_sec=2.6, ir_decay=2.6, wet_gain=0.6, delay=None):
    N = tr.n
    def get(k): return tr.buses.get(k, np.zeros((N + SR * 4, 2)))
    dry = np.zeros((N + SR * 4, 2)); send = np.zeros_like(dry)
    for k, g in levels.items(): dry += get(k) * g
    for k, g in sends.items(): send += get(k) * g * levels.get(k, 1.0)
    if delay:
        name, d_s = delay; src = get(name) * levels.get(name, 1.0); d = int(d_s * SR)
        for k, g in ((1, 0.3), (2, 0.16), (3, 0.08)):
            ch = k % 2
            dry[d * k:, ch] += src[:-d * k, 1 - ch] * g
    ir = reverb_ir(ir_sec, ir_decay)
    wet = np.stack([fftconvolve(send[:, c], ir[:, c]) for c in range(2)], axis=1)
    full = np.zeros((max(len(dry), len(wet)), 2)); full[:len(dry)] += dry; full[:len(wet)] += wet * wet_gain
    mix = fold_loop(full, N)
    mix = np.tanh(mix * 1.1); mix /= np.abs(mix).max() / 0.92
    return mix

def write_wav(path, mix):
    import wave
    pcm = (np.clip(mix, -1, 1) * 32767).astype('<i2')
    with wave.open(path, 'wb') as w:
        w.setnchannels(2); w.setsampwidth(2); w.setframerate(SR); w.writeframes(pcm.tobytes())

# ---------------------------------------------------------------- ピコピコ（第5形態と同じ音色）
def chip_lead(freq, dur, duty=0.25, vib=0.25, attack=0.008, release=0.22):
    nn = int(dur * SR)
    ph = phase(freq, nn + int((release + 0.05) * SR), vib=vib)
    env = env_adsr(nn, attack, 0.12, 0.75, release)
    m = min(len(ph), len(env))
    w = additive_pulse(ph[:m], freq, duty) * 0.65 + additive_saw(ph[:m] * 1.003, freq, 24) * 0.35
    return w * env[:m]

def chip_arp(freq, dur, duty=0.125, sustain=0.25):
    nn = int(dur * SR); ph = phase(freq, nn + 3000)
    env = env_adsr(nn, 0.002, 0.07, sustain, 0.05)[:len(ph)]
    return additive_pulse(ph[:len(env)], freq, duty, 14) * env

def chip_bass(freq, dur, cutoff=1400):
    nn = int(dur * SR); ph = phase(freq, nn + 2000)
    env = env_adsr(nn, 0.004, 0.08, 0.6, 0.04)[:len(ph)]
    w = additive_pulse(ph[:len(env)], freq, 0.5, 16) * 0.6 + additive_saw(ph[:len(env)], freq, 20) * 0.6
    return lowpass(w, cutoff) * env

def chip_pad(freq, dur, rng, attack=0.05, cutoff=2200):
    nn = int(dur * SR); env = env_adsr(nn, attack, 0.3, 0.8, 0.4)
    out = np.zeros(len(env))
    for det in (-0.08, 0.0, 0.08):
        ph = phase(freq * 2 ** (det / 12), len(env))
        out += lowpass(naive_saw(ph + rng.random() * 6.28), cutoff)
    return out * env

def chip_kick(rng, low=45, punch=110, decay=9):
    nn = int(0.25 * SR); tt = np.arange(nn) / SR
    f = low + punch * np.exp(-tt * 30)
    w = np.sin(2 * np.pi * np.cumsum(f) / SR) * np.exp(-tt * decay)
    w[:60] += rng.standard_normal(60) * 0.3
    return w

def chip_snare(rng, tone=185):
    nn = int(0.22 * SR); tt = np.arange(nn) / SR
    w = highpass(rng.standard_normal(nn), 1200) * np.exp(-tt * 16) * 0.7
    return w + np.sin(2 * np.pi * tone * tt) * np.exp(-tt * 25) * 0.5

def chip_hat(rng):
    nn = int(0.05 * SR); tt = np.arange(nn) / SR
    return highpass(rng.standard_normal(nn), 7000) * np.exp(-tt * 70)

def chip_crash(rng):
    nn = int(1.6 * SR); tt = np.arange(nn) / SR
    return highpass(rng.standard_normal(nn), 4000) * np.exp(-tt * 2.5)

def chip_tom(rng, f0=140):
    nn = int(0.3 * SR); tt = np.arange(nn) / SR
    f = f0 * (0.7 + 0.5 * np.exp(-tt * 20))
    return np.sin(2 * np.pi * np.cumsum(f) / SR) * np.exp(-tt * 10)
