package jet.opengl.demos.nvidia.waves.crest.loddata;

import com.nvidia.developer.opengl.models.obj.Material;

import jet.opengl.demos.nvidia.waves.crest.CommandBuffer;
import jet.opengl.demos.nvidia.waves.crest.OceanRenderer;
import jet.opengl.demos.nvidia.waves.crest.helpers.IPropertyWrapper;
import jet.opengl.demos.nvidia.waves.crest.helpers.PropertyWrapperMaterial;
import jet.opengl.postprocessing.common.GLenum;
import jet.opengl.postprocessing.shader.GLSLProgram;
import jet.opengl.postprocessing.texture.Texture2D;
import jet.opengl.postprocessing.texture.Texture2DDesc;
import jet.opengl.postprocessing.texture.TextureGL;
import jet.opengl.postprocessing.texture.TextureUtils;
import jet.opengl.postprocessing.util.Numeric;
import jet.opengl.postprocessing.util.Rectf;

public class LodDataMgrAnimWaves extends LodDataMgr {

    public String SimName() { return "AnimatedWaves";  }
    // shape format. i tried RGB111110Float but error becomes visible. one option would be to use a UNORM setup.
    public int TextureFormat() { return GLenum.GL_RGBA16F; }
    protected boolean NeedToReadWriteTextureData() { return true; }

//        [Tooltip("Read shape textures back to the CPU for collision purposes.")]
    public boolean _readbackShapeForCollision = true;

    /// <summary>
    /// Turn shape combine pass on/off. Debug only - ifdef'd out in standalone
    /// </summary>
    public static boolean _shapeCombinePass = true;

    /// <summary>
    /// Ping pong between render targets to do the combine. Disabling this uses a compute shader instead which doesn't need
    /// to copy back and forth between targets, but has dodgy historical support as pre-DX11.3 hardware may not support typed UAV loads.
    /// </summary>
    public static boolean _shapeCombinePassPingPong = true;

    Texture2D _waveBuffers;
    Texture2D _combineBuffer;

    final String ShaderName = "ShapeCombine";

    int krnl_ShapeCombine = -1;
    int krnl_ShapeCombine_DISABLE_COMBINE = -1;
    int krnl_ShapeCombine_FLOW_ON = -1;
    int krnl_ShapeCombine_FLOW_ON_DISABLE_COMBINE = -1;
    int krnl_ShapeCombine_DYNAMIC_WAVE_SIM_ON = -1;
    int krnl_ShapeCombine_DYNAMIC_WAVE_SIM_ON_DISABLE_COMBINE = -1;
    int krnl_ShapeCombine_FLOW_ON_DYNAMIC_WAVE_SIM_ON = -1;
    int krnl_ShapeCombine_FLOW_ON_DYNAMIC_WAVE_SIM_ON_DISABLE_COMBINE = -1;

    GLSLProgram _combineShader;
//    PropertyWrapperCompute _combineProperties;
    PropertyWrapperMaterial[] _combineMaterial;

    static int sp_LD_TexArray_AnimatedWaves_Compute = 0 ; //Shader.PropertyToID("_LD_TexArray_AnimatedWaves_Compute");

    public void UseSettings(SimSettingsBase settings) { OceanRenderer.Instance._simSettingsAnimatedWaves = (SimSettingsAnimatedWaves)settings; }
    public SimSettingsBase CreateDefaultSettings()
    {
        SimSettingsAnimatedWaves settings = new SimSettingsAnimatedWaves();
        settings.name = SimName() + " Auto-generated Settings";
        return settings;
    }

