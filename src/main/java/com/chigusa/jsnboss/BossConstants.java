package com.chigusa.jsnboss;

import net.minecraft.resources.ResourceLocation;

/** MOD ID はここだけ自分のものに変えれば OK（assets/data フォルダ名も合わせて変更） */
public final class BossConstants {
    public static final String MODID = "jsnboss";

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MODID, path);
    }

    private BossConstants() {}
}
