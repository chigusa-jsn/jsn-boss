package com.chigusa.jsnboss.mob;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.util.Mth;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.monster.Slime;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import org.jetbrains.annotations.Nullable;

/** テーマ亜種のスライム（テーマは登録のときに決まる。効果・ドロップは VariantEffects） */
public class ThemedSlime extends Slime implements Themed {
    private static final EntityDataAccessor<Integer> TIER =
            SynchedEntityData.defineId(ThemedSlime.class, EntityDataSerializers.INT);
    private final Theme theme;

    public ThemedSlime(EntityType<? extends Slime> type, Level level, Theme theme) {
        super(type, level);
        this.theme = theme;
    }

    @Override
    public Theme theme() { return theme; }

    @Override
    public int oreTier() { return Mth.clamp(this.entityData.get(TIER), 0, 3); }

    public void setOreTier(int t) {
        this.entityData.set(TIER, Mth.clamp(t, 0, 3));
        OreTiers.apply(this, t);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(TIER, 0);
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType type, @Nullable SpawnGroupData data) {
        SpawnGroupData d = super.finalizeSpawn(level, difficulty, type, data);
        if (theme == Theme.ORE) setOreTier(OreTiers.forY(this.getBlockY()));
        return d;
    }

    @Override
    protected Component getTypeName() {
        if (theme != Theme.ORE) return super.getTypeName();
        return Component.translatable("entity.jsnboss." + BuiltInRegistries.ENTITY_TYPE.getKey(getType()).getPath() + "." + OreTiers.NAMES[oreTier()]);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("OreTier", oreTier());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (theme == Theme.ORE) setOreTier(tag.getInt("OreTier"));
    }

    /** 大きさ4で HP 50（分裂した小さいのは少なめ） */
    @Override
    public void setSize(int size, boolean resetHealth) {
        super.setSize(size, resetHealth);
        var hp = this.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH);
        if (hp != null) hp.setBaseValue(Math.max(1, size) * 12.5);
        if (resetHealth) this.setHealth(this.getMaxHealth());
    }
}
