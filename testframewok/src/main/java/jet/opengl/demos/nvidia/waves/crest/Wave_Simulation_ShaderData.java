package jet.opengl.demos.nvidia.waves.crest;

import org.lwjgl.util.vector.Vector4f;

import java.nio.ByteBuffer;

import jet.opengl.demos.nvidia.waves.ocean.TechniqueParams;
import jet.opengl.postprocessing.texture.TextureGL;

class Wave_Simulation_ShaderData implements TechniqueParams {

    TextureGL _LD_TexArray_WaveBuffer;
    Vector4f[] _LD_Params_Source;
    Vector4f[] _LD_Params;

    Vector4f[] _LD_Pos_Scale_Source;
    Vector4f[] _LD_Pos_Scale;

    TextureGL _LD_TexArray_AnimatedWaves;
    TextureGL _LD_TexArray_AnimatedWaves_Source;
    TextureGL _CombineBuffer;
    TextureGL _LD_TexArray_AnimatedWaves_Compute;

    TextureGL _LD_TexArray_DynamicWaves;
    TextureGL _LD_TexArray_DynamicWaves_Source;

    TextureGL _LD_TexArray_Flow;
    TextureGL _LD_TexArray_Flow_Source;

    TextureGL _LD_TexArray_Foam;
    TextureGL _LD_TexArray_Foam_Source;

    TextureGL _LD_TexArray_SeaFloorDepth;
    TextureGL _LD_TexArray_SeaFloorDepth_Source;

    TextureGL _LD_TexArray_Shadow;
    TextureGL _LD_TexArray_Shadow_Source;

    int _LD_SliceIndex;
    TextureGL _LD_TexArray_Target;

    float _SimDeltaTime;
    float _SimDeltaTimePrev;
    float _LODChange;

    float _HorizDisplace = 3f;
    //        [Range(0f, 1f), Tooltip("Clamp displacement to help prevent self-intersection in steep waves. Zero means unclamped.")]
    float _DisplaceClamp = 0.3f;

    float _Damping;
    float _Gravity;

    final Vector4f _LaplacianAxisX = new Vector4f();

    float _FoamFadeRate;
    float _WaveFoamStrength;
    float _WaveFoamCoverage;
    float _ShorelineFoamMaxDepth;
    float _ShorelineFoamStrength;

    float _Weight;

    Vector4f[] _TwoPiOverWavelengths;
    Vector4f[] _Amplitudes;
    Vector4f[] _WaveDirX;
    Vector4f[] _WaveDirZ;
    Vector4f[] _Phases;
    Vector4f[] _ChopAmps;
    int _NumInBatch;
    float _AttenuationInShallows;
    int _NumWaveVecs;

    final Vector4f _TargetPointData = new Vector4f();

    @Override
    public ByteBuffer store(ByteBuffer buf) {
        return null;
    }
}
