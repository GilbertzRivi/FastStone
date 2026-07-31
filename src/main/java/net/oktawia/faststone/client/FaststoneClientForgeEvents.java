package net.oktawia.faststone.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.oktawia.faststone.Faststone;
import net.oktawia.faststone.blocks.gates.LogicGateBlock;
import net.oktawia.faststone.entities.gates.LogicGateBlockEntity;

@Mod.EventBusSubscriber(
        modid = Faststone.MODID,
        bus = Mod.EventBusSubscriber.Bus.FORGE,
        value = Dist.CLIENT
)
public class FaststoneClientForgeEvents {

    @SubscribeEvent
    public static void renderGatePortTooltip(RenderGuiOverlayEvent.Post event) {
        if (!event.getOverlay().id().equals(VanillaGuiOverlay.CROSSHAIR.id())) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();

        if (minecraft.options.hideGui) {
            return;
        }

        if (minecraft.player == null || minecraft.level == null) {
            return;
        }

        if (!(minecraft.hitResult instanceof BlockHitResult hit)) {
            return;
        }

        BlockPos pos = hit.getBlockPos();

        if (!(minecraft.level.getBlockEntity(pos) instanceof LogicGateBlockEntity gate)) {
            return;
        }

        if (!(minecraft.level.getBlockState(pos).getBlock() instanceof LogicGateBlock)) {
            return;
        }

        Direction side = LogicGateBlock.getClickedVisiblePortSide(
                gate,
                minecraft.level,
                pos,
                hit.getLocation()
        );

        if (side == null) {
            return;
        }

        Component text = Component.translatable(gate.getPortDisplayTranslationKey(side));

        GuiGraphics graphics = event.getGuiGraphics();

        int x = graphics.guiWidth() / 2 + 10;
        int y = graphics.guiHeight() / 2 - 12;

        graphics.drawString(
                minecraft.font,
                text,
                x + 1,
                y + 1,
                0xAA000000,
                false
        );

        graphics.drawString(
                minecraft.font,
                text,
                x,
                y,
                0xFFFFFFFF,
                false
        );
    }

    private FaststoneClientForgeEvents() {
    }
}