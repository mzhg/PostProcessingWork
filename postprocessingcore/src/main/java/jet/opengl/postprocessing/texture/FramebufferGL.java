package jet.opengl.postprocessing.texture;

import jet.opengl.postprocessing.common.Disposeable;
import jet.opengl.postprocessing.common.GLFuncProvider;
import jet.opengl.postprocessing.common.GLFuncProviderFactory;
import jet.opengl.postprocessing.common.GLStateTracker;
import jet.opengl.postprocessing.common.GLenum;

import static jet.opengl.postprocessing.common.GLenum.GL_FRAMEBUFFER;
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

public class FramebufferGL implements Disposeable {
    private int m_Framebuffer;
    private int m_Width, m_Height;
    private int m_AttachCount;
    private TextureGL[] m_AttachedTextures = new TextureGL[8];
    private final boolean[] m_Owed = new boolean[8];

    static final int CUBE_FACES[] =
    {
            GLenum.GL_TEXTURE_CUBE_MAP_POSITIVE_X, GLenum.GL_TEXTURE_CUBE_MAP_NEGATIVE_X,
            GLenum.GL_TEXTURE_CUBE_MAP_POSITIVE_Y, GLenum.GL_TEXTURE_CUBE_MAP_NEGATIVE_Y,
            GLenum.GL_TEXTURE_CUBE_MAP_POSITIVE_Z, GLenum.GL_TEXTURE_CUBE_MAP_NEGATIVE_Z,
    };

    static int measureTextureAttachment(TextureGL pTex, int index)
    {
//        assert(pTex);
//        assert(index < 8u);

        int format_conponemt = TextureUtils.measureFormat(pTex.getFormat());
        switch (format_conponemt)
        {
            case GLenum.GL_DEPTH_COMPONENT: return GLenum.GL_DEPTH_ATTACHMENT;
            case GLenum.GL_DEPTH_STENCIL:	 return GLenum.GL_DEPTH_STENCIL_ATTACHMENT;
            case GLenum.GL_STENCIL:		 return GLenum.GL_STENCIL_ATTACHMENT;
            default:
                return GLenum.GL_COLOR_ATTACHMENT0 + index;
        }
    }

    public void addTextures(TextureGL[] textures, TextureAttachDesc[] descs){
        GLFuncProvider gl = GLFuncProviderFactory.getGLFuncProvider();
        bind();

        for (int i = m_AttachCount; i < textures.length; i++)
        {
            TextureGL pTex = textures[i];
            TextureAttachDesc desc = descs[i];
            m_AttachedTextures[i] = pTex;
            switch (desc.type)
            {
                case TEXTURE:
                    if (pTex != null)
                    {
                        gl.glFramebufferTexture(GL_FRAMEBUFFER, measureTextureAttachment(pTex, desc.index), pTex.getTexture(), desc.level);
                        m_AttachCount++;
                    }
                    else
                    {
                        // TODO
                    }
                    break;
                case TEXTURE_1D:
                    if (pTex != null)
                    {
                        assert(pTex.getTarget() == GL_TEXTURE_1D /*|| pTex.getTarget() == GL_TEXTURE_1D_ARRAY*/);
                        gl.glFramebufferTexture1D(GL_FRAMEBUFFER, measureTextureAttachment(pTex, desc.index), GL_TEXTURE_1D, pTex.getTexture(), desc.level);
                        m_AttachCount++;
                    }
                    else
                    {
                        // TODO
                    }
                    break;
                case TEXTURE_2D:
                    if (pTex != null)
                    {
                        int target = pTex.getTarget();
                        assert(target == GL_TEXTURE_2D || target == GL_TEXTURE_CUBE_MAP || target == GL_TEXTURE_RECTANGLE ||
                                target == GL_TEXTURE_2D_MULTISAMPLE);
                        if (target == GL_TEXTURE_CUBE_MAP)
                        {
                            for (int j = 0; j < CUBE_FACES.length; j++)
                            {
                                gl.glFramebufferTexture2D(GL_FRAMEBUFFER, measureTextureAttachment(pTex, desc.index), CUBE_FACES[j], pTex.getTexture(), desc.level);
                            }
                        }
                        else
                        {
                            gl.glFramebufferTexture2D(GL_FRAMEBUFFER, measureTextureAttachment(pTex, desc.index), target, pTex.getTexture(), desc.level);
                        }
                        m_AttachCount++;
                    }
                    else
                    {
                        // TODO
                    }
                    break;
                case TEXTURE_3D:
                    if (pTex != null)
                    {
                        assert(pTex.getTarget() == GL_TEXTURE_3D /*|| pTex.getTarget() == GL_TEXTURE_1D_ARRAY*/);
                        gl.glFramebufferTexture3D(GL_FRAMEBUFFER, measureTextureAttachment(pTex, desc.index), GL_TEXTURE_3D, pTex.getTexture(), desc.level, desc.layer);
                        m_AttachCount++;
                    }
                    else
                    {
                        // TODO
                    }
                    break;
                case TEXTURE_LAYER:
                    if (pTex != null)
                    {
                        int target = pTex.getTarget();
                        assert(target == GL_TEXTURE_3D || target == GL_TEXTURE_2D_ARRAY || target == GL_TEXTURE_1D_ARRAY
                                || target == GL_TEXTURE_2D_MULTISAMPLE_ARRAY || target == GL_TEXTURE_CUBE_MAP_ARRAY);
                        gl.glFramebufferTextureLayer(GL_FRAMEBUFFER, measureTextureAttachment(pTex, desc.index), pTex.getTexture(), desc.level, desc.layer);
                        m_AttachCount++;
                    }
                    else
                    {
                        // TODO
                    }
                    break;
                default:
                    break;
            }
        }
    }
    public Texture2D addTexture2D(Texture2DDesc texDesc, TextureAttachDesc attachDesc){
        Texture2D texture2D = TextureUtils.createTexture2D(texDesc, null);
        m_Owed[m_AttachCount] = true;
        addTextures(new TextureGL[]{texture2D}, new TextureAttachDesc[]{attachDesc});
        return texture2D;
    }

    public void bind(){
        if (m_Framebuffer == 0)
        {
            GLFuncProvider gl = GLFuncProviderFactory.getGLFuncProvider();
            m_Framebuffer = gl.glGenFramebuffer();
//            g_FBOCaches.insert(std::pair<GLuint, FramebufferGL*>(m_Framebuffer, this));
        }

        GLStateTracker.getInstance().setFramebuffer(m_Framebuffer);
    }

    public void unbind(){
        GLStateTracker.getInstance().setFramebuffer(0);
    }

    @Override
    public void dispose() {
        GLFuncProvider gl = GLFuncProviderFactory.getGLFuncProvider();
        if (m_Framebuffer != 0)
        {
            gl.glDeleteFramebuffer(m_Framebuffer);
//            g_FBOCaches.erase(m_Framebuffer);
            m_Framebuffer = 0;
        }

        for (int i = 0; i < m_Owed.length; i++)
        {
            if (m_Owed[i])
            {
//                SAFE_DELETE(m_AttachedTextures[i]);
                m_Owed[i] = false;
                if(m_AttachedTextures[i] != null) {
                    m_AttachedTextures[i].dispose();
                    m_AttachedTextures[i] = null;
                }
            }
        }
    }
}
