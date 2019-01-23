package jet.opengl.demos.intel.coarse;

import com.nvidia.developer.opengl.models.sdkmesh.SDKmesh;
import com.nvidia.developer.opengl.utils.NvGPUTimer;

import org.lwjgl.util.vector.Matrix;
import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Readable;
import org.lwjgl.util.vector.Vector3f;
import org.lwjgl.util.vector.Vector4f;

import java.nio.ByteBuffer;
import java.util.Random;

import jet.opengl.demos.amdfx.common.CFirstPersonCamera;
import jet.opengl.demos.intel.cput.D3D11_INPUT_ELEMENT_DESC;
import jet.opengl.demos.intel.cput.ID3D11InputLayout;
import jet.opengl.postprocessing.buffer.BufferGL;
import jet.opengl.postprocessing.common.Disposeable;
import jet.opengl.postprocessing.common.GLFuncProvider;
import jet.opengl.postprocessing.common.GLFuncProviderFactory;
import jet.opengl.postprocessing.common.GLenum;
import jet.opengl.postprocessing.shader.GLSLProgram;
import jet.opengl.postprocessing.shader.Macro;
import jet.opengl.postprocessing.shader.ShaderProgram;
import jet.opengl.postprocessing.texture.RenderTargets;
import jet.opengl.postprocessing.texture.SamplerDesc;
import jet.opengl.postprocessing.texture.SamplerUtils;
import jet.opengl.postprocessing.texture.Texture2D;
import jet.opengl.postprocessing.texture.Texture2DDesc;
import jet.opengl.postprocessing.texture.TextureGL;
import jet.opengl.postprocessing.texture.TextureUtils;
import jet.opengl.postprocessing.util.CacheBuffer;
import jet.opengl.postprocessing.util.CommonUtil;
import jet.opengl.postprocessing.util.Numeric;

final class App implements Disposeable {

    static final int MAX_LIGHTS_POWER = 10;
    static final int MAX_LOOP_COUNT  = 100;
    static final int MAX_LIGHTS = (1<<MAX_LIGHTS_POWER);

    // This determines the tile size for light binning and associated tradeoffs
    static final int COMPUTE_SHADER_TILE_GROUP_DIM = 32;
    static final int COMPUTE_SHADER_TILE_GROUP_SIZE = (COMPUTE_SHADER_TILE_GROUP_DIM*COMPUTE_SHADER_TILE_GROUP_DIM);

    // If enabled, defers scheduling of per-pixel-shaded pixels until after top-left pixel
    // has been shaded across the whole tile. This allows better SIMD packing and scheduling.
    static final int DEFER_PER_PIXEL = 1;

    private static final int
            TIMER_GBUFFER_GEN = 0,
            TIMER_SHADING = 1,
            TIMER_SKYBOX = 2,
            TIMER_COUNT = 3;

    private static final int
            T_BEGIN = 0,
            T_END = 1,
//            T_VALID = 2,
            T_NUM = 2;

    private GLFuncProvider gl;

    private int mMSAASamples;
    private float mTotalTime;

    private ID3D11InputLayout mMeshVertexLayout;

//    private ShaderProgram mGeometryVS;

    private GLSLProgram mGeometryPassVS;

    private GLSLProgram mGBufferPS;
    private GLSLProgram mGBufferAlphaTestPS;

    private GLSLProgram mForwardPS;
    private GLSLProgram mForwardAlphaTestPS;
    private GLSLProgram mForwardAlphaTestOnlyPS;

    private SDKmesh mSkyboxMesh;
//    VertexShader* mSkyboxVS;
    private GLSLProgram mSkyboxPS;

//    VertexShader* mFullScreenTriangleVS;

    private GLSLProgram mRequiresPerSampleShadingPS;

    private GLSLProgram mBasicLoopPS;
    private GLSLProgram mBasicLoopPerSamplePS;

    private GLSLProgram mComputeShaderTileCS;

//    VertexShader* mGPUQuadVS;
//    GeometryShader* mGPUQuadGS;
    private GLSLProgram mGPUQuadPS;
    private GLSLProgram mGPUQuadPerSamplePS;

    private GLSLProgram mGPUQuadDLPS;
    private GLSLProgram mGPUQuadDLPerSamplePS;

    private GLSLProgram mGPUQuadDLResolvePS;
    private GLSLProgram mGPUQuadDLResolvePerSamplePS;

    private BufferGL mPerFrameConstants;

    private Runnable mRasterizerState;
    private Runnable mDoubleSidedRasterizerState;

    private Runnable mDepthState;
    private Runnable mWriteStencilState;
    private Runnable mEqualStencilState;

    private Runnable mGeometryBlendState;
    private Runnable mLightingBlendState;

    private int mDiffuseSampler;
    private int mDPSDiffuseSampler;

    private Texture2D[] mGBuffer;
    // Handy cache of list of RT pointers for G-buffer
    private Texture2D[] mGBufferRTV;
    // Handy cache of list of SRV pointers for the G-buffer
    private Texture2D[] mGBufferSRV;
    private int mGBufferWidth;
    private int mGBufferHeight;

    // We use a different lit buffer (different bind flags and MSAA handling) depending on whether we
    // write to it from the pixel shader (render target) or compute shader (UAV)
    private Texture2D mLitBufferPS;
    private BufferGL mLitBufferCS;

    // A temporary accumulation buffer used for deferred lighting
    private Texture2D mDeferredLightingAccumBuffer;

    private Texture2D mDepthBuffer;
    // We also need a read-only depth stencil view for techniques that read the G-buffer while also using Z-culling
    private Texture2D mDepthBufferReadOnlyDSV;

    // Lighting state
    private int mActiveLights;
    private PointLightInitTransform[] mLightInitialTransform;
    private PointLight[] mPointLightParameters;
    private Vector3f[] mPointLightPositionWorld;

    private BufferGL mLightBuffer;
    private RenderTargets m_FBO;

    private boolean mDebug = true;

    // Queries
    private NvGPUTimer[] m_pTimers = new NvGPUTimer[TIMER_COUNT];
    private final FrameStats m_frameStats = new FrameStats();

    @Override
    public void dispose() {

    }

    private static final class PerFrameConstants implements Readable {
        static final int SIZE = Matrix4f.SIZE*4 + Vector4f.SIZE * 4;

        final Matrix4f mCameraWorldViewProj = new Matrix4f();
        final Matrix4f mCameraWorldView = new Matrix4f();
        final Matrix4f mCameraViewProj = new Matrix4f();
        final Matrix4f mCameraProj = new Matrix4f();
        final Vector4f mCameraNearFar = new Vector4f();

        int mFramebufferDimensionsX;
        int mFramebufferDimensionsY;
        int mFramebufferDimensionsZ;
        int mFramebufferDimensionsW;

        UIConstants mUI;

        @Override
        public ByteBuffer store(ByteBuffer buf) {
            mCameraWorldViewProj.store(buf);
            mCameraWorldView.store(buf);
            mCameraViewProj.store(buf);
            mCameraProj.store(buf);
            mCameraNearFar.store(buf);

            buf.putInt(mFramebufferDimensionsX);
            buf.putInt(mFramebufferDimensionsY);
            buf.putInt(mFramebufferDimensionsZ);
            buf.putInt(mFramebufferDimensionsW);

            if(mUI != null){
                buf.putInt(mUI.forcePerPixel);
                buf.putInt(mUI.lightingOnly);
                buf.putInt(mUI.faceNormals);
                buf.putInt(mUI.visualizeLightCount);
                buf.putInt(mUI.visualizePerSampleShading);
                buf.putInt(mUI.lightCullTechnique);
                buf.putLong(0);
            }else{
                buf.putLong(0);
                buf.putLong(0);
                buf.putLong(0);
                buf.putLong(0);
            }

            return buf;
        }
    }

