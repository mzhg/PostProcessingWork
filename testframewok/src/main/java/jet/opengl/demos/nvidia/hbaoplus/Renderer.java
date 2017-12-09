package jet.opengl.demos.nvidia.hbaoplus;

import jet.opengl.postprocessing.common.GLCheck;
import jet.opengl.postprocessing.common.GLFuncProvider;
import jet.opengl.postprocessing.common.GLFuncProviderFactory;
import jet.opengl.postprocessing.common.GLenum;

final class Renderer implements GFSDK_SSAO_Context_GL{

	private InputDepthInfo m_InputDepth;
    private InputNormalInfo m_InputNormal = new InputNormalInfo();
    private GlobalConstantBuffer m_GlobalCB = new GlobalConstantBuffer();
    private PerPassConstantBuffers m_PerPassCBs = new PerPassConstantBuffers();
    private RenderTargets m_RTs = new RenderTargets();
    private OutputInfo m_Output = new OutputInfo();
    private Shaders m_Shaders;
    private VAO m_VAO;
    private AppState m_AppState;
    private RenderOptions m_Options;
    private Viewports m_Viewports;
    private int m_FullResViewDepthTextureId;
    private GLFuncProvider gl;
//    GFSDK_SSAO_CustomHeap m_NewDelete;
//    GFSDK_SSAO_GLFunctions m_GL;
//    GFSDK::SSAO::BuildVersion m_BuildVersion;
    
    public Renderer() {
    	m_InputDepth = new InputDepthInfo();
    	m_Options = new RenderOptions();
    	m_Viewports = new Viewports();
    	m_AppState = new AppState();
    	m_Output = new OutputInfo();
    	
    	create();
	}
    
    public GFSDK_SSAO_Status create(){
    	try {
            gl = GLFuncProviderFactory.getGLFuncProvider();
			createResources();
    	} catch (Exception e) {
    		e.printStackTrace();
    		ReleaseResources();
			
			return GFSDK_SSAO_Status.GFSDK_SSAO_GL_RESOURCE_CREATION_FAILED;
		}
    	
    	return GFSDK_SSAO_Status.GFSDK_SSAO_OK;
    }
    
	@Override
	public long getAllocatedVideoMemoryBytes() {
		return m_RTs.GetCurrentAllocatedVideoMemoryBytes();
	}
	
	@Override
	public GFSDK_SSAO_Status renderAO(GFSDK_SSAO_InputData_GL inputData, GFSDK_SSAO_Parameters parameters,
			GFSDK_SSAO_Output_GL output, int renderMask) {
		GFSDK_SSAO_Status Status;

	    Status = SetDataFlow(inputData, parameters, output);
	    if (Status != GFSDK_SSAO_Status.GFSDK_SSAO_OK)
	    {
	        return Status;
	    }

	    Status = m_RTs.preCreate(/*m_GL,*/ m_Options);
	    if (Status != GFSDK_SSAO_Status.GFSDK_SSAO_OK)
	    {
	        return Status;
	    }

//	    GFSDK::SSAO::GL::AppState AppState;
	    m_AppState.save(/*m_GL*/);

	    Render(renderMask);

	    m_AppState.restore(/*m_GL*/);
		return GFSDK_SSAO_Status.GFSDK_SSAO_OK;
	}
	
	@Override
	public GFSDK_SSAO_Status preCreateFBOs(GFSDK_SSAO_Parameters parameters, int viewportWidth, int viewportHeight) {
		SetAOResolution(viewportWidth, viewportHeight);
		m_Options.setRenderOptions(parameters);
		return m_RTs.preCreate(m_Options);
	}
	@Override
	public GFSDK_SSAO_Status getProjectionMatrixDepthRange(GFSDK_SSAO_InputData_GL InputData,
			GFSDK_SSAO_ProjectionMatrixDepthRange OutputDepthRange) {
		// TODO
		throw new RuntimeException("Unsupport this method in OpneGL!");
	}
	@Override
	public void dispose() {
		ReleaseResources();
	}
	
