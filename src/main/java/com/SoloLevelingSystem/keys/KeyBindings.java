package com.SoloLevelingSystem.keys;

import com.SoloLevelingSystem.SoloLevelingSystem;
import com.SoloLevelingSystem.network.SpawnEntitiesMessage;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.client.settings.KeyModifier;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mojang.blaze3d.platform.InputConstants;

@Mod.EventBusSubscriber(modid = SoloLevelingSystem.MODID, value = Dist.CLIENT)
public class KeyBindings {
    private static final Logger LOGGER = LoggerFactory.getLogger(KeyBindings.class);
    private static final long COOLDOWN = 0; // Cooldown en milisegundos (0 = sin cooldown)
    private static long lastInvocationTime = 0;

    // Definir el keybinding con el constructor correcto
    public static final KeyMapping SUMMON_KEY = new KeyMapping(
            "key." + SoloLevelingSystem.MODID + ".summon", // nombre de la tecla
            KeyConflictContext.IN_GAME,                     // contexto
            InputConstants.Type.KEYSYM,                     // tipo de entrada
            GLFW.GLFW_KEY_R,                               // tecla
            "key.categories." + SoloLevelingSystem.MODID    // categoría
    );

    @Mod.EventBusSubscriber(modid = SoloLevelingSystem.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static class KeyRegister {
        @SubscribeEvent
        public static void registerKeys(RegisterKeyMappingsEvent event) {
            LOGGER.info("Registering key bindings");
            event.register(SUMMON_KEY);
        }
    }

    @SubscribeEvent
    public static void onKeyInput(InputEvent.Key event) {
        if (SUMMON_KEY.consumeClick()) {
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastInvocationTime >= COOLDOWN) {
                LOGGER.debug("Summon key pressed, sending message to server");
                SoloLevelingSystem.CHANNEL.sendToServer(new SpawnEntitiesMessage());
                lastInvocationTime = currentTime;
            } else {
                if (Minecraft.getInstance().player != null) {
                    Minecraft.getInstance().player.sendSystemMessage(
                            Component.literal("Invocaciones no disponibles")
                    );
                }
                LOGGER.debug("Cooldown not finished. Time remaining: {} ms",
                        COOLDOWN - (currentTime - lastInvocationTime));
            }
        }
    }

    public static boolean isSummonKeyPressed() {
        return SUMMON_KEY.isDown();
    }

    public static long getCooldownRemaining() {
        long currentTime = System.currentTimeMillis();
        long timeElapsed = currentTime - lastInvocationTime;
        return Math.max(0, COOLDOWN - timeElapsed);
    }
}