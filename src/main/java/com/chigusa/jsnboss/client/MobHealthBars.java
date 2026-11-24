package com.chigusa.jsnboss.client;

import com.chigusa.jsnboss.BossConstants;
import com.chigusa.jsnboss.entity.AbstractBoss;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderNameTagEvent;
import org.joml.Matrix4f;

import java.util.HashMap;
import java.util.Map;

/**
 * 近くのモブの頭の上に、小さな体力バーと数字を出す（ボスはボスバーに数字が出るので出さない）。
 * 体力が減ると色が 緑 → 黄 → 赤 に変わり、受けたダメージは白い帯で少し遅れて減る。
 */
@EventBusSubscriber(modid = BossConstants.MODID, value = Dist.CLIENT)
public final class MobHealthBars {
    private static final double RANGE = 24.0;
    private static final float W = 40.0F, H = 4.0F;

    private static final class Trail {
        float value = -1;
        long holdUntil, last, seen;
    }

    private static final Map<Integer, Trail> TRAILS = new HashMap<>();

    private MobHealthBars() {}

    @SubscribeEvent
    public static void onNameTag(RenderNameTagEvent event) {
        Entity ent = event.getEntity();
        if (!(ent instanceof Mob mob) || ent instanceof AbstractBoss) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui || !mob.isAlive() || mob.isInvisibleTo(mc.player)) return;
        if (mob.distanceToSqr(mc.gameRenderer.getMainCamera().getPosition()) > RANGE * RANGE) return;
        float max = mob.getMaxHealth();
        if (max <= 0) return;
        float hp = Mth.clamp(mob.getHealth(), 0, max);
        float ratio = hp / max;
        float trail = trail(mob, ratio);

        PoseStack ps = event.getPoseStack();
        ps.pushPose();
        double up = mob.getBbHeight() + 0.45 + (mob.hasCustomName() && mob.isCustomNameVisible() ? 0.3 : 0.0);
        ps.translate(0, up, 0);
        ps.mulPose(mc.getEntityRenderDispatcher().cameraOrientation());
        ps.scale(0.025F, -0.025F, 0.025F);
        Matrix4f m = ps.last().pose();
        MultiBufferSource buf = event.getMultiBufferSource();
        int light = event.getPackedLight();

        VertexConsumer vc = buf.getBuffer(RenderType.textBackground());
        float x0 = -W / 2, x1 = W / 2;
        quad(vc, m, x0 - 1, -1, x1 + 1, H + 1, 0xC0101010, light, 0.02F);   // 枠
        quad(vc, m, x0, 0, x1, H, 0xFF3A2A2A, light, 0.015F);               // 空
        if (trail > ratio) quad(vc, m, x0 + W * ratio, 0, x0 + W * trail, H, 0xFFF0EAD0, light, 0.01F);
        if (ratio > 0) {
            int col = ratio > 0.5F ? 0xFF4ADE5A : ratio > 0.25F ? 0xFFF0C83A : 0xFFE8483A;
            quad(vc, m, x0, 0, x0 + W * ratio, H, col, light, 0.005F);
            quad(vc, m, x0, 0, x0 + W * ratio, 1, lighten(col), light, 0.004F);   // つや
        }

        // 数字（小さめ）
        Font font = mc.font;
        String text = num(hp) + " / " + num(max);
        ps.pushPose();
        ps.translate(0, -7.5, 0);
        ps.scale(0.75F, 0.75F, 0.75F);
        Matrix4f tm = ps.last().pose();
        font.drawInBatch(text, -font.width(text) / 2.0F, 0, 0xFFFFFFFF, false, tm, buf, Font.DisplayMode.NORMAL, 0x60000000, light);
        ps.popPose();
        ps.popPose();

        if (TRAILS.size() > 256) prune();
    }

    private static String num(float v) {
        return v >= 100 || v == Math.floor(v) ? String.valueOf((int) Math.ceil(v)) : String.format("%.1f", v);
    }

    private static float trail(LivingEntity e, float ratio) {
        long now = Util.getMillis();
        Trail t = TRAILS.computeIfAbsent(e.getId(), k -> new Trail());
        float dt = t.last == 0 ? 0 : Math.min(0.1F, (now - t.last) / 1000.0F);
        t.last = now;
        t.seen = now;
        if (t.value < 0 || ratio >= t.value) {
            t.value = ratio;
        } else if (now >= t.holdUntil) {
            t.value = Math.max(ratio, t.value - 0.8F * dt);
        }
        if (e.hurtTime > 8) t.holdUntil = now + 500;
        return t.value;
    }

    private static void prune() {
        long now = Util.getMillis();
        TRAILS.values().removeIf(t -> now - t.seen > 5000);
    }

    private static int lighten(int argb) {
        int r = Math.min(255, ((argb >> 16) & 255) + 70), g = Math.min(255, ((argb >> 8) & 255) + 70), b = Math.min(255, (argb & 255) + 70);
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }

    private static void quad(VertexConsumer vc, Matrix4f m, float x0, float y0, float x1, float y1, int argb, int light, float z) {
        vc.addVertex(m, x0, y0, 0).setColor(argb).setLight(light);
        vc.addVertex(m, x0, y1, 0).setColor(argb).setLight(light);
        vc.addVertex(m, x1, y1, 0).setColor(argb).setLight(light);
        vc.addVertex(m, x1, y0, 0).setColor(argb).setLight(light);
    }
}
