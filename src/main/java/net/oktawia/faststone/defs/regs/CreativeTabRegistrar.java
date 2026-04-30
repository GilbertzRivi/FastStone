package net.oktawia.faststone.defs.regs;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.oktawia.faststone.Faststone;
import net.oktawia.faststone.defs.LangDefs;

public final class CreativeTabRegistrar {

    public static final ResourceLocation ID = Faststone.makeId("tab");

    public static final CreativeModeTab TAB = CreativeModeTab.builder()
            .title(Component.translatable(LangDefs.MOD_NAME.getTranslationKey()))
            .icon(() -> new ItemStack(BlockRegistrar.LOGIC_CABLE.get()))
            .displayItems(CreativeTabRegistrar::populate)
            .build();

    private static void populate(CreativeModeTab.ItemDisplayParameters ignored, CreativeModeTab.Output out) {
        ItemRegistrar.ITEMS.getEntries().forEach(ro -> out.accept(ro.get()));
        BlockRegistrar.BLOCK_ITEMS.getEntries().forEach(ro -> out.accept(ro.get()));
    }

    private CreativeTabRegistrar() {}
}
