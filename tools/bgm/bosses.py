"""ボスごとのピコピコ曲。python3 bosses.py fire_knight ice_witch ... で wav を出す"""
import sys
from chipsong import render, write_wav, SR

SONGS = {}

SONGS['fire_knight'] = dict(name='紅蓮の騎士', bpm=160,
    chords={'Am': [45, 48, 52], 'F': [41, 45, 48], 'G': [43, 47, 50], 'E': [40, 44, 47], 'Dm': [38, 41, 45], 'C': [36, 40, 43]},
    arp=([0, 1, 2, 3], 2, 2), pad='power',
    sections=[
        dict(id='intro', chords=['Am', 'Am', 'F', 'E'], drums='gallop', bass='gallop', fill='snare', crash=False),
        dict(id='A', chords=['Am', 'F', 'G', 'Am', 'Am', 'F', 'E', 'E'], drums='gallop', bass='gallop', double=-12,
             mel="""A4:3 A4:1 C5:2 E5:2 A5:4 G5:2 E5:2  F5:6 E5:2 C5:4 A4:4  G4:3 B4:1 D5:2 G5:2 F5:4 D5:4  E5:12 r:4
A4:3 A4:1 C5:2 E5:2 A5:4 B5:2 C6:2  A5:6 G5:2 F5:4 C5:4  G#5:8 B5:4 G#5:4  E5:16"""),
        dict(id='B', chords=['Dm', 'C', 'F', 'E', 'Dm', 'C', 'E', 'E'], drums='drive', bass='pump', double=-12, arp=([0, 1, 2, 3], 1, 2),
             mel="""D6:4 C6:2 A5:2 F5:4 A5:4  G5:4 E5:2 C5:2 E5:4 G5:4  A5:6 C6:2 F6:8  E6:8 D6:4 B5:4
D6:4 E6:2 F6:2 E6:4 D6:4  C6:4 B5:2 A5:2 G5:4 E5:4  B5:4 C6:4 D6:4 E6:4  E6:12 r:4"""),
    ])

SONGS['ice_witch'] = dict(name='凍てつく円舞', bpm=150, steps=12, duty=0.125,
    chords={'F#m': [42, 45, 49], 'D': [38, 42, 45], 'E': [40, 44, 47], 'C#': [37, 41, 44], 'Bm': [35, 38, 42]},
    arp=([0, 1, 2, 3, 2, 1], 2, 3), arp_gain=0.11,
    sections=[
        dict(id='intro', chords=['F#m', 'D', 'F#m', 'C#'], drums='light', bass='waltz', fill='snare', crash=False, pad_attack=0.3),
        dict(id='A', chords=['F#m', 'D', 'E', 'C#', 'F#m', 'D', 'Bm', 'C#'], drums='waltz', bass='waltz',
             mel="""C#6:4 B5:2 A5:2 F#5:4  A5:6 F#5:2 D5:4  B5:4 G#5:2 E5:2 B5:4  G#5:8 F5:4
C#6:4 D6:2 C#6:2 A5:4  F#6:6 E6:2 D6:4  D6:4 C#6:2 B5:2 F#5:4  F5:4 G#5:4 C#6:4"""),
        dict(id='B', chords=['Bm', 'C#', 'F#m', 'D', 'Bm', 'C#', 'F#m', 'F#m'], drums='waltz2', bass='waltz', double=-12, duty=0.25,
             mel="""F#6:6 E6:2 D6:4  F6:6 D#6:2 C#6:4  C#6:4 A5:4 F#5:4  A5:8 B5:2 C#6:2
D6:4 F#6:4 B6:4  G#6:8 F6:4  F#6:12  C#6:4 r:8"""),
    ])

