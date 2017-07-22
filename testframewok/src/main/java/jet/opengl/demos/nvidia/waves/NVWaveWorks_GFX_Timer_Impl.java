package jet.opengl.demos.nvidia.waves;

import jet.opengl.postprocessing.common.GLFuncProvider;
import jet.opengl.postprocessing.common.GLFuncProviderFactory;
import jet.opengl.postprocessing.common.GLenum;

import static jet.opengl.demos.nvidia.waves.nv_water_d3d_api.nv_water_d3d_api_d3d11;
import static jet.opengl.demos.nvidia.waves.nv_water_d3d_api.nv_water_d3d_api_gl2;
import static jet.opengl.demos.nvidia.waves.nv_water_d3d_api.nv_water_d3d_api_undefined;
import static jet.opengl.postprocessing.common.GLenum.GL_FALSE;
import static jet.opengl.postprocessing.common.GLenum.GL_QUERY_RESULT;
import static jet.opengl.postprocessing.common.GLenum.GL_QUERY_RESULT_AVAILABLE;

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
                TimerQueryData tqd = m_pTimersPool.addInactiveQuery();
//                ID3D11DeviceContext* pDC_d3d11 = pGC->d3d11();
//                pDC_d3d11->End(tqd.m_d3d._11.m_pTimerQuery);
                // TODO
            }
            break;
            case nv_water_d3d_api_gnm:
            {
                throw new UnsupportedOperationException();
            }
            case nv_water_d3d_api_gl2:
            {
                TimerQueryData tqd = m_pTimersPool.addInactiveQuery();
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

    long waitTimerQuery(int ix){
// No built-in sync in DX, roll our own as best we can...
//        HRESULT status = S_FALSE;
//        do
//        {
//            status = getTimerQuery(pGC, ix, t);
//            if(S_FALSE == status)
//            {
//                Sleep(0);
//            }
//        }
//        while(S_FALSE == status);
//
//        return status;

        return 0;
    }

    long getTimerQuery(/*Graphics_Context* pGC,*/ int ix){
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
                    result = gl.glGetQueryObjectui64ui(tqd.m_GLTimerQuery, GL_QUERY_RESULT_AVAILABLE);
                    if(result == GL_FALSE)
                    {
                        hr = HRESULT.S_FALSE;
                    }
                    else
                    {
                        result = gl.glGetQueryObjectui64ui(tqd.m_GLTimerQuery, GL_QUERY_RESULT);
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
                    result = gl.glGetQueryObjectui64ui(tqd.m_GLTimerQuery, GL_QUERY_RESULT_AVAILABLE);
                    if(result == GL_FALSE)
                    {
                        hr = HRESULT.S_FALSE;
                    }
                    else
                    {
                        result = gl.glGetQueryObjectui64ui(tqd.m_GLTimerQuery, GL_QUERY_RESULT);
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
        }

        return tqd.m_timestampResult;
    }

    // Pair-wise get/wait
    long getTimerQueries(/*Graphics_Context* pGC,*/ int ix1, int ix2/*, UINT64& tdiff*/){
//        UINT64 stamp1;
//        HRESULT hr1 = getTimerQuery(pGC, ix1, stamp1);
//        if(S_FALSE == hr1)
//            return S_FALSE;
//        UINT64 stamp2;
//        HRESULT hr2 = getTimerQuery(pGC, ix2, stamp2);
//        if(S_FALSE == hr2)
//            return S_FALSE;
//
//        if(S_OK == hr1 && S_OK ==hr2)
//        {
//            tdiff = stamp2 - stamp1;
//            return S_OK;
//        }
//        else if(S_OK == hr1)
//        {
//            return hr2;
//        }
//        else
//        {
//            return hr1;
//        }

        return 0;
    }

    long waitTimerQueries(/*Graphics_Context* pGC,*/ int ix1, int ix2/*, UINT64& tdiff*/){
// No built-in sync in DX, roll our own as best we can...
//        HRESULT status = S_FALSE;
//        do
//        {
//            status = getTimerQueries(pGC, ix1, ix2, tdiff);
//            if(S_FALSE == status)
//            {
//                Sleep(0);
//            }
//        }
//        while(S_FALSE == status);
//
//        return status;
        return 0;
    }

    // Disjoint queries wrapper
    HRESULT beginDisjoint(){

        return null;
    }
    HRESULT endDisjoint(){
        return null;

    }

    int getCurrentDisjointQuery(){
        return 9;
    }
    void releaseDisjointQuery(int ix){}
    HRESULT waitDisjointQuery(/*Graphics_Context* pGC,*/ int ix/*, UINT64& f*/) { return null;}
    HRESULT getDisjointQuery(/*Graphics_Context* pGC,*/ int ix/*, UINT64& f*/){
        return null;
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
//                    SAFE_RELEASE(dqd.m_d3d._11.m_pDisjointTimerQuery); TODO
                }
                for(int i = 0; i != m_pTimersPool.getNumQueries(); ++i)
                {
                    TimerQueryData tqd = m_pTimersPool.getQueryData(i);
//                    SAFE_RELEASE(tqd.m_d3d._11.m_pTimerQuery); TODO
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

    private void releaseAll(){
        releaseAllResources();
        m_d3dAPI = nv_water_d3d_api_undefined;
    }
}
