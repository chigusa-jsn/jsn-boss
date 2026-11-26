package com.chigusa.jsnboss.client;

import com.chigusa.jsnboss.BossConstants;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.LerpingBossEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.CustomizeGuiOverlayEvent;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/**
 * ボス専用のボスバー（キングダムハーツ方式）。
 * 形態ごとにバーが 1 本。なくなると次の形態で満タンまで溜まり直す。
 * 残りの本数は小さな玉で、受けたダメージは少し遅れて減る帯で見せる。
 *
 * サーバーからは「今のバーの残り」と「控えの本数（overlay に入れてある）」が届く。
 */
@EventBusSubscriber(modid = BossConstants.MODID, value = Dist.CLIENT)
public final class BossBarRenderer {

    /** ボス → バーの本数 */
    private static final Map<String, Integer> STAGES = new HashMap<>();
    static {
        for (String b : new String[]{"fire_knight", "ice_witch", "dark_mage", "blade_dragon", "white_fox", "wind_cat",
                "thunder_lion", "rock_giant", "ender_sentinel", "creeper_mother", "fenrir", "iron_spider", "fallen_valkyrie", "yggdrasil_spirit"}) {
            STAGES.put(b, 2);
        }
        STAGES.put("zombie_king", 3);
        STAGES.put("yggdrasil", 3);
    }

    private static final int TEX_W = 512, TEX_H = 64;
    private static final int FRAME_W = 240, FRAME_H = 32;
    private static final int WIN_X = 36, WIN_Y = 13, WIN_W = 198, WIN_H = 7;
    private static final int TRAIL_COLOR = 0xFFFFF2B0;

    private static final class State {
        float shown = -1, trail = -1;
        int left = -1;
        long holdUntil, flashUntil, lastSeen, lastTime;
        boolean refilling;
    }

    private static final Map<UUID, State> STATES = new HashMap<>();

    private BossBarRenderer() {}

    @SubscribeEvent
    public static void onBossBar(CustomizeGuiOverlayEvent.BossEventProgress event) {
        LerpingBossEvent bar = event.getBossEvent();
        String boss = bossKey(bar.getName());
        if (boss == null) return;
        Integer mini = com.chigusa.jsnboss.registry.ModDungeon.MID_BAR_COLOR.get(boss);
        if (mini != null) {
            drawMini(event, bar, boss, mini);
            return;
        }
        Integer total = STAGES.get(boss);
        if (total == null) return;

        int left = barsLeft(bar.getOverlay());
        int stage = Mth.clamp(total - 1 - left, 0, total - 1);
        ResourceLocation tex = ResourceLocation.fromNamespaceAndPath(BossConstants.MODID,
                "textures/gui/bossbar/" + boss + "_" + (stage + 1) + ".png");

        long now = Util.getMillis();
        State st = update(bar, left, now);

        GuiGraphics g = event.getGuiGraphics();
        int x0 = g.guiWidth() / 2 - FRAME_W / 2;
        int y0 = event.getY() - 10;
        int wx = x0 + WIN_X, wy = y0 + WIN_Y;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        // 空の帯 → 遅れて減る帯 → 今のバー → 枠
        g.blit(tex, wx, wy, WIN_W, WIN_H, 484, 16, 16, 14, TEX_W, TEX_H);
        int fw = Math.round(WIN_W * st.shown);
        int tw = Math.round(WIN_W * st.trail);
        if (tw > fw) g.fill(wx + fw, wy, wx + tw, wy + WIN_H, TRAIL_COLOR);
        if (fw > 0) g.blit(tex, wx, wy, fw, WIN_H, 484, 0, 16, 14, TEX_W, TEX_H);
        if (now < st.flashUntil) {
            int a = (int) (200 * (st.flashUntil - now) / 400.0F);
            g.fill(wx, wy, wx + WIN_W, wy + WIN_H, (Mth.clamp(a, 0, 255) << 24) | 0xFFFFFF);
        }
        g.blit(tex, x0, y0, FRAME_W, FRAME_H, 0, 0, 480, 64, TEX_W, TEX_H);
        // 控えのバー
        for (int i = 0; i < left; i++) {
            g.blit(tex, x0 + 40 + i * 8, y0 + 25, 6, 6, 484, 32, 12, 12, TEX_W, TEX_H);
        }
        RenderSystem.disableBlend();

        Font font = Minecraft.getInstance().font;
        Component name = bar.getName();
        g.drawString(font, name, wx + WIN_W / 2 - font.width(name) / 2, y0, 0xFFFFFF);

        // 今のバーの残り HP（バーの右下に小さく）
        float barMax = barMax(boss, left, bar);
        if (barMax > 0) {
            String text = (int) Math.ceil(Mth.clamp(bar.getProgress(), 0, 1) * barMax) + " / " + Math.round(barMax);
            g.pose().pushPose();
            g.pose().translate(wx + WIN_W, y0 + 25.5F, 0);
            g.pose().scale(0.75F, 0.75F, 1.0F);
            g.drawString(font, text, -font.width(text), 0, 0xFFF4F0E0);
            g.pose().popPose();
        }

        event.setIncrement(FRAME_H + 4);
        event.setCanceled(true);

        if (STATES.size() > 8) prune(now);
    }

