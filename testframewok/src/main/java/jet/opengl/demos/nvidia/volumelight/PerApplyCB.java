package jet.opengl.demos.nvidia.volumelight;

import java.nio.ByteBuffer;

import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector3f;
import org.lwjgl.util.vector.Vector4f;
import org.lwjgl.util.vector.Writable;

import jet.opengl.demos.intel.fluid.utils.UniformGrid;
import jet.opengl.postprocessing.buffer.BufferGL;
import jet.opengl.postprocessing.util.CacheBuffer;

final class PerApplyCB implements Writable{
	static final int SIZE = Matrix4f.SIZE + Vector4f.SIZE * 2;
	// c0
    final Matrix4f mHistoryXform = new Matrix4f();
    // c4
    float fFilterThreshold;
    float fHistoryFactor;
//    float pad1[2];
    // c5
    final Vector3f vFogLight = new Vector3f();
    float fMultiScattering;
    
    /*void store(UniformBlockData uniforms){
    	if(uniforms == null)
    		return;
    	
    	uniforms.set("mHistoryXform", mHistoryXform);
    	uniforms.set("fFilterThreshold", fFilterThreshold);
    	uniforms.set("fHistoryFactor", fHistoryFactor);
    	uniforms.set("vFogLight", vFogLight);
    	uniforms.set("fMultiScattering", fMultiScattering);
    }*/

	void store(BufferGL unfiorms){
		if(unfiorms == null) return;

		ByteBuffer buffer = CacheBuffer.getCachedByteBuffer(SIZE);
		mHistoryXform.store(buffer);
		buffer.putFloat(fFilterThreshold);
		buffer.putFloat(fHistoryFactor);
		buffer.putLong(0);

		vFogLight.store(buffer);
		buffer.putFloat(fMultiScattering);

		buffer.flip();
		if(buffer.remaining() != SIZE)
			throw new AssertionError();

		unfiorms.bind();
		unfiorms.update(0, buffer);
	}
    
	@Override
	public PerApplyCB load(ByteBuffer buf) {
		mHistoryXform.load(buf);
		fFilterThreshold = buf.getFloat();
		fHistoryFactor = buf.getFloat();
		buf.getFloat();  // pad1[0]
		buf.getFloat();  // pad1[1]
		
		vFogLight.load(buf);
		fMultiScattering = buf.getFloat();
		
		return this;
	}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("PerApplyCB:\n");
		sb.append("mHistoryXform = ").append(mHistoryXform).append('\n');
		sb.append("fFilterThreshold = ").append(fFilterThreshold).append('\n');
		sb.append("fHistoryFactor = ").append(fHistoryFactor).append('\n');
		sb.append("vFogLight = ").append(vFogLight).append('\n');
		sb.append("fMultiScattering = ").append(fMultiScattering).append('\n');
		return sb.toString();
	}
}
