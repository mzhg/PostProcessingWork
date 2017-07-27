package jet.opengl.demos.nvidia.waves.samples;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL33;
import org.lwjgl.opengl.GL40;
import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector3f;

import jet.util.buffer.GLUtil;
import jet.util.opengl.shader.libs.SimpleProgram;

public class OceanShadingPorgram extends SimpleProgram{

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
		super(prefix + "ocean_shading.vert", prefix + "ocean_shading.frag");
		
		GL20.glUseProgram(programId);
		mWaterbodyColorLoc = GL20.glGetUniformLocation(programId, "g_WaterbodyColor");
		m_matWorldViewProjLoc = GL20.glGetUniformLocation(programId, "g_matWorldViewProj");
		mSunDirLoc = GL20.glGetUniformLocation(programId, "g_SunDir");
		m_matLocalLoc = GL20.glGetUniformLocation(programId, "g_matLocal");
		m_texPerlinLoc = GL20.glGetUniformLocation(programId, "g_texPerlin");
		GL20.glUniform1i(m_texPerlinLoc, 0);
		mUVScaleLoc = GL20.glGetUniformLocation(programId, "g_UVScale");
		mPerlinAmplitudeLoc = GL20.glGetUniformLocation(programId, "g_PerlinAmplitude");
		mSunColorLoc = GL20.glGetUniformLocation(programId, "g_SunColor");
		mUVOffsetLoc = GL20.glGetUniformLocation(programId, "g_UVOffset");
		mLocalEyeLoc = GL20.glGetUniformLocation(programId, "g_LocalEye");
		mPerlinOctaveLoc = GL20.glGetUniformLocation(programId, "g_PerlinOctave");
		m_texFresnelLoc = GL20.glGetUniformLocation(programId, "g_texFresnel");
		GL20.glUniform1i(m_texFresnelLoc, 3);
		mBendParamLoc = GL20.glGetUniformLocation(programId, "g_BendParam");
		mPerlinSizeLoc = GL20.glGetUniformLocation(programId, "g_PerlinSize");
		mTexelLength_x2Loc = GL20.glGetUniformLocation(programId, "g_TexelLength_x2");
		mPerlinMovementLoc = GL20.glGetUniformLocation(programId, "g_PerlinMovement");
		m_texDisplacementLoc = GL20.glGetUniformLocation(programId, "g_texDisplacement");
		GL20.glUniform1i(m_texDisplacementLoc, 1);
		mSkyColorLoc = GL20.glGetUniformLocation(programId, "g_SkyColor");
		mShinenessLoc = GL20.glGetUniformLocation(programId, "g_Shineness");
		mPerlinGradientLoc = GL20.glGetUniformLocation(programId, "g_PerlinGradient");
		mUVBaseLoc = GL20.glGetUniformLocation(programId, "g_UVBase");
		m_samplerCubeLoc = GL20.glGetUniformLocation(programId, "g_samplerCube");
		GL20.glUniform1i(m_samplerCubeLoc, 4);
		m_texGradientLoc = GL20.glGetUniformLocation(programId, "g_texGradient");
		GL20.glUniform1i(m_texGradientLoc, 2);
		
		mOceanSurfPSIndex = GL40.glGetSubroutineIndex(programId, GL20.GL_FRAGMENT_SHADER, "OceanSurfPS");
		mWireframePSIndex = GL40.glGetSubroutineIndex(programId, GL20.GL_FRAGMENT_SHADER, "WireframePS");
		