    App(/*ID3D11Device* d3dDevice, unsigned*/ int activeLights, int msaaSamples){
        gl = GLFuncProviderFactory.getGLFuncProvider();
        m_FBO = new RenderTargets();

        mMSAASamples = msaaSamples;

        // Set up macros
        Macro defines[] = { new Macro("MSAA_SAMPLES", mMSAASamples)};

        // Create shaders
//        mGeometryVS = new VertexShader(d3dDevice, L"Rendering.hlsl", "GeometryVS", defines);

        final String root = "Intel\\DeferredCoarsePixelShading\\shaders\\";
        final String quadVS = "shader_libs/PostProcessingDefaultScreenSpaceVS";

        mGeometryPassVS = GLSLProgram.createProgram(root + "GeometryVS.vert", null, defines);
        mGBufferPS = //new PixelShader(d3dDevice, L"GBuffer.hlsl", "GBufferPS", defines);
                GLSLProgram.createProgram(root + "GeometryVS.vert", root + "GBufferPS.frag", defines);
        mGBufferAlphaTestPS = //new PixelShader(d3dDevice, L"GBuffer.hlsl", "GBufferAlphaTestPS", defines);
                GLSLProgram.createProgram(root + "GeometryVS.vert", root + "GBufferAlphaTestPS.frag", defines);

        mForwardPS = //new PixelShader(d3dDevice, L"Forward.hlsl", "ForwardPS", defines);
                GLSLProgram.createProgram(root + "GeometryVS.vert", root + "ForwardPS.frag", defines);

        mForwardAlphaTestPS = //new PixelShader(d3dDevice, L"Forward.hlsl", "ForwardAlphaTestPS", defines);
                GLSLProgram.createProgram(root + "GeometryVS.vert", root + "ForwardAlphaTestPS.frag", defines);

        mForwardAlphaTestOnlyPS = //new PixelShader(d3dDevice, L"Forward.hlsl", "ForwardAlphaTestOnlyPS", defines);
                GLSLProgram.createProgram(root + "GeometryVS.vert", root + "ForwardAlphaTestOnlyPS.frag", defines);

//        mFullScreenTriangleVS = new VertexShader(d3dDevice, L"Rendering.hlsl", "FullScreenTriangleVS", defines);

//        mSkyboxVS = new VertexShader(d3dDevice, L"SkyboxToneMap.hlsl", "SkyboxVS", defines);
        mSkyboxPS = //new PixelShader(d3dDevice, L"SkyboxToneMap.hlsl", "SkyboxPS", defines);
                GLSLProgram.createProgram(root + "SkyboxVS.vert", root + "SkyboxPS.frag", defines);

        mRequiresPerSampleShadingPS = // new PixelShader(d3dDevice, L"GBuffer.hlsl", "RequiresPerSampleShadingPS", defines);
                GLSLProgram.createProgram(quadVS, root + "RequiresPerSampleShadingPS.frag", defines);

        mBasicLoopPS = //new PixelShader(d3dDevice, L"BasicLoop.hlsl", "BasicLoopPS", defines);
                GLSLProgram.createProgram(quadVS, root + "BasicLoopPS.frag", defines);
        mBasicLoopPerSamplePS = // new PixelShader(d3dDevice, L"BasicLoop.hlsl", "BasicLoopPerSamplePS", defines);
                GLSLProgram.createProgram(quadVS, root + "BasicLoopPerSamplePS.frag", defines);
        mComputeShaderTileCS = // new ComputeShader(d3dDevice, NULL, L"ComputeShaderTileCS.fxo");
                GLSLProgram.createProgram(root + "ComputeShaderTileCS.comp", defines);
//        mGPUQuadVS = new VertexShader(d3dDevice, L"GPUQuad.hlsl", "GPUQuadVS", defines);
//        mGPUQuadGS = new GeometryShader(d3dDevice, L"GPUQuad.hlsl", "GPUQuadGS", defines);
        mGPUQuadPS = //new PixelShader(d3dDevice, L"GPUQuad.hlsl", "GPUQuadPS", defines);
                GLSLProgram.createProgram(root + "GPUQuadVS.vert", root+ "GPUQuadGS.gemo", root + "GPUQuadPS.frag", defines);
        mGPUQuadPerSamplePS = //new PixelShader(d3dDevice, L"GPUQuad.hlsl", "GPUQuadPerSamplePS", defines);
                GLSLProgram.createProgram(root + "GPUQuadVS.vert", root+ "GPUQuadGS.gemo", root + "GPUQuadPerSamplePS.frag", defines);

        mGPUQuadDLPS = // new PixelShader(d3dDevice, L"GPUQuadDL.hlsl", "GPUQuadDLPS", defines);
                GLSLProgram.createProgram(root + "GPUQuadVS.vert", root+ "GPUQuadGS.gemo", root + "GPUQuadDLPS.frag", defines);
        mGPUQuadDLPerSamplePS = //new PixelShader(d3dDevice, L"GPUQuadDL.hlsl", "GPUQuadDLPerSamplePS", defines);
                GLSLProgram.createProgram(root + "GPUQuadVS.vert", root+ "GPUQuadGS.gemo", root + "GPUQuadDLPerSamplePS.frag", defines);

        mGPUQuadDLResolvePS = // new PixelShader(d3dDevice, L"GPUQuadDL.hlsl", "GPUQuadDLResolvePS", defines);
                GLSLProgram.createProgram(quadVS,  root + "GPUQuadDLResolvePS.frag", defines);
        mGPUQuadDLResolvePerSamplePS = //new PixelShader(d3dDevice, L"GPUQuadDL.hlsl", "GPUQuadDLResolvePerSamplePS", defines);
                GLSLProgram.createProgram(quadVS, root + "GPUQuadDLResolvePerSamplePS.frag", defines);

        // Create input layout
        {
            // We need the vertex shader bytecode for this... rather than try to wire that all through the
            // shader interface, just recompile the vertex shader.
            /*UINT shaderFlags = D3D10_SHADER_ENABLE_STRICTNESS | D3D10_SHADER_PACK_MATRIX_ROW_MAJOR;
            ID3D10Blob *bytecode = 0;
            HRESULT hr = D3DCompileFromFile(L"Rendering.hlsl", defines, D3D_COMPILE_STANDARD_FILE_INCLUDE, "GeometryVS", "vs_5_0", shaderFlags, 0, &bytecode, 0);
            if (FAILED(hr)) {
                assert(false);      // It worked earlier...
            }*/

            final int DXGI_FORMAT_R32G32B32_FLOAT = GLenum.GL_RGB32F;
            final int DXGI_FORMAT_R32G32_FLOAT = GLenum.GL_RG32F;
            final int D3D11_INPUT_PER_VERTEX_DATA = 0;

            final D3D11_INPUT_ELEMENT_DESC layout[] =
            {
                    new D3D11_INPUT_ELEMENT_DESC("position",  0, DXGI_FORMAT_R32G32B32_FLOAT, 0, 0,  D3D11_INPUT_PER_VERTEX_DATA, 0),
                    new D3D11_INPUT_ELEMENT_DESC("normal",    0, DXGI_FORMAT_R32G32B32_FLOAT, 0, 12, D3D11_INPUT_PER_VERTEX_DATA, 0),
                    new D3D11_INPUT_ELEMENT_DESC("texCoord",  0, DXGI_FORMAT_R32G32_FLOAT,    0, 24, D3D11_INPUT_PER_VERTEX_DATA, 0),
            };

            mMeshVertexLayout = ID3D11InputLayout.createInputLayoutFrom(layout);

            /*d3dDevice->CreateInputLayout(
                    layout, ARRAYSIZE(layout),
                    bytecode->GetBufferPointer(),
                    bytecode->GetBufferSize(),
                    &mMeshVertexLayout);

            bytecode->Release();*/
        }

        // Create standard rasterizer state
        {
            /*CD3D11_RASTERIZER_DESC desc(D3D11_DEFAULT);
            d3dDevice->CreateRasterizerState(&desc, &mRasterizerState);*/

            mRasterizerState= ()->
            {
                gl.glEnable(GLenum.GL_CULL_FACE);
                gl.glCullFace(GLenum.GL_BACK);
                gl.glFrontFace(GLenum.GL_CCW);
            };

            /*desc.CullMode = D3D11_CULL_NONE;
            d3dDevice->CreateRasterizerState(&desc, &mDoubleSidedRasterizerState);*/

            mDoubleSidedRasterizerState=()->
            {
                gl.glDisable(GLenum.GL_CULL_FACE);
            };
        }

        {
            /*CD3D11_DEPTH_STENCIL_DESC desc(D3D11_DEFAULT);
            // NOTE: Complementary Z => GREATER test
            desc.DepthFunc = D3D11_COMPARISON_GREATER_EQUAL;
            d3dDevice->CreateDepthStencilState(&desc, &mDepthState);*/

            mDepthState = ()->
            {
                gl.glEnable(GLenum.GL_DEPTH_TEST);
                gl.glDepthFunc(GLenum.GL_LEQUAL);
                gl.glDepthMask(true);
            };
        }

        // Stencil states for MSAA
        {
            /*CD3D11_DEPTH_STENCIL_DESC desc(
                FALSE, D3D11_DEPTH_WRITE_MASK_ZERO, D3D11_COMPARISON_GREATER_EQUAL,   // Depth
                TRUE, 0xFF, 0xFF,                                                     // Stencil
                D3D11_STENCIL_OP_REPLACE, D3D11_STENCIL_OP_REPLACE, D3D11_STENCIL_OP_REPLACE, D3D11_COMPARISON_ALWAYS, // Front face stencil
                D3D11_STENCIL_OP_REPLACE, D3D11_STENCIL_OP_REPLACE, D3D11_STENCIL_OP_REPLACE, D3D11_COMPARISON_ALWAYS  // Back face stencil
            );
            d3dDevice->CreateDepthStencilState(&desc, &mWriteStencilState);*/

            mWriteStencilState = ()->
            {
                gl.glDisable(GLenum.GL_DEPTH_TEST);
                gl.glDepthMask(false);
                gl.glDepthFunc(GLenum.GL_LEQUAL);

                gl.glEnable(GLenum.GL_STENCIL_TEST);
                gl.glStencilMask(0xFF);
                gl.glStencilOp(GLenum.GL_REPLACE, GLenum.GL_REPLACE, GLenum.GL_REPLACE);
                gl.glStencilFunc(GLenum.GL_ALWAYS, 0, 0xFF);
            };
        }
        {
            /*CD3D11_DEPTH_STENCIL_DESC desc(
                TRUE, D3D11_DEPTH_WRITE_MASK_ZERO, D3D11_COMPARISON_GREATER_EQUAL,    // Depth
                TRUE, 0xFF, 0xFF,                                                     // Stencil
                D3D11_STENCIL_OP_KEEP, D3D11_STENCIL_OP_KEEP, D3D11_STENCIL_OP_KEEP, D3D11_COMPARISON_EQUAL, // Front face stencil
                D3D11_STENCIL_OP_KEEP, D3D11_STENCIL_OP_KEEP, D3D11_STENCIL_OP_KEEP, D3D11_COMPARISON_EQUAL  // Back face stencil
            );
            d3dDevice->CreateDepthStencilState(&desc, &mEqualStencilState);*/

            mEqualStencilState = ()->
            {
                gl.glEnable(GLenum.GL_DEPTH_TEST);
                gl.glDepthMask(false);
                gl.glDepthFunc(GLenum.GL_GEQUAL);

                gl.glEnable(GLenum.GL_STENCIL_TEST);
                gl.glStencilMask(0xFF);
                gl.glStencilOp(GLenum.GL_KEEP, GLenum.GL_KEEP, GLenum.GL_KEEP);
                gl.glStencilFunc(GLenum.GL_EQUAL, 1, 0xFF);
            };
        }

        // Create geometry phase blend state
        {
            /*CD3D11_BLEND_DESC desc(D3D11_DEFAULT);
            d3dDevice->CreateBlendState(&desc, &mGeometryBlendState);*/

            mGeometryBlendState = ()->
            {
                gl.glDisable(GLenum.GL_BLEND);
            };
        }

        // Create lighting phase blend state
        {
            /*CD3D11_BLEND_DESC desc(D3D11_DEFAULT);
            // Additive blending
            desc.RenderTarget[0].BlendEnable = true;
            desc.RenderTarget[0].SrcBlend = D3D11_BLEND_ONE;
            desc.RenderTarget[0].DestBlend = D3D11_BLEND_ONE;
            desc.RenderTarget[0].BlendOp = D3D11_BLEND_OP_ADD;
            desc.RenderTarget[0].SrcBlendAlpha = D3D11_BLEND_ONE;
            desc.RenderTarget[0].DestBlendAlpha = D3D11_BLEND_ONE;
            desc.RenderTarget[0].BlendOpAlpha = D3D11_BLEND_OP_ADD;
            d3dDevice->CreateBlendState(&desc, &mLightingBlendState);*/

            mLightingBlendState = ()->
            {
                gl.glEnable(GLenum.GL_BLEND);
                gl.glBlendFunc(GLenum.GL_ONE, GLenum.GL_ONE);
            };
        }

        // Create constant buffers
        {
            /*CD3D11_BUFFER_DESC desc(
                sizeof(PerFrameConstants),
                D3D11_BIND_CONSTANT_BUFFER,
                D3D11_USAGE_DYNAMIC,
                D3D11_CPU_ACCESS_WRITE);

            d3dDevice->CreateBuffer(&desc, 0, &mPerFrameConstants);*/

            mPerFrameConstants = new BufferGL();
            mPerFrameConstants.initlize(GLenum.GL_UNIFORM_BUFFER, PerFrameConstants.SIZE, null, GLenum.GL_DYNAMIC_COPY);
        }

        // Create sampler state
        {
            /*CD3D11_SAMPLER_DESC desc(D3D11_DEFAULT);
            desc.Filter = D3D11_FILTER_ANISOTROPIC;
            desc.AddressU = D3D11_TEXTURE_ADDRESS_WRAP;
            desc.AddressV = D3D11_TEXTURE_ADDRESS_WRAP;
            desc.AddressW = D3D11_TEXTURE_ADDRESS_WRAP;
            desc.MaxAnisotropy = 16;
            d3dDevice->CreateSamplerState(&desc, &mDiffuseSampler);
            desc.MipLODBias = 1; todo
            d3dDevice->CreateSamplerState(&desc, &mDPSDiffuseSampler);*/

            SamplerDesc desc = new SamplerDesc();
            desc.anisotropic = 16;

            mDPSDiffuseSampler = mDiffuseSampler = SamplerUtils.createSampler(desc);
        }

        // Create skybox mesh
//        mSkyboxMesh.Create(d3dDevice, L"Media\\Skybox\\Skybox.sdkmesh");  todo

        // Initialize queries for timers.
        /*D3D11_QUERY_DESC desc0 = {D3D11_QUERY_TIMESTAMP, 0};
        D3D11_QUERY_DESC desc1 = {D3D11_QUERY_TIMESTAMP_DISJOINT, 0};
        for (UINT i = 0; i < TIMER_COUNT; ++i) {
            d3dDevice->CreateQuery(&desc0, &m_pTimers[i][T_BEGIN]);
            d3dDevice->CreateQuery(&desc0, &m_pTimers[i][T_END]);
            d3dDevice->CreateQuery(&desc1, &m_pTimers[i][T_VALID]);
        }*/

        for(int i = 0; i < m_pTimers.length; i++)
            m_pTimers[i] = new NvGPUTimer();

        // StephanieB5: removed unused parameter ID3D11Device* d3dDevice
        InitializeLightParameters();
        SetActiveLights(/*d3dDevice,*/ activeLights);
    }

