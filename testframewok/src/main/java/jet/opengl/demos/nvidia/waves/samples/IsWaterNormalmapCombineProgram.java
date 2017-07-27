package jet.opengl.demos.nvidia.waves.samples;

import org.lwjgl.opengl.GL20;
import org.lwjgl.util.vector.Vector2f;
import org.lwjgl.util.vector.Vector3f;

import jet.util.opengl.shader.GLSLProgram;

/** No cull face, No depth Test, Binding WaterBumpTexture texture with sampler SampleLinearWrap at texture unit0. */
/*public*/ class IsWaterNormalmapCombineProgram extends IsBaseProgram{

	final int waterBumpTexcoordShiftIndex;
	final int cameraPositionIndex;
	public IsWaterNormalmapCombineProgram(String prefix) {
		super(prefix+"WaterNormalmapCombinePS.frag", "g_WaterBumpTexture");
		
		cameraPositionIndex =GLSLProgram.getUniformLocation(programId, "g_CameraPosition");
		waterBumpTexcoordShiftIndex =GLSLProgram.getUniformLocation(programId, "g_WaterBumpTexcoordShift");
	}
	
	public void applyCameraPosition(Vector3f pos){
		GL20.glUniform3f(cameraPositionIndex, pos.x, pos.y, pos.z);
	}
	
	public void applyWaterBumpTexcoordShift(Vector2f v){
		GL20.glUniform2f(waterBumpTexcoordShiftIndex, v.x, v.y);
	}

}
