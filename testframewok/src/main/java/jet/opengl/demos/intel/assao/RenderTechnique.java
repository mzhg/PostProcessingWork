package jet.opengl.demos.intel.assao;

import org.lwjgl.util.vector.Vector2f;
import org.lwjgl.util.vector.Vector2i;
import org.lwjgl.util.vector.Vector4f;

import java.io.IOException;

import jet.opengl.postprocessing.shader.GLSLProgram;
import jet.opengl.postprocessing.shader.Macro;
import jet.opengl.postprocessing.util.CacheBuffer;


final class RenderTechnique extends GLSLProgram{
	public static final int TEX2D_DEPTH_BUFFER = 0;
	public static final int TEX2D_NORMAL_BUFFER = 1;
	public static final int TEX2D_VIEW_DEPTH = 2;
	public static final int TEX2D_VIEW_DEPTH1 = 3;
	public static final int TEX2D_VIEW_DEPTH2 = 4;
	public static final int TEX2D_VIEW_DEPTH3 = 5;
	public static final int TEX2D_IMPORTANCE_MAP = 6;
	public static final int TEX2D_LOAD_COUNTER = 7;
	public static final int TEX2D_BLUR_INPUT = 8;
	public static final int TEX2D_FINAL_SSAO = 9;
	
	private int m_g_ASSAOConsts_DetailAOStrengthLoc = -1;
	private int m_g_ASSAOConsts_EffectShadowPowLoc = -1;
	private int m_g_ASSAOConsts_LoadCounterAvgDivLoc = -1;
	private int m_g_ASSAOConsts_EffectShadowStrengthLoc = -1;
	private int m_g_ASSAOConsts_DepthPrecisionOffsetModLoc = -1;
	private int m_g_ASSAOConsts_Viewport2xPixelSize_x_025Loc = -1;
	private int m_g_ASSAOConsts_NDCToViewAddLoc = -1;
	private int m_g_ASSAOConsts_NormalsUnpackMulLoc = -1;
	private int m_g_ASSAOConsts_Viewport2xPixelSizeLoc = -1;
	private int m_g_ASSAOConsts_EffectShadowClampLoc = -1;
	private int m_g_ASSAOConsts_CameraTanHalfFOVLoc = -1;
	private int m_g_ASSAOConsts_EffectFadeOutAddLoc = -1;
	private int m_g_ASSAOConsts_Dummy0Loc = -1;
	private int m_g_ASSAOConsts_PerPassFullResUVOffsetLoc = -1;
	private int m_g_ASSAOConsts_ViewportPixelSizeLoc = -1;
	private int m_g_ASSAOConsts_DepthUnpackConstsLoc = -1;
	private int m_g_ASSAOConsts_EffectSamplingRadiusNearLimitRecLoc = -1;
	private int m_g_ASSAOConsts_EffectRadiusLoc = -1;
	private int m_g_ASSAOConsts_EffectHorizonAngleThresholdLoc = -1;
	private int m_g_ASSAOConsts_NegRecEffectRadiusLoc = -1;
	private int m_g_ASSAOConsts_AdaptiveSampleCountLimitLoc = -1;
	private int m_g_ASSAOConsts_PassIndexLoc = -1;
	private int m_g_ASSAOConsts_EffectFadeOutMulLoc = -1;
	private int m_g_ASSAOConsts_NDCToViewMulLoc = -1;
	private int m_g_ASSAOConsts_InvSharpnessLoc = -1;
	private int m_g_ASSAOConsts_NormalsUnpackAddLoc = -1;
	private int m_g_ASSAOConsts_HalfViewportPixelSizeLoc = -1;
	private int m_g_ASSAOConsts_PerPassFullResCoordOffsetLoc = -1;
	private int m_g_ASSAOConsts_QuarterResPixelSizeLoc = -1;
	private int m_PatternRotScaleMatrices = -1;


	public RenderTechnique(String shaderFile, Macro[] macros){
		final String shader_path = "Intel/ASSAODemo/shaders/";
		try {
			setSourceFromFiles(shader_path + "Quad_VS.vert", shader_path + shaderFile, macros);
		} catch (IOException e) {
			e.printStackTrace();
		}

		int dot = shaderFile.indexOf('.');
		setName(shaderFile.substring(0, dot));
		
		init();
	}
	
