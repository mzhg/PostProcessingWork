package jet.opengl.demos.intel.va;

import java.nio.ByteBuffer;

import jet.opengl.demos.intel.cput.D3D11_INPUT_ELEMENT_DESC;
import jet.opengl.postprocessing.buffer.BufferGL;
import jet.opengl.postprocessing.common.BlendState;
import jet.opengl.postprocessing.common.DepthStencilState;
import jet.opengl.postprocessing.common.GLCheck;
import jet.opengl.postprocessing.common.GLFuncProvider;
import jet.opengl.postprocessing.common.GLFuncProviderFactory;
import jet.opengl.postprocessing.common.GLenum;
import jet.opengl.postprocessing.common.RasterizerState;
import jet.opengl.postprocessing.texture.SamplerDesc;
import jet.opengl.postprocessing.texture.SamplerUtils;
import jet.opengl.postprocessing.texture.Texture2D;
import jet.opengl.postprocessing.texture.Texture2DDesc;
import jet.opengl.postprocessing.texture.TextureDataDesc;
import jet.opengl.postprocessing.texture.TextureGL;
import jet.opengl.postprocessing.texture.TextureUtils;
import jet.opengl.postprocessing.util.CacheBuffer;

/**
 * Created by mazhen'gui on 2017/11/20.
 */

public final class VaDirectXTools {
    private static RasterizerState g_RS_CullNone_Fill = null;
    private static RasterizerState g_RS_CullCCW_Fill = null;
    private static RasterizerState g_RS_CullCW_Fill = null;
    private static RasterizerState g_RS_CullNone_Wireframe = null;
    private static RasterizerState g_RS_CullCW_Wireframe = null;
    private static RasterizerState g_RS_CullCCW_Wireframe = null;


    private static DepthStencilState g_DSS_DepthEnabledL_DepthWrite = null;
    private static DepthStencilState g_DSS_DepthEnabledG_DepthWrite = null;
    private static DepthStencilState g_DSS_DepthEnabledLE_NoDepthWrite = null;
    private static DepthStencilState g_DSS_DepthEnabledL_NoDepthWrite = null;
    private static DepthStencilState g_DSS_DepthEnabledGE_NoDepthWrite = null;
    private static DepthStencilState g_DSS_DepthEnabledG_NoDepthWrite = null;
    private static DepthStencilState g_DSS_DepthDisabled_NoDepthWrite = null;
    private static DepthStencilState g_DSS_DepthPassAlways_DepthWrite = null;
    private static DepthStencilState g_DSS_DepthDisabled_StencilCreateMask = null;
    private static DepthStencilState g_DSS_DepthDisabled_StencilUseMask = null;

    private static BlendState g_BS_Opaque = null;
    private static BlendState g_BS_Additive = null;
    private static BlendState g_BS_AlphaBlend = null;
    private static BlendState g_BS_PremultAlphaBlend = null;
    private static BlendState g_BS_Mult = null;

    private static Texture2D g_Texture2D_SRV_White1x1 = null;

    private static Texture2D g_Texture2D_SRV_Noise2D = null;
    private static Texture2D g_Texture2D_SRV_Noise3D = null;

    private static int g_samplerStatePointClamp = 0;
    private static int g_samplerStatePointWrap = 0;
    private static int g_samplerStatePointMirror = 0;
    private static int g_samplerStateLinearClamp = 0;
    private static int g_samplerStateLinearWrap = 0;
    private static int g_samplerStateAnisotropicClamp = 0;
    private static int g_samplerStateAnisotropicWrap = 0;

    private VaDirectXTools(){}

    public static void AssertSetToD3DContextAllShaderTypes(/*ID3D11DeviceContext * context,*/ BufferGL buffer, int slot){
        if(GLCheck.CHECK){

        }
    }

    public static void AssertSetToD3DContextAllShaderTypes(/*ID3D11DeviceContext * context,*/ int samplerState, int slot){
        if(GLCheck.CHECK){

        }
    }

    public static void AssertSetToD3DContextAllShaderTypes(/*ID3D11DeviceContext * context,*/ TextureGL srv, int slot){
        if(GLCheck.CHECK){

        }
    }

