package jet.opengl.demos.nvidia.waves.crest;

import jet.opengl.demos.nvidia.waves.crest.helpers.TextureArrayHelpers;
import jet.opengl.demos.nvidia.waves.ocean.Technique;
import jet.opengl.postprocessing.common.GLenum;
import jet.opengl.postprocessing.shader.GLSLUtil;
import jet.opengl.postprocessing.texture.Texture2D;
import jet.opengl.postprocessing.texture.Texture2DDesc;
import jet.opengl.postprocessing.util.Numeric;

/** A persistent simulation that moves around with a displacement LOD.*/
abstract class Wave_Simulation_Persistent_Pass extends Wave_Simulation_Pass{
    Texture2D _sources;

    protected Technique _shader;

    protected abstract String ShaderSim ();
    protected abstract Technique krnl_ShaderSim ();

    float _substepDtPrevious = 1f / 60f;

//    public static int sp_SimDeltaTime = 0; //Shader.PropertyToID("_SimDeltaTime");
//    static int sp_SimDeltaTimePrev = 1; //Shader.PropertyToID("_SimDeltaTimePrev");

    protected void InitData() {
        super.InitData();

        _shader = ShaderManager.getInstance().getProgram(ShaderSim());

        int resolution = m_Clipmap.getLodDataResolution();
        Texture2DDesc desc = new Texture2DDesc(resolution, resolution, TextureFormat());
        _sources = CreateLodDataTextures(desc, SimName() + "_1");

        TextureArrayHelpers.ClearToBlack(_targets);
        TextureArrayHelpers.ClearToBlack(_sources);
    }

    public void BindSourceData(Wave_Simulation_ShaderData properties, boolean paramsOnly, boolean usePrevTransform, boolean sourceLod /*= false*/)
    {
        Wave_LOD_Transform.RenderData[] renderData = usePrevTransform ?
                m_Clipmap.m_LodTransform._renderDataSource
                : m_Clipmap.m_LodTransform._renderData;

        BindData(properties, paramsOnly ? TextureArrayHelpers.BlackTextureArray : _sources, true, renderData, sourceLod);
    }

    /** int numSubsteps in the first, float substepDt in the second*/
    public abstract long GetSimSubstepData(float frameDt/*, out int numSubsteps, out float substepDt*/);

    public void BuildCommandBuffer(float deltaTime)
    {
        super.BuildCommandBuffer(deltaTime);

//        gl.glClearTexImage(_sources.getTexture(), 0, GLenum.GL_RGBA, GLenum.GL_FLOAT, null);
//        gl.glClearTexImage(_targets.getTexture(), 0, GLenum.GL_RGBA, GLenum.GL_FLOAT, null);

        final int lodCount = m_Clipmap.m_LodTransform.LodCount();

        m_ShaderData._TexelsPerWave = m_Clipmap.getMinTexelsPerWave();

        long result = GetSimSubstepData(/*ocean.DeltaTime()*/deltaTime/*, out numSubsteps, out substepDt*/);
        int numSubsteps = Numeric.decodeFirst(result);
        float substepDt = Float.intBitsToFloat(Numeric.decodeSecond(result));

        for (int stepi = 0; stepi < numSubsteps; stepi++)
        {
//            SwapRTs(ref _sources, ref _targets);
            {
                Texture2D tmp = _sources;
                _sources = _targets;
                _targets = tmp;
            }

//            _renderSimProperties.Initialise(buf, krnl_ShaderSim(), -1);
            Technique currentTech = krnl_ShaderSim();
            currentTech.setName(ShaderSim());

//            _renderSimProperties.SetFloat(sp_SimDeltaTime, substepDt);
//            _renderSimProperties.SetFloat(sp_SimDeltaTimePrev, _substepDtPrevious);
            m_ShaderData._SimDeltaTime = substepDt;
            m_ShaderData._SimDeltaTimePrev = _substepDtPrevious;

            // compute which lod data we are sampling source data from. if a scale change has happened this can be any lod up or down the chain.
            // this is only valid on the first update step, after that the scale src/target data are in the right places.
            float srcDataIdxChange = ((stepi == 0) ? ScaleDifferencePow2 : 0);

            // only take transform from previous frame on first substep
            boolean usePreviousFrameTransform = stepi == 0;

            // bind data to slot 0 - previous frame data
//            ValidateSourceData(usePreviousFrameTransform);
            BindSourceData(m_ShaderData, false, usePreviousFrameTransform, true);

            SetAdditionalSimParams(m_ShaderData);

//            buf.SetGlobalFloat(sp_LODChange, srcDataIdxChange);
            m_ShaderData._LODChange = srcDataIdxChange;

            /*_renderSimProperties.SetTexture(
                    sp_LD_TexArray_Target,
                    DataTexture()
            );*/
            m_ShaderData._LD_TexArray_Target = DataTexture();

//            _renderSimProperties.DispatchShaderMultiLOD();
            currentTech.enable(m_ShaderData);
            int LodDataResolution = m_Clipmap.getLodDataResolution();
            gl.glDispatchCompute(LodDataResolution / THREAD_GROUP_SIZE_X,
                    LodDataResolution / THREAD_GROUP_SIZE_Y,
                    lodCount);
            gl.glMemoryBarrier(GLenum.GL_SHADER_IMAGE_ACCESS_BARRIER_BIT);
            GLSLUtil.fenceSync();

            gl.glBindImageTexture(1, 0, 0, true, 0, GLenum.GL_WRITE_ONLY, GLenum.GL_RGBA8);

            if(Wave_Simulation.g_CapatureFrame)
                currentTech.printPrograminfo();

            currentTech.disable();

            for (int lodIdx = lodCount - 1; lodIdx >= 0; lodIdx--)
            {
//                buf.SetRenderTarget(_targets, _targets.depthBuffer, 0, CubemapFace.Unknown, lodIdx);  todo
                setRenderTarget(_targets, lodIdx);  // todo notice the depthbuffer.
                SubmitDraws(lodIdx);
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

    /** Set any sim-specific shader params.*/
    protected void SetAdditionalSimParams(Wave_Simulation_ShaderData simMaterial) { }
}
