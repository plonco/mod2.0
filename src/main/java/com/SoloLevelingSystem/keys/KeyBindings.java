package com.SoloLevelingSystem.keys;

import com.SoloLevelingSystem.SoloLevelingSystem;
import com.SoloLevelingSystem.network.SpawnEntitiesMessage;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = SoloLevelingSystem.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class KeyBindings {
    private static KeyMapping spawnKey;

    @SubscribeEvent
    public static void registerKeys(RegisterKeyMappingsEvent event) {
        spawnKey = new KeyMapping(
                "key.solo_leveling_system.spawn",
                org.lwjgl.glfw.GLFW.GLFW_KEY_G,
                "key.categories.solo_leveling_system"
        );
        event.register(spawnKey);
    }

    @Mod.EventBusSubscriber(modid = SoloLevelingSystem.MODID)
    public static class KeyEventListener {
        @SubscribeEvent
        public static void onKeyInput(InputEvent.Key event) {
            if (spawnKey.consumeClick()) {
                // Usar el método getChannel() en lugar de acceder directamente a CHANNEL
                SoloLevelingSystem.getChannel().sendToServer(new SpawnEntitiesMessage());
            }
        }
    }
}