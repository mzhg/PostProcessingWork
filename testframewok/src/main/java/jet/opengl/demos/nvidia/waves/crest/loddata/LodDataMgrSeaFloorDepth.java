package jet.opengl.demos.nvidia.waves.crest.loddata;

import java.util.List;

import jet.opengl.demos.nvidia.waves.crest.CommandBuffer;
import jet.opengl.demos.nvidia.waves.crest.OceanRenderer;
import jet.opengl.demos.nvidia.waves.crest.helpers.IPropertyWrapper;
import jet.opengl.demos.nvidia.waves.crest.helpers.TextureArrayHelpers;
import jet.opengl.postprocessing.common.GLenum;

/**
 * Renders depth of the ocean (height of sea level above ocean floor), by rendering the relative height of tagged objects from top down.
 */
public class LodDataMgrSeaFloorDepth extends LodDataMgr {
    public String SimName () { return "SeaFloorDepth"; }
    public int TextureFormat() { return GLenum.GL_R16F; }
    protected boolean NeedToReadWriteTextureData () { return false; }

    public SimSettingsBase CreateDefaultSettings() { return null; }
    public void UseSettings(SimSettingsBase settings) { }

    boolean _targetsClear = false;

    public final String ShaderName = "Crest/Inputs/Depth/Cached Depths";

    public void BuildCommandBuffer(OceanRenderer ocean, CommandBuffer buf)
    {
        super.BuildCommandBuffer(ocean, buf);

        // if there is nothing in the scene tagged up for depth rendering, and we have cleared the RTs, then we can early out
        List<ILodDataInput> drawList = RegisterLodDataInputBase.GetRegistrar(getClass());
        if (drawList.size() == 0 && _targetsClear)
        {
            return;
        }

        for (int lodIdx = OceanRenderer.Instance.CurrentLodCount() - 1; lodIdx >= 0; lodIdx--)
        {
            /*buf.SetRenderTarget(_targets, 0, CubemapFace.Unknown, lodIdx);
            buf.ClearRenderTarget(false, true, Color.white * 1000f);
            buf.SetGlobalFloat(sp_LD_SliceIndex, lodIdx);
            SubmitDraws(lodIdx, buf);
            todo
            */
        }

        // targets have now been cleared, we can early out next time around
        if (drawList.size() == 0)
        {
            _targetsClear = true;
        }
    }

    public static String TextureArrayName = "_LD_TexArray_SeaFloorDepth";
    private static TextureArrayParamIds textureArrayParamIds = new TextureArrayParamIds(TextureArrayName);
    public static int ParamIdSampler(boolean sourceLod /*= false*/) { return textureArrayParamIds.GetId(sourceLod); }
    protected int GetParamIdSampler(boolean sourceLod /*= false*/)
    {
        return ParamIdSampler(sourceLod);
    }
    public static void BindNull(IPropertyWrapper properties, boolean sourceLod /*= false*/)
    {
        properties.SetTexture(ParamIdSampler(sourceLod), TextureArrayHelpers.BlackTextureArray);
    }
}
