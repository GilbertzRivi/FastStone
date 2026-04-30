package net.oktawia.faststone.defs.regs;

import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.oktawia.faststone.Faststone;

public class MenuRegistrar {

    public static final DeferredRegister<MenuType<?>> MENU_TYPES =
            DeferredRegister.create(ForgeRegistries.MENU_TYPES, Faststone.MODID);

    private static <T extends AbstractContainerMenu> RegistryObject<MenuType<T>> register(
            String id,
            MenuType.MenuSupplier<T> factory
    ) {
        return MENU_TYPES.register(
                id,
                () -> new MenuType<>(factory, FeatureFlags.DEFAULT_FLAGS)
        );
    }

    private MenuRegistrar() {
    }
}