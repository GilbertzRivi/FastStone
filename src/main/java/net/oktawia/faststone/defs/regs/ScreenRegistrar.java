package net.oktawia.faststone.defs.regs;

import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.oktawia.faststone.client.screens.LogicDisplayConfigScreen;
import net.oktawia.faststone.client.screens.LogicGateConfigScreen;

@OnlyIn(Dist.CLIENT)
public final class ScreenRegistrar {

    public static void register() {
        MenuScreens.register(
                MenuRegistrar.LOGIC_GATE_CONFIG.get(),
                LogicGateConfigScreen::new
        );

        MenuScreens.register(
                MenuRegistrar.LOGIC_DISPLAY_CONFIG.get(),
                LogicDisplayConfigScreen::new
        );
    }

    private ScreenRegistrar() {
    }
}