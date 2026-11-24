package com.chigusa.jsnboss.vfx;

import com.chigusa.jsnboss.BossConstants;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModParticles {
    public static final DeferredRegister<ParticleType<?>> PARTICLE_TYPES =
            DeferredRegister.create(Registries.PARTICLE_TYPE, BossConstants.MODID);

    /** 回る詠唱陣（柄・ため・空・発動の光。CircleOptions で送る） */
    public static final DeferredHolder<ParticleType<?>, ParticleType<CircleOptions>> SPELL_CIRCLE =
            PARTICLE_TYPES.register("spell_circle", CircleOptions::type);

    private ModParticles() {}
}
