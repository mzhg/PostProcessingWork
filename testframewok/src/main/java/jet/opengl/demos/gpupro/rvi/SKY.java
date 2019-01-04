package jet.opengl.demos.gpupro.rvi;

import jet.opengl.postprocessing.common.GLFuncProvider;
import jet.opengl.postprocessing.common.GLFuncProviderFactory;
import jet.opengl.postprocessing.common.GLenum;
import jet.opengl.postprocessing.shader.GLSLProgram;

// SKY
//   Extremely simple sky post-processor. Since all previously rendered opaque geometry
//   had incremented the stencil buffer, for the sky a constant colored full-screen quad
//   is only rendered where the stencil buffer is still 0.
final class SKY extends IPOST_PROCESSOR implements ICONST{
    private DX11_RENDER_TARGET sceneRT;
    private RENDER_TARGET_CONFIG rtConfig;
    private GLSLProgram skyShader;
    private Runnable depthStencilState;
    private GLFuncProvider gl;

    SKY(){  name = "SKY"; }
    @Override
    boolean Create() {
        gl = GLFuncProviderFactory.getGLFuncProvider();
        sceneRT = DX11_RENDERER.getInstance().GetRenderTarget(GBUFFER_RT_ID);

        // only render into the accumulation render-target of the GBuffer
        RT_CONFIG_DESC desc = new RT_CONFIG_DESC();
        desc.numColorBuffers = 1;
        rtConfig = DX11_RENDERER.getInstance().CreateRenderTargetConfig(desc);
        final String root = "gpupro\\RasterizedVoxelIG\\shaders\\";
        skyShader = //DEMO::resourceManager->LoadShader("shaders/sky.sdr");
                GLSLProgram.createProgram(root + "postQuad.vert", root + "sky.frag", null);

        // only render sky, where stencil buffer is still 0
//        DEPTH_STENCIL_DESC depthStencilDesc;
//        depthStencilDesc.stencilTest = true;
//        depthStencilDesc.stencilRef = 1;
//        depthStencilDesc.stencilFunc = GREATER_COMP_FUNC;
//        depthStencilState = DX11_RENDERER.getInstance().CreateDepthStencilState(depthStencilDesc);
//        if(!depthStencilState)
//            return false;

        depthStencilState = ()->
        {
            gl.glDisable(GLenum.GL_DEPTH_TEST);
            gl.glEnable(GLenum.GL_STENCIL_TEST);
            gl.glStencilFunc(GLenum.GL_GREATER, 1, 0xFF);
            gl.glStencilMask(0xFF);
            gl.glStencilOp(GLenum.GL_KEEP, GLenum.GL_KEEP, GLenum.GL_KEEP);
        };

        return true;
    }

    @Override
    DX11_RENDER_TARGET GetOutputRT() {
        return sceneRT;
    }

    @Override
    void AddSurfaces() {
        SURFACE surface = new SURFACE();
        surface.renderTarget = sceneRT;
        surface.renderTargetConfig = rtConfig;
        surface.renderOrder = RenderOrder.SKY_RO;
        surface.shader = skyShader;
        DX11_RENDERER.getInstance().SetupPostProcessSurface(surface);
        surface.depthStencilState = depthStencilState;
        DX11_RENDERER.getInstance().AddSurface(surface);
    }
}
