package jet.opengl.render.volumeline;

import org.lwjgl.util.vector.Matrix4f;

import java.io.IOException;

import jet.opengl.postprocessing.common.Disposeable;
import jet.opengl.postprocessing.common.GLAPIVersion;
import jet.opengl.postprocessing.common.GLFuncProvider;
import jet.opengl.postprocessing.common.GLFuncProviderFactory;
import jet.opengl.postprocessing.texture.Texture2D;

public abstract class VolumeLinesRenderer  implements Disposeable {

	public enum Type{
		SIMPLE,
		VERTEX,
		GEOMETRY,
		DEFAULT,
	}

	public static VolumeLinesRenderer createInstance(Type type, int maxLines) throws IOException{
		if(type == null)
			throw new NullPointerException("Type can't be null...");

		if(type == Type.DEFAULT){
			GLFuncProvider gl = GLFuncProviderFactory.getGLFuncProvider();
			GLAPIVersion version = gl.getGLAPIVersion();
			if(version.major >= 3 && ((version.minor >= 2) || (version.ES && version.minor >= 1))){
				type = Type.GEOMETRY;
			}else{
				type = Type.VERTEX;
			}
		}

		switch (type){
			case SIMPLE: return new SimpleLineRenderer(maxLines);
			case VERTEX: return new VertexLinesRenderer(maxLines);
			case GEOMETRY: return new GeometryLinesRenderer(maxLines);
			default:
			case DEFAULT: throw new Error("Inner Error!!!");
		}
	}
	
	public abstract void begin(float radius, Matrix4f mvpMat, Matrix4f mvMat, Matrix4f pMat, Texture2D texture,
			float screenRatio);
	
	public abstract void line(float x0, float y0, float z0, float x1, float y1, float z1);
	
	public abstract void lines(int _nbLines, float[] pLines, int offset);
	
	public abstract void end();
	
	/** Dispose the program */
	public abstract void dispose();
	
	public abstract int getLineCount();
	
	public abstract int getMaxLineCount();
	
	/* 
	 * Draw the lines to fragment.<br> 
	 * <b>Note:</b>This method has bug. Do not call the method.  
	 */
//	public abstract void draw();
}
