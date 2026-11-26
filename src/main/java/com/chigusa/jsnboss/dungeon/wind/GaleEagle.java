package com.chigusa.jsnboss.dungeon.wind;

import com.chigusa.jsnboss.dungeon.DungeonMob;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.BossEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/** 疾風の鷲（強い敵・HP220）。空から急降下して突っ込み、羽ばたきの突風で押し戻す */
public class GaleEagle extends DungeonMob {
    static final Move DIVE = new Move("dive", 28, 50, 0, 16, 3, true);
    static final Move GUST = new Move("gust", 24, 90, 0, 10, 2, true);
    private static final List<Move> MOVES = List.of(DIVE, GUST);

    public GaleEagle(EntityType<? extends Monster> type, Level level) {
        super(type, level, Tier.STRONG, BossEvent.BossBarColor.GREEN);
        setNoGravity(true);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 220.0).add(Attributes.ARMOR, 4.0).add(Attributes.ATTACK_DAMAGE, 9.0)
                .add(Attributes.MOVEMENT_SPEED, 0.4).add(Attributes.FOLLOW_RANGE, 32.0).add(Attributes.FLYING_SPEED, 0.4);
    }

    @Override protected boolean flying() { return true; }
    @Override protected double hoverHeight() { return 6.0; }
    @Override protected double keepDistance() { return 7.0; }
    @Override protected List<Move> moves() { return MOVES; }

    @Override
    protected void tickMove(Move m, int t, LivingEntity target) {
        if (target == null) return;
        face(target, 30.0F);
        if (m == DIVE) {
            if (t == 6) playSound(SoundEvents.PHANTOM_SWOOP, 1.2F, 1.2F);
            if (t >= 8 && t < 22) {
                Vec3 to = target.position().add(0, target.getBbHeight() * 0.5, 0).subtract(position());
                setDeltaMovement(to.normalize().scale(1.1));
                hurtMarked = true;
                hitAround(1.6, 9.0F, 1.0);
            }
            if (t == 22) setDeltaMovement(getDeltaMovement().scale(0.2).add(0, 0.6, 0));
        } else if (m == GUST) {
            if (t == 9 || t == 16) {
                Vec3 f = aim(target).multiply(1, 0, 1).normalize();
                for (LivingEntity e : inRadius(position(), 10, 8)) {
                    Vec3 to = e.position().subtract(position()).multiply(1, 0, 1);
                    if (to.lengthSqr() < 1.0E-4 || to.normalize().dot(f) < 0.5) continue;
                    damage(e, 3.0F, 0);
                    e.setDeltaMovement(e.getDeltaMovement().add(f.scale(1.2)).add(0, 0.25, 0));
                    e.hurtMarked = true;
                }
                if (level() instanceof ServerLevel sl) sl.sendParticles(ParticleTypes.GUST, getX() + f.x * 3, getY(), getZ() + f.z * 3, 3, 1, 0.5, 1, 0);
                playSound(SoundEvents.BREEZE_WIND_CHARGE_BURST.value(), 1.0F, 1.2F);
            }
        }
    }
}
