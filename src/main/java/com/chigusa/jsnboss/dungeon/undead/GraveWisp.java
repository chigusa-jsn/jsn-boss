package com.chigusa.jsnboss.dungeon.undead;

import com.chigusa.jsnboss.dungeon.DungeonMob;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.BossEvent;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;

import java.util.List;

/** 墓場の魂（弱い敵・HP40）。半透明の幽霊。壁をすり抜けて近づき、触ると弱体化 */
public class GraveWisp extends DungeonMob {
    static final Move TOUCH = new Move("touch", 16, 30, 0, 1.8, 1, false);
    private static final List<Move> MOVES = List.of(TOUCH);

    public GraveWisp(EntityType<? extends Monster> type, Level level) {
        super(type, level, Tier.WEAK, BossEvent.BossBarColor.WHITE);
        setNoGravity(true);
        this.noPhysics = true;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 40.0).add(Attributes.ATTACK_DAMAGE, 2.0).add(Attributes.MOVEMENT_SPEED, 0.22)
                .add(Attributes.FOLLOW_RANGE, 24.0).add(Attributes.FLYING_SPEED, 0.22);
    }

    @Override protected boolean flying() { return true; }
    @Override protected double hoverHeight() { return 0.6; }
    @Override protected List<Move> moves() { return MOVES; }

    @Override
    public void tick() {
        this.noPhysics = true;                                   // 壁をすり抜ける
        super.tick();
    }

    @Override
    protected void tickMove(Move m, int t, LivingEntity target) {
        if (t == 7) {
            for (LivingEntity e : hitAround(1.6, 2.0F, 0)) {
                e.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 120, 0));
                e.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 120, 0));
            }
            playSound(SoundEvents.VEX_CHARGE, 1.0F, 0.6F);
        }
    }

    @Override
    protected void clientTick() {
        if (random.nextInt(3) == 0) level().addParticle(ParticleTypes.SOUL, getRandomX(0.4), getY() + 0.2, getRandomZ(0.4), 0, -0.01, 0);
    }
}