    public static void SetToD3DContextAllShaderTypes( /*ID3D11DeviceContext * context,*/ BufferGL buffer, int slot )
    {
//        VA_ASSERT( slot >= 0, L"vaDirectXTools::SetToD3DContextAllShaderTypes : If you've left the DefaultSlot template parameter at default then you have to specify a valid shader slot!" )
        if(slot < 0)
            throw new IllegalArgumentException("vaDirectXTools::SetToD3DContextAllShaderTypes : If you've left the DefaultSlot template parameter at default then you have to specify a valid shader slot!");

        /*context->PSSetConstantBuffers( (uint32)slot, 1, &buffer );
        context->CSSetConstantBuffers( (uint32)slot, 1, &buffer );
        context->VSSetConstantBuffers( (uint32)slot, 1, &buffer );
        context->DSSetConstantBuffers( (uint32)slot, 1, &buffer );
        context->GSSetConstantBuffers( (uint32)slot, 1, &buffer );
        context->HSSetConstantBuffers( (uint32)slot, 1, &buffer );*/
        GLFuncProvider gl = GLFuncProviderFactory.getGLFuncProvider();
        if(buffer != null) {
            gl.glBindBufferBase(buffer.getTarget(), slot, buffer.getBuffer());
        }else{
            gl.glBindBufferBase(GLenum.GL_UNIFORM_BUFFER, slot, 0);
        }
    }

    public static void SetToD3DContextAllShaderTypes( /*ID3D11DeviceContext * context,*/ TextureGL buffer, int slot )
    {
//        VA_ASSERT( slot >= 0, L"vaDirectXTools::SetToD3DContextAllShaderTypes : If you've left the DefaultSlot template parameter at default then you have to specify a valid shader slot!" )
        if(slot < 0)
            throw new IllegalArgumentException("vaDirectXTools::SetToD3DContextAllShaderTypes : If you've left the DefaultSlot template parameter at default then you have to specify a valid shader slot!");

        /*context->PSSetConstantBuffers( (uint32)slot, 1, &buffer );
        context->CSSetConstantBuffers( (uint32)slot, 1, &buffer );
        context->VSSetConstantBuffers( (uint32)slot, 1, &buffer );
        context->DSSetConstantBuffers( (uint32)slot, 1, &buffer );
        context->GSSetConstantBuffers( (uint32)slot, 1, &buffer );
        context->HSSetConstantBuffers( (uint32)slot, 1, &buffer );*/
        final GLFuncProvider gl = GLFuncProviderFactory.getGLFuncProvider();
        gl.glActiveTexture(GLenum.GL_TEXTURE0 + slot);
        if(buffer != null) {
            gl.glBindTexture(buffer.getTarget(), buffer.getTexture());
        }else{
            gl.glBindTexture(GLenum.GL_TEXTURE_2D, 0);
        }
    }

    public static void SetToD3DContextAllShaderTypes( /*ID3D11DeviceContext * context,*/ int sampler, int slot )
    {
//        VA_ASSERT( slot >= 0, L"vaDirectXTools::SetToD3DContextAllShaderTypes : If you've left the DefaultSlot template parameter at default then you have to specify a valid shader slot!" );
        if(slot < 0)
            throw new IllegalArgumentException("vaDirectXTools::SetToD3DContextAllShaderTypes : If you've left the DefaultSlot template parameter at default then you have to specify a valid shader slot!");

        /*context->PSSetSamplers( (uint32)slot, 1, &sampler );
        context->CSSetSamplers( (uint32)slot, 1, &sampler );
        context->VSSetSamplers( (uint32)slot, 1, &sampler );
        context->DSSetSamplers( (uint32)slot, 1, &sampler );
        context->GSSetSamplers( (uint32)slot, 1, &sampler );
        context->HSSetSamplers( (uint32)slot, 1, &sampler );*/
        GLFuncProviderFactory.getGLFuncProvider().glBindSampler(slot, sampler);
    }

