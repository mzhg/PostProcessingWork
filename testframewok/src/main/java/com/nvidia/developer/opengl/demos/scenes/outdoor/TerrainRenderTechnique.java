package com.nvidia.developer.opengl.demos.scenes.outdoor;

import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector4f;
import jet.opengl.postprocessing.shader.Macro;
import jet.opengl.postprocessing.util.CacheBuffer;

final class TerrainRenderTechnique extends RenderTechnique{
	
	public static final int TEX2D_AMBIENT_SKY_LIGHT = 0;
	public static final int TEX2D_OCCLUDED_NET_DENSITY_TO_ATMTOP = 1;
	public static final int TEX2D_NORMAL_MAP = 2;
	public static final int TEX2D_SHADOWMAP = 3;
	public static final int TEX2D_MTRLMAP = 4;
	public static final int TEX2D_TILE_TEXTURES = 5;
	public static final int TEX2D_TILE_NORMAL_MAPS = 10;
	public static final int TEX2D_ELEVATION_MAP = 11;

//	uniform mat4 g_WorldViewProj;
//	uniform mat4 g_WorldToLightView;
//	uniform vec4 g_f4ExtraterrestrialSunColor;
//	uniform vec4 g_f4LightSpaceScale[NUM_SHADOW_CASCADES];
//	uniform vec4 g_f4LightSpaceScaledBias[NUM_SHADOW_CASCADES];
//	uniform vec4 g_f4CascadeCamSpaceZEnd[NUM_SHADOW_CASCADES];
//	uniform float g_fBaseMtrlTilingScale;
//	uniform vec4 g_f4CascadeColors[NUM_SHADOW_CASCADES];
	
	private int worldViewProjIndex;
	private int worldToLightViewIndex;
	private int lightSpaceScaleIndex;
	private int lightSpaceScaledBiasIndex;
	private int cascadeCamSpaceZEndIndex;
	private int baseMtrlTilingScaleIndex;
	private int cascadeColorsScaleIndex;
	private int farPlaneZIndex;
	private int nearPlaneZIndex;
	
	private static final Vector4f[] tmpVec4 = new Vector4f[4];
	
	public TerrainRenderTechnique(String filename, Macro[] macros, int type) {
		super(filename, macros, type);
	}

	public TerrainRenderTechnique(String filename, Macro[] macros) {
		super(filename, macros);
	}

	public TerrainRenderTechnique(String filename) {
		super(filename);
	}

	@Override
	protected void initUniformIndices() {
		super.initUniformIndices();
		
		worldViewProjIndex = gl.glGetUniformLocation(getProgram(), "g_WorldViewProj");
		worldToLightViewIndex = gl.glGetUniformLocation(getProgram(), "g_WorldToLightView");
		lightSpaceScaleIndex = gl.glGetUniformLocation(getProgram(), "g_f4LightSpaceScale");
		lightSpaceScaledBiasIndex = gl.glGetUniformLocation(getProgram(), "g_f4LightSpaceScaledBias");
		cascadeCamSpaceZEndIndex = gl.glGetUniformLocation(getProgram(), "g_fCascadeCamSpaceZEnd");
		baseMtrlTilingScaleIndex = gl.glGetUniformLocation(getProgram(), "g_fBaseMtrlTilingScale");
		cascadeColorsScaleIndex = gl.glGetUniformLocation(getProgram(), "g_f4CascadeColors");
		
		farPlaneZIndex				  = gl.glGetUniformLocation(getProgram(), "g_fFarPlaneZ");
		nearPlaneZIndex				  = gl.glGetUniformLocation(getProgram(), "g_fNearPlaneZ");
	}
	
	public void setupUniforms(SLightAttribs attribs){
		super.setupUniforms(attribs);
		
		setWorldToLightViewMatrix(attribs.shadowAttribs.mWorldToLightViewT);
		
		tmpVec4[0] = attribs.shadowAttribs.cascades[0].f4LightSpaceScale;
		tmpVec4[1] = attribs.shadowAttribs.cascades[1].f4LightSpaceScale;
		tmpVec4[2] = attribs.shadowAttribs.cascades[2].f4LightSpaceScale;
		tmpVec4[3] = attribs.shadowAttribs.cascades[3].f4LightSpaceScale;
		setLightSpaceScale(tmpVec4);
		
		tmpVec4[0] = attribs.shadowAttribs.cascades[0].f4LightSpaceScaledBias;
		tmpVec4[1] = attribs.shadowAttribs.cascades[1].f4LightSpaceScaledBias;
		tmpVec4[2] = attribs.shadowAttribs.cascades[2].f4LightSpaceScaledBias;
		tmpVec4[3] = attribs.shadowAttribs.cascades[3].f4LightSpaceScaledBias;
		setLightSpaceScaleBias(tmpVec4);
		
		setCascadeCamSpaceZEnd(attribs.shadowAttribs.fCascadeCamSpaceZEnd);
	}
	
	public void setCameraPlane(float far, float near){
		if(farPlaneZIndex >= 0){
			gl.glProgramUniform1f(getProgram(), farPlaneZIndex, far);
		}
		
		if(nearPlaneZIndex >= 0){
			gl.glProgramUniform1f(getProgram(), nearPlaneZIndex, near);
		}
	}
	
	public void setBaseMtrlTilingScale(float scale){
		if(baseMtrlTilingScaleIndex >= 0){
			gl.glProgramUniform1f(getProgram(), baseMtrlTilingScaleIndex, scale);
		}
	}
	
	public void setWorldViewProjMatrix(Matrix4f worldViewProj){
		if(worldViewProjIndex >= 0){
			gl.glProgramUniformMatrix4fv(getProgram(), worldViewProjIndex, false, CacheBuffer.wrap(worldViewProj));
		}
	}
	
	public void setWorldToLightViewMatrix(Matrix4f worldToLightView){
		if(worldToLightViewIndex >= 0){
			gl.glProgramUniformMatrix4fv(getProgram(),worldToLightViewIndex, false, CacheBuffer.wrap(worldToLightView));
		}
	}
	
	public void setLightSpaceScale(Vector4f[] scale){
		if(lightSpaceScaleIndex >= 0){
			gl.glProgramUniform4fv(getProgram(), lightSpaceScaleIndex, CacheBuffer.wrap(scale));
		}
	}
	
	public void setLightSpaceScaleBias(Vector4f[] bias){
		if(lightSpaceScaledBiasIndex >= 0){
			gl.glProgramUniform4fv(getProgram(), lightSpaceScaledBiasIndex, CacheBuffer.wrap(bias));
		}
	}
	
	public void setCascadeCamSpaceZEnd(float[] camSpaceZ){
		if(cascadeCamSpaceZEndIndex >= 0){
			gl.glProgramUniform1fv(getProgram(), cascadeCamSpaceZEndIndex, CacheBuffer.wrap(camSpaceZ));
		}
	}
	
	public void setCascadeColors(Vector4f[] colors){
		if(cascadeColorsScaleIndex >= 0){
			gl.glProgramUniform4fv(getProgram(), cascadeColorsScaleIndex, CacheBuffer.wrap(colors));
		}
	}
}
