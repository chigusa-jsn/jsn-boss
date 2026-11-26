package com.chigusa.jsnboss;

import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;

/**
 * SynchedEntityData の ID 登録表（ClassTreeIdRegistry）はスレッドセーフではない。
 * エンティティクラスが初めて読み込まれた瞬間にそこへ書き込むので、
 * サーバースレッドで新しいモブ／弾が初登場するのと同時に、
 * クライアントスレッドで JEI やクリエタブが絵画などを初めて読み込むと
 * 表が壊れてクラッシュする（Painting の NoClassDefFoundError）。
 * 起動時のメインスレッドで先に全部読み込んでおいて、競合を起こさないようにする。
 */
public final class EarlyClassInit {
    private static final String[] OURS = {
        "mob.ThemedZombie", "mob.ThemedSkeleton", "mob.ThemedSpider", "mob.ThemedSlime",
        "mob.ThemedBlaze", "mob.ThemedCreeper", "mob.OreZombie", "mob.MobBoltEntity",
        "mob.MossZombie", "mob.GoldZombie", "mob.HoneySpider", "mob.SkySkeleton",
        "mob.SculkSkeleton", "mob.PrismarineDrowned", "mob.GlowSlime",
        "mob.FireSkeleton", "mob.FrostBlaze", "mob.FrostCreeper", "mob.MagmaSpider",
        "mob.PoisonCloudArrow", "mob.PoisonSkeleton", "mob.SoulBlaze", "mob.ThunderZombie",
        "mob.TombZombie", "mob.VariantBlaze",
        "entity.AbstractBoss", "entity.ShockwaveEntity", "entity.FissureEntity", "entity.BoulderEntity",
        "entity.FrostFieldEntity", "entity.IceSpearEntity", "entity.RockPillarEntity",
        "entity.YggdrasilSpiritEntity", "entity.SpiritFairyEntity", "entity.FallenValkyrieEntity",
        "entity.DroneShotEntity", "entity.LightSwordEntity", "entity.LightningMarkEntity",
        "entity.FeatherShotEntity", "entity.FirePillarEntity", "entity.BladeSpikeEntity",
        "entity.FoxfireEntity", "entity.DarkOrbEntity", "entity.KnightFireballEntity",
        "entity.IceSpikeEntity", "entity.FlameSlashEntity", "entity.FlyingSwordEntity",
        "entity.YggdrasilEntity", "entity.TornadoEntity",
        "dungeon.DungeonMob", "dungeon.fire.FlameSquire", "dungeon.fire.LavaArmor", "dungeon.fire.FlameEye",
        "dungeon.fire.EmberImp", "dungeon.fire.CinderBat",
        "dungeon.ice.FrostMaiden", "dungeon.ice.PackAlpha", "dungeon.ice.FrostKnight", "dungeon.ice.FrostfangBear",
        "dungeon.ice.FrostSprite", "dungeon.ice.IcicleCrawler",
        "dungeon.undead.RottingGuard", "dungeon.undead.GraveReaper", "dungeon.undead.BoneJuggernaut",
        "dungeon.undead.CrawlingBones", "dungeon.undead.GraveWisp",
        "dungeon.dark.ShadowApprentice", "dungeon.dark.ShadowAssassin", "dungeon.dark.Grimoire",
        "dungeon.dark.ShadeImp", "dungeon.dark.DarkEye",
        "dungeon.wind.WhirlLynx", "dungeon.wind.GaleEagle", "dungeon.wind.TornadoSpirit", "dungeon.wind.WindPuff", "dungeon.wind.GaleWeasel",
        "dungeon.thunder.ThunderLioness", "dungeon.thunder.VoltSentinel", "dungeon.thunder.StormLizard",
        "dungeon.thunder.SparkJelly", "dungeon.thunder.VoltBeetle",
        "dungeon.earth.BoulderGolem", "dungeon.earth.StoneTortoise", "dungeon.earth.ClayBrute",
        "dungeon.earth.PebbleGolem", "dungeon.earth.BurrowMole",
        "dungeon.voidset.VoidScout", "dungeon.voidset.VoidStalker", "dungeon.voidset.RiftWraith",
        "dungeon.voidset.VoidShard", "dungeon.voidset.WatcherMote",
        "dungeon.nature.SporeNursery", "dungeon.nature.VenusSnapper", "dungeon.nature.ThornStag",
        "dungeon.nature.MushroomSoldier", "dungeon.nature.SeedSquirrel",
        "dungeon.machine.ReconDrone", "dungeon.machine.CogTank", "dungeon.machine.SteamAutomaton",
        "dungeon.machine.ClockworkMouse", "dungeon.machine.MiniDrone",
        "dungeon.elite.BladeWyvern", "dungeon.elite.BlackwingAttendant", "dungeon.elite.FoxfireMaiden", "dungeon.elite.TreantWarden",
    };

    /** バニラで「生き物じゃない」ため起動時に読み込まれず、後から遅れて読まれがちなもの */
    private static final Class<?>[] VANILLA = {
        net.minecraft.world.entity.decoration.Painting.class,
        net.minecraft.world.entity.decoration.ItemFrame.class,
        net.minecraft.world.entity.decoration.GlowItemFrame.class,
        net.minecraft.world.entity.item.ItemEntity.class,
        net.minecraft.world.entity.item.FallingBlockEntity.class,
        net.minecraft.world.entity.item.PrimedTnt.class,
        net.minecraft.world.entity.AreaEffectCloud.class,
        net.minecraft.world.entity.Interaction.class,
        net.minecraft.world.entity.Display.BlockDisplay.class,
        net.minecraft.world.entity.Display.ItemDisplay.class,
        net.minecraft.world.entity.Display.TextDisplay.class,
        net.minecraft.world.entity.vehicle.Boat.class,
        net.minecraft.world.entity.vehicle.ChestBoat.class,
        net.minecraft.world.entity.vehicle.MinecartFurnace.class,
        net.minecraft.world.entity.vehicle.MinecartCommandBlock.class,
        net.minecraft.world.entity.boss.enderdragon.EndCrystal.class,
        net.minecraft.world.entity.projectile.Arrow.class,
        net.minecraft.world.entity.projectile.ThrownTrident.class,
        net.minecraft.world.entity.projectile.FireworkRocketEntity.class,
        net.minecraft.world.entity.projectile.FishingHook.class,
        net.minecraft.world.entity.projectile.WitherSkull.class,
        net.minecraft.world.entity.projectile.ThrownPotion.class,
        net.minecraft.world.entity.projectile.LargeFireball.class,
        net.minecraft.world.entity.projectile.SmallFireball.class,
        net.minecraft.world.entity.projectile.DragonFireball.class,
        net.minecraft.world.entity.projectile.ShulkerBullet.class,
        net.minecraft.world.entity.projectile.windcharge.WindCharge.class,
        net.minecraft.world.entity.projectile.windcharge.BreezeWindCharge.class,
        net.minecraft.world.entity.OminousItemSpawner.class,
    };

    public static void onCommonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(EarlyClassInit::run);
    }

    private static void run() {
        ClassLoader cl = EarlyClassInit.class.getClassLoader();
        for (Class<?> c : VANILLA) init(c.getName(), cl);
        for (String n : OURS) init("com.chigusa.jsnboss." + n, cl);
    }

    private static void init(String name, ClassLoader cl) {
        try {
            Class.forName(name, true, cl);
        } catch (Throwable t) {
            com.mojang.logging.LogUtils.getLogger().warn("[jsnboss] early init failed: {}", name, t);
        }
    }
}
