package jet.opengl.demos.gpupro.culling;

import jet.opengl.postprocessing.buffer.BufferGL;
import jet.opengl.postprocessing.common.GLenum;
import jet.opengl.postprocessing.shader.GLSLProgram;
import jet.opengl.postprocessing.texture.RenderTargets;
import jet.opengl.postprocessing.texture.Texture2D;
import jet.opengl.postprocessing.texture.TextureUtils;

final class AdaptiveOITRenderer extends TransparencyRenderer{

    private BufferGL mFragmentListNodes;
    private BufferGL mFragmentListConstants;
    private Texture2D mFragmentListFirstNodeOffset;
    private RenderTargets mFBO;
    private GLSLProgram m_pDXResolvePS;

    private Runnable mAOITCompositeBlendState;
    private int mLisTexNodeCount = 1 << 22;

    @Override
    protected void onCreate() {
        super.onCreate();

        mAOITCompositeBlendState = ()->
        {
            gl.glEnable(GLenum.GL_BLEND);
            gl.glBlendFunc(GLenum.GL_ONE, GLenum.GL_ONE_MINUS_SRC_ALPHA);
        };
    }

    @Override
    final OITType getType() {
        return OITType.Adaptive;
    }

    @Override
    void renderScene(Renderer sceneRender, Scene scene) {
        gl.glClearTexImage(mFragmentListFirstNodeOffset.getTexture(), 0, TextureUtils.measureFormat(mFragmentListFirstNodeOffset.getFormat()),
                TextureUtils.measureDataType(mFragmentListFirstNodeOffset.getFormat()), null);
//        FillFragmentListConstants( mLisTexNodeCount * 2);
    }

    private void ResolveDX(Texture2D pOutput){
        mFBO.bind();
        mFBO.setRenderTexture(pOutput, null);

//        ID3D11ShaderResourceView* pAOITClearMaskSRV[] = { mpClearMaskRT->GetShaderResourceView()};

        /*UINT bindIndex;
        if (GetBindIndex(m_pDXResolvePSReflection, "gFragmentListFirstNodeAddressSRV", &bindIndex) == S_OK) {
            pD3DImmediateContext->PSSetShaderResources(bindIndex,1,  &mFragmentListFirstNodeOffset.m_pSRV);  TODO
        }
        if (GetBindIndex(m_pDXResolvePSReflection, "gFragmentListNodesSRV", &bindIndex) == S_OK) {
            pD3DImmediateContext->PSSetShaderResources(bindIndex,1,  &mFragmentListNodes.m_pSRV);  TODO
        }*/

        /*pD3DImmediateContext->PSSetShader(m_pDXResolvePS, NULL, NULL);
        pD3DImmediateContext->OMSetBlendState(mAOITCompositeBlendState, 0, 0xffffffff);*/
        m_pDXResolvePS.enable();
        mAOITCompositeBlendState.run();

        DrawFullScreenQuad();

        gl.glDisable(GLenum.GL_BLEND);
    }

    /*
        DrawFullScreenQuad
        Helper functions for drawing a full screen quad (used for the final tonemapping and bloom composite.
        Renders the quad with and passes vertex position and texture coordinates to current pixel shader
    */
    private void DrawFullScreenQuad(){
        gl.glBindVertexArray(0);
        gl.glBindBuffer(GLenum.GL_ARRAY_BUFFER, 0);
        gl.glBindBuffer(GLenum.GL_ELEMENT_ARRAY_BUFFER_ARB, 0);

        gl.glDrawArrays(GLenum.GL_TRIANGLES, 0, 3);
    }

    @Override
    public void dispose() {

    }
}
