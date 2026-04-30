package net.oktawia.faststone.blocks.gates;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.oktawia.faststone.entities.gates.LogicNotGateBlockEntity;
import org.jetbrains.annotations.Nullable;

public class LogicNotGateBlock extends LogicGateBlock {

    public LogicNotGateBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new LogicNotGateBlockEntity(pos, state);
    }
}