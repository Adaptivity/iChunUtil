package us.ichun.module.tabula.common.project.components;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.client.model.ModelRenderer;
import org.apache.commons.lang3.RandomStringUtils;
import us.ichun.module.tabula.common.project.ProjectInfo;

import java.util.ArrayList;

public class CubeInfo
{
    public CubeInfo(String name)
    {
        this.name = name;
        dimensions = new int[] { 1, 1, 1 };
        scale = new double[] { 1D, 1D, 1D };
        identifier = RandomStringUtils.randomAscii(ProjectInfo.IDENTIFIER_LENGTH);
    }

    public String name;
    public int[] dimensions = new int[3];

    public double[] position = new double[3];
    public double[] offset = new double[3];
    public double[] rotation = new double[3];

    public double[] scale = new double[3];

    public int[] txOffset = new int[2];
    public boolean txMirror = false;

    private ArrayList<CubeInfo> children = new ArrayList<CubeInfo>();
    public String parentIdentifier;

    public String identifier;

    @SideOnly(Side.CLIENT)
    public transient ModelRenderer modelCube;

    public void addChild(CubeInfo info)
    {
        children.add(info);
        info.parentIdentifier = identifier;
    }

    public void removeChild(CubeInfo info)
    {
        children.remove(info);
        if(info.parentIdentifier.equals(identifier))
        {
            info.parentIdentifier = null;
        }
    }

    public ArrayList<CubeInfo> getChildren()
    {
        return children;
    }
}
