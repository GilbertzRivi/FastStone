package net.oktawia.faststone.entities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.oktawia.faststone.logic.interfaces.LogicNetworkPort;
import net.oktawia.faststone.logic.network.LogicNetworkGraph;

public abstract class LogicEndpointBlockEntity extends BlockEntity implements LogicNetworkPort {

    public LogicEndpointBlockEntity(
            BlockEntityType<?> type,
            BlockPos pos,
            BlockState state
    ) {
        super(type, pos, state);
    }

    public LogicCableBlockEntity getMaster(Direction side) {
        if (this.level == null) {
            return null;
        }

        BlockEntity be = this.level.getBlockEntity(this.worldPosition.relative(side));

        if (!(be instanceof LogicCableBlockEntity cable)) {
            return null;
        }

        return cable.getMaster();
    }

    @Override
    public void onLoad() {
        super.onLoad();

        if (this.level != null && !this.level.isClientSide) {
            LogicNetworkGraph.scheduleRebuildAround(this.level, this.worldPosition);
        }
    }
}