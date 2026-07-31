package net.oktawia.faststone.blocks.gates;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.oktawia.faststone.entities.gates.LogicAndGateBlockEntity;
import org.jetbrains.annotations.Nullable;

public class LogicAndGateBlock extends LogicGateBlock {

    private static final int CORE_COLOR = 0x6B9FFF;

    public LogicAndGateBlock(BlockBehaviour.Properties properties) {
        super(properties, CORE_COLOR);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new LogicAndGateBlockEntity(pos, state);
    }
}