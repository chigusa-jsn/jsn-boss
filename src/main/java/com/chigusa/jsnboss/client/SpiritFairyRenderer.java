package com.chigusa.jsnboss.client;

import com.chigusa.jsnboss.BossConstants;
import com.chigusa.jsnboss.entity.SpiritFairyEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;

/** 妖精：属性ごとに色の違うテクスチャ。羽は半透明 */
public class SpiritFairyRenderer extends GeoEntityRenderer<SpiritFairyEntity> {
    public SpiritFairyRenderer(EntityRendererProvider.Context context) {
        super(context, new DefaultedEntityGeoModel<SpiritFairyEntity>(BossConstants.id("spirit_fairy"), false) {
            @Override
            public ResourceLocation getTextureResource(SpiritFairyEntity e) {
                return BossConstants.id("textures/entity/spirit_fairy_" + e.element().id + ".png");
            }

            /** 頭の小物は、その妖精の属性のものだけ見せる */
            @Override
            public void setCustomAnimations(SpiritFairyEntity e, long instanceId,
                                            software.bernie.geckolib.animation.AnimationState<SpiritFairyEntity> state) {
                super.setCustomAnimations(e, instanceId, state);
                for (com.chigusa.jsnboss.vfx.Element el : com.chigusa.jsnboss.vfx.Element.values()) {
                    var bone = getAnimationProcessor().getBone("acc_" + el.id);
                    if (bone != null) {
                        boolean hide = el != e.element();
                        bone.setHidden(hide);
                        bone.setChildrenHidden(hide);
                    }
                }
            }
        });
        addRenderLayer(new AutoGlowingGeoLayer<>(this));
        withScale(1.2F);
        this.shadowRadius = 0.3F;
    }

    @Override
    public RenderType getRenderType(SpiritFairyEntity animatable, ResourceLocation texture, MultiBufferSource bufferSource, float partialTick) {
        return RenderType.entityTranslucent(texture);
    }
}
