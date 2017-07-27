package jet.opengl.demos.nvidia.waves.samples;

import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector3f;

import java.io.IOException;

import jet.opengl.postprocessing.common.GLenum;
import jet.opengl.postprocessing.shader.GLSLProgram;
import jet.opengl.postprocessing.util.CacheBuffer;

final class OceanShadingPorgram extends GLSLProgram{

	private int mWaterbodyColorLoc = -1;
	private int m_matWorldViewProjLoc = -1;
	private int mSunDirLoc = -1;
	private int m_matLocalLoc = -1;
	private int m_texPerlinLoc = -1;
	private int mUVScaleLoc = -1;
	private int mPerlinAmplitudeLoc = -1;
	private int mSunColorLoc = -1;
	private int mUVOffsetLoc = -1;
	private int mLocalEyeLoc = -1;
	private int mPerlinOctaveLoc = -1;
	private int m_texFresnelLoc = -1;
	private int mBendParamLoc = -1;
	private int mPerlinSizeLoc = -1;
	private int mTexelLength_x2Loc = -1;
	private int mPerlinMovementLoc = -1;
	private int m_texDisplacementLoc = -1;
	private int mSkyColorLoc = -1;
	private int mShinenessLoc = -1;
	private int mPerlinGradientLoc = -1;
	private int mUVBaseLoc = -1;
	private int m_samplerCubeLoc = -1;
	private int m_texGradientLoc = -1;
	
	private int mOceanSurfPSIndex = -1;
	private int mWireframePSIndex = -1;
	
	public OceanShadingPorgram(String prefix) {
//		super(prefix + "ocean_shading.vert", prefix + "ocean_shading.frag");
		try {
			setSourceFromFiles("nvidia/WaveWorks/shaders/ocean_shading.vert", "nvidia/WaveWorks/shaders/ocean_shading.frag");
		} catch (IOException e) {
			e.printStackTrace();
		}

		int programId=getProgram();
		gl.glUseProgram(programId);
		mWaterbodyColorLoc = gl.glGetUniformLocation(programId, "g_WaterbodyColor");
		m_matWorldViewProjLoc = gl.glGetUniformLocation(programId, "g_matWorldViewProj");
		mSunDirLoc = gl.glGetUniformLocation(programId, "g_SunDir");
		m_matLocalLoc = gl.glGetUniformLocation(programId, "g_matLocal");
		m_texPerlinLoc = gl.glGetUniformLocation(programId, "g_texPerlin");
		gl.glUniform1i(m_texPerlinLoc, 0);
		mUVScaleLoc = gl.glGetUniformLocation(programId, "g_UVScale");
		mPerlinAmplitudeLoc = gl.glGetUniformLocation(programId, "g_PerlinAmplitude");
		mSunColorLoc = gl.glGetUniformLocation(programId, "g_SunColor");
		mUVOffsetLoc = gl.glGetUniformLocation(programId, "g_UVOffset");
		mLocalEyeLoc = gl.glGetUniformLocation(programId, "g_LocalEye");
		mPerlinOctaveLoc = gl.glGetUniformLocation(programId, "g_PerlinOctave");
		m_texFresnelLoc = gl.glGetUniformLocation(programId, "g_texFresnel");
		gl.glUniform1i(m_texFresnelLoc, 3);
		mBendParamLoc = gl.glGetUniformLocation(programId, "g_BendParam");
		mPerlinSizeLoc = gl.glGetUniformLocation(programId, "g_PerlinSize");
		mTexelLength_x2Loc = gl.glGetUniformLocation(programId, "g_TexelLength_x2");
		mPerlinMovementLoc = gl.glGetUniformLocation(programId, "g_PerlinMovement");
		m_texDisplacementLoc = gl.glGetUniformLocation(programId, "g_texDisplacement");
		gl.glUniform1i(m_texDisplacementLoc, 1);
		mSkyColorLoc = gl.glGetUniformLocation(programId, "g_SkyColor");
		mShinenessLoc = gl.glGetUniformLocation(programId, "g_Shineness");
		mPerlinGradientLoc = gl.glGetUniformLocation(programId, "g_PerlinGradient");
		mUVBaseLoc = gl.glGetUniformLocation(programId, "g_UVBase");
		m_samplerCubeLoc = gl.glGetUniformLocation(programId, "g_samplerCube");
		gl.glUniform1i(m_samplerCubeLoc, 4);
		m_texGradientLoc = gl.glGetUniformLocation(programId, "g_texGradient");
		gl.glUniform1i(m_texGradientLoc, 2);
		
		mOceanSurfPSIndex = gl.glGetSubroutineIndex(programId, GLenum.GL_FRAGMENT_SHADER, "OceanSurfPS");
		mWireframePSIndex = gl.glGetSubroutineIndex(programId, GLenum.GL_FRAGMENT_SHADER, "WireframePS");

		gl.glUseProgram(0);
	}
	
