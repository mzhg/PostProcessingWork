package jet.opengl.demos.nvidia.waves.crest;

import jet.opengl.demos.intel.fluid.scene.Light;
import jet.opengl.demos.nvidia.waves.crest.helpers.TextureArrayHelpers;
import jet.opengl.demos.nvidia.waves.ocean.Technique;
import jet.opengl.postprocessing.buffer.BufferGL;
import jet.opengl.postprocessing.common.GLenum;
import jet.opengl.postprocessing.texture.Texture2DDesc;
import jet.opengl.postprocessing.texture.TextureGL;

/**
 *  Stores shadowing data to use during ocean shading. Shadowing is persistent and supports sampling across
 *  many frames and jittered sampling for (very) soft shadows.
 */
final class Wave_Simulation_Shadow_Pass extends Wave_Simulation_Pass {
    public String SimName () { return "Shadow"; }
    public int TextureFormat() { return GLenum.GL_RG16; }

    public static boolean s_processData = true;

    Light _mainLight;

    // LWRP version needs access to this externally, hence public get
    public BufferGL BufCopyShadowMap;

    TextureGL _sources;
    private Technique krnl_UpdateShadow;
    private final String UpdateShadow = "UpdateShadow";

    static int sp_CenterPos = 0; //Shader.PropertyToID("_CenterPos");
    static int sp_Scale = 1; //Shader.PropertyToID("_Scale");
    static int sp_CamPos = 2; //Shader.PropertyToID("_CamPos");
    static int sp_CamForward = 3; //Shader.PropertyToID("_CamForward");
    static int sp_JitterDiameters_CurrentFrameWeights = 4; //Shader.PropertyToID("_JitterDiameters_CurrentFrameWeights");
    static int sp_MainCameraProjectionMatrix = 5; //Shader.PropertyToID("_MainCameraProjectionMatrix");
    static int sp_SimDeltaTime = 6; //Shader.PropertyToID("_SimDeltaTime");
    static int sp_LD_SliceIndex_Source = 7; //Shader.PropertyToID("_LD_SliceIndex_Source");
    static int sp_LD_TexArray_Target = 8; //Shader.PropertyToID("_LD_TexArray_Target");

    protected void InitData()
    {
        super.InitData();

        krnl_UpdateShadow = ShaderManager.getInstance().getProgram(UpdateShadow);
        int resolution = OceanRenderer.Instance.LodDataResolution();
        Texture2DDesc desc = new Texture2DDesc(resolution, resolution, TextureFormat());
        _sources = CreateLodDataTextures(desc, SimName() + "_1");

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

    private boolean enabled = true;

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

//        ValidateSourceData();

        // clear the shadow collection. it will be overwritten with shadow values IF the shadows render,
        // which only happens if there are (nontransparent) shadow receivers around
        TextureArrayHelpers.ClearToBlack(_targets);

        Wave_LOD_Transform lt = m_Clipmap.m_LodTransform;
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

    public void BindSourceData(Wave_Simulation_ShaderData simMaterial, boolean paramsOnly)
    {
        Wave_LOD_Transform.RenderData[] rd = m_Clipmap.m_LodTransform._renderDataSource;
        BindData(simMaterial, paramsOnly ?null :  _sources, true, rd, true);
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

    /*public static String TextureArrayName = "_LD_TexArray_Shadow";
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
            properties._LD_TexArray_Shadow_Source = null;
        }else{
            properties._LD_TexArray_Shadow = null;
        }
    }
}
