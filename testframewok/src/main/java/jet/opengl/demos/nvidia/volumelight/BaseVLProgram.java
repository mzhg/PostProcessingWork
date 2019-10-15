package jet.opengl.demos.nvidia.volumelight;

import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector2f;
import org.lwjgl.util.vector.Vector3f;
import org.lwjgl.util.vector.Vector4f;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;

import jet.opengl.postprocessing.buffer.BufferGL;
import jet.opengl.postprocessing.common.GLCheck;
import jet.opengl.postprocessing.common.GLFuncProvider;
import jet.opengl.postprocessing.common.GLFuncProviderFactory;
import jet.opengl.postprocessing.common.GLenum;
import jet.opengl.postprocessing.core.OpenGLProgram;
import jet.opengl.postprocessing.shader.GLSLProgram;
import jet.opengl.postprocessing.shader.Macro;
import jet.opengl.postprocessing.shader.ShaderLoader;
import jet.opengl.postprocessing.shader.ShaderSourceItem;
import jet.opengl.postprocessing.shader.ShaderType;
import jet.opengl.postprocessing.util.CacheBuffer;
import jet.opengl.postprocessing.util.CommonUtil;
import jet.opengl.postprocessing.util.Pair;

abstract class BaseVLProgram implements OpenGLProgram {

	private boolean[] containCBs = new boolean[4];
	private BufferGL[] uniformData;
	ContextImp_OpenGL context;
	
	private static final Macro SAMPLEMODE_SINGLE = new Macro("SAMPLEMODE_SINGLE", RenderVolumeDesc.SAMPLEMODE_SINGLE);
	private static final Macro SAMPLEMODE_MSAA = new Macro("SAMPLEMODE_MSAA", RenderVolumeDesc.SAMPLEMODE_MSAA);
	
	private static final Macro LIGHTMODE_OMNI = new Macro("LIGHTMODE_OMNI", RenderVolumeDesc.LIGHTMODE_OMNI);
	private static final Macro LIGHTMODE_SPOTLIGHT = new Macro("LIGHTMODE_SPOTLIGHT", RenderVolumeDesc.LIGHTMODE_SPOTLIGHT);
	
	private static final Macro ATTENUATIONMODE_NONE = new Macro("ATTENUATIONMODE_NONE", RenderVolumeDesc.ATTENUATIONMODE_NONE);
	private static final Macro ATTENUATIONMODE_POLYNOMIAL = new Macro("ATTENUATIONMODE_POLYNOMIAL", RenderVolumeDesc.ATTENUATIONMODE_POLYNOMIAL);
	private static final Macro ATTENUATIONMODE_INV_POLYNOMIAL = new Macro("ATTENUATIONMODE_INV_POLYNOMIAL", RenderVolumeDesc.ATTENUATIONMODE_INV_POLYNOMIAL);
	
	private static final Macro COMPUTEPASS_CALCULATE = new Macro("COMPUTEPASS_CALCULATE", RenderVolumeDesc.COMPUTEPASS_CALCULATE);
	private static final Macro COMPUTEPASS_SUM = new Macro("COMPUTEPASS_SUM", RenderVolumeDesc.COMPUTEPASS_SUM);
	
	static final Macro[] sampleModeSingle = {  //577
			SAMPLEMODE_SINGLE,	
			SAMPLEMODE_MSAA,	
			new Macro("SAMPLEMODE", SAMPLEMODE_SINGLE.value)
	};
	
	static final Macro[] sampleModeMSAA = {
			SAMPLEMODE_SINGLE,	
			SAMPLEMODE_MSAA,	
			new Macro("SAMPLEMODE", SAMPLEMODE_MSAA.value)
	};
	
	static final Macro[] lightModeOMNI = {
		LIGHTMODE_OMNI,
		LIGHTMODE_SPOTLIGHT,
		new Macro("LIGHTMODE", "LIGHTMODE_OMNI")
	};
	
	static final Macro[] lightModeSpot = {
			LIGHTMODE_OMNI,
			LIGHTMODE_SPOTLIGHT,
			new Macro("LIGHTMODE", "LIGHTMODE_SPOTLIGHT")
	};
	
	static final Macro[] attenuationNone = {
			ATTENUATIONMODE_NONE,
			ATTENUATIONMODE_POLYNOMIAL,
			ATTENUATIONMODE_INV_POLYNOMIAL,
			
			new Macro("ATTENUATIONMODE", "ATTENUATIONMODE_NONE")
	};
	
	static final Macro[] attenuationPoly = {
			ATTENUATIONMODE_NONE,
			ATTENUATIONMODE_POLYNOMIAL,
			ATTENUATIONMODE_INV_POLYNOMIAL,
			
			new Macro("ATTENUATIONMODE", "ATTENUATIONMODE_POLYNOMIAL")
	};
	
