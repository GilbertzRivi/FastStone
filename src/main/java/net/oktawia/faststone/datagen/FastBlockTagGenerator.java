package net.oktawia.faststone.datagen;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.tags.BlockTags;
import net.minecraftforge.common.data.BlockTagsProvider;
import net.minecraftforge.common.data.ExistingFileHelper;
import net.oktawia.faststone.Faststone;
import net.oktawia.faststone.defs.regs.BlockRegistrar;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CompletableFuture;

public class FastBlockTagGenerator extends BlockTagsProvider {
    public FastBlockTagGenerator(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider, @Nullable ExistingFileHelper existingFileHelper) {
        super(output, lookupProvider, Faststone.MODID, existingFileHelper);
    }

    @Override
    protected void addTags(HolderLookup.Provider pProvider) {
        for (var block : BlockRegistrar.getBlocks()){
            this.tag(BlockTags.NEEDS_IRON_TOOL)
                    .add(block);
            this.tag(BlockTags.MINEABLE_WITH_PICKAXE)
                    .add(block);
        }
    }
}
