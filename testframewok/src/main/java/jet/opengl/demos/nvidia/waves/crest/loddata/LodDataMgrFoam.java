package jet.opengl.demos.nvidia.waves.crest.loddata;

import jet.opengl.demos.nvidia.waves.crest.OceanRenderer;
import jet.opengl.demos.nvidia.waves.crest.helpers.IPropertyWrapper;
import jet.opengl.demos.nvidia.waves.crest.helpers.TextureArrayHelpers;
import jet.opengl.postprocessing.shader.GLSLProgram;
import jet.opengl.postprocessing.util.Numeric;

/** A persistent foam simulation that moves around with a displacement LOD. The input is fully combined water surface shape. */
public class LodDataMgrFoam extends LodDataMgrPersistent {

    protected String ShaderSim () { return "UpdateFoam";  }
    protected GLSLProgram krnl_ShaderSim () { return _shader.FindKernel(ShaderSim);}
    public String SimName () { return "Foam"; }
    public int TextureFormat (){ return Settings()._renderTextureFormat; }

    SimSettingsFoam Settings() {return OceanRenderer.Instance._simSettingsFoam; }
    public void UseSettings(SimSettingsBase settings) { OceanRenderer.Instance._simSettingsFoam = (SimSettingsFoam)settings; }
    public SimSettingsBase CreateDefaultSettings()
    {
        SimSettingsFoam settings = new SimSettingsFoam();
        settings.name = SimName() + " Auto-generated Settings";
        return settings;
    }

    static int sp_FoamFadeRate = 0; //Shader.PropertyToID("_FoamFadeRate");
    static int sp_WaveFoamStrength = 1; //Shader.PropertyToID("_WaveFoamStrength");
    static int sp_WaveFoamCoverage = 2; //Shader.PropertyToID("_WaveFoamCoverage");
    static int sp_ShorelineFoamMaxDepth = 3; //Shader.PropertyToID("_ShorelineFoamMaxDepth");
    static int sp_ShorelineFoamStrength = 4; //Shader.PropertyToID("_ShorelineFoamStrength");


    protected void Start()
    {
        super.Start();

//#if UNITY_EDITOR
//        if (!OceanRenderer.Instance.OceanMaterial.IsKeywordEnabled("_FOAM_ON"))
//        {
//            Debug.LogWarning("Foam is not enabled on the current ocean material and will not be visible.", this);
//        }
//#endif
    }

    protected void SetAdditionalSimParams(IPropertyWrapper simMaterial)
    {
        super.SetAdditionalSimParams(simMaterial);

        simMaterial.SetFloat(sp_FoamFadeRate, Settings()._foamFadeRate);
        simMaterial.SetFloat(sp_WaveFoamStrength, Settings()._waveFoamStrength);
        simMaterial.SetFloat(sp_WaveFoamCoverage, Settings()._waveFoamCoverage);
        simMaterial.SetFloat(sp_ShorelineFoamMaxDepth, Settings()._shorelineFoamMaxDepth);
        simMaterial.SetFloat(sp_ShorelineFoamStrength, Settings()._shorelineFoamStrength);

        // assign animated waves - to slot 1 current frame data
        OceanRenderer.Instance._lodDataAnimWaves.BindResultData(simMaterial);

        // assign sea floor depth - to slot 1 current frame data
        if (OceanRenderer.Instance._lodDataSeaDepths != null)
        {
            OceanRenderer.Instance._lodDataSeaDepths.BindResultData(simMaterial);
        }
        else
        {
            LodDataMgrSeaFloorDepth.BindNull(simMaterial, false);
        }

        // assign flow - to slot 1 current frame data
        if (OceanRenderer.Instance._lodDataFlow != null)
        {
            OceanRenderer.Instance._lodDataFlow.BindResultData(simMaterial);
        }
        else
        {
            LodDataMgrFlow.BindNull(simMaterial, false);
        }
    }

    public long GetSimSubstepData(float frameDt/*, out int numSubsteps, out float substepDt*/)
    {
        // foam always does just one sim step
        float substepDt = frameDt;
        int numSubsteps = 1;

        return Numeric.encode(numSubsteps, Float.floatToIntBits(substepDt));
    }

    public static String TextureArrayName = "_LD_TexArray_Foam";
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
