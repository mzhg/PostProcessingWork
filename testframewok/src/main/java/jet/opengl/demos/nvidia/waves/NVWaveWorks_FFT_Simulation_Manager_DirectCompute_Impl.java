package jet.opengl.demos.nvidia.waves;

import java.util.ArrayList;

import jet.opengl.postprocessing.common.GLCheck;

/**
 * Created by mazhen'gui on 2017/7/22.
 */

final class NVWaveWorks_FFT_Simulation_Manager_DirectCompute_Impl implements NVWaveWorks_FFT_Simulation_Manager {

    private final ArrayList<NVWaveWorks_FFT_Simulation_DirectCompute_Impl> m_Simulations=new ArrayList<>();

    private long m_NextKickID;

    private boolean m_StagingCursorIsValid;
    private long m_StagingCursorKickID;

    private HRESULT checkForReadbackResults(){
        HRESULT hr;

        // The goal here is to evolve the readback state of all our simulations in lockstep, so that either all our simulations collect
        // a single readback or else none do (IOW: 'some' is *not* permitted, because it would break lockstep)

//        NVWaveWorks_FFT_Simulation_DirectCompute_Impl** pBeginSimulationsSrc = (NVWaveWorks_FFT_Simulation_DirectCompute_Impl**)_alloca(m_Simulations.size() * sizeof(NVWaveWorks_FFT_Simulation_DirectCompute_Impl*));
//        memcpy(pBeginSimulationsSrc,m_Simulations.begin(),m_Simulations.size() * sizeof(NVWaveWorks_FFT_Simulation_DirectCompute_Impl*));
//        NVWaveWorks_FFT_Simulation_DirectCompute_Impl** pEndSimulationsSrc = pBeginSimulationsSrc + m_Simulations.size();
        NVWaveWorks_FFT_Simulation_DirectCompute_Impl[] pSimulationsSrc= m_Simulations.toArray(new NVWaveWorks_FFT_Simulation_DirectCompute_Impl[m_Simulations.size()]);


//        NVWaveWorks_FFT_Simulation_DirectCompute_Impl** pBeginSimulationsNoResult = (NVWaveWorks_FFT_Simulation_DirectCompute_Impl**)_alloca(m_Simulations.size() * sizeof(NVWaveWorks_FFT_Simulation_DirectCompute_Impl*));;
//        NVWaveWorks_FFT_Simulation_DirectCompute_Impl** pEndSimulationsNoResult = pBeginSimulationsNoResult;
        NVWaveWorks_FFT_Simulation_DirectCompute_Impl[] pSimulationsNoResult=new NVWaveWorks_FFT_Simulation_DirectCompute_Impl[m_Simulations.size()];
        int iSimulationsNoResultCount=0;

        // Do an initial walk thru and see if any readbacks arrived (without blocking), and write any that did not get a readback result into dst
        for(NVWaveWorks_FFT_Simulation_DirectCompute_Impl pSim : pSimulationsSrc)
        {
            hr = pSim.collectSingleReadbackResult(false);
            if(hr== HRESULT.E_FAIL)
            {
                return hr;
            }

            if(HRESULT.S_FALSE == hr)
            {
//                (*pEndSimulationsNoResult) = (*pSim);
//                ++pEndSimulationsNoResult;
                pSimulationsNoResult[iSimulationsNoResultCount++]= pSim;
            }
        }

        // If no results are ready, we're in sync so don't try again
        if(iSimulationsNoResultCount != m_Simulations.size())
        {
            // Otherwise, wait on the remaining results
            for(NVWaveWorks_FFT_Simulation_DirectCompute_Impl pSim : pSimulationsNoResult)
            {
                if(pSim==null)
                    break;

                hr=pSim.collectSingleReadbackResult(true);
                if(hr== HRESULT.E_FAIL)
                {
                    return hr;
                }
            }
        }

//#if defined(_DEV) || defined (DEBUG)
        VerifyReadbackLockstep();
//#endif

        GLCheck.checkError();
        return HRESULT.S_OK;
    }

