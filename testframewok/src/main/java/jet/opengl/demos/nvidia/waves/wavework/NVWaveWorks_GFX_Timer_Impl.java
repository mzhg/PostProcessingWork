package jet.opengl.demos.nvidia.waves.wavework;

import jet.opengl.postprocessing.common.GLCheck;
import jet.opengl.postprocessing.common.GLFuncProvider;
import jet.opengl.postprocessing.common.GLFuncProviderFactory;
import jet.opengl.postprocessing.common.GLenum;

import static jet.opengl.demos.nvidia.waves.wavework.nv_water_d3d_api.nv_water_d3d_api_d3d11;
import static jet.opengl.demos.nvidia.waves.wavework.nv_water_d3d_api.nv_water_d3d_api_gl2;
import static jet.opengl.demos.nvidia.waves.wavework.nv_water_d3d_api.nv_water_d3d_api_undefined;

/**
 * Created by mazhen'gui on 2017/7/22.
 */

final class NVWaveWorks_GFX_Timer_Impl {
    static final int InvalidQueryIndex = -1;

    private GFSDK_WaveWorks_GFX_DisjointQuery_Pool_Impl m_pDisjointTimersPool;
    private int m_CurrentDisjointTimerQuery;

    private GFSDK_WaveWorks_GFX_TimerQuery_Pool_Impl m_pTimersPool;

    // D3D API handling
    private nv_water_d3d_api m_d3dAPI;
    private GLFuncProvider gl;

    NVWaveWorks_GFX_Timer_Impl(){
        m_d3dAPI = nv_water_d3d_api_undefined;

        m_pDisjointTimersPool = null;
        m_pTimersPool = null;

        m_CurrentDisjointTimerQuery = -1;
    }

    boolean initD3D11(){
        if(nv_water_d3d_api_d3d11 != m_d3dAPI)
        {
            releaseAll();
        }
        /*else if(m_d3d._11.m_pd3d11Device != pD3DDevice)
        {
            releaseAll();
        }*/

        if(nv_water_d3d_api_undefined == m_d3dAPI)
        {
            m_d3dAPI = nv_water_d3d_api_d3d11;

            allocateAllResources();
        }

        return true;
    }
    boolean initGnm() { return false;}
    boolean initGL2(){
        if(nv_water_d3d_api_gl2 != m_d3dAPI)
        {
            releaseAll();
        }


        if(nv_water_d3d_api_undefined == m_d3dAPI)
        {
            m_d3dAPI = nv_water_d3d_api_gl2;
            allocateAllResources();
        }
        return true;
    }

    // Timer queries wrapper
    int issueTimerQuery(){
        int ix;
        if(0 == m_pTimersPool.getNumInactiveQueries()){
            switch (m_d3dAPI){
                case nv_water_d3d_api_d3d11:
                {
                    TimerQueryData tqd = m_pTimersPool.addInactiveQuery();

//                    D3D11_QUERY_DESC query_desc;
//                    query_desc.Query = D3D11_QUERY_TIMESTAMP;
//                    query_desc.MiscFlags = 0;
//                    V_RETURN(m_d3d._11.m_pd3d11Device->CreateQuery(&query_desc, &tqd.m_d3d._11.m_pTimerQuery));
                    tqd.m_GLTimerQuery = gl.glGenQuery();
                }
                break;
                case nv_water_d3d_api_gnm:
                {
                    throw new UnsupportedOperationException();
                }
                case nv_water_d3d_api_gl2:
                {
                    TimerQueryData tqd = m_pTimersPool.addInactiveQuery();
                    tqd.m_GLTimerQuery = gl.glGenQuery();
                }
                break;
                default:
                    return -1;
            }
        }

        ix = m_pTimersPool.activateQuery();

        // Begin the query
        switch (m_d3dAPI){
            case nv_water_d3d_api_d3d11:
            {
                TimerQueryData tqd = m_pTimersPool.getQueryData(ix);
//                ID3D11DeviceContext* pDC_d3d11 = pGC->d3d11();
//                pDC_d3d11->End(tqd.m_d3d._11.m_pTimerQuery);
                GLCheck.checkError();
                gl.glQueryCounter(tqd.m_GLTimerQuery, GLenum.GL_TIMESTAMP);
                GLCheck.checkError();
            }
            break;
            case nv_water_d3d_api_gnm:
            {
                throw new UnsupportedOperationException();
            }
            case nv_water_d3d_api_gl2:
            {
                TimerQueryData tqd = m_pTimersPool.getQueryData(ix);
                gl.glQueryCounter(tqd.m_GLTimerQuery, GLenum.GL_TIMESTAMP);
            }
            break;
            default:
                return -1;
        }

        return ix;
    }


    void releaseTimerQuery(int ix){
        m_pTimersPool.releaseQuery(ix);
    }

