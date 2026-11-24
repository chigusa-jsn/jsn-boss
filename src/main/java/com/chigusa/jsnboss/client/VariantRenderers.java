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

    /** テーマ亜種のテクスチャ：variant/<名前>.png（鉱石は variant/<名前>_<段階>.png） */
    private static ResourceLocation themed(net.minecraft.world.entity.Mob e) {
        String id = net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(e.getType()).getPath();
        if (e instanceof com.chigusa.jsnboss.mob.Themed t && t.theme() == com.chigusa.jsnboss.mob.Theme.ORE) {
            id += "_" + com.chigusa.jsnboss.mob.OreTiers.NAMES[t.oreTier()];
        }
        return tex(id);
    }

    private static boolean glows(net.minecraft.world.entity.Entity e) {
        return e instanceof com.chigusa.jsnboss.mob.Themed t && t.theme() == com.chigusa.jsnboss.mob.Theme.GLOW;
    }

    @SuppressWarnings("unchecked")
    private static void registerThemed(EntityRenderersEvent.RegisterRenderers event) {
        for (var en : com.chigusa.jsnboss.registry.ModThemed.TYPES.entrySet()) {
            var type = en.getValue().get();
            switch (com.chigusa.jsnboss.registry.ModThemed.BASE_OF.get(en.getKey())) {
                case "zombie" -> event.registerEntityRenderer((net.minecraft.world.entity.EntityType<Zombie>) type, ctx -> new ZombieRenderer(ctx) {
                    @Override public ResourceLocation getTextureLocation(Zombie e) { return themed(e); }
                    @Override protected int getBlockLightLevel(Zombie e, net.minecraft.core.BlockPos p) { return glows(e) ? 15 : super.getBlockLightLevel(e, p); }
                });
                case "skeleton" -> event.registerEntityRenderer((net.minecraft.world.entity.EntityType<net.minecraft.world.entity.monster.Skeleton>) type, ctx -> new SkeletonRenderer(ctx) {
                    @Override public ResourceLocation getTextureLocation(AbstractSkeleton e) { return themed(e); }
                    @Override protected int getBlockLightLevel(AbstractSkeleton e, net.minecraft.core.BlockPos p) { return glows(e) ? 15 : super.getBlockLightLevel(e, p); }
                });
                case "spider" -> event.registerEntityRenderer((net.minecraft.world.entity.EntityType<Spider>) type, ctx -> new SpiderRenderer<Spider>(ctx) {
                    @Override public ResourceLocation getTextureLocation(Spider e) { return themed(e); }
                    @Override protected int getBlockLightLevel(Spider e, net.minecraft.core.BlockPos p) { return glows(e) ? 15 : super.getBlockLightLevel(e, p); }
                });
                case "slime" -> event.registerEntityRenderer((net.minecraft.world.entity.EntityType<net.minecraft.world.entity.monster.Slime>) type, ctx -> new net.minecraft.client.renderer.entity.SlimeRenderer(ctx) {
                    @Override public ResourceLocation getTextureLocation(net.minecraft.world.entity.monster.Slime e) { return themed(e); }
                    @Override protected int getBlockLightLevel(net.minecraft.world.entity.monster.Slime e, net.minecraft.core.BlockPos p) { return glows(e) ? 15 : super.getBlockLightLevel(e, p); }
                });
                case "blaze" -> event.registerEntityRenderer((net.minecraft.world.entity.EntityType<Blaze>) type, ctx -> new BlazeRenderer(ctx) {
                    @Override public ResourceLocation getTextureLocation(Blaze e) { return themed(e); }
                });
                default -> event.registerEntityRenderer((net.minecraft.world.entity.EntityType<Creeper>) type, ctx -> new CreeperRenderer(ctx) {
                    @Override public ResourceLocation getTextureLocation(Creeper e) { return themed(e); }
                    @Override protected int getBlockLightLevel(Creeper e, net.minecraft.core.BlockPos p) { return glows(e) ? 15 : super.getBlockLightLevel(e, p); }
                });
            }
        }
    }

    public static void register(EntityRenderersEvent.RegisterRenderers event) {
        registerThemed(event);
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
