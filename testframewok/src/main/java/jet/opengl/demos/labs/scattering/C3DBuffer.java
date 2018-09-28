package jet.opengl.demos.labs.scattering;

import java.nio.ByteBuffer;

import jet.opengl.postprocessing.texture.TextureUtils;
import jet.opengl.postprocessing.util.BufferUtils;

class C3DBuffer {
    int m_nWidth;				// The width of the buffer (x axis)
    int m_nHeight;				// The height of the buffer (y axis)
    int m_nDepth;				// The depth of the buffer (z axis)
    int m_nDataType;			// The data type stored in the buffer (i.e. GL_UNSIGNED_BYTE, GL_FLOAT)
    int m_nChannels;			// The number of channels of data stored in the buffer
    int m_nElementSize;			// The size of one element in the buffer
    ByteBuffer m_pBuffer;	    // The pointer to the pixel buffer

    void Init(int nWidth,int nHeight, int nDepth, int nDataType, int nChannels, ByteBuffer pBuffer)
    {
        // If the buffer is already initialized to the specified settings, then nothing needs to be done
        if(m_pBuffer != null && m_nWidth == nWidth && m_nHeight == nHeight && m_nDataType == nDataType && m_nChannels == nChannels)
            return;

        Cleanup();
        m_nWidth = nWidth;
        m_nHeight = nHeight;
        m_nDepth = nDepth;
        m_nDataType = nDataType;
        m_nChannels = nChannels;
        m_nElementSize = m_nChannels * TextureUtils.measureDataTypeSize(m_nDataType);
        if(pBuffer != null)
            m_pBuffer = pBuffer;
        else
        {
//            m_pAlloc = new unsigned char[GetBufferSize() + ALIGN_MASK];
//            m_pBuffer = (void *)ALIGN(m_pAlloc);
            m_pBuffer = BufferUtils.createByteBuffer(GetBufferSize() /*+ ALIGN_MASK*/);
        }
    }

    void Cleanup()
    {
        m_pBuffer = null;
    }

    int GetWidth()  		{ return m_nWidth; }
    int GetHeight() 		{ return m_nHeight; }
    int GetDepth() 		{ return m_nDepth; }
    int GetDataType() 		{ return m_nDataType; }
    int GetChannels() 		{ return m_nChannels; }
    int GetBufferSize() 	{ return m_nWidth * m_nHeight * m_nDepth * m_nElementSize; }
    ByteBuffer GetBuffer() 		{ return m_pBuffer; }
}
