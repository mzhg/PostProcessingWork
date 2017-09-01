package jet.opengl.demos.nvidia.fire;

import com.nvidia.developer.opengl.app.NvSampleApp;

import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector3f;

import jet.opengl.postprocessing.common.GLCheck;
import jet.opengl.postprocessing.common.GLFuncProvider;
import jet.opengl.postprocessing.common.GLenum;
import jet.opengl.postprocessing.texture.Texture2D;

/**
 * Created by mazhen'gui on 2017/9/1.
 */

public final class PerlinFire extends NvSampleApp {
    // Fire parameters

    static final boolean DEFAULT_JITTER = true;
    static final int DEFAULT_SAMPLING_RATE =16;
    static final float DEFAULT_SPEED = 0.6f;
    static final float DEFAULT_NOISE_SCALE = 1.35f;
    static final float DEFAULT_ROUGHNESS = 3.20f;
    static final float DEFAULT_SHAPE_SIZE =3.0f;
    static final float DEFAULT_FREQUENCY1 =1.0f;
    static final float DEFAULT_FREQUENCY2 =0.5f;
    static final float DEFAULT_FREQUENCY3 =0.25f;
    static final float DEFAULT_FREQUENCY4 =0.125f;
    static final float DEFAULT_FREQUENCY5 =0.0625f;

    // Textures and related shader resource views
    Texture2D g_pDepthBuffer = null;
    Texture2D g_pDepthBufferSRV = null;
    Texture2D g_pDepthBufferDSV = null;

    Texture2D g_pFireTexture = null;
    Texture2D g_pFireTextureSRV = null;

    Texture2D g_pNoiseTexture = null;
    Texture2D g_pJitterTextureSRV = null;
    Texture2D g_pPermTextureSRV = null;

// Textures and views for shadow mapping

    int g_pCubeMapDepth = 0;
    int g_pCubeMapDepthViewArray[] = { 0, 0, 0, 0, 0, 0 };
    int g_pCubeMapTextureRV = 0;


// Effect techniques
    RenderProgram g_pCurrentTechnique = null;
    RenderProgram g_pPerlinFire3D = null;
    RenderProgram g_pPerlinFire4D = null;
    RenderProgram g_pPerlinFire3DFlow = null;
    RenderProgram g_pGeometryTechnique = null;
    RenderProgram g_pGeometryTechniqueAux = null;

    final UniformData m_uniformData = new UniformData();
    int nSamplingRate = DEFAULT_SAMPLING_RATE;
    float g_fSpeed = DEFAULT_SPEED;
    float g_fShapeSize = DEFAULT_SHAPE_SIZE;
    int g_CubeMapSize = 800;
    private GLFuncProvider gl;

    PerlinFire(){
        m_uniformData.bJitter = DEFAULT_JITTER;

        m_uniformData.fNoiseScale = DEFAULT_NOISE_SCALE;
        m_uniformData.fRoughness = DEFAULT_ROUGHNESS;

        m_uniformData.fFrequencyWeights[0] = DEFAULT_FREQUENCY1;
        m_uniformData.fFrequencyWeights[1] = DEFAULT_FREQUENCY2;
        m_uniformData.fFrequencyWeights[2] = DEFAULT_FREQUENCY3;
        m_uniformData.fFrequencyWeights[3] = DEFAULT_FREQUENCY4;
        m_uniformData.fFrequencyWeights[4] = DEFAULT_FREQUENCY5;
    }

    // Prepare cube map texture array

