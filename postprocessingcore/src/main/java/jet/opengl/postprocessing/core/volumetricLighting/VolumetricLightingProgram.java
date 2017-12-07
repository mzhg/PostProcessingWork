package jet.opengl.postprocessing.core.volumetricLighting;

import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.ReadableVector3f;
import org.lwjgl.util.vector.Vector2f;
import org.lwjgl.util.vector.Vector4f;

import java.io.IOException;

import jet.opengl.postprocessing.core.PostProcessingFrameAttribs;
import jet.opengl.postprocessing.shader.GLSLProgram;
import jet.opengl.postprocessing.shader.Macro;
import jet.opengl.postprocessing.shader.ShaderLoader;
import jet.opengl.postprocessing.shader.ShaderSourceItem;
import jet.opengl.postprocessing.shader.ShaderType;
import jet.opengl.postprocessing.util.CacheBuffer;
import jet.opengl.postprocessing.util.Numeric;

final class VolumetricLightingProgram extends GLSLProgram{
	
	public static final int TEX2D_DEPTH_BUFFER = 0;
	public static final int TEX2D_CAM_SPACEZ = 1;
	public static final int TEX2D_SLICE_END_POINTS = 2;
	public static final int TEX2D_COORDINATES = 3;
	public static final int TEX2D_EPIPOLAR_CAM = 4;
	public static final int TEX2D_INTERP_SOURCE = 5;
	public static final int TEX2D_SHADOWM_MAP = 6;
	public static final int TEX2D_SLICE_UV_ORIGIN = 7;
	public static final int TEX2D_MIN_MAX_DEPTH = 8;
	public static final int TEX2D_STAINED_DEPTH = 9;
	public static final int TEX2D_INIT_INSCTR = 10;
	public static final int TEX2D_COLOR_BUFFER = 11;
	public static final int TEX2D_SCATTERED_COLOR = 12;
	public static final int TEX2D_DOWNSCALED_INSCTR = 13;
	public static final int TEX2D_PRECOMPUTED_INSCTR = 14;
	public static final int TEX2D_SHADOWM_BUFFER = 15;

//	Texture2D<float>  g_tex2DDepthBuffer            : register( t0 );
//	Texture2D<float>  g_tex2DCamSpaceZ              : register( t0 );
//	Texture2D<float4> g_tex2DSliceEndPoints         : register( t4 );
//	Texture2D<float2> g_tex2DCoordinates            : register( t1 );
//	Texture2D<float>  g_tex2DEpipolarCamSpaceZ      : register( t2 );
//	Texture2D<uint2>  g_tex2DInterpolationSource    : register( t7 );
//	Texture2D<float>  g_tex2DLightSpaceDepthMap     : register( t3 );
//	Texture2D<float4> g_tex2DSliceUVDirAndOrigin    : register( t2 );
//	Texture2D<MIN_MAX_DATA_FORMAT> g_tex2DMinMaxLightSpaceDepth  : register( t4 );
//	Texture2D<float4> g_tex2DStainedGlassColorDepth : register( t5 );
//	Texture2D<float3> g_tex2DInitialInsctrIrradiance: register( t6 );
//	Texture2D<float4> g_tex2DColorBuffer            : register( t1 );
//	Texture2D<float3> g_tex2DScatteredColor         : register( t3 );
//	Texture2D<float3> g_tex2DDownscaledInsctrRadiance: register( t2 );
//	Texture2D<INSCTR_LUT_FORMAT> g_tex2DPrecomputedPointLightInsctr: register( t6 );
	
	private int mAngularRayleighBetaLoc = -1;
	private int mLightWorldPosLoc = -1;
	private int mShowLightingOnlyLoc = -1;
	private int mMaxShadowMapStepLoc = -1;
	private int mLightScreenPosLoc = -1;
	private int mFarPlaneZLoc = -1;
	private int mRefinementThresholdLoc = -1;
	private int mAngularMieBetaLoc = -1;
	private int mLightColorAndIntensityLoc = -1;
	private int mShowDepthBreaksLoc = -1;
	private int mCameraPosLoc = -1;
	private int mHG_gLoc = -1;
	private int mMaxStepsAlongRayLoc = -1;
	private int mTotalMieBetaLoc = -1;
	private int mEpipoleSamplingDensityFactorLoc = -1;
	private int mDirOnLightLoc = -1;
	private int mExposureLoc = -1;
	private int mSrcDstMinMaxLevelOffsetLoc = -1;
	private int mShadowMapTexelSizeLoc = -1;
	private int mMaxTracingDistanceLoc = -1;
	private int mSummTotalBetaLoc = -1;
	private int mNearPlaneZLoc = -1;
	private int mWorldToLightProjSpaceLoc = -1;
	private int mTotalRayleighBetaLoc = -1;
	private int mMinMaxShadowMapResolutionLoc = -1;
	private int mIsLightOnScreenLoc = -1;
	private int mProjLoc = -1;
	private int mViewProjInvLoc = -1;
	private int mSpotLightAxisAndCosAngleLoc = -1;
	private int mCorrectScatteringAtDepthBreaksLoc = -1;
	private int mCameraUVAndDepthInShadowMapLoc = -1;
	private CharSequence mShaderSource;

