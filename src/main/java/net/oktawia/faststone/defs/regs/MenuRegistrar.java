package net.oktawia.faststone.defs.regs;

import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.oktawia.faststone.Faststone;
import net.oktawia.faststone.menus.LogicDisplayConfigMenu;
import net.oktawia.faststone.menus.LogicGateConfigMenu;

public class MenuRegistrar {

    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(ForgeRegistries.MENU_TYPES, Faststone.MODID);

    public static final RegistryObject<MenuType<LogicGateConfigMenu>> LOGIC_GATE_CONFIG =
            MENUS.register("logic_gate_config", () ->
                    IForgeMenuType.create(LogicGateConfigMenu::new)
            );

    public static final RegistryObject<MenuType<LogicDisplayConfigMenu>> LOGIC_DISPLAY_CONFIG =
            MENUS.register("logic_display_config", () ->
                    IForgeMenuType.create(LogicDisplayConfigMenu::new)
            );

    private MenuRegistrar() {
    }
}