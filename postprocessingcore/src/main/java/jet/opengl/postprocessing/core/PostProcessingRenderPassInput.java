package jet.opengl.postprocessing.core;

import java.util.List;

import jet.opengl.postprocessing.texture.Texture2D;
import jet.opengl.postprocessing.texture.Texture2DDesc;
import jet.opengl.postprocessing.texture.TextureGL;

/**
 * Created by mazhen'gui on 2017/4/17.
 */

public class PostProcessingRenderPassInput extends  PostProcessingRenderPass {
    private Texture2D m_inputTexture;

    public PostProcessingRenderPassInput(String name, Texture2D tex) {
        super(name);
        m_inputTexture = tex;
        set(0,0);
    }

    @Override
    public void process(PostProcessingRenderContext context, PostProcessingParameters parameters) {}

    @Override
    public void computeOutDesc(int index, Texture2DDesc out) {
        if(index == 0){
            m_inputTexture.getDesc(out);
        }
    }

    @Override
    public Texture2D getInput(int idx) {
        return idx == 0 ? m_inputTexture : null;
    }

    @Override
    public Texture2D getOutputTexture(int idx) {
        return idx == 0 ? m_inputTexture : null;
    }

    public void setDependency(int slot, PostProcessingRenderPass dependencyPass, int depentSlot) {}

    void reset() {}

    void addDependency(int depentSlot) {}

    void setDependencies(int depentSlot, int dependencies) { }
    int getDependencyCount(int depentSlot) {
        return 0;
    }

    void setInputTextureInternal(Texture2D texture){
        m_inputTexture = texture;
    }

    void resolveDependencies() {}

    void setInputTextures(List<TextureGL> _inputTextures) {}

    void setOutputRenderTexture(int slot, Texture2D texture) {}

    void increaseDependency(int depentSlot) {}
    public void markOutputSlot(int slot) {}
}
