package jet.opengl.render.debug;

import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector3f;
import org.lwjgl.util.vector.Vector4f;
import org.lwjgl.util.vector.VectorInterpolation;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;

import jet.opengl.postprocessing.common.BlendState;
import jet.opengl.postprocessing.common.DepthStencilState;
import jet.opengl.postprocessing.common.Disposeable;
import jet.opengl.postprocessing.common.GLCheck;
import jet.opengl.postprocessing.common.GLFuncProvider;
import jet.opengl.postprocessing.common.GLFuncProviderFactory;
import jet.opengl.postprocessing.common.GLStateTracker;
import jet.opengl.postprocessing.common.GLenum;

public class FrustumeRender implements Disposeable {

	private static final int POSITION = 0;
	private static final int COLOR = 1;
	
	private static final Vector3f[] CUBE_POINTS = {
			new Vector3f(-1,-1,-1),
			new Vector3f(+1,-1,-1),
			new Vector3f(+1,+1,-1),
			new Vector3f(-1,+1,-1),
			
			new Vector3f(-1,-1,+1),
			new Vector3f(+1,-1,+1),
			new Vector3f(+1,+1,+1),
			new Vector3f(-1,+1,+1),
	};
	
	public enum Plane{
		FRONT,
		BACK,
		TOP,
		BOTTOM,
		LEFT,
		RIGHT,
		ALL
	}
	
	public static class Params{
		public final Matrix4f frustumeMat = new Matrix4f();
		public final Matrix4f viewProjMat = new Matrix4f();
		public Plane drawPlane = Plane.ALL;
		public final Vector4f[] planeColos = {
				new Vector4f(1, 0, 0, 0.5f),  // front -- Red
				new Vector4f(0, 1, 0, 0.5f),  // back
				new Vector4f(1, 1, 0, 0.5f),  // left
				new Vector4f(0, 1, 1, 0.5f),  // right
				new Vector4f(1, 1, 1, 0.5f),  // top
				new Vector4f(1, 0, 1, 0.5f)	  // bottom
		};
		
		public boolean blendEnabled = true;
	}
	
	final Vector3f[] worldPos = new Vector3f[8];
	final Matrix4f projInvert = new Matrix4f();
	final PlaneWrapper[] planes = new PlaneWrapper[6];
	
	private int m_VAO;  // Vertex Array
	private int m_VPO;  // Vertex Position Buffer
	private int m_VCO;  // Vertex Color Buffer;
	private int m_IBO;  // Indices Buffer
//	private FrustumeProgram program;
	private LightFrustumeProgram program;

	DepthStencilState m_dsstate;
	BlendState m_bsstate;
	
	public FrustumeRender() {
		for(int i = 0; i < worldPos.length;i++)
			worldPos[i] = new Vector3f();

		
		Plane[] planeNames = Plane.values();
		for(int i = 0;i < 6; i++){
			planes[i] = new PlaneWrapper(planeNames[i]);
		}

		m_dsstate = new DepthStencilState();
		m_dsstate.depthEnable = true;

		m_bsstate = new BlendState();
		m_bsstate.blendEnable = true;
		m_bsstate.srcBlend = GLenum.GL_SRC_ALPHA;
		m_bsstate.destBlend = GLenum.GL_ONE_MINUS_SRC_ALPHA;
		m_bsstate.srcBlendAlpha = GLenum.GL_ZERO;
		m_bsstate.destBlendAlpha = GLenum.GL_ONE;
	}
	
	private PlaneWrapper getPlane(Plane name){
		for(PlaneWrapper p : planes){
			if(p.name == name)
				return p;
		}
		
		throw new IllegalArgumentException();
	}
	
	private void createBuffers(){
		if(m_VAO != 0)
			return;
		
//		GLError.checkError();
//		m_VAO = GL30.glGenVertexArrays();
//		GL30.glBindVertexArray(0);
//		GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
//		GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, 0);
//		GLError.checkError();
	}
	
	private void prepare(Params params){
		createBuffers();
		
		if(Matrix4f.invert(params.frustumeMat, projInvert) == null){
			System.err.println("Not a invertable matrix");
			projInvert.setIdentity();
		}
		
		if(params.blendEnabled && params.drawPlane == Plane.ALL){
			for(int i = 0; i < CUBE_POINTS.length; i++){
				Matrix4f.transformCoord(projInvert, CUBE_POINTS[i], worldPos[i]);
			}
			
			for(int i = 0; i < planes.length; i++){
				planes[i].update(params.viewProjMat);
			}
			
			Arrays.sort(planes);
		}
		
//		GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, m_VCO);
//		GL15.glBufferSubData(GL15.GL_ARRAY_BUFFER, 0, GLUtil.wrap4fv(params.planeColos));
//		GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
	}
	