	private void init(){
		// TODO Don't forget initlize the program here.
		int m_program = getProgram();
		m_g_ASSAOConsts_DetailAOStrengthLoc = gl.glGetUniformLocation(m_program, "g_ASSAOConsts.DetailAOStrength");
		m_g_ASSAOConsts_EffectShadowPowLoc = gl.glGetUniformLocation(m_program, "g_ASSAOConsts.EffectShadowPow");
		m_g_ASSAOConsts_LoadCounterAvgDivLoc = gl.glGetUniformLocation(m_program, "g_ASSAOConsts.LoadCounterAvgDiv");
		m_g_ASSAOConsts_EffectShadowStrengthLoc = gl.glGetUniformLocation(m_program, "g_ASSAOConsts.EffectShadowStrength");
		m_g_ASSAOConsts_DepthPrecisionOffsetModLoc = gl.glGetUniformLocation(m_program, "g_ASSAOConsts.DepthPrecisionOffsetMod");
		m_g_ASSAOConsts_Viewport2xPixelSize_x_025Loc = gl.glGetUniformLocation(m_program, "g_ASSAOConsts.Viewport2xPixelSize_x_025");
		m_g_ASSAOConsts_NDCToViewAddLoc = gl.glGetUniformLocation(m_program, "g_ASSAOConsts.NDCToViewAdd");
		m_g_ASSAOConsts_NormalsUnpackMulLoc = gl.glGetUniformLocation(m_program, "g_ASSAOConsts.NormalsUnpackMul");
		m_g_ASSAOConsts_Viewport2xPixelSizeLoc = gl.glGetUniformLocation(m_program, "g_ASSAOConsts.Viewport2xPixelSize");
		m_g_ASSAOConsts_EffectShadowClampLoc = gl.glGetUniformLocation(m_program, "g_ASSAOConsts.EffectShadowClamp");
		m_g_ASSAOConsts_CameraTanHalfFOVLoc = gl.glGetUniformLocation(m_program, "g_ASSAOConsts.CameraTanHalfFOV");
		m_g_ASSAOConsts_EffectFadeOutAddLoc = gl.glGetUniformLocation(m_program, "g_ASSAOConsts.EffectFadeOutAdd");
		m_g_ASSAOConsts_Dummy0Loc = gl.glGetUniformLocation(m_program, "g_ASSAOConsts.Dummy0");
		m_g_ASSAOConsts_PerPassFullResUVOffsetLoc = gl.glGetUniformLocation(m_program, "g_ASSAOConsts.PerPassFullResUVOffset");
		m_g_ASSAOConsts_ViewportPixelSizeLoc = gl.glGetUniformLocation(m_program, "g_ASSAOConsts.ViewportPixelSize");
		m_g_ASSAOConsts_DepthUnpackConstsLoc = gl.glGetUniformLocation(m_program, "g_ASSAOConsts.DepthUnpackConsts");
		m_g_ASSAOConsts_EffectSamplingRadiusNearLimitRecLoc = gl.glGetUniformLocation(m_program, "g_ASSAOConsts.EffectSamplingRadiusNearLimitRec");
		m_g_ASSAOConsts_EffectRadiusLoc = gl.glGetUniformLocation(m_program, "g_ASSAOConsts.EffectRadius");
		m_g_ASSAOConsts_EffectHorizonAngleThresholdLoc = gl.glGetUniformLocation(m_program, "g_ASSAOConsts.EffectHorizonAngleThreshold");
		m_g_ASSAOConsts_NegRecEffectRadiusLoc = gl.glGetUniformLocation(m_program, "g_ASSAOConsts.NegRecEffectRadius");
		m_g_ASSAOConsts_AdaptiveSampleCountLimitLoc = gl.glGetUniformLocation(m_program, "g_ASSAOConsts.AdaptiveSampleCountLimit");
		m_g_ASSAOConsts_PassIndexLoc = gl.glGetUniformLocation(m_program, "g_ASSAOConsts.PassIndex");
		m_g_ASSAOConsts_EffectFadeOutMulLoc = gl.glGetUniformLocation(m_program, "g_ASSAOConsts.EffectFadeOutMul");
		m_g_ASSAOConsts_NDCToViewMulLoc = gl.glGetUniformLocation(m_program, "g_ASSAOConsts.NDCToViewMul");
		m_g_ASSAOConsts_InvSharpnessLoc = gl.glGetUniformLocation(m_program, "g_ASSAOConsts.InvSharpness");
		m_g_ASSAOConsts_NormalsUnpackAddLoc = gl.glGetUniformLocation(m_program, "g_ASSAOConsts.NormalsUnpackAdd");
		m_g_ASSAOConsts_HalfViewportPixelSizeLoc = gl.glGetUniformLocation(m_program, "g_ASSAOConsts.HalfViewportPixelSize");
		m_g_ASSAOConsts_PerPassFullResCoordOffsetLoc = gl.glGetUniformLocation(m_program, "g_ASSAOConsts.PerPassFullResCoordOffset");
		m_g_ASSAOConsts_QuarterResPixelSizeLoc = gl.glGetUniformLocation(m_program, "g_ASSAOConsts.QuarterResPixelSize");
		m_PatternRotScaleMatrices = gl.glGetUniformLocation(m_program, "g_ASSAOConsts.PatternRotScaleMatrices");
	}
	