SONGS['zombie_king'] = dict(name='腐王の行進', bpm=126, pad='power', bass_cutoff=900, snare_tone=150,
    chords={'Cm': [36, 39, 43], 'Ab': [44, 48, 51], 'Fm': [41, 44, 48], 'G': [43, 47, 50], 'Db': [37, 41, 44]},
    sections=[
        dict(id='intro', chords=['Cm', 'Cm', 'Db', 'Cm'], drums='march', bass='dotted', crash=False),
        dict(id='A', chords=['Cm', 'Ab', 'Fm', 'G', 'Cm', 'Ab', 'Db', 'G'], drums='march', bass='dotted', double=-12,
             arp=([0, 2, 1, 2], 4, 2),
             mel="""C5:6 C5:2 Eb5:4 D5:4  C5:6 Bb4:2 Ab4:8  F4:4 Ab4:4 C5:4 F5:4  D5:8 B4:8
C5:6 C5:2 G5:4 F5:4  Eb5:6 D5:2 C5:8  Db5:4 F5:4 Ab5:4 F5:4  G5:12 r:4"""),
        dict(id='B', chords=['Fm', 'G', 'Cm', 'Ab', 'Fm', 'Db', 'G', 'G'], drums='rock', bass='root8', double=-12,
             arp=([0, 1, 2, 1], 2, 2),
             mel="""Ab5:4 G5:2 F5:2 C5:8  B4:4 D5:4 G5:8  C6:6 Bb5:2 G5:4 Eb5:4  Ab5:12 G5:2 F5:2
F5:4 Ab5:4 C6:4 F6:4  F6:6 Eb6:2 Db6:8  D6:8 B5:8  G5:4 r:12"""),
    ])

SONGS['dark_mage'] = dict(name='深淵の術式', bpm=172,
    chords={'Em': [40, 43, 47], 'C': [36, 40, 43], 'B': [35, 39, 42], 'Am': [45, 48, 52], 'D#dim': [39, 42, 45], 'G': [43, 47, 50]},
    arp=([0, 2, 1, 3], 1, 2),
    sections=[
        dict(id='intro', chords=['Em', 'Em', 'C', 'B'], drums='drive', bass='riff16', pad=None, fill='snare', crash=False),
        dict(id='A', chords=['Em', 'C', 'Am', 'B', 'Em', 'C', 'D#dim', 'B'], drums='drive', bass='riff16', arp=([0, 1, 2, 3], 2, 2),
             mel="""E5:2 G5:2 B5:2 E6:2 D#6:2 B5:2 G5:2 F#5:2  E5:6 C5:2 G5:4 E5:4  A5:2 C6:2 E6:2 C6:2 A5:4 E5:4  D#5:8 F#5:4 B5:4
G5:3 F#5:3 E5:2 B5:4 E6:4  G6:6 E6:2 C6:8  A5:4 F#5:4 D#5:4 C6:4  B5:12 r:4"""),
        dict(id='B', chords=['Am', 'B', 'Em', 'C', 'Am', 'B', 'Em', 'Em'], drums='break', bass='pump', double=-12,
             mel="""C6:3 B5:3 A5:2 E6:8  D#6:3 C#6:3 B5:2 F#6:8  G6:4 F#6:4 E6:4 B5:4  C6:4 E6:4 G6:4 E6:4
A6:6 G6:2 F6:4 E6:4  D#6:6 E6:2 F#6:8  E6:16  B5:2 G5:2 E5:2 B4:2 r:8"""),
    ])

SONGS['blade_dragon'] = dict(name='影刃の咆哮', bpm=176, pad='power',
    chords={'C#m': [37, 40, 44], 'A': [45, 49, 52], 'B': [35, 39, 42], 'G#': [44, 48, 51], 'F#m': [42, 45, 49], 'E': [40, 44, 47]},
    arp=([0, 2, 1, 3], 1, 2),
    sections=[
        dict(id='intro', chords=['C#m', 'C#m', 'A', 'G#'], drums='drive', bass='riff16', fill='snare', crash=False),
        dict(id='A', chords=['C#m', 'A', 'B', 'G#', 'C#m', 'A', 'F#m', 'G#'], drums='drive', bass='riff16', double=-12,
             mel="""C#5:2 C#5:2 r:2 E5:2 G#5:4 C#6:4  B5:2 A5:2 r:2 E5:2 A5:8  F#5:2 F#5:2 r:2 B5:2 D#6:4 B5:4  C6:12 r:4
E6:4 D#6:2 C#6:2 G#5:4 E5:4  A5:4 B5:2 C#6:2 E6:8  F#6:4 E6:2 C#6:2 A5:4 F#5:4  G#5:8 C6:8"""),
        dict(id='B', chords=['A', 'B', 'C#m', 'E', 'A', 'B', 'G#', 'G#'], drums='break', bass='pump', double=-12,
             mel="""C#6:6 B5:2 A5:8  D#6:6 C#6:2 B5:8  E6:4 G#6:12  G#6:4 F#6:4 E6:4 B5:4
A5:2 C#6:2 E6:2 A6:2 G#6:4 E6:4  F#6:2 D#6:2 B5:2 F#6:2 D#6:8  C6:4 D#6:4 G#6:8  G#6:12 r:4"""),
    ])

