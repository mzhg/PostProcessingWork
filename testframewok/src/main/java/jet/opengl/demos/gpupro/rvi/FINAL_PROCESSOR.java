package jet.opengl.demos.gpupro.rvi;


import javax.xml.ws.handler.PortInfo;

import jet.opengl.postprocessing.shader.GLSLProgram;

// Copies content of the accumulation buffer (of the GBuffer) into the back buffer.
final class FINAL_PROCESSOR extends IPOST_PROCESSOR implements ICONST {
    private DX11_RENDER_TARGET sceneRT;
    private DX11_RENDER_TARGET backBufferRT;
    private GLSLProgram finalPassShader;

    FINAL_PROCESSOR(){
        name = "finalProcessor";
    }

    @Override
    boolean Create() {
        sceneRT = DX11_RENDERER.getInstance().GetRenderTarget(GBUFFER_RT_ID);
        backBufferRT = DX11_RENDERER.getInstance().GetRenderTarget(BACK_BUFFER_RT_ID);
        final String root = "gpupro\\RasterizedVoxelIG\\shaders\\";
        finalPassShader = //DEMO::resourceManager->LoadShader("shaders/finalPass.sdr");
                GLSLProgram.createProgram(root+"postQuad.vert", root + "postQuad.gemo", root+"finalPass.frag", null);

        return true;
    }

    @Override
    DX11_RENDER_TARGET GetOutputRT() {
        return backBufferRT;
    }

    @Override
    void AddSurfaces() {
        SURFACE surface = new SURFACE();
        surface.renderTarget = backBufferRT;
        surface.renderOrder = RenderOrder.POST_PROCESS_RO;
        surface.colorTexture = sceneRT.GetTexture(0);
        surface.shader = finalPassShader;
        DX11_RENDERER.getInstance().SetupPostProcessSurface(surface);
        DX11_RENDERER.getInstance().AddSurface(surface);
    }
}
