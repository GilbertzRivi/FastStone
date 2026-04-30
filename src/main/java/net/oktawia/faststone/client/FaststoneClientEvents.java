package net.oktawia.faststone.client;

import net.minecraft.client.color.block.BlockColor;
import net.minecraft.client.color.item.ItemColor;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.client.event.RegisterColorHandlersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.oktawia.faststone.Faststone;
import net.oktawia.faststone.blocks.LogicCableBlock;
import net.oktawia.faststone.client.render.LogicCableBlockEntityRenderer;
import net.oktawia.faststone.client.render.LogicGateBlockEntityRenderer;
import net.oktawia.faststone.defs.regs.BlockEntityRegistrar;
import net.oktawia.faststone.defs.regs.BlockRegistrar;
import net.oktawia.faststone.items.LogicCableBlockItem;

@Mod.EventBusSubscriber(
        modid = Faststone.MODID,
        bus = Mod.EventBusSubscriber.Bus.MOD,
        value = Dist.CLIENT
)
public class FaststoneClientEvents {

    public static final ResourceLocation LOGIC_INPUT_PART_MODEL =
            Faststone.makeId("block/part/logic_input_part");

    public static final ResourceLocation LOGIC_OUTPUT_PART_MODEL =
            Faststone.makeId("block/part/logic_output_part");

    @SubscribeEvent
    public static void registerAdditionalModels(ModelEvent.RegisterAdditional event) {
        event.register(LOGIC_INPUT_PART_MODEL);
        event.register(LOGIC_OUTPUT_PART_MODEL);
    }

    @SubscribeEvent
    public static void registerBlockColors(RegisterColorHandlersEvent.Block event) {
        BlockColor logicCableColor = (state, level, pos, tintIndex) -> {
            if (tintIndex != 0) {
                return 0xFFFFFF;
            }

            if (state.hasProperty(LogicCableBlock.COLOR)) {
                return state.getValue(LogicCableBlock.COLOR).getRgb();
            }

            return 0xFFFFFF;
        };

        event.register(logicCableColor, BlockRegistrar.LOGIC_CABLE.get());
    }

    @SubscribeEvent
    public static void registerItemColors(RegisterColorHandlersEvent.Item event) {
        ItemColor logicCableItemColor = (stack, tintIndex) -> {
            if (tintIndex != 0) {
                return 0xFFFFFF;
            }

            if (stack.getItem() instanceof LogicCableBlockItem cableItem) {
                return cableItem.getColor().getRgb();
            }

            return 0xFFFFFF;
        };

        for (var item : BlockRegistrar.LOGIC_CABLE_ITEMS.values()) {
            event.register(logicCableItemColor, item.get());
        }
    }

    @SubscribeEvent
    public static void registerBlockEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(
                BlockEntityRegistrar.LOGIC_CABLE.get(),
                LogicCableBlockEntityRenderer::new
        );

        event.registerBlockEntityRenderer(
                BlockEntityRegistrar.LOGIC_NOT_GATE.get(),
                LogicGateBlockEntityRenderer::new
        );
    }
}