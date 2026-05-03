package me.testblocks.network;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class VisualMappingManager {
    private static final Map<Integer, Integer> customToVanilla = new ConcurrentHashMap<>();

    public static void registerMapping(int customId, int vanillaId) {
        customToVanilla.put(customId, vanillaId);
    }

    public static int getMappedId(int customId) {
        return customToVanilla.getOrDefault(customId, customId);
    }

    public static Map<Integer, Integer> getMappings() {
        return customToVanilla;
    }
}
