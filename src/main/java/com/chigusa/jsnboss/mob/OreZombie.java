package com.chigusa.jsnboss.mob;

import com.chigusa.jsnboss.BossConstants;
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
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import org.jetbrains.annotations.Nullable;

/**
 * 鉱石のゾンビ：体が鉱石でできたゾンビ。深いところほど硬い鉱石になる。
 * 石（y30〜）・鉄（y0〜）・金（y-32〜）・ダイヤ（それより下）の4段階で、防御力が上がる。
 */
public class OreZombie extends Zombie implements Themed {
    @Override
    public Theme theme() { return Theme.ORE; }

    public static final String[] TIERS = {"stone", "iron", "gold", "diamond"};
    private static final double[] ARMOR = {4, 8, 10, 14};
    private static final double[] TOUGH = {0, 1, 2, 4};
    private static final EntityDataAccessor<Integer> TIER =
            SynchedEntityData.defineId(OreZombie.class, EntityDataSerializers.INT);

    public OreZombie(EntityType<? extends Zombie> type, Level level) { super(type, level); }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(TIER, 0);
    }

    @Override
    public int oreTier() { return tier(); }

    public int tier() { return Mth.clamp(this.entityData.get(TIER), 0, 3); }

    public void setTier(int t) {
        t = Mth.clamp(t, 0, 3);
        this.entityData.set(TIER, t);
        var armor = getAttribute(Attributes.ARMOR);
        var tough = getAttribute(Attributes.ARMOR_TOUGHNESS);
        if (armor != null) {
            armor.removeModifier(BossConstants.id("ore_armor"));
            armor.addPermanentModifier(new AttributeModifier(BossConstants.id("ore_armor"), ARMOR[t], AttributeModifier.Operation.ADD_VALUE));
        }
        if (tough != null) {
            tough.removeModifier(BossConstants.id("ore_tough"));
            tough.addPermanentModifier(new AttributeModifier(BossConstants.id("ore_tough"), TOUGH[t], AttributeModifier.Operation.ADD_VALUE));
        }
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType type, @Nullable SpawnGroupData data) {
        SpawnGroupData d = super.finalizeSpawn(level, difficulty, type, data);
        int y = this.getBlockY();
        setTier(y >= 30 ? 0 : y >= 0 ? 1 : y >= -32 ? 2 : 3);
        return d;
    }

    @Override
    protected Component getTypeName() {
        return Component.translatable("entity.jsnboss.ore_zombie." + TIERS[tier()]);
    }

    @Override
    protected boolean isSunSensitive() { return false; }

    @Override
    protected boolean convertsInWater() { return false; }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("OreTier", tier());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        setTier(tag.getInt("OreTier"));
    }
}
