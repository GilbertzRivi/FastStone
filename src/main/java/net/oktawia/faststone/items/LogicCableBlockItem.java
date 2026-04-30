package net.oktawia.faststone.items;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;
import net.oktawia.faststone.logic.LogicCableColor;

public class LogicCableBlockItem extends BlockItem {

    private final LogicCableColor color;

    public LogicCableBlockItem(Block block, Properties properties, LogicCableColor color) {
        super(block, properties);
        this.color = color;
    }

    public LogicCableColor getColor() {
        return color;
    }
}