		GL20.glUseProgram(0);
	}
	
	public void enableOceanSurfPS() {GL40.glUniformSubroutinesui(GL20.GL_FRAGMENT_SHADER, mOceanSurfPSIndex);}
	public void enableWireframePS() {GL40.glUniformSubroutinesui(GL20.GL_FRAGMENT_SHADER, mWireframePSIndex);}
	
	public void setWaterbodyColor(Vector3f v) { GL20.glUniform3f(mWaterbodyColorLoc, v.x, v.y, v.z);}
	public void setMatWorldViewProj(Matrix4f mat) { GL20.glUniformMatrix4fv(m_matWorldViewProjLoc, false, GLUtil.wrap(mat));}
	public void setSunDir(Vector3f v) { GL20.glUniform3f(mSunDirLoc, v.x, v.y, v.z);}
	public void setMatLocal(Matrix4f mat) { GL20.glUniformMatrix4fv(m_matLocalLoc, false, GLUtil.wrap(mat));}
	public void setTexPerlin(int texture, int sampler) { 
		GL13.glActiveTexture(GL13.GL_TEXTURE0 + 0);
		GL11.glBindTexture(GL11.GL_TEXTURE_2D, texture);
		GL33.glBindSampler(0, sampler);
	}
	public void setUVScale(float f) { GL20.glUniform1f(mUVScaleLoc, f);}
	public void setPerlinAmplitude(Vector3f v) { GL20.glUniform3f(mPerlinAmplitudeLoc, v.x, v.y, v.z);}
	public void setSunColor(Vector3f v) { GL20.glUniform3f(mSunColorLoc, v.x, v.y, v.z);}
	public void setUVOffset(float f) { GL20.glUniform1f(mUVOffsetLoc, f);}
	public void setLocalEye(Vector3f v) { GL20.glUniform3f(mLocalEyeLoc, v.x, v.y, v.z);}
	public void setPerlinOctave(Vector3f v) { GL20.glUniform3f(mPerlinOctaveLoc, v.x, v.y, v.z);}
	public void setTexFresnel(int texture, int sampler) { 
		GL13.glActiveTexture(GL13.GL_TEXTURE0 + 3);
		GL11.glBindTexture(GL11.GL_TEXTURE_1D, texture);
		GL33.glBindSampler(3, sampler);
	}
	public void setBendParam(Vector3f v) { GL20.glUniform3f(mBendParamLoc, v.x, v.y, v.z);}
	public void setPerlinSize(float f) { GL20.glUniform1f(mPerlinSizeLoc, f);}
	public void setTexelLength_x2(float f) { GL20.glUniform1f(mTexelLength_x2Loc, f);}
	public void setPerlinMovement(float x, float y) { GL20.glUniform2f(mPerlinMovementLoc, x, y);}
	public void setTexDisplacement(int texture, int sampler) { 
		GL13.glActiveTexture(GL13.GL_TEXTURE0 + 1);
		GL11.glBindTexture(GL11.GL_TEXTURE_2D, texture);
		GL33.glBindSampler(1, sampler);
	}
	public void setSkyColor(Vector3f v) { GL20.glUniform3f(mSkyColorLoc, v.x, v.y, v.z);}
	public void setShineness(float f) { GL20.glUniform1f(mShinenessLoc, f);}
	public void setPerlinGradient(Vector3f v) { GL20.glUniform3f(mPerlinGradientLoc, v.x, v.y, v.z);}
	public void setUVBase(float x, float y) { GL20.glUniform2f(mUVBaseLoc, x, y);}
	public void setSamplerCube(int texture, int sampler) { 
		GL13.glActiveTexture(GL13.GL_TEXTURE0 + 4);
		GL11.glBindTexture(GL13.GL_TEXTURE_CUBE_MAP, texture);
		GL33.glBindSampler(4, sampler);
	}
	public void setTexGradient(int texture, int sampler) { 
		GL13.glActiveTexture(GL13.GL_TEXTURE0 + 2);
		GL11.glBindTexture(GL11.GL_TEXTURE_2D, texture);
		GL33.glBindSampler(2, sampler);
	}
	
	@Override
	public void disable() {
		GL20.glUseProgram(0);
		
		GL13.glActiveTexture(GL13.GL_TEXTURE4);
		GL11.glBindTexture(GL13.GL_TEXTURE_CUBE_MAP, 0);
		
		GL13.glActiveTexture(GL13.GL_TEXTURE3);
		GL11.glBindTexture(GL11.GL_TEXTURE_1D, 0);
		
		GL13.glActiveTexture(GL13.GL_TEXTURE2);
		GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);
		GL13.glActiveTexture(GL13.GL_TEXTURE1);
		GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);
		GL13.glActiveTexture(GL13.GL_TEXTURE0);
		GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);
		
		for(int i = 0; i < 5; i++)
			GL33.glBindSampler(i, 0);
	}
}
