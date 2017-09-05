package jet.opengl.demos.nvidia.face.sample;

/**
 * Created by mazhen'gui on 2017/9/5.
 */

final class CGpuProfiler {
    static final int
            GTS_BeginFrame = 0,
            GTS_ShadowMap = 1,
            GTS_Skin = 2,
            GTS_Eyes = 3,
            GTS_EndFrame = 4,
            GTS_Max = 5;

    /** Which of the two sets of queries are we currently issuing?*/
    private int m_iFrameQuery;							//
    /** Which of the two did we last collect? */
    private int m_iFrameCollect;						//
//    ID3D11Query * m_apQueryTsDisjoint[2];		// "Timestamp disjoint" query; records whether timestamps are valid
    /** Individual timestamp queries for each relevant point in the frame */
    private final int[][] m_apQueryTs = new int[GTS_Max][2];		//
    /** Flags recording which timestamps were actually used in a frame */
    private final boolean[][] m_fTsUsed = new boolean[GTS_Max][2];					//

    /** Last frame's timings (each relative to previous GTS) */
    private final float[] m_adT = new float[GTS_Max];						//
    private float m_gpuFrameTime;
    /** Timings averaged over 0.5 second */
    private final float[] m_adTAvg = new float[GTS_Max];					//
    private float m_gpuFrameTimeAvg;

    /** Total timings thus far within this averaging period */
    private final float[] m_adTTotal = new float[GTS_Max];					//
    private float m_gpuFrameTimeTotal;
    /** Frames rendered in current averaging period */
    private int m_frameCount;							//
    /** Time (in ms) at which current averaging period started */
    private int m_tBeginAvg;

    CGpuProfiler(){

    }

    void Init(){

    }
    void Release(){

    }

    void BeginFrame(){

    }
    void Timestamp(int gts){

    }
    void EndFrame(){

    }

    // Wait on GPU for last frame's data (not the current frame's) to be available
    void WaitForDataAndUpdate(){

    }

    float Dt(int gts)  { return m_adT[gts]; }
    float GPUFrameTime()  { return m_gpuFrameTime; }
    float DtAvg(int gts)  { return m_adTAvg[gts]; }
    float GPUFrameTimeAvg()  { return m_gpuFrameTimeAvg; }
}
