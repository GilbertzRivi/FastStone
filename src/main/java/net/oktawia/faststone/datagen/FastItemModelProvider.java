package net.oktawia.faststone.datagen;

import net.minecraft.data.PackOutput;
import net.minecraft.world.item.Item;
import net.minecraftforge.client.model.generators.ItemModelBuilder;
import net.minecraftforge.client.model.generators.ItemModelProvider;
import net.minecraftforge.common.data.ExistingFileHelper;
import net.minecraftforge.registries.ForgeRegistries;
import net.oktawia.faststone.Faststone;
import net.oktawia.faststone.defs.regs.BlockRegistrar;
import net.oktawia.faststone.defs.regs.ItemRegistrar;
import net.oktawia.faststone.items.LogicCableBlockItem;

public class FastItemModelProvider extends ItemModelProvider {

    public FastItemModelProvider(PackOutput output, ExistingFileHelper existingFileHelper) {
        super(output, Faststone.MODID, existingFileHelper);
    }

    @Override
    protected void registerModels() {
        for (var item : ItemRegistrar.getItems()) {
            if (shouldSkipManualItemModel(item)) {
                continue;
            }

            simpleItem(item);
        }

        for (var item : BlockRegistrar.getBlockItems()) {
            if (shouldSkipManualItemModel(item)) {
                continue;
            }

            if (item instanceof LogicCableBlockItem) {
                logicCableItem(item);
            } else {
                simpleBlockItem(item);
            }
        }
    }

    private boolean shouldSkipManualItemModel(Item item) {
        String path = ForgeRegistries.ITEMS.getKey(item).getPath();

        return path.equals("logic_cable")
                || path.equals("logic_bus")
                || path.equals("logic_input_part")
                || path.equals("logic_output_part")
                || path.contains("gate");
    }

    private ItemModelBuilder simpleItem(Item item) {
        String path = ForgeRegistries.ITEMS.getKey(item).getPath();

        return withExistingParent(path, mcLoc("item/generated"))
                .texture("layer0", modLoc("item/" + path));
    }

    private ItemModelBuilder simpleBlockItem(Item item) {
        String path = ForgeRegistries.ITEMS.getKey(item).getPath();

        return withExistingParent(path, modLoc("block/" + path));
    }

    private ItemModelBuilder logicCableItem(Item item) {
        String path = ForgeRegistries.ITEMS.getKey(item).getPath();

        return withExistingParent(path, modLoc("item/logic_cable"));
    }
}