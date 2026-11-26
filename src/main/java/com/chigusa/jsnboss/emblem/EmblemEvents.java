package com.chigusa.jsnboss.emblem;

import com.chigusa.jsnboss.BossConstants;
import com.chigusa.jsnboss.registry.ModEmblems;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import top.theillusivec4.curios.api.CuriosApi;

/** 紋章の効果（攻撃・被弾・撃破）と、ボスを倒したときの核の配布 */
@EventBusSubscriber(modid = BossConstants.MODID)
public final class EmblemEvents {
    /** 核をもらえる範囲（ブロック） */
    private static final double CORE_RANGE = 48.0;
    private static final String BLINK_CD = "jsnboss_emblem_blink";

    private EmblemEvents() {}

    /** つけている紋章（なければ null） */
    public static EmblemItem worn(LivingEntity e) {
        if (e == null) return null;
        return CuriosApi.getCuriosInventory(e)
                .flatMap(h -> h.findFirstCurio(s -> s.getItem() instanceof EmblemItem))
                .map(r -> (EmblemItem) r.stack().getItem())
                .orElse(null);
    }

    private static EmblemItem worn(LivingEntity e, String key) {
        EmblemItem it = worn(e);
        return it != null && it.key().equals(key) ? it : null;
    }

    // ============================================================ ダメージ
    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onDamage(LivingIncomingDamageEvent event) {
        LivingEntity victim = event.getEntity();
        if (victim.level().isClientSide) return;
        DamageSource src = event.getSource();

        // ---- 受ける側
        EmblemItem def = worn(victim);
        if (def != null) {
            switch (def.key()) {
                case "crimson" -> {
                    if (src.is(DamageTypeTags.IS_FIRE)) event.setAmount(event.getAmount() * (float) (1 - def.pick(0.3, 0.5, 0.7)));
                }
                case "frostbloom" -> {
                    if (src.is(DamageTypeTags.IS_FREEZING)) {
                        event.setCanceled(true);
                        return;
                    }
                }
                case "verdant" -> {
                    if (src.is(DamageTypeTags.IS_EXPLOSION)) event.setAmount(event.getAmount() * (float) (1 - def.pick(0.3, 0.5, 0.7)));
                }
                case "void" -> {
                    if (src.getEntity() != null && !src.is(DamageTypeTags.BYPASSES_INVULNERABILITY) && tryBlink(victim, def)) {
                        event.setCanceled(true);
                        return;
                    }
                }
                default -> {
                }
            }
        }

        // ---- 攻撃する側
        if (!(src.getEntity() instanceof LivingEntity attacker) || attacker == victim) return;
        EmblemItem atk = worn(attacker);
        if (atk == null) return;
        boolean melee = src.getDirectEntity() == attacker;
        switch (atk.key()) {
            case "crimson" -> {
                if (melee) victim.igniteForSeconds((float) atk.pick(2, 3, 4));
            }
            case "frostbloom" -> {
                if (melee) victim.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN,
                        (int) atk.pick(20, 40, 40), atk.rank() == 3 && attacker.getRandom().nextFloat() < 0.3F ? 2 : 1));
            }
            case "abyss" -> {
                if (melee && isBehind(attacker, victim)) {
                    event.setAmount(event.getAmount() * (float) (1 + atk.pick(0.15, 0.25, 0.35)));
                    if (victim.level() instanceof ServerLevel sl)
                        sl.sendParticles(ParticleTypes.WITCH, victim.getX(), victim.getY(0.6), victim.getZ(), 8, 0.3, 0.3, 0.3, 0.02);
                }
            }
            case "thunder" -> {
                if (melee && attacker.getRandom().nextFloat() < atk.pick(0.10, 0.15, 0.20)) {
                    event.setAmount(event.getAmount() + 3.0F);
                    strikeVisual(victim);
                }
            }
            case "mecha" -> {
                if (src.is(DamageTypeTags.IS_PROJECTILE)) event.setAmount(event.getAmount() * (float) (1 + atk.pick(0.1, 0.2, 0.3)));
            }
            case "fallen" -> {
                if (melee && attacker.fallDistance > 0.0F && !attacker.onGround())
                    event.setAmount(event.getAmount() * (float) (1 + atk.pick(0.2, 0.35, 0.5)));
            }
            default -> {
            }
        }
    }

    /** 攻撃した側が相手の背中側にいるか */
    private static boolean isBehind(LivingEntity attacker, LivingEntity victim) {
        Vec3 look = Vec3.directionFromRotation(0, victim.yBodyRot);
        Vec3 to = attacker.position().subtract(victim.position()).multiply(1, 0, 1);
        if (to.lengthSqr() < 1.0E-4) return false;
        return look.dot(to.normalize()) < -0.3;
    }

    /** 見た目だけの雷（地形も燃やさない） */
    private static void strikeVisual(LivingEntity at) {
        if (!(at.level() instanceof ServerLevel sl)) return;
        LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(sl);
        if (bolt == null) return;
        bolt.moveTo(at.getX(), at.getY(), at.getZ());
        bolt.setVisualOnly(true);
        sl.addFreshEntity(bolt);
    }

    /** 虚空の紋章：少し離れた場所へ跳んで攻撃をかわす（8秒に1回まで） */
    private static boolean tryBlink(LivingEntity e, EmblemItem em) {
        long now = e.level().getGameTime();
        long ready = e.getPersistentData().getLong(BLINK_CD);
        if (now < ready || e.getRandom().nextFloat() >= em.pick(0.10, 0.15, 0.20)) return false;
        Vec3 from = e.position();
        for (int i = 0; i < 12; i++) {
            double a = e.getRandom().nextDouble() * Mth.TWO_PI;
            double d = 3 + e.getRandom().nextDouble() * 3;
            double x = from.x + Math.cos(a) * d, z = from.z + Math.sin(a) * d;
            double y = from.y + e.getRandom().nextInt(3) - 1;
            if (e.randomTeleport(x, y, z, true)) {
                e.getPersistentData().putLong(BLINK_CD, now + 160);
                e.level().playSound(null, from.x, from.y, from.z, SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.8F, 1.3F);
                if (e.level() instanceof ServerLevel sl)
                    sl.sendParticles(ParticleTypes.PORTAL, from.x, from.y + 1, from.z, 24, 0.3, 0.6, 0.3, 0.3);
                return true;
            }
        }
        return false;
    }

    // ============================================================ 撃破
    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        LivingEntity dead = event.getEntity();
        if (dead.level().isClientSide) return;
        DamageSource src = event.getSource();

        // 鎖狼の紋章：倒した相手のぶん回復
        if (src.getEntity() instanceof LivingEntity killer) {
            EmblemItem em = worn(killer, "chain_wolf");
            if (em != null) killer.heal(em.rank() * 2.0F);
        }

        // ボスの核：近くにいた全員に1つずつ（/kill では配らない）
        ResourceLocation id = EntityType.getKey(dead.getType());
        if (!id.getNamespace().equals(BossConstants.MODID) || src.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) return;
        String key = ModEmblems.BY_BOSS.get(id.getPath());
        if (key == null || !(dead.level() instanceof ServerLevel sl)) return;
        for (ServerPlayer p : sl.players()) {
            if (p.isSpectator() || p.distanceToSqr(dead) > CORE_RANGE * CORE_RANGE) continue;
            ItemStack core = new ItemStack(ModEmblems.CORES.get(key).get());
            p.sendSystemMessage(Component.translatable("emblem.jsnboss.got_core", core.getHoverName()).withStyle(ChatFormatting.GOLD));
            if (!p.getInventory().add(core)) p.drop(core, false);
            sl.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.6F, 1.4F);
        }
    }
}
