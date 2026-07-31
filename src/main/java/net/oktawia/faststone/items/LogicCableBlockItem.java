package net.oktawia.faststone.items;

import lombok.Getter;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.oktawia.faststone.Faststone;
import net.oktawia.faststone.defs.LangDefs;
import net.oktawia.faststone.logic.LogicCableColor;
import org.jetbrains.annotations.Nullable;

import java.util.List;

@Getter
public class LogicCableBlockItem extends BlockItem {

    private final LogicCableColor color;

    public LogicCableBlockItem(Block block, Properties properties, LogicCableColor color) {
        super(block, properties);
        this.color = color;
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            @Nullable Level level,
            List<Component> tooltip,
            TooltipFlag flag
    ) {
        super.appendHoverText(stack, level, tooltip, flag);

        tooltip.add(Component.translatable(
                LangDefs.TOOLTIP_LOGIC_CABLE_COLOR.getTranslationKey(),
                Component.translatable(
                        "tooltip.faststone.logic_cable.color." + color.getSerializedName()
                )
        ).withStyle(ChatFormatting.GRAY));
    }
}