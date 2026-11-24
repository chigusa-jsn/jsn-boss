package com.chigusa.jsnboss.registry;

import com.chigusa.jsnboss.mob.Theme;
import com.chigusa.jsnboss.mob.ThemedBlaze;
import com.chigusa.jsnboss.mob.ThemedCreeper;
import com.chigusa.jsnboss.mob.ThemedSkeleton;
import com.chigusa.jsnboss.mob.ThemedSlime;
import com.chigusa.jsnboss.mob.ThemedSpider;
import com.chigusa.jsnboss.mob.ThemedZombie;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.common.DeferredSpawnEggItem;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

import static com.chigusa.jsnboss.registry.ModEntities.ENTITY_TYPES;
import static com.chigusa.jsnboss.registry.ModItems.ITEMS;

/**
 * テーマ亜種：テーマ8種類（森・べたべた・海・空・黄金・残響・鉱石・光る）× モブ6種類
 * （ゾンビ・スケルトン・クモ・スライム・ブレイズ・クリーパー）。先に作った8体（moss_zombie など）は ModMobs にある。
 * 名前は「テーマ_モブ」（例：sky_zombie, gold_blaze）。
 */
public final class ModThemed {
    public static final String[] BASES = {"zombie", "skeleton", "spider", "slime", "blaze", "creeper"};

    /** 名前 → 登録 */
    public static final Map<String, DeferredHolder<EntityType<?>, ? extends EntityType<? extends Mob>>> TYPES = new LinkedHashMap<>();
    /** 名前 → モブの種類（zombie など） */
    public static final Map<String, String> BASE_OF = new LinkedHashMap<>();
    public static final Map<String, Theme> THEME_OF = new LinkedHashMap<>();
    public static final Map<String, DeferredItem<DeferredSpawnEggItem>> EGGS = new LinkedHashMap<>();

    static {
        for (Theme th : Theme.values()) {
            for (String base : BASES) {
                if (existing(th, base) != null) continue;
                String id = th.id + "_" + base;
                DeferredHolder<EntityType<?>, ? extends EntityType<? extends Mob>> h = switch (base) {
                    case "zombie" -> ENTITY_TYPES.register(id, () -> EntityType.Builder.<ThemedZombie>of((t, l) -> new ThemedZombie(t, l, th), MobCategory.MONSTER)
                            .sized(0.6F, 1.95F).eyeHeight(1.74F).clientTrackingRange(8).build(id));
                    case "skeleton" -> ENTITY_TYPES.register(id, () -> EntityType.Builder.<ThemedSkeleton>of((t, l) -> new ThemedSkeleton(t, l, th), MobCategory.MONSTER)
                            .sized(0.6F, 1.99F).eyeHeight(1.74F).clientTrackingRange(8).build(id));
                    case "spider" -> ENTITY_TYPES.register(id, () -> EntityType.Builder.<ThemedSpider>of((t, l) -> new ThemedSpider(t, l, th), MobCategory.MONSTER)
                            .sized(1.4F, 0.9F).eyeHeight(0.65F).clientTrackingRange(8).build(id));
                    case "slime" -> ENTITY_TYPES.register(id, () -> EntityType.Builder.<ThemedSlime>of((t, l) -> new ThemedSlime(t, l, th), MobCategory.MONSTER)
                            .sized(0.52F, 0.52F).eyeHeight(0.325F).spawnDimensionsScale(4.0F).clientTrackingRange(10).build(id));
                    case "blaze" -> ENTITY_TYPES.register(id, () -> EntityType.Builder.<ThemedBlaze>of((t, l) -> new ThemedBlaze(t, l, th), MobCategory.MONSTER)
                            .sized(0.6F, 1.8F).fireImmune().clientTrackingRange(8).build(id));
                    default -> ENTITY_TYPES.register(id, () -> EntityType.Builder.<ThemedCreeper>of((t, l) -> new ThemedCreeper(t, l, th), MobCategory.MONSTER)
                            .sized(0.6F, 1.7F).clientTrackingRange(8).build(id));
                };
                TYPES.put(id, h);
                BASE_OF.put(id, base);
                THEME_OF.put(id, th);
                Supplier<? extends EntityType<? extends Mob>> sup = h;
                EGGS.put(id, ITEMS.register(id + "_spawn_egg", () -> new DeferredSpawnEggItem(sup, th.eggBg, th.eggFg, new Item.Properties())));
            }
        }
    }

    /** 先に作った8体（テーマとモブの組み合わせがすでにあるもの） */
    public static Supplier<? extends EntityType<? extends Mob>> existing(Theme th, String base) {
        return switch (th) {
            case MOSS -> base.equals("zombie") ? ModMobs.MOSS_ZOMBIE : null;
            case HONEY -> base.equals("spider") ? ModMobs.HONEY_SPIDER : null;
            case SEA -> base.equals("zombie") ? ModMobs.PRISMARINE_DROWNED : null;
            case SKY -> base.equals("skeleton") ? ModMobs.SKY_SKELETON : null;
            case GOLD -> base.equals("zombie") ? ModMobs.GOLD_ZOMBIE : null;
            case SCULK -> base.equals("skeleton") ? ModMobs.SCULK_SKELETON : null;
            case ORE -> base.equals("zombie") ? ModMobs.ORE_ZOMBIE : null;
            case GLOW -> base.equals("slime") ? ModMobs.GLOW_SLIME : null;
        };
    }

    /** テーマとモブの種類から、その亜種の EntityType */
    public static EntityType<? extends Mob> get(Theme th, String base) {
        Supplier<? extends EntityType<? extends Mob>> ex = existing(th, base);
        if (ex != null) return ex.get();
        var h = TYPES.get(th.id + "_" + base);
        return h == null ? null : h.get();
    }

    public static void init() {}

    private ModThemed() {}
}
