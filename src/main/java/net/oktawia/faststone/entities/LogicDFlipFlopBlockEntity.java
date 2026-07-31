package net.oktawia.faststone.entities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.state.BlockState;
import net.oktawia.faststone.defs.LangDefs;
import net.oktawia.faststone.defs.regs.BlockEntityRegistrar;
import net.oktawia.faststone.entities.gates.LogicGateBlockEntity;
import net.oktawia.faststone.logic.LogicPortMode;

public class LogicDFlipFlopBlockEntity extends LogicGateBlockEntity {

    public static final String STATE_NONE = "none";
    public static final String STATE_D = "d";
    public static final String STATE_CLOCK = "clock";
    public static final String STATE_Q = "q";
    public static final String STATE_NOT_Q = "not_q";

    private boolean q = false;
    private boolean lastClock = false;
    private long observedNetworkTick = Long.MIN_VALUE;

    public LogicDFlipFlopBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntityRegistrar.LOGIC_D_FLIP_FLOP.get(), pos, state);
    }

    @Override
    protected String getDefaultPortStateId(Direction side) {
        return STATE_NONE;
    }

    @Override
    protected String getNextPortStateId(Direction side, String currentStateId) {
        return switch (currentStateId) {
            case STATE_NONE -> STATE_D;
            case STATE_D -> STATE_CLOCK;
            case STATE_CLOCK -> STATE_Q;
            case STATE_Q -> STATE_NOT_Q;
            case STATE_NOT_Q -> STATE_NONE;
            default -> STATE_NONE;
        };
    }

    @Override
    protected LogicPortMode getNetworkModeForState(Direction side, String stateId) {
        return switch (stateId) {
            case STATE_D, STATE_CLOCK -> LogicPortMode.INPUT;
            case STATE_Q, STATE_NOT_Q -> LogicPortMode.OUTPUT;
            default -> LogicPortMode.NONE;
        };
    }

    @Override
    protected int getRenderColorForState(Direction side, String stateId) {
        return switch (stateId) {
            case STATE_D -> 0x3377FF;
            case STATE_CLOCK -> 0xFFFF33;
            case STATE_Q -> 0xFF3333;
            case STATE_NOT_Q -> 0xAA55FF;
            default -> 0x000000;
        };
    }

    @Override
    protected String getTranslationKeyForState(Direction side, String stateId) {
        return switch (stateId) {
            case STATE_D -> LangDefs.GATE_PORT_D.getTranslationKey();
            case STATE_CLOCK -> LangDefs.GATE_PORT_CLOCK.getTranslationKey();
            case STATE_Q -> LangDefs.GATE_PORT_Q.getTranslationKey();
            case STATE_NOT_Q -> LangDefs.GATE_PORT_NOT_Q.getTranslationKey();
            default -> LangDefs.GATE_PORT_NONE.getTranslationKey();
        };
    }

    @Override
    protected String normalizePortStateId(Direction side, String stateId) {
        return switch (stateId) {
            case STATE_D, STATE_CLOCK, STATE_Q, STATE_NOT_Q -> stateId;
            default -> STATE_NONE;
        };
    }

    @Override
    protected void beforeReceiveLogicInput(Direction side, boolean value, long networkTickId) {
        if (observedNetworkTick == networkTickId) {
            return;
        }

        if (observedNetworkTick != Long.MIN_VALUE) {
            samplePreviousTickInputs();
        }

        observedNetworkTick = networkTickId;
    }

    @Override
    protected void afterReceiveLogicInput(Direction side, boolean value, long networkTickId) {
        updateOutputPorts();
    }

    @Override
    protected void recomputeOutputs() {
        updateOutputPorts();
    }

    private void samplePreviousTickInputs() {
        boolean clock = getOrInputState(STATE_CLOCK);
        boolean data = getOrInputState(STATE_D);

        if (clock && !lastClock) {
            q = data;
        }

        lastClock = clock;
    }

    private void updateOutputPorts() {
        setOutputsForState(STATE_Q, q);
        setOutputsForState(STATE_NOT_Q, !q);
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putBoolean("Q", q);
        tag.putBoolean("LastClock", lastClock);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        q = tag.getBoolean("Q");
        lastClock = tag.getBoolean("LastClock");
        observedNetworkTick = Long.MIN_VALUE;
        updateOutputPorts();
    }
}