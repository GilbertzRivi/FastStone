package net.oktawia.faststone.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.oktawia.faststone.blocks.gates.LogicGateBlock;
import net.oktawia.faststone.entities.LogicBufferBlockEntity;
import org.jetbrains.annotations.Nullable;

public class LogicBufferBlock extends LogicGateBlock {

    private static final int CORE_COLOR = 0x7FFFFF;

    public LogicBufferBlock(BlockBehaviour.Properties properties) {
        super(properties, CORE_COLOR);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new LogicBufferBlockEntity(pos, state);
    }
}