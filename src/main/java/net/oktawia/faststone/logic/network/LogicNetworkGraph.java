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

        collectEndpoints(level, wires, color, inputs, outputs);

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
                cable.becomeMaster(wires, inputs, outputs);
            } else {
                cable.becomeMember(masterPos);
            }
        }

        return wires;
    }

    private static Set<BlockPos> collectWiresForColor(
            ServerLevel level,
            BlockPos startPos,
            LogicCableColor color
    ) {
        if (!isWireForColor(level, startPos, color)) {
            return Set.of();
        }

        Set<BlockPos> visited = new HashSet<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();

        visited.add(startPos.immutable());
        queue.add(startPos.immutable());

        while (!queue.isEmpty() && visited.size() < NETWORK_HARD_LIMIT) {
            BlockPos pos = queue.removeFirst();
            BlockState state = level.getBlockState(pos);

            if (!isWireForColor(level, pos, color)) {
                continue;
            }

            for (Direction dir : Direction.values()) {
                if (!isConnectedOnSide(state, dir)) {
                    continue;
                }

                BlockPos nextPos = pos.relative(dir);

                if (!isWireForColor(level, nextPos, color)) {
                    continue;
                }

                BlockState nextState = level.getBlockState(nextPos);

                if (!isConnectedOnSide(nextState, dir.getOpposite())) {
                    continue;
                }

                if (visited.add(nextPos.immutable())) {
                    queue.add(nextPos.immutable());
                }
            }
        }

        return visited;
    }

    private static boolean isWireForColor(
            ServerLevel level,
            BlockPos pos,
            LogicCableColor color
    ) {
        BlockState state = level.getBlockState(pos);

        if (state.getBlock() instanceof LogicBusBlock) {
            return true;
        }

        if (state.getBlock() instanceof LogicCableBlock) {
            return state.hasProperty(LogicCableBlock.COLOR)
                    && state.getValue(LogicCableBlock.COLOR) == color;
        }

        return false;
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
            List<LogicEndpointRef> outputs
    ) {
        Set<LogicEndpointRef> seenInputs = new HashSet<>();
        Set<LogicEndpointRef> seenOutputs = new HashSet<>();

        for (BlockPos wirePos : wires) {
            BlockState wireState = level.getBlockState(wirePos);

            if (!(wireState.getBlock() instanceof LogicCableBlock)) {
                continue;
            }

            if (!wireState.hasProperty(LogicCableBlock.COLOR)) {
                continue;
            }

            if (wireState.getValue(LogicCableBlock.COLOR) != color) {
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
                        outputs
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
            List<LogicEndpointRef> outputs
    ) {
        for (Direction side : Direction.values()) {
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

            if (!state.hasProperty(LogicCableBlock.COLOR)) {
                continue;
            }

            if (state.getValue(LogicCableBlock.COLOR) != color) {
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
}