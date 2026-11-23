package com.chigusa.jsnboss.client;

import com.chigusa.jsnboss.entity.RockPillarEntity;
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

/** 岩の柱：石のブロックを細くなるように積み、地面から突き出して沈む */
public class RockPillarRenderer extends EntityRenderer<RockPillarEntity> {
    private static final BlockState[] LAYERS = {
            Blocks.STONE.defaultBlockState(), Blocks.COBBLESTONE.defaultBlockState(), Blocks.MOSSY_COBBLESTONE.defaultBlockState()};
    private static final BlockState[] ROOTS = {
            Blocks.ROOTED_DIRT.defaultBlockState(), Blocks.DARK_OAK_LOG.defaultBlockState(), Blocks.MANGROVE_ROOTS.defaultBlockState()};
    private static final float[] WIDTH = {1.4F, 1.15F, 0.85F};
    private final BlockRenderDispatcher blocks;

    public RockPillarRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.blocks = context.getBlockRenderDispatcher();
    }

    @Override
    public void render(RockPillarEntity entity, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffers, int light) {
        float out = entity.progress(partialTick);
        if (out <= 0) return;
        pose.pushPose();
        pose.translate(0, -RockPillarEntity.HEIGHT * (1 - out), 0);
        pose.mulPose(Axis.YP.rotationDegrees(entity.getYRot()));
        for (int i = 0; i < LAYERS.length; i++) {
            float w = WIDTH[i];
            pose.pushPose();
            pose.translate(0, i, 0);
            pose.mulPose(Axis.YP.rotationDegrees(i * 17.0F));
            pose.scale(w, 1.0F, w);
            pose.translate(-0.5, 0, -0.5);
            blocks.renderSingleBlock((entity.variant() == RockPillarEntity.ROOT ? ROOTS : LAYERS)[i], pose, buffers, light, OverlayTexture.NO_OVERLAY);
            pose.popPose();
        }
        pose.popPose();
        super.render(entity, yaw, partialTick, pose, buffers, light);
    }

    @Override
    public ResourceLocation getTextureLocation(RockPillarEntity entity) {
        return TextureAtlas.LOCATION_BLOCKS;
    }
}