    static void vaDirectXTools_OnDeviceCreated( /*ID3D11Device* device, IDXGISwapChain* swapChain*/ )
    {
//        HRESULT hr;

//        g_rasterizerStatesLib = new vaDirectXTools_RasterizerStatesLib( device );

        {
            /*CD3D11_RASTERIZER_DESC desc = CD3D11_RASTERIZER_DESC( CD3D11_DEFAULT( ) );

            desc.FillMode = D3D11_FILL_SOLID;
            desc.CullMode = D3D11_CULL_NONE;
            device->CreateRasterizerState( &desc, &g_RS_CullNone_Fill );
            desc.FillMode = D3D11_FILL_WIREFRAME;
            device->CreateRasterizerState( &desc, &g_RS_CullNone_Wireframe );*/

            g_RS_CullNone_Fill = new RasterizerState();
            g_RS_CullNone_Fill.fillMode = GLenum.GL_FILL;
            g_RS_CullNone_Fill.cullFaceEnable = false;

            g_RS_CullNone_Wireframe = new RasterizerState();
            g_RS_CullNone_Wireframe.fillMode = GLenum.GL_LINE;
            g_RS_CullNone_Wireframe.cullFaceEnable = false;


            /*desc.FillMode = D3D11_FILL_SOLID;
            desc.CullMode = D3D11_CULL_BACK;
            desc.FrontCounterClockwise = true;

            device->CreateRasterizerState( &desc, &g_RS_CullCW_Fill );
            desc.FillMode = D3D11_FILL_WIREFRAME;
            device->CreateRasterizerState( &desc, &g_RS_CullCW_Wireframe );*/

            g_RS_CullCW_Fill = new RasterizerState();
            g_RS_CullCW_Fill.fillMode = GLenum.GL_FILL;
            g_RS_CullCW_Fill.cullFaceEnable = true;
            g_RS_CullCW_Fill.cullMode = GLenum.GL_BACK;
            g_RS_CullCW_Fill.frontCounterClockwise = true;

            g_RS_CullCW_Wireframe = new RasterizerState();
            g_RS_CullCW_Wireframe.fillMode = GLenum.GL_LINE;
            g_RS_CullCW_Wireframe.cullFaceEnable = true;
            g_RS_CullCW_Wireframe.cullMode = GLenum.GL_BACK;
            g_RS_CullCW_Wireframe.frontCounterClockwise = true;

            /*desc.FillMode = D3D11_FILL_SOLID;
            desc.CullMode = D3D11_CULL_BACK;
            desc.FrontCounterClockwise = false;

            device->CreateRasterizerState( &desc, &g_RS_CullCCW_Fill );
            desc.FillMode = D3D11_FILL_WIREFRAME;
            device->CreateRasterizerState( &desc, &g_RS_CullCCW_Wireframe );*/

            g_RS_CullCCW_Fill = new RasterizerState();
            g_RS_CullCCW_Fill.fillMode = GLenum.GL_FILL;
            g_RS_CullCCW_Fill.cullFaceEnable = true;
            g_RS_CullCCW_Fill.cullMode = GLenum.GL_BACK;
            g_RS_CullCCW_Fill.frontCounterClockwise = false;

            g_RS_CullCCW_Wireframe = new RasterizerState();
            g_RS_CullCCW_Wireframe.fillMode = GLenum.GL_LINE;
            g_RS_CullCCW_Wireframe.cullFaceEnable = true;
            g_RS_CullCCW_Wireframe.cullMode = GLenum.GL_BACK;
            g_RS_CullCCW_Wireframe.frontCounterClockwise = false;
        }

        {
            /*CD3D11_DEPTH_STENCIL_DESC desc = CD3D11_DEPTH_STENCIL_DESC( CD3D11_DEFAULT( ) );

            desc.DepthEnable = TRUE;
            desc.DepthFunc = D3D11_COMPARISON_ALWAYS;
            V( device->CreateDepthStencilState( &desc, &g_DSS_DepthPassAlways_DepthWrite ) );*/
            g_DSS_DepthPassAlways_DepthWrite = new DepthStencilState();
            g_DSS_DepthPassAlways_DepthWrite.depthEnable = true;
            g_DSS_DepthPassAlways_DepthWrite.depthFunc = GLenum.GL_ALWAYS;

            /*desc.DepthEnable = TRUE;
            desc.DepthFunc = D3D11_COMPARISON_LESS;
            V( device->CreateDepthStencilState( &desc, &g_DSS_DepthEnabledL_DepthWrite ) );*/
            g_DSS_DepthEnabledL_DepthWrite = new DepthStencilState();
            g_DSS_DepthEnabledL_DepthWrite.depthEnable = true;
            g_DSS_DepthEnabledL_DepthWrite.depthFunc = GLenum.GL_LESS;

            /*desc.DepthFunc = D3D11_COMPARISON_GREATER;
            V( device->CreateDepthStencilState( &desc, &g_DSS_DepthEnabledG_DepthWrite ) );*/
            g_DSS_DepthEnabledG_DepthWrite = new DepthStencilState();
            g_DSS_DepthEnabledG_DepthWrite.depthEnable = true;
            g_DSS_DepthEnabledG_DepthWrite.depthFunc = GLenum.GL_GREATER;

            /*desc.DepthWriteMask = D3D11_DEPTH_WRITE_MASK_ZERO;
            desc.DepthFunc = D3D11_COMPARISON_LESS;
            V( device->CreateDepthStencilState( &desc, &g_DSS_DepthEnabledL_NoDepthWrite ) );*/
            g_DSS_DepthEnabledL_NoDepthWrite = new DepthStencilState();
            g_DSS_DepthEnabledL_NoDepthWrite.depthFunc = GLenum.GL_LESS;
            g_DSS_DepthEnabledL_NoDepthWrite.depthWriteMask = false;
            g_DSS_DepthEnabledL_NoDepthWrite.depthEnable = true;

            /*desc.DepthFunc = D3D11_COMPARISON_LESS_EQUAL;
            V( device->CreateDepthStencilState( &desc, &g_DSS_DepthEnabledLE_NoDepthWrite ) );*/
            g_DSS_DepthEnabledLE_NoDepthWrite = new DepthStencilState();
            g_DSS_DepthEnabledLE_NoDepthWrite.depthFunc = GLenum.GL_LEQUAL;
            g_DSS_DepthEnabledLE_NoDepthWrite.depthWriteMask = false;
            g_DSS_DepthEnabledLE_NoDepthWrite.depthEnable = true;

            /*desc.DepthFunc = D3D11_COMPARISON_GREATER;
            V( device->CreateDepthStencilState( &desc, &g_DSS_DepthEnabledG_NoDepthWrite ) );*/
            g_DSS_DepthEnabledG_NoDepthWrite = new DepthStencilState();
            g_DSS_DepthEnabledG_NoDepthWrite.depthFunc = GLenum.GL_GREATER;
            g_DSS_DepthEnabledG_NoDepthWrite.depthWriteMask = false;
            g_DSS_DepthEnabledG_NoDepthWrite.depthEnable = true;

            /*desc.DepthFunc = D3D11_COMPARISON_GREATER_EQUAL;
            V( device->CreateDepthStencilState( &desc, &g_DSS_DepthEnabledGE_NoDepthWrite ) );*/
            g_DSS_DepthEnabledGE_NoDepthWrite = new DepthStencilState();
            g_DSS_DepthEnabledGE_NoDepthWrite.depthFunc = GLenum.GL_GEQUAL;
            g_DSS_DepthEnabledGE_NoDepthWrite.depthWriteMask = false;
            g_DSS_DepthEnabledGE_NoDepthWrite.depthEnable = true;

            /*desc.DepthEnable = FALSE;
            desc.DepthFunc = D3D11_COMPARISON_LESS;
            V( device->CreateDepthStencilState( &desc, &g_DSS_DepthDisabled_NoDepthWrite ) );*/
            g_DSS_DepthDisabled_NoDepthWrite = new DepthStencilState();
            g_DSS_DepthDisabled_NoDepthWrite.depthWriteMask = false;
            g_DSS_DepthDisabled_NoDepthWrite.depthEnable = false;

            /*desc.StencilEnable = true;
            desc.StencilReadMask = 0xFF;
            desc.StencilWriteMask = 0xFF;
            desc.FrontFace.StencilFailOp = D3D11_STENCIL_OP_KEEP;
            desc.FrontFace.StencilDepthFailOp = D3D11_STENCIL_OP_KEEP;
            desc.FrontFace.StencilPassOp = D3D11_STENCIL_OP_REPLACE;
            desc.FrontFace.StencilFunc = D3D11_COMPARISON_ALWAYS;
            desc.BackFace = desc.FrontFace;
            V( device->CreateDepthStencilState( &desc, &g_DSS_DepthDisabled_StencilCreateMask ) );*/
            g_DSS_DepthDisabled_StencilCreateMask = new DepthStencilState();
            g_DSS_DepthDisabled_StencilCreateMask.stencilEnable = true;
            g_DSS_DepthDisabled_StencilCreateMask.frontFace.stencilFailOp = GLenum.GL_KEEP;
            g_DSS_DepthDisabled_StencilCreateMask.frontFace.stencilDepthFailOp = GLenum.GL_KEEP;
            g_DSS_DepthDisabled_StencilCreateMask.frontFace.stencilPassOp = GLenum.GL_REPLACE;
            g_DSS_DepthDisabled_StencilCreateMask.frontFace.stencilWriteMask = 0xFF;
            g_DSS_DepthDisabled_StencilCreateMask.frontFace.stencilFunc = GLenum.GL_ALWAYS;
            g_DSS_DepthDisabled_StencilCreateMask.backFace.set(g_DSS_DepthDisabled_StencilCreateMask.frontFace);


            /*desc.FrontFace.StencilFailOp = D3D11_STENCIL_OP_KEEP;
            desc.FrontFace.StencilDepthFailOp = D3D11_STENCIL_OP_KEEP;
            desc.FrontFace.StencilPassOp = D3D11_STENCIL_OP_KEEP;
            desc.FrontFace.StencilFunc = D3D11_COMPARISON_EQUAL;
            desc.BackFace = desc.FrontFace;
            V( device->CreateDepthStencilState( &desc, &g_DSS_DepthDisabled_StencilUseMask ) );*/

            g_DSS_DepthDisabled_StencilUseMask = new DepthStencilState();
            g_DSS_DepthDisabled_StencilUseMask.stencilEnable = true;
            g_DSS_DepthDisabled_StencilUseMask.frontFace.stencilFailOp = GLenum.GL_KEEP;
            g_DSS_DepthDisabled_StencilUseMask.frontFace.stencilDepthFailOp = GLenum.GL_KEEP;
            g_DSS_DepthDisabled_StencilUseMask.frontFace.stencilPassOp = GLenum.GL_KEEP;
            g_DSS_DepthDisabled_StencilUseMask.frontFace.stencilWriteMask = 0xFF;
            g_DSS_DepthDisabled_StencilUseMask.frontFace.stencilFunc = GLenum.GL_EQUAL;
            g_DSS_DepthDisabled_StencilUseMask.backFace.set(g_DSS_DepthDisabled_StencilCreateMask.frontFace);
        }

        {
            /*CD3D11_BLEND_DESC desc = CD3D11_BLEND_DESC( CD3D11_DEFAULT( ) );

            desc.RenderTarget[0].BlendEnable = false;

            V( device->CreateBlendState( &desc, &g_BS_Opaque ) );*/
            g_BS_Opaque = new BlendState();
            g_BS_Opaque.blendEnable = false;

            /*desc.RenderTarget[0].BlendEnable = true;
            desc.RenderTarget[0].BlendOp = D3D11_BLEND_OP_ADD;
            desc.RenderTarget[0].BlendOpAlpha = D3D11_BLEND_OP_ADD;

            desc.RenderTarget[0].SrcBlend = D3D11_BLEND_ONE;
            desc.RenderTarget[0].SrcBlendAlpha = D3D11_BLEND_ONE;
            desc.RenderTarget[0].DestBlend = D3D11_BLEND_ONE;
            desc.RenderTarget[0].DestBlendAlpha = D3D11_BLEND_ONE;
            V( device->CreateBlendState( &desc, &g_BS_Additive ) );*/
            g_BS_Additive = new BlendState();
            g_BS_Additive.blendEnable = true;
            g_BS_Additive.blendOp = GLenum.GL_FUNC_ADD;
            g_BS_Additive.srcBlend = GLenum.GL_ONE;
            g_BS_Additive.srcBlendAlpha = GLenum.GL_ONE;
            g_BS_Additive.destBlend = GLenum.GL_ONE;
            g_BS_Additive.destBlendAlpha = GLenum.GL_ONE;

            /*desc.RenderTarget[0].SrcBlend = D3D11_BLEND_SRC_ALPHA;
            desc.RenderTarget[0].SrcBlendAlpha = D3D11_BLEND_ZERO;
            desc.RenderTarget[0].DestBlend = D3D11_BLEND_INV_SRC_ALPHA;
            desc.RenderTarget[0].DestBlendAlpha = D3D11_BLEND_ONE;
            V( device->CreateBlendState( &desc, &g_BS_AlphaBlend ) );*/
            g_BS_AlphaBlend = new BlendState();
            g_BS_AlphaBlend.blendEnable = true;
            g_BS_AlphaBlend.blendOp = GLenum.GL_FUNC_ADD;
            g_BS_AlphaBlend.srcBlend = GLenum.GL_SRC_ALPHA;
            g_BS_AlphaBlend.srcBlendAlpha = GLenum.GL_ZERO;
            g_BS_AlphaBlend.destBlend = GLenum.GL_ONE_MINUS_SRC_ALPHA;
            g_BS_AlphaBlend.destBlendAlpha = GLenum.GL_ONE;

            /*desc.RenderTarget[0].SrcBlend = D3D11_BLEND_ONE;
            desc.RenderTarget[0].SrcBlendAlpha = D3D11_BLEND_ZERO;
            desc.RenderTarget[0].DestBlend = D3D11_BLEND_INV_SRC_ALPHA;
            desc.RenderTarget[0].DestBlendAlpha = D3D11_BLEND_ONE;
            V( device->CreateBlendState( &desc, &g_BS_PremultAlphaBlend ) );*/
            g_BS_PremultAlphaBlend = new BlendState();
            g_BS_PremultAlphaBlend.blendEnable = true;
            g_BS_PremultAlphaBlend.blendOp = GLenum.GL_FUNC_ADD;
            g_BS_PremultAlphaBlend.srcBlend = GLenum.GL_ONE;
            g_BS_PremultAlphaBlend.srcBlendAlpha = GLenum.GL_ZERO;
            g_BS_PremultAlphaBlend.destBlend = GLenum.GL_ONE_MINUS_SRC_ALPHA;
            g_BS_PremultAlphaBlend.destBlendAlpha = GLenum.GL_ONE;

            /*desc.RenderTarget[0].SrcBlend = D3D11_BLEND_ZERO;
            desc.RenderTarget[0].SrcBlendAlpha = D3D11_BLEND_ZERO;
            desc.RenderTarget[0].DestBlend = D3D11_BLEND_SRC_COLOR;
            desc.RenderTarget[0].DestBlendAlpha = D3D11_BLEND_SRC_ALPHA;
            V( device->CreateBlendState( &desc, &g_BS_Mult ) );*/
            g_BS_Mult = new BlendState();
            g_BS_Mult.blendEnable = true;
            g_BS_Mult.blendOp = GLenum.GL_FUNC_ADD;
            g_BS_Mult.srcBlend = GLenum.GL_ZERO;
            g_BS_Mult.srcBlendAlpha = GLenum.GL_ZERO;
            g_BS_Mult.destBlend = GLenum.GL_SRC_COLOR;
            g_BS_Mult.destBlendAlpha = GLenum.GL_SRC_ALPHA;

        }

        {
            /*D3D11_SUBRESOURCE_DATA data;
            uint8 whitePixel[4] = { 255, 255, 255, 255 };
            data.pSysMem = whitePixel;
            data.SysMemPitch = sizeof( whitePixel );
            data.SysMemSlicePitch = data.SysMemPitch;

            ID3D11Texture2D * texture2D = vaDirectXTools::CreateTexture2D( DXGI_FORMAT_R8G8B8A8_UNORM, 1, 1, &data );
            g_Texture2D_SRV_White1x1 = vaDirectXTools::CreateShaderResourceView( texture2D );

            vaDirectXCore::NameObject( texture2D, "g_Texture2D_SRV_White1x1" );
            SAFE_RELEASE( texture2D );*/
            ByteBuffer whitePixel = CacheBuffer.getCachedByteBuffer(4);
            whitePixel.put((byte)255).put((byte)255).put((byte)255).put((byte)255).flip();
            g_Texture2D_SRV_White1x1 = TextureUtils.createTexture2D(new Texture2DDesc(1, 1, GLenum.GL_RGBA8),
                    new TextureDataDesc(GLenum.GL_RGBA, GLenum.GL_UNSIGNED_BYTE, whitePixel));
            g_Texture2D_SRV_White1x1.setName("g_Texture2D_SRV_White1x1");
        }

        // samplers
        {
            /*CD3D11_SAMPLER_DESC desc = CD3D11_SAMPLER_DESC( CD3D11_DEFAULT( ) );
            desc.Filter = D3D11_FILTER_MIN_MAG_MIP_POINT;
            desc.AddressU = desc.AddressV = desc.AddressW = D3D11_TEXTURE_ADDRESS_CLAMP;
            device->CreateSamplerState( &desc, &g_samplerStatePointClamp );
            desc.AddressU = desc.AddressV = desc.AddressW = D3D11_TEXTURE_ADDRESS_WRAP;
            device->CreateSamplerState( &desc, &g_samplerStatePointWrap );
            desc.AddressU = desc.AddressV = desc.AddressW = D3D11_TEXTURE_ADDRESS_MIRROR;
            device->CreateSamplerState( &desc, &g_samplerStatePointMirror );

            desc.Filter = D3D11_FILTER_MIN_MAG_MIP_LINEAR;
            desc.AddressU = desc.AddressV = desc.AddressW = D3D11_TEXTURE_ADDRESS_CLAMP;
            device->CreateSamplerState( &desc, &g_samplerStateLinearClamp );
            desc.AddressU = desc.AddressV = desc.AddressW = D3D11_TEXTURE_ADDRESS_WRAP;
            device->CreateSamplerState( &desc, &g_samplerStateLinearWrap );

            desc.Filter = D3D11_FILTER_ANISOTROPIC;
            desc.AddressU = desc.AddressV = desc.AddressW = D3D11_TEXTURE_ADDRESS_CLAMP;
            device->CreateSamplerState( &desc, &g_samplerStateAnisotropicClamp );
            desc.AddressU = desc.AddressV = desc.AddressW = D3D11_TEXTURE_ADDRESS_WRAP;
            device->CreateSamplerState( &desc, &g_samplerStateAnisotropicWrap );*/

            SamplerDesc desc = new SamplerDesc();
            desc.minFilter = GLenum.GL_NEAREST_MIPMAP_NEAREST;
            desc.magFilter = GLenum.GL_NEAREST;
            desc.wrapR = desc.wrapS = desc.wrapT = GLenum.GL_CLAMP_TO_EDGE;
            g_samplerStatePointClamp = SamplerUtils.createSampler(desc);
            desc.wrapR = desc.wrapS = desc.wrapT = GLenum.GL_REPEAT;
            g_samplerStatePointWrap = SamplerUtils.createSampler(desc);
            desc.wrapR = desc.wrapS = desc.wrapT = GLenum.GL_MIRRORED_REPEAT;
            g_samplerStatePointMirror = SamplerUtils.createSampler(desc);

            desc.minFilter = GLenum.GL_LINEAR_MIPMAP_LINEAR;
            desc.magFilter = GLenum.GL_LINEAR;
            desc.wrapR = desc.wrapS = desc.wrapT = GLenum.GL_CLAMP_TO_EDGE;
            g_samplerStateLinearClamp = SamplerUtils.createSampler(desc);
            desc.wrapR = desc.wrapS = desc.wrapT = GLenum.GL_MIRRORED_REPEAT;
            g_samplerStateLinearWrap = SamplerUtils.createSampler(desc);

            desc.anisotropic = 1;  // Default value.
            desc.wrapR = desc.wrapS = desc.wrapT = GLenum.GL_CLAMP_TO_EDGE;
            g_samplerStateAnisotropicClamp = SamplerUtils.createSampler(desc);
            desc.wrapR = desc.wrapS = desc.wrapT = GLenum.GL_MIRRORED_REPEAT;
            g_samplerStateAnisotropicWrap = SamplerUtils.createSampler(desc);
        }

        {
            //SAFE_RELEASE( m_noiseTexture );

            //V( GetDevice()->CreateTexture( m_noiseTextureResolution, m_noiseTextureResolution, 0, 0, D3DFMT_A8R8G8B8, D3DPOOL_MANAGED, &m_noiseTexture, null) );

            //int levelCount = m_noiseTexture->GetLevelCount();
            //int resolution = m_noiseTextureResolution;

            //for( int level = 0; level < levelCount; level++ )
            //{
            //   D3DLOCKED_RECT noiseLockedRect;
            //   V( m_noiseTexture->LockRect( level, &noiseLockedRect, null, 0 ) );

            //   unsigned int * texRow = (unsigned int *)noiseLockedRect.pBits;

            //   for( int y = 0; y < resolution; y++ )
            //   {
            //      for( int x = 0; x < resolution; x++ )
            //      {
            //         texRow[x] = (0xFF & (int)(randf() * 256.0f));
            //         texRow[x] |= (0xFF & (int)(randf() * 256.0f)) << 8;

            //         float ang = randf();
            //         float fx = sinf( ang * (float)PI * 2.0f ) * 0.5f + 0.5f;
            //         float fy = sinf( ang * (float)PI * 2.0f ) * 0.5f + 0.5f;

            //         texRow[x] |= (0xFF & (int)(fx * 256.0f)) << 16;
            //         texRow[x] |= (0xFF & (int)(fy * 256.0f)) << 24;
            //      }
            //      texRow += noiseLockedRect.Pitch / sizeof(*texRow);
            //   }
            //   V( m_noiseTexture->UnlockRect(level) );
            //   resolution /= 2;
            //vaDirectXCore::NameObject( texture2D, "g_Texture2D_SRV_White1x1" );
            //}
        }
    }

