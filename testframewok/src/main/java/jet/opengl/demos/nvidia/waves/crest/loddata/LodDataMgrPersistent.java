package jet.opengl.demos.nvidia.waves.crest.loddata;

import jet.opengl.demos.nvidia.waves.crest.CommandBuffer;
import jet.opengl.demos.nvidia.waves.crest.OceanRenderer;
import jet.opengl.demos.nvidia.waves.crest.helpers.IPropertyWrapper;
import jet.opengl.demos.nvidia.waves.crest.helpers.PropertyWrapperCompute;
import jet.opengl.demos.nvidia.waves.crest.helpers.TextureArrayHelpers;
import jet.opengl.demos.nvidia.waves.crest.helpers.Time;
import jet.opengl.postprocessing.shader.GLSLProgram;
import jet.opengl.postprocessing.texture.Texture2DDesc;
import jet.opengl.postprocessing.texture.TextureGL;
import jet.opengl.postprocessing.util.Numeric;

/** A persistent simulation that moves around with a displacement LOD. */
public abstract class LodDataMgrPersistent extends LodDataMgr {
    protected boolean NeedToReadWriteTextureData () { return true; }

    TextureGL _sources;
    PropertyWrapperCompute _renderSimProperties;

    static int sp_LD_TexArray_Target = 0; //Shader.PropertyToID("_LD_TexArray_Target");

    protected GLSLProgram _shader;

    protected abstract String ShaderSim ();
    protected abstract GLSLProgram krnl_ShaderSim ();

    float _substepDtPrevious = 1f / 60f;

    public static int sp_SimDeltaTime = 0; //Shader.PropertyToID("_SimDeltaTime");
    static int sp_SimDeltaTimePrev = 1; //Shader.PropertyToID("_SimDeltaTimePrev");

    protected void Start()
    {
        super.Start();

        CreateProperties(OceanRenderer.Instance.CurrentLodCount());
    }

    void CreateProperties(int lodCount)
    {
//        _shader = Resources.Load<ComputeShader>(ShaderSim);
        _renderSimProperties = new PropertyWrapperCompute();
    }

    protected void InitData() {
        super.InitData();

        int resolution = OceanRenderer.Instance.LodDataResolution();
        Texture2DDesc desc = new Texture2DDesc(resolution, resolution, TextureFormat());
        _sources = CreateLodDataTextures(desc, SimName() + "_1", NeedToReadWriteTextureData());

        TextureArrayHelpers.ClearToBlack(_targets);
        TextureArrayHelpers.ClearToBlack(_sources);
    }

    public void ValidateSourceData(boolean usePrevTransform)
    {
        LodTransform.RenderData[] renderDataToValidate = usePrevTransform ?
                OceanRenderer.Instance._lodTransform._renderDataSource
                : OceanRenderer.Instance._lodTransform._renderData;
        int validationFrame = usePrevTransform ? /*BuildCommandBufferBase._lastUpdateFrame - Time.frameCount*/-1 : 0;
        for (LodTransform.RenderData renderData : renderDataToValidate)
        {
            renderData.Validate(validationFrame, this);
        }
    }

    public void BindSourceData(IPropertyWrapper properties, boolean paramsOnly, boolean usePrevTransform, boolean sourceLod /*= false*/)
    {
        LodTransform.RenderData[] renderData = usePrevTransform ?
                OceanRenderer.Instance._lodTransform._renderDataSource
                : OceanRenderer.Instance._lodTransform._renderData;

        BindData(properties, paramsOnly ? TextureArrayHelpers.BlackTextureArray : _sources, true, renderData, sourceLod);
    }

    /** int numSubsteps in the first, float substepDt in the second*/
    public abstract long GetSimSubstepData(float frameDt/*, out int numSubsteps, out float substepDt*/);

    public void BuildCommandBuffer(OceanRenderer ocean, CommandBuffer buf)
    {
        super.BuildCommandBuffer(ocean, buf);

        int lodCount = ocean.CurrentLodCount();

        long result = GetSimSubstepData(/*ocean.DeltaTime()*/Time.deltaTime/*, out numSubsteps, out substepDt*/);
        int numSubsteps = Numeric.decodeFirst(result);
        float substepDt = Float.intBitsToFloat(Numeric.decodeSecond(result));

        for (int stepi = 0; stepi < numSubsteps; stepi++)
        {
//            SwapRTs(ref _sources, ref _targets);
            {
                TextureGL tmp = _sources;
                _sources = _targets;
                _targets = tmp;
            }

            _renderSimProperties.Initialise(buf, krnl_ShaderSim(), -1);

            _renderSimProperties.SetFloat(sp_SimDeltaTime, substepDt);
            _renderSimProperties.SetFloat(sp_SimDeltaTimePrev, _substepDtPrevious);

            // compute which lod data we are sampling source data from. if a scale change has happened this can be any lod up or down the chain.
            // this is only valid on the first update step, after that the scale src/target data are in the right places.
            float srcDataIdxChange = ((stepi == 0) ? ScaleDifferencePow2 : 0);

            // only take transform from previous frame on first substep
            boolean usePreviousFrameTransform = stepi == 0;

            // bind data to slot 0 - previous frame data
            ValidateSourceData(usePreviousFrameTransform);
            BindSourceData(_renderSimProperties, false, usePreviousFrameTransform, true);

            SetAdditionalSimParams(_renderSimProperties);

            buf.SetGlobalFloat(sp_LODChange, srcDataIdxChange);

            _renderSimProperties.SetTexture(
                    sp_LD_TexArray_Target,
                    DataTexture()
            );

            _renderSimProperties.DispatchShaderMultiLOD();

            for (int lodIdx = lodCount - 1; lodIdx >= 0; lodIdx--)
            {
                buf.SetRenderTarget(_targets, _targets.depthBuffer, 0, CubemapFace.Unknown, lodIdx);
                SubmitDraws(lodIdx, buf);
            }

            _substepDtPrevious = substepDt;
        }

        // any post-sim steps. the dyn waves updates the copy sim material, which the anim wave will later use to copy in
        // the dyn waves results.
        for (int lodIdx = lodCount - 1; lodIdx >= 0; lodIdx--)
        {
            BuildCommandBufferInternal(lodIdx);
        }
    }

    protected boolean BuildCommandBufferInternal(int lodIdx)
    {
        return true;
    }

    /// <summary>
    /// Set any sim-specific shader params.
    /// </summary>
    protected void SetAdditionalSimParams(IPropertyWrapper simMaterial)
    {
    }
}