    public HRESULT initD3D11() {    return HRESULT.S_OK;}
    public HRESULT initGL2() {    return HRESULT.S_OK;}
    public HRESULT initNoGraphics() {    return HRESULT.S_OK;}
    public HRESULT initGnm() {   return HRESULT.S_OK;}

    @Override
    public NVWaveWorks_FFT_Simulation createSimulation(GFSDK_WaveWorks_Detailed_Simulation_Params.Cascade params) {
        NVWaveWorks_FFT_Simulation_DirectCompute_Impl pResult = new NVWaveWorks_FFT_Simulation_DirectCompute_Impl(this,params);
        m_Simulations.add(pResult);
        return pResult;
    }

    @Override
    public void releaseSimulation(NVWaveWorks_FFT_Simulation pSimulation) {
        m_Simulations.remove(pSimulation);
//        SAFE_DELETE(pSimulation);TODO release simulation
    }

    @Override
    public HRESULT kick(double dSimTime, long[] kickID) {
        HRESULT hr;

        kickID[0] = m_NextKickID;

        // Check for readback results - note that we do this at the manager level in order to guarantee lockstep between
        // the simulations that form a cascade. We either want all of simulations to collect a result, or none - some is
        // not an option
        checkForReadbackResults();

        // Kick all the sims
        for(NVWaveWorks_FFT_Simulation_DirectCompute_Impl pSim : m_Simulations)
        {
            hr=pSim.kick(/*pGC,*/dSimTime,kickID[0]);
            if(hr==HRESULT.E_FAIL)
                return hr;
        }

        m_StagingCursorIsValid = true;
        m_StagingCursorKickID = m_NextKickID;
        ++m_NextKickID;
        return HRESULT.S_OK;
    }

    @Override
    public boolean getStagingCursor(long[] pKickID) {
        if(pKickID!=null && m_StagingCursorIsValid)
        {
		    pKickID[0] = m_StagingCursorKickID;
        }

        return m_StagingCursorIsValid;
    }

    @Override
    public boolean getReadbackCursor(long[] pKickID) {
        if(0 == m_Simulations.size())
            return false;

        // We rely on collectSingleReadbackResult() to maintain lockstep between the cascade members, therefore we can in theory
        // query any member to get the readback cursor...

        // ...but let's check that theory in debug builds!!!
        VerifyReadbackLockstep();

        return m_Simulations.get(0).getReadbackCursor(pKickID);
    }

    @Override
    public AdvanceCursorResult advanceStagingCursor(boolean block) {
        // The DirectCompute pipeline is not async wrt the API, so there can never be any pending kicks and we can return immediately
        return AdvanceCursorResult.AdvanceCursorResult_None;
    }

    @Override
    public AdvanceCursorResult advanceReadbackCursor(boolean block) {
        if(0 == m_Simulations.size())
            return AdvanceCursorResult.AdvanceCursorResult_None;

        // First, check whether we even have readbacks in-flight
	    final boolean hasReadbacksInFlightSim0 = m_Simulations.get(0).hasReadbacksInFlight();

        // Usual paranoid verficiation that we're maintaining lockstep...
//#ifdef _DEV
        VerifyReadbackLockstep();
//#endif

        if(!hasReadbacksInFlightSim0)
        {
            return AdvanceCursorResult.AdvanceCursorResult_None;
        }

        if(!block)
        {
            // Non-blocking case - in order to maintain lockstep, either all of the simulations should consume a readback,
            // or none. Therefore we need to do an initial pass to test whether the 'all' case applies (and bail if not)...
            for(NVWaveWorks_FFT_Simulation_DirectCompute_Impl pSim : m_Simulations)
            {
                HRESULT hr = pSim.canCollectSingleReadbackResultWithoutBlocking();
                if(hr==HRESULT.E_FAIL)
                {
                    return AdvanceCursorResult.AdvanceCursorResult_Failed;
                }
                else if(HRESULT.S_FALSE == hr)
                {
                    // Cannot advance, would have blocked -> bail
                    return AdvanceCursorResult.AdvanceCursorResult_WouldBlock;
                }
            }
        }

        // We have readbacks in flight, and in the non-blocking case we *should* be in a position to consume them without
        // any waiting, so just visit each simulation in turn with a blocking wait for the next readback to complete...
        for(NVWaveWorks_FFT_Simulation_DirectCompute_Impl pSim : m_Simulations)
        {
            if(pSim.collectSingleReadbackResult(true) ==HRESULT.E_FAIL)
            {
                return AdvanceCursorResult.AdvanceCursorResult_Failed;
            }
        }

        VerifyReadbackLockstep();

        return AdvanceCursorResult.AdvanceCursorResult_Succeeded;
    }

