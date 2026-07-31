package net.oktawia.faststone.defs.regs;

import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.oktawia.faststone.Faststone;
import net.oktawia.faststone.entities.*;
import net.oktawia.faststone.entities.gates.*;

public class BlockEntityRegistrar {

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, Faststone.MODID);

    public static final RegistryObject<BlockEntityType<LogicCableBlockEntity>> LOGIC_CABLE =
            BLOCK_ENTITIES.register("logic_cable", () ->
                    BlockEntityType.Builder.of(
                            LogicCableBlockEntity::new,
                            BlockRegistrar.LOGIC_CABLE.get()
                    ).build(null)
            );

    public static final RegistryObject<BlockEntityType<LogicNotGateBlockEntity>> LOGIC_NOT_GATE =
            BLOCK_ENTITIES.register("logic_not_gate", () ->
                    BlockEntityType.Builder.of(
                            LogicNotGateBlockEntity::new,
                            BlockRegistrar.LOGIC_NOT_GATE.get()
                    ).build(null)
            );

    public static final RegistryObject<BlockEntityType<LogicAndGateBlockEntity>> LOGIC_AND_GATE =
            BLOCK_ENTITIES.register("logic_and_gate", () ->
                    BlockEntityType.Builder.of(
                            LogicAndGateBlockEntity::new,
                            BlockRegistrar.LOGIC_AND_GATE.get()
                    ).build(null)
            );

    public static final RegistryObject<BlockEntityType<LogicXorGateBlockEntity>> LOGIC_XOR_GATE =
            BLOCK_ENTITIES.register("logic_xor_gate", () ->
                    BlockEntityType.Builder.of(
                            LogicXorGateBlockEntity::new,
                            BlockRegistrar.LOGIC_XOR_GATE.get()
                    ).build(null)
            );

    public static final RegistryObject<BlockEntityType<LogicXnorGateBlockEntity>> LOGIC_XNOR_GATE =
            BLOCK_ENTITIES.register("logic_xnor_gate", () ->
                    BlockEntityType.Builder.of(
                            LogicXnorGateBlockEntity::new,
                            BlockRegistrar.LOGIC_XNOR_GATE.get()
                    ).build(null)
            );

    public static final RegistryObject<BlockEntityType<LogicNandGateBlockEntity>> LOGIC_NAND_GATE =
            BLOCK_ENTITIES.register("logic_nand_gate", () ->
                    BlockEntityType.Builder.of(
                            LogicNandGateBlockEntity::new,
                            BlockRegistrar.LOGIC_NAND_GATE.get()
                    ).build(null)
            );

    public static final RegistryObject<BlockEntityType<LogicConstantGateBlockEntity>> LOGIC_CONSTANT_GATE =
            BLOCK_ENTITIES.register("logic_constant_gate", () ->
                    BlockEntityType.Builder.of(
                            LogicConstantGateBlockEntity::new,
                            BlockRegistrar.LOGIC_CONSTANT_GATE.get()
                    ).build(null)
            );

    public static final RegistryObject<BlockEntityType<LogicSrLatchBlockEntity>> LOGIC_SR_LATCH =
            BLOCK_ENTITIES.register("logic_sr_latch", () ->
                    BlockEntityType.Builder.of(
                            LogicSrLatchBlockEntity::new,
                            BlockRegistrar.LOGIC_SR_LATCH.get()
                    ).build(null)
            );

    public static final RegistryObject<BlockEntityType<LogicDFlipFlopBlockEntity>> LOGIC_D_FLIP_FLOP =
            BLOCK_ENTITIES.register("logic_d_flip_flop", () ->
                    BlockEntityType.Builder.of(
                            LogicDFlipFlopBlockEntity::new,
                            BlockRegistrar.LOGIC_D_FLIP_FLOP.get()
                    ).build(null)
            );

    public static final RegistryObject<BlockEntityType<LogicClockBlockEntity>> LOGIC_CLOCK =
            BLOCK_ENTITIES.register("logic_clock", () ->
                    BlockEntityType.Builder.of(
                            LogicClockBlockEntity::new,
                            BlockRegistrar.LOGIC_CLOCK.get()
                    ).build(null)
            );

    public static final RegistryObject<BlockEntityType<LogicBufferBlockEntity>> LOGIC_BUFFER =
            BLOCK_ENTITIES.register("logic_buffer", () ->
                    BlockEntityType.Builder.of(
                            LogicBufferBlockEntity::new,
                            BlockRegistrar.LOGIC_BUFFER.get()
                    ).build(null)
            );

    private BlockEntityRegistrar() {
    }
}