package net.oktawia.faststone.logic;

public enum LogicPortMode {
    NONE,
    INPUT,
    OUTPUT,
    BOTH;

    public boolean readsFromCable() {
        return this == INPUT || this == BOTH;
    }

    public boolean writesToCable() {
        return this == OUTPUT || this == BOTH;
    }
}