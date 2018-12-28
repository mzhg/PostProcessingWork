package jet.opengl.demos.gpupro.rvi;

import jet.opengl.postprocessing.common.Disposeable;
import jet.opengl.postprocessing.common.GLFuncProvider;
import jet.opengl.postprocessing.common.GLFuncProviderFactory;
import jet.opengl.postprocessing.common.GLenum;
import jet.opengl.postprocessing.texture.AttachType;
import jet.opengl.postprocessing.texture.RenderTargets;
import jet.opengl.postprocessing.texture.Texture2DDesc;
import jet.opengl.postprocessing.texture.TextureAttachDesc;
import jet.opengl.postprocessing.texture.TextureGL;
import jet.opengl.postprocessing.texture.TextureUtils;

// DX11_RENDER_TARGET
//   Render-target to render/ write into. Can be configured via RENDER_TARGET_CONFIG for each draw-call/ dispatch.
final class DX11_RENDER_TARGET implements Disposeable {
    private int width,height,depth;
    private boolean depthStencil;
    private int numColorBuffers;
    private  int clearMask;
    private boolean clearTarget = true;
    private TextureGL[] renderTargetViews;
    private TextureGL[] attachViews;
    private TextureAttachDesc[] attachDesc;
    private DX11_TEXTURE[] frameBufferTextures;
    private TextureGL depthStencilView;
    private DX11_TEXTURE depthStencilTexture;
    private RenderTargets fbo;

    boolean Create(int width,int height,int depth,int format/*=TEX_FORMAT_RGB16F*/,boolean depthStencil/*=false*/,
                int numColorBuffers/*=1*/,int sampler/*=0*/){
        this.width = width;
        this.height = height;
        this.depth = depth;
        this.depthStencil = depthStencil;
        if((numColorBuffers<0)||(numColorBuffers>8))
            return false;
        this.numColorBuffers = numColorBuffers;
        if((numColorBuffers>0)||(depthStencil))
        {
            /*viewport.TopLeftX = 0.0f;
            viewport.TopLeftY = 0.0f;
            viewport.Width = (float)width;
            viewport.Height = (float)height;
            viewport.MinDepth = 0.0f;
            viewport.MaxDepth = 1.0f;*/

            if(numColorBuffers>0)
            {
                clearMask = GLenum.GL_COLOR_BUFFER_BIT;
                frameBufferTextures = new DX11_TEXTURE[numColorBuffers];
//                if(!frameBufferTextures)
//                    return false;
                renderTargetViews = new TextureGL[numColorBuffers];
//                if(!renderTargetViews)
//                    return false;
                for(int i=0;i<numColorBuffers;i++)
                {
                    if(!frameBufferTextures[i].CreateRenderable(width,height,depth,format,sampler))
                        return false;
                    /*if(DEMO::renderer->GetDevice()->CreateRenderTargetView(frameBufferTextures[i].texture,NULL,&renderTargetViews[i])!=S_OK)
                    return false;*/

                    renderTargetViews[i] = frameBufferTextures[i].GetUnorderdAccessView();
                }
            }

            if(depthStencil)
            {
                clearMask |= (GLenum.GL_DEPTH_BUFFER_BIT /*| GLenum.GL_STENCIL_BUFFER_BIT*/);
                depthStencilTexture = new DX11_TEXTURE();
//                if(!depthStencilTexture)
//                    return false;
                if(!depthStencilTexture.CreateRenderable(width,height,depth,GLenum.GL_DEPTH_COMPONENT24,sampler))
                    return false;

               /* D3D11_DEPTH_STENCIL_VIEW_DESC descDSV;
                ZeroMemory(&descDSV,sizeof(descDSV));
                descDSV.Format = DXGI_FORMAT_D24_UNORM_S8_UINT;
                descDSV.ViewDimension = D3D11_DSV_DIMENSION_TEXTURE2D;
                descDSV.Texture2D.MipSlice = 0;
                if(DEMO::renderer->GetDevice()->CreateDepthStencilView(depthStencilTexture->texture,&descDSV,&depthStencilView)!=S_OK)
                return false;*/
                depthStencilView = depthStencilTexture.GetUnorderdAccessView();
            }

            attachViews = new TextureGL[numColorBuffers + (depthStencil?1:0)];
            System.arraycopy(renderTargetViews, 0, attachViews, 0, numColorBuffers);
            if(depthStencil)
                renderTargetViews[numColorBuffers] = depthStencilView;

            attachDesc = generateAttachDescs(attachViews);
        }

        return true;
    }

