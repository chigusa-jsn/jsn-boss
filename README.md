# JSN Boss

Minecraft 1.21.1 / NeoForge 向けのオリジナルボス MOD。モデルとアニメーションは GeckoLib。

## ボス

### 炎の騎士（`jsnboss:fire_knight`）

身長3.5ブロック、HP 400。燃える大剣を持った黒鉄の騎士。HP が半分を切ると第2形態になり、炎が青く変わる。

| 技 | 第1形態 | 第2形態 | 使う距離 |
|---|---|---|---|
| 横なぎ | 12 | 16 | 近い |
| 3連撃 | — | 12・12・18 | 近い |
| 突進斬り | 15 | 20 | 中くらい |
| 跳躍叩きつけ | — | 22 | 中〜遠い |
| 火柱 | 10＋炎上（1本） | **必ず15**（3本） | どこでも |
| 火の玉 | 9＋炎上（1発） | 12＋蒼炎（3発、燃える地面が残る） | 遠い |
| 飛ぶ斬撃 | 10（横：ジャンプで避ける） | 14（縦→横の2連発） | 中〜遠い |

- 数字は難易度ノーマルの値。変えるときは `FireKnightEntity.java` の `dmg(第1形態, 第2形態)` を書き換える
- 青い火柱は防具・エンチャント・効果・盾・難易度を無視する（`data/minecraft/tags/damage_type/`）。不死のトーテムは効く
- 蒼炎：1秒ごとに1ダメージ。水に入っても消えない

## 遊び方

```
./gradlew runClient
```

- クリエイティブの「スポーンエッグ」タブに「炎の騎士のスポーンエッグ」
- コマンド：`/summon jsnboss:fire_knight`
- 第2形態をすぐ見たいとき：`/data merge entity @e[type=jsnboss:fire_knight,limit=1] {Health:190f}`

配布用の jar は `./gradlew build` で `build/libs/` にできる。GitHub に push すると Actions が自動でビルドする（jar は Actions の実行結果からダウンロードできる）。

## ファイルの場所

| 内容 | 場所 |
|---|---|
| ボスの AI・技 | `src/main/java/com/chigusa/jsnboss/entity/FireKnightEntity.java` |
| 火の玉・斬撃・火柱 | `src/main/java/com/chigusa/jsnboss/entity/` |
| モデル | `src/main/resources/assets/jsnboss/geo/entity/fire_knight.geo.json` |
| アニメーション | `src/main/resources/assets/jsnboss/animations/entity/fire_knight.animation.json` |
| テクスチャ（第1/第2形態、発光マスク） | `src/main/resources/assets/jsnboss/textures/entity/` |

`tools/` にはモデル・テクスチャ・アニメーションを作り直すための Python スクリプトが入っている（ゲームには関係しない）。モデルとアニメーションは Blockbench（GeckoLib プラグイン）で開いて直接いじってもOK。
