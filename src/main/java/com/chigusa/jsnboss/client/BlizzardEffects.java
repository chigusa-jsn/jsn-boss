package com.chigusa.jsnboss.client;

import com.chigusa.jsnboss.BossConstants;
import com.chigusa.jsnboss.entity.IceWitchEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;

/**
 * 氷の魔女・第2形態の「吹雪」。近くにいると白い霧で遠くが見えにくくなり、横なぐりの雪が降る。
 * 魔女から離れるほど弱くなる。
 */
@EventBusSubscriber(modid = BossConstants.MODID, value = Dist.CLIENT)
public final class BlizzardEffects {
    private static final double FULL = 28.0, FADE = 40.0;
    private static final float FOG_DISTANCE = 22.0F;
    private static float intensity, previous;

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        previous = intensity;
        if (mc.player == null || mc.level == null || mc.isPaused()) {
            intensity = 0;
            return;
        }
        float want = 0;
        for (IceWitchEntity w : mc.level.getEntitiesOfClass(IceWitchEntity.class, mc.player.getBoundingBox().inflate(FADE),
                w -> w.isPhase2() && !w.isClone() && w.isAlive())) {
            double d = w.distanceTo(mc.player);
            want = Math.max(want, (float) Mth.clamp((FADE - d) / (FADE - FULL), 0, 1));
        }
        intensity = Mth.approach(intensity, want, 0.02F);      // ゆっくり強まる・弱まる
        if (intensity <= 0) return;

        RandomSource r = mc.level.random;
        int n = (int) (14 * intensity);
        for (int i = 0; i < n; i++) {
            double x = mc.player.getX() + (r.nextDouble() - 0.5) * 24;
            double y = mc.player.getY() + r.nextDouble() * 8 - 1;
            double z = mc.player.getZ() + (r.nextDouble() - 0.5) * 24;
            mc.level.addParticle(i % 4 == 0 ? ParticleTypes.WHITE_ASH : ParticleTypes.SNOWFLAKE, x, y, z, -0.35, -0.12, 0.1);
        }
    }

    private static float current(double partialTick) {
        return Mth.lerp((float) partialTick, previous, intensity);
    }

    @SubscribeEvent
    public static void onFog(ViewportEvent.RenderFog event) {
        float k = current(event.getPartialTick());
        if (k <= 0) return;
        float far = Mth.lerp(k, event.getFarPlaneDistance(), Math.min(event.getFarPlaneDistance(), FOG_DISTANCE));
        event.setFarPlaneDistance(far);
        event.setNearPlaneDistance(Mth.lerp(k, event.getNearPlaneDistance(), 0.0F));
        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onFogColor(ViewportEvent.ComputeFogColor event) {
        float k = current(event.getPartialTick()) * 0.8F;
        if (k <= 0) return;
        event.setRed(Mth.lerp(k, event.getRed(), 0.82F));
        event.setGreen(Mth.lerp(k, event.getGreen(), 0.86F));
        event.setBlue(Mth.lerp(k, event.getBlue(), 0.95F));
    }

    private BlizzardEffects() {}
}
