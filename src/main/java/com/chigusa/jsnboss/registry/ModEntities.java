package com.chigusa.jsnboss.registry;

import com.chigusa.jsnboss.BossConstants;
import com.chigusa.jsnboss.entity.FireKnightEntity;
import com.chigusa.jsnboss.entity.FirePillarEntity;
import com.chigusa.jsnboss.entity.FlameSlashEntity;
import com.chigusa.jsnboss.entity.FrostFieldEntity;
import com.chigusa.jsnboss.entity.IceSpearEntity;
import com.chigusa.jsnboss.entity.IceSpikeEntity;
import com.chigusa.jsnboss.entity.IceWitchEntity;
import com.chigusa.jsnboss.entity.KnightFireballEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(Registries.ENTITY_TYPE, BossConstants.MODID);

    /** 幅1.6 × 高さ3.5ブロック */
    public static final DeferredHolder<EntityType<?>, EntityType<FireKnightEntity>> FIRE_KNIGHT =
            ENTITY_TYPES.register("fire_knight", () -> EntityType.Builder.of(FireKnightEntity::new, MobCategory.MONSTER)
                    .sized(1.6F, 3.5F).fireImmune().clientTrackingRange(10).build("fire_knight"));

    public static final DeferredHolder<EntityType<?>, EntityType<KnightFireballEntity>> KNIGHT_FIREBALL =
            ENTITY_TYPES.register("knight_fireball", () -> EntityType.Builder.<KnightFireballEntity>of(KnightFireballEntity::new, MobCategory.MISC)
                    .sized(0.8F, 0.8F).clientTrackingRange(6).updateInterval(1).noSave().build("knight_fireball"));

    public static final DeferredHolder<EntityType<?>, EntityType<FlameSlashEntity>> FLAME_SLASH =
            ENTITY_TYPES.register("flame_slash", () -> EntityType.Builder.<FlameSlashEntity>of(FlameSlashEntity::new, MobCategory.MISC)
                    .sized(0.5F, 0.5F).clientTrackingRange(6).updateInterval(1).noSave().build("flame_slash"));

    public static final DeferredHolder<EntityType<?>, EntityType<FirePillarEntity>> FIRE_PILLAR =
            ENTITY_TYPES.register("fire_pillar", () -> EntityType.Builder.<FirePillarEntity>of(FirePillarEntity::new, MobCategory.MISC)
                    .sized(1.0F, 1.0F).clientTrackingRange(8).noSave().build("fire_pillar"));

    // ------------------------------------------------------------ 氷の魔女
    /** 幅1.2 × 高さ3.1ブロック */
    public static final DeferredHolder<EntityType<?>, EntityType<IceWitchEntity>> ICE_WITCH =
            ENTITY_TYPES.register("ice_witch", () -> EntityType.Builder.<IceWitchEntity>of(IceWitchEntity::new, MobCategory.MONSTER)
                    .sized(1.2F, 3.1F).immuneTo(net.minecraft.world.level.block.Blocks.POWDER_SNOW).clientTrackingRange(10).build("ice_witch"));

    /** 分身（見た目は同じ、HP1、ボスバーなし） */
    public static final DeferredHolder<EntityType<?>, EntityType<IceWitchEntity>> ICE_WITCH_CLONE =
            ENTITY_TYPES.register("ice_witch_clone", () -> EntityType.Builder.<IceWitchEntity>of(IceWitchEntity::new, MobCategory.MONSTER)
                    .sized(1.2F, 3.1F).clientTrackingRange(10).noSave().noSummon().build("ice_witch_clone"));

    public static final DeferredHolder<EntityType<?>, EntityType<IceSpearEntity>> ICE_SPEAR =
            ENTITY_TYPES.register("ice_spear", () -> EntityType.Builder.<IceSpearEntity>of(IceSpearEntity::new, MobCategory.MISC)
                    .sized(0.5F, 0.5F).clientTrackingRange(6).updateInterval(1).noSave().build("ice_spear"));

    public static final DeferredHolder<EntityType<?>, EntityType<IceSpikeEntity>> ICE_SPIKE =
            ENTITY_TYPES.register("ice_spike", () -> EntityType.Builder.<IceSpikeEntity>of(IceSpikeEntity::new, MobCategory.MISC)
                    .sized(1.0F, 1.0F).clientTrackingRange(8).noSave().build("ice_spike"));

    public static final DeferredHolder<EntityType<?>, EntityType<FrostFieldEntity>> FROST_FIELD =
            ENTITY_TYPES.register("frost_field", () -> EntityType.Builder.<FrostFieldEntity>of(FrostFieldEntity::new, MobCategory.MISC)
                    .sized(1.0F, 0.5F).clientTrackingRange(8).noSave().build("frost_field"));

    private ModEntities() {}
}
