package jet.opengl.demos.nvidia.volumelight;

import org.lwjgl.util.vector.Vector4f;
import org.lwjgl.util.vector.Writable;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.HashMap;

import jet.opengl.postprocessing.buffer.BufferGL;
import jet.opengl.postprocessing.common.GLCheck;
import jet.opengl.postprocessing.common.GLFuncProvider;
import jet.opengl.postprocessing.common.GLFuncProviderFactory;
import jet.opengl.postprocessing.common.GLenum;
import jet.opengl.postprocessing.core.OpenGLProgram;
import jet.opengl.postprocessing.texture.RenderTargets;
import jet.opengl.postprocessing.texture.SamplerDesc;
import jet.opengl.postprocessing.texture.SamplerUtils;
import jet.opengl.postprocessing.texture.Texture2D;
import jet.opengl.postprocessing.texture.Texture2DDesc;
import jet.opengl.postprocessing.texture.TextureGL;
import jet.opengl.postprocessing.texture.TextureUtils;
import jet.opengl.postprocessing.util.CacheBuffer;
import jet.opengl.postprocessing.util.CommonUtil;
import jet.opengl.postprocessing.util.DebugTools;
import jet.opengl.render.debug.FrustumeRender;

final class ContextImp_OpenGL extends ContextImp_Common implements VLConstant{

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
//	private final HashMap<ProgramIndex, ApplyProgram> applyPrograms = new HashMap<>();
	private final HashMap<Object, BaseVLProgram> programsCache = new HashMap<>();
	private final RenderVolumeDesc renderVolumeDesc = new RenderVolumeDesc();
	private final ComputeLightLUTDesc computeLightLUTDesc = new ComputeLightLUTDesc();
	private final ApplyDesc applyDesc = new ApplyDesc();
	
	private ComputePhaseLookupProgram computePhaseLookup_PS;
	private DownsampleDepthProgram downsampleDepth_PS;
	private ResolvePogram resolve_PS;
	private TempoalFilterProgram tempoalFilter_PS;
	private int dummyVAO;
	
	private boolean debug = true;
	private boolean mbPrintProgram = false;
	private boolean m_bStaticSceneTest = false;
	
	private final Vector4f[] faceColors_ = {
		new Vector4f(1, 0, 0, 0.5f),
		new Vector4f(0, 1, 0, 0.5f),
		new Vector4f(1, 1, 0, 0.5f),
		new Vector4f(0, 1, 1, 0.5f),
		new Vector4f(1, 1, 1, 0.5f),
		new Vector4f(1, 0, 1, 0.5f)
	};
	
	private FrustumeRender  m_frustumeRender;
	private GLFuncProvider gl;
	
	// Private default constructor
	// Should never be called
	/*private*/ ContextImp_OpenGL() {
	}
	
	
	/*private*/ ContextImp_OpenGL(ContextDesc pContextDesc) {
		super(pContextDesc);
	}

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
    	computePhaseLookup_PS = new ComputePhaseLookupProgram(this);
    	downsampleDepth_PS = new DownsampleDepthProgram(this, isOutputMSAA());
    	resolve_PS = new ResolvePogram(this, isInternalMSAA(), true);
    	tempoalFilter_PS = new TempoalFilterProgram(this, true);
    	
    	dummyVAO = gl.glGenVertexArray();
    	
    	if(m_bStaticSceneTest){
    		System.out.println("Load attribs from disk.");
    		loadTestDataTo("PerApplyCB.dat", perApplyStruct);
    		loadTestDataTo("PerContextCB.dat", perContextStruct);
    		loadTestDataTo("PerFrameCB.dat", perFrameStruct);
    		loadTestDataTo("PerVolumeCB.dat", perVolumeStruct);
    		
    		System.out.println(perApplyStruct);
    		System.out.println(perContextStruct);
    		System.out.println(perFrameStruct);
    		System.out.println(perVolumeStruct);
    	}
    	