    void OnD3D11ResizedSwapChain(int width, int height){
        mGBufferWidth = width;
        mGBufferHeight = height;

        // Create/recreate any textures related to screen size
        /*mGBuffer.resize(0);
        mGBufferRTV.resize(0);
        mGBufferSRV.resize(0);*/
        mLitBufferPS = null;
        mLitBufferCS = null;
        mDeferredLightingAccumBuffer = null;
        mDepthBuffer = null;
        if(mDepthBufferReadOnlyDSV != null)
            mDepthBufferReadOnlyDSV.dispose();

        Texture2DDesc desc = new Texture2DDesc(width, height, mMSAASamples > 1? GLenum.GL_DEPTH24_STENCIL8 : GLenum.GL_DEPTH_COMPONENT32F);
        desc.sampleCount = mMSAASamples;

        /*DXGI_SAMPLE_DESC sampleDesc;
        sampleDesc.Count = mMSAASamples;
        sampleDesc.Quality = 0;*/

        // standard depth/stencil buffer
        mDepthBuffer = /*shared_ptr<Depth2D>(new Depth2D(
                d3dDevice, mGBufferWidth, mGBufferHeight,
                D3D11_BIND_DEPTH_STENCIL | D3D11_BIND_SHADER_RESOURCE,
                sampleDesc,
                mMSAASamples > 1    // Include stencil if using MSAA
        ))*/ TextureUtils.createTexture2D(desc, null);

        // read-only depth stencil view
        {
            /*D3D11_DEPTH_STENCIL_VIEW_DESC desc;
            mDepthBuffer->GetDepthStencil()->GetDesc(&desc);
            desc.Flags = D3D11_DSV_READ_ONLY_DEPTH;

            d3dDevice->CreateDepthStencilView(mDepthBuffer->GetTexture(), &desc, &mDepthBufferReadOnlyDSV);*/

            mDepthBufferReadOnlyDSV = mDepthBuffer;
        }

        // NOTE: The next set of buffers are not all needed at the same time... a given technique really only needs one of them.
        // We allocate them all up front for quick swapping between techniques and to keep the code as simple as possible.

        // lit buffers
        /*mLitBufferPS = shared_ptr<Texture2D>(new Texture2D(
                d3dDevice, mGBufferWidth, mGBufferHeight, DXGI_FORMAT_R16G16B16A16_FLOAT,
                D3D11_BIND_RENDER_TARGET | D3D11_BIND_SHADER_RESOURCE,
                sampleDesc));*/
        desc.format = GLenum.GL_RGBA16F;
        mLitBufferPS = TextureUtils.createTexture2D(desc,null);

        mLitBufferCS = /*shared_ptr< StructuredBuffer<FramebufferFlatElement> >(new StructuredBuffer<FramebufferFlatElement>(
                d3dDevice, mGBufferWidth * mGBufferHeight * mMSAASamples,
                D3D11_BIND_UNORDERED_ACCESS | D3D11_BIND_SHADER_RESOURCE));*/
                new BufferGL();
        mLitBufferCS.initlize(GLenum.GL_SHADER_STORAGE_BUFFER, mGBufferWidth * mGBufferHeight * mMSAASamples * 8, null, GLenum.GL_STREAM_COPY);
        mLitBufferCS.createTextureBuffer(GLenum.GL_RG32UI);

        // deferred lighting accumulation buffer
        /*mDeferredLightingAccumBuffer = shared_ptr<Texture2D>(new Texture2D(
                d3dDevice, mGBufferWidth, mGBufferHeight, DXGI_FORMAT_R16G16B16A16_FLOAT,
                D3D11_BIND_RENDER_TARGET | D3D11_BIND_SHADER_RESOURCE,
                sampleDesc));*/
        mDeferredLightingAccumBuffer = TextureUtils.createTexture2D(desc, null);

        // G-Buffer
        mGBuffer = new Texture2D[6];  // last slot for the depth-stenil buffer
        // normal_specular
        /*mGBuffer.push_back(shared_ptr<Texture2D>(new Texture2D(
                d3dDevice, mGBufferWidth, mGBufferHeight, DXGI_FORMAT_R16G16B16A16_FLOAT,
                D3D11_BIND_RENDER_TARGET | D3D11_BIND_SHADER_RESOURCE,
                sampleDesc)));*/
        mGBuffer[0] = TextureUtils.createTexture2D(desc, null);

        // albedo
        /*mGBuffer.push_back(shared_ptr<Texture2D>(new Texture2D(
                d3dDevice, mGBufferWidth, mGBufferHeight, DXGI_FORMAT_R8G8B8A8_UNORM,
                D3D11_BIND_RENDER_TARGET | D3D11_BIND_SHADER_RESOURCE,
                sampleDesc)));*/
        desc.format = GLenum.GL_RGBA8;
        mGBuffer[1] = TextureUtils.createTexture2D(desc, null);

        // sampled w/ biased sampler.
        /*mGBuffer.push_back(shared_ptr<Texture2D>(new Texture2D(
                d3dDevice, mGBufferWidth, mGBufferHeight, DXGI_FORMAT_R8G8B8A8_UNORM,
                D3D11_BIND_RENDER_TARGET | D3D11_BIND_SHADER_RESOURCE,
                sampleDesc)));*/
        mGBuffer[2] = TextureUtils.createTexture2D(desc, null);

        // positionZgrad
        /*mGBuffer.push_back(shared_ptr<Texture2D>(new Texture2D(
                d3dDevice, mGBufferWidth, mGBufferHeight, DXGI_FORMAT_R16G16_FLOAT,
                D3D11_BIND_RENDER_TARGET | D3D11_BIND_SHADER_RESOURCE,
                sampleDesc)));*/
        desc.format = GLenum.GL_RG16F;
        mGBuffer[3] = TextureUtils.createTexture2D(desc, null);

        // view space Z
        /*mGBuffer.push_back(shared_ptr<Texture2D>(new Texture2D(
                d3dDevice, mGBufferWidth, mGBufferHeight, DXGI_FORMAT_R32_FLOAT,
                D3D11_BIND_RENDER_TARGET | D3D11_BIND_SHADER_RESOURCE,
                sampleDesc)));*/

        desc.format = GLenum.GL_R32F;
        mGBuffer[4] = TextureUtils.createTexture2D(desc, null);

        mGBufferRTV = mGBuffer;
        mGBufferSRV = mGBuffer;

        // Set up GBuffer resource list
        /*mGBufferRTV.resize(mGBuffer.size(), 0);
        mGBufferSRV.resize(mGBuffer.size(), 0);
        for (std::size_t i = 0; i < mGBuffer.size(); ++i) {
            mGBufferRTV[i] = mGBuffer[i]->GetRenderTarget();
            mGBufferSRV[i] = mGBuffer[i]->GetShaderResource();
        }*/
    }

