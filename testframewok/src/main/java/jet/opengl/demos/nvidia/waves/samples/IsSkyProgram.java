package jet.opengl.demos.nvidia.waves.samples;

import org.lwjgl.util.vector.Matrix4f;

import jet.opengl.postprocessing.common.GLenum;
import jet.opengl.postprocessing.util.CacheBuffer;


/** TODO */
/*public*/ class IsSkyProgram extends IsBaseProgram{

	int matIndex;
	
	int skyPS;
	int colorPS;
	public IsSkyProgram(String prefix) {
		super(prefix + "SkyVS.vert", prefix + "SkyPS.frag", "g_SkyTexture");
		
		matIndex = getUniformLocation("g_ModelViewProjectionMatrix");
		skyPS = gl.glGetSubroutineIndex(getProgram(), GLenum.GL_FRAGMENT_SHADER, "SkyPS");
		colorPS = gl.glGetSubroutineIndex(getProgram(), GLenum.GL_FRAGMENT_SHADER, "ColorPS");
	}
	
	public void setupSkyPass() { gl.glUniformSubroutinesui(GLenum.GL_FRAGMENT_SHADER, skyPS);}
	public void setupColorPass() {gl.glUniformSubroutinesui(GLenum.GL_FRAGMENT_SHADER, colorPS);}
	public void setModelViewProjectionMatrix(Matrix4f mat){gl.glUniformMatrix4fv(matIndex, false, CacheBuffer.wrap(mat));}
}
