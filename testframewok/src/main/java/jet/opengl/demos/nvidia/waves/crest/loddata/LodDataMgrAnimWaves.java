package jet.opengl.demos.nvidia.waves.crest.loddata;

import com.nvidia.developer.opengl.models.obj.Material;

import jet.opengl.demos.nvidia.waves.crest.OceanRenderer;
import jet.opengl.demos.nvidia.waves.crest.helpers.PropertyWrapperMaterial;
import jet.opengl.postprocessing.common.GLenum;
import jet.opengl.postprocessing.shader.GLSLProgram;
import jet.opengl.postprocessing.texture.Texture2D;
import jet.opengl.postprocessing.texture.Texture2DDesc;
import jet.opengl.postprocessing.texture.TextureUtils;

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
        _combineShader = Resources.Load<ComputeShader>(ShaderName);
        krnl_ShapeCombine = _combineShader.FindKernel("ShapeCombine");
        krnl_ShapeCombine_DISABLE_COMBINE = _combineShader.FindKernel("ShapeCombine_DISABLE_COMBINE");
        krnl_ShapeCombine_FLOW_ON = _combineShader.FindKernel("ShapeCombine_FLOW_ON");
        krnl_ShapeCombine_FLOW_ON_DISABLE_COMBINE = _combineShader.FindKernel("ShapeCombine_FLOW_ON_DISABLE_COMBINE");
        krnl_ShapeCombine_DYNAMIC_WAVE_SIM_ON = _combineShader.FindKernel("ShapeCombine_DYNAMIC_WAVE_SIM_ON");
        krnl_ShapeCombine_DYNAMIC_WAVE_SIM_ON_DISABLE_COMBINE = _combineShader.FindKernel("ShapeCombine_DYNAMIC_WAVE_SIM_ON_DISABLE_COMBINE");
        krnl_ShapeCombine_FLOW_ON_DYNAMIC_WAVE_SIM_ON = _combineShader.FindKernel("ShapeCombine_FLOW_ON_DYNAMIC_WAVE_SIM_ON");
        krnl_ShapeCombine_FLOW_ON_DYNAMIC_WAVE_SIM_ON_DISABLE_COMBINE = _combineShader.FindKernel("ShapeCombine_FLOW_ON_DYNAMIC_WAVE_SIM_ON_DISABLE_COMBINE");
//        _combineProperties = new PropertyWrapperCompute();

        int resolution = OceanRenderer.Instance.LodDataResolution();
        Texture2DDesc desc = new Texture2DDesc(resolution, resolution, TextureFormat());

        _waveBuffers = (Texture2D) CreateLodDataTextures(desc, "WaveBuffer", false);

        _combineBuffer = TextureUtils.createTexture2D(desc, null);

        var combineShader = Shader.Find("Hidden/Crest/Simulation/Combine Animated Wave LODs");
        _combineMaterial = new PropertyWrapperMaterial[OceanRenderer.Instance.CurrentLodCount];
        for (int i = 0; i < _combineMaterial.Length; i++)
        {
            Material mat = new Material(combineShader);
            _combineMaterial[i] = new PropertyWrapperMaterial(mat);
        }
    }

}
