package net.oktawia.faststone.client.screens;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.oktawia.faststone.defs.LangDefs;
import net.oktawia.faststone.logic.LogicDisplayMode;
import net.oktawia.faststone.menus.LogicDisplayConfigMenu;
import net.oktawia.faststone.network.NetworkHandler;
import net.oktawia.faststone.network.SetLogicDisplayModePacket;

public class LogicDisplayConfigScreen extends AbstractContainerScreen<LogicDisplayConfigMenu> {

    private LogicDisplayMode currentMode = LogicDisplayMode.DIGITAL;
    private Button modeButton;

    public LogicDisplayConfigScreen(
            LogicDisplayConfigMenu menu,
            Inventory inventory,
            Component title
    ) {
        super(menu, inventory, title);

        imageWidth = 176;
        imageHeight = 60;
    }

    @Override
    protected void init() {
        super.init();

        LogicDisplayMode mode = menu.getCurrentMode(minecraft.player);

        if (mode != null) {
            currentMode = mode;
        }

        modeButton = Button.builder(
                modeLabel(currentMode),
                button -> toggleMode()
        ).bounds(leftPos + 8, topPos + 31, 160, 22).build();

        addRenderableWidget(modeButton);
    }

    private void toggleMode() {
        currentMode = currentMode.toggle();
        modeButton.setMessage(modeLabel(currentMode));
        NetworkHandler.sendToServer(new SetLogicDisplayModePacket(menu.getPos(), menu.getSide(), currentMode));
    }

    private Component modeLabel(LogicDisplayMode mode) {
        return mode == LogicDisplayMode.DIGITAL
                ? Component.translatable(LangDefs.SCREEN_LOGIC_DISPLAY_DIGITAL.getTranslationKey())
                : Component.translatable(LangDefs.SCREEN_LOGIC_DISPLAY_ANALOG.getTranslationKey());
    }

    @Override
    protected void renderBg(
            GuiGraphics graphics,
            float partialTick,
            int mouseX,
            int mouseY
    ) {
        graphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, 0xCC202020);
        graphics.fill(leftPos + 1, topPos + 1, leftPos + imageWidth - 1, topPos + imageHeight - 1, 0xCC303030);
    }

    @Override
    protected void renderLabels(
            GuiGraphics graphics,
            int mouseX,
            int mouseY
    ) {
        graphics.drawString(font, title, 8, 6, 0xFFFFFF, false);
        graphics.drawString(
                font,
                Component.translatable(LangDefs.SCREEN_LOGIC_DISPLAY_MODE.getTranslationKey()),
                8,
                21,
                0xD0D0D0,
                false
        );
    }

    @Override
    public void render(
            GuiGraphics graphics,
            int mouseX,
            int mouseY,
            float partialTick
    ) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }
}
