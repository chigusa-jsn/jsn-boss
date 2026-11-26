package com.chigusa.jsnboss.dungeon.nature;

import com.chigusa.jsnboss.dungeon.DungeonMob;
import com.chigusa.jsnboss.entity.BoulderEntity;
import com.chigusa.jsnboss.entity.CreeperMotherEntity;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.BossEvent;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/** 胞子の苗床（中ボス・HP500）。マザークリーパーの眷属。動かない大きな花。子クリーパーを生み、毒の胞子、ツタの引き寄せ、種の爆弾 */
public class SporeNursery extends DungeonMob {
    static final Move BIRTH = new Move("birth", 30, 300, 0, 24, 2, false);
    static final Move SPORES = new Move("spores", 30, 110, 0, 6, 3, false);
    static final Move VINES = new Move("vines", 26, 70, 3, 11, 3, true);
    static final Move SEEDS = new Move("seeds", 26, 60, 4, 20, 3, true);
    private static final List<Move> MOVES = List.of(BIRTH, SPORES, VINES, SEEDS);

    public SporeNursery(EntityType<? extends Monster> type, Level level) {
        super(type, level, Tier.MID, BossEvent.BossBarColor.GREEN);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 500.0).add(Attributes.ARMOR, 6.0).add(Attributes.ATTACK_DAMAGE, 6.0)
                .add(Attributes.MOVEMENT_SPEED, 0.0).add(Attributes.KNOCKBACK_RESISTANCE, 1.0).add(Attributes.FOLLOW_RANGE, 28.0);
    }

    @Override protected boolean stationary() { return true; }
    @Override protected List<Move> moves() { return MOVES; }
    @Override public boolean isPushable() { return false; }

    @Override
    protected void tickMove(Move m, int t, LivingEntity target) {
        ServerLevel sl = level() instanceof ServerLevel s ? s : null;
        if (m == BIRTH) {
            if (t == 16 && sl != null) {
                int near = level().getEntitiesOfClass(Creeper.class, getBoundingBox().inflate(16), c -> c.getTags().contains(CreeperMotherEntity.KID_TAG)).size();
                for (int i = 0; i < Math.min(2, 4 - near); i++) {
                    Creeper kid = EntityType.CREEPER.create(sl);
                    if (kid == null) continue;
                    double a = random.nextDouble() * Math.PI * 2;
                    kid.moveTo(getX() + Math.cos(a) * 2.5, getY() + 0.5, getZ() + Math.sin(a) * 2.5, random.nextFloat() * 360, 0);
                    kid.finalizeSpawn(sl, sl.getCurrentDifficultyAt(kid.blockPosition()), MobSpawnType.MOB_SUMMONED, null);
                    kid.addTag(CreeperMotherEntity.KID_TAG);                 // 爆発でブロックを壊さない
                    kid.setDeltaMovement(Math.cos(a) * 0.4, 0.5, Math.sin(a) * 0.4);
                    if (target != null) kid.setTarget(target);
                    sl.addFreshEntity(kid);
                    sl.sendParticles(ParticleTypes.POOF, kid.getX(), kid.getY() + 0.5, kid.getZ(), 10, 0.3, 0.3, 0.3, 0.05);
                }
                playSound(SoundEvents.SLIME_SQUISH, 2.0F, 0.5F);
            }
        } else if (m == SPORES) {
            if (t >= 10 && t < 28 && t % 6 == 4) {
                for (LivingEntity e : inRadius(position(), 5.5, 4)) {
                    e.addEffect(new MobEffectInstance(MobEffects.POISON, 80, 0));
                    damage(e, 2.0F, 0);
                }
                if (sl != null) sl.sendParticles(ParticleTypes.SPORE_BLOSSOM_AIR, getX(), getY() + 2, getZ(), 60, 3, 1.2, 3, 0.02);
                if (t == 10) playSound(SoundEvents.AZALEA_LEAVES_BREAK, 2.0F, 0.6F);
            }
        } else if (m == VINES) {
            if (t == 12 && target != null && hasLineOfSight(target)) {
                Vec3 pull = position().subtract(target.position()).multiply(1, 0, 1).normalize().scale(1.4);
                target.setDeltaMovement(pull.x, 0.3, pull.z);
                target.hurtMarked = true;
                damage(target, 4.0F, 0);
                target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 1));
                if (sl != null) {
                    Vec3 a = position().add(0, 0.5, 0), b = target.position().add(0, 0.5, 0);
                    for (int i = 0; i <= 10; i++) {
                        Vec3 p = a.lerp(b, i / 10.0);
                        sl.sendParticles(ParticleTypes.COMPOSTER, p.x, p.y, p.z, 2, 0.05, 0.05, 0.05, 0);
                    }
                }
                playSound(SoundEvents.VINE_BREAK, 1.5F, 0.6F);
            }
        } else if (m == SEEDS) {
            if (t == 12 && target != null) {
                int n = enraged() ? 5 : 3;
                Vec3 from = position().add(0, getBbHeight(), 0);
                for (int i = 0; i < n; i++) {
                    Vec3 at = target.position().add((random.nextDouble() - 0.5) * 5, 0, (random.nextDouble() - 0.5) * 5);
                    if (i == 0) at = target.position();
                    BoulderEntity.throwSeed(this, from, at, 6.0F);
                }
                playSound(SoundEvents.DISPENSER_LAUNCH, 1.5F, 0.7F);
            }
        }
    }

    @Override
    protected void clientTick() {
        if (random.nextInt(3) == 0) level().addParticle(ParticleTypes.SPORE_BLOSSOM_AIR, getRandomX(1.0), getY() + 2.5, getRandomZ(1.0), 0, 0, 0);
    }
}
