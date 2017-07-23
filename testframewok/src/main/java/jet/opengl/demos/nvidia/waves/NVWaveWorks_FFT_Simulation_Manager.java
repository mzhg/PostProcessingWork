package jet.opengl.demos.nvidia.waves;

/**
 * Created by mazhen'gui on 2017/7/22.
 */

public interface NVWaveWorks_FFT_Simulation_Manager {

    HRESULT initD3D11();
    HRESULT initGL2();
    HRESULT initNoGraphics();
    HRESULT initGnm();

    // Simulation lifetime management
    NVWaveWorks_FFT_Simulation createSimulation(GFSDK_WaveWorks_Detailed_Simulation_Params.Cascade params);
    void releaseSimulation(NVWaveWorks_FFT_Simulation pSimulation);

    // Pipeline synchronization
    HRESULT kick(/*Graphics_Context* pGC,*/ double dSimTime, long[] kickID);
    boolean getStagingCursor(long[] pKickID);					// Returns true iff the staging cursor is valid
    boolean getReadbackCursor(long[] pKickID);					// Returns true iff the readback cursor is valid


    AdvanceCursorResult advanceStagingCursor(boolean block);
    AdvanceCursorResult advanceReadbackCursor(boolean block);

    WaitCursorResult waitStagingCursor();

    HRESULT archiveDisplacements();

    // Hooks
    HRESULT beforeReinit(GFSDK_WaveWorks_Detailed_Simulation_Params params, boolean reinitOnly);
    HRESULT getTimings(GFSDK_WaveWorks_Simulation_Manager_Timings timings);
}
