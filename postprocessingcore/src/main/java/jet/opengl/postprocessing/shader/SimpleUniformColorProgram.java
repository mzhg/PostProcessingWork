package jet.opengl.postprocessing.shader;

import org.lwjgl.util.vector.Matrix4f;
import java.io.IOException;

import jet.opengl.postprocessing.util.CacheBuffer;

public class SimpleUniformColorProgram extends GLSLProgram{

	int posAttrLoc;
	int mvpUnifomLoc;
	int colorUniformLoc;

	public SimpleUniformColorProgram() throws IOException{
		setSourceFromFiles("shader_libs/simple/simple_v_uc.glvs", "shader_libs/simple/simple_v_uc.glfs");
		enable();
		
		posAttrLoc = getAttribLocation("aPosition");
		colorUniformLoc = getUniformLocation("uColor");
		mvpUnifomLoc = getUniformLocation("uMvp");
	}
	
	public int getPositionAttribLocation() {return posAttrLoc;}
//	public int getColorUniformLocation()    {return colorUniformLoc;}
//	public int getMvpUniformLocation()     {return mvpUnifomLoc;}

	/** Call this method should call {@link #enable()} first. */
	public void setMVP(Matrix4f mvp){
		gl.glUniformMatrix4fv(mvpUnifomLoc, false, CacheBuffer.wrap(mvp != null ? mvp: Matrix4f.IDENTITY));
	}
	
	/** Call this method should call {@link #enable()} first. */
	public void setUniformColor(float r, float g, float b, float a){
		gl.glUniform4f(colorUniformLoc, r, g, b, a);
	}
}
