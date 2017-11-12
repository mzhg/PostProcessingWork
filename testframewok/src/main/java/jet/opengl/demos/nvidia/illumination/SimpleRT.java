package jet.opengl.demos.nvidia.illumination;

import org.lwjgl.util.vector.Vector2i;

import jet.opengl.postprocessing.common.Disposeable;
import jet.opengl.postprocessing.texture.Texture2D;
import jet.opengl.postprocessing.texture.Texture2DDesc;
import jet.opengl.postprocessing.texture.Texture3D;
import jet.opengl.postprocessing.texture.Texture3DDesc;
import jet.opengl.postprocessing.texture.TextureGL;
import jet.opengl.postprocessing.texture.TextureUtils;
import jet.opengl.postprocessing.util.CommonUtil;

/**
 * Created by Administrator on 2017/11/12 0012.
 */

final class SimpleRT extends BasicLPVTransforms implements Disposeable{
    //static int g_numResources = 0;
    static int g_numResourcesRT = 0;
    static int g_numResources2D = 0;
    static int g_numResources3D = 0;
    static int g_numResourcesRTV = 0;
    static int g_numResourcesUAV = 0;

    private int m_numRTs;
    int m_width3D;
    int m_height3D;
    int m_depth3D;

    private int m_width2D;
    private int m_height2D;

    private boolean m_use2DTex = true;
    private int m_numCols;
    private int m_numRows;

    private int m_numChannels;
    private Texture2D[]             m_pTex2Ds;
    private Texture3D[]             m_pTex3Ds;
    private TextureGL[]     m_pRTVs;
    private TextureGL[]  m_pUAVs;


    int getNumRTs() { return m_numRTs; }
    TextureGL[]    m_pSRVs;
    void create(int numRTs)
    {
        g_numResourcesRT += numRTs;

        m_numRTs = numRTs;
        m_pSRVs = new TextureGL[m_numRTs];
        for(int i=0; i<numRTs; i++)
            m_pSRVs[i] = null;
    }

    TextureGL get_ppSRV(int rt)
    {
        return (m_numRTs > rt) ? m_pSRVs[rt]  : null;
    }
    TextureGL get_pSRV(int rt)
    {
        return (m_numRTs > rt) ? m_pSRVs[rt]  : null;
    }

    private void createResources(int numRTs)
    {
        g_numResources2D += numRTs;
        g_numResources3D += numRTs;
        g_numResourcesRTV += numRTs;
        g_numResourcesUAV += numRTs;

        create(numRTs);
        m_pTex2Ds = new Texture2D[numRTs];
        m_pTex3Ds = new Texture3D[numRTs];
        m_pRTVs =   new TextureGL[numRTs];
        m_pUAVs =   new TextureGL[numRTs];
        for(int i=0; i<numRTs; i++)
        {
            m_pTex2Ds[i] = null;
            m_pTex3Ds[i] = null;
            m_pRTVs[i] = null;
            m_pUAVs[i] = null;
        }
    }

