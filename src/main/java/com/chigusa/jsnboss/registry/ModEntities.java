package com.chigusa.jsnboss.registry;

import com.chigusa.jsnboss.BossConstants;
import com.chigusa.jsnboss.entity.FireKnightEntity;
import com.chigusa.jsnboss.entity.FirePillarEntity;
import com.chigusa.jsnboss.entity.FlameSlashEntity;
import com.chigusa.jsnboss.entity.FrostFieldEntity;
import com.chigusa.jsnboss.entity.IceSpearEntity;
import com.chigusa.jsnboss.entity.IceSpikeEntity;
import com.chigusa.jsnboss.entity.IceWitchEntity;
import com.chigusa.jsnboss.entity.GraveHandEntity;
import com.chigusa.jsnboss.entity.ShockwaveEntity;
import com.chigusa.jsnboss.entity.ZombieKingEntity;
import com.chigusa.jsnboss.entity.DarkMageEntity;
import com.chigusa.jsnboss.entity.DarkOrbEntity;
import com.chigusa.jsnboss.entity.BladeDragonEntity;
import com.chigusa.jsnboss.entity.BladeSpikeEntity;
import com.chigusa.jsnboss.entity.FlyingSwordEntity;
import com.chigusa.jsnboss.entity.FoxfireEntity;
import com.chigusa.jsnboss.entity.LightPillarEntity;
import com.chigusa.jsnboss.entity.LightSwordEntity;
import com.chigusa.jsnboss.entity.WhiteFoxEntity;
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

    // ------------------------------------------------------------ ゾンビの王
    /** 幅2.2 × 高さ4.0ブロック */
    public static final DeferredHolder<EntityType<?>, EntityType<ZombieKingEntity>> ZOMBIE_KING =
            ENTITY_TYPES.register("zombie_king", () -> EntityType.Builder.<ZombieKingEntity>of(ZombieKingEntity::new, MobCategory.MONSTER)
                    .sized(2.2F, 4.0F).clientTrackingRange(10).build("zombie_king"));

    public static final DeferredHolder<EntityType<?>, EntityType<ShockwaveEntity>> SHOCKWAVE =
            ENTITY_TYPES.register("shockwave", () -> EntityType.Builder.<ShockwaveEntity>of(ShockwaveEntity::new, MobCategory.MISC)
                    .sized(1.0F, 0.5F).clientTrackingRange(10).noSave().build("shockwave"));

    public static final DeferredHolder<EntityType<?>, EntityType<GraveHandEntity>> GRAVE_HAND =
            ENTITY_TYPES.register("grave_hand", () -> EntityType.Builder.<GraveHandEntity>of(GraveHandEntity::new, MobCategory.MISC)
                    .sized(1.0F, 1.0F).clientTrackingRange(8).noSave().build("grave_hand"));

    // ------------------------------------------------------------ 闇の魔導士
    /** 幅1.0 × 高さ2.6ブロック */
    public static final DeferredHolder<EntityType<?>, EntityType<DarkMageEntity>> DARK_MAGE =
            ENTITY_TYPES.register("dark_mage", () -> EntityType.Builder.<DarkMageEntity>of(DarkMageEntity::new, MobCategory.MONSTER)
                    .sized(1.0F, 2.6F).clientTrackingRange(10).build("dark_mage"));

    public static final DeferredHolder<EntityType<?>, EntityType<DarkOrbEntity>> DARK_ORB =
            ENTITY_TYPES.register("dark_orb", () -> EntityType.Builder.<DarkOrbEntity>of(DarkOrbEntity::new, MobCategory.MISC)
                    .sized(0.6F, 0.6F).clientTrackingRange(6).updateInterval(1).noSave().build("dark_orb"));

    // ------------------------------------------------------------ 影刃竜
    /** 幅3.0 × 高さ2.4ブロック（体は鼻先から尻尾まで約5.5ブロック） */
    public static final DeferredHolder<EntityType<?>, EntityType<BladeDragonEntity>> BLADE_DRAGON =
            ENTITY_TYPES.register("blade_dragon", () -> EntityType.Builder.<BladeDragonEntity>of(BladeDragonEntity::new, MobCategory.MONSTER)
                    .sized(3.0F, 2.4F).clientTrackingRange(12).build("blade_dragon"));

    public static final DeferredHolder<EntityType<?>, EntityType<FlyingSwordEntity>> FLYING_SWORD =
            ENTITY_TYPES.register("flying_sword", () -> EntityType.Builder.<FlyingSwordEntity>of(FlyingSwordEntity::new, MobCategory.MISC)
                    .sized(0.5F, 0.5F).clientTrackingRange(6).updateInterval(1).noSave().build("flying_sword"));

    public static final DeferredHolder<EntityType<?>, EntityType<BladeSpikeEntity>> BLADE_SPIKE =
            ENTITY_TYPES.register("blade_spike", () -> EntityType.Builder.<BladeSpikeEntity>of(BladeSpikeEntity::new, MobCategory.MISC)
                    .sized(1.0F, 2.5F).clientTrackingRange(8).noSave().build("blade_spike"));

    // ------------------------------------------------------------ 光の白狐
    /** 幅2.0 × 高さ2.6ブロック */
    public static final DeferredHolder<EntityType<?>, EntityType<WhiteFoxEntity>> WHITE_FOX =
            ENTITY_TYPES.register("white_fox", () -> EntityType.Builder.<WhiteFoxEntity>of(WhiteFoxEntity::new, MobCategory.MONSTER)
                    .sized(2.0F, 2.6F).clientTrackingRange(10).build("white_fox"));

    /** 分身（見た目は第1形態のまま、HP1、ボスバーなし） */
    public static final DeferredHolder<EntityType<?>, EntityType<WhiteFoxEntity>> WHITE_FOX_CLONE =
            ENTITY_TYPES.register("white_fox_clone", () -> EntityType.Builder.<WhiteFoxEntity>of(WhiteFoxEntity::new, MobCategory.MONSTER)
                    .sized(2.0F, 2.6F).clientTrackingRange(10).noSave().noSummon().build("white_fox_clone"));

    public static final DeferredHolder<EntityType<?>, EntityType<FoxfireEntity>> FOXFIRE =
            ENTITY_TYPES.register("foxfire", () -> EntityType.Builder.<FoxfireEntity>of(FoxfireEntity::new, MobCategory.MISC)
                    .sized(0.6F, 0.6F).clientTrackingRange(6).updateInterval(1).noSave().build("foxfire"));

    public static final DeferredHolder<EntityType<?>, EntityType<LightPillarEntity>> LIGHT_PILLAR =
            ENTITY_TYPES.register("light_pillar", () -> EntityType.Builder.<LightPillarEntity>of(LightPillarEntity::new, MobCategory.MISC)
                    .sized(1.0F, 1.0F).clientTrackingRange(8).noSave().build("light_pillar"));

    public static final DeferredHolder<EntityType<?>, EntityType<LightSwordEntity>> LIGHT_SWORD =
            ENTITY_TYPES.register("light_sword", () -> EntityType.Builder.<LightSwordEntity>of(LightSwordEntity::new, MobCategory.MISC)
                    .sized(1.0F, 2.5F).clientTrackingRange(8).noSave().build("light_sword"));

    private ModEntities() {}
}
