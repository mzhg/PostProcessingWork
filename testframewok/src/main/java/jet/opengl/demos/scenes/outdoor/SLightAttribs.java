package jet.opengl.demos.scenes.outdoor;

import org.lwjgl.util.vector.Readable;
import org.lwjgl.util.vector.Vector4f;
import org.lwjgl.util.vector.Writable;

import java.nio.ByteBuffer;

final class SLightAttribs implements Readable, Writable{

	static final int SIZE = 5 * 16 + SShadowMapAttribs.SIZE + 16;
	
	final Vector4f f4DirOnLight = new Vector4f();
	final Vector4f f4AmbientLight = new Vector4f();
	final Vector4f f4LightScreenPos = new Vector4f();
	final Vector4f f4ExtraterrestrialSunColor = new Vector4f();

    boolean bIsLightOnScreen/*, b1, b2, b3*/;
    float f3DummyX, f3DummyY, f3DummyZ;

    final SShadowMapAttribs shadowAttribs = new SShadowMapAttribs();
    
    @Override
    public String toString() {
    	StringBuilder sb = new StringBuilder();
    	sb.append("SLightAttribs:\n");
    	sb.append("f4DirOnLight = ").append(f4DirOnLight).append('\n');
    	sb.append("f4AmbientLight = ").append(f4AmbientLight).append('\n');
    	sb.append("f4LightScreenPos = ").append(f4LightScreenPos).append('\n');
    	sb.append("f4ExtraterrestrialSunColor = ").append(f4ExtraterrestrialSunColor).append('\n');
    	sb.append("bIsLightOnScreen = ").append(bIsLightOnScreen).append('\n');
    	sb.append("shadowAttribs = ").append(shadowAttribs).append('\n');
    	return sb.toString();
    }

	@Override
	public ByteBuffer store(ByteBuffer buf) {
		f4DirOnLight.store(buf);
		f4AmbientLight.store(buf);
		f4LightScreenPos.store(buf);
		f4ExtraterrestrialSunColor.store(buf);
		buf.putInt(bIsLightOnScreen ? 1 : 0);
//		buf.putInt(0);
//		buf.putInt(0);
//		buf.putInt(0);
//		buf.putFloat(0);
//		buf.putFloat(0);
//		buf.putFloat(0);
//		buf.putFloat(0);
		int newPos = buf.position() + 7 *4;
		buf.position(newPos);
		shadowAttribs.store(buf);
		return buf;
	}

	@Override
	public Writable load(ByteBuffer buf) {
		f4DirOnLight.load(buf);
		f4AmbientLight.load(buf);
		f4LightScreenPos.load(buf);
		f4ExtraterrestrialSunColor.load(buf);
		bIsLightOnScreen = buf.getInt()!=0;
		f3DummyX = buf.getFloat();
		f3DummyY = buf.getFloat();
		f3DummyZ = buf.getFloat();
		shadowAttribs.load(buf);
		return null;
	}
	
}