    public static BlendState GetBS_Opaque( )
    {
        return g_BS_Opaque;
    }

    public static BlendState GetBS_Additive( )
    {
        return g_BS_Additive;
    }

    public static BlendState GetBS_AlphaBlend( )
    {
        return g_BS_AlphaBlend;
    }

    public static BlendState GetBS_PremultAlphaBlend( )
    {
        return g_BS_PremultAlphaBlend;
    }

    public static BlendState GetBS_Mult( )
    {
        return g_BS_Mult;
    }

    public static RasterizerState GetRS_CullNone_Fill( )
    {
        return g_RS_CullNone_Fill;
    }

    public static RasterizerState GetRS_CullCCW_Fill( )
    {
        return g_RS_CullCCW_Fill;
    }

    public static RasterizerState GetRS_CullCW_Fill( )
    {
        return g_RS_CullCW_Fill;
    }

    public static RasterizerState GetRS_CullNone_Wireframe( )
    {
        return g_RS_CullNone_Wireframe;
    }

    public static RasterizerState GetRS_CullCCW_Wireframe( )
    {
        return g_RS_CullCCW_Wireframe;
    }

    public static RasterizerState GetRS_CullCW_Wireframe( )
    {
        return g_RS_CullCW_Wireframe;
    }

    public static DepthStencilState GetDSS_DepthEnabledL_DepthWrite( )
    {
        return g_DSS_DepthEnabledL_DepthWrite;
    }

