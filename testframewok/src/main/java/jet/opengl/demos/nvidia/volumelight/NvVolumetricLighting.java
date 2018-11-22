package jet.opengl.demos.nvidia.volumelight;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import jet.opengl.postprocessing.buffer.BufferGL;
import jet.opengl.postprocessing.common.Disposeable;
import jet.opengl.postprocessing.common.GLCheck;
import jet.opengl.postprocessing.common.GLFuncProvider;
import jet.opengl.postprocessing.common.GLFuncProviderFactory;
import jet.opengl.postprocessing.common.GLenum;
import jet.opengl.postprocessing.core.OpenGLProgram;
import jet.opengl.postprocessing.shader.GLSLProgram;
import jet.opengl.postprocessing.shader.GLSLUtil;
import jet.opengl.postprocessing.shader.Macro;
import jet.opengl.postprocessing.texture.RenderTargets;
import jet.opengl.postprocessing.texture.SamplerDesc;
import jet.opengl.postprocessing.texture.SamplerUtils;
import jet.opengl.postprocessing.texture.Texture2D;
import jet.opengl.postprocessing.util.CacheBuffer;
import jet.opengl.postprocessing.util.CommonUtil;

public class NvVolumetricLighting extends ContextImp_Common implements Disposeable {

    BufferGL pPerContextCB;
    BufferGL  pPerFrameCB;
    BufferGL  pPerVolumeCB;
    BufferGL  pPerApplyCB;

    private final PerContextCB perContextStruct = new PerContextCB();
    private final PerApplyCB perApplyStruct = new PerApplyCB();
    private final PerFrameCB perFrameStruct = new PerFrameCB();
    private final PerVolumeCB perVolumeStruct = new PerVolumeCB();

    private Texture2D pDepth_;  // Depth target
    private Texture2D pPhaseLUT_;
    private final Texture2D[] pLightLUT_P_ = new Texture2D[2];
    private final Texture2D[] pLightLUT_S1_ = new Texture2D[2];
    private final Texture2D[] pLightLUT_S2_ = new Texture2D[2];
    private Texture2D pAccumulation_;
    private Texture2D pResolvedAccumulation_;
    private Texture2D pResolvedDepth_;
    private final Texture2D[] pFilteredAccumulation_ = new Texture2D[2];
    private final Texture2D[] pFilteredDepth_ = new Texture2D[2];
    private Texture2D pAccumulatedOutput_;
    private RenderTargets rtManager;

    int samplerPoint;
    int samplerLinear;

    // shader declars
//    private final HashMap<>
    private final RenderVolumeDesc renderVolumeDesc = new RenderVolumeDesc();
    private final ComputeLightLUTDesc computeLightLUTDesc = new ComputeLightLUTDesc();
    private final ApplyDesc applyDesc = new ApplyDesc();

    private GLSLProgram computePhaseLookup_PS;
    private GLSLProgram downsampleDepth_PS;
    private GLSLProgram resolve_PS;
    private GLSLProgram tempoalFilter_PS;
    private int dummyVAO;

    private boolean debug = true;
    private boolean mbPrintProgram = false;

    private GLFuncProvider gl;