	static final Macro[] attenuationPolyInv = {
			ATTENUATIONMODE_NONE,
			ATTENUATIONMODE_POLYNOMIAL,
			ATTENUATIONMODE_INV_POLYNOMIAL,
			
			new Macro("ATTENUATIONMODE", "ATTENUATIONMODE_INV_POLYNOMIAL")
	};
	
	static final Macro[] computepassCalculate = {
			COMPUTEPASS_CALCULATE,
			COMPUTEPASS_SUM,
			
			new Macro("COMPUTEPASS", "COMPUTEPASS_CALCULATE")
	};
	
	static final Macro[] computepassSum = {
			COMPUTEPASS_CALCULATE,
			COMPUTEPASS_SUM,
			
			new Macro("COMPUTEPASS", "COMPUTEPASS_SUM")
	};
	
//	uniform float2 g_vOutputSize;				//: packoffset(c0);
//	uniform float2 g_vOutputSize_Inv;			//: packoffset(c0.z);
//	uniform float2 g_vBufferSize;				//: packoffset(c1);
//	uniform float2 g_vBufferSize_Inv;			//: packoffset(c1.z);
//	uniform float g_fResMultiplier;				//: packoffset(c2);
//	uniform uint g_uBufferSamples;				//: packoffset(c2.y);
//
//	uniform float4x4 g_mProj;					//: packoffset(c0);
//	uniform float4x4 g_mViewProj;				//: packoffset(c4);
//	uniform float4x4 g_mViewProjInv;				//: packoffset(c8);
//	uniform float2 g_vOutputViewportSize;        //: packoffset(c12);
//	uniform float2 g_vOutputViewportSize_Inv;    //: packoffset(c12.z);
//	uniform float2 g_vViewportSize;              //: packoffset(c13);
//	uniform float2 g_vViewportSize_Inv;          //: packoffset(c13.z);
//	uniform float3 g_vEyePosition;				//: packoffset(c14);
//	uniform float2 g_vJitterOffset;				//: packoffset(c15);
//	uniform float g_fZNear;						//: packoffset(c15.z);
//	uniform float g_fZFar;						//: packoffset(c15.w);
//	uniform float3 g_vScatterPower;              //: packoffset(c16);
//	uniform uint g_uNumPhaseTerms;       		//: packoffset(c16.w);
//	uniform float3 g_vSigmaExtinction;           //: packoffset(c17);
//	uniform uint g_uPhaseFunc[4];				//: packoffset(c18);
//	uniform float4 g_vPhaseParams[4];			//: packoffset(c22);
//
//	uniform float4x4 g_mLightToWorld;			//  : packoffset(c0);
//	uniform float g_fLightFalloffAngle;			//	: packoffset(c4.x);
//	uniform float g_fLightFalloffPower;			//	: packoffset(c4.y);
//	uniform float g_fGridSectionSize;			//	: packoffset(c4.z);
//	uniform float g_fLightToEyeDepth;			//	: packoffset(c4.w);
//	uniform float g_fLightZNear;                 //  : packoffset(c5);
//	uniform float g_fLightZFar;                  //  : packoffset(c5.y);
//	uniform float4 g_vLightAttenuationFactors;	//	: packoffset(c6);
//	uniform float4x4 g_mLightProj[4];			//  : packoffset(c7);
//	uniform float4x4 g_mLightProjInv[4];			//  : packoffset(c23);
//	uniform float3 g_vLightDir;					//	: packoffset(c39);
//	uniform float g_fGodrayBias;					//	: packoffset(c39.w);
//	uniform float3 g_vLightPos;					//	: packoffset(c40);
//	uniform uint g_uMeshResolution;      		//  : packoffset(c40.w);
//	uniform float3 g_vLightIntensity;			//	: packoffset(c41);
//	uniform float g_fTargetRaySize;				//	: packoffset(c41.w);
//	uniform float4 g_vElementOffsetAndScale[4];	//	: packoffset(c42); 
//	uniform float4 g_vShadowMapDim;				//	: packoffset(c46);
//	uniform uint g_uElementIndex[4];	    		//  : packoffset(c47);
//
//	uniform float4x4 g_mHistoryXform;	       //   : packoffset(c0);	
//	uniform float g_fFilterThreshold;		   //	: packoffset(c4);
//	uniform float g_fHistoryFactor;			   //	: packoffset(c4.y);
//	uniform float3 g_vFogLight;				   //   : packoffset(c5);
//	uniform float g_fMultiScattering;		   //   : packoffset(c5.w);
	
