package net.oktawia.faststone.logic.network;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.oktawia.faststone.Faststone;
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

    public static final int DEFAULT_NETWORK_TICKS_PER_GAME_TICK = 64;
    public static final int MIN_NETWORK_TICKS_PER_GAME_TICK = 0;
    public static final int MAX_NETWORK_TICKS_PER_GAME_TICK = 4096;

    private static int networkTicksPerGameTick = DEFAULT_NETWORK_TICKS_PER_GAME_TICK;

    private static final Map<ServerLevel, Set<BlockPos>> MASTERS = new WeakHashMap<>();

    private static long networkTickId = 0;
    private static long gameTickId = 0;

    public static int getNetworkTicksPerGameTick() {
        return networkTicksPerGameTick;
    }

    public static int setNetworkTicksPerGameTick(int value) {
        int clamped = Math.max(
                MIN_NETWORK_TICKS_PER_GAME_TICK,
                Math.min(MAX_NETWORK_TICKS_PER_GAME_TICK, value)
        );

        networkTicksPerGameTick = clamped;
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
        LogicNetworkGraph.clearLevel(level);
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

        int ticksThisGameTick = networkTicksPerGameTick;

        if (ticksThisGameTick <= 0) {
            return;
        }

        List<LogicCableBlockEntity> masters = getValidMasters(level);

        for (int i = 0; i < ticksThisGameTick; i++) {
            networkTickId++;

            for (LogicCableBlockEntity master : masters) {
                master.networkEvaluate();
            }

            for (LogicCableBlockEntity master : masters) {
                master.networkCommit(networkTickId);
            }
        }

        gameTickId++;

        for (LogicCableBlockEntity master : masters) {
            master.networkAfterGameTick(gameTickId);
        }
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