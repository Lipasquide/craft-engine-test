package me.testblocks.network;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class VisualMappingManager {
    private static final Map<Integer, Integer> global = new ConcurrentHashMap<>();
    private static final Map<UUID, Map<Integer, Integer>> perPlayer = new ConcurrentHashMap<>();

    public static void register(int custom, int vanilla) {
        global.put(custom, vanilla);
    }

    public static int get(UUID player, int id) {
        Map<Integer, Integer> map = perPlayer.get(player);
        if (map != null && map.containsKey(id)) return map.get(id);
        return global.getOrDefault(id, id);
    }

    public static void registerForPlayer(UUID uuid, int custom, int vanilla) {
        perPlayer.computeIfAbsent(uuid, k -> new ConcurrentHashMap<>())
                 .put(custom, vanilla);
    }

    public static void removePlayer(UUID uuid) {
        perPlayer.remove(uuid);
    }
}
