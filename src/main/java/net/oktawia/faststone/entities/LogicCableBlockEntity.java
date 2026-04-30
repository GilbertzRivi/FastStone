package net.oktawia.faststone.entities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.oktawia.faststone.defs.regs.BlockEntityRegistrar;
import net.oktawia.faststone.logic.parts.LogicCablePartType;
import net.oktawia.faststone.logic.network.LogicEndpointRef;
import net.oktawia.faststone.logic.network.LogicNetworkClock;
import net.oktawia.faststone.logic.network.LogicNetworkGraph;
import net.oktawia.faststone.logic.LogicPortMode;
import net.oktawia.faststone.logic.interfaces.LogicNetworkPort;

import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class LogicCableBlockEntity extends BlockEntity implements LogicNetworkPort {

    private static final float VISUAL_SYNC_EPSILON = 0.015F;

    private final EnumMap<Direction, LogicCablePartType> parts = new EnumMap<>(Direction.class);

    private final boolean[] outputAccumulated = new boolean[Direction.values().length];
    private final boolean[] outputDesired = new boolean[Direction.values().length];
    private final boolean[] outputRedstone = new boolean[Direction.values().length];
    private final long[] outputLastNetworkTick = new long[Direction.values().length];

    private boolean master = false;
    private BlockPos masterPos = null;

    private Set<BlockPos> networkCables = Set.of();
    private List<LogicEndpointRef> inputs = List.of();
    private List<LogicEndpointRef> outputs = List.of();

    private boolean currentSignal = false;
    private boolean nextSignal = false;

    private float visualStrength = 0.0F;
    private int visualHighTicks = 0;
    private int visualTotalTicks = 0;

    public LogicCableBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntityRegistrar.LOGIC_CABLE.get(), pos, state);

        for (Direction direction : Direction.values()) {
            parts.put(direction, LogicCablePartType.NONE);
            outputLastNetworkTick[direction.ordinal()] = -1;
        }
    }

    public boolean isMaster() {
        return master;
    }

    public BlockPos getMasterPos() {
        return masterPos;
    }

    public LogicCablePartType getPart(Direction side) {
        return parts.getOrDefault(side, LogicCablePartType.NONE);
    }

    public boolean hasPart(Direction side) {
        return getPart(side) != LogicCablePartType.NONE;
    }

    public boolean setPart(Direction side, LogicCablePartType type) {
        LogicCablePartType old = getPart(side);

        if (old == type) {
            return false;
        }

        if (old == LogicCablePartType.OUTPUT) {
            setRedstoneOutput(side, false);
        }

        parts.put(side, type);

        int index = side.ordinal();
        outputAccumulated[index] = false;
        outputDesired[index] = false;
        outputLastNetworkTick[index] = -1;

        setChanged();
        syncToClient();

        if (this.level != null && !this.level.isClientSide) {
            LogicNetworkGraph.scheduleRebuildAround(this.level, this.worldPosition);
        }

        return true;
    }

    public LogicCablePartType removePart(Direction side) {
        LogicCablePartType old = getPart(side);

        if (old == LogicCablePartType.NONE) {
            return LogicCablePartType.NONE;
        }

        if (old == LogicCablePartType.OUTPUT) {
            setRedstoneOutput(side, false);
        }

        parts.put(side, LogicCablePartType.NONE);

        int index = side.ordinal();
        outputAccumulated[index] = false;
        outputDesired[index] = false;
        outputLastNetworkTick[index] = -1;

        setChanged();
        syncToClient();

        if (this.level != null && !this.level.isClientSide) {
            LogicNetworkGraph.scheduleRebuildAround(this.level, this.worldPosition);
        }

        return old;
    }

    public boolean isRedstoneOutputPowered(Direction physicalSide) {
        return outputRedstone[physicalSide.ordinal()];
    }

    public boolean getSignal() {
        LogicCableBlockEntity master = getMaster();

        if (master == null) {
            return false;
        }

        return master.currentSignal;
    }

    public boolean isVisualSignalHigh() {
        return getVisualSignalStrength() > 0.01F;
    }

    public float getVisualSignalStrength() {
        LogicCableBlockEntity master = getMaster();

        if (master == null) {
            return 0.0F;
        }

        return master.visualStrength;
    }

    public LogicCableBlockEntity getMaster() {
        if (this.level == null) {
            return null;
        }

        if (this.master) {
            return this;
        }

        if (this.masterPos == null) {
            return null;
        }

        BlockEntity be = this.level.getBlockEntity(this.masterPos);

        if (!(be instanceof LogicCableBlockEntity cable)) {
            return null;
        }

        if (!cable.isMaster()) {
            return null;
        }

        return cable;
    }

    public void write(boolean value) {
        LogicCableBlockEntity master = getMaster();

        if (master == null) {
            return;
        }

        if (master != this) {
            master.write(value);
            return;
        }

        this.nextSignal |= value;
    }

    public void becomeMember(BlockPos masterPos) {
        if (this.level instanceof ServerLevel serverLevel && this.master) {
            LogicNetworkClock.unregisterMaster(serverLevel, this.worldPosition);
        }

        this.master = false;
        this.masterPos = masterPos.immutable();

        this.networkCables = Set.of();
        this.inputs = List.of();
        this.outputs = List.of();

        this.nextSignal = false;
        this.visualHighTicks = 0;
        this.visualTotalTicks = 0;

        setChanged();
        syncToClient();
    }

    public void becomeMaster(
            Set<BlockPos> networkCables,
            List<LogicEndpointRef> inputs,
            List<LogicEndpointRef> outputs
    ) {
        this.master = true;
        this.masterPos = this.worldPosition.immutable();

        this.networkCables = Set.copyOf(networkCables);
        this.inputs = List.copyOf(inputs);
        this.outputs = List.copyOf(outputs);

        this.nextSignal = false;
        this.visualHighTicks = 0;
        this.visualTotalTicks = 0;

        if (this.level instanceof ServerLevel serverLevel) {
            LogicNetworkClock.registerMaster(serverLevel, this.worldPosition);
        }

        setChanged();
        syncToClient();
    }

    public void clearNetworkCache() {
        if (this.level instanceof ServerLevel serverLevel && this.master) {
            LogicNetworkClock.unregisterMaster(serverLevel, this.worldPosition);
        }

        this.master = false;
        this.masterPos = null;

        this.networkCables = Set.of();
        this.inputs = List.of();
        this.outputs = List.of();

        this.nextSignal = false;
        this.visualHighTicks = 0;
        this.visualTotalTicks = 0;

        setChanged();
    }

    public void networkEvaluate() {
        if (!this.master || this.level == null) {
            return;
        }

        this.nextSignal = false;

        for (LogicEndpointRef ref : this.inputs) {
            BlockEntity be = this.level.getBlockEntity(ref.pos());

            if (be instanceof LogicNetworkPort port) {
                if (port.readLogicOutput(ref.side())) {
                    write(true);
                }
            }
        }
    }

    public void networkCommit(long networkTickId) {
        if (!this.master || this.level == null) {
            return;
        }

        this.currentSignal = this.nextSignal;

        this.visualTotalTicks++;

        if (this.currentSignal) {
            this.visualHighTicks++;
        }

        for (LogicEndpointRef ref : this.outputs) {
            BlockEntity be = this.level.getBlockEntity(ref.pos());

            if (be instanceof LogicNetworkPort port) {
                port.receiveLogicInput(ref.side(), this.currentSignal, networkTickId);
            }
        }

        setChanged();
    }

    public void networkAfterGameTick(long gameTickId) {
        if (!this.master || this.level == null) {
            return;
        }

        float newVisualStrength = 0.0F;

        if (this.visualTotalTicks > 0) {
            newVisualStrength = (float) this.visualHighTicks / (float) this.visualTotalTicks;
        }

        this.visualHighTicks = 0;
        this.visualTotalTicks = 0;

        if (Math.abs(this.visualStrength - newVisualStrength) > VISUAL_SYNC_EPSILON) {
            this.visualStrength = newVisualStrength;
            setChanged();
            syncToClient();
        }

        Set<BlockPos> touched = new HashSet<>();

        for (LogicEndpointRef ref : this.outputs) {
            if (!touched.add(ref.pos())) {
                continue;
            }

            BlockEntity be = this.level.getBlockEntity(ref.pos());

            if (be instanceof LogicNetworkPort port) {
                port.afterLogicGameTick(gameTickId);
            }
        }
    }

    @Override
    public LogicPortMode getLogicPortMode(Direction side) {
        return switch (getPart(side)) {
            case INPUT -> LogicPortMode.OUTPUT;
            case OUTPUT -> LogicPortMode.INPUT;
            case NONE -> LogicPortMode.NONE;
        };
    }

    @Override
    public boolean readLogicOutput(Direction side) {
        if (this.level == null || getPart(side) != LogicCablePartType.INPUT) {
            return false;
        }

        BlockPos readPos = this.worldPosition.relative(side);

        return this.level.getSignal(readPos, side) > 0;
    }

    @Override
    public void receiveLogicInput(Direction side, boolean value, long networkTickId) {
        if (getPart(side) != LogicCablePartType.OUTPUT) {
            return;
        }

        int index = side.ordinal();

        if (outputLastNetworkTick[index] != networkTickId) {
            outputLastNetworkTick[index] = networkTickId;
            outputAccumulated[index] = false;
        }

        outputAccumulated[index] |= value;
        outputDesired[index] = outputAccumulated[index];
    }

    @Override
    public void afterLogicGameTick(long gameTickId) {
        for (Direction side : Direction.values()) {
            if (getPart(side) != LogicCablePartType.OUTPUT) {
                continue;
            }

            setRedstoneOutput(side, outputDesired[side.ordinal()]);
        }
    }

    private void setRedstoneOutput(Direction side, boolean powered) {
        int index = side.ordinal();

        if (outputRedstone[index] == powered) {
            return;
        }

        outputRedstone[index] = powered;
        setChanged();

        if (this.level == null || this.level.isClientSide) {
            return;
        }

        BlockState state = getBlockState();

        this.level.updateNeighborsAt(this.worldPosition, state.getBlock());
        this.level.updateNeighborsAt(this.worldPosition.relative(side), state.getBlock());
    }

    private void syncToClient() {
        if (!(this.level instanceof ServerLevel serverLevel)) {
            return;
        }

        BlockState state = getBlockState();
        serverLevel.sendBlockUpdated(this.worldPosition, state, state, Block.UPDATE_CLIENTS);
    }

    @Override
    public void onLoad() {
        super.onLoad();

        if (this.level != null && !this.level.isClientSide) {
            LogicNetworkGraph.scheduleRebuildAround(this.level, this.worldPosition);
        }
    }

    @Override
    public void setRemoved() {
        if (this.level instanceof ServerLevel serverLevel && this.master) {
            LogicNetworkClock.unregisterMaster(serverLevel, this.worldPosition);
        }

        super.setRemoved();
    }

    @Override
    public void onChunkUnloaded() {
        if (this.level instanceof ServerLevel serverLevel && this.master) {
            LogicNetworkClock.unregisterMaster(serverLevel, this.worldPosition);
        }

        super.onChunkUnloaded();
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);

        tag.putBoolean("CurrentSignal", this.currentSignal);
        tag.putFloat("VisualStrength", this.visualStrength);

        CompoundTag partsTag = new CompoundTag();
        CompoundTag outputsTag = new CompoundTag();

        for (Direction side : Direction.values()) {
            partsTag.putString(side.getName(), getPart(side).getSerializedName());
            outputsTag.putBoolean(side.getName(), outputRedstone[side.ordinal()]);
        }

        tag.put("Parts", partsTag);
        tag.put("RedstoneOutputs", outputsTag);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);

        this.currentSignal = tag.getBoolean("CurrentSignal");
        this.visualStrength = tag.getFloat("VisualStrength");

        if (tag.contains("Parts")) {
            CompoundTag partsTag = tag.getCompound("Parts");

            for (Direction side : Direction.values()) {
                parts.put(side, LogicCablePartType.byName(partsTag.getString(side.getName())));
            }
        }

        if (tag.contains("RedstoneOutputs")) {
            CompoundTag outputsTag = tag.getCompound("RedstoneOutputs");

            for (Direction side : Direction.values()) {
                outputRedstone[side.ordinal()] = outputsTag.getBoolean(side.getName());
            }
        }

        this.nextSignal = false;
        this.visualHighTicks = 0;
        this.visualTotalTicks = 0;

        for (Direction side : Direction.values()) {
            int index = side.ordinal();
            outputAccumulated[index] = false;
            outputDesired[index] = outputRedstone[index];
            outputLastNetworkTick[index] = -1;
        }
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
        tag.putBoolean("Master", this.master);
        tag.putFloat("VisualStrength", this.visualStrength);

        if (this.masterPos != null) {
            tag.putBoolean("HasMasterPos", true);
            tag.putLong("MasterPos", this.masterPos.asLong());
        } else {
            tag.putBoolean("HasMasterPos", false);
        }

        CompoundTag partsTag = new CompoundTag();

        for (Direction side : Direction.values()) {
            partsTag.putString(side.getName(), getPart(side).getSerializedName());
        }

        tag.put("Parts", partsTag);
    }

    private void loadClientData(CompoundTag tag) {
        this.master = tag.getBoolean("Master");
        this.visualStrength = tag.getFloat("VisualStrength");

        if (tag.getBoolean("HasMasterPos")) {
            this.masterPos = BlockPos.of(tag.getLong("MasterPos"));
        } else {
            this.masterPos = null;
        }

        if (tag.contains("Parts")) {
            CompoundTag partsTag = tag.getCompound("Parts");

            for (Direction side : Direction.values()) {
                parts.put(side, LogicCablePartType.byName(partsTag.getString(side.getName())));
            }
        }
    }
}