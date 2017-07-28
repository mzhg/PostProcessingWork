package jet.opengl.demos.nvidia.waves.samples;

import org.lwjgl.util.vector.Matrix4f;

import java.io.IOException;

import jet.opengl.postprocessing.common.GLenum;
import jet.opengl.postprocessing.shader.AttribBinder;
import jet.opengl.postprocessing.shader.GLSLProgram;
import jet.opengl.postprocessing.util.CacheBuffer;

final class SkyBoxProgram extends GLSLProgram {

	private final int attribPos;
	private final int uMVP;
	
	// envMap
	public SkyBoxProgram() {
		this(-1);
	}
	
	public SkyBoxProgram(final int posBind) {

		try {
			final String shaderPath = "nvidia/WaveWorks/shaders/";
			setAttribBinding(new AttribBinder("PosAttribute", posBind >=0?posBind:0));
			setSourceFromFiles(shaderPath + "simple_cube_map.vert", shaderPath + "simple_cube_map.frag");
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		attribPos = getAttribLocation("PosAttribute");
		
		uMVP = getUniformLocation("uMVP");
		
		int texindex = getUniformLocation("envMap");
		gl.glUseProgram(getProgram());
		gl.glUniform1i(texindex, 0);
		gl.glUniformMatrix4fv(uMVP, false, CacheBuffer.wrap(Matrix4f.IDENTITY));
		gl.glUseProgram(0);
	}
	
	public int getCubeMapUnit() { return GLenum.GL_TEXTURE0;}
	public int getAttribPosition() { return attribPos;}
	public void applyMVP(Matrix4f mat) {gl.glUniformMatrix4fv(uMVP, false, CacheBuffer.wrap(mat != null ? mat : Matrix4f.IDENTITY));}
}
