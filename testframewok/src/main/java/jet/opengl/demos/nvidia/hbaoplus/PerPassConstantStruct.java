package jet.opengl.demos.nvidia.hbaoplus;

import org.lwjgl.util.vector.Vector2f;
import org.lwjgl.util.vector.Vector4f;

import java.nio.ByteBuffer;

public class PerPassConstantStruct {
	
	static final int SIZE = 2 * 16;

	final Vector4f f4Jitter = new Vector4f();
	
	final Vector2f f2Offset = new Vector2f();
	float fSliceIndex;
	int uSliceIndex;
	
	void store(ByteBuffer buf){
		f4Jitter.store(buf);
		f2Offset.store(buf);
		buf.putFloat(fSliceIndex);
		buf.putInt(uSliceIndex);
	}
}
