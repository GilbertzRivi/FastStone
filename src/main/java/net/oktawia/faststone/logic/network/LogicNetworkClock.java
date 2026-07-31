package net.oktawia.faststone.logic.network;

import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.oktawia.faststone.Faststone;
import net.oktawia.faststone.client.LogicDisplaySync;
import net.oktawia.faststone.entities.LogicCableBlockEntity;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

@Mod.EventBusSubscriber(modid = Faststone.MODID)
public class LogicNetworkClock {

    public static final float DEFAULT_NETWORK_TICK_RATE = 64.0F;
    public static final float MIN_NETWORK_TICK_RATE = 0.0F;
    public static final float MAX_NETWORK_TICK_RATE = 4096.0F;

    @Getter
    private static float networkTickRate = DEFAULT_NETWORK_TICK_RATE;

    private static final Map<ServerLevel, Set<BlockPos>> MASTERS = new WeakHashMap<>();
    private static final Map<ServerLevel, Float> TICK_ACCUMULATORS = new WeakHashMap<>();

    private static long networkTickId = 0;
    private static long gameTickId = 0;

    public static float setNetworkTickRate(float value) {
        float clamped = Math.max(
                MIN_NETWORK_TICK_RATE,
                Math.min(MAX_NETWORK_TICK_RATE, value)
        );

        networkTickRate = clamped;
        TICK_ACCUMULATORS.clear();
        return clamped;
    }

    public static void registerMaster(ServerLevel level, BlockPos pos) {
        MASTERS
                .computeIfAbsent(level, ignored -> new HashSet<>())
                .add(pos.immutable());
    }

    public static void unregisterMaster(ServerLevel level, BlockPos pos) {
        Set<BlockPos> set = MASTERS.get(level);

        if (set != null) {
            set.remove(pos);
        }
    }

    public static void clearLevel(ServerLevel level) {
        MASTERS.remove(level);
        TICK_ACCUMULATORS.remove(level);
        LogicNetworkGraph.clearLevel(level);
        LogicDisplaySync.clearLevel(level);
    }

    @SubscribeEvent
    public static void onLevelTick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        if (!(event.level instanceof ServerLevel level)) {
            return;
        }

        if (level.isClientSide) {
            return;
        }

        LogicNetworkGraph.processPendingRebuilds(level);

        float rate = networkTickRate;

        if (rate <= 0.0F) {
            return;
        }

        float accumulator = TICK_ACCUMULATORS.getOrDefault(level, 0.0F) + rate;
        int ticksThisGameTick = (int) accumulator;
        TICK_ACCUMULATORS.put(level, accumulator - ticksThisGameTick);

        if (ticksThisGameTick <= 0) {
            return;
        }

        List<LogicCableBlockEntity> masters = getValidMasters(level);

        for (int i = 0; i < ticksThisGameTick; i++) {
            networkTickId++;

            for (LogicCableBlockEntity master : masters) {
                master.networkEvaluate(networkTickId);
            }

            LogicDisplaySync.beginNetworkSample(level);

            for (LogicCableBlockEntity master : masters) {
                master.networkCommit(networkTickId);
            }
        }

        gameTickId++;

        for (LogicCableBlockEntity master : masters) {
            master.networkAfterGameTick(gameTickId);
        }

        LogicDisplaySync.endGameTick(level);
    }

    @SubscribeEvent
    public static void onLevelUnload(LevelEvent.Unload event) {
        if (event.getLevel() instanceof ServerLevel level) {
            clearLevel(level);
        }
    }

    private static List<LogicCableBlockEntity> getValidMasters(ServerLevel level) {
        Set<BlockPos> set = MASTERS.computeIfAbsent(level, ignored -> new HashSet<>());
        List<LogicCableBlockEntity> result = new ArrayList<>();

        Iterator<BlockPos> iterator = set.iterator();

        while (iterator.hasNext()) {
            BlockPos pos = iterator.next();
            BlockEntity be = level.getBlockEntity(pos);

            if (!(be instanceof LogicCableBlockEntity cable) || !cable.isMaster()) {
                iterator.remove();
                continue;
            }

            result.add(cable);
        }

        return result;
    }
}