	VolumetricLightingProgram(String fragfile, Macro[] macros){
		try {
			setSourceFromFiles("shader_libs/PostProcessingDefaultScreenSpaceVS.vert", "shader_libs/LightSctr/" + fragfile, macros);
		} catch (IOException e) {
			e.printStackTrace();
		}

		int dot = fragfile.indexOf('.');
		setName(fragfile.substring(0, dot));

		initUniformIndices();
	}

	VolumetricLightingProgram(Void unused, String computeFile, Macro[] macros){
		try {
			CharSequence computeSrc = ShaderLoader.loadShaderFile("shader_libs/LightSctr/" + computeFile, false);
			ShaderSourceItem cs_item = new ShaderSourceItem(computeSrc, ShaderType.COMPUTE);
			cs_item.macros = macros;
			setSourceFromStrings(cs_item);
		} catch (IOException e) {
			e.printStackTrace();
		}

		int dot = computeFile.indexOf('.');
		setName(computeFile.substring(0, dot));

		initUniformIndices();
	}

	VolumetricLightingProgram(String vertFile, String gemoFile, String fragFile, Macro[] macros){
		try {
			CharSequence vertSrc = ShaderLoader.loadShaderFile("shader_libs/LightSctr/" + vertFile, false);
			CharSequence geomSrc = ShaderLoader.loadShaderFile("shader_libs/LightSctr/" + gemoFile, false);
			CharSequence fragSrc = ShaderLoader.loadShaderFile("shader_libs/LightSctr/" + fragFile, false);

			ShaderSourceItem vs_item = new ShaderSourceItem(vertSrc, ShaderType.VERTEX);
			ShaderSourceItem gs_item = new ShaderSourceItem(geomSrc, ShaderType.GEOMETRY);
			ShaderSourceItem ps_item = new ShaderSourceItem(fragSrc, ShaderType.FRAGMENT);

			vs_item.macros = macros;
			gs_item.macros = macros;
			ps_item.macros = macros;
			setSourceFromStrings(vs_item, gs_item, ps_item);
			enable();
		} catch (IOException e) {
			e.printStackTrace();
		}

		int dot = fragFile.indexOf('.');
		setName(fragFile.substring(0, dot));

		initUniformIndices();
	}

	/*
	void printShaderSource(String filename){
		try {
			FileUtils.write(mShaderSource, new File(filename), false);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void printPrograminfo(){
		System.out.println("----------------------------"+debugName +"-----------------------------------------" );
		ProgramProperties props = GLSLUtil.getProperties(getProgram());
		System.out.println(props);
	}*/
	
