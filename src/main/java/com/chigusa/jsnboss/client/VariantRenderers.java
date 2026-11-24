package com.chigusa.jsnboss.client;

import com.chigusa.jsnboss.BossConstants;
import com.chigusa.jsnboss.registry.ModEntities;
import com.chigusa.jsnboss.registry.ModMobs;
import net.minecraft.client.renderer.entity.BlazeRenderer;
import net.minecraft.client.renderer.entity.CreeperRenderer;
import net.minecraft.client.renderer.entity.HuskRenderer;
import net.minecraft.client.renderer.entity.NoopRenderer;
import net.minecraft.client.renderer.entity.SkeletonRenderer;
import net.minecraft.client.renderer.entity.SpiderRenderer;
import net.minecraft.client.renderer.entity.TippableArrowRenderer;
import net.minecraft.client.renderer.entity.ZombieRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.monster.AbstractSkeleton;
import net.minecraft.world.entity.monster.Blaze;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Spider;
import net.minecraft.world.entity.monster.Zombie;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/** 亜種はバニラのモデルそのままで、テクスチャだけ差し替える（textures/entity/variant/） */
public final class VariantRenderers {
    private VariantRenderers() {}

    private static ResourceLocation tex(String name) {
        return BossConstants.id("textures/entity/variant/" + name + ".png");
    }

    public static void register(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModMobs.SOUL_BLAZE.get(), ctx -> new BlazeRenderer(ctx) {
            @Override public ResourceLocation getTextureLocation(Blaze e) { return tex("soul_blaze"); }
        });
        event.registerEntityRenderer(ModMobs.FROST_BLAZE.get(), ctx -> new BlazeRenderer(ctx) {
            @Override public ResourceLocation getTextureLocation(Blaze e) { return tex("frost_blaze"); }
        });
        event.registerEntityRenderer(ModMobs.TOMB_ZOMBIE.get(), ctx -> new HuskRenderer(ctx) {
            @Override public ResourceLocation getTextureLocation(Zombie e) { return tex("tomb_zombie"); }
        });
        event.registerEntityRenderer(ModMobs.THUNDER_ZOMBIE.get(), ctx -> new ZombieRenderer(ctx) {
            @Override public ResourceLocation getTextureLocation(Zombie e) { return tex("thunder_zombie"); }
        });
        event.registerEntityRenderer(ModMobs.FIRE_SKELETON.get(), ctx -> new SkeletonRenderer(ctx) {
            @Override public ResourceLocation getTextureLocation(AbstractSkeleton e) { return tex("fire_skeleton"); }
        });
        event.registerEntityRenderer(ModMobs.POISON_SKELETON.get(), ctx -> new SkeletonRenderer(ctx) {
            @Override public ResourceLocation getTextureLocation(AbstractSkeleton e) { return tex("poison_skeleton"); }
        });
        event.registerEntityRenderer(ModMobs.FROST_CREEPER.get(), ctx -> new CreeperRenderer(ctx) {
            @Override public ResourceLocation getTextureLocation(Creeper e) { return tex("frost_creeper"); }
        });
        event.registerEntityRenderer(ModMobs.MAGMA_SPIDER.get(), ctx -> new SpiderRenderer<Spider>(ctx) {
            @Override public ResourceLocation getTextureLocation(Spider e) { return tex("magma_spider"); }
        });
        // ---- 友達の案の亜種
        event.registerEntityRenderer(ModMobs.MOSS_ZOMBIE.get(), ctx -> new ZombieRenderer(ctx) {
            @Override public ResourceLocation getTextureLocation(Zombie e) { return tex("moss_zombie"); }
        });
        event.registerEntityRenderer(ModMobs.GOLD_ZOMBIE.get(), ctx -> new ZombieRenderer(ctx) {
            @Override public ResourceLocation getTextureLocation(Zombie e) { return tex("gold_zombie"); }
        });
        event.registerEntityRenderer(ModMobs.ORE_ZOMBIE.get(), ctx -> new ZombieRenderer(ctx) {
            @Override public ResourceLocation getTextureLocation(Zombie e) {
                int t = e instanceof com.chigusa.jsnboss.mob.OreZombie oz ? oz.tier() : 0;
                return tex("ore_zombie_" + com.chigusa.jsnboss.mob.OreZombie.TIERS[t]);
            }
        });
        event.registerEntityRenderer(ModMobs.PRISMARINE_DROWNED.get(), ctx -> new net.minecraft.client.renderer.entity.DrownedRenderer(ctx) {
            @Override public ResourceLocation getTextureLocation(Zombie e) { return tex("prismarine_drowned"); }
        });
        event.registerEntityRenderer(ModMobs.HONEY_SPIDER.get(), ctx -> new SpiderRenderer<Spider>(ctx) {
            @Override public ResourceLocation getTextureLocation(Spider e) { return tex("honey_spider"); }
        });
        event.registerEntityRenderer(ModMobs.SKY_SKELETON.get(), ctx -> new SkeletonRenderer(ctx) {
            @Override public ResourceLocation getTextureLocation(AbstractSkeleton e) { return tex("sky_skeleton"); }
        });
        event.registerEntityRenderer(ModMobs.SCULK_SKELETON.get(), ctx -> new SkeletonRenderer(ctx) {
            @Override public ResourceLocation getTextureLocation(AbstractSkeleton e) { return tex("sculk_skeleton"); }
        });
        event.registerEntityRenderer(ModMobs.GLOW_SLIME.get(), ctx -> new net.minecraft.client.renderer.entity.SlimeRenderer(ctx) {
            @Override public ResourceLocation getTextureLocation(net.minecraft.world.entity.monster.Slime e) { return tex("glow_slime"); }
            @Override protected int getBlockLightLevel(net.minecraft.world.entity.monster.Slime e, net.minecraft.core.BlockPos pos) { return 15; }
        });
        event.registerEntityRenderer(ModMobs.POISON_CLOUD_ARROW.get(), TippableArrowRenderer::new);
        event.registerEntityRenderer(ModEntities.MOB_BOLT.get(), NoopRenderer::new);
    }
}
