package com.chigusa.jsnboss;

import com.chigusa.jsnboss.entity.FireKnightEntity;
import com.chigusa.jsnboss.entity.IceWitchEntity;
import com.chigusa.jsnboss.entity.ZombieKingEntity;
import com.chigusa.jsnboss.entity.DarkMageEntity;
import com.chigusa.jsnboss.entity.BladeDragonEntity;
import com.chigusa.jsnboss.entity.WhiteFoxEntity;
import com.chigusa.jsnboss.entity.CreeperMotherEntity;
import com.chigusa.jsnboss.entity.FallenValkyrieEntity;
import com.chigusa.jsnboss.entity.FenrirEntity;
import com.chigusa.jsnboss.entity.GoldenFruitEntity;
import com.chigusa.jsnboss.entity.YggdrasilEntity;
import com.chigusa.jsnboss.entity.IronSpiderEntity;
import com.chigusa.jsnboss.entity.EnderSentinelEntity;
import com.chigusa.jsnboss.entity.RockGiantEntity;
import com.chigusa.jsnboss.entity.ThunderLionEntity;
import com.chigusa.jsnboss.entity.WindCatEntity;
import com.chigusa.jsnboss.registry.ModEffects;
import com.chigusa.jsnboss.registry.ModEntities;
import com.chigusa.jsnboss.registry.ModItems;
import com.chigusa.jsnboss.registry.ModMobs;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;
import net.minecraft.world.item.CreativeModeTabs;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;

/**
 * ボス関係の登録をまとめたもの。
 * 自分の MOD のメインクラス（@Mod がついたクラス）のコンストラクタで
 *     BossSetup.init(modEventBus);
 * を1行呼ぶだけで OK。
 */
public final class BossSetup {
    /** 最大HPの上限。バニラは1024なので、HP1500以上のボスのために引き上げる */
    public static final double MAX_HEALTH_CAP = 100000.0;

    public static void init(IEventBus modBus) {
        raiseMaxHealthCap();
        ModMobs.init();
        com.chigusa.jsnboss.registry.ModThemed.init();
        ModEntities.ENTITY_TYPES.register(modBus);
        ModItems.ITEMS.register(modBus);
        ModEffects.MOB_EFFECTS.register(modBus);
        com.chigusa.jsnboss.registry.ModBlocks.BLOCKS.register(modBus);
        com.chigusa.jsnboss.vfx.ModParticles.PARTICLE_TYPES.register(modBus);
        modBus.addListener(BossSetup::onAttributes);
        modBus.addListener(EarlyClassInit::onCommonSetup);
        com.chigusa.jsnboss.registry.ModTabs.TABS.register(modBus);
    }

    /** accesstransformer.cfg で書き換え可能にした上限値を引き上げる（ほかのMODがもっと高くしていたら触らない） */
    private static void raiseMaxHealthCap() {
        if (Attributes.MAX_HEALTH.value() instanceof RangedAttribute health && health.getMaxValue() < MAX_HEALTH_CAP) {
            health.maxValue = MAX_HEALTH_CAP;
        }
    }