    protected void InitData()
    {
        super.InitData();

        // Setup the RenderTexture and compute shader for combining
        // different animated wave LODs. As we use a single texture array
        // for all LODs, we employ a compute shader as only they can
        // read and write to the same texture.
//        _combineShader = Resources.Load<ComputeShader>(ShaderName);
//        krnl_ShapeCombine = _combineShader.FindKernel("ShapeCombine");
//        krnl_ShapeCombine_DISABLE_COMBINE = _combineShader.FindKernel("ShapeCombine_DISABLE_COMBINE");
//        krnl_ShapeCombine_FLOW_ON = _combineShader.FindKernel("ShapeCombine_FLOW_ON");
//        krnl_ShapeCombine_FLOW_ON_DISABLE_COMBINE = _combineShader.FindKernel("ShapeCombine_FLOW_ON_DISABLE_COMBINE");
//        krnl_ShapeCombine_DYNAMIC_WAVE_SIM_ON = _combineShader.FindKernel("ShapeCombine_DYNAMIC_WAVE_SIM_ON");
//        krnl_ShapeCombine_DYNAMIC_WAVE_SIM_ON_DISABLE_COMBINE = _combineShader.FindKernel("ShapeCombine_DYNAMIC_WAVE_SIM_ON_DISABLE_COMBINE");
//        krnl_ShapeCombine_FLOW_ON_DYNAMIC_WAVE_SIM_ON = _combineShader.FindKernel("ShapeCombine_FLOW_ON_DYNAMIC_WAVE_SIM_ON");
//        krnl_ShapeCombine_FLOW_ON_DYNAMIC_WAVE_SIM_ON_DISABLE_COMBINE = _combineShader.FindKernel("ShapeCombine_FLOW_ON_DYNAMIC_WAVE_SIM_ON_DISABLE_COMBINE");
//        _combineProperties = new PropertyWrapperCompute();

        int resolution = OceanRenderer.Instance.LodDataResolution();
        Texture2DDesc desc = new Texture2DDesc(resolution, resolution, TextureFormat());

        _waveBuffers = (Texture2D) CreateLodDataTextures(desc, "WaveBuffer", false);

        _combineBuffer = TextureUtils.createTexture2D(desc, null);

        /*var combineShader = Shader.Find("Hidden/Crest/Simulation/Combine Animated Wave LODs");
        _combineMaterial = new PropertyWrapperMaterial[OceanRenderer.Instance.CurrentLodCount];
        for (int i = 0; i < _combineMaterial.length; i++)
        {
            Material mat = new Material(combineShader);
            _combineMaterial[i] = new PropertyWrapperMaterial(mat);
        }*/
    }

    FilterWavelength _filterWavelength = new FilterWavelength();

    public class FilterNoLodPreference implements IDrawFilter
    {
        public long Filter(ILodDataInput data/*, out int isTransition*/)
        {
//            isTransition = 0;
//            return data.Wavelength == 0f ? 1f : 0f;
            return Numeric.encode(Float.floatToIntBits(data.Wavelength() == 0f ? 1f : 0f), 0);
        }

    }
    FilterNoLodPreference _filterNoLodPreference = new FilterNoLodPreference();

    public void BuildCommandBuffer(OceanRenderer ocean, CommandBuffer buf)
    {
        super.BuildCommandBuffer(ocean, buf);

        int lodCount = OceanRenderer.Instance.CurrentLodCount();

        // Validation
        for (int lodIdx = 0; lodIdx < OceanRenderer.Instance.CurrentLodCount(); lodIdx++)
        {
            OceanRenderer.Instance._lodTransform._renderData[lodIdx].Validate( 0, this);
        }

        // lod-dependent data
        _filterWavelength._lodCount = lodCount;
        for (int lodIdx = lodCount - 1; lodIdx >= 0; lodIdx--)
        {
//            buf.SetRenderTarget(_waveBuffers, 0, CubemapFace.Unknown, lodIdx);  todo
//            buf.ClearRenderTarget(false, true, new Color(0f, 0f, 0f, 0f));

            // draw any data with lod preference
            _filterWavelength._lodIdx = lodIdx;
            _filterWavelength._lodMaxWavelength = OceanRenderer.Instance._lodTransform.MaxWavelength(lodIdx);
            _filterWavelength._lodMinWavelength = _filterWavelength._lodMaxWavelength / 2f;
            _filterWavelength._globalMaxWavelength = OceanRenderer.Instance._lodTransform.MaxWavelength(OceanRenderer.Instance.CurrentLodCount() - 1);
            SubmitDrawsFiltered(lodIdx, buf, _filterWavelength);
        }

        // Combine the LODs - copy results from biggest LOD down to LOD 0
        if (_shapeCombinePassPingPong)
        {
            CombinePassPingPong(buf);
        }
        else
        {
            CombinePassCompute(buf);
        }

        // lod-independent data
        for (int lodIdx = lodCount - 1; lodIdx >= 0; lodIdx--)
        {
//            buf.SetRenderTarget(_targets, 0, CubemapFace.Unknown, lodIdx);

            // draw any data that did not express a preference for one lod or another
            SubmitDrawsFiltered(lodIdx, buf, _filterNoLodPreference);
        }
    }

