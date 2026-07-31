package net.oktawia.faststone.logic;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.FloatArgumentType;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.oktawia.faststone.Faststone;
import net.oktawia.faststone.client.render.LogicCableBlockEntityRenderer;
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
                                                FloatArgumentType.floatArg(
                                                        LogicNetworkClock.MIN_NETWORK_TICK_RATE,
                                                        LogicNetworkClock.MAX_NETWORK_TICK_RATE
                                                )
                                        )
                                        .executes(context -> {
                                            float requestedTickrate = FloatArgumentType.getFloat(
                                                    context,
                                                    "tickrate"
                                            );

                                            float actualTickrate = LogicNetworkClock.setNetworkTickRate(
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

                                            return (int) actualTickrate;
                                        })
                                )
                        )
        );
    }

    @Mod.EventBusSubscriber(modid = Faststone.MODID, value = Dist.CLIENT)
    public static class Client {

        @SubscribeEvent
        public static void registerClientCommands(RegisterClientCommandsEvent event) {
            event.getDispatcher().register(
                    Commands.literal("faststone")
                            .then(Commands.literal("cablerender")
                                    .executes(ctx -> {
                                        boolean nowEnabled = !LogicCableBlockEntityRenderer.cableGlowEnabled;
                                        LogicCableBlockEntityRenderer.cableGlowEnabled = nowEnabled;
                                        Minecraft.getInstance().player.sendSystemMessage(
                                                Component.literal("Cable glow render: " + (nowEnabled ? "on" : "off"))
                                        );
                                        return Command.SINGLE_SUCCESS;
                                    })
                                    .then(Commands.literal("on")
                                            .executes(ctx -> {
                                                LogicCableBlockEntityRenderer.cableGlowEnabled = true;
                                                Minecraft.getInstance().player.sendSystemMessage(
                                                        Component.literal("Cable glow render: on")
                                                );
                                                return Command.SINGLE_SUCCESS;
                                            })
                                    )
                                    .then(Commands.literal("off")
                                            .executes(ctx -> {
                                                LogicCableBlockEntityRenderer.cableGlowEnabled = false;
                                                Minecraft.getInstance().player.sendSystemMessage(
                                                        Component.literal("Cable glow render: off")
                                                );
                                                return Command.SINGLE_SUCCESS;
                                            })
                                    )
                            )
            );
        }

        private Client() {
        }
    }

    private static int showTickrate(net.minecraft.commands.CommandSourceStack source) {
        float tickrate = LogicNetworkClock.getNetworkTickRate();

        source.sendSuccess(
                () -> Component.literal(
                        "Fast Stone network tickrate is "
                                + tickrate
                                + " network ticks/game tick"
                ),
                false
        );

        return (int) tickrate;
    }
}