package me.testblocks.network;

import io.netty.channel.*;
import org.bukkit.entity.Player;

import java.lang.reflect.Field;
import java.util.Map;

public class PacketInterceptor extends ChannelDuplexHandler {

    private final Player player;
    private final Map<Integer, Integer> blockMapping;

    public PacketInterceptor(Player player, Map<Integer, Integer> blockMapping) {
        this.player = player;
        this.blockMapping = blockMapping;
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object packet, ChannelPromise promise) throws Exception {
        String packetName = packet.getClass().getSimpleName();

        // Single block update
        if (packetName.equals("ClientboundBlockUpdatePacket")) {
            remapBlockUpdatePacket(packet);
        }
        // Bulk chunk load (Initial loading)
        else if (packetName.equals("ClientboundLevelChunkWithLightPacket")) {
            // Chunk remapping requires complex ByteBuf manipulation of PalettedContainers.
            // In a production environment like CraftEngine, this is done by reading the Palette
            // and checking for our custom IDs.
        }

        super.write(ctx, packet, promise);
    }

    private void remapBlockUpdatePacket(Object packet) {
        try {
            Field blockStateField = packet.getClass().getDeclaredField("blockState");
            blockStateField.setAccessible(true);
            Object blockState = blockStateField.get(packet);

            Class<?> blockClass = Class.forName("net.minecraft.world.level.block.Block");
            java.lang.reflect.Method getIdMethod = blockClass.getMethod("getId", Class.forName("net.minecraft.world.level.block.state.BlockState"));
            int id = (int) getIdMethod.invoke(null, blockState);

            int mappedId = VisualMappingManager.getMappedId(id);
            if (mappedId != id) {
                java.lang.reflect.Method stateByIdMethod = blockClass.getMethod("stateById", int.class);
                Object remappedState = stateByIdMethod.invoke(null, mappedId);
                blockStateField.set(packet, remappedState);
            }
        } catch (Exception ignore) {}
    }

    public static void inject(Player player, Map<Integer, Integer> mapping) {
        try {
            Object craftPlayer = player;
            Object entityPlayer = craftPlayer.getClass().getMethod("getHandle").invoke(craftPlayer);
            Object connection = entityPlayer.getClass().getField("connection").get(entityPlayer);
            Object networkManager = connection.getClass().getField("networkManager").get(connection);

            Field channelField = networkManager.getClass().getField("channel");
            channelField.setAccessible(true);
            Channel channel = (Channel) channelField.get(networkManager);

            if (channel.pipeline().get("testblocks_interceptor") == null) {
                channel.pipeline().addBefore("packet_handler", "testblocks_interceptor", new PacketInterceptor(player, mapping));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