SONGS['white_fox'] = dict(name='九尾の灯', bpm=152, duty=0.125,
    chords={'D': [38, 42, 45], 'Bm': [35, 38, 42], 'G': [43, 47, 50], 'A': [45, 49, 52], 'Em': [40, 43, 47]},
    arp=([0, 1, 2, 3, 2, 1, 0, 1], 1, 3), arp_gain=0.09,
    sections=[
        dict(id='intro', chords=['D', 'Bm', 'G', 'A'], drums='light', bass='oct8', fill='snare', crash=False, pad_attack=0.3),
        dict(id='A', chords=['Bm', 'G', 'A', 'D', 'Bm', 'G', 'Em', 'A'], drums='rock', bass='oct8',
             mel="""F#5:4 A5:2 B5:2 A5:4 F#5:4  E5:6 D5:2 B4:8  E5:4 F#5:2 A5:2 B5:4 D6:4  A5:12 r:4
B5:4 D6:2 E6:2 D6:4 B5:4  A5:6 F#5:2 E5:8  D5:4 E5:2 F#5:2 A5:4 B5:4  E5:12 r:4"""),
        dict(id='B', chords=['G', 'A', 'Bm', 'D', 'G', 'A', 'Em', 'A'], drums='drive', bass='pump', double=-12, duty=0.25,
             mel="""D6:6 B5:2 A5:8  E6:6 D6:2 B5:8  F#6:4 E6:2 D6:2 B5:4 A5:4  A5:4 B5:4 D6:8
B5:2 D6:2 E6:2 G6:2 F#6:4 E6:4  E6:6 F#6:2 A6:8  B6:8 A6:4 F#6:4  E6:12 r:4"""),
    ])

SONGS['wind_cat'] = dict(name='疾風の猫', bpm=180,
    chords={'G': [43, 47, 50], 'F': [41, 45, 48], 'C': [36, 40, 43], 'Em': [40, 43, 47], 'D': [38, 42, 45], 'Am': [45, 48, 52]},
    arp=([0, 1, 2, 3], 1, 2),
    sections=[
        dict(id='intro', chords=['G', 'F', 'G', 'F'], drums='drive', bass='oct8', fill='snare', crash=False),
        dict(id='A', chords=['G', 'F', 'C', 'G', 'Em', 'C', 'D', 'D'], drums='rock', bass='oct8', arp=([0, 1, 2, 3], 2, 2),
             mel="""D5:2 G5:2 B5:2 D6:2 C6:4 B5:4  A5:2 F5:2 C5:2 F5:2 A5:4 C6:4  G5:4 E5:2 C5:2 E5:4 G5:4  B5:6 A5:2 G5:8
G5:2 B5:2 E6:4 D6:2 B5:2 G5:4  E5:4 G5:4 C6:4 E6:4  F#6:6 E6:2 D6:8  A5:4 B5:4 C6:4 D6:4"""),
        dict(id='B', chords=['Am', 'D', 'G', 'Em', 'C', 'D', 'G', 'G'], drums='break', bass='pump', double=-12,
             mel="""E6:2 E6:2 r:2 E6:2 D6:4 C6:4  F#6:2 F#6:2 r:2 F#6:2 E6:4 D6:4  G6:4 D6:4 B5:4 G5:4  E6:8 B5:8
C6:2 E6:2 G6:2 E6:2 C6:2 E6:2 G6:4  F#6:2 A6:2 F#6:2 D6:2 A5:4 F#6:4  G6:12 F6:2 D6:2  G6:4 r:12"""),
    ])

