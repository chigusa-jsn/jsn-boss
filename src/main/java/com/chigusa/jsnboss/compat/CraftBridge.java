package com.chigusa.jsnboss.compat;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.fml.ModList;

import java.lang.reflect.Method;

/**
 * jsn-Craft とのつなぎ。jsn-Craft が入っていなくても動くように、リフレクションで呼ぶ。
 * 大詠唱に失敗したとき、プレイヤーのスタミナとマナを吸い取る（jsn-Craft がなければ満腹度と経験値を吸う）。
 */
public final class CraftBridge {
    private static Method setResources;
    private static boolean tried;

    /** スタミナとマナを 0 にする。jsn-Craft がなければ満腹度を減らし、経験値を 1 レベル吸う */
    public static void drain(ServerPlayer p) {
        if (!tried) {
            tried = true;
            if (ModList.get().isLoaded("jsn_craft")) {
                try {
                    setResources = Class.forName("com.chigusa.jsncraft.stat.ResourceService")
                            .getMethod("set", ServerPlayer.class, float.class, float.class);
                } catch (Throwable ignored) {
                    setResources = null;
                }
            }
        }
        if (setResources != null) {
            try {
                setResources.invoke(null, p, 0.0F, 0.0F);
                return;
            } catch (Throwable ignored) {
            }
        }
        p.getFoodData().setFoodLevel(Math.max(0, p.getFoodData().getFoodLevel() - 10));
        p.getFoodData().setSaturation(0.0F);
        if (p.experienceLevel > 0) p.giveExperienceLevels(-1);
    }

    private CraftBridge() {}
}