    Status createResources(){
        gl = GLFuncProviderFactory.getGLFuncProvider();

        final int LIGHT_LUT_WDOTV_RESOLUTION = VLConstant.LIGHT_LUT_WDOTV_RESOLUTION;
        final int LIGHT_LUT_DEPTH_RESOLUTION = VLConstant.LIGHT_LUT_DEPTH_RESOLUTION;
        pDepth_ = create(getInternalBufferWidth(), getInternalBufferHeight(), getInternalSampleCount(), GLenum.GL_DEPTH24_STENCIL8,"NvVl::Depth");
        pPhaseLUT_ = create(1, VLConstant.LIGHT_LUT_WDOTV_RESOLUTION, 1, GLenum.GL_RGBA16F, "NvVl::Phase LUT");
        pLightLUT_P_[0] = create(LIGHT_LUT_DEPTH_RESOLUTION, LIGHT_LUT_WDOTV_RESOLUTION, 1, GLenum.GL_RGBA16F, "NvVl::Light LUT Point [0]");
        pLightLUT_P_[1] = create(LIGHT_LUT_DEPTH_RESOLUTION, LIGHT_LUT_WDOTV_RESOLUTION, 1, GLenum.GL_RGBA16F, "NvVl::Light LUT Point [1]");
        pLightLUT_S1_[0] = create(LIGHT_LUT_DEPTH_RESOLUTION, LIGHT_LUT_WDOTV_RESOLUTION, 1, GLenum.GL_RGBA16F, "NvVl::Light LUT Spot 1 [0]");
        pLightLUT_S1_[1] = create(LIGHT_LUT_DEPTH_RESOLUTION, LIGHT_LUT_WDOTV_RESOLUTION, 1, GLenum.GL_RGBA16F, "NvVl::Light LUT Spot 1 [1]");
        pLightLUT_S2_[0] = create(LIGHT_LUT_DEPTH_RESOLUTION, LIGHT_LUT_WDOTV_RESOLUTION, 1, GLenum.GL_RGBA16F, "NvVl::Light LUT Spot 2 [0]");
        pLightLUT_S2_[1] = create(LIGHT_LUT_DEPTH_RESOLUTION, LIGHT_LUT_WDOTV_RESOLUTION, 1, GLenum.GL_RGBA16F, "NvVl::Light LUT Spot 2 [1]");
        pAccumulation_ = create(getInternalBufferWidth(), getInternalBufferHeight(), getInternalSampleCount(), GLenum.GL_RGBA16F, "NvVl::Accumulation");

        if(isInternalMSAA() || getFilterMode() == FilterMode.TEMPORAL){
            pResolvedAccumulation_ = create(getInternalBufferWidth(), getInternalBufferHeight(), 1, GLenum.GL_RGBA16F, "NvVl::Resolved Accumulation");
            pResolvedDepth_ = create(getInternalBufferWidth(), getInternalBufferHeight(), 1, GLenum.GL_RG16F, "NvVl::Resolved Depth");
        }

        if (getFilterMode() == FilterMode.TEMPORAL)
        {
            for (int i=0; i<2; ++i)
            {
//                V_CREATE(pFilteredDepth_[i], RenderTarget::Create(device, getInternalBufferWidth(), getInternalBufferHeight(), 1, DXGI_FORMAT_R16G16_FLOAT, "NvVl::Filtered Depth"));
                pFilteredDepth_[i] = create(getInternalBufferWidth(), getInternalBufferHeight(), 1, GLenum.GL_RG16F, "NvVl::Filtered Depth");
//        		V_CREATE(pFilteredAccumulation_[i], RenderTarget::Create(device, getInternalBufferWidth(), getInternalBufferHeight(), 1, DXGI_FORMAT_R16G16B16A16_FLOAT, "NvVl::Filtered Accumulation"));
                pFilteredAccumulation_[i] = create(getInternalBufferWidth(), getInternalBufferHeight(), 1, GLenum.GL_RGBA16F, "NvVl::Filtered Accumulation");
            }
        }

        SamplerDesc desc = new SamplerDesc();
        desc.minFilter = GLenum.GL_NEAREST;
        desc.magFilter = GLenum.GL_NEAREST;
        samplerPoint = SamplerUtils.createSampler(desc);

        desc.minFilter = GLenum.GL_LINEAR;
        desc.magFilter = GLenum.GL_LINEAR;
        samplerLinear = SamplerUtils.createSampler(desc);

        rtManager = new RenderTargets();
        // TODO Create shader objects.
        List<Macro> macros = new ArrayList<>();
        macros.add(new Macro("USE_UNIFORM_BLOCK", 1));
        Macro[] arrays = isOutputMSAA() ? BaseVLProgram.sampleModeMSAA : BaseVLProgram.sampleModeSingle;
        for(int i = 0; i < arrays.length; i++)
            macros.add( arrays[i]);

        computePhaseLookup_PS = createProgram("ComputePhaseLookup_PS.frag", macros.toArray(new Macro[macros.size()]));
        downsampleDepth_PS = createProgram("DownsampleDepth_PS.frag", macros.toArray(new Macro[macros.size()]));
        resolve_PS = createProgram("Resolve_PS.frag", macros.toArray(new Macro[macros.size()]));
//        tempoalFilter_PS = new TempoalFilterProgram(this, true);

        dummyVAO = gl.glGenVertexArray();

        pPerContextCB = new BufferGL();
        pPerContextCB.initlize(GLenum.GL_UNIFORM_BUFFER, PerContextCB.SIZE, null, GLenum.GL_STREAM_READ);

        pPerApplyCB = new BufferGL();
        pPerApplyCB.initlize(GLenum.GL_UNIFORM_BUFFER, PerApplyCB.SIZE, null, GLenum.GL_STREAM_READ);

        pPerFrameCB = new BufferGL();
        pPerFrameCB.initlize(GLenum.GL_UNIFORM_BUFFER, perFrameStruct.SIZE, null, GLenum.GL_STREAM_READ);

        pPerVolumeCB = new BufferGL();
        pPerVolumeCB.initlize(GLenum.GL_UNIFORM_BUFFER, perVolumeStruct.SIZE, null, GLenum.GL_STREAM_READ);
        return Status.OK;
    }

