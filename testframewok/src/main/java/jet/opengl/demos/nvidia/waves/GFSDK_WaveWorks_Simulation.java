package jet.opengl.demos.nvidia.waves;

import org.lwjgl.util.vector.Matrix3f;
import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector2f;
import org.lwjgl.util.vector.Vector4f;

import jet.opengl.postprocessing.common.Disposeable;
import jet.opengl.postprocessing.shader.GLSLProgram;
import jet.opengl.postprocessing.texture.Texture2D;

/**
 * Created by Administrator on 2017/7/23 0023.
 */

public class GFSDK_WaveWorks_Simulation implements Disposeable{
    private CascadeState[] cascade_states=new CascadeState[GFSDK_WaveWorks_Detailed_Simulation_Params.MaxNumCascades];

    // To preserve SLI scaling, we operate some resources that have inter-frame dependencies on a round-robin basis...
    private int m_num_GPU_slots;		// the number of GPU slots allocated for per-GPU resources (e.g. gradient maps)
    private int m_active_GPU_slot;		// the index of the active GPU within m_num_GPU_slots

    private float m_total_rms;

    private double m_dSimTime;
    private final double[] m_dSimTimeFIFO=new double[GFSDK_WaveWorks.MaxNumGPUs+1];
    private int m_numValidEntriesInSimTimeFIFO;
    private double m_dFoamSimDeltaTime;

    // Some kinds of simulation require a manager to hook simulation-level events
    private NVWaveWorks_FFT_Simulation_Manager m_pSimulationManager;

    // Scheduler to use for CPU work (optional)
    private GFSDK_WaveWorks_CPU_Scheduler_Interface m_pOptionalScheduler;

    // GFX timing services
    private NVWaveWorks_GFX_Timer_Impl m_pGFXTimer;

    // D3D API handling
    private nv_water_d3d_api m_d3dAPI;

    public GFSDK_WaveWorks_Simulation(){

    }

    public HRESULT initD3D11(GFSDK_WaveWorks_Detailed_Simulation_Params params, GFSDK_WaveWorks_CPU_Scheduler_Interface pOptionalScheduler/*, ID3D11Device* pD3DDevice*/);
    public HRESULT initGnm(GFSDK_WaveWorks_Detailed_Simulation_Params params, GFSDK_WaveWorks_CPU_Scheduler_Interface pOptionalScheduler);
    public HRESULT initGL2(GFSDK_WaveWorks_Detailed_Simulation_Params params, Object pGLContext);
    public HRESULT initNoGraphics(GFSDK_WaveWorks_Detailed_Simulation_Params params);
    public HRESULT reinit(GFSDK_WaveWorks_Detailed_Simulation_Params params);

    public void setSimulationTime(double dAppTime);
    public float getConservativeMaxDisplacementEstimate();
    public void updateRMS(GFSDK_WaveWorks_Detailed_Simulation_Params params);

    public HRESULT kick(long[] pKickID, /*Graphics_Context* pGC,*/ GFSDK_WaveWorks_Savestate pSavestateImpl);
    public HRESULT getStats(GFSDK_WaveWorks_Simulation_Stats stats);

    public boolean getStagingCursor(long[] pKickID);
    public HRESULT advanceStagingCursor(/*Graphics_Context* pGC,*/ boolean block, boolean[] wouldBlock, GFSDK_WaveWorks_Savestate pSavestateImpl);
    public HRESULT waitStagingCursor();
    public boolean getReadbackCursor(long[] pKickID);
    public HRESULT advanceReadbackCursor(boolean block, boolean[] wouldBlock);

    public HRESULT archiveDisplacements();

    public HRESULT setRenderState(	//Graphics_Context* pGC,
							Matrix4f matView,
							int[] pShaderInputRegisterMappings , GFSDK_WaveWorks_Savestate pSavestateImpl, GFSDK_WaveWorks_Simulation_GL_Pool pGlPool
    );

    public HRESULT getDisplacements(	Vector2f[] inSamplePoints,
                                Vector4f[] outDisplacements,
                                 int numSamples
    );

    public HRESULT getArchivedDisplacements(	float coord,
                                         Vector2f[] inSamplePoints,
                                         Vector4f[] outDisplacements,
                                         int numSamples
    );

    public static HRESULT getShaderInputCountD3D11();
    public static HRESULT getShaderInputDescD3D11(int inputIndex, GFSDK_WaveWorks_ShaderInput_Desc pDesc);
    public static HRESULT getShaderInputCountGnm();
    public static HRESULT getShaderInputDescGnm(int inputIndex, GFSDK_WaveWorks_ShaderInput_Desc pDesc);
    public static HRESULT getShaderInputCountGL2();
    public static HRESULT getTextureUnitCountGL2(boolean useTextureArrays);
    public static HRESULT getShaderInputDescGL2(int inputIndex, GFSDK_WaveWorks_ShaderInput_Desc pDesc);

