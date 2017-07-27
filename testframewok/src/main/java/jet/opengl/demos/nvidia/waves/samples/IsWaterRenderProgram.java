package jet.opengl.demos.nvidia.waves.samples;

import jet.opengl.postprocessing.common.GLenum;

class IsWaterRenderProgram extends IsCoreBaseProgram{
	int waterPatchPS;
	int colorPS;
	
	public IsWaterRenderProgram(String prefix) {
		super("WaterRenderProgram", prefix+"RenderHeightfieldVS.vert", prefix+"RenderHeightfieldHS.gltc", 
				prefix+"WaterPatchDS.glte", prefix+"WaterPatchPS.frag");
		
		waterPatchPS = gl.glGetSubroutineIndex(getProgram(), GLenum.GL_FRAGMENT_SHADER, "WaterPatchPS");
		colorPS = gl.glGetSubroutineIndex(getProgram(), GLenum.GL_FRAGMENT_SHADER, "ColorPS");
	}
	
	public void setupWaterPatchPass(){ gl.glUniformSubroutinesui(GLenum.GL_FRAGMENT_SHADER, waterPatchPS);}
	public void setupColorPass(){ gl.glUniformSubroutinesui(GLenum.GL_FRAGMENT_SHADER, colorPS);}
}
