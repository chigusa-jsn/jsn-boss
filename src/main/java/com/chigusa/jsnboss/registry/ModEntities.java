package com.chigusa.jsnboss.registry;

import com.chigusa.jsnboss.BossConstants;
import com.chigusa.jsnboss.entity.FireKnightEntity;
import com.chigusa.jsnboss.entity.FirePillarEntity;
import com.chigusa.jsnboss.entity.FlameSlashEntity;
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

    private ModEntities() {}
}