	private int mViewportSize_InvLoc = -1;
	private int mElementOffsetAndScaleLoc = -1;
	private int mTargetRaySizeLoc = -1;
	private int mNumPhaseTermsLoc = -1;
	private int mLightFalloffPowerLoc = -1;
	private int mBufferSize_InvLoc = -1;
	private int mLightAttenuationFactorsLoc = -1;
	private int mOutputSizeLoc = -1;
	private int mLightProjLoc = -1;
	private int mOutputViewportSize_InvLoc = -1;
	private int mLightPosLoc = -1;
	private int mOutputSize_InvLoc = -1;
	private int mViewProjLoc = -1;
	private int mLightDirLoc = -1;
	private int mBufferSizeLoc = -1;
	private int mPhaseFuncLoc = -1;
	private int mLightProjInvLoc = -1;
	private int mPhaseParamsLoc = -1;
	private int mZNearLoc = -1;
	private int mLightFalloffAngleLoc = -1;
	private int mMultiScatteringLoc = -1;
	private int mMeshResolutionLoc = -1;
	private int mHistoryFactorLoc = -1;
	private int mLightZFarLoc = -1;
	private int mLightToWorldLoc = -1;
	private int mShadowMapDimLoc = -1;
	private int mLightIntensityLoc = -1;
	private int mResMultiplierLoc = -1;
	private int mOutputViewportSizeLoc = -1;
	private int mViewportSizeLoc = -1;
	private int mGodrayBiasLoc = -1;
	private int mZFarLoc = -1;
	private int mFogLightLoc = -1;
	private int mSigmaExtinctionLoc = -1;
	private int mHistoryXformLoc = -1;
	private int mGridSectionSizeLoc = -1;
	private int mLightZNearLoc = -1;
	private int mBufferSamplesLoc = -1;
	private int mProjLoc = -1;
	private int mElementIndexLoc = -1;
	private int mEyePositionLoc = -1;
	private int mJitterOffsetLoc = -1;
	private int mScatterPowerLoc = -1;
	private int mViewProjInvLoc = -1;
	private int mFilterThresholdLoc = -1;
	private int mLightToEyeDepthLoc = -1;

	protected int m_programId;
    protected GLFuncProvider gl;
	private String m_name;

	protected int textureLocation;
	
	public BaseVLProgram(ContextImp_OpenGL context) {
		this.context = context;
		gl = GLFuncProviderFactory.getGLFuncProvider();
	}

	public void setName(String name){
	    m_name = name;
    }

    public String getName() { return m_name;}
	
	public void setupUniforms(PerContextCB data){
//		final Vector2f vOutputSize = new Vector2f();
//		final Vector2f vOutputSize_Inv = new Vector2f();
//		
//		final Vector2f vBufferSize = new Vector2f();
//		final Vector2f vBufferSize_Inv = new Vector2f();
//		
//		float fResMultiplier;
//		int uSampleCount;
		
		setOutputSize(data.vOutputSize);
		setOutputSize_Inv(data.vOutputSize_Inv);
		setBufferSize(data.vBufferSize);
		setBufferSize_Inv(data.vBufferSize_Inv);
		setResMultiplier(data.fResMultiplier);
		setBufferSamples(data.uSampleCount);
	}
	
	public void setupUniforms(PerApplyCB data){
//		final Matrix4f mHistoryXform = new Matrix4f();
//	    // c4
//	    float fFilterThreshold;
//	    float fHistoryFactor;
////	    float pad1[2];
//	    // c5
//	    final Vector3f vFogLight = new Vector3f();
//	    float fMultiScattering;
		
		setHistoryXform(data.mHistoryXform);
		setFilterThreshold(data.fFilterThreshold);
		setHistoryFactor(data.fHistoryFactor);
		setFogLight(data.vFogLight);
		setMultiScattering(data.fMultiScattering);
	}
	
	public void setupUniforms(PerFrameCB data){
//		final Matrix4f mProj = new Matrix4f();
//		final Matrix4f mViewProj = new Matrix4f();
//		final Matrix4f mViewProj_Inv = new Matrix4f();
//		
//		final Vector2f vOutputViewportSize = new Vector2f();
//		final Vector2f vOutputViewportSize_Inv = new Vector2f();
//		
//		final Vector2f vViewportSize = new Vector2f();
//		final Vector2f vViewportSize_Inv = new Vector2f();
//		
//		final Vector3f vEyePosition = new Vector3f();
//		
//		final Vector2f vJitterOffset = new Vector2f();
//		float fZNear;
//	    float fZFar;
//	    
//	    final Vector3f vScatterPower = new Vector3f();
//	    int uNumPhaseTerms;
//	    
//	    final Vector3f vSigmaExtinction = new Vector3f();
//	    
//	    final int[] uPhaseFunc = new int[VLConstant.MAX_PHASE_TERMS * 4];
//	    final Vector4f[] vPhaseParams = new Vector4f[VLConstant.MAX_PHASE_TERMS];
		
		setProj(data.mProj);
		setViewProj(data.mViewProj);
		setViewProjInv(data.mViewProj_Inv);
		
		setOutputViewportSize(data.vOutputViewportSize);
		setOutputViewportSize_Inv(data.vOutputViewportSize_Inv);
		
		setViewportSize(data.vViewportSize);
		setViewportSize_Inv(data.vOutputViewportSize_Inv);
		
		setEyePosition(data.vEyePosition);
		setJitterOffset(data.vJitterOffset);
		setZNear(data.fZNear);
		setZFar(data.fZFar);
		setScatterPower(data.vScatterPower);
		setNumPhaseTerms(data.uNumPhaseTerms);
		setSigmaExtinction(data.vSigmaExtinction);
		for(int i = 0; i < 4; i++){
			phaseFunc[i] = data.uPhaseFunc[i][0];
		}
		setPhaseFunc(phaseFunc);
		setPhaseParams(data.vPhaseParams);
	}
	