    void CombinePassPingPong(CommandBuffer buf)
    {
        int lodCount = OceanRenderer.Instance.CurrentLodCount();
        final int shaderPassCombineIntoAux = 0, shaderPassCopyResultBack = 1;

        // combine waves
        for (int lodIdx = lodCount - 1; lodIdx >= 0; lodIdx--)
        {
            // The per-octave wave buffers
            BindWaveBuffer(_combineMaterial[lodIdx], false);

            // Bind this LOD data (displacements). Option to disable the combine pass - very useful debugging feature.
            if (_shapeCombinePass)
            {
                BindResultData(_combineMaterial[lodIdx]);
            }
            else
            {
                BindNull(_combineMaterial[lodIdx], false);
            }

            // Dynamic waves
            if (OceanRenderer.Instance._lodDataDynWaves != null)
            {
                OceanRenderer.Instance._lodDataDynWaves.BindCopySettings(_combineMaterial[lodIdx]);
                OceanRenderer.Instance._lodDataDynWaves.BindResultData(_combineMaterial[lodIdx]);
            }
            else
            {
                LodDataMgrDynWaves.BindNull(_combineMaterial[lodIdx], false);
            }

            // Flow
            if (OceanRenderer.Instance._lodDataFlow != null)
            {
                OceanRenderer.Instance._lodDataFlow.BindResultData(_combineMaterial[lodIdx]);
            }
            else
            {
//                LodDataMgrFlow.BindNull(_combineMaterial[lodIdx]);
            }

            _combineMaterial[lodIdx].SetFloat(LodDataMgr.sp_LD_SliceIndex, lodIdx);

            // Combine this LOD's waves with waves from the LODs above into auxiliary combine buffer
            /*buf.SetRenderTarget(_combineBuffer);  todo
            buf.DrawProcedural(Matrix4x4.identity, _combineMaterial[lodIdx].material, shaderPassCombineIntoAux, MeshTopology.Triangles, 3);

            // Copy combine buffer back to lod texture array
            buf.SetRenderTarget(_targets, 0, CubemapFace.Unknown, lodIdx);
            _combineMaterial[lodIdx].SetTexture(Shader.PropertyToID("_CombineBuffer"), _combineBuffer);
            buf.DrawProcedural(Matrix4x4.identity, _combineMaterial[lodIdx].material, shaderPassCopyResultBack, MeshTopology.Triangles, 3);*/
        }
    }

    void CombinePassCompute(CommandBuffer buf)
    {
        int lodCount = OceanRenderer.Instance.CurrentLodCount();

        int combineShaderKernel = krnl_ShapeCombine;
        int combineShaderKernel_lastLOD = krnl_ShapeCombine_DISABLE_COMBINE;
        {
            boolean isFlowOn = OceanRenderer.Instance._lodDataFlow != null;
            boolean isDynWavesOn = OceanRenderer.Instance._lodDataDynWaves != null;
            // set the shader kernels that we will use.
            if (isFlowOn && isDynWavesOn)
            {
                combineShaderKernel = krnl_ShapeCombine_FLOW_ON_DYNAMIC_WAVE_SIM_ON;
                combineShaderKernel_lastLOD = krnl_ShapeCombine_FLOW_ON_DYNAMIC_WAVE_SIM_ON_DISABLE_COMBINE;
            }
            else if (isFlowOn)
            {
                combineShaderKernel = krnl_ShapeCombine_FLOW_ON;
                combineShaderKernel_lastLOD = krnl_ShapeCombine_FLOW_ON_DISABLE_COMBINE;
            }
            else if (isDynWavesOn)
            {
                combineShaderKernel = krnl_ShapeCombine_DYNAMIC_WAVE_SIM_ON;
                combineShaderKernel_lastLOD = krnl_ShapeCombine_DYNAMIC_WAVE_SIM_ON_DISABLE_COMBINE;
            }
        }

        // combine waves
        for (int lodIdx = lodCount - 1; lodIdx >= 0; lodIdx--)
        {
            int selectedShaderKernel;
            if (lodIdx < lodCount - 1 && _shapeCombinePass)
            {
                selectedShaderKernel = combineShaderKernel;
            }
            else
            {
                selectedShaderKernel = combineShaderKernel_lastLOD;
            }

//            _combineProperties.Initialise(buf, _combineShader, selectedShaderKernel);

            // The per-octave wave buffers
            BindWaveBuffer(/*_combineProperties*/null, false);
            // Bind this LOD data (displacements)
            BindResultData(/*_combineProperties,*/ null);

            // Dynamic waves
            if (OceanRenderer.Instance._lodDataDynWaves != null)
            {
                OceanRenderer.Instance._lodDataDynWaves.BindCopySettings(/*_combineProperties*/null);
                OceanRenderer.Instance._lodDataDynWaves.BindResultData(/*_combineProperties*/null);
            }
            else
            {
                LodDataMgrDynWaves.BindNull(/*_combineProperties*/null, false);
            }

            // Flow
            if (OceanRenderer.Instance._lodDataFlow != null)
            {
                OceanRenderer.Instance._lodDataFlow.BindResultData(/*_combineProperties*/null);
            }
            else
            {
                LodDataMgrFlow.BindNull(/*_combineProperties*/null, false);
            }

            // Set the animated waves texture where the results will be combined.
            /*_combineProperties.SetTexture(  todo
                    sp_LD_TexArray_AnimatedWaves_Compute,
                    DataTexture
            );

            _combineProperties.SetFloat(sp_LD_SliceIndex, lodIdx);
            _combineProperties.DispatchShader();*/
        }
    }

