package com.chigusa.jsnboss.mob;

/** テーマ亜種のテーマ（友達の案） */
public enum Theme {
    MOSS("moss", 0x4A6A2E, 0x8AE05A),
    HONEY("honey", 0x4A3218, 0xFFB020),
    SEA("sea", 0x4AA08E, 0xA8FFF0),
    SKY("sky", 0xE8F4FF, 0x8AB8E8),
    GOLD("gold", 0xE8B830, 0xFFF4A0),
    SCULK("sculk", 0x1A2A30, 0x2AC8C8),
    ORE("ore", 0x7A7A7A, 0x5AF0E8),
    GLOW("glow", 0xA8E83A, 0xF4FFB0);

    public final String id;
    public final int eggBg, eggFg;

    Theme(String id, int eggBg, int eggFg) {
        this.id = id;
        this.eggBg = eggBg;
        this.eggFg = eggFg;
    }
}
