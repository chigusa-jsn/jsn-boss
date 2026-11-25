package com.chigusa.jsnboss.client;

import com.chigusa.jsnboss.BossConstants;
import com.chigusa.jsnboss.entity.YggdrasilSpiritEntity;
import com.chigusa.jsnboss.registry.ModSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.MusicManager;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/**
 * ボス戦BGM。対象のボスが近くにいる間だけループで流し、いなくなったらフェードアウトする。
 * 流れている間はバニラのBGMを止めておく。音量は「音楽」スライダーに従う。
 */
@EventBusSubscriber(modid = BossConstants.MODID, value = Dist.CLIENT)
public final class BossMusic {
    private static final double RANGE = 80.0;
    private static Track current;

    /** 今流すべき曲（なければ null） */
    private static SoundEvent wanted(Minecraft mc) {
        Player p = mc.player;
        if (p == null || mc.level == null) return null;
        for (Entity e : mc.level.entitiesForRendering()) {
            if (e instanceof YggdrasilSpiritEntity s && s.isAlive() && s.isPhase2() && s.distanceToSqr(p) < RANGE * RANGE) {
                return ModSounds.MUSIC_SPIRIT_PHASE5.get();
            }
        }
        return null;
    }

    @SubscribeEvent
    public static void onTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        SoundEvent want = wanted(mc);

        // 止まった／ワールドを抜けて音が消えたら手放す
        if (current != null && (current.isStopped() || current.age++ > 10 && !mc.getSoundManager().isActive(current))) current = null;

        if (want != null) {
            if (current == null) {
                current = new Track(want);
                mc.getMusicManager().stopPlaying();
                mc.getSoundManager().play(current);
            } else if (current.sound != want) {
                current.fadeOut();                  // 曲が変わるときは前の曲が消えてから次へ
            } else {
                current.fadingOut = false;          // 戻ってきたらフェードを取り消す
            }
            MusicManager mm = mc.getMusicManager();
            mm.nextSongDelay = Math.max(mm.nextSongDelay, 200);   // バニラ曲を始めさせない
        } else if (current != null) {
            current.fadeOut();
        }
    }

    private static final class Track extends AbstractTickableSoundInstance {
        final SoundEvent sound;
        boolean fadingOut;
        int age;
        float level = 0.0F;

        Track(SoundEvent sound) {
            super(sound, SoundSource.MUSIC, SoundInstance.createUnseededRandom());
            this.sound = sound;
            this.looping = true;
            this.delay = 0;
            this.relative = true;
            this.attenuation = SoundInstance.Attenuation.NONE;
            this.volume = 0.01F;
        }

        void fadeOut() { this.fadingOut = true; }

        @Override
        public void tick() {
            if (fadingOut) {
                level -= 1.0F / 60;                  // 3秒でフェードアウト
                if (level <= 0) { stop(); return; }
            } else if (level < 1.0F) {
                level = Math.min(1.0F, level + 1.0F / 20);   // 1秒でフェードイン
            }
            this.volume = Math.max(0.01F, level);
        }

        @Override
        public boolean canStartSilent() { return true; }
    }

    private BossMusic() {}
}
