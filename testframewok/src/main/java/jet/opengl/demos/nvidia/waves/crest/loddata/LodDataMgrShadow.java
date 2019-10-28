package jet.opengl.demos.nvidia.waves.crest.loddata;

import org.lwjgl.util.vector.Transform;

import jet.opengl.demos.intel.fluid.scene.Light;
import jet.opengl.demos.intel.fluid.utils.Camera;
import jet.opengl.demos.nvidia.waves.crest.OceanRenderer;
import jet.opengl.demos.nvidia.waves.crest.helpers.IPropertyWrapper;
import jet.opengl.demos.nvidia.waves.crest.helpers.PropertyWrapperCompute;
import jet.opengl.demos.nvidia.waves.crest.helpers.TextureArrayHelpers;
import jet.opengl.demos.nvidia.waves.crest.helpers.Time;
import jet.opengl.postprocessing.buffer.BufferGL;
import jet.opengl.postprocessing.common.GLenum;
import jet.opengl.postprocessing.shader.GLSLProgram;
import jet.opengl.postprocessing.texture.Texture2DDesc;
import jet.opengl.postprocessing.texture.TextureGL;
import jet.opengl.postprocessing.util.LogUtil;

/**
 *  Stores shadowing data to use during ocean shading. Shadowing is persistent and supports sampling across
 *  many frames and jittered sampling for (very) soft shadows.
 */
public class LodDataMgrShadow extends LodDataMgr {
    public String SimName () { return "Shadow"; }
    public int TextureFormat() { return GLenum.GL_RG16F; }
    protected boolean NeedToReadWriteTextureData () { return true; }

    public static boolean s_processData = true;

    Light _mainLight;
    Camera _cameraMain;

    // LWRP version needs access to this externally, hence public get
    public BufferGL BufCopyShadowMap;

    TextureGL _sources;
    PropertyWrapperCompute _renderProperties;
    GLSLProgram _updateShadowShader;
    private int krnl_UpdateShadow;
    public final String UpdateShadow = "UpdateShadow";

    static int sp_CenterPos = 0; //Shader.PropertyToID("_CenterPos");
    static int sp_Scale = 1; //Shader.PropertyToID("_Scale");
    static int sp_CamPos = 2; //Shader.PropertyToID("_CamPos");
    static int sp_CamForward = 3; //Shader.PropertyToID("_CamForward");
    static int sp_JitterDiameters_CurrentFrameWeights = 4; //Shader.PropertyToID("_JitterDiameters_CurrentFrameWeights");
    static int sp_MainCameraProjectionMatrix = 5; //Shader.PropertyToID("_MainCameraProjectionMatrix");
    static int sp_SimDeltaTime = 6; //Shader.PropertyToID("_SimDeltaTime");
    static int sp_LD_SliceIndex_Source = 7; //Shader.PropertyToID("_LD_SliceIndex_Source");
    static int sp_LD_TexArray_Target = 8; //Shader.PropertyToID("_LD_TexArray_Target");

    SimSettingsShadow Settings() { return OceanRenderer.Instance._simSettingsShadow; }
    public void UseSettings(SimSettingsBase settings) { OceanRenderer.Instance._simSettingsShadow = (SimSettingsShadow)settings; }
    public SimSettingsBase CreateDefaultSettings()
    {
        SimSettingsShadow settings = new SimSettingsShadow();
        settings.name = SimName() + " Auto-generated Settings";
        return settings;
    }

    protected void Start()
    {
        super.Start();

        _renderProperties = new PropertyWrapperCompute();
//        _updateShadowShader = Resources.Load<ComputeShader>(UpdateShadow);  todo

        try
        {
//            krnl_UpdateShadow = _updateShadowShader.FindKernel(UpdateShadow); todo
        }
        catch (Exception e)
        {
            LogUtil.e(LogUtil.LogType.DEFAULT, "Could not load shadow update kernel. Disabling shadows.");
            enabled = false;
            return;
        }

//        _cameraMain = Camera.main; todo
        if (_cameraMain == null)
        {
            Transform viewpoint = OceanRenderer.Instance.Viewpoint();
//            _cameraMain = viewpoint != null ? viewpoint.GetComponent<Camera>() : null;

            if (_cameraMain == null)
            {
                LogUtil.e(LogUtil.LogType.DEFAULT, "Could not find main camera, disabling shadow data");
                enabled = false;
                return;
            }
        }

/*#if UNITY_EDITOR
        if (!OceanRenderer.Instance.OceanMaterial.IsKeywordEnabled("_SHADOWS_ON"))
        {
            Debug.LogWarning("Shadowing is not enabled on the current ocean material and will not be visible.", this);
        }
#endif*/
    }

    protected void InitData()
    {
        super.InitData();

        int resolution = OceanRenderer.Instance.LodDataResolution();
        Texture2DDesc desc = new Texture2DDesc(resolution, resolution, TextureFormat());
        _sources = CreateLodDataTextures(desc, SimName() + "_1", NeedToReadWriteTextureData());

        TextureArrayHelpers.ClearToBlack(_sources);
        TextureArrayHelpers.ClearToBlack(_targets);
    }

    boolean StartInitLight()
    {
        _mainLight = OceanRenderer.Instance._primaryLight;

        /*if (_mainLight.type != LightType.Directional)  todo
        {
            LogUtil.e(LogUtil.LogType.DEFAULT, ()->"Primary light must be of type Directional.");
            return false;
        }

        if (_mainLight.shadows == LightShadows.None)
        {
            LogUtil.e(LogUtil.LogType.DEFAULT, ()->"Shadows must be enabled on primary light to enable ocean shadowing (types Hard and Soft are equivalent for the ocean system).");
            return false;
        }*/

        return true;
    }