    public static DepthStencilState GetDSS_DepthEnabledG_DepthWrite( )
    {
        return g_DSS_DepthEnabledG_DepthWrite;
    }

    public static DepthStencilState GetDSS_DepthEnabledL_NoDepthWrite( )
    {
        return g_DSS_DepthEnabledL_NoDepthWrite;
    }

    public static DepthStencilState GetDSS_DepthEnabledLE_NoDepthWrite( )
    {
        return g_DSS_DepthEnabledLE_NoDepthWrite;
    }

    public static DepthStencilState GetDSS_DepthEnabledG_NoDepthWrite( )
    {
        return g_DSS_DepthEnabledG_NoDepthWrite;
    }

    public static DepthStencilState GetDSS_DepthEnabledGE_NoDepthWrite( )
    {
        return g_DSS_DepthEnabledGE_NoDepthWrite;
    }

    public static DepthStencilState GetDSS_DepthDisabled_NoDepthWrite( )
    {
        return g_DSS_DepthDisabled_NoDepthWrite;
    }

    public static DepthStencilState GetDSS_DepthPassAlways_DepthWrite( )
    {
        return g_DSS_DepthPassAlways_DepthWrite;
    }

    public static DepthStencilState GetDSS_DepthDisabled_NoDepthWrite_StencilCreateMask( )
    {
        return g_DSS_DepthDisabled_StencilCreateMask;
    }

