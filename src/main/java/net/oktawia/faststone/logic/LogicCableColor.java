package net.oktawia.faststone.logic;

import net.minecraft.util.StringRepresentable;

public enum LogicCableColor implements StringRepresentable {
    WHITE("white", 0xFFFFFF),
    ORANGE("orange", 0xFF9933),
    MAGENTA("magenta", 0xFF55FF),
    LIGHT_BLUE("light_blue", 0x55AAFF),
    YELLOW("yellow", 0xFFFF33),
    LIME("lime", 0x55FF55),
    PINK("pink", 0xFF77AA),
    GRAY("gray", 0x555555),
    LIGHT_GRAY("light_gray", 0xAAAAAA),
    CYAN("cyan", 0x33FFFF),
    PURPLE("purple", 0xAA55FF),
    BLUE("blue", 0x3377FF),
    BROWN("brown", 0x8B5A2B),
    GREEN("green", 0x33AA33),
    RED("red", 0xFF3333),
    BLACK("black", 0x111111),
    COLORLESS("colorless", 0xB0B0B0);

    private final String name;
    private final int rgb;

    LogicCableColor(String name, int rgb) {
        this.name = name;
        this.rgb = rgb;
    }

    public int getRgb() {
        return rgb;
    }

    public String getItemRegistryName() {
        return "logic_cable_" + name;
    }

    @Override
    public String getSerializedName() {
        return name;
    }

    public boolean isColorless() {
        return this == COLORLESS;
    }

    public boolean canConnectTo(LogicCableColor other) {
        return other != null
                && (this == other || this.isColorless() || other.isColorless());
    }

    public static boolean areCompatible(LogicCableColor a, LogicCableColor b) {
        return a != null && a.canConnectTo(b);
    }

    public static LogicCableColor byName(String name) {
        for (LogicCableColor color : values()) {
            if (color.getSerializedName().equals(name)) {
                return color;
            }
        }

        return RED;
    }
}