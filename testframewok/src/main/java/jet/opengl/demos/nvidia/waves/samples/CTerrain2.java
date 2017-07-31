package jet.opengl.demos.nvidia.waves.samples;

import com.nvidia.developer.opengl.utils.NvImage;

import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.ReadableVector3f;
import org.lwjgl.util.vector.Vector3f;
import org.lwjgl.util.vector.Vector4f;

import java.io.IOException;

import jet.opengl.postprocessing.buffer.BufferGL;
import jet.opengl.postprocessing.buffer.VertexArrayObject;
import jet.opengl.postprocessing.common.GLenum;
import jet.opengl.postprocessing.texture.Texture2D;
import jet.opengl.postprocessing.texture.Texture2DDesc;
import jet.opengl.postprocessing.texture.TextureDataDesc;
import jet.opengl.postprocessing.texture.TextureUtils;
import jet.opengl.postprocessing.util.CacheBuffer;
import jet.opengl.postprocessing.util.CommonUtil;
import jet.opengl.postprocessing.util.Numeric;

/**
 * Created by mazhen'gui on 2017/7/27.
 */

final class CTerrain2 implements Constants{
    private static final int NumMarkersXY = 10;
    private static final int NumMarkers = NumMarkersXY*NumMarkersXY;

    int MultiSampleCount;
    int MultiSampleQuality;

    Texture2D rock_bump_textureSRV;

    Texture2D sky_textureSRV;

    Texture2D foam_intensity_textureSRV;

    Texture2D foam_diffuse_textureSRV;

    Texture2D reflection_color_resource;
    Texture2D reflection_color_resourceSRV;
    Texture2D reflection_color_resourceRTV;
    Texture2D refraction_color_resource;
    Texture2D refraction_color_resourceSRV;
    Texture2D refraction_color_resourceRTV;

    Texture2D shadowmap_resource;
    Texture2D shadowmap_resourceSRV;
    Texture2D shadowmap_resourceDSV;

    Texture2D reflection_depth_resource;
    Texture2D reflection_depth_resourceDSV;

    Texture2D refraction_depth_resource;
    Texture2D refraction_depth_resourceRTV;
    Texture2D refraction_depth_resourceSRV;

    Texture2D main_color_resource;
    Texture2D main_color_resourceSRV;
    Texture2D main_color_resourceRTV;
    Texture2D main_depth_resource;
    Texture2D main_depth_resourceDSV;
    Texture2D main_depth_resourceSRV;
    Texture2D main_color_resource_resolved;
    Texture2D main_color_resource_resolvedSRV;

//    ID3D11Device* pDevice;
//    ID3DX11Effect* pEffect;

    final float[][]		height = new float[terrain_gridpoints+1][terrain_gridpoints+1];
    final Vector3f[][]  normal = new Vector3f[terrain_gridpoints+1][terrain_gridpoints+1];
    final Vector3f[][]	tangent = new Vector3f[terrain_gridpoints + 1][terrain_gridpoints + 1];
    final Vector3f[][]	binormal = new Vector3f[terrain_gridpoints + 1][terrain_gridpoints + 1];

    Texture2D heightmap_texture;
    Texture2D heightmap_textureSRV;

    Texture2D layerdef_texture;
    Texture2D layerdef_textureSRV;

    Texture2D depthmap_texture;
    Texture2D depthmap_textureSRV;

    BufferGL heightfield_vertexbuffer;
    BufferGL sky_vertexbuffer;

    VertexArrayObject heightfield_inputlayout;
    VertexArrayObject trianglestrip_inputlayout;

    SampleD3D11 m_context;

    private final Matrix4f m_ProjView = new Matrix4f();
    private final Matrix4f m_ProjViewInv = new Matrix4f();

    CTerrain2(){
        for(int i = 0; i < normal.length; i++){
            for(int j= 0;j < normal[i].length; j++){
                normal[i][j] = new Vector3f();
            }
        }

        for(int i = 0; i < tangent.length; i++){
            for(int j= 0;j < tangent[i].length; j++){
                tangent[i][j] = new Vector3f();
            }
        }

        for(int i = 0; i < binormal.length; i++){
            for(int j= 0;j < binormal[i].length; j++){
                binormal[i][j] = new Vector3f();
            }
        }
    }

    void Initialize(SampleD3D11 context){
//        pEffect = effect;
//        pDevice = device;
        m_context = context;


//        const D3D11_INPUT_ELEMENT_DESC TerrainLayout =
//                { "PATCH_PARAMETERS",  0, DXGI_FORMAT_R32G32B32A32_FLOAT,   0, 0,  D3D11_INPUT_PER_VERTEX_DATA, 0 };
//
//        D3DX11_PASS_DESC passDesc;
//        effect->GetTechniqueByName("RenderHeightfield")->GetPassByIndex(0)->GetDesc(&passDesc);
//
//        device->CreateInputLayout( &TerrainLayout, 1, passDesc.pIAInputSignature, passDesc.IAInputSignatureSize, &heightfield_inputlayout );
//
//        const D3D11_INPUT_ELEMENT_DESC SkyLayout [] =
//                {
//                        { "POSITION",  0, DXGI_FORMAT_R32G32B32A32_FLOAT,   0, 0,  D3D11_INPUT_PER_VERTEX_DATA, 0 },
//                        { "TEXCOORD",  0, DXGI_FORMAT_R32G32_FLOAT,   0, 16,  D3D11_INPUT_PER_VERTEX_DATA, 0 }
//                };
//
//        effect->GetTechniqueByName("RenderSky")->GetPassByIndex(0)->GetDesc(&passDesc);
//
//        device->CreateInputLayout( SkyLayout,
//                2,
//                passDesc.pIAInputSignature,
//                passDesc.IAInputSignatureSize,
//                &trianglestrip_inputlayout );

        CreateTerrain();
    }

    void DeInitialize(){
        CommonUtil.safeRelease(main_color_resource);
        CommonUtil.safeRelease(main_color_resourceSRV);
        CommonUtil.safeRelease(main_color_resourceRTV);

        CommonUtil.safeRelease(main_color_resource_resolved);
        CommonUtil.safeRelease(main_color_resource_resolvedSRV);

        CommonUtil.safeRelease(main_depth_resource);
        CommonUtil.safeRelease(main_depth_resourceDSV);
        CommonUtil.safeRelease(main_depth_resourceSRV);

        CommonUtil.safeRelease(reflection_color_resource);
        CommonUtil.safeRelease(reflection_color_resourceSRV);
        CommonUtil.safeRelease(reflection_color_resourceRTV);
        CommonUtil.safeRelease(refraction_color_resource);
        CommonUtil.safeRelease(refraction_color_resourceSRV);
        CommonUtil.safeRelease(refraction_color_resourceRTV);

        CommonUtil.safeRelease(reflection_depth_resource);
        CommonUtil.safeRelease(reflection_depth_resourceDSV);
        CommonUtil.safeRelease(refraction_depth_resource);
        CommonUtil.safeRelease(refraction_depth_resourceSRV);
        CommonUtil.safeRelease(refraction_depth_resourceRTV);

        CommonUtil.safeRelease(shadowmap_resource);
        CommonUtil.safeRelease(shadowmap_resourceDSV);
        CommonUtil.safeRelease(shadowmap_resourceSRV);
    }

    void ReCreateBuffers(){
        DeInitialize();

        Texture2DDesc tex_desc = new Texture2DDesc();
        tex_desc.width              = (int)(BackbufferWidth*main_buffer_size_multiplier);
        tex_desc.height             = (int)(BackbufferHeight*main_buffer_size_multiplier);
        tex_desc.mipLevels          = 1;
        tex_desc.arraySize          = 1;
        tex_desc.format             = GLenum.GL_RGBA8; // DXGI_FORMAT_R8G8B8A8_UNORM;
//        tex_desc.SampleDesc.Count   = MultiSampleCount;
//        tex_desc.SampleDesc.Quality = MultiSampleQuality;
//        tex_desc.Usage              = D3D11_USAGE_DEFAULT;
//        tex_desc.BindFlags          = D3D11_BIND_SHADER_RESOURCE | D3D11_BIND_RENDER_TARGET;
//        tex_desc.CPUAccessFlags     = 0;
//        tex_desc.MiscFlags          = 0;

        main_color_resourceRTV = main_color_resourceSRV = TextureUtils.createTexture2D(tex_desc, null);
        main_color_resource_resolvedSRV = TextureUtils.createTexture2D(tex_desc, null);  // sample count must be 1.

        tex_desc.format             = GLenum.GL_DEPTH_COMPONENT32F;
        main_depth_resourceSRV      = main_depth_resourceDSV = TextureUtils.createTexture2D(tex_desc, null);

        tex_desc.width              = (int)(BackbufferWidth*reflection_buffer_size_multiplier);
        tex_desc.height             = (int)(BackbufferHeight*reflection_buffer_size_multiplier);
        tex_desc.mipLevels          = (int)Math.max(1.0f, Math.log(Math.max((float)tex_desc.width,(float)tex_desc.height))/Math.log(2.0f));
        tex_desc.arraySize          = 1;
        tex_desc.format             = GLenum.GL_RGBA8;

        reflection_color_resourceSRV = reflection_color_resourceRTV = TextureUtils.createTexture2D(tex_desc, null);

        tex_desc.width              = (int)(BackbufferWidth*refraction_buffer_size_multiplier);
        tex_desc.height             = (int)(BackbufferHeight*refraction_buffer_size_multiplier);
        tex_desc.mipLevels          = (int)Math.max(1.0f,Math.log(Math.max((float)tex_desc.width,(float)tex_desc.height))/Math.log(2.0f));
        tex_desc.arraySize          = 1;
        tex_desc.format             = GLenum.GL_RGBA8;

        refraction_color_resourceSRV = refraction_color_resourceRTV = TextureUtils.createTexture2D(tex_desc, null);

        tex_desc.width              = (int)(BackbufferWidth*reflection_buffer_size_multiplier);
        tex_desc.height             = (int)(BackbufferHeight*reflection_buffer_size_multiplier);
        tex_desc.mipLevels          = 1;
        tex_desc.arraySize          = 1;
        tex_desc.format             = GLenum.GL_DEPTH_COMPONENT32F;

        reflection_depth_resourceDSV = TextureUtils.createTexture2D(tex_desc, null);

        tex_desc.width              = (int)(BackbufferWidth*refraction_buffer_size_multiplier);
        tex_desc.height             = (int)(BackbufferHeight*refraction_buffer_size_multiplier);
        tex_desc.mipLevels          = 1;
        tex_desc.arraySize          = 1;
        tex_desc.format             = GLenum.GL_DEPTH_COMPONENT32F;

        refraction_depth_resourceSRV = TextureUtils.createTexture2D(tex_desc, null);

        // recreating shadowmap resource
        tex_desc.width              = shadowmap_resource_buffer_size_xy;
        tex_desc.height             = shadowmap_resource_buffer_size_xy;
        tex_desc.mipLevels          = 1;
        tex_desc.arraySize          = 1;
        tex_desc.format             = GLenum.GL_DEPTH_COMPONENT32F;

        shadowmap_resourceSRV = shadowmap_resourceDSV = TextureUtils.createTexture2D(tex_desc, null);
    }

