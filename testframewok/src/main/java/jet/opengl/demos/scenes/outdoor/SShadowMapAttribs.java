package jet.opengl.demos.scenes.outdoor;

import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Readable;
import org.lwjgl.util.vector.Writable;

import java.nio.ByteBuffer;
import java.util.Arrays;

final class SShadowMapAttribs implements Readable, Writable{

	static final int SIZE = Matrix4f.SIZE * (1+ LSConstant.MAX_CASCADES) + (4 + SCascadeAttribs.SIZE) * LSConstant.MAX_CASCADES + 4 * 4;
	
	final Matrix4f mWorldToLightViewT = new Matrix4f();
	final SCascadeAttribs[] cascades = new SCascadeAttribs[LSConstant.MAX_CASCADES];
	final float[] fCascadeCamSpaceZEnd = new float[LSConstant.MAX_CASCADES];
	final Matrix4f[] mWorldToShadowMapUVDepthT = new Matrix4f[LSConstant.MAX_CASCADES];
	
	boolean bVisualizeCascades;
	float f3PaddingX, f3PaddingY, f3PaddingZ;
	
	public SShadowMapAttribs() {
		for (int i = 0; i < cascades.length; i++) {
			cascades[i] = new SCascadeAttribs();
			mWorldToShadowMapUVDepthT[i] = new Matrix4f();
		}
	}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("SShadowMapAttribs:\n");
		sb.append("mWorldToLightViewT = ").append(mWorldToLightViewT).append('\n');
		sb.append("cascades = ").append(Arrays.toString(cascades)).append('\n');
		sb.append("fCascadeCamSpaceZEnd = ").append(Arrays.toString(fCascadeCamSpaceZEnd)).append('\n');
		sb.append("mWorldToShadowMapUVDepthT = ").append(Arrays.toString(mWorldToShadowMapUVDepthT)).append('\n');
		sb.append("bVisualizeCascades = ").append(bVisualizeCascades).append('\n');
		return sb.toString();
	}

	@Override
	public ByteBuffer store(ByteBuffer buf) {
		mWorldToLightViewT.store(buf);  mWorldToLightViewT.transpose();
		for(int i = 0; i < cascades.length; i++)
			cascades[i].store(buf);
		for(int i = 0; i < fCascadeCamSpaceZEnd.length; i++)
			buf.putFloat(fCascadeCamSpaceZEnd[i]);
		for(int i = 0; i < mWorldToShadowMapUVDepthT.length; i++){
			mWorldToShadowMapUVDepthT[i].store(buf);
//			mWorldToShadowMapUVDepthT[i].transpose();
		}
		
		buf.putInt(bVisualizeCascades ? 1 : 0);
		buf.putFloat(0);
		buf.putFloat(0);
		buf.putFloat(0);
		
		return buf;
	}

	@Override
	public Writable load(ByteBuffer buf) {
		mWorldToLightViewT.load(buf); mWorldToLightViewT.transpose();
		for(int i = 0; i < cascades.length; i++)
			cascades[i].load(buf);
		for(int i = 0; i < fCascadeCamSpaceZEnd.length; i++)
			fCascadeCamSpaceZEnd[i] = buf.getFloat();
		for(int i = 0; i < mWorldToShadowMapUVDepthT.length; i++){
			mWorldToShadowMapUVDepthT[i].load(buf);
			mWorldToShadowMapUVDepthT[i].transpose();
		}
		bVisualizeCascades = buf.getInt() != 0;
		f3PaddingX = buf.getFloat();
		f3PaddingY = buf.getFloat();
		f3PaddingZ = buf.getFloat();
		return this;
	}
}
