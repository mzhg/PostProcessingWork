package jet.opengl.postprocessing.texture;

import jet.opengl.postprocessing.common.Disposeable;
import jet.opengl.postprocessing.common.GLFuncProvider;
import jet.opengl.postprocessing.common.GLFuncProviderFactory;
import jet.opengl.postprocessing.common.GLStateTracker;

import static jet.opengl.postprocessing.common.GLenum.GL_COLOR_ATTACHMENT0;
import static jet.opengl.postprocessing.common.GLenum.GL_DEPTH_ATTACHMENT;
import static jet.opengl.postprocessing.common.GLenum.GL_DEPTH_COMPONENT;
import static jet.opengl.postprocessing.common.GLenum.GL_DEPTH_STENCIL;
import static jet.opengl.postprocessing.common.GLenum.GL_DEPTH_STENCIL_ATTACHMENT;
import static jet.opengl.postprocessing.common.GLenum.GL_FRAMEBUFFER;
import static jet.opengl.postprocessing.common.GLenum.GL_STENCIL;
import static jet.opengl.postprocessing.common.GLenum.GL_STENCIL_ATTACHMENT;
import static jet.opengl.postprocessing.common.GLenum.GL_TEXTURE_1D;
import static jet.opengl.postprocessing.common.GLenum.GL_TEXTURE_1D_ARRAY;
import static jet.opengl.postprocessing.common.GLenum.GL_TEXTURE_2D;
import static jet.opengl.postprocessing.common.GLenum.GL_TEXTURE_2D_ARRAY;
import static jet.opengl.postprocessing.common.GLenum.GL_TEXTURE_2D_MULTISAMPLE;
import static jet.opengl.postprocessing.common.GLenum.GL_TEXTURE_2D_MULTISAMPLE_ARRAY;
import static jet.opengl.postprocessing.common.GLenum.GL_TEXTURE_3D;
import static jet.opengl.postprocessing.common.GLenum.GL_TEXTURE_CUBE_MAP;
import static jet.opengl.postprocessing.common.GLenum.GL_TEXTURE_CUBE_MAP_ARRAY;
import static jet.opengl.postprocessing.common.GLenum.GL_TEXTURE_RECTANGLE;

/**
 * Created by mazhen'gui on 2017/4/15.
 */

public class RenderTargets implements Disposeable{

    private final AttachInfo m_DepthAttach = new AttachInfo();
    private final AttachInfo m_StencilAttach = new AttachInfo();
    private final AttachInfo m_DepthStencilAttach = new AttachInfo();
    private final AttachInfo[] m_ColorAttaches = new AttachInfo[8];

    private int m_Framebuffer;

    public RenderTargets(){
        for(int i = 0; i < m_ColorAttaches.length; i++){
            m_ColorAttaches[i] = new AttachInfo();
        }
    }

    public void dispose(){
        GLFuncProvider gl = GLFuncProviderFactory.getGLFuncProvider();
        if (m_Framebuffer != 0)
        {
            gl.glDeleteFramebuffer(m_Framebuffer);
//            g_FBOCaches.erase(m_Framebuffer);
            m_Framebuffer = 0;
        }
    }

    public void bind(){
        if (m_Framebuffer == 0)
        {
            GLFuncProvider gl = GLFuncProviderFactory.getGLFuncProvider();
            m_Framebuffer = gl.glGenFramebuffer();
//            g_FBOCaches.insert(std::pair<GLuint, FramebufferGL*>(m_Framebuffer, this));
        }

        GLStateTracker.getInstance().bindFramebuffer(GL_FRAMEBUFFER, m_Framebuffer);
    }

    public void unbind(){
        GLStateTracker.getInstance().bindFramebuffer(GL_FRAMEBUFFER, 0);
    }

    static void deAttachTexture(int attachment, AttachType type)
    {
        GLFuncProvider gl = GLFuncProviderFactory.getGLFuncProvider();
        switch (type)
        {
            case TEXTURE_1D:
                gl.glFramebufferTexture1D(GL_FRAMEBUFFER, attachment, GL_TEXTURE_1D, 0, 0);
                break;
            case TEXTURE_2D:
                gl.glFramebufferTexture2D(GL_FRAMEBUFFER, attachment, GL_TEXTURE_2D, 0, 0);
                break;
            case TEXTURE_3D:
                gl.glFramebufferTexture3D(GL_FRAMEBUFFER, attachment, GL_TEXTURE_3D, 0, 0, 0);
                break;
            case TEXTURE_LAYER:
                gl.glFramebufferTextureLayer(GL_FRAMEBUFFER, attachment, GL_TEXTURE_3D, 0, 0);
                break;
            case TEXTURE:
                gl.glFramebufferTexture(GL_FRAMEBUFFER, attachment, 0, 0);
                break;
            default:
                break;
        }
    }

