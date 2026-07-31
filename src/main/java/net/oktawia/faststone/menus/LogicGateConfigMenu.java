package net.oktawia.faststone.menus;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.oktawia.faststone.defs.regs.MenuRegistrar;
import net.oktawia.faststone.logic.interfaces.LogicGateConfigurable;
import org.jetbrains.annotations.Nullable;

public class LogicGateConfigMenu extends AbstractContainerMenu {

    private final BlockPos pos;

    public LogicGateConfigMenu(
            int containerId,
            Inventory inventory,
            FriendlyByteBuf buffer
    ) {
        this(containerId, inventory, buffer.readBlockPos());
    }

    public LogicGateConfigMenu(
            int containerId,
            Inventory inventory,
            BlockPos pos
    ) {
        super(MenuRegistrar.LOGIC_GATE_CONFIG.get(), containerId);
        this.pos = pos;
    }

    public BlockPos getPos() {
        return pos;
    }

    @Nullable
    public LogicGateConfigurable getConfigurable(Player player) {
        if (player.level().getBlockEntity(pos) instanceof LogicGateConfigurable configurable) {
            return configurable;
        }

        return null;
    }

    @Override
    public ItemStack quickMoveStack(Player p_38941_, int p_38942_) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return getConfigurable(player) != null
                && player.distanceToSqr(
                        pos.getX() + 0.5D,
                        pos.getY() + 0.5D,
                        pos.getZ() + 0.5D
                ) <= 64.0D;
    }
}