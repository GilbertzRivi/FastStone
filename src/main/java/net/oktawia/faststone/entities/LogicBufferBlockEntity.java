package net.oktawia.faststone.entities;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.state.BlockState;
import net.oktawia.faststone.defs.LangDefs;
import net.oktawia.faststone.defs.regs.BlockEntityRegistrar;
import net.oktawia.faststone.entities.gates.LogicGateBlockEntity;
import net.oktawia.faststone.logic.interfaces.LogicGateConfigurable;

import java.util.ArrayDeque;

public class LogicBufferBlockEntity extends LogicGateBlockEntity implements LogicGateConfigurable {

    private static final class ScheduledValue {
        private final long tick;
        private final boolean value;

        private ScheduledValue(long tick, boolean value) {
            this.tick = tick;
            this.value = value;
        }
    }

    private int delayTicks = 1;

    private boolean currentOutput = false;

    private long currentInputTick = Long.MIN_VALUE;
    private long enqueuedInputTick = Long.MIN_VALUE;
    private boolean currentInputValue = false;

    private final ArrayDeque<ScheduledValue> scheduledValues = new ArrayDeque<>();

    public LogicBufferBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntityRegistrar.LOGIC_BUFFER.get(), pos, state);
    }

    @Override
    public void beforeLogicNetworkTick(long networkTickId) {
        finalizeInputBefore(networkTickId);
        processDueValues(networkTickId);
        recomputeOutputs();
    }

    @Override
    protected void beforeReceiveLogicInput(
            net.minecraft.core.Direction side,
            boolean value,
            long networkTickId
    ) {
        if (currentInputTick != networkTickId) {
            finalizeInputBefore(networkTickId);

            currentInputTick = networkTickId;
            currentInputValue = false;
        }
    }

    @Override
    protected void afterReceiveLogicInput(
            net.minecraft.core.Direction side,
            boolean value,
            long networkTickId
    ) {
        currentInputValue = getOrInputState(STATE_INPUT);
        processDueValues(networkTickId);
        recomputeOutputs();
    }

    @Override
    protected void recomputeOutputs() {
        setOutputsForState(STATE_OUTPUT, currentOutput);
    }

    private void finalizeInputBefore(long networkTickId) {
        if (currentInputTick == Long.MIN_VALUE) {
            return;
        }

        if (currentInputTick >= networkTickId) {
            return;
        }

        if (enqueuedInputTick == currentInputTick) {
            return;
        }

        long outputTick = currentInputTick + delayTicks;
        scheduledValues.addLast(new ScheduledValue(outputTick, currentInputValue));
        enqueuedInputTick = currentInputTick;
    }

    private void processDueValues(long networkTickId) {
        while (!scheduledValues.isEmpty() && scheduledValues.peekFirst().tick <= networkTickId) {
            currentOutput = scheduledValues.removeFirst().value;
        }
    }

    @Override
    public BlockPos getConfigBlockPos() {
        return worldPosition;
    }

    @Override
    public String getConfigTitleTranslationKey() {
        return LangDefs.SCREEN_LOGIC_BUFFER_TITLE.getTranslationKey();
    }

    @Override
    public String getConfigLabelTranslationKey() {
        return LangDefs.SCREEN_LOGIC_BUFFER_DELAY.getTranslationKey();
    }

    @Override
    public int getConfigValue() {
        return delayTicks;
    }

    @Override
    public void setConfigValue(int value) {
        delayTicks = Math.max(1, Math.min(4096, value));
        scheduledValues.clear();

        currentInputTick = Long.MIN_VALUE;
        enqueuedInputTick = Long.MIN_VALUE;
        currentInputValue = false;

        setChanged();
        syncToClient();
    }

    @Override
    public int getMinConfigValue() {
        return 1;
    }

    @Override
    public int getMaxConfigValue() {
        return 4096;
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);

        tag.putInt("DelayTicks", delayTicks);
        tag.putBoolean("CurrentOutput", currentOutput);
        tag.putLong("CurrentInputTick", currentInputTick);
        tag.putLong("EnqueuedInputTick", enqueuedInputTick);
        tag.putBoolean("CurrentInputValue", currentInputValue);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);

        delayTicks = Math.max(1, tag.getInt("DelayTicks"));
        currentOutput = tag.getBoolean("CurrentOutput");
        currentInputTick = tag.getLong("CurrentInputTick");
        enqueuedInputTick = tag.getLong("EnqueuedInputTick");
        currentInputValue = tag.getBoolean("CurrentInputValue");

        scheduledValues.clear();
        recomputeOutputs();
    }

    @Override
    protected void saveClientExtra(CompoundTag tag) {
        tag.putInt("DelayTicks", delayTicks);
    }

    @Override
    protected void loadClientExtra(CompoundTag tag) {
        if (tag.contains("DelayTicks")) {
            delayTicks = Math.max(1, tag.getInt("DelayTicks"));
        }
    }
}