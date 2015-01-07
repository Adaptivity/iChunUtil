package us.ichun.mods.ichunutil.client.model;

import us.ichun.mods.ichunutil.common.core.util.ObfHelper;

import java.lang.reflect.Field;
import java.util.*;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelBiped;
import net.minecraft.client.model.ModelBox;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.client.renderer.GLAllocation;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RendererLivingEntity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraftforge.fml.relauncher.ReflectionHelper.UnableToAccessFieldException;

public class ModelHelper 
{

	public static Random rand = new Random();
	
	//Taken and modified from Morph.

	//TODO do i still need this? Try looking at Tabula's.
	public static ArrayList<ModelRenderer> getModelCubesCopy(ArrayList<ModelRenderer> modelList, ModelBase base, EntityLivingBase ent)
	{
		if(Minecraft.getMinecraft().getRenderManager().renderEngine != null && Minecraft.getMinecraft().getRenderManager().livingPlayer != null && ent != null)
		{
			for(int i = 0; i < modelList.size(); i++)
			{
				ModelRenderer cube = modelList.get(i);
                if(cube.compiled)
                {
                    GLAllocation.deleteDisplayLists(cube.displayList);
                    cube.compiled = false;
                }
			}
			Minecraft.getMinecraft().getRenderManager().getEntityRenderObject(ent).doRender(ent, 0.0D, -500D, 0.0D, 0.0F, 1.0F);
			
			ArrayList<ModelRenderer> modelListCopy = new ArrayList<ModelRenderer>(modelList);
			ArrayList<ModelRenderer> list = new ArrayList<ModelRenderer>();
			
			for(int i = modelListCopy.size() - 1; i >= 0; i--)
			{
				ModelRenderer cube = modelListCopy.get(i);
				try
				{
                    if(!cube.compiled)
                    {
                        modelListCopy.remove(i);
                    }
				}
				catch(Exception e)
				{
					ObfHelper.obfWarning();
					e.printStackTrace();
				}
			}
			for(ModelRenderer cube : modelListCopy)
			{
				list.add(buildCopy(cube, base, 0, true, true));
			}
			return list;
		}
		else
		{
		
			ArrayList<ModelRenderer> list = new ArrayList<ModelRenderer>();
	
			for(int i = 0; i < modelList.size(); i++)
			{
				ModelRenderer cube = (ModelRenderer)modelList.get(i);
				list.add(buildCopy(cube, base, 0, true, true));
			}

			return list;
		}
	}

	//TODO redo this. Tabula has a better/close to perfect reconstruction
	@Deprecated
	public static ModelRenderer buildCopy(ModelRenderer original, ModelBase copyBase, int depth, boolean hasFullModelBox, boolean exactDupe) //exactDupes cannot be modified or it may cause issues!
	{
        int txOffsetX = original.textureOffsetX;
        int txOffsetY = original.textureOffsetY;

		ModelRenderer cubeCopy = new ModelRenderer(copyBase, txOffsetX, txOffsetY);
		cubeCopy.mirror = original.mirror;
		cubeCopy.textureHeight = original.textureHeight;
		cubeCopy.textureWidth = original.textureWidth;

		for(int j = 0; j < original.cubeList.size(); j++)
		{
			ModelBox box = (ModelBox)original.cubeList.get(j);
			float param7 = 0.0F;

			if(exactDupe)
			{
				ModelBox boxCopy = new ModelBox(cubeCopy, txOffsetX, txOffsetY, box.posX1, box.posY1, box.posZ1, (int)Math.abs(box.posX2 - box.posX1), (int)Math.abs(box.posY2 - box.posY1), (int)Math.abs(box.posZ2 - box.posZ1), 0.0F);
                boxCopy.quadList = box.quadList;
				cubeCopy.cubeList.add(boxCopy);
			}
			else
			{
				if(hasFullModelBox)
				{
					cubeCopy.addBox(box.posX1, box.posY1, box.posZ1, (int)Math.abs(box.posX2 - box.posX1), (int)Math.abs(box.posY2 - box.posY1), (int)Math.abs(box.posZ2 - box.posZ1));
				}
				else
				{
					ModelBox randBox = (ModelBox)original.cubeList.get(rand.nextInt(original.cubeList.size()));
	
					float x = randBox.posX1 + ((randBox.posX2 - randBox.posX1) > 0F ? rand.nextInt(((int)(randBox.posX2 - randBox.posX1) > 0) ? (int)(randBox.posX2 - randBox.posX1) : 1) : 0F);
					float y = randBox.posY1 + ((randBox.posY2 - randBox.posY1) > 0F ? rand.nextInt(((int)(randBox.posY2 - randBox.posY1) > 0) ? (int)(randBox.posY2 - randBox.posY1) : 1) : 0F);
					float z = randBox.posZ1 + ((randBox.posZ2 - randBox.posZ1) > 0F ? rand.nextInt(((int)(randBox.posZ2 - randBox.posZ1) > 0) ? (int)(randBox.posZ2 - randBox.posZ1) : 1) : 0F);
					
					cubeCopy.addBox(x, y, z, (int)Math.abs(box.posX2 - box.posX1), (int)Math.abs(box.posY2 - box.posY1), (int)Math.abs(box.posZ2 - box.posZ1));
				}
			}
		}

		if(original.childModels != null && depth < 20)
		{
			for(int i = 0; i < original.childModels.size(); i++)
			{
				ModelRenderer child = (ModelRenderer)original.childModels.get(i);
				cubeCopy.addChild(buildCopy(child, copyBase, depth + 1, hasFullModelBox, exactDupe));
			}
		}

		cubeCopy.setRotationPoint(original.rotationPointX, original.rotationPointY, original.rotationPointZ);

		cubeCopy.rotateAngleX = original.rotateAngleX;
		cubeCopy.rotateAngleY = original.rotateAngleY;
		cubeCopy.rotateAngleZ = original.rotateAngleZ;
		return cubeCopy;
	}
	
