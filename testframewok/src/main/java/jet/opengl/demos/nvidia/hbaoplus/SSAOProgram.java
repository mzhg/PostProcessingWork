package jet.opengl.demos.nvidia.hbaoplus;

import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.ReadableVector2f;
import org.lwjgl.util.vector.ReadableVector4f;
import org.lwjgl.util.vector.Vector2f;

import java.io.IOException;

import jet.opengl.postprocessing.shader.GLSLProgram;
import jet.opengl.postprocessing.shader.Macro;
import jet.opengl.postprocessing.shader.ShaderLoader;
import jet.opengl.postprocessing.shader.ShaderSourceItem;
import jet.opengl.postprocessing.shader.ShaderType;
import jet.opengl.postprocessing.util.CacheBuffer;

class SSAOProgram extends GLSLProgram{
	private int m_fNDotVBiasLoc = -1;
	private int m_fBlurViewDepth1Loc = -1;
	private int m_f2InvFullResolutionLoc = -1;
	private int m_fBlurViewDepth0Loc = -1;
	private int m_fBackgroundAORadiusPixelsLoc = -1;
	private int m_f2InvQuarterResolutionLoc = -1;
	private int m_fViewDepthThresholdSharpnessLoc = -1;
	private int m_fLinearizeDepthBLoc = -1;
	private int m_fForegroundAORadiusPixelsLoc = -1;
	private int m_f44NormalMatrixLoc = -1;
	private int m_f2UVToViewBLoc = -1;
	private int m_f2UVToViewALoc = -1;
	private int m_fLinearizeDepthALoc = -1;
	private int m_fNormalDecodeScaleLoc = -1;
	private int m_f2InputViewportTopLeftLoc = -1;
	private int m_iDebugNormalComponentLoc = -1;
	private int m_fLargeScaleAOAmountLoc = -1;
	private int m_fInverseDepthRangeALoc = -1;
	private int m_fInverseDepthRangeBLoc = -1;
	private int m_fRadiusToScreenLoc = -1;
	private int m_fBlurSharpness0Loc = -1;
	private int m_fBlurSharpness1Loc = -1;
	private int m_fViewDepthThresholdNegInvLoc = -1;
	private int m_fNormalDecodeBiasLoc = -1;
	private int m_fR2Loc = -1;
	private int m_fPowExponentLoc = -1;
	private int m_fNegInvR2Loc = -1;
	private int m_fSmallScaleAOAmountLoc = -1;
	
	private int m_f4JitterLoc = -1;
	private int m_f2OffsetLoc = -1;
	private int m_fSliceIndexLoc = -1;
	private int m_uSliceIndexLoc = -1;
	
