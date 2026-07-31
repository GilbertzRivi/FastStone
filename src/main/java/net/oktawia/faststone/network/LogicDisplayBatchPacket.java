package net.oktawia.faststone.network;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import net.oktawia.faststone.client.LogicDisplayClientState;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public record LogicDisplayBatchPacket(List<Entry> entries) {

    public record Entry(
            BlockPos pos,
            Direction side,
            int sampleCount,
            byte[] packedSamples
    ) {
    }

    private static final int MAX_ENTRIES = 8192;
    private static final int MAX_SAMPLE_BYTES = 8192;

    public static void encode(LogicDisplayBatchPacket packet, FriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.entries.size());

        for (Entry entry : packet.entries) {
            buffer.writeBlockPos(entry.pos());
            buffer.writeByte(entry.side().ordinal());
            buffer.writeVarInt(entry.sampleCount());
            buffer.writeByteArray(entry.packedSamples());
        }
    }

    public static LogicDisplayBatchPacket decode(FriendlyByteBuf buffer) {
        int count = Math.min(buffer.readVarInt(), MAX_ENTRIES);
        List<Entry> entries = new ArrayList<>(count);

        Direction[] directions = Direction.values();

        for (int i = 0; i < count; i++) {
            BlockPos pos = buffer.readBlockPos();
            int sideOrdinal = buffer.readUnsignedByte();
            int sampleCount = buffer.readVarInt();
            byte[] samples = buffer.readByteArray(MAX_SAMPLE_BYTES);

            Direction side = sideOrdinal >= 0 && sideOrdinal < directions.length
                    ? directions[sideOrdinal]
                    : Direction.NORTH;

            entries.add(new Entry(pos, side, sampleCount, samples));
        }

        return new LogicDisplayBatchPacket(entries);
    }

    public static void handle(
            LogicDisplayBatchPacket packet,
            Supplier<NetworkEvent.Context> contextSupplier
    ) {
        NetworkEvent.Context context = contextSupplier.get();

        context.enqueueWork(() -> {
            if (Minecraft.getInstance().level == null) {
                return;
            }

            LogicDisplayClientState.accept(packet);
        });

        context.setPacketHandled(true);
    }
}