    boolean CreateBackBuffer(){
        width = 1280;
        height = 720;
        depth = 1;
        depthStencil = true;
        clearMask =GLenum.GL_COLOR_BUFFER_BIT|GLenum.GL_DEPTH_BUFFER_BIT;
        numColorBuffers = 1;

        /*viewport.TopLeftX = 0.0f;
        viewport.TopLeftY = 0.0f;
        viewport.Width = (float)width;
        viewport.Height = (float)height;
        viewport.MinDepth = 0.0f;
        viewport.MaxDepth = 1.0f;*/

        /*ID3D11Texture2D* backBufferTexture = NULL;
        if(DEMO::renderer->GetSwapChain()->GetBuffer(0,__uuidof( ID3D11Texture2D),(LPVOID*)&backBufferTexture)!=S_OK)
        return false;
        renderTargetViews = new ID3D11RenderTargetView*[numColorBuffers];
        if(!renderTargetViews)
            return false;
        if(DEMO::renderer->GetDevice()->CreateRenderTargetView(backBufferTexture,NULL,&renderTargetViews[0])!=S_OK)
        {
            backBufferTexture->Release();
            return false;
        }
        backBufferTexture->Release();*/
        renderTargetViews = new TextureGL[1];
        Texture2DDesc desc = new Texture2DDesc(width, height, GLenum.GL_RGBA16F);
        renderTargetViews[0] =TextureUtils.createTexture2D(desc, null);


        depthStencilTexture = new DX11_TEXTURE();
//        if(!depthStencilTexture)
//            return false;
        if(!depthStencilTexture.CreateRenderable(width,height,depth,GLenum.GL_DEPTH_COMPONENT24, 0))
            return false;
        /*D3D11_DEPTH_STENCIL_VIEW_DESC descDSV;
        ZeroMemory(&descDSV,sizeof(descDSV));
        descDSV.Format = DXGI_FORMAT_D24_UNORM_S8_UINT;
        descDSV.ViewDimension = D3D11_DSV_DIMENSION_TEXTURE2D;
        descDSV.Texture2D.MipSlice = 0;
        if(DEMO::renderer->GetDevice()->CreateDepthStencilView(depthStencilTexture->texture,&descDSV,&depthStencilView)!=S_OK)
        return false;*/

        depthStencilView = depthStencilTexture.GetUnorderdAccessView();
        attachViews = new TextureGL[]{
                renderTargetViews[0],
                depthStencilView
        };

        attachDesc = generateAttachDescs(attachViews);

        return true;
    }

    private TextureAttachDesc[] generateAttachDescs(TextureGL[] attachViews){
        TextureAttachDesc[] attachDesc = new TextureAttachDesc[attachViews.length];
        for(int i = 0; i < attachDesc.length;i++) {
            TextureAttachDesc attachInfo = new TextureAttachDesc();
            attachInfo.type = AttachType.TEXTURE;
            attachInfo.index = i;
            attachDesc[i] =attachInfo;
        }

        return attachDesc;
    }

