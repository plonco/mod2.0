package com.SoloLevelingSystem.storage;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.server.level.ServerLevel;

import java.util.*;

public class PlayerSummons extends SavedData {
    private final Map<UUID, Set<ResourceLocation>> normalSummons = new HashMap<>();
    private final Map<UUID, Set<ResourceLocation>> minibossSummons = new HashMap<>();
    private final Map<UUID, Set<ResourceLocation>> bossSummons = new HashMap<>();

    public static final int MAX_NORMAL_SUMMONS = 5;
    public static final int MAX_MINIBOSS_SUMMONS = 2;
    public static final int MAX_BOSS_SUMMONS = 1;

    public static PlayerSummons get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(
                PlayerSummons::new,
                PlayerSummons::new,
                "solo_leveling_summons"
        );
    }

    public PlayerSummons() {
    }

    public PlayerSummons(CompoundTag tag) {
        loadFromNbt(tag);
    }

    public boolean addSummon(UUID playerId, ResourceLocation entityId, SummonType type) {
        Map<UUID, Set<ResourceLocation>> summons = getSummonsMapForType(type);
        Set<ResourceLocation> playerSummons = summons.computeIfAbsent(playerId, k -> new HashSet<>());

        if (playerSummons.size() < getMaxSummonsForType(type)) {
            playerSummons.add(entityId);
            setDirty();
            return true;
        }
        return false;
    }

    public Set<ResourceLocation> getSummons(UUID playerId, SummonType type) {
        return getSummonsMapForType(type).getOrDefault(playerId, new HashSet<>());
    }

    private Map<UUID, Set<ResourceLocation>> getSummonsMapForType(SummonType type) {
        return switch (type) {
            case NORMAL -> normalSummons;
            case MINIBOSS -> minibossSummons;
            case BOSS -> bossSummons;
        };
    }

    private int getMaxSummonsForType(SummonType type) {
        return switch (type) {
            case NORMAL -> MAX_NORMAL_SUMMONS;
            case MINIBOSS -> MAX_MINIBOSS_SUMMONS;
            case BOSS -> MAX_BOSS_SUMMONS;
        };
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        saveSummonsToNbt(tag, "normal_summons", normalSummons);
        saveSummonsToNbt(tag, "miniboss_summons", minibossSummons);
        saveSummonsToNbt(tag, "boss_summons", bossSummons);
        return tag;
    }

    private void loadFromNbt(CompoundTag tag) {
        loadSummonsFromNbt(tag, "normal_summons", normalSummons);
        loadSummonsFromNbt(tag, "miniboss_summons", minibossSummons);
        loadSummonsFromNbt(tag, "boss_summons", bossSummons);
    }

    private void saveSummonsToNbt(CompoundTag tag, String key, Map<UUID, Set<ResourceLocation>> summons) {
        CompoundTag summonsTag = new CompoundTag();
        summons.forEach((playerId, entities) -> {
            ListTag entityList = new ListTag();
            entities.forEach(entity -> entityList.add(StringTag.valueOf(entity.toString())));
            summonsTag.put(playerId.toString(), entityList);
        });
        tag.put(key, summonsTag);
    }

    private void loadSummonsFromNbt(CompoundTag tag, String key, Map<UUID, Set<ResourceLocation>> summons) {
        summons.clear();
        if (tag.contains(key)) {
            CompoundTag summonsTag = tag.getCompound(key);
            summonsTag.getAllKeys().forEach(playerId -> {
                UUID id = UUID.fromString(playerId);
                Set<ResourceLocation> entities = new HashSet<>();
                ListTag entityList = summonsTag.getList(playerId, 8); // 8 is the NBT type for String
                entityList.forEach(entityTag ->
                        entities.add(new ResourceLocation(entityTag.getAsString())));
                summons.put(id, entities);
            });
        }
    }

    public enum SummonType {
        NORMAL,
        MINIBOSS,
        BOSS
    }
}