# JSN Boss

Minecraft 1.21.1 / NeoForge 向けのオリジナルボス MOD。モデルとアニメーションは GeckoLib。

## ボス

### 炎の騎士（`jsnboss:fire_knight`）

身長3.5ブロック、HP 1500。燃える大剣を持った黒鉄の騎士。HP が半分（750）を切ると第2形態になり、炎が青く変わる。

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

### 氷の魔女（`jsnboss:ice_witch`）

身長約3ブロック、HP 1500。少し浮いて移動し、7〜12ブロックの距離を保って魔法で戦う。近づくとテレポートで逃げる。HP が半分（750）を切ると第2形態「吹雪」になり、氷の光がオーロラ紫に変わる。

| 技 | 第1形態 | 第2形態 |
|---|---|---|
| 氷の槍 | 9×3連射 | 12×3連射（3方向） |
| 氷のトゲ | 12（足元に1本） | 15（魔女から奥へトゲの列） |
| 凍てつく大地 | 半径5、8秒 | 半径7、8秒 |
| 氷の爆発（近づかれたとき） | 12 | 16 |
| テレポート（消えるときに足元で） | 8 | 10 |
| 雹 | — | 12×8発（地面に予告） |
| 分身 | — | 3人に分かれる。分身はHP1で弱い槍だけ。**本物だけ周りに結晶が回っている** |
| 絶対零度（HP40%以下） | — | 22（半径14。雪の輪の外へ逃げれば当たらない） |

- 氷の攻撃に当たると「凍える」（粉雪に埋まったときと同じ）。溜まると動きが鈍り、凍結ダメージを受ける。**革防具を着ていると凍らない**
- 第2形態の近くでは吹雪で遠くが見えにくくなる（離れると晴れる）

### ゾンビの王（`jsnboss:zombie_king`）

身長約4ブロック、HP 2000。重くて遅いが一撃が重い。手下のゾンビが生きている間は1体につき毎秒2回復する。HP が半分（1000）を切ると第2形態「不死の王」になり、目と宝石が緑に光る。第2形態で倒されると一度だけ HP 500 で復活する（実質 HP 2500）。

| 技 | 第1形態 | 第2形態 |
|---|---|---|
| 王笏の振り下ろし | 18 | 22 |
| 薙ぎ払い | 13 | 17 |
| 衝撃波（地面を走る輪、ジャンプで避ける） | 12（1回） | 15（2回） |
| 手下召喚（最大6体） | ゾンビ3体 | 鉄の鎧と剣の近衛兵4体 |
| 腐毒のブレス（前方8ブロック） | — | 0.25秒ごとに3＋毒II |
| 死者の手（全員の足元、予告0.9秒） | — | 10＋1.5秒ほぼ動けない |
| 復活の衝撃（半径7） | — | 10 |

## 最大HPの上限について

バニラの最大HPは1024が上限なので、この MOD は起動時に上限を100000まで引き上げている（`BossSetup.raiseMaxHealthCap()`、`META-INF/accesstransformer.cfg`）。ほかのモブのHPは変わらない。

## 遊び方

```
./gradlew runClient
```

- クリエイティブの「スポーンエッグ」タブに各ボスのスポーンエッグ
- コマンド：`/summon jsnboss:fire_knight`、`/summon jsnboss:ice_witch`、`/summon jsnboss:zombie_king`
- 第2形態をすぐ見たいとき：`/data merge entity @e[type=jsnboss:fire_knight,limit=1] {Health:740f}`（ice_witch も同じ、zombie_king は 990f）

配布用の jar は `./gradlew build` で `build/libs/` にできる。GitHub に push すると Actions が自動でビルドする（jar は Actions の実行結果からダウンロードできる）。

## ファイルの場所

| 内容 | 場所 |
|---|---|
| ボス共通の土台 | `src/main/java/com/chigusa/jsnboss/entity/AbstractBoss.java` |
| ボスの AI・技 | `entity/FireKnightEntity.java`、`entity/IceWitchEntity.java`、`entity/ZombieKingEntity.java` |
| 火の玉・斬撃・火柱 | `src/main/java/com/chigusa/jsnboss/entity/` |
| モデル | `src/main/resources/assets/jsnboss/geo/entity/fire_knight.geo.json` |
| アニメーション | `src/main/resources/assets/jsnboss/animations/entity/fire_knight.animation.json` |
| テクスチャ（第1/第2形態、発光マスク） | `src/main/resources/assets/jsnboss/textures/entity/` |

`tools/` にはモデル・テクスチャ・アニメーションを作り直すための Python スクリプトが入っている（ゲームには関係しない）。モデルとアニメーションは Blockbench（GeckoLib プラグイン）で開いて直接いじってもOK。
