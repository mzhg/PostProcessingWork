package jet.opengl.renderer.Unreal4.views;

import jet.opengl.postprocessing.buffer.BufferGL;
import jet.opengl.postprocessing.texture.Texture2D;
import jet.opengl.postprocessing.texture.Texture3D;

public class FViewUniformShaderParameters extends FInstancedViewUniformShaderParameters {

    public Texture3D VolumetricLightmapIndirectionTexture;
    public Texture3D VolumetricLightmapBrickAmbientVector;
    public Texture3D VolumetricLightmapBrickSHCoefficients0;
    public Texture3D VolumetricLightmapBrickSHCoefficients1;
    public Texture3D VolumetricLightmapBrickSHCoefficients2;
    public Texture3D VolumetricLightmapBrickSHCoefficients3;
    public Texture3D VolumetricLightmapBrickSHCoefficients4;
    public Texture3D VolumetricLightmapBrickSHCoefficients5;
    public Texture3D SkyBentNormalBrickTexture;
    public Texture3D DirectionalLightShadowingBrickTexture;
    public Texture3D GlobalDistanceFieldTexture0;
    public Texture3D GlobalDistanceFieldTexture1;
    public Texture3D GlobalDistanceFieldTexture2;
    public Texture3D GlobalDistanceFieldTexture3;
    public Texture2D AtmosphereTransmittanceTexture;
    public Texture2D AtmosphereIrradianceTexture;
    public Texture3D AtmosphereInscatterTexture;
    public Texture2D PerlinNoiseGradientTexture;
    public Texture3D PerlinNoise3DTexture;
    public Texture2D SobolSamplingTexture;
    public Texture2D PreIntegratedBRDF;
    public BufferGL PrimitiveSceneData;
    public BufferGL LightmapSceneData;
    public Texture2D TransmittanceLutTexture;
    public Texture2D SkyViewLutTexture;
    public Texture2D DistantSkyLightLutTexture;
    public Texture3D CameraAerialPerspectiveVolume;
}
