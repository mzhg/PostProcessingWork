package jet.opengl.demos.nvidia.illumination;

import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.ReadableVector3f;

import jet.opengl.postprocessing.common.Disposeable;
import jet.opengl.postprocessing.texture.TextureGL;
import jet.opengl.postprocessing.util.CommonUtil;

/**
 * Created by Administrator on 2017/11/12 0012.
 */

interface RTCollection extends Disposeable{
//    SimpleRT[] m_collection;
//    int m_levels;

    /*RTCollection()
    {
        m_collection = null;
        m_levels = 0;
    }

    @Override
    public void dispose() {
        for(int i=0;i<m_levels;i++)
            CommonUtil.safeRelease(m_collection[i]);
        m_collection = null;
    }*/

    int Create2D(int levels, /*ID3D11Device* pd3dDevice,*/ int width2D, int height2D, int width3D, int height3D, int depth3D, int format, boolean uav /*= false*/ );

    int Create3D( int levels, /*ID3D11Device* pd3dDevice,*/ int width, int height, int depth, int format, boolean uav /*= false*/ );

    int Create2DArray( int levels, /*ID3D11Device* pd3dDevice,*/ int width, int height, int depth, int format, boolean uav /*= false*/ );

    void setLPVTransformsRotatedAndOffset(float LPVscale, ReadableVector3f LPVtranslate, Matrix4f cameraViewMatrix, ReadableVector3f viewVector);

/*
    ID3D11Texture2D* get2DTexture(int level, int rt = 0)
    {
        return m_collection[level]->get_pTex2D(rt);
    }

    ID3D11Texture3D* get3DTexture(int level, int rt = 0)
    {
        return m_collection[level]->get_pTex3D(rt);
    }*/

    default TextureGL getShaderResourceView(int level, int rt /*= 0*/)
    {
        return getRenderTarget(level).get_pSRV(rt);
    }

    default TextureGL getShaderResourceViewpp(int level, int rt/* = 0*/)
    {
        return getRenderTarget(level).get_ppSRV(rt);
    }

    default TextureGL getRenderTargetView(int level, int rt /*= 0*/)
    {
        return getRenderTarget(level).get_pRTV(rt);
    }

    SimpleRT getRenderTarget(int level);// {return m_collection[level]; }

    int getNumLevels();
}
