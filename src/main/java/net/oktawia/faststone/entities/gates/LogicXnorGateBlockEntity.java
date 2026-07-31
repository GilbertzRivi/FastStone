package net.oktawia.faststone.entities.gates;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.oktawia.faststone.defs.regs.BlockEntityRegistrar;

public class LogicXnorGateBlockEntity extends LogicGateBlockEntity {

    public LogicXnorGateBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntityRegistrar.LOGIC_XNOR_GATE.get(), pos, state);
    }

    @Override
    protected void recomputeOutputs() {
        setAllOutputs(getInputPortCount() > 0 && getHighInputCount() % 2 == 0);
    }
}