    private final boolean[] colorHandled = new boolean[8];
    private final TextureGL[] texArrays = new TextureGL[1];
    private final TextureAttachDesc[] descArrays = new TextureAttachDesc[1];

    public void setRenderTexture(TextureGL texture, TextureAttachDesc desc){
        texArrays[0] = texture;
        descArrays[0] = desc;

        setRenderTextures(texArrays, descArrays);
    }

    public void setRenderTextures(TextureGL[] textures, TextureAttachDesc[] descs){
        boolean depthHandled = false;
        boolean depthStencilHandled = false;
        boolean stencilHandled = false;
//        bool colorHandled[8] = { false };

        bind();
        for (int i = 0; i < textures.length; i++)
        {
            TextureGL pTex = textures[i];
            TextureAttachDesc desc = descs[i];
            int index = desc.index;

            assert(index < 8);

            int format_conponemt = TextureUtils.measureFormat(pTex.getFormat());
            switch (format_conponemt)
            {
                case GL_DEPTH_COMPONENT:
                    assert(!depthHandled);
                    handleTextureAttachment(pTex, GL_DEPTH_ATTACHMENT, desc, m_DepthAttach);
                    depthHandled = true;
                    break;
                case GL_DEPTH_STENCIL:
                    assert(!depthStencilHandled);
                    handleTextureAttachment(pTex, GL_DEPTH_STENCIL_ATTACHMENT, desc, m_DepthStencilAttach);
                    depthStencilHandled = true;
                    break;
                case GL_STENCIL:
                    assert(!stencilHandled);
                    handleTextureAttachment(pTex, GL_STENCIL_ATTACHMENT, desc, m_StencilAttach);
                    stencilHandled = true;
                    break;
                default:
                    assert(!colorHandled[index]);
                    handleTextureAttachment(pTex, GL_COLOR_ATTACHMENT0 + index, desc, m_ColorAttaches[index]);
                    colorHandled[index] = true;
                    return ;
            }
        }

        // unbind the previouse textures attchment.
        if (!depthHandled && m_DepthAttach.attached)
        {
            deAttachTexture(GL_DEPTH_ATTACHMENT, m_DepthAttach.type);
            m_DepthAttach.attached = false;
        }

        if (!depthStencilHandled && m_DepthStencilAttach.attached)
        {
            deAttachTexture(GL_DEPTH_STENCIL_ATTACHMENT, m_DepthStencilAttach.type);
            m_DepthStencilAttach.attached = false;
        }

        if (!stencilHandled && m_StencilAttach.attached)
        {
            deAttachTexture(GL_STENCIL_ATTACHMENT, m_StencilAttach.type);
            m_StencilAttach.attached = false;
        }

        for (int i = 0; i < 8; i++)
        {
            if (!colorHandled[i] && m_ColorAttaches[i].attached)
            {
                deAttachTexture(GL_COLOR_ATTACHMENT0 + i, m_ColorAttaches[i].type);
                m_ColorAttaches[i].attached = false;
            }
        }
    }