	protected void initUniformIndices(){
		mAngularRayleighBetaLoc = getUniformLocation("g_f4AngularRayleighBeta");
		mLightWorldPosLoc = getUniformLocation("g_f4LightWorldPos");
		mShowLightingOnlyLoc = getUniformLocation("g_bShowLightingOnly");
		mMaxShadowMapStepLoc = getUniformLocation("g_uiMaxShadowMapStep");
		mLightScreenPosLoc = getUniformLocation("g_f4LightScreenPos");
		mFarPlaneZLoc = getUniformLocation("g_fFarPlaneZ");
		mRefinementThresholdLoc = getUniformLocation("g_fRefinementThreshold");
		mAngularMieBetaLoc = getUniformLocation("g_f4AngularMieBeta");
		mLightColorAndIntensityLoc = getUniformLocation("g_f4LightColorAndIntensity");
		mShowDepthBreaksLoc = getUniformLocation("g_bShowDepthBreaks");
		mCameraPosLoc = getUniformLocation("g_f4CameraPos");
		mHG_gLoc = getUniformLocation("g_f4HG_g");
		mMaxStepsAlongRayLoc = getUniformLocation("g_fMaxStepsAlongRay");
		mTotalMieBetaLoc = getUniformLocation("g_f4TotalMieBeta");
		mEpipoleSamplingDensityFactorLoc = getUniformLocation("m_uiEpipoleSamplingDensityFactor");
		mDirOnLightLoc = getUniformLocation("g_f4DirOnLight");
		mExposureLoc = getUniformLocation("g_fExposure");
		mSrcDstMinMaxLevelOffsetLoc = getUniformLocation("g_ui4SrcDstMinMaxLevelOffset");
		mShadowMapTexelSizeLoc = getUniformLocation("g_f2ShadowMapTexelSize");
		mMaxTracingDistanceLoc = getUniformLocation("g_fMaxTracingDistance");
		mSummTotalBetaLoc = getUniformLocation("g_f4SummTotalBeta");
		mNearPlaneZLoc = getUniformLocation("g_fNearPlaneZ");
		mWorldToLightProjSpaceLoc = getUniformLocation("g_WorldToLightProjSpace");
		mTotalRayleighBetaLoc = getUniformLocation("g_f4TotalRayleighBeta");
		mMinMaxShadowMapResolutionLoc = getUniformLocation("g_uiMinMaxShadowMapResolution");
		mIsLightOnScreenLoc = getUniformLocation("g_bIsLightOnScreen");
		mProjLoc = getUniformLocation("g_Proj");
		mViewProjInvLoc = getUniformLocation("g_ViewProjInv");
		mSpotLightAxisAndCosAngleLoc = getUniformLocation("g_f4SpotLightAxisAndCosAngle");
		mCorrectScatteringAtDepthBreaksLoc = getUniformLocation("g_bCorrectScatteringAtDepthBreaks");
		mCameraUVAndDepthInShadowMapLoc = getUniformLocation("g_f4CameraUVAndDepthInShadowMap");
	}
	
	void setUniform(SLightAttribs attribs){
//		final Vector4f f4DirOnLight = new Vector4f();
//		final Vector4f f4LightColorAndIntensity = new Vector4f();
//		final Vector4f f4AmbientLight = new Vector4f();
//		final Vector4f f4CameraUVAndDepthInShadowMap = new Vector4f();
//		final Vector4f f4LightScreenPos = new Vector4f();
//		final Vector4f f4LightWorldPos = new Vector4f();                // For point and spot lights only
//		final Vector4f f4SpotLightAxisAndCosAngle = new Vector4f();     // For spot light only
//
//	    boolean bIsLightOnScreen;
////	    Vector3f f3Dummy = new Vector3f();
//	    
//	    final Matrix4f mLightViewT = new Matrix4f();
//	    final Matrix4f mLightProjT = new Matrix4f();
//	    final Matrix4f mWorldToLightProjSpaceT = new Matrix4f();
//	    final Matrix4f mCameraProjToLightProjSpaceT = new Matrix4f();
		setDirOnLight(attribs.f4DirOnLight);
		setLightColorAndIntensity(attribs.f4LightColorAndIntensity);
		setCameraUVAndDepthInShadowMap(attribs.f4CameraUVAndDepthInShadowMap);
		setLightScreenPos(attribs.f4LightScreenPos);
		setLightWorldPos(attribs.f4LightWorldPos);
		setSpotLightAxisAndCosAngle(attribs.f4SpotLightAxisAndCosAngle);
		
		setIsLightOnScreen(attribs.bIsLightOnScreen);
		setWorldToLightProjSpace(attribs.mWorldToLightProjSpaceT);

		setShadowMapTexelSize(attribs.m_f2ShadowMapTexelSize);
		setMinMaxShadowMapResolution(attribs.m_uiMinMaxShadowMapResolution);
	}
	
	void setUniform(PostProcessingFrameAttribs attribs){
//		final Vector4f f4CameraPos = new Vector4f();
//		final Matrix4f mViewT = new Matrix4f();
//		final Matrix4f mProjT = new Matrix4f();
//		final Matrix4f mViewProjInvT = new Matrix4f();
//		float mZFar;
//		float mZNear;

		ReadableVector3f cameraPos = attribs.getCameraPos();
		setCameraPos(cameraPos.getX(),cameraPos.getY(), cameraPos.getZ());
		setProj(attribs.projMat);
		setViewProjInv(attribs.getViewProjInvertMatrix());
		setFarPlaneZ(attribs.cameraFar);
		setNearPlaneZ(attribs.cameraNear);
	}
	
