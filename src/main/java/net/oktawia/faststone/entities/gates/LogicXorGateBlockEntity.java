package net.oktawia.faststone.entities.gates;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.oktawia.faststone.defs.regs.BlockEntityRegistrar;

public class LogicXorGateBlockEntity extends LogicGateBlockEntity {

    public LogicXorGateBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntityRegistrar.LOGIC_XOR_GATE.get(), pos, state);
    }

    @Override
    protected void recomputeOutputs() {
        setAllOutputs(getHighInputCount() % 2 == 1);
    }
}