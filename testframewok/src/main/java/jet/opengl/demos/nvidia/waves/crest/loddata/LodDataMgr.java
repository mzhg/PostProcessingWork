package jet.opengl.demos.nvidia.waves.crest.loddata;

import org.lwjgl.util.vector.Vector4f;

import java.util.List;

import jet.opengl.demos.nvidia.waves.crest.CommandBuffer;
import jet.opengl.demos.nvidia.waves.crest.MonoBehaviour;
import jet.opengl.demos.nvidia.waves.crest.OceanRenderer;
import jet.opengl.demos.nvidia.waves.crest.helpers.IPropertyWrapper;
import jet.opengl.postprocessing.texture.Texture2DDesc;
import jet.opengl.postprocessing.texture.TextureGL;
import jet.opengl.postprocessing.texture.TextureUtils;

public abstract class LodDataMgr extends MonoBehaviour {
    public abstract String SimName();  // { get; }

    public abstract SimSettingsBase CreateDefaultSettings();
    public abstract void UseSettings(SimSettingsBase settings);

    public abstract int TextureFormat(); // { get; }

    // NOTE: This MUST match the value in OceanLODData.hlsl, as it
    // determines the size of the texture arrays in the shaders.
    public final int MAX_LOD_COUNT = 15;

    protected abstract int GetParamIdSampler(boolean sourceLod /*= false*/);

    protected abstract boolean NeedToReadWriteTextureData(); // { get; }

    protected TextureGL _targets;

    public TextureGL DataTexture() { return _targets;  }

    public static int sp_LD_SliceIndex = 0; //Shader.PropertyToID("_LD_SliceIndex");
    protected static int sp_LODChange = 1; //Shader.PropertyToID("_LODChange");

    // shape texture resolution
    int _shapeRes = -1;

    // ocean scale last frame - used to detect scale changes
    float _oceanLocalScalePrev = -1f;

    int _scaleDifferencePow2 = 0;
    protected int ScaleDifferencePow2() {  return _scaleDifferencePow2; }

    public LodDataMgr(){
        for(int i = 0; i < _BindData_paramIdPosScales.length; i++){
            _BindData_paramIdPosScales[i] = new Vector4f();
            _BindData_paramIdOceans[i] = new Vector4f();
        }
    }

    protected void Start()
    {
        InitData();
    }

    public static TextureGL CreateLodDataTextures(Texture2DDesc desc, String name, boolean needToReadWriteTextureData)
    {
        /*RenderTexture result = new RenderTexture(desc);
        result.wrapMode = TextureWrapMode.Clamp;
        result.antiAliasing = 1;
        result.filterMode = FilterMode.Bilinear;
        result.anisoLevel = 0;
        result.useMipMap = false;
        result.name = name;
        result.dimension = TextureDimension.Tex2DArray;
        result.volumeDepth = OceanRenderer.Instance.CurrentLodCount;
        result.enableRandomWrite = needToReadWriteTextureData;
        result.Create();
        return result;*/

        return TextureUtils.createTexture2D(desc, null);
    }

    protected void InitData()
    {
//        (SystemInfo.SupportsRenderTextureFormat(TextureFormat), "The graphics device does not support the render texture format " + TextureFormat.ToString());

//        Debug.Assert(OceanRenderer.Instance.CurrentLodCount <= MAX_LOD_COUNT);

        int resolution = OceanRenderer.Instance.LodDataResolution();
        Texture2DDesc desc = new Texture2DDesc(resolution, resolution, TextureFormat());
        _targets = CreateLodDataTextures(desc, SimName(), NeedToReadWriteTextureData());
    }

    public void UpdateLodData()
    {
        int width = OceanRenderer.Instance.LodDataResolution();
        // debug functionality to resize RT if different size was specified.
        if (_shapeRes == -1)
        {
            _shapeRes = width;
        }
        else if (width != _shapeRes)
        {
            _targets.dispose();
//            _targets.width = _targets.height = _shapeRes;
//            _targets.Create();
            Texture2DDesc desc = new Texture2DDesc(_shapeRes, _shapeRes, TextureFormat());
            _targets = TextureUtils.createTexture2D(desc, null);
        }

        // determine if this LOD has changed scale and by how much (in exponent of 2)
        float oceanLocalScale = OceanRenderer.Instance.transform.getScaleX();
        if (_oceanLocalScalePrev == -1f) _oceanLocalScalePrev = oceanLocalScale;
        float ratio = oceanLocalScale / _oceanLocalScalePrev;
        _oceanLocalScalePrev = oceanLocalScale;
        float ratio_l2 = (float) (Math.log(ratio) / Math.log(2f));
        _scaleDifferencePow2 = Math.round(ratio_l2);
    }

    public void BindResultData(IPropertyWrapper properties){
        BindResultData(properties, true);
    }

