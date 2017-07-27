package jet.opengl.demos.nvidia.waves.samples;

import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL40;
import org.lwjgl.util.vector.Matrix4f;

import jet.util.buffer.GLUtil;
import jet.util.opengl.shader.GLSLProgram;

/** TODO */
/*public*/ class IsSkyProgram extends IsBaseProgram{

	int matIndex;
	
	int skyPS;
	int colorPS;
	public IsSkyProgram(String prefix) {
		super(prefix + "SkyVS.vert", prefix + "SkyPS.frag", "g_SkyTexture");
		
		matIndex = GLSLProgram.getUniformLocation(programId, "g_ModelViewProjectionMatrix");
		skyPS = GL40.glGetSubroutineIndex(programId, GL20.GL_FRAGMENT_SHADER, "SkyPS");
		colorPS = GL40.glGetSubroutineIndex(programId, GL20.GL_FRAGMENT_SHADER, "ColorPS");
	}
	
	public void setupSkyPass() { GL40.glUniformSubroutinesui(GL20.GL_FRAGMENT_SHADER, skyPS);}
	public void setupColorPass() {GL40.glUniformSubroutinesui(GL20.GL_FRAGMENT_SHADER, colorPS);}
	public void setModelViewProjectionMatrix(Matrix4f mat){GL20.glUniformMatrix4fv(matIndex, false, GLUtil.wrap(mat));}
}
