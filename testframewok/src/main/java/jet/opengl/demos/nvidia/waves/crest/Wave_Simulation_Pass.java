package jet.opengl.demos.nvidia.waves.crest;

import org.lwjgl.util.vector.Vector4f;

import java.util.ArrayList;

import jet.opengl.postprocessing.common.GLFuncProvider;
import jet.opengl.postprocessing.common.GLFuncProviderFactory;
import jet.opengl.postprocessing.texture.AttachType;
import jet.opengl.postprocessing.texture.RenderTargets;
import jet.opengl.postprocessing.texture.Texture2D;
import jet.opengl.postprocessing.texture.Texture2DDesc;
import jet.opengl.postprocessing.texture.TextureAttachDesc;
import jet.opengl.postprocessing.texture.TextureGL;
import jet.opengl.postprocessing.texture.TextureUtils;

abstract class Wave_Simulation_Pass implements Wave_Const{
    /** Get the name of the shader of the simulation */
    abstract String SimName();  // { get; }

    /** Get the format of the render target
     */
    abstract int TextureFormat(); // { get; }

    protected Texture2D _targets;

    protected Wave_Simulation m_Simulation;
    protected Wave_CDClipmap m_Clipmap;

    protected final ArrayList<Wave_LodData_Input> m_Inputs = new ArrayList<>();

    protected static final Wave_FilterData g_FilterData = new Wave_FilterData();

    protected RenderTargets m_FBO;
    protected final TextureAttachDesc m_AttachDesc = new TextureAttachDesc();
    protected GLFuncProvider gl;
    protected final Wave_Simulation_ShaderData m_ShaderData = new Wave_Simulation_ShaderData();

    public void addLodDataInput(Wave_LodData_Input input){
        m_Inputs.add(input);
    }

    public TextureGL DataTexture() {
        return _targets;
    }

//    public static int sp_LD_SliceIndex = 0; //Shader.PropertyToID("_LD_SliceIndex");
//    protected static int sp_LODChange = 1; //Shader.PropertyToID("_LODChange");

    // shape texture resolution
    private int _shapeRes = -1;

    // ocean scale last frame - used to detect scale changes
    float _oceanLocalScalePrev = -1f;

    int ScaleDifferencePow2;

    public Wave_Simulation_Pass() {
        for (int i = 0; i < _BindData_paramIdPosScales.length; i++) {
            _BindData_paramIdPosScales[i] = new Vector4f();
            _BindData_paramIdOceans[i] = new Vector4f();
        }
    }

    void init(Wave_CDClipmap clipmap, Wave_Simulation simulation) {
        m_Clipmap = clipmap;
        m_Simulation = simulation;
        InitData();
    }

    Texture2D CreateLodDataTextures(Texture2DDesc desc, String name) {
        desc.arraySize = m_Clipmap.m_LodTransform.LodCount();
        Texture2D texture =  TextureUtils.createTexture2D(desc, null);
        texture.setName(name);
        return texture;
    }

    protected void InitData() {
        gl = GLFuncProviderFactory.getGLFuncProvider();

        int resolution = m_Clipmap.getLodDataResolution();
        Texture2DDesc desc = new Texture2DDesc(resolution, resolution, TextureFormat());
        _targets = CreateLodDataTextures(desc, SimName());

        if(m_FBO == null)
            m_FBO = new RenderTargets();
    }

    public void UpdateLodData() {
        int width = m_Clipmap.getLodDataResolution();
        // debug functionality to resize RT if different size was specified.
        if (_shapeRes == -1) {
            _shapeRes = width;
        } else if (width != _shapeRes) {
            _targets.dispose();
//            _targets.width = _targets.height = _shapeRes;
//            _targets.Create();
            Texture2DDesc desc = new Texture2DDesc(_shapeRes, _shapeRes, 1, _targets.getArraySize(), TextureFormat(), 1);
            _targets = TextureUtils.createTexture2D(desc, null);
        }

        // determine if this LOD has changed scale and by how much (in exponent of 2)
        float oceanLocalScale = m_Clipmap.getScale();
        if (_oceanLocalScalePrev == -1f) _oceanLocalScalePrev = oceanLocalScale;
        float ratio = oceanLocalScale / _oceanLocalScalePrev;
        _oceanLocalScalePrev = oceanLocalScale;
        float ratio_l2 = (float) (Math.log(ratio) / Math.log(2f));
        ScaleDifferencePow2 = Math.round(ratio_l2);
    }