	public void setUnfiorms(ASSAOConstants data){
		setDetailAOStrength(data.DetailAOStrength);
		setEffectShadowPow(data.EffectShadowPow);
		setLoadCounterAvgDiv(data.LoadCounterAvgDiv);
		setEffectShadowStrength(data.EffectShadowStrength);
		setDepthPrecisionOffsetMod(data.DepthPrecisionOffsetMod);
		setViewport2xPixelSize_x_025(data.Viewport2xPixelSize_x_025);
		setNDCToViewAdd(data.NDCToViewAdd);
		setNormalsUnpackMul(data.NormalsUnpackMul);
		setViewport2xPixelSize(data.Viewport2xPixelSize);
		setEffectShadowClamp(data.EffectShadowClamp);
		setCameraTanHalfFOV(data.CameraTanHalfFOV);
		setEffectFadeOutAdd(data.EffectFadeOutAdd);
		setPerPassFullResUVOffset(data.PerPassFullResUVOffset);
		setViewportPixelSize(data.ViewportPixelSize);
		setDepthUnpackConsts(data.DepthUnpackConsts);
		setEffectSamplingRadiusNearLimitRec(data.EffectSamplingRadiusNearLimitRec);
		setEffectRadius(data.EffectRadius);
		setEffectHorizonAngleThreshold(data.EffectHorizonAngleThreshold);
		setNegRecEffectRadius(data.NegRecEffectRadius);
		setAdaptiveSampleCountLimit(data.AdaptiveSampleCountLimit);
		setPassIndex(data.PassIndex);
		setEffectFadeOutMul(data.EffectFadeOutMul);
		setNDCToViewMul(data.NDCToViewMul);
		setInvSharpness(data.InvSharpness);
		setNormalsUnpackAdd(data.NormalsUnpackAdd);
		setHalfViewportPixelSize(data.HalfViewportPixelSize);
		setPerPassFullResCoordOffset(data.PerPassFullResCoordOffset);
		setQuarterResPixelSize(data.QuarterResPixelSize);
		setPatternRotScaleMatrices(data.PatternRotScaleMatrices);
	}
	
	/*public void printPrograminfo(){
		System.out.println("----------------------------"+m_debugName +"-----------------------------------------" );
		ProgramProperties props = GLSLUtil.getProperties(getProgram());
		System.out.println(props);
	}*/
	