    public static DepthStencilState GetDSS_DepthDisabled_NoDepthWrite_StencilUseMask( )
    {
        return g_DSS_DepthDisabled_StencilUseMask;
    }

    public static Texture2D GetTexture2D_SRV_White1x1( )
    {
        return g_Texture2D_SRV_White1x1;
    }

    public static int GetSamplerStatePointClamp( ) { return g_samplerStatePointClamp; }
    public static int GetSamplerStatePointWrap( ) { return g_samplerStatePointWrap; }
    public static int GetSamplerStatePointMirror( ) { return g_samplerStatePointMirror; }
    public static int GetSamplerStateLinearClamp( ) { return g_samplerStateLinearClamp; }
    public static int GetSamplerStateLinearWrap( ) { return g_samplerStateLinearWrap; }
    public static int GetSamplerStateAnisotropicClamp( ) { return g_samplerStateAnisotropicClamp; }
    public static int GetSamplerStateAnisotropicWrap( ) { return g_samplerStateAnisotropicWrap; }

    public static int GetSamplerStatePtrPointClamp( ) { return g_samplerStatePointClamp; }
    public static int GetSamplerStatePtrPointWrap( ) { return g_samplerStatePointWrap; }
    public static int GetSamplerStatePtrLinearClamp( ) { return g_samplerStateLinearClamp; }
    public static int GetSamplerStatePtrLinearWrap( ) { return g_samplerStateLinearWrap; }
    public static int GetSamplerStatePtrAnisotropicClamp( ) { return g_samplerStateAnisotropicClamp; }
    public static int GetSamplerStatePtrAnisotropicWrap( ) { return g_samplerStateAnisotropicWrap; }

    public static D3D11_INPUT_ELEMENT_DESC CD3D11_INPUT_ELEMENT_DESC(String semanticName, int semanticIndex, int format, int inputSlot,
                                                                     int alignedByteOffset, int inputSlotClass, int instanceDataStepRate){
        D3D11_INPUT_ELEMENT_DESC desc = new D3D11_INPUT_ELEMENT_DESC();
        desc.SemanticName = semanticName;
        desc.SemanticIndex = semanticIndex;
        desc.Format = format;
        desc.InputSlot = inputSlot;
        desc.AlignedByteOffset = alignedByteOffset;
        desc.InputSlotClass = inputSlotClass;
        desc.InstanceDataStepRate = instanceDataStepRate;
        return desc;
    }
}
