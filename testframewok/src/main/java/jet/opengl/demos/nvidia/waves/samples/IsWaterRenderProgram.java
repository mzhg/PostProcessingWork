package jet.opengl.demos.nvidia.waves.samples;

import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL40;

class IsWaterRenderProgram extends IsCoreBaseProgram{
	int waterPatchPS;
	int colorPS;
	
	public IsWaterRenderProgram(String prefix) {
		super("WaterRenderProgram", prefix+"RenderHeightfieldVS.vert", prefix+"RenderHeightfieldHS.gltc", 
				prefix+"WaterPatchDS.glte", prefix+"WaterPatchPS.frag");
		
		waterPatchPS = GL40.glGetSubroutineIndex(programId, GL20.GL_FRAGMENT_SHADER, "WaterPatchPS");
		colorPS = GL40.glGetSubroutineIndex(programId, GL20.GL_FRAGMENT_SHADER, "ColorPS");
	}
	
	public void setupWaterPatchPass(){ GL40.glUniformSubroutinesui(GL20.GL_FRAGMENT_SHADER, waterPatchPS);}
	public void setupColorPass(){ GL40.glUniformSubroutinesui(GL20.GL_FRAGMENT_SHADER, colorPS);}
}
