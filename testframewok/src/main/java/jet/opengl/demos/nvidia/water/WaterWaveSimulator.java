package jet.opengl.demos.nvidia.water;

import jet.opengl.demos.nvidia.waves.samples.OceanParameter;
import jet.opengl.demos.nvidia.waves.samples.OceanSimulator;
import jet.opengl.postprocessing.common.Disposeable;
import jet.opengl.postprocessing.texture.Texture2D;

/**
 * Created by mazhen'gui on 2017/8/22.
 */

public interface WaterWaveSimulator extends Disposeable{
    Texture2D getDisplacementMap();
    Texture2D getGradMap();
    Texture2D getNormalMap();

    void updateSimulation(float time);

    static OceanSimulator createOceanSimulator(OceanParameter params){
        return new OceanSimulator("nvidia/WaveWorks/shaders/", params);
    }
}