    private void handleTextureAttachment(TextureGL pTex, int attachment, TextureAttachDesc desc, AttachInfo info){
        GLFuncProvider gl = GLFuncProviderFactory.getGLFuncProvider();
        info.type = desc.type;
        switch (desc.type)
        {
            case TEXTURE_1D:
                if (pTex != null)
                {
                    assert(pTex.getTarget() == GL_TEXTURE_1D);
                    if (!info.attached || info.textureTarget != pTex.getTarget() || info.textureId != pTex.getTexture() || info.level != desc.level)
                    {
                        gl.glFramebufferTexture1D(GL_FRAMEBUFFER, attachment, pTex.getTarget(), pTex.getTexture(), desc.level);
                        info.attached = true;
                        info.textureTarget = pTex.getTarget();
                        info.textureId = pTex.getTexture();
                        info.level = desc.level;
                    }
                }
                else
                {
                    if (info.attached)
                    {
                        gl.glFramebufferTexture1D(GL_FRAMEBUFFER, attachment, GL_TEXTURE_1D, 0, 0);
                        info.attached = false;
                        info.textureTarget = GL_TEXTURE_1D;
                        info.textureId = 0;
                    }
                }
                break;
            case TEXTURE_3D:
                if (pTex != null)
                {
                    assert(pTex.getTarget() == GL_TEXTURE_3D);
                    if (!info.attached || info.textureTarget != pTex.getTarget() || info.textureId != pTex.getTexture() || info.level != desc.level || info.layer != desc.layer)
                    {
                        gl.glFramebufferTexture3D(GL_FRAMEBUFFER, attachment, pTex.getTarget(), pTex.getTexture(), desc.level, desc.layer);
                        info.attached = true;
                        info.textureTarget = pTex.getTarget();
                        info.textureId = pTex.getTexture();
                        info.level = desc.level;
                        info.layer = desc.layer;
                    }
                }
                else
                {
                    if (info.attached)
                    {
                        gl.glFramebufferTexture3D(GL_FRAMEBUFFER, attachment, GL_TEXTURE_3D, 0, 0, 0);
                        info.attached = false;
                        info.textureTarget = GL_TEXTURE_3D;
                        info.textureId = 0;
                        info.level = 0;
                        info.layer = 0;
                    }

                }
                break;
            case TEXTURE_2D:
                if (pTex!=null)
                {
                    int target = pTex.getTarget();
                    assert(target == GL_TEXTURE_2D || target == GL_TEXTURE_CUBE_MAP || target == GL_TEXTURE_RECTANGLE ||
                            target == GL_TEXTURE_2D_MULTISAMPLE);
                    if (!info.attached || info.textureTarget != pTex.getTarget() || info.textureId != pTex.getTexture() || info.level != desc.level)
                    {
                        if (target == GL_TEXTURE_CUBE_MAP)
                        {
                            for (int i = 0; i < FramebufferGL.CUBE_FACES.length; i++)
                            {
                                gl.glFramebufferTexture2D(GL_FRAMEBUFFER, attachment, FramebufferGL.CUBE_FACES[i], pTex.getTexture(), desc.level);
                            }
                        }
                        else
                        {
                            gl.glFramebufferTexture2D(GL_FRAMEBUFFER, attachment, target, pTex.getTexture(), desc.level);
                        }

                        info.attached = true;
                        info.textureTarget = target;
                        info.textureId = pTex.getTexture();
                        info.level = desc.level;
                    }
                }
                else
                {
                    if (info.attached)
                    {
                        gl.glFramebufferTexture2D(GL_FRAMEBUFFER, attachment, GL_TEXTURE_2D, 0, 0);
                        info.attached = false;
                        info.textureTarget = GL_TEXTURE_2D;
                        info.textureId = 0;
                        info.level = 0;
                    }

                }
                break;
            case TEXTURE:
                if (pTex !=null)

                {
                    if (!info.attached || info.textureId != pTex.getTexture() || info.level != desc.level)
                    {
                        gl.glFramebufferTexture(GL_FRAMEBUFFER, attachment, pTex.getTexture(), desc.level);
                        info.attached = true;
                        info.textureId = pTex.getTexture();
                        info.level = desc.level;
                    }
                }
                else
                {
                    if (info.attached)
                    {
                        gl.glFramebufferTexture(GL_FRAMEBUFFER, attachment, 0, 0);
                        info.attached = false;
                        info.textureId = 0;
                        info.level = 0;
                    }
                }
                break;
            case TEXTURE_LAYER:
                if (pTex != null)
                {
                    int target = pTex.getTarget();
                    assert(target == GL_TEXTURE_3D || target == GL_TEXTURE_2D_ARRAY || target == GL_TEXTURE_1D_ARRAY
                            || target == GL_TEXTURE_2D_MULTISAMPLE_ARRAY || target == GL_TEXTURE_CUBE_MAP_ARRAY);
                    if (!info.attached || info.textureId != pTex.getTexture() || info.level != desc.level || info.layer != desc.layer)
                    {
                        gl.glFramebufferTextureLayer(GL_FRAMEBUFFER, attachment, pTex.getTexture(), desc.level, desc.layer);
                        info.attached = true;
                        info.textureId = pTex.getTexture();
                        info.level = desc.level;
                        info.layer = desc.layer;
                    }
                }
                else
                {
                    if (info.attached)
                    {
                        gl.glFramebufferTextureLayer(GL_FRAMEBUFFER, attachment, 0, 0, 0);
                        info.attached = false;
                        info.textureId = 0;
                        info.level = 0;
                        info.layer = 0;
                    }

                }
                break;
            default:
                assert(false);
                break;
        }
    }

    private final static class AttachInfo{
        int textureId;
        int textureTarget;
        boolean attached;
        int level;
        int layer;
        AttachType type;
    }
}
