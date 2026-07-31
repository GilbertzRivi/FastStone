package net.oktawia.faststone.logic;

public enum LogicDisplayMode {
    DIGITAL("digital"),
    ANALOG("analog");

    private final String name;

    LogicDisplayMode(String name) {
        this.name = name;
    }

    public String getSerializedName() {
        return name;
    }

    public LogicDisplayMode toggle() {
        return this == DIGITAL ? ANALOG : DIGITAL;
    }

    public static LogicDisplayMode byName(String name) {
        for (LogicDisplayMode mode : values()) {
            if (mode.name.equals(name)) {
                return mode;
            }
        }
        return DIGITAL;
    }
}
