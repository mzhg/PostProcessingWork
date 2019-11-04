package jet.opengl.demos.nvidia.waves.crest;

import jet.opengl.demos.nvidia.waves.ocean.Technique;
import jet.opengl.postprocessing.common.GLenum;
import jet.opengl.postprocessing.texture.TextureGL;
import jet.opengl.postprocessing.util.Numeric;

final class Wave_Simulation_Dynamic_Pass extends Wave_Simulation_Persistent_Pass {

    @Override
    protected String ShaderSim () { return "UpdateDynWaves";  }
    @Override
    protected Technique krnl_ShaderSim () { return ShaderManager.getInstance().getProgram(ShaderSim()); }

    @Override
    public String SimName () { return "DynamicWaves"; }
    @Override
    public int TextureFormat () { return GLenum.GL_RG16F; }

    public boolean _rotateLaplacian = true;

    private final String DYNWAVES_KEYWORD = "_DYNAMIC_WAVE_SIM_ON";  // TODO this important.

    boolean[] _active;
    public boolean simActive(int lodIdx) { return _active[lodIdx]; }

    @Override
    protected void InitData()
    {
        super.InitData();

        _active = new boolean[OceanRenderer.Instance.CurrentLodCount()];
        for (int i = 0; i < _active.length; i++) _active[i] = true;
    }

    @Override
    protected boolean BuildCommandBufferInternal(int lodIdx)
    {
        if (!super.BuildCommandBufferInternal(lodIdx))
            return false;

        final Wave_Simulation_Params params = m_Simulation.m_Params;
        // check if the sim should be running
        float texelWidth = m_Clipmap.m_LodTransform._renderData[lodIdx]._texelWidth;
        _active[lodIdx] = texelWidth >= /*Settings()._minGridSize*/ params.min_gridsize &&
                (texelWidth <= /*Settings()._maxGridSize*/params.max_gridsize || /*Settings()._maxGridSize*/params.max_gridsize == 0f);

        return true;
    }

    public void BindCopySettings(Wave_Simulation_ShaderData target)
    {
//        target.SetFloat(sp_HorizDisplace, Settings()._horizDisplace);
//        target.SetFloat(sp_DisplaceClamp, Settings()._displaceClamp);
        final Wave_Simulation_Params params = m_Simulation.m_Params;
        target._HorizDisplace = params.horiz_displace;
        target._DisplaceClamp = params.displace_clamp;
    }

    protected void SetAdditionalSimParams(Wave_Simulation_ShaderData simMaterial)
    {
        super.SetAdditionalSimParams(simMaterial);

        final Wave_Simulation_Params params = m_Simulation.m_Params;
//        simMaterial.SetFloat(sp_Damping, Settings()._damping);
//        simMaterial.SetFloat(sp_Gravity, /*OceanRenderer.Instance.Gravity*/9.8f * Settings()._gravityMultiplier);
        simMaterial._Damping = params.damping;
        simMaterial._Gravity = 9.8f * params.gravityMultiplier;

        float laplacianKernelAngle = _rotateLaplacian ? Numeric.PI * 2f * Numeric.random() : 0f;
//        simMaterial.SetVector(sp_LaplacianAxisX, new Vector4f((float)Math.cos(laplacianKernelAngle), (float)Math.sin(laplacianKernelAngle),0,0));
        simMaterial._LaplacianAxisX.set((float)Math.sin(laplacianKernelAngle), (float)Math.cos(laplacianKernelAngle),0,0);
        // assign sea floor depth - to slot 1 current frame data. minor bug here - this depth will actually be from the previous frame,
        // because the depth is scheduled to render just before the animated waves, and this sim happens before animated waves.
        if (m_Simulation._lodDataSeaDepths!= null)
        {
            m_Simulation._lodDataSeaDepths.BindResultData(simMaterial);
        }
        else
        {
            Wave_Simulation_SeaFloorDepth_Pass.BindNull(simMaterial, false);
        }

        if (m_Simulation._lodDataFlow != null)
        {
            m_Simulation._lodDataFlow.BindResultData(simMaterial);
        }
        else
        {
            Wave_Simulation_Flow_Pass.BindNull(simMaterial, false);
        }
    }

    /** present int first, active in second. */
    public long CountWaveSims(int countFrom)
    {
        int o_present = m_Clipmap.m_LodTransform.LodCount();
        int o_active = 0;
        for (int i = 0; i < o_present; i++)
        {
            if (i < countFrom) continue;
            if (!m_Simulation._lodDataDynWaves.simActive(i)) continue;

            o_active++;
        }

        return Numeric.encode(o_present, o_active);
    }

    float maxSimDt(int lodIdx)
    {
        // Limit timestep based on Courant constant: https://www.uio.no/studier/emner/matnat/ifi/nedlagte-emner/INF2340/v05/foiler/sim04.pdf
        float Cmax = /*Settings()._courantNumber*/ m_Simulation.m_Params.courant_number;
        float minWavelength = m_Clipmap.m_LodTransform.MaxWavelength(lodIdx) / 2f;
        float waveSpeed = (float) Wave_Spectrum.computeWaveSpeed(minWavelength, m_Simulation.m_Params.gravityMultiplier);
        // 0.5f because its 2D
        float maxDt = 0.5f * Cmax * m_Clipmap.calcGridSize(lodIdx) / waveSpeed;
        return maxDt;
    }

    public long GetSimSubstepData(float frameDt/*, out int numSubsteps, out float substepDt*/)
    {
        OceanRenderer ocean = OceanRenderer.Instance;

        // lod 0 will always be most demanding - wave speed is square root of wavelength, so waves will be fast relative to stability in
        // lowest lod, and slow relative to stability in largest lod.
        float maxDt = maxSimDt(0);

        int numSubsteps = (int)Math.ceil(frameDt / maxDt);
        // Always do at least one step so that the sim moves around when time is frozen
        numSubsteps = Numeric.clamp(numSubsteps, 1, /*Settings()._maxSimStepsPerFrame*/m_Simulation.m_Params.max_simsteps_perframe);
        float substepDt = Math.min(maxDt, frameDt / numSubsteps);

        return Numeric.encode(numSubsteps, Float.floatToIntBits(substepDt));
    }

    /*public static String TextureArrayName = "_LD_TexArray_DynamicWaves";
    private static LodDataMgr.TextureArrayParamIds textureArrayParamIds = new LodDataMgr.TextureArrayParamIds(TextureArrayName);
    public static int ParamIdSampler(boolean sourceLod *//*= false*//*) { return textureArrayParamIds.GetId(sourceLod); }*/
//    protected int GetParamIdSampler(boolean sourceLod /*= false*/)
//    {
//        throw new UnsupportedOperationException();
//    }
    public static void BindNull(Wave_Simulation_ShaderData properties, boolean sourceLod /*= false*/)
    {
//        properties.SetTexture(ParamIdSampler(sourceLod), TextureArrayHelpers.BlackTextureArray);
        if(sourceLod){
            properties._LD_TexArray_DynamicWaves_Source = null;
        }else{
            properties._LD_TexArray_DynamicWaves = null;
        }
    }

    @Override
    protected void applySampler(Wave_Simulation_ShaderData properties, boolean sourceLod, TextureGL applyData) {
        if(sourceLod){
            properties._LD_TexArray_DynamicWaves_Source = applyData;
        }else{
            properties._LD_TexArray_DynamicWaves = applyData;
        }
    }
}
