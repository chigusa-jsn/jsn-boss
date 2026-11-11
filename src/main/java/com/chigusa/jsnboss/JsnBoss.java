package com.chigusa.jsnboss;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;

/** MOD のメインクラス */
@Mod(BossConstants.MODID)
public class JsnBoss {
    public JsnBoss(IEventBus modEventBus, ModContainer modContainer) {
        BossSetup.init(modEventBus);
    }
}
