package com.chigusa.jsnboss.registry;

import com.chigusa.jsnboss.BossConstants;
import com.chigusa.jsnboss.block.FieldLayerBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(BossConstants.MODID);

    /** ユグドラシルの精霊の「床の層」（アイテムはなし） */
    public static final DeferredBlock<FieldLayerBlock> FIELD_LAYER = BLOCKS.register("field_layer",
            () -> new FieldLayerBlock(BlockBehaviour.Properties.of()
                    .noCollission().noOcclusion().noLootTable()
                    .strength(-1.0F, 3600000.0F)
                    .lightLevel(FieldLayerBlock::light)
                    .sound(SoundType.AMETHYST)
                    .pushReaction(PushReaction.DESTROY)
                    .replaceable()));

    private ModBlocks() {}
}
