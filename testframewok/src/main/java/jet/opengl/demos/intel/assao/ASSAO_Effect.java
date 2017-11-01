package jet.opengl.demos.intel.assao;

interface ASSAO_Effect{

	void                PreAllocateVideoMemory(ASSAO_Inputs inputs);
    void                DeleteAllocatedVideoMemory();

    // Returns currently allocated video memory in bytes; only valid after PreAllocateVideoMemory / Draw calls
    int        GetAllocatedVideoMemory();

//    void                GetVersion( int & major, int & minor )                                          = 0;

    /**
     * Apply the SSAO effect to the currently selected render target using provided settings and platform-dependent inputs
     * @param settings
     * @param inputs
     */
    void                Draw(ASSAO_Settings settings, ASSAO_Inputs inputs);
}
