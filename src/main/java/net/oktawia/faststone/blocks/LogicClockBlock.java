package net.oktawia.faststone.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.oktawia.faststone.blocks.gates.LogicGateBlock;
import net.oktawia.faststone.entities.LogicClockBlockEntity;
import org.jetbrains.annotations.Nullable;

public class LogicClockBlock extends LogicGateBlock {

    private static final int CORE_COLOR = 0x303030;

    public LogicClockBlock(BlockBehaviour.Properties properties) {
        super(properties, CORE_COLOR);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new LogicClockBlockEntity(pos, state);
    }
}