package net.oktawia.faststone.logic.interfaces;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.oktawia.faststone.logic.LogicCableColor;

public interface LogicConnectable {

    boolean canConnectLogic(
            BlockState state,
            BlockGetter level,
            BlockPos pos,
            Direction side,
            LogicCableColor cableColor
    );
}