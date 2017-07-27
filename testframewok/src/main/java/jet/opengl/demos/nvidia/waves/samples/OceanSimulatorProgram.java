package jet.opengl.demos.nvidia.waves.samples;

import java.io.IOException;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL40;

import jet.util.opengl.shader.libs.FullscreenProgram;
import jet.util.opengl.shader.loader.ShaderLoader;

public class OceanSimulatorProgram extends FullscreenProgram{

	private int mOutWidthLoc = -1;
	private int mActualDimLoc = -1;
	private int mDyAddressOffsetLoc = -1;
	private int mGridLenLoc = -1;
	private int mChoppyScaleLoc = -1;
	private int mDxAddressOffsetLoc = -1;
	private int mInWidthLoc = -1;
	private int mTimeLoc = -1;
	private int mOutHeightLoc = -1;
	private int mUpdateDisplacementPSLoc = -1;
	private int mGenGradientFoldingPSLoc = -1;
	
	public OceanSimulatorProgram() {
		initUnifoms();
	}
	
	@Override
	protected CharSequence loadFragShaderSource() throws IOException {
		return ShaderLoader.loadShaderFile("advance/OceanCSDemo/shaders/ocean_simulator.frag", false);
	}
	
	private void initUnifoms(){
		mOutWidthLoc = GL20.glGetUniformLocation(programId, "g_OutWidth");
		mActualDimLoc = GL20.glGetUniformLocation(programId, "g_ActualDim");
		mDyAddressOffsetLoc = GL20.glGetUniformLocation(programId, "g_DyAddressOffset");
		mGridLenLoc = GL20.glGetUniformLocation(programId, "g_GridLen");
		mChoppyScaleLoc = GL20.glGetUniformLocation(programId, "g_ChoppyScale");
		mDxAddressOffsetLoc = GL20.glGetUniformLocation(programId, "g_DxAddressOffset");
		mInWidthLoc = GL20.glGetUniformLocation(programId, "g_InWidth");
		mTimeLoc = GL20.glGetUniformLocation(programId, "g_Time");
		mOutHeightLoc = GL20.glGetUniformLocation(programId, "g_OutHeight");
		
		mUpdateDisplacementPSLoc = GL40.glGetSubroutineIndex(programId, GL20.GL_FRAGMENT_SHADER, "UpdateDisplacementPS");
		mGenGradientFoldingPSLoc = GL40.glGetSubroutineIndex(programId, GL20.GL_FRAGMENT_SHADER, "GenGradientFoldingPS");
		
		System.out.println("UpdateDisplacementPS = " + mUpdateDisplacementPSLoc);
		System.out.println("GenGradientFoldingPS = " + mGenGradientFoldingPSLoc);
	}
	
	@Override
	protected void setupProgramDefaultValues() {
		int m_samplerDisplacementMapLoc = GL20.glGetUniformLocation(programId, "g_samplerDisplacementMap");
		GL20.glUniform1i(m_samplerDisplacementMapLoc, 0);
	}
	
	public void setOutWidth(int i) {GL20.glUniform1i(mOutWidthLoc, i);}
	public void setActualDim(int i) { GL20.glUniform1i(mActualDimLoc, i);}
	public void setDyAddressOffset(int i) { GL20.glUniform1i(mDyAddressOffsetLoc, i);}
	public void setGridLen(float f) { GL20.glUniform1f(mGridLenLoc, f);}
	public void setChoppyScale(float f) { GL20.glUniform1f(mChoppyScaleLoc, f);}
	public void setDxAddressOffset(int i) { GL20.glUniform1i(mDxAddressOffsetLoc, i);}
	public void setInWidth(int i) { GL20.glUniform1i(mInWidthLoc, i);}
	public void setTime(float f) { GL20.glUniform1f(mTimeLoc, f);}
	public void setOutHeight(int i) { GL20.glUniform1i(mOutHeightLoc, i);}
	public void setSamplerDisplacementMap(int texture) { 
		GL13.glActiveTexture(GL13.GL_TEXTURE0 + 0);
		GL11.glBindTexture(GL11.GL_TEXTURE_2D, texture);
	}
	public void enableUpdateDisplacementPS(){ 
//		GLError.checkError();
//		int active = GL40.glGetProgramStagei(programId, GL20.GL_FRAGMENT_SHADER, GL40.GL_ACTIVE_SUBROUTINES);
//		System.out.println("ACTIVE_SUBROUTINES = " + active);
//		System.out.println("mUpdateDisplacementPSLoc = " + mUpdateDisplacementPSLoc);
		GL40.glUniformSubroutinesui(GL20.GL_FRAGMENT_SHADER, mUpdateDisplacementPSLoc);
//		GLError.checkError();
	}
	public void enableGenGradientFoldingPS(){ GL40.glUniformSubroutinesui(GL20.GL_FRAGMENT_SHADER, mGenGradientFoldingPSLoc);}
}