	void setUniform(SMiscDynamicParams params){
		setMaxStepsAlongRay(params.fMaxStepsAlongRay);
		setSrcDstMinMaxLevelOffset(params.ui4SrcMinMaxLevelXOffset, params.ui4SrcMinMaxLevelYOffset, 
				params.ui4DstMinMaxLevelXOffset, params.ui4DstMinMaxLevelYOffset);
	}
	
	void setUniform(LightScatteringFrameAttribs attribs) {
		setShowLightingOnly(attribs.m_bShowLightingOnly);
		setMaxShadowMapStep(attribs.m_uiMaxShadowMapStep);
		if(attribs.m_fRefinementThreshold < Numeric.EPSILON)
			attribs.m_fRefinementThreshold = 2.0f;
		
		setRefinementThreshold(attribs.m_fRefinementThreshold);
		setShowDepthBreaks(attribs.m_bShowDepthBreaks);
		setEpipoleSamplingDensityFactor(attribs.m_uiEpipoleSamplingDensityFactor);
		setExposure(attribs.m_fExposure);
		setMaxTracingDistance(attribs.m_fMaxTracingDistance);
	}

	void setUniform(LightScatteringInitAttribs attribs){
		setCorrectScatteringAtDepthBreaks(attribs.m_bCorrectScatteringAtDepthBreaks);
	}

	void setUniform(SParticipatingMediaScatteringParams attribs) {
//		 final Vector4f f4TotalRayleighBeta = new Vector4f();
//		    final Vector4f f4AngularRayleighBeta = new Vector4f();
//		    final Vector4f f4TotalMieBeta = new Vector4f();
//		    final Vector4f f4AngularMieBeta = new Vector4f();
//		    final Vector4f f4HG_g = new Vector4f(); 
//		    final Vector4f f4SummTotalBeta = new Vector4f();
		    
		setAngularRayleighBeta(attribs.f4AngularRayleighBeta);
		setAngularMieBeta(attribs.f4AngularMieBeta);
		setTotalRayleighBeta(attribs.f4TotalRayleighBeta);
		setTotalMieBeta(attribs.f4TotalMieBeta);
		setHG_g(attribs.f4HG_g);
		setSummTotalBeta(attribs.f4SummTotalBeta);
	}
	