	private final int[] phaseFunc = new int[4]; 
	
	public void setupUniforms(PerVolumeCB data){
//	    final Matrix4f mLightToWorld = new Matrix4f();
		setLightToWorld(data.mLightToWorld);
	    // c4
//	    float fLightFalloffAngle;
//	    float fLightFalloffPower;
//	    float fGridSectionSize;
//	    float fLightToEyeDepth;
		setLightFalloffAngle(data.fLightFalloffAngle);
		setLightFalloffPower(data.fLightFalloffPower);
		setGridSectionSize(data.fGridSectionSize);
		setLightToEyeDepth(data.fLightToEyeDepth);
	    // c5
//	    float fLightZNear;
//	    float fLightZFar;
		setLightZNear(data.fLightZNear);
		setLightZFar(data.fLightZFar);
//	    float pad[2];
	    // c6
//	    final Vector4f vAttenuationFactors = new Vector4f();
		setLightAttenuationFactors(data.vAttenuationFactors);
	    // c7+16
//	    NvMat44 mLightProj[4];
//	    final Matrix4f[] mLightProj = new Matrix4f[4];
		setLightProj(data.mLightProj);
	    // c23+16
//	    NvMat44 mLightProj_Inv[4];
//	    final Matrix4f[] mLightProj_Inv = new Matrix4f[4];
		setLightProjInv(data.mLightProj_Inv);
	    // c39
//	    final Vector3f vLightDir = new Vector3f();
//	    float fDepthBias;
		setLightDir(data.vLightDir);
		setGodrayBias(data.fDepthBias);
	    // c40
//	    final Vector3f vLightPos = new Vector3f();
//	    int uMeshResolution;
		setLightPos(data.vLightPos);
		setMeshResolution(data.uMeshResolution);
	    // c41
//	    final Vector3f vLightIntensity = new Vector3f();
//	    float fTargetRaySize;
		setLightIntensity(data.vLightIntensity);
		setTargetRaySize(data.fTargetRaySize);
	    // c42+4
//	    final Vector4f[] vElementOffsetAndScale = new Vector4f[4];
		setElementOffsetAndScale(data.vElementOffsetAndScale);
	    // c46
//	    final Vector4f vShadowMapDim = new Vector4f();
		setShadowMapDim(data.vShadowMapDim);
	    // c47+4
	    // Only first index of each "row" is used.
	    // (Need to do this because HLSL can only stride arrays by full offset)
//	    uint32_t uElementIndex[4][4];
//	    final int[] uElementIndex = new int[16];
		for(int i = 0; i < 4; i++){
			phaseFunc[i] = data.uElementIndex[i*4];
		}
		setElementIndex(phaseFunc);
	}