    boolean PrepareCubeMap(/*ID3D10Device * pd3dDevice*/)
    {
        // Create cubic depth stencil texture.

        /*D3D10_TEXTURE2D_DESC dstex;
        dstex.Width = g_CubeMapSize;
        dstex.Height = g_CubeMapSize;
        dstex.MipLevels = 1;
        dstex.ArraySize = 6;
        dstex.SampleDesc.Count = 1;
        dstex.SampleDesc.Quality = 0;
        dstex.Format = DXGI_FORMAT_R24G8_TYPELESS;
        dstex.Usage = D3D10_USAGE_DEFAULT;
        dstex.BindFlags = D3D10_BIND_DEPTH_STENCIL | D3D10_BIND_SHADER_RESOURCE;
        dstex.CPUAccessFlags = 0;
        dstex.MiscFlags = D3D10_RESOURCE_MISC_TEXTURECUBE;
        if( FAILED( pd3dDevice->CreateTexture2D( &dstex, NULL, &g_pCubeMapDepth ) ) )
        {
            DXUTTRACE( L"Failed to create depth stencil texture\n" );
            return false;
        }*/
        g_pCubeMapDepth =gl.glGenTexture();
        gl.glBindTexture(GLenum.GL_TEXTURE_CUBE_MAP, g_pCubeMapDepth);
        for(int i = 0;i < 6;i++){
            gl.glTexImage2D(GLenum.GL_TEXTURE_CUBE_MAP_NEGATIVE_X + i,0, GLenum.GL_DEPTH24_STENCIL8, g_CubeMapSize, g_CubeMapSize);
        }
        gl.glBindTexture(GLenum.GL_TEXTURE_CUBE_MAP, 0);


        // Create the depth stencil view for the entire cube

//        D3D10_DEPTH_STENCIL_VIEW_DESC DescDS;

        for( int i = 0; i < 6; i ++ )
        {
            /*DescDS.Format = DXGI_FORMAT_D24_UNORM_S8_UINT;
            DescDS.ViewDimension = D3D10_DSV_DIMENSION_TEXTURE2DARRAY;
            DescDS.Texture2DArray.FirstArraySlice = i;
            DescDS.Texture2DArray.ArraySize = (unsigned) 1;
            DescDS.Texture2DArray.MipSlice = 0;

            if( FAILED( pd3dDevice->CreateDepthStencilView( g_pCubeMapDepth, &DescDS, &(g_pCubeMapDepthViewArray[i]) ) ) )
            {
                DXUTTRACE( L"Failed to create depth stencil view for a depth cube map\n" );
                return false;
            }*/

            g_pCubeMapDepthViewArray[i] = gl.glGenTexture();
            gl.glTextureView(g_pCubeMapDepthViewArray[i], GLenum.GL_TEXTURE_2D, g_pCubeMapDepth, GLenum.GL_DEPTH24_STENCIL8, 0,1, i, 1);
            GLCheck.checkError();
        }

        // Create the shader resource view for the shadow map

        /*D3D10_SHADER_RESOURCE_VIEW_DESC SRVDesc;
        ZeroMemory( &SRVDesc, sizeof( SRVDesc ) );
        SRVDesc.Format = DXGI_FORMAT_R24_UNORM_X8_TYPELESS;
        SRVDesc.ViewDimension = D3D10_SRV_DIMENSION_TEXTURECUBE;
        SRVDesc.Texture2DArray.MipLevels = 1;
        SRVDesc.Texture2DArray.MostDetailedMip = 0;
        SRVDesc.Texture2DArray.FirstArraySlice = 0;
        SRVDesc.Texture2DArray.ArraySize = 6;
        if( FAILED( pd3dDevice->CreateShaderResourceView( g_pCubeMapDepth, &SRVDesc, &g_pCubeMapTextureRV ) ) )
        {
            DXUTTRACE( L"Failed to create shader resource view for a depth stencil\n" );
            return false;
        }
        return true;*/
        g_pCubeMapTextureRV = g_pCubeMapDepth;
        return true;
    }

// Set matrices for cube mapping

