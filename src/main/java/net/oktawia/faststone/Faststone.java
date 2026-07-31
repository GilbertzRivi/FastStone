package net.oktawia.faststone;

import com.mojang.logging.LogUtils;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.RegisterEvent;
import net.oktawia.faststone.defs.regs.*;
import net.oktawia.faststone.network.NetworkHandler;
import org.slf4j.Logger;

@Mod(Faststone.MODID)
public class Faststone {
    public static final String MODID = "faststone";
    private static final Logger LOGGER = LogUtils.getLogger();

    public Faststone() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        modEventBus.addListener(this::commonSetup);

        ItemRegistrar.ITEMS.register(modEventBus);
        BlockRegistrar.BLOCKS.register(modEventBus);
        BlockRegistrar.BLOCK_ITEMS.register(modEventBus);
        BlockEntityRegistrar.BLOCK_ENTITIES.register(modEventBus);
        MenuRegistrar.MENUS.register(modEventBus);

        modEventBus.addListener(this::registerCreativeTab);

        MinecraftForge.EVENT_BUS.register(this);
    }

    public static ResourceLocation makeId(String path) {
        return ResourceLocation.fromNamespaceAndPath(Faststone.MODID, path);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        LOGGER.info("FastStone loading...");
        event.enqueueWork(() -> {
            NetworkHandler.registerMessages();
        });
    }


    private void registerCreativeTab(final RegisterEvent evt) {
        if (evt.getRegistryKey().equals(Registries.CREATIVE_MODE_TAB)) {
            evt.register(
                    Registries.CREATIVE_MODE_TAB,
                    CreativeTabRegistrar.ID,
                    () -> CreativeTabRegistrar.TAB
            );
        }
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
    }

    @Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModEvents {

        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {
            event.enqueueWork(ScreenRegistrar::register);
        }
    }
}