    HRESULT waitTimerQuery(int ix, long[] time){
// No built-in sync in DX, roll our own as best we can...
        HRESULT status = HRESULT.S_FALSE;
        do
        {
            status = getTimerQuery(/*pGC,*/ ix, time);
            if(HRESULT.S_FALSE == status)
            {
                try {
                    Thread.sleep(0);
                } catch (InterruptedException e) {
//                    e.printStackTrace();
                }
            }
        }
        while(HRESULT.S_FALSE == status);
//
//        return status;

        return status;
    }

    HRESULT getTimerQuery(/*Graphics_Context* pGC,*/ int ix, long[] time){
        TimerQueryData tqd = m_pTimersPool.getQueryData(ix);
        if(HRESULT.S_FALSE == tqd.m_status)
        {
            long result = 0;
            HRESULT hr;
            switch(m_d3dAPI)
            {
                case nv_water_d3d_api_d3d11:
                {
//                    ID3D11DeviceContext* pDC_d3d11 = pGC->d3d11();
//                    hr = pDC_d3d11->GetData(tqd.m_d3d._11.m_pTimerQuery, &result, sizeof(result), 0);
                    result = gl.glGetQueryObjectuiv(tqd.m_GLTimerQuery, GLenum.GL_QUERY_RESULT_AVAILABLE);
                    if(result == GLenum.GL_FALSE)
                    {
                        hr = HRESULT.S_FALSE;
                    }
                    else
                    {
                        result = gl.glGetQueryObjectui64ui(tqd.m_GLTimerQuery, GLenum.GL_QUERY_RESULT);
                        hr = HRESULT.S_OK;
                    }
                }
                break;
                case nv_water_d3d_api_gnm:
                {
                    throw new UnsupportedOperationException();
                }

                case nv_water_d3d_api_gl2:
                {
                    result = gl.glGetQueryObjectuiv(tqd.m_GLTimerQuery, GLenum.GL_QUERY_RESULT_AVAILABLE);
                    if(result == GLenum.GL_FALSE)
                    {
                        hr = HRESULT.S_FALSE;
                    }
                    else
                    {
                        result = gl.glGetQueryObjectui64ui(tqd.m_GLTimerQuery, GLenum.GL_QUERY_RESULT);
                        hr = HRESULT.S_OK;
                    }
                }
                break;

                default:
                {
                    // Unexpected API
                    hr = HRESULT.E_FAIL;
                }
                break;
            }

            switch(hr)
            {
                case S_FALSE:
                    break;
                case S_OK:
                    tqd.m_timestampResult = result;
                    tqd.m_status = HRESULT.S_OK;
                    break;
                default:
                    tqd.m_timestampResult = 0;
                    tqd.m_status = hr;
                    break;
            }
            return hr;
        }

        if(HRESULT.S_FALSE != tqd.m_status && time != null)
        {
            time[0] = tqd.m_timestampResult;
        }

        return tqd.m_status;
    }

    // Pair-wise get/wait
    HRESULT getTimerQueries(/*Graphics_Context* pGC,*/ int ix1, int ix2, long[] tdiff){
        long[] stamp1 ={0};
        HRESULT hr1 = getTimerQuery(/*pGC,*/ ix1, stamp1);
        if(HRESULT.S_FALSE == hr1)
            return HRESULT.S_FALSE;
        long[] stamp2={0};
        HRESULT hr2 = getTimerQuery(/*pGC,*/ ix2, stamp2);
        if(HRESULT.S_FALSE == hr2)
            return HRESULT.S_FALSE;

        if(HRESULT.S_OK == hr1 && HRESULT.S_OK ==hr2)
        {
            tdiff[0] = stamp2[0] - stamp1[0];
            return HRESULT.S_OK;
        }
        else if(HRESULT.S_OK == hr1)
        {
            return hr2;
        }
        else
        {
            return hr1;
        }
    }

    HRESULT waitTimerQueries(/*Graphics_Context* pGC,*/ int ix1, int ix2, long[] tdiff){
// No built-in sync in DX, roll our own as best we can...
        HRESULT status = HRESULT.S_FALSE;
        do
        {
            status = getTimerQueries(/*pGC,*/ ix1, ix2, tdiff);
            if(HRESULT.S_FALSE == status)
            {
                try {
                    Thread.sleep(0);
                } catch (InterruptedException e) {
                }
            }
        }
        while(HRESULT.S_FALSE == status);

        return status;
    }