	private void initUniform(){
		m_fNDotVBiasLoc = gl.glGetUniformLocation(m_program, "g_fNDotVBias");
		m_fBlurViewDepth1Loc = gl.glGetUniformLocation(m_program, "g_fBlurViewDepth1");
		m_f2InvFullResolutionLoc = gl.glGetUniformLocation(m_program, "g_f2InvFullResolution");
		m_fBlurViewDepth0Loc = gl.glGetUniformLocation(m_program, "g_fBlurViewDepth0");
		m_fBackgroundAORadiusPixelsLoc = gl.glGetUniformLocation(m_program, "g_fBackgroundAORadiusPixels");
		m_f2InvQuarterResolutionLoc = gl.glGetUniformLocation(m_program, "g_f2InvQuarterResolution");
//		m_iUnusedLoc = GL20.glGetUniformLocation(m_program, "g_iUnused");
		m_fViewDepthThresholdSharpnessLoc = gl.glGetUniformLocation(m_program, "g_fViewDepthThresholdSharpness");
		m_fLinearizeDepthBLoc = gl.glGetUniformLocation(m_program, "g_fLinearizeDepthB");
		m_fForegroundAORadiusPixelsLoc = gl.glGetUniformLocation(m_program, "g_fForegroundAORadiusPixels");
		m_f44NormalMatrixLoc = gl.glGetUniformLocation(m_program, "g_f44NormalMatrix");
		m_f2UVToViewBLoc = gl.glGetUniformLocation(m_program, "g_f2UVToViewB");
		m_f2UVToViewALoc = gl.glGetUniformLocation(m_program, "g_f2UVToViewA");
		m_fLinearizeDepthALoc = gl.glGetUniformLocation(m_program, "g_fLinearizeDepthA");
		m_fNormalDecodeScaleLoc = gl.glGetUniformLocation(m_program, "g_fNormalDecodeScale");
		m_f2InputViewportTopLeftLoc = gl.glGetUniformLocation(m_program, "g_f2InputViewportTopLeft");
//		m_u4BuildVersionLoc = GL20.glGetUniformLocation(m_program, "g_u4BuildVersion");
		m_iDebugNormalComponentLoc = gl.glGetUniformLocation(m_program, "g_iDebugNormalComponent");
		m_fLargeScaleAOAmountLoc = gl.glGetUniformLocation(m_program, "g_fLargeScaleAOAmount");
		m_fInverseDepthRangeALoc = gl.glGetUniformLocation(m_program, "g_fInverseDepthRangeA");
		m_fInverseDepthRangeBLoc = gl.glGetUniformLocation(m_program, "g_fInverseDepthRangeB");
		m_fRadiusToScreenLoc = gl.glGetUniformLocation(m_program, "g_fRadiusToScreen");
		m_fBlurSharpness0Loc = gl.glGetUniformLocation(m_program, "g_fBlurSharpness0");
		m_fBlurSharpness1Loc = gl.glGetUniformLocation(m_program, "g_fBlurSharpness1");
		m_fViewDepthThresholdNegInvLoc = gl.glGetUniformLocation(m_program, "g_fViewDepthThresholdNegInv");
		m_fNormalDecodeBiasLoc = gl.glGetUniformLocation(m_program, "g_fNormalDecodeBias");
		m_fR2Loc = gl.glGetUniformLocation(m_program, "g_fR2");
		m_fPowExponentLoc = gl.glGetUniformLocation(m_program, "g_fPowExponent");
		m_fNegInvR2Loc = gl.glGetUniformLocation(m_program, "g_fNegInvR2");
		m_fSmallScaleAOAmountLoc = gl.glGetUniformLocation(m_program, "g_fSmallScaleAOAmount");
		
		m_f4JitterLoc = gl.glGetUniformLocation(m_program, "g_PerPassConstants.f4Jitter");
		m_f2OffsetLoc = gl.glGetUniformLocation(m_program, "g_PerPassConstants.f2Offset");
		m_fSliceIndexLoc = gl.glGetUniformLocation(m_program, "g_PerPassConstants.fSliceIndex");
		m_uSliceIndexLoc = gl.glGetUniformLocation(m_program, "g_PerPassConstants.uSliceIndex");
	}
	
	public void setUniformData(GlobalConstantStruct data){
		setInvQuarterResolution(data.f2InvQuarterResolution);
		setInvFullResolution(data.f2InvFullResolution);
		
		setUVToViewA(data.f2UVToViewA);
		setUVToViewB(data.f2UVToViewB);
		
		setRadiusToScreen(data.fRadiusToScreen);
		setR2(data.fR2);
		setNegInvR2(data.fNegInvR2);
		setNDotVBias(data.fNDotVBias);
		
		setSmallScaleAOAmount(data.fSmallScaleAOAmount);
		setLargeScaleAOAmount(data.fLargeScaleAOAmount);
		setPowExponent(data.fPowExponent);
		
		setBlurViewDepth0(data.fBlurViewDepth0);
		setBlurViewDepth1(data.fBlurViewDepth1);
		setBlurSharpness0(data.fBlurSharpness0);
		setBlurSharpness1(data.fBlurSharpness1);
		
		setLinearizeDepthA(data.fLinearizeDepthA);
		setLinearizeDepthB(data.fLinearizeDepthB);
		setInverseDepthRangeA(data.fInverseDepthRangeA);
		setInverseDepthRangeB(data.fInverseDepthRangeB);
		
		setInputViewportTopLeft(data.f2InputViewportTopLeft);
		setViewDepthThresholdNegInv(data.fViewDepthThresholdNegInv);
		setViewDepthThresholdSharpness(data.fViewDepthThresholdSharpness);
		
		setBackgroundAORadiusPixels(data.fBackgroundAORadiusPixels);
		setForegroundAORadiusPixels(data.fForegroundAORadiusPixels);
		setDebugNormalComponent(data.iDebugNormalComponent);
		
		setNormalMatrix(data.f44NormalMatrix);
		setNormalDecodeScale(data.fNormalDecodeScale);
		setNormalDecodeBias(data.fNormalDecodeBias);
	}
	
