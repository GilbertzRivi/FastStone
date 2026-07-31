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
import net.oktawia.faststone.blocks.gates.*;
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

    public static final RegistryObject<Block> LOGIC_BUS =
            BLOCKS.register("logic_bus", () -> new LogicBusBlock(
                    BlockBehaviour.Properties.of()
                            .strength(0.3F)
                            .sound(SoundType.METAL)
                            .noOcclusion()
            ));

    public static final RegistryObject<Block> LOGIC_NOT_GATE =
            BLOCKS.register("logic_not_gate", () -> new LogicNotGateBlock(
                    gateProperties()
            ));

    public static final RegistryObject<Block> LOGIC_AND_GATE =
            BLOCKS.register("logic_and_gate", () -> new LogicAndGateBlock(
                    gateProperties()
            ));

    public static final RegistryObject<Block> LOGIC_NAND_GATE =
            BLOCKS.register("logic_nand_gate", () -> new LogicNandGateBlock(
                    gateProperties()
            ));

    public static final RegistryObject<Block> LOGIC_XOR_GATE =
            BLOCKS.register("logic_xor_gate", () -> new LogicXorGateBlock(
                    gateProperties()
            ));

    public static final RegistryObject<Block> LOGIC_XNOR_GATE =
            BLOCKS.register("logic_xnor_gate", () -> new LogicXnorGateBlock(
                    gateProperties()
            ));

    public static final RegistryObject<Block> LOGIC_CONSTANT_GATE =
            BLOCKS.register("logic_constant_gate", () -> new LogicConstantGateBlock(
                    gateProperties()
            ));

    public static final RegistryObject<Block> LOGIC_SR_LATCH =
            BLOCKS.register("logic_sr_latch", () -> new LogicSrLatchBlock(
                    gateProperties()
            ));

    public static final RegistryObject<Block> LOGIC_D_FLIP_FLOP =
            BLOCKS.register("logic_d_flip_flop", () -> new LogicDFlipFlopBlock(
                    gateProperties()
            ));

    public static final RegistryObject<Block> LOGIC_CLOCK =
            BLOCKS.register("logic_clock", () -> new LogicClockBlock(
                    gateProperties()
            ));

    public static final RegistryObject<Block> LOGIC_BUFFER =
            BLOCKS.register("logic_buffer", () -> new LogicBufferBlock(
                    gateProperties()
            ));

    public static final Map<LogicCableColor, RegistryObject<Item>> LOGIC_CABLE_ITEMS =
            new EnumMap<>(LogicCableColor.class);

    static {
        for (LogicCableColor color : LogicCableColor.values()) {
            LOGIC_CABLE_ITEMS.put(color, registerLogicCableItem(color));
        }
    }

    public static final RegistryObject<Item> LOGIC_BUS_BLOCK_ITEM =
            BLOCK_ITEMS.register("logic_bus", () ->
                    new BlockItem(LOGIC_BUS.get(), new Item.Properties())
            );

    public static final RegistryObject<Item> LOGIC_NOT_GATE_BLOCK_ITEM =
            BLOCK_ITEMS.register("logic_not_gate", () ->
                    new BlockItem(LOGIC_NOT_GATE.get(), new Item.Properties())
            );

    public static final RegistryObject<Item> LOGIC_AND_GATE_BLOCK_ITEM =
            BLOCK_ITEMS.register("logic_and_gate", () ->
                    new BlockItem(LOGIC_AND_GATE.get(), new Item.Properties())
            );

    public static final RegistryObject<Item> LOGIC_NAND_GATE_BLOCK_ITEM =
            BLOCK_ITEMS.register("logic_nand_gate", () ->
                    new BlockItem(LOGIC_NAND_GATE.get(), new Item.Properties())
            );

    public static final RegistryObject<Item> LOGIC_XOR_GATE_BLOCK_ITEM =
            BLOCK_ITEMS.register("logic_xor_gate", () ->
                    new BlockItem(LOGIC_XOR_GATE.get(), new Item.Properties())
            );

    public static final RegistryObject<Item> LOGIC_XNOR_GATE_BLOCK_ITEM =
            BLOCK_ITEMS.register("logic_xnor_gate", () ->
                    new BlockItem(LOGIC_XNOR_GATE.get(), new Item.Properties())
            );

    public static final RegistryObject<Item> LOGIC_CONSTANT_GATE_BLOCK_ITEM =
            BLOCK_ITEMS.register("logic_constant_gate", () ->
                    new BlockItem(LOGIC_CONSTANT_GATE.get(), new Item.Properties())
            );

    public static final RegistryObject<Item> LOGIC_SR_LATCH_BLOCK_ITEM =
            BLOCK_ITEMS.register("logic_sr_latch", () ->
                    new BlockItem(LOGIC_SR_LATCH.get(), new Item.Properties())
            );

    public static final RegistryObject<Item> LOGIC_D_FLIP_FLOP_BLOCK_ITEM =
            BLOCK_ITEMS.register("logic_d_flip_flop", () ->
                    new BlockItem(LOGIC_D_FLIP_FLOP.get(), new Item.Properties())
            );

    public static final RegistryObject<Item> LOGIC_CLOCK_BLOCK_ITEM =
            BLOCK_ITEMS.register("logic_clock", () ->
                    new BlockItem(LOGIC_CLOCK.get(), new Item.Properties())
            );

    public static final RegistryObject<Item> LOGIC_BUFFER_BLOCK_ITEM =
            BLOCK_ITEMS.register("logic_buffer", () ->
                    new BlockItem(LOGIC_BUFFER.get(), new Item.Properties())
            );

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

    private static RegistryObject<Item> registerLogicCableItem(LogicCableColor color) {
        return BLOCK_ITEMS.register(color.getItemRegistryName(), () ->
                new LogicCableBlockItem(
                        LOGIC_CABLE.get(),
                        new Item.Properties(),
                        color
                )
        );
    }

    private static BlockBehaviour.Properties gateProperties() {
        return BlockBehaviour.Properties.of()
                .strength(0.5F)
                .sound(SoundType.METAL)
                .noOcclusion();
    }

    private BlockRegistrar() {
    }
}