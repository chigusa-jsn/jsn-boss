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
