package jet.opengl.demos.nvidia.shadows;

import com.nvidia.developer.opengl.utils.ShadowmapGenerateProgram;

import jet.opengl.postprocessing.util.BoundingBox;

/**
 * Created by mazhen'gui on 2017/11/8.
 */

public interface ShadowSceneController {
    void onShadowRender(ShadowMapParams shadowMapParams, ShadowmapGenerateProgram program, int cascade);
    /*protected abstract void getShadowCasterBoundingBox( BoundingBox boundingBox);*/
    void addShadowCasterBoundingBox(int index, BoundingBox boundingBox);

    int getShadowCasterCount();
}
