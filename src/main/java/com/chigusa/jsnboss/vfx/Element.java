package com.chigusa.jsnboss.vfx;

import net.minecraft.util.StringRepresentable;
import org.joml.Vector3f;

/** ユグドラシルの精霊が使う8つの属性 */
public enum Element implements StringRepresentable {
    FIRE("fire", "炎", Circles.FIRE, 0xFF5A28, true),
    ICE("ice", "氷", Circles.ICE, 0x8AE0FF, true),
    WIND("wind", "風", Circles.WIND, 0xA8FFC8, false),
    THUNDER("thunder", "雷", Circles.THUNDER, 0xFFE040, true),
    EARTH("earth", "土", Circles.EARTH, 0xC08A4A, true),
    DARK("dark", "闇", Circles.DARK, 0x9A40FF, true),
    LIGHT("light", "光", Circles.HOLY, 0xFFF6C0, false),
    WOOD("wood", "木", Circles.NATURE, 0x5AE070, true);

    public final String id, jp;
    public final int circle, rgb;
    /** 攻撃が当たった床が、この属性の床に変わるか */
    public final boolean leavesField;

    Element(String id, String jp, int circle, int rgb, boolean leavesField) {
        this.id = id;
        this.jp = jp;
        this.circle = circle;
        this.rgb = rgb;
        this.leavesField = leavesField;
    }

    public Vector3f color() {
        return new Vector3f(((rgb >> 16) & 255) / 255F, ((rgb >> 8) & 255) / 255F, (rgb & 255) / 255F);
    }

    public Element next() {
        return values()[(ordinal() + 1) % values().length];
    }

    @Override
    public String getSerializedName() {
        return id;
    }
}
