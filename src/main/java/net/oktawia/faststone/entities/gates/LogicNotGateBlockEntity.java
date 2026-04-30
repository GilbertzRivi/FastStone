package net.oktawia.faststone.entities.gates;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.oktawia.faststone.defs.regs.BlockEntityRegistrar;
import net.oktawia.faststone.logic.LogicPortMode;

public class LogicNotGateBlockEntity extends LogicGateBlockEntity {

    public static final String STATE_NONE = "none";
    public static final String STATE_INPUT = "input";
    public static final String STATE_OUTPUT = "output";

    public LogicNotGateBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntityRegistrar.LOGIC_NOT_GATE.get(), pos, state);
    }

    @Override
    protected String getDefaultPortStateId(Direction side) {
        return STATE_NONE;
    }

    @Override
    protected String getNextPortStateId(Direction side, String currentStateId) {
        return switch (currentStateId) {
            case STATE_NONE -> STATE_INPUT;
            case STATE_INPUT -> STATE_OUTPUT;
            case STATE_OUTPUT -> STATE_NONE;
            default -> STATE_NONE;
        };
    }

    @Override
    protected LogicPortMode getNetworkModeForState(Direction side, String stateId) {
        return switch (stateId) {
            case STATE_INPUT -> LogicPortMode.INPUT;
            case STATE_OUTPUT -> LogicPortMode.OUTPUT;
            default -> LogicPortMode.NONE;
        };
    }

    @Override
    protected String normalizePortStateId(Direction side, String stateId) {
        return switch (stateId) {
            case STATE_INPUT, STATE_OUTPUT -> stateId;
            default -> STATE_NONE;
        };
    }

    @Override
    protected void recomputeOutputs() {
        boolean input = getAnyInput();
        boolean output = !input;

        for (Direction direction : Direction.values()) {
            setOutput(direction, output);
        }
    }

    @Override
    protected int getRenderColorForState(Direction side, String stateId) {
        return switch (stateId) {
            case STATE_INPUT -> 0x3377FF;
            case STATE_OUTPUT -> 0xFF3333;
            default -> 0x000000;
        };
    }
}