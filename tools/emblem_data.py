"""紋章システムのデータ（モデル・レシピ・Curios・ルート・言語）を書き出す。
python3 tools/emblem_data.py
"""
import json, os
from PIL import Image

ROOT = os.path.join(os.path.dirname(__file__), '..', 'src/main/resources')
NS = 'jsnboss'

# key, ボス, 中ボス, 日本語, 英語
SETS = [
    ('crimson', 'fire_knight', 'flame_squire', '紅蓮', 'Crimson'),
    ('frostbloom', 'ice_witch', 'frost_maiden', '氷華', 'Frostbloom'),
    ('corpse_king', 'zombie_king', 'rotting_guard', '屍王', 'Corpse King'),
    ('abyss', 'dark_mage', 'shadow_apprentice', '深淵', 'Abyss'),
    ('shadow_blade', 'blade_dragon', 'blade_wyvern', '影刃', 'Shadow Blade'),
    ('foxfire', 'white_fox', 'foxfire_maiden', '狐火', 'Foxfire'),
    ('gale', 'wind_cat', 'whirl_lynx', '疾風', 'Gale'),
    ('thunder', 'thunder_lion', 'thunder_lioness', '雷鳴', 'Thunder'),
    ('boulder', 'rock_giant', 'boulder_golem', '巨岩', 'Boulder'),
    ('void', 'ender_sentinel', 'void_scout', '虚空', 'Void'),
    ('verdant', 'creeper_mother', 'spore_nursery', '翠爆', 'Verdant Blast'),
    ('chain_wolf', 'fenrir', 'pack_alpha', '鎖狼', 'Chain Wolf'),
    ('mecha', 'iron_spider', 'recon_drone', '機甲', 'Mecha'),
    ('fallen', 'fallen_valkyrie', 'blackwing_attendant', '堕天', 'Fallen'),
    ('star_tree', 'yggdrasil_spirit', 'treant_warden', '星樹', 'Star Tree'),
]

# 効果の説明（I / II / III）
JA = {
    'crimson': lambda a, b: f'攻撃が相手を{a}秒燃やす・火のダメージ -{b}%',
    'frostbloom': lambda a, b: f'凍えない・攻撃で相手に鈍化II {a}秒' + b,
    'corpse_king': lambda a, b: f'最大体力 +{a}',
    'abyss': lambda a, b: f'背後からの攻撃 +{a}%',
    'shadow_blade': lambda a, b: f'攻撃力 +{a}',
    'foxfire': lambda a, b: f'幸運 +{a}・体力が半分以下で再生',
    'gale': lambda a, b: f'移動速度 +{a}%',
    'thunder': lambda a, b: f'攻撃時 {a}% で落雷（+3ダメージ）',
    'boulder': lambda a, b: f'防具 +{a}・ノックバック耐性 +{b}%',
    'void': lambda a, b: f'攻撃を受けたとき {a}% でワープして回避（8秒に1回）',
    'verdant': lambda a, b: f'爆発のダメージ -{a}%',
    'chain_wolf': lambda a, b: f'敵を倒すと体力 {a} 回復',
    'mecha': lambda a, b: f'飛び道具のダメージ +{a}%',
    'fallen': lambda a, b: f'落下中の攻撃 +{a}%',
    'star_tree': lambda a, b: f'{a}秒ごとに体力 2 回復',
}
EN = {
    'crimson': lambda a, b: f'Attacks set targets on fire for {a}s; fire damage taken -{b}%',
    'frostbloom': lambda a, b: f'Immune to freezing; attacks apply Slowness II for {a}s' + b,
    'corpse_king': lambda a, b: f'Max Health +{a}',
    'abyss': lambda a, b: f'Attacks from behind +{a}%',
    'shadow_blade': lambda a, b: f'Attack Damage +{a}',
    'foxfire': lambda a, b: f'Luck +{a}; regenerate while below half health',
    'gale': lambda a, b: f'Movement Speed +{a}%',
    'thunder': lambda a, b: f'{a}% chance on hit to call lightning (+3 damage)',
    'boulder': lambda a, b: f'Armor +{a}; Knockback Resistance +{b}%',
    'void': lambda a, b: f'{a}% chance to blink away from a hit (once per 8s)',
    'verdant': lambda a, b: f'Explosion damage taken -{a}%',
    'chain_wolf': lambda a, b: f'Heal {a} health on kill',
    'mecha': lambda a, b: f'Projectile damage +{a}%',
    'fallen': lambda a, b: f'Attacks while falling +{a}%',
    'star_tree': lambda a, b: f'Heal 2 health every {a}s',
}
VALS = {
    'crimson': [(2, 30), (3, 50), (4, 70)],
    'frostbloom': [(1, ''), (2, ''), (2, None)],
    'corpse_king': [(2, 0), (4, 0), (6, 0)],
    'abyss': [(15, 0), (25, 0), (35, 0)],
    'shadow_blade': [(1, 0), (2, 0), (3, 0)],
    'foxfire': [(1, 0), (2, 0), (3, 0)],
    'gale': [(5, 0), (10, 0), (15, 0)],
    'thunder': [(10, 0), (15, 0), (20, 0)],
    'boulder': [(2, 10), (4, 20), (6, 30)],
    'void': [(10, 0), (15, 0), (20, 0)],
    'verdant': [(30, 0), (50, 0), (70, 0)],
    'chain_wolf': [(2, 0), (4, 0), (6, 0)],
    'mecha': [(10, 0), (20, 0), (30, 0)],
    'fallen': [(20, 0), (35, 0), (50, 0)],
    'star_tree': [(8, 0), (6, 0), (4, 0)],
}
ROMAN = ['I', 'II', 'III']


