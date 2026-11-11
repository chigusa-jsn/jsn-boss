package com.chigusa.jsnboss.registry;

import com.chigusa.jsnboss.BossConstants;
import com.chigusa.jsnboss.effect.LacerationEffect;
import com.chigusa.jsnboss.effect.SoulBurnEffect;
import com.chigusa.jsnboss.effect.StunEffect;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffect;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModEffects {
    public static final DeferredRegister<MobEffect> MOB_EFFECTS =
            DeferredRegister.create(Registries.MOB_EFFECT, BossConstants.MODID);

    public static final DeferredHolder<MobEffect, SoulBurnEffect> SOUL_BURN =
            MOB_EFFECTS.register("soul_burn", SoulBurnEffect::new);

    public static final DeferredHolder<MobEffect, LacerationEffect> LACERATION =
            MOB_EFFECTS.register("laceration", LacerationEffect::new);

    public static final DeferredHolder<MobEffect, StunEffect> STUN =
            MOB_EFFECTS.register("stun", StunEffect::new);

    private ModEffects() {}
}
