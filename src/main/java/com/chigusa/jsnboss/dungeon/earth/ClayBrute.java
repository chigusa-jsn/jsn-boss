package com.chigusa.jsnboss.dungeon.earth;

import com.chigusa.jsnboss.dungeon.DungeonMob;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
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
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/** 粘土の巨兵（強い敵・HP280）。泥の塊を投げて足を遅くし、両こぶしで地面を叩く */
public class ClayBrute extends DungeonMob {
    static final Move MUD = new Move("mud", 24, 60, 3, 16, 3, true);
    static final Move SMASH = new Move("smash", 28, 36, 0, 3.6, 4, false);
    private static final List<Move> MOVES = List.of(MUD, SMASH);
    private Vec3 mudTarget = Vec3.ZERO;

    public ClayBrute(EntityType<? extends Monster> type, Level level) {
        super(type, level, Tier.STRONG, BossEvent.BossBarColor.RED);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 280.0).add(Attributes.ARMOR, 8.0).add(Attributes.ATTACK_DAMAGE, 10.0)
                .add(Attributes.MOVEMENT_SPEED, 0.22).add(Attributes.KNOCKBACK_RESISTANCE, 0.8).add(Attributes.FOLLOW_RANGE, 28.0);
    }

    @Override protected List<Move> moves() { return MOVES; }

    @Override
    protected void tickMove(Move m, int t, LivingEntity target) {
        ServerLevel sl = level() instanceof ServerLevel s ? s : null;
        var mud = new BlockParticleOption(ParticleTypes.BLOCK, Blocks.MUD.defaultBlockState());
        if (m == MUD) {
            if (t == 10 && target != null) { mudTarget = target.position(); playSound(SoundEvents.MUD_BREAK, 1.2F, 0.6F); }
            if (t >= 10 && t <= 20 && sl != null) {                  // 放物線で飛ぶ泥
                double f = (t - 10) / 10.0;
                Vec3 from = position().add(0, getBbHeight(), 0);
                Vec3 p = from.lerp(mudTarget, f).add(0, Math.sin(f * Math.PI) * 3, 0);
                sl.sendParticles(mud, p.x, p.y, p.z, 6, 0.15, 0.15, 0.15, 0);
            }
            if (t == 20) {
                for (LivingEntity e : inRadius(mudTarget, 2.2, 2)) {
                    if (damage(e, 5.0F, 0.2)) e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 80, 2));
                }
                if (sl != null) sl.sendParticles(mud, mudTarget.x, mudTarget.y + 0.2, mudTarget.z, 50, 1.2, 0.1, 1.2, 0.1);
                playSound(SoundEvents.MUD_PLACE, 1.4F, 0.6F);
            }
        } else if (m == SMASH) {
            if (t == 14) {
                Vec3 at = position().add(forward().scale(2));
                for (LivingEntity e : inRadius(at, 2.8, 2)) { if (damage(e, 10.0F, 0.6)) launch(e, 0.45); }
                if (sl != null) sl.sendParticles(mud, at.x, getY() + 0.2, at.z, 40, 1.4, 0.1, 1.4, 0.15);
                playSound(SoundEvents.MUD_BREAK, 1.5F, 0.4F);
            }
        }
    }
}