    void Move(float elapsedTime){
        mTotalTime += elapsedTime;

        // Update positions of active lights
        for ( int i = 0; i < mActiveLights; ++i) {
            PointLightInitTransform initTransform = mLightInitialTransform[i];
            float angle = initTransform.angle + mTotalTime * initTransform.animationSpeed;
            mPointLightPositionWorld[i].set(
                    initTransform.radius * (float)Math.cos(angle),
                    initTransform.height,
                    initTransform.radius * (float)Math.sin(angle));
        }
    }

    private PerFrameConstants constants = new PerFrameConstants();

    void Render(//ID3D11DeviceContext* d3dDeviceContext,
                Texture2D backBuffer,
                SDKmesh mesh_opaque,
                SDKmesh mesh_alpha,
                TextureGL skybox,
                Matrix4f worldMatrix,
                CFirstPersonCamera viewerCamera,
//                const D3D11_VIEWPORT* viewport,
                UIConstants ui){
        Matrix4f cameraProj = viewerCamera.GetProjMatrix();
        Matrix4f cameraView = viewerCamera.GetViewMatrix();

//        XMMATRIX cameraViewInv = XMMatrixInverse(nullptr, cameraView);

        // Compute composite matrices
//        XMMATRIX cameraViewProj = cameraView * cameraProj;
//        XMMATRIX cameraWorldViewProj = worldMatrix * cameraViewProj;

        // Fill in frame constants
        {
            /*D3D11_MAPPED_SUBRESOURCE mappedResource;
            d3dDeviceContext->Map(mPerFrameConstants, 0, D3D11_MAP_WRITE_DISCARD, 0, &mappedResource);
            PerFrameConstants* constants = static_cast<PerFrameConstants *>(mappedResource.pData);*/

            /*XMStoreFloat4x4(&constants->mCameraWorldViewProj, cameraWorldViewProj);
            XMStoreFloat4x4(&constants->mCameraWorldView, worldMatrix * cameraView);
            XMStoreFloat4x4(&constants->mCameraViewProj, cameraViewProj);
            XMStoreFloat4x4(&constants->mCameraProj, cameraProj);*/

            Matrix4f.mul(cameraProj, cameraView, constants.mCameraViewProj);
            Matrix4f.mul(constants.mCameraViewProj, worldMatrix, constants.mCameraWorldViewProj);
            Matrix4f.mul(cameraView, worldMatrix, constants.mCameraWorldView);
            constants.mCameraProj.load(cameraProj);

            // NOTE: Complementary Z => swap near/far back
            constants.mCameraNearFar.set(viewerCamera.GetFarClip(), viewerCamera.GetNearClip(), 0.0f, 0.0f);

            constants.mFramebufferDimensionsX = mGBufferWidth;
            constants.mFramebufferDimensionsY = mGBufferHeight;
            constants.mFramebufferDimensionsZ = 0;     // Unused
            constants.mFramebufferDimensionsW = 0;     // Unused

            constants.mUI = ui;

//            d3dDeviceContext->Unmap(mPerFrameConstants, 0);

            ByteBuffer buffer = CacheBuffer.getCachedByteBuffer(PerFrameConstants.SIZE);
            constants.store(buffer).flip();

            mPerFrameConstants.update(0, buffer);
        }

        // Geometry phase
        /*if (mesh_opaque.IsLoaded()) {  todo
            mesh_opaque.ComputeInFrustumFlags(cameraWorldViewProj);
        }
        if (mesh_alpha.IsLoaded()) {
            mesh_alpha.ComputeInFrustumFlags(cameraWorldViewProj);
        }*/

        // Setup lights
        BufferGL lightBufferSRV = SetupLights(/*d3dDeviceContext,*/ cameraView);
        gl.glViewport(0,0, backBuffer.getWidth(), backBuffer.getHeight());

        // Forward rendering takes a different path here
        // StephanieB5 removed unnecessary parameters from RenderForward and RenderGBuffer
        // The parameters were CFirstPersonCamera* viewerCamera and UIConstants* ui
        if (ui.lightCullTechnique == LightCullTechnique.CULL_FORWARD_NONE) {
            RenderForward(/*d3dDeviceContext,*/ mesh_opaque, mesh_alpha, lightBufferSRV, /*viewport,*/ false);
        } else if (ui.lightCullTechnique == LightCullTechnique.CULL_FORWARD_PREZ_NONE) {
            RenderForward(/*d3dDeviceContext,*/ mesh_opaque, mesh_alpha, lightBufferSRV, /*viewport,*/ true);
        } else {
            RenderGBuffer(/*d3dDeviceContext,*/ mesh_opaque, mesh_alpha/*, viewport*/);
            ComputeLighting(/*d3dDeviceContext,*/ lightBufferSRV, /*viewport,*/ ui);
        }

        // Render skybox and tonemap
        RenderSkyboxAndToneMap(/*d3dDeviceContext,*/ backBuffer, skybox,
                mDepthBuffer/*->GetShaderResource()*/, /*viewport,*/ ui);

//#ifdef _PERF
        m_frameStats.m_totalGBuffGen = 1000 *  m_pTimers[TIMER_GBUFFER_GEN].getScaledCycles(); // GetGPUCounterSeconds(d3dDeviceContext, &m_pTimers[TIMER_GBUFFER_GEN][0]);
        m_frameStats.m_totalShadingTime= 1000 * m_pTimers[TIMER_SHADING].getScaledCycles(); // GetGPUCounterSeconds(d3dDeviceContext, &[0]);
        m_frameStats.m_totalSkyBox = 1000 * m_pTimers[TIMER_SKYBOX].getScaledCycles(); // GetGPUCounterSeconds(d3dDeviceContext, &[0]);
//#endif // _PERF
    }

    void SetActiveLights(/*ID3D11Device* d3dDevice, unsigned*/ int activeLights){
        mActiveLights = activeLights;

//        delete mLightBuffer;
//        mLightBuffer = new StructuredBuffer<PointLight>(d3dDevice, activeLights, D3D11_BIND_SHADER_RESOURCE, true);
        mLightBuffer = new BufferGL();
        mLightBuffer.initlize(GLenum.GL_SHADER_STORAGE_BUFFER, activeLights *PointLight.SIZE, null, GLenum.GL_STREAM_COPY);

        // Make sure all the active lights are set up
        Move(0.0f);
    }

    int GetActiveLights()  { return mActiveLights; }
    FrameStats GetFrameStats()  { return m_frameStats; }


