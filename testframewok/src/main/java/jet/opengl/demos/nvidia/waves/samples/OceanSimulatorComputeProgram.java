package jet.opengl.demos.nvidia.waves.samples;

import java.io.IOException;

import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL43;

import jet.util.opengl.shader.GLSLProgram;
import jet.util.opengl.shader.libs.SimpleProgram;
import jet.util.opengl.shader.loader.ShaderLoader;

public class OceanSimulatorComputeProgram extends SimpleProgram{

	private int mOutWidthLoc = -1;
	private int mActualDimLoc = -1;
	private int mChoppyScaleLoc = -1;
	private int mInWidthLoc = -1;
	private int mTimeLoc = -1;
	private int mDtxAddressOffsetLoc = -1;
	private int mOutHeightLoc = -1;
	private int mDtyAddressOffsetLoc = -1;
	
	public OceanSimulatorComputeProgram(String prefix) {
		CharSequence source = null;
		
		try {
			source = ShaderLoader.loadShaderFile(prefix + "ocean_simulator.glcs", false);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		GLSLProgram program = new GLSLProgram();
		program.setSourceFromStrings(new GLSLProgram.ShaderSourceItem[]{new GLSLProgram.ShaderSourceItem(source, GL43.GL_COMPUTE_SHADER)});
		programId = program.getProgram();
		
		mOutWidthLoc = GL20.glGetUniformLocation(programId, "g_OutWidth");
		mActualDimLoc = GL20.glGetUniformLocation(programId, "g_ActualDim");
		mChoppyScaleLoc = GL20.glGetUniformLocation(programId, "g_ChoppyScale");
		mInWidthLoc = GL20.glGetUniformLocation(programId, "g_InWidth");
		mTimeLoc = GL20.glGetUniformLocation(programId, "g_Time");
		mDtxAddressOffsetLoc = GL20.glGetUniformLocation(programId, "g_DtxAddressOffset");
		mOutHeightLoc = GL20.glGetUniformLocation(programId, "g_OutHeight");
		mDtyAddressOffsetLoc = GL20.glGetUniformLocation(programId, "g_DtyAddressOffset");
	}
	
	public void setOutWidth(int i) { if(mOutWidthLoc >=0) GL20.glUniform1i(mOutWidthLoc, i);}
	public void setActualDim(int i) { if(mActualDimLoc >=0) GL20.glUniform1i(mActualDimLoc, i);}
	public void setChoppyScale(float f) { if(mChoppyScaleLoc >=0) GL20.glUniform1f(mChoppyScaleLoc, f);}
	public void setInWidth(int i) { if(mInWidthLoc >=0) GL20.glUniform1i(mInWidthLoc, i);}
	public void setTime(float f) { if(mTimeLoc >=0) GL20.glUniform1f(mTimeLoc, f);}
	public void setDtxAddressOffset(int i) { if(mDtxAddressOffsetLoc >=0) GL20.glUniform1i(mDtxAddressOffsetLoc, i);}
	public void setOutHeight(int i) { if(mOutHeightLoc >=0) GL20.glUniform1i(mOutHeightLoc, i);}
	public void setDtyAddressOffset(int i) { if(mDtyAddressOffsetLoc >=0) GL20.glUniform1i(mDtyAddressOffsetLoc, i);}
}
