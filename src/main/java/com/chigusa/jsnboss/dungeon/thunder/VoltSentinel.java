package com.chigusa.jsnboss.dungeon.thunder;

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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** 雷の機兵（強い敵・HP280）。銅の人形。近くの相手から相手へつながる連鎖雷と、まわりへの放電 */
public class VoltSentinel extends DungeonMob {
    static final Move CHAIN = new Move("chain", 28, 60, 0, 14, 3, true);
    static final Move DISCHARGE = new Move("discharge", 32, 80, 0, 4, 3, false);
    private static final List<Move> MOVES = List.of(CHAIN, DISCHARGE);

    public VoltSentinel(EntityType<? extends Monster> type, Level level) {
        super(type, level, Tier.STRONG, BossEvent.BossBarColor.YELLOW);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 280.0).add(Attributes.ARMOR, 10.0).add(Attributes.ATTACK_DAMAGE, 8.0)
                .add(Attributes.MOVEMENT_SPEED, 0.22).add(Attributes.KNOCKBACK_RESISTANCE, 0.8).add(Attributes.FOLLOW_RANGE, 28.0);
    }

    @Override protected List<Move> moves() { return MOVES; }

    @Override
    protected void tickMove(Move m, int t, LivingEntity target) {
        if (m == CHAIN) {
            if (t == 14 && target != null) chain(target);
        } else if (m == DISCHARGE) {
            if (t == 12 || t == 18) {
                for (LivingEntity e : inRadius(position(), 4.5, 3)) {
                    if (damage(e, 6.0F, 0.6)) shock(e, 40);
                }
                if (level() instanceof ServerLevel sl) sl.sendParticles(ParticleTypes.ELECTRIC_SPARK, getX(), getY() + 1.2, getZ(), 60, 2.2, 0.8, 2.2, 0.3);
                playSound(SoundEvents.BEACON_DEACTIVATE, 1.0F, 1.8F);
            }
        }
    }

    /** 最初の相手から、6ブロック以内の別の相手へ最大3回つながる */
    private void chain(LivingEntity first) {
        Set<LivingEntity> hit = new HashSet<>();
        Vec3 from = new Vec3(getX(), getY() + 2.4, getZ());
        LivingEntity cur = first;
        for (int i = 0; i < 3 && cur != null; i++) {
            Vec3 to = cur.position().add(0, cur.getBbHeight() * 0.5, 0);
            sparkLine(from, to);
            if (damage(cur, i == 0 ? 8.0F : 6.0F, 0.3)) shock(cur, 30);
            hit.add(cur);
            from = to;
            List<LivingEntity> next = new ArrayList<>(inRadius(cur.position(), 6, 3));
            next.removeIf(hit::contains);
            cur = next.isEmpty() ? null : next.get(0);
        }
        playSound(SoundEvents.TRIDENT_THUNDER.value(), 0.5F, 1.8F);
    }

    @Override
    protected void clientTick() {
        if (random.nextInt(3) == 0) level().addParticle(ParticleTypes.ELECTRIC_SPARK, getX(), getY() + 2.8, getZ(), 0, 0.05, 0);
    }
}