    	return Status.OK;
    }
    
    
    static void loadTestDataTo(String filename, Writable dst){
		final String root = "E:\\textures\\VolumetricLighting\\attribs\\";
		ByteBuffer data = DebugTools.loadBinary(root + filename);
		dst.load(data);
	}
    
    @Override
    public void dispose() {
    	if(pPerContextCB != null){
    		pPerContextCB.dispose();
    		pPerContextCB = null;
    	}

    	if(pPerFrameCB != null){
    		pPerFrameCB.dispose();
    		pPerFrameCB = null;
    	}

    	if(pPerVolumeCB != null){
    		pPerVolumeCB.dispose();
    		pPerVolumeCB = null;
    	}

    	if(pPerApplyCB != null){
    		pPerApplyCB.dispose();
    		pPerApplyCB = null;
    	}

    	if(pDepth_ != null){
    		pDepth_.dispose();
    		pDepth_ = null;
    	}

    	if(pPhaseLUT_ != null){
    		pPhaseLUT_.dispose();
    		pPhaseLUT_ = null;
    	}

    	for(int i = 0; i < pLightLUT_P_.length; i++){
    		if(pLightLUT_P_[i] != null){
    			pLightLUT_P_[i].dispose();
    			pLightLUT_P_[i] = null;
    		}
    	}

    	for(int i = 0; i < pLightLUT_S1_.length; i++){
    		if(pLightLUT_S1_[i] != null){
    			pLightLUT_S1_[i].dispose();
    			pLightLUT_S1_[i] = null;
    		}
    	}

    	for(int i = 0; i < pLightLUT_S2_.length; i++){
    		if(pLightLUT_S2_[i] != null){
    			pLightLUT_S2_[i].dispose();
    			pLightLUT_S2_[i] = null;
    		}
    	}

    	if(pAccumulation_ != null){
    		pAccumulation_.dispose();
    		pAccumulation_ = null;
    	}

    	if(pResolvedAccumulation_ != null){
    		pResolvedAccumulation_.dispose();
    		pResolvedAccumulation_ = null;
    	}

    	if(pResolvedDepth_ != null){
    		pResolvedDepth_.dispose();
    		pResolvedDepth_ = null;
    	}

    	for(int i = 0; i < pFilteredAccumulation_.length; i++){
    		if(pFilteredAccumulation_[i] != null){
    			pFilteredAccumulation_[i].dispose();
    			pFilteredAccumulation_[i] = null;
    		}
    	}

    	for(int i = 0; i < pFilteredDepth_.length; i++){
    		if(pFilteredDepth_[i] != null){
    			pFilteredDepth_[i].dispose();
    			pFilteredDepth_[i] = null;
    		}
    	}

    	if(pAccumulatedOutput_ != null){
    		pAccumulatedOutput_.dispose();
    		pAccumulatedOutput_ = null;
    	}
    	
    	if(dummyVAO != 0){
    		gl.glDeleteVertexArray(dummyVAO);
    		dummyVAO = 0;
    	}
    }
    
    public static void main(String[] args) {
//		CodeGen.genSafeDelete(ContextImp_OpenGL.class);
	}
    
    private static Texture2D create(int width, int height, int samples, int format, String debug_name){
    	Texture2DDesc desc = new Texture2DDesc();
    	desc.width = width;
    	desc.height = height;
    	desc.sampleCount = samples;
    	desc.format = format;
    	desc.mipLevels = 1;
    	desc.arraySize = 1;
    	
    	return TextureUtils.createTexture2D(desc, null);
    }
    
    private void setupTextures(RenderVolumeProgram program, int shadowMap, ShadowMapDesc pShadowMapDesc){
    	if(pShadowMapDesc != null){
    		program.setShadowMap(pShadowMapDesc.eType == ShadowMapLayout.PARABOLOID ? GLenum.GL_TEXTURE_2D_ARRAY : GLenum.GL_TEXTURE_2D,
    				shadowMap, samplerPoint);
    		
//    		if(pShadowMapDesc.eType == ShadowMapLayout.PARABOLOID){
//    			System.out.println("GL_TEXTURE_2D_ARRAY");
//    		}else{
//    			System.out.println("GL_TEXTURE_2D");
//    		}
    	}
    	
    	program.setLightLUT_P(pLightLUT_P_[1].getTexture());
    	program.setLightLUT_S1(pLightLUT_S1_[1].getTexture());
    	program.setLightLUT_S2(pLightLUT_S2_[1].getTexture());
    	program.setPhaseLUT(pPhaseLUT_.getTexture());
    	program.setSceneDepth(pDepth_.getTarget(), pDepth_.getTexture());
    	program.tagLocation();
    }
    
    private void setupUniforms(BaseVLProgram program){
    	program.setupUniforms(perApplyStruct);
    	program.setupUniforms(perContextStruct);
    	program.setupUniforms(perFrameStruct);
    	program.setupUniforms(perVolumeStruct);
    }

    private void printProgram(OpenGLProgram program, String name){
		program.setName(name);
		program.printPrograminfo();
	}
	
	@Override
	protected Status beginAccumulation_Start(int sceneDepth, ViewerDesc pViewerDesc, MediumDesc pMediumDesc) {
//		NV_PERFEVENT_BEGIN(dxCtx, "NvVl::BeginAccumulation");
		
		if (!isInitialized_)
	    {
	        // Update the per-context constant buffer on the first frame it's used
	        isInitialized_ = true;
//	        setupCB_PerContext(pPerContextCB->Map(dxCtx));
//	        pPerContextCB->Unmap(dxCtx);
	        
	        if(!m_bStaticSceneTest)
	        	setupCB_PerContext(perContextStruct);
	        perContextStruct.store(pPerContextCB);
	    }
		
		// Setup the constant buffer
		if(!m_bStaticSceneTest)
			setupCB_PerFrame(pViewerDesc, pMediumDesc, perFrameStruct);
		perFrameStruct.store(pPerFrameCB);
		
		if(!mbPrintProgram){
			System.out.println("PhaseFunc: " + Arrays.toString(perFrameStruct.uPhaseFunc));
			System.out.println("PhaseParams: " + Arrays.toString(perFrameStruct.vPhaseParams));
		
		}
		
		gl.glBindVertexArray(dummyVAO);
		
		return Status.OK;
	}

	@Override
	protected Status beginAccumulation_UpdateMediumLUT() {
//		NV_PERFEVENT(dxCtx, "UpdateMediumLUT");

		rtManager.bind();
		rtManager.setRenderTexture(pPhaseLUT_, null);
		gl.glClearColor(0,0,0,0);
//		rtManager.clearRenderTarget(0, 0);
		gl.glClear(GLenum.GL_COLOR_BUFFER_BIT);
		gl.glViewport(0, 0, 1, VLConstant.LIGHT_LUT_WDOTV_RESOLUTION);
		
		computePhaseLookup_PS.enable();
		setupUniforms(computePhaseLookup_PS);
    	gl.glDrawArrays(GLenum.GL_TRIANGLES, 0, 3);
    	computePhaseLookup_PS.disable();
    	
    	if(!mbPrintProgram){
    		printProgram(computePhaseLookup_PS, "UpdateMediumLUT");
    		saveTextureAsText(pPhaseLUT_, "UpdateMediumLUTGL.txt");
    		System.out.println("------------");
    	}
    	
		return Status.OK;
	}

	@Override
	protected Status beginAccumulation_CopyDepth(int sceneDepth) {
//		NV_PERFEVENT(dxCtx, "CopyDepth");
		
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
		downsampleDepth_PS.enable(sceneDepth, samplerPoint);
		setupUniforms(downsampleDepth_PS);
		gl.glDrawArrays(GLenum.GL_TRIANGLES, 0, 3);
		gl.glDepthFunc(GLenum.GL_LESS);
		gl.glDisable(GLenum.GL_DEPTH_TEST);
		
		if(!mbPrintProgram){
    		printProgram(downsampleDepth_PS, "CopyDepth");
    		System.out.println("InternalBufferWidth = " + getInternalBufferWidth());
    		System.out.println("InternalBufferHeight = " + getInternalBufferHeight());
    		
    		saveTextureAsText(pDepth_, "CopyDepthGL.txt");
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

	private final int[] renderVolume_Textures = new int[5];
	@Override
	protected Status renderVolume_Start(int shadowMap, ShadowMapDesc pShadowMapDesc, LightDesc pLightDesc,
			VolumeDesc pVolumeDesc) {
//		NV_PERFEVENT_BEGIN(dxCtx, "NvVl::RenderVolume");
		
		// Setup the constant buffer
		if(!m_bStaticSceneTest)
			setupCB_PerVolume(pShadowMapDesc, pLightDesc, pVolumeDesc, perVolumeStruct);
		if(pPerVolumeCB != null){
			perVolumeStruct.store(pPerVolumeCB);
		}
		
		return Status.OK;
	}
	
	@Override
	protected Status renderVolume_DoVolume_Directional(int shadowMap, ShadowMapDesc pShadowMapDesc,
			LightDesc pLightDesc, VolumeDesc pVolumeDesc) {
		
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
		gl.glClearDepthf(1.f);
		
		gl.glClearStencil(0xFF);
		gl.glClear(GLenum.GL_STENCIL_BUFFER_BIT | GLenum.GL_COLOR_BUFFER_BIT | GLenum.GL_DEPTH_BUFFER_BIT);
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
	    
	    renderVolume_Textures[0] = shadowMap;
	    renderVolume_Textures[1] = pPhaseLUT_.getTexture();
	    
	    ds_render_volume(0xFF, false);
	    no_cull_face();
	    drawFrustumGrid(mesh_resolution, shadowMap, pShadowMapDesc);
	    
	    if(!mbPrintProgram){
//	    	saveTextureAsText(pAccumulation_, "Direction_Light_GL0.txt");
	    	saveTextureAsText(pDepth_, "Direction_Light_DS_GL0.txt");
	    }
	    
	    //--------------------------------------------------------------------------
	    // Remove the illumination from the base of the scene (re-lit by sky later)
	    drawFrustumBase(mesh_resolution, shadowMap, pShadowMapDesc);
	    
	    
	    if(!mbPrintProgram){
//	    	saveTextureAsText(pAccumulation_, "Direction_Light_GL1.txt");
	    	saveTextureAsText(pDepth_, "Direction_Light_DS_GL1.txt");
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
	    	saveTextureAsText(pDepth_, "Direction_Light_DS_GL2.txt");
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
	    	saveTextureAsText(pDepth_, "Direction_Light_DS_GL3.txt");
	    }
	    
	    if(!mbPrintProgram){
	    	saveTextureAsText(pAccumulation_, "Directional_GL.txt");
	    	saveTextureAsText(pDepth_, "Directional_DS_GL.txt");
	    }
		return Status.OK;
	}
	
	@Override
	protected Status renderVolume_DoVolume_Spotlight(int shadowMap, ShadowMapDesc pShadowMapDesc, LightDesc pLightDesc,
			VolumeDesc pVolumeDesc) {
//		NV_PERFEVENT(dxCtx, "Spotlight");

	    int mesh_resolution = getCoarseResolution(pVolumeDesc);
	    if(!mbPrintProgram){
			System.err.println("Spotlight");
			System.err.println("mesh_resolution = " + mesh_resolution);
		}
	    
	    GLCheck.checkError();
	    //--------------------------------------------------------------------------
	    // Create look-up table
	    if(pLightDesc.eFalloffMode == SpotlightFalloffMode.NONE){
//	    	NV_PERFEVENT(dxCtx, "Generate Light LUT");
	    	computeLightLUTDesc.lightMode = RenderVolumeDesc.LIGHTMODE_OMNI;
	    	computeLightLUTDesc.attenuationMode = pLightDesc.eAttenuationMode.ordinal();
	    	computeLightLUTDesc.computePass = RenderVolumeDesc.COMPUTEPASS_CALCULATE;
	    	
	    	renderVolume_Textures[0] = pPhaseLUT_.getTexture();
	    	ComputeLightLUTProgram program = getComputeLightLUTProgram(computeLightLUTDesc, "Generate Light LUT(CALCULATE)");
	    	program.enable(renderVolume_Textures);
	    	setupUniforms(program);
//	    	pLightLUT_P_[0].bindImage(0, GL15.GL_WRITE_ONLY);
	    	gl.glBindImageTexture(0, pLightLUT_P_[0].getTexture(), 0, false, 0, GLenum.GL_WRITE_ONLY, pLightLUT_P_[0].getFormat());
			gl.glDispatchCompute(LIGHT_LUT_DEPTH_RESOLUTION / 32, LIGHT_LUT_WDOTV_RESOLUTION / 8, 1);
			gl.glMemoryBarrier(GLenum.GL_SHADER_IMAGE_ACCESS_BARRIER_BIT);
	    	
	    	if(!mbPrintProgram){
				printProgram(program, "ComputeLightLUT Falloff NONE0");
				System.out.println("programID: " + program);
			}
	    	
	    	renderVolume_Textures[0] = pLightLUT_P_[0].getTexture();
	    	computeLightLUTDesc.computePass = RenderVolumeDesc.COMPUTEPASS_SUM;
	    	program = getComputeLightLUTProgram(computeLightLUTDesc, "Generate Light LUT(SUM)");
	    	program.enable(renderVolume_Textures);
	    	setupUniforms(program);
//	    	pLightLUT_P_[1].bindImage(0, GL15.GL_WRITE_ONLY);
			gl.glBindImageTexture(0, pLightLUT_P_[1].getTexture(), 0, false, 0, GLenum.GL_WRITE_ONLY, pLightLUT_P_[1].getFormat());
			gl.glDispatchCompute(1, LIGHT_LUT_WDOTV_RESOLUTION / 4, 1);
			gl.glMemoryBarrier(GLenum.GL_SHADER_IMAGE_ACCESS_BARRIER_BIT);
	    	
	    	program.disable();
//	    	pLightLUT_P_[1].unbind();
			gl.glBindImageTexture(0, 0, 0, false, 0, GLenum.GL_WRITE_ONLY, GLenum.GL_RGBA8);
	    	
	    	if(!mbPrintProgram){
				printProgram(program, "ComputeLightLUT Falloff NONE1");
				
				saveTextureAsText(pLightLUT_P_[1], "pLightLUT_P_1_GL.txt");
			}
	    }else if(pLightDesc.eFalloffMode == SpotlightFalloffMode.FIXED){
	    	// TODO  NV_PERFEVENT(dxCtx, "Generate Light LUT");
	    	computeLightLUTDesc.lightMode = RenderVolumeDesc.LIGHTMODE_SPOTLIGHT;
	    	computeLightLUTDesc.attenuationMode = pLightDesc.eAttenuationMode.ordinal();
	    	computeLightLUTDesc.computePass = RenderVolumeDesc.COMPUTEPASS_CALCULATE;
	    	
	    	renderVolume_Textures[0] = pPhaseLUT_.getTexture();
	    	ComputeLightLUTProgram program = getComputeLightLUTProgram(computeLightLUTDesc, "Generate Light LUT(CALCULATE)");
	    	program.enable(renderVolume_Textures);
	    	setupUniforms(program);
	    	
	    	/*pLightLUT_P_[0].bindImage(0, GL15.GL_WRITE_ONLY);
	    	pLightLUT_S1_[0].bindImage(1, GL15.GL_WRITE_ONLY);
	    	pLightLUT_S2_[0].bindImage(2, GL15.GL_WRITE_ONLY);*/

			gl.glBindImageTexture(0, pLightLUT_P_[0].getTexture(), 0, false, 0, GLenum.GL_WRITE_ONLY, pLightLUT_P_[0].getFormat());
			gl.glBindImageTexture(0, pLightLUT_S1_[0].getTexture(), 0, false, 0, GLenum.GL_WRITE_ONLY, pLightLUT_S1_[0].getFormat());
			gl.glBindImageTexture(0, pLightLUT_S2_[0].getTexture(), 0, false, 0, GLenum.GL_WRITE_ONLY, pLightLUT_S2_[0].getFormat());

			gl.glDispatchCompute(LIGHT_LUT_DEPTH_RESOLUTION / 32, LIGHT_LUT_WDOTV_RESOLUTION / 8, 1);
			gl.glMemoryBarrier(GLenum.GL_SHADER_IMAGE_ACCESS_BARRIER_BIT);
	    	
	    	if(!mbPrintProgram){
				printProgram(program, "ComputeLightLUT Falloff FIXED0");
				System.out.println("programID: " + program);
			}
	    	
	    	renderVolume_Textures[0] = pLightLUT_P_[0].getTexture();
	    	renderVolume_Textures[1] = pLightLUT_S1_[0].getTexture();
	    	renderVolume_Textures[2] = pLightLUT_S2_[0].getTexture();
	    	computeLightLUTDesc.computePass = RenderVolumeDesc.COMPUTEPASS_SUM;
	    	program = getComputeLightLUTProgram(computeLightLUTDesc, "Generate Light LUT(SUM)");
	    	program.enable(renderVolume_Textures);
	    	setupUniforms(program);
	    	
	    	/*pLightLUT_P_[1].bindImage(0, GL15.GL_WRITE_ONLY);
	    	pLightLUT_S1_[1].bindImage(1, GL15.GL_WRITE_ONLY);
	    	pLightLUT_S2_[1].bindImage(2, GL15.GL_WRITE_ONLY);*/

			gl.glBindImageTexture(0, pLightLUT_P_[1].getTexture(), 0, false, 0, GLenum.GL_WRITE_ONLY, pLightLUT_P_[1].getFormat());
			gl.glBindImageTexture(0, pLightLUT_S1_[1].getTexture(), 0, false, 0, GLenum.GL_WRITE_ONLY, pLightLUT_S1_[1].getFormat());
			gl.glBindImageTexture(0, pLightLUT_S2_[1].getTexture(), 0, false, 0, GLenum.GL_WRITE_ONLY, pLightLUT_S2_[1].getFormat());

			gl.glDispatchCompute(1, LIGHT_LUT_WDOTV_RESOLUTION / 4, 3);
			gl.glMemoryBarrier(GLenum.GL_SHADER_IMAGE_ACCESS_BARRIER_BIT);
	    	
	    	program.disable();
	    	/*pLightLUT_P_[1].unbind();
	    	pLightLUT_S1_[1].unbind();
	    	pLightLUT_S2_[1].unbind();*/
			gl.glBindImageTexture(0, 0, 0, false, 0, GLenum.GL_WRITE_ONLY, pLightLUT_P_[1].getFormat());
			gl.glBindImageTexture(0, 0, 0, false, 0, GLenum.GL_WRITE_ONLY, pLightLUT_S1_[1].getFormat());
			gl.glBindImageTexture(0, 0, 0, false, 0, GLenum.GL_WRITE_ONLY, pLightLUT_S2_[1].getFormat());
	    	
	    	if(!mbPrintProgram){
				printProgram(program, "ComputeLightLUT Falloff FIXED1");
				System.out.println("programID: " + program);
				
				saveTextureAsText(pLightLUT_P_[1], "pLightLUT_P_1_GL.txt");
				saveTextureAsText(pLightLUT_S1_[1], "pLightLUT_S1_GL.txt");
				saveTextureAsText(pLightLUT_S2_[1], "pLightLUT_S2_GL.txt");
			}
	    }
	    
	    //--------------------------------------------------------------------------
	    // Draw tessellated grid
		rtManager.bind();
	    rtManager.setRenderTextures(CommonUtil.toArray(pAccumulation_, pDepth_), null);
	    gl.glViewport(0, 0, getInternalViewportWidth(), getInternalViewportHeight());

	    gl.glClearColor(0,0,0,0);
		gl.glClearStencil(0xFF);
		gl.glClear(GLenum.GL_STENCIL_BUFFER_BIT|GLenum.GL_COLOR_BUFFER_BIT);
//	    GL30.glClearBufferiv(GL11.GL_STENCIL, 0, GLUtil.wrapi1(0xFF));
//	    rtManager.clearDepthStencilTarget(1, 0);
//	    rtManager.clearRenderTarget(0, 0);
	    
	 // Determine DS/HS permutation
	    renderVolumeDesc.shadowMapType = RenderVolumeDesc.SHADOWMAPTYPE_ATLAS;
	    renderVolumeDesc.cascadeCount = RenderVolumeDesc.CASCADECOUNT_1;
	    renderVolumeDesc.volumeType = RenderVolumeDesc.VOLUMETYPE_FRUSTUM;
	    switch (pVolumeDesc.eTessQuality) {
		case HIGH:  		   renderVolumeDesc.maxtessfactor = RenderVolumeDesc.MAXTESSFACTOR_HIGH;  break;
		case MEDIUM:  default: renderVolumeDesc.maxtessfactor = RenderVolumeDesc.MAXTESSFACTOR_MEDIUM;  break;
		case LOW: 			   renderVolumeDesc.maxtessfactor = RenderVolumeDesc.MAXTESSFACTOR_LOW;  break;
	    }
	    
	    renderVolumeDesc.sampleMode = isInternalMSAA() ? RenderVolumeDesc.SAMPLEMODE_MSAA : RenderVolumeDesc.SAMPLEMODE_SINGLE;
	    renderVolumeDesc.lightMode = RenderVolumeDesc.LIGHTMODE_SPOTLIGHT;
	    renderVolumeDesc.passMode = RenderVolumeDesc.PASSMODE_GEOMETRY;
	    renderVolumeDesc.attenuationMode = pLightDesc.eAttenuationMode.ordinal();
	    renderVolumeDesc.falloffMode = pLightDesc.eFalloffMode.ordinal();
	    
	    renderVolume_Textures[0] = shadowMap;
//	    renderVolume_Textures[1] = pPhaseLUT_.getTexture();
	    renderVolume_Textures[1] = pLightLUT_P_[1].getTexture();
	    renderVolume_Textures[2] = pLightLUT_S1_[1].getTexture();
	    renderVolume_Textures[3] = pLightLUT_S2_[1].getTexture();
	    
//	    bs_additive_modulate(0,0,0,0);
	    gl.glStencilMask(0xFF);
	    bs_additive();
	    no_cull_face();
	    ds_render_volume(0xFF, false);
	    drawFrustumGrid(mesh_resolution, shadowMap, pShadowMapDesc);
	    
	    if(!mbPrintProgram){
//	    	saveTextureAsText(pAccumulation_, "SpotLight_GL.txt");
	    	saveTextureAsText(pDepth_, "SpotLight_DS_GL1.txt");
	    }
	    
	    //--------------------------------------------------------------------------
	    // Render the bounds of the spotlight volume
	    ds_render_volume(0xFF, false);
	    rs_cull_front();
	    drawFrustumCap(mesh_resolution, shadowMap, pShadowMapDesc);
	    
	    if(!mbPrintProgram){
//	    	saveTextureAsText(pAccumulation_, "SpotLight_GL.txt");
	    	saveTextureAsText(pDepth_, "SpotLight_DS_GL2.txt");
	    }
	    //--------------------------------------------------------------------------
	    // Finish the rendering by filling in stenciled gaps
	    ds_finish_volume(0xFF);
	    renderVolume_Textures[0] = pDepth_.getTexture();  // TODO
	    renderVolumeDesc.passMode = RenderVolumeDesc.PASSMODE_FINAL;
//	    rtManager.setTexture2DRenderTargets(pAccumulation_.getTexture(), 0);
	    drawQuad(shadowMap, pShadowMapDesc);
	    
	    if(mbSavedDepthBuffer){
	    	mbSavedDepthBuffer = false;
	    	/*try {
				DebugTools.saveTextureAsText(pDepth_.getTarget(), pDepth_.getTexture(), 0, "E:/textures/depthStencil.txt");
			} catch (IOException e) {
				e.printStackTrace();
			}*/
	    }
	    
	    if(!mbPrintProgram){
	    	saveTextureAsText(pAccumulation_, "SpotLight_GL.txt");
	    	saveTextureAsText(pDepth_, "SpotLight_DS_GL.txt");
	    }
	    
	    /*if(showFace){  TODO
	    	gl.glClearBufferfv(GLenum.GL_COLOR, 0, CacheBuffer.wrap(0f, 0,0,0));
	    	GL11.glEnable(GL11.GL_BLEND);
	    	GL14.glBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ZERO);
	    	GL11.glDisable(GL11.GL_CULL_FACE);
	    	GL11.glDisable(GL11.GL_STENCIL_TEST);
	    	GL11.glEnable(GL11.GL_DEPTH_TEST);
	    	GL11.glDepthFunc(GL11.GL_LEQUAL);
	    	GL11.glDepthMask(false);
	    	
	    	GLError.checkError();
	    	if(frustumeProgram == null){
	    		frustumeProgram = new LightFrustumeProgram();
	    	}
	    	
	    	frustumeProgram.enable();
	    	frustumeProgram.setFaceColors(faceColors_);
	    	GLError.checkError();
	    	frustumeProgram.setLightToWorld(perVolumeStruct.mLightToWorld);
	    	frustumeProgram.setViewProj(perFrameStruct.mViewProj);
	    	GLError.checkError();
	    	for(int i = 0; i < 6; i++){
	    		if(faceIdx == 6){ // Render all faces.
	    			renderFace(i);
	    		}else if(i == faceIdx){
	    			renderFace(i);
	    		}
	    	}
	    	
	    	frustumeProgram.disable();
	    }*/
	    
	    gl.glDisable(GLenum.GL_CULL_FACE);
		gl.glDisable(GLenum.GL_STENCIL_TEST);
		gl.glDisable(GLenum.GL_DEPTH_TEST);
		gl.glDisable(GLenum.GL_BLEND);
		gl.glCullFace(GLenum.GL_BACK);
		gl.glFrontFace(GLenum.GL_CCW);
	    
		return Status.OK;
	}
	
	private void renderFace(int idx){
		/*frustumeProgram.setFaceID(idx);
		GL11.glDrawArrays(GL11.GL_TRIANGLE_FAN, 0, 4);
		GLError.checkError();*/
	}

	private void bindImage(TextureGL tex, int unit, int access){
		gl.glBindImageTexture(unit, tex != null ? tex.getTexture():0, 0, false, 0, access, tex!=null?tex.getFormat():GLenum.GL_RGBA8);
	}

	@Override
	protected Status renderVolume_DoVolume_Omni(int shadowMap, ShadowMapDesc pShadowMapDesc, LightDesc pLightDesc,
			VolumeDesc pVolumeDesc) {
//		NV_PERFEVENT(dxCtx, "Omni");
		
		if(!mbPrintProgram){
			System.err.println("Omni");
		}
		
		int mesh_resolution = getCoarseResolution(pVolumeDesc);
		
		//--------------------------------------------------------------------------
	    // Create look-up table
		{
			computeLightLUTDesc.lightMode = RenderVolumeDesc.LIGHTMODE_OMNI;
			computeLightLUTDesc.attenuationMode = pLightDesc.eAttenuationMode.ordinal();
			computeLightLUTDesc.computePass = RenderVolumeDesc.COMPUTEPASS_CALCULATE;
			
	    	renderVolume_Textures[0] = pPhaseLUT_.getTexture();
	    	ComputeLightLUTProgram program = getComputeLightLUTProgram(computeLightLUTDesc, "Generate Light LUT(CALCULATE)");
	    	program.enable(renderVolume_Textures);
	    	setupUniforms(program);
	    	
	    	bindImage(pLightLUT_P_[0],0, GLenum.GL_WRITE_ONLY);

	    	gl.glDispatchCompute(LIGHT_LUT_DEPTH_RESOLUTION / 32, LIGHT_LUT_WDOTV_RESOLUTION / 8, 1);
			gl.glMemoryBarrier(GLenum.GL_SHADER_IMAGE_ACCESS_BARRIER_BIT);
	    	
	    	renderVolume_Textures[0] = pLightLUT_P_[0].getTexture();
	    	computeLightLUTDesc.computePass = RenderVolumeDesc.COMPUTEPASS_SUM;
	    	program = getComputeLightLUTProgram(computeLightLUTDesc, "Generate Light LUT(SUM)");
	    	program.enable(renderVolume_Textures);
	    	setupUniforms(program);
	    	
	    	bindImage(pLightLUT_P_[1],0, GLenum.GL_WRITE_ONLY);
	    	gl.glDispatchCompute(1, LIGHT_LUT_WDOTV_RESOLUTION / 4, 1);
			gl.glMemoryBarrier(GLenum.GL_SHADER_IMAGE_ACCESS_BARRIER_BIT);
	    	
	    	program.disable();
//	    	pLightLUT_P_[1].unbind();
			bindImage(null,0, GLenum.GL_WRITE_ONLY);
	    	
	    	if(!mbPrintProgram){
		    	saveTextureAsText(pLightLUT_P_[1], "Omni_LUT_GL.txt");
		    }
		}
		
		//--------------------------------------------------------------------------
	    // Draw tessellated grid
		rtManager.setRenderTextures(CommonUtil.toArray(pAccumulation_, pDepth_), null);
		gl.glViewport(0, 0, getInternalViewportWidth(), getInternalViewportHeight());
		gl.glClearStencil(0xFF);
		gl.glClearColor(0,0,0,0);
		gl.glClear(GLenum.GL_STENCIL_BUFFER_BIT|GLenum.GL_COLOR_BUFFER_BIT);
//		rtManager.clearRenderTarget(0, 0);
		// TODO RSSetState(states.rs.cull_none);
		// TODO OMSetBlendState(states.bs.additive, nullptr, 0xFFFFFFFF);
		bs_additive();
		no_cull_face();
		
		// Determine DS/HS permutation
		renderVolumeDesc.shadowMapType = RenderVolumeDesc.SHADOWMAPTYPE_ARRAY;
		renderVolumeDesc.cascadeCount = RenderVolumeDesc.CASCADECOUNT_1;
		renderVolumeDesc.volumeType = RenderVolumeDesc.VOLUMETYPE_PARABOLOID;
		switch (pVolumeDesc.eTessQuality) {
		case HIGH:  		   renderVolumeDesc.maxtessfactor = RenderVolumeDesc.MAXTESSFACTOR_HIGH;  break;
		case MEDIUM:  default: renderVolumeDesc.maxtessfactor = RenderVolumeDesc.MAXTESSFACTOR_MEDIUM;  break;
		case LOW: 			   renderVolumeDesc.maxtessfactor = RenderVolumeDesc.MAXTESSFACTOR_LOW;  break;
	    }
		
		renderVolumeDesc.sampleMode = isInternalMSAA() ? RenderVolumeDesc.SAMPLEMODE_MSAA : RenderVolumeDesc.SAMPLEMODE_SINGLE;
	    renderVolumeDesc.lightMode = RenderVolumeDesc.LIGHTMODE_OMNI;
	    renderVolumeDesc.passMode = RenderVolumeDesc.PASSMODE_GEOMETRY;
	    renderVolumeDesc.attenuationMode = pLightDesc.eAttenuationMode.ordinal();
	    renderVolumeDesc.falloffMode = 0 ;// pLightDesc.eFalloffMode.ordinal();
	    
	    renderVolume_Textures[0] = shadowMap;
	    renderVolume_Textures[1] = pLightLUT_P_[1].getTexture();
	    
	    ds_render_volume(0xFF, false);
	    drawOmniVolume(mesh_resolution, shadowMap, pShadowMapDesc);
	    
	    //--------------------------------------------------------------------------
	    // Finish the rendering by filling in stenciled gaps
	    ds_finish_volume(0xFF);
//	    rtManager.setTexture2DRenderTargets(pAccumulation_.getTexture(), 0);
//	    renderVolume_Textures[0] = pDepth_.getTexture(); // TODO
	    renderVolumeDesc.passMode = RenderVolumeDesc.PASSMODE_FINAL;
	    drawQuad(shadowMap, pShadowMapDesc);
	    
	    
	    if(!mbPrintProgram){
	    	saveTextureAsText(pAccumulation_, "Omni_GL.txt");
	    	saveTextureAsText(pDepth_, "Omni_DS_GL.txt");
	    }
	    // TODO reset the OpenGL states to default
		return Status.OK;
	}

	@Override
	protected Status renderVolume_End(int shadowMap, ShadowMapDesc pShadowMapDesc, LightDesc pLightDesc,
			VolumeDesc pVolumeDesc) {
		gl.glBindFramebuffer(GLenum.GL_FRAMEBUFFER, 0);

		gl.glDisable(GLenum.GL_STENCIL_TEST);
		gl.glDisable(GLenum.GL_DEPTH_TEST);
		gl.glDisable(GLenum.GL_BLEND);
		gl.glDepthMask(true);
		
		if(!mbPrintProgram){
			System.err.println("renderVolume_End");
		}
		
		return Status.OK;
	}

	@Override
	protected Status endAccumulation_Imp() {
		return Status.OK;
	}

	@Override
	protected Status applyLighting_Start(int sceneTarget, int sceneDepth, PostprocessDesc pPostprocessDesc) {
//		NV_PERFEVENT_BEGIN(dxCtx, "NvVl::ApplyLighting");
		
		// TODO Setup the constant buffer
		if(!m_bStaticSceneTest)
			setupCB_PerApply(pPostprocessDesc, perApplyStruct);
		if(pPerApplyCB != null){
			perApplyStruct.store(pPerApplyCB);
		}
		
		gl.glViewport(0, 0, getInternalViewportWidth(), getInternalViewportHeight());

		gl.glDisable(GLenum.GL_DEPTH_TEST);
		gl.glDisable(GLenum.GL_BLEND);
		
		pAccumulatedOutput_ = pAccumulation_;
		return Status.OK;
	}
	
	private final int[] m_ResolveTargets = new int[2];

	@Override
	protected Status applyLighting_Resolve(PostprocessDesc pPostprocessDesc) {
//		NV_PERFEVENT(dxCtx, "Resolve");
		renderVolume_Textures[0] = pAccumulation_.getTexture();
		renderVolume_Textures[1] = pDepth_.getTexture();
		
		m_ResolveTargets[0] = pResolvedAccumulation_.getTexture();
		m_ResolveTargets[1] = pResolvedDepth_.getTexture();
		
		rtManager.setRenderTextures(CommonUtil.toArray(pResolvedAccumulation_,pResolvedDepth_ ), null);
		gl.glViewport(0, 0, pResolvedAccumulation_.getWidth(), pResolvedAccumulation_.getHeight());
		resolve_PS.enable(renderVolume_Textures);
		setupUniforms(resolve_PS);
		
//		pResolvedAccumulation_.bindImage(0, GL15.GL_WRITE_ONLY);
//		pResolvedDepth_.bindImage(1, GL15.GL_WRITE_ONLY);
		
		gl.glDrawArrays(GLenum.GL_TRIANGLES, 0, 3);
		
		resolve_PS.disable();
		if(!mbPrintProgram){
			printProgram(resolve_PS, "applyLighting_Resolve");
			saveTextureAsText(pResolvedAccumulation_, "Resolve_Accum_GL.txt");
			saveTextureAsText(pResolvedDepth_, "Resolve_Depth_GL.txt");
		}
		
		pAccumulatedOutput_ =pResolvedAccumulation_;
		return Status.OK;
	}

	@Override
	protected Status applyLighting_TemporalFilter(int sceneDepth, PostprocessDesc pPostprocessDesc) {
//		NV_PERFEVENT(dxCtx, "TemporalFilter");
		renderVolume_Textures[0] = pResolvedAccumulation_.getTexture();
		renderVolume_Textures[1] = pFilteredAccumulation_[lastFrameIndex_].getTexture();
		renderVolume_Textures[2] = pResolvedDepth_.getTexture();
		renderVolume_Textures[3] = pFilteredDepth_[lastFrameIndex_].getTexture();
		
		m_ResolveTargets[0] = pFilteredAccumulation_[nextFrameIndex_].getTexture();
		m_ResolveTargets[1] = pFilteredDepth_[nextFrameIndex_].getTexture();
		rtManager.setRenderTextures(CommonUtil.toArray(pFilteredAccumulation_[nextFrameIndex_], pFilteredDepth_[nextFrameIndex_]), null);
		gl.glViewport(0, 0, pFilteredAccumulation_[nextFrameIndex_].getWidth(), pFilteredAccumulation_[nextFrameIndex_].getHeight());
		
		tempoalFilter_PS.enable(renderVolume_Textures);
		setupUniforms(tempoalFilter_PS);
//		pFilteredAccumulation_[nextFrameIndex_].bindImage(0, GL15.GL_WRITE_ONLY);
//		pFilteredDepth_[nextFrameIndex_].bindImage(1, GL15.GL_WRITE_ONLY);
		
		gl.glDrawArrays(GLenum.GL_TRIANGLES, 0, 3);
//		pFilteredAccumulation_[nextFrameIndex_].unbind();
//		pFilteredDepth_[nextFrameIndex_].unbind();
		tempoalFilter_PS.disable();
		pAccumulatedOutput_ = pFilteredAccumulation_[nextFrameIndex_];
		
		return Status.OK;
	}

	@Override
	protected Status applyLighting_Composite(int sceneTarget, int sceneDepth, PostprocessDesc pPostprocessDesc) {
//		NV_PERFEVENT(dxCtx, "Composite");
		gl.glViewport(0, 0, getOutputViewportWidth(), getOutputViewportHeight());
		if(debugFlags_ == DebugFlags.NO_BLENDING){
			bs_debug_blend();
		}else if(debugFlags_ == DebugFlags.NONE){
			bs_additive_modulate(pPostprocessDesc.fBlendfactor, pPostprocessDesc.fBlendfactor, pPostprocessDesc.fBlendfactor, pPostprocessDesc.fBlendfactor);
		}else {
			gl.glEnable(GLenum.GL_BLEND);
			gl.glBlendFuncSeparate(GLenum.GL_ONE, GLenum.GL_ZERO, GLenum.GL_ONE, GLenum.GL_ZERO);
		}
//		rtManager.setTexture2DRenderTargets(sceneTarget, 0);
		rtManager.bind();
		gl.glFramebufferTexture2D(GLenum.GL_FRAMEBUFFER, GLenum.GL_COLOR_ATTACHMENT0, GLenum.GL_TEXTURE_2D, sceneTarget, 0);
		gl.glFramebufferTexture2D(GLenum.GL_FRAMEBUFFER, GLenum.GL_DEPTH_STENCIL_ATTACHMENT, GLenum.GL_TEXTURE_2D, 0, 0);

		applyDesc.sampleMode = isOutputMSAA() ? RenderVolumeDesc.SAMPLEMODE_MSAA : RenderVolumeDesc.SAMPLEMODE_SINGLE;
		switch (pPostprocessDesc.eUpsampleQuality)
	    {
	    default:
	    case POINT:
	    	applyDesc.upsampleMode = RenderVolumeDesc.UPSAMPLEMODE_POINT;
	        break;
	    case BILINEAR:
	    	applyDesc.upsampleMode = RenderVolumeDesc.UPSAMPLEMODE_BILINEAR;
	        break;

	    case BILATERAL:
	    	applyDesc.upsampleMode = RenderVolumeDesc.UPSAMPLEMODE_BILATERAL;
	        break;
	    }
	    if (pPostprocessDesc.bDoFog == false)
	        applyDesc.fogMode = RenderVolumeDesc.FOGMODE_NONE;
	    else if (pPostprocessDesc.bIgnoreSkyFog == true)
	        applyDesc.fogMode = RenderVolumeDesc.FOGMODE_NOSKY;
	    else
	        applyDesc.fogMode = RenderVolumeDesc.FOGMODE_FULL;
	    
	    renderVolume_Textures[0] = pAccumulatedOutput_.getTexture();
	    renderVolume_Textures[1] = sceneDepth;
	    renderVolume_Textures[3] = pPhaseLUT_.getTexture();
	    
	    if(pFilteredDepth_[nextFrameIndex_] != null){
	    	renderVolume_Textures[2] = pFilteredDepth_[nextFrameIndex_].getTexture();
	    }
	    
	    ApplyProgram program = getApplyProgram(applyDesc, "Composite");
//	    program.enable(renderVolume_Textures);
	    program.enable();
	    program.setSceneDepth(GLenum.GL_TEXTURE_2D, sceneDepth);
	    program.setGodraysBuffer(pAccumulatedOutput_.getTexture());
	    if(pFilteredDepth_[nextFrameIndex_] != null)
	    {
	    	Texture2D depthTex = pFilteredDepth_[nextFrameIndex_];
	    	program.setGodraysDepth(depthTex.getTarget(), depthTex.getTexture());
	    }
	    setupUniforms(program);
	    
	    gl.glDrawArrays(GLenum.GL_TRIANGLES, 0, 3);
	    program.disable();
	    
	    if(!mbPrintProgram){
	    	printProgram(program, "applyLighting_Composite");
	    	saveTextureAsText(sceneTarget, "Composite_GL.txt");
	    }
	    
		return Status.OK;
	}

	@Override
	protected Status applyLighting_End(int sceneTarget, int sceneDepth, PostprocessDesc pPostprocessDesc) {
		gl.glBindVertexArray(0);
		mbPrintProgram = true;
		gl.glDepthMask(true);
		return Status.OK;
	}
	
	private void drawFrustumGrid(int resolution, int shadowMap, ShadowMapDesc pShadowMapDesc){
		renderVolumeDesc.meshMode = RenderVolumeDesc.MESHMODE_FRUSTUM_GRID;
		renderVolumeDesc.includeTesslation = true;
		renderVolumeDesc.useQuadVS = false;
		renderVolumeDesc.debugPS = false;
		
		boolean constCB = (pPerVolumeCB == null);
		
		RenderVolumeProgram program = getRenderVolumeShader(renderVolumeDesc);
		if(constCB){
			perVolumeStruct.store(pPerVolumeCB);
		}
		
//		program.enable(renderVolume_Textures);
		program.enable();
		setupTextures(program, shadowMap, pShadowMapDesc);
		setupUniforms(program);
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
		
		renderVolume_Textures[0] = renderVolume_Textures[1];
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
		
		renderVolume_Textures[0] = renderVolume_Textures[1];
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
		RenderVolumeProgram program = (RenderVolumeProgram) programsCache.get(desc);
		if(program == null){
			RenderVolumeDesc key = new RenderVolumeDesc(desc);
			program = new RenderVolumeProgram(this, key);
			programsCache.put(key, program);
		}
		
		return program;
	}
	
	private ComputeLightLUTProgram getComputeLightLUTProgram(ComputeLightLUTDesc desc, String debugName){
		ComputeLightLUTProgram program = (ComputeLightLUTProgram)programsCache.get(desc);
		if(program == null){
			ComputeLightLUTDesc key = new ComputeLightLUTDesc(desc);
			program = new ComputeLightLUTProgram(this, key, debugName);
			programsCache.put(key, program);
		}
		
		return program;
	}
	
	private ApplyProgram getApplyProgram(ApplyDesc desc, String debugName){
		ApplyProgram program = (ApplyProgram)programsCache.get(desc);
		if(program == null){
			ApplyDesc key = new ApplyDesc(desc);
			program = new ApplyProgram(this, key, debugName);
			programsCache.put(key, program);
		}
		
		return program;
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
	
	static void saveTextureAsText(TextureGL texture, String filename){
    	/*try {
			DebugTools.saveTextureAsText(texture.getTarget(), texture.getTexture(), 0, "E:/textures/VolumetricLighting/" + filename);
		} catch (IOException e) {
			e.printStackTrace();
		}*/
    }
	
	static void saveTextureAsText(int texture, String filename){
    	/*try {
			DebugTools.saveTextureAsText(GLenum.GL_TEXTURE_2D, texture, 0, "E:/textures/VolumetricLighting/" + filename);
		} catch (IOException e) {
			e.printStackTrace();
		}*/
    }
}