    private void setNumChannels(int format)
    {
        /*if(format==DXGI_FORMAT_R32G32B32A32_TYPELESS || format==DXGI_FORMAT_R32G32B32A32_FLOAT  || format==DXGI_FORMAT_R32G32B32A32_UINT || format==DXGI_FORMAT_R32G32B32A32_SINT ||
                format==DXGI_FORMAT_R16G16B16A16_TYPELESS || format==DXGI_FORMAT_R16G16B16A16_FLOAT || format==DXGI_FORMAT_R16G16B16A16_UNORM || format==DXGI_FORMAT_R16G16B16A16_UINT || format==DXGI_FORMAT_R16G16B16A16_SNORM || format==DXGI_FORMAT_R16G16B16A16_SINT ||
                format==DXGI_FORMAT_R8G8B8A8_TYPELESS || format==DXGI_FORMAT_R8G8B8A8_UNORM || format==DXGI_FORMAT_R8G8B8A8_UNORM_SRGB || format==DXGI_FORMAT_R8G8B8A8_UINT || format==DXGI_FORMAT_R8G8B8A8_SNORM || format==DXGI_FORMAT_R8G8B8A8_SINT)
            m_numChannels = 4;
        else if(format==DXGI_FORMAT_R32G32B32_TYPELESS || format==DXGI_FORMAT_R32G32B32_FLOAT || format==DXGI_FORMAT_R32G32B32_UINT || format==DXGI_FORMAT_R32G32B32_SINT)
            m_numChannels = 3;
        else if(format==DXGI_FORMAT_R32G32_TYPELESS || format==DXGI_FORMAT_R32G32_FLOAT || format==DXGI_FORMAT_R32G32_UINT || format==DXGI_FORMAT_R32G32_SINT ||
                format==DXGI_FORMAT_R16G16_TYPELESS || format==DXGI_FORMAT_R16G16_FLOAT || format==DXGI_FORMAT_R16G16_UNORM || format==DXGI_FORMAT_R16G16_UINT || format==DXGI_FORMAT_R16G16_SNORM || format==DXGI_FORMAT_R16G16_SINT ||
                format==DXGI_FORMAT_R8G8_TYPELESS || format==DXGI_FORMAT_R8G8_UNORM || format==DXGI_FORMAT_R8G8_UINT || format==DXGI_FORMAT_R8G8_SNORM || format==DXGI_FORMAT_R8G8_SINT)
            m_numChannels = 2;
        else if(format==DXGI_FORMAT_R32_TYPELESS || format==DXGI_FORMAT_D32_FLOAT || format==DXGI_FORMAT_R32_FLOAT || format==DXGI_FORMAT_R32_UINT || format==DXGI_FORMAT_R32_SINT ||
                format==DXGI_FORMAT_R16_TYPELESS || format==DXGI_FORMAT_R16_FLOAT || format==DXGI_FORMAT_D16_UNORM || format==DXGI_FORMAT_R16_UNORM || format==DXGI_FORMAT_R16_UINT || format==DXGI_FORMAT_R16_SNORM || format==DXGI_FORMAT_R16_SINT ||
                format==DXGI_FORMAT_R8_TYPELESS || format==DXGI_FORMAT_R8_UNORM || format==DXGI_FORMAT_R8_UINT || format==DXGI_FORMAT_R8_SNORM || format==DXGI_FORMAT_R8_SINT || format==DXGI_FORMAT_A8_UNORM)
            m_numChannels = 1;
        else
            m_numChannels = -1; //didnt recognize the format, please add the format to the list above*/

        m_numChannels = TextureUtils.getFormatChannels(format);
    }

    int getWidth2D() {return m_width2D;}
    int getHeight2D() {return m_height2D;}

    int getWidth3D() {return m_width3D;}
    int getHeight3D() {return m_height3D;}
    int getDepth3D() {return m_depth3D;}

    int getNumCols() {return m_numCols;}
    int getNumRows() {return m_numRows;}

    int getNumChannels() { return m_numChannels; }

    boolean is2DTexture() { return m_use2DTex; }