    private void assertBuffers(OpenGLProgram program){
        GLCheck.checkError();

        GLSLUtil.assertUniformBuffer(program, "cbContext", 0, PerContextCB.SIZE);
        GLSLUtil.assertUniformBuffer(program, "cbFrame", 1, PerFrameCB.SIZE);
        GLSLUtil.assertUniformBuffer(program, "cbVolume", 2, PerVolumeCB.SIZE);
        GLSLUtil.assertUniformBuffer(program, "cbApply", 3, PerApplyCB.SIZE);
        GLCheck.checkError();
    }

    @Override
    protected Status beginAccumulation_Start(int sceneDepth, ViewerDesc pViewerDesc, MediumDesc pMediumDesc) {
        if (!isInitialized_)
        {
            // Update the per-context constant buffer on the first frame it's used
            isInitialized_ = true;
//	        setupCB_PerContext(pPerContextCB->Map(dxCtx));
//	        pPerContextCB->Unmap(dxCtx);

            setupCB_PerContext(perContextStruct);
            perContextStruct.store(pPerContextCB);
        }

        // Setup the constant buffer
        setupCB_PerFrame(pViewerDesc, pMediumDesc, perFrameStruct);
        perFrameStruct.store(pPerFrameCB);

        gl.glBindBufferBase(GLenum.GL_UNIFORM_BUFFER, 0, pPerContextCB.getBuffer());
        gl.glBindBufferBase(GLenum.GL_UNIFORM_BUFFER, 1, pPerFrameCB.getBuffer());

        if(!mbPrintProgram){
            System.out.println("PhaseFunc: " + Arrays.toString(perFrameStruct.uPhaseFunc));
            System.out.println("PhaseParams: " + Arrays.toString(perFrameStruct.vPhaseParams));
        }

        gl.glBindVertexArray(dummyVAO);

        return Status.OK;
    }

    @Override
    protected Status beginAccumulation_UpdateMediumLUT() {
        rtManager.bind();
        rtManager.setRenderTexture(pPhaseLUT_, null);
        gl.glClearColor(0,0,0,0);
        gl.glClear(GLenum.GL_COLOR_BUFFER_BIT);
        gl.glViewport(0, 0, 1, VLConstant.LIGHT_LUT_WDOTV_RESOLUTION);

        computePhaseLookup_PS.enable();
        gl.glDrawArrays(GLenum.GL_TRIANGLES, 0, 3);
        computePhaseLookup_PS.disable();

        if(!mbPrintProgram){
            assertBuffers(computePhaseLookup_PS);
            printProgram(computePhaseLookup_PS, "UpdateMediumLUT");
            saveTextureAsText(pPhaseLUT_, "UpdateMediumLUTGL_3.txt");
        }

        return Status.OK;
    }