	public void draw(Params params){
		if(params.drawPlane == null)
			return;
		
		prepare(params);

		final GLStateTracker stateTracker = GLStateTracker.getInstance();
		try{
			stateTracker.saveStates();
			stateTracker.setDepthStencilState(m_dsstate);
			stateTracker.setRasterizerState(null);
			if(params.blendEnabled){
				stateTracker.setBlendState(m_bsstate);
			}else{
				stateTracker.setBlendState(null);
			}

			stateTracker.setVAO(null);

			if(program == null){
				try {
					program = new LightFrustumeProgram();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}

			stateTracker.setProgram(program);
//		program.setLightToWorldMatrix(projInvert);;
//		program.setViewProjMatrix(params.viewProjMat);
			program.setLightToWorld(projInvert);
			program.setViewProj(params.viewProjMat);
			program.setFaceColors(params.planeColos);
//			GL30.glBindVertexArray(m_VAO);
//			GL11.glEnable(GL11.GL_DEPTH_TEST);
//			GL11.glDisable(GL11.GL_CULL_FACE);
//		GL11.glDepthMask(false);

			final GLFuncProvider gl = GLFuncProviderFactory.getGLFuncProvider();
			if(params.drawPlane == Plane.ALL){
//			GL11.glDrawElements(GL11.GL_TRIANGLES, 36, GL11.GL_UNSIGNED_BYTE, 0);

				if(!params.blendEnabled){
					program.setFaceID(/*Plane.ALL.ordinal()*/6);
//				GL31.glDrawArraysInstanced(GL11.GL_TRIANGLE_FAN, 0, 4, 6);
					for(int i = 0; i < 6; i++){
						program.setFaceID(i);
						gl.glDrawArrays(GLenum.GL_TRIANGLE_STRIP, 0, 4);
					}
				}else{
					for(PlaneWrapper plane: planes){
						program.setFaceID(plane.name.ordinal());
						gl.glDrawArrays(GLenum.GL_TRIANGLE_STRIP, 0, 4);
					}
				}
			}else{
//			GL11.glDrawElements(GL11.GL_TRIANGLES, 6, GL11.GL_UNSIGNED_BYTE, getPlane(params.drawPlane).offset);
				program.setFaceID(params.drawPlane.ordinal());
				gl.glDrawArrays(GLenum.GL_TRIANGLE_STRIP, 0, 4);
			}

			if(GLCheck.CHECK){
				GLCheck.checkError();
			}
		}finally {
			stateTracker.restoreStates();
			stateTracker.reset();
		}
	}
	
	private final class PlaneWrapper implements Comparable<PlaneWrapper>{
		Plane name;
		
		final int[] projPos = new int[4];
		final Vector3f center = new Vector3f();
		
		int offset;
		
		public PlaneWrapper(Plane name) {
			this.name = name;
			
			switch (name) {
			case FRONT:
				projPos[0] = 0;
				projPos[1] = 1;
				projPos[2] = 2;
				projPos[3] = 3;
				break;
			case BACK:
				projPos[0] = 4;
				projPos[1] = 5;
				projPos[2] = 6;
				projPos[3] = 7;
				break;
			case LEFT:
				projPos[0] = 0;
				projPos[1] = 4;
				projPos[2] = 7;
				projPos[3] = 3;
				break;
			case RIGHT:
				projPos[0] = 1;
				projPos[1] = 5;
				projPos[2] = 6;
				projPos[3] = 2;
				break;
			case BOTTOM:
				projPos[0] = 0;
				projPos[1] = 1;
				projPos[2] = 5;
				projPos[3] = 4;
				break;
			case TOP:
				projPos[0] = 3;
				projPos[1] = 2;
				projPos[2] = 6;
				projPos[3] = 7;
				break;
			default:
				throw new IllegalArgumentException();
			}
		}
		
		void update(Matrix4f viewPoj){
			Vector3f x = worldPos[projPos[0]];
			Vector3f y = worldPos[projPos[1]];
			Vector3f z = worldPos[projPos[2]];
			Vector3f w = worldPos[projPos[3]];
			
			VectorInterpolation.bilinear(x, y, z, w, 0.5f, 0.5f, center);
			Matrix4f.transformCoord(viewPoj, center, center);
		}
		
		void packIndices(ByteBuffer buf){
			offset = buf.position();
			
			buf.put((byte)projPos[0]);
			buf.put((byte)projPos[1]);
			buf.put((byte)projPos[2]);
		
			buf.put((byte)projPos[0]);
			buf.put((byte)projPos[2]);
			buf.put((byte)projPos[3]);
		}

		@Override
		public int compareTo(PlaneWrapper o) {
			float thisZ = center.z;
			float otheZ = o.center.z;
			if(thisZ < otheZ)
				return 1;
			else if(thisZ > otheZ)
				return -1;
			else
				return 0;
		}
	}

	@Override
	public void dispose() {
		if(m_VAO == 0)
			return;
		
//		GL30.glDeleteVertexArrays(m_VAO);
//		GL15.glDeleteBuffers(m_VPO);
//		GL15.glDeleteBuffers(m_VCO);
//		GL15.glDeleteBuffers(m_IBO);
		m_VAO = 0;
		m_VPO = 0;
		m_VCO = 0;
		m_IBO = 0;
	}
}
