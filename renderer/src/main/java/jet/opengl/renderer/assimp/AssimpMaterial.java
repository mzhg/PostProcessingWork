package jet.opengl.renderer.assimp;

import org.lwjgl.util.vector.Vector4f;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;

import jet.opengl.postprocessing.common.Disposeable;
import jet.opengl.postprocessing.common.GLFuncProvider;
import jet.opengl.postprocessing.common.GLFuncProviderFactory;
import jet.opengl.postprocessing.common.GLenum;
import jet.opengl.postprocessing.texture.Texture2D;
import jet.opengl.postprocessing.texture.TextureUtils;
import jet.opengl.postprocessing.util.NvImage;

public class AssimpMaterial implements Disposeable {

    final Texture2D[] mTextures = new Texture2D[AssimpTextureType.values().length];
    final int[] mSamplers = new int[AssimpTextureType.values().length];

    final Vector4f mAmbientColor = new Vector4f();
    final Vector4f mDiffuseColor = new Vector4f();
    final Vector4f mSpecularColor = new Vector4f();

    float mRoughness;

    public Texture2D getTextureBy(AssimpTextureType type){
        return mTextures[type.ordinal()];
    }

    private static final HashMap<String, Texture2D> texCache = new HashMap<>();

    static Texture2D loadTexture2D(String filename, boolean flip, boolean genmipmap) throws IOException {
        Texture2D tex = texCache.get(filename);
        if(tex != null){
            return tex;
        }else{
            if(filename.endsWith("DDS") || filename.endsWith("dds")){
                int tex_id = NvImage.uploadTextureFromDDSFile(filename);
                tex = TextureUtils.createTexture2D(GLenum.GL_TEXTURE_2D, tex_id);
            }else{
                tex = TextureUtils.createTexture2DFromFile(filename, flip, genmipmap);
            }
            texCache.put(filename, tex);
            return tex;
        }
    }

    @Override
    public void dispose() {
        if(GLFuncProviderFactory.isInitlized()){
            GLFuncProvider gl = GLFuncProviderFactory.getGLFuncProvider();
            for(int i = 0; i < mTextures.length; i++){
                if(mTextures[i] != null){
                    mTextures[i].dispose();
                    mTextures[i] = null;
                }

                if(mSamplers[i] != 0){
                    gl.glDeleteSampler(mSamplers[i]);
                    mSamplers[i] = 0;
                }
            }
        }else{
            Arrays.fill(mTextures, null);
            Arrays.fill(mSamplers, 0);
        }
    }
}
