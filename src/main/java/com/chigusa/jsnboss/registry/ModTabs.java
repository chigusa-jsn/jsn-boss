package com.chigusa.jsnboss.registry;

import com.chigusa.jsnboss.BossConstants;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** クリエイティブタブ：「JSN Boss ボス」「JSN Boss 亜種モブ」「JSN Boss ダンジョンモブ」 */
public final class ModTabs {
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, BossConstants.MODID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> BOSSES = TABS.register("bosses", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.jsnboss.bosses"))
            .icon(() -> new ItemStack(ModItems.FIRE_KNIGHT_SPAWN_EGG.get()))
            .displayItems((params, out) -> {
                out.accept(ModItems.FIRE_KNIGHT_SPAWN_EGG.get());
                out.accept(ModItems.ICE_WITCH_SPAWN_EGG.get());
                out.accept(ModItems.ZOMBIE_KING_SPAWN_EGG.get());
                out.accept(ModItems.DARK_MAGE_SPAWN_EGG.get());
                out.accept(ModItems.BLADE_DRAGON_SPAWN_EGG.get());
                out.accept(ModItems.WHITE_FOX_SPAWN_EGG.get());
                out.accept(ModItems.WIND_CAT_SPAWN_EGG.get());
                out.accept(ModItems.THUNDER_LION_SPAWN_EGG.get());
                out.accept(ModItems.ROCK_GIANT_SPAWN_EGG.get());
                out.accept(ModItems.ENDER_SENTINEL_SPAWN_EGG.get());
                out.accept(ModItems.CREEPER_MOTHER_SPAWN_EGG.get());
                out.accept(ModItems.FENRIR_SPAWN_EGG.get());
                out.accept(ModItems.IRON_SPIDER_SPAWN_EGG.get());
                out.accept(ModItems.FALLEN_VALKYRIE_SPAWN_EGG.get());
                out.accept(ModItems.YGGDRASIL_SPAWN_EGG.get());
                out.accept(ModItems.YGGDRASIL_SPIRIT_SPAWN_EGG.get());
            })
            .build());

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> MOBS = TABS.register("mobs", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.jsnboss.mobs"))
            .icon(() -> new ItemStack(ModMobs.SOUL_BLAZE_EGG.get()))
            .withTabsBefore(BOSSES.getId())
            .displayItems((params, out) -> {
                for (var egg : ModMobs.EGGS) out.accept(egg.get());
                for (var egg : ModThemed.EGGS.values()) out.accept(egg.get());
            })
            .build());

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> DUNGEON = TABS.register("dungeon", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.jsnboss.dungeon"))
            .icon(() -> new ItemStack(ModDungeon.EGGS.get(0).get()))
            .withTabsBefore(MOBS.getId())
            .displayItems((params, out) -> {
                for (var egg : ModDungeon.EGGS) out.accept(egg.get());
            })
            .build());

    private ModTabs() {}
}
