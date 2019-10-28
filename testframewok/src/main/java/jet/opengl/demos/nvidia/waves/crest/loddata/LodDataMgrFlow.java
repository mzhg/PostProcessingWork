package jet.opengl.demos.nvidia.waves.crest.loddata;

import java.awt.Color;
import java.util.List;

import jet.opengl.demos.nvidia.waves.crest.CommandBuffer;
import jet.opengl.demos.nvidia.waves.crest.OceanRenderer;
import jet.opengl.demos.nvidia.waves.crest.helpers.IPropertyWrapper;
import jet.opengl.demos.nvidia.waves.crest.helpers.TextureArrayHelpers;
import jet.opengl.postprocessing.common.GLenum;

/** A persistent flow simulation that moves around with a displacement LOD. The input is fully combined water surface shape. */
public class LodDataMgrFlow extends LodDataMgr {
    public String SimName () { return "Flow"; }
    public int TextureFormat () { return GLenum.GL_R16F; }
    protected boolean NeedToReadWriteTextureData () { return false; }

    public SimSettingsBase Settings() { return /*OceanRenderer.Instance._simSettingsFlow*/null;  }
    public void UseSettings(SimSettingsBase settings) { /*OceanRenderer.Instance._simSettingsFlow = settings as SimSettingsFlow;*/ }
    public SimSettingsBase CreateDefaultSettings()
    {
//        var settings = ScriptableObject.CreateInstance<SimSettingsFlow>();
//        settings.name = SimName + " Auto-generated Settings";
//        return settings;

        return null;
    }

    boolean _targetsClear = false;

    public final String FLOW_KEYWORD = "_FLOW_ON";

    protected void Start()
    {
        super.Start();

/*#if UNITY_EDITOR
        if (!OceanRenderer.Instance.OceanMaterial.IsKeywordEnabled(FLOW_KEYWORD))
        {
            Debug.LogWarning("Flow is not enabled on the current ocean material and will not be visible.", this);
        }
#endif*/
    }

    protected void OnEnable()
    {
//        Shader.EnableKeyword(FLOW_KEYWORD);
    }

    protected void OnDisable()
    {
//        Shader.DisableKeyword(FLOW_KEYWORD);
    }

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
            /*buf.SetRenderTarget(_targets, 0, CubemapFace.Unknown, lodIdx);  todo
            buf.ClearRenderTarget(false, true, Color.black);
            buf.SetGlobalFloat(sp_LD_SliceIndex, lodIdx);
            SubmitDraws(lodIdx, buf);*/
        }

        // targets have now been cleared, we can early out next time around
        if (drawList.size() == 0)
        {
            _targetsClear = true;
        }
    }

    public static String TextureArrayName = "_LD_TexArray_Flow";
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
