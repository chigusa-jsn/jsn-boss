package com.chigusa.jsnboss.registry;

import com.chigusa.jsnboss.emblem.EmblemItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.neoforged.neoforge.registries.DeferredItem;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.chigusa.jsnboss.registry.ModItems.ITEMS;

/**
 * 紋章システム：ボスの核・中ボスの欠片・紋章 I/II/III（15種類）。
 * 核はボスを倒したとき近くの全員に配られる（EmblemEvents）。欠片は中ボスのルートテーブルに入っている。
 */
public final class ModEmblems {
    /** key：アイテム名の頭。boss：核をくれるボス。mid：欠片を落とす中ボス */
    public record EmblemSet(String key, String boss, String mid, String ja, String en) {}

    public static final List<EmblemSet> SETS = List.of(
            new EmblemSet("crimson", "fire_knight", "flame_squire", "紅蓮", "Crimson"),
            new EmblemSet("frostbloom", "ice_witch", "frost_maiden", "氷華", "Frostbloom"),
            new EmblemSet("corpse_king", "zombie_king", "rotting_guard", "屍王", "Corpse King"),
            new EmblemSet("abyss", "dark_mage", "shadow_apprentice", "深淵", "Abyss"),
            new EmblemSet("shadow_blade", "blade_dragon", "blade_wyvern", "影刃", "Shadow Blade"),
            new EmblemSet("foxfire", "white_fox", "foxfire_maiden", "狐火", "Foxfire"),
            new EmblemSet("gale", "wind_cat", "whirl_lynx", "疾風", "Gale"),
            new EmblemSet("thunder", "thunder_lion", "thunder_lioness", "雷鳴", "Thunder"),
            new EmblemSet("boulder", "rock_giant", "boulder_golem", "巨岩", "Boulder"),
            new EmblemSet("void", "ender_sentinel", "void_scout", "虚空", "Void"),
            new EmblemSet("verdant", "creeper_mother", "spore_nursery", "翠爆", "Verdant Blast"),
            new EmblemSet("chain_wolf", "fenrir", "pack_alpha", "鎖狼", "Chain Wolf"),
            new EmblemSet("mecha", "iron_spider", "recon_drone", "機甲", "Mecha"),
            new EmblemSet("fallen", "fallen_valkyrie", "blackwing_attendant", "堕天", "Fallen"),
            new EmblemSet("star_tree", "yggdrasil_spirit", "treant_warden", "星樹", "Star Tree"));

    public static final Map<String, DeferredItem<Item>> CORES = new LinkedHashMap<>();
    public static final Map<String, DeferredItem<Item>> SHARDS = new LinkedHashMap<>();
    /** key → [I, II, III] */
    public static final Map<String, List<DeferredItem<EmblemItem>>> EMBLEMS = new LinkedHashMap<>();
    /** ボスの名前（entity id のパス）→ key */
    public static final Map<String, String> BY_BOSS = new LinkedHashMap<>();

    static {
        for (EmblemSet s : SETS) {
            CORES.put(s.key(), ITEMS.register(s.key() + "_core",
                    () -> new Item(new Item.Properties().rarity(Rarity.RARE).stacksTo(16))));
            SHARDS.put(s.key(), ITEMS.register(s.key() + "_shard",
                    () -> new Item(new Item.Properties().rarity(Rarity.UNCOMMON))));
            List<DeferredItem<EmblemItem>> ranks = new ArrayList<>();
            for (int r = 1; r <= 3; r++) {
                final int rank = r;
                ranks.add(ITEMS.register(s.key() + "_emblem_" + r, () -> new EmblemItem(s.key(), rank)));
            }
            EMBLEMS.put(s.key(), ranks);
            BY_BOSS.put(s.boss(), s.key());
        }
    }

    public static void init() {}

    private ModEmblems() {}
}
