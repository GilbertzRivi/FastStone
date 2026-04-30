package net.oktawia.faststone.logic;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.oktawia.faststone.Faststone;
import net.oktawia.faststone.logic.network.LogicNetworkClock;

@Mod.EventBusSubscriber(modid = Faststone.MODID)
public class FaststoneCommands {

    @SubscribeEvent
    public static void registerCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(
                Commands.literal("faststone")
                        .requires(source -> source.hasPermission(2))

                        .then(Commands.literal("tickrate")
                                .executes(context -> showTickrate(context.getSource()))

                                .then(Commands.argument(
                                                "tickrate",
                                                IntegerArgumentType.integer(
                                                        LogicNetworkClock.MIN_NETWORK_TICKS_PER_GAME_TICK,
                                                        LogicNetworkClock.MAX_NETWORK_TICKS_PER_GAME_TICK
                                                )
                                        )
                                        .executes(context -> {
                                            int requestedTickrate = IntegerArgumentType.getInteger(
                                                    context,
                                                    "tickrate"
                                            );

                                            int actualTickrate = LogicNetworkClock.setNetworkTicksPerGameTick(
                                                    requestedTickrate
                                            );

                                            context.getSource().sendSuccess(
                                                    () -> Component.literal(
                                                            "Fast Stone network tickrate set to "
                                                                    + actualTickrate
                                                                    + " network ticks/game tick"
                                                    ),
                                                    true
                                            );

                                            return actualTickrate;
                                        })
                                )
                        )
        );
    }

    private static int showTickrate(net.minecraft.commands.CommandSourceStack source) {
        int tickrate = LogicNetworkClock.getNetworkTicksPerGameTick();

        source.sendSuccess(
                () -> Component.literal(
                        "Fast Stone network tickrate is "
                                + tickrate
                                + " network ticks/game tick"
                ),
                false
        );

        return tickrate;
    }
}