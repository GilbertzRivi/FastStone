package net.oktawia.faststone.datagen;

import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.level.block.Block;
import net.oktawia.faststone.defs.regs.BlockRegistrar;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

public class FastBlockLootTables extends BlockLootSubProvider {
    public FastBlockLootTables() {
        super(Set.of(), FeatureFlags.REGISTRY.allFlags());
    }

    @Override
    protected void generate() {
        for (var block : BlockRegistrar.getBlocks()){
            this.dropSelf(block);
        }
    }

    @Override
    protected @NotNull Iterable<Block> getKnownBlocks(){
        return BlockRegistrar.getBlocks().stream()::iterator;
    }

}
