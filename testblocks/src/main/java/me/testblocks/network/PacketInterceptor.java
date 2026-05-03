package me.testblocks.network;

import io.netty.channel.*;
import org.bukkit.entity.Player;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.UUID;

public class PacketInterceptor extends ChannelDuplexHandler {

    private final Player player;

    public PacketInterceptor(Player player) {
        this.player = player;
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object packet, ChannelPromise promise) throws Exception {
        String packetName = packet.getClass().getSimpleName();

        if (packetName.equals("ClientboundBlockUpdatePacket")) {
            remapBlockUpdatePacket(packet, player.getUniqueId());
        }
        else if (packetName.equals("ClientboundLevelChunkWithLightPacket")) {
            remapChunkPacket(packet, player.getUniqueId());
        }

        super.write(ctx, packet, promise);
    }

    private void remapBlockUpdatePacket(Object packet, UUID uuid) {
        try {
            Field blockStateField = packet.getClass().getDeclaredField("blockState");
            blockStateField.setAccessible(true);
            Object blockState = blockStateField.get(packet);

            Class<?> blockClass = Class.forName("net.minecraft.world.level.block.Block");
            Method getIdMethod = blockClass.getMethod("getId", Class.forName("net.minecraft.world.level.block.state.BlockState"));
            int id = (int) getIdMethod.invoke(null, blockState);

            int mappedId = VisualMappingManager.get(uuid, id);
            if (mappedId != id) {
                Method stateByIdMethod = blockClass.getMethod("stateById", int.class);
                Object remappedState = stateByIdMethod.invoke(null, mappedId);
                blockStateField.set(packet, remappedState);
            }
        } catch (Exception ignore) {}
    }

    private void remapChunkPacket(Object packet, UUID uuid) {
        try {
            Field chunkDataField = packet.getClass().getDeclaredField("chunkData");
            chunkDataField.setAccessible(true);
            Object chunkData = chunkDataField.get(packet);

            Field sectionsField = chunkData.getClass().getDeclaredField("sections");
            sectionsField.setAccessible(true);
            Object[] sections = (Object[]) sectionsField.get(chunkData);

            if (sections == null) return;

            Class<?> blockClass = Class.forName("net.minecraft.world.level.block.Block");
            Method getId = blockClass.getMethod("getId", Class.forName("net.minecraft.world.level.block.state.BlockState"));
            Method stateById = blockClass.getMethod("stateById", int.class);

            for (Object section : sections) {
                if (section == null) continue;

                Field statesField = section.getClass().getDeclaredField("states");
                statesField.setAccessible(true);
                Object palette = statesField.get(section);

                Method getSize = palette.getClass().getMethod("getSize");
                Method get = palette.getClass().getMethod("get", int.class);
                Method set = palette.getClass().getMethod("set", int.class, Object.class);

                int size = (int) getSize.invoke(palette);

                for (int i = 0; i < size; i++) {
                    Object state = get.invoke(palette, i);
                    int oldId = (int) getId.invoke(null, state);
                    int newId = VisualMappingManager.get(uuid, oldId);

                    if (oldId != newId) {
                        Object newState = stateById.invoke(null, newId);
                        set.invoke(palette, i, newState);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void inject(Player player) {
        try {
            Object craftPlayer = player;
            Object entityPlayer = craftPlayer.getClass().getMethod("getHandle").invoke(craftPlayer);
            Object connection = entityPlayer.getClass().getField("connection").get(entityPlayer);
            Object networkManager = connection.getClass().getField("networkManager").get(connection);

            Field channelField = networkManager.getClass().getField("channel");
            channelField.setAccessible(true);
            Channel channel = (Channel) channelField.get(networkManager);

            synchronized (channel) {
                if (channel.pipeline().get("testblocks_interceptor") == null) {
                    channel.pipeline().addBefore("packet_handler", "testblocks_interceptor", new PacketInterceptor(player));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
