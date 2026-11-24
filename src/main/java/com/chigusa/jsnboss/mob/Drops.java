package com.chigusa.jsnboss.mob;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionContents;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;

/** 亜種・エリートのドロップを足す道具 */
final class Drops {
    private final LivingDropsEvent event;
    private final LivingEntity e;
    final RandomSource r;

    Drops(LivingDropsEvent event) {
        this.event = event;
        this.e = event.getEntity();
        this.r = e.getRandom();
    }

    void add(ItemStack stack) {
        if (stack.isEmpty()) return;
        event.getDrops().add(new ItemEntity(e.level(), e.getX(), e.getY() + 0.5, e.getZ(), stack));
    }

    /** chance の確率で min〜max 個 */
    void maybe(float chance, Item item, int min, int max) {
        if (item != null && item != Items.AIR && r.nextFloat() < chance) add(new ItemStack(item, min + r.nextInt(max - min + 1)));
    }

    /** リストからいくつか選んで落とす（ふつうのドロップ） */
    void pick(int count, Item... items) {
        for (int i = 0; i < count; i++) add(new ItemStack(items[r.nextInt(items.length)], 1 + r.nextInt(2)));
    }

    void potion(float chance, Holder<Potion> potion) {
        if (r.nextFloat() < chance) add(PotionContents.createItemStack(Items.POTION, potion));
    }

    /** ほかの MOD のアイテム（入っていなければ何もしない）。例：lightmanscurrency:coin_copper */
    void modItem(float chance, String id, int min, int max) {
        Item item = BuiltInRegistries.ITEM.getOptional(ResourceLocation.parse(id)).orElse(null);
        maybe(chance, item, min, max);
    }
}
