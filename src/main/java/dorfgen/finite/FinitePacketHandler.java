package dorfgen.finite;

import javax.vecmath.Vector3f;

import dorfgen.finite.FinitePacketHandler.MessageClient.MessageHandlerClient;
import dorfgen.finite.FinitePacketHandler.MessageServer.MessageHandlerServer;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.NetworkRegistry.TargetPoint;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class FinitePacketHandler
{
    public static SimpleNetworkWrapper packetPipeline = NetworkRegistry.INSTANCE.newSimpleChannel("dorfgen");
    public static IPlayerProvider provider = null;

    static
    {
        packetPipeline.registerMessage(MessageHandlerClient.class, MessageClient.class, 1, Side.CLIENT);
        packetPipeline.registerMessage(MessageHandlerServer.class, MessageServer.class, 2, Side.SERVER);
    }

    public static class MessageClient implements IMessage
    {
        PacketBuffer             buffer;
        public static final byte BLASTAFFECTED = 1;
        public static final byte TELEPORTID    = 2;

        public MessageClient()
        {
        }

        public MessageClient(byte[] data)
        {
            this.buffer = new PacketBuffer(Unpooled.buffer());
            buffer.writeBytes(data);
        }

        public MessageClient(PacketBuffer buffer)
        {
            this.buffer = buffer;
        }

        public MessageClient(byte channel, NBTTagCompound nbt)
        {
            this.buffer = new PacketBuffer(Unpooled.buffer());
            buffer.writeByte(channel);
            buffer.writeNBTTagCompoundToBuffer(nbt);
        }

        @Override
        public void fromBytes(ByteBuf buf)
        {
            if (buffer == null)
            {
                buffer = new PacketBuffer(Unpooled.buffer());
            }
            buffer.writeBytes(buf);
        }

        @Override
        public void toBytes(ByteBuf buf)
        {
            if (buffer == null)
            {
                buffer = new PacketBuffer(Unpooled.buffer());
            }
            buf.writeBytes(buffer);
        }

        public static class MessageHandlerClient implements IMessageHandler<MessageClient, MessageServer>
        {
            public void handleClientSide(EntityPlayer player, PacketBuffer buffer)
            {
                byte channel = buffer.readByte();

                if (player == null)
                {
                    new NullPointerException("Packet recieved by null player").printStackTrace();
                    return;
                }
                if (channel == TELEPORTID)
                {
                    int id = buffer.readInt();
                    if (player.worldObj.getEntityByID(id) != null) player.worldObj.getEntityByID(id).setDead();
                }

            }

            @Override
            public MessageServer onMessage(MessageClient message, MessageContext ctx)
            {
                handleClientSide(provider.getPlayer(), message.buffer);
                return null;
            }
        }
        
        @SideOnly(Side.CLIENT)
        private static class PlayerProvider implements IPlayerProvider
        {
            static
            {
                FinitePacketHandler.provider = new PlayerProvider();
            }
            @Override
            public EntityPlayer getPlayer()
            {
                return net.minecraft.client.Minecraft.getMinecraft().thePlayer;
            }
        }
    }

    public static class MessageServer implements IMessage
    {
        PacketBuffer buffer;

        public MessageServer()
        {
        };

        public MessageServer(byte[] data)
        {
            this.buffer = new PacketBuffer(Unpooled.buffer());
            buffer.writeBytes(data);
        }

        public MessageServer(PacketBuffer buffer)
        {
            this.buffer = buffer;
        }

        public MessageServer(byte channel, NBTTagCompound nbt)
        {
            this.buffer = new PacketBuffer(Unpooled.buffer());
            buffer.writeByte(channel);
            buffer.writeNBTTagCompoundToBuffer(nbt);
        }

        @Override
        public void fromBytes(ByteBuf buf)
        {
            if (buffer == null)
            {
                buffer = new PacketBuffer(Unpooled.buffer());
            }
            buffer.writeBytes(buf);
        }

        @Override
        public void toBytes(ByteBuf buf)
        {
            if (buffer == null)
            {
                buffer = new PacketBuffer(Unpooled.buffer());
            }
            buf.writeBytes(buffer);
        }

        public static class MessageHandlerServer implements IMessageHandler<MessageServer, IMessage>
        {
            public void handleServerSide(EntityPlayer player, PacketBuffer buffer)
            {

            }

            @Override
            public IMessage onMessage(MessageServer message, MessageContext ctx)
            {
                EntityPlayer player = ctx.getServerHandler().playerEntity;
                handleServerSide(player, message.buffer);
                return null;
            }
        }

    }

    public static MessageServer makeServerPacket(byte channel, byte[] data)
    {
        byte[] packetData = new byte[data.length + 1];
        packetData[0] = channel;

        for (int i = 1; i < packetData.length; i++)
        {
            packetData[i] = data[i - 1];
        }
        return new MessageServer(packetData);
    }

    public static MessageClient makeClientPacket(byte channel, byte[] data)
    {
        byte[] packetData = new byte[data.length + 1];
        packetData[0] = channel;

        for (int i = 1; i < packetData.length; i++)
        {
            packetData[i] = data[i - 1];
        }
        return new MessageClient(packetData);
    }

    public static void sendToAllNear(IMessage toSend, Vector3f point, int dimID, double distance)
    {
        packetPipeline.sendToAllAround(toSend, new TargetPoint(dimID, point.x, point.y, point.z, distance));
    }
}
