package net.oktawia.faststone.client.screens;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.oktawia.faststone.defs.LangDefs;
import net.oktawia.faststone.logic.interfaces.LogicGateConfigurable;
import net.oktawia.faststone.menus.LogicGateConfigMenu;
import net.oktawia.faststone.network.NetworkHandler;
import net.oktawia.faststone.network.SetLogicGateConfigPacket;

public class LogicGateConfigScreen extends AbstractContainerScreen<LogicGateConfigMenu> {

    private EditBox valueBox;

    public LogicGateConfigScreen(
            LogicGateConfigMenu menu,
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

        LogicGateConfigurable configurable = menu.getConfigurable(minecraft.player);

        int value = 1;
        Component label = Component.empty();

        if (configurable != null) {
            value = configurable.getConfigValue();
            label = Component.translatable(configurable.getConfigLabelTranslationKey());
            titleLabelX = 8;
        }

        valueBox = new EditBox(
                font,
                leftPos + 8,
                topPos + 32,
                80,
                20,
                label
        );

        valueBox.setValue(Integer.toString(value));
        valueBox.setFilter(text -> text.matches("\\d*"));
        addRenderableWidget(valueBox);

        addRenderableWidget(Button.builder(
                Component.translatable(LangDefs.SCREEN_LOGIC_GATE_CONFIG_SAVE.getTranslationKey()),
                button -> saveAndClose()
        ).bounds(leftPos + 96, topPos + 31, 72, 22).build());
    }

    private void saveAndClose() {
        int value = 1;

        try {
            value = Integer.parseInt(valueBox.getValue());
        } catch (NumberFormatException ignored) {
        }

        NetworkHandler.sendToServer(new SetLogicGateConfigPacket(menu.getPos(), value));

        if (minecraft != null && minecraft.player != null) {
            minecraft.player.closeContainer();
        }
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
        LogicGateConfigurable configurable = menu.getConfigurable(minecraft.player);

        Component label = Component.empty();

        if (configurable != null) {
            label = Component.translatable(configurable.getConfigLabelTranslationKey());
        }

        graphics.drawString(font, title, 8, 6, 0xFFFFFF, false);
        graphics.drawString(font, label, 8, 21, 0xD0D0D0, false);
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