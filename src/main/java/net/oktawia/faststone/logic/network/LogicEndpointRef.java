package net.oktawia.faststone.logic.network;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

public record LogicEndpointRef(
        BlockPos pos,
        Direction side
) {
}