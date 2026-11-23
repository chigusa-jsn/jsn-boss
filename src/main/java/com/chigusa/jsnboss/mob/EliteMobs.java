package com.chigusa.jsnboss.mob;

import com.chigusa.jsnboss.BossConstants;
import com.chigusa.jsnboss.entity.AbstractBoss;
import com.chigusa.jsnboss.entity.IceWitchEntity;
import com.chigusa.jsnboss.registry.ModEffects;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.ElderGuardian;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.living.LivingExperienceDropEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

/**
 * エリートモブ：自然に湧いた敵モブが、ときどき特性つきの強い個体になる。
 * 名前の頭に特性がつき（見たときだけ表示）、周りにオーラの粒が出る。HP 1.5倍・攻撃力 1.3倍。
 * 倒すとエメラルドなどを多めに落とし、経験値は3倍。
 */
@EventBusSubscriber(modid = BossConstants.MODID)
public final class EliteMobs {
    /** 自然に湧いたモンスターがエリートになる確率 */
    public static final float CHANCE = 0.06F;
    private static final String KEY = "jsnboss_elite";
    private static final String CHILD = "jsnboss_split_child";
    private static final String SUMMONED = "jsnboss_elite_summoned";

    public enum Affix {
        FIRE(ChatFormatting.GOLD, ParticleTypes.FLAME),
        ICE(ChatFormatting.AQUA, ParticleTypes.SNOWFLAKE),
        THUNDER(ChatFormatting.YELLOW, ParticleTypes.ELECTRIC_SPARK),
        GIANT(ChatFormatting.DARK_GREEN, ParticleTypes.CLOUD),
        VAMPIRIC(ChatFormatting.DARK_RED, ParticleTypes.CRIMSON_SPORE),
        REGEN(ChatFormatting.LIGHT_PURPLE, ParticleTypes.HAPPY_VILLAGER),
        SPLIT(ChatFormatting.GREEN, ParticleTypes.ITEM_SLIME),
        SUMMON(ChatFormatting.DARK_PURPLE, ParticleTypes.WITCH);

        final ChatFormatting color;
        final ParticleOptions particle;

        Affix(ChatFormatting color, ParticleOptions particle) {
            this.color = color;
            this.particle = particle;
        }
    }

    private EliteMobs() {}

    /** エリートにしてよいモブか（ボスや大型モブ、MOD のボスは除く） */
    public static boolean eligible(Mob mob) {
        if (!(mob instanceof Monster) || mob instanceof AbstractBoss || mob instanceof WitherBoss || mob instanceof Warden
                || mob instanceof ElderGuardian) return false;
        String ns = BuiltInRegistries.ENTITY_TYPE.getKey(mob.getType()).getNamespace();
        return ns.equals("minecraft") || ns.equals(BossConstants.MODID);
    }

    public static Affix affixOf(Entity e) {
        if (e == null || !e.getPersistentData().contains(KEY)) return null;
        try {
            return Affix.valueOf(e.getPersistentData().getString(KEY));
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    /** 自然スポーンのときに呼ぶ：確率でエリートにする */
    public static void maybeMakeElite(Mob mob, RandomSource rand, MobSpawnType type) {
        if (type != MobSpawnType.NATURAL && type != MobSpawnType.STRUCTURE) return;
        if (!eligible(mob) || rand.nextFloat() >= CHANCE) return;
        makeElite(mob, Affix.values()[rand.nextInt(Affix.values().length)]);
    }

    public static void makeElite(Mob mob, Affix affix) {
        mob.getPersistentData().putString(KEY, affix.name());
        boolean giant = affix == Affix.GIANT;
        modify(mob, Attributes.MAX_HEALTH, "elite_health", giant ? 1.0 : 0.5);
        modify(mob, Attributes.ATTACK_DAMAGE, "elite_damage", giant ? 0.5 : 0.3);
        if (giant) {
            modify(mob, Attributes.SCALE, "elite_scale", 0.5);
            modify(mob, Attributes.KNOCKBACK_RESISTANCE, "elite_knockback", 0.5);
        }
        if (affix == Affix.FIRE) mob.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, MobEffectInstance.INFINITE_DURATION, 0, false, false));
        mob.setHealth(mob.getMaxHealth());
        mob.setCustomName(Component.translatable("elite.jsnboss." + affix.name().toLowerCase(), mob.getType().getDescription())
                .withStyle(affix.color));
        mob.setCustomNameVisible(false);
    }

