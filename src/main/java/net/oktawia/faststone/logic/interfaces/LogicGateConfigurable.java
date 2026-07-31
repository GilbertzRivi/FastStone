package net.oktawia.faststone.logic.interfaces;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.oktawia.faststone.menus.LogicGateConfigMenu;
import org.jetbrains.annotations.Nullable;

public interface LogicGateConfigurable extends MenuProvider {

    BlockPos getConfigBlockPos();

    String getConfigTitleTranslationKey();

    String getConfigLabelTranslationKey();

    int getConfigValue();

    void setConfigValue(int value);

    int getMinConfigValue();

    int getMaxConfigValue();

    @Override
    default Component getDisplayName() {
        return Component.translatable(getConfigTitleTranslationKey());
    }

    @Nullable
    @Override
    default AbstractContainerMenu createMenu(
            int containerId,
            Inventory inventory,
            Player player
    ) {
        return new LogicGateConfigMenu(containerId, inventory, getConfigBlockPos());
    }
}