    // StephanieB5: removed unused parameter ID3D11Device* d3dDevice
    private void InitializeLightParameters(){
        mPointLightParameters = new PointLight[MAX_LIGHTS];
        mLightInitialTransform = new PointLightInitTransform[MAX_LIGHTS];
        mPointLightPositionWorld = new Vector3f[MAX_LIGHTS];

        // Use a constant seed for consistency
//        std::tr1::mt19937 rng(1337);
        Random rng = new Random(1337);

//        std::tr1::uniform_real<float> radiusNormDist(0.0f, 1.0f);
        final float maxRadius = 100.0f;
//        std::tr1::uniform_real<float> angleDist(0.0f, 2.0f * XM_PI);
//        std::tr1::uniform_real<float> heightDist(0.0f, 20.0f);
//        std::tr1::uniform_real<float> animationSpeedDist(2.0f, 20.0f);
//        std::tr1::uniform_int<int> animationDirection(0, 1);
//        std::tr1::uniform_real<float> hueDist(0.0f, 1.0f);
//        std::tr1::uniform_real<float> intensityDist(0.1f, 0.5f);
//        std::tr1::uniform_real<float> attenuationDist(2.0f, 15.0f);
        final float attenuationStartFactor = 0.8f;

        for (int i = 0; i < MAX_LIGHTS; ++i) {
            PointLight params = mPointLightParameters[i] = new PointLight();
            PointLightInitTransform init = mLightInitialTransform[i] = new PointLightInitTransform();
            mPointLightPositionWorld[i] = new Vector3f();

            init.radius = (float) (Math.sqrt(rng.nextDouble()) * maxRadius);
            init.angle = /*angleDist(rng)*/rng.nextFloat() * Numeric.PI * 2;
            init.height = /*heightDist(rng)*/rng.nextFloat() * 20;
            // Normalize by arc length
            init.animationSpeed = (/*animationDirection(rng)*/rng.nextInt(1) * 2 - 1) * /*animationSpeedDist(rng)*/(18*rng.nextFloat()+2) / init.radius;

            // HSL->RGB, vary light hue
            /*XMVECTOR color = intensityDist(rng) * HueToRGB(hueDist(rng));
            XMStoreFloat3(&params.color, color);*/
            HueToRGB(rng.nextFloat(), params.color);
            params.color.scale(rng.nextFloat() * 0.4f + 0.1f);
            params.attenuationEnd = /*attenuationDist(rng)*/ rng.nextFloat() * 13 + 2;
            params.attenuationBegin = attenuationStartFactor * params.attenuationEnd;
        }
    }

    private static void HueToRGB(float hue, Vector3f color)
    {
        hue *= 6;
        float intPart = (int)Math.floor(hue);
        float fracPart =  hue - intPart; // modff(hue * 6.0f, &intPart);
        int region = (int) intPart;

        switch (region) {
            case 0: color.set(1.0f, fracPart, 0.0f); return;
            case 1: color.set(1.0f - fracPart, 1.0f, 0.0f);return;
            case 2: color.set(0.0f, 1.0f, fracPart);return;
            case 3: color.set(0.0f, 1.0f - fracPart, 1.0f);return;
            case 4: color.set(fracPart, 0.0f, 1.0f);return;
            case 5: color.set(1.0f, 0.0f, 1.0f - fracPart);return;
        }

        color.set(0.0f, 0.0f, 0.0f);
    }

    // Notes:
    // - Most of these functions should all be called after initializing per frame/pass constants, etc.
    //   as the shaders that they invoke bind those constant buffers.

    // Set up shader light buffer
    private BufferGL  SetupLights(//ID3D11DeviceContext* d3dDeviceContext,
                                           Matrix4f cameraView){
        // Transform light world positions into view space and store in our parameters array
        /*XMVector3TransformCoordStream(&mPointLightParameters[0].positionView, sizeof(PointLight),
        &mPointLightPositionWorld[0], sizeof(XMFLOAT3), mActiveLights, cameraView);*/

        for(int i = 0; i < mActiveLights; i++){
            Matrix4f.transformVector(cameraView, mPointLightPositionWorld[i],mPointLightParameters[i].positionView );
        }

        // Copy light list into shader buffer
        {
//            PointLight light = mLightBuffer->MapDiscard(d3dDeviceContext);
            ByteBuffer buffer = CacheBuffer.getCachedByteBuffer(PointLight.SIZE * mActiveLights);
            for (int i = 0; i < mActiveLights; ++i) {
//                light[i] = mPointLightParameters[i];
                mPointLightParameters[i].store(buffer);
            }
//            mLightBuffer->Unmap(d3dDeviceContext);
            buffer.flip();
            mLightBuffer.update(0, buffer);
        }

        return mLightBuffer/*->GetShaderResource()*/;
    }

    // StephanieB5 removed unused parameters CFirstPersonCamera* viewerCamera & UIConstants* ui
    // Forward rendering of geometry into
    TextureGL  RenderForward(//ID3D11DeviceContext* d3dDeviceContext,
                             SDKmesh mesh_opaque,
                             SDKmesh mesh_alpha,
                             BufferGL lightBufferSRV,
//                                             const D3D11_VIEWPORT* viewport,
                             boolean doPreZ){
        // Clear lit and depth buffer
//        d3dDeviceContext->ClearRenderTargetView(mLitBufferPS->GetRenderTarget(), zeros);
        gl.glClearTexImage(mLitBufferPS.getTexture(), 0, TextureUtils.measureFormat(mLitBufferPS.getFormat()), TextureUtils.measureDataType(mLitBufferPS.getFormat()), null);
        // NOTE: Complementary Z buffer: clear to 0 (far)!
//        d3dDeviceContext->ClearDepthStencilView(mDepthBuffer->GetDepthStencil(), D3D11_CLEAR_DEPTH, 0.0f, 0);
        gl.glClearTexImage(mDepthBuffer.getTexture(), 0, GLenum.GL_DEPTH_COMPONENT, GLenum.GL_FLOAT, CacheBuffer.wrap(1.f));

//        d3dDeviceContext->IASetInputLayout(mMeshVertexLayout);

        /*d3dDeviceContext->VSSetConstantBuffers(0, 1, &mPerFrameConstants);
        d3dDeviceContext->VSSetShader(mGeometryVS->GetShader(), 0, 0);*/
        gl.glBindBufferBase(GLenum.GL_UNIFORM_BUFFER, 0, mPerFrameConstants.getBuffer());


//        d3dDeviceContext->GSSetShader(0, 0, 0);

//        d3dDeviceContext->RSSetViewports(1, viewport);

//        d3dDeviceContext->PSSetConstantBuffers(0, 1, &mPerFrameConstants);
//        d3dDeviceContext->PSSetShaderResources(5, 1, &lightBufferSRV);  todo
//        d3dDeviceContext->PSSetSamplers(0, 1, &mDiffuseSampler);
        // Diffuse texture set per-material by DXUT mesh routines

//        d3dDeviceContext->OMSetDepthStencilState(mDepthState, 0);
        mDepthState.run();
        m_FBO.bind();
        // Pre-Z pass if requested
        if (doPreZ) {
//            d3dDeviceContext->OMSetRenderTargets(0, 0, mDepthBuffer->GetDepthStencil());
            m_FBO.setRenderTexture(mDepthBuffer, null);

            // Render opaque geometry
//            if (mesh_opaque.IsLoaded())
            {
                /*d3dDeviceContext->RSSetState(mRasterizerState);
                d3dDeviceContext->PSSetShader(0, 0, 0);
                mesh_opaque.Render(d3dDeviceContext, 0);*/
                mGeometryPassVS.enable();
                mRasterizerState.run();
                mesh_opaque.render(-1, -1,-1);
            }

            // Render alpha tested geometry
//            if (mesh_alpha.IsLoaded())
            {
                /*d3dDeviceContext->RSSetState(mDoubleSidedRasterizerState);
                // NOTE: Use simplified alpha test shader that only clips
                d3dDeviceContext->PSSetShader(mForwardAlphaTestOnlyPS->GetShader(), 0, 0);
                mesh_alpha.Render(d3dDeviceContext, 0);*/

                mForwardAlphaTestOnlyPS.enable();
                mDoubleSidedRasterizerState.run();
                mesh_alpha.render(-1, -1,-1);
            }
        }

        // Set up render targets
        /*ID3D11RenderTargetView *renderTargets[1] = {mLitBufferPS->GetRenderTarget()};
        d3dDeviceContext->OMSetRenderTargets(1, renderTargets, mDepthBuffer->GetDepthStencil());*/
        m_FBO.setRenderTextures(CommonUtil.toArray(mLitBufferPS, mDepthBuffer), null);

//        d3dDeviceContext->OMSetBlendState(mGeometryBlendState, 0, 0xFFFFFFFF);
        mGeometryBlendState.run();

        // Render opaque geometry
//        if (mesh_opaque.IsLoaded())
        {
            /*d3dDeviceContext->RSSetState(mRasterizerState);
            d3dDeviceContext->PSSetShader(mForwardPS->GetShader(), 0, 0);
            mesh_opaque.Render(d3dDeviceContext, 0);*/

            mRasterizerState.run();
            mForwardPS.enable();
            mesh_opaque.render(0, 1, 2);
        }

        // Render alpha tested geometry
//        if (mesh_alpha.IsLoaded())
        {
            /*d3dDeviceContext->RSSetState(mDoubleSidedRasterizerState);
            d3dDeviceContext->PSSetShader(mForwardAlphaTestPS->GetShader(), 0, 0);
            mesh_alpha.Render(d3dDeviceContext, 0);*/

            mDoubleSidedRasterizerState.run();
            mForwardAlphaTestPS.enable();
            mesh_alpha.render(0, 1, 2);
        }

        // Cleanup (aka make the runtime happy)
//        d3dDeviceContext->OMSetRenderTargets(0, 0, 0);

        return mLitBufferPS/*->GetShaderResource()*/;
    }