	private void setAngularRayleighBeta(Vector4f v) { if(mAngularRayleighBetaLoc >= 0)gl.glUniform4f(mAngularRayleighBetaLoc, v.x, v.y, v.z, v.w);}
	private void setLightWorldPos(Vector4f v) { if(mLightWorldPosLoc >= 0)gl.glUniform4f(mLightWorldPosLoc, v.x, v.y, v.z, 1);}
	private void setShowLightingOnly(boolean b) { if(mShowLightingOnlyLoc >= 0)gl.glUniform1i(mShowLightingOnlyLoc, b ? 1 : 0);}
	private void setMaxShadowMapStep(int i) { if(mMaxShadowMapStepLoc >= 0)gl.glUniform1i(mMaxShadowMapStepLoc, i);}
	private void setLightScreenPos(Vector4f v) { if(mLightScreenPosLoc >= 0)gl.glUniform4f(mLightScreenPosLoc, v.x, v.y, v.z, v.w);}
	private void setFarPlaneZ(float f) { if(mFarPlaneZLoc >= 0)gl.glUniform1f(mFarPlaneZLoc, f);}
	private void setRefinementThreshold(float f) { if(mRefinementThresholdLoc >= 0)gl.glUniform1f(mRefinementThresholdLoc, f);}
	private void setAngularMieBeta(Vector4f v) { if(mAngularMieBetaLoc >= 0)gl.glUniform4f(mAngularMieBetaLoc, v.x, v.y, v.z, v.w);}
	private void setLightColorAndIntensity(Vector4f v) { if(mLightColorAndIntensityLoc >= 0)gl.glUniform4f(mLightColorAndIntensityLoc, v.x, v.y, v.z, v.w);}
	private void setShowDepthBreaks(boolean b) { if(mShowDepthBreaksLoc >= 0)gl.glUniform1i(mShowDepthBreaksLoc, b ? 1 : 0);}
	private void setCameraPos(float x, float y, float z) { if(mCameraPosLoc >= 0)gl.glUniform4f(mCameraPosLoc, x, y, z, 1);}
	private void setHG_g(Vector4f v) { if(mHG_gLoc >= 0)gl.glUniform4f(mHG_gLoc, v.x, v.y, v.z, v.w);}
	private void setMaxStepsAlongRay(float f) { if(mMaxStepsAlongRayLoc >= 0)gl.glUniform1f(mMaxStepsAlongRayLoc, f);}
	private void setTotalMieBeta(Vector4f v) { if(mTotalMieBetaLoc >= 0)gl.glUniform4f(mTotalMieBetaLoc, v.x, v.y, v.z, v.w);}
	private void setEpipoleSamplingDensityFactor(int i) { if(mEpipoleSamplingDensityFactorLoc >= 0)gl.glUniform1i(mEpipoleSamplingDensityFactorLoc, i);}
	private void setDirOnLight(Vector4f v) { if(mDirOnLightLoc >= 0)gl.glUniform4f(mDirOnLightLoc, v.x, v.y, v.z, 0);}
	private void setExposure(float f) { if(mExposureLoc >= 0)gl.glUniform1f(mExposureLoc, f);}
	private void setShadowMapTexelSize(Vector2f v) { if(mShadowMapTexelSizeLoc >= 0)gl.glUniform2f(mShadowMapTexelSizeLoc, v.x, v.y);}
	private void setMaxTracingDistance(float f) { if(mMaxTracingDistanceLoc >= 0)gl.glUniform1f(mMaxTracingDistanceLoc, f);}
	private void setSummTotalBeta(Vector4f v) { if(mSummTotalBetaLoc >= 0)gl.glUniform4f(mSummTotalBetaLoc, v.x, v.y, v.z, v.w);}
	private void setNearPlaneZ(float f) { if(mNearPlaneZLoc >= 0)gl.glUniform1f(mNearPlaneZLoc, f);}
	private void setWorldToLightProjSpace(Matrix4f mat) { if(mWorldToLightProjSpaceLoc >= 0)gl.glUniformMatrix4fv(mWorldToLightProjSpaceLoc, false, CacheBuffer.wrap(mat));}
	private void setTotalRayleighBeta(Vector4f v) { if(mTotalRayleighBetaLoc >= 0)gl.glUniform4f(mTotalRayleighBetaLoc, v.x, v.y, v.z, v.w);}
	private void setMinMaxShadowMapResolution(int i) { if(mMinMaxShadowMapResolutionLoc >= 0)gl.glUniform1i(mMinMaxShadowMapResolutionLoc, i);}
	private void setIsLightOnScreen(boolean b) { if(mIsLightOnScreenLoc >= 0)gl.glUniform1i(mIsLightOnScreenLoc, b ? 1 : 0);}
	private void setProj(Matrix4f mat) { if(mProjLoc >= 0)gl.glUniformMatrix4fv(mProjLoc, false, CacheBuffer.wrap(mat));}
	private void setViewProjInv(Matrix4f mat) { if(mViewProjInvLoc >= 0)gl.glUniformMatrix4fv(mViewProjInvLoc, false, CacheBuffer.wrap(mat));}
	private void setSpotLightAxisAndCosAngle(Vector4f v) { if(mSpotLightAxisAndCosAngleLoc >= 0)gl.glUniform4f(mSpotLightAxisAndCosAngleLoc, v.x, v.y, v.z, v.w);}
	private void setCorrectScatteringAtDepthBreaks(boolean b) { if(mCorrectScatteringAtDepthBreaksLoc >= 0)gl.glUniform1i(mCorrectScatteringAtDepthBreaksLoc, b ? 1 : 0);}
	private void setCameraUVAndDepthInShadowMap(Vector4f v) { if(mCameraUVAndDepthInShadowMapLoc >= 0)gl.glUniform4f(mCameraUVAndDepthInShadowMapLoc, v.x, v.y, v.z, v.w);}
	private void setSrcDstMinMaxLevelOffset(int i0, int i1, int i2, int i3) {if(mSrcDstMinMaxLevelOffsetLoc >= 0)gl.glUniform4i(mSrcDstMinMaxLevelOffsetLoc, i0, i1, i2, i3);}
}
