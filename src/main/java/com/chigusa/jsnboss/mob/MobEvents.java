package com.chigusa.jsnboss.mob;

import com.chigusa.jsnboss.BossConstants;
import com.chigusa.jsnboss.entity.IceWitchEntity;
import com.chigusa.jsnboss.registry.ModMobs;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BiomeTags;
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
        int y = pos.getY();
        if (t == EntityType.BLAZE) {
            float f = r.nextFloat();
            if (f < 0.10F) return ModMobs.SOUL_BLAZE.get();
            if (f < 0.20F) return ModMobs.FROST_BLAZE.get();
        } else if (t == EntityType.HUSK) {
            if (r.nextFloat() < 0.20F) return ModMobs.TOMB_ZOMBIE.get();
        } else if (t == EntityType.ZOMBIE) {
            if (r.nextFloat() < (sl.isThundering() ? 0.30F : 0.04F)) return ModMobs.THUNDER_ZOMBIE.get();
            if (y < 40 && r.nextFloat() < 0.12F) return ModMobs.ORE_ZOMBIE.get();                 // 地下：鉱石のゾンビ
            boolean forest = biome.is(BiomeTags.IS_FOREST) || biome.is(BiomeTags.IS_JUNGLE) || biome.is(BiomeTags.IS_TAIGA);
            if (r.nextFloat() < (forest ? 0.15F : 0.04F)) return ModMobs.MOSS_ZOMBIE.get();        // 森：苔ゾンビ
            if (r.nextFloat() < (biome.is(BiomeTags.IS_BADLANDS) ? 0.15F : 0.03F)) return ModMobs.GOLD_ZOMBIE.get();
        } else if (t == EntityType.SKELETON) {
            float f = r.nextFloat();
            if (f < 0.05F) return ModMobs.FIRE_SKELETON.get();
            if (f < 0.10F) return ModMobs.POISON_SKELETON.get();
            boolean deepDark = biome.is(net.minecraft.world.level.biome.Biomes.DEEP_DARK);
            if (y < 0 && r.nextFloat() < (deepDark ? 0.40F : 0.15F)) return ModMobs.SCULK_SKELETON.get();
            if (r.nextFloat() < (y > 100 ? 0.25F : 0.04F)) return ModMobs.SKY_SKELETON.get();
        } else if (t == EntityType.CREEPER) {
            boolean cold = biome.value().coldEnoughToSnow(pos);
            if (r.nextFloat() < (cold ? 0.35F : 0.03F)) return ModMobs.FROST_CREEPER.get();
        } else if (t == EntityType.SPIDER) {
            float f = r.nextFloat();
            if (f < 0.06F) return ModMobs.MAGMA_SPIDER.get();
            if (f < 0.12F) return ModMobs.HONEY_SPIDER.get();
        } else if (t == EntityType.DROWNED) {
            if (r.nextFloat() < 0.15F) return ModMobs.PRISMARINE_DROWNED.get();
        } else if (t == EntityType.SLIME) {
            if (r.nextFloat() < 0.15F) return ModMobs.GLOW_SLIME.get();
        }
        return null;
    }

    /** 氷のクリーパー：ブロックを壊さないかわりに、巻き込んだ相手を凍らせる */
    @SubscribeEvent
    public static void onExplosion(ExplosionEvent.Detonate event) {
        Entity src = event.getExplosion().getDirectSourceEntity();
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