    // StephanieB5: removed unused parameters CFirstPersonCamera* viewerCamera & UIConstants* ui
    // Draws geometry into G-buffer
    void RenderGBuffer(//ID3D11DeviceContext* d3dDeviceContext,
                       SDKmesh mesh_opaque,
                       SDKmesh mesh_alpha/*,
                       const D3D11_VIEWPORT* viewport*/){
//#ifdef _PERF
//        d3dDeviceContext->Begin(m_pTimers[TIMER_GBUFFER_GEN][T_VALID]);
//        d3dDeviceContext->End(m_pTimers[TIMER_GBUFFER_GEN][T_BEGIN]);
        if(mDebug){
            m_pTimers[TIMER_GBUFFER_GEN].reset();
            m_pTimers[TIMER_GBUFFER_GEN].start();
        }

//#endif // _PERF

        // Clear GBuffer
        // NOTE: We actually only need to clear the depth buffer here since we replace unwritten (i.e. far plane) samples
        // with the skybox. We use the depth buffer to reconstruct position and only in-frustum positions are shaded.
        // NOTE: Complementary Z buffer: clear to 0 (far)!
//        d3dDeviceContext->ClearDepthStencilView(mDepthBuffer->GetDepthStencil(), D3D11_CLEAR_DEPTH | D3D11_CLEAR_STENCIL, 0.0f, 0);
        gl.glClearTexImage(mDepthBuffer.getTexture(), 0,GLenum.GL_DEPTH_COMPONENT, GLenum.GL_FLOAT, CacheBuffer.wrap(1.f));

//        d3dDeviceContext->IASetInputLayout(mMeshVertexLayout);

//        d3dDeviceContext->VSSetConstantBuffers(0, 1, &mPerFrameConstants);
//        d3dDeviceContext->VSSetShader(mGeometryVS->GetShader(), 0, 0);
        gl.glBindBufferBase(GLenum.GL_UNIFORM_BUFFER, 0, mPerFrameConstants.getBuffer());

//        d3dDeviceContext->GSSetShader(0, 0, 0);

//        d3dDeviceContext->RSSetViewports(1, viewport);

        /*d3dDeviceContext->PSSetConstantBuffers(0, 1, &mPerFrameConstants);
        d3dDeviceContext->PSSetSamplers(0, 1, &mDiffuseSampler);  todo sampler
        d3dDeviceContext->PSSetSamplers(1, 1, &mDPSDiffuseSampler);*/

        // Diffuse texture set per-material by DXUT mesh routines

        // Set up render GBuffer render targets
//        d3dDeviceContext->OMSetDepthStencilState(mDepthState, 0);
        mDepthState.run();
        m_FBO.bind();
//        d3dDeviceContext->OMSetRenderTargets(static_cast<UINT>(mGBufferRTV.size()), &mGBufferRTV.front(), mDepthBuffer->GetDepthStencil());
        mGBufferRTV[5] = mDepthBuffer;
        m_FBO.setRenderTextures(mGBufferRTV, null);

//        d3dDeviceContext->OMSetBlendState(mGeometryBlendState, 0, 0xFFFFFFFF);
        mGeometryBlendState.run();

        // Render opaque geometry
//        if (mesh_opaque.IsLoaded())
        {
            /*d3dDeviceContext->RSSetState(mRasterizerState);
            d3dDeviceContext->PSSetShader(mGBufferPS->GetShader(), 0, 0);
            mesh_opaque.Render(d3dDeviceContext, 0);*/
            mRasterizerState.run();
            mGBufferPS.enable();
            mesh_opaque.render(0,1,-1);
        }

        // Render alpha tested geometry
//        if (mesh_alpha.IsLoaded())
        {
            /*d3dDeviceContext->RSSetState(mDoubleSidedRasterizerState);
            d3dDeviceContext->PSSetShader(mGBufferAlphaTestPS->GetShader(), 0, 0);
            mesh_alpha.Render(d3dDeviceContext, 0);*/
            mDoubleSidedRasterizerState.run();
            mGBufferAlphaTestPS.enable();
            mesh_alpha.render(0,1,-1);
        }

        // Cleanup (aka make the runtime happy)
//        d3dDeviceContext->OMSetRenderTargets(0, 0, 0);
//#ifdef _PERF
//        d3dDeviceContext->End(m_pTimers[TIMER_GBUFFER_GEN][T_END]);
//        d3dDeviceContext->End(m_pTimers[TIMER_GBUFFER_GEN][T_VALID]);
        if(mDebug){
            m_pTimers[TIMER_GBUFFER_GEN].stop();
        }

//#endif // _PERF
    }

    // Handles skybox, tone mapping, etc
    void RenderSkyboxAndToneMap(//ID3D11DeviceContext* d3dDeviceContext,
                                Texture2D backBuffer,
                                TextureGL skybox,
                                Texture2D depthSRV,
            /*const D3D11_VIEWPORT* viewport,*/
                                UIConstants ui){
//        #ifdef _PERF
//        d3dDeviceContext->Begin(m_pTimers[TIMER_SKYBOX][T_VALID]);
//        d3dDeviceContext->End(m_pTimers[TIMER_SKYBOX][T_BEGIN]);

        if(mDebug){
            m_pTimers[TIMER_SKYBOX].reset();
            m_pTimers[TIMER_SKYBOX].start();
        }
//#endif // _PERF

        /*D3D11_VIEWPORT skyboxViewport(*viewport);
        skyboxViewport.MinDepth = 1.0f;
        skyboxViewport.MaxDepth = 1.0f;

        d3dDeviceContext->IASetInputLayout(mMeshVertexLayout);*/

//        d3dDeviceContext->VSSetConstantBuffers(0, 1, &mPerFrameConstants);
//        d3dDeviceContext->VSSetShader(mSkyboxVS->GetShader(), 0, 0);
        gl.glBindBufferBase(GLenum.GL_UNIFORM_BUFFER, 0, mPerFrameConstants.getBuffer());

//        d3dDeviceContext->RSSetState(mDoubleSidedRasterizerState);
//        d3dDeviceContext->RSSetViewports(1, &skyboxViewport);
        mDoubleSidedRasterizerState.run();

        /*d3dDeviceContext->PSSetConstantBuffers(0, 1, &mPerFrameConstants);
        d3dDeviceContext->PSSetSamplers(0, 1, &mDiffuseSampler);  todo sampler
        d3dDeviceContext->PSSetShader(mSkyboxPS->GetShader(), 0, 0);*/

        mSkyboxPS.enable();

//        d3dDeviceContext->PSSetShaderResources(5, 1, &skybox);
//        d3dDeviceContext->PSSetShaderResources(6, 1, &depthSRV);
        gl.glBindTextureUnit(0, skybox.getTexture());
        gl.glBindTextureUnit(1, depthSRV.getTexture());

        // Bind the appropriate lit buffer depending on the technique
//        ID3D11ShaderResourceView* litViews[2] = {0, 0};
        switch (ui.lightCullTechnique) {
            // Compute-shader based techniques use the flattened MSAA buffer
            case LightCullTechnique. CULL_COMPUTE_SHADER_TILE:
//                litViews[1] = mLitBufferCS->GetShaderResource();
                gl.glBindTextureUnit(7, mLitBufferCS.getTexture());  // todo the bindging slot
                break;
            default:
//                litViews[0] = mLitBufferPS->GetShaderResource();
                gl.glBindTextureUnit(6, mLitBufferPS.getTexture());  // todo the bindging slot
                break;
        }
//        d3dDeviceContext->PSSetShaderResources(7, 2, litViews);

        /*d3dDeviceContext->OMSetRenderTargets(1, &backBuffer, 0);
        d3dDeviceContext->OMSetBlendState(mGeometryBlendState, 0, 0xFFFFFFFF);*/

        m_FBO.setRenderTexture(backBuffer, null);
        mGeometryBlendState.run();

//        mSkyboxMesh.Render(d3dDeviceContext);  todo render sky box mesh

        // Cleanup (aka make the runtime happy)
        /*d3dDeviceContext->OMSetRenderTargets(0, 0, 0);
        ID3D11ShaderResourceView* nullViews[10] = {0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
        d3dDeviceContext->PSSetShaderResources(0, 10, nullViews);
#ifdef _PERF
        d3dDeviceContext->End(m_pTimers[TIMER_SKYBOX][T_END]);
        d3dDeviceContext->End(m_pTimers[TIMER_SKYBOX][T_VALID]);
#endif // _PERF*/

        if(mDebug){
            m_pTimers[TIMER_SKYBOX].stop();
        }
    }

