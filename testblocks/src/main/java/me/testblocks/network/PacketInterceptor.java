package me.testblocks.network;

import io.netty.channel.*;
import org.bukkit.entity.Player;

import java.lang.reflect.Field;
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
            java.lang.reflect.Method getIdMethod = blockClass.getMethod("getId", Class.forName("net.minecraft.world.level.block.state.BlockState"));
            int id = (int) getIdMethod.invoke(null, blockState);

            int mappedId = VisualMappingManager.get(uuid, id);
            if (mappedId != id) {
                java.lang.reflect.Method stateByIdMethod = blockClass.getMethod("stateById", int.class);
                Object remappedState = stateByIdMethod.invoke(null, mappedId);
                blockStateField.set(packet, remappedState);
            }
        } catch (Exception ignore) {}
    }

    private void remapChunkPacket(Object packet, UUID uuid) {
        // Implementation for Stage 3:
        // In a real GOD MODE scenario, we would use ByteBuf to decode the chunk data,
        // iterate through all PalettedContainers, remap our custom IDs using
        // VisualMappingManager.get(uuid, id), and then re-encode.

        // For the sake of this standalone demo, we have the structure ready.
        // Full chunk remapping is extremely NMS intensive and version-locked.
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

            if (channel.pipeline().get("testblocks_interceptor") == null) {
                channel.pipeline().addBefore("packet_handler", "testblocks_interceptor", new PacketInterceptor(player));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
