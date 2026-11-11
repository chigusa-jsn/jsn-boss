package com.chigusa.jsnboss.client;

import com.chigusa.jsnboss.entity.FlyingSwordEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;

/** 飛ぶ剣：剣のアイテムを、進む向きに切っ先を向けて大きめに描く */
public class FlyingSwordRenderer extends EntityRenderer<FlyingSwordEntity> {
    private final ItemRenderer items;

    public FlyingSwordRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.items = context.getItemRenderer();
    }

    @Override
    public void render(FlyingSwordEntity entity, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffers, int light) {
        pose.pushPose();
        pose.translate(0, 0.25, 0);
        // 矢と同じ向きの合わせ方：X 軸がまっすぐ進む向きになる
        pose.mulPose(Axis.YP.rotationDegrees(Mth.lerp(partialTick, entity.yRotO, entity.getYRot()) - 90.0F));
        pose.mulPose(Axis.ZP.rotationDegrees(Mth.lerp(partialTick, entity.xRotO, entity.getXRot())));
        // アイテムの剣は右上を向いているので、切っ先が +X を向くように回す
        pose.mulPose(Axis.ZP.rotationDegrees(-45.0F));
        pose.scale(1.6F, 1.6F, 1.6F);
        items.renderStatic(entity.getSwordStack(), ItemDisplayContext.FIXED, light, OverlayTexture.NO_OVERLAY,
                pose, buffers, entity.level(), entity.getId());
        pose.popPose();
        super.render(entity, yaw, partialTick, pose, buffers, light);
    }

    @Override
    public ResourceLocation getTextureLocation(FlyingSwordEntity entity) {
        return TextureAtlas.LOCATION_BLOCKS;
    }
}
