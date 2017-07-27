package jet.opengl.demos.nvidia.waves.samples;

import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL40;

import jet.util.opengl.shader.GLSLProgram;

class IsRenderHeightfieldProgram extends IsCoreBaseProgram{

	int renderHeightFieldPS;
	int colorPS;
	int render_shadowmap_index;
		
	public IsRenderHeightfieldProgram(String prefix) {
		super("RenderHeightfieldProgram", prefix+"RenderHeightfieldVS.vert", prefix+"RenderHeightfieldHS.gltc", 
				prefix+"RenderHeightfieldHD.glte", prefix+"RenderHeightfieldPS.frag");
		
		renderHeightFieldPS = GL40.glGetSubroutineIndex(programId, GL20.GL_FRAGMENT_SHADER, "RenderHeightFieldPS");
		colorPS = GL40.glGetSubroutineIndex(programId, GL20.GL_FRAGMENT_SHADER, "ColorPS");
		render_shadowmap_index = GLSLProgram.getUniformLocation(programId, "g_RenderShadowmap");
	}

	public void setupRenderHeightFieldPass(){ GL40.glUniformSubroutinesui(GL20.GL_FRAGMENT_SHADER, renderHeightFieldPS);}
	public void setupColorPass(){ GL40.glUniformSubroutinesui(GL20.GL_FRAGMENT_SHADER, colorPS);}
	public void setRenderShadowmap(boolean flag) { GL20.glUniform1i(render_shadowmap_index, flag ? 1 : 0);}
}