	public static ArrayList<ModelRenderer> getModelCubes(ModelBase parent)
	{
		ArrayList<ModelRenderer> list = new ArrayList<ModelRenderer>();

		ArrayList<ModelRenderer[]> list1 = new ArrayList<ModelRenderer[]>();

		if(parent != null)
		{
			Class clz = parent.getClass();
			while(clz != ModelBase.class && ModelBase.class.isAssignableFrom(clz))
			{
				try
				{
					Field[] fields = clz.getDeclaredFields();
					for(Field f : fields)
					{
						f.setAccessible(true);
						if(f.getType() == ModelRenderer.class)
						{
							if(clz == ModelBiped.class && !(f.getName().equalsIgnoreCase("bipedCloak") || f.getName().equalsIgnoreCase("k") || f.getName().equalsIgnoreCase("field_78122_k")) || clz != ModelBiped.class)
							{
								ModelRenderer rend = (ModelRenderer)f.get(parent);
								if(rend != null)
								{
									list.add(rend); // Add normal parent fields
								}
							}
						}
						else if(f.getType() == ModelRenderer[].class)
						{
							ModelRenderer[] rend = (ModelRenderer[])f.get(parent);
							if(rend != null)
							{
								list1.add(rend);
							}
						}
					}
					clz = clz.getSuperclass();
				}
				catch(Exception e)
				{
					throw new UnableToAccessFieldException(new String[0], e);
				}
			}
		}

		for(ModelRenderer[] cubes : list1)
		{
			for(ModelRenderer cube : cubes)
			{
				if(cube != null && !list.contains(cube))
				{
					list.add(cube); //Add stuff like flying blaze rods stored in MR[] fields.
				}
			}
		}

		ArrayList<ModelRenderer> children = new ArrayList<ModelRenderer>();

		for(ModelRenderer cube : list)
		{
			for(ModelRenderer child : getChildren(cube, true, 0))
			{
				if(!children.contains(child))
				{
					children.add(child);
				}
			}
		}

		for(ModelRenderer child : children)
		{
			list.remove(child);
		}
		
		return list;
	}

    public static HashMap<String, ModelRenderer> getModelCubesWithNames(ModelBase parent)
    {
        HashMap<String, ModelRenderer> list = new HashMap<String, ModelRenderer>();

        HashMap<String,  ModelRenderer[]> list1 = new HashMap<String, ModelRenderer[]>();

        if(parent != null)
        {
            Class clz = parent.getClass();
            while(clz != ModelBase.class && ModelBase.class.isAssignableFrom(clz))
            {
                try
                {
                    Field[] fields = clz.getDeclaredFields();
                    for(Field f : fields)
                    {
                        f.setAccessible(true);
                        if(f.getType() == ModelRenderer.class)
                        {
                            if(clz == ModelBiped.class && !(f.getName().equalsIgnoreCase("bipedCloak") || f.getName().equalsIgnoreCase("k") || f.getName().equalsIgnoreCase("field_78122_k")) || clz != ModelBiped.class)
                            {
                                ModelRenderer rend = (ModelRenderer)f.get(parent);
                                if(rend != null)
                                {
									String name = f.getName();
									if(rend.boxName != null)
									{
										name = rend.boxName;
										while(list.containsKey(name))
										{
											name = name + "_";
										}
									}
                                    list.put(name, rend); // Add normal parent fields
                                }
                            }
                        }
                        else if(f.getType() == ModelRenderer[].class)
                        {
                            ModelRenderer[] rend = (ModelRenderer[])f.get(parent);
                            if(rend != null)
                            {
                                list1.put(f.getName(), rend);
                            }
                        }
                    }
                    clz = clz.getSuperclass();
                }
                catch(Exception e)
                {
                    throw new UnableToAccessFieldException(new String[0], e);
                }
            }
        }

        for(Map.Entry<String, ModelRenderer[]> e : list1.entrySet())
        {
            int count = 1;
            for(ModelRenderer cube : e.getValue())
            {
                if(cube != null && !list.containsValue(cube))
                {
                    list.put(e.getKey() + count, cube); //Add stuff like flying blaze rods stored in MR[] fields.
                    count++;
                }
            }
        }

        ArrayList<ModelRenderer> children = new ArrayList<ModelRenderer>();

        for(Map.Entry<String, ModelRenderer> e : list.entrySet())
        {
            ModelRenderer cube = e.getValue();
            for(ModelRenderer child : getChildren(cube, true, 0))
            {
                if(!children.contains(child))
                {
                    children.add(child);
                }
            }
        }

        for(ModelRenderer child : children)
        {
            Iterator<Map.Entry<String, ModelRenderer>> ite = list.entrySet().iterator();
            while(ite.hasNext())
            {
                Map.Entry<String, ModelRenderer> e = ite.next();
                if(e.getValue() == child)
                {
                    ite.remove();
                }
            }
        }

        return list;
    }

