package jet.opengl.demos.nvidia.waves.wavework;

import org.lwjgl.util.vector.Vector2f;
import org.lwjgl.util.vector.Vector4f;

import jet.opengl.postprocessing.texture.Texture2D;

/**
 * Created by mazhen'gui on 2017/7/21.
 */

public interface NVWaveWorks_FFT_Simulation {

    boolean initD3D11(/*ID3D11Device* pD3DDevice*/);
    default boolean initGnm() { return false; };
    default boolean initGL2(/*void* pGLContext*/) {return false; };
    boolean initNoGraphics();

    HRESULT reinit(GFSDK_WaveWorks_Detailed_Simulation_Params.Cascade params);

    HRESULT addDisplacements(Vector2f[] inSamplePoints,
                             Vector4f[] outDisplacements, int numSamples
    );

    HRESULT addArchivedDisplacements(	float coord,Vector2f[] inSamplePoints,
                                      Vector4f[] outDisplacements, int numSamples);

    HRESULT getTimings(NVWaveWorks_FFT_Simulation_Timings time);

    long getDisplacementMapVersion();	// Returns the kickID of the last time the displacement map was updated

    Texture2D GetDisplacementMapD3D11();
//    virtual sce::Gnm::Texture* GetDisplacementMapGnm() { return NULL; }
    int					   GetDisplacementMapGL2();
}
