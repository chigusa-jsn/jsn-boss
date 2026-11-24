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
 * 名前の頭に特性がつき（見たときだけ表示）、周りにオーラの粒が出る。HP 100（巨大な・肉の は 150）・攻撃力 1.3倍。
 * 倒すと特性ごとのドロップを落とし、経験値は3倍。
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
        SUMMON(ChatFormatting.DARK_PURPLE, ParticleTypes.WITCH),
        // 友達の案
        POISON(ChatFormatting.DARK_GREEN, ParticleTypes.WARPED_SPORE),
        MEAT(ChatFormatting.RED, ParticleTypes.DAMAGE_INDICATOR),
        LUCKY(ChatFormatting.GREEN, ParticleTypes.WAX_ON, 1),
        EXPLOSIVE(ChatFormatting.GOLD, ParticleTypes.SMOKE),
        REVIVE(ChatFormatting.WHITE, ParticleTypes.TOTEM_OF_UNDYING, 1),
        DESPAIR(ChatFormatting.DARK_GRAY, ParticleTypes.SCULK_SOUL, 1);

        final ChatFormatting color;
        final ParticleOptions particle;
        /** 出やすさ（強い特性は出にくい） */
        final int weight;

        Affix(ChatFormatting color, ParticleOptions particle) {
            this(color, particle, 3);
        }

        Affix(ChatFormatting color, ParticleOptions particle, int weight) {
            this.color = color;
            this.particle = particle;
            this.weight = weight;
        }

        static Affix random(RandomSource r) {
            int total = 0;
            for (Affix a : values()) total += a.weight;
            int n = r.nextInt(total);
            for (Affix a : values()) {
                n -= a.weight;
                if (n < 0) return a;
            }
            return FIRE;
        }
    }

    /** エリートの HP（ふつう 100、巨大な・肉の は 150） */
    public static final double HP = 100.0, HP_BIG = 150.0, SPLIT_CHILD_HP = 25.0, REVIVE_HP = 50.0;
    private static final String REVIVED = "jsnboss_elite_revived";

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
        makeElite(mob, Affix.random(rand));
    }

    public static void makeElite(Mob mob, Affix affix) {
        mob.getPersistentData().putString(KEY, affix.name());
        boolean giant = affix == Affix.GIANT;
        setMaxHealth(mob, "elite_health", giant || affix == Affix.MEAT ? HP_BIG : HP);
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

    /** 最大 HP をちょうど hp にする（元のモブの HP に関係なく） */
    private static void setMaxHealth(Mob mob, String id, double hp) {
        AttributeInstance inst = mob.getAttribute(Attributes.MAX_HEALTH);
        if (inst == null) return;
        inst.removeModifier(BossConstants.id(id));
        inst.addPermanentModifier(new AttributeModifier(BossConstants.id(id), hp - inst.getBaseValue(), AttributeModifier.Operation.ADD_VALUE));
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
        if (a == Affix.REGEN && mob.tickCount % 20 == 0 && mob.getHealth() < mob.getMaxHealth()
                && mob.tickCount - mob.getLastHurtByMobTimestamp() > 60) mob.heal(2.0F);    // 殴られてから3秒は回復しない
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
            case POISON -> victim.addEffect(new MobEffectInstance(MobEffects.POISON, 80, 1));
            case LUCKY -> victim.addEffect(randomEffect(attacker.getRandom(), LUCKY_EFFECTS));
            case DESPAIR -> victim.addEffect(randomEffect(attacker.getRandom(), DESPAIR_EFFECTS));
            default -> {}
        }
    }

    /** 幸運の：いい効果か悪い効果か、運しだい */
    private static final java.util.List<net.minecraft.core.Holder<net.minecraft.world.effect.MobEffect>> LUCKY_EFFECTS = java.util.List.of(
            MobEffects.LUCK, MobEffects.UNLUCK, MobEffects.MOVEMENT_SPEED, MobEffects.MOVEMENT_SLOWDOWN, MobEffects.REGENERATION,
            MobEffects.POISON, MobEffects.JUMP, MobEffects.LEVITATION, MobEffects.GLOWING, MobEffects.CONFUSION, MobEffects.HEAL, MobEffects.HUNGER);
    /** 絶望の：悪い効果だけ */
    private static final java.util.List<net.minecraft.core.Holder<net.minecraft.world.effect.MobEffect>> DESPAIR_EFFECTS = java.util.List.of(
            MobEffects.MOVEMENT_SLOWDOWN, MobEffects.WEAKNESS, MobEffects.WITHER, MobEffects.BLINDNESS, MobEffects.HUNGER,
            MobEffects.DIG_SLOWDOWN, MobEffects.CONFUSION, MobEffects.DARKNESS, MobEffects.POISON, MobEffects.LEVITATION);

    private static MobEffectInstance randomEffect(RandomSource r, java.util.List<net.minecraft.core.Holder<net.minecraft.world.effect.MobEffect>> list) {
        var eff = list.get(r.nextInt(list.size()));
        int dur = eff == MobEffects.LEVITATION ? 30 : eff == MobEffects.HEAL ? 1 : 100;
        return new MobEffectInstance(eff, dur, eff == MobEffects.WITHER ? 1 : 0);
    }

    /** 復活の：1回だけ HP 50 で起き上がる / 爆発の：倒すと爆発（ブロックは壊さない）/ 絶望の：倒すとウォーデン */
    @SubscribeEvent
    public static void onDeathSpecial(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof Mob mob) || !(mob.level() instanceof ServerLevel sl)) return;
        Affix a = affixOf(mob);
        if (a == Affix.REVIVE && !mob.getPersistentData().getBoolean(REVIVED)) {
            event.setCanceled(true);
            mob.getPersistentData().putBoolean(REVIVED, true);
            mob.setHealth((float) REVIVE_HP);
            mob.removeAllEffects();
            sl.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, mob.getX(), mob.getY() + 1, mob.getZ(), 60, 0.5, 0.8, 0.5, 0.4);
            sl.playSound(null, mob.getX(), mob.getY(), mob.getZ(), net.minecraft.sounds.SoundEvents.TOTEM_USE, mob.getSoundSource(), 1.0F, 1.0F);
        } else if (a == Affix.EXPLOSIVE) {
            sl.explode(mob, mob.getX(), mob.getY() + 0.5, mob.getZ(), 3.0F, net.minecraft.world.level.Level.ExplosionInteraction.NONE);
        } else if (a == Affix.DESPAIR) {
            VariantEffects.summonWarden(sl, mob.position());
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
            setMaxHealth(m, "split_health", SPLIT_CHILD_HP);
            m.setHealth(m.getMaxHealth());
            if (mob.getTarget() != null) m.setTarget(mob.getTarget());
            sl.addFreshEntity(m);
        }
        sl.sendParticles(ParticleTypes.ITEM_SLIME, mob.getX(), mob.getY() + 1, mob.getZ(), 30, 0.4, 0.5, 0.4, 0.1);
    }

    /** ごほうび：特性ごとのドロップ。友達の案がない特性はエメラルド 1〜3、ときどきダイヤや金のリンゴ */
    @SubscribeEvent
    public static void onDrops(LivingDropsEvent event) {
        LivingEntity e = event.getEntity();
        Affix a = affixOf(e);
        if (a == null || !(e.level() instanceof ServerLevel)) return;
        Drops d = new Drops(event);
        switch (a) {
            case REGEN -> {
                d.maybe(0.6F, Items.GHAST_TEAR, 1, 2);
                d.maybe(0.03F, Items.ENCHANTED_GOLDEN_APPLE, 1, 1);
            }
            case FIRE -> {
                d.pick(2, Items.FLINT, Items.BLAZE_POWDER);
                d.maybe(0.3F, Items.BLAZE_ROD, 1, 1);
                d.maybe(0.05F, Items.DRAGON_BREATH, 1, 1);
            }
            case POISON -> {
                d.pick(2, Items.POISONOUS_POTATO, Items.PUFFERFISH, Items.SPIDER_EYE);
                d.potion(0.15F, net.minecraft.world.item.alchemy.Potions.POISON);
            }
            case MEAT -> d.pick(3, Items.BEEF, Items.PORKCHOP, Items.CHICKEN, Items.MUTTON, Items.RABBIT);
            case LUCKY -> {
                d.pick(1, Items.RABBIT_FOOT, Items.RABBIT_HIDE);
                d.potion(0.10F, net.minecraft.world.item.alchemy.Potions.LUCK);
                d.maybe(0.10F, Items.DIAMOND, 1, 2);
                d.maybe(0.05F, Items.DRAGON_BREATH, 1, 1);
                d.maybe(0.05F, Items.ECHO_SHARD, 1, 1);
                d.maybe(0.03F, Items.ENCHANTED_GOLDEN_APPLE, 1, 1);
                d.maybe(0.03F, Items.HEART_OF_THE_SEA, 1, 1);
                d.maybe(0.01F, Items.NETHER_STAR, 1, 1);
            }
            case EXPLOSIVE -> {
                d.maybe(0.9F, Items.GUNPOWDER, 2, 4);
                d.pick(1, Items.FIRE_CHARGE, Items.FIREWORK_ROCKET, Items.COAL);
            }
            case REVIVE -> {
                d.maybe(0.5F, Items.GHAST_TEAR, 1, 1);
                d.potion(0.25F, net.minecraft.world.item.alchemy.Potions.HEALING);
                d.potion(0.20F, net.minecraft.world.item.alchemy.Potions.REGENERATION);
                d.maybe(0.15F, Items.GOLDEN_APPLE, 1, 1);
                d.maybe(0.03F, Items.ENCHANTED_GOLDEN_APPLE, 1, 1);
                d.maybe(0.05F, Items.TOTEM_OF_UNDYING, 1, 1);
            }
            case DESPAIR -> {
                d.pick(2, Items.SUSPICIOUS_STEW, Items.DEAD_BUSH, Items.BONE_MEAL);
                d.maybe(0.05F, Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE, 1, 1);
            }
            default -> {
                d.add(new ItemStack(Items.EMERALD, 1 + d.r.nextInt(3)));
                d.maybe(0.25F, Items.DIAMOND, 1, 1);
                d.maybe(0.10F, Items.GOLDEN_APPLE, 1, 1);
            }
        }
    }

    private static void add(LivingDropsEvent event, LivingEntity e, ItemStack stack) {
        event.getDrops().add(new ItemEntity(e.level(), e.getX(), e.getY() + 0.5, e.getZ(), stack));
    }

    @SubscribeEvent
    public static void onExperience(LivingExperienceDropEvent event) {
        if (affixOf(event.getEntity()) != null) event.setDroppedExperience(event.getDroppedExperience() * 3);
    }

    /** 試す用のコマンド：/jsnboss elite <モブ> <特性>（例：/jsnboss elite minecraft:zombie fire） */
    @SubscribeEvent
    public static void onCommands(net.neoforged.neoforge.event.RegisterCommandsEvent event) {
        event.getDispatcher().register(net.minecraft.commands.Commands.literal("jsnboss")
                .requires(src -> src.hasPermission(2))
                .then(net.minecraft.commands.Commands.literal("elite")
                        .then(net.minecraft.commands.Commands.argument("entity",
                                        net.minecraft.commands.arguments.ResourceArgument.resource(event.getBuildContext(), net.minecraft.core.registries.Registries.ENTITY_TYPE))
                                .then(net.minecraft.commands.Commands.argument("affix", com.mojang.brigadier.arguments.StringArgumentType.word())
                                        .suggests((ctx, b) -> net.minecraft.commands.SharedSuggestionProvider.suggest(
                                                java.util.Arrays.stream(Affix.values()).map(x -> x.name().toLowerCase()), b))
                                        .executes(ctx -> {
                                            var type = net.minecraft.commands.arguments.ResourceArgument.getSummonableEntityType(ctx, "entity").value();
                                            Affix affix;
                                            try {
                                                affix = Affix.valueOf(com.mojang.brigadier.arguments.StringArgumentType.getString(ctx, "affix").toUpperCase());
                                            } catch (IllegalArgumentException ex) {
                                                ctx.getSource().sendFailure(Component.literal("affix: " + java.util.Arrays.stream(Affix.values()).map(x -> x.name().toLowerCase()).collect(java.util.stream.Collectors.joining(", "))));
                                                return 0;
                                            }
                                            var pos = ctx.getSource().getPosition();
                                            Mob m = spawnElite(ctx.getSource().getLevel(), type, pos.x, pos.y, pos.z, affix);
                                            return m == null ? 0 : 1;
                                        })))));
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