	void createResources(){
		if(m_GlobalCB == null){
			m_GlobalCB = new GlobalConstantBuffer();
			m_GlobalCB.create(/*m_GL*/);
		}
		
		if(m_PerPassCBs == null){
			m_PerPassCBs = new PerPassConstantBuffers();
			m_PerPassCBs.create(/*m_GL*/);
		}
		
		if(m_Shaders == null){
			m_Shaders = new Shaders();
			m_Shaders.create(/*m_GL*/);
		}
		
		if(m_VAO == null){
			m_VAO = new VAO();
			m_VAO.create(/*m_GL*/);
		}
	}
    
	void ReleaseResources(){
		if(m_GlobalCB != null){
			m_GlobalCB.release();
			m_GlobalCB = null;
		}
		
		if(m_PerPassCBs != null){
			m_PerPassCBs.release();
			m_PerPassCBs = null;
		}
		
		if(m_Shaders != null){
			m_Shaders.release();
			m_Shaders = null;
		}
		
		if(m_VAO != null){
			m_VAO.release();
			m_VAO = null;
		}
		
		if(m_RTs != null){
			m_RTs.release();
			m_RTs = null;
		}
    }
    void SetAOResolution(int Width, int Height){
    	if (Width  != m_RTs.getFullWidth() ||
    	        Height != m_RTs.getFullHeight())
    	    {
    	        m_RTs.releaseResources(/*m_GL*/);
    	        m_RTs.setFullResolution(Width, Height);
    	        m_Viewports.setFullResolution(Width, Height);
    	        m_GlobalCB.setResolutionConstants(m_Viewports);
    	    }
    }

    private void SetFullscreenState(){
    	States.setRasterizerStateFullscreenNoScissor(/*m_GL*/);
    	States.setDepthStencilStateDisabled(/*m_GL*/);
    	States.setBlendStateDisabled(/*m_GL*/);
    	States.setSharedBlendState(/*m_GL*/);

        // Our draw calls do not source any vertex or index buffers
        // but we still need to bind a dummy VAO to avoid GL errors on OSX
        m_VAO.bind(/*m_GL*/);

        setFullViewport();
    }
    
    private void Render(int RenderMask){
    	m_GlobalCB.updateBuffer(/*m_GL,*/ RenderMask);
    	GLCheck.checkError();
        SetFullscreenState();
        GLCheck.checkError();
        RenderHBAOPlus(RenderMask);
        GLCheck.checkError();
    }
    
    private void RenderHBAOPlus(int RenderMask){
//    	ASSERT_GL_ERROR(m_GL);

        if ((RenderMask & GFSDK_SSAO_RenderMask.GFSDK_SSAO_DRAW_Z) !=0)
        {
            DrawLinearDepth(getCopyDepthPS());
        }

        if ((RenderMask & GFSDK_SSAO_RenderMask.GFSDK_SSAO_DRAW_DEBUG_N)!=0)
        {
            DrawDebugNormals(m_Shaders.debugNormals_PS(getFetchNormalPermutation()));
        }

        if ((RenderMask & GFSDK_SSAO_RenderMask.GFSDK_SSAO_DRAW_AO) !=0)
        {
            DrawDeinterleavedDepth(m_Shaders.deinterleaveDepth_PS);

            if (!m_InputNormal.texture.isSet())
            {
                DrawReconstructedNormal(m_Shaders.reconstructNormal_PS);
            }

            DrawCoarseAO(m_Shaders.coarseAO_PS(getEnableForegroundAOPermutation(), getEnableBackgroundAOPermutation(), 
            		getEnableDepthThresholdPermutation(), getFetchNormalPermutation()));

            if (m_Options.blur.enable)
            {
                DrawReinterleavedAO_PreBlur(m_Shaders.reinterleaveAO_PS(getEnableBlurPermutation()));
                DrawBlurX(m_Shaders.blurX_PS(getEnableSharpnessProfilePermutation(), getBlurKernelRadiusPermutation()));
                DrawBlurY(m_Shaders.blurX_PS(getEnableSharpnessProfilePermutation(), getBlurKernelRadiusPermutation()));
            }
            else
            {
                DrawReinterleavedAO(m_Shaders.reinterleaveAO_PS(getEnableBlurPermutation()));
            }
        }
    }

