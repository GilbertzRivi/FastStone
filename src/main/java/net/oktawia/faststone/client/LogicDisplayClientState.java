package net.oktawia.faststone.client;

import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.oktawia.faststone.logic.LogicDisplayMode;
import net.oktawia.faststone.network.LogicDisplayBatchPacket;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public final class LogicDisplayClientState {

    private static final long PLAYBACK_DURATION_MS = 50L;
    private static final long EXPIRE_AFTER_MS = 250L;

    private static final Map<Key, Playback> PLAYBACKS = new HashMap<>();

    private LogicDisplayClientState() {
    }

    public static void accept(LogicDisplayBatchPacket packet) {
        long now = Util.getMillis();

        for (LogicDisplayBatchPacket.Entry entry : packet.entries()) {
            if (entry.sampleCount() <= 0 || entry.packedSamples().length == 0) {
                continue;
            }

            PLAYBACKS.put(
                    new Key(entry.pos().immutable(), entry.side()),
                    new Playback(
                            entry.sampleCount(),
                            entry.packedSamples().clone(),
                            now
                    )
            );
        }

        cleanup(now);
    }

    public static float getDisplayStrength(BlockPos pos, Direction side, LogicDisplayMode mode) {
        long now = Util.getMillis();
        cleanup(now);

        Playback playback = PLAYBACKS.get(new Key(pos, side));

        if (playback == null || playback.sampleCount() <= 0) {
            return 0.0f;
        }

        if (mode == LogicDisplayMode.ANALOG) {
            int onCount = 0;

            for (int i = 0; i < playback.sampleCount(); i++) {
                if (getBit(playback.samples(), i)) {
                    onCount++;
                }
            }

            return (float) onCount / playback.sampleCount();
        }

        long elapsed = now - playback.startMillis();

        if (elapsed < 0L) {
            return 0.0f;
        }

        int index;

        if (elapsed >= PLAYBACK_DURATION_MS) {
            index = playback.sampleCount() - 1;
        } else {
            index = (int) ((elapsed * playback.sampleCount()) / PLAYBACK_DURATION_MS);
            index = Math.max(0, Math.min(index, playback.sampleCount() - 1));
        }

        return getBit(playback.samples(), index) ? 1.0f : 0.0f;
    }

    private static void cleanup(long now) {
        Iterator<Map.Entry<Key, Playback>> iterator = PLAYBACKS.entrySet().iterator();

        while (iterator.hasNext()) {
            Map.Entry<Key, Playback> entry = iterator.next();

            if (now - entry.getValue().startMillis() > EXPIRE_AFTER_MS) {
                iterator.remove();
            }
        }
    }

    private static boolean getBit(byte[] bytes, int index) {
        if (index < 0) {
            return false;
        }

        int byteIndex = index >> 3;

        if (byteIndex < 0 || byteIndex >= bytes.length) {
            return false;
        }

        int bit = index & 7;

        return (bytes[byteIndex] & (1 << bit)) != 0;
    }

    private record Key(BlockPos pos, Direction side) {
    }

    private record Playback(
            int sampleCount,
            byte[] samples,
            long startMillis
    ) {
    }
}