    void Create2D(int width2D, int height2D, int width3D, int height3D, int depth3D, int format){
        Create2D(width2D, height2D, width3D, height3D, depth3D, format, false, 1);
    }
    void Create2D( /*ID3D11Device* pd3dDevice,*/ int width2D, int height2D, int width3D, int height3D, int depth3D, int format, boolean uav /*= false*/, int numRTs /*= 1*/ )
    {
//        HRESULT hr;

        m_use2DTex = true;

        createResources(numRTs);
        setNumChannels(format);

        m_width2D = width2D;
        m_height2D = height2D;
        m_width3D = width3D;
        m_height3D = height3D;
        m_depth3D = depth3D;
        Vector2i result = new Vector2i();
        Grid.ComputeRowsColsForFlat3DTexture( m_depth3D, /*m_numCols, m_numRows*/ result );
        m_numCols = result.x;
        m_numRows = result.y;

        Texture2DDesc desc = new Texture2DDesc();
//        ZeroMemory( &desc, sizeof( D3D11_TEXTURE2D_DESC ) );
        desc.arraySize = 1;
//        desc.BindFlags = D3D11_BIND_SHADER_RESOURCE | D3D11_BIND_RENDER_TARGET;
//        if (uav) desc.BindFlags |= D3D11_BIND_UNORDERED_ACCESS;
//        desc.Usage = D3D11_USAGE_DEFAULT;
        desc.format = format;
        desc.width = width2D;
        desc.height = height2D;
        desc.mipLevels = 1;
        desc.sampleCount = 1;
        for(int i=0; i<m_numRTs; i++)
//            V_RETURN( pd3dDevice->CreateTexture2D( &desc, NULL, &m_pTex2Ds[i] ) );
            m_pTex2Ds[i] = TextureUtils.createTexture2D(desc, null);

        // create the shader resource view
        /*D3D11_SHADER_RESOURCE_VIEW_DESC descSRV;
        descSRV.Format = desc.Format;
        descSRV.ViewDimension = D3D11_SRV_DIMENSION_TEXTURE2D;
        descSRV.Texture2D.MipLevels = 1;
        descSRV.Texture2D.MostDetailedMip = 0;*/
        for(int i=0; i<m_numRTs; i++)
//            V_RETURN( pd3dDevice->CreateShaderResourceView( m_pTex2Ds[i], &descSRV, &m_pSRVs[i] ) );
            m_pSRVs[i] = m_pTex2Ds[i];

        // create the render target view
        /*D3D11_RENDER_TARGET_VIEW_DESC descRT;
        descRT.Format = desc.Format;
        descRT.ViewDimension = D3D11_RTV_DIMENSION_TEXTURE2D;
        descRT.Texture2D.MipSlice = 0;*/
        for(int i=0; i<m_numRTs; i++)
//            V_RETURN( pd3dDevice->CreateRenderTargetView( m_pTex2Ds[i], &descRT, &m_pRTVs[i] ) );
            m_pRTVs[i] = m_pTex2Ds[i];

        if (uav)
        {
            // create the unordered access view
            /*D3D11_UNORDERED_ACCESS_VIEW_DESC descUAV;
            descUAV.Format = desc.Format;
            descUAV.ViewDimension = D3D11_UAV_DIMENSION_TEXTURE2D;
            descUAV.Texture2D.MipSlice = 0;*/
            for(int i=0; i<m_numRTs; i++)
//                V_RETURN( pd3dDevice->CreateUnorderedAccessView( m_pTex2Ds[i], &descUAV, &m_pUAVs[i] ) );
                m_pUAVs[i] = m_pTex2Ds[i];
        }

//        return S_OK;
    }