    private int getResolveDepthPermutation()
    {
        return (m_InputDepth.texture.sampleCount == 1) ?    Shaders.RESOLVE_DEPTH_0 :
        													Shaders.RESOLVE_DEPTH_1;
    }
    private int getFetchNormalPermutation()
    {
        return (!m_InputNormal.texture.isSet())          ?  Shaders.FETCH_GBUFFER_NORMAL_0 :
               (m_InputNormal.texture.sampleCount == 1)  ?  Shaders.FETCH_GBUFFER_NORMAL_1 :
            	   											Shaders.FETCH_GBUFFER_NORMAL_2;
    }
    
    private int getEnableForegroundAOPermutation()
    {
        return (m_Options.enableForegroundAO) ?     Shaders.ENABLE_FOREGROUND_AO_1 :
        											Shaders.ENABLE_FOREGROUND_AO_0;
    }
    
    private int getEnableBackgroundAOPermutation()
    {
        return (m_Options.enableBackgroundAO) ?     Shaders.ENABLE_BACKGROUND_AO_1 :
        											Shaders.ENABLE_BACKGROUND_AO_0;
    }
    
    private int getEnableDepthThresholdPermutation()
    {
        return (m_Options.enableDepthThreshold) ?   Shaders.ENABLE_DEPTH_THRESHOLD_1 :
        											Shaders.ENABLE_DEPTH_THRESHOLD_0;
    }
    
    private int getEnableBlurPermutation()
    {
        return (m_Options.blur.enable) ? 			Shaders.ENABLE_BLUR_1 :
        											Shaders.ENABLE_BLUR_0;
    }
    
    private int getBlurKernelRadiusPermutation()
    {
        return (m_Options.blur.radius == GFSDK_SSAO_BlurRadius.GFSDK_SSAO_BLUR_RADIUS_2) ?  Shaders.KERNEL_RADIUS_2 :
        																					Shaders.KERNEL_RADIUS_4;
    }
    
    private int getEnableSharpnessProfilePermutation()
    {
        return (m_Options.blur.SharpnessProfile.enable) ? Shaders.ENABLE_SHARPNESS_PROFILE_1 :
        												  Shaders.ENABLE_SHARPNESS_PROFILE_0;
    }
    
    private CopyDepth_PS getCopyDepthPS()
    {
         return (m_InputDepth.depthTextureType == GFSDK_SSAO_DepthTextureType.GFSDK_SSAO_VIEW_DEPTHS) ?
            m_Shaders.copyDepth_PS(getResolveDepthPermutation()) :
            m_Shaders.linearizeDepth_PS(getResolveDepthPermutation());
    }

    private int getAODepthWrapMode()
    {
        return (m_Options.depthClampMode == GFSDK_SSAO_DepthClampMode.GFSDK_SSAO_CLAMP_TO_EDGE) ? GLenum.GL_CLAMP_TO_EDGE : GLenum.GL_CLAMP_TO_BORDER;
    }
    private int getFullResNormalTexture()
    {
        return (m_InputNormal.texture.isSet()) ? m_InputNormal.texture.textureID : m_RTs.getFullResNormalTexture().getTexture();
    }
    
    private int getFullResNormalTextureTarget()
    {
        return (m_InputNormal.texture.isSet()) ? m_InputNormal.texture.target : GLenum.GL_TEXTURE_2D;
    }

    private void DrawLinearDepth(CopyDepth_PS Program){
    	if (m_InputDepth.depthTextureType == GFSDK_SSAO_DepthTextureType.GFSDK_SSAO_VIEW_DEPTHS &&
    	        m_InputDepth.texture.sampleCount == 1 &&
    	        m_InputDepth.viewport.rectCoversFullInputTexture)
    	    {
    	        m_FullResViewDepthTextureId = m_InputDepth.texture.textureID;
    	        return;
    	    }

    	    {
    	        gl.glBindFramebuffer(GLenum.GL_DRAW_FRAMEBUFFER, m_RTs.getFullResViewDepthTexture(/*m_GL*/).getFramebuffer());

    	        Program.enable(/*m_GL*/);
    	        Program.setDepthTexture(/*m_GL, */m_InputDepth.texture.target, m_InputDepth.texture.textureID);

    	        gl.glDrawArrays(GLenum.GL_TRIANGLES, 0, 3);
//    	        ASSERT_GL_ERROR(m_GL);

    	        m_FullResViewDepthTextureId = m_RTs.getFullResViewDepthTexture(/*m_GL*/).getTexture();
    	    }
    }
    