    private static void modify(Mob mob, net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute> attr, String id, double amount) {
        AttributeInstance inst = mob.getAttribute(attr);
        if (inst == null || inst.hasModifier(BossConstants.id(id))) return;
        inst.addPermanentModifier(new AttributeModifier(BossConstants.id(id), amount, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
    }

    // ============================================================ events
    /** オーラ、再生、仲間を呼ぶ */
    @SubscribeEvent
    public static void onTick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof Mob mob) || !(mob.level() instanceof ServerLevel sl) || !mob.isAlive()) return;
        Affix a = affixOf(mob);
        if (a == null) return;
        if (mob.tickCount % 4 == 0) {
            sl.sendParticles(a.particle, mob.getX(), mob.getY() + mob.getBbHeight() * 0.5, mob.getZ(), 2,
                    mob.getBbWidth() * 0.5, mob.getBbHeight() * 0.4, mob.getBbWidth() * 0.5, 0.01);
        }
        if (a == Affix.REGEN && mob.tickCount % 20 == 0 && mob.getHealth() < mob.getMaxHealth()) mob.heal(1.0F);
        if (a == Affix.SUMMON && mob.tickCount % 240 == 0 && mob.getTarget() != null) {
            long near = sl.getEntitiesOfClass(Mob.class, mob.getBoundingBox().inflate(16), m -> m.getPersistentData().getBoolean(SUMMONED)).size();
            if (near < 3) {
                Entity kid = mob.getType().create(sl);
                if (kid instanceof Mob m) {
                    double ang = mob.getRandom().nextDouble() * Math.PI * 2;
                    m.moveTo(mob.getX() + Math.cos(ang) * 2, mob.getY(), mob.getZ() + Math.sin(ang) * 2, mob.getYRot(), 0);
                    m.finalizeSpawn(sl, sl.getCurrentDifficultyAt(m.blockPosition()), MobSpawnType.MOB_SUMMONED, null);
                    m.getPersistentData().putBoolean(SUMMONED, true);
                    m.setTarget(mob.getTarget());
                    sl.addFreshEntity(m);
                    sl.sendParticles(ParticleTypes.WITCH, m.getX(), m.getY() + 1, m.getZ(), 20, 0.4, 0.6, 0.4, 0.05);
                }
            }
        }
    }

    /** エリートの攻撃に特性の効果をつける（矢などの飛び道具も、撃った本人がエリートなら効く） */
    @SubscribeEvent
    public static void onDamage(LivingDamageEvent.Post event) {
        Entity src = event.getSource().getEntity();
        Affix a = affixOf(src);
        if (a == null || !(src instanceof LivingEntity attacker)) return;
        LivingEntity victim = event.getEntity();
        switch (a) {
            case FIRE -> victim.igniteForSeconds(5);
            case ICE -> {
                IceWitchEntity.freeze(victim, 140);
                victim.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 1));
            }
            case THUNDER -> victim.addEffect(new MobEffectInstance(ModEffects.STUN, 20, 0));
            case VAMPIRIC -> attacker.heal(event.getNewDamage() * 0.5F);
            default -> {}
        }
    }

    /** 分裂：倒されると小さな2体に分かれる */
    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof Mob mob) || !(mob.level() instanceof ServerLevel sl)) return;
        if (affixOf(mob) != Affix.SPLIT || mob.getPersistentData().getBoolean(CHILD)) return;
        for (int i = 0; i < 2; i++) {
            Entity e = mob.getType().create(sl);
            if (!(e instanceof Mob m)) continue;
            m.moveTo(mob.getX() + (i == 0 ? 0.5 : -0.5), mob.getY(), mob.getZ(), mob.getYRot(), 0);
            m.finalizeSpawn(sl, sl.getCurrentDifficultyAt(m.blockPosition()), MobSpawnType.MOB_SUMMONED, null);
            m.getPersistentData().putBoolean(CHILD, true);
            modify(m, Attributes.SCALE, "split_scale", -0.35);
            modify(m, Attributes.MAX_HEALTH, "split_health", -0.5);
            m.setHealth(m.getMaxHealth());
            if (mob.getTarget() != null) m.setTarget(mob.getTarget());
            sl.addFreshEntity(m);
        }
        sl.sendParticles(ParticleTypes.ITEM_SLIME, mob.getX(), mob.getY() + 1, mob.getZ(), 30, 0.4, 0.5, 0.4, 0.1);
    }

    /** ごほうび：エメラルド 1〜3、ときどきダイヤや金のリンゴ */
    @SubscribeEvent
    public static void onDrops(LivingDropsEvent event) {
        LivingEntity e = event.getEntity();
        if (affixOf(e) == null || !(e.level() instanceof ServerLevel)) return;
        RandomSource r = e.getRandom();
        add(event, e, new ItemStack(Items.EMERALD, 1 + r.nextInt(3)));
        if (r.nextFloat() < 0.25F) add(event, e, new ItemStack(Items.DIAMOND));
        if (r.nextFloat() < 0.10F) add(event, e, new ItemStack(Items.GOLDEN_APPLE));
    }

    private static void add(LivingDropsEvent event, LivingEntity e, ItemStack stack) {
        event.getDrops().add(new ItemEntity(e.level(), e.getX(), e.getY() + 0.5, e.getZ(), stack));
    }

    @SubscribeEvent
    public static void onExperience(LivingExperienceDropEvent event) {
        if (affixOf(event.getEntity()) != null) event.setDroppedExperience(event.getDroppedExperience() * 3);
    }

    /** /summon などで試すとき用：EntityType からエリートを作る */
    public static Mob spawnElite(ServerLevel sl, EntityType<?> type, double x, double y, double z, Affix affix) {
        Entity e = type.create(sl);
        if (!(e instanceof Mob m)) return null;
        m.moveTo(x, y, z, 0, 0);
        m.finalizeSpawn(sl, sl.getCurrentDifficultyAt(m.blockPosition()), MobSpawnType.COMMAND, null);
        makeElite(m, affix);
        sl.addFreshEntity(m);
        return m;
    }
}
