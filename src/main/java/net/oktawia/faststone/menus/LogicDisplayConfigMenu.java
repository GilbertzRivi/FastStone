package net.oktawia.faststone.menus;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.oktawia.faststone.defs.regs.MenuRegistrar;
import net.oktawia.faststone.entities.LogicCableBlockEntity;
import net.oktawia.faststone.logic.LogicDisplayMode;
import net.oktawia.faststone.logic.parts.LogicCablePartType;
import org.jetbrains.annotations.Nullable;

public class LogicDisplayConfigMenu extends AbstractContainerMenu {

    private final BlockPos pos;
    private final Direction side;

    public LogicDisplayConfigMenu(int containerId, Inventory inventory, FriendlyByteBuf buffer) {
        this(containerId, inventory, buffer.readBlockPos(), Direction.values()[buffer.readUnsignedByte() % Direction.values().length]);
    }

    public LogicDisplayConfigMenu(int containerId, Inventory inventory, BlockPos pos, Direction side) {
        super(MenuRegistrar.LOGIC_DISPLAY_CONFIG.get(), containerId);
        this.pos = pos;
        this.side = side;
    }

    public BlockPos getPos() {
        return pos;
    }

    public Direction getSide() {
        return side;
    }

    @Nullable
    public LogicDisplayMode getCurrentMode(Player player) {
        if (!(player.level().getBlockEntity(pos) instanceof LogicCableBlockEntity cable)) {
            return null;
        }

        return cable.getDisplayMode(side);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        if (!(player.level().getBlockEntity(pos) instanceof LogicCableBlockEntity cable)) {
            return false;
        }

        if (cable.getPart(side) != LogicCablePartType.DISPLAY) {
            return false;
        }

        return player.distanceToSqr(
                pos.getX() + 0.5D,
                pos.getY() + 0.5D,
                pos.getZ() + 0.5D
        ) <= 64.0D;
    }
}