    public static ArrayList<ModelRenderer> getMultiModelCubes(ArrayList<ModelBase> parent)
	{
		ArrayList<ModelRenderer> list = new ArrayList<ModelRenderer>();
		for(ModelBase base : parent)
		{
			list.addAll(getModelCubes(base));
		}
		return list;
	}

	public static ArrayList<ModelRenderer> getChildren(ModelRenderer parent, boolean recursive, int depth)
	{
		ArrayList<ModelRenderer> list = new ArrayList<ModelRenderer>();
		if(parent.childModels != null && depth < 20)
		{
			for(int i = 0; i < parent.childModels.size(); i++)
			{
				ModelRenderer child = (ModelRenderer)parent.childModels.get(i);
				if(recursive)
				{
					ArrayList<ModelRenderer> children = getChildren(child, recursive, depth + 1);
					for(ModelRenderer child1 : children)
					{
						if(!list.contains(child1))
						{
							list.add(child1);
						}
					}
				}
				if(!list.contains(child))
				{
					list.add(child);
				}
			}
		}
		return list;
	}
	
	public static ModelBase getPossibleModel(Render rend)
	{
		ArrayList<ArrayList<ModelBase>> models = new ArrayList<ArrayList<ModelBase>>();

		if(rend != null)
		{
			try
			{
				Class clz = rend.getClass();
				while(clz != Render.class)
				{
					ArrayList<ModelBase> priorityLevel = new ArrayList<ModelBase>();
					
					Field[] fields = clz.getDeclaredFields();
					for(Field f : fields)
					{
						f.setAccessible(true);
						if(ModelBase.class.isAssignableFrom(f.getType()))
						{
							ModelBase base = (ModelBase)f.get(rend);
							if(base != null)
							{
								priorityLevel.add(base); // Add normal parent fields
							}
						}
						else if(ModelBase[].class.isAssignableFrom(f.getType()))
						{
							ModelBase[] modelBases = (ModelBase[])f.get(rend);
							if(modelBases != null)
							{
								for(ModelBase base : modelBases)
								{
									priorityLevel.add(base);
								}
							}
						}
					}
					
					models.add(priorityLevel);
					
					if(clz == RendererLivingEntity.class)
					{
						ArrayList<ModelBase> topPriority = new ArrayList<ModelBase>();
						for(Field f : fields)
						{
							f.setAccessible(true);
							if(ModelBase.class.isAssignableFrom(f.getType()) && (f.getName().equalsIgnoreCase("mainModel") || f.getName().equalsIgnoreCase("field_77045_g")))
							{
								ModelBase base = (ModelBase)f.get(rend);
								if(base != null)
								{
									topPriority.add(base);
								}
							}
						}
						models.add(topPriority);
					}
					clz = clz.getSuperclass();
				}
			}
			catch(Exception e)
			{
				throw new UnableToAccessFieldException(new String[0], e);
			}
		}

		ModelBase base1 = null;
		int priorityLevel = -1;
		int size = -1;

		int currentPriority = 0;
		
		for(ArrayList<ModelBase> modelList : models)
		{
			for(ModelBase base : modelList)
			{
				ArrayList<ModelRenderer> mrs = getModelCubes(base);
				if(mrs.size() > size || mrs.size() == size && currentPriority > priorityLevel)
				{
					size = mrs.size();
					base1 = base;
					priorityLevel = currentPriority;
				}
			}
			currentPriority++;
		}

		return base1;
	}
	
	public static ArrayList<ModelRenderer> getModelCubes(EntityLivingBase living) 
	{
		ArrayList<ModelRenderer> map = classToModelRendererMap.get(living.getClass());
		if(map == null)
		{
			map = getModelCubes(getPossibleModel(Minecraft.getMinecraft().getRenderManager().getEntityRenderObject(living)));
			classToModelRendererMap.put(living.getClass(), map);
		}
		return map;
	}
	
	public static HashMap<Class<? extends EntityLivingBase>, ArrayList<ModelRenderer>> classToModelRendererMap = new HashMap<Class<? extends EntityLivingBase>, ArrayList<ModelRenderer>>();
}
