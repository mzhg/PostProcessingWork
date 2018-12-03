package jet.opengl.demos.nvidia.volumelight;

import org.lwjgl.util.vector.Vector2f;
import org.lwjgl.util.vector.Vector4f;
import org.lwjgl.util.vector.Writable;

import java.nio.ByteBuffer;

import jet.opengl.postprocessing.buffer.BufferGL;
import jet.opengl.postprocessing.util.CacheBuffer;

final class PerContextCB implements Writable{
	static final int SIZE = Vector4f.SIZE * 3;

	final Vector2f vOutputSize = new Vector2f();
	final Vector2f vOutputSize_Inv = new Vector2f();
	
	final Vector2f vBufferSize = new Vector2f();
	final Vector2f vBufferSize_Inv = new Vector2f();
	
	float fResMultiplier;
	int uSampleCount;
	
	/*void store(UniformBlockData uniforms){
		if(uniforms == null)
			return;
		
		uniforms.set("vOutputSize", vOutputSize);
		uniforms.set("vOutputSize_Inv", vOutputSize_Inv);
		uniforms.set("vBufferSize", vBufferSize);
		uniforms.set("vBufferSize_Inv", vBufferSize_Inv);
		uniforms.set("fResMultiplier", fResMultiplier);
		uniforms.set("uSampleCount", uSampleCount);
	}*/

	void store(BufferGL unfiorms){
		if(unfiorms == null) return;

		ByteBuffer buffer = CacheBuffer.getCachedByteBuffer(SIZE);
		vOutputSize.store(buffer);
		vOutputSize_Inv.store(buffer);
		vBufferSize.store(buffer);
		vBufferSize_Inv.store(buffer);

		buffer.putFloat(fResMultiplier);
		buffer.putInt(uSampleCount);
		buffer.putLong(0);

		buffer.flip();
		if(buffer.remaining() != SIZE)
			throw new AssertionError();

		unfiorms.bind();
		unfiorms.update(0, buffer);
	}
	
	@Override
	public PerContextCB load(ByteBuffer buf) {
		vOutputSize.load(buf);
		vOutputSize_Inv.load(buf);
		
		vBufferSize.load(buf);
		vBufferSize_Inv.load(buf);
		
		fResMultiplier = buf.getFloat();
		uSampleCount = buf.getInt();
		
		buf.getFloat();  //pad1[0]
		buf.getFloat();  //pad1[1]
		return this;
	}

	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("PerContextCB:\n");
		sb.append("vOutputSize = ").append(vOutputSize).append('\n');
		sb.append("vOutputSize_Inv = ").append(vOutputSize_Inv).append('\n');
		sb.append("vBufferSize = ").append(vBufferSize).append('\n');
		sb.append("vBufferSize_Inv = ").append(vBufferSize_Inv).append('\n');
		sb.append("fResMultiplier = ").append(fResMultiplier).append('\n');
		sb.append("uSampleCount = ").append(uSampleCount).append('\n');
		return sb.toString();
	}
}
