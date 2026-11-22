package com.chigusa.jsnboss.client;

import com.chigusa.jsnboss.entity.BoulderEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/** 大岩・落石・破片：回転しながら飛ぶ岩のブロック（大きさは BoulderEntity の size） */
public class BoulderRenderer extends EntityRenderer<BoulderEntity> {
    private static final BlockState ROCK = Blocks.MOSSY_COBBLESTONE.defaultBlockState();
    private static final BlockState CORE = Blocks.COBBLESTONE.defaultBlockState();
    private static final BlockState END_ROCK = Blocks.END_STONE.defaultBlockState();
    private static final BlockState END_CORE = Blocks.END_STONE_BRICKS.defaultBlockState();
    private final BlockRenderDispatcher blocks;

    public BoulderRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.blocks = context.getBlockRenderDispatcher();
        this.shadowRadius = 0.8F;
    }

    @Override
    public void render(BoulderEntity entity, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffers, int light) {
        float s = entity.size();
        boolean end = entity.variant() == BoulderEntity.END;
        float spin = (entity.tickCount + partialTick) * (entity.mode() == BoulderEntity.FALL ? 6.0F : 14.0F);
        pose.pushPose();
        pose.translate(0, s / 2, 0);
        pose.mulPose(Axis.XP.rotationDegrees(spin));
        pose.mulPose(Axis.ZP.rotationDegrees(spin * 0.6F + entity.getId() * 37));
        // 2つの立方体を少しずらして重ね、ゴツゴツした岩に見せる
        pose.pushPose();
        pose.scale(s, s, s);
        pose.translate(-0.5, -0.5, -0.5);
        blocks.renderSingleBlock(end ? END_ROCK : ROCK, pose, buffers, light, OverlayTexture.NO_OVERLAY);
        pose.popPose();
        pose.pushPose();
        pose.mulPose(Axis.YP.rotationDegrees(45));
        pose.mulPose(Axis.XP.rotationDegrees(30));
        pose.scale(s * 0.85F, s * 0.85F, s * 0.85F);
        pose.translate(-0.5, -0.5, -0.5);
        blocks.renderSingleBlock(end ? END_CORE : CORE, pose, buffers, light, OverlayTexture.NO_OVERLAY);
        pose.popPose();
        pose.popPose();
        super.render(entity, yaw, partialTick, pose, buffers, light);
    }

    @Override
    public ResourceLocation getTextureLocation(BoulderEntity entity) {
        return TextureAtlas.LOCATION_BLOCKS;
    }
}