    void Bind(RENDER_TARGET_CONFIG rtConfig){
        GLFuncProvider gl = GLFuncProviderFactory.getGLFuncProvider();
        if((numColorBuffers>0)||(depthStencil))
//            DEMO::renderer->GetDeviceContext()->RSSetViewports(1,&viewport);
            gl.glViewport(0,0, width, height);
        if(fbo == null)
            fbo = new RenderTargets();
        fbo.bind();

        if(rtConfig == null)
        {
            if((numColorBuffers>0)||(depthStencil)){
                fbo.setRenderTextures(attachViews, attachDesc);
            }
//                DEMO::renderer->GetDeviceContext()->OMSetRenderTargets(numColorBuffers,renderTargetViews,depthStencilView);
        }
        else
        {
            RT_CONFIG_DESC rtConfigDesc = rtConfig.GetDesc();
            if(!rtConfigDesc.computeTarget)
            {
                throw new UnsupportedOperationException();
                /*if(rtConfigDesc.numStructuredBuffers==0) todo
                {
                    DEMO::renderer->GetDeviceContext()->OMSetRenderTargets(rtConfigDesc.numColorBuffers,
                        &renderTargetViews[rtConfigDesc.firstColorBufferIndex],depthStencilView);
                }
                else
                {
                    final int MAX_NUM_SB_BUFFERS = 2;
                    assert(rtConfigDesc.numStructuredBuffers<=MAX_NUM_SB_BUFFERS);
                    ID3D11UnorderedAccessView *sbUnorderedAccessViews[MAX_NUM_SB_BUFFERS];
                    for(int i=0;i<rtConfigDesc.numStructuredBuffers;i++)
                        sbUnorderedAccessViews[i] = ((DX11_STRUCTURED_BUFFER*)rtConfigDesc.structuredBuffers[i])->GetUnorderdAccessView();
                    DEMO::renderer->GetDeviceContext()->OMSetRenderTargetsAndUnorderedAccessViews(numColorBuffers,
                        &renderTargetViews[rtConfigDesc.firstColorBufferIndex],depthStencilView,rtConfigDesc.numColorBuffers,
                        rtConfigDesc.numStructuredBuffers,sbUnorderedAccessViews,NULL);
                }*/
            }
            else
            {
//                DEMO::renderer->GetDeviceContext()->OMSetRenderTargets(0,NULL,NULL);
                gl.glBindFramebuffer(GLenum.GL_FRAMEBUFFER, 0);
                if(rtConfigDesc.numStructuredBuffers==0)
                {
                    throw new UnsupportedOperationException();
//                    assert(rtConfigDesc.numColorBuffers<=MAX_NUM_COLOR_BUFFERS);
//                    ID3D11UnorderedAccessView *sbUnorderedAccessViews[MAX_NUM_COLOR_BUFFERS];
//                    for(int i=0;i<rtConfigDesc.numColorBuffers;i++)
//                        sbUnorderedAccessViews[i] = frameBufferTextures[i].GetUnorderdAccessView();
//                    DEMO::renderer->GetDeviceContext()->CSSetUnorderedAccessViews(0,rtConfigDesc.numColorBuffers,&sbUnorderedAccessViews[rtConfigDesc.firstColorBufferIndex],NULL);
                }
                else
                {
                    throw new UnsupportedOperationException();
//                    assert(rtConfigDesc.numStructuredBuffers<=MAX_NUM_SB_BUFFERS);
//                    ID3D11UnorderedAccessView *sbUnorderedAccessViews[MAX_NUM_SB_BUFFERS];
//                    for(int i=0;i<rtConfigDesc.numStructuredBuffers;i++)
//                        sbUnorderedAccessViews[i] = ((DX11_STRUCTURED_BUFFER*)rtConfigDesc.structuredBuffers[i])->GetUnorderdAccessView();
//                    DEMO::renderer->GetDeviceContext()->CSSetUnorderedAccessViews(0,rtConfigDesc.numStructuredBuffers,sbUnorderedAccessViews,NULL);
                }
            }
        }

        if(clearTarget)
        {
            Clear(clearMask);
            clearTarget = false;
        }
    }

    // indicate, that render-target should be cleared
    void Reset()
    {
        clearTarget = true;
    }

    void Clear(int newClearMask) {
        GLFuncProvider gl = GLFuncProviderFactory.getGLFuncProvider();
        gl.glClear(newClearMask);
    }

    DX11_TEXTURE GetTexture(int index){
        if((index<0)||(index>=numColorBuffers))
            return null;
        return frameBufferTextures[index];
    }

    DX11_TEXTURE GetDepthStencilTexture(){
        return depthStencilTexture;
    }

    int GetWidth()
    {
        return width;
    }

    int GetHeight()
    {
        return height;
    }

    int GetDepth()
    {
        return depth;
    }

    @Override
    public void dispose() {

    }
}
