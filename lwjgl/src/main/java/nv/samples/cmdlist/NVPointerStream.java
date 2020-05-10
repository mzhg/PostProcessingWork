package nv.samples.cmdlist;

final class NVPointerStream {

    int          m_max;
    int          m_begin;
    int          m_end;
    int          m_cur;

    byte[]       m_data;

    void init(byte[] data, int start, int size)
    {
        m_data  = data;
        m_begin = start;
        m_end   = m_begin + size;
        m_cur   = m_begin;
        m_max   = size;
    }

    int size()
    {
        return m_cur - m_begin;
    }

    int  capacity()
    {
        return m_max;
    }
}
