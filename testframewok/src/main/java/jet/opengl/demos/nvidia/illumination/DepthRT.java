package jet.opengl.demos.nvidia.illumination;

/**
 * Created by Administrator on 2017/11/13 0013.
 */

import jet.opengl.postprocessing.texture.Texture2D;
import jet.opengl.postprocessing.texture.Texture2DDesc;
import jet.opengl.postprocessing.texture.TextureGL;
import jet.opengl.postprocessing.texture.TextureUtils;

/** assuming that a DepthRT will not want to have more than a single Render target / texture (vs number of them as in SimpleRT) */
final class DepthRT {
    int m_numRTs;
    int getNumRTs() { return m_numRTs; }
    TextureGL m_pSRVs;
    void create(int numRTs)
    {
        SimpleRT.g_numResourcesRT += numRTs;

        m_numRTs = numRTs;
        /*m_pSRVs = new ID3D11ShaderResourceView*[m_numRTs];
        for(int i=0; i<numRTs; i++)
            m_pSRVs[i] = NULL;*/
    }

     boolean is2DTexture() { return true; } //is this always true?

    TextureGL get_ppSRV(int rt)
    {
        return (m_numRTs > rt) ? m_pSRVs  : null;
    }
    TextureGL get_pSRV(int rt)
    {
        return (m_numRTs > rt) ? m_pSRVs  : null;
    }

    Texture2D pTexture;
    Texture2D pDSV;


    DepthRT( /*ID3D11Device* pd3dDevice,*/ Texture2DDesc pTexDesc )/*: RenderTarget()*/
    {
        /*HRESULT hr;
        V( pd3dDevice->CreateTexture2D( pTexDesc, NULL, &pTexture ) );*/
        pTexture = TextureUtils.createTexture2D(pTexDesc, null    );

        create(1);

        /*D3D11_SHADER_RESOURCE_VIEW_DESC srvDesc;
        srvDesc.Format = DXGI_FORMAT_R32_FLOAT;
        srvDesc.ViewDimension = D3D11_SRV_DIMENSION_TEXTURE2D;
        srvDesc.Texture2D.MostDetailedMip = 0;
        srvDesc.Texture2D.MipLevels = 1;
        V( pd3dDevice->CreateShaderResourceView( pTexture, &srvDesc, get_ppSRV(0) ) );*/
        m_pSRVs = pDSV = pTexture;

        /*D3D11_DEPTH_STENCIL_VIEW_DESC dsvDesc;
        dsvDesc.Format = DXGI_FORMAT_D32_FLOAT;
        dsvDesc.ViewDimension = D3D11_DSV_DIMENSION_TEXTURE2DMS;
        dsvDesc.Texture2D.MipSlice = 0;
        dsvDesc.Flags = 0;
        V( pd3dDevice->CreateDepthStencilView( pTexture, &dsvDesc, &pDSV ) );*/
    }
}