    private void DrawDebugNormals(DebugNormals_PS Program){
    	States.setBlendStateDisabled(/*m_GL*/);

        gl.glBindFramebuffer(GLenum.GL_DRAW_FRAMEBUFFER, m_Output.fboId);

        Program.enable(/*m_GL*/);
        Program.setDepthTexture(/*m_GL,*/ m_FullResViewDepthTextureId, getAODepthWrapMode());
        Program.setNormalTexture(/*m_GL,*/ m_InputNormal.texture.target, m_InputNormal.texture.textureID);

        gl.glDrawArrays(GLenum.GL_TRIANGLES, 0, 3);
//        ASSERT_GL_ERROR(m_GL);
    }
    
    void DrawDeinterleavedDepth(DeinterleaveDepth_PS Program){
    	setQuarterViewport();

        Program.enable(/*m_GL*/);
        Program.setDepthTexture(/*m_GL,*/ m_FullResViewDepthTextureId);

        int PassIndex = 0;
        for (int SliceIndex = 0; SliceIndex < 16; SliceIndex += /*MAX_NUM_MRTS*/ 8)
        {
            gl.glBindBufferBase(GLenum.GL_UNIFORM_BUFFER, m_PerPassCBs.GetBindingPoint(), m_PerPassCBs.getBufferId(SliceIndex));
            gl.glBindFramebuffer(GLenum.GL_DRAW_FRAMEBUFFER, m_RTs.getQuarterResViewDepthTextureArray(/*m_GL,*/ m_Options).getOctaSliceFramebuffer(PassIndex++));
            gl.glDrawArrays(GLenum.GL_TRIANGLES, 0, 3);
        }

        setFullViewport();
//        ASSERT_GL_ERROR(m_GL);
    }
    
    void DrawReconstructedNormal(ReconstructNormal_PS Program){
        gl.glBindFramebuffer(GLenum.GL_DRAW_FRAMEBUFFER, m_RTs.getFullResNormalTexture(/*m_GL*/).getFramebuffer());

        Program.enable(/*m_GL*/);
        Program.setDepthTexture(/*m_GL,*/ m_FullResViewDepthTextureId, getAODepthWrapMode());

        gl.glDrawArrays(GLenum.GL_TRIANGLES, 0, 3);
//        ASSERT_GL_ERROR(m_GL);
    }
    
    void DrawCoarseAO(CoarseAO_PS Program){
    	setQuarterViewport();

        Program.enable(/*m_GL*/);
        Program.setDepthTexture(/*m_GL,*/ m_RTs.getQuarterResViewDepthTextureArray(/*m_GL,*/ m_Options).getTextureArray(), getAODepthWrapMode());
        Program.setNormalTexture(/*m_GL,*/ getFullResNormalTextureTarget(), getFullResNormalTexture());

        gl.glBindFramebuffer(GLenum.GL_DRAW_FRAMEBUFFER, m_RTs.getQuarterResAOTextureArray(/*m_GL*/).getLayeredFramebuffer());

        for (int SliceIndex = 0; SliceIndex < 16; ++SliceIndex)
        {
            gl.glBindBufferBase(GLenum.GL_UNIFORM_BUFFER, m_PerPassCBs.GetBindingPoint(), m_PerPassCBs.getBufferId(SliceIndex));
            gl.glDrawArrays(GLenum.GL_POINTS, 0, 1);
        }

        setFullViewport();
//        ASSERT_GL_ERROR(m_GL);
    }
    
    void DrawReinterleavedAO(ReinterleaveAO_PS Program){
//    	ASSERT(!m_Options.Blur.Enable);

        gl.glBindFramebuffer(GLenum.GL_DRAW_FRAMEBUFFER, m_Output.fboId);
        setOutputBlendState(/*m_GL*/);
        setFullViewport();

        Program.enable(/*m_GL*/);
        Program.setAOTexture(/*m_GL,*/ m_RTs.getQuarterResAOTextureArray(/*m_GL*/).getTextureArray());

        gl.glDrawArrays(GLenum.GL_TRIANGLES, 0, 3);
//        ASSERT_GL_ERROR(m_GL);
    }
    
