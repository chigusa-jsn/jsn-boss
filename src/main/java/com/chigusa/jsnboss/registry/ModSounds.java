package com.chigusa.jsnboss.registry;

import com.chigusa.jsnboss.BossConstants;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** オリジナルBGMなどの音 */
public final class ModSounds {
    public static final DeferredRegister<SoundEvent> SOUND_EVENTS = DeferredRegister.create(Registries.SOUND_EVENT, BossConstants.MODID);

    /** ユグドラシル（第1〜3形態）のボス曲 */
    public static final DeferredHolder<SoundEvent, SoundEvent> MUSIC_YGGDRASIL = register("music.yggdrasil");
    /** ユグドラシル精霊・第4形態のボス曲 */
    public static final DeferredHolder<SoundEvent, SoundEvent> MUSIC_SPIRIT_PHASE4 = register("music.spirit_phase4");
    /** ユグドラシル精霊・第5形態のボス曲 */
    public static final DeferredHolder<SoundEvent, SoundEvent> MUSIC_SPIRIT_PHASE5 = register("music.spirit_phase5");

    /** 普通のボスの曲（キーはエンティティIDのパス。分身は "_clone" を外して引く） */
    public static final java.util.Map<String, DeferredHolder<SoundEvent, SoundEvent>> BOSS_MUSIC = new java.util.HashMap<>();
    static {
        for (String id : new String[] {"fire_knight", "ice_witch", "zombie_king", "dark_mage", "blade_dragon", "white_fox", "wind_cat", "thunder_lion", "rock_giant", "ender_sentinel", "creeper_mother", "fenrir", "iron_spider", "fallen_valkyrie"}) {
            BOSS_MUSIC.put(id, register("music." + id));
        }
    }

    private static DeferredHolder<SoundEvent, SoundEvent> register(String name) {
        return SOUND_EVENTS.register(name, () -> SoundEvent.createVariableRangeEvent(
                ResourceLocation.fromNamespaceAndPath(BossConstants.MODID, name)));
    }

    private ModSounds() {}
}