    public final void BindResultData(Wave_Simulation_ShaderData properties) {
        BindResultData(properties, true);
    }

    public void BindResultData(Wave_Simulation_ShaderData properties, boolean blendOut /*= true*/) {
        BindData(properties, _targets, blendOut, m_Clipmap.m_LodTransform._renderData, false);
    }

    protected void setRenderTarget(TextureGL target, int layer){
        m_AttachDesc.index = 0;
        m_AttachDesc.layer = layer;
        m_AttachDesc.level = 0;
        m_AttachDesc.type = AttachType.TEXTURE_LAYER;

        m_FBO.bind();
        m_FBO.setRenderTexture(target, m_AttachDesc);
        gl.glViewport(0,0,target.getWidth(), target.getHeight());
    }

    // Avoid heap allocations instead BindData
    private Vector4f[] _BindData_paramIdPosScales = new Vector4f[MAX_LOD_COUNT];
    // Used in child
    protected Vector4f[] _BindData_paramIdOceans = new Vector4f[MAX_LOD_COUNT];

    protected void BindData(Wave_Simulation_ShaderData properties, TextureGL applyData, boolean blendOut, Wave_LOD_Transform.RenderData[] renderData, boolean sourceLod /*= false*/) {
        if (applyData != null) {
//            properties.SetTexture(GetParamIdSampler(sourceLod), applyData);  todo
        }

        for (int lodIdx = 0; lodIdx < OceanRenderer.Instance.CurrentLodCount(); lodIdx++) {
            // NOTE: gets zeroed by unity, see https://www.alanzucconi.com/2016/10/24/arrays-shaders-unity-5-4/
            _BindData_paramIdPosScales[lodIdx].set(
                    renderData[lodIdx]._posSnapped.x, renderData[lodIdx]._posSnapped.z,
                    OceanRenderer.Instance.CalcLodScale(lodIdx), 0f);
            _BindData_paramIdOceans[lodIdx].set(renderData[lodIdx]._texelWidth, renderData[lodIdx]._textureRes, 1f, 1f / renderData[lodIdx]._textureRes);
        }

        // Duplicate the last element as the shader accesses element {slice index + 1] in a few situations. This way going
        // off the end of this parameter is the same as going off the end of the texture array with our clamped sampler.
        _BindData_paramIdPosScales[OceanRenderer.Instance.CurrentLodCount()] = _BindData_paramIdPosScales[OceanRenderer.Instance.CurrentLodCount() - 1];
        _BindData_paramIdOceans[OceanRenderer.Instance.CurrentLodCount()] = _BindData_paramIdOceans[OceanRenderer.Instance.CurrentLodCount() - 1];

//        properties.SetVectorArray(LodTransform.ParamIdPosScale(sourceLod), _BindData_paramIdPosScales);
//        properties.SetVectorArray(LodTransform.ParamIdOcean(sourceLod), _BindData_paramIdOceans);
    }

    public void BuildCommandBuffer() { }

    protected final void SubmitDraws(int lodIdx) {
        Wave_LOD_Transform lt = m_Clipmap.m_LodTransform;
//        lt._renderData[lodIdx].Validate(0, this);

//        lt.SetViewProjectionMatrices(lodIdx, buf);

        for (Wave_LodData_Input draw : m_Inputs) {
            draw.draw(1f, false, m_ShaderData);
        }
    }

    protected final void SubmitDrawsFiltered(int lodIdx, Wave_DrawFilter filter) {
        Wave_LOD_Transform lt = m_Clipmap.m_LodTransform;
//        lt._renderData[lodIdx].Validate(0, this);

//        lt.SetViewProjectionMatrices(lodIdx, buf);  todo

        for (Wave_LodData_Input draw : m_Inputs) {
            if (!draw.enabled()) {
                continue;
            }

//            int isTransition;
//            float weight = filter.Filter(draw, out isTransition);
            filter.Filter(draw, m_Clipmap, g_FilterData);
            float weight = g_FilterData.weight;
            boolean isTransition = g_FilterData.isTransition;
            if (weight > 0f) {
                draw.draw(/*buf,*/ weight, isTransition, m_ShaderData);
            }
        }
    }
}
