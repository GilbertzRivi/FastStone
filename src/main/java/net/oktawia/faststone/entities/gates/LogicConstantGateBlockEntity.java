package net.oktawia.faststone.entities.gates;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.oktawia.faststone.defs.regs.BlockEntityRegistrar;
import net.oktawia.faststone.logic.LogicPortMode;

public class LogicConstantGateBlockEntity extends LogicGateBlockEntity {

    public static final String STATE_NONE = "none";
    public static final String STATE_OUTPUT = "output";

    public LogicConstantGateBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntityRegistrar.LOGIC_CONSTANT_GATE.get(), pos, state);
    }

    @Override
    protected String getDefaultPortStateId(Direction side) {
        return STATE_NONE;
    }

    @Override
    protected String getNextPortStateId(Direction side, String currentStateId) {
        return switch (currentStateId) {
            case STATE_NONE -> STATE_OUTPUT;
            case STATE_OUTPUT -> STATE_NONE;
            default -> STATE_NONE;
        };
    }

    @Override
    protected LogicPortMode getNetworkModeForState(Direction side, String stateId) {
        return switch (stateId) {
            case STATE_OUTPUT -> LogicPortMode.OUTPUT;
            default -> LogicPortMode.NONE;
        };
    }

    @Override
    protected String normalizePortStateId(Direction side, String stateId) {
        return switch (stateId) {
            case STATE_OUTPUT -> STATE_OUTPUT;
            default -> STATE_NONE;
        };
    }

    @Override
    protected void recomputeOutputs() {
        setOutputsForState(STATE_OUTPUT, true);
    }
}