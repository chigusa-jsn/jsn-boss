package com.chigusa.jsnboss.client;

import com.chigusa.jsnboss.entity.GoldenFruitEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** 黄金の実：くるくる回る、光る大きな金のリンゴ */
public class GoldenFruitRenderer extends EntityRenderer<GoldenFruitEntity> {
    private static final ItemStack APPLE = new ItemStack(Items.ENCHANTED_GOLDEN_APPLE);
    private final ItemRenderer items;

    public GoldenFruitRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.items = context.getItemRenderer();
    }

    @Override
    public void render(GoldenFruitEntity entity, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffers, int light) {
        float t = entity.tickCount + partialTick;
        pose.pushPose();
        pose.translate(0, 0.4 + Mth.sin(t * 0.1F) * 0.1F, 0);
        pose.mulPose(Axis.YP.rotationDegrees(t * 4.0F));
        pose.scale(1.2F, 1.2F, 1.2F);
        items.renderStatic(APPLE, ItemDisplayContext.GROUND, LightTexture.FULL_BRIGHT,
                entity.hurtTime > 0 ? OverlayTexture.pack(0, 3) : OverlayTexture.NO_OVERLAY, pose, buffers, entity.level(), entity.getId());
        pose.popPose();
        super.render(entity, yaw, partialTick, pose, buffers, light);
    }

    @Override
    public ResourceLocation getTextureLocation(GoldenFruitEntity entity) {
        return TextureAtlas.LOCATION_BLOCKS;
    }
}
