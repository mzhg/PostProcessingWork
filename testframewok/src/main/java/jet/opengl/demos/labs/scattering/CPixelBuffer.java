package jet.opengl.demos.labs.scattering;

import java.nio.ByteBuffer;

import jet.opengl.postprocessing.common.GLenum;

/*******************************************************************************
 * Class: CPixelBuffer
 ********************************************************************************
 * This class implements a general-purpose pixel buffer to be used for anything.
 * It is often used by CTexture to set up OpenGL textures, so many of the
 * parameters you use to initialize it look like the parameters you would pass
 * to glTexImage1D or glTexImage2D. Some of the standard pixel buffer routines
 * call fast MMX functions implemented in PixelBuffer.asm.
 *******************************************************************************/
final class CPixelBuffer extends C3DBuffer{
    int m_nFormat;				// The format of the pixel data (i.e. GL_LUMINANCE, GL_RGBA)

    void Init(int nWidth, int nHeight, int nDepth, int nChannels/*=3*/, int nFormat/*=GL_RGB*/, int nDataType/*=GL_UNSIGNED_BYTE*/, ByteBuffer pBuffer) {
        super.Init(nWidth, nHeight, nDepth, nDataType, nChannels, pBuffer);
        m_nFormat = nFormat;
    }

    void Init(int nWidth, int nHeight, int nDepth, int nChannels/*=3*/, int nFormat/*=GL_RGB*/){
        this.Init(nWidth, nHeight, nDepth, nChannels, nFormat, GLenum.GL_UNSIGNED_BYTE, null);
        m_nFormat = nFormat;
    }

    void MakeCloudCell(float fExpose, float fSizeDisc) {
        int i;
        int n = 0;
        byte nIntensity;
        for(int y=0; y<m_nHeight; y++)
        {
            float fDy = (y+0.5f)/m_nHeight - 0.5f;
            for(int x=0; x<m_nWidth; x++)
            {
                float fDx = (x+0.5f)/m_nWidth - 0.5f;
                float fDist = (float)Math.sqrt(fDx*fDx + fDy*fDy);
                float fIntensity = (float) (2.0 - Math.min(2.0, Math.pow(2.0, Math.max(fDist-fSizeDisc,0.0)*fExpose)));
                switch(m_nDataType)
                {
                    case GLenum.GL_UNSIGNED_BYTE:
                        nIntensity = (byte)(fIntensity*255 + 0.5f);
                        for(i=0; i<m_nChannels; i++)
//                            ((unsigned char *)m_pBuffer)[n++] = nIntensity;
                            m_pBuffer.put(n++, nIntensity);
                        break;
                    case GLenum.GL_FLOAT:
                        for(i=0; i<m_nChannels; i++)
//                            ((float *)m_pBuffer)[n++] = fIntensity;
                            m_pBuffer.putFloat(4 * n++, fIntensity);
                        break;
                }
            }
        }
    }

    void MakeGlow1D() {
        int nIndex=0;
        for(int x=0; x<m_nWidth; x++) {
            float fIntensity = (float) Math.pow((float)x / m_nWidth, 0.75f);
            for(int i=0; i<m_nChannels-1; i++)
//                ((unsigned char *)m_pBuffer)[nIndex++] = (unsigned char)255;
                m_pBuffer.put(nIndex++, (byte)255);
//            ((unsigned char *)m_pBuffer)[nIndex++] = (unsigned char)(fIntensity*255 + 0.5f);
            m_pBuffer.put(nIndex++, (byte)(fIntensity*255 + 0.5f));
        }
    }

    void MakeGlow2D(float fExposure, float fRadius)
    {
        int nIndex=0;
        for(int y=0; y<m_nHeight; y++)
        {
            for(int x=0; x<m_nWidth; x++)
            {
                float fX = ((m_nWidth-1)*0.5f - x) / (float)(m_nWidth-1);
                float fY = ((m_nHeight-1)*0.5f - y) / (float)(m_nHeight-1);
                float fDist = Math.max(0.0f, (float)Math.sqrt(fX*fX + fY*fY) - fRadius);

                float fIntensity = (float)Math.exp(-fExposure * fDist);
                byte c = (byte)(fIntensity*192 + 0.5f);
                for(int i=0; i<m_nChannels; i++)
//                    ((unsigned char *)m_pBuffer)[nIndex++] = c;
                    m_pBuffer.put(nIndex++, c);
            }
        }
    }
}