def dump(path, obj):
    path = os.path.join(ROOT, path)
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, 'w', encoding='utf-8') as f:
        json.dump(obj, f, ensure_ascii=False, indent=2)
        f.write('\n')


def item(n):
    return f'{NS}:{n}'


def main():
    all_emblems = []
    lang_ja, lang_en = {}, {}
    for key, boss, mid, ja, en in SETS:
        core, shard = f'{key}_core', f'{key}_shard'
        ems = [f'{key}_emblem_{r}' for r in (1, 2, 3)]
        all_emblems += ems
        for n in [core, shard] + ems:
            dump(f'assets/{NS}/models/item/{n}.json', {'parent': 'minecraft:item/generated', 'textures': {'layer0': f'{NS}:item/{n}'}})
        # レシピ
        dump(f'data/{NS}/recipe/{ems[0]}.json', {
            'type': 'minecraft:crafting_shaped', 'category': 'equipment',
            'pattern': ['GSG', 'SCS', 'GSG'],
            'key': {'G': {'item': 'minecraft:gold_ingot'}, 'S': {'item': item(shard)}, 'C': {'item': item(core)}},
            'result': {'id': item(ems[0]), 'count': 1}})
        dump(f'data/{NS}/recipe/{ems[1]}.json', {
            'type': 'minecraft:crafting_shapeless', 'category': 'equipment',
            'ingredients': [{'item': item(ems[0])}, {'item': item(core)}] + [{'item': item(shard)}] * 6,
            'result': {'id': item(ems[1]), 'count': 1}})
        dump(f'data/{NS}/recipe/{ems[2]}.json', {
            'type': 'minecraft:crafting_shapeless', 'category': 'equipment',
            'ingredients': [{'item': item(ems[1])}] + [{'item': item(core)}] * 2 + [{'item': item(shard)}] * 6,
            'result': {'id': item(ems[2]), 'count': 1}})
        # 中ボスのルートテーブルに欠片を足す
        lp = os.path.join(ROOT, f'data/{NS}/loot_table/entities/{mid}.json')
        with open(lp, encoding='utf-8') as f:
            lt = json.load(f)
        lt['pools'] = [p for p in lt['pools'] if not any(e.get('name', '').endswith('_shard') for e in p.get('entries', []))]
        lt['pools'].append({'rolls': 1, 'entries': [{'type': 'minecraft:item', 'name': item(shard), 'functions': [
            {'function': 'minecraft:set_count', 'count': {'type': 'minecraft:uniform', 'min': 1, 'max': 2}},
            {'function': 'minecraft:enchanted_count_increase', 'enchantment': 'minecraft:looting',
             'count': {'type': 'minecraft:uniform', 'min': 0, 'max': 1}}]}],
            'conditions': [{'condition': 'minecraft:killed_by_player'}]})
        dump(f'data/{NS}/loot_table/entities/{mid}.json', lt)
        # 言語
        lang_ja[f'item.{NS}.{core}'] = f'{ja}の核'
        lang_ja[f'item.{NS}.{shard}'] = f'{ja}の欠片'
        lang_en[f'item.{NS}.{core}'] = f'{en} Core'
        lang_en[f'item.{NS}.{shard}'] = f'{en} Shard'
        for r in range(3):
            lang_ja[f'item.{NS}.{ems[r]}'] = f'{ja}の紋章 {ROMAN[r]}'
            lang_en[f'item.{NS}.{ems[r]}'] = f'{en} Emblem {ROMAN[r]}'
            a, b = VALS[key][r]
            if key == 'frostbloom':
                bj = '（30%でもう一段遅く）' if r == 2 else ''
                be = ' (30% chance of Slowness III)' if r == 2 else ''
                lang_ja[f'emblem.{NS}.{key}.effect_{r + 1}'] = JA[key](a, bj)
                lang_en[f'emblem.{NS}.{key}.effect_{r + 1}'] = EN[key](a, be)
            else:
                lang_ja[f'emblem.{NS}.{key}.effect_{r + 1}'] = JA[key](a, b)
                lang_en[f'emblem.{NS}.{key}.effect_{r + 1}'] = EN[key](a, b)

    # Curios：紋章の枠
    dump(f'data/{NS}/curios/slots/emblem.json', {'size': 1, 'order': 160, 'icon': f'{NS}:slot/emblem'})
    dump(f'data/{NS}/curios/entities/{NS}.json', {'entities': ['player'], 'slots': ['emblem']})
    dump('data/curios/tags/item/emblem.json', {'replace': False, 'values': [item(n) for n in all_emblems]})

    lang_ja.update({
        'itemGroup.jsnboss.emblems': 'JSN Boss：紋章',
        'curios.identifier.emblem': '紋章',
        'curios.modifiers.emblem': '紋章につけたとき：',
        'emblem.jsnboss.rank_1': 'ランク I',
        'emblem.jsnboss.rank_2': 'ランク II',
        'emblem.jsnboss.rank_3': 'ランク III',
        'emblem.jsnboss.next': '次のランク：%s',
        'emblem.jsnboss.got_core': '%s を手に入れた！',
    })
    lang_en.update({
        'itemGroup.jsnboss.emblems': 'JSN Boss: Emblems',
        'curios.identifier.emblem': 'Emblem',
        'curios.modifiers.emblem': 'When on emblem:',
        'emblem.jsnboss.rank_1': 'Rank I',
        'emblem.jsnboss.rank_2': 'Rank II',
        'emblem.jsnboss.rank_3': 'Rank III',
        'emblem.jsnboss.next': 'Next rank: %s',
        'emblem.jsnboss.got_core': 'You obtained %s!',
    })
    for code, add in (('ja_jp', lang_ja), ('en_us', lang_en)):
        p = os.path.join(ROOT, f'assets/{NS}/lang/{code}.json')
        with open(p, encoding='utf-8') as f:
            lang = json.load(f)
        lang.update(add)
        with open(p, 'w', encoding='utf-8') as f:
            json.dump(lang, f, ensure_ascii=False, indent=2)
            f.write('\n')

    # 枠のアイコン（灰色のメダル）
    icon = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    g = (55, 55, 55, 140)
    for y in range(16):
        for x in range(16):
            d = ((x + .5 - 8) ** 2 + (y + .5 - 8) ** 2) ** .5
            if 5.5 <= d <= 6.6 or (d <= 3.2 and abs(x - 7.5) + abs(y - 7.5) <= 3.5):
                icon.putpixel((x, y), g)
    for (x, y) in ((7, 0), (8, 0), (7, 15), (8, 15), (0, 7), (0, 8), (15, 7), (15, 8)):
        icon.putpixel((x, y), g)
    ip = os.path.join(ROOT, f'assets/{NS}/textures/slot/emblem.png')
    os.makedirs(os.path.dirname(ip), exist_ok=True)
    icon.save(ip)


if __name__ == '__main__':
    main()
