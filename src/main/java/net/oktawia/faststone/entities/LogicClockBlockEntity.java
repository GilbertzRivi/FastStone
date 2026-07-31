package net.oktawia.faststone.entities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.state.BlockState;
import net.oktawia.faststone.defs.LangDefs;
import net.oktawia.faststone.defs.regs.BlockEntityRegistrar;
import net.oktawia.faststone.entities.gates.LogicGateBlockEntity;
import net.oktawia.faststone.logic.LogicPortMode;
import net.oktawia.faststone.logic.interfaces.LogicGateConfigurable;

public class LogicClockBlockEntity extends LogicGateBlockEntity implements LogicGateConfigurable {

    public static final String STATE_NONE = "none";
    public static final String STATE_CLOCK_A = "clock_a";
    public static final String STATE_CLOCK_B = "clock_b";

    private int intervalTicks = 1;
    private boolean phase = true;
    private long lastSeenNetworkTick = Long.MIN_VALUE;
    private long lastToggleNetworkTick = Long.MIN_VALUE;

    public LogicClockBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntityRegistrar.LOGIC_CLOCK.get(), pos, state);
    }

    @Override
    protected String getDefaultPortStateId(Direction side) {
        return STATE_NONE;
    }

    @Override
    protected String getNextPortStateId(Direction side, String currentStateId) {
        return switch (currentStateId) {
            case STATE_NONE -> STATE_CLOCK_A;
            case STATE_CLOCK_A -> STATE_CLOCK_B;
            case STATE_CLOCK_B -> STATE_NONE;
            default -> STATE_NONE;
        };
    }

    @Override
    protected LogicPortMode getNetworkModeForState(Direction side, String stateId) {
        return switch (stateId) {
            case STATE_CLOCK_A, STATE_CLOCK_B -> LogicPortMode.OUTPUT;
            default -> LogicPortMode.NONE;
        };
    }

    @Override
    protected int getRenderColorForState(Direction side, String stateId) {
        return switch (stateId) {
            case STATE_CLOCK_A -> 0xFF3333;
            case STATE_CLOCK_B -> 0x3377FF;
            default -> 0x000000;
        };
    }

    @Override
    protected String getTranslationKeyForState(Direction side, String stateId) {
        return switch (stateId) {
            case STATE_CLOCK_A -> LangDefs.GATE_PORT_CLOCK_A.getTranslationKey();
            case STATE_CLOCK_B -> LangDefs.GATE_PORT_CLOCK_B.getTranslationKey();
            default -> LangDefs.GATE_PORT_NONE.getTranslationKey();
        };
    }

    @Override
    protected String normalizePortStateId(Direction side, String stateId) {
        return switch (stateId) {
            case STATE_CLOCK_A, STATE_CLOCK_B -> stateId;
            default -> STATE_NONE;
        };
    }

    @Override
    public void beforeLogicNetworkTick(long networkTickId) {
        if (lastSeenNetworkTick == networkTickId) {
            return;
        }

        if (lastToggleNetworkTick == Long.MIN_VALUE || lastToggleNetworkTick > networkTickId) {
            lastToggleNetworkTick = networkTickId;
        } else if (networkTickId - lastToggleNetworkTick >= intervalTicks) {
            long elapsedIntervals = (networkTickId - lastToggleNetworkTick) / intervalTicks;

            if ((elapsedIntervals & 1L) == 1L) {
                phase = !phase;
            }

            lastToggleNetworkTick += elapsedIntervals * intervalTicks;
        }

        lastSeenNetworkTick = networkTickId;
        recomputeOutputs();
        setChanged();
    }

    @Override
    protected void recomputeOutputs() {
        setOutputsForState(STATE_CLOCK_A, phase);
        setOutputsForState(STATE_CLOCK_B, !phase);
    }

    @Override
    public BlockPos getConfigBlockPos() {
        return worldPosition;
    }

    @Override
    public String getConfigTitleTranslationKey() {
        return LangDefs.SCREEN_LOGIC_CLOCK_TITLE.getTranslationKey();
    }

    @Override
    public String getConfigLabelTranslationKey() {
        return LangDefs.SCREEN_LOGIC_CLOCK_INTERVAL.getTranslationKey();
    }

    @Override
    public int getConfigValue() {
        return intervalTicks;
    }

    @Override
    public void setConfigValue(int value) {
        intervalTicks = Math.max(1, Math.min(1024, value));
        lastToggleNetworkTick = Long.MIN_VALUE;
        setChanged();
        syncToClient();
    }

    @Override
    public int getMinConfigValue() {
        return 1;
    }

    @Override
    public int getMaxConfigValue() {
        return 1024;
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);

        tag.putInt("IntervalTicks", intervalTicks);
        tag.putBoolean("Phase", phase);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);

        intervalTicks = Math.max(1, Math.min(1024, tag.getInt("IntervalTicks")));
        phase = tag.contains("Phase") ? tag.getBoolean("Phase") : true;

        lastSeenNetworkTick = Long.MIN_VALUE;
        lastToggleNetworkTick = Long.MIN_VALUE;

        recomputeOutputs();
    }

    @Override
    protected void saveClientExtra(CompoundTag tag) {
        tag.putInt("IntervalTicks", intervalTicks);
    }

    @Override
    protected void loadClientExtra(CompoundTag tag) {
        if (tag.contains("IntervalTicks")) {
            intervalTicks = Math.max(1, Math.min(1024, tag.getInt("IntervalTicks")));
        }
    }
}