	protected void initUniformData(){
		/*uniformData = Util.toArray(
				context.pPerContextCB = createUniformBlock(0,context.pPerContextCB, PerContextCB.class, GL15.GL_STREAM_READ),
				context.pPerFrameCB = createUniformBlock(1,context.pPerFrameCB, PerFrameCB.class, GL15.GL_DYNAMIC_COPY),
				context.pPerVolumeCB = createUniformBlock(2,context.pPerVolumeCB, PerVolumeCB.class, GL15.GL_STREAM_READ),
				context.pPerApplyCB = createUniformBlock(3,context.pPerApplyCB, PerApplyCB.class, GL15.GL_STREAM_READ)
			);*/
		
		int m_program = getProgram();
		
		gl.glUseProgram(m_program);
		mViewportSize_InvLoc = gl.glGetUniformLocation(m_program, "g_vViewportSize_Inv");
		mElementOffsetAndScaleLoc = gl.glGetUniformLocation(m_program, "g_vElementOffsetAndScale");
		mTargetRaySizeLoc = gl.glGetUniformLocation(m_program, "g_fTargetRaySize");
		mNumPhaseTermsLoc = gl.glGetUniformLocation(m_program, "g_uNumPhaseTerms");
		mLightFalloffPowerLoc = gl.glGetUniformLocation(m_program, "g_fLightFalloffPower");
		mBufferSize_InvLoc = gl.glGetUniformLocation(m_program, "g_vBufferSize_Inv");
		mLightAttenuationFactorsLoc = gl.glGetUniformLocation(m_program, "g_vLightAttenuationFactors");
		mOutputSizeLoc = gl.glGetUniformLocation(m_program, "g_vOutputSize");
		mLightProjLoc = gl.glGetUniformLocation(m_program, "g_mLightProj");
		mOutputViewportSize_InvLoc = gl.glGetUniformLocation(m_program, "g_vOutputViewportSize_Inv");
		mLightPosLoc = gl.glGetUniformLocation(m_program, "g_vLightPos");
		mOutputSize_InvLoc = gl.glGetUniformLocation(m_program, "g_vOutputSize_Inv");
		mViewProjLoc = gl.glGetUniformLocation(m_program, "g_mViewProj");
		mLightDirLoc = gl.glGetUniformLocation(m_program, "g_vLightDir");
		mBufferSizeLoc = gl.glGetUniformLocation(m_program, "g_vBufferSize");
		mPhaseFuncLoc = gl.glGetUniformLocation(m_program, "g_uPhaseFunc");
		mLightProjInvLoc = gl.glGetUniformLocation(m_program, "g_mLightProjInv");
		mPhaseParamsLoc = gl.glGetUniformLocation(m_program, "g_vPhaseParams");
		mZNearLoc = gl.glGetUniformLocation(m_program, "g_fZNear");
		mLightFalloffAngleLoc = gl.glGetUniformLocation(m_program, "g_fLightFalloffAngle");
		mMultiScatteringLoc = gl.glGetUniformLocation(m_program, "g_fMultiScattering");
		mMeshResolutionLoc = gl.glGetUniformLocation(m_program, "g_uMeshResolution");
		mHistoryFactorLoc = gl.glGetUniformLocation(m_program, "g_fHistoryFactor");
		mLightZFarLoc = gl.glGetUniformLocation(m_program, "g_fLightZFar");
		mLightToWorldLoc = gl.glGetUniformLocation(m_program, "g_mLightToWorld");
		mShadowMapDimLoc = gl.glGetUniformLocation(m_program, "g_vShadowMapDim");
		mLightIntensityLoc = gl.glGetUniformLocation(m_program, "g_vLightIntensity");
		mResMultiplierLoc = gl.glGetUniformLocation(m_program, "g_fResMultiplier");
		mOutputViewportSizeLoc = gl.glGetUniformLocation(m_program, "g_vOutputViewportSize");
		mViewportSizeLoc = gl.glGetUniformLocation(m_program, "g_vViewportSize");
		mGodrayBiasLoc = gl.glGetUniformLocation(m_program, "g_fGodrayBias");
		mZFarLoc = gl.glGetUniformLocation(m_program, "g_fZFar");
		mFogLightLoc = gl.glGetUniformLocation(m_program, "g_vFogLight");
		mSigmaExtinctionLoc = gl.glGetUniformLocation(m_program, "g_vSigmaExtinction");
		mHistoryXformLoc = gl.glGetUniformLocation(m_program, "g_mHistoryXform");
		mGridSectionSizeLoc = gl.glGetUniformLocation(m_program, "g_fGridSectionSize");
		mLightZNearLoc = gl.glGetUniformLocation(m_program, "g_fLightZNear");
		mBufferSamplesLoc = gl.glGetUniformLocation(m_program, "g_uBufferSamples");
		mProjLoc = gl.glGetUniformLocation(m_program, "g_mProj");
		mElementIndexLoc = gl.glGetUniformLocation(m_program, "g_uElementIndex");
		mEyePositionLoc = gl.glGetUniformLocation(m_program, "g_vEyePosition");
		mJitterOffsetLoc = gl.glGetUniformLocation(m_program, "g_vJitterOffset");
		mScatterPowerLoc = gl.glGetUniformLocation(m_program, "g_vScatterPower");
		mViewProjInvLoc = gl.glGetUniformLocation(m_program, "g_mViewProjInv");
		mFilterThresholdLoc = gl.glGetUniformLocation(m_program, "g_fFilterThreshold");
		mLightToEyeDepthLoc = gl.glGetUniformLocation(m_program, "g_fLightToEyeDepth");
		gl.glUseProgram(0);
	}
	
	void bindTexture(int target, int textureID, int sampler, int unit){
		/*gl.glActiveTexture(GLenum.GL_TEXTURE0 + unit);
		gl.glBindTexture(target, textureID);*/
		gl.glBindTextureUnit(unit, textureID);
		gl.glBindSampler(unit, sampler);
	}
	
	private static Macro[] combine(Macro[]...args){
		int length = 0;
		for(Macro[] array : args){
			length += array.length;
		}
		
		Macro[] result = new Macro[length];
		int offset = 0;
		for(Macro[] array : args){
			System.arraycopy(array, 0, result, offset, array.length);
			offset += array.length;
		}
		
		return result;
	}
	
