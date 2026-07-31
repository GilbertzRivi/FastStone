package net.oktawia.faststone.logic.network;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.oktawia.faststone.blocks.LogicBusBlock;
import net.oktawia.faststone.blocks.LogicCableBlock;
import net.oktawia.faststone.entities.LogicCableBlockEntity;
import net.oktawia.faststone.logic.LogicCableColor;
import net.oktawia.faststone.logic.LogicPortMode;
import net.oktawia.faststone.logic.interfaces.LogicNetworkPort;
import net.oktawia.faststone.logic.parts.LogicCablePartType;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

public class LogicNetworkGraph {

    private static final int NETWORK_HARD_LIMIT = 8192;

    private static final Map<ServerLevel, Set<BlockPos>> PENDING_REBUILDS = new WeakHashMap<>();

    public static void scheduleRebuildAround(Level level, BlockPos pos) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }

        if (serverLevel.isClientSide) {
            return;
        }

        PENDING_REBUILDS
                .computeIfAbsent(serverLevel, ignored -> new LinkedHashSet<>())
                .add(pos.immutable());
    }

    public static void processPendingRebuilds(ServerLevel level) {
        Set<BlockPos> pending = PENDING_REBUILDS.remove(level);

        if (pending == null || pending.isEmpty()) {
            return;
        }

        Set<BlockPos> processed = new HashSet<>();

        for (BlockPos pos : pending) {
            if (!processed.add(pos)) {
                continue;
            }

            rebuildAround(level, pos);
        }
    }

    public static void clearLevel(ServerLevel level) {
        PENDING_REBUILDS.remove(level);
    }

    public static void rebuildAround(ServerLevel level, BlockPos pos) {
        Set<BlockPos> coloredCableStarts = new LinkedHashSet<>();

        collectColoredCableStartsAround(level, pos, coloredCableStarts);

        for (Direction dir : Direction.values()) {
            collectColoredCableStartsAround(level, pos.relative(dir), coloredCableStarts);
        }

        Set<BlockPos> alreadyRebuilt = new HashSet<>();

        for (BlockPos start : coloredCableStarts) {
            if (alreadyRebuilt.contains(start)) {
                continue;
            }

            BlockState startState = level.getBlockState(start);

            if (!(startState.getBlock() instanceof LogicCableBlock)) {
                continue;
            }

            LogicCableColor color = startState.getValue(LogicCableBlock.COLOR);
            Set<BlockPos> rebuilt = rebuildFromColoredCable(level, start, color);

            alreadyRebuilt.addAll(rebuilt);
        }
    }

    private static void collectColoredCableStartsAround(
            ServerLevel level,
            BlockPos pos,
            Set<BlockPos> starts
    ) {
        BlockState state = level.getBlockState(pos);

        if (state.getBlock() instanceof LogicCableBlock) {
            starts.add(pos.immutable());
            return;
        }

        if (state.getBlock() instanceof LogicBusBlock) {
            Set<BlockPos> busCluster = collectBusCluster(level, pos);

            for (BlockPos busPos : busCluster) {
                for (Direction dir : Direction.values()) {
                    BlockPos adjacentPos = busPos.relative(dir);
                    BlockState adjacentState = level.getBlockState(adjacentPos);

                    if (adjacentState.getBlock() instanceof LogicCableBlock) {
                        starts.add(adjacentPos.immutable());
                    }
                }
            }
        }
    }

    private static Set<BlockPos> collectBusCluster(ServerLevel level, BlockPos startPos) {
        if (!(level.getBlockState(startPos).getBlock() instanceof LogicBusBlock)) {
            return Set.of();
        }

        Set<BlockPos> visited = new HashSet<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();

        visited.add(startPos.immutable());
        queue.add(startPos.immutable());

        while (!queue.isEmpty() && visited.size() < NETWORK_HARD_LIMIT) {
            BlockPos pos = queue.removeFirst();
            BlockState state = level.getBlockState(pos);

            if (!(state.getBlock() instanceof LogicBusBlock)) {
                continue;
            }

            for (Direction dir : Direction.values()) {
                if (!state.getValue(LogicBusBlock.prop(dir))) {
                    continue;
                }

                BlockPos nextPos = pos.relative(dir);
                BlockState nextState = level.getBlockState(nextPos);

                if (!(nextState.getBlock() instanceof LogicBusBlock)) {
                    continue;
                }

                if (!nextState.getValue(LogicBusBlock.prop(dir.getOpposite()))) {
                    continue;
                }

                if (visited.add(nextPos.immutable())) {
                    queue.add(nextPos.immutable());
                }
            }
        }

        return visited;
    }

    private static Set<BlockPos> rebuildFromColoredCable(
            ServerLevel level,
            BlockPos startPos,
            LogicCableColor color
    ) {
        Set<BlockPos> wires = collectWiresForColor(level, startPos, color);

        if (wires.isEmpty()) {
            return Set.of();
        }

        BlockPos masterPos = chooseMasterColoredCable(level, wires, color);

        if (masterPos == null) {
            return wires;
        }

        List<LogicEndpointRef> inputs = new ArrayList<>();
        List<LogicEndpointRef> outputs = new ArrayList<>();
        List<LogicDisplayRef> displays = new ArrayList<>();

        collectEndpoints(level, wires, color, inputs, outputs, displays);

        for (BlockPos wirePos : wires) {
            BlockEntity be = level.getBlockEntity(wirePos);

            if (be instanceof LogicCableBlockEntity cable) {
                cable.clearNetworkCache();
            }
        }

        for (BlockPos wirePos : wires) {
            BlockEntity be = level.getBlockEntity(wirePos);

            if (!(be instanceof LogicCableBlockEntity cable)) {
                continue;
            }

            if (wirePos.equals(masterPos)) {
                cable.becomeMaster(wires, inputs, outputs, displays);
            } else {
                cable.becomeMember(masterPos);
            }
        }

        return wires;
    }

    private static Set<BlockPos> collectWiresForColor(
            ServerLevel level,
            BlockPos startPos,
            LogicCableColor ignoredColor
    ) {
        BlockState startState = level.getBlockState(startPos);

        if (!(startState.getBlock() instanceof LogicCableBlock)) {
            return Set.of();
        }

        if (!startState.hasProperty(LogicCableBlock.COLOR)) {
            return Set.of();
        }

        LogicCableColor startColor = startState.getValue(LogicCableBlock.COLOR);
        LogicCableColor startChannel = startColor.isColorless()
                ? LogicCableColor.COLORLESS
                : startColor;

        Set<BlockPos> wires = new HashSet<>();
        Set<TraversalNode> visited = new HashSet<>();
        ArrayDeque<TraversalNode> queue = new ArrayDeque<>();

        TraversalNode start = new TraversalNode(startPos.immutable(), startChannel);

        visited.add(start);
        queue.add(start);

        while (!queue.isEmpty() && wires.size() < NETWORK_HARD_LIMIT) {
            TraversalNode node = queue.removeFirst();
            BlockPos pos = node.pos();
            BlockState state = level.getBlockState(pos);

            if (!isWireState(state)) {
                continue;
            }

            wires.add(pos.immutable());

            for (Direction dir : Direction.values()) {
                if (!isConnectedOnSide(state, dir)) {
                    continue;
                }

                BlockPos nextPos = pos.relative(dir);
                BlockState nextState = level.getBlockState(nextPos);

                if (!isWireState(nextState)) {
                    continue;
                }

                if (!isConnectedOnSide(nextState, dir.getOpposite())) {
                    continue;
                }

                LogicCableColor nextChannel = getNextTraversalChannel(
                        state,
                        node.channel(),
                        nextState
                );

                if (nextChannel == null) {
                    continue;
                }

                TraversalNode nextNode = new TraversalNode(
                        nextPos.immutable(),
                        nextChannel
                );

                if (visited.add(nextNode)) {
                    queue.add(nextNode);
                }
            }
        }

        return wires;
    }

    private static boolean isConnectedOnSide(BlockState state, Direction dir) {
        if (state.getBlock() instanceof LogicCableBlock) {
            return state.getValue(LogicCableBlock.prop(dir));
        }

        if (state.getBlock() instanceof LogicBusBlock) {
            return state.getValue(LogicBusBlock.prop(dir));
        }

        return false;
    }

    private static void collectEndpoints(
            ServerLevel level,
            Set<BlockPos> wires,
            LogicCableColor color,
            List<LogicEndpointRef> inputs,
            List<LogicEndpointRef> outputs,
            List<LogicDisplayRef> displays
    ) {
        Set<LogicEndpointRef> seenInputs = new HashSet<>();
        Set<LogicEndpointRef> seenOutputs = new HashSet<>();

        for (BlockPos wirePos : wires) {
            BlockState wireState = level.getBlockState(wirePos);

            if (!(wireState.getBlock() instanceof LogicCableBlock)) {
                continue;
            }

            LogicCableBlockEntity cable = null;
            BlockEntity wireBe = level.getBlockEntity(wirePos);

            if (wireBe instanceof LogicCableBlockEntity cableBe) {
                cable = cableBe;
            }

            if (cable != null) {
                collectCableParts(
                        cable,
                        wirePos,
                        seenInputs,
                        seenOutputs,
                        inputs,
                        outputs,
                        displays
                );
            }

            collectAdjacentEndpointBlocks(
                    level,
                    wirePos,
                    wireState,
                    wires,
                    cable,
                    seenInputs,
                    seenOutputs,
                    inputs,
                    outputs
            );
        }
    }

    private static void collectCableParts(
            LogicCableBlockEntity cable,
            BlockPos cablePos,
            Set<LogicEndpointRef> seenInputs,
            Set<LogicEndpointRef> seenOutputs,
            List<LogicEndpointRef> inputs,
            List<LogicEndpointRef> outputs,
            List<LogicDisplayRef> displays
    ) {
        for (Direction side : Direction.values()) {
            LogicCablePartType part = cable.getPart(side);

            if (part == LogicCablePartType.DISPLAY) {
                displays.add(new LogicDisplayRef(cablePos.immutable(), side));
                continue;
            }

            LogicPortMode mode = cable.getLogicPortMode(side);

            if (mode == LogicPortMode.NONE) {
                continue;
            }

            LogicEndpointRef ref = new LogicEndpointRef(cablePos.immutable(), side);

            if (mode.writesToCable() && seenInputs.add(ref)) {
                inputs.add(ref);
            }

            if (mode.readsFromCable() && seenOutputs.add(ref)) {
                outputs.add(ref);
            }
        }
    }

    private static void collectAdjacentEndpointBlocks(
            ServerLevel level,
            BlockPos wirePos,
            BlockState wireState,
            Set<BlockPos> wires,
            LogicCableBlockEntity cable,
            Set<LogicEndpointRef> seenInputs,
            Set<LogicEndpointRef> seenOutputs,
            List<LogicEndpointRef> inputs,
            List<LogicEndpointRef> outputs
    ) {
        for (Direction dir : Direction.values()) {
            if (!wireState.getValue(LogicCableBlock.prop(dir))) {
                continue;
            }

            if (cable != null && cable.hasPart(dir)) {
                continue;
            }

            BlockPos endpointPos = wirePos.relative(dir);

            if (wires.contains(endpointPos)) {
                continue;
            }

            BlockEntity be = level.getBlockEntity(endpointPos);

            if (!(be instanceof LogicNetworkPort endpoint)) {
                continue;
            }

            Direction endpointSide = dir.getOpposite();
            LogicPortMode mode = endpoint.getLogicPortMode(endpointSide);

            if (mode == LogicPortMode.NONE) {
                continue;
            }

            LogicEndpointRef ref = new LogicEndpointRef(endpointPos.immutable(), endpointSide);

            if (mode.writesToCable() && seenInputs.add(ref)) {
                inputs.add(ref);
            }

            if (mode.readsFromCable() && seenOutputs.add(ref)) {
                outputs.add(ref);
            }
        }
    }

    private static BlockPos chooseMasterColoredCable(
            ServerLevel level,
            Set<BlockPos> wires,
            LogicCableColor color
    ) {
        BlockPos master = null;

        for (BlockPos pos : wires) {
            BlockState state = level.getBlockState(pos);

            if (!(state.getBlock() instanceof LogicCableBlock)) {
                continue;
            }

            if (master == null || compareMasterCandidate(pos, master) > 0) {
                master = pos.immutable();
            }
        }

        return master;
    }

    private static int compareMasterCandidate(BlockPos a, BlockPos b) {
        int x = Integer.compare(a.getX(), b.getX());

        if (x != 0) {
            return x;
        }

        int y = Integer.compare(a.getY(), b.getY());

        if (y != 0) {
            return y;
        }

        return Integer.compare(a.getZ(), b.getZ());
    }

    private static boolean isWireState(BlockState state) {
        return state.getBlock() instanceof LogicCableBlock
                || state.getBlock() instanceof LogicBusBlock;
    }

    private static LogicCableColor getNextTraversalChannel(
            BlockState currentState,
            LogicCableColor currentChannel,
            BlockState nextState
    ) {
        boolean currentCable = currentState.getBlock() instanceof LogicCableBlock;
        boolean currentBus = currentState.getBlock() instanceof LogicBusBlock;
        boolean nextCable = nextState.getBlock() instanceof LogicCableBlock;
        boolean nextBus = nextState.getBlock() instanceof LogicBusBlock;

        if (currentBus && nextBus) {
            return currentChannel;
        }

        if (currentCable && nextBus) {
            if (!currentState.hasProperty(LogicCableBlock.COLOR)) {
                return null;
            }

            LogicCableColor currentColor = currentState.getValue(LogicCableBlock.COLOR);

            return currentColor.isColorless()
                    ? LogicCableColor.COLORLESS
                    : currentColor;
        }

        if (currentBus && nextCable) {
            if (!nextState.hasProperty(LogicCableBlock.COLOR)) {
                return null;
            }

            LogicCableColor nextColor = nextState.getValue(LogicCableBlock.COLOR);

            if (currentChannel.isColorless()) {
                return nextColor.isColorless()
                        ? LogicCableColor.COLORLESS
                        : nextColor;
            }

            if (nextColor.isColorless()) {
                return LogicCableColor.COLORLESS;
            }

            if (nextColor == currentChannel) {
                return currentChannel;
            }

            return null;
        }

        if (currentCable && nextCable) {
            if (!currentState.hasProperty(LogicCableBlock.COLOR)
                    || !nextState.hasProperty(LogicCableBlock.COLOR)) {
                return null;
            }

            LogicCableColor currentColor = currentState.getValue(LogicCableBlock.COLOR);
            LogicCableColor nextColor = nextState.getValue(LogicCableBlock.COLOR);

            if (!LogicCableColor.areCompatible(currentColor, nextColor)) {
                return null;
            }

            return nextColor.isColorless()
                    ? LogicCableColor.COLORLESS
                    : nextColor;
        }

        return null;
    }

    private record TraversalNode(BlockPos pos, LogicCableColor channel) {
    }
}