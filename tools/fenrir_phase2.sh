#!/bin/sh
# フェンリル：第2形態（金色・赤い目）のテクスチャを作ってから、第1形態（銀色）を作り直す。
# python3 で同じ形・同じ UV のまま色だけ変えて塗る。
set -e
T=src/main/resources/assets/jsnboss/textures/entity
GOLD=1 python3 tools/fenrir_gen.py
mv $T/fenrir.png $T/fenrir_phase2.png
mv $T/fenrir_glowmask.png $T/fenrir_phase2_glowmask.png
python3 tools/fenrir_gen.py
