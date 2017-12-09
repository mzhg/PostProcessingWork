package jet.opengl.demos.nvidia.hbaoplus;

import java.io.IOException;
import java.util.HashMap;

import jet.opengl.postprocessing.common.Disposeable;
import jet.opengl.postprocessing.common.GLCheck;
import jet.opengl.postprocessing.common.GLFuncProvider;
import jet.opengl.postprocessing.common.GLFuncProviderFactory;
import jet.opengl.postprocessing.common.GLenum;

public class HBAOPlusPostProcess implements Disposeable {

//	private final GlobalConstantStruct m_UniformData = new GlobalConstantStruct();
	private final GlobalConstantBuffer m_GlobalCB = new GlobalConstantBuffer();
	private final PerPassConstantBuffers m_PerPassCB = new PerPassConstantBuffers();
	
	private InputDepthInfo m_InputDepth;
    private InputNormalInfo m_InputNormal = new InputNormalInfo();
    private RenderTargets m_RTs = new RenderTargets();
    private final OutputInfo m_Output = new OutputInfo();
    private VAO m_VAO;
    private AppState m_AppState;
    private RenderOptions m_Options;
    private Viewports m_Viewports;
    private int m_FullResViewDepthTextureId;
    
    private final ProgramDesc m_TempDesc = new ProgramDesc();
    private final HashMap<ProgramDesc, SSAOProgram> m_ProgramMap = new HashMap<>();
    private GLFuncProvider gl;
    
    public HBAOPlusPostProcess() {
    	m_InputDepth = new InputDepthInfo();
    	m_Options = new RenderOptions();
    	m_Viewports = new Viewports();
    	m_AppState = new AppState();
	}
    
    public void create(){
    	try {
			createResources();
    	} catch (Exception e) {
    		e.printStackTrace();
    		ReleaseResources();
		}
    }
    
	public long getAllocatedVideoMemoryBytes() {
		return m_RTs.GetCurrentAllocatedVideoMemoryBytes();
	}
	
	public void performancePostProcessing(GFSDK_SSAO_InputData_GL inputData, GFSDK_SSAO_Parameters parameters,
			GFSDK_SSAO_Output_GL output, int renderMask){
		
		setDataFlow(inputData, parameters, output);

	    m_RTs.preCreate(/*m_GL,*/ m_Options);

//	    GFSDK::SSAO::GL::AppState AppState;
	    m_AppState.save(/*m_GL*/);

	    render(renderMask);

	    m_AppState.restore(/*m_GL*/);
	}
	
	public GFSDK_SSAO_Status preCreateFBOs(GFSDK_SSAO_Parameters parameters, int viewportWidth, int viewportHeight) {
		setAOResolution(viewportWidth, viewportHeight);
		m_Options.setRenderOptions(parameters);
		return m_RTs.preCreate(m_Options);
	}

	@Override
	public void dispose() {
		ReleaseResources();
	}
	
	private void createResources(){
        gl = GLFuncProviderFactory.getGLFuncProvider();
        m_AppState.initlizeGL();
		
		if(m_VAO == null){
			m_VAO = new VAO();
			m_VAO.create(/*m_GL*/);
		}
		
		m_PerPassCB.create();
	}
    
	private void ReleaseResources(){
		if(m_VAO != null){
			m_VAO.release();
			m_VAO = null;
		}
		
		if(m_RTs != null){
			m_RTs.release();
			m_RTs = null;
		}
    }
    void setAOResolution(int Width, int Height){
    	if (Width  != m_RTs.getFullWidth() ||
    	        Height != m_RTs.getFullHeight())
    	    {
    	        m_RTs.releaseResources(/*m_GL*/);
    	        m_RTs.setFullResolution(Width, Height);
    	        m_Viewports.setFullResolution(Width, Height);
    	        m_GlobalCB.setResolutionConstants(m_Viewports);
    	    }
    }

    private void setFullscreenState(){
    	States.setRasterizerStateFullscreenNoScissor(/*m_GL*/);
    	States.setDepthStencilStateDisabled(/*m_GL*/);
    	States.setBlendStateDisabled(/*m_GL*/);
    	States.setSharedBlendState(/*m_GL*/);

        // Our draw calls do not source any vertex or index buffers
        // but we still need to bind a dummy VAO to avoid GL errors on OSX
        m_VAO.bind(/*m_GL*/);

        setFullViewport();
    }
    
