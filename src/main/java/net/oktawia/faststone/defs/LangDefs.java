package net.oktawia.faststone.defs;

public enum LangDefs {
    MOD_NAME("gui.faststone.mod_name", "Fast Stone"),
    GATE_PORT_NONE("tooltip.faststone.gate_port.none", "None"),
    GATE_PORT_INPUT("tooltip.faststone.gate_port.input", "Input"),
    GATE_PORT_OUTPUT("tooltip.faststone.gate_port.output", "Output"),
    GATE_PORT_SET("tooltip.faststone.gate_port.set", "Set"),
    GATE_PORT_RESET("tooltip.faststone.gate_port.reset", "Reset"),
    GATE_PORT_Q("tooltip.faststone.gate_port.q", "Q"),
    GATE_PORT_NOT_Q("tooltip.faststone.gate_port.not_q", "!Q"),
    GATE_PORT_D("tooltip.faststone.gate_port.d", "D"),
    GATE_PORT_CLOCK("tooltip.faststone.gate_port.clock", "Clock"),
    GATE_PORT_CLOCK_A("tooltip.faststone.gate_port.clock_a", "Clock A"),
    GATE_PORT_CLOCK_B("tooltip.faststone.gate_port.clock_b", "Clock B"),
    SCREEN_LOGIC_CLOCK_TITLE("screen.faststone.logic_clock.title", "Clock"),
    SCREEN_LOGIC_CLOCK_INTERVAL("screen.faststone.logic_clock.interval", "Toggle every network ticks"),
    SCREEN_LOGIC_BUFFER_TITLE("screen.faststone.logic_buffer.title", "Buffer"),
    SCREEN_LOGIC_BUFFER_DELAY("screen.faststone.logic_buffer.delay", "Delay in network ticks"),
    SCREEN_LOGIC_GATE_CONFIG_SAVE("screen.faststone.logic_gate_config.save", "Save"),
    SCREEN_LOGIC_DISPLAY_TITLE("screen.faststone.logic_display.title", "Display"),
    SCREEN_LOGIC_DISPLAY_MODE("screen.faststone.logic_display.mode", "Mode"),
    SCREEN_LOGIC_DISPLAY_DIGITAL("screen.faststone.logic_display.digital", "Digital"),
    SCREEN_LOGIC_DISPLAY_ANALOG("screen.faststone.logic_display.analog", "Analog"),
    TOOLTIP_LOGIC_CABLE_COLOR("tooltip.faststone.logic_cable.color", "Color: %s"),
    TOOLTIP_LOGIC_CABLE_COLOR_WHITE("tooltip.faststone.logic_cable.color.white", "White"),
    TOOLTIP_LOGIC_CABLE_COLOR_ORANGE("tooltip.faststone.logic_cable.color.orange", "Orange"),
    TOOLTIP_LOGIC_CABLE_COLOR_MAGENTA("tooltip.faststone.logic_cable.color.magenta", "Magenta"),
    TOOLTIP_LOGIC_CABLE_COLOR_LIGHT_BLUE("tooltip.faststone.logic_cable.color.light_blue", "Light Blue"),
    TOOLTIP_LOGIC_CABLE_COLOR_YELLOW("tooltip.faststone.logic_cable.color.yellow", "Yellow"),
    TOOLTIP_LOGIC_CABLE_COLOR_LIME("tooltip.faststone.logic_cable.color.lime", "Lime"),
    TOOLTIP_LOGIC_CABLE_COLOR_PINK("tooltip.faststone.logic_cable.color.pink", "Pink"),
    TOOLTIP_LOGIC_CABLE_COLOR_GRAY("tooltip.faststone.logic_cable.color.gray", "Gray"),
    TOOLTIP_LOGIC_CABLE_COLOR_LIGHT_GRAY("tooltip.faststone.logic_cable.color.light_gray", "Light Gray"),
    TOOLTIP_LOGIC_CABLE_COLOR_CYAN("tooltip.faststone.logic_cable.color.cyan", "Cyan"),
    TOOLTIP_LOGIC_CABLE_COLOR_PURPLE("tooltip.faststone.logic_cable.color.purple", "Purple"),
    TOOLTIP_LOGIC_CABLE_COLOR_BLUE("tooltip.faststone.logic_cable.color.blue", "Blue"),
    TOOLTIP_LOGIC_CABLE_COLOR_BROWN("tooltip.faststone.logic_cable.color.brown", "Brown"),
    TOOLTIP_LOGIC_CABLE_COLOR_GREEN("tooltip.faststone.logic_cable.color.green", "Green"),
    TOOLTIP_LOGIC_CABLE_COLOR_RED("tooltip.faststone.logic_cable.color.red", "Red"),
    TOOLTIP_LOGIC_CABLE_COLOR_BLACK("tooltip.faststone.logic_cable.color.black", "Black"),
    TOOLTIP_LOGIC_CABLE_COLOR_COLORLESS("tooltip.faststone.logic_cable.color.colorless", "Colorless");

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