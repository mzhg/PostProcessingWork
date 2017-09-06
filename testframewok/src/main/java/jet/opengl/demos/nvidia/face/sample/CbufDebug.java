package jet.opengl.demos.nvidia.face.sample;

import java.nio.ByteBuffer;

/**
 * Created by mazhen'gui on 2017/9/5.
 */

final class CbufDebug {
    static final int SIZE = 5 * 4;
    float			m_debug;			// Mapped to spacebar - 0 if up, 1 if down
    float			m_debugSlider0;		// Mapped to debug slider in UI
    float			m_debugSlider1;		// ...
    float			m_debugSlider2;		// ...
    float			m_debugSlider3;		// ...

    ByteBuffer store(ByteBuffer buffer){
        buffer.putFloat(m_debug);
        buffer.putFloat(m_debugSlider0);
        buffer.putFloat(m_debugSlider1);
        buffer.putFloat(m_debugSlider2);
        buffer.putFloat(m_debugSlider3);
        return buffer;
    }
}
