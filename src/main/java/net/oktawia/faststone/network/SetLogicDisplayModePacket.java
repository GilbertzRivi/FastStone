package net.oktawia.faststone.network;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import net.oktawia.faststone.entities.LogicCableBlockEntity;
import net.oktawia.faststone.logic.LogicDisplayMode;

import java.util.function.Supplier;

public record SetLogicDisplayModePacket(BlockPos pos, Direction side, LogicDisplayMode mode) {

    public static void encode(SetLogicDisplayModePacket packet, FriendlyByteBuf buffer) {
        buffer.writeBlockPos(packet.pos());
        buffer.writeByte(packet.side().ordinal());
        buffer.writeByte(packet.mode().ordinal());
    }

    public static SetLogicDisplayModePacket decode(FriendlyByteBuf buffer) {
        BlockPos pos = buffer.readBlockPos();
        Direction side = Direction.values()[buffer.readUnsignedByte() % Direction.values().length];
        LogicDisplayMode mode = LogicDisplayMode.values()[buffer.readUnsignedByte() % LogicDisplayMode.values().length];
        return new SetLogicDisplayModePacket(pos, side, mode);
    }

    public static void handle(
            SetLogicDisplayModePacket packet,
            Supplier<NetworkEvent.Context> contextSupplier
    ) {
        NetworkEvent.Context context = contextSupplier.get();

        context.enqueueWork(() -> {
            var player = context.getSender();

            if (player == null) {
                return;
            }

            if (!(player.level().getBlockEntity(packet.pos()) instanceof LogicCableBlockEntity cable)) {
                return;
            }

            cable.setDisplayMode(packet.side(), packet.mode());
        });

        context.setPacketHandled(true);
    }
}
