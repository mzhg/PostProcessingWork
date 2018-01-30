package jet.opengl.demos.intel.avsm;

import org.lwjgl.util.vector.Readable;
import org.lwjgl.util.vector.Vector4f;

import java.nio.ByteBuffer;

/**
 * Created by mazhen'gui on 2017/11/1.
 */

final class ParticlePerFrameConstants implements Readable{
    static final int SIZE = Vector4f.SIZE * 2;
    float  mScale;                             // Scene scale factor
    float  mParticleSize;                      // Particles size in (pre)projection space
    float  mParticleOpacity;				   // (Max) Particle contrbution to the CDF
    float  mParticleAlpha;				       // (Max) Particles transparency
    float  mbSoftParticles;				       // Soft Particles Enable-Disable
    float  mSoftParticlesSaturationDepth;      // Saturation Depth for Soft Particles.

    @Override
    public ByteBuffer store(ByteBuffer buf) {
        int pos = buf.position();
        buf.putFloat(mScale);
        buf.putFloat(mParticleSize);
        buf.putFloat(mParticleOpacity);
        buf.putFloat(mParticleAlpha);
        buf.putFloat(mbSoftParticles);
        buf.putFloat(mSoftParticlesSaturationDepth);
        buf.position(pos + SIZE);
        return buf;
    }
}
