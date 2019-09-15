package jet.opengl.renderer.Unreal4;

import java.util.Map;

import jet.opengl.postprocessing.texture.TextureGL;
import jet.opengl.postprocessing.util.CachaRes;
import jet.opengl.renderer.Unreal4.scenes.ULightComponent;
import jet.opengl.renderer.Unreal4.views.FViewMatrices;

// Structure that hold all information related to previous frame.
public class FPreviousViewInfo {
    // View matrices.
    public final FViewMatrices ViewMatrices = new FViewMatrices();

    // Scene color's PreExposure.
    public float SceneColorPreExposure = 1.0f;

    // Depth buffer and Normals of the previous frame generating this history entry for bilateral kernel rejection.
    @CachaRes
    public TextureGL DepthBuffer;
    @CachaRes
    public TextureGL GBufferA;
    @CachaRes
    public TextureGL GBufferB;
    @CachaRes
    public TextureGL GBufferC;

    // Temporal AA result of last frame
    FTemporalAAHistory TemporalAAHistory;

    // Temporal AA history for diaphragm DOF.
    FTemporalAAHistory DOFSetupHistory;

    // Temporal AA history for SSR
    FTemporalAAHistory SSRHistory;

    // Scene color input for SSR, that can be different from TemporalAAHistory.RT[0] if there is a SSR
    // input post process material.
    @CachaRes
    public TextureGL CustomSSRInput;

    // History for the reflections
    FScreenSpaceFilteringHistory ReflectionsHistory;

    // History for the ambient occlusion
    FScreenSpaceFilteringHistory AmbientOcclusionHistory;

    // History for global illumination
    FScreenSpaceFilteringHistory DiffuseIndirectHistory;

    // History for sky light
    FScreenSpaceFilteringHistory SkyLightHistory;

    // History for shadow denoising.
    Map<ULightComponent, FScreenSpaceFilteringHistory> ShadowHistories;
}