    void DrawReinterleavedAO_PreBlur(ReinterleaveAO_PS Program){
//    	ASSERT(m_Options.Blur.Enable);

        gl.glBindFramebuffer(GLenum.GL_DRAW_FRAMEBUFFER, m_RTs.getFullResAOZTexture2(/*m_GL*/).getFramebuffer());

        Program.enable(/*m_GL*/);
        Program.setAOTexture(/*m_GL,*/ m_RTs.getQuarterResAOTextureArray(/*m_GL*/).getTextureArray());
        Program.setDepthTexture(/*m_GL,*/ m_FullResViewDepthTextureId);

        gl.glDrawArrays(GLenum.GL_TRIANGLES, 0, 3);
//        ASSERT_GL_ERROR(m_GL);
    }
    
    void DrawBlurX(Blur_PS Program){
        gl.glBindFramebuffer(GLenum.GL_DRAW_FRAMEBUFFER, m_RTs.getFullResAOZTexture(/*m_GL*/).getFramebuffer());

        Program.enable(/*m_GL*/);
        Program.setAODepthTexture(/*m_GL,*/ m_RTs.getFullResAOZTexture2(/*m_GL*/).getTexture());

        gl.glDrawArrays(GLenum.GL_TRIANGLES, 0, 3);
//        ASSERT_GL_ERROR(m_GL);
    }
    void DrawBlurY(Blur_PS Program){
        gl.glBindFramebuffer(GLenum.GL_DRAW_FRAMEBUFFER, m_Output.fboId);
        setOutputBlendState(/*m_GL*/);
        setFullViewport();

        Program.enable(/*m_GL*/);
        Program.setAODepthTexture(/*m_GL,*/ m_RTs.getFullResAOZTexture(/*m_GL*/).getTexture());

        gl.glDrawArrays(GLenum.GL_TRIANGLES, 0, 3);
//        ASSERT_GL_ERROR(m_GL);
    }

    private GFSDK_SSAO_Status SetDataFlow(GFSDK_SSAO_InputData_GL InputData, GFSDK_SSAO_Parameters Parameters, GFSDK_SSAO_Output_GL Output){
    	GFSDK_SSAO_Status Status;

        Status = SetInputData(InputData);
        if (Status != GFSDK_SSAO_Status.GFSDK_SSAO_OK)
        {
            return Status;
        }

        Status = SetAOParameters(Parameters);
        if (Status != GFSDK_SSAO_Status.GFSDK_SSAO_OK)
        {
            return Status;
        }

        Status = setOutput(Output);
        if (Status != GFSDK_SSAO_Status.GFSDK_SSAO_OK)
        {
            return Status;
        }

        Status = ValidateDataFlow();
        if (Status != GFSDK_SSAO_Status.GFSDK_SSAO_OK)
        {
            return Status;
        }

        return GFSDK_SSAO_Status.GFSDK_SSAO_OK;
    }
    
    private GFSDK_SSAO_Status ValidateDataFlow(){
    	if (m_InputNormal.texture.isSet())
        {
            if (m_InputNormal.texture.width    != m_InputDepth.texture.width ||
                m_InputNormal.texture.height   != m_InputDepth.texture.height)
            {
                return GFSDK_SSAO_Status.GFSDK_SSAO_INVALID_NORMAL_TEXTURE_RESOLUTION;
            }
            if (m_InputNormal.texture.sampleCount != m_InputDepth.texture.sampleCount)
            {
                return GFSDK_SSAO_Status.GFSDK_SSAO_INVALID_NORMAL_TEXTURE_SAMPLE_COUNT;
            }
        }

        return GFSDK_SSAO_Status.GFSDK_SSAO_OK;
    }
    
