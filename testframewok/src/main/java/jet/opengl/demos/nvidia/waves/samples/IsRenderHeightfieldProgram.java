package jet.opengl.demos.nvidia.waves.samples;

import jet.opengl.postprocessing.common.GLenum;

class IsRenderHeightfieldProgram extends IsCoreBaseProgram{

	int renderHeightFieldPS;
	int colorPS;
	int render_shadowmap_index;
		
	public IsRenderHeightfieldProgram(String prefix) {
		super("RenderHeightfieldProgram", prefix+"RenderHeightfieldVS.vert", prefix+"RenderHeightfieldHS.gltc", 
				prefix+"RenderHeightfieldHD.glte", prefix+"RenderHeightfieldPS.frag");
		
		renderHeightFieldPS = gl.glGetSubroutineIndex(getProgram(), GLenum.GL_FRAGMENT_SHADER, "RenderHeightFieldPS");
		colorPS = gl.glGetSubroutineIndex(getProgram(), GLenum.GL_FRAGMENT_SHADER, "ColorPS");
		render_shadowmap_index = getUniformLocation("g_RenderShadowmap");
	}

	public void setupRenderHeightFieldPass(){ gl.glUniformSubroutinesui(GLenum.GL_FRAGMENT_SHADER, renderHeightFieldPS);}
	public void setupColorPass(){ gl.glUniformSubroutinesui(GLenum.GL_FRAGMENT_SHADER, colorPS);}
	public void setRenderShadowmap(boolean flag) { gl.glUniform1i(render_shadowmap_index, flag ? 1 : 0);}
}