    void ComputeLighting(//ID3D11DeviceContext* d3dDeviceContext,
                         BufferGL lightBufferSRV,
//                         const D3D11_VIEWPORT* viewport,
                         UIConstants ui){
        // TODO: Clean up the branchiness here a bit... refactor into small functions

        gl.glBindBufferBase(GLenum.GL_UNIFORM_BUFFER, 0, mPerFrameConstants.getBuffer());

        switch (ui.lightCullTechnique) {

            case LightCullTechnique.CULL_COMPUTE_SHADER_TILE:
            {
//#ifdef _PERF
                /*d3dDeviceContext->Begin(m_pTimers[TIMER_SHADING][T_VALID]);
                d3dDeviceContext->End(m_pTimers[TIMER_SHADING][T_BEGIN]);*/

                if(mDebug){
                    m_pTimers[TIMER_SHADING].reset();
                    m_pTimers[TIMER_SHADING].start();
                }
//#endif // _PERF

                // No need to clear, we write all pixels

                // Compute shader setup (always does all the lights at once)
//                d3dDeviceContext->CSSetConstantBuffers(0, 1, &mPerFrameConstants);
                gl.glBindBufferBase(GLenum.GL_UNIFORM_BUFFER, 0, mPerFrameConstants.getBuffer());
                for(int i = 0; i < mGBufferSRV.length - 1; i++){
                    gl.glBindTextureUnit(i, mGBufferSRV[i].getTexture());
                }

//                d3dDeviceContext->CSSetShaderResources(0, static_cast<UINT>(mGBufferSRV.size()), &mGBufferSRV.front());
//                d3dDeviceContext->CSSetShaderResources(5, 1, &lightBufferSRV);
                gl.glBindBufferBase(GLenum.GL_SHADER_STORAGE_BUFFER, 1, lightBufferSRV.getBuffer());

//                ID3D11UnorderedAccessView *litBufferUAV = mLitBufferCS.GetUnorderedAccess();
//                d3dDeviceContext->CSSetUnorderedAccessViews(0, 1, &litBufferUAV, 0);

                gl.glBindBufferBase(GLenum.GL_SHADER_STORAGE_BUFFER, 2, mLitBufferCS.getBuffer());

//                d3dDeviceContext->CSSetShader(mComputeShaderTileCS->GetShader(), 0, 0);
                mComputeShaderTileCS.enable();

                // Dispatch
                int dispatchWidth = (mGBufferWidth + COMPUTE_SHADER_TILE_GROUP_DIM - 1) / COMPUTE_SHADER_TILE_GROUP_DIM;
                int dispatchHeight = (mGBufferHeight + COMPUTE_SHADER_TILE_GROUP_DIM - 1) / COMPUTE_SHADER_TILE_GROUP_DIM;
                gl.glDispatchCompute(dispatchWidth, dispatchHeight, 1);
                gl.glMemoryBarrier(GLenum.GL_SHADER_STORAGE_BARRIER_BIT);
//#ifdef _PERF
//                d3dDeviceContext->End(m_pTimers[TIMER_SHADING][T_END]);
//                d3dDeviceContext->End(m_pTimers[TIMER_SHADING][T_VALID]);
                if(mDebug){
                    m_pTimers[TIMER_SHADING].stop();
                }
//#endif // _PERF

            }
            break;

            case LightCullTechnique.CULL_QUAD:
            case LightCullTechnique.CULL_QUAD_DEFERRED_LIGHTING: {
                boolean deferredLighting = (ui.lightCullTechnique == LightCullTechnique.CULL_QUAD_DEFERRED_LIGHTING);
                Texture2D accumulateBuffer = deferredLighting ? mDeferredLightingAccumBuffer : mLitBufferPS;

                // Clear
//                const float zeros[4] = {0.0f, 0.0f, 0.0f, 0.0f};
//                d3dDeviceContext->ClearRenderTargetView(accumulateBuffer->GetRenderTarget(), zeros);
                gl.glClearTexImage(accumulateBuffer.getTexture(), 0, TextureUtils.measureFormat(accumulateBuffer.getFormat()), TextureUtils.measureDataType(accumulateBuffer.getFormat()), null);

                if (mMSAASamples > 1) {
                    // Full screen triangle setup
                    /*d3dDeviceContext->IASetInputLayout(0);
                    d3dDeviceContext->IASetPrimitiveTopology(D3D11_PRIMITIVE_TOPOLOGY_TRIANGLELIST);
                    d3dDeviceContext->IASetVertexBuffers(0, 0, 0, 0, 0);

                    d3dDeviceContext->VSSetShader(mFullScreenTriangleVS->GetShader(), 0, 0);
                    d3dDeviceContext->GSSetShader(0, 0, 0);

                    d3dDeviceContext->RSSetState(mRasterizerState);
                    d3dDeviceContext->RSSetViewports(1, viewport);*/

                    mRasterizerState.run();

//                    d3dDeviceContext->PSSetConstantBuffers(0, 1, &mPerFrameConstants);
//                    d3dDeviceContext->PSSetShaderResources(0, static_cast<UINT>(mGBufferSRV.size()), &mGBufferSRV.front());
                    gl.glBindBufferBase(GLenum.GL_UNIFORM_BUFFER, 0, mPerFrameConstants.getBuffer());
                    for(int i = 0; i < mGBufferSRV.length - 1; i++){
                        gl.glBindTextureUnit(i, mGBufferSRV[i].getTexture());
                    }
//                    d3dDeviceContext->PSSetShaderResources(5, 1, &lightBufferSRV);
                    gl.glBindBufferBase(GLenum.GL_SHADER_STORAGE_BUFFER, 1, lightBufferSRV.getBuffer());

                    // Set stencil mask for samples that require per-sample shading
                    /*d3dDeviceContext->PSSetShader(mRequiresPerSampleShadingPS->GetShader(), 0, 0);
                    d3dDeviceContext->OMSetDepthStencilState(mWriteStencilState, 1);
                    d3dDeviceContext->OMSetRenderTargets(0, 0, mDepthBufferReadOnlyDSV);
                    d3dDeviceContext->Draw(3, 0);*/

                    mRequiresPerSampleShadingPS.enable();
                    mWriteStencilState.run();
                    m_FBO.bind();
                    m_FBO.setRenderTexture(mDepthBufferReadOnlyDSV, null);
                    gl.glDrawArrays(GLenum.GL_TRIANGLES, 0, 3);
                }

                // Point primitives expanded into quads in the geometry shader
                /*d3dDeviceContext->IASetInputLayout(0);
                d3dDeviceContext->IASetPrimitiveTopology(D3D11_PRIMITIVE_TOPOLOGY_POINTLIST);
                d3dDeviceContext->IASetVertexBuffers(0, 0, 0, 0, 0);
                d3dDeviceContext->VSSetConstantBuffers(0, 1, &mPerFrameConstants);
                d3dDeviceContext->VSSetShaderResources(5, 1, &lightBufferSRV);*/

                gl.glBindBufferBase(GLenum.GL_SHADER_STORAGE_BUFFER, 1, lightBufferSRV.getBuffer());
//                d3dDeviceContext->VSSetShader(mGPUQuadVS->GetShader(), 0, 0);

//                d3dDeviceContext->GSSetShader(mGPUQuadGS->GetShader(), 0, 0);

//                d3dDeviceContext->RSSetState(mRasterizerState);
                mRasterizerState.run();
//                d3dDeviceContext->RSSetViewports(1, viewport);

                /*d3dDeviceContext->PSSetConstantBuffers(0, 1, &mPerFrameConstants);
                d3dDeviceContext->PSSetShaderResources(0, static_cast<UINT>(mGBufferSRV.size()), &mGBufferSRV.front());
                d3dDeviceContext->PSSetShaderResources(5, 1, &lightBufferSRV);*/

                for(int i = 0; i < mGBufferSRV.length - 1; i++){
                    gl.glBindTextureUnit(i, mGBufferSRV[i].getTexture());
                }

                // Additively blend into lit buffer
//                ID3D11RenderTargetView * renderTargets[1] = {accumulateBuffer->GetRenderTarget()};
                // Use depth buffer for culling but no writes (use the read-only DSV)
//                d3dDeviceContext->OMSetRenderTargets(1, renderTargets, mDepthBufferReadOnlyDSV);

                m_FBO.bind();
                m_FBO.setRenderTextures(CommonUtil.toArray(accumulateBuffer, mDepthBufferReadOnlyDSV), null);
//                d3dDeviceContext->OMSetBlendState(mLightingBlendState, 0, 0xFFFFFFFF);
                mLightingBlendState.run();

                // Dispatch one point per light

                // Do pixel frequency shading
//                d3dDeviceContext->PSSetShader(deferredLighting ? mGPUQuadDLPS->GetShader() : mGPUQuadPS->GetShader(), 0, 0);
                if(deferredLighting)
                    mGPUQuadDLPS.enable();
                else
                    mGPUQuadPS.enable();

//                d3dDeviceContext->OMSetDepthStencilState(mEqualStencilState, 0);
                mEqualStencilState.run();
//                d3dDeviceContext->Draw(mActiveLights, 0);
                gl.glDrawArrays(GLenum.GL_POINTS, 0, mActiveLights);

                if (mMSAASamples > 1) {
                    // Do sample frequency shading
//                    d3dDeviceContext->PSSetShader(deferredLighting ? mGPUQuadDLPerSamplePS->GetShader() : mGPUQuadPerSamplePS->GetShader(), 0, 0);
                    if(deferredLighting)
                        mGPUQuadDLPerSamplePS.enable();
                    else
                        mGPUQuadPerSamplePS.enable();

//                    d3dDeviceContext->OMSetDepthStencilState(mEqualStencilState, 1);
                    mEqualStencilState.run();
//                    d3dDeviceContext->Draw(mActiveLights, 0);
                    gl.glDrawArrays(GLenum.GL_POINTS, 0, mActiveLights);
                }

                if (deferredLighting) {
                    // Final screen-space pass to combine diffuse and specular
                    /*d3dDeviceContext->IASetInputLayout(0);
                    d3dDeviceContext->IASetPrimitiveTopology(D3D11_PRIMITIVE_TOPOLOGY_TRIANGLELIST);
                    d3dDeviceContext->IASetVertexBuffers(0, 0, 0, 0, 0);

                    d3dDeviceContext->VSSetShader(mFullScreenTriangleVS->GetShader(), 0, 0);
                    d3dDeviceContext->GSSetShader(0, 0, 0);*/

                    /*ID3D11RenderTargetView * resolveRenderTargets[1] = {mLitBufferPS->GetRenderTarget()};
                    d3dDeviceContext->OMSetRenderTargets(1, resolveRenderTargets, mDepthBufferReadOnlyDSV);
                    d3dDeviceContext->OMSetBlendState(mGeometryBlendState, 0, 0xFFFFFFFF);*/

                    m_FBO.bind();
                    m_FBO.setRenderTextures(CommonUtil.toArray(mLitBufferPS, mDepthBufferReadOnlyDSV), null);
                    mGeometryBlendState.run();

                    /*ID3D11ShaderResourceView * accumulateBufferSRV = accumulateBuffer->GetShaderResource();
                    d3dDeviceContext->PSSetShaderResources(7, 1, &accumulateBufferSRV);*/

                    gl.glBindTextureUnit(7, accumulateBuffer.getTexture());

                    // Do pixel frequency resolve
                    /*d3dDeviceContext->PSSetShader(mGPUQuadDLResolvePS->GetShader(), 0, 0);
                    d3dDeviceContext->OMSetDepthStencilState(mEqualStencilState, 0);
                    d3dDeviceContext->Draw(3, 0);*/

                    mGPUQuadDLResolvePS.enable();
                    mEqualStencilState.run();
                    gl.glDrawArrays(GLenum.GL_TRIANGLES, 0, 3);

                    if (mMSAASamples > 1) {
                        // Do sample frequency resolve
                        /*d3dDeviceContext->PSSetShader(mGPUQuadDLResolvePerSamplePS->GetShader(), 0, 0);
                        d3dDeviceContext->OMSetDepthStencilState(mEqualStencilState, 1);
                        d3dDeviceContext->Draw(3, 0);*/

                        mGPUQuadDLResolvePerSamplePS.enable();
                        mEqualStencilState.run();
                        gl.glDrawArrays(GLenum.GL_TRIANGLES, 0, 3);
                    }
                }
            }
            break;

            case LightCullTechnique.CULL_DEFERRED_NONE:
            {
                // Clear
//                const float zeros[4] = {0.0f, 0.0f, 0.0f, 0.0f};
//                d3dDeviceContext->ClearRenderTargetView(mLitBufferPS->GetRenderTarget(), zeros);
                gl.glClearTexImage(mLitBufferPS.getTexture(), 0, TextureUtils.measureFormat(mLitBufferPS.getFormat()), TextureUtils.measureDataType(mLitBufferPS.getFormat()), null);

                // Full screen triangle setup
                /*d3dDeviceContext->IASetInputLayout(0);
                d3dDeviceContext->IASetPrimitiveTopology(D3D11_PRIMITIVE_TOPOLOGY_TRIANGLELIST);
                d3dDeviceContext->IASetVertexBuffers(0, 0, 0, 0, 0);

                d3dDeviceContext->VSSetShader(mFullScreenTriangleVS->GetShader(), 0, 0);
                d3dDeviceContext->GSSetShader(0, 0, 0);

                d3dDeviceContext->RSSetState(mRasterizerState);
                d3dDeviceContext->RSSetViewports(1, viewport);*/

                mRasterizerState.run();

                /*d3dDeviceContext->PSSetConstantBuffers(0, 1, &mPerFrameConstants);
                d3dDeviceContext->PSSetShaderResources(0, static_cast<UINT>(mGBufferSRV.size()), &mGBufferSRV.front());
                d3dDeviceContext->PSSetShaderResources(5, 1, &lightBufferSRV);*/

                gl.glBindBufferBase(GLenum.GL_UNIFORM_BUFFER, 0, mPerFrameConstants.getBuffer());
                for(int i = 0; i < mGBufferSRV.length - 1; i++){
                    gl.glBindTextureUnit(i, mGBufferSRV[i].getTexture());
                }
//                    d3dDeviceContext->PSSetShaderResources(5, 1, &lightBufferSRV);
                gl.glBindBufferBase(GLenum.GL_SHADER_STORAGE_BUFFER, 1, lightBufferSRV.getBuffer());

                if (mMSAASamples > 1) {
                    // Set stencil mask for samples that require per-sample shading
                    /*d3dDeviceContext->PSSetShader(mRequiresPerSampleShadingPS->GetShader(), 0, 0);
                    d3dDeviceContext->OMSetDepthStencilState(mWriteStencilState, 1);
                    d3dDeviceContext->OMSetRenderTargets(0, 0, mDepthBufferReadOnlyDSV);
                    d3dDeviceContext->Draw(3, 0);*/

                    mRequiresPerSampleShadingPS.enable();
                    mWriteStencilState.run();
                    m_FBO.setRenderTexture(mDepthBufferReadOnlyDSV, null);
                    gl.glDrawArrays(GLenum.GL_TRIANGLES, 0, 3);
                }

                // Additively blend into back buffer
                /*ID3D11RenderTargetView * renderTargets[1] = {mLitBufferPS->GetRenderTarget()};
                d3dDeviceContext->OMSetRenderTargets(1, renderTargets, mDepthBufferReadOnlyDSV);
                d3dDeviceContext->OMSetBlendState(mLightingBlendState, 0, 0xFFFFFFFF);*/

                m_FBO.setRenderTextures(CommonUtil.toArray(mLitBufferPS, mDepthBufferReadOnlyDSV), null);
                mLightingBlendState.run();

                // Do pixel frequency shading
                /*d3dDeviceContext->PSSetShader(mBasicLoopPS->GetShader(), 0, 0);
                d3dDeviceContext->OMSetDepthStencilState(mEqualStencilState, 0);
                d3dDeviceContext->Draw(3, 0);*/
                mBasicLoopPS.enable();
                mEqualStencilState.run();
                gl.glDrawArrays(GLenum.GL_TRIANGLES, 0, 3);

                if (mMSAASamples > 1) {
                    // Do sample frequency shading
                    /*d3dDeviceContext->PSSetShader(mBasicLoopPerSamplePS->GetShader(), 0, 0);
                    d3dDeviceContext->OMSetDepthStencilState(mEqualStencilState, 1);
                    d3dDeviceContext->Draw(3, 0);*/

                    mBasicLoopPerSamplePS.enable();
                    mEqualStencilState.run();
                    gl.glDrawArrays(GLenum.GL_TRIANGLES, 0, 3);
                }
            }
            break;

        };  // switch

        // Cleanup (aka make the runtime happy)
        /*d3dDeviceContext->VSSetShader(0, 0, 0);  todo
        d3dDeviceContext->GSSetShader(0, 0, 0);
        d3dDeviceContext->PSSetShader(0, 0, 0);
        d3dDeviceContext->OMSetRenderTargets(0, 0, 0);
        ID3D11ShaderResourceView* nullSRV[8] = {0, 0, 0, 0, 0, 0, 0, 0};
        d3dDeviceContext->VSSetShaderResources(0, 8, nullSRV);
        d3dDeviceContext->PSSetShaderResources(0, 8, nullSRV);
        d3dDeviceContext->CSSetShaderResources(0, 8, nullSRV);
        ID3D11UnorderedAccessView *nullUAV[1] = {0};
        d3dDeviceContext->CSSetUnorderedAccessViews(0, 1, nullUAV, 0);*/
    }

