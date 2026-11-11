package com.chigusa.jsnboss.client;

import com.chigusa.jsnboss.BossConstants;
import com.chigusa.jsnboss.entity.AbstractBoss;
import com.chigusa.jsnboss.registry.ModEntities;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.NoopRenderer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;

/** クライアント側：描画の登録と、画面の揺れ */
public final class BossClientEvents {

    @EventBusSubscriber(modid = BossConstants.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static final class ModBus {
        @SubscribeEvent
        public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
            event.registerEntityRenderer(ModEntities.FIRE_KNIGHT.get(), FireKnightRenderer::new);
            // 火の玉・斬撃・火柱はパーティクルで見せるので、モデルは描かない
            event.registerEntityRenderer(ModEntities.KNIGHT_FIREBALL.get(), NoopRenderer::new);
            event.registerEntityRenderer(ModEntities.FLAME_SLASH.get(), NoopRenderer::new);
            event.registerEntityRenderer(ModEntities.FIRE_PILLAR.get(), NoopRenderer::new);
        }
    }

    @EventBusSubscriber(modid = BossConstants.MODID, value = Dist.CLIENT)
    public static final class GameBus {
        private static final double SHAKE_RANGE = 24.0;

        /** 近くのボスが地面を叩いたら、距離に応じてカメラを揺らす */
        @SubscribeEvent
        public static void onCamera(ViewportEvent.ComputeCameraAngles event) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null || mc.level == null) return;
            float strength = 0.0F;
            for (AbstractBoss knight : mc.level.getEntitiesOfClass(AbstractBoss.class,
                    mc.player.getBoundingBox().inflate(SHAKE_RANGE))) {
                int shake = knight.getShake();
                if (shake <= 0) continue;
                float falloff = 1.0F - (float) (knight.distanceTo(mc.player) / SHAKE_RANGE);
                strength = Math.max(strength, Math.min(1.0F, shake / 10.0F) * Math.max(0.0F, falloff));
            }
            if (strength <= 0.0F) return;
            double time = mc.player.tickCount + event.getPartialTick();
            event.setPitch((float) (event.getPitch() + Math.sin(time * 2.7) * strength * 2.5));
            event.setYaw((float) (event.getYaw() + Math.cos(time * 3.1) * strength * 2.0));
            event.setRoll((float) (event.getRoll() + Math.sin(time * 1.9) * strength * 1.5));
        }
    }

    private BossClientEvents() {}
}
