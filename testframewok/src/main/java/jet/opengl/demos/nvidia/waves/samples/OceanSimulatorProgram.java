package jet.opengl.demos.nvidia.waves.samples;

import java.io.IOException;

import jet.opengl.postprocessing.common.GLenum;
import jet.opengl.postprocessing.shader.GLSLProgram;

public class OceanSimulatorProgram extends GLSLProgram{

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
		try {
			setSourceFromFiles("nvidia/WaveWorks/shaders/fullscreen.vert", "nvidia/WaveWorks/shaders/ocean_simulator.frag");
		} catch (IOException e) {
			e.printStackTrace();
		}

		initUnifoms();
	}
	
	private void initUnifoms(){
		int programId = getProgram();
		mOutWidthLoc = gl.glGetUniformLocation(programId, "g_OutWidth");
		mActualDimLoc = gl.glGetUniformLocation(programId, "g_ActualDim");
		mDyAddressOffsetLoc = gl.glGetUniformLocation(programId, "g_DyAddressOffset");
		mGridLenLoc = gl.glGetUniformLocation(programId, "g_GridLen");
		mChoppyScaleLoc = gl.glGetUniformLocation(programId, "g_ChoppyScale");
		mDxAddressOffsetLoc = gl.glGetUniformLocation(programId, "g_DxAddressOffset");
		mInWidthLoc = gl.glGetUniformLocation(programId, "g_InWidth");
		mTimeLoc = gl.glGetUniformLocation(programId, "g_Time");
		mOutHeightLoc = gl.glGetUniformLocation(programId, "g_OutHeight");
		
		mUpdateDisplacementPSLoc = gl.glGetSubroutineIndex(programId, GLenum.GL_FRAGMENT_SHADER, "UpdateDisplacementPS");
		mGenGradientFoldingPSLoc = gl.glGetSubroutineIndex(programId, GLenum.GL_FRAGMENT_SHADER, "GenGradientFoldingPS");
		
		System.out.println("UpdateDisplacementPS = " + mUpdateDisplacementPSLoc);
		System.out.println("GenGradientFoldingPS = " + mGenGradientFoldingPSLoc);
	}
	
	public void setOutWidth(int i) {gl.glUniform1i(mOutWidthLoc, i);}
	public void setActualDim(int i) { gl.glUniform1i(mActualDimLoc, i);}
	public void setDyAddressOffset(int i) { gl.glUniform1i(mDyAddressOffsetLoc, i);}
	public void setGridLen(float f) { gl.glUniform1f(mGridLenLoc, f);}
	public void setChoppyScale(float f) { gl.glUniform1f(mChoppyScaleLoc, f);}
	public void setDxAddressOffset(int i) { gl.glUniform1i(mDxAddressOffsetLoc, i);}
	public void setInWidth(int i) { gl.glUniform1i(mInWidthLoc, i);}
	public void setTime(float f) { gl.glUniform1f(mTimeLoc, f);}
	public void setOutHeight(int i) { gl.glUniform1i(mOutHeightLoc, i);}
	public void setSamplerDisplacementMap(int texture) {
		gl.glActiveTexture(GLenum.GL_TEXTURE0 + 0);
		gl.glBindTexture(GLenum.GL_TEXTURE_2D, texture);
	}
	public void enableUpdateDisplacementPS(){ 
//		GLError.checkError();
//		int active = GL40.glGetProgramStagei(programId, GL20.GL_FRAGMENT_SHADER, GL40.GL_ACTIVE_SUBROUTINES);
//		System.out.println("ACTIVE_SUBROUTINES = " + active);
//		System.out.println("mUpdateDisplacementPSLoc = " + mUpdateDisplacementPSLoc);
		gl.glUniformSubroutinesui(GLenum.GL_FRAGMENT_SHADER, mUpdateDisplacementPSLoc);
//		GLError.checkError();
	}
	public void enableGenGradientFoldingPS(){ gl.glUniformSubroutinesui(GLenum.GL_FRAGMENT_SHADER, mGenGradientFoldingPSLoc);}
}