    float GetGPUCounterSeconds(int[] query ) {
        // Get GPU counters
        long queryTimeA, queryTimeB;

        /*D3D11_QUERY_DATA_TIMESTAMP_DISJOINT queryTimeData;
        while( S_OK != d3dDeviceContext->GetData(query[0], &queryTimeA,
        sizeof(UINT64), 0) ) {}
        while( S_OK != d3dDeviceContext->GetData(query[1], &queryTimeB,
        sizeof(UINT64), 0) ) {}
        while( S_OK != d3dDeviceContext->GetData(query[2], &queryTimeData,
        sizeof(D3D11_QUERY_DATA_TIMESTAMP_DISJOINT), 0) ) {}

        if (0 == queryTimeData.Disjoint) {
            UINT64 deltaTimeTicks = queryTimeB - queryTimeA;
            return (float)deltaTimeTicks / (float)queryTimeData.Frequency;
        } else {
            return 0.0f;
        }*/

        int result;

        do{
            result = gl.glGetQueryObjectuiv(query[0], GLenum.GL_QUERY_RESULT_AVAILABLE);
        }while (result == GLenum.GL_FALSE);
        queryTimeA = gl.glGetQueryObjectui64ui(query[0], GLenum.GL_QUERY_RESULT);

        do{
            result = gl.glGetQueryObjectuiv(query[1], GLenum.GL_QUERY_RESULT_AVAILABLE);
        }while (result == GLenum.GL_FALSE);
        queryTimeB = gl.glGetQueryObjectui64ui(query[1], GLenum.GL_QUERY_RESULT);

        return (float)((double)(queryTimeA - queryTimeB) * 1.e-6);
    }
}
