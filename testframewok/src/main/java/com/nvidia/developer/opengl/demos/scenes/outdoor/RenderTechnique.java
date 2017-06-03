package com.nvidia.developer.opengl.demos.scenes.outdoor;

import org.lwjgl.util.vector.ReadableVector2f;
import org.lwjgl.util.vector.ReadableVector3f;
import org.lwjgl.util.vector.ReadableVector4f;

import java.io.IOException;

import jet.opengl.postprocessing.common.GLCheck;
import jet.opengl.postprocessing.common.GLFuncProvider;
import jet.opengl.postprocessing.common.GLFuncProviderFactory;
import jet.opengl.postprocessing.common.GLenum;
import jet.opengl.postprocessing.shader.GLSLProgram;
import jet.opengl.postprocessing.shader.Macro;
import jet.opengl.postprocessing.shader.ShaderLoader;
import jet.opengl.postprocessing.shader.ShaderProgram;
import jet.opengl.postprocessing.shader.ShaderSourceItem;
import jet.opengl.postprocessing.shader.ShaderType;

abstract class RenderTechnique extends ShaderProgram {

//	Texture2D<float>  g_tex2DDepthBuffer            : register( t0 );
//	Texture2D<float>  g_tex2DCamSpaceZ              : register( t0 );
//	Texture2D<float4> g_tex2DSliceEndPoints         : register( t4 );
//	Texture2D<float2> g_tex2DCoordinates            : register( t1 );
//	Texture2D<float>  g_tex2DEpipolarCamSpaceZ      : register( t2 );
//	Texture2D<uint2>  g_tex2DInterpolationSource    : register( t6 );
//	Texture2DArray<float> g_tex2DLightSpaceDepthMap : register( t3 );
//	Texture2D<float4> g_tex2DSliceUVDirAndOrigin    : register( t6 );
//	Texture2D<MIN_MAX_DATA_FORMAT> g_tex2DMinMaxLightSpaceDepth  : register( t4 );
//	Texture2D<float3> g_tex2DInitialInsctrIrradiance: register( t5 );
//	Texture2D<float4> g_tex2DColorBuffer            : register( t1 );
//	Texture2D<float3> g_tex2DScatteredColor         : register( t3 );
//	Texture2D<float2> g_tex2DOccludedNetDensityToAtmTop : register( t5 );
//	Texture2D<float3> g_tex2DEpipolarExtinction     : register( t6 );
//	Texture3D<float3> g_tex3DSingleSctrLUT          : register( t7 );
//	Texture3D<float3> g_tex3DHighOrderSctrLUT       : register( t8 );
//	Texture3D<float3> g_tex3DMultipleSctrLUT        : register( t9 );
//	Texture2D<float3> g_tex2DSphereRandomSampling   : register( t1 );
//	Texture3D<float3> g_tex3DPreviousSctrOrder      : register( t0 );
//	Texture3D<float3> g_tex3DPointwiseSctrRadiance  : register( t0 );
//	Texture2D<float>  g_tex2DAverageLuminance       : register( t10 );
//	Texture2D<float>  g_tex2DLowResLuminance        : register( t0 );
	
	private String debugName;
	
	private int earthRadiusIndex;
	private int dirOnLightIndex;
	private int atmTopHeightIndex;
	private int particleScaleHeightIndex;
	private int atmToRadiusIndex;
	
	private int rayleighExtinctionCoeffIndex;
	private int mieExtinctionCoeffIndex;
	private int extraterrestrialSunColorIndex;
	GLFuncProvider gl;
	
	public RenderTechnique(String filename){
		this(filename, null, GLenum.GL_FRAGMENT_SHADER);
	}
	
	public RenderTechnique(String filename, Macro[] macros){
		this(filename, macros, GLenum.GL_FRAGMENT_SHADER);
	}
	
	public RenderTechnique(String filename, Macro[] macros, int type) {
		gl = GLFuncProviderFactory.getGLFuncProvider();
		compile(filename, type, macros);
	}
	
	final void compile(String filename, int type, Macro... macros){
		ShaderSourceItem item = new ShaderSourceItem();
		try {
			item.source = ShaderLoader.loadShaderFile(filename, false);
		} catch (IOException e) {
			e.printStackTrace();
		}
		item.type = ShaderType.wrap(type);
		item.macros = macros;
//		item.compileVersion = Integer.parseInt(GLSLUtil.getGLSLVersion());
		
		if(debugName == null)
			debugName = getClass().getSimpleName();
		GLSLProgram.createFromString(item, this, debugName);

		initUniformIndices();
		GLCheck.checkError(debugName);
	}
	