    /** ダンジョンの中ボス用：細い1本のバー（名前は小さめ、HP は右下） */
    private static void drawMini(CustomizeGuiOverlayEvent.BossEventProgress event, LerpingBossEvent bar, String boss, int color) {
        long now = Util.getMillis();
        State st = update(bar, 0, now);
        GuiGraphics g = event.getGuiGraphics();
        Font font = Minecraft.getInstance().font;
        int w = 150, h = 4;
        int cx = g.guiWidth() / 2;
        int x0 = cx - w / 2, y0 = event.getY();

        Component name = bar.getName();
        g.pose().pushPose();
        g.pose().translate(cx, y0, 0);
        g.pose().scale(0.8F, 0.8F, 1.0F);
        g.drawString(font, name, -font.width(name) / 2, 0, 0xFFF4E8DC);
        g.pose().popPose();

        int by = y0 + 9;
        g.fill(x0 - 1, by - 1, x0 + w + 1, by + h + 1, 0xE0100C0C);
        g.fill(x0, by, x0 + w, by + h, 0xFF3A2A26);
        int fw = Math.round(w * st.shown), tw = Math.round(w * st.trail);
        if (tw > fw) g.fill(x0 + fw, by, x0 + tw, by + h, TRAIL_COLOR);
        if (fw > 0) {
            g.fill(x0, by, x0 + fw, by + h, color);
            g.fill(x0, by, x0 + fw, by + 1, lighten(color));
        }
        float half = barMax(boss, 0, bar);
        if (half > 0) {
            float max = half * 2.0F;
            String text = (int) Math.ceil(Mth.clamp(bar.getProgress(), 0, 1) * max) + " / " + Math.round(max);
            g.pose().pushPose();
            g.pose().translate(x0 + w, by + h + 2, 0);
            g.pose().scale(0.7F, 0.7F, 1.0F);
            g.drawString(font, text, -font.width(text), 0, 0xFFF4F0E0);
            g.pose().popPose();
        }
        event.setIncrement(24);
        event.setCanceled(true);
    }

    private static int lighten(int argb) {
        int r = (argb >> 16) & 255, gg = (argb >> 8) & 255, b = argb & 255;
        r += (255 - r) * 2 / 5; gg += (255 - gg) * 2 / 5; b += (255 - b) * 2 / 5;
        return 0xFF000000 | (r << 16) | (gg << 8) | b;
    }

    private static State update(LerpingBossEvent bar, int left, long now) {
        State st = STATES.computeIfAbsent(bar.getId(), id -> new State());
        float target = Mth.clamp(bar.getProgress(), 0.0F, 1.0F);
        float dt = st.lastTime == 0 ? 0.0F : Math.min(0.1F, (now - st.lastTime) / 1000.0F);
        st.lastTime = now;
        st.lastSeen = now;

        if (st.left == -1 || st.shown < 0) {                 // 初めて見たとき
            st.left = left; st.shown = target; st.trail = target;
        } else if (left < st.left) {                         // 次のバーへ：空から溜まり直す
            st.left = left; st.shown = 0.0F; st.trail = 0.0F; st.refilling = true;
            st.flashUntil = now + 400;
        } else if (left > st.left) {
            st.left = left; st.shown = target; st.trail = target;
        }

        if (target >= st.shown) {                            // 回復・溜まり直し
            float speed = st.refilling ? 0.8F : 1.5F;
            st.shown = Math.min(target, st.shown + speed * dt);
            if (st.shown >= target) st.refilling = false;
        } else {                                             // ダメージ
            st.shown = target;
            st.holdUntil = now + 700;
        }

        if (st.trail > st.shown) {
            if (now >= st.holdUntil) st.trail = Math.max(st.shown, st.trail - 0.6F * dt);
        } else {
            st.trail = st.shown;
        }
        return st;
    }

    private static void prune(long now) {
        Iterator<State> it = STATES.values().iterator();
        while (it.hasNext()) {
            if (now - it.next().lastSeen > 10_000) it.remove();
        }
    }

    /** 今のバー1本ぶんの HP。近くにいるそのボスの最大 HP から出す（ゾンビの王の復活後は 500、神樹は最大 HP） */
    private static final Map<UUID, float[]> MAX_CACHE = new HashMap<>();

    private static float barMax(String boss, int left, LerpingBossEvent bar) {
        if (boss.equals("zombie_king") && left == 0) return 500.0F;
        long now = Util.getMillis();
        float[] c = MAX_CACHE.get(bar.getId());
        if (c == null || now - (long) c[1] > 1000 || c[2] != left) {
            float max = -1;
            var mc = Minecraft.getInstance();
            if (mc.level != null && mc.player != null) {
                double best = Double.MAX_VALUE;
                for (var e : mc.level.getEntitiesOfClass(com.chigusa.jsnboss.entity.AbstractBoss.class,
                        mc.player.getBoundingBox().inflate(160))) {
                    String key = net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(e.getType()).getPath();
                    if (!key.equals(boss)) continue;
                    double d = e.distanceToSqr(mc.player);
                    if (d < best) {
                        best = d;
                        max = e.getMaxHealth();
                    }
                }
            }
            if (max > 0 && !(boss.equals("yggdrasil") && left == 0)) max *= 0.5F;
            c = new float[]{max, now, left};
            MAX_CACHE.put(bar.getId(), c);
            if (MAX_CACHE.size() > 16) MAX_CACHE.clear();
        }
        return c[0];
    }

    private static int barsLeft(BossEvent.BossBarOverlay overlay) {
        return switch (overlay) {
            case PROGRESS -> 0;
            case NOTCHED_6 -> 1;
            case NOTCHED_10 -> 2;
            case NOTCHED_12 -> 3;
            case NOTCHED_20 -> 4;
        };
    }

    /** 名前の翻訳キー entity.jsnboss.xxx からボスを見分ける */
    private static String bossKey(Component name) {
        if (name.getContents() instanceof TranslatableContents tc) {
            String key = tc.getKey();
            String prefix = "entity." + BossConstants.MODID + ".";
            if (key.startsWith(prefix)) {
                String b = key.substring(prefix.length());
                if (b.endsWith("_clone")) b = b.substring(0, b.length() - 6);
                return b;
            }
        }
        return null;
    }
}
