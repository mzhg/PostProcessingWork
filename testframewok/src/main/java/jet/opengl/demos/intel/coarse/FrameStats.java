package jet.opengl.demos.intel.coarse;

final class FrameStats {

    void Accumulate( FrameStats stats) {
        m_totalShadingTime      += stats.m_totalShadingTime;
        m_totalGBuffGen         += stats.m_totalGBuffGen;
        m_totalSkyBox           += stats.m_totalSkyBox;
    }

    void Normalize(int numFrames) {
        assert(numFrames != 0);
        m_totalShadingTime      /= numFrames;
        m_totalGBuffGen         /= numFrames;
        m_totalSkyBox           /= numFrames;
    }

    float   m_totalShadingTime;
    float   m_totalSkyBox;
    float   m_totalGBuffGen;
}