    @Override
    protected Status beginAccumulation_CopyDepth(int sceneDepth) {
        rtManager.setRenderTexture(pDepth_, null);
        gl.glClearDepthf(1.f);
        gl.glClear(GLenum.GL_DEPTH_BUFFER_BIT);

        if(debug){
            if(pDepth_.getWidth() != getInternalBufferWidth() ||
                    pDepth_.getHeight() != getInternalBufferHeight()){
                String str = String.format("pDepth' size = (%d, %d), InternalBufferSize = (%d, %d)", pDepth_.getWidth(), pDepth_.getHeight(),
                        getInternalBufferWidth(), getInternalBufferHeight());
                throw new IllegalArgumentException(str);
            }
        }

        gl.glViewport(0, 0, getInternalViewportWidth(), getInternalViewportHeight());
        gl.glEnable(GLenum.GL_DEPTH_TEST);
        gl.glDepthFunc(GLenum.GL_ALWAYS);
        downsampleDepth_PS.enable();
        gl.glBindTextureUnit(0, sceneDepth);
        gl.glBindSampler(0, samplerPoint);
        gl.glDrawArrays(GLenum.GL_TRIANGLES, 0, 3);
        gl.glDepthFunc(GLenum.GL_LESS);
        gl.glDisable(GLenum.GL_DEPTH_TEST);

        if(!mbPrintProgram){
            assertBuffers(downsampleDepth_PS);

            printProgram(downsampleDepth_PS, "CopyDepth");
            saveTextureAsText(pDepth_, "CopyDepthGL_3.txt");
        }

        return Status.OK;
    }

    @Override
    protected Status beginAccumulation_End(int sceneDepth, ViewerDesc pViewerDesc, MediumDesc pMediumDesc) {
        rtManager.setRenderTexture(pAccumulation_, null);
        gl.glClearBufferfv(GLenum.GL_COLOR, 0, CacheBuffer.wrap(0.f, 0.f, 0f, 0));

//		NV_PERFEVENT_END(dxCtx);
        return Status.OK;
    }

    @Override
    protected Status renderVolume_Start(int shadowMap, ShadowMapDesc pShadowMapDesc, LightDesc pLightDesc, VolumeDesc pVolumeDesc) {
        //		NV_PERFEVENT_BEGIN(dxCtx, "NvVl::RenderVolume");

        // Setup the constant buffer
        setupCB_PerVolume(pShadowMapDesc, pLightDesc, pVolumeDesc, perVolumeStruct);
        if(pPerVolumeCB != null){
            perVolumeStruct.store(pPerVolumeCB);
        }

        gl.glBindBufferBase(GLenum.GL_UNIFORM_BUFFER, 2, pPerVolumeCB.getBuffer());

        return Status.OK;
    }

