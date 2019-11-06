package jet.opengl.demos.nvidia.waves.crest;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;

import jet.opengl.demos.nvidia.waves.ocean.Technique;
import jet.opengl.postprocessing.common.GLenum;
import jet.opengl.postprocessing.core.PostProcessingDefaultProgram;
import jet.opengl.postprocessing.texture.Texture2D;
import jet.opengl.postprocessing.texture.Texture2DDesc;
import jet.opengl.postprocessing.texture.TextureGL;
import jet.opengl.postprocessing.texture.TextureUtils;
import jet.opengl.postprocessing.util.CacheBuffer;
import jet.opengl.postprocessing.util.DebugTools;
import jet.opengl.postprocessing.util.Rectf;

final class Wave_Simulation_Animation_Pass extends Wave_Simulation_Pass {
    public String SimName() { return "AnimatedWaves";  }
    // shape format. i tried RGB111110Float but error becomes visible. one option would be to use a UNORM setup.
    public int TextureFormat() { return GLenum.GL_RGBA16F; }

    /** Turn shape combine pass on/off. Debug only - ifdef'd out in standalone*/
    private static boolean _shapeCombinePass = true;
    private Texture2D _waveBuffers;
    private Texture2D _combineBuffer;

    private Technique krnl_ShapeCombine = null;
    private Technique krnl_ShapeCombine_DISABLE_COMBINE = null;
    private Technique krnl_ShapeCombine_FLOW_ON = null;
    private Technique krnl_ShapeCombine_FLOW_ON_DISABLE_COMBINE = null;
    private Technique krnl_ShapeCombine_DYNAMIC_WAVE_SIM_ON = null;
    private Technique krnl_ShapeCombine_DYNAMIC_WAVE_SIM_ON_DISABLE_COMBINE = null;
    private Technique krnl_ShapeCombine_FLOW_ON_DYNAMIC_WAVE_SIM_ON = null;
    private Technique krnl_ShapeCombine_FLOW_ON_DYNAMIC_WAVE_SIM_ON_DISABLE_COMBINE = null;

    private Technique _combineMaterial;
    private PostProcessingDefaultProgram _copyBack;

    private Wave_Length_Filter _filterWavelength = new Wave_Length_Filter();
    private FilterNoLodPreference _filterNoLodPreference = new FilterNoLodPreference();

    private Wave_Gerstner_Batched _gerstnerBatched;

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
        krnl_ShapeCombine = ShaderManager.getInstance().getProgram("ShapeCombine");
        krnl_ShapeCombine_DISABLE_COMBINE = ShaderManager.getInstance().getProgram("ShapeCombine_DISABLE_COMBINE");
        krnl_ShapeCombine_FLOW_ON = ShaderManager.getInstance().getProgram("ShapeCombine_FLOW_ON");
        krnl_ShapeCombine_FLOW_ON_DISABLE_COMBINE = ShaderManager.getInstance().getProgram("ShapeCombine_FLOW_ON_DISABLE_COMBINE");
        krnl_ShapeCombine_DYNAMIC_WAVE_SIM_ON = ShaderManager.getInstance().getProgram("ShapeCombine_DYNAMIC_WAVE_SIM_ON");
        krnl_ShapeCombine_DYNAMIC_WAVE_SIM_ON_DISABLE_COMBINE = ShaderManager.getInstance().getProgram("ShapeCombine_DYNAMIC_WAVE_SIM_ON_DISABLE_COMBINE");
        krnl_ShapeCombine_FLOW_ON_DYNAMIC_WAVE_SIM_ON = ShaderManager.getInstance().getProgram("ShapeCombine_FLOW_ON_DYNAMIC_WAVE_SIM_ON");
        krnl_ShapeCombine_FLOW_ON_DYNAMIC_WAVE_SIM_ON_DISABLE_COMBINE = ShaderManager.getInstance().getProgram("ShapeCombine_FLOW_ON_DYNAMIC_WAVE_SIM_ON_DISABLE_COMBINE");

        int resolution = m_Clipmap.getLodDataResolution();
        Texture2DDesc desc = new Texture2DDesc(resolution, resolution, TextureFormat());
        _waveBuffers = CreateLodDataTextures(desc, "WaveBuffer");

        desc.arraySize = 1;
        _combineBuffer = TextureUtils.createTexture2D(desc, null);
        _combineBuffer.setMagFilter(GLenum.GL_NEAREST);
        _combineBuffer.setMinFilter(GLenum.GL_NEAREST);
        _combineBuffer.setWrapS(GLenum.GL_CLAMP_TO_EDGE);
        _combineBuffer.setWrapT(GLenum.GL_CLAMP_TO_EDGE);

