package jet.opengl.demos.nvidia.shadows;

import com.nvidia.developer.opengl.utils.BoundingBox;
import com.nvidia.developer.opengl.utils.ShadowmapGenerateProgram;

import java.util.List;

/**
 * Created by mazhen'gui on 2017/11/8.
 */

public interface ShadowSceneController {
    void onShadowRender(ShadowMapParams shadowMapParams, ShadowmapGenerateProgram program, int cascade, int objIndex);
    /*protected abstract void getShadowCasterBoundingBox( BoundingBox boundingBox);*/
    void addShadowCasterBoundingBox(int index, BoundingBox boundingBox);

    /**
     * Get the bounding box of the object specfied by <code>index</code>
     * @param index
     * @param boundingBox The result.
     * @return The set of small bounding boxes whom the object is composed of that can improve the shadow quality. Can be return null.
     */
    List<BoundingBox> getObjectBoundingBox(int index, BoundingBox boundingBox);

    int getShadowCasterCount();
}