    // Disjoint queries wrapper
    HRESULT beginDisjoint(){
        if(0 == m_pDisjointTimersPool.getNumInactiveQueries())
        {
            // Add D3D resources
//            #if WAVEWORKS_ENABLE_GRAPHICS
            switch(m_d3dAPI)
            {

//                #if WAVEWORKS_ENABLE_D3D11
                case nv_water_d3d_api_d3d11:
                case nv_water_d3d_api_gl2:
                {
                    HRESULT hr;
                    DisjointQueryData dqd = m_pDisjointTimersPool.addInactiveQuery();
//                    D3D11_QUERY_DESC query_desc;
//                    query_desc.Query = D3D11_QUERY_TIMESTAMP_DISJOINT;
//                    query_desc.MiscFlags = 0;
//                    V_RETURN(m_d3d._11.m_pd3d11Device->CreateQuery(&query_desc, &dqd.m_d3d._11.m_pDisjointTimerQuery));
                }
                break;
//                #endif
//                #if WAVEWORKS_ENABLE_GNM
//                case nv_water_d3d_api_gnm:
//                {
//				/*DisjointQueryData& dqd = */ m_pDisjointTimersPool->addInactiveQuery();
//                }
//                break;
//                #endif
//
//                #if WAVEWORKS_ENABLE_GL
//                case nv_water_d3d_api_gl2:
//                {
//				/*DisjointQueryData& dqd =*/ m_pDisjointTimersPool->addInactiveQuery();
//                    // GL doesn't have disjoint queries atm, so doing nothing
//                }
//                break;
//                #endif

                default:
                    // Unexpected API
                    return HRESULT.E_FAIL;
            }
//            #endif // WAVEWORKS_ENABLE_GRAPHICS
        }

        // Make an inactive query current
        assert(m_CurrentDisjointTimerQuery == -1);
        m_CurrentDisjointTimerQuery = m_pDisjointTimersPool.activateQuery();

        // Begin the disjoint query
        /*#if WAVEWORKS_ENABLE_GRAPHICS
        switch(m_d3dAPI)
        {
            #if WAVEWORKS_ENABLE_D3D11
            case nv_water_d3d_api_d3d11:
            {
                ID3D11DeviceContext* pDC_d3d11 = pGC->d3d11();
                const DisjointQueryData& dqd = m_pDisjointTimersPool->getQueryData(m_CurrentDisjointTimerQuery);
                pDC_d3d11->Begin(dqd.m_d3d._11.m_pDisjointTimerQuery);
            }
            break;
            #endif
            #if WAVEWORKS_ENABLE_GNM
            case nv_water_d3d_api_gnm:
            {
			*//*const DisjointQueryData& dqd =*//* m_pDisjointTimersPool->getQueryData(m_CurrentDisjointTimerQuery);
            }
            break;
            #endif

            #if WAVEWORKS_ENABLE_GL
            case nv_water_d3d_api_gl2:
            {
                // GL doesn't have disjoint queries atm, so doing nothing
            }
            break;
            #endif
            default:
                // Unexpected API
                return E_FAIL;
        }
        #endif // WAVEWORKS_ENABLE_GRAPHICS*/

        return HRESULT.S_OK;
    }
    HRESULT endDisjoint(){
        assert(m_CurrentDisjointTimerQuery != -1);

        // End the disjoint query
        /*#if WAVEWORKS_ENABLE_GRAPHICS
        switch(m_d3dAPI)
        {

            #if WAVEWORKS_ENABLE_D3D11
            case nv_water_d3d_api_d3d11:
            {
                ID3D11DeviceContext* pDC_d3d11 = pGC->d3d11();
                const DisjointQueryData& dqd = m_pDisjointTimersPool->getQueryData(m_CurrentDisjointTimerQuery);
                pDC_d3d11->End(dqd.m_d3d._11.m_pDisjointTimerQuery);
            }
            break;
            #endif
            #if WAVEWORKS_ENABLE_GNM
            case nv_water_d3d_api_gnm:
            {
			*//*const DisjointQueryData& dqd =*//* m_pDisjointTimersPool->getQueryData(m_CurrentDisjointTimerQuery);
            }
            break;
            #endif

            #if WAVEWORKS_ENABLE_GL
            case nv_water_d3d_api_gl2:
            {
                // GL doesn't have disjoint queries atm, so doing nothing
            }
            break;
            #endif

            default:
                // Unexpected API
                return E_FAIL;
        }
        #endif // WAVEWORKS_ENABLE_GRAPHICS*/

        // Release the query (but others may have referenced it by now...)
        m_pDisjointTimersPool.releaseQuery(m_CurrentDisjointTimerQuery);
        m_CurrentDisjointTimerQuery = -1;

        return HRESULT.S_OK;
    }

    int getCurrentDisjointQuery(){
        assert(m_CurrentDisjointTimerQuery != -1);

        m_pDisjointTimersPool.addRefQuery(m_CurrentDisjointTimerQuery);	// udpate ref-count
        return m_CurrentDisjointTimerQuery;
    }

