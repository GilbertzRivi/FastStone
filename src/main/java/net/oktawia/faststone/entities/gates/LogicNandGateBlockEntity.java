package net.oktawia.faststone.entities.gates;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.oktawia.faststone.defs.regs.BlockEntityRegistrar;

public class LogicNandGateBlockEntity extends LogicGateBlockEntity {

    public LogicNandGateBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntityRegistrar.LOGIC_NAND_GATE.get(), pos, state);
    }

    @Override
    protected void recomputeOutputs() {
        setAllOutputs(!areAllInputsHigh());
    }
}