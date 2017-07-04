package jet.opengl.demos.scenes.outdoor;

import org.lwjgl.util.vector.Readable;
import org.lwjgl.util.vector.Vector4f;
import org.lwjgl.util.vector.Writable;

import java.nio.ByteBuffer;

final class SCascadeAttribs implements Readable, Writable{

	public static final int SIZE = 3 * 16;
	
	final Vector4f f4LightSpaceScale = new Vector4f();
	final Vector4f f4LightSpaceScaledBias = new Vector4f();
    final Vector4f f4StartEndZ = new Vector4f();
    
	@Override
	public ByteBuffer store(ByteBuffer buf) {
		f4LightSpaceScale.store(buf);
		f4LightSpaceScaledBias.store(buf);
		f4StartEndZ.store(buf);
		return buf;
	}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("SCascadeAttribs:\n");
		sb.append("f4LightSpaceScale = ").append(f4LightSpaceScale).append('\n');
		sb.append("f4LightSpaceScaledBias = ").append(f4LightSpaceScaledBias).append('\n');
		sb.append("f4StartEndZ = ").append(f4StartEndZ).append('\n');
		return sb.toString();
	}

	@Override
	public Writable load(ByteBuffer buf) {
		f4LightSpaceScale.load(buf);
		f4LightSpaceScaledBias.load(buf);
		f4StartEndZ.load(buf);

		return this;
	}
}
