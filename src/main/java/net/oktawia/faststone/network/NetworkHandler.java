package net.oktawia.faststone.network;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;
import net.oktawia.faststone.Faststone;

public final class NetworkHandler {

    private static final String PROTOCOL_VERSION = "1";

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            Faststone.makeId("main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    private static int nextId = 0;

    private NetworkHandler() {}

    public static void registerMessages() {
        CHANNEL.registerMessage(
                nextId++,
                SetLogicGateConfigPacket.class,
                SetLogicGateConfigPacket::encode,
                SetLogicGateConfigPacket::decode,
                SetLogicGateConfigPacket::handle
        );
        CHANNEL.registerMessage(
                nextId++,
                LogicDisplayBatchPacket.class,
                LogicDisplayBatchPacket::encode,
                LogicDisplayBatchPacket::decode,
                LogicDisplayBatchPacket::handle
        );
        CHANNEL.registerMessage(
                nextId++,
                SetLogicDisplayModePacket.class,
                SetLogicDisplayModePacket::encode,
                SetLogicDisplayModePacket::decode,
                SetLogicDisplayModePacket::handle
        );
    }

    public static void sendToPlayer(ServerPlayer player, Object packet) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
    }

    public static void sendToTrackingChunk(LevelChunk chunk, Object packet) {
        CHANNEL.send(PacketDistributor.TRACKING_CHUNK.with(() -> chunk), packet);
    }

    public static void sendToServer(Object packet) {
        CHANNEL.sendToServer(packet);
    }
}