    @Override
    protected Status renderVolume_DoVolume_Directional(int shadowMap, ShadowMapDesc pShadowMapDesc, LightDesc pLightDesc, VolumeDesc pVolumeDesc) {
        //		NV_PERFEVENT(dxCtx, "Directional");

        if(!mbPrintProgram){
            System.err.println("Directional");
        }

        bs_additive();

        //--------------------------------------------------------------------------
        // Draw tessellated grid
        int mesh_resolution = getCoarseResolution(pVolumeDesc) ;

        rtManager.setRenderTextures(CommonUtil.toArray(pAccumulation_, pDepth_), null);
        gl.glClearColor(0,0,0,0);
        gl.glClearStencil(0xFF);
        gl.glClear(GLenum.GL_STENCIL_BUFFER_BIT | GLenum.GL_COLOR_BUFFER_BIT);
        gl.glStencilMask(0xFF);

        // Determine DS/HS permutation
        switch (pVolumeDesc.eTessQuality) {
            case HIGH:  		   renderVolumeDesc.maxtessfactor = RenderVolumeDesc.MAXTESSFACTOR_HIGH;  break;
            case MEDIUM:  default: renderVolumeDesc.maxtessfactor = RenderVolumeDesc.MAXTESSFACTOR_MEDIUM;  break;
            case LOW: 			   renderVolumeDesc.maxtessfactor = RenderVolumeDesc.MAXTESSFACTOR_LOW;  break;
        }

        switch (pShadowMapDesc.eType)
        {
            case SIMPLE:
            case CASCADE_ATLAS:
                renderVolumeDesc.shadowMapType = RenderVolumeDesc.SHADOWMAPTYPE_ATLAS;
                break;

            case CASCADE_ARRAY:
                renderVolumeDesc.shadowMapType = RenderVolumeDesc.SHADOWMAPTYPE_ARRAY;
                break;

            default:
                return Status.INVALID_PARAMETER;
        };

        switch (pShadowMapDesc.uElementCount)
        {
            case 0:
                if (pShadowMapDesc.eType != ShadowMapLayout.SIMPLE)
                {
                    return Status.INVALID_PARAMETER;
                }
            case 1:
                renderVolumeDesc.cascadeCount = RenderVolumeDesc.CASCADECOUNT_1;
                break;
            case 2:
                renderVolumeDesc.cascadeCount = RenderVolumeDesc.CASCADECOUNT_2;
                break;
            case 3:
                renderVolumeDesc.cascadeCount = RenderVolumeDesc.CASCADECOUNT_3;
                break;
            case 4:
                renderVolumeDesc.cascadeCount = RenderVolumeDesc.CASCADECOUNT_4;
                break;

            default:
                return Status.INVALID_PARAMETER;
        };

        renderVolumeDesc.volumeType = RenderVolumeDesc.VOLUMETYPE_FRUSTUM;

        // Determine PS permutation
//	    RenderVolumeDesc.Desc ps_desc;
        renderVolumeDesc.sampleMode = isInternalMSAA() ? RenderVolumeDesc.SAMPLEMODE_MSAA : RenderVolumeDesc.SAMPLEMODE_SINGLE;
        renderVolumeDesc.lightMode = RenderVolumeDesc.LIGHTMODE_DIRECTIONAL;
        renderVolumeDesc.passMode = RenderVolumeDesc.PASSMODE_GEOMETRY;
        renderVolumeDesc.attenuationMode = RenderVolumeDesc.ATTENUATIONMODE_NONE; // unused for directional
        renderVolumeDesc.falloffMode = 0;

        ds_render_volume(0xFF, false);
        no_cull_face();
        drawFrustumGrid(mesh_resolution, shadowMap, pShadowMapDesc);

        if(!mbPrintProgram){
//	    	saveTextureAsText(pAccumulation_, "Direction_Light_GL0.txt");
            saveTextureAsText(pDepth_, "Direction_Light_DS_GL0_2.txt");
        }

        //--------------------------------------------------------------------------
        // Remove the illumination from the base of the scene (re-lit by sky later)
        drawFrustumBase(mesh_resolution, shadowMap, pShadowMapDesc);


        if(!mbPrintProgram){
//	    	saveTextureAsText(pAccumulation_, "Direction_Light_GL1.txt");
            saveTextureAsText(pDepth_, "Direction_Light_DS_GL1_2.txt");
        }

        if(debugFlags_ == DebugFlags.WIREFRAME){
            return Status.OK;
        }

        renderVolumeDesc.passMode = RenderVolumeDesc.PASSMODE_SKY;
        ds_render_volume_boundary(0xFF, false);
        renderVolumeDesc.useQuadVS = true;
        drawQuad(shadowMap, pShadowMapDesc);

        if(!mbPrintProgram){
//	    	saveTextureAsText(pAccumulation_, "Direction_Light_GL2.txt");
            saveTextureAsText(pDepth_, "Direction_Light_DS_GL2_2.txt");
        }

        //--------------------------------------------------------------------------
        // Finish the rendering by filling in stenciled gaps
        // TODO Notice the texture settingup
        ds_finish_volume(0xFF);
//	    renderVolume_Textures[0] = renderVolume_Textures[1];
//	    rtManager.setTexture2DRenderTargets(pAccumulation_.getTexture(), pDepth_.getTexture());
        renderVolumeDesc.passMode = RenderVolumeDesc.PASSMODE_FINAL;
        drawQuad(shadowMap, pShadowMapDesc);

        if(!mbPrintProgram){
//	    	saveTextureAsText(pAccumulation_, "Direction_Light_GL3.txt");
            saveTextureAsText(pDepth_, "Direction_Light_DS_GL3_3.txt");
        }

        if(!mbPrintProgram){
            saveTextureAsText(pAccumulation_, "Directional_GL_3.txt");
            saveTextureAsText(pDepth_, "Directional_DS_GL_3.txt");
        }
        return Status.OK;
    }

    @Override
    protected Status renderVolume_DoVolume_Spotlight(int shadowMap, ShadowMapDesc pShadowMapDesc, LightDesc pLightDesc, VolumeDesc pVolumeDesc) {
        return null;
    }

    @Override
    protected Status renderVolume_DoVolume_Omni(int shadowMap, ShadowMapDesc pShadowMapDesc, LightDesc pLightDesc, VolumeDesc pVolumeDesc) {
        return null;
    }