	private void setDetailAOStrength(float f) { if(m_g_ASSAOConsts_DetailAOStrengthLoc >=0)gl.glUniform1f(m_g_ASSAOConsts_DetailAOStrengthLoc, f);}
	private void setEffectShadowPow(float f) { if(m_g_ASSAOConsts_EffectShadowPowLoc >=0)gl.glUniform1f(m_g_ASSAOConsts_EffectShadowPowLoc, f);}
	private void setLoadCounterAvgDiv(float f) { if(m_g_ASSAOConsts_LoadCounterAvgDivLoc >=0)gl.glUniform1f(m_g_ASSAOConsts_LoadCounterAvgDivLoc, f);}
	private void setEffectShadowStrength(float f) { if(m_g_ASSAOConsts_EffectShadowStrengthLoc >=0)gl.glUniform1f(m_g_ASSAOConsts_EffectShadowStrengthLoc, f);}
	private void setDepthPrecisionOffsetMod(float f) { if(m_g_ASSAOConsts_DepthPrecisionOffsetModLoc >=0)gl.glUniform1f(m_g_ASSAOConsts_DepthPrecisionOffsetModLoc, f);}
	private void setViewport2xPixelSize_x_025(Vector2f v) { if(m_g_ASSAOConsts_Viewport2xPixelSize_x_025Loc >=0)gl.glUniform2f(m_g_ASSAOConsts_Viewport2xPixelSize_x_025Loc, v.x, v.y);}
	private void setNDCToViewAdd(Vector2f v) { if(m_g_ASSAOConsts_NDCToViewAddLoc >=0)gl.glUniform2f(m_g_ASSAOConsts_NDCToViewAddLoc, v.x, v.y);}
	private void setNormalsUnpackMul(float f) { if(m_g_ASSAOConsts_NormalsUnpackMulLoc >=0)gl.glUniform1f(m_g_ASSAOConsts_NormalsUnpackMulLoc, f);}
	private void setViewport2xPixelSize(Vector2f v) { if(m_g_ASSAOConsts_Viewport2xPixelSizeLoc >=0)gl.glUniform2f(m_g_ASSAOConsts_Viewport2xPixelSizeLoc, v.x, v.y);}
	private void setEffectShadowClamp(float f) { if(m_g_ASSAOConsts_EffectShadowClampLoc >=0)gl.glUniform1f(m_g_ASSAOConsts_EffectShadowClampLoc, f);}
	private void setCameraTanHalfFOV(Vector2f v) { if(m_g_ASSAOConsts_CameraTanHalfFOVLoc >=0)gl.glUniform2f(m_g_ASSAOConsts_CameraTanHalfFOVLoc, v.x, v.y);}
	private void setEffectFadeOutAdd(float f) { if(m_g_ASSAOConsts_EffectFadeOutAddLoc >=0)gl.glUniform1f(m_g_ASSAOConsts_EffectFadeOutAddLoc, f);}
	private void setPerPassFullResUVOffset(Vector2f v) { if(m_g_ASSAOConsts_PerPassFullResUVOffsetLoc >=0)gl.glUniform2f(m_g_ASSAOConsts_PerPassFullResUVOffsetLoc, v.x, v.y);}
	private void setViewportPixelSize(Vector2f v) { if(m_g_ASSAOConsts_ViewportPixelSizeLoc >=0)gl.glUniform2f(m_g_ASSAOConsts_ViewportPixelSizeLoc, v.x, v.y);}
	private void setDepthUnpackConsts(Vector2f v) { if(m_g_ASSAOConsts_DepthUnpackConstsLoc >=0)gl.glUniform2f(m_g_ASSAOConsts_DepthUnpackConstsLoc, v.x, v.y);}
	private void setEffectSamplingRadiusNearLimitRec(float f) { if(m_g_ASSAOConsts_EffectSamplingRadiusNearLimitRecLoc >=0)gl.glUniform1f(m_g_ASSAOConsts_EffectSamplingRadiusNearLimitRecLoc, f);}
	private void setEffectRadius(float f) { if(m_g_ASSAOConsts_EffectRadiusLoc >=0)gl.glUniform1f(m_g_ASSAOConsts_EffectRadiusLoc, f);}
	private void setEffectHorizonAngleThreshold(float f) { if(m_g_ASSAOConsts_EffectHorizonAngleThresholdLoc >=0)gl.glUniform1f(m_g_ASSAOConsts_EffectHorizonAngleThresholdLoc, f);}
	private void setNegRecEffectRadius(float f) { if(m_g_ASSAOConsts_NegRecEffectRadiusLoc >=0)gl.glUniform1f(m_g_ASSAOConsts_NegRecEffectRadiusLoc, f);}
	private void setAdaptiveSampleCountLimit(float f) { if(m_g_ASSAOConsts_AdaptiveSampleCountLimitLoc >=0)gl.glUniform1f(m_g_ASSAOConsts_AdaptiveSampleCountLimitLoc, f);}
	private void setPassIndex(int i) { if(m_g_ASSAOConsts_PassIndexLoc >=0)gl.glUniform1i(m_g_ASSAOConsts_PassIndexLoc, i);}
	private void setEffectFadeOutMul(float f) { if(m_g_ASSAOConsts_EffectFadeOutMulLoc >=0)gl.glUniform1f(m_g_ASSAOConsts_EffectFadeOutMulLoc, f);}
	private void setNDCToViewMul(Vector2f v) { if(m_g_ASSAOConsts_NDCToViewMulLoc >=0)gl.glUniform2f(m_g_ASSAOConsts_NDCToViewMulLoc, v.x, v.y);}
	private void setInvSharpness(float f) { if(m_g_ASSAOConsts_InvSharpnessLoc >=0)gl.glUniform1f(m_g_ASSAOConsts_InvSharpnessLoc, f);}
	private void setNormalsUnpackAdd(float f) { if(m_g_ASSAOConsts_NormalsUnpackAddLoc >=0)gl.glUniform1f(m_g_ASSAOConsts_NormalsUnpackAddLoc, f);}
	private void setHalfViewportPixelSize(Vector2f v) { if(m_g_ASSAOConsts_HalfViewportPixelSizeLoc >=0)gl.glUniform2f(m_g_ASSAOConsts_HalfViewportPixelSizeLoc, v.x, v.y);}
	private void setPerPassFullResCoordOffset(Vector2i v) { if(m_g_ASSAOConsts_PerPassFullResCoordOffsetLoc >=0)gl.glUniform2i(m_g_ASSAOConsts_PerPassFullResCoordOffsetLoc, v.x, v.y);}
	private void setQuarterResPixelSize(Vector2f v) { if(m_g_ASSAOConsts_QuarterResPixelSizeLoc >=0)gl.glUniform2f(m_g_ASSAOConsts_QuarterResPixelSizeLoc, v.x, v.y);}
	private void setPatternRotScaleMatrices(Vector4f[] mats){
		if(m_PatternRotScaleMatrices >=0){
			gl.glUniform4fv(m_PatternRotScaleMatrices, CacheBuffer.wrap(mats));
		}
	}
}