        /*var combineShader = Shader.Find("Hidden/Crest/Simulation/Combine Animated Wave LODs");
        _combineMaterial = new PropertyWrapperMaterial[OceanRenderer.Instance.CurrentLodCount];
        for (int i = 0; i < _combineMaterial.length; i++)
        {
            Material mat = new Material(combineShader);
            _combineMaterial[i] = new PropertyWrapperMaterial(mat);
        }*/
        final String materialName = String.format("Hidden/Crest/Simulation/Combine Animated Wave LODs%d%d", m_Simulation.m_Params.create_dynamic_wave?1:0,m_Simulation.m_Params.create_flow?1:0);
        _combineMaterial = ShaderManager.getInstance().getProgram(materialName);
        _combineMaterial.setName("AnimCombinePass");

        try {
            _copyBack = new PostProcessingDefaultProgram();
        } catch (IOException e) {
            e.printStackTrace();
        }

        _gerstnerBatched = new Wave_Gerstner_Batched();
        _gerstnerBatched.init(m_Simulation, m_Clipmap);

        for(Wave_LodData_Input input : _gerstnerBatched.getBatches()){
            addLodDataInput(input);
        }
    }

    private final class FilterNoLodPreference implements Wave_DrawFilter
    {
        @Override
        public void Filter(Wave_LodData_Input data, Wave_CDClipmap clipmap, Wave_FilterData result) {
            result.isTransition = false;
            result.weight = data.wavelength() == 0f ? 1f : 0f;
        }
    }

    public void BuildCommandBuffer(float deltaTime)
    {
        super.BuildCommandBuffer(deltaTime);

        _gerstnerBatched.update(deltaTime);

        final int lodCount = m_Clipmap.m_LodTransform.LodCount();

        // lod-dependent data
        _filterWavelength._lodCount = lodCount;
        gl.glClearTexImage(_waveBuffers.getTexture(), 0, GLenum.GL_RGBA, GLenum.GL_FLOAT, null);
        gl.glClearTexImage(_targets.getTexture(), 0, GLenum.GL_RGBA, GLenum.GL_FLOAT, null);
        for (int lodIdx = lodCount - 1; lodIdx >= 0; lodIdx--)
        {
//            buf.SetRenderTarget(_waveBuffers, 0, CubemapFace.Unknown, lodIdx);
//            buf.ClearRenderTarget(false, true, new Color(0f, 0f, 0f, 0f));
            setRenderTarget(_waveBuffers, lodIdx);

            // draw any data with lod preference
            _filterWavelength._lodIdx = lodIdx;
            _filterWavelength._lodMaxWavelength = m_Clipmap.m_LodTransform.MaxWavelength(lodIdx);
            _filterWavelength._lodMinWavelength = _filterWavelength._lodMaxWavelength / 2f;
            _filterWavelength._globalMaxWavelength = m_Clipmap.m_LodTransform.MaxWavelength(lodCount - 1);
            SubmitDrawsFiltered(lodIdx, _filterWavelength);
        }

        saveTextur(_waveBuffers, "WaveBuffer.txt");

        // Combine the LODs - copy results from biggest LOD down to LOD 0
        if (m_Simulation.m_Params.shape_combine_pass_pingpong)
        {
            CombinePassPingPong();
        }
        else
        {
            CombinePassCompute();
        }

        // lod-independent data
        for (int lodIdx = lodCount - 1; lodIdx >= 0; lodIdx--)
        {
//            buf.SetRenderTarget(_targets, 0, CubemapFace.Unknown, lodIdx);
            setRenderTarget(_targets, lodIdx);

            // draw any data that did not express a preference for one lod or another
            SubmitDrawsFiltered(lodIdx, _filterNoLodPreference);
        }

        saveTextur(_targets, "AnimWave.txt");
    }

    void CombinePassPingPong()
    {
        int lodCount = m_Clipmap.m_LodTransform.LodCount();
        gl.glDisable(GLenum.GL_CULL_FACE);
        gl.glDisable(GLenum.GL_BLEND);
        // combine waves
        for (int lodIdx = lodCount - 1; lodIdx >= 0; lodIdx--)
        {
            // The per-octave wave buffers
            BindWaveBuffer(m_ShaderData, false);

            // Bind this LOD data (displacements). Option to disable the combine pass - very useful debugging feature.
            if (_shapeCombinePass)
            {
                BindResultData(m_ShaderData);
            }
            else
            {
                BindNull(m_ShaderData, false);
            }

            // Dynamic waves
            if (m_Simulation._lodDataDynWaves != null)
            {
                m_Simulation._lodDataDynWaves.BindCopySettings(m_ShaderData);
                m_Simulation._lodDataDynWaves.BindResultData(m_ShaderData);
            }
            else
            {
                Wave_Simulation_Dynamic_Pass.BindNull(m_ShaderData, false);
            }

            // Flow
            if (m_Simulation._lodDataFlow != null)
            {
                m_Simulation._lodDataFlow.BindResultData(m_ShaderData);
            }
            else
            {
                Wave_Simulation_Flow_Pass.BindNull(m_ShaderData, false);
            }

//            _combineMaterial[lodIdx].SetFloat(LodDataMgr.sp_LD_SliceIndex, lodIdx);
            m_ShaderData._LD_SliceIndex = lodIdx;

            // Combine this LOD's waves with waves from the LODs above into auxiliary combine buffer
            /*buf.SetRenderTarget(_combineBuffer);
            buf.DrawProcedural(Matrix4x4.identity, _combineMaterial[lodIdx].material, shaderPassCombineIntoAux, MeshTopology.Triangles, 3);*/
            m_FBO.bind();
            m_FBO.setRenderTexture(_combineBuffer, null);
            gl.glViewport(0,0,_combineBuffer.getWidth(), _combineBuffer.getHeight());

            _combineMaterial.enable(m_ShaderData);
            gl.glDrawArrays(GLenum.GL_TRIANGLES, 0, 3);

            // Copy combine buffer back to lod texture array
            /*buf.SetRenderTarget(_targets, 0, CubemapFace.Unknown, lodIdx);
            _combineMaterial[lodIdx].SetTexture(Shader.PropertyToID("_CombineBuffer"), _combineBuffer);
            buf.DrawProcedural(Matrix4x4.identity, _combineMaterial[lodIdx].material, shaderPassCopyResultBack, MeshTopology.Triangles, 3);*/
            _combineMaterial.printOnce();

            setRenderTarget(_targets, lodIdx);
            /*m_ShaderData._CombineBuffer = _combineBuffer;
            _combineMaterial.enable(m_ShaderData);*/
            gl.glBindTextureUnit(0,_combineBuffer.getTexture());
            gl.glBindSampler(0,0);
            _copyBack.enable();

            gl.glDrawArrays(GLenum.GL_TRIANGLES, 0, 3);
        }
    }

    void CombinePassCompute()
    {
        int lodCount = m_Clipmap.m_LodTransform.LodCount();

        Technique combineShaderKernel = krnl_ShapeCombine;
        Technique combineShaderKernel_lastLOD = krnl_ShapeCombine_DISABLE_COMBINE;
        {
            boolean isFlowOn = m_Simulation._lodDataFlow != null;
            boolean isDynWavesOn = m_Simulation._lodDataDynWaves != null;
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
            Technique selectedShaderKernel;
            if (lodIdx < lodCount - 1 && _shapeCombinePass)
            {
                selectedShaderKernel = combineShaderKernel;
            }
            else
            {
                selectedShaderKernel = combineShaderKernel_lastLOD;
            }

            // The per-octave wave buffers
            BindWaveBuffer(/*_combineProperties*/m_ShaderData, false);
            // Bind this LOD data (displacements)
            BindResultData(/*_combineProperties,*/ m_ShaderData);

            // Dynamic waves
            if (m_Simulation._lodDataDynWaves != null)
            {
                m_Simulation._lodDataDynWaves.BindCopySettings(/*_combineProperties*/m_ShaderData);
                m_Simulation._lodDataDynWaves.BindResultData(/*_combineProperties*/m_ShaderData);
            }
            else
            {
                Wave_Simulation_Dynamic_Pass.BindNull(/*_combineProperties*/m_ShaderData, false);
            }

            // Flow
            if (m_Simulation._lodDataFlow != null)
            {
                m_Simulation._lodDataFlow.BindResultData(/*_combineProperties*/m_ShaderData);
            }
            else
            {
                Wave_Simulation_Flow_Pass.BindNull(/*_combineProperties*/m_ShaderData, false);
            }

            // Set the animated waves texture where the results will be combined.
            /*_combineProperties.SetTexture(
                    sp_LD_TexArray_AnimatedWaves_Compute,
                    DataTexture
            );

            _combineProperties.SetFloat(sp_LD_SliceIndex, lodIdx);
            _combineProperties.DispatchShader();*/

            m_ShaderData._LD_TexArray_AnimatedWaves_Compute = DataTexture();
            m_ShaderData._LD_SliceIndex = lodIdx;
            selectedShaderKernel.enable(m_ShaderData);

            gl.glDispatchCompute(
                    m_Clipmap.m_LodTransform.LodCount() / THREAD_GROUP_SIZE_X,
                    m_Clipmap.m_LodTransform.LodCount() / THREAD_GROUP_SIZE_Y,
                    1
            );

            selectedShaderKernel.setName("Combine Compute");
            selectedShaderKernel.printOnce();
        }
    }

    public void BindWaveBuffer(Wave_Simulation_ShaderData properties, boolean sourceLod /*= false*/)
    {
//        properties.SetTexture(/*Shader.PropertyToID("_LD_TexArray_WaveBuffer")*/0, _waveBuffers);
        properties._LD_TexArray_WaveBuffer = _waveBuffers;
        BindData(properties, null, true, m_Clipmap.m_LodTransform._renderData, sourceLod);
    }

    protected void BindData(Wave_Simulation_ShaderData properties, TextureGL applyData, boolean blendOut, Wave_LOD_Transform.RenderData[] renderData, boolean sourceLod /*= false*/)
    {
        super.BindData(properties, applyData, blendOut, renderData, sourceLod);

        Wave_LOD_Transform lt = m_Clipmap.m_LodTransform;

        for (int lodIdx = 0; lodIdx < m_Clipmap.m_LodTransform.LodCount(); lodIdx++)
        {
            // need to blend out shape if this is the largest lod, and the ocean might get scaled down later (so the largest lod will disappear)
            boolean needToBlendOutShape = lodIdx == m_Clipmap.m_LodTransform.LodCount() - 1 && m_Clipmap.scaleCouldDecrease() && blendOut;
            float shapeWeight = needToBlendOutShape ? m_Clipmap.getViewerAltitudeLevelAlpha() : 1f;
            _BindData_paramIdOceans[lodIdx].set(
                    lt._renderData[lodIdx]._texelWidth,
                    lt._renderData[lodIdx]._textureRes, shapeWeight,
                    1f / lt._renderData[lodIdx]._textureRes);
        }
//        properties.SetVectorArray(LodTransform.ParamIdOcean(sourceLod), _BindData_paramIdOceans);
        if(sourceLod){
            properties._LD_Params_Source = _BindData_paramIdOceans;
        }else{
            properties._LD_Params = _BindData_paramIdOceans;
        }
    }

    /// <summary>
    /// Returns index of lod that completely covers the sample area, and contains wavelengths that repeat no more than twice across the smaller
    /// spatial length. If no such lod available, returns -1. This means high frequency wavelengths are filtered out, and the lod index can
    /// be used for each sample in the sample area.
    /// </summary>
    public int SuggestDataLOD(Rectf sampleAreaXZ)
    {
        return SuggestDataLOD(sampleAreaXZ, Math.min(sampleAreaXZ.width, sampleAreaXZ.height));
    }
    public int SuggestDataLOD(Rectf sampleAreaXZ, float minSpatialLength)
    {
        int lodCount = m_Clipmap.m_LodTransform.LodCount();
        Wave_LOD_Transform lt = m_Clipmap.m_LodTransform;

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

    /*public static String TextureArrayName = "_LD_TexArray_AnimatedWaves";
    private static LodDataMgr.TextureArrayParamIds textureArrayParamIds = new LodDataMgr.TextureArrayParamIds(TextureArrayName);
    public static int ParamIdSampler(boolean sourceLod *//*= false*//*) { return textureArrayParamIds.GetId(sourceLod); }
    protected int GetParamIdSampler(boolean sourceLod *//*= false*//*)
    {
        return ParamIdSampler(sourceLod);
    }*/
    public static void BindNull(Wave_Simulation_ShaderData properties, boolean sourceLod /*= false*/)
    {
//        properties.SetTexture(ParamIdSampler(sourceLod), /*TextureArrayHelpers.BlackTextureArray*/null);
        if(sourceLod){
            properties._LD_TexArray_AnimatedWaves_Source = null;
        }else{
            properties._LD_TexArray_AnimatedWaves = null;
        }
    }

    @Override
    protected void applySampler(Wave_Simulation_ShaderData properties, boolean sourceLod, TextureGL applyData) {
        if(sourceLod){
            properties._LD_TexArray_AnimatedWaves_Source = applyData;
        }else{
            properties._LD_TexArray_AnimatedWaves = applyData;
        }
    }

    private static final HashSet<String> gSavedTextures = new HashSet<>();
    static void saveTextur(TextureGL tex, String filename){
        filename = "E:/textures/crest/" +filename;
        if(!gSavedTextures.contains(filename)){
            try {
                DebugTools.saveTextureAsText(tex.getTarget(),tex.getTexture(), 0, filename);
            } catch (IOException e) {
                e.printStackTrace();
            }

            gSavedTextures.add(filename);
        }
    }
}
