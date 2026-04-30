package net.oktawia.faststone.defs.regs;

import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.oktawia.faststone.Faststone;
import net.oktawia.faststone.entities.LogicCableBlockEntity;
import net.oktawia.faststone.entities.gates.LogicNotGateBlockEntity;

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

    private BlockEntityRegistrar() {
    }
}