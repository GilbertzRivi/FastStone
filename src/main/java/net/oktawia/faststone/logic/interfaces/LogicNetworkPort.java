package net.oktawia.faststone.logic.interfaces;

import net.minecraft.core.Direction;
import net.oktawia.faststone.logic.LogicPortMode;

public interface LogicNetworkPort {

    LogicPortMode getLogicPortMode(Direction side);

    default boolean readLogicOutput(Direction side) {
        return false;
    }

    default void receiveLogicInput(Direction side, boolean value, long networkTickId) {
    }

    default void afterLogicGameTick(long gameTickId) {
    }
}