package com.chigusa.jsnboss.vfx;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;

/**
 * 詠唱陣をサーバーから出す道具。柄の番号は particles/spell_circle.json の並び（jsn-Craft と同じ絵）。
 */
public final class Circles {
    public static final int SWORD = 0, FIRE = 1, ICE = 2, THUNDER = 3, STAR = 4, RUNE = 5, ALCHEMY = 6, HOLY = 7,
            NATURE = 8, GEM = 9, WATER = 10, EARTH = 11, WIND = 12, DARK = 13;

    /** 1枚出す */
    public static void circle(ServerLevel level, Vec3 p, int pattern, float size, int ticks, int style) {
        level.sendParticles(new CircleOptions(pattern, size, ticks, style), p.x, p.y, p.z, 1, 0, 0, 0, 0);
    }

    /** 地面のため（詠唱）の陣：大小2枚を逆回りに重ねて、ticks で育ちきる */
    public static void charge(ServerLevel level, Vec3 feet, int pattern, float size, int ticks) {
        circle(level, feet.add(0, 0.05, 0), pattern, size, ticks, CircleOptions.CHARGE);
        circle(level, feet.add(0, 0.08, 0), pattern, size * 0.55F, ticks, CircleOptions.CHARGE | CircleOptions.REVERSE);
    }

    /** 大詠唱：地面に3枚、空に1枚の大きな陣 */
    public static void grand(ServerLevel level, Vec3 feet, int pattern, float size, int ticks) {
        circle(level, feet.add(0, 0.05, 0), pattern, size, ticks, CircleOptions.CHARGE);
        circle(level, feet.add(0, 0.08, 0), pattern, size * 0.62F, ticks, CircleOptions.CHARGE | CircleOptions.REVERSE);
        circle(level, feet.add(0, 0.11, 0), pattern, size * 0.3F, ticks, CircleOptions.CHARGE);
        circle(level, feet.add(0, 12.0, 0), pattern, size * 0.8F, ticks, CircleOptions.CHARGE | CircleOptions.SKY | CircleOptions.REVERSE);
    }

    /** 発動の瞬間：パッと光って広がる */
    public static void flash(ServerLevel level, Vec3 p, int pattern, float size) {
        circle(level, p.add(0, 0.06, 0), pattern, size, 14, CircleOptions.FLASH);
    }

    private Circles() {}
}
