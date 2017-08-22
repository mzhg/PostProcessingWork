package jet.opengl.demos.nvidia.water;

import jet.opengl.postprocessing.buffer.VertexArrayObject;
import jet.opengl.postprocessing.texture.Texture2D;

/**
 * Created by mazhen'gui on 2017/8/22.
 */

public class WaterSimulationParams {
    public WaterSimulationOutput outputMethod;

    public Texture2D outputColorTex;
    public Texture2D outputDepthTex;

    public VertexArrayObject outputVAO;
}
