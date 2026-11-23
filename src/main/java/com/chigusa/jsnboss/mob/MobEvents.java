package com.chigusa.jsnboss.mob;

import com.chigusa.jsnboss.BossConstants;
import com.chigusa.jsnboss.entity.IceWitchEntity;
import com.chigusa.jsnboss.registry.ModMobs;
import net.minecraft.core.BlockPos;
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
        float f = r.nextFloat();
        if (t == EntityType.BLAZE) {
            if (f < 0.10F) return ModMobs.SOUL_BLAZE.get();
            if (f < 0.20F) return ModMobs.FROST_BLAZE.get();
        } else if (t == EntityType.HUSK) {
            if (f < 0.20F) return ModMobs.TOMB_ZOMBIE.get();
        } else if (t == EntityType.ZOMBIE) {
            if (f < (sl.isThundering() ? 0.30F : 0.04F)) return ModMobs.THUNDER_ZOMBIE.get();
        } else if (t == EntityType.SKELETON) {
            if (f < 0.05F) return ModMobs.FIRE_SKELETON.get();
            if (f < 0.10F) return ModMobs.POISON_SKELETON.get();
        } else if (t == EntityType.CREEPER) {
            BlockPos pos = mob.blockPosition();
            boolean cold = sl.getBiome(pos).value().coldEnoughToSnow(pos);
            if (f < (cold ? 0.35F : 0.03F)) return ModMobs.FROST_CREEPER.get();
        } else if (t == EntityType.SPIDER) {
            if (f < 0.06F) return ModMobs.MAGMA_SPIDER.get();
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
