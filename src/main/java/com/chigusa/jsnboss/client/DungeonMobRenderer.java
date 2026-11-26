package com.chigusa.jsnboss.client;

import com.chigusa.jsnboss.BossConstants;
import com.chigusa.jsnboss.dungeon.DungeonMob;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;

/** ダンジョンモブ共通の見た目：geo / animations / textures の entity/&lt;id&gt; を読み、_glowmask を光らせる */
public class DungeonMobRenderer extends GeoEntityRenderer<DungeonMob> {
    public DungeonMobRenderer(EntityRendererProvider.Context context, String id, float shadow, float scale) {
        super(context, new DefaultedEntityGeoModel<>(BossConstants.id(id), false));
        addRenderLayer(new AutoGlowingGeoLayer<>(this));
        this.shadowRadius = shadow;
        if (scale != 1.0F) withScale(scale);
        this.translucent = com.chigusa.jsnboss.registry.ModDungeon.TRANSLUCENT.contains(id);
    }

    private final boolean translucent;

    @Override
    public net.minecraft.client.renderer.RenderType getRenderType(DungeonMob animatable, net.minecraft.resources.ResourceLocation texture,
                                                                   net.minecraft.client.renderer.MultiBufferSource bufferSource, float partialTick) {
        return translucent ? net.minecraft.client.renderer.RenderType.entityTranslucent(texture)
                : super.getRenderType(animatable, texture, bufferSource, partialTick);
    }

    @Override
    protected float getDeathMaxRotation(DungeonMob entity) {
        return 0.0F;
    }
}