    @Override
    protected Status renderVolume_End(int shadowMap, ShadowMapDesc pShadowMapDesc, LightDesc pLightDesc, VolumeDesc pVolumeDesc) {
        return null;
    }

    @Override
    protected Status endAccumulation_Imp() {
        return null;
    }

    @Override
    protected Status applyLighting_Start(int sceneTarget, int sceneDepth, PostprocessDesc pPostprocessDesc) {
        return null;
    }

    @Override
    protected Status applyLighting_Resolve(PostprocessDesc pPostprocessDesc) {
        return null;
    }

    @Override
    protected Status applyLighting_TemporalFilter(int sceneDepth, PostprocessDesc pPostprocessDesc) {
        return null;
    }

    @Override
    protected Status applyLighting_Composite(int sceneTarget, int sceneDepth, PostprocessDesc pPostprocessDesc) {
        return null;
    }

    @Override
    protected Status applyLighting_End(int sceneTarget, int sceneDepth, PostprocessDesc pPostprocessDesc) {
        return null;
    }

    @Override
    public void dispose() {

    }

    private void drawFrustumGrid(int resolution, int shadowMap, ShadowMapDesc pShadowMapDesc){
        renderVolumeDesc.meshMode = RenderVolumeDesc.MESHMODE_FRUSTUM_GRID;
        renderVolumeDesc.includeTesslation = true;
        renderVolumeDesc.useQuadVS = false;
        renderVolumeDesc.debugPS = false;

        boolean constCB = (pPerVolumeCB == null);

        RenderVolumeProgram program = null; // getRenderVolumeShader(renderVolumeDesc);
        if(constCB){
            perVolumeStruct.store(pPerVolumeCB);
        }

//		program.enable(renderVolume_Textures);
        program.enable();
//        setupTextures(program, shadowMap, pShadowMapDesc);
//        setupUniforms(program);
        if(debugFlags_ == DebugFlags.WIREFRAME){
            gl.glPolygonMode(GLenum.GL_FRONT_AND_BACK, GLenum.GL_LINE);
        }

        int vtx_count = 4 * resolution * resolution;
        gl.glPatchParameteri(GLenum.GL_PATCH_VERTICES, 4);
        gl.glDrawArrays(GLenum.GL_PATCHES, 0, vtx_count);

        if(!mbPrintProgram){
            printProgram(program, "FrustumGrid");
        }
    }

    private void drawFrustumBase(int resolution, int shadowMap, ShadowMapDesc pShadowMapDesc){
//		NV_PERFEVENT(dxCtx, "DrawFrustumBase");

        renderVolumeDesc.includeTesslation = false;
        renderVolumeDesc.useQuadVS = false;
        renderVolumeDesc.meshMode = RenderVolumeDesc.MESHMODE_FRUSTUM_BASE;
        renderVolumeDesc.debugPS = (debugFlags_ == DebugFlags.WIREFRAME);

        boolean constCB = (pPerVolumeCB == null);
        RenderVolumeProgram program = getRenderVolumeShader(renderVolumeDesc);
        if(constCB && pPerVolumeCB != null){
            perVolumeStruct.store(pPerVolumeCB);
        }

//		program.enable(renderVolume_Textures);
        program.enable();
        setupTextures(program, shadowMap, pShadowMapDesc);
        setupUniforms(program);

        if(debugFlags_ == DebugFlags.WIREFRAME){
            gl.glPolygonMode(GLenum.GL_FRONT_AND_BACK, GLenum.GL_LINE);
        }
        gl.glDrawArrays(GLenum.GL_TRIANGLES, 0, 6);

        if(!mbPrintProgram){
            printProgram(program, "FrustumBase");
        }
    }

