package com.chigusa.jsnboss.block;

import com.chigusa.jsnboss.entity.AbstractBoss;
import com.chigusa.jsnboss.entity.YggdrasilSpiritEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * ユグドラシルの精霊が地面の上に敷く、薄い「床の層」。元の地形にはさわらない（空いているところにだけ置く）。
 * warn=true は攻撃の予告（光る）。warn=false は攻撃のあとに残る属性の床。
 * 消すのは精霊の仕事。精霊がいないのに残っていたら（サーバーが落ちたときなど）、ランダムtickで勝手に消える。
 * 掘っても何も落ちない。
 */
public class FieldLayerBlock extends Block {

    public enum Kind implements StringRepresentable {
        FIRE, ICE, WIND, THUNDER, EARTH, DARK, LIGHT, WOOD, SANCTUARY;

        @Override
        public String getSerializedName() {
            return name().toLowerCase(java.util.Locale.ROOT);
        }
    }

    public static final EnumProperty<Kind> KIND = EnumProperty.create("kind", Kind.class);
    public static final BooleanProperty WARN = BooleanProperty.create("warn");
    private static final VoxelShape SHAPE = Block.box(0, 0, 0, 16, 1, 16);

    public FieldLayerBlock(Properties props) {
        super(props);
        registerDefaultState(this.stateDefinition.any().setValue(KIND, Kind.FIRE).setValue(WARN, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(KIND, WARN);
    }

    public static int light(BlockState s) {
        if (s.getValue(WARN)) return 11;
        return s.getValue(KIND) == Kind.SANCTUARY ? 12 : 7;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return SHAPE;
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return Shapes.empty();
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), Direction.UP);
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction dir, BlockState neighbor, LevelAccessor level,
                                     BlockPos pos, BlockPos neighborPos) {
        return dir == Direction.DOWN && !canSurvive(state, level, pos)
                ? net.minecraft.world.level.block.Blocks.AIR.defaultBlockState()
                : super.updateShape(state, dir, neighbor, level, pos, neighborPos);
    }

    /** 精霊が近くにいなければ消える（置きっぱなし防止） */
    @Override
    protected boolean isRandomlyTicking(BlockState state) {
        return true;
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (level.getEntitiesOfClass(YggdrasilSpiritEntity.class, new AABB(pos).inflate(64)).isEmpty()) {
            level.removeBlock(pos, false);
        }
    }

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (level.isClientSide || state.getValue(WARN) || !(entity instanceof LivingEntity e)) return;
        if (e instanceof AbstractBoss || e instanceof com.chigusa.jsnboss.entity.SpiritFairyEntity || e.isSpectator()) return;
        if (e instanceof Player p && p.isCreative()) return;
        if (e.tickCount % 10 != 0) return;
        ServerLevel sl = (ServerLevel) level;
        switch (state.getValue(KIND)) {
            case FIRE -> e.igniteForSeconds(3.0F);                                       // マグマの床：燃える
            case ICE -> {                                                                // 凍った床：遅くなる
                e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, 1, false, false));
                e.setTicksFrozen(Math.min(e.getTicksRequiredToFreeze() + 20, e.getTicksFrozen() + 25));
            }
            case THUNDER -> {                                                            // 帯電した床：ときどき感電
                if (e.getRandom().nextInt(3) == 0 && e.hurt(sl.damageSources().lightningBolt(), 4.0F)) {
                    sl.sendParticles(ParticleTypes.ELECTRIC_SPARK, e.getX(), e.getY() + 0.5, e.getZ(), 12, 0.3, 0.5, 0.3, 0.2);
                }
            }
            case EARTH -> e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, 0, false, false)); // ひび割れ
            case DARK -> e.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 50, 0, false, false));          // 闇の霧
            case WOOD -> {                                                               // 花の床：踏むと散る
                level.removeBlock(pos, false);
                sl.sendParticles(ParticleTypes.CHERRY_LEAVES, pos.getX() + 0.5, pos.getY() + 0.2, pos.getZ() + 0.5, 4, 0.3, 0.1, 0.3, 0.02);
            }
            default -> {}
        }
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(state.getValue(WARN) ? 3 : 8) != 0) return;
        double x = pos.getX() + random.nextDouble(), y = pos.getY() + 0.1, z = pos.getZ() + random.nextDouble();
        if (state.getValue(WARN)) {
            level.addParticle(ParticleTypes.END_ROD, x, y, z, 0, 0.04, 0);
            return;
        }
        switch (state.getValue(KIND)) {
            case FIRE -> level.addParticle(random.nextBoolean() ? ParticleTypes.FLAME : ParticleTypes.LAVA, x, y, z, 0, 0.02, 0);
            case ICE -> level.addParticle(ParticleTypes.SNOWFLAKE, x, y, z, 0, 0.02, 0);
            case THUNDER -> level.addParticle(ParticleTypes.ELECTRIC_SPARK, x, y, z, 0, 0.05, 0);
            case EARTH -> level.addParticle(ParticleTypes.WHITE_ASH, x, y, z, 0, 0.02, 0);
            case DARK -> level.addParticle(ParticleTypes.SQUID_INK, x, y, z, 0, 0.01, 0);
            case WOOD -> level.addParticle(ParticleTypes.CHERRY_LEAVES, x, y + 0.3, z, 0, 0, 0);
            case SANCTUARY -> level.addParticle(ParticleTypes.END_ROD, x, y, z, 0, 0.05, 0);
            default -> {}
        }
    }
}