	/*private UniformBlockData createUniformBlock(int index, UniformBlockData uniforms, Class<?> clazz, int usage){
		if(uniforms == null){
			 uniforms = new UniformBlockData(clazz);
		}
		
		if(!uniforms.isValid()){
			uniforms.build(programId, usage);
		}
		
		String blockName = clazz.getSimpleName();
		int uboIdx = GL31.glGetUniformBlockIndex(programId, blockName);
		containCBs[index] =  uboIdx>= 0;
		if(uboIdx >=0){
			String shaderName = getClass().getSimpleName();
			System.out.println(shaderName + ": " + blockName + " " + uboIdx);
		}
		
		return uniforms;
	}*/
	
	@Override
	public void enable() {
		gl.glUseProgram(m_programId);
		
		for(int i = 0; i < containCBs.length; i++){
			if(containCBs[i] && uniformData[i] != null){
				uniformData[i].bind();
			}
		}
	}
	
	@Override
	public void disable() {
		gl.glUseProgram(0);

        // unbind the texture.
        for(int i = textureLocation - 1; i>=0; i--){
            gl.glActiveTexture(GLenum.GL_TEXTURE0 + i);
            gl.glBindTexture(GLenum.GL_TEXTURE_2D, 0);
            gl.glBindSampler(i, 0);
        }
		
		for(int i = 0; i < containCBs.length; i++){
			if(containCBs[i]){
				uniformData[i].unbind();
			}
		}
	}
	
	protected Pair<String, Macro[]> getVSShader() {
		return new Pair<>("Quad_VS.vert", null);
	}
	