    private void drawFrustumCap(int resolution, int shadowMap, ShadowMapDesc pShadowMapDesc){
//		NV_PERFEVENT(dxCtx, "DrawFrustumCap");

        renderVolumeDesc.includeTesslation = false;
        renderVolumeDesc.useQuadVS = false;
        renderVolumeDesc.meshMode = RenderVolumeDesc.MESHMODE_FRUSTUM_CAP;
        renderVolumeDesc.debugPS = (debugFlags_ == DebugFlags.WIREFRAME);

        boolean constCB = (pPerVolumeCB == null);
        RenderVolumeProgram program = getRenderVolumeShader(renderVolumeDesc);
        if(constCB && pPerVolumeCB != null){
            perVolumeStruct.store(pPerVolumeCB);
        }

        program.enable();
        setupTextures(program, shadowMap, pShadowMapDesc);
        setupUniforms(program);

        if(debugFlags_ == DebugFlags.WIREFRAME){
            gl.glPolygonMode(GLenum.GL_FRONT_AND_BACK, GLenum.GL_LINE);
        }

        int vtx_count = 4*3*(resolution+1) + 6;
        gl.glDrawArrays(GLenum.GL_TRIANGLES, 0, vtx_count);

        if(!mbPrintProgram){
            printProgram(program, "FrustumCap");
        }
    }

    private void drawOmniVolume(int resolution, int shadowMap, ShadowMapDesc pShadowMapDesc){
//		NV_PERFEVENT(dxCtx, "DrawOmniVolume");

        renderVolumeDesc.includeTesslation = true;
        renderVolumeDesc.useQuadVS = false;
        renderVolumeDesc.meshMode = RenderVolumeDesc.MESHMODE_OMNI_VOLUME;
        renderVolumeDesc.debugPS = (debugFlags_ == DebugFlags.WIREFRAME);

        boolean constCB = (pPerVolumeCB == null);
        RenderVolumeProgram program = getRenderVolumeShader(renderVolumeDesc);
        if(constCB && pPerVolumeCB != null){
            perVolumeStruct.store(pPerVolumeCB);
        }

//		program.enable(renderVolume_Textures);
        program.enable();
        setupTextures(program, shadowMap, pShadowMapDesc);
        setupUniforms(program);

        if(debugFlags_ == DebugFlags.WIREFRAME){
            gl.glPolygonMode(GLenum.GL_FRONT_AND_BACK, GLenum.GL_LINE);
        }

        int vtx_count = 6 * 4 * resolution * resolution;
        gl.glPatchParameteri(GLenum.GL_PATCH_VERTICES, 4);
        gl.glDrawArrays(GLenum.GL_PATCHES, 0, vtx_count);

        if(!mbPrintProgram){
            printProgram(program, "OmniVolume");
        }
    }

    private void drawQuad(int shadowMap, ShadowMapDesc pShadowMapDesc){
//		NV_PERFEVENT(dxCtx, "DrawOmniVolume");

        renderVolumeDesc.includeTesslation = false;
        renderVolumeDesc.useQuadVS = true;
        renderVolumeDesc.debugPS = false;

        boolean constCB = (pPerVolumeCB == null);
        RenderVolumeProgram program = getRenderVolumeShader(renderVolumeDesc);
        if(constCB && pPerVolumeCB != null){
            perVolumeStruct.store(pPerVolumeCB);
        }

//		program.enable(renderVolume_Textures);
        program.enable();
        setupTextures(program, shadowMap, pShadowMapDesc);
        setupUniforms(program);

        no_cull_face();
//		GL11.glDisable(GL11.GL_CULL_FACE);
//		GL11.glFrontFace(GL11.GL_CCW);

        gl.glDrawArrays(GLenum.GL_TRIANGLES, 0, 3);
        program.disable();

        if(!mbPrintProgram){
            printProgram(program, "drawQuad");
        }
    }

    private RenderVolumeProgram getRenderVolumeShader(RenderVolumeDesc desc){
        return null;
    }

    private void setupTextures(RenderVolumeProgram program, int shadowMap, ShadowMapDesc pShadowMapDesc){ }

    private void setupUniforms(BaseVLProgram program){ }

