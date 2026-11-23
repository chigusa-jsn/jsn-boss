package com.chigusa.jsnboss;

import com.chigusa.jsnboss.entity.AbstractBoss;
import com.chigusa.jsnboss.entity.CreeperMotherEntity;
import net.minecraft.world.entity.Entity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.ExplosionEvent;

/** ゲーム中のイベント（サーバー・クライアント共通） */
@EventBusSubscriber(modid = BossConstants.MODID)
public final class BossEvents {
    private BossEvents() {}

    /** マザークリーパーの子どもの爆発は、ブロックを壊さず、ボスと仲間の子どもには当たらない */
    @SubscribeEvent
    public static void onExplosion(ExplosionEvent.Detonate event) {
        Entity src = event.getExplosion().getDirectSourceEntity();
        if (src == null || !src.getTags().contains(CreeperMotherEntity.KID_TAG)) return;
        event.getAffectedBlocks().clear();
        event.getAffectedEntities().removeIf(e -> e instanceof AbstractBoss || e.getTags().contains(CreeperMotherEntity.KID_TAG));
    }
}
