package com.chigusa.jsnboss.dungeon.elite;

import com.chigusa.jsnboss.dungeon.DungeonMob;
import com.chigusa.jsnboss.entity.FeatherShotEntity;
import com.chigusa.jsnboss.entity.LightSwordEntity;
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

/** 黒翼の従者（中ボス・HP500）。堕ちた戦乙女の従者。黒い羽根の扇撃ち、槍を構えての急降下、光の剣を降らせる */
public class BlackwingAttendant extends DungeonMob {
    static final Move FEATHERS = new Move("feathers", 24, 40, 2, 20, 4, true);
    static final Move DIVE = new Move("dive", 32, 70, 0, 16, 3, true);
    static final Move SWORDS = new Move("swords", 28, 90, 0, 18, 3, true);
    private static final List<Move> MOVES = List.of(FEATHERS, DIVE, SWORDS);

    public BlackwingAttendant(EntityType<? extends Monster> type, Level level) {
        super(type, level, Tier.MID, BossEvent.BossBarColor.PURPLE);
        setNoGravity(true);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 500.0).add(Attributes.ARMOR, 10.0).add(Attributes.ATTACK_DAMAGE, 10.0)
                .add(Attributes.MOVEMENT_SPEED, 0.32).add(Attributes.KNOCKBACK_RESISTANCE, 0.6)
                .add(Attributes.FOLLOW_RANGE, 32.0).add(Attributes.FLYING_SPEED, 0.32);
    }

    @Override protected boolean flying() { return true; }
    @Override protected double hoverHeight() { return 3.5; }
    @Override protected double keepDistance() { return 6.0; }
    @Override protected List<Move> moves() { return MOVES; }

    @Override
    protected void tickMove(Move m, int t, LivingEntity target) {
        if (target == null) return;
        face(target, 30.0F);
        if (m == FEATHERS) {
            if (t == 12) {
                Vec3 dir = aim(target);
                int n = enraged() ? 7 : 5;
                for (int i = 0; i < n; i++) {
                    Vec3 d = dir.yRot((float) Math.toRadians((i - (n - 1) / 2.0) * 10));
                    FeatherShotEntity.shoot(this, eyePos().add(d), d.scale(1.1), 6.0F);
                }
                playSound(SoundEvents.PHANTOM_FLAP, 1.2F, 0.8F);
            }
        } else if (m == DIVE) {
            if (t < 10) setDeltaMovement(getDeltaMovement().scale(0.5).add(0, 0.12, 0));      // 一度上がってから
            if (t == 10) playSound(SoundEvents.TRIDENT_RIPTIDE_1.value(), 1.2F, 0.8F);
            if (t >= 10 && t < 24) {
                Vec3 to = target.position().add(0, target.getBbHeight() * 0.5, 0).subtract(position());
                setDeltaMovement(to.normalize().scale(1.2));
                hurtMarked = true;
                hitAround(1.8, 12.0F, 1.0);
            }
            if (t == 24) setDeltaMovement(getDeltaMovement().scale(0.2).add(0, 0.6, 0));
        } else if (m == SWORDS) {
            if (t == 12) {
                LightSwordEntity.spawn(this, target.getX(), groundY(target.getX(), target.getY() + 1, target.getZ()), target.getZ(), 10, 10.0F);
                Vec3 side = aim(target).yRot((float) Math.toRadians(90)).scale(2.5);
                LightSwordEntity.spawn(this, target.getX() + side.x, target.getY(), target.getZ() + side.z, 16, 10.0F);
                if (enraged()) LightSwordEntity.spawn(this, target.getX() - side.x, target.getY(), target.getZ() - side.z, 22, 10.0F);
                playSound(SoundEvents.BEACON_POWER_SELECT, 1.0F, 1.4F);
            }
        }
    }
}
