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
        for (String b : new String[]{"fire_knight", "ender_sentinel"}) {
            STAGES.put(b, 2);
        }
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

        event.setIncrement(FRAME_H + 4);
        event.setCanceled(true);

        if (STATES.size() > 8) prune(now);
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