    @Override
    public void dispose() {

    }

    private HRESULT updateGradientMaps(/*Graphics_Context* pGC,*/ GFSDK_WaveWorks_Savestate pSavestateImpl);
    private HRESULT updateGradientMapsD3D11(/*Graphics_Context* pGC,*/ GFSDK_WaveWorks_Savestate pSavestateImpl);
    private HRESULT updateGradientMapsGnm(/*Graphics_Context* pGC,*/ GFSDK_WaveWorks_Savestate pSavestateImpl);
    private HRESULT updateGradientMapsGL2(/*Graphics_Context* pGC*/);

    private HRESULT setRenderStateD3D11(//	ID3D11DeviceContext* pDC,
									Matrix4f matView,
									int[] pShaderInputRegisterMappings,
                                    GFSDK_WaveWorks_Savestate pSavestateImpl
    );
    private HRESULT setRenderStateGnm(//		sce::Gnmx::LightweightGfxContext* gfxContext,
									Matrix4f matView,
									int[] pShaderInputRegisterMappings,
                                      GFSDK_WaveWorks_Savestate pSavestateImpl
    );
    private HRESULT setRenderStateGL2(		Matrix4f matView,
									int[] pShaderInputRegisterMappings, GFSDK_WaveWorks_Simulation_GL_Pool glPool
    ){

    }

    private void consumeGPUSlot(){

    }

    private static final class D3D11Objects
    {
        Texture2D[] m_pd3d11GradientMap=new Texture2D[GFSDK_WaveWorks.MaxNumGPUs];			// (ABGR16F) - round-robin, to avoid SLI-inteframe dependencies
        Texture2D[] m_pd3d11GradientRenderTarget=new Texture2D[GFSDK_WaveWorks.MaxNumGPUs];	// (ditto)
        Texture2D m_pd3d11FoamEnergyMap;		// (R16F)
        Texture2D   m_pd3d11FoamEnergyRenderTarget;// (ditto)
    };

    private static final class GL2Objects
    {
        int[] m_GL2GradientMap=new int[GFSDK_WaveWorks.MaxNumGPUs];			// (ABGR16F) - round-robin, to avoid SLI-inteframe dependencies
        int[] m_GL2GradientFBO=new int[GFSDK_WaveWorks.MaxNumGPUs];			// (ditto)
        int m_GL2FoamEnergyMap;					// (R16F)
        int m_GL2FoamEnergyFBO;					// (ditto)
    };

    private static final class Union{
        final D3D11Objects _11=new D3D11Objects();
        final GL2Objects _GL2=new GL2Objects();
    }

    private static final class CascadeState{
        NVWaveWorks_Mesh m_pQuadMesh;
        NVWaveWorks_FFT_Simulation m_pFFTSimulation;

        // The kickID that originated the last update to this displacement map, allowing us to track when
        // the map is out of date and needs another update...
        long m_gradient_map_version;

        // Set when the gradient map is newly created and therefore in need of an intitial clear
        boolean[] m_gradient_map_needs_clear=new boolean[GFSDK_WaveWorks.MaxNumGPUs];

        final Union m_d3d = new Union();
    }

    private static final class D3D11GlobalObjects
    {
//        ID3D11Device* m_pd3d11Device;

        // Shaders for grad calc
        GLSLProgram m_pd3d11GradCalcProgram;
//        ID3D11PixelShader* m_pd3d11GradCalcPS;
        int m_pd3d11GradCalcPixelShaderCB;
        int m_pd3d11PointSampler;
        Runnable m_pd3d11NoDepthStencil;
        Runnable m_pd3d11AlwaysSolidRasterizer;
        Runnable m_pd3d11CalcGradBlendState;
        Runnable m_pd3d11AccumulateFoamBlendState;
        Runnable m_pd3d11WriteAccumulatedFoamBlendState;

        // State for main rendering
        int m_pd3d11LinearNoMipSampler;
        int m_pd3d11GradMapSampler;
        int		m_pd3d11PixelShaderCB;
        int		m_pd3d11VertexDomainShaderCB;

        // Shaders for foam generation
        GLSLProgram m_pd3d11FoamGenProgram;
//        ID3D11PixelShader* m_pd3d11FoamGenPS;
        int m_pd3d11FoamGenPixelShaderCB;
    };
}
