package com.chigusa.jsnboss.emblem;

import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

import java.util.List;

/**
 * 紋章（Curios の「紋章」枠に1つだけつけられる）。
 * 能力値で表せる効果はここで、攻撃・被弾・撃破のときの効果は EmblemEvents で。
 */
public class EmblemItem extends Item implements ICurioItem {
    private final String key;
    private final int rank;

    public EmblemItem(String key, int rank) {
        super(new Item.Properties().stacksTo(1).fireResistant()
                .rarity(rank == 3 ? Rarity.EPIC : rank == 2 ? Rarity.RARE : Rarity.UNCOMMON));
        this.key = key;
        this.rank = rank;
    }

    public String key() { return key; }
    public int rank() { return rank; }

    /** ランクごとの数値（I / II / III） */
    public double pick(double a, double b, double c) {
        return rank == 3 ? c : rank == 2 ? b : a;
    }

    @Override
    public Multimap<Holder<Attribute>, AttributeModifier> getAttributeModifiers(SlotContext ctx, ResourceLocation id, ItemStack stack) {
        Multimap<Holder<Attribute>, AttributeModifier> map = LinkedHashMultimap.create();
        switch (key) {
            case "corpse_king" -> map.put(Attributes.MAX_HEALTH, mod(id, 0, rank * 2.0, AttributeModifier.Operation.ADD_VALUE));
            case "shadow_blade" -> map.put(Attributes.ATTACK_DAMAGE, mod(id, 0, rank, AttributeModifier.Operation.ADD_VALUE));
            case "foxfire" -> map.put(Attributes.LUCK, mod(id, 0, rank, AttributeModifier.Operation.ADD_VALUE));
            case "gale" -> map.put(Attributes.MOVEMENT_SPEED, mod(id, 0, rank * 0.05, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
            case "boulder" -> {
                map.put(Attributes.ARMOR, mod(id, 0, rank * 2.0, AttributeModifier.Operation.ADD_VALUE));
                map.put(Attributes.KNOCKBACK_RESISTANCE, mod(id, 1, rank * 0.1, AttributeModifier.Operation.ADD_VALUE));
            }
            default -> {
            }
        }
        return map;
    }

    private static AttributeModifier mod(ResourceLocation id, int n, double v, AttributeModifier.Operation op) {
        return new AttributeModifier(ResourceLocation.fromNamespaceAndPath(id.getNamespace(), id.getPath() + "/emblem_" + n), v, op);
    }

    @Override
    public void curioTick(SlotContext ctx, ItemStack stack) {
        LivingEntity e = ctx.entity();
        if (e.level().isClientSide) return;
        switch (key) {
            case "frostbloom" -> e.setTicksFrozen(0);
            case "star_tree" -> {
                int every = (int) pick(160, 120, 80);
                if (e.tickCount % every == 0 && e.getHealth() < e.getMaxHealth()) e.heal(2.0F);
            }
            case "foxfire" -> {
                // 体力が半分以下のあいだ、ゆっくり回復する
                if (e.tickCount % 50 == 0 && e.getHealth() <= e.getMaxHealth() * 0.5F) e.heal(1.0F);
            }
            default -> {
            }
        }
    }

    @Override
    public boolean canEquipFromUse(SlotContext ctx, ItemStack stack) {
        return true;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> tip, TooltipFlag flag) {
        tip.add(Component.translatable("emblem.jsnboss.rank_" + rank).withStyle(ChatFormatting.GOLD));
        tip.add(Component.translatable("emblem.jsnboss." + key + ".effect_" + rank).withStyle(ChatFormatting.AQUA));
        if (rank < 3) {
            tip.add(Component.translatable("emblem.jsnboss.next", Component.translatable("emblem.jsnboss." + key + ".effect_" + (rank + 1)))
                    .withStyle(ChatFormatting.DARK_GRAY));
        }
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return rank == 3;
    }
}