SONGS['thunder_lion'] = dict(name='雷鳴の王獣', bpm=164, pad='power',
    chords={'Bm': [35, 38, 42], 'G': [43, 47, 50], 'A': [45, 49, 52], 'F#': [42, 46, 49], 'Em': [40, 43, 47], 'D': [38, 42, 45]},
    arp=([0, 1, 2, 3], 2, 2),
    sections=[
        dict(id='intro', chords=['Bm', 'Bm', 'G', 'F#'], drums='rock', bass='pump', crash=False),
        dict(id='A', chords=['Bm', 'G', 'A', 'F#', 'Bm', 'G', 'Em', 'F#'], drums='rock', bass='pump', double=-12,
             mel="""B4:4 D5:4 F#5:4 B5:4  B5:6 A5:2 G5:8  A5:4 C#6:4 E6:4 C#6:4  A#5:12 r:4
B5:2 B5:2 r:2 D6:2 F#6:4 D6:4  G6:6 F#6:2 D6:8  E6:4 D6:2 B5:2 G5:4 E5:4  F#5:8 A#5:8"""),
        dict(id='B', chords=['G', 'A', 'Bm', 'D', 'G', 'A', 'F#', 'F#'], drums='drive', bass='riff16', double=-12, arp=([0, 1, 2, 3], 1, 2),
             mel="""D6:4 E6:4 G6:8  E6:4 F#6:4 A6:8  B6:6 A6:2 F#6:4 D6:4  F#6:12 E6:2 D6:2
B5:4 D6:4 G6:4 B6:4  A6:6 G6:2 E6:8  F#6:4 A#6:12  F#6:12 r:4"""),
    ])

SONGS['rock_giant'] = dict(name='大地の巨像', bpm=120, pad='power', bass_cutoff=900, snare_tone=150,
    chords={'Gm': [43, 46, 50], 'Eb': [39, 43, 46], 'F': [41, 45, 48], 'D': [38, 42, 45], 'Cm': [36, 39, 43]},
    arp=([0, 2, 1, 2], 2, 2),
    sections=[
        dict(id='intro', chords=['Gm', 'Gm', 'Eb', 'D'], drums='half', bass='whole', crash=False),
        dict(id='A', chords=['Gm', 'Eb', 'F', 'D', 'Gm', 'Eb', 'Cm', 'D'], drums='half', bass='dotted', double=-12,
             mel="""G4:6 Bb4:2 D5:8  Eb5:6 D5:2 Bb4:8  C5:4 D5:4 F5:4 A5:4  F#5:12 r:4
G5:6 F5:2 D5:4 Bb4:4  G5:4 Bb5:4 Eb6:8  D6:4 C6:2 Bb5:2 G5:4 Eb5:4  D5:8 F#5:8"""),
        dict(id='B', chords=['Cm', 'D', 'Gm', 'Eb', 'Cm', 'D', 'D', 'D'], drums='rock', bass='root8', double=-12,
             mel="""Eb6:6 D6:2 C6:8  D6:6 C6:2 A5:8  Bb5:4 D6:4 G6:8  G6:6 F6:2 Eb6:8
C6:4 Eb6:4 G6:8  F#6:6 E6:2 D6:8  A6:16  D6:4 r:12"""),
    ])

SONGS['ender_sentinel'] = dict(name='虚空の眼', bpm=140, duty=0.5,
    chords={'Fm': [41, 44, 48], 'Db': [37, 41, 44], 'Bdim': [35, 38, 41], 'C': [36, 40, 43], 'Gb': [42, 46, 49], 'Bbm': [46, 49, 53]},
    arp=([0, 2, 1, 3, 2, 0], 2, 3), arp_gain=0.09,
    sections=[
        dict(id='intro', chords=['Fm', 'Gb', 'Fm', 'Gb'], drums='light', bass='whole', fill='snare', crash=False, pad_attack=0.5),
        dict(id='A', chords=['Fm', 'Db', 'Bbm', 'C', 'Fm', 'Gb', 'Bdim', 'C'], drums='rock', bass='oct8',
             mel="""C6:6 B5:2 C6:4 Ab5:4  F5:8 Ab5:4 Db6:4  Db6:6 C6:2 Bb5:4 F5:4  E5:12 r:4
F5:4 Ab5:4 C6:4 F6:4  Gb6:6 F6:2 Db6:8  D6:4 F6:4 Ab6:4 F6:4  E6:8 G6:8"""),
        dict(id='B', chords=['Db', 'Gb', 'Fm', 'C', 'Db', 'Gb', 'C', 'C'], drums='drive', bass='riff16', double=-12, arp=([0, 1, 2, 3], 1, 2),
             mel="""Ab5:2 Db6:2 F6:4 Ab6:8  Bb6:6 Ab6:2 Gb6:8  F6:4 C6:4 Ab5:4 F5:4  G5:4 C6:4 E6:8
F6:6 Eb6:2 Db6:8  Db6:4 Gb6:4 Bb6:8  B6:8 G6:4 E6:4  C6:12 r:4"""),
    ])

