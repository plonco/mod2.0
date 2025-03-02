package com.SoloLevelingSystem.keys;

import com.SoloLevelingSystem.SoloLevelingSystem;
import com.SoloLevelingSystem.network.SpawnEntitiesMessage;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.MutableComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;
import net.minecraft.network.chat.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod.EventBusSubscriber(modid = SoloLevelingSystem.MODID, value = Dist.CLIENT)
public class KeyBindings {
    private static final Logger LOGGER = LoggerFactory.getLogger(KeyBindings.class);
    private static final long COOLDOWN = 0; //3 * 60 * 1000; // 3 minutes in milliseconds (configurable)
    private static long lastInvocationTime = 0;
    private static final int KEY_INVOKE = GLFW.GLFW_KEY_R; //Configurable Key.

    @SubscribeEvent
    public static void onKeyInput(InputEvent.Key event) {
        if (event.getKey() == KEY_INVOKE && event.getAction() == GLFW.GLFW_PRESS) {
            long currentTime = System.currentTimeMillis();
            long timeElapsed = currentTime - lastInvocationTime;

            if (timeElapsed >= COOLDOWN) {
                SoloLevelingSystem.CHANNEL.sendToServer(new SpawnEntitiesMessage());
                lastInvocationTime = currentTime;
                Minecraft.getInstance().player.sendSystemMessage(Component.literal("Invocando entidades...")); // Retroalimentación
            } else {
                long timeLeft = (COOLDOWN - timeElapsed) / 1000; // Tiempo restante en segundos
                MutableComponent message = Component.literal("Invocaciones no disponibles. Tiempo restante: " + timeLeft + " segundos.");
                Minecraft.getInstance().player.sendSystemMessage(message);
                LOGGER.debug("Cooldown not finished");
            }
        }
    }
}