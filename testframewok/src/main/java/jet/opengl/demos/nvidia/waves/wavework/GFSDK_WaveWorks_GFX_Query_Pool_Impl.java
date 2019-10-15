package jet.opengl.demos.nvidia.waves.wavework;

/**
 * Created by mazhen'gui on 2017/7/22.
 */

class GFSDK_WaveWorks_GFX_Query_Pool_Impl<QueryDataType extends QueryData> {
    private QueryDataType[] m_pQueriesData;
    private int m_NumQueries;

    private int[] m_pInactiveQueries;
    private int m_NumInactiveQueries;

    private PoolObjectFactory<QueryDataType> m_factorty;

    GFSDK_WaveWorks_GFX_Query_Pool_Impl(PoolObjectFactory<QueryDataType> factorty) {m_factorty = factorty;}

    int getNumQueries() { return m_pQueriesData != null ? m_pQueriesData.length : 0; }
    int getNumInactiveQueries() { return m_NumInactiveQueries; }

    QueryDataType addInactiveQuery(){
        int newQueryIndex = m_NumQueries;
        int newNumQueries = m_NumQueries + 1;
        QueryDataType[] pNewDatas = (QueryDataType[]) new QueryData[newNumQueries];
        int[] pNewInactiveQueries = new int[newNumQueries];

//        memcpy(pNewDatas, m_pQueriesData, m_NumQueries * sizeof(m_pQueriesData[0]));
//        memcpy(pNewInactiveQueries, m_pInactiveQueries, m_NumInactiveQueries * sizeof(m_pInactiveQueries[0]));
        if(m_pQueriesData != null){
            System.arraycopy(m_pQueriesData, 0, pNewDatas, 0,m_NumQueries);
            System.arraycopy(m_pInactiveQueries, 0, pNewInactiveQueries, 0,m_NumInactiveQueries);
        }

        m_pQueriesData = pNewDatas;
        m_pInactiveQueries = pNewInactiveQueries;

        // Fixup newbies
        if(m_pQueriesData[newQueryIndex] == null)
            m_pQueriesData[newQueryIndex] = m_factorty.createObject();
        m_pQueriesData[newQueryIndex].m_refCount = 0;
        m_pInactiveQueries[m_NumInactiveQueries] = newQueryIndex;
        ++m_NumInactiveQueries;

        m_NumQueries = newNumQueries;

        return m_pQueriesData[newQueryIndex];
    }

    int activateQuery(){
        assert(m_NumInactiveQueries > 0);

        --m_NumInactiveQueries;

        int result = m_pInactiveQueries[m_NumInactiveQueries];
        m_pQueriesData[result].m_status = HRESULT.S_FALSE;
        m_pQueriesData[result].m_refCount = 1;

        return result;
    }

    void releaseQuery(int ix){
        assert(ix < m_NumQueries);
        assert(m_pQueriesData[ix].m_refCount > 0);

        --m_pQueriesData[ix].m_refCount;
        if(0 == m_pQueriesData[ix].m_refCount)
        {
            // return to inactive pool
            assert(m_NumInactiveQueries < m_NumQueries);
            m_pInactiveQueries[m_NumInactiveQueries] = ix;
            ++m_NumInactiveQueries;
        }
    }

    void addRefQuery(int ix){
        assert(ix < m_NumQueries);
        assert(m_pQueriesData[ix].m_refCount > 0);	// Because it is invalid to use a zero-ref'd query

        ++m_pQueriesData[ix].m_refCount;
    }

    QueryDataType getQueryData(int ix){
        assert(ix < m_NumQueries);

        return m_pQueriesData[ix];
    }

    interface PoolObjectFactory<T>{
        public T createObject();
    }
}