    void Create2DArray( /*ID3D11Device* pd3dDevice,*/ int width3D, int height3D, int depth3D, int format){
        Create2DArray(width3D, height3D, depth3D, format, false, 1);
    }
    void Create2DArray( /*ID3D11Device* pd3dDevice,*/ int width3D, int height3D, int depth3D, int format, boolean uav /*= false*/, int numRTs /*= 1*/  )
    {

//        HRESULT hr;

        m_use2DTex = false;

        createResources(numRTs);
        setNumChannels(format);

        m_width3D = width3D;
        m_height3D = height3D;
        m_depth3D = depth3D;
//        ComputeRowColsForFlat3DTexture( m_depth3D, m_numCols, m_numRows );
        Vector2i result = new Vector2i();
        Grid.ComputeRowsColsForFlat3DTexture( m_depth3D, /*m_numCols, m_numRows*/ result );
        m_numCols = result.x;
        m_numRows = result.y;
        m_width2D = m_numCols*width3D;
        m_height2D = m_numRows*height3D;

        //create the texture
        Texture2DDesc desc = new Texture2DDesc();
        /*ZeroMemory( &desc, sizeof( D3D11_TEXTURE2D_DESC ) );
        desc.BindFlags = D3D11_BIND_SHADER_RESOURCE | D3D11_BIND_RENDER_TARGET;
        if (uav) desc.BindFlags |= D3D11_BIND_UNORDERED_ACCESS;
        desc.Usage = D3D11_USAGE_DEFAULT;*/
        desc.format = format;
        desc.width = width3D;
        desc.height = height3D;
        desc.arraySize = depth3D;
        desc.mipLevels = 1;
        desc.sampleCount = 1;
        for(int i=0; i<m_numRTs; i++)
//            V_RETURN( pd3dDevice->CreateTexture2D( &desc, NULL, &m_pTex2Ds[i] ) );
            m_pTex2Ds[i] = TextureUtils.createTexture2D(desc, null);

        // create the shader resource view
        /*D3D11_SHADER_RESOURCE_VIEW_DESC descSRV;
        descSRV.Format = desc.Format;
        descSRV.ViewDimension = D3D11_SRV_DIMENSION_TEXTURE2DARRAY;
        descSRV.Texture2DArray.ArraySize = depth3D;
        descSRV.Texture2DArray.FirstArraySlice = 0;
        descSRV.Texture2DArray.MipLevels = 1;
        descSRV.Texture2DArray.MostDetailedMip = 0;*/
        for(int i=0; i<m_numRTs; i++)
//            V_RETURN( pd3dDevice->CreateShaderResourceView( m_pTex2Ds[i], &descSRV, &m_pSRVs[i] ) );
            m_pSRVs[i] = m_pTex2Ds[i];

        // create the render target view
        /*D3D11_RENDER_TARGET_VIEW_DESC descRT;
        descRT.Format = desc.Format;
        descRT.ViewDimension = D3D11_RTV_DIMENSION_TEXTURE2DARRAY;
        descRT.Texture2DArray.ArraySize = depth3D;
        descRT.Texture2DArray.MipSlice = 0;
        descRT.Texture2DArray.FirstArraySlice = 0;*/
        for(int i=0; i<m_numRTs; i++)
//            V_RETURN( pd3dDevice->CreateRenderTargetView( m_pTex2Ds[i], &descRT, &m_pRTVs[i] ) );
            m_pRTVs[i] = m_pTex2Ds[i];

        if (uav)
        {
            // create the unordered access view
            /*D3D11_UNORDERED_ACCESS_VIEW_DESC descUAV;
            descUAV.Format = desc.Format;
            descUAV.ViewDimension = D3D11_UAV_DIMENSION_TEXTURE2DARRAY;
            descUAV.Texture2DArray.ArraySize = depth3D;
            descUAV.Texture2DArray.FirstArraySlice = 0;
            descUAV.Texture2DArray.MipSlice = 0;*/
            for(int i=0; i<m_numRTs; i++)
//                V_RETURN( pd3dDevice->CreateUnorderedAccessView( m_pTex2Ds[i], &descUAV, &m_pUAVs[i] ) );
                m_pUAVs[i] = m_pTex2Ds[i];
        }

//        return S_OK;
    }