    private void render(int RenderMask){
//    	m_GlobalCB.updateBuffer(/*m_GL,*/ RenderMask);  TODO do not need this
    	GLCheck.checkError();
        setFullscreenState();
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
//            DrawDebugNormals(m_Shaders.debugNormals_PS(getFetchNormalPermutation()));
        	m_TempDesc.reset();
        	m_TempDesc.fragFile = "DebugNormals_PS.frag";
        	m_TempDesc.fetchGBufferNormal = getFetchNormalPermutation();
        	DrawDebugNormals(getProgram(m_TempDesc));
        }

        if ((RenderMask & GFSDK_SSAO_RenderMask.GFSDK_SSAO_DRAW_AO) !=0)
        {
        	m_TempDesc.reset();
        	m_TempDesc.fragFile = "DeinterleaveDepth_PS.frag";
//            DrawDeinterleavedDepth(m_Shaders.deinterleaveDepth_PS);
        	DrawDeinterleavedDepth(getProgram(m_TempDesc));

            if (!m_InputNormal.texture.isSet())
            {
//            	m_TempDesc.reset();
            	m_TempDesc.fragFile = "ReconstructNormal_PS.frag";
//                DrawReconstructedNormal(m_Shaders.reconstructNormal_PS);
            	DrawReconstructedNormal(getProgram(m_TempDesc));
            }
            
            
            m_TempDesc.fragFile = "CoarseAO_PS.frag";
            m_TempDesc.foregroundAO = getEnableForegroundAOPermutation();
            m_TempDesc.backgroundAO = getEnableBackgroundAOPermutation();
            m_TempDesc.depthThreshold = getEnableDepthThresholdPermutation();
            m_TempDesc.fetchGBufferNormal = getFetchNormalPermutation();
            
//            DrawCoarseAO(m_Shaders.coarseAO_PS(getEnableForegroundAOPermutation(), getEnableBackgroundAOPermutation(), 
//            		getEnableDepthThresholdPermutation(), getFetchNormalPermutation()));
            
            DrawCoarseAO(getProgram(m_TempDesc));

            if (m_Options.blur.enable)
            {
            	m_TempDesc.reset();
            	m_TempDesc.fragFile = "ReinterleaveAO_PS.frag";
            	m_TempDesc.blur = getEnableBlurPermutation();
//                DrawReinterleavedAO_PreBlur(m_Shaders.reinterleaveAO_PS(getEnableBlurPermutation()));
            	DrawReinterleavedAO_PreBlur(getProgram(m_TempDesc));
            	
            	m_TempDesc.blur = false;
            	m_TempDesc.fragFile = "BlurX_PS.frag";
            	m_TempDesc.sharpnessProfile = getEnableSharpnessProfilePermutation();
            	m_TempDesc.kernelRadius = getBlurKernelRadiusPermutation();
            	
//                DrawBlurX(m_Shaders.blurX_PS(getEnableSharpnessProfilePermutation(), getBlurKernelRadiusPermutation()));
            	DrawBlurX(getProgram(m_TempDesc));
            	
            	m_TempDesc.fragFile = "BlurY_PS.frag";
//                DrawBlurY(m_Shaders.blurX_PS(getEnableSharpnessProfilePermutation(), getBlurKernelRadiusPermutation()));
            	DrawBlurY(getProgram(m_TempDesc));
            }
            else
            {
            	m_TempDesc.reset();
            	m_TempDesc.fragFile = "ReinterleaveAO_PS.frag";
            	m_TempDesc.blur = getEnableBlurPermutation();
//                DrawReinterleavedAO(m_Shaders.reinterleaveAO_PS(getEnableBlurPermutation()));
            	DrawReinterleavedAO(getProgram(m_TempDesc));
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
    
    private boolean getEnableForegroundAOPermutation()
    {
        return (m_Options.enableForegroundAO)/* ?     Shaders.ENABLE_FOREGROUND_AO_1 :
        											Shaders.ENABLE_FOREGROUND_AO_0*/;
    }
    
    private boolean getEnableBackgroundAOPermutation()
    {
        return (m_Options.enableBackgroundAO)/* ?     Shaders.ENABLE_BACKGROUND_AO_1 :
        											Shaders.ENABLE_BACKGROUND_AO_0*/;
    }
    
    private boolean getEnableDepthThresholdPermutation()
    {
        return (m_Options.enableDepthThreshold)/* ?   Shaders.ENABLE_DEPTH_THRESHOLD_1 :
        											Shaders.ENABLE_DEPTH_THRESHOLD_0*/;
    }
    
    private boolean getEnableBlurPermutation()
    {
        return (m_Options.blur.enable) /*? 			Shaders.ENABLE_BLUR_1 :
        											Shaders.ENABLE_BLUR_0*/;
    }
    
    private int getBlurKernelRadiusPermutation()
    {
        return (m_Options.blur.radius == GFSDK_SSAO_BlurRadius.GFSDK_SSAO_BLUR_RADIUS_2) ?  Shaders.KERNEL_RADIUS_2 :
        																					Shaders.KERNEL_RADIUS_4;
    }
    
    private boolean getEnableSharpnessProfilePermutation()
    {
        return (m_Options.blur.SharpnessProfile.enable) /*? Shaders.ENABLE_SHARPNESS_PROFILE_1 :
        												  Shaders.ENABLE_SHARPNESS_PROFILE_0*/;
    }
    
    private SSAOProgram getCopyDepthPS()
    {
//         return (m_InputDepth.depthTextureType == GFSDK_SSAO_DepthTextureType.GFSDK_SSAO_VIEW_DEPTHS) ?
//            m_Shaders.copyDepth_PS(getResolveDepthPermutation()) :
//            m_Shaders.linearizeDepth_PS(getResolveDepthPermutation());
    	
    	m_TempDesc.reset();
    	m_TempDesc.fragFile = (m_InputDepth.depthTextureType == GFSDK_SSAO_DepthTextureType.GFSDK_SSAO_VIEW_DEPTHS) ?
    			"CopyDepth_PS.frag" : "LinearizeDepth_PS.frag";
    	m_TempDesc.resolveDepth = getResolveDepthPermutation() != 0;
    	return getProgram(m_TempDesc);
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

    private void DrawLinearDepth(SSAOProgram Program){
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
//    	        Program.setDepthTexture(/*m_GL, */m_InputDepth.texture.target, m_InputDepth.texture.textureID);
    	        // TODO binding Textures don't forget
    	        
    	        Program.setUniformData(m_GlobalCB.m_Data);

                gl.glDrawArrays(GLenum.GL_TRIANGLES, 0, 3);
//    	        ASSERT_GL_ERROR(m_GL);

    	        m_FullResViewDepthTextureId = m_RTs.getFullResViewDepthTexture(/*m_GL*/).getTexture();
    	    }
    }
    
    private void DrawDebugNormals(SSAOProgram Program){
    	States.setBlendStateDisabled(/*m_GL*/);

        gl.glBindFramebuffer(GLenum.GL_DRAW_FRAMEBUFFER, m_Output.fboId);

        Program.enable(/*m_GL*/);
        Program.setUniformData(m_GlobalCB.m_Data);
//        Program.setDepthTexture(/*m_GL,*/ m_FullResViewDepthTextureId, getAODepthWrapMode());
//        Program.setNormalTexture(/*m_GL,*/ m_InputNormal.texture.target, m_InputNormal.texture.textureID);
     // TODO binding Textures don't forget

        gl.glDrawArrays(GLenum.GL_TRIANGLES, 0, 3);
//        ASSERT_GL_ERROR(m_GL);
    }
    
    void DrawDeinterleavedDepth(SSAOProgram Program){
    	setQuarterViewport();

        Program.enable(/*m_GL*/);
//        Program.setDepthTexture(/*m_GL,*/ m_FullResViewDepthTextureId);
     // TODO binding Textures don't forget
        Program.setUniformData(m_GlobalCB.m_Data);

        int PassIndex = 0;
        for (int SliceIndex = 0; SliceIndex < 16; SliceIndex += /*MAX_NUM_MRTS*/ 8)
        {
//            GL30.glBindBufferBase(GL31.GL_UNIFORM_BUFFER, m_PerPassCBs.GetBindingPoint(), m_PerPassCBs.getBufferId(SliceIndex));
            gl.glBindFramebuffer(GLenum.GL_FRAMEBUFFER, m_RTs.getQuarterResViewDepthTextureArray(/*m_GL,*/ m_Options).getOctaSliceFramebuffer(PassIndex++));
            // TODO PerPass Uniform data
        	// TODO Framebuffer Layer object

            gl.glDrawArrays(GLenum.GL_TRIANGLES, 0, 3);
        }

        setFullViewport();
//        ASSERT_GL_ERROR(m_GL);
    }
    
    void DrawReconstructedNormal(SSAOProgram Program){
        gl.glBindFramebuffer(GLenum.GL_FRAMEBUFFER, m_RTs.getFullResNormalTexture(/*m_GL*/).getFramebuffer());

        Program.enable(/*m_GL*/);
//        Program.setDepthTexture(/*m_GL,*/ m_FullResViewDepthTextureId, getAODepthWrapMode());
        Program.setUniformData(m_GlobalCB.m_Data);
        // TODO binding Textures don't forget

        gl.glDrawArrays(GLenum.GL_TRIANGLES, 0, 3);
//        ASSERT_GL_ERROR(m_GL);
    }
    
    private SSAOProgram getProgram(ProgramDesc desc){
    	SSAOProgram program = m_ProgramMap.get(desc);
    	if(program == null){
    		ProgramDesc key = new ProgramDesc(desc);
    		program = new SSAOProgram();
            try {
                program.compile("PostProcessingDefaultScreenSpaceVS.vert", desc.geomFile, desc.fragFile, desc.getMacros());
            } catch (IOException e) {
                e.printStackTrace();
            }
            m_ProgramMap.put(key, program);
    	}
    	
    	return program;
    }
    
    void DrawCoarseAO(SSAOProgram Program){
    	setQuarterViewport();

        Program.enable(/*m_GL*/);
//        Program.setDepthTexture(/*m_GL,*/ m_RTs.getQuarterResViewDepthTextureArray(/*m_GL,*/ m_Options).getTextureArray(), getAODepthWrapMode());
//        Program.setNormalTexture(/*m_GL,*/ getFullResNormalTextureTarget(), getFullResNormalTexture());
        Program.setUniformData(m_GlobalCB.m_Data);
        // TODO binding Textures don't forget

        gl.glBindFramebuffer(GLenum.GL_DRAW_FRAMEBUFFER, m_RTs.getQuarterResAOTextureArray(/*m_GL*/).getLayeredFramebuffer());
        // TODO
        
        for (int SliceIndex = 0; SliceIndex < 16; ++SliceIndex)
        {
//            GL30.glBindBufferBase(GL31.GL_UNIFORM_BUFFER, m_PerPassCBs.GetBindingPoint(), m_PerPassCBs.getBufferId(SliceIndex));
        	// TODO
            gl.glDrawArrays(GLenum.GL_POINTS, 0, 1);
        }

        setFullViewport();
//        ASSERT_GL_ERROR(m_GL);
    }
    
    void DrawReinterleavedAO(SSAOProgram Program){
//    	ASSERT(!m_Options.Blur.Enable);

    	gl.glBindFramebuffer(GLenum.GL_DRAW_FRAMEBUFFER, m_Output.fboId);
        setOutputBlendState(/*m_GL*/);
        setFullViewport();

        Program.enable(/*m_GL*/);
//        Program.setAOTexture(/*m_GL,*/ m_RTs.getQuarterResAOTextureArray(/*m_GL*/).getTextureArray());
        Program.setUniformData(m_GlobalCB.m_Data);
        // TODO binding Textures don't forget

        gl.glDrawArrays(GLenum.GL_TRIANGLES, 0, 3);
//        ASSERT_GL_ERROR(m_GL);
    }
    
    void DrawReinterleavedAO_PreBlur(SSAOProgram Program){
//    	ASSERT(m_Options.Blur.Enable);

    	gl.glBindFramebuffer(GLenum.GL_FRAMEBUFFER, m_RTs.getFullResAOZTexture2(/*m_GL*/).getFramebuffer());

        Program.enable(/*m_GL*/);
//        Program.setAOTexture(/*m_GL,*/ m_RTs.getQuarterResAOTextureArray(/*m_GL*/).getTextureArray());
//        Program.setDepthTexture(/*m_GL,*/ m_FullResViewDepthTextureId);
        Program.setUniformData(m_GlobalCB.m_Data);
        // TODO binding Textures don't forget

        gl.glDrawArrays(GLenum.GL_TRIANGLES, 0, 3);
//        ASSERT_GL_ERROR(m_GL);
    }
    
    void DrawBlurX(SSAOProgram Program){
        gl.glBindFramebuffer(GLenum.GL_DRAW_FRAMEBUFFER, m_RTs.getFullResAOZTexture(/*m_GL*/).getFramebuffer());

        Program.enable(/*m_GL*/);
//        Program.setAODepthTexture(/*m_GL,*/ m_RTs.getFullResAOZTexture2(/*m_GL*/).getTexture());
        Program.setUniformData(m_GlobalCB.m_Data);
        // TODO binding Textures don't forget

        gl.glDrawArrays(GLenum.GL_TRIANGLES, 0, 3);
//        ASSERT_GL_ERROR(m_GL);
    }
    void DrawBlurY(SSAOProgram Program){
        gl.glBindFramebuffer(GLenum.GL_DRAW_FRAMEBUFFER, m_Output.fboId);
        setOutputBlendState(/*m_GL*/);
        setFullViewport();

        Program.enable(/*m_GL*/);
//        Program.setAODepthTexture(/*m_GL,*/ m_RTs.getFullResAOZTexture(/*m_GL*/).getTexture());
        Program.setUniformData(m_GlobalCB.m_Data);
        // TODO binding Textures don't forget

        gl.glDrawArrays(GLenum.GL_TRIANGLES, 0, 3);
//        ASSERT_GL_ERROR(m_GL);
    }

    private void setDataFlow(GFSDK_SSAO_InputData_GL InputData, GFSDK_SSAO_Parameters Parameters, GFSDK_SSAO_Output_GL Output){
    	setInputData(InputData);
    	setAOParameters(Parameters);
        
        setOutput(Output);
        validateDataFlow();
    }
    
    private GFSDK_SSAO_Status validateDataFlow(){
    	if (m_InputNormal.texture.isSet())
        {
            if (m_InputNormal.texture.width    != m_InputDepth.texture.width ||
                m_InputNormal.texture.height   != m_InputDepth.texture.height)
            {
            	throw new IllegalArgumentException(GFSDK_SSAO_Status.GFSDK_SSAO_INVALID_NORMAL_TEXTURE_RESOLUTION.name());
            }
            if (m_InputNormal.texture.sampleCount != m_InputDepth.texture.sampleCount)
            {
            	throw new IllegalArgumentException(GFSDK_SSAO_Status.GFSDK_SSAO_INVALID_NORMAL_TEXTURE_SAMPLE_COUNT.name());
            }
        }

        return GFSDK_SSAO_Status.GFSDK_SSAO_OK;
    }
    
    private void setInputData(GFSDK_SSAO_InputData_GL InputData){
    	setInputDepths(InputData.depthData);

    	setInputNormals(InputData.normalData);
    }
    
     void setInputDepths(GFSDK_SSAO_InputDepthData DepthData){
//    	m_InputDepth = GFSDK::SSAO::GL::InputDepthInfo();

        m_InputDepth.setData(/*m_GL,*/ DepthData);
        

        m_GlobalCB.setDepthData(m_InputDepth);

        setAOResolution((m_InputDepth.texture.width), (m_InputDepth.texture.height));
    }
    
    GFSDK_SSAO_Status setInputNormals(GFSDK_SSAO_InputNormalData NormalData){
//    	m_InputNormal = GFSDK::SSAO::GL::InputNormalInfo();

        if (!NormalData.enable)
        {
            // Input normals disabled. In this case, the lib reconstructs normals from depths.
            return GFSDK_SSAO_Status.GFSDK_SSAO_OK;
        }

        GFSDK_SSAO_Status Status = m_InputNormal.setData(/*m_GL,*/ NormalData);
        if (Status != GFSDK_SSAO_Status.GFSDK_SSAO_OK)
        {
            throw new IllegalArgumentException(Status.name());
        }

        m_GlobalCB.setNormalData(NormalData);

        return GFSDK_SSAO_Status.GFSDK_SSAO_OK;
    }
    
    GFSDK_SSAO_Status setAOParameters(GFSDK_SSAO_Parameters Params){
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
