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

import java.util.List;

/** もぐら（弱い敵・HP60）。地面に潜って近づき、足もとから飛び出す（ブロックは壊さない）。潜っている間は攻撃が当たらない */
public class BurrowMole extends DungeonMob {
    static final Move BURROW = new Move("burrow", 50, 90, 3, 16, 2, false);
    static final Move SCRATCH = new Move("scratch", 12, 16, 0, 2.0, 2, false);
    private static final List<Move> MOVES = List.of(BURROW, SCRATCH);
    private boolean underground;

    public BurrowMole(EntityType<? extends Monster> type, Level level) {
        super(type, level, Tier.WEAK, BossEvent.BossBarColor.WHITE);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 60.0).add(Attributes.ARMOR, 2.0).add(Attributes.ATTACK_DAMAGE, 4.0)
                .add(Attributes.MOVEMENT_SPEED, 0.26).add(Attributes.FOLLOW_RANGE, 20.0);
    }

    @Override protected List<Move> moves() { return MOVES; }

    @Override
    protected void tickMove(Move m, int t, LivingEntity target) {
        var dirt = new BlockParticleOption(ParticleTypes.BLOCK, Blocks.DIRT.defaultBlockState());
        ServerLevel sl = level() instanceof ServerLevel s ? s : null;
        if (m == BURROW) {
            if (t == 8) { underground = true; setInvisible(true); playSound(SoundEvents.ROOTED_DIRT_BREAK, 1.0F, 0.8F); }
            if (underground && t < 40) {
                if (target != null && t % 5 == 0) getNavigation().moveTo(target, 1.6);
                if (sl != null) sl.sendParticles(dirt, getX(), getY() + 0.1, getZ(), 3, 0.3, 0.05, 0.3, 0.05);
            }
            if (t == 40) {
                if (target != null && distanceTo(target) < 6) teleportTo(target.getX(), groundY(target.getX(), target.getY() + 1, target.getZ()), target.getZ());
                underground = false;
                setInvisible(false);
                for (LivingEntity e : hitAround(1.6, 5.0F, 0.2)) launch(e, 0.5);
                if (sl != null) sl.sendParticles(dirt, getX(), getY() + 0.2, getZ(), 40, 0.6, 0.3, 0.6, 0.15);
                playSound(SoundEvents.ROOTED_DIRT_BREAK, 1.4F, 0.6F);
            }
        } else if (m == SCRATCH) {
            if (t == 6) hitFront(2.2, 60, 4.0F, 0.2);
        }
    }

    @Override
    protected void bossAiStep() {
        if (!isAttacking() && underground) { underground = false; setInvisible(false); }
        super.bossAiStep();
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (underground && !source.is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY)) return false;
        return super.hurt(source, amount);
    }

    @Override
    public void die(DamageSource source) {
        setInvisible(false);
        super.die(source);
    }
}
