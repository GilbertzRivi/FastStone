package net.oktawia.faststone.defs.regs;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.oktawia.faststone.Faststone;
import net.oktawia.faststone.blocks.*;
import net.oktawia.faststone.blocks.gates.LogicNotGateBlock;
import net.oktawia.faststone.items.LogicCableBlockItem;
import net.oktawia.faststone.logic.LogicCableColor;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class BlockRegistrar {

    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, Faststone.MODID);

    public static final DeferredRegister<Item> BLOCK_ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, Faststone.MODID);

    public static final RegistryObject<Block> LOGIC_CABLE =
            BLOCKS.register("logic_cable", () -> new LogicCableBlock(
                    BlockBehaviour.Properties.of()
                            .strength(0.3F)
                            .sound(SoundType.METAL)
                            .noOcclusion()
            ));

    public static final Map<LogicCableColor, RegistryObject<Item>> LOGIC_CABLE_ITEMS =
            new EnumMap<>(LogicCableColor.class);

    static {
        for (LogicCableColor color : LogicCableColor.values()) {
            LOGIC_CABLE_ITEMS.put(color, registerLogicCableItem(color));
        }
    }

    private static RegistryObject<Item> registerLogicCableItem(LogicCableColor color) {
        return BLOCK_ITEMS.register(color.getItemRegistryName(), () ->
                new LogicCableBlockItem(
                        LOGIC_CABLE.get(),
                        new Item.Properties(),
                        color
                )
        );
    }

    public static RegistryObject<Item> getLogicCableItem(LogicCableColor color) {
        return LOGIC_CABLE_ITEMS.get(color);
    }

    public static List<Block> getBlocks() {
        return BLOCKS.getEntries()
                .stream()
                .map(RegistryObject::get)
                .toList();
    }

    public static List<Item> getBlockItems() {
        return BLOCK_ITEMS.getEntries()
                .stream()
                .map(RegistryObject::get)
                .toList();
    }

    public static final RegistryObject<Block> LOGIC_NOT_GATE =
            BLOCKS.register("logic_not_gate", () -> new LogicNotGateBlock(
                    BlockBehaviour.Properties.of()
                            .strength(0.5F)
                            .sound(SoundType.METAL)
                            .noOcclusion()
            ));

    public static final RegistryObject<Block> LOGIC_BUS =
            BLOCKS.register("logic_bus", () -> new LogicBusBlock(
                    BlockBehaviour.Properties.of()
                            .strength(0.3F)
                            .sound(SoundType.METAL)
                            .noOcclusion()
            ));

    public static final RegistryObject<Item> LOGIC_NOT_GATE_BLOCK_ITEM =
            BLOCK_ITEMS.register("logic_not_gate", () ->
                    new BlockItem(LOGIC_NOT_GATE.get(), new Item.Properties())
            );

    public static final RegistryObject<Item> LOGIC_BUS_BLOCK_ITEM =
            BLOCK_ITEMS.register("logic_bus", () ->
                    new BlockItem(LOGIC_BUS.get(), new Item.Properties())
            );

    private BlockRegistrar() {}
}