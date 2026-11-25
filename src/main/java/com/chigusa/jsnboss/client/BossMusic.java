package com.chigusa.jsnboss.client;

import com.chigusa.jsnboss.BossConstants;
import com.chigusa.jsnboss.entity.YggdrasilEntity;
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
        SoundEvent best = null;
        int bestPrio = -1;
        for (Entity e : mc.level.entitiesForRendering()) {
            if (!e.isAlive() || e.distanceToSqr(p) > RANGE * RANGE) continue;
            SoundEvent s = null;
            int prio = 0;
            if (e instanceof YggdrasilSpiritEntity spirit) {
                s = spirit.isPhase2() ? ModSounds.MUSIC_SPIRIT_PHASE5.get() : ModSounds.MUSIC_SPIRIT_PHASE4.get();
                prio = spirit.isPhase2() ? 3 : 2;
            } else if (e instanceof YggdrasilEntity) {
                s = ModSounds.MUSIC_YGGDRASIL.get();
                prio = 1;
            }
            if (s != null && prio > bestPrio) { best = s; bestPrio = prio; }
        }
        return best;
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
                current.fadeOut(20);                // 曲が変わるときは1秒で前の曲を消してから次へ
            } else {
                current.fadingOut = false;          // 戻ってきたらフェードを取り消す
                current.fadeTicks = 60;
            }
            MusicManager mm = mc.getMusicManager();
            mm.nextSongDelay = Math.max(mm.nextSongDelay, 200);   // バニラ曲を始めさせない
        } else if (current != null) {
            current.fadeOut(60);
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

        int fadeTicks = 60;

        void fadeOut(int ticks) {
            if (!fadingOut || ticks < fadeTicks) fadeTicks = ticks;
            this.fadingOut = true;
        }

        @Override
        public void tick() {
            if (fadingOut) {
                level -= 1.0F / fadeTicks;
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
