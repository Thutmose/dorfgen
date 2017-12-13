package dorfgen.finite;

import javax.vecmath.Vector3f;

import dorfgen.WorldGenerator;
import dorfgen.conversion.DorfMap;
import dorfgen.worldgen.common.BiomeProviderFinite;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.EntityEvent.EnteringChunk;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class FiniteHandler
{
    public FiniteHandler()
    {
        MinecraftForge.EVENT_BUS.register(this);
    }

    private void wrap(DorfMap dorfs, Entity entity, Vector3f pos)
    {
        int[][] image = dorfs.biomeMap;
        int dx = 0, dy = 0, xMin = 0, xMax = 0, yMin = 0, yMax = 0;
        int scale = dorfs.scale;
        dx = dorfs.shiftX(0);
        dy = dorfs.shiftZ(0);
        xMin = -dx;
        yMin = -dy;
        xMax = image.length * scale - dx;
        yMax = image[0].length * scale - dy;
        Vector3f posOld = new Vector3f();
        posOld.set(pos);

        if (pos.x > xMax)
        {
            pos.x = xMin + (pos.x - xMax) + 1;
        }
        else if (pos.x < xMin)
        {
            pos.x = xMax - (xMin - pos.x) - 1;
        }
        if (pos.z > yMax)
        {
            pos.z = yMin + (pos.z - yMax) + 1;
        }
        else if (pos.z < yMin)
        {
            pos.z = yMax - (yMin - pos.z) - 1;
        }
        boolean newIn = DorfMap.inBounds(dorfs.shiftX((int) pos.x) / scale, dorfs.shiftZ((int) pos.z) / scale,
                dorfs.biomeMap);
        if (entity instanceof EntityPlayer) WorldGenerator.log("Wrapped: " + pos + " " + xMin + " " + xMax + " " + yMin
                + " " + yMax + " " + scale + " " + posOld + " " + newIn);
        Transporter.teleportEntity(entity, pos, entity.dimension, false);
    }

    @SubscribeEvent
    public void hurt(LivingHurtEvent evt)
    {
        World world = evt.getEntity().getEntityWorld();
        if (evt.getEntity() instanceof EntityPlayer)
        {
            if (world.getBiomeProvider() instanceof BiomeProviderFinite)
            {
                int scale = ((BiomeProviderFinite) world.getBiomeProvider()).scale;
                DorfMap dorfs = ((BiomeProviderFinite) world.getBiomeProvider()).map;
                Vector3f pos = new Vector3f();
                pos.x = (float) evt.getEntity().posX;
                pos.y = (float) evt.getEntity().posY;
                pos.z = (float) evt.getEntity().posZ;
                if (!DorfMap.inBounds(dorfs.shiftX((int) pos.x) / scale, dorfs.shiftZ((int) pos.z) / scale,
                        dorfs.biomeMap) && evt.getSource().getDamageType().equals("inWall"))
                {
                    wrap(dorfs, evt.getEntity(), pos);
                    evt.setCanceled(true);
                }
            }
        }

    }

    @SubscribeEvent
    public void Wrap(EnteringChunk evt)
    {
        World world = evt.getEntity().getEntityWorld();
        if (world.isRemote || !(evt.getEntity() instanceof EntityPlayer) || evt.getEntity().isDead) return;
        if (world.getBiomeProvider() instanceof BiomeProviderFinite)
        {
            int scale = ((BiomeProviderFinite) world.getBiomeProvider()).scale;
            DorfMap dorfs = ((BiomeProviderFinite) world.getBiomeProvider()).map;

            Vector3f pos = new Vector3f();
            pos.x = (float) evt.getEntity().posX;
            pos.y = (float) evt.getEntity().posY;
            pos.z = (float) evt.getEntity().posZ;
            int[] shift = new int[2];
            shift[0] = dorfs.shift.getX();
            shift[1] = dorfs.shift.getZ();
            if (!DorfMap.inBounds(dorfs.shiftX((int) pos.x) / scale, dorfs.shiftZ((int) pos.z) / scale, dorfs.biomeMap))
            {
                wrap(dorfs, evt.getEntity(), pos);
            }
        }
    }
}
