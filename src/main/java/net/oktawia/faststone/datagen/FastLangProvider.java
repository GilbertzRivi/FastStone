package net.oktawia.faststone.datagen;

import net.minecraft.data.PackOutput;
import net.minecraftforge.common.data.LanguageProvider;
import net.minecraftforge.registries.ForgeRegistries;
import net.oktawia.faststone.Faststone;
import net.oktawia.faststone.defs.regs.BlockRegistrar;
import net.oktawia.faststone.defs.regs.ItemRegistrar;
import net.oktawia.faststone.defs.LangDefs;

public class FastLangProvider extends LanguageProvider {
    public FastLangProvider(PackOutput output, String locale) {
        super(output, Faststone.MODID, locale);
    }

    @Override
    protected void addTranslations() {
        for (var item : ItemRegistrar.getItems()){
            this.add(item.getDescriptionId(), toTitle(ForgeRegistries.ITEMS.getKey(item).getPath()));
        }
        for (var block : BlockRegistrar.getBlocks()){
            this.add(block.getDescriptionId(), toTitle(ForgeRegistries.BLOCKS.getKey(block).getPath()));
        }
        for (var entry : LangDefs.values()) {
            this.add(entry.getTranslationKey(), entry.getEnglishText());
        }
    }

    private static String toTitle(String id) {
        StringBuilder out = new StringBuilder();

        for (String part : id.split("_")) {
            if (part.isEmpty()) continue;

            if (part.chars().anyMatch(Character::isDigit)) {
                out.append(part.toUpperCase());
            } else {
                out.append(Character.toUpperCase(part.charAt(0)))
                        .append(part.substring(1).toLowerCase());
            }
            out.append(' ');
        }
        return out.toString().trim();
    }
}