    public void BindResultData(IPropertyWrapper properties, boolean blendOut /*= true*/)
    {
        BindData(properties, _targets, blendOut, ref OceanRenderer.Instance._lodTransform._renderData);
    }

    // Avoid heap allocations instead BindData
    private Vector4f[] _BindData_paramIdPosScales = new Vector4f[MAX_LOD_COUNT];
    // Used in child
    protected Vector4f[] _BindData_paramIdOceans = new Vector4f[MAX_LOD_COUNT];
    protected void BindData(IPropertyWrapper properties, TextureGL applyData, boolean blendOut, LodTransform.RenderData[] renderData, boolean sourceLod /*= false*/)
    {
        if (applyData != null)
        {
            properties.SetTexture(GetParamIdSampler(sourceLod), applyData);
        }

        LodTransform lt = OceanRenderer.Instance._lodTransform;
        for (int lodIdx = 0; lodIdx < OceanRenderer.Instance.CurrentLodCount; lodIdx++)
        {
            // NOTE: gets zeroed by unity, see https://www.alanzucconi.com/2016/10/24/arrays-shaders-unity-5-4/
            _BindData_paramIdPosScales[lodIdx].set(
                    renderData[lodIdx]._posSnapped.x, renderData[lodIdx]._posSnapped.z,
                    OceanRenderer.Instance.CalcLodScale(lodIdx), 0f);
            _BindData_paramIdOceans[lodIdx].set(renderData[lodIdx]._texelWidth, renderData[lodIdx]._textureRes, 1f, 1f / renderData[lodIdx]._textureRes);
        }

        // Duplicate the last element as the shader accesses element {slice index + 1] in a few situations. This way going
        // off the end of this parameter is the same as going off the end of the texture array with our clamped sampler.
        _BindData_paramIdPosScales[OceanRenderer.Instance.CurrentLodCount] = _BindData_paramIdPosScales[OceanRenderer.Instance.CurrentLodCount - 1];
        _BindData_paramIdOceans[OceanRenderer.Instance.CurrentLodCount] = _BindData_paramIdOceans[OceanRenderer.Instance.CurrentLodCount - 1];

        properties.SetVectorArray(LodTransform.ParamIdPosScale(sourceLod), _BindData_paramIdPosScales);
        properties.SetVectorArray(LodTransform.ParamIdOcean(sourceLod), _BindData_paramIdOceans);
    }

    public static<LodDataType extends LodDataMgr, LodDataSettings extends  SimSettingsBase> LodDataType Create(GameObject attachGO, LodDataSettings settings)
    /*where LodDataType : LodDataMgr where LodDataSettings : SimSettingsBase*/
    {
        LodDataType sim = attachGO.AddComponent<LodDataType>();

        if (settings == null)
        {
            settings = (LodDataSettings)sim.CreateDefaultSettings();
        }
        sim.UseSettings(settings);

        return sim;
    }

    public void BuildCommandBuffer(OceanRenderer ocean, CommandBuffer buf) { }

    public interface IDrawFilter
    {
        long Filter(ILodDataInput data/*, out int isTransition*/);
    }

    protected void SubmitDraws(int lodIdx, CommandBuffer buf, int frameCount)
    {
        LodTransform lt = OceanRenderer.Instance._lodTransform;
        lt._renderData[lodIdx].Validate(frameCount,0, this);

        lt.SetViewProjectionMatrices(lodIdx, buf);

        var drawList = RegisterLodDataInputBase.GetRegistrar(GetType());
        foreach (var draw in drawList)
        {
            draw.Draw(buf, 1f, 0);
        }
    }

    protected void SubmitDrawsFiltered(int lodIdx, CommandBuffer buf, IDrawFilter filter, int frameCount)
    {
        LodTransform lt = OceanRenderer.Instance._lodTransform;
        lt._renderData[lodIdx].Validate(frameCount,0, this);

        lt.SetViewProjectionMatrices(lodIdx, buf);

        List<ILodDataInput> drawList = RegisterLodDataInputBase.GetRegistrar(getClass());
        for (ILodDataInput draw : drawList)
        {
            if (!draw.Enabled())
            {
                continue;
            }

//            int isTransition;
//            float weight = filter.Filter(draw, out isTransition);
            long result = filter.Filter(draw);

            if (weight > 0f)
            {
                draw.Draw(buf, weight, isTransition);
            }
        }
    }

    protected static class TextureArrayParamIds
    {
        private int _paramId;
        private int _paramId_Source;
        public TextureArrayParamIds(String textureArrayName)
        {
            _paramId = 0; //Shader.PropertyToID(textureArrayName);
            // Note: string concatonation does generate a small amount of
            // garbage. However, this is called on initialisation so should
            // be ok for now? Something worth considering for the future if
            // we want to go garbage-free.
            _paramId_Source = 1; //Shader.PropertyToID(textureArrayName + "_Source");
        }


        public int GetId(boolean sourceLod) { return sourceLod ? _paramId_Source : _paramId; }
    }
}
