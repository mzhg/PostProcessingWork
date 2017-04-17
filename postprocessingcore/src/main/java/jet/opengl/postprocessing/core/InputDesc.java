package jet.opengl.postprocessing.core;

/**
 * Created by mazhen'gui on 2017/4/17.
 */

final class InputDesc {
    PostProcessingRenderPass dependencyPass;
    int slot;

    InputDesc(){}

    InputDesc(PostProcessingRenderPass dependencyPass) {
        this.dependencyPass = dependencyPass;
    }
}