    private static void onAttributes(EntityAttributeCreationEvent event) {
        event.put(ModEntities.FIRE_KNIGHT.get(), FireKnightEntity.createAttributes().build());
        event.put(ModEntities.ICE_WITCH.get(), IceWitchEntity.createAttributes().build());
        event.put(ModEntities.ICE_WITCH_CLONE.get(), IceWitchEntity.createCloneAttributes().build());
        event.put(ModEntities.ZOMBIE_KING.get(), ZombieKingEntity.createAttributes().build());
        event.put(ModEntities.DARK_MAGE.get(), DarkMageEntity.createAttributes().build());
        event.put(ModEntities.BLADE_DRAGON.get(), BladeDragonEntity.createAttributes().build());
        event.put(ModEntities.WHITE_FOX.get(), WhiteFoxEntity.createAttributes().build());
        event.put(ModEntities.WHITE_FOX_CLONE.get(), WhiteFoxEntity.createCloneAttributes().build());
        event.put(ModEntities.WIND_CAT.get(), WindCatEntity.createAttributes().build());
        event.put(ModEntities.THUNDER_LION.get(), ThunderLionEntity.createAttributes().build());
        event.put(ModEntities.ROCK_GIANT.get(), RockGiantEntity.createAttributes().build());
        event.put(ModEntities.ENDER_SENTINEL.get(), EnderSentinelEntity.createAttributes().build());
        event.put(ModEntities.CREEPER_MOTHER.get(), CreeperMotherEntity.createAttributes().build());
        event.put(ModEntities.FENRIR.get(), FenrirEntity.createAttributes().build());
        event.put(ModEntities.IRON_SPIDER.get(), IronSpiderEntity.createAttributes().build());
        event.put(ModEntities.FALLEN_VALKYRIE.get(), FallenValkyrieEntity.createAttributes().build());
        event.put(ModEntities.YGGDRASIL.get(), YggdrasilEntity.createAttributes().build());
        event.put(ModEntities.GOLDEN_FRUIT.get(), GoldenFruitEntity.createAttributes().build());
        event.put(ModEntities.YGGDRASIL_SPIRIT.get(), com.chigusa.jsnboss.entity.YggdrasilSpiritEntity.createAttributes().build());
        event.put(ModEntities.SPIRIT_FAIRY.get(), com.chigusa.jsnboss.entity.SpiritFairyEntity.createAttributes().build());

        // 亜種（バニラと同じ能力値に、少しだけ上乗せ）
        event.put(ModMobs.SOUL_BLAZE.get(), net.minecraft.world.entity.monster.Blaze.createAttributes().add(Attributes.MAX_HEALTH, 50.0).build());
        event.put(ModMobs.FROST_BLAZE.get(), net.minecraft.world.entity.monster.Blaze.createAttributes().add(Attributes.MAX_HEALTH, 50.0).build());
        event.put(ModMobs.TOMB_ZOMBIE.get(), net.minecraft.world.entity.monster.Zombie.createAttributes().add(Attributes.MAX_HEALTH, 50.0).build());
        event.put(ModMobs.THUNDER_ZOMBIE.get(), net.minecraft.world.entity.monster.Zombie.createAttributes().add(Attributes.MAX_HEALTH, 50.0).build());
        event.put(ModMobs.FIRE_SKELETON.get(), net.minecraft.world.entity.monster.AbstractSkeleton.createAttributes().add(Attributes.MAX_HEALTH, 50.0).build());
        event.put(ModMobs.POISON_SKELETON.get(), net.minecraft.world.entity.monster.AbstractSkeleton.createAttributes().add(Attributes.MAX_HEALTH, 50.0).build());
        event.put(ModMobs.FROST_CREEPER.get(), net.minecraft.world.entity.monster.Creeper.createAttributes().add(Attributes.MAX_HEALTH, 50.0).build());
        event.put(ModMobs.MAGMA_SPIDER.get(), net.minecraft.world.entity.monster.Spider.createAttributes().add(Attributes.MAX_HEALTH, 50.0).build());
        event.put(ModMobs.MOSS_ZOMBIE.get(), net.minecraft.world.entity.monster.Zombie.createAttributes().add(Attributes.MAX_HEALTH, 50.0).build());
        event.put(ModMobs.GOLD_ZOMBIE.get(), net.minecraft.world.entity.monster.Zombie.createAttributes().add(Attributes.MAX_HEALTH, 50.0).build());
        event.put(ModMobs.ORE_ZOMBIE.get(), net.minecraft.world.entity.monster.Zombie.createAttributes().add(Attributes.MAX_HEALTH, 50.0).build());
        event.put(ModMobs.PRISMARINE_DROWNED.get(), net.minecraft.world.entity.monster.Zombie.createAttributes().add(Attributes.MAX_HEALTH, 50.0).build());
        event.put(ModMobs.HONEY_SPIDER.get(), net.minecraft.world.entity.monster.Spider.createAttributes().add(Attributes.MAX_HEALTH, 50.0).build());
        event.put(ModMobs.SKY_SKELETON.get(), net.minecraft.world.entity.monster.AbstractSkeleton.createAttributes().add(Attributes.MAX_HEALTH, 50.0).build());
        event.put(ModMobs.SCULK_SKELETON.get(), net.minecraft.world.entity.monster.AbstractSkeleton.createAttributes().add(Attributes.MAX_HEALTH, 50.0).build());
        event.put(ModMobs.GLOW_SLIME.get(), net.minecraft.world.entity.monster.Monster.createMonsterAttributes().build());
        // テーマ亜種（HP 50）
        for (var e : com.chigusa.jsnboss.registry.ModThemed.TYPES.entrySet()) {
            String base = com.chigusa.jsnboss.registry.ModThemed.BASE_OF.get(e.getKey());
            var attrs = switch (base) {
                case "zombie" -> net.minecraft.world.entity.monster.Zombie.createAttributes();
                case "skeleton" -> net.minecraft.world.entity.monster.AbstractSkeleton.createAttributes();
                case "spider" -> net.minecraft.world.entity.monster.Spider.createAttributes();
                case "slime" -> net.minecraft.world.entity.monster.Monster.createMonsterAttributes();
                case "blaze" -> net.minecraft.world.entity.monster.Blaze.createAttributes();
                default -> net.minecraft.world.entity.monster.Creeper.createAttributes();
            };
            if (!base.equals("slime")) attrs.add(Attributes.MAX_HEALTH, 50.0);
            event.put(e.getValue().get(), attrs.build());
        }
    }


    private BossSetup() {}
}
