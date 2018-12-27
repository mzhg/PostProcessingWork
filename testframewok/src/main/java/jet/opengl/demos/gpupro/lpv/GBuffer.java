package jet.opengl.demos.gpupro.lpv;

import jet.opengl.postprocessing.common.Disposeable;
import jet.opengl.postprocessing.common.GLenum;
import jet.opengl.postprocessing.texture.FramebufferGL;
import jet.opengl.postprocessing.texture.TextureAttachDesc;
import jet.opengl.postprocessing.texture.TextureGL;

final class GBuffer implements Disposeable {
    int width, height;
    FramebufferGL fboMan;
    CTextureManager texMan;

    GBuffer(CTextureManager tMgr, int w, int h){
        texMan = tMgr;
        texMan.createTexture("g_Color", "", width, height, GLenum.GL_NEAREST, GLenum.GL_RGBA16F, false);
        texMan.createTexture("g_Normal", "", width, height, GLenum.GL_NEAREST, GLenum.GL_RGBA16F, false);
        texMan.createTexture("g_Depth", "", width, height, GLenum.GL_LINEAR, GLenum.GL_DEPTH_COMPONENT32,  true);


        TextureGL bindTexs[] = {
                texMan.get("g_Color") ,
                texMan.get("g_Normal") ,
                texMan.get("g_Depth") ,
        };

        TextureAttachDesc colorAttachDesc = new TextureAttachDesc();
        TextureAttachDesc normalAttachDesc = new TextureAttachDesc();
        normalAttachDesc.index = 1;

        TextureAttachDesc attachDescs[]= {
                colorAttachDesc,
                normalAttachDesc,
                colorAttachDesc
        };

        fboMan = new FramebufferGL();
        fboMan.bind();
        fboMan.addTextures(bindTexs, attachDescs);
        fboMan.unbind();

        /*fboMan->initFbo();
        fboMan->bindToFbo(GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, texMan["g_Color"]);
        fboMan->bindToFbo(GL_COLOR_ATTACHMENT1, GL_TEXTURE_2D, texMan["g_Normal"]);
        fboMan->bindToFbo(GL_DEPTH_ATTACHMENT, GL_TEXTURE_2D, texMan["g_Depth"]);
        fboMan->setDrawBuffers();
        if (!fboMan->checkFboStatus()){
            return;
        }*/
    }
    void bindToRender(){ fboMan.bind(); }

    void unbind(){ fboMan.unbind(); }

    @Override
    public void dispose() {
        fboMan.dispose();
    }
}