    private GLSLProgram createProgram(String fragfile, Macro...macros){
        final String root = "nvidia/NvVolumetricLighting/shaders/";
        try {
            GLSLProgram program = GLSLProgram.createFromFiles(root + "Quad_VS.vert", root + fragfile, macros);
            String debugName;
            int index=fragfile.lastIndexOf('.');
            debugName = fragfile.substring(0, index);
            program.setName(debugName);
            return program;
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    private void ds_render_volume(int ref, boolean flag){
        int front = flag ? GLenum.GL_BACK : GLenum.GL_FRONT;
        int back  = flag ? GLenum.GL_FRONT : GLenum.GL_BACK;

        gl.glEnable(GLenum.GL_STENCIL_TEST);
        gl.glStencilFunc(GLenum.GL_ALWAYS, ref, 0xFF);
        gl.glStencilOpSeparate(front, GLenum.GL_KEEP, GLenum.GL_INCR_WRAP, GLenum.GL_KEEP);
        gl.glStencilOpSeparate(back, GLenum.GL_KEEP, GLenum.GL_DECR_WRAP, GLenum.GL_KEEP);

        gl.glEnable(GLenum.GL_DEPTH_TEST);
        gl.glDepthFunc(GLenum.GL_LEQUAL);
        gl.glDepthMask(false);
    }

    private void ds_render_volume_boundary(int ref, boolean flag){
        int front = flag ? GLenum.GL_BACK : GLenum.GL_FRONT;
        int back  = flag ? GLenum.GL_FRONT : GLenum.GL_BACK;

        gl.glEnable(GLenum.GL_STENCIL_TEST);
        gl.glStencilFuncSeparate(front, GLenum.GL_NEVER, ref, 0xFF);
        gl.glStencilFuncSeparate(back, GLenum.GL_ALWAYS, ref, 0xFF);

        gl.glStencilOpSeparate(front, GLenum.GL_KEEP, GLenum.GL_KEEP, GLenum.GL_KEEP);
        gl.glStencilOpSeparate(back, GLenum.GL_KEEP, GLenum.GL_DECR_WRAP, GLenum.GL_KEEP);

        gl.glStencilMask(0xFF);
        gl.glEnable(GLenum.GL_DEPTH_TEST);
        gl.glDepthFunc(GLenum.GL_LEQUAL);
        gl.glDepthMask(false);
    }

    private void ds_render_volume_cap(int ref){
        gl.glEnable(GLenum.GL_STENCIL_TEST);
        gl.glStencilFunc(GLenum.GL_LEQUAL, ref, 0xFF);

        gl.glStencilOpSeparate(GLenum.GL_FRONT, GLenum.GL_KEEP, GLenum.GL_INCR, GLenum.GL_KEEP);
        gl.glStencilOpSeparate(GLenum.GL_BACK, GLenum.GL_KEEP, GLenum.GL_DECR, GLenum.GL_KEEP);

        gl.glDisable(GLenum.GL_DEPTH_TEST);
    }

    private void ds_finish_volume(int ref){
        gl.glEnable(GLenum.GL_STENCIL_TEST);

        gl.glStencilFuncSeparate(GLenum.GL_FRONT, GLenum.GL_NEVER  , ref, 0xFF);
        gl.glStencilFuncSeparate(GLenum.GL_BACK, GLenum.GL_GREATER , ref, 0xFF);
        gl.glStencilOp(GLenum.GL_KEEP, GLenum.GL_KEEP, GLenum.GL_KEEP);

        gl.glDepthMask(false);
        gl.glDisable(GLenum.GL_DEPTH_TEST);
    }

    private void bs_additive(){
        gl.glEnable(GLenum.GL_BLEND);
        gl.glBlendFunc(GLenum.GL_ONE, GLenum.GL_ONE);
        gl.glBlendEquation(GLenum.GL_FUNC_ADD);
    }

    private void bs_debug_blend(){
        gl.glEnable(GLenum.GL_BLEND);
        gl.glBlendFuncSeparate(GLenum.GL_ONE, GLenum.GL_ZERO, GLenum.GL_ZERO, GLenum.GL_DST_ALPHA);
    }

    private void bs_additive_modulate(float r, float g, float b, float a){
        gl.glEnable(GLenum.GL_BLEND);
        gl.glBlendFuncSeparate(GLenum.GL_CONSTANT_COLOR, GLenum.GL_DST_COLOR, GLenum.GL_ZERO, GLenum.GL_ONE);
        gl.glBlendColor(r, g, b, a);
    }

    private void rs_cull_front(){
        gl.glCullFace(GLenum.GL_FRONT);
        gl.glFrontFace(GLenum.GL_CW);
        gl.glEnable(GLenum.GL_CULL_FACE);
    }

    private void no_cull_face(){
        gl.glDisable(GLenum.GL_CULL_FACE);
        gl.glFrontFace(GLenum.GL_CW);
    }
}
