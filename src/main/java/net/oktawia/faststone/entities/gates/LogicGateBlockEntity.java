package net.oktawia.faststone.entities.gates;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.oktawia.faststone.entities.LogicEndpointBlockEntity;
import net.oktawia.faststone.logic.LogicCableColor;
import net.oktawia.faststone.logic.LogicPortMode;
import net.oktawia.faststone.logic.interfaces.LogicNetworkPort;
import net.oktawia.faststone.logic.network.LogicNetworkGraph;

import java.util.EnumMap;

public abstract class LogicGateBlockEntity extends LogicEndpointBlockEntity implements LogicNetworkPort {

    private final EnumMap<Direction, String> portStates = new EnumMap<>(Direction.class);
    private final EnumMap<Direction, LogicCableColor> portColors = new EnumMap<>(Direction.class);

    private final boolean[] inputAccumulated = new boolean[Direction.values().length];
    private final boolean[] inputValues = new boolean[Direction.values().length];
    private final boolean[] outputValues = new boolean[Direction.values().length];
    private final long[] inputLastNetworkTick = new long[Direction.values().length];

    public LogicGateBlockEntity(
            BlockEntityType<?> type,
            BlockPos pos,
            BlockState state
    ) {
        super(type, pos, state);

        for (Direction direction : Direction.values()) {
            portStates.put(direction, getDefaultPortStateId(direction));
            portColors.put(direction, LogicCableColor.RED);
            inputLastNetworkTick[direction.ordinal()] = -1;
        }
    }

    protected abstract String getDefaultPortStateId(Direction side);

    protected abstract String getNextPortStateId(Direction side, String currentStateId);

    protected abstract LogicPortMode getNetworkModeForState(Direction side, String stateId);

    protected abstract void recomputeOutputs();

    protected String normalizePortStateId(Direction side, String stateId) {
        if (stateId == null || stateId.isBlank()) {
            return getDefaultPortStateId(side);
        }

        return stateId;
    }

    public String getPortStateId(Direction side) {
        return portStates.getOrDefault(side, getDefaultPortStateId(side));
    }

    public LogicCableColor getPortColor(Direction side) {
        return portColors.getOrDefault(side, LogicCableColor.RED);
    }

    public void setPortColor(Direction side, LogicCableColor color) {
        if (getPortColor(side) == color) {
            return;
        }

        portColors.put(side, color);
        setChanged();
        syncToClient();
    }

    public boolean hasVisiblePort(Direction side) {
        return getLogicPortMode(side) != LogicPortMode.NONE;
    }

    public boolean canCableConnect(Direction side, LogicCableColor cableColor) {
        if (getLogicPortMode(side) == LogicPortMode.NONE) {
            return false;
        }

        setPortColor(side, cableColor);
        return true;
    }

    public String cyclePortState(Direction side) {
        String oldStateId = getPortStateId(side);
        String newStateId = normalizePortStateId(
                side,
                getNextPortStateId(side, oldStateId)
        );

        portStates.put(side, newStateId);

        int index = side.ordinal();
        inputAccumulated[index] = false;
        inputValues[index] = false;
        outputValues[index] = false;
        inputLastNetworkTick[index] = -1;

        recomputeOutputs();

        setChanged();
        syncToClient();

        if (level != null && !level.isClientSide) {
            LogicNetworkGraph.scheduleRebuildAround(level, worldPosition);
        }

        return newStateId;
    }

    @Override
    public LogicPortMode getLogicPortMode(Direction side) {
        return getNetworkModeForState(side, getPortStateId(side));
    }

    @Override
    public boolean readLogicOutput(Direction side) {
        if (!getLogicPortMode(side).writesToCable()) {
            return false;
        }

        return outputValues[side.ordinal()];
    }

    @Override
    public void receiveLogicInput(Direction side, boolean value, long networkTickId) {
        if (!getLogicPortMode(side).readsFromCable()) {
            return;
        }

        int index = side.ordinal();

        if (inputLastNetworkTick[index] != networkTickId) {
            inputLastNetworkTick[index] = networkTickId;
            inputAccumulated[index] = false;
        }

        inputAccumulated[index] |= value;
        inputValues[index] = inputAccumulated[index];

        recomputeOutputs();
        setChanged();
    }

    protected boolean getInput(Direction side) {
        return getLogicPortMode(side).readsFromCable()
                && inputValues[side.ordinal()];
    }

    protected boolean getAnyInput() {
        boolean value = false;

        for (Direction direction : Direction.values()) {
            value |= getInput(direction);
        }

        return value;
    }

    protected void setOutput(Direction side, boolean value) {
        if (!getLogicPortMode(side).writesToCable()) {
            outputValues[side.ordinal()] = false;
            return;
        }

        outputValues[side.ordinal()] = value;
    }

