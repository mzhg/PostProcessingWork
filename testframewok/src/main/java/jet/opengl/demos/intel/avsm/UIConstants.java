package jet.opengl.demos.intel.avsm;

import org.lwjgl.util.vector.Readable;
import org.lwjgl.util.vector.Vector4f;

import java.nio.ByteBuffer;

/**
 * Created by mazhen'gui on 2017/10/9.
 */

final class UIConstants implements Readable{
    static final int SIZE = Vector4f.SIZE * 3;
    int faceNormals;
    int enableStats;
    int volumeShadowMethod;
    int enableVolumeShadowLookup;   // 1
    int pauseParticleAnimaton;
    int particleOpacity;
    int vertexShaderShadowLookup;
    int tessellate;                // 2
    int wireframe;
    int lightingOnly;              // 3

    float        particleSize;
    float  TessellationDensity;               // Edge, inside, minimum tessellation factor and 1/desired triangle size   4

    @Override
    public ByteBuffer store(ByteBuffer buf) {
        buf.putInt(faceNormals);
        buf.putInt(enableStats);
        buf.putInt(volumeShadowMethod);
        buf.putInt(enableVolumeShadowLookup);

        buf.putInt(pauseParticleAnimaton);
        buf.putInt(particleOpacity);
        buf.putInt(vertexShaderShadowLookup);
        buf.putInt(tessellate);

        buf.putInt(wireframe);
        buf.putInt(lightingOnly);
        buf.putFloat(particleSize);
        buf.putFloat(TessellationDensity);  // TODO need check.
        return buf;
    }
}
