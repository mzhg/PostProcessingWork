package jet.opengl.demos.nvidia.waves.crest.loddata;

import org.lwjgl.util.vector.Vector4f;

import jet.opengl.demos.nvidia.waves.crest.OceanRenderer;
import jet.opengl.demos.nvidia.waves.crest.helpers.IPropertyWrapper;
import jet.opengl.demos.nvidia.waves.crest.helpers.TextureArrayHelpers;
import jet.opengl.demos.nvidia.waves.crest.shapes.OceanWaveSpectrum;
import jet.opengl.postprocessing.common.GLenum;
import jet.opengl.postprocessing.shader.GLSLProgram;
import jet.opengl.postprocessing.util.Numeric;

/** A dynamic shape simulation that moves around with a displacement LOD.*/
public class LodDataMgrDynWaves extends LodDataMgrPersistent {

    protected String ShaderSim () { return "UpdateDynWaves";  }
    protected GLSLProgram krnl_ShaderSim () { return /*_shader.FindKernel(ShaderSim())*/null; }

    public String SimName () { return "DynamicWaves"; }
    public int TextureFormat () { return GLenum.GL_RG16F; }

    SimSettingsWave Settings (){ return OceanRenderer.Instance._simSettingsDynamicWaves;  }
    public void UseSettings(SimSettingsBase settings) { OceanRenderer.Instance._simSettingsDynamicWaves = (SimSettingsWave)settings; }
    public SimSettingsBase CreateDefaultSettings()
    {
        SimSettingsWave settings = new SimSettingsWave();
        settings.name = SimName() + " Auto-generated Settings";
        return settings;
    }

    public boolean _rotateLaplacian = true;

    public final String DYNWAVES_KEYWORD = "_DYNAMIC_WAVE_SIM_ON";

    boolean[] _active;
    public boolean SimActive(int lodIdx) { return _active[lodIdx]; }

    static int sp_HorizDisplace = 0; //Shader.PropertyToID("_HorizDisplace");
    static int sp_DisplaceClamp = 1; //Shader.PropertyToID("_DisplaceClamp");
    static int sp_Damping = 2; //Shader.PropertyToID("_Damping");
    static int sp_Gravity = 3; //Shader.PropertyToID("_Gravity");
    static int sp_LaplacianAxisX = 4; //Shader.PropertyToID("_LaplacianAxisX");

    protected void InitData()
    {
        super.InitData();

        _active = new boolean[OceanRenderer.Instance.CurrentLodCount()];
        for (int i = 0; i < _active.length; i++) _active[i] = true;
    }

    protected void OnEnable()
    {
//        Shader.EnableKeyword(DYNWAVES_KEYWORD);
    }

    protected void OnDisable()
    {
//        Shader.DisableKeyword(DYNWAVES_KEYWORD);
    }

    protected boolean BuildCommandBufferInternal(int lodIdx)
    {
        if (!super.BuildCommandBufferInternal(lodIdx))
            return false;

        // check if the sim should be running
        float texelWidth = OceanRenderer.Instance._lodTransform._renderData[lodIdx].Validate(0, this)._texelWidth;
        _active[lodIdx] = texelWidth >= Settings()._minGridSize && (texelWidth <= Settings()._maxGridSize || Settings()._maxGridSize == 0f);

        return true;
    }

    public void BindCopySettings(IPropertyWrapper target)
    {
        target.SetFloat(sp_HorizDisplace, Settings()._horizDisplace);
        target.SetFloat(sp_DisplaceClamp, Settings()._displaceClamp);
    }

    protected void SetAdditionalSimParams(IPropertyWrapper simMaterial)
    {
        super.SetAdditionalSimParams(simMaterial);

        simMaterial.SetFloat(sp_Damping, Settings()._damping);
        simMaterial.SetFloat(sp_Gravity, /*OceanRenderer.Instance.Gravity*/9.8f * Settings()._gravityMultiplier);

        float laplacianKernelAngle = _rotateLaplacian ? Numeric.PI * 2f * Numeric.random() : 0f;
        simMaterial.SetVector(sp_LaplacianAxisX, new Vector4f((float)Math.cos(laplacianKernelAngle), (float)Math.sin(laplacianKernelAngle),0,0));

        // assign sea floor depth - to slot 1 current frame data. minor bug here - this depth will actually be from the previous frame,
        // because the depth is scheduled to render just before the animated waves, and this sim happens before animated waves.
        if (OceanRenderer.Instance._lodDataSeaDepths!= null)
        {
            OceanRenderer.Instance._lodDataSeaDepths.BindResultData(simMaterial);
        }
        else
        {
            LodDataMgrSeaFloorDepth.BindNull(simMaterial, false);
        }

        if (OceanRenderer.Instance._lodDataFlow != null)
        {
            OceanRenderer.Instance._lodDataFlow.BindResultData(simMaterial);
        }
        else
        {
            LodDataMgrFlow.BindNull(simMaterial, false);
        }
    }

    /** present int first, active in second. */
    public static long CountWaveSims(int countFrom)
    {
        int o_present = OceanRenderer.Instance.CurrentLodCount();
        int o_active = 0;
        for (int i = 0; i < o_present; i++)
        {
            if (i < countFrom) continue;
            if (!OceanRenderer.Instance._lodDataDynWaves.SimActive(i)) continue;

            o_active++;
        }

        return Numeric.encode(o_present, o_active);
    }

    float MaxSimDt(int lodIdx)
    {
        OceanRenderer ocean = OceanRenderer.Instance;

        // Limit timestep based on Courant constant: https://www.uio.no/studier/emner/matnat/ifi/nedlagte-emner/INF2340/v05/foiler/sim04.pdf
        float Cmax = Settings()._courantNumber;
        float minWavelength = ocean._lodTransform.MaxWavelength(lodIdx) / 2f;
        float waveSpeed = (float) OceanWaveSpectrum.ComputeWaveSpeed(minWavelength, Settings()._gravityMultiplier);
        // 0.5f because its 2D
        float maxDt = 0.5f * Cmax * ocean.CalcGridSize(lodIdx) / waveSpeed;
        return maxDt;
    }

    public long GetSimSubstepData(float frameDt/*, out int numSubsteps, out float substepDt*/)
    {
        OceanRenderer ocean = OceanRenderer.Instance;

        // lod 0 will always be most demanding - wave speed is square root of wavelength, so waves will be fast relative to stability in
        // lowest lod, and slow relative to stability in largest lod.
        float maxDt = MaxSimDt(0);

        int numSubsteps = (int)Math.ceil(frameDt / maxDt);
        // Always do at least one step so that the sim moves around when time is frozen
        numSubsteps = Numeric.clamp(numSubsteps, 1, Settings()._maxSimStepsPerFrame);
        float substepDt = Math.min(maxDt, frameDt / numSubsteps);

        return Numeric.encode(numSubsteps, Float.floatToIntBits(substepDt));
    }

    public static String TextureArrayName = "_LD_TexArray_DynamicWaves";
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
