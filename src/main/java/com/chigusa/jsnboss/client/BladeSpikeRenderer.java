package com.chigusa.jsnboss.client;

import com.chigusa.jsnboss.entity.BladeSpikeEntity;
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

/** 刃の道：地面から大きな剣が切っ先を上にして突き出し、しばらくして沈む */
public class BladeSpikeRenderer extends EntityRenderer<BladeSpikeEntity> {
    private static final float SIZE = 2.8F;
    private final ItemRenderer items;

    public BladeSpikeRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.items = context.getItemRenderer();
    }

    @Override
    public void render(BladeSpikeEntity entity, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffers, int light) {
        float age = entity.tickCount + partialTick - entity.getWarmup();
        if (age < 0) return;                                        // 予告中は見えない
        float rise = Mth.clamp(age / 3.0F, 0, 1);                   // 3 tick で突き出す
        float sink = Mth.clamp((age - (BladeSpikeEntity.OUT - 4)) / 4.0F, 0, 1);   // 最後の 4 tick で沈む
        float out = rise * (1 - sink);
        pose.pushPose();
        pose.translate(0, -SIZE * 0.55F + SIZE * 0.75F * out, 0);
        pose.mulPose(Axis.YP.rotationDegrees(entity.getYRot()));
        pose.mulPose(Axis.ZP.rotationDegrees(45.0F));               // 右上向きの剣を、切っ先が真上を向くように
        pose.scale(SIZE, SIZE, SIZE);
        items.renderStatic(entity.getSwordStack(), ItemDisplayContext.FIXED, light, OverlayTexture.NO_OVERLAY,
                pose, buffers, entity.level(), entity.getId());
        pose.popPose();
        super.render(entity, yaw, partialTick, pose, buffers, light);
    }

    @Override
    public ResourceLocation getTextureLocation(BladeSpikeEntity entity) {
        return TextureAtlas.LOCATION_BLOCKS;
    }
}