	private void setViewportSize_Inv(Vector2f v) { if(mViewportSize_InvLoc >= 0) gl.glUniform2f(mViewportSize_InvLoc, v.x, v.y);}
	private void setElementOffsetAndScale(Vector4f[] v) { if(mElementOffsetAndScaleLoc >= 0)gl.glUniform4fv(mElementOffsetAndScaleLoc, CacheBuffer.wrap(v));}
	private void setTargetRaySize(float f) {if(mTargetRaySizeLoc >= 0) gl.glUniform1f(mTargetRaySizeLoc, f);}
	private void setNumPhaseTerms(int i) { if(mNumPhaseTermsLoc >= 0)gl.glUniform1i(mNumPhaseTermsLoc, i);}
	private void setLightFalloffPower(float f) { if(mLightFalloffPowerLoc >= 0)gl.glUniform1f(mLightFalloffPowerLoc, f);}
	private void setBufferSize_Inv(Vector2f v) { if(mBufferSize_InvLoc >= 0)gl.glUniform2f(mBufferSize_InvLoc, v.x, v.y);}
	private void setLightAttenuationFactors(Vector4f v) { if(mLightAttenuationFactorsLoc >= 0)gl.glUniform4f(mLightAttenuationFactorsLoc, v.x, v.y, v.z, v.w);}
	private void setOutputSize(Vector2f v) { if(mOutputSizeLoc >= 0 )gl.glUniform2f(mOutputSizeLoc, v.x, v.y);}
	private void setLightProj(Matrix4f[] mat) { if(mLightProjLoc >= 0)gl.glUniformMatrix4fv(mLightProjLoc, false, CacheBuffer.wrap(mat));}
	private void setOutputViewportSize_Inv(Vector2f v) { if(mOutputViewportSize_InvLoc>= 0)gl.glUniform2f(mOutputViewportSize_InvLoc, v.x, v.y);}
	private void setLightPos(Vector3f v) { if(mLightPosLoc >= 0)gl.glUniform3f(mLightPosLoc, v.x, v.y, v.z);}
	private void setOutputSize_Inv(Vector2f v) { if(mOutputSize_InvLoc >= 0)gl.glUniform2f(mOutputSize_InvLoc, v.x, v.y);}
	private void setViewProj(Matrix4f mat) { if(mViewProjLoc >= 0)gl.glUniformMatrix4fv(mViewProjLoc, false, CacheBuffer.wrap(mat));}
	private void setLightDir(Vector3f v) { if(mLightDirLoc >= 0)gl.glUniform3f(mLightDirLoc, v.x, v.y, v.z);}
	private void setBufferSize(Vector2f v) { if(mBufferSizeLoc >= 0)gl.glUniform2f(mBufferSizeLoc, v.x, v.y);}
	private void setPhaseFunc(int[] i) { if(mPhaseFuncLoc >= 0)gl.glUniform4i(mPhaseFuncLoc,i[0],i[1],i[2],i[3]);}
	private void setLightProjInv(Matrix4f[] mat) { if(mLightProjInvLoc >= 0)gl.glUniformMatrix4fv(mLightProjInvLoc, false, CacheBuffer.wrap(mat));}
	private void setPhaseParams(Vector4f[] v) { if(mPhaseParamsLoc >= 0)gl.glUniform4fv(mPhaseParamsLoc, CacheBuffer.wrap(v));}
	private void setZNear(float f) { if(mZNearLoc >= 0)gl.glUniform1f(mZNearLoc, f);}
	private void setLightFalloffAngle(float f) { if(mLightFalloffAngleLoc >= 0)gl.glUniform1f(mLightFalloffAngleLoc, f);}
	private void setMultiScattering(float f) { if(mMultiScatteringLoc >= 0)gl.glUniform1f(mMultiScatteringLoc, f);}
	private void setMeshResolution(int i) { if(mMeshResolutionLoc >= 0)gl.glUniform1i(mMeshResolutionLoc, i);}
	private void setHistoryFactor(float f) { if(mHistoryFactorLoc >= 0)gl.glUniform1f(mHistoryFactorLoc, f);}
	private void setLightZFar(float f) { if(mLightZFarLoc >= 0)gl.glUniform1f(mLightZFarLoc, f);}
	private void setLightToWorld(Matrix4f mat) { if(mLightToWorldLoc >= 0)gl.glUniformMatrix4fv(mLightToWorldLoc, false, CacheBuffer.wrap(mat));}
	private void setShadowMapDim(Vector4f v) { if(mShadowMapDimLoc >= 0)gl.glUniform4f(mShadowMapDimLoc, v.x, v.y, v.z, v.w);}
	private void setLightIntensity(Vector3f v) { if(mLightIntensityLoc >= 0)gl.glUniform3f(mLightIntensityLoc, v.x, v.y, v.z);}
	private void setResMultiplier(float f) { if(mResMultiplierLoc >= 0)gl.glUniform1f(mResMultiplierLoc, f);}
	private void setOutputViewportSize(Vector2f v) { if(mOutputViewportSizeLoc >= 0)gl.glUniform2f(mOutputViewportSizeLoc, v.x, v.y);}
	private void setViewportSize(Vector2f v) { if(mViewportSizeLoc >= 0)gl.glUniform2f(mViewportSizeLoc, v.x, v.y);}
	private void setGodrayBias(float f) { if(mGodrayBiasLoc >= 0)gl.glUniform1f(mGodrayBiasLoc, f);}
	private void setZFar(float f) { if(mZFarLoc >= 0)gl.glUniform1f(mZFarLoc, f);}
	private void setFogLight(Vector3f v) { if(mFogLightLoc >= 0)gl.glUniform3f(mFogLightLoc, v.x, v.y, v.z);}
	private void setSigmaExtinction(Vector3f v) { if(mSigmaExtinctionLoc >= 0)gl.glUniform3f(mSigmaExtinctionLoc, v.x, v.y, v.z);}
	private void setHistoryXform(Matrix4f mat) { if(mHistoryXformLoc >= 0)gl.glUniformMatrix4fv(mHistoryXformLoc, false, CacheBuffer.wrap(mat));}
	private void setGridSectionSize(float f) { if(mGridSectionSizeLoc >= 0)gl.glUniform1f(mGridSectionSizeLoc, f);}
	private void setLightZNear(float f) { if(mLightZNearLoc >= 0)gl.glUniform1f(mLightZNearLoc, f);}
	private void setBufferSamples(int i) { if(mBufferSamplesLoc >= 0)gl.glUniform1i(mBufferSamplesLoc, i);}
	private void setProj(Matrix4f mat) { if(mProjLoc >= 0)gl.glUniformMatrix4fv(mProjLoc, false, CacheBuffer.wrap(mat));}
	private void setElementIndex(int[] i) { if(mElementIndexLoc >= 0)gl.glUniform1iv(mElementIndexLoc, CacheBuffer.wrap(i));}
	private void setEyePosition(Vector3f v) { if(mEyePositionLoc >= 0)gl.glUniform3f(mEyePositionLoc, v.x, v.y, v.z);}
	private void setJitterOffset(Vector2f v) { if(mJitterOffsetLoc >= 0)gl.glUniform2f(mJitterOffsetLoc, v.x, v.y);}
	private void setScatterPower(Vector3f v) { if(mScatterPowerLoc >= 0)gl.glUniform3f(mScatterPowerLoc, v.x, v.y, v.z);}
	private void setViewProjInv(Matrix4f mat) { if(mViewProjInvLoc >= 0)gl.glUniformMatrix4fv(mViewProjInvLoc, false, CacheBuffer.wrap(mat));}
	private void setFilterThreshold(float f) { if(mFilterThresholdLoc >= 0)gl.glUniform1f(mFilterThresholdLoc, f);}
	private void setLightToEyeDepth(float f) { if(mLightToEyeDepthLoc >= 0)gl.glUniform1f(mLightToEyeDepthLoc, f);}

	/** Get the fragment shader files. */
	protected Pair<String, Macro[]> getPSShader(){
		return null;
	}

