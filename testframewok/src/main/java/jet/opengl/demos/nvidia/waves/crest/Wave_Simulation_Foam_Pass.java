package jet.opengl.demos.nvidia.waves.crest;

import jet.opengl.demos.nvidia.waves.ocean.Technique;
import jet.opengl.postprocessing.util.Numeric;

/** A persistent foam simulation that moves around with a displacement LOD. The input is fully combined water surface shape. */
final class Wave_Simulation_Foam_Pass extends Wave_Simulation_Persistent_Pass{
    @Override
    protected String ShaderSim () { return "UpdateFoam";  }
    @Override
    protected Technique krnl_ShaderSim () { return ShaderManager.getInstance().getProgram(ShaderSim());}
    public String SimName () { return "Foam"; }
    public int TextureFormat (){ return m_Simulation.m_Params.foam_texture_format; }

//    static int sp_FoamFadeRate = 0; //Shader.PropertyToID("_FoamFadeRate");
//    static int sp_WaveFoamStrength = 1; //Shader.PropertyToID("_WaveFoamStrength");
//    static int sp_WaveFoamCoverage = 2; //Shader.PropertyToID("_WaveFoamCoverage");
//    static int sp_ShorelineFoamMaxDepth = 3; //Shader.PropertyToID("_ShorelineFoamMaxDepth");
//    static int sp_ShorelineFoamStrength = 4; //Shader.PropertyToID("_ShorelineFoamStrength");

    protected void SetAdditionalSimParams(Wave_Simulation_ShaderData simMaterial)
    {
        super.SetAdditionalSimParams(simMaterial);

//        simMaterial.SetFloat(sp_FoamFadeRate, Settings()._foamFadeRate);
//        simMaterial.SetFloat(sp_WaveFoamStrength, Settings()._waveFoamStrength);
//        simMaterial.SetFloat(sp_WaveFoamCoverage, Settings()._waveFoamCoverage);
//        simMaterial.SetFloat(sp_ShorelineFoamMaxDepth, Settings()._shorelineFoamMaxDepth);
//        simMaterial.SetFloat(sp_ShorelineFoamStrength, Settings()._shorelineFoamStrength);

        simMaterial._FoamFadeRate = m_Simulation.m_Params.foam_fade_rate;
        simMaterial._WaveFoamStrength = m_Simulation.m_Params.foam_strength;
        simMaterial._WaveFoamCoverage = m_Simulation.m_Params.foam_coverage;
        simMaterial._ShorelineFoamMaxDepth = m_Simulation.m_Params.shoreline_foam_maxdepth;
        simMaterial._ShorelineFoamStrength = m_Simulation.m_Params.shoreline_foam_strength;

        // assign animated waves - to slot 1 current frame data
        m_Simulation._lodDataAnimWaves.BindResultData(simMaterial);

        // assign sea floor depth - to slot 1 current frame data
        if (m_Simulation._lodDataSeaDepths != null)
        {
            m_Simulation._lodDataSeaDepths.BindResultData(simMaterial);
        }
        else
        {
            Wave_Simulation_SeaFloorDepth_Pass.BindNull(simMaterial, false);
        }

        // assign flow - to slot 1 current frame data
        if (m_Simulation._lodDataFlow != null)
        {
            m_Simulation._lodDataFlow.BindResultData(simMaterial);
        }
        else
        {
            Wave_Simulation_Flow_Pass.BindNull(simMaterial, false);
        }
    }

    @Override
    public long GetSimSubstepData(float frameDt/*, out int numSubsteps, out float substepDt*/)
    {
        // foam always does just one sim step
        float substepDt = frameDt;
        int numSubsteps = 1;

        return Numeric.encode(numSubsteps, Float.floatToIntBits(substepDt));
    }

    /*public static String TextureArrayName = "_LD_TexArray_Foam";
    private static LodDataMgr.TextureArrayParamIds textureArrayParamIds = new LodDataMgr.TextureArrayParamIds(TextureArrayName);
    public static int ParamIdSampler(boolean sourceLod *//*= false*//*) { return textureArrayParamIds.GetId(sourceLod); }
    protected int GetParamIdSampler(boolean sourceLod *//*= false*//*)
    {
        return ParamIdSampler(sourceLod);
    }*/
    public static void BindNull(Wave_Simulation_ShaderData properties, boolean sourceLod /*= false*/)
    {
//        properties.SetTexture(ParamIdSampler(sourceLod), TextureArrayHelpers.BlackTextureArray);
        if(sourceLod){
            properties._LD_TexArray_Foam_Source = null;
        }else
            properties._LD_TexArray_Foam = null;
    }
}