    public void UpdateLodData()
    {
        if (!enabled)
        {
            return;
        }

        super.UpdateLodData();

        if (_mainLight != OceanRenderer.Instance._primaryLight)
        {
            if (_mainLight != null)
            {
//                _mainLight.RemoveCommandBuffer(LightEvent.BeforeScreenspaceMask, BufCopyShadowMap); todo
                BufCopyShadowMap = null;
                TextureArrayHelpers.ClearToBlack(_sources);
                TextureArrayHelpers.ClearToBlack(_targets);
            }
            _mainLight = null;
        }

        if (OceanRenderer.Instance._primaryLight == null)
        {
            if (!Settings()._allowNullLight)
            {
                LogUtil.i(LogUtil.LogType.DEFAULT, "Primary light must be specified on OceanRenderer script to enable shadows.");
            }
            return;
        }

        if (_mainLight == null)
        {
            if (!StartInitLight())
            {
                enabled = false;
                return;
            }
        }

        if (BufCopyShadowMap == null && s_processData)
        {
            BufCopyShadowMap = new BufferGL();
            BufCopyShadowMap.setName("Shadow data");
//            _mainLight.AddCommandBuffer(LightEvent.BeforeScreenspaceMask, BufCopyShadowMap);
        }
        else if (!s_processData && BufCopyShadowMap != null)
        {
//            _mainLight.RemoveCommandBuffer(LightEvent.BeforeScreenspaceMask, BufCopyShadowMap);
            BufCopyShadowMap = null;
        }

        if (!s_processData)
        {
            return;
        }

//        SwapRTs(ref _sources, ref _targets);
        {

        }

//        BufCopyShadowMap.Clear();

        ValidateSourceData();

        // clear the shadow collection. it will be overwritten with shadow values IF the shadows render,
        // which only happens if there are (nontransparent) shadow receivers around
        TextureArrayHelpers.ClearToBlack(_targets);

        LodTransform lt = OceanRenderer.Instance._lodTransform;
        for (int lodIdx = lt.LodCount() - 1; lodIdx >= 0; lodIdx--)
        {
            /*_renderProperties.Initialise(BufCopyShadowMap, _updateShadowShader, krnl_UpdateShadow);  todo

            lt._renderData[lodIdx].Validate(0, this);
            _renderProperties.SetVector(sp_CenterPos, lt._renderData[lodIdx]._posSnapped);
            var scale = OceanRenderer.Instance.CalcLodScale(lodIdx);
            _renderProperties.SetVector(sp_Scale, new Vector3(scale, 1f, scale));
            _renderProperties.SetVector(sp_CamPos, OceanRenderer.Instance.Viewpoint.position);
            _renderProperties.SetVector(sp_CamForward, OceanRenderer.Instance.Viewpoint.forward);
            _renderProperties.SetVector(sp_JitterDiameters_CurrentFrameWeights, new Vector4(Settings._jitterDiameterSoft, Settings._jitterDiameterHard, Settings._currentFrameWeightSoft, Settings._currentFrameWeightHard));
            _renderProperties.SetMatrix(sp_MainCameraProjectionMatrix, _cameraMain.projectionMatrix * _cameraMain.worldToCameraMatrix);
            _renderProperties.SetFloat(sp_SimDeltaTime, OceanRenderer.Instance.DeltaTimeDynamics);

            // compute which lod data we are sampling previous frame shadows from. if a scale change has happened this can be any lod up or down the chain.
            var srcDataIdx = lodIdx + ScaleDifferencePow2;
            srcDataIdx = Mathf.Clamp(srcDataIdx, 0, lt.LodCount - 1);
            _renderProperties.SetFloat(sp_LD_SliceIndex, lodIdx);
            _renderProperties.SetFloat(sp_LD_SliceIndex_Source, srcDataIdx);
            BindSourceData(_renderProperties, false);
            _renderProperties.SetTexture(sp_LD_TexArray_Target, _targets);
            _renderProperties.DispatchShader();*/
        }
    }

    public void ValidateSourceData()
    {
        for (LodTransform.RenderData renderData : OceanRenderer.Instance._lodTransform._renderDataSource)
        {
            renderData.Validate(/*BuildCommandBufferBase._lastUpdateFrame - Time.frameCount*/0, this);
        }
    }

    public void BindSourceData(IPropertyWrapper simMaterial, boolean paramsOnly)
    {
        LodTransform.RenderData[] rd = OceanRenderer.Instance._lodTransform._renderDataSource;
//        BindData(simMaterial, paramsOnly ? Texture2D.blackTexture : _sources as Texture, true, ref rd, true);  todo
    }

    protected void OnEnable()
    {
        RemoveCommandBuffers();
    }

    protected void OnDisable()
    {
        RemoveCommandBuffers();
    }

    void RemoveCommandBuffers()
    {
        if (BufCopyShadowMap != null)
        {
            if (_mainLight != null)
            {
//                _mainLight.RemoveCommandBuffer(LightEvent.BeforeScreenspaceMask, BufCopyShadowMap);
            }
            BufCopyShadowMap = null;
        }
    }

    public static String TextureArrayName = "_LD_TexArray_Shadow";
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