    public void BindWaveBuffer(IPropertyWrapper properties, boolean sourceLod /*= false*/)
    {
        properties.SetTexture(/*Shader.PropertyToID("_LD_TexArray_WaveBuffer")*/0, _waveBuffers);
        BindData(properties, null, true, OceanRenderer.Instance._lodTransform._renderData, sourceLod);
    }

    protected void BindData(IPropertyWrapper properties, TextureGL applyData, boolean blendOut, LodTransform.RenderData[] renderData, boolean sourceLod /*= false*/)
    {
        super.BindData(properties, applyData, blendOut, renderData, sourceLod);

        LodTransform lt = OceanRenderer.Instance._lodTransform;

        for (int lodIdx = 0; lodIdx < OceanRenderer.Instance.CurrentLodCount(); lodIdx++)
        {
            // need to blend out shape if this is the largest lod, and the ocean might get scaled down later (so the largest lod will disappear)
            boolean needToBlendOutShape = lodIdx == OceanRenderer.Instance.CurrentLodCount() - 1 && OceanRenderer.Instance.ScaleCouldDecrease() && blendOut;
            float shapeWeight = needToBlendOutShape ? OceanRenderer.Instance.ViewerAltitudeLevelAlpha : 1f;
            _BindData_paramIdOceans[lodIdx].set(
                    lt._renderData[lodIdx]._texelWidth,
                    lt._renderData[lodIdx]._textureRes, shapeWeight,
                    1f / lt._renderData[lodIdx]._textureRes);
        }
        properties.SetVectorArray(LodTransform.ParamIdOcean(sourceLod), _BindData_paramIdOceans);
    }

    /// <summary>
    /// Returns index of lod that completely covers the sample area, and contains wavelengths that repeat no more than twice across the smaller
    /// spatial length. If no such lod available, returns -1. This means high frequency wavelengths are filtered out, and the lod index can
    /// be used for each sample in the sample area.
    /// </summary>
    public static int SuggestDataLOD(Rectf sampleAreaXZ)
    {
        return SuggestDataLOD(sampleAreaXZ, Math.min(sampleAreaXZ.width, sampleAreaXZ.height));
    }
    public static int SuggestDataLOD(Rectf sampleAreaXZ, float minSpatialLength)
    {
        int lodCount = OceanRenderer.Instance.CurrentLodCount();
        LodTransform lt = OceanRenderer.Instance._lodTransform;

        for (int lod = 0; lod < lodCount; lod++)
        {

            // Shape texture needs to completely contain sample area
            Rectf lodRect = lt._renderData[lod].RectXZ();
            // Shrink rect by 1 texel border - this is to make finite differences fit as well
            lodRect.x += lt._renderData[lod]._texelWidth; lodRect.y += lt._renderData[lod]._texelWidth;
            lodRect.width -= 2f * lt._renderData[lod]._texelWidth; lodRect.height -= 2f * lt._renderData[lod]._texelWidth;
            if (!lodRect.contains(sampleAreaXZ.x, sampleAreaXZ.y) || !lodRect.contains(sampleAreaXZ.x + sampleAreaXZ.width, sampleAreaXZ.y + sampleAreaXZ.height))
                continue;

            // The smallest wavelengths should repeat no more than twice across the smaller spatial length. Unless we're
            // in the last LOD - then this is the best we can do.
            float minWL = lt.MaxWavelength(lod) / 2f;
            if (minWL < minSpatialLength / 2f && lod < lodCount - 1)
                continue;

            return lod;
        }

        return -1;
    }

    public static String TextureArrayName = "_LD_TexArray_AnimatedWaves";
    private static TextureArrayParamIds textureArrayParamIds = new TextureArrayParamIds(TextureArrayName);
    public static int ParamIdSampler(boolean sourceLod /*= false*/) { return textureArrayParamIds.GetId(sourceLod); }
    protected int GetParamIdSampler(boolean sourceLod /*= false*/)
    {
        return ParamIdSampler(sourceLod);
    }
    public static void BindNull(IPropertyWrapper properties, boolean sourceLod /*= false*/)
    {
        properties.SetTexture(ParamIdSampler(sourceLod), /*TextureArrayHelpers.BlackTextureArray*/null);
    }

}
