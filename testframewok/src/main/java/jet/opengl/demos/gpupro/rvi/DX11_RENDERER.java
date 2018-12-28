package jet.opengl.demos.gpupro.rvi;

import java.util.ArrayList;
import java.util.List;

import jet.opengl.postprocessing.common.Disposeable;

final class DX11_RENDERER implements Disposeable {

    // list of all render-target configs
    private List<RENDER_TARGET_CONFIG> renderTargetConfigs;

    // list of all render-targets
    private List<DX11_RENDER_TARGET> renderTargets = new ArrayList<>();

    // list of all vertex-buffers
//    private List<DX11_VERTEX_BUFFER*> vertexBuffers;

    // list of all index-buffers
//    LIST<DX11_INDEX_BUFFER*> indexBuffers;

    // list of all uniform-buffers
//    LIST<DX11_UNIFORM_BUFFER*> uniformBuffers;

    // list of all structured buffers
    private List<DX11_STRUCTURED_BUFFER> structuredBuffers = new ArrayList<>();

    // list of all cameras
//    LIST<CAMERA*> cameras;

    // list of all dynamic lights
    private List<ILIGHT> lights = new ArrayList<>();

    // list of all dynamically created meshes
//    LIST<MESH*> meshes;

    // list of all post-processors
    private List<IPOST_PROCESSOR> postProcessors;

    // list of all per frame passed surfaces
//    private List<SURFACE> surfaces;

    // render-states, frequently used by post-processors
    Runnable noneCullRS;
    Runnable noDepthTestDSS;
    Runnable defaultBS;

    // helper variables
//    SURFACE lastSurface;
    boolean frameCleared;

    // DirectX 11 objects
//    ID3D11Device *device;
//    ID3D11DeviceContext *deviceContext;
//    IDXGISwapChain *swapChain;

    private DX11_RENDERER(){

    }

    private static DX11_RENDERER instance;

    static DX11_RENDERER getInstance(){
        if(instance == null)
            instance = new DX11_RENDERER();

        return instance;
    }

    DX11_RENDER_TARGET GetRenderTarget(int ID)
    {
        if((ID<0)||(ID>=renderTargets.size()))
            return null;
        return renderTargets.get(ID);
    }

    DX11_RENDER_TARGET CreateRenderTarget(int width,int height,int depth,int format,boolean depthStencil,int numColorBuffers,
                                                          int sampler)
    {
        DX11_RENDER_TARGET renderTarget = new DX11_RENDER_TARGET();
        if(!renderTarget.Create(width,height,depth,format,depthStencil,numColorBuffers,sampler))
        {
//            SAFE_DELETE(renderTarget);
//            return NULL;
        }
        renderTargets.add(renderTarget);
        return renderTarget;
    }

    DX11_STRUCTURED_BUFFER CreateStructuredBuffer(int bindingPoint,int elementCount,int elementSize)
    {
        DX11_STRUCTURED_BUFFER structuredBuffer = new DX11_STRUCTURED_BUFFER();
//        if(!structuredBuffer)
//            return NULL;
        if(!structuredBuffer.Create(bindingPoint,elementCount,elementSize))
        {
//            SAFE_DELETE(structuredBuffer);
//            return NULL;
            throw new IllegalStateException();
        }
        structuredBuffers.add(structuredBuffer);
        return structuredBuffer;
    }

    RENDER_TARGET_CONFIG CreateRenderTargetConfig(RT_CONFIG_DESC desc)
    {
        for(int i=0;i<renderTargetConfigs.size();i++)
        {
            if(renderTargetConfigs.get(i).GetDesc().equals(desc))
            return renderTargetConfigs.get(i);
        }
        RENDER_TARGET_CONFIG renderTargetConfig = new RENDER_TARGET_CONFIG();
//        if(!renderTargetConfig)
//            return NULL;
        if(!renderTargetConfig.Create(desc))
        {
//            SAFE_DELETE(renderTargetConfig);
//            return NULL;
        }
        renderTargetConfigs.add(renderTargetConfig);
        return renderTargetConfig;
    }

    ILIGHT GetLight(int index)
    {
        if((index<0)||(index>=lights.size()))
            return null;
        return lights.get(index);
    }

    int GetNumLights()
    {
        return lights.size();
    }

    @Override
    public void dispose() {

    }
}
