package jet.opengl.demos.intel.va;

import org.lwjgl.util.vector.Readable;
import org.lwjgl.util.vector.Writable;

import java.io.IOException;
import java.nio.ByteBuffer;

import jet.opengl.postprocessing.util.BufferUtils;
import jet.opengl.postprocessing.util.Numeric;

/**
 * Created by mazhen'gui on 2017/11/17.
 */

public class VaMemoryStream extends VaStream {

    private ByteBuffer m_buffer;
    private int        m_bufferSize;
    private int        m_pos;
    //
    private boolean                    m_autoBuffer;
//    int64                   m_autoBufferCapacity;
    private final byte[] m_Caches = new byte[8];

    public VaMemoryStream( ByteBuffer buffer, int bufferSize ){
        m_buffer = buffer;
        m_bufferSize = bufferSize;
//        m_autoBufferCapacity = 0;
        m_pos = 0;
        m_autoBuffer = false;
    }

    public VaMemoryStream( int initialSize){
        this(initialSize, 64);
    }

    public VaMemoryStream( int initialSize, int reserve /*= 64*/ ){
        reserve = Math.max( reserve, initialSize );
        assert( reserve > 0 );
        assert( reserve >= initialSize );
        m_buffer = BufferUtils.createByteBuffer(reserve);
//        m_autoBufferCapacity = reserve;
        m_bufferSize = initialSize;
        m_pos = 0;
        m_autoBuffer = true;
    }

    @Override
    public boolean IsOpen() {
        return m_buffer != null;
    }

    @Override
    public int GetLength() throws IOException {
        return m_buffer.capacity();
    }

    @Override
    public int GetPosition() throws IOException {
        return m_pos;
    }

    @Override
    public boolean CanSeek() {
        return true;
    }

    @Override
    public void Seek(int position) throws IOException {
        if(position < 0 || position >= m_buffer.capacity())
            throw new IndexOutOfBoundsException();

        m_pos = position;
    }

    @Override
    public void Truncate() throws IOException {}

    @Override
    public int Read(byte[] buffer, int offset, int count) throws IOException {
        /*assert( outCountRead == NULL ); // not implemented!
        outCountRead;*/

        if( count + m_pos > m_bufferSize )
        {
            assert( false ); // buffer overrun, should handle this differently now that we've got outCountRead
            throw new IOException("Buffer under flow");
            //count = m_bufferSize - m_pos;
        }

//        memcpy(buffer, m_buffer + m_pos, count);
        m_buffer.position(m_pos);
        m_buffer.get(buffer, offset, count);

        m_pos += count;

        return count;
    }

    @Override
    public int Write(byte[] buffer, int offset, int count) throws IOException {
        if( (count + m_pos) > m_bufferSize )
        {
            if( m_autoBuffer )
            {
                int bufferCapacity = m_buffer.capacity();
                if( (count + m_pos) > bufferCapacity )
                {
                    Grow( count + m_pos + bufferCapacity );
                }
                m_bufferSize = count + m_pos;
            }
            else
            {
                // buffer overrun, should handle this differently now that we've got
                throw new IllegalStateException();
            }
        }

//        memcpy(m_buffer + m_pos, buffer, count);
        m_buffer.position(m_pos);
        m_buffer.put(buffer, offset, count);

        m_pos += count;

        return count;

    }

    private void Grow(int newCapacity){
        if(newCapacity > m_buffer.capacity()){
            ByteBuffer newBuffer = BufferUtils.createByteBuffer(newCapacity);
            m_buffer.position(0).limit(m_buffer.capacity());
            newBuffer.put(m_buffer);

            m_buffer = newBuffer;
        }
    }

    public void Resize( int newSize )
    {
        if( m_autoBuffer )
        {
            int bufferCapacity = m_buffer.capacity();
            if( newSize > bufferCapacity )
            {
                Grow( newSize );
            }
            m_bufferSize = newSize;
        }
        else
        {
            // buffer overrun
            assert( false );
            return;
        }
    }

    @Override
    public long ReadLong() throws IOException {
        Read(m_Caches, 0, 8);
        return Numeric.getLong(m_Caches, 0);
    }

    @Override
    public byte Read() throws IOException {
        Read(m_Caches, 0, 1);
        return m_Caches[0];
    }

    @Override
    public short ReadShort() throws IOException {
        Read(m_Caches, 0, 2);
        return Numeric.getShort(m_Caches, 0);
    }

    @Override
    public int ReadInt() throws IOException {
        Read(m_Caches, 0, 4);
        return Numeric.getInt(m_Caches, 0);
    }

    @Override
    public float ReadFloat() throws IOException {
        Read(m_Caches, 0, 4);
        return Numeric.getFloat(m_Caches, 0);
    }

    @Override
    public void ReadObject(int size, Writable obj) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean WriteLong(long value) throws IOException {
        Numeric.getBytes(value, m_Caches, 0);
        return Write(m_Caches, 0, 8) > 0;
    }

    @Override
    public boolean Write(byte value) throws IOException {
        m_Caches[0] = value;
        return Write(m_Caches, 0, 1) > 0;
    }

    @Override
    public boolean WriteShort(short value) throws IOException {
        Numeric.getBytes(value, m_Caches, 0);
        return Write(m_Caches, 0, 2) > 0;
    }

    @Override
    public boolean WriteInt(int value) throws IOException {
        Numeric.getBytes(value, m_Caches, 0);
        return Write(m_Caches, 0, 4) > 0;
    }

    @Override
    public boolean WriteFloat(float value) throws IOException {
        Numeric.getBytes(value, m_Caches, 0);
        return Write(m_Caches, 0, 4) > 0;
    }

    @Override
    public boolean WriteObject(int size, Readable obj) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void close() throws IOException {

    }
}
