package com.chigusa.jsnboss.dungeon.earth;

import com.chigusa.jsnboss.dungeon.DungeonMob;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/** 岩甲羅の亀（強い敵・HP300）。甲羅にこもって回転突進（こもっている間はダメージ半分）と噛みつき */
public class StoneTortoise extends DungeonMob {
    static final Move SPIN = new Move("spin", 48, 90, 2, 14, 3, true);
    static final Move BITE = new Move("bite", 16, 24, 0, 2.6, 3, false);
    private static final List<Move> MOVES = List.of(SPIN, BITE);
    private boolean hidden;

    public StoneTortoise(EntityType<? extends Monster> type, Level level) {
        super(type, level, Tier.STRONG, BossEvent.BossBarColor.GREEN);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 300.0).add(Attributes.ARMOR, 16.0).add(Attributes.ATTACK_DAMAGE, 8.0)
                .add(Attributes.MOVEMENT_SPEED, 0.17).add(Attributes.KNOCKBACK_RESISTANCE, 1.0).add(Attributes.FOLLOW_RANGE, 24.0);
    }

    @Override protected List<Move> moves() { return MOVES; }

    @Override
    protected void tickMove(Move m, int t, LivingEntity target) {
        if (m == SPIN) {
            hidden = t >= 6 && t < 42;
            if (t >= 8 && t < 40) {
                if (target != null) {
                    Vec3 to = target.position().subtract(position()).multiply(1, 0, 1);
                    Vec3 v = getDeltaMovement().multiply(1, 0, 1).scale(0.8).add(to.normalize().scale(0.14));
                    double sp = v.length();
                    if (sp > 0.65) v = v.scale(0.65 / sp);
                    setDeltaMovement(v.x, getDeltaMovement().y, v.z);
                    hurtMarked = true;
                }
                if (t % 6 == 0) hitOnce.clear();                    // 回り続けている間は何度も当たる
                hitAround(1.9, 9.0F, 1.2);
                if (level() instanceof ServerLevel sl) {
                    sl.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.STONE.defaultBlockState()), getX(), getY() + 0.2, getZ(), 4, 0.6, 0.1, 0.6, 0.1);
                }
                if (t % 8 == 0) playSound(SoundEvents.GRINDSTONE_USE, 0.6F, 0.6F);
            }
            if (t == 40) setDeltaMovement(0, getDeltaMovement().y, 0);
        } else if (m == BITE) {
            if (t == 8) { hitFront(2.8, 50, 8.0F, 0.4); playSound(SoundEvents.TURTLE_HURT, 1.0F, 0.5F); }
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (hidden && isAttacking()) amount *= 0.5F;
        return super.hurt(source, amount);
    }

    @Override
    protected void bossAiStep() {
        if (!isAttacking()) hidden = false;
        super.bossAiStep();
    }
}
