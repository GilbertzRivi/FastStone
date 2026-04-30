package net.oktawia.faststone.defs;

public enum LangDefs {
    MOD_NAME("gui.faststone.mod_name", "Fast Stone");
    private final String key;
    private final String value;

    LangDefs(String key, String value) {
        this.key = key;
        this.value = value;
    }

    public String getTranslationKey() {
        return key;
    }

    public String getEnglishText() {
        return value;
    }
}