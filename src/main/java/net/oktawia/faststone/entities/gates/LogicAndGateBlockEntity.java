package net.oktawia.faststone.entities.gates;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.oktawia.faststone.defs.regs.BlockEntityRegistrar;

public class LogicAndGateBlockEntity extends LogicGateBlockEntity {

    public LogicAndGateBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntityRegistrar.LOGIC_AND_GATE.get(), pos, state);
    }

    @Override
    protected void recomputeOutputs() {
        setAllOutputs(areAllInputsHigh());
    }
}