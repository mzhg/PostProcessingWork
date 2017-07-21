package jet.opengl.demos.nvidia.waves;

import static com.sun.org.apache.xpath.internal.objects.XBoolean.S_FALSE;

/**
 * Created by mazhen'gui on 2017/7/21.
 */

public interface NVWaveWorks_FFT_Simulation {

    boolean initD3D11(/*ID3D11Device* pD3DDevice*/);
    default boolean initGnm() { return false; };
    default boolean initGL2(/*void* pGLContext*/) {return false; };
    boolean initNoGraphics();

    boolean reinit(const GFSDK_WaveWorks_Detailed_Simulation_Params::Cascade& params);

    virtual HRESULT addDisplacements(	const gfsdk_float2* inSamplePoints,
                                         gfsdk_float4* outDisplacements,
                                         UINT numSamples
    ) = 0;

    virtual HRESULT addArchivedDisplacements(	float coord,
                                                 const gfsdk_float2* inSamplePoints,
                                                 gfsdk_float4* outDisplacements,
                                                 UINT numSamples
    ) = 0;

    virtual HRESULT getTimings(NVWaveWorks_FFT_Simulation_Timings&) const = 0;

    virtual gfsdk_U64 getDisplacementMapVersion() const = 0;	// Returns the kickID of the last time the displacement map was updated

    // NB: None of these AddRef's the underlying D3D resource
    virtual ID3D11ShaderResourceView** GetDisplacementMapD3D11() = 0;
    virtual sce::Gnm::Texture* GetDisplacementMapGnm() { return NULL; }
    virtual GLuint					   GetDisplacementMapGL2() = 0;
}
