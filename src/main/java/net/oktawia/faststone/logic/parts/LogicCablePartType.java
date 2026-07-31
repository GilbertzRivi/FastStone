package net.oktawia.faststone.logic.parts;

import net.minecraft.util.StringRepresentable;

public enum LogicCablePartType implements StringRepresentable {
    NONE("none"),
    INPUT("input"),
    OUTPUT("output"),
    DISPLAY("display");

    private final String name;

    LogicCablePartType(String name) {
        this.name = name;
    }

    @Override
    public String getSerializedName() {
        return name;
    }

    public static LogicCablePartType byName(String name) {
        for (LogicCablePartType type : values()) {
            if (type.name.equals(name)) {
                return type;
            }
        }

        return NONE;
    }
}