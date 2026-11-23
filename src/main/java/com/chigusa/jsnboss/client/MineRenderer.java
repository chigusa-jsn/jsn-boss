package com.chigusa.jsnboss.client;

import com.chigusa.jsnboss.entity.MineEntity;
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

/** 地雷：小さな TNT。爆発が近づくほど速く白く点滅する */
public class MineRenderer extends EntityRenderer<MineEntity> {
    private final BlockRenderDispatcher blocks;

    public MineRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.blocks = context.getBlockRenderDispatcher();
        this.shadowRadius = 0.3F;
    }

    @Override
    public void render(MineEntity entity, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffers, int light) {
        int rate = entity.tickCount > MineEntity.FUSE - 20 ? 2 : 6;
        boolean flash = (entity.tickCount / rate) % 2 == 0;
        pose.pushPose();
        pose.mulPose(Axis.YP.rotationDegrees(entity.getId() * 47 % 360));
        pose.scale(0.5F, 0.5F, 0.5F);
        pose.translate(-0.5, 0, -0.5);
        blocks.renderSingleBlock(Blocks.TNT.defaultBlockState(), pose, buffers, light,
                flash ? OverlayTexture.pack(OverlayTexture.u(1.0F), 10) : OverlayTexture.NO_OVERLAY);
        pose.popPose();
        super.render(entity, yaw, partialTick, pose, buffers, light);
    }

    @Override
    public ResourceLocation getTextureLocation(MineEntity entity) {
        return TextureAtlas.LOCATION_BLOCKS;
    }
}
