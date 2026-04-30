package net.oktawia.faststone.defs.regs;

import net.minecraft.world.item.Item;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.oktawia.faststone.Faststone;
import net.oktawia.faststone.items.LogicCablePartItem;
import net.oktawia.faststone.logic.parts.LogicCablePartType;

import java.util.List;

public class ItemRegistrar {

    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, Faststone.MODID);

    public static List<Item> getItems() {
        return ITEMS.getEntries()
                .stream()
                .map(RegistryObject::get)
                .toList();
    }

    public static final RegistryObject<Item> LOGIC_INPUT_PART =
            ITEMS.register("logic_input_part", () ->
                    new LogicCablePartItem(
                            new Item.Properties(),
                            LogicCablePartType.INPUT
                    )
            );

    public static final RegistryObject<Item> LOGIC_OUTPUT_PART =
            ITEMS.register("logic_output_part", () ->
                    new LogicCablePartItem(
                            new Item.Properties(),
                            LogicCablePartType.OUTPUT
                    )
            );

    private ItemRegistrar() {}
}