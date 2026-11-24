package com.chigusa.jsnboss.mob;

import com.chigusa.jsnboss.BossConstants;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.living.LivingEvent;

/**
 * 友達の案の亜種の効果とドロップ（殴りも矢も、ここでまとめて処理する）。
 * 森の：鈍足 / べたべた：くっつく（鈍足＋ジャンプできない）/ 海の：採掘速度低下 / 空の：打ち上げ /
 * 黄金の：弱体化＋毒 / 残響の：暗闇（倒すと10%でウォーデン）/ 鉱石：防御力（OreZombie）/ 光る：発光
 */
@EventBusSubscriber(modid = BossConstants.MODID)
public final class VariantEffects {
    private static final String STICKY = "jsnboss_sticky_until";

    private VariantEffects() {}

    /** 攻撃した本人（矢なら撃った本人） */
    static Entity attacker(net.minecraft.world.damagesource.DamageSource src) {
        Entity e = src.getEntity();
        if (e == null && src.getDirectEntity() instanceof Projectile p) e = p.getOwner();
        return e;
    }

    @SubscribeEvent
    public static void onDamage(LivingDamageEvent.Post event) {
        if (event.getNewDamage() <= 0) return;
        Entity a = attacker(event.getSource());
        LivingEntity v = event.getEntity();
        if (!(a instanceof Themed th) || !(v.level() instanceof ServerLevel sl) || v == a) return;
        switch (th.theme()) {
            case MOSS -> {                                              // 森の：鈍足
                v.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 1));
                sl.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.MOSS_BLOCK.defaultBlockState()),
                        v.getX(), v.getY() + 0.8, v.getZ(), 15, 0.3, 0.4, 0.3, 0.1);
            }
            case HONEY -> {                                             // べたべた：くっつく
                v.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 1));
                v.getPersistentData().putLong(STICKY, sl.getGameTime() + 60);
                sl.sendParticles(ParticleTypes.DRIPPING_HONEY, v.getX(), v.getY() + 1, v.getZ(), 12, 0.3, 0.4, 0.3, 0);
            }
            case SEA -> v.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 120, 1));   // 海の：採掘速度低下
            case SKY -> {                                               // 空の：打ち上げ
                v.setDeltaMovement(v.getDeltaMovement().x, Math.max(v.getDeltaMovement().y, 0) + 0.9, v.getDeltaMovement().z);
                v.hurtMarked = true;
                sl.sendParticles(ParticleTypes.CLOUD, v.getX(), v.getY() + 0.2, v.getZ(), 15, 0.4, 0.1, 0.4, 0.05);
            }
            case GOLD -> {                                              // 黄金の：弱体化＋毒
                v.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 100, 0));
                v.addEffect(new MobEffectInstance(MobEffects.POISON, 60, 0));
            }
            case SCULK -> v.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 120, 0));       // 残響の：暗闇
            case GLOW -> v.addEffect(new MobEffectInstance(MobEffects.GLOWING, 200, 0));         // 光る：発光
            case ORE -> {}                                              // 鉱石：防御力（OreTiers）
        }
    }

    /** べたべたにくっつかれている間はジャンプできない */
    @SubscribeEvent
    public static void onJump(LivingEvent.LivingJumpEvent event) {
        LivingEntity e = event.getEntity();
        long until = e.getPersistentData().getLong(STICKY);
        if (until > 0 && e.level().getGameTime() < until) {
            Vec3 m = e.getDeltaMovement();
            e.setDeltaMovement(m.x * 0.3, 0, m.z * 0.3);
        }
    }

    /** 残響の：倒すと 10% でウォーデンが地面から現れる */
    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof Themed th && th.theme() == Theme.SCULK && event.getEntity().level() instanceof ServerLevel sl
                && event.getEntity().getRandom().nextFloat() < 0.10F) {
            summonWarden(sl, event.getEntity().position());
        }
    }

    /** ウォーデンを地面から呼び出す（出てくるアニメーションつき） */
    public static void summonWarden(ServerLevel sl, Vec3 pos) {
        if (sl.getDifficulty() == net.minecraft.world.Difficulty.PEACEFUL) return;
        Warden w = EntityType.WARDEN.create(sl);
        if (w == null) return;
        w.moveTo(pos.x, pos.y, pos.z, 0, 0);
        w.finalizeSpawn(sl, sl.getCurrentDifficultyAt(w.blockPosition()), MobSpawnType.TRIGGERED, null);
        sl.addFreshEntity(w);
        sl.playSound(null, pos.x, pos.y, pos.z, SoundEvents.WARDEN_EMERGE, w.getSoundSource(), 3.0F, 1.0F);
    }

    // ============================================================ ドロップ
    @SubscribeEvent
    public static void onDrops(LivingDropsEvent event) {
        LivingEntity e = event.getEntity();
        if (!(e instanceof Themed th) || !(e.level() instanceof ServerLevel)) return;
        // スライムは分裂するので、大きいものだけ落とす（レアは大きさ4だけ）
        boolean rare = true;
        if (e instanceof net.minecraft.world.entity.monster.Slime sl) {
            if (sl.getSize() < 2) return;
            rare = sl.getSize() >= 4;
        }
        Drops d = new Drops(event);
        switch (th.theme()) {
            case MOSS -> d.pick(2, Items.OAK_SAPLING, Items.BIRCH_SAPLING, Items.SPRUCE_SAPLING, Items.JUNGLE_SAPLING, Items.CHERRY_SAPLING,
                    Items.WHEAT_SEEDS, Items.BEETROOT_SEEDS, Items.MELON_SEEDS, Items.PUMPKIN_SEEDS, Items.SWEET_BERRIES, Items.GLOW_BERRIES);
            case HONEY -> {
                d.pick(2, Items.HONEYCOMB, Items.HONEY_BOTTLE, Items.SLIME_BALL);
                if (rare) {
                    d.maybe(0.06F, Items.HONEY_BLOCK, 1, 1);
                    d.maybe(0.06F, Items.HONEYCOMB_BLOCK, 1, 1);
                    d.maybe(0.06F, Items.SLIME_BLOCK, 1, 1);
                }
            }
            case SEA -> {
                d.pick(2, Items.PRISMARINE_SHARD, Items.PRISMARINE_CRYSTALS);
                if (rare) {
                    d.maybe(0.15F, Items.NAUTILUS_SHELL, 1, 1);
                    d.maybe(0.10F, Items.SEA_LANTERN, 1, 1);
                }
            }
            case SKY -> {
                d.maybe(0.9F, Items.FEATHER, 1, 3);
                d.maybe(0.5F, Items.WIND_CHARGE, 1, 3);
                if (rare) d.maybe(0.15F, Items.BREEZE_ROD, 1, 1);
            }
            case GOLD -> {
                d.maybe(0.9F, Items.GOLD_NUGGET, 2, 6);
                d.maybe(0.30F, Items.GOLD_INGOT, 1, 2);
                d.maybe(0.15F, Items.GOLDEN_CARROT, 1, 2);
                d.maybe(0.10F, Items.GLISTERING_MELON_SLICE, 1, 2);
                if (rare) {
                    d.maybe(0.05F, Items.GOLDEN_APPLE, 1, 1);
                    d.maybe(0.02F, Items.GOLD_BLOCK, 1, 1);
                }
                d.modItem(0.60F, "lightmanscurrency:coin_copper", 2, 5);    // ライトマンの通貨（少量）
                d.modItem(0.20F, "lightmanscurrency:coin_iron", 1, 3);      // 中量
                if (rare) d.modItem(0.05F, "lightmanscurrency:coin_gold", 1, 1);   // 大量
            }
            case SCULK -> {
                d.pick(2, Items.AMETHYST_SHARD, Items.LAPIS_LAZULI);
                d.maybe(0.20F, Items.EMERALD, 1, 2);
                if (rare) {
                    d.maybe(0.05F, Items.ECHO_SHARD, 1, 1);
                    d.maybe(0.03F, Items.SCULK_SHRIEKER, 1, 1);
                }
            }
            case ORE -> {
                Item[] raw = {Items.COAL, Items.RAW_IRON, Items.RAW_GOLD, Items.DIAMOND};
                Item[] ore = {Items.COAL_ORE, Items.IRON_ORE, Items.GOLD_ORE, Items.DIAMOND_ORE};
                int t = th.oreTier();
                d.maybe(0.8F, Items.COBBLESTONE, 1, 3);
                d.maybe(t == 3 ? 0.25F : 0.5F, raw[t], 1, t == 3 ? 1 : 2);
                if (rare) d.maybe(0.08F, ore[t], 1, 1);
            }
            case GLOW -> {
                d.pick(1, Items.GLOW_LICHEN, Items.REDSTONE, Items.GLOWSTONE_DUST);
                if (rare) {
                    d.maybe(0.10F, d.r.nextBoolean() ? Items.OCHRE_FROGLIGHT : Items.VERDANT_FROGLIGHT, 1, 1);
                    d.maybe(0.005F, Items.NETHER_STAR, 1, 1);
                }
            }
        }
    }
}