SONGS['creeper_mother'] = dict(name='翠の爆母', bpm=148,
    chords={'G#m': [44, 47, 51], 'E': [40, 44, 47], 'F#': [42, 46, 49], 'C#m': [37, 40, 44], 'D#': [39, 43, 46]},
    arp=([0, 1, 2, 3], 2, 2),
    sections=[
        dict(id='intro', chords=['G#m', 'G#m', 'E', 'D#'], drums='light', bass='pump', pad=None, arp=([3], 1, 2), fill='snare', crash=False),
        dict(id='A', chords=['G#m', 'E', 'F#', 'D#', 'G#m', 'E', 'C#m', 'D#'], drums='rock', bass='pump',
             mel="""G#5:2 G#5:2 G#5:2 G#5:2 B5:4 D#6:4  E6:6 D#6:2 B5:8  A#5:2 A#5:2 A#5:2 A#5:2 C#6:4 F#6:4  G6:12 r:4
D#6:4 C#6:2 B5:2 G#5:4 D#5:4  E5:4 G#5:4 B5:4 E6:4  E6:6 D#6:2 C#6:8  D#6:8 G5:8"""),
        dict(id='B', chords=['E', 'F#', 'G#m', 'C#m', 'E', 'F#', 'D#', 'D#'], drums='break', bass='riff16', double=-12,
             mel="""B5:2 r:2 B5:2 r:2 E6:8  C#6:2 r:2 C#6:2 r:2 F#6:8  G#6:4 F#6:4 D#6:4 B5:4  C#6:6 D#6:2 E6:8
G#6:4 E6:4 B5:4 E6:4  A#6:4 F#6:4 C#6:4 F#6:4  G6:2 G6:2 G6:2 G6:2 G6:2 G6:2 G6:2 G6:2  D#6:4 r:12"""),
    ])

SONGS['fenrir'] = dict(name='鎖断の狼', bpm=200, steps=12, pad='power',
    chords={'Am': [45, 48, 52], 'G': [43, 47, 50], 'D': [38, 42, 45], 'Em': [40, 43, 47], 'F': [41, 45, 48], 'E': [40, 44, 47]},
    arp=([0, 1, 2, 3, 2, 1], 2, 2),
    sections=[
        dict(id='intro', chords=['Am', 'Am', 'G', 'E'], drums='six8', bass='dotted', fill='snare', crash=False),
        dict(id='A', chords=['Am', 'G', 'D', 'Am', 'Am', 'G', 'F', 'E'], drums='gallop8', bass='dotted', double=-12,
             mel="""A5:4 E5:2 A5:4 B5:2  C6:4 B5:2 G5:4 D5:2  F#5:4 A5:2 D6:4 C6:2  A5:12
E6:4 D6:2 C6:4 B5:2  D6:4 C6:2 B5:4 G5:2  A5:4 C6:2 F6:4 E6:2  E6:6 G#5:6"""),
        dict(id='B', chords=['F', 'G', 'Am', 'Em', 'F', 'G', 'E', 'E'], drums='gallop8', bass='dotted', double=-12, arp=([0, 1, 2, 3], 1, 2),
             mel="""C6:6 F6:6  D6:6 G6:6  E6:4 D6:2 C6:4 A5:2  B5:6 E6:6
F6:4 E6:2 C6:4 A5:2  G6:4 F#6:2 D6:4 B5:2  G#6:6 B6:6  E6:6 r:6"""),
        dict(id='C', chords=['Am', 'F', 'G', 'E', 'Am', 'F', 'E', 'E'], drums='six8', bass='dotted', double=-12, crash=True,
             mel="""A6:12  A6:6 G6:6  G6:6 D6:6  E6:12
C6:4 E6:2 A6:4 G6:2  F6:4 E6:2 C6:4 A5:2  B5:4 D6:2 G#6:4 E6:2  E6:6 r:6"""),
    ])