    void LoadTextures(){
        try {
            int rock_bump_texture = NvImage.uploadTextureFromDDSFile("nvidia/WaveWorks/textures/rock_bump6.dds");
            rock_bump_textureSRV = TextureUtils.createTexture2D(GLenum.GL_TEXTURE_2D, rock_bump_texture);

            int sky_texture = NvImage.uploadTextureFromDDSFile("nvidia/WaveWorks/textures/SunsetFair.dds");
            sky_textureSRV = TextureUtils.createTexture2D(GLenum.GL_TEXTURE_2D, sky_texture);

            int foam_intensity_texture = NvImage.uploadTextureFromDDSFile("nvidia/WaveWorks/textures/foam_intensity_perlin2.dds");
            foam_intensity_textureSRV = TextureUtils.createTexture2D(GLenum.GL_TEXTURE_2D, foam_intensity_texture);

            int foam_diffuse_texture = NvImage.uploadTextureFromDDSFile("nvidia/WaveWorks/textures/foam24bit.dds");
            foam_diffuse_textureSRV = TextureUtils.createTexture2D(GLenum.GL_TEXTURE_2D, foam_diffuse_texture);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    void Render(Matrix4f matProj, Matrix4f matView){
//        ID3D11DeviceContext* pContext;
        float origin[/*2*/]={0,0};
        int stride=4*4;
        int offset=0;
        int cRT = 1;

//        pDevice->GetImmediateContext(&pContext);

//        pEffect->GetVariableByName("g_HeightfieldTexture")->AsShaderResource()->SetResource(heightmap_textureSRV);
//        pEffect->GetVariableByName("g_LayerdefTexture")->AsShaderResource()->SetResource(layerdef_textureSRV);
//        pEffect->GetVariableByName("g_RockBumpTexture")->AsShaderResource()->SetResource(rock_bump_textureSRV);
//        pEffect->GetVariableByName("g_SkyTexture")->AsShaderResource()->SetResource(sky_textureSRV);
//        pEffect->GetVariableByName("g_HeightFieldOrigin")->AsVector()->SetFloatVector(origin);
//        pEffect->GetVariableByName("g_HeightFieldSize")->AsScalar()->SetFloat(terrain_gridpoints*terrain_geometry_scale);

        ID3D11RenderTargetView *colorBuffer = DXUTGetD3D11RenderTargetView();
        ID3D11DepthStencilView  *backBuffer = DXUTGetD3D11DepthStencilView();
        D3D11_VIEWPORT currentViewport;
        D3D11_VIEWPORT reflection_Viewport;
        D3D11_VIEWPORT refraction_Viewport;
        D3D11_VIEWPORT shadowmap_resource_viewport;
        D3D11_VIEWPORT water_normalmap_resource_viewport;
        D3D11_VIEWPORT main_Viewport;

        float ClearColor[4] = { 0.0f, 0.0f, 0.0f, 1.0f };
        float RefractionClearColor[4] = { 0.5f, 0.5f, 0.5f, 1.0f };

        reflection_Viewport.Width=(float)BackbufferWidth*reflection_buffer_size_multiplier;
        reflection_Viewport.Height=(float)BackbufferHeight*reflection_buffer_size_multiplier;
        reflection_Viewport.MaxDepth=1;
        reflection_Viewport.MinDepth=0;
        reflection_Viewport.TopLeftX=0;
        reflection_Viewport.TopLeftY=0;

        refraction_Viewport.Width=(float)BackbufferWidth*refraction_buffer_size_multiplier;
        refraction_Viewport.Height=(float)BackbufferHeight*refraction_buffer_size_multiplier;
        refraction_Viewport.MaxDepth=1;
        refraction_Viewport.MinDepth=0;
        refraction_Viewport.TopLeftX=0;
        refraction_Viewport.TopLeftY=0;

        main_Viewport.Width=(float)BackbufferWidth*main_buffer_size_multiplier;
        main_Viewport.Height=(float)BackbufferHeight*main_buffer_size_multiplier;
        main_Viewport.MaxDepth=1;
        main_Viewport.MinDepth=0;
        main_Viewport.TopLeftX=0;
        main_Viewport.TopLeftY=0;

        shadowmap_resource_viewport.Width=shadowmap_resource_buffer_size_xy;
        shadowmap_resource_viewport.Height=shadowmap_resource_buffer_size_xy;
        shadowmap_resource_viewport.MaxDepth=1;
        shadowmap_resource_viewport.MinDepth=0;
        shadowmap_resource_viewport.TopLeftX=0;
        shadowmap_resource_viewport.TopLeftY=0;

        water_normalmap_resource_viewport.Width=water_normalmap_resource_buffer_size_xy;
        water_normalmap_resource_viewport.Height=water_normalmap_resource_buffer_size_xy;
        water_normalmap_resource_viewport.MaxDepth=1;
        water_normalmap_resource_viewport.MinDepth=0;
        water_normalmap_resource_viewport.TopLeftX=0;
        water_normalmap_resource_viewport.TopLeftY=0;


        //saving scene color buffer and back buffer to constants
        pContext->RSGetViewports( &cRT, &currentViewport);
        pContext->OMGetRenderTargets( 1, &colorBuffer, &backBuffer );


        // selecting shadowmap_resource rendertarget
        pEffect->GetVariableByName("g_ShadowmapTexture")->AsShaderResource()->SetResource(NULL);

        pContext->RSSetViewports(1,&shadowmap_resource_viewport);
        pContext->OMSetRenderTargets( 0, NULL, shadowmap_resourceDSV);
        pContext->ClearDepthStencilView( shadowmap_resourceDSV, D3D11_CLEAR_DEPTH, 1.0, 0 );

        //drawing terrain to shadowmap
        SetupLightView(cam);

        pEffect->GetVariableByName("g_ShadowmapTexture")->AsShaderResource()->SetResource(NULL);

        pEffect->GetVariableByName("g_ApplyFog")->AsScalar()->SetFloat(0.0f);

        pContext->IASetInputLayout(heightfield_inputlayout);
        pContext->IASetPrimitiveTopology(D3D11_PRIMITIVE_TOPOLOGY_1_CONTROL_POINT_PATCHLIST);
        pEffect->GetTechniqueByName("RenderHeightfield")->GetPassByIndex(2)->Apply(0, pContext);
        stride=sizeof(float)*4;
        pContext->IASetVertexBuffers(0,1,&heightfield_vertexbuffer,&stride,&offset);
        pContext->Draw(terrain_numpatches_1d*terrain_numpatches_1d, 0);

        pEffect->GetTechniqueByName("Default")->GetPassByIndex(0)->Apply(0, pContext);

        pEffect->GetVariableByName("g_ShadowmapTexture")->AsShaderResource()->SetResource(shadowmap_resourceSRV);


        // setting up reflection rendertarget
        pContext->RSSetViewports(1,&reflection_Viewport);
        pContext->OMSetRenderTargets( 1, &reflection_color_resourceRTV, reflection_depth_resourceDSV);
        pContext->ClearRenderTargetView( reflection_color_resourceRTV, RefractionClearColor );
        pContext->ClearDepthStencilView( reflection_depth_resourceDSV, D3D11_CLEAR_DEPTH, 1.0, 0 );

        SetupReflectionView(cam);
        // drawing sky to reflection RT
        pEffect->GetTechniqueByName("RenderSky")->GetPassByIndex(0)->Apply(0, pContext);
        pContext->IASetInputLayout(trianglestrip_inputlayout);
        pContext->IASetPrimitiveTopology(D3D11_PRIMITIVE_TOPOLOGY_TRIANGLESTRIP);
        stride=sizeof(float)*6;
        pContext->IASetVertexBuffers(0,1,&sky_vertexbuffer,&stride,&offset);
        pContext->Draw(sky_gridpoints*(sky_gridpoints+2)*2, 0);

        // drawing terrain to reflection RT
        pEffect->GetVariableByName("g_ApplyFog")->AsScalar()->SetFloat(0.0f);
        pContext->IASetInputLayout(heightfield_inputlayout);
        pContext->IASetPrimitiveTopology(D3D11_PRIMITIVE_TOPOLOGY_1_CONTROL_POINT_PATCHLIST);
        pEffect->GetTechniqueByName("RenderHeightfield")->GetPassByIndex(0)->Apply(0, pContext);
        stride=sizeof(float)*4;
        pContext->IASetVertexBuffers(0,1,&heightfield_vertexbuffer,&stride,&offset);
        pContext->Draw(terrain_numpatches_1d*terrain_numpatches_1d, 0);


        pEffect->GetTechniqueByName("Default")->GetPassByIndex(0)->Apply(0, pContext);

        // setting up main rendertarget
        pContext->RSSetViewports(1,&main_Viewport);
        pContext->OMSetRenderTargets( 1, &main_color_resourceRTV, main_depth_resourceDSV);
        pContext->ClearRenderTargetView( main_color_resourceRTV, ClearColor );
        pContext->ClearDepthStencilView( main_depth_resourceDSV, D3D11_CLEAR_DEPTH, 1.0, 0 );
        SetupNormalView(cam);

        // drawing terrain to main buffer

        pEffect->GetVariableByName("g_ApplyFog")->AsScalar()->SetFloat(1.0f);
        pEffect->GetVariableByName("g_FoamIntensityTexture")->AsShaderResource()->SetResource(foam_intensity_textureSRV);
        pEffect->GetVariableByName("g_FoamDiffuseTexture")->AsShaderResource()->SetResource(foam_diffuse_textureSRV);

        XMMATRIX topDownMatrix;
        g_pOceanSurf->pDistanceFieldModule->GetWorldToTopDownTextureMatrix( topDownMatrix );

        XMFLOAT4X4 tdMat;
        XMStoreFloat4x4(&tdMat, topDownMatrix);

        pEffect->GetVariableByName("g_WorldToTopDownTextureMatrix")->AsMatrix()->SetMatrix((FLOAT*)&tdMat);
        pEffect->GetVariableByName("g_Time" )->AsScalar()->SetFloat( g_ShoreTime );
        pEffect->GetVariableByName("g_DataTexture" )->AsShaderResource()->SetResource( g_pOceanSurf->pDistanceFieldModule->GetDataTextureSRV() );
        pEffect->GetVariableByName("g_GerstnerSteepness")->AsScalar()->SetFloat( g_GerstnerSteepness );
        pEffect->GetVariableByName("g_BaseGerstnerAmplitude")->AsScalar()->SetFloat( g_BaseGerstnerAmplitude );
        pEffect->GetVariableByName("g_BaseGerstnerWavelength")->AsScalar()->SetFloat( g_BaseGerstnerWavelength );
        pEffect->GetVariableByName("g_BaseGerstnerSpeed")->AsScalar()->SetFloat( g_BaseGerstnerSpeed );
        pEffect->GetVariableByName("g_BaseGerstnerParallelness")->AsScalar()->SetFloat( g_GerstnerParallelity );
        pEffect->GetVariableByName("g_WindDirection")->AsVector()->SetFloatVector( &g_WindDir.x );

        pContext->IASetInputLayout(heightfield_inputlayout);
        pContext->IASetPrimitiveTopology(D3D11_PRIMITIVE_TOPOLOGY_1_CONTROL_POINT_PATCHLIST);
        pEffect->GetTechniqueByName("RenderHeightfield")->GetPassByIndex(0)->Apply(0, pContext);
        stride=sizeof(float)*4;
        pContext->IASetVertexBuffers(0,1,&heightfield_vertexbuffer,&stride,&offset);
        pContext->Draw(terrain_numpatches_1d*terrain_numpatches_1d, 0);


        // resolving main buffer color to refraction color resource
        pContext->ResolveSubresource(refraction_color_resource,0,main_color_resource,0,DXGI_FORMAT_R8G8B8A8_UNORM);
        pContext->GenerateMips(refraction_color_resourceSRV);

        // resolving main buffer depth to refraction depth resource manually
        pContext->RSSetViewports( 1, &main_Viewport );
        pContext->OMSetRenderTargets( 1, &refraction_depth_resourceRTV, NULL);
        pContext->ClearRenderTargetView(refraction_depth_resourceRTV, ClearColor);

        pContext->IASetInputLayout(trianglestrip_inputlayout);
        pContext->IASetPrimitiveTopology(D3D11_PRIMITIVE_TOPOLOGY_TRIANGLESTRIP);

        switch(MultiSampleCount)
        {
            case 1:
                pEffect->GetVariableByName("g_RefractionDepthTextureMS1")->AsShaderResource()->SetResource(main_depth_resourceSRV);
                pEffect->GetTechniqueByName("RefractionDepthManualResolve")->GetPassByIndex(0)->Apply(0, pContext);
                break;
            case 2:
                pEffect->GetVariableByName("g_RefractionDepthTextureMS2")->AsShaderResource()->SetResource(main_depth_resourceSRV);
                pEffect->GetTechniqueByName("RefractionDepthManualResolve")->GetPassByIndex(1)->Apply(0, pContext);
                break;
            case 4:
                pEffect->GetVariableByName("g_RefractionDepthTextureMS4")->AsShaderResource()->SetResource(main_depth_resourceSRV);
                pEffect->GetTechniqueByName("RefractionDepthManualResolve")->GetPassByIndex(2)->Apply(0, pContext);
                break;
            default:
                pEffect->GetVariableByName("g_RefractionDepthTextureMS1")->AsShaderResource()->SetResource(main_depth_resourceSRV);
                pEffect->GetTechniqueByName("RefractionDepthManualResolve1")->GetPassByIndex(0)->Apply(0, pContext);
                break;
        }



        stride=sizeof(float)*6;
        pContext->IASetVertexBuffers(0,1,&heightfield_vertexbuffer,&stride,&offset);
        pContext->Draw(4, 0); // just need to pass 4 vertices to shader
        pEffect->GetTechniqueByName("Default")->GetPassByIndex(0)->Apply(0, pContext);

        // clearing resource bindings
        switch(MultiSampleCount)
        {
            case 1:
                pEffect->GetVariableByName("g_RefractionDepthTextureMS1")->AsShaderResource()->SetResource(NULL);
                pEffect->GetTechniqueByName("RefractionDepthManualResolve")->GetPassByIndex(0)->Apply(0, pContext);
                break;
            case 2:
                pEffect->GetVariableByName("g_RefractionDepthTextureMS2")->AsShaderResource()->SetResource(NULL);
                pEffect->GetTechniqueByName("RefractionDepthManualResolve")->GetPassByIndex(1)->Apply(0, pContext);
                break;
            case 4:
                pEffect->GetVariableByName("g_RefractionDepthTextureMS4")->AsShaderResource()->SetResource(NULL);
                pEffect->GetTechniqueByName("RefractionDepthManualResolve")->GetPassByIndex(2)->Apply(0, pContext);
                break;
            default:
                pEffect->GetVariableByName("g_RefractionDepthTextureMS1")->AsShaderResource()->SetResource(NULL);
                pEffect->GetTechniqueByName("RefractionDepthManualResolve1")->GetPassByIndex(0)->Apply(0, pContext);
                break;
        }
        pEffect->GetTechniqueByName("Default")->GetPassByIndex(0)->Apply(0, pContext);

        // getting back to rendering to main buffer
        pContext->RSSetViewports(1,&main_Viewport);
        pContext->OMSetRenderTargets( 1, &main_color_resourceRTV, main_depth_resourceDSV);

        // drawing water surface to main buffer
        if(g_QueryStats)
        {
            pContext->Begin(g_pPipelineQuery);
        }

        ID3DX11Effect* oceanFX = g_pOceanSurf->m_pOceanFX;

        oceanFX->GetVariableByName("g_ReflectionTexture")->AsShaderResource()->SetResource(reflection_color_resourceSRV);
        oceanFX->GetVariableByName("g_RefractionTexture")->AsShaderResource()->SetResource(refraction_color_resourceSRV);
        oceanFX->GetVariableByName("g_RefractionDepthTextureResolved")->AsShaderResource()->SetResource(refraction_depth_resourceSRV);
        oceanFX->GetVariableByName("g_ShadowmapTexture")->AsShaderResource()->SetResource(shadowmap_resourceSRV);
        oceanFX->GetVariableByName("g_FoamIntensityTexture")->AsShaderResource()->SetResource(foam_intensity_textureSRV);
        oceanFX->GetVariableByName("g_FoamDiffuseTexture")->AsShaderResource()->SetResource(foam_diffuse_textureSRV);

        XMMATRIX matView = XMMatrixSet(1, 0, 0, 0, 0, 0, 1, 0, 0, 1, 0, 0, 0, 0, 0, 1) * cam->GetViewMatrix();
        XMMATRIX matProj = cam->GetProjMatrix();
        XMMATRIX matMVP = matView * matProj;
        XMVECTOR cameraPosition = cam->GetEyePt();
        XMVECTOR lightPosition = XMVectorSet(14000.0f, 6500.0f, 4000.0f, 0);

// 	const D3DXMATRIX matView = D3DXMATRIX(1,0,0,0,0,0,1,0,0,1,0,0,0,0,0,1) * *cam->GetViewMatrix();
// 	const D3DXMATRIX matProj = *cam->GetProjMatrix();
// 	D3DXVECTOR3 cameraPosition = *cam->GetEyePt();
// 	D3DXVECTOR3 lightPosition = D3DXVECTOR3(14000.0f,6500.0f,4000.0f);
//
        oceanFX->GetVariableByName("g_ModelViewMatrix")->AsMatrix()->SetMatrix((FLOAT*)&matView);
        oceanFX->GetVariableByName("g_ModelViewProjectionMatrix")->AsMatrix()->SetMatrix((FLOAT*)&matMVP);
        oceanFX->GetVariableByName("g_CameraPosition")->AsVector()->SetFloatVector((FLOAT*)&cameraPosition);
        oceanFX->GetVariableByName("g_LightPosition")->AsVector()->SetFloatVector((FLOAT*)&lightPosition);
        oceanFX->GetVariableByName("g_Wireframe")->AsScalar()->SetFloat(g_Wireframe ? 1.0f : 0.0f);

        XMFLOAT4 winSize = XMFLOAT4(main_Viewport.Width, main_Viewport.Height, 0, 0);
        oceanFX->GetVariableByName("g_WinSize")->AsVector()->SetFloatVector((FLOAT*)&winSize);
        g_pOceanSurf->renderShaded(pContext, matView,matProj,g_hOceanSimulation, g_hOceanSavestate, g_WindDir, g_GerstnerSteepness, g_BaseGerstnerAmplitude, g_BaseGerstnerWavelength, g_BaseGerstnerSpeed, g_GerstnerParallelity, g_ShoreTime);

        g_pOceanSurf->getQuadTreeStats(g_ocean_quadtree_stats);

        if(g_QueryStats)
        {
            pContext->End(g_pPipelineQuery);
            while(S_OK != pContext->GetData(g_pPipelineQuery, &g_PipelineQueryData, sizeof(g_PipelineQueryData), 0));
        }

        // clearing resource bindings
        oceanFX->GetVariableByName("g_ReflectionTexture")->AsShaderResource()->SetResource(NULL);
        oceanFX->GetVariableByName("g_RefractionTexture")->AsShaderResource()->SetResource(NULL);
        oceanFX->GetVariableByName("g_RefractionDepthTextureResolved")->AsShaderResource()->SetResource(NULL);
        oceanFX->GetVariableByName("g_ShadowmapTexture")->AsShaderResource()->SetResource(NULL);
        oceanFX->GetTechniqueByName("RenderOceanSurfTech")->GetPassByIndex(0)->Apply(0, pContext);

        // drawing readback markers to main buffer
        const UINT vbOffset = 0;
        const UINT vertexStride = sizeof(XMFLOAT4);
        XMFLOAT4 contactPos;

        pContext->IASetInputLayout(g_pOceanSurf->m_pRayContactLayout);
        pContext->IASetVertexBuffers(0, 1, &g_pOceanSurf->m_pContactVB, &vertexStride, &vbOffset);
        pContext->IASetIndexBuffer(g_pOceanSurf->m_pContactIB, DXGI_FORMAT_R16_UINT, 0);
        pContext->IASetPrimitiveTopology(D3D10_PRIMITIVE_TOPOLOGY_TRIANGLELIST);
        for( int i = 0; i < NumMarkers; i++)
        {
            contactPos = XMFLOAT4(g_readback_marker_positions[i].x, g_readback_marker_positions[i].y, g_readback_marker_positions[i].z, 0);
            g_pOceanSurf->m_pOceanFX->GetVariableByName("g_ContactPosition")->AsVector()->SetFloatVector((FLOAT*)&contactPos);
            g_pOceanSurf->m_pRenderRayContactTechnique->GetPassByIndex(0)->Apply(0, pContext);
            pContext->DrawIndexed(12, 0, 0);
        }


        // drawing raycast contacts to main buffer
        pContext->IASetInputLayout(g_pOceanSurf->m_pRayContactLayout);
        pContext->IASetVertexBuffers(0, 1, &g_pOceanSurf->m_pContactVB, &vertexStride, &vbOffset);
        pContext->IASetIndexBuffer(g_pOceanSurf->m_pContactIB, DXGI_FORMAT_R16_UINT, 0);
        pContext->IASetPrimitiveTopology(D3D10_PRIMITIVE_TOPOLOGY_TRIANGLELIST);

        for( int i = 0; i < NumMarkers; i++)
        {
            XMStoreFloat4(&contactPos, g_raycast_hitpoints[i]);

            g_pOceanSurf->m_pOceanFX->GetVariableByName("g_ContactPosition")->AsVector()->SetFloatVector((FLOAT*)&contactPos);
            g_pOceanSurf->m_pRenderRayContactTechnique->GetPassByIndex(0)->Apply(0, pContext);
            pContext->DrawIndexed(12, 0, 0);
        }

        XMFLOAT4 origPos, rayDirection;

        // drawing rays to main buffer
        pContext->IASetPrimitiveTopology(D3D10_PRIMITIVE_TOPOLOGY_LINESTRIP);
        for( int i = 0; i < NumMarkers; i++)
        {
            XMStoreFloat4(&origPos, g_raycast_origins[i]);
            g_pOceanSurf->m_pOceanFX->GetVariableByName("g_OriginPosition")->AsVector()->SetFloatVector((FLOAT*)&origPos);

            XMVECTOR vecRayDir = g_raycast_directions[i] * 100.0f;
            XMStoreFloat4(&rayDirection, vecRayDir);

            g_pOceanSurf->m_pOceanFX->GetVariableByName("g_RayDirection")->AsVector()->SetFloatVector((FLOAT*) &rayDirection);
            g_pOceanSurf->m_pRenderRayContactTechnique->GetPassByIndex(1)->Apply(0, pContext);
            pContext->DrawIndexed(2, 0, 0);
        }

        //drawing sky to main buffer
        pEffect->GetTechniqueByName("RenderSky")->GetPassByIndex(0)->Apply(0, pContext);

        pContext->IASetInputLayout(trianglestrip_inputlayout);
        pContext->IASetPrimitiveTopology(D3D11_PRIMITIVE_TOPOLOGY_TRIANGLESTRIP);

        stride=sizeof(float)*6;
        pContext->IASetVertexBuffers(0,1,&sky_vertexbuffer,&stride,&offset);
        pContext->Draw(sky_gridpoints*(sky_gridpoints+2)*2, 0);

        pEffect->GetTechniqueByName("Default")->GetPassByIndex(0)->Apply(0, pContext);

        //restoring scene color buffer and back buffer
        pContext->OMSetRenderTargets( 1, &colorBuffer, backBuffer);
        pContext->ClearDepthStencilView(backBuffer, D3D11_CLEAR_DEPTH | D3D11_CLEAR_STENCIL, 1.f, 0);
        pContext->ClearRenderTargetView(colorBuffer, ClearColor);
        pContext->RSSetViewports( 1, &currentViewport );

        //resolving main buffer
        pContext->ResolveSubresource(main_color_resource_resolved,0,main_color_resource,0,DXGI_FORMAT_R8G8B8A8_UNORM);

        //drawing main buffer to back buffer
        pEffect->GetVariableByName("g_MainTexture")->AsShaderResource()->SetResource(main_color_resource_resolvedSRV);
        pEffect->GetVariableByName("g_MainBufferSizeMultiplier")->AsScalar()->SetFloat(main_buffer_size_multiplier);

        pContext->IASetInputLayout(trianglestrip_inputlayout);
        pContext->IASetPrimitiveTopology(D3D11_PRIMITIVE_TOPOLOGY_TRIANGLESTRIP);
        pEffect->GetTechniqueByName("MainToBackBuffer")->GetPassByIndex(0)->Apply(0, pContext);
        stride=sizeof(float)*6;
        pContext->IASetVertexBuffers(0,1,&heightfield_vertexbuffer,&stride,&offset);
        pContext->Draw(4, 0); // just need to pass 4 vertices to shader
        pEffect->GetTechniqueByName("Default")->GetPassByIndex(0)->Apply(0, pContext);

        pEffect->GetVariableByName("g_MainTexture")->AsShaderResource()->SetResource(NULL);

        SAFE_RELEASE ( colorBuffer );
        SAFE_RELEASE ( backBuffer );

        pContext->Release();
    }

    void RenderTerrainToHeightField(Matrix4f worldToViewMatrix, Matrix4f viewToProjectionMatrix,
                                    ReadableVector3f eyePositionWS, ReadableVector3f viewDirectionWS){
        float origin[/*2*/]={0,0};
        int stride=4*4;
        int offset=0;

//        pEffect->GetVariableByName("g_HeightfieldTexture")->AsShaderResource()->SetResource(heightmap_textureSRV);
//        pEffect->GetVariableByName("g_LayerdefTexture")->AsShaderResource()->SetResource(layerdef_textureSRV);
//        pEffect->GetVariableByName("g_RockBumpTexture")->AsShaderResource()->SetResource(rock_bump_textureSRV);
//        pEffect->GetVariableByName("g_SkyTexture")->AsShaderResource()->SetResource(sky_textureSRV);
//        pEffect->GetVariableByName("g_HeightFieldOrigin")->AsVector()->SetFloatVector(origin);
//        pEffect->GetVariableByName("g_HeightFieldSize")->AsScalar()->SetFloat(terrain_gridpoints*terrain_geometry_scale);

//        XMMATRIX worldToProjectionMatrix = worldToViewMatrix * viewToProjectionMatrix;
//        XMMATRIX projectionToWorldMatrix = XMMatrixInverse(NULL, worldToProjectionMatrix);
        Matrix4f.mul(viewToProjectionMatrix, worldToViewMatrix, m_ProjView);
        Matrix4f.invert(m_ProjView,m_ProjViewInv);
//    D3DXMatrixInverse(&projectionToWorldMatrix, NULL, &worldToProjectionMatrix);

//        XMFLOAT4X4 wvMat, wtpMat, ptwMat;
//        XMFLOAT3 epWS, vdWS;
//
//        XMStoreFloat4x4(&wvMat, worldToViewMatrix);
//        XMStoreFloat4x4(&wtpMat, worldToProjectionMatrix);
//        XMStoreFloat4x4(&ptwMat, projectionToWorldMatrix);
//        XMStoreFloat3(&epWS, eyePositionWS);
//        XMStoreFloat3(&vdWS, viewDirectionWS);
//
//        pEffect->GetVariableByName("g_ModelViewMatrix")->AsMatrix()->SetMatrix( (FLOAT*) &wvMat );
//        pEffect->GetVariableByName("g_ModelViewProjectionMatrix")->AsMatrix()->SetMatrix((FLOAT*)&wtpMat);
//        pEffect->GetVariableByName("g_ModelViewProjectionMatrixInv")->AsMatrix()->SetMatrix((FLOAT*)&ptwMat);
//        pEffect->GetVariableByName("g_CameraPosition")->AsVector()->SetFloatVector((FLOAT*)&epWS);
//        pEffect->GetVariableByName("g_CameraDirection")->AsVector()->SetFloatVector((FLOAT*)&vdWS);
//
//        pEffect->GetVariableByName("g_HalfSpaceCullSign")->AsScalar()->SetFloat(1.0);
//        pEffect->GetVariableByName("g_HalfSpaceCullPosition")->AsScalar()->SetFloat(terrain_minheight*20);
//
//        // drawing terrain to main buffer
//        pEffect->GetVariableByName("g_ApplyFog")->AsScalar()->SetFloat(0.0f);  TODO


//        enum PassPermutation   TODO
//        {
//            kPermutationSolid = 0,
//            kPermutationWireframe,
//            kPermutationDepthOnly,
//            kPermutationDataOnly
//        };
//
//        pContext->IASetInputLayout(heightfield_inputlayout);
//        pContext->IASetPrimitiveTopology(D3D11_PRIMITIVE_TOPOLOGY_1_CONTROL_POINT_PATCHLIST);
//        pEffect->GetTechniqueByName("RenderHeightfield")->GetPassByIndex( kPermutationDataOnly )->Apply( 0, pContext );
//        stride = sizeof(float) * 4;
//        pContext->IASetVertexBuffers( 0, 1, &heightfield_vertexbuffer, &stride,&offset );
//        pContext->Draw(terrain_numpatches_1d*terrain_numpatches_1d, 0);
    }

    private static int gp_wrap( int a)
    {
        if(a<0) return (a+terrain_gridpoints);
        if(a>=terrain_gridpoints) return (a-terrain_gridpoints);
        return a;
    }

    float bilinear_interpolation(float fx, float fy, float a, float b, float c, float d)
    {
        float s1,s2,s3,s4;
        s1=fx*fy;
        s2=(1-fx)*fy;
        s3=(1-fx)*(1-fy);
        s4=fx*(1-fy);
        return((a*s3+b*s4+c*s1+d*s2));
    }

    void CreateTerrain(){
        int i,j,k,l;
        float x,z;
        int ix,iz;
        float[] backterrain;
        int currentstep=terrain_gridpoints;
        float mv,rm;
        float offset=0,yscale=0,maxheight=0,minheight=0;

        float []height_linear_array;
        float []patches_rawdata;
//        HRESULT result;
        TextureDataDesc subresource_data = new TextureDataDesc();
        Texture2DDesc tex_desc = new Texture2DDesc();
//        D3D11_SHADER_RESOURCE_VIEW_DESC textureSRV_desc;

//        backterrain = (float *) malloc((terrain_gridpoints+1)*(terrain_gridpoints+1)*sizeof(float));
        backterrain = new float[(terrain_gridpoints+1)*(terrain_gridpoints+1)];
        rm=terrain_fractalinitialvalue;
        backterrain[0]=0;
        backterrain[0+terrain_gridpoints*terrain_gridpoints]=0;
        backterrain[terrain_gridpoints]=0;
        backterrain[terrain_gridpoints+terrain_gridpoints*terrain_gridpoints]=0;
        currentstep=terrain_gridpoints;
//        srand(3);

        // generating fractal terrain using square-diamond method
        while (currentstep>1)
        {
            //square step;
            i=0;
            j=0;


            while (i<terrain_gridpoints)
            {
                j=0;
                while (j<terrain_gridpoints)
                {

                    mv=backterrain[i+terrain_gridpoints*j];
                    mv+=backterrain[(i+currentstep)+terrain_gridpoints*j];
                    mv+=backterrain[(i+currentstep)+terrain_gridpoints*(j+currentstep)];
                    mv+=backterrain[i+terrain_gridpoints*(j+currentstep)];
                    mv/=4.0;
                    backterrain[i+currentstep/2+terrain_gridpoints*(j+currentstep/2)]=(mv+rm*(Numeric.random()-0.5f));
                    j+=currentstep;
                }
                i+=currentstep;
            }

            //diamond step;
            i=0;
            j=0;

            while (i<terrain_gridpoints)
            {
                j=0;
                while (j<terrain_gridpoints)
                {

                    mv=0;
                    mv=backterrain[i+terrain_gridpoints*j];
                    mv+=backterrain[(i+currentstep)+terrain_gridpoints*j];
                    mv+=backterrain[(i+currentstep/2)+terrain_gridpoints*(j+currentstep/2)];
                    mv+=backterrain[i+currentstep/2+terrain_gridpoints*gp_wrap(j-currentstep/2)];
                    mv/=4;
                    backterrain[i+currentstep/2+terrain_gridpoints*j]=(float)(mv+rm*(Numeric.random()-0.5f));

                    mv=0;
                    mv=backterrain[i+terrain_gridpoints*j];
                    mv+=backterrain[i+terrain_gridpoints*(j+currentstep)];
                    mv+=backterrain[(i+currentstep/2)+terrain_gridpoints*(j+currentstep/2)];
                    mv+=backterrain[gp_wrap(i-currentstep/2)+terrain_gridpoints*(j+currentstep/2)];
                    mv/=4;
                    backterrain[i+terrain_gridpoints*(j+currentstep/2)]=(float)(mv+rm*(Numeric.random()-0.5f));

                    mv=0;
                    mv=backterrain[i+currentstep+terrain_gridpoints*j];
                    mv+=backterrain[i+currentstep+terrain_gridpoints*(j+currentstep)];
                    mv+=backterrain[(i+currentstep/2)+terrain_gridpoints*(j+currentstep/2)];
                    mv+=backterrain[gp_wrap(i+currentstep/2+currentstep)+terrain_gridpoints*(j+currentstep/2)];
                    mv/=4;
                    backterrain[i+currentstep+terrain_gridpoints*(j+currentstep/2)]=(float)(mv+rm*(Numeric.random()-0.5f));

                    mv=0;
                    mv=backterrain[i+currentstep+terrain_gridpoints*(j+currentstep)];
                    mv+=backterrain[i+terrain_gridpoints*(j+currentstep)];
                    mv+=backterrain[(i+currentstep/2)+terrain_gridpoints*(j+currentstep/2)];
                    mv+=backterrain[i+currentstep/2+terrain_gridpoints*gp_wrap(j+currentstep/2+currentstep)];
                    mv/=4;
                    backterrain[i+currentstep/2+terrain_gridpoints*(j+currentstep)]=(float)(mv+rm*(Numeric.random()-0.5f));
                    j+=currentstep;
                }
                i+=currentstep;
            }
            //changing current step;
            currentstep/=2;
            rm*=terrain_fractalfactor;
        }

        // scaling to minheight..maxheight range
        for (i=0;i<terrain_gridpoints+1;i++)
            for (j=0;j<terrain_gridpoints+1;j++)
            {
                height[i][j]=backterrain[i+terrain_gridpoints*j];
            }
        maxheight=height[0][0];
        minheight=height[0][0];
        for(i=0;i<terrain_gridpoints+1;i++)
            for(j=0;j<terrain_gridpoints+1;j++)
            {
                if(height[i][j]>maxheight) maxheight=height[i][j];
                if(height[i][j]<minheight) minheight=height[i][j];
            }
        offset=minheight-terrain_minheight;
        yscale=(terrain_maxheight-terrain_minheight)/(maxheight-minheight);

        for(i=0;i<terrain_gridpoints+1;i++)
            for(j=0;j<terrain_gridpoints+1;j++)
            {
                height[i][j]-=minheight;
                height[i][j]*=yscale;
                height[i][j]+=terrain_minheight;
            }

        // moving down edges of heightmap
        for (i=0;i<terrain_gridpoints+1;i++)
            for (j=0;j<terrain_gridpoints+1;j++)
            {
                mv=((i-terrain_gridpoints/2.0f)*(i-terrain_gridpoints/2.0f)+(j-terrain_gridpoints/2.0f)*(j-terrain_gridpoints/2.0f));
                rm=((terrain_gridpoints*0.7f)*(terrain_gridpoints*0.7f)/4.0f);
                height[i][j]-=((mv-rm)/900.0f)*terrain_geometry_scale;
            }


        // terrain banks
        for(k=0;k<4;k++)
        {
            for(i=0;i<terrain_gridpoints+1;i++)
                for(j=0;j<terrain_gridpoints+1;j++)
                {
                    mv=height[i][j];
                    if((mv) > 0.02f)
                    {
                        mv -= 0.02f;
                    }
                    if(mv < -3.00f)
                    {
                        mv += 0.5f;
                    }
                    height[i][j]=mv;
                }
        }

//        XMVECTOR vec1, vec2, vec3;
        final Vector4f vec1 = new Vector4f();
        final Vector4f vec2 = new Vector4f();
        final Vector3f vec3 = new Vector3f();

        // smoothing
        for(k=0;k<terrain_smoothsteps;k++)
        {
            for(i=0;i<terrain_gridpoints+1;i++)
                for(j=0;j<terrain_gridpoints+1;j++)
                {
                    vec1.set(2 * terrain_geometry_scale, terrain_geometry_scale*(height[gp_wrap(i + 1)][j] - height[gp_wrap(i - 1)][j]), 0, 0);
                    vec2.set(0, -terrain_geometry_scale*(height[i][gp_wrap(j + 1)] - height[i][gp_wrap(j - 1)]), -2 * terrain_geometry_scale, 0);

// 				vec1.x=2*terrain_geometry_scale;
// 				vec1.y=terrain_geometry_scale*(height[gp_wrap(i+1)][j]-height[gp_wrap(i-1)][j]);
// 				vec1.z=0;
// 				vec2.x=0;
// 				vec2.y=-terrain_geometry_scale*(height[i][gp_wrap(j+1)]-height[i][gp_wrap(j-1)]);
// 				vec2.z=-2*terrain_geometry_scale;

//                    XMVECTOR vec3 = XMVector3Cross(vec1, vec2);
//                    vec3 = XMVector3Normalize(vec3);
                    Vector3f.cross(vec1 , vec2, vec3);
                    vec3.normalise();

                    if(((vec3.y>terrain_rockfactor)||(height[i][j]<1.2f)))
                    {
                        rm=terrain_smoothfactor1;
                        mv=height[i][j]*(1.0f-rm) +rm*0.25f*(height[gp_wrap(i-1)][j]+height[i][gp_wrap(j-1)]+height[gp_wrap(i+1)][j]+height[i][gp_wrap(j+1)]);
                        backterrain[i+terrain_gridpoints*j]=mv;
                    }
                    else
                    {
                        rm=terrain_smoothfactor2;
                        mv=height[i][j]*(1.0f-rm) +rm*0.25f*(height[gp_wrap(i-1)][j]+height[i][gp_wrap(j-1)]+height[gp_wrap(i+1)][j]+height[i][gp_wrap(j+1)]);
                        backterrain[i+terrain_gridpoints*j]=mv;
                    }

                }
            for (i=0;i<terrain_gridpoints+1;i++)
                for (j=0;j<terrain_gridpoints+1;j++)
                {
                    height[i][j]=(backterrain[i+terrain_gridpoints*j]);
                }
        }
        for(i=0;i<terrain_gridpoints+1;i++)
            for(j=0;j<terrain_gridpoints+1;j++)
            {
                rm=0.5f;
                mv=height[i][j]*(1.0f-rm) +rm*0.25f*(height[gp_wrap(i-1)][j]+height[i][gp_wrap(j-1)]+height[gp_wrap(i+1)][j]+height[i][gp_wrap(j+1)]);
                backterrain[i+terrain_gridpoints*j]=mv;
            }
        for (i=0;i<terrain_gridpoints+1;i++)
            for (j=0;j<terrain_gridpoints+1;j++)
            {
                height[i][j]=(backterrain[i+terrain_gridpoints*j]);
            }


        backterrain = null;

        //calculating normals
        for (i=0;i<terrain_gridpoints+1;i++)
            for (j=0;j<terrain_gridpoints+1;j++)
            {
                vec1.set(2 * terrain_geometry_scale, terrain_geometry_scale*(height[gp_wrap(i + 1)][j] - height[gp_wrap(i - 1)][j]), 0, 0);
                vec2.set(0, -terrain_geometry_scale*(height[i][gp_wrap(j + 1)] - height[i][gp_wrap(j - 1)]), -2 * terrain_geometry_scale, 0);

// 			vec1.x=2*terrain_geometry_scale;
// 			vec1.y=terrain_geometry_scale*(height[gp_wrap(i+1)][j]-height[gp_wrap(i-1)][j]);
// 			vec1.z=0;
// 			vec2.x=0;
// 			vec2.y=-terrain_geometry_scale*(height[i][gp_wrap(j+1)]-height[i][gp_wrap(j-1)]);
// 			vec2.z=-2*terrain_geometry_scale;

//                vec3 = XMVector3Cross(vec1, vec2);
//                vec3 = XMVector3Normalize(vec3);\
                Vector3f.cross(vec1 , vec2, vec3);
                vec3.normalise();

//                XMStoreFloat3(&normal[i][j], vec3);
                normal[i][j].set(vec3);
            }


        // buiding layerdef
        byte[] temp_layerdef_map_texture_pixels=new byte[terrain_layerdef_map_texture_size*terrain_layerdef_map_texture_size*4];
        byte[] layerdef_map_texture_pixels= new byte[terrain_layerdef_map_texture_size*terrain_layerdef_map_texture_size*4];
        for(i=0;i<terrain_layerdef_map_texture_size;i++)
            for(j=0;j<terrain_layerdef_map_texture_size;j++)
            {
                x=(float)(terrain_gridpoints)*((float)i/(float)terrain_layerdef_map_texture_size);
                z=(float)(terrain_gridpoints)*((float)j/(float)terrain_layerdef_map_texture_size);
                ix=(int)Math.floor(x);
                iz=(int)Math.floor(z);
                rm=bilinear_interpolation(x-ix,z-iz,height[ix][iz],height[ix+1][iz],height[ix+1][iz+1],height[ix][iz+1])*terrain_geometry_scale;

                temp_layerdef_map_texture_pixels[(j*terrain_layerdef_map_texture_size+i)*4+0]=0;
                temp_layerdef_map_texture_pixels[(j*terrain_layerdef_map_texture_size+i)*4+1]=0;
                temp_layerdef_map_texture_pixels[(j*terrain_layerdef_map_texture_size+i)*4+2]=0;
                temp_layerdef_map_texture_pixels[(j*terrain_layerdef_map_texture_size+i)*4+3]=0;

                if((rm>terrain_height_underwater_start)&&(rm<=terrain_height_underwater_end))
                {
                    temp_layerdef_map_texture_pixels[(j*terrain_layerdef_map_texture_size+i)*4+0]= (byte) 255;
                    temp_layerdef_map_texture_pixels[(j*terrain_layerdef_map_texture_size+i)*4+1]=0;
                    temp_layerdef_map_texture_pixels[(j*terrain_layerdef_map_texture_size+i)*4+2]=0;
                    temp_layerdef_map_texture_pixels[(j*terrain_layerdef_map_texture_size+i)*4+3]=0;
                }

                if((rm>terrain_height_sand_start)&&(rm<=terrain_height_sand_end))
                {
                    temp_layerdef_map_texture_pixels[(j*terrain_layerdef_map_texture_size+i)*4+0]=0;
                    temp_layerdef_map_texture_pixels[(j*terrain_layerdef_map_texture_size+i)*4+1]= (byte) 255;
                    temp_layerdef_map_texture_pixels[(j*terrain_layerdef_map_texture_size+i)*4+2]=0;
                    temp_layerdef_map_texture_pixels[(j*terrain_layerdef_map_texture_size+i)*4+3]=0;
                }

                if((rm>terrain_height_grass_start)&&(rm<=terrain_height_grass_end))
                {
                    temp_layerdef_map_texture_pixels[(j*terrain_layerdef_map_texture_size+i)*4+0]=0;
                    temp_layerdef_map_texture_pixels[(j*terrain_layerdef_map_texture_size+i)*4+1]=0;
                    temp_layerdef_map_texture_pixels[(j*terrain_layerdef_map_texture_size+i)*4+2]= (byte) 255;
                    temp_layerdef_map_texture_pixels[(j*terrain_layerdef_map_texture_size+i)*4+3]=0;
                }

                mv=bilinear_interpolation(x-ix,z-iz,normal[ix][iz].y,normal[ix+1][iz].y,normal[ix+1][iz+1].y,normal[ix][iz+1].y);

                if((mv<terrain_slope_grass_start)&&(rm>terrain_height_sand_end))
                {
                    temp_layerdef_map_texture_pixels[(j*terrain_layerdef_map_texture_size+i)*4+0]=0;
                    temp_layerdef_map_texture_pixels[(j*terrain_layerdef_map_texture_size+i)*4+1]=0;
                    temp_layerdef_map_texture_pixels[(j*terrain_layerdef_map_texture_size+i)*4+2]=0;
                    temp_layerdef_map_texture_pixels[(j*terrain_layerdef_map_texture_size+i)*4+3]=0;
                }

                if((mv<terrain_slope_rocks_start)&&(rm>terrain_height_rocks_start))
                {
                    temp_layerdef_map_texture_pixels[(j*terrain_layerdef_map_texture_size+i)*4+0]=0;
                    temp_layerdef_map_texture_pixels[(j*terrain_layerdef_map_texture_size+i)*4+1]=0;
                    temp_layerdef_map_texture_pixels[(j*terrain_layerdef_map_texture_size+i)*4+2]=0;
                    temp_layerdef_map_texture_pixels[(j*terrain_layerdef_map_texture_size+i)*4+3]= (byte) 255;
                }

            }
        for(i=0;i<terrain_layerdef_map_texture_size;i++)
            for(j=0;j<terrain_layerdef_map_texture_size;j++)
            {
                layerdef_map_texture_pixels[(j*terrain_layerdef_map_texture_size+i)*4+0]=temp_layerdef_map_texture_pixels[(j*terrain_layerdef_map_texture_size+i)*4+0];
                layerdef_map_texture_pixels[(j*terrain_layerdef_map_texture_size+i)*4+1]=temp_layerdef_map_texture_pixels[(j*terrain_layerdef_map_texture_size+i)*4+1];
                layerdef_map_texture_pixels[(j*terrain_layerdef_map_texture_size+i)*4+2]=temp_layerdef_map_texture_pixels[(j*terrain_layerdef_map_texture_size+i)*4+2];
                layerdef_map_texture_pixels[(j*terrain_layerdef_map_texture_size+i)*4+3]=temp_layerdef_map_texture_pixels[(j*terrain_layerdef_map_texture_size+i)*4+3];
            }


        for(i=2;i<terrain_layerdef_map_texture_size-2;i++)
            for(j=2;j<terrain_layerdef_map_texture_size-2;j++)
            {
                int n1=0;
                int n2=0;
                int n3=0;
                int n4=0;
                for(k=-2;k<3;k++)
                    for(l=-2;l<3;l++)
                    {
                        n1+=temp_layerdef_map_texture_pixels[((j+k)*terrain_layerdef_map_texture_size+i+l)*4+0];
                        n2+=temp_layerdef_map_texture_pixels[((j+k)*terrain_layerdef_map_texture_size+i+l)*4+1];
                        n3+=temp_layerdef_map_texture_pixels[((j+k)*terrain_layerdef_map_texture_size+i+l)*4+2];
                        n4+=temp_layerdef_map_texture_pixels[((j+k)*terrain_layerdef_map_texture_size+i+l)*4+3];
                    }
                layerdef_map_texture_pixels[(j*terrain_layerdef_map_texture_size+i)*4+0]=(byte)(n1/25);
                layerdef_map_texture_pixels[(j*terrain_layerdef_map_texture_size+i)*4+1]=(byte)(n2/25);
                layerdef_map_texture_pixels[(j*terrain_layerdef_map_texture_size+i)*4+2]=(byte)(n3/25);
                layerdef_map_texture_pixels[(j*terrain_layerdef_map_texture_size+i)*4+3]=(byte)(n4/25);
            }

        // putting the generated data to textures
//        subresource_data.pSysMem = layerdef_map_texture_pixels;
//        subresource_data.SysMemPitch = terrain_layerdef_map_texture_size*4;
//        subresource_data.SysMemSlicePitch = 0;
        subresource_data.data = layerdef_map_texture_pixels;
        subresource_data.format = TextureUtils.measureFormat(GLenum.GL_RGBA8);
        subresource_data.type = TextureUtils.measureDataType(GLenum.GL_RGBA8);

        tex_desc.width = terrain_layerdef_map_texture_size;
        tex_desc.height = terrain_layerdef_map_texture_size;
        tex_desc.mipLevels = 1;
        tex_desc.arraySize = 1;
        tex_desc.format = GLenum.GL_RGBA8; // DXGI_FORMAT_R8G8B8A8_UNORM;
//        tex_desc.SampleDesc.Count = 1;
//        tex_desc.SampleDesc.Quality = 0;
//        tex_desc.Usage = D3D11_USAGE_DEFAULT;
//        tex_desc.BindFlags = D3D11_BIND_SHADER_RESOURCE;
//        tex_desc.CPUAccessFlags = 0;
//        tex_desc.MiscFlags = 0;
//        result=pDevice->CreateTexture2D(&tex_desc,&subresource_data,&layerdef_texture);
        layerdef_textureSRV = layerdef_texture = TextureUtils.createTexture2D(tex_desc, subresource_data);

//        ZeroMemory(&textureSRV_desc,sizeof(textureSRV_desc));
//        textureSRV_desc.Format=tex_desc.Format;
//        textureSRV_desc.ViewDimension=D3D11_SRV_DIMENSION_TEXTURE2D;
//        textureSRV_desc.Texture2D.MipLevels=tex_desc.MipLevels;
//        textureSRV_desc.Texture2D.MostDetailedMip=0;
//        pDevice->CreateShaderResourceView(layerdef_texture,&textureSRV_desc,&layerdef_textureSRV);
//
//        free(temp_layerdef_map_texture_pixels);
//        free(layerdef_map_texture_pixels);

        height_linear_array = new float [terrain_gridpoints*terrain_gridpoints*4];
        patches_rawdata = new float [terrain_numpatches_1d*terrain_numpatches_1d*4];

        for(i=0;i<terrain_gridpoints;i++)
            for(j=0; j<terrain_gridpoints;j++)
            {
                height_linear_array[(i+j*terrain_gridpoints)*4+0]=normal[i][j].x;
                height_linear_array[(i+j*terrain_gridpoints)*4+1]=normal[i][j].y;
                height_linear_array[(i+j*terrain_gridpoints)*4+2]=normal[i][j].z;
                height_linear_array[(i+j*terrain_gridpoints)*4+3]=height[i][j];
            }
//        subresource_data.pSysMem = height_linear_array;
//        subresource_data.SysMemPitch = terrain_gridpoints*4*sizeof(float);
//        subresource_data.SysMemSlicePitch = 0;
        subresource_data.data = height_linear_array;
        subresource_data.format = TextureUtils.measureFormat(GLenum.GL_RGBA32F);
        subresource_data.type = TextureUtils.measureDataType(GLenum.GL_RGBA32F);

        tex_desc.width = terrain_gridpoints;
        tex_desc.height = terrain_gridpoints;
        tex_desc.mipLevels = 1;
        tex_desc.arraySize = 1;
        tex_desc.format = GLenum.GL_RGBA32F;
//        tex_desc.SampleDesc.Count = 1;
//        tex_desc.SampleDesc.Quality = 0;
//        tex_desc.Usage = D3D11_USAGE_DEFAULT;
//        tex_desc.BindFlags = D3D11_BIND_SHADER_RESOURCE;
//        tex_desc.CPUAccessFlags = 0;
//        tex_desc.MiscFlags = 0;
//        result=pDevice->CreateTexture2D(&tex_desc,&subresource_data,&heightmap_texture);

//        free(height_linear_array);

//        ZeroMemory(&textureSRV_desc,sizeof(textureSRV_desc));
//        textureSRV_desc.Format=tex_desc.Format;
//        textureSRV_desc.ViewDimension=D3D11_SRV_DIMENSION_TEXTURE2D;
//        textureSRV_desc.Texture2D.MipLevels=tex_desc.MipLevels;
//        pDevice->CreateShaderResourceView(heightmap_texture,&textureSRV_desc,&heightmap_textureSRV);
        heightmap_texture = heightmap_textureSRV = TextureUtils.createTexture2D(tex_desc, subresource_data);

        //building depthmap
        byte[] depth_shadow_map_texture_pixels= new byte[terrain_depth_shadow_map_texture_size*terrain_depth_shadow_map_texture_size*4];
        for(i=0;i<terrain_depth_shadow_map_texture_size;i++)
            for(j=0;j<terrain_depth_shadow_map_texture_size;j++)
            {
                x=(float)(terrain_gridpoints)*((float)i/(float)terrain_depth_shadow_map_texture_size);
                z=(float)(terrain_gridpoints)*((float)j/(float)terrain_depth_shadow_map_texture_size);
                ix=(int)Math.floor(x);
                iz=(int)Math.floor(z);
                rm=bilinear_interpolation(x-ix,z-iz,height[ix][iz],height[ix+1][iz],height[ix+1][iz+1],height[ix][iz+1])*terrain_geometry_scale;

                if(rm>0)
                {
                    depth_shadow_map_texture_pixels[(j*terrain_depth_shadow_map_texture_size+i)*4+0]=0;
                    depth_shadow_map_texture_pixels[(j*terrain_depth_shadow_map_texture_size+i)*4+1]=0;
                    depth_shadow_map_texture_pixels[(j*terrain_depth_shadow_map_texture_size+i)*4+2]=0;
                }
                else
                {
                    float no=(1.0f*255.0f*(rm/(terrain_minheight*terrain_geometry_scale)))-1.0f;
                    if(no>255) no=255;
                    if(no<0) no=0;
                    depth_shadow_map_texture_pixels[(j*terrain_depth_shadow_map_texture_size+i)*4+0]=(byte)no;

                    no=(10.0f*255.0f*(rm/(terrain_minheight*terrain_geometry_scale)))-80.0f;
                    if(no>255) no=255;
                    if(no<0) no=0;
                    depth_shadow_map_texture_pixels[(j*terrain_depth_shadow_map_texture_size+i)*4+1]=(byte)no;

                    no=(100.0f*255.0f*(rm/(terrain_minheight*terrain_geometry_scale)))-300.0f;
                    if(no>255) no=255;
                    if(no<0) no=0;
                    depth_shadow_map_texture_pixels[(j*terrain_depth_shadow_map_texture_size+i)*4+2]=(byte)no;
                }
                depth_shadow_map_texture_pixels[(j*terrain_depth_shadow_map_texture_size+i)*4+3]=0;
            }

        subresource_data.data = depth_shadow_map_texture_pixels;
        subresource_data.format = GLenum.GL_RGBA;
        subresource_data.type = GLenum.GL_UNSIGNED_BYTE;

        tex_desc.width = terrain_depth_shadow_map_texture_size;
        tex_desc.height = terrain_depth_shadow_map_texture_size;
        tex_desc.mipLevels = 1;
        tex_desc.arraySize = 1;
        tex_desc.format = GLenum.GL_RGBA8; // DXGI_FORMAT_R8G8B8A8_UNORM;
//        tex_desc.SampleDesc.Count = 1;
//        tex_desc.SampleDesc.Quality = 0;
//        tex_desc.Usage = D3D11_USAGE_DEFAULT;
//        tex_desc.BindFlags = D3D11_BIND_SHADER_RESOURCE;
//        tex_desc.CPUAccessFlags = 0;
//        tex_desc.MiscFlags = 0;
//        result=pDevice->CreateTexture2D(&tex_desc,&subresource_data,&depthmap_texture);
//
//        ZeroMemory(&textureSRV_desc,sizeof(textureSRV_desc));
//        textureSRV_desc.Format=tex_desc.Format;
//        textureSRV_desc.ViewDimension=D3D11_SRV_DIMENSION_TEXTURE2D;
//        textureSRV_desc.Texture2D.MipLevels=tex_desc.MipLevels;
//        pDevice->CreateShaderResourceView(depthmap_texture,&textureSRV_desc,&depthmap_textureSRV);

        depth_shadow_map_texture_pixels = null;

        // creating terrain vertex buffer
        for(i=0;i<terrain_numpatches_1d;i++)
            for(j=0;j<terrain_numpatches_1d;j++)
            {
                patches_rawdata[(i+j*terrain_numpatches_1d)*4+0]=i*terrain_geometry_scale*terrain_gridpoints/terrain_numpatches_1d;
                patches_rawdata[(i+j*terrain_numpatches_1d)*4+1]=j*terrain_geometry_scale*terrain_gridpoints/terrain_numpatches_1d;
                patches_rawdata[(i+j*terrain_numpatches_1d)*4+2]=terrain_geometry_scale*terrain_gridpoints/terrain_numpatches_1d;
                patches_rawdata[(i+j*terrain_numpatches_1d)*4+3]=terrain_geometry_scale*terrain_gridpoints/terrain_numpatches_1d;
            }

//        D3D11_BUFFER_DESC buf_desc;
//        memset(&buf_desc,0,sizeof(buf_desc));
//
//        buf_desc.ByteWidth = terrain_numpatches_1d*terrain_numpatches_1d*4*sizeof(float);
//        buf_desc.BindFlags = D3D11_BIND_VERTEX_BUFFER;
//        buf_desc.Usage = D3D11_USAGE_DEFAULT;
//
//        subresource_data.pSysMem=patches_rawdata;
//        subresource_data.SysMemPitch=0;
//        subresource_data.SysMemSlicePitch=0;
//
//        result=pDevice->CreateBuffer(&buf_desc,&subresource_data,&heightfield_vertexbuffer);
//        free (patches_rawdata);
        heightfield_vertexbuffer = new BufferGL();
        heightfield_vertexbuffer.initlize(GLenum.GL_ARRAY_BUFFER, terrain_numpatches_1d*terrain_numpatches_1d*4*4, CacheBuffer.wrap(patches_rawdata), GLenum.GL_STATIC_DRAW);
        heightfield_vertexbuffer.unbind();

        // creating sky vertex buffer
        float[] sky_vertexdata;
        int floatnum;
        sky_vertexdata = new float [sky_gridpoints*(sky_gridpoints+2)*2*6];

        for(j=0;j<sky_gridpoints;j++)
        {
            i=0;
            floatnum=(j*(sky_gridpoints+2)*2)*6;
            sky_vertexdata[floatnum+0]= (float) (terrain_gridpoints*terrain_geometry_scale*0.5f+4000.0f*Math.cos(2.0*Math.PI*i/sky_gridpoints)*Math.cos(-0.5f*Math.PI+Math.PI*j/sky_gridpoints));
            sky_vertexdata[floatnum+1]= (float) (4000.0f*Math.sin(-0.5f*Math.PI+Math.PI*j/sky_gridpoints));
            sky_vertexdata[floatnum+2]= (float) (terrain_gridpoints*terrain_geometry_scale*0.5f+4000.0f*Math.sin(2.0f*Math.PI*(float)i/(float)sky_gridpoints)*Math.cos(-0.5f*Math.PI+Math.PI*(float)j/(float)sky_gridpoints));
            sky_vertexdata[floatnum+3]=1;
            sky_vertexdata[floatnum+4]=(sky_texture_angle+(float)i/(float)sky_gridpoints);
            sky_vertexdata[floatnum+5]=2.0f-2.0f*(float)(j+0)/(float)sky_gridpoints;
            floatnum+=6;
            for(i=0;i<sky_gridpoints+1;i++)
            {
                sky_vertexdata[floatnum+0]= (float) (terrain_gridpoints*terrain_geometry_scale*0.5f+4000.0f*Math.cos(2.0f*Math.PI*(float)i/(float)sky_gridpoints)*Math.cos(-0.5f*Math.PI+Math.PI*(float)j/(float)sky_gridpoints));
                sky_vertexdata[floatnum+1]= (float) (4000.0f*Math.sin(-0.5f*Math.PI+Math.PI*(float)(j)/(float)sky_gridpoints));
                sky_vertexdata[floatnum+2]= (float) (terrain_gridpoints*terrain_geometry_scale*0.5f+4000.0f*Math.sin(2.0f*Math.PI*(float)i/(float)sky_gridpoints)*Math.cos(-0.5f*Math.PI+Math.PI*(float)j/(float)sky_gridpoints));
                sky_vertexdata[floatnum+3]=1;
                sky_vertexdata[floatnum+4]=(sky_texture_angle+(float)i/(float)sky_gridpoints);
                sky_vertexdata[floatnum+5]=2.0f-2.0f*(float)(j+0)/(float)sky_gridpoints;
                floatnum+=6;
                sky_vertexdata[floatnum+0]= (float) (terrain_gridpoints*terrain_geometry_scale*0.5f+4000.0f*Math.cos(2.0f*Math.PI*(float)i/(float)sky_gridpoints)*Math.cos(-0.5f*Math.PI+Math.PI*(float)(j+1)/(float)sky_gridpoints));
                sky_vertexdata[floatnum+1]= (float) (4000.0f*Math.sin(-0.5f*Math.PI+Math.PI*(float)(j+1)/(float)sky_gridpoints));
                sky_vertexdata[floatnum+2]= (float) (terrain_gridpoints*terrain_geometry_scale*0.5f+4000.0f*Math.sin(2.0f*Math.PI*(float)i/(float)sky_gridpoints)*Math.cos(-0.5f*Math.PI+Math.PI*(float)(j+1)/(float)sky_gridpoints));
                sky_vertexdata[floatnum+3]=1;
                sky_vertexdata[floatnum+4]=(sky_texture_angle+(float)i/(float)sky_gridpoints);
                sky_vertexdata[floatnum+5]=2.0f-2.0f*(float)(j+1)/(float)sky_gridpoints;
                floatnum+=6;
            }
            i=sky_gridpoints;
            sky_vertexdata[floatnum+0]= (float) (terrain_gridpoints*terrain_geometry_scale*0.5f+4000.0f*Math.cos(2.0f*Math.PI*(float)i/(float)sky_gridpoints)*Math.cos(-0.5f*Math.PI+Math.PI*(float)(j+1)/(float)sky_gridpoints));
            sky_vertexdata[floatnum+1]= (float) (4000.0f*Math.sin(-0.5f*Math.PI+Math.PI*(float)(j+1)/(float)sky_gridpoints));
            sky_vertexdata[floatnum+2]= (float) (terrain_gridpoints*terrain_geometry_scale*0.5f+4000.0f*Math.sin(2.0f*Math.PI*(float)i/(float)sky_gridpoints)*Math.cos(-0.5f*Math.PI+Math.PI*(float)(j+1)/(float)sky_gridpoints));
            sky_vertexdata[floatnum+3]=1;
            sky_vertexdata[floatnum+4]=(sky_texture_angle+(float)i/(float)sky_gridpoints);
            sky_vertexdata[floatnum+5]=2.0f-2.0f*(float)(j+1)/(float)sky_gridpoints;
            floatnum+=6;
        }

//        memset(&buf_desc,0,sizeof(buf_desc));
//
//        buf_desc.ByteWidth = sky_gridpoints*(sky_gridpoints+2)*2*6*sizeof(float);
//        buf_desc.BindFlags = D3D11_BIND_VERTEX_BUFFER;
//        buf_desc.Usage = D3D11_USAGE_DEFAULT;
//
//        subresource_data.pSysMem=sky_vertexdata;
//        subresource_data.SysMemPitch=0;
//        subresource_data.SysMemSlicePitch=0;
//
//        result=pDevice->CreateBuffer(&buf_desc,&subresource_data,&sky_vertexbuffer);
//
//        free (sky_vertexdata);
        sky_vertexbuffer = new BufferGL();
        sky_vertexbuffer.initlize(GLenum.GL_ARRAY_BUFFER, sky_gridpoints*(sky_gridpoints+2)*2*6*4, CacheBuffer.wrap(sky_vertexdata) ,GLenum.GL_STATIC_DRAW);
    }

    float DynamicTesselationFactor;
    void SetupNormalView(Matrix4f matProj, Matrix4f matView ){

    }

    void SetupReflectionView(Matrix4f matProj, Matrix4f matView){

    }

    void SetupRefractionView(Matrix4f matProj, Matrix4f matView ){

    }

    void SetupLightView(Matrix4f matProj, Matrix4f matView){

    }
    float BackbufferWidth;
    float BackbufferHeight;
}