    protected void setAllOutputs(boolean value) {
        for (Direction direction : Direction.values()) {
            setOutput(direction, value);
        }
    }

    public void onGateNeighborChanged(Direction side) {
        if (!getLogicPortMode(side).readsFromCable()) {
            return;
        }

        if (level == null) {
            return;
        }

        BlockState neighborState = level.getBlockState(worldPosition.relative(side));

        if (!(neighborState.getBlock() instanceof net.oktawia.faststone.blocks.LogicCableBlock)) {
            inputValues[side.ordinal()] = false;
            inputAccumulated[side.ordinal()] = false;
            recomputeOutputs();
            setChanged();
        }
    }

    private void syncToClient() {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }

        BlockState state = getBlockState();
        serverLevel.sendBlockUpdated(worldPosition, state, state, Block.UPDATE_CLIENTS);
    }

    @Override
    public void onLoad() {
        super.onLoad();

        if (level != null && !level.isClientSide) {
            LogicNetworkGraph.scheduleRebuildAround(level, worldPosition);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);

        CompoundTag statesTag = new CompoundTag();
        CompoundTag colorsTag = new CompoundTag();
        CompoundTag inputsTag = new CompoundTag();
        CompoundTag outputsTag = new CompoundTag();

        for (Direction direction : Direction.values()) {
            String name = direction.getName();

            statesTag.putString(name, getPortStateId(direction));
            colorsTag.putString(name, getPortColor(direction).getSerializedName());

            inputsTag.putBoolean(name, inputValues[direction.ordinal()]);
            outputsTag.putBoolean(name, outputValues[direction.ordinal()]);
        }

        tag.put("PortStates", statesTag);
        tag.put("PortColors", colorsTag);
        tag.put("Inputs", inputsTag);
        tag.put("Outputs", outputsTag);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);

        if (tag.contains("PortStates")) {
            CompoundTag statesTag = tag.getCompound("PortStates");

            for (Direction direction : Direction.values()) {
                portStates.put(
                        direction,
                        normalizePortStateId(direction, statesTag.getString(direction.getName()))
                );
            }
        }

        if (tag.contains("PortColors")) {
            CompoundTag colorsTag = tag.getCompound("PortColors");

            for (Direction direction : Direction.values()) {
                portColors.put(
                        direction,
                        LogicCableColor.byName(colorsTag.getString(direction.getName()))
                );
            }
        }

        if (tag.contains("Inputs")) {
            CompoundTag inputsTag = tag.getCompound("Inputs");

            for (Direction direction : Direction.values()) {
                inputValues[direction.ordinal()] = inputsTag.getBoolean(direction.getName());
            }
        }

        if (tag.contains("Outputs")) {
            CompoundTag outputsTag = tag.getCompound("Outputs");

            for (Direction direction : Direction.values()) {
                outputValues[direction.ordinal()] = outputsTag.getBoolean(direction.getName());
            }
        }

        for (Direction direction : Direction.values()) {
            inputAccumulated[direction.ordinal()] = false;
            inputLastNetworkTick[direction.ordinal()] = -1;
        }

        recomputeOutputs();
    }

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = super.getUpdateTag();
        saveClientData(tag);
        return tag;
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void onDataPacket(Connection connection, ClientboundBlockEntityDataPacket packet) {
        CompoundTag tag = packet.getTag();

        if (tag != null) {
            loadClientData(tag);
        }
    }

    @Override
    public void handleUpdateTag(CompoundTag tag) {
        loadClientData(tag);
    }

    private void saveClientData(CompoundTag tag) {
        CompoundTag statesTag = new CompoundTag();
        CompoundTag colorsTag = new CompoundTag();

        for (Direction direction : Direction.values()) {
            statesTag.putString(direction.getName(), getPortStateId(direction));
            colorsTag.putString(direction.getName(), getPortColor(direction).getSerializedName());
        }

        tag.put("PortStates", statesTag);
        tag.put("PortColors", colorsTag);
    }

    private void loadClientData(CompoundTag tag) {
        if (tag.contains("PortStates")) {
            CompoundTag statesTag = tag.getCompound("PortStates");

            for (Direction direction : Direction.values()) {
                portStates.put(
                        direction,
                        normalizePortStateId(direction, statesTag.getString(direction.getName()))
                );
            }
        }

        if (tag.contains("PortColors")) {
            CompoundTag colorsTag = tag.getCompound("PortColors");

            for (Direction direction : Direction.values()) {
                portColors.put(
                        direction,
                        LogicCableColor.byName(colorsTag.getString(direction.getName()))
                );
            }
        }
    }

    protected abstract int getRenderColorForState(Direction side, String stateId);

    public int getPortRenderColor(Direction side) {
        return getRenderColorForState(side, getPortStateId(side));
    }
}