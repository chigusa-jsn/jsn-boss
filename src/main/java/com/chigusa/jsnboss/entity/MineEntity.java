package com.chigusa.jsnboss.entity;

import com.chigusa.jsnboss.registry.ModEntities;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/** 地雷：放り出されて地面に落ち、0.5 秒後から作動。踏まれるか 3 秒たつと爆発（ブロックは壊さない） */
public class MineEntity extends Entity {
    public static final int FUSE = 70, ARM = 10;
    private static final Vector3f GREEN = new Vector3f(0.5F, 1.0F, 0.4F);

    private AbstractBoss owner;
    private float damage = 10.0F;

    public MineEntity(EntityType<? extends MineEntity> type, Level level) {
        super(type, level);
    }

    public static void toss(AbstractBoss owner, Vec3 from, Vec3 vel, float damage) {
        MineEntity e = new MineEntity(ModEntities.MINE.get(), owner.level());
        e.owner = owner;
        e.damage = damage;
        e.setPos(from);
        e.setDeltaMovement(vel);
        owner.level().addFreshEntity(e);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {}

    @Override
    public void tick() {
        super.tick();
        Vec3 v = this.getDeltaMovement().add(0, -0.06, 0);
        this.move(MoverType.SELF, v);
        this.setDeltaMovement(this.onGround() ? v.multiply(0.5, 0, 0.5) : v.scale(0.98));
        if (level().isClientSide) {
            int rate = this.tickCount > FUSE - 20 ? 3 : 10;
            if (this.tickCount % rate == 0) {
                level().addParticle(new DustParticleOptions(GREEN, 1.5F), getX(), getY() + 0.5, getZ(), 0, 0, 0);
                level().addParticle(ParticleTypes.SMOKE, getX(), getY() + 0.5, getZ(), 0, 0.03, 0);
            }
            return;
        }
        if (this.tickCount % 10 == 0) playSound(SoundEvents.NOTE_BLOCK_HAT.value(), 0.6F, 1.5F + this.tickCount / 100.0F);
        boolean stepped = this.tickCount > ARM && !level().getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate(0.6, 1.0, 0.6),
                e -> e.isAlive() && !(e instanceof AbstractBoss) && !e.isSpectator()).isEmpty();
        if (stepped || this.tickCount >= FUSE) {
            Blasts.blast(level(), owner, this.position(), 2.5, damage);
            this.discard();
        }
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {}

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {}

    @Override
    public boolean isPickable() { return false; }
}