    void Create3D( /*ID3D11Device* pd3dDevice,*/ int width3D, int height3D, int depth3D, int format, boolean uav /*= false*/, int numRTs /*= 1*/  )
    {
//        HRESULT hr;

        m_use2DTex = false;

        createResources(numRTs);
        setNumChannels(format);

        m_width3D = width3D;
        m_height3D = height3D;
        m_depth3D = depth3D;
//        ComputeRowColsForFlat3DTexture( m_depth3D, m_numCols, m_numRows );
        Vector2i result = new Vector2i();
        Grid.ComputeRowsColsForFlat3DTexture( m_depth3D, /*m_numCols, m_numRows*/ result );
        m_numCols = result.x;
        m_numRows = result.y;
        m_width2D = m_numCols*width3D;
        m_height2D = m_numRows*height3D;


        Texture3DDesc desc = new Texture3DDesc();
        /*ZeroMemory( &desc, sizeof( D3D11_TEXTURE3D_DESC ) );
        desc.BindFlags = D3D11_BIND_SHADER_RESOURCE | D3D11_BIND_RENDER_TARGET;
        if (uav) desc.BindFlags |= D3D11_BIND_UNORDERED_ACCESS;
        desc.Usage = D3D11_USAGE_DEFAULT;*/
        desc.format = format;
        desc.width = width3D;
        desc.height = height3D;
        desc.depth = depth3D;
        desc.mipLevels = 1;
        for(int i=0; i<m_numRTs; i++)
//            V_RETURN( pd3dDevice->CreateTexture3D( &desc, NULL, &m_pTex3Ds[i] ) );
            m_pTex3Ds[i] = TextureUtils.createTexture3D(desc, null);

        // create the shader resource view
        /*D3D11_SHADER_RESOURCE_VIEW_DESC descSRV;
        descSRV.Format = desc.Format;
        descSRV.ViewDimension = D3D11_SRV_DIMENSION_TEXTURE3D;
        descSRV.Texture3D.MipLevels = 1;
        descSRV.Texture3D.MostDetailedMip = 0;*/
        for(int i=0; i<m_numRTs; i++)
//            V_RETURN( pd3dDevice->CreateShaderResourceView( m_pTex3Ds[i], &descSRV, &m_pSRVs[i] ) );
            m_pSRVs[i] = m_pTex3Ds[i];

        // create the render target view
        /*D3D11_RENDER_TARGET_VIEW_DESC descRT;
        descRT.Format = desc.Format;
        descRT.ViewDimension = D3D11_RTV_DIMENSION_TEXTURE3D;
        descRT.Texture3D.MipSlice = 0;
        descRT.Texture3D.FirstWSlice = 0;
        descRT.Texture3D.WSize = depth3D;*/
        for(int i=0; i<m_numRTs; i++)
//            V_RETURN( pd3dDevice->CreateRenderTargetView( m_pTex3Ds[i], &descRT, &m_pRTVs[i] ) );
            m_pRTVs[i] = m_pTex3Ds[i];

        if (uav)
        {
            /*// create the unordered access view
            D3D11_UNORDERED_ACCESS_VIEW_DESC descUAV;
            descUAV.Format = desc.Format;
            descUAV.ViewDimension = D3D11_UAV_DIMENSION_TEXTURE3D;
            descUAV.Texture3D.MipSlice = 0;
            descUAV.Texture3D.FirstWSlice = 0;
            descUAV.Texture3D.WSize = depth3D;*/
            for(int i=0; i<m_numRTs; i++)
//                V_RETURN( pd3dDevice->CreateUnorderedAccessView( m_pTex3Ds[i], &descUAV, &m_pUAVs[i] ) );
                m_pUAVs[i] = m_pTex3Ds[i];
        }

//        return S_OK;
    }

    @Override
    public void dispose()
    {
        for(int i=0; i<m_numRTs; i++)
        {
            CommonUtil.safeRelease( m_pTex2Ds[i] );
            CommonUtil.safeRelease( m_pTex3Ds[i] );
            CommonUtil.safeRelease( m_pRTVs[i] );
            CommonUtil.safeRelease( m_pUAVs[i] );
        }

        /*if(m_pTex2Ds)
            delete[] m_pTex2Ds;
        if(m_pTex3Ds)
            delete[] m_pTex3Ds;
        if(m_pRTVs)
            delete[] m_pRTVs;
        if(m_pUAVs)
            delete[] m_pUAVs;*/

        m_pTex2Ds = null;
        m_pTex3Ds = null;
        m_pRTVs = null;
        m_pUAVs = null;

    }

    TextureGL get_pRTV(int i)
    {
        return (m_numRTs > i) ? m_pRTVs[i]  : null;
    }

    TextureGL get_pUAV(int i)
    {
        return (m_numRTs > i) ? m_pUAVs[i]  : null;
    }
}
