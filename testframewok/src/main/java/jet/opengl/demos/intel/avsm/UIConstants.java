package jet.opengl.demos.intel.avsm;

import org.lwjgl.util.vector.Vector4f;

/**
 * Created by mazhen'gui on 2017/10/9.
 */

final class UIConstants {
    static final int SIZE = Vector4f.SIZE * 4;
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
}
