package com.chigusa.jsnboss.mob;

import com.chigusa.jsnboss.BossConstants;
import com.chigusa.jsnboss.entity.IceWitchEntity;
import com.chigusa.jsnboss.registry.ModMobs;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.level.biome.Biomes;
import com.chigusa.jsnboss.registry.ModThemed;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.FinalizeSpawnEvent;
import net.neoforged.neoforge.event.level.ExplosionEvent;

/** 亜種の自然スポーン（バニラのモブを一定の確率で亜種に置き換える）、エリート化、氷のクリーパーの爆発 */
@EventBusSubscriber(modid = BossConstants.MODID)
public final class MobEvents {
    private MobEvents() {}

    @SubscribeEvent
    public static void onFinalizeSpawn(FinalizeSpawnEvent event) {
        if (!(event.getLevel() instanceof ServerLevel sl)) return;
        MobSpawnType st = event.getSpawnType();
        if (st != MobSpawnType.NATURAL && st != MobSpawnType.STRUCTURE) return;
        Mob mob = event.getEntity();
        RandomSource r = sl.getRandom();
        EntityType<? extends Mob> variant = pickVariant(mob, sl, r);
        if (variant != null) {
            event.setSpawnCancelled(true);
            Mob v = variant.create(sl);
            if (v == null) return;
            v.moveTo(event.getX(), event.getY(), event.getZ(), mob.getYRot(), 0);
            v.finalizeSpawn(sl, event.getDifficulty(), st, null);
            EliteMobs.maybeMakeElite(v, r, st);
            sl.addFreshEntity(v);
            return;
        }
        EliteMobs.maybeMakeElite(mob, r, st);
    }

    /** どのバニラのモブを、どのくらいの確率で亜種にするか */
    private static EntityType<? extends Mob> pickVariant(Mob mob, ServerLevel sl, RandomSource r) {
        EntityType<?> t = mob.getType();
        BlockPos pos = mob.blockPosition();
        var biome = sl.getBiome(pos);
        // ---- 属性の亜種（前からあるもの）
        if (t == EntityType.BLAZE) {
            float f = r.nextFloat();
            if (f < 0.10F) return ModMobs.SOUL_BLAZE.get();
            if (f < 0.20F) return ModMobs.FROST_BLAZE.get();
        } else if (t == EntityType.HUSK) {
            if (r.nextFloat() < 0.20F) return ModMobs.TOMB_ZOMBIE.get();
        } else if (t == EntityType.ZOMBIE) {
            if (r.nextFloat() < (sl.isThundering() ? 0.30F : 0.04F)) return ModMobs.THUNDER_ZOMBIE.get();
        } else if (t == EntityType.SKELETON) {
            float f = r.nextFloat();
            if (f < 0.05F) return ModMobs.FIRE_SKELETON.get();
            if (f < 0.10F) return ModMobs.POISON_SKELETON.get();
        } else if (t == EntityType.CREEPER) {
            if (r.nextFloat() < (biome.value().coldEnoughToSnow(pos) ? 0.35F : 0.03F)) return ModMobs.FROST_CREEPER.get();
        } else if (t == EntityType.SPIDER) {
            if (r.nextFloat() < 0.06F) return ModMobs.MAGMA_SPIDER.get();
        } else if (t == EntityType.DROWNED) {
            if (r.nextFloat() < 0.15F) return ModMobs.PRISMARINE_DROWNED.get();
            return null;
        }
        // ---- テーマ亜種：その場所に合うテーマから選ぶ（全体で 2 割くらい）
        String base = t == EntityType.ZOMBIE ? "zombie" : t == EntityType.SKELETON ? "skeleton" : t == EntityType.SPIDER ? "spider"
                : t == EntityType.SLIME ? "slime" : t == EntityType.BLAZE ? "blaze" : t == EntityType.CREEPER ? "creeper" : null;
        if (base == null) return null;
        java.util.List<Theme> themes = themesAt(sl, pos);
        if (themes.isEmpty()) return null;
        Theme th = themes.get(r.nextInt(themes.size()));
        if (r.nextFloat() < 0.20F) {
            if (th == Theme.SEA && base.equals("zombie")) return ModMobs.PRISMARINE_DROWNED.get();
            return ModThemed.get(th, base);
        }
        // ふだんは出ないブレイズとスライムも、テーマの場所ではときどき出る
        if (!base.equals("blaze") && !base.equals("slime") && r.nextFloat() < 0.015F) {
            return ModThemed.get(th, r.nextBoolean() ? "blaze" : "slime");
        }
        return null;
    }

