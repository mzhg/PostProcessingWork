package jet.opengl.demos.demos.scenes.outdoor;

import org.lwjgl.util.vector.Readable;
import org.lwjgl.util.vector.Vector4f;

import java.nio.ByteBuffer;

final class STerrainAttribs implements Readable{

	static final int SIZE = 10 * 16;
	private static final int MAX_CASCADES = 8;
	float m_fElevationScale = 0.1f;
    float m_fElevationSamplingInterval = 0.1f;
    float m_fEarthRadius = 6360000.f;
    float m_fBaseMtrlTilingScale = 100.f;
    final Vector4f m_f4TilingScale = new Vector4f(100.f, 100.f, 100.f, 100.f);
	final Vector4f[] f4CascadeColors = new Vector4f[MAX_CASCADES];
	
	public STerrainAttribs() {
		f4CascadeColors[0] = new Vector4f(0,1,0,1);
		f4CascadeColors[1] = new Vector4f(0,0,1,1);
		f4CascadeColors[2] = new Vector4f(1,1,0,1);
		f4CascadeColors[3] = new Vector4f(0,1,1,1);
		f4CascadeColors[4] = new Vector4f(1,0,1,1);
		f4CascadeColors[5] = new Vector4f(0.3f, 1, 0.7f,1);
		f4CascadeColors[6] = new Vector4f(0.7f, 0.3f,1,1);
		f4CascadeColors[7] = new Vector4f(1, 0.7f, 0.3f, 1);
	}
	
	@Override
	public ByteBuffer store(ByteBuffer buf) {
		buf.putFloat(m_fElevationScale);
		buf.putFloat(m_fElevationSamplingInterval);
		buf.putFloat(m_fEarthRadius);
		buf.putFloat(m_fBaseMtrlTilingScale);
		m_f4TilingScale.store(buf);
		for(int i = 0; i < f4CascadeColors.length; i++)
			f4CascadeColors[i].store(buf);
		return buf;
	}
}
