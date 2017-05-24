package jet.opengl.render.debug;

import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector4f;

import java.io.IOException;

import jet.opengl.postprocessing.shader.GLSLProgram;
import jet.opengl.postprocessing.util.CacheBuffer;

final class LightFrustumeProgram extends GLSLProgram{
	private int faceIDIndex;
	private int lightToWorldIndex;
	private int faceColorsIndex;
	private int viewProjIndex;
	
	public LightFrustumeProgram() throws IOException {
		final String filepath = "shader_libs/simple/";
		setSourceFromFiles(filepath + "LightFrustume.vert", filepath + "LightFrustume.frag");
	
		faceIDIndex = getUniformLocation("iFaceID");
		lightToWorldIndex = getUniformLocation("g_LightToWorld");
		viewProjIndex = getUniformLocation("g_ViewProj");
		faceColorsIndex = getUniformLocation("f4FaceColors");
	}
	
	public void setFaceID(int idx){
		gl.glUniform1i(faceIDIndex, idx);
	}
	
	public void setLightToWorld(Matrix4f mat){
		gl.glUniformMatrix4fv(lightToWorldIndex, false, CacheBuffer.wrap(mat));
	}
	
	public void setViewProj(Matrix4f mat){
		gl.glUniformMatrix4fv(viewProjIndex, false, CacheBuffer.wrap(mat));
	}
	
	public void setFaceColors(Vector4f[] colors){
		gl.glUniform4fv(faceColorsIndex, CacheBuffer.wrap(colors));
	}
}
