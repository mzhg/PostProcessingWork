package jet.opengl.demos.nvidia.volumelight;

import java.nio.ByteBuffer;

import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector3f;
import org.lwjgl.util.vector.Writable;

import jet.opengl.postprocessing.buffer.BufferGL;

final class PerApplyCB implements Writable{

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
		throw new UnsupportedOperationException();
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
