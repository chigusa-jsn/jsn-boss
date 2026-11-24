package com.chigusa.jsnboss.mob;

import com.chigusa.jsnboss.BossConstants;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

/** 鉱石テーマの段階：石（y30〜）・鉄（y0〜）・金（y-32〜）・ダイヤ（それより下）。深いほど防御力が高い */
public final class OreTiers {
    public static final String[] NAMES = {"stone", "iron", "gold", "diamond"};
    private static final double[] ARMOR = {4, 8, 10, 14};
    private static final double[] TOUGH = {0, 1, 2, 4};

    public static int forY(int y) {
        return y >= 30 ? 0 : y >= 0 ? 1 : y >= -32 ? 2 : 3;
    }

    public static void apply(Mob mob, int t) {
        t = Mth.clamp(t, 0, 3);
        var armor = mob.getAttribute(Attributes.ARMOR);
        var tough = mob.getAttribute(Attributes.ARMOR_TOUGHNESS);
        if (armor != null) {
            armor.removeModifier(BossConstants.id("ore_armor"));
            armor.addPermanentModifier(new AttributeModifier(BossConstants.id("ore_armor"), ARMOR[t], AttributeModifier.Operation.ADD_VALUE));
        }
        if (tough != null) {
            tough.removeModifier(BossConstants.id("ore_tough"));
            tough.addPermanentModifier(new AttributeModifier(BossConstants.id("ore_tough"), TOUGH[t], AttributeModifier.Operation.ADD_VALUE));
        }
    }

    private OreTiers() {}
}