	public void enableOceanSurfPS() {gl.glUniformSubroutinesui(GLenum.GL_FRAGMENT_SHADER, mOceanSurfPSIndex);}
	public void enableWireframePS() {gl.glUniformSubroutinesui(GLenum.GL_FRAGMENT_SHADER, mWireframePSIndex);}
	
	public void setWaterbodyColor(Vector3f v) { gl.glUniform3f(mWaterbodyColorLoc, v.x, v.y, v.z);}
	public void setMatWorldViewProj(Matrix4f mat) { gl.glUniformMatrix4fv(m_matWorldViewProjLoc, false, CacheBuffer.wrap(mat));}
	public void setSunDir(Vector3f v) { gl.glUniform3f(mSunDirLoc, v.x, v.y, v.z);}
	public void setMatLocal(Matrix4f mat) { gl.glUniformMatrix4fv(m_matLocalLoc, false, CacheBuffer.wrap(mat));}
	public void setTexPerlin(int texture, int sampler) {
		gl.glActiveTexture(GLenum.GL_TEXTURE0 + 0);
		gl.glBindTexture(GLenum.GL_TEXTURE_2D, texture);
		gl.glBindSampler(0, sampler);
	}
	public void setUVScale(float f) { gl.glUniform1f(mUVScaleLoc, f);}
	public void setPerlinAmplitude(Vector3f v) { gl.glUniform3f(mPerlinAmplitudeLoc, v.x, v.y, v.z);}
	public void setSunColor(Vector3f v) { gl.glUniform3f(mSunColorLoc, v.x, v.y, v.z);}
	public void setUVOffset(float f) { gl.glUniform1f(mUVOffsetLoc, f);}
	public void setLocalEye(Vector3f v) { gl.glUniform3f(mLocalEyeLoc, v.x, v.y, v.z);}
	public void setPerlinOctave(Vector3f v) { gl.glUniform3f(mPerlinOctaveLoc, v.x, v.y, v.z);}
	public void setTexFresnel(int texture, int sampler) {
		gl.glActiveTexture(GLenum.GL_TEXTURE0 + 3);
		gl.glBindTexture(GLenum.GL_TEXTURE_1D, texture);
		gl.glBindSampler(3, sampler);
	}
	public void setBendParam(Vector3f v) { gl.glUniform3f(mBendParamLoc, v.x, v.y, v.z);}
	public void setPerlinSize(float f) { gl.glUniform1f(mPerlinSizeLoc, f);}
	public void setTexelLength_x2(float f) { gl.glUniform1f(mTexelLength_x2Loc, f);}
	public void setPerlinMovement(float x, float y) { gl.glUniform2f(mPerlinMovementLoc, x, y);}
	public void setTexDisplacement(int texture, int sampler) {
		gl.glActiveTexture(GLenum.GL_TEXTURE0 + 1);
		gl.glBindTexture(GLenum.GL_TEXTURE_2D, texture);
		gl.glBindSampler(1, sampler);
	}
	public void setSkyColor(Vector3f v) { gl.glUniform3f(mSkyColorLoc, v.x, v.y, v.z);}
	public void setShineness(float f) { gl.glUniform1f(mShinenessLoc, f);}
	public void setPerlinGradient(Vector3f v) { gl.glUniform3f(mPerlinGradientLoc, v.x, v.y, v.z);}
	public void setUVBase(float x, float y) { gl.glUniform2f(mUVBaseLoc, x, y);}
	public void setSamplerCube(int texture, int sampler) {
		gl.glActiveTexture(GLenum.GL_TEXTURE0 + 4);
		gl.glBindTexture(GLenum.GL_TEXTURE_CUBE_MAP, texture);
		gl.glBindSampler(4, sampler);
	}
	public void setTexGradient(int texture, int sampler) {
		gl.glActiveTexture(GLenum.GL_TEXTURE0 + 2);
		gl.glBindTexture(GLenum.GL_TEXTURE_2D, texture);
		gl.glBindSampler(2, sampler);
	}
	
	@Override
	public void disable() {
		gl.glUseProgram(0);

		gl.glActiveTexture(GLenum.GL_TEXTURE4);
		gl.glBindTexture(GLenum.GL_TEXTURE_CUBE_MAP, 0);

		gl.glActiveTexture(GLenum.GL_TEXTURE3);
		gl.glBindTexture(GLenum.GL_TEXTURE_1D, 0);

		gl.glActiveTexture(GLenum.GL_TEXTURE2);
		gl.glBindTexture(GLenum.GL_TEXTURE_2D, 0);
		gl.glActiveTexture(GLenum.GL_TEXTURE1);
		gl.glBindTexture(GLenum.GL_TEXTURE_2D, 0);
		gl.glActiveTexture(GLenum.GL_TEXTURE0);
		gl.glBindTexture(GLenum.GL_TEXTURE_2D, 0);
		
		for(int i = 0; i < 5; i++)
			gl.glBindSampler(i, 0);
	}
}
