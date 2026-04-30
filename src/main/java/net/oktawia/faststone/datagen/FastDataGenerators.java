package net.oktawia.faststone.datagen;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.minecraftforge.common.data.ExistingFileHelper;
import net.minecraftforge.data.event.GatherDataEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.oktawia.faststone.Faststone;

import java.util.concurrent.CompletableFuture;

@Mod.EventBusSubscriber(modid = Faststone.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class FastDataGenerators {
    @SubscribeEvent
    public static void gatherData(GatherDataEvent event) {
        DataGenerator generator = event.getGenerator();
        PackOutput packOutput = generator.getPackOutput();
        ExistingFileHelper existingFileHelper = event.getExistingFileHelper();
        CompletableFuture<HolderLookup.Provider> lookupProvider = event.getLookupProvider();

        generator.addProvider(event.includeServer(), new FastRecipeProvider(packOutput));
        generator.addProvider(event.includeServer(), FastLootTableProvider.create(packOutput));

        generator.addProvider(event.includeClient(), new FastBlockStateProvider(packOutput, existingFileHelper));
        generator.addProvider(event.includeClient(), new FastItemModelProvider(packOutput, existingFileHelper));

        generator.addProvider(event.includeClient(), new FastLangProvider(packOutput, "en_us"));

        FastBlockTagGenerator blockTagGenerator = generator.addProvider(event.includeServer(),
                new FastBlockTagGenerator(packOutput, lookupProvider, existingFileHelper));
        generator.addProvider(event.includeServer(), new FastItemTagGenerator(packOutput, lookupProvider, blockTagGenerator.contentsGetter(), existingFileHelper));
    }
}