SONGS['iron_spider'] = dict(name='機甲の八脚', bpm=138, duty=0.5, bass_cutoff=1800,
    chords={'F#m': [42, 45, 49], 'G': [43, 47, 50], 'D': [38, 42, 45], 'C#': [37, 41, 44], 'Bm': [35, 38, 42], 'A': [45, 49, 52]},
    arp=([0, 3, 1, 2], 1, 2),
    sections=[
        dict(id='intro', chords=['F#m', 'G', 'F#m', 'G'], drums='drive', bass='riff16', pad=None, fill='snare', crash=False),
        dict(id='A', chords=['F#m', 'G', 'D', 'C#', 'F#m', 'G', 'Bm', 'C#'], drums='drive', bass='riff16',
             mel="""F#5:2 F#5:2 F#6:2 F#5:2 E6:4 C#6:4  D6:2 D6:2 G6:2 D6:2 B5:4 G5:4  A5:2 A5:2 D6:2 A5:2 F#6:4 D6:4  F6:8 G#5:8
A5:4 C#6:4 F#6:4 A6:4  G6:6 F#6:2 D6:8  F#6:4 D6:2 B5:2 F#5:4 B5:4  C#6:12 r:4"""),
        dict(id='B', chords=['Bm', 'C#', 'F#m', 'D', 'G', 'A', 'C#', 'C#'], drums='break', bass='pump', double=-12,
             mel="""B5:2 D6:2 F#6:2 B6:2 A6:4 F#6:4  G#6:2 F6:2 C#6:2 G#5:2 C#6:8  A6:6 G#6:2 F#6:8  F#6:4 A6:4 D6:8
B6:4 G6:4 D6:4 B5:4  C#6:4 E6:4 A6:8  G#6:2 G#6:2 G#6:2 G#6:2 F6:4 C#6:4  C#6:4 r:12"""),
    ])

SONGS['fallen_valkyrie'] = dict(name='堕天の戦乙女', bpm=170,
    chords={'Ebm': [39, 42, 46], 'Cb': [35, 39, 42], 'Db': [37, 41, 44], 'Bb': [46, 50, 53], 'Abm': [44, 47, 51], 'Gb': [42, 46, 49]},
    arp=([0, 1, 2, 3], 1, 2),
    sections=[
        dict(id='intro', chords=['Ebm', 'Cb', 'Ebm', 'Bb'], drums='light', bass='whole', fill='snare', crash=False,
             pad_attack=0.4, arp=([0, 1, 2, 3], 2, 3)),
        dict(id='A', chords=['Ebm', 'Cb', 'Db', 'Bb', 'Ebm', 'Cb', 'Abm', 'Bb'], drums='drive', bass='oct8', double=-12,
             mel="""Eb5:4 Gb5:2 Bb5:2 Eb6:6 Db6:2  B5:6 Bb5:2 Gb5:8  Ab5:4 Bb5:2 Db6:2 F6:4 Db6:4  D6:12 r:4
Gb6:4 F6:2 Eb6:2 Bb5:4 Gb5:4  Ab5:4 B5:4 Eb6:4 Gb6:4  F6:6 Eb6:2 B5:8  Bb5:8 D6:8"""),
        dict(id='B', chords=['Cb', 'Db', 'Ebm', 'Gb', 'Cb', 'Db', 'Bb', 'Bb'], drums='break', bass='pump', double=-12,
             mel="""Gb6:6 F6:2 Eb6:8  F6:6 Eb6:2 Db6:8  Eb6:4 Gb6:4 Bb6:8  Bb6:6 Ab6:2 Gb6:8
Eb6:2 Gb6:2 B6:4 Bb6:4 Gb6:4  F6:2 Ab6:2 F6:4 Db6:4 Ab5:4  D6:4 F6:4 Bb6:8  Bb6:12 r:4"""),
    ])

def _add_reprise():
    """最後にテーマ(A)をもう一度、Bの勢いのまま重ねて盛り上げる"""
    for song in SONGS.values():
        secs = song['sections']
        if any(x['id'] == 'A2' for x in secs): continue
        a = next(x for x in secs if x['id'] == 'A'); b = next(x for x in secs if x['id'] == 'B')
        a2 = dict(a); a2['id'] = 'A2'; a2['drums'] = b['drums']; a2['bass'] = b.get('bass', a.get('bass'))
        a2['double'] = -12; a2['duty'] = b.get('duty', song.get('duty', 0.25))
        secs.append(a2)
_add_reprise()

if __name__ == '__main__':
    import time
    for k in sys.argv[1:]:
        t = time.time(); mix = render(SONGS[k], seed=sum(map(ord, k)))
        write_wav(f'/tmp/bgm_{k}.wav', mix); print(k, round(len(mix) / SR, 1), 's', round(time.time() - t, 1))