    void InitCubeMatrices( Vector3f cubeCenter )
    {
        final Vector3f vLookDir = new Vector3f();
        final Vector3f vUpDir = new Vector3f();
        final Matrix4f[] cubeViewMatrices = m_uniformData.mCubeViewMatrixs;
        final Matrix4f cubeProjMatrix = m_uniformData.mCubeProjMatrix;

//        vLookDir = D3DXVECTOR3( 1.0f, 0.0f, 0.0f ) + (* (D3DXVECTOR3 *) cubeCenter);
//        vUpDir = D3DXVECTOR3( 0.0f, 1.0f, 0.0f );
//        D3DXMatrixLookAtLH( &cubeViewMatrices[0], (D3DXVECTOR3 *) cubeCenter, &vLookDir, &vUpDir );
        Vector3f.add(cubeCenter, Vector3f.X_AXIS, vLookDir);
        vUpDir.set(0.0f, 1.0f, 0.0f);
        Matrix4f.lookAt(cubeCenter, vLookDir, vUpDir, cubeViewMatrices[0]);

//        vLookDir = D3DXVECTOR3( -1.0f, 0.0f, 0.0f ) + (* (D3DXVECTOR3 *) cubeCenter);
//        vUpDir = D3DXVECTOR3( 0.0f, 1.0f, 0.0f );
//        D3DXMatrixLookAtLH( &cubeViewMatrices[1], (D3DXVECTOR3 *) cubeCenter, &vLookDir, &vUpDir );
        Vector3f.add(cubeCenter, Vector3f.X_AXIS_NEG, vLookDir);
        vUpDir.set(0.0f, 1.0f, 0.0f);
        Matrix4f.lookAt(cubeCenter, vLookDir, vUpDir, cubeViewMatrices[1]);

//        vLookDir = D3DXVECTOR3( 0.0f, 1.0f,  0.0f ) + (* (D3DXVECTOR3 *) cubeCenter);
//        vUpDir = D3DXVECTOR3( 0.0f, 0.0f, -1.0f );
//        D3DXMatrixLookAtLH( &cubeViewMatrices[2], (D3DXVECTOR3 *) cubeCenter, &vLookDir, &vUpDir );
        Vector3f.add(cubeCenter, Vector3f.Y_AXIS, vLookDir);
        vUpDir.set(0.0f, 0.0f, -1.0f);
        Matrix4f.lookAt(cubeCenter, vLookDir, vUpDir, cubeViewMatrices[2]);

//        vLookDir = D3DXVECTOR3( 0.0f, -1.0f, 0.0f ) + (* (D3DXVECTOR3 *) cubeCenter);
//        vUpDir = D3DXVECTOR3( 0.0f,  0.0f, 1.0f );
//        D3DXMatrixLookAtLH( &cubeViewMatrices[3], (D3DXVECTOR3 *) cubeCenter, &vLookDir, &vUpDir );
        Vector3f.add(cubeCenter, Vector3f.Y_AXIS_NEG, vLookDir);
        vUpDir.set(0.0f, 0.0f, 1.0f);
        Matrix4f.lookAt(cubeCenter, vLookDir, vUpDir, cubeViewMatrices[3]);

//        vLookDir = D3DXVECTOR3( 0.0f, 0.0f, 1.0f ) + (* (D3DXVECTOR3 *) cubeCenter);
//        vUpDir = D3DXVECTOR3( 0.0f, 1.0f, 0.0f );
//        D3DXMatrixLookAtLH( &cubeViewMatrices[4], (D3DXVECTOR3 *) cubeCenter, &vLookDir, &vUpDir );
        Vector3f.add(cubeCenter, Vector3f.Z_AXIS, vLookDir);
        vUpDir.set(0.0f, 1.0f, 0.0f);
        Matrix4f.lookAt(cubeCenter, vLookDir, vUpDir, cubeViewMatrices[4]);

//        vLookDir = D3DXVECTOR3( 0.0f, 0.0f, -1.0f ) + (* (D3DXVECTOR3 *) cubeCenter);
//        vUpDir = D3DXVECTOR3( 0.0f, 1.0f,  0.0f );
//        D3DXMatrixLookAtLH( &cubeViewMatrices[5], (D3DXVECTOR3 *) cubeCenter, &vLookDir, &vUpDir );
        Vector3f.add(cubeCenter, Vector3f.Z_AXIS_NEG, vLookDir);
        vUpDir.set(0.0f, 1.0f, 0.0f);
        Matrix4f.lookAt(cubeCenter, vLookDir, vUpDir, cubeViewMatrices[5]);

//        D3DXMatrixPerspectiveFovLH( &cubeProjMatrix, (float)D3DX_PI * 0.5f, 1.0f, 0.2f, 200.0f );
        Matrix4f.perspective(90, 1.0f, 0.2f, 200.0f, cubeProjMatrix);

//        g_pmCubeViewMatrixVariable->SetMatrixArray( (float *)cubeViewMatrices, 0, 6 );
//        g_pmCubeProjMatrixVariable->SetMatrix( cubeProjMatrix );
    }
}
