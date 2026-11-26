package com.chigusa.jsnboss.dungeon.voidset;

import com.chigusa.jsnboss.dungeon.DungeonMob;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/** 虚空の狩人（強い敵・HP250）。鉤爪の2連撃と背後への瞬間移動斬り。攻撃されると、ときどき瞬間移動でかわす */
public class VoidStalker extends DungeonMob {
    static final Move STRIKE = new Move("strike", 20, 22, 0, 3.0, 4, false);
    static final Move BLINK = new Move("blink", 24, 70, 4, 18, 3, true);
    private static final List<Move> MOVES = List.of(STRIKE, BLINK);
    private int dodgeCd;

    public VoidStalker(EntityType<? extends Monster> type, Level level) {
        super(type, level, Tier.STRONG, BossEvent.BossBarColor.PURPLE);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 250.0).add(Attributes.ARMOR, 4.0).add(Attributes.ATTACK_DAMAGE, 8.0)
                .add(Attributes.MOVEMENT_SPEED, 0.33).add(Attributes.FOLLOW_RANGE, 28.0).add(Attributes.STEP_HEIGHT, 1.0);
    }

    @Override protected List<Move> moves() { return MOVES; }

    @Override
    protected void tickMove(Move m, int t, LivingEntity target) {
        if (m == STRIKE) {
            if (t == 5 || t == 11) { hitFront(3.0, 60, 6.0F, 0.3); playSound(SoundEvents.PLAYER_ATTACK_SWEEP, 0.7F, 1.4F); }
        } else if (m == BLINK) {
            if (t == 8 && target != null) {
                Vec3 behind = target.position().subtract(Vec3.directionFromRotation(0, target.getYRot()).scale(1.6));
                blinkTo(behind.x, behind.z, target.getY());
                face(target, 360);
            }
            if (t == 12) hitFront(3.0, 80, 10.0F, 0.5);
        }
    }

    @Override
    protected void bossAiStep() {
        if (dodgeCd > 0) dodgeCd--;
        super.bossAiStep();
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (!level().isClientSide && dodgeCd <= 0 && !isAttacking() && source.getEntity() != null && random.nextFloat() < 0.3F) {
            dodgeCd = 80;
            double a = random.nextDouble() * Math.PI * 2;
            if (blinkTo(getX() + Math.cos(a) * 6, getZ() + Math.sin(a) * 6, getY())) return false;   // かわした
        }
        return super.hurt(source, amount);
    }

    @Override
    protected void clientTick() {
        if (random.nextInt(2) == 0) level().addParticle(ParticleTypes.PORTAL, getRandomX(0.5), getRandomY() - 0.25, getRandomZ(0.5), 0, 0, 0);
    }
}