	public void printPrograminfo(){
//		System.out.println("----------------------------"+debugName +"-----------------------------------------" );
//		ProgramProperties props = GLSLUtil.getProperties(getProgram());
//		System.out.println(props);
	}
	
	public void setupUniforms(SAirScatteringAttribs attribs){
		setEarthRadius(attribs.fEarthRadius);
		setAtmTopHeight(attribs.fAtmTopHeight);
		setAtmToRadius(attribs.fAtmTopRadius);
		setParticleScaleHeight(attribs.f2ParticleScaleHeight);
		
		setRayleighExtinctionCoeff(attribs.f4RayleighExtinctionCoeff);
		setMieExtinctionCoeff(attribs.f4MieExtinctionCoeff);
	}
	
	public void setupUniforms(SLightAttribs attribs){
		setDirOnLight(attribs.f4DirOnLight);
		setExtraterrestrialSunColor(attribs.f4ExtraterrestrialSunColor);
	}
	
	public String getDebugName() { return debugName;}
	public void   setDebugName(String name) { debugName = name;}
	
	protected void initUniformIndices(){
		earthRadiusIndex = gl.glGetUniformLocation(getProgram(), "g_fEarthRadius");
		dirOnLightIndex = gl.glGetUniformLocation(getProgram(), "g_f4DirOnLight");
		atmTopHeightIndex = gl.glGetUniformLocation(getProgram(), "g_fAtmTopHeight");
		particleScaleHeightIndex = gl.glGetUniformLocation(getProgram(), "g_f2ParticleScaleHeight");
		atmToRadiusIndex = gl.glGetUniformLocation(getProgram(), "g_fAtmTopRadius");
		
		rayleighExtinctionCoeffIndex = gl.glGetUniformLocation(getProgram(), "g_f4RayleighExtinctionCoeff");
		mieExtinctionCoeffIndex = gl.glGetUniformLocation(getProgram(), "g_f4MieExtinctionCoeff");
		extraterrestrialSunColorIndex = gl.glGetUniformLocation(getProgram(), "g_f4ExtraterrestrialSunColor");
	}
	
	public void setEarthRadius(float radius) {
		if(earthRadiusIndex >= 0){
			gl.glProgramUniform1f(getProgram(), earthRadiusIndex, radius);
		}
	}
	
	public void setAtmToRadius(float radius) {
		if(atmToRadiusIndex >= 0){
			gl.glProgramUniform1f(getProgram(), atmToRadiusIndex, radius);
		}
	}
	
	public void setParticleScaleHeight(ReadableVector2f scaleHeight) {
		if(particleScaleHeightIndex >= 0){
			gl.glProgramUniform2f(getProgram(), particleScaleHeightIndex, scaleHeight.getX(), scaleHeight.getY());
		}
	}
	
	public void setDirOnLight(ReadableVector3f dir){
		if(dirOnLightIndex >= 0){
			gl.glProgramUniform4f(getProgram(), dirOnLightIndex, dir.getX(), dir.getY(), dir.getZ(), 0);
		}
	}
	
	public void setDirOnLight(float x, float y, float z){
		if(dirOnLightIndex >= 0){
			gl.glProgramUniform4f(getProgram(), dirOnLightIndex, x, y, z, 0);
		}
	}
	
	public void setAtmTopHeight(float height){
		if(atmTopHeightIndex >= 0){
			gl.glProgramUniform1f(getProgram(), atmTopHeightIndex, height);
		}
	}
	
	public void setRayleighExtinctionCoeff(ReadableVector4f coeff){
		if(rayleighExtinctionCoeffIndex >= 0){
			gl.glProgramUniform4f(getProgram(), rayleighExtinctionCoeffIndex, coeff.getX(), coeff.getY(), coeff.getZ(), coeff.getW());
		}
	}
	
	public void setMieExtinctionCoeff(ReadableVector4f coeff){
		if(mieExtinctionCoeffIndex >= 0){
			gl.glProgramUniform4f(getProgram(), mieExtinctionCoeffIndex, coeff.getX(), coeff.getY(), coeff.getZ(), coeff.getW());
		}
	}
	
	private void setExtraterrestrialSunColor(ReadableVector3f rgb){
		if(extraterrestrialSunColorIndex >= 0){
			gl.glProgramUniform4f(getProgram(), extraterrestrialSunColorIndex, rgb.getX(), rgb.getY(), rgb.getZ(), 1);
		}
	}
	
	static CharSequence loadShader(String filename){
		try {
			return ShaderLoader.loadShaderFile(filename, false);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return "";
	}
}
