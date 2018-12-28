package jet.opengl.demos.gpupro.rvi;

import java.io.IOException;

import jet.opengl.postprocessing.common.Disposeable;
import jet.opengl.postprocessing.common.GLFuncProvider;
import jet.opengl.postprocessing.common.GLFuncProviderFactory;
import jet.opengl.postprocessing.texture.SamplerUtils;
import jet.opengl.postprocessing.texture.Texture2DDesc;
import jet.opengl.postprocessing.texture.TextureGL;
import jet.opengl.postprocessing.texture.TextureUtils;

// DX11_TEXTURE
//   Manages a texture.
final class DX11_TEXTURE implements Disposeable {
    private String name;
    private int sampler;

    private TextureGL texture;

    DX11_TEXTURE()
    {
        sampler = 0;
        texture = null;
    }


    boolean LoadFromFile(String name,int sampler){
        this.name = name;
        try {
            texture = TextureUtils.createTexture2DFromFile(name, false);
        } catch (IOException e) {
            e.printStackTrace();

            return false;
        }

        if(sampler == 0){
            this.sampler = SamplerUtils.getDefaultSampler();
        }else{
            this.sampler = sampler;
        }

        return true;
    }

    // creates render-target texture
    boolean CreateRenderable(int width,int height,int depth,int format,int sampler){
        name = "renderTargetTexture";

        Texture2DDesc desc = new Texture2DDesc(width, height,format);
        desc.arraySize = depth;
        texture = TextureUtils.createTexture2D(desc, null);

        if(sampler == 0){
            this.sampler = SamplerUtils.getDefaultSampler();
        }else{
            this.sampler = sampler;
        }

        return true;
    }

    void Bind(int bindingPoint){
        GLFuncProvider gl = GLFuncProviderFactory.getGLFuncProvider();
        gl.glBindTextureUnit(bindingPoint, texture.getTexture());
        gl.glBindSampler(bindingPoint, sampler);
    }

    TextureGL GetUnorderdAccessView()
    {
        return texture;
    }

    int GetSampler()
    {
        return sampler;
    }

	String GetName()
    {
        return name;
    }

    @Override
    public void dispose() {
        SAFE_RELEASE(texture);
    }
}
