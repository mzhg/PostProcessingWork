package jet.opengl.demos.nvidia.waves;

import org.lwjgl.util.vector.Vector2f;
import org.lwjgl.util.vector.Vector4f;

import java.nio.ByteBuffer;

import jet.opengl.postprocessing.texture.Texture2D;

/**
 * Created by mazhen'gui on 2017/7/22.
 */

final class NVWaveWorks_FFT_Simulation_CPU_Impl implements NVWaveWorks_FFT_Simulation{
    private GFSDK_WaveWorks_Detailed_Simulation_Params.Cascade m_next_params;
    private GFSDK_WaveWorks_Detailed_Simulation_Params.Cascade m_params;
    private boolean m_params_are_dirty;
    private final NoGraphicsObjects _noGFX = new NoGraphicsObjects();

    // D3D API handling
    private nv_water_d3d_api m_d3dAPI;

    //initial spectrum data
    private float[] m_gauss_data;		// We cache the Gaussian distribution which underlies h0 in order to avoid having to re-run the
    // random number generator when we re-calculate h0 (e.g. when windspeed changes)
    private float[] m_h0_data;
    private float[] m_omega_data;
    private float[] m_sqrt_table; //pre-computed coefficient for speed-up computation of update spectrum

    //in-out buffer for FFTCPU, it holds 3 FFT images sequentially
    private long[] m_fftCPU_io_buffer;

    // "safe" buffers with data for readbacks, filled by working threads
    private final Vector4f[][] m_readback_buffer = new Vector4f[2][];
    private Vector4f[] m_active_readback_buffer;	// The readback buffer currently being served - this can potentially be a different buffer from the
    // double-buffered pair in m_readback_buffer[], since one of those could have been swapped for one
    // from the FIFO when an archiving operation occured

    private CircularFIFO<ReadbackFIFOSlot> m_pReadbackFIFO;

    private volatile long m_ref_count_update_h0, m_ref_count_update_ht, m_ref_count_FFT_X, m_ref_count_FFT_Y, m_ref_count_update_texture;

    // current index of a texture that is mapped and filled by working threads
    // can be 0 or 1. Other texture is returned to user and can be safely used for rendering
    private int m_mapped_texture_index;

    private ByteBuffer m_mapped_texture_ptr; //pointer to a mapped texture that is filling by working threads
    private int m_mapped_texture_row_pitch;

    private double m_doubletime;

    private boolean m_H0UpdateRequired;

    private long m_DisplacementMapVersion;

    private boolean m_pipelineNextReinit;

    // Simulation primitives
    /** Returns true if this is the last row to be updated */
    public boolean UpdateH0(int row){
        return false;
    }

    void pipelineNextReinit() { m_pipelineNextReinit = true; }

    @Override
    public boolean initD3D11() {
        return false;
    }

    @Override
    public boolean initNoGraphics() {
        return false;
    }

    @Override
    public HRESULT reinit(GFSDK_WaveWorks_Detailed_Simulation_Params.Cascade params) {
        return HRESULT.E_FAIL;
    }

    @Override
    public HRESULT addDisplacements(Vector2f[] inSamplePoints, Vector4f[] outDisplacements, int numSamples) {
        return HRESULT.E_FAIL;
    }

    @Override
    public HRESULT addArchivedDisplacements(float coord, Vector2f[] inSamplePoints, Vector4f[] outDisplacements, int numSamples) {
        return HRESULT.E_FAIL;
    }

    @Override
    public HRESULT getTimings(NVWaveWorks_FFT_Simulation_Timings time) {
        return HRESULT.E_FAIL;
    }

    @Override
    public long getDisplacementMapVersion() {
        return 0;
    }

    @Override
    public Texture2D GetDisplacementMapD3D11() {
        return null;
    }

    @Override
    public int GetDisplacementMapGL2() {
        return 0;
    }

    private HRESULT allocateAllResources(){
        return null;
    }
    private void releaseAllResources(){

    }

    private void releaseAll(){

    }

    private HRESULT initGaussAndOmega(){
        return null;
    }

//    private void UpdateH0(const Task& task);
//    private void UpdateHt(const Task& task);
//    private void ComputeFFT(const Task& task);
//    private void UpdateTexture(const Task& task);

    private static final class NoGraphicsObjects
    {
        Object[] m_pnogfxDisplacementMap = new Object[2];
        int m_nogfxDisplacementMapRowPitch;
    }

    private static final class ReadbackFIFOSlot
    {
        long kickID;
        Vector4f[] buffer;
    }

    /** Returns true if this is the last row to be updated */
//    bool UpdateHt(int row);			//
//    bool UpdateTexture(int row);	// Returns true if this is the last row to be updated
//
//    // FFT simulation primitives - 2 paths here:
//    // - the 'legacy' path models the entire NxN 2D FFT as a single task
//    // - the new path models each group of N-wide 1D FFT's as a single task
//    bool ComputeFFT_XY_NxN(int index);		// Returns true if this is the last FFT to be processed
//    bool ComputeFFT_X(int XYZindex, int subIndex);
//    bool ComputeFFT_Y(int XYZindex, int subIndex);
//
//    int GetNumRowsIn_FFT_X() const;
//    int GetNumRowsIn_FFT_Y() const;
//
//    HRESULT OnInitiateSimulationStep(Graphics_Context* pGC, double dSimTime);
//    void OnCompleteSimulationStep(gfsdk_U64 kickID);
//
//    gfsdk_U64 getDisplacementMapVersion() const { return m_DisplacementMapVersion; }
//    ID3D11ShaderResourceView** GetDisplacementMapD3D11();
//    sce::Gnm::Texture* GetDisplacementMapGnm();
//    GLuint					   GetDisplacementMapGL2();
//
//    GFSDK_WaveWorks_Detailed_Simulation_Params.Cascade GetParams() { return m_params; }
//
//    bool IsH0UpdateRequired() const { return m_H0UpdateRequired; }
//    void SetH0UpdateNotRequired() { m_H0UpdateRequired = false; }
//
//    HRESULT archiveDisplacements(gfsdk_U64 kickID);
//
//    void calcReinit(GFSDK_WaveWorks_Detailed_Simulation_Params.Cascade params, bool& bRelease, bool& bAllocate, bool& bReinitH0, bool& bReinitGaussAndOmega);
}
