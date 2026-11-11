package com.chigusa.jsnboss.entity;

import com.chigusa.jsnboss.registry.ModEffects;
import com.chigusa.jsnboss.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * 刃の道：地面が割れて（予告）、そこから大きな剣が突き出す。描画は BladeSpikeRenderer。
 */
public class BladeSpikeEntity extends Entity {
    private static final EntityDataAccessor<Integer> WARMUP =
            SynchedEntityData.defineId(BladeSpikeEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> KIND =
            SynchedEntityData.defineId(BladeSpikeEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> RED =
            SynchedEntityData.defineId(BladeSpikeEntity.class, EntityDataSerializers.BOOLEAN);

    private static final ItemStack[] SWORDS = {
            new ItemStack(Items.IRON_SWORD), new ItemStack(Items.DIAMOND_SWORD), new ItemStack(Items.NETHERITE_SWORD),
            new ItemStack(Items.GOLDEN_SWORD)};
    public static final int OUT = 14;      // 剣が地上に出ている時間
    private static final double RADIUS = 0.9;

    private AbstractBoss owner;
    private float damage = 14.0F;
    private final Set<UUID> hits = new HashSet<>();

    public BladeSpikeEntity(EntityType<? extends BladeSpikeEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
    }

    public static void spawn(AbstractBoss owner, double x, double y, double z, int warmup, float damage) {
        BladeSpikeEntity e = new BladeSpikeEntity(ModEntities.BLADE_SPIKE.get(), owner.level());
        e.owner = owner;
        e.damage = damage;
        e.setPos(x, y, z);
        e.setYRot(owner.getRandom().nextFloat() * 360);
        e.entityData.set(WARMUP, warmup);
        e.entityData.set(KIND, owner.getRandom().nextInt(SWORDS.length));
        e.entityData.set(RED, owner.isPhase2());
        owner.level().addFreshEntity(e);
    }

    public int getWarmup() { return this.entityData.get(WARMUP); }
    public boolean isRed() { return this.entityData.get(RED); }
    public ItemStack getSwordStack() { return SWORDS[Math.floorMod(this.entityData.get(KIND), SWORDS.length)]; }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(WARMUP, 12);
        builder.define(KIND, 0);
        builder.define(RED, false);
    }

    @Override
    public void tick() {
        super.tick();
        int t = this.tickCount, w = getWarmup();
        if (level().isClientSide) {
            particles(t, w);
            return;
        }
        if (t == w) this.playSound(SoundEvents.ANVIL_PLACE, 0.7F, 1.6F);
        if (t >= w && t < w + 4) strike();
        if (t >= w + OUT) this.discard();
    }

    private void strike() {
        for (LivingEntity e : level().getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate(RADIUS + 1, 3, RADIUS + 1),
                e -> e.isAlive() && !(e instanceof AbstractBoss) && !e.isSpectator())) {
            double dx = e.getX() - getX(), dz = e.getZ() - getZ();
            if (Math.sqrt(dx * dx + dz * dz) - e.getBbWidth() / 2.0 > RADIUS) continue;
            if (e.getY() > getY() + 2.5 || e.getY() + e.getBbHeight() < getY()) continue;
            if (!hits.add(e.getUUID())) continue;
            boolean ok = owner != null ? e.hurt(this.damageSources().mobAttack(owner), damage)
                    : e.hurt(this.damageSources().generic(), damage);
            if (ok) {
                e.setDeltaMovement(e.getDeltaMovement().add(0, 0.6, 0));
                e.hurtMarked = true;
                if (isRed()) e.addEffect(new MobEffectInstance(ModEffects.LACERATION, 60, 0));
            }
        }
    }

    private void particles(int t, int w) {
        BlockState ground = level().getBlockState(BlockPos.containing(getX(), getY() - 0.5, getZ()));
        if (ground.isAir()) ground = Blocks.STONE.defaultBlockState();
        BlockParticleOption block = new BlockParticleOption(ParticleTypes.BLOCK, ground);
        if (t < w) {
            // 予告：地面にひびが入って土ぼこり
            if (t > w - 10) {
                level().addParticle(block, getX() + (random.nextDouble() - 0.5), getY() + 0.1, getZ() + (random.nextDouble() - 0.5), 0, 0.15, 0);
            }
            if (t % 3 == 0) level().addParticle(ParticleTypes.CRIT, getX(), getY() + 0.1, getZ(), 0, 0.1, 0);
        } else if (t < w + 3) {
            for (int i = 0; i < 8; i++) {
                level().addParticle(block, getX() + (random.nextDouble() - 0.5), getY() + 0.2, getZ() + (random.nextDouble() - 0.5),
                        (random.nextDouble() - 0.5) * 0.3, 0.3, (random.nextDouble() - 0.5) * 0.3);
            }
        }
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {}

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {}

    @Override
    public boolean isPickable() { return false; }
}
