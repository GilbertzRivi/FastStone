package net.oktawia.faststone.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.oktawia.faststone.logic.interfaces.LogicGateConfigurable;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class SetLogicGateConfigPacket {

    private final BlockPos pos;
    private final int value;

    public SetLogicGateConfigPacket(BlockPos pos, int value) {
        this.pos = pos;
        this.value = value;
    }

    public static void encode(SetLogicGateConfigPacket packet, FriendlyByteBuf buffer) {
        buffer.writeBlockPos(packet.pos);
        buffer.writeInt(packet.value);
    }

    public static SetLogicGateConfigPacket decode(FriendlyByteBuf buffer) {
        return new SetLogicGateConfigPacket(
                buffer.readBlockPos(),
                buffer.readInt()
        );
    }

    public static void handle(
            SetLogicGateConfigPacket packet,
            Supplier<NetworkEvent.Context> contextSupplier
    ) {
        NetworkEvent.Context context = contextSupplier.get();

        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();

            if (player == null) {
                return;
            }

            if (player.distanceToSqr(
                    packet.pos.getX() + 0.5D,
                    packet.pos.getY() + 0.5D,
                    packet.pos.getZ() + 0.5D
            ) > 64.0D) {
                return;
            }

            if (!(player.level().getBlockEntity(packet.pos) instanceof LogicGateConfigurable configurable)) {
                return;
            }

            int clamped = Mth.clamp(
                    packet.value,
                    configurable.getMinConfigValue(),
                    configurable.getMaxConfigValue()
            );

            configurable.setConfigValue(clamped);
        });

        context.setPacketHandled(true);
    }
}