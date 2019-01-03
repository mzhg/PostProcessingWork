package jet.opengl.demos.gpupro.rvi;

import com.nvidia.developer.opengl.app.NvSampleApp;

public class RasterizedVoxelIGDemo extends NvSampleApp implements ICONST{
    CAMERA mainCamera;
    GLOBAL_ILLUM globalIllum;
    DIRECTIONAL_LIGHT dirLight;
    PATH_POINT_LIGHT[] pathPointLights = new PATH_POINT_LIGHT[NUM_PATH_POINT_LIGHTS];
    boolean pathLigthsEnabled;
    boolean pathLightsAnimated;
    boolean showHelp;
}