	/** Get the geometric shader files. */
	protected Pair<String, Macro[]> getGSShader(){
		return null;
	}

	/** Get the tessellation hull shader. */
	protected Pair<String, Macro[]> getHSShader(){
		return null;
	}

	/** Get the tessellation domain shader. */
	protected Pair<String, Macro[]> getDSShader(){
		return null;
	}

	/** A new style way to create program. Call this method must be implementing the {@link #getVSShader()} and {@link #getPSShader()} */
	protected void compileProgram(){
		if(m_programId != 0)
			return;

		Object parameter = getParameter();
		m_programId = getCachedProgram(parameter);
		if(m_programId == 0){
			ShaderSourceItem vs_item = null;
			ShaderSourceItem hs_item = null;
			ShaderSourceItem ds_item = null;
			ShaderSourceItem gs_item = null;
			ShaderSourceItem ps_item = null;

			Pair<String, Macro[]> vertexShader = getVSShader();
			if(vertexShader != null){
				vs_item = createShaderItem(vertexShader, ShaderType.VERTEX);
			}

			Pair<String, Macro[]> hullShader = getHSShader();
			if(hullShader != null){
				hs_item = createShaderItem(hullShader, ShaderType.TESS_CONTROL);
			}

			Pair<String, Macro[]> domainShader = getDSShader();
			if(domainShader != null){
				ds_item = createShaderItem(domainShader, ShaderType.TESS_EVAL);
			}

			Pair<String, Macro[]> geometryShader = getGSShader();
			if(geometryShader != null){
				gs_item = createShaderItem(geometryShader, ShaderType.GEOMETRY);
			}

			Pair<String, Macro[]> fragmentShader = getPSShader();
			if(fragmentShader != null){
				ps_item = createShaderItem(fragmentShader, ShaderType.FRAGMENT);
			}

			GLSLProgram program = new GLSLProgram();
			program.setSourceFromStrings(vs_item, hs_item, ds_item, gs_item, ps_item);
			m_programId = program.getProgram();

			g_GlobalProgamCache.put(parameter, m_programId);
		}

		initUniformData();
		GLCheck.checkError();
	}

    protected void compileComputeProgram(String csFile, Macro[] csMacros){
        if(m_programId != 0)
            return;

        Object parameter = getParameter();
        m_programId = getCachedProgram(parameter);
        if(m_programId == 0){
            CharSequence cs_source = loadShader(csFile);

            ShaderSourceItem cs_item = new ShaderSourceItem();
            cs_item.macros = csMacros;
			if(ContextImp_Common.USE_UNIFORM_BLOCK){
				if(cs_item.macros != null){
					int length = cs_item.macros.length;
					cs_item.macros = Arrays.copyOf(cs_item.macros, length+1);
					cs_item.macros[length] = new Macro("USE_UNIFORM_BLOCK", 1);
				}else{
					cs_item.macros = CommonUtil.toArray(new Macro("USE_UNIFORM_BLOCK", 1));
				}
			}

            cs_item.source = cs_source;
            cs_item.type = ShaderType.COMPUTE;


            GLSLProgram program = new GLSLProgram();
            program.setSourceFromStrings(cs_item);
            m_programId = program.getProgram();

            g_GlobalProgamCache.put(parameter, m_programId);
        }
    }

	protected abstract Object getParameter();

    @Override
    public int getProgram() {
        return m_programId;
    }

    protected static final HashMap<Object, Integer> g_GlobalProgamCache = new HashMap<>();

	protected static int getCachedProgram (Object obj){
		Integer programId = g_GlobalProgamCache.get(obj);
		if(programId != null)
			return programId.intValue();
		else
			return 0;
	}

	private ShaderSourceItem createShaderItem(Pair<String, Macro[]> shaderInfo, ShaderType shaderType){
		ShaderSourceItem item = new ShaderSourceItem();

		item.source = loadShader(shaderInfo.first);
		item.macros = shaderInfo.second;
		if(ContextImp_Common.USE_UNIFORM_BLOCK){
			if(item.macros != null){
				int length = item.macros.length;
				item.macros = Arrays.copyOf(item.macros, length+1);
				item.macros[length] = new Macro("USE_UNIFORM_BLOCK", 1);
			}else{
				item.macros = CommonUtil.toArray(new Macro("USE_UNIFORM_BLOCK", 1));
			}
		}

		item.type = shaderType;

		return item;
	}

	/** Load the shader source from class path. */
	protected static CharSequence loadShader(String filename){
		final String root = "nvidia/NvVolumetricLighting/shaders/";
		try {
			CharSequence shaderSrc = ShaderLoader.loadShaderFile(root + filename, false);
			return shaderSrc;
		} catch (IOException e) {
			e.printStackTrace();
		}

		return null;
	}

    @Override
    public void dispose() {
        gl.glDeleteProgram(m_programId);
    }
}