	public void setUniformData(PerPassConstantStruct data){
		setJitter(data.f4Jitter);
		setOffset(data.f2Offset);
		setSliceIndex(data.fSliceIndex);
		setSliceIndex(data.uSliceIndex);
	}
	
	private void setJitter(ReadableVector4f v){
		if(m_f4JitterLoc >= 0){
			gl.glUniform4f(m_f4JitterLoc, v.getX(), v.getY(), v.getZ(), v.getW());
		}
	}
	
	private void setOffset(ReadableVector2f v){
		if(m_f2OffsetLoc >= 0){
			gl.glUniform2f(m_f2OffsetLoc, v.getX(), v.getY());
		}
	}
	
	private void setSliceIndex(float v){
		if(m_fSliceIndexLoc >= 0){
			gl.glUniform1f(m_fSliceIndexLoc, v);
		}
	}
	
	private void setSliceIndex(int v){
		if(m_uSliceIndexLoc >= 0){
			gl.glUniform1ui(m_uSliceIndexLoc, v);
		}
	}
	
	private void setNDotVBias(float f) { if(m_fNDotVBiasLoc >= 0) gl.glUniform1f(m_fNDotVBiasLoc, f);}
	private void setBlurViewDepth1(float f) { if(m_fBlurViewDepth1Loc >= 0)  gl.glUniform1f(m_fBlurViewDepth1Loc, f);}
	private void setInvFullResolution(Vector2f v) { if(m_f2InvFullResolutionLoc >= 0) gl.glUniform2f(m_f2InvFullResolutionLoc, v.x, v.y);}
	private void setBlurViewDepth0(float f) { if(m_fBlurViewDepth0Loc >= 0)gl.glUniform1f(m_fBlurViewDepth0Loc, f);}
	private void setBackgroundAORadiusPixels(float f) { if(m_fBackgroundAORadiusPixelsLoc >= 0) gl.glUniform1f(m_fBackgroundAORadiusPixelsLoc, f);}
	private void setInvQuarterResolution(Vector2f v) { if(m_f2InvQuarterResolutionLoc >= 0) gl.glUniform2f(m_f2InvQuarterResolutionLoc, v.x, v.y);}
//	private void setIUnused(int i) { GL20.glUniform1i(m_iUnusedLoc, i);}
	private void setViewDepthThresholdSharpness(float f) { if(m_fViewDepthThresholdSharpnessLoc >= 0) gl.glUniform1f(m_fViewDepthThresholdSharpnessLoc, f);}
	private void setLinearizeDepthB(float f) { if(m_fLinearizeDepthBLoc >= 0) gl.glUniform1f(m_fLinearizeDepthBLoc, f);}
	private void setForegroundAORadiusPixels(float f) {if(m_fForegroundAORadiusPixelsLoc >= 0) gl.glUniform1f(m_fForegroundAORadiusPixelsLoc, f);}
	private void setNormalMatrix(Matrix4f mat) { if(m_f44NormalMatrixLoc >= 0) gl.glUniformMatrix4fv(m_f44NormalMatrixLoc, false, CacheBuffer.wrap(mat));}
	private void setUVToViewB(Vector2f v) { if(m_f2UVToViewBLoc >= 0) gl.glUniform2f(m_f2UVToViewBLoc, v.x, v.y);}
	private void setUVToViewA(Vector2f v) { if(m_f2UVToViewALoc >= 0) gl.glUniform2f(m_f2UVToViewALoc, v.x, v.y);}
	private void setLinearizeDepthA(float f) { if(m_fLinearizeDepthALoc >= 0) gl.glUniform1f(m_fLinearizeDepthALoc, f);}
	private void setNormalDecodeScale(float f) { if(m_fNormalDecodeScaleLoc >= 0) gl.glUniform1f(m_fNormalDecodeScaleLoc, f);}
	private void setInputViewportTopLeft(Vector2f v) { if(m_f2InputViewportTopLeftLoc >= 0) gl.glUniform2f(m_f2InputViewportTopLeftLoc, v.x, v.y);}
	private void setDebugNormalComponent(int i) { if(m_iDebugNormalComponentLoc >= 0) gl.glUniform1i(m_iDebugNormalComponentLoc, i);}
	private void setLargeScaleAOAmount(float f) { if(m_fLargeScaleAOAmountLoc >= 0) gl.glUniform1f(m_fLargeScaleAOAmountLoc, f);}
	private void setInverseDepthRangeA(float f) { if(m_fInverseDepthRangeALoc >= 0) gl.glUniform1f(m_fInverseDepthRangeALoc, f);}
	private void setInverseDepthRangeB(float f) { if(m_fInverseDepthRangeBLoc >= 0) gl.glUniform1f(m_fInverseDepthRangeBLoc, f);}
	private void setRadiusToScreen(float f) { if(m_fRadiusToScreenLoc >= 0) gl.glUniform1f(m_fRadiusToScreenLoc, f);}
	private void setBlurSharpness0(float f) { if(m_fBlurSharpness0Loc >= 0) gl.glUniform1f(m_fBlurSharpness0Loc, f);}
	private void setBlurSharpness1(float f) { if(m_fBlurSharpness1Loc >= 0) gl.glUniform1f(m_fBlurSharpness1Loc, f);}
	private void setViewDepthThresholdNegInv(float f) { if(m_fViewDepthThresholdNegInvLoc >= 0) gl.glUniform1f(m_fViewDepthThresholdNegInvLoc, f);}
	private void setNormalDecodeBias(float f) { if(m_fNormalDecodeBiasLoc >= 0) gl.glUniform1f(m_fNormalDecodeBiasLoc, f);}
	private void setR2(float f) { if(m_fR2Loc >= 0) gl.glUniform1f(m_fR2Loc, f);}
	private void setPowExponent(float f) { if(m_fPowExponentLoc >= 0) gl.glUniform1f(m_fPowExponentLoc, f);}
	private void setNegInvR2(float f) { if(m_fNegInvR2Loc >= 0) gl.glUniform1f(m_fNegInvR2Loc, f);}
	private void setSmallScaleAOAmount(float f) { if(m_fSmallScaleAOAmountLoc >= 0) gl.glUniform1f(m_fSmallScaleAOAmountLoc, f);}
	
	public void compile(String vertfile, String geomFile, String fragfile, Macro... macros) throws IOException{
		ShaderSourceItem vs_item = null;
		ShaderSourceItem gs_item = null;
		ShaderSourceItem ps_item = null;

		final String shader_path = "nvidia/HBAOPlush/";
		if(vertfile !=null){
			vs_item = new ShaderSourceItem(ShaderLoader.loadShaderFile(shader_path + vertfile, false), ShaderType.VERTEX);
		}

		if(geomFile != null){
			gs_item = new ShaderSourceItem(ShaderLoader.loadShaderFile(shader_path + geomFile, false), ShaderType.GEOMETRY);
		}

		if(fragfile != null){
			ps_item = new ShaderSourceItem(ShaderLoader.loadShaderFile(shader_path + fragfile, false), ShaderType.FRAGMENT);
		}

		setSourceFromStrings(vs_item, gs_item, ps_item);
		initUniform();
	}
}
