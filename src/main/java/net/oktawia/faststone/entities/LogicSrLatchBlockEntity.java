package net.oktawia.faststone.entities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.state.BlockState;
import net.oktawia.faststone.defs.LangDefs;
import net.oktawia.faststone.defs.regs.BlockEntityRegistrar;
import net.oktawia.faststone.entities.gates.LogicGateBlockEntity;
import net.oktawia.faststone.logic.LogicPortMode;

public class LogicSrLatchBlockEntity extends LogicGateBlockEntity {

    public static final String STATE_NONE = "none";
    public static final String STATE_SET = "set";
    public static final String STATE_RESET = "reset";
    public static final String STATE_Q = "q";
    public static final String STATE_NOT_Q = "not_q";

    private boolean q = false;

    public LogicSrLatchBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntityRegistrar.LOGIC_SR_LATCH.get(), pos, state);
    }

    @Override
    protected String getDefaultPortStateId(Direction side) {
        return STATE_NONE;
    }

    @Override
    protected String getNextPortStateId(Direction side, String currentStateId) {
        return switch (currentStateId) {
            case STATE_NONE -> STATE_SET;
            case STATE_SET -> STATE_RESET;
            case STATE_RESET -> STATE_Q;
            case STATE_Q -> STATE_NOT_Q;
            case STATE_NOT_Q -> STATE_NONE;
            default -> STATE_NONE;
        };
    }

    @Override
    protected LogicPortMode getNetworkModeForState(Direction side, String stateId) {
        return switch (stateId) {
            case STATE_SET, STATE_RESET -> LogicPortMode.INPUT;
            case STATE_Q, STATE_NOT_Q -> LogicPortMode.OUTPUT;
            default -> LogicPortMode.NONE;
        };
    }

    @Override
    protected int getRenderColorForState(Direction side, String stateId) {
        return switch (stateId) {
            case STATE_SET -> 0x33FF55;
            case STATE_RESET -> 0xFF9933;
            case STATE_Q -> 0xFF3333;
            case STATE_NOT_Q -> 0xAA55FF;
            default -> 0x000000;
        };
    }

    @Override
    protected String getTranslationKeyForState(Direction side, String stateId) {
        return switch (stateId) {
            case STATE_SET -> LangDefs.GATE_PORT_SET.getTranslationKey();
            case STATE_RESET -> LangDefs.GATE_PORT_RESET.getTranslationKey();
            case STATE_Q -> LangDefs.GATE_PORT_Q.getTranslationKey();
            case STATE_NOT_Q -> LangDefs.GATE_PORT_NOT_Q.getTranslationKey();
            default -> LangDefs.GATE_PORT_NONE.getTranslationKey();
        };
    }

    @Override
    protected String normalizePortStateId(Direction side, String stateId) {
        return switch (stateId) {
            case STATE_SET, STATE_RESET, STATE_Q, STATE_NOT_Q -> stateId;
            default -> STATE_NONE;
        };
    }

    @Override
    protected void recomputeOutputs() {
        boolean set = getOrInputState(STATE_SET);
        boolean reset = getOrInputState(STATE_RESET);

        if (set && reset) {
            setOutputsForState(STATE_Q, false);
            setOutputsForState(STATE_NOT_Q, false);
            return;
        }

        if (set) {
            q = true;
        } else if (reset) {
            q = false;
        }

        setOutputsForState(STATE_Q, q);
        setOutputsForState(STATE_NOT_Q, !q);
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putBoolean("Q", q);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        q = tag.getBoolean("Q");
        recomputeOutputs();
    }
}