    private GFSDK_SSAO_Status SetInputData(GFSDK_SSAO_InputData_GL InputData){
    	GFSDK_SSAO_Status Status;

        Status = SetInputDepths(InputData.depthData);
        if (Status != GFSDK_SSAO_Status.GFSDK_SSAO_OK)
        {
            return Status;
        }

        Status = SetInputNormals(InputData.normalData);
        if (Status != GFSDK_SSAO_Status.GFSDK_SSAO_OK)
        {
            return Status;
        }

        return GFSDK_SSAO_Status.GFSDK_SSAO_OK;
    }
    GFSDK_SSAO_Status SetInputDepths(GFSDK_SSAO_InputDepthData DepthData){
//    	m_InputDepth = GFSDK::SSAO::GL::InputDepthInfo();

        GFSDK_SSAO_Status Status = m_InputDepth.setData(/*m_GL,*/ DepthData);
        if (Status != GFSDK_SSAO_Status.GFSDK_SSAO_OK)
        {
            return Status;
        }

        m_GlobalCB.setDepthData(m_InputDepth);

        SetAOResolution((m_InputDepth.texture.width), (m_InputDepth.texture.height));

        return GFSDK_SSAO_Status.GFSDK_SSAO_OK;
    }
    
    GFSDK_SSAO_Status SetInputNormals(GFSDK_SSAO_InputNormalData NormalData){
//    	m_InputNormal = GFSDK::SSAO::GL::InputNormalInfo();

        if (!NormalData.enable)
        {
            // Input normals disabled. In this case, the lib reconstructs normals from depths.
            return GFSDK_SSAO_Status.GFSDK_SSAO_OK;
        }

        GFSDK_SSAO_Status Status = m_InputNormal.setData(/*m_GL,*/ NormalData);
        if (Status != GFSDK_SSAO_Status.GFSDK_SSAO_OK)
        {
            return Status;
        }

        m_GlobalCB.setNormalData(NormalData);

        return GFSDK_SSAO_Status.GFSDK_SSAO_OK;
    }
    
    GFSDK_SSAO_Status SetAOParameters(GFSDK_SSAO_Parameters Params){
    	if (Params.blur.enable != m_Options.blur.enable ||
    	        Params.depthStorage != m_Options.depthStorage)
    	    {
    	        m_RTs.releaseResources(/*m_GL*/);
    	    }

    	    m_GlobalCB.setAOParameters(Params, m_InputDepth);
    	    m_Options.setRenderOptions(Params);

    	    return GFSDK_SSAO_Status.GFSDK_SSAO_OK;
    }
    
    GFSDK_SSAO_Status setOutput(GFSDK_SSAO_Output_GL output){
//    	m_Output = GFSDK::SSAO::GL::OutputInfo();

    	m_Output.init(output);
//        GFSDK_SSAO_Status Status = m_Output.init(output);
//        if (Status != GFSDK_SSAO_OK)
//        {
//            return Status;
//        }

        return GFSDK_SSAO_Status.GFSDK_SSAO_OK;
    }

    private void setFullViewport()
    {
        gl.glViewport(
            (int)(m_Viewports.fullRes.topLeftX), 
            (int)(m_Viewports.fullRes.topLeftY),
            (int)(m_Viewports.fullRes.width),
            (int)(m_Viewports.fullRes.height));
    }

    private void setQuarterViewport()
    {
        gl.glViewport(
            (int)(m_Viewports.quarterRes.topLeftX),
            (int)(m_Viewports.quarterRes.topLeftY),
            (int)(m_Viewports.quarterRes.width),
            (int)(m_Viewports.quarterRes.height));
    }

    private void setOutputBlendState(/*const GFSDK_SSAO_GLFunctions& GL*/)
    {
        if (m_Output.blend.mode == GFSDK_SSAO_BlendMode.GFSDK_SSAO_OVERWRITE_RGB)
        {
            States.setBlendStateDisabledPreserveAlpha(/*GL*/);
        }
        else if (m_Output.blend.mode == GFSDK_SSAO_BlendMode.GFSDK_SSAO_MULTIPLY_RGB)
        {
        	States.setBlendStateMultiplyPreserveAlpha(/*GL*/);
        }
        else
        {
        	States.setCustomBlendState(/*GL,*/ m_Output.blend.customState);
        }
    }
}
