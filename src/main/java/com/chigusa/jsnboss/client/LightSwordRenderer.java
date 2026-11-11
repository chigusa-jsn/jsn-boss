package com.chigusa.jsnboss.client;

import com.chigusa.jsnboss.entity.LightSwordEntity;
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

/** 光の剣：金の剣を最大の明るさで描き、切っ先を下にして空から落とす */
public class LightSwordRenderer extends EntityRenderer<LightSwordEntity> {
    private static final ItemStack SWORD = new ItemStack(Items.GOLDEN_SWORD);
    private static final float SIZE = 2.6F;
    private final ItemRenderer items;

    public LightSwordRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.items = context.getItemRenderer();
    }

    @Override
    public void render(LightSwordEntity entity, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffers, int light) {
        float age = entity.tickCount + partialTick;
        float fallStart = entity.getWarmup() - LightSwordEntity.FALL;
        if (age < fallStart) return;                                     // 予告中は見えない
        float fall = Mth.clamp((age - fallStart) / LightSwordEntity.FALL, 0, 1);
        float height = (float) (LightSwordEntity.DROP * (1 - fall * fall));   // だんだん速く落ちる
        float fade = Mth.clamp((entity.getWarmup() + LightSwordEntity.STUCK - age) / 4.0F, 0, 1);
        pose.pushPose();
        pose.translate(0, height + SIZE * 0.35F, 0);
        pose.mulPose(Axis.YP.rotationDegrees(entity.getYRot()));
        pose.mulPose(Axis.ZP.rotationDegrees(-135.0F));                 // 切っ先を真下へ
        pose.scale(SIZE * fade, SIZE * fade, SIZE * fade);
        items.renderStatic(SWORD, ItemDisplayContext.FIXED, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY,
                pose, buffers, entity.level(), entity.getId());
        pose.popPose();
        super.render(entity, yaw, partialTick, pose, buffers, light);
    }

    @Override
    public ResourceLocation getTextureLocation(LightSwordEntity entity) {
        return TextureAtlas.LOCATION_BLOCKS;
    }
}
