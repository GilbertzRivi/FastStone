package net.oktawia.faststone.blocks.gates;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.oktawia.faststone.entities.gates.LogicXorGateBlockEntity;
import org.jetbrains.annotations.Nullable;

public class LogicXorGateBlock extends LogicGateBlock {

    private static final int CORE_COLOR = 0x2DFF6B;

    public LogicXorGateBlock(BlockBehaviour.Properties properties) {
        super(properties, CORE_COLOR);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new LogicXorGateBlockEntity(pos, state);
    }
}