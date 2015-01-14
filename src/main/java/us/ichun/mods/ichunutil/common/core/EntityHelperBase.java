package us.ichun.mods.ichunutil.common.core;

import com.google.common.collect.Iterables;
import com.mojang.authlib.Agent;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.GameProfileRepository;
import com.mojang.authlib.ProfileLookupCallback;
import com.mojang.authlib.properties.Property;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import us.ichun.mods.ichunutil.common.core.util.ObfHelper;
import us.ichun.mods.ichunutil.common.iChunUtil;
import net.minecraft.block.Block;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.Item;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.*;
import net.minecraft.world.World;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public class EntityHelperBase
{
    private static final UUID uuidExample = UUID.fromString("DEADBEEF-DEAD-BEEF-DEAD-DEADBEEFD00D");
    private static GameProfileRepository profileRepo;
    private static HashMap<String, GameProfile> nameToPartialProfileMap = new HashMap<String, GameProfile>();
    private static HashMap<String, GameProfile> nameToFullProfileMap = new HashMap<String, GameProfile>();

    public static GameProfile getFullGameProfileFromName(String name)
    {
        if(nameToFullProfileMap.containsKey(name))
        {
            return nameToFullProfileMap.get(name);
        }

        GameProfile gp = new GameProfile(null, name);
        if (!StringUtils.isNullOrEmpty(gp.getName()))
        {
            if (!gp.isComplete() || !gp.getProperties().containsKey("textures"))
            {
                GameProfile gameprofile = getPartialGameProfileFromName(gp.getName());

                if (gameprofile != null)
                {
                    Property property = (Property)Iterables.getFirst(gameprofile.getProperties().get("textures"), (Object)null);

                    if (property == null)
                    {
                        gameprofile = iChunUtil.proxy.getSessionService().fillProfileProperties(gameprofile, true);

                        nameToFullProfileMap.put(gameprofile.getName(), gameprofile);
                    }
                    return gameprofile;
                }
            }
        }
        return new GameProfile(uuidExample, name);
    }

    public static GameProfile getSimpleGameProfileFromName(String name)
    {
        return new GameProfile(uuidExample, name);
    }

    public static GameProfile getPartialGameProfileFromName(String name)
    {
        if(nameToFullProfileMap.containsKey(name))
        {
            return nameToFullProfileMap.get(name);
        }
        else if(nameToPartialProfileMap.containsKey(name))
        {
            return nameToPartialProfileMap.get(name);
        }
        final GameProfile[] agameprofile = new GameProfile[1];
        ProfileLookupCallback profilelookupcallback = new ProfileLookupCallback()
        {
            public void onProfileLookupSucceeded(GameProfile p_onProfileLookupSucceeded_1_)
            {
                agameprofile[0] = p_onProfileLookupSucceeded_1_;
            }
            public void onProfileLookupFailed(GameProfile p_onProfileLookupFailed_1_, Exception p_onProfileLookupFailed_2_)
            {
                agameprofile[0] = null;
            }
        };
        if(profileRepo == null)
        {
            profileRepo = iChunUtil.proxy.createProfileRepo();
        }
        profileRepo.findProfilesByNames(new String[] {name}, Agent.MINECRAFT, profilelookupcallback);

        if (agameprofile[0] == null)
        {
            UUID uuid = EntityPlayer.getUUID(new GameProfile((UUID)null, name));
            GameProfile gameprofile = new GameProfile(uuid, name);
            profilelookupcallback.onProfileLookupSucceeded(gameprofile);
        }
        else
        {
            nameToPartialProfileMap.put(agameprofile[0].getName(), agameprofile[0]);
        }

        return agameprofile[0];
    }

    public static void getUUIDFromUsernames(String...names)
    {
        StringBuilder sb = new StringBuilder();
        sb.append("\n\nUUIDs from Names\n");
        ArrayList<String> namesList = new ArrayList<String>();
        ArrayList<String> uuidList = new ArrayList<String>();
        for(String s : names)
        {
            GameProfile gp = getPartialGameProfileFromName(s);
            namesList.add(gp.getName());
            uuidList.add("  \"" + gp.getId().toString() + "\"");
        }
        for(int i = 0; i < namesList.size(); i++)
        {
            sb.append(namesList.get(i)).append("\n");
        }
        for(int i = 0; i < uuidList.size(); i++)
        {
            sb.append(uuidList.get(i)).append("\n");
        }
        System.out.println(sb.toString());
    }

    public static MovingObjectPosition getEntityLook(EntityLivingBase ent, double d)
    {
        return getEntityLook(ent, d, false);
    }

    public static MovingObjectPosition getEntityLook(EntityLivingBase ent, double d, boolean ignoreEntities)
    {
        return getEntityLook(ent, d, ignoreEntities, 1.0F);
    }

    public static MovingObjectPosition getEntityLook(EntityLivingBase ent, double d, boolean ignoreEntities, float renderTick)
    {
        if (ent == null)
        {
            return null;
        }

        double d1 = d;
        MovingObjectPosition mop = rayTrace(ent, d, renderTick);
        Vec3 vec3d = ent.getPositionEyes(renderTick);

        if (mop != null)
        {
            d1 = mop.hitVec.distanceTo(vec3d);
        }

        double dd2 = d;

        if (d1 > dd2)
        {
            d1 = dd2;
        }

        d = d1;
        Vec3 vec3d1 = ent.getLook(renderTick);
        Vec3 vec3d2 = vec3d.addVector(vec3d1.xCoord * d, vec3d1.yCoord * d, vec3d1.zCoord * d);

        if (!ignoreEntities)
        {
            Entity entity1 = null;
            float f1 = 1.0F;
            List list = ent.worldObj.getEntitiesWithinAABBExcludingEntity(ent, ent.getEntityBoundingBox().addCoord(vec3d1.xCoord * d, vec3d1.yCoord * d, vec3d1.zCoord * d).expand(f1, f1, f1));
            double d2 = 0.0D;

            for (int i = 0; i < list.size(); i++)
            {
                Entity entity = (Entity)list.get(i);

                if (!entity.canBeCollidedWith())
                {
                    continue;
                }

                float f2 = entity.getCollisionBorderSize();
                AxisAlignedBB axisalignedbb = entity.getEntityBoundingBox().expand(f2, f2, f2);
                MovingObjectPosition movingobjectposition = axisalignedbb.calculateIntercept(vec3d, vec3d2);

                if (axisalignedbb.isVecInside(vec3d))
                {
                    if (0.0D < d2 || d2 == 0.0D)
                    {
                        entity1 = entity;
                        d2 = 0.0D;
                    }

                    continue;
                }

                if (movingobjectposition == null)
                {
                    continue;
                }

                double d3 = vec3d.distanceTo(movingobjectposition.hitVec);

                if (d3 < d2 || d2 == 0.0D)
                {
                    entity1 = entity;
                    d2 = d3;
                }
            }

            if (entity1 != null)
            {
                mop = new MovingObjectPosition(entity1);
            }
        }

        return mop;
    }

    public static MovingObjectPosition rayTrace(EntityLivingBase ent, double distance, float par3)
    {
        Vec3 var4 = ent.getPositionEyes(par3);
        Vec3 var5 = ent.getLook(par3);
        Vec3 var6 = var4.addVector(var5.xCoord * distance, var5.yCoord * distance, var5.zCoord * distance);
        return ent.worldObj.rayTraceBlocks(var4, var6, false, false, true);
    }

    public static MovingObjectPosition rayTrace(World world, Vec3 vec3d, Vec3 vec3d1, boolean flag, boolean flag1, boolean goThroughTransparentBlocks)
    {
        return rayTrace(world, vec3d, vec3d1, flag, flag1, goThroughTransparentBlocks, 200);
    }

    public static MovingObjectPosition rayTrace(World world, Vec3 vec3d, Vec3 vec3d1, boolean flag, boolean flag1, boolean goThroughTransparentBlocks, int distance)
    {
        if (Double.isNaN(vec3d.xCoord) || Double.isNaN(vec3d.yCoord) || Double.isNaN(vec3d.zCoord))
        {
            return null;
        }

        if (Double.isNaN(vec3d1.xCoord) || Double.isNaN(vec3d1.yCoord) || Double.isNaN(vec3d1.zCoord))
        {
            return null;
        }

        int i = MathHelper.floor_double(vec3d1.xCoord);
        int j = MathHelper.floor_double(vec3d1.yCoord);
        int k = MathHelper.floor_double(vec3d1.zCoord);
        int l = MathHelper.floor_double(vec3d.xCoord);
        int i1 = MathHelper.floor_double(vec3d.yCoord);
        int j1 = MathHelper.floor_double(vec3d.zCoord);
        BlockPos blockpos = new BlockPos(l, i1, j1);
        IBlockState iblockstate = world.getBlockState(blockpos);
        Block block = iblockstate.getBlock();

        if ((!flag1 || block.getCollisionBoundingBox(world, blockpos, iblockstate) != null) && block.canCollideCheck(iblockstate, flag))
        {
            MovingObjectPosition movingobjectposition = block.collisionRayTrace(world, blockpos, vec3d, vec3d1);

            if (movingobjectposition != null)
            {
                return movingobjectposition;
            }
        }

        for (int l1 = distance; l1-- >= 0;)
        {
            if (Double.isNaN(vec3d.xCoord) || Double.isNaN(vec3d.yCoord) || Double.isNaN(vec3d.zCoord))
            {
                return null;
            }

            if (l == i && i1 == j && j1 == k)
            {
                return null;
            }

            boolean flag5 = true;
            boolean flag3 = true;
            boolean flag4 = true;
            double d0 = 999.0D;
            double d1 = 999.0D;
            double d2 = 999.0D;

            if (i > l)
            {
                d0 = (double)l + 1.0D;
            }
            else if (i < l)
            {
                d0 = (double)l + 0.0D;
            }
            else
            {
                flag5 = false;
            }

            if (j > i1)
            {
                d1 = (double)i1 + 1.0D;
            }
            else if (j < i1)
            {
                d1 = (double)i1 + 0.0D;
            }
            else
            {
                flag3 = false;
            }

            if (k > j1)
            {
                d2 = (double)j1 + 1.0D;
            }
            else if (k < j1)
            {
                d2 = (double)j1 + 0.0D;
            }
            else
            {
                flag4 = false;
            }

            double d3 = 999.0D;
            double d4 = 999.0D;
            double d5 = 999.0D;
            double d6 = vec3d1.xCoord - vec3d.xCoord;
            double d7 = vec3d1.yCoord - vec3d.yCoord;
            double d8 = vec3d1.zCoord - vec3d.zCoord;

            if (flag5)
            {
                d3 = (d0 - vec3d.xCoord) / d6;
            }

            if (flag3)
            {
                d4 = (d1 - vec3d.yCoord) / d7;
            }

            if (flag4)
            {
                d5 = (d2 - vec3d.zCoord) / d8;
            }

            if (d3 == -0.0D)
            {
                d3 = -1.0E-4D;
            }

            if (d4 == -0.0D)
            {
                d4 = -1.0E-4D;
            }

            if (d5 == -0.0D)
            {
                d5 = -1.0E-4D;
            }

            EnumFacing enumfacing;

            if (d3 < d4 && d3 < d5)
            {
                enumfacing = i > l ? EnumFacing.WEST : EnumFacing.EAST;
                vec3d = new Vec3(d0, vec3d.yCoord + d7 * d3, vec3d.zCoord + d8 * d3);
            }
            else if (d4 < d5)
            {
                enumfacing = j > i1 ? EnumFacing.DOWN : EnumFacing.UP;
                vec3d = new Vec3(vec3d.xCoord + d6 * d4, d1, vec3d.zCoord + d8 * d4);
            }
            else
            {
                enumfacing = k > j1 ? EnumFacing.NORTH : EnumFacing.SOUTH;
                vec3d = new Vec3(vec3d.xCoord + d6 * d5, vec3d.yCoord + d7 * d5, d2);
            }

            l = MathHelper.floor_double(vec3d.xCoord) - (enumfacing == EnumFacing.EAST ? 1 : 0);
            i1 = MathHelper.floor_double(vec3d.yCoord) - (enumfacing == EnumFacing.UP ? 1 : 0);
            j1 = MathHelper.floor_double(vec3d.zCoord) - (enumfacing == EnumFacing.SOUTH ? 1 : 0);

            blockpos = new BlockPos(l, i1, j1);
            IBlockState iblockstate1 = world.getBlockState(blockpos);
            Block block1 = iblockstate1.getBlock();

            if (goThroughTransparentBlocks && isTransparent(block1))
            {
                continue;
            }

            if ((!flag1 || block1.getCollisionBoundingBox(world, blockpos, iblockstate1) != null) && block1.canCollideCheck(iblockstate1, flag))
            {
                MovingObjectPosition movingobjectposition1 = block1.collisionRayTrace(world, blockpos, vec3d, vec3d1);

                if (movingobjectposition1 != null)
                {
                    return movingobjectposition1;
                }
            }
        }

        return null;
    }

    public static boolean hasFuel(InventoryPlayer inventory, Item item, int damage, int amount)
    {
        if (amount <= 0)
        {
            return true;
        }

        int amountFound = 0;

        for (int var3 = 0; var3 < inventory.mainInventory.length; ++var3)
        {
            if (inventory.mainInventory[var3] != null && inventory.mainInventory[var3].getItem() == item && inventory.mainInventory[var3].getItemDamage() == damage)
            {
                amountFound += inventory.mainInventory[var3].stackSize;

                if (amountFound >= amount)
                {
                    return true;
                }
            }
        }

        return false;
    }

    public static boolean isTransparent(Block block)
    {
        return block.getLightOpacity() != 0xff;
    }

    public static boolean isLookingAtMoon(World world, EntityLivingBase ent, float renderTick, boolean goThroughTransparentBlocks)
    {
        if (ent.dimension == -1 || ent.dimension == 1)
        {
            return false;
        }

        //13000 - 18000 - 23000
        //0.26 - 0.50 - 0.74
        //rotYaw = -88 to -92, 268 to 272
        //opposite for the other end, -268 to -272, 88 to 92
        //at 0.26 = -88 to -92
        //at 0.4 = -86 to -94
        //at 0.425 = -85 to -95
        //at 0.45 = -83 to -97
        //at 0.475 = -78 to -102
        //at 0.4875 = -64 to -116 //can 360 from here on i guess?
        //at 0.5 = -0 to -180
        // y = range, x = 0.45 etc, e = standard constant
        // y=e^(8.92574x) - 90 and y=-e^(8.92574x) - 90
        // 1.423 = 0.26 to 0.4
        // 1.52 = 0.4 to
        double de = 2.71828183D;
        float f = world.getCelestialAngle(1.0F);

        if (!(f >= 0.26D && f <= 0.74D))
        {
            return false;
        }

        float f2 = f > 0.5F ? f - 0.5F : 0.5F - f;
        float f3 = ent.rotationYaw > 0F ? 270 : -90;
        f3 = f > 0.5F ? ent.rotationYaw > 0F ? 90 : -270 : f3;
        f = f > 0.5F ? 1.0F - f : f;

        if (f <= 0.475)
        {
            de = 2.71828183D;
        }
        else if (f <= 0.4875)
        {
            de = 3.88377D;
        }
        else if (f <= 0.4935)
        {
            de = 4.91616;
        }
        else if (f <= 0.4965)
        {
            de = 5.40624;
        }
        else if (f <= 0.5000)
        {
            de = 9.8;
        }

        //yaw check = player.rotationYaw % 360 <= Math.pow(de, (4.92574 * mc.theWorld.getCelestialAngle(1.0F))) + f3 && mc.thePlayer.rotationYaw % 360 >= -Math.pow(de, (4.92574 * mc.theWorld.getCelestialAngle(1.0F))) + f3
        boolean yawCheck = ent.rotationYaw % 360 <= Math.pow(de, (4.92574 * world.getCelestialAngle(1.0F))) + f3 && ent.rotationYaw % 360 >= -Math.pow(de, (4.92574 * world.getCelestialAngle(1.0F))) + f3;
        float ff = world.getCelestialAngle(1.0F);
        ff = ff > 0.5F ? 1.0F - ff : ff;
        ff -= 0.26F;
        ff = (ff / 0.26F) * -94F - 4F;
        //pitch check = mc.thePlayer.rotationPitch <= ff + 2.5F && mc.thePlayer.rotationPitch >= ff - 2.5F
        boolean pitchCheck = ent.rotationPitch <= ff + 2.5F && ent.rotationPitch >= ff - 2.5F;
        Vec3 vec3d = ent.getPositionEyes(renderTick);
        Vec3 vec3d1 = ent.getLook(renderTick);
        Vec3 vec3d2 = vec3d.addVector(vec3d1.xCoord * 500D, vec3d1.yCoord * 500D, vec3d1.zCoord * 500D);
        boolean mopCheck = rayTrace(ent.worldObj, vec3d, vec3d2, true, false, goThroughTransparentBlocks, 500) == null;
        return (yawCheck && pitchCheck && mopCheck);
    }

    public static boolean consumeInventoryItem(InventoryPlayer inventory, Item item, int damage, int amount)
    {
        if (amount <= 0)
        {
            return true;
        }

        int amountFound = 0;

        for (int var3 = 0; var3 < inventory.mainInventory.length; ++var3)
        {
            if (inventory.mainInventory[var3] != null && inventory.mainInventory[var3].getItem() == item && inventory.mainInventory[var3].getItemDamage() == damage)
            {
                amountFound += inventory.mainInventory[var3].stackSize;

                if (amountFound >= amount)
                {
                    break;
                }
            }
        }

        if (amountFound >= amount)
        {
            for (int var3 = 0; var3 < inventory.mainInventory.length; ++var3)
            {
                if (inventory.mainInventory[var3] != null && inventory.mainInventory[var3].getItem() == item && inventory.mainInventory[var3].getItemDamage() == damage)
                {
                    while (amount > 0 && inventory.mainInventory[var3] != null && inventory.mainInventory[var3].stackSize > 0)
                    {
                        amount--;
                        inventory.mainInventory[var3].stackSize--;

                        if (inventory.mainInventory[var3].stackSize <= 0)
                        {
                            inventory.mainInventory[var3] = null;
                        }

                        if (amount <= 0)
                        {
                            return true;
                        }
                    }
                }
            }
        }

        return false;
    }

    public static String getDeathSound(Class clz, EntityLivingBase ent)
    {
        try
        {
            Method m = clz.getDeclaredMethod(ObfHelper.obfuscation ? ObfHelper.getDeathSoundObf : ObfHelper.getDeathSoundDeobf);
            m.setAccessible(true);
            return (String)m.invoke(ent);
        }
        catch(NoSuchMethodException e)
        {
            if(clz != EntityLivingBase.class)
            {
                return getDeathSound(clz.getSuperclass(), ent);
            }
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }
        return "game.neutral.die";
    }

    public static String getHurtSound(Class clz, EntityLivingBase ent)
    {
        try
        {
            Method m = clz.getDeclaredMethod(ObfHelper.obfuscation ? ObfHelper.getHurtSoundObf : ObfHelper.getHurtSoundDeobf);
            m.setAccessible(true);
            return (String)m.invoke(ent);
        }
        catch(NoSuchMethodException e)
        {
            if(clz != EntityLivingBase.class)
            {
                return getHurtSound(clz.getSuperclass(), ent);
            }
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }
        return "game.neutral.hurt";
    }

    public static float updateRotation(float oriRot, float intendedRot, float maxChange)
    {
        float var4 = MathHelper.wrapAngleTo180_float(intendedRot - oriRot);

        if (var4 > maxChange)
        {
            var4 = maxChange;
        }

        if (var4 < -maxChange)
        {
            var4 = -maxChange;
        }

        return oriRot + var4;
    }

    public static float interpolateRotation(float prevRotation, float nextRotation, float partialTick)
    {
        float f3;

        for (f3 = nextRotation - prevRotation; f3 < -180.0F; f3 += 360.0F)
        {
            ;
        }

        while (f3 >= 180.0F)
        {
            f3 -= 360.0F;
        }

        return prevRotation + partialTick * f3;
    }

    public static float interpolateValues(float prevVal, float nextVal, float partialTick)
    {
        return prevVal + partialTick * (nextVal - prevVal);
    }

    public static void faceEntity(Entity facer, Entity faced, float maxYaw, float maxPitch)
    {
        double d0 = faced.posX - facer.posX;
        double d1 = faced.posZ - facer.posZ;
        double d2;

        if (faced instanceof EntityLivingBase)
        {
            EntityLivingBase entitylivingbase = (EntityLivingBase)faced;
            d2 = entitylivingbase.posY + (double)entitylivingbase.getEyeHeight() - (facer.posY + (double)facer.getEyeHeight());
        }
        else
        {
            d2 = (faced.getEntityBoundingBox().minY + faced.getEntityBoundingBox().maxY) / 2.0D - (facer.posY + (double)facer.getEyeHeight());
        }

        double d3 = (double)MathHelper.sqrt_double(d0 * d0 + d1 * d1);
        float f2 = (float)(Math.atan2(d1, d0) * 180.0D / Math.PI) - 90.0F;
        float f3 = (float)(-(Math.atan2(d2, d3) * 180.0D / Math.PI));
        facer.rotationPitch = updateRotation(facer.rotationPitch, f3, maxPitch);
        facer.rotationYaw = updateRotation(facer.rotationYaw, f2, maxYaw);
    }

    public static void setVelocity(Entity entity, double d, double d1, double d2)
    {
        entity.motionX = d;
        entity.motionY = d1;
        entity.motionZ = d2;
    }

    public static boolean destroyBlocksInAABB(Entity ent, AxisAlignedBB aabb)
    {
        int i = MathHelper.floor_double(aabb.minX);
        int j = MathHelper.floor_double(aabb.minY);
        int k = MathHelper.floor_double(aabb.minZ);
        int l = MathHelper.floor_double(aabb.maxX);
        int i1 = MathHelper.floor_double(aabb.maxY);
        int j1 = MathHelper.floor_double(aabb.maxZ);
        boolean flag = false;
        boolean flag1 = false;

        for (int k1 = i; k1 <= l; ++k1)
        {
            for (int l1 = j; l1 <= i1; ++l1)
            {
                for (int i2 = k; i2 <= j1; ++i2)
                {
                    Block block = ent.worldObj.getBlockState(new BlockPos(k1, l1, i2)).getBlock();

                    if (block != null)
                    {
                        if (block.getBlockHardness(ent.worldObj, new BlockPos(k1, l1, i2)) >= 0F && block.canEntityDestroy(ent.worldObj, new BlockPos(k1, l1, i2), ent) && ent.worldObj.getGameRules().getGameRuleBooleanValue("mobGriefing"))
                        {
                            flag1 = (ent.worldObj.isRemote || (ent.worldObj.setBlockToAir(new BlockPos(k1, l1, i2)) || flag1));
                        }
                        else
                        {
                            flag = true;
                        }
                    }
                }
            }
        }

        if (flag1)
        {
            double d0 = aabb.minX + (aabb.maxX - aabb.minX) * (double)ent.worldObj.rand.nextFloat();
            double d1 = aabb.minY + (aabb.maxY - aabb.minY) * (double)ent.worldObj.rand.nextFloat();
            double d2 = aabb.minZ + (aabb.maxZ - aabb.minZ) * (double)ent.worldObj.rand.nextFloat();
            ent.worldObj.spawnParticle(EnumParticleTypes.EXPLOSION_LARGE, d0, d1, d2, 0.0D, 0.0D, 0.0D);
        }

        return flag;
    }

    public static void addPosition(Entity living, double offset, boolean subtract, int axis)
    {
        if (axis == 0) //X axis
        {
            if (subtract)
            {
                living.lastTickPosX -= offset;
                living.prevPosX -= offset;
                living.posX -= offset;
            }
            else
            {
                living.lastTickPosX += offset;
                living.prevPosX += offset;
                living.posX += offset;
            }
        }
        else if (axis == 1) //Y axis
        {
            if (subtract)
            {
                living.lastTickPosY -= offset;
                living.prevPosY -= offset;
                living.posY -= offset;
            }
            else
            {
                living.lastTickPosY += offset;
                living.prevPosY += offset;
                living.posY += offset;
            }
        }
        else if (axis == 2) //Z axis
        {
            if (subtract)
            {
                living.lastTickPosZ -= offset;
                living.prevPosZ -= offset;
                living.posZ -= offset;
            }
            else
            {
                living.lastTickPosZ += offset;
                living.prevPosZ += offset;
                living.posZ += offset;
            }
        }
    }

    @SideOnly(Side.CLIENT)
    public static Render getEntityClassRenderObject(Class par1Class)
    {
        Render render = (Render)Minecraft.getMinecraft().getRenderManager().entityRenderMap.get(par1Class);
        if (render == null && par1Class != Entity.class)
        {
            render = getEntityClassRenderObject(par1Class.getSuperclass());
        }
        return render;
    }

    public static NBTTagCompound getPlayerPersistentData(EntityPlayer player)
    {
        NBTTagCompound persistentTag = player.getEntityData().getCompoundTag(EntityPlayer.PERSISTED_NBT_TAG);
        player.getEntityData().setTag(EntityPlayer.PERSISTED_NBT_TAG, persistentTag);
        return persistentTag;
    }
}
