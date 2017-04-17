package jet.opengl.postprocessing.core.radialblur;

import jet.opengl.postprocessing.core.PostProcessingParameters;
import jet.opengl.postprocessing.core.PostProcessingRenderContext;
import jet.opengl.postprocessing.core.PostProcessingRenderPass;
import jet.opengl.postprocessing.texture.Texture2D;
import jet.opengl.postprocessing.texture.Texture2DDesc;

/**
 * Created by mazhen'gui on 2017/4/17.
 */

public class PostProcessingRadialBlur extends PostProcessingRenderPass {

    public PostProcessingRadialBlur() {
        super("Radial Blur");
        set(1,1);
    }

    @Override
    public void process(PostProcessingRenderContext context, PostProcessingParameters parameters) {

    }

    @Override
    public void computeOutDesc(int index, Texture2DDesc out) {
        Texture2D input = getInput(0);
        if(input != null){
            input.getDesc(out);
        }
    }
}
