package jet.opengl.demos.nvidia.waves;

/**
 * Created by mazhen'gui on 2017/7/22.
 */

final class CircularFIFO<T> {
    private T[] m_ptr;
    private int m_capacity;
    private int m_range_begin_index;
    private int m_range_count;

    CircularFIFO(int capacity, GFSDK_WaveWorks_GFX_Query_Pool_Impl.PoolObjectFactory<T> factory){
        m_ptr = (T[]) new Object[capacity];
        m_capacity = capacity;

        for(int i = 0; i < capacity; i++){
            m_ptr[i] = factory.createObject();
        }
    }

    int capacity()  { return m_capacity; }
    int range_count()  { return m_range_count; }

    T raw_at(int ix)
    {
        assert(ix < m_capacity);
        return m_ptr[ix];
    }

    // NB: ix = 0 means 'most-recently-added', hence the reverse indexing...
    T range_at(int ix)
    {
        assert(ix < m_range_count);
        return m_ptr[(m_range_begin_index+m_range_count-ix-1)%m_capacity];
    }

    // Recycles the oldest entry in the FIFO if necessary
    T consume_one()
    {
        assert(m_capacity > 0);

        if(m_capacity == m_range_count)
        {
            // The FIFO is full, so free up the oldest entry
            m_range_begin_index = (m_range_begin_index+1) % m_capacity;
            --m_range_count;
        }

        final int raw_result_index = (m_range_begin_index+m_range_count) % m_capacity;
        ++m_range_count;

        return m_ptr[raw_result_index];
    }
}
