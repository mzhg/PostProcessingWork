package jet.opengl.renderer.Unreal4.volumetricfog;

import jet.opengl.postprocessing.texture.Texture3D;
import jet.opengl.postprocessing.util.CachaRes;

public class FVolumetricFogViewResources {
    public FVolumetricFogGlobalData VolumetricFogGlobalData;

    @CachaRes
    public Texture3D IntegratedLightScattering;
}