    void releaseDisjointQuery(int ix){m_pDisjointTimersPool.releaseQuery(ix);}
    HRESULT waitDisjointQuery(/*Graphics_Context* pGC,*/ int ix, long[] f) {
        // No built-in sync in DX, roll our own as best we can...
        HRESULT status = HRESULT.S_FALSE;
        do
        {
            status = getDisjointQuery(/*pGC,*/ ix, f);
            if(HRESULT.S_FALSE == status)
            {
                try {
                    Thread.sleep(0);
                } catch (InterruptedException e) {
//                    e.printStackTrace();
                }
            }
        }
        while(HRESULT.S_FALSE == status);

        return status;
    }

    HRESULT getDisjointQuery(/*Graphics_Context* pGC,*/ int ix, long[] f){
        DisjointQueryData dqd = m_pDisjointTimersPool.getQueryData(ix);
        if(HRESULT.S_FALSE == dqd.m_status)
        {
            HRESULT hr = HRESULT.E_FAIL;
            boolean WasDisjoint = false;
            long RawF = 0;

//            #if WAVEWORKS_ENABLE_GRAPHICS
            switch(m_d3dAPI)
            {
                /*#if WAVEWORKS_ENABLE_D3D11
                case nv_water_d3d_api_d3d11:
                {
                    ID3D11DeviceContext* pDC_d3d11 = pGC->d3d11();

                    D3D11_QUERY_DATA_TIMESTAMP_DISJOINT result;
                    hr = pDC_d3d11->GetData(dqd.m_d3d._11.m_pDisjointTimerQuery, &result, sizeof(result), 0);

                    RawF = result.Frequency;
                    WasDisjoint = result.Disjoint;
                }
                break;
                #endif
                #if WAVEWORKS_ENABLE_GNM
                case nv_water_d3d_api_gnm:
                {
                    hr = S_OK;
                }
                break;
                #endif
                #if WAVEWORKS_ENABLE_GL*/
                case nv_water_d3d_api_d3d11:
                case nv_water_d3d_api_gl2:
                {
                    // GL doesn't have disjoint queries atm, so assuming the queries are not disjoint
                    hr = HRESULT.S_OK;
                    RawF = 1000000000;
                    WasDisjoint = false;
                }
                break;
//                #endif
                default:
                    // Unexpected API
                    return HRESULT.E_FAIL;
            }
//            #endif // WAVEWORKS_ENABLE_GRAPHICS

            switch(hr)
            {
                case S_FALSE:
                    break;
                case S_OK:
                    dqd.m_freqResult = WasDisjoint ? 0 : RawF;
                    dqd.m_status = WasDisjoint ? HRESULT.E_FAIL : HRESULT.S_OK;
                    break;
                default:
                    dqd.m_freqResult = 0;
                    dqd.m_status = hr;
                    break;
            }
        }

        if(HRESULT.S_FALSE != dqd.m_status)
        {
            f[0] = dqd.m_freqResult;
        }

        return dqd.m_status;
    }

    private void allocateAllResources(){
//        SAFE_DELETE(m_pDisjointTimersPool);
        m_pDisjointTimersPool = new GFSDK_WaveWorks_GFX_DisjointQuery_Pool_Impl();

//        SAFE_DELETE(m_pTimersPool);
        m_pTimersPool = new GFSDK_WaveWorks_GFX_TimerQuery_Pool_Impl();

        gl = GLFuncProviderFactory.getGLFuncProvider();
    }

    private void releaseAllResources(){
        switch(m_d3dAPI)
        {
            case nv_water_d3d_api_d3d11:
            {
                for(int i = 0; i != m_pDisjointTimersPool.getNumQueries(); ++i)
                {
                    DisjointQueryData dqd = m_pDisjointTimersPool.getQueryData(i);
                    gl.glDeleteQuery(dqd.m_pDisjointTimerQuery);
                }
                for(int i = 0; i != m_pTimersPool.getNumQueries(); ++i)
                {
                    TimerQueryData tqd = m_pTimersPool.getQueryData(i);
                    gl.glDeleteQuery(tqd.m_GLTimerQuery);
                }
                break;
            }
            case nv_water_d3d_api_gl2:
            {
                for(int i = 0; i != m_pTimersPool.getNumQueries(); ++i)
                {
                    TimerQueryData tqd = m_pTimersPool.getQueryData(i);
                    if(tqd.m_GLTimerQuery > 0) GLFuncProviderFactory.getGLFuncProvider().glDeleteQuery(tqd.m_GLTimerQuery);
                }
                break;
            }
        }

        m_pDisjointTimersPool = null;
        m_pTimersPool = null;
    }

    void releaseAll(){
        releaseAllResources();
        m_d3dAPI = nv_water_d3d_api_undefined;
    }
}
