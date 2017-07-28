package jet.opengl.demos.nvidia.waves.samples;

import java.io.IOException;

import jet.opengl.postprocessing.shader.GLSLProgram;
import jet.opengl.postprocessing.shader.ShaderLoader;
import jet.opengl.postprocessing.shader.ShaderSourceItem;
import jet.opengl.postprocessing.shader.ShaderType;

public class OceanSimulatorComputeProgram extends GLSLProgram{

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
		setSourceFromStrings(new ShaderSourceItem[]{new ShaderSourceItem(source, ShaderType.COMPUTE)});
		int programId = getProgram();
		
		mOutWidthLoc = gl.glGetUniformLocation(programId, "g_OutWidth");
		mActualDimLoc = gl.glGetUniformLocation(programId, "g_ActualDim");
		mChoppyScaleLoc = gl.glGetUniformLocation(programId, "g_ChoppyScale");
		mInWidthLoc = gl.glGetUniformLocation(programId, "g_InWidth");
		mTimeLoc = gl.glGetUniformLocation(programId, "g_Time");
		mDtxAddressOffsetLoc = gl.glGetUniformLocation(programId, "g_DtxAddressOffset");
		mOutHeightLoc = gl.glGetUniformLocation(programId, "g_OutHeight");
		mDtyAddressOffsetLoc = gl.glGetUniformLocation(programId, "g_DtyAddressOffset");
	}
	
	public void setOutWidth(int i) { if(mOutWidthLoc >=0) gl.glUniform1i(mOutWidthLoc, i);}
	public void setActualDim(int i) { if(mActualDimLoc >=0) gl.glUniform1i(mActualDimLoc, i);}
	public void setChoppyScale(float f) { if(mChoppyScaleLoc >=0) gl.glUniform1f(mChoppyScaleLoc, f);}
	public void setInWidth(int i) { if(mInWidthLoc >=0) gl.glUniform1i(mInWidthLoc, i);}
	public void setTime(float f) { if(mTimeLoc >=0) gl.glUniform1f(mTimeLoc, f);}
	public void setDtxAddressOffset(int i) { if(mDtxAddressOffsetLoc >=0) gl.glUniform1i(mDtxAddressOffsetLoc, i);}
	public void setOutHeight(int i) { if(mOutHeightLoc >=0) gl.glUniform1i(mOutHeightLoc, i);}
	public void setDtyAddressOffset(int i) { if(mDtyAddressOffsetLoc >=0) gl.glUniform1i(mDtyAddressOffsetLoc, i);}
}
