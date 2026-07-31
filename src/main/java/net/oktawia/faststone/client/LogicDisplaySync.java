package net.oktawia.faststone.client;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.oktawia.faststone.logic.network.LogicDisplayRef;
import net.oktawia.faststone.logic.network.LogicNetworkClock;
import net.oktawia.faststone.network.LogicDisplayBatchPacket;
import net.oktawia.faststone.network.NetworkHandler;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

public final class LogicDisplaySync {

    private static final int BATCH_GAME_TICKS = 1;
    private static final int MAX_SAMPLES_PER_BATCH = 1024;
    private static final int SYNC_RADIUS_BLOCKS = 96;
    private static final int MAX_PACKET_BYTES = 256_000;
    private static final int MAX_ENTRIES_PER_PACKET = 4096;

    private static final Map<ServerLevel, LevelBuffer> LEVELS = new WeakHashMap<>();

    private LogicDisplaySync() {
    }

    public static void beginNetworkSample(ServerLevel level) {
        buffer(level).beginNetworkSample();
    }

    public static void record(
            ServerLevel level,
            List<LogicDisplayRef> displays,
            boolean high
    ) {
        if (displays.isEmpty()) {
            return;
        }

        LevelBuffer buffer = buffer(level);
        int sampleIndex = buffer.currentSampleIndex();

        if (sampleIndex < 0) {
            return;
        }

        for (LogicDisplayRef display : displays) {
            buffer.history(display).set(sampleIndex, high);
        }
    }

    public static void endGameTick(ServerLevel level) {
        LevelBuffer buffer = buffer(level);

        buffer.gameTicks++;

        if (buffer.gameTicks < BATCH_GAME_TICKS) {
            return;
        }

        sendAndReset(level, buffer);
    }

    public static void clearLevel(ServerLevel level) {
        LEVELS.remove(level);
    }

    private static LevelBuffer buffer(ServerLevel level) {
        return LEVELS.computeIfAbsent(level, ignored -> new LevelBuffer());
    }

    private static void sendAndReset(ServerLevel level, LevelBuffer buffer) {
        if (buffer.sampleCount <= 0 || buffer.histories.isEmpty()) {
            buffer.reset();
            return;
        }

        for (ServerPlayer player : level.players()) {
            sendToPlayer(level, player, buffer);
        }

        buffer.reset();
    }

    private static void sendToPlayer(
            ServerLevel level,
            ServerPlayer player,
            LevelBuffer buffer
    ) {
        List<LogicDisplayBatchPacket.Entry> entries = new ArrayList<>();
        int estimatedBytes = 8;

        int radiusSq = SYNC_RADIUS_BLOCKS * SYNC_RADIUS_BLOCKS;

        for (Map.Entry<LogicDisplayRef, BitSet> entry : buffer.histories.entrySet()) {
            if (entries.size() >= MAX_ENTRIES_PER_PACKET) {
                break;
            }

            LogicDisplayRef ref = entry.getKey();

            if (!isNear(player, ref.pos(), radiusSq)) {
                continue;
            }

            if (!level.isLoaded(ref.pos())) {
                continue;
            }

            byte[] packed = entry.getValue().toByteArray();

            if (packed.length == 0) {
                packed = new byte[1];
            }

            int neededBytes =
                    8
                            + 1
                            + 5
                            + 5
                            + packed.length;

            if (estimatedBytes + neededBytes > MAX_PACKET_BYTES) {
                break;
            }

            estimatedBytes += neededBytes;

            entries.add(new LogicDisplayBatchPacket.Entry(
                    ref.pos(),
                    ref.side(),
                    buffer.sampleCount,
                    packed
            ));
        }

        if (entries.isEmpty()) {
            return;
        }

        NetworkHandler.sendToPlayer(
                player,
                new LogicDisplayBatchPacket(entries)
        );
    }

    private static boolean isNear(ServerPlayer player, BlockPos pos, int radiusSq) {
        double dx = pos.getX() + 0.5D - player.getX();
        double dy = pos.getY() + 0.5D - player.getY();
        double dz = pos.getZ() + 0.5D - player.getZ();

        return dx * dx + dy * dy + dz * dz <= radiusSq;
    }

    private static final class LevelBuffer {
        private final Map<LogicDisplayRef, BitSet> histories = new HashMap<>();

        private int gameTicks = 0;
        private int rawSampleCursor = 0;
        private int sampleCount = 0;
        private int currentSampleIndex = -1;
        private int sampleStride = 1;

        private void beginNetworkSample() {
            int expectedSamples = Math.max(
                    1,
                    (int) (LogicNetworkClock.getNetworkTickRate() * BATCH_GAME_TICKS)
            );

            sampleStride = Math.max(
                    1,
                    (expectedSamples + MAX_SAMPLES_PER_BATCH - 1) / MAX_SAMPLES_PER_BATCH
            );

            int raw = rawSampleCursor++;

            if (raw % sampleStride != 0) {
                currentSampleIndex = -1;
                return;
            }

            if (sampleCount >= MAX_SAMPLES_PER_BATCH) {
                currentSampleIndex = -1;
                return;
            }

            currentSampleIndex = sampleCount++;
        }

        private int currentSampleIndex() {
            return currentSampleIndex;
        }

        private BitSet history(LogicDisplayRef ref) {
            return histories.computeIfAbsent(
                    ref,
                    ignored -> new BitSet(MAX_SAMPLES_PER_BATCH)
            );
        }

        private void reset() {
            histories.clear();
            gameTicks = 0;
            rawSampleCursor = 0;
            sampleCount = 0;
            currentSampleIndex = -1;
            sampleStride = 1;
        }
    }
}