    /**
     * その場所に出るテーマ。
     * 森の＝森・ジャングル・タイガ・沼 / べたべた＝花の森・草原・ヒマワリ平原・桜の林 / 海の＝海・砂浜・川 /
     * 空の＝高さ100より上・山 / 黄金の＝荒野・ネザー / 残響の＝高さ0より下 / 鉱石の＝高さ40より下・ネザー /
     * 光る＝繁茂した洞窟・沼・マングローブ・キノコ島
     */
    public static java.util.List<Theme> themesAt(ServerLevel sl, BlockPos pos) {
        java.util.List<Theme> out = new java.util.ArrayList<>();
        var b = sl.getBiome(pos);
        if (sl.dimension() == net.minecraft.world.level.Level.NETHER) {
            out.add(Theme.GOLD);
            out.add(Theme.ORE);
            return out;
        }
        if (sl.dimension() != net.minecraft.world.level.Level.OVERWORLD) return out;
        int y = pos.getY();
        if (b.is(BiomeTags.IS_FOREST) || b.is(BiomeTags.IS_JUNGLE) || b.is(BiomeTags.IS_TAIGA)
                || b.is(Biomes.SWAMP) || b.is(Biomes.MANGROVE_SWAMP)) out.add(Theme.MOSS);
        if (b.is(Biomes.FLOWER_FOREST) || b.is(Biomes.MEADOW) || b.is(Biomes.PLAINS) || b.is(Biomes.SUNFLOWER_PLAINS)
                || b.is(Biomes.CHERRY_GROVE)) out.add(Theme.HONEY);
        if (b.is(BiomeTags.IS_OCEAN) || b.is(BiomeTags.IS_BEACH) || b.is(BiomeTags.IS_RIVER)) out.add(Theme.SEA);
        if (y > 100 || b.is(BiomeTags.IS_MOUNTAIN)) out.add(Theme.SKY);
        if (b.is(BiomeTags.IS_BADLANDS)) out.add(Theme.GOLD);
        if (y < 0) out.add(Theme.SCULK);
        if (y < 40) out.add(Theme.ORE);
        if (b.is(Biomes.LUSH_CAVES) || b.is(Biomes.SWAMP) || b.is(Biomes.MANGROVE_SWAMP) || b.is(Biomes.MUSHROOM_FIELDS)) out.add(Theme.GLOW);
        return out;
    }

    /** 氷のクリーパー：ブロックを壊さないかわりに、巻き込んだ相手を凍らせる */
    @SubscribeEvent
    public static void onExplosion(ExplosionEvent.Detonate event) {
        Entity src = event.getExplosion().getDirectSourceEntity();
        if (src instanceof Themed && src instanceof net.minecraft.world.entity.monster.Creeper) {
            event.getAffectedBlocks().clear();                  // テーマ亜種のクリーパーもブロックを壊さない
            return;
        }
        if (!(src instanceof FrostCreeper)) return;
        event.getAffectedBlocks().clear();
        for (Entity e : event.getAffectedEntities()) {
            if (e instanceof LivingEntity le) {
                IceWitchEntity.freeze(le, 300);
                le.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 120, 2));
            }
        }
        if (event.getLevel() instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.SNOWFLAKE, src.getX(), src.getY() + 1, src.getZ(), 150, 2.5, 1.5, 2.5, 0.1);
        }
    }
}
