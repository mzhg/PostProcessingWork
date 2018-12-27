package jet.opengl.demos.gpupro.lpv;

import org.lwjgl.util.vector.Vector3f;
import org.lwjgl.util.vector.Vector3i;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import jet.opengl.postprocessing.common.Disposeable;
import jet.opengl.postprocessing.common.GLFuncProvider;
import jet.opengl.postprocessing.common.GLFuncProviderFactory;
import jet.opengl.postprocessing.common.GLenum;
import jet.opengl.postprocessing.texture.Texture2D;
import jet.opengl.postprocessing.texture.Texture2DDesc;
import jet.opengl.postprocessing.texture.Texture3D;
import jet.opengl.postprocessing.texture.Texture3DDesc;
import jet.opengl.postprocessing.texture.TextureGL;
import jet.opengl.postprocessing.texture.TextureUtils;

final class CTextureManager implements Disposeable {
    private final Map<String,TextureGL> textures = new HashMap<>();
    private boolean clearTextureExtension = true;

    TextureGL get(String name){ return textures.get(name);}
    
    void createTexture(String name, String filePath, int w, int h, int filter, int internalformat,  boolean depth){
        if(!textures.containsKey(name)){
            Texture2D texture;
            if(filePath != null){
                try {
                    texture = TextureUtils.createTexture2DFromFile(filePath, false);
                } catch (IOException e) {
                    e.printStackTrace();
                    texture = null;
                }
            }else{
                Texture2DDesc desc = new Texture2DDesc(w,h, internalformat);
                texture = TextureUtils.createTexture2D(desc, null);
            }

            if(texture == null)
                return;

            textures.put(name, texture);
            final GLFuncProvider gl = GLFuncProviderFactory.getGLFuncProvider();
            gl.glTextureParameteri(texture.getTexture(), GLenum.GL_TEXTURE_MAG_FILTER, filter);
            gl.glTextureParameteri(texture.getTexture(), GLenum.GL_TEXTURE_MIN_FILTER, filter);
            gl.glTextureParameteri(texture.getTexture(), GLenum.GL_TEXTURE_WRAP_S, GLenum.GL_CLAMP_TO_EDGE);
            gl.glTextureParameteri(texture.getTexture(), GLenum.GL_TEXTURE_WRAP_T, GLenum.GL_CLAMP_TO_EDGE);

            if (depth) {
                gl.glTextureParameteri(texture.getTexture(), GLenum.GL_TEXTURE_COMPARE_MODE, GLenum.GL_COMPARE_R_TO_TEXTURE);
                gl.glTextureParameteri(texture.getTexture(), GLenum.GL_TEXTURE_COMPARE_FUNC, GLenum.GL_LEQUAL);
            }
        }
    }

    void createRGBA16F3DTexture(String name, Vector3i dim, int filter, int wrap){
        if(!textures.containsKey(name)){
            Texture3DDesc desc = new Texture3DDesc(dim.x, dim.y, dim.z, 1, GLenum.GL_RGBA16F);
            Texture3D texture = TextureUtils.createTexture3D(desc, null);

            textures.put(name, texture);

            final GLFuncProvider gl = GLFuncProviderFactory.getGLFuncProvider();
            gl.glTextureParameteri(texture.getTexture(), GLenum.GL_TEXTURE_MAG_FILTER, filter);
            gl.glTextureParameteri(texture.getTexture(), GLenum.GL_TEXTURE_MIN_FILTER, filter);
            gl.glTextureParameteri(texture.getTexture(), GLenum.GL_TEXTURE_WRAP_S, wrap);
            gl.glTextureParameteri(texture.getTexture(), GLenum.GL_TEXTURE_WRAP_T, wrap);
            gl.glTextureParameteri(texture.getTexture(), GLenum.GL_TEXTURE_WRAP_R, wrap);
        }
    }

    void createRGBA3DTexture(String name, Vector3f dim, int filter, int wrap){
        if(!textures.containsKey(name)){
            Texture3DDesc desc = new Texture3DDesc((int)dim.x, (int)dim.y, (int)dim.z, 1, GLenum.GL_RGBA8);
            Texture3D texture = TextureUtils.createTexture3D(desc, null);

            textures.put(name, texture);

            final GLFuncProvider gl = GLFuncProviderFactory.getGLFuncProvider();
            gl.glTextureParameteri(texture.getTexture(), GLenum.GL_TEXTURE_MAG_FILTER, filter);
            gl.glTextureParameteri(texture.getTexture(), GLenum.GL_TEXTURE_MIN_FILTER, filter);
            gl.glTextureParameteri(texture.getTexture(), GLenum.GL_TEXTURE_WRAP_S, wrap);
            gl.glTextureParameteri(texture.getTexture(), GLenum.GL_TEXTURE_WRAP_T, wrap);
            gl.glTextureParameteri(texture.getTexture(), GLenum.GL_TEXTURE_WRAP_R, wrap);
        }
    }

    void clear3Dtexture(TextureGL texture){
        final GLFuncProvider gl = GLFuncProviderFactory.getGLFuncProvider();
        gl.glClearTexImage(texture.getTexture(), 0, TextureUtils.measureFormat(texture.getFormat()), TextureUtils.measureDataType(texture.getFormat()), null);
    }
    void setClearTextureExtension(boolean v) {
        clearTextureExtension = v;
    }

    @Override
    public void dispose() {
        for(TextureGL tex : textures.values()){
            tex.dispose();
        }

        textures.clear();
    }
}
