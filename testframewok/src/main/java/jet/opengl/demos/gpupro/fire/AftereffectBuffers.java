package jet.opengl.demos.gpupro.fire;

import java.nio.IntBuffer;

import jet.opengl.postprocessing.common.GLFuncProvider;
import jet.opengl.postprocessing.common.GLFuncProviderFactory;
import jet.opengl.postprocessing.common.GLenum;
import jet.opengl.postprocessing.util.CacheBuffer;

/**
 * Created by mazhen'gui on 2017/10/28.
 */

final class AftereffectBuffers {
    int[] d_dimX = new int[3];
    int[] d_dimY = new int[3];
    int[] d_textures = new int[3];
    int[] d_framebuffers = new int[3];
    int[] d_depthbuffer = new int[2];

    private GLFuncProvider gl;

    void dispose(){
        if( d_depthbuffer[0] != 0) gl.glDeleteRenderbuffers(CacheBuffer.wrap(d_depthbuffer));
        if( d_framebuffers[0] != 0) gl.glDeleteFramebuffers(CacheBuffer.wrap(d_framebuffers));
        if( d_textures[0] != 0) gl.glDeleteTextures(CacheBuffer.wrap(d_textures));
    }

    boolean init( int screen_width, int screen_height,
                  int mipped_screen_width, int mipped_screen_height,
                  int float_tex_width, int float_tex_height){
        gl = GLFuncProviderFactory.getGLFuncProvider();

        d_dimX[0] = screen_width;
        d_dimY[0] = screen_height;
        d_dimX[1] = float_tex_width;
        d_dimY[1] = float_tex_height;
        d_dimX[2] = mipped_screen_width;
        d_dimY[2] = mipped_screen_height;

        IntBuffer framebuffers = CacheBuffer.getCachedIntBuffer(3);
        gl.glGenFramebuffers(framebuffers);
        framebuffers.get(d_framebuffers);

        IntBuffer textures = CacheBuffer.getCachedIntBuffer(3);
        gl.glGenTextures(textures);
        textures.get(d_textures);

        IntBuffer depthbuffer = CacheBuffer.getCachedIntBuffer(2);
        gl.glGenRenderbuffers(depthbuffer);
        depthbuffer.get(d_depthbuffer);

        /**************************************************************************************************************/

        gl.glBindFramebuffer(GLenum.GL_FRAMEBUFFER_EXT,d_framebuffers[0]);
        gl.glBindTexture(GLenum.GL_TEXTURE_2D,d_textures[0]);

        gl.glTexParameteri(GLenum.GL_TEXTURE_2D,GLenum.GL_TEXTURE_MAX_LEVEL,3);
        gl.glTexImage2D(GLenum.GL_TEXTURE_2D,0,GLenum.GL_RGBA8,d_dimX[0],d_dimY[0],0,GLenum.GL_RGBA,GLenum.GL_UNSIGNED_BYTE,null);
        gl.glTexParameteri(GLenum.GL_TEXTURE_2D, GLenum.GL_TEXTURE_MAG_FILTER, GLenum.GL_LINEAR);
        gl.glTexParameteri(GLenum.GL_TEXTURE_2D, GLenum.GL_TEXTURE_MIN_FILTER, GLenum.GL_LINEAR_MIPMAP_NEAREST);
        gl.glTexParameteri(GLenum.GL_TEXTURE_2D, GLenum.GL_TEXTURE_WRAP_S, GLenum.GL_CLAMP_TO_EDGE);
        gl.glTexParameteri(GLenum.GL_TEXTURE_2D, GLenum.GL_TEXTURE_WRAP_T, GLenum.GL_CLAMP_TO_EDGE);
//        GL11.glTexEnvf(GL11.GL_TEXTURE_ENV, GL11.GL_TEXTURE_ENV_MODE, GL11.GL_REPLACE);

        gl.glFramebufferTexture2D(GLenum.GL_FRAMEBUFFER_EXT, GLenum.GL_COLOR_ATTACHMENT0_EXT, GLenum.GL_TEXTURE_2D,d_textures[0],0);

        gl.glBindRenderbuffer(GLenum.GL_RENDERBUFFER_EXT,d_depthbuffer[0]);
        gl.glRenderbufferStorage(GLenum.GL_RENDERBUFFER_EXT, GLenum.GL_DEPTH_COMPONENT24,d_dimX[0],d_dimY[0]);
        gl.glFramebufferRenderbuffer(GLenum.GL_FRAMEBUFFER_EXT, GLenum.GL_DEPTH_ATTACHMENT_EXT, GLenum.GL_RENDERBUFFER_EXT,d_depthbuffer[0]);

        /**************************************************************************************************************/


        gl.glBindFramebuffer(GLenum.GL_FRAMEBUFFER_EXT,d_framebuffers[1]);
        gl.glBindTexture(GLenum.GL_TEXTURE_2D,d_textures[1]);
        gl.glTexParameteri(GLenum.GL_TEXTURE_2D, GLenum.GL_TEXTURE_MAX_LEVEL,0);
        gl.glTexImage2D(GLenum.GL_TEXTURE_2D,0, GLenum.GL_RGB16F_ARB,d_dimX[1],d_dimY[1],0,GLenum.GL_RGB,GLenum.GL_HALF_FLOAT_ARB, null);
        gl.glTexParameteri(GLenum.GL_TEXTURE_2D, GLenum.GL_TEXTURE_MAG_FILTER, GLenum.GL_LINEAR);
//        gl.glTexEnvf(GL11.GL_TEXTURE_ENV, GL11.GL_TEXTURE_ENV_MODE, GL11.GL_REPLACE);
        gl.glFramebufferTexture2D(GLenum.GL_FRAMEBUFFER_EXT, GLenum.GL_COLOR_ATTACHMENT0_EXT,GLenum.GL_TEXTURE_2D,d_textures[1],0);
        gl.glBindRenderbuffer(GLenum.GL_RENDERBUFFER_EXT,d_depthbuffer[1]);
        gl.glRenderbufferStorage(GLenum.GL_RENDERBUFFER_EXT,GLenum.GL_DEPTH_COMPONENT24,d_dimX[1],d_dimY[1]);
        gl.glFramebufferRenderbuffer(GLenum.GL_FRAMEBUFFER_EXT, GLenum.GL_DEPTH_ATTACHMENT_EXT, GLenum.GL_RENDERBUFFER_EXT,d_depthbuffer[1]);


        /*************************************************************************************************************/

        gl.glBindFramebuffer(GLenum.GL_FRAMEBUFFER_EXT,d_framebuffers[2]);
        gl.glBindTexture(GLenum.GL_TEXTURE_2D,d_textures[2]);
        gl.glTexParameteri(GLenum.GL_TEXTURE_2D,GLenum.GL_TEXTURE_MAX_LEVEL,0);
        gl.glTexImage2D(GLenum.GL_TEXTURE_2D,0,GLenum.GL_RGBA8,d_dimX[2],d_dimY[2],0,GLenum.GL_RGBA, GLenum.GL_UNSIGNED_BYTE, null);
        gl.glTexParameteri(GLenum.GL_TEXTURE_2D,GLenum.GL_TEXTURE_MAG_FILTER,GLenum.GL_LINEAR);
        gl.glTexParameteri(GLenum.GL_TEXTURE_2D,GLenum.GL_TEXTURE_WRAP_S,GLenum.GL_CLAMP_TO_EDGE);
        gl.glTexParameteri(GLenum.GL_TEXTURE_2D,GLenum.GL_TEXTURE_WRAP_T,GLenum.GL_CLAMP_TO_EDGE);
//        gl.glTexEnvf(GL11.GL_TEXTURE_ENV,GL11.GL_TEXTURE_ENV_MODE,GL11.GL_REPLACE);
        gl.glFramebufferTexture2D(GLenum.GL_FRAMEBUFFER_EXT, GLenum.GL_COLOR_ATTACHMENT0_EXT,GLenum.GL_TEXTURE_2D,d_textures[2],0);


        /*************************************************************************************************************/

        return true;
    }

    int getTextureNo(int i){ return d_textures[i]; }
    int getFramebufferNo(int i){ return d_framebuffers[i]; }
    int getDimX(int i){ return d_dimX[i]; }
    int getDimY(int i){ return d_dimY[i]; }
}