    @Override
    public WaitCursorResult waitStagingCursor() {
        // The DirectCompute pipeline is not async wrt the API, so there can never be any pending kicks and we can return immediately
        return WaitCursorResult.WaitCursorResult_None;
    }

    @Override
    public HRESULT archiveDisplacements() {
        if(!getReadbackCursor(null))
        {
            return HRESULT.E_FAIL;
        }

        for(NVWaveWorks_FFT_Simulation_DirectCompute_Impl pSim : m_Simulations)
        {
            if(pSim.archiveDisplacements()==HRESULT.E_FAIL){
                return HRESULT.E_FAIL;
            }
        }

        return HRESULT.S_OK;
    }

    @Override
    public HRESULT beforeReinit(GFSDK_WaveWorks_Detailed_Simulation_Params params, boolean reinitOnly) {
        return HRESULT.S_OK;
    }

    @Override
    public HRESULT getTimings(GFSDK_WaveWorks_Simulation_Manager_Timings timings) {
        // DirectCompute implementation doesn't update these CPU implementation related timings
        timings.time_start_to_stop = 0;
        timings.time_total = 0;
        timings.time_wait_for_completion = 0;
        return HRESULT.S_OK;
    }

    HRESULT beforeReallocateSimulation(){
        HRESULT hr;

        // A simulation is about to be reallocated...

        // Implication 1: at least some displacement map contents will become undefined and
        // will need a kick to make them valid again, which in turn means that we can no longer
        // consider any kick that was previously staged as still being staged...
        m_StagingCursorIsValid = false;

        // Implication 2: some of the readback tracking will be reset, meaning we break
        // lockstep. We can avoid this by forcible resetting all readback tracking
        for(NVWaveWorks_FFT_Simulation_DirectCompute_Impl pSim : m_Simulations)
        {
            hr=pSim.resetReadbacks();
            if(hr!=HRESULT.S_OK)
                return hr;
        }

        return HRESULT.S_OK;
    }

    private void VerifyReadbackLockstep(){
        if(GLCheck.CHECK == false)
            return;

        if(m_Simulations.size() > 1)
        {
            long[] sim0KickID=new long[1];
            boolean sim0GRCresult = m_Simulations.get(0).getReadbackCursor(sim0KickID);
            for(int i=1; i < m_Simulations.size(); i++)
            {
                NVWaveWorks_FFT_Simulation_DirectCompute_Impl pSim=m_Simulations.get(i);
                long[] simNKickID=new long[1];
                boolean simNGRCresult = pSim.getReadbackCursor(simNKickID);
                assert(simNGRCresult == sim0GRCresult);
                if(simNGRCresult != sim0GRCresult){
                    throw new IllegalStateException();
                }
                if(sim0GRCresult)
                {
                    assert(sim0KickID[0] == simNKickID[0]);
                    if(sim0KickID[0] != simNKickID[0]){
                        throw new IllegalStateException();
                    }
                }
            }
        }
    }

    @Override
    public void dispose() {
        // TODO
    }
}
