package jet.opengl.demos.Unreal4.atmosphere;

import jet.opengl.demos.Unreal4.UE4View;
import jet.opengl.postprocessing.shader.GLSLProgram;
import jet.opengl.postprocessing.texture.Texture2D;
import jet.opengl.postprocessing.texture.Texture3D;

public class AtmosphereRendering {

    private static final String PRECOMPUTED_DATA_PATH = "E:/textures/AtmosphereRendering/";

    private GLSLProgram mAtmosphereRenderingProgram;
    private Texture2D AtmosphereTransmittance;
    private Texture2D AtmosphereIrradiance;
    private Texture3D AtmosphereInscatter;

    public void RenderAtmosphere(UE4View view){

    }

}
