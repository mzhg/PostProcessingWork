package jet.opengl.demos.gpupro.lpv;

import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector2f;
import org.lwjgl.util.vector.Vector3f;

import java.util.List;

import jet.opengl.postprocessing.util.StackByte;
import jet.opengl.postprocessing.util.StackFloat;
import jet.opengl.postprocessing.util.StackInt;

final class SEngine {
    boolean activeRendering = false;
    int flags = EngineTypes.EFLAG_MAXIMIZED;
//    EngineTypes.EGPUPlatform gpuPlatform;
//    EngineTypes.EGPUVendor gpuVendor;
    ELPVMode lpvMode = ELPVMode.LPV_MODE_COMPUTE;
    ELPVTechnique lpvTechnique = ELPVTechnique.LPV_TECHNIQUE_GATHERING;
    boolean lpvGV = false;
    boolean lpvLobe = false;
    boolean lpvSky = false;
    boolean sslpv = false;
    boolean timing = false;
    boolean noColors = false;

//    glm::vec2 cursor;
//    glm::vec2 cursorOld;

//    EngineTypes.EKey keys;
//    std::map<uint32, EngineTypes.EKey> keysMap;
//    bool keyMode;

    final StackInt activeLpvModel = new StackInt();
    int scenes = 0;
    int lpvModelsCount = 0;
    boolean camPlaying = false;
    int activeSceneIndex = 0;
    List<List<Vector3f>> camTrackPos;
    List<List<Vector3f>> camTrackRot;
    float camTrack;

    int tickOld;
    int tickNew;
    float simulationStep = 1;
    float fps;
    int fpsCounter;

    int drawCalls;

    boolean updateFrustum = true;
    boolean showGeometryBuffer;
    boolean showShadowBuffer;
    int defaultScreenWidth = 1280;
    int defaultScreenHeight = 720;

    int multisampling = EngineTypes.DEFAULT_MULTISAMPLING;
    int maxTextureSize = EngineTypes.MAX_TEXTURE_SIZE;

    int shadowTextureSize = EngineTypes.SHADOW_TEXTURE_SIZE;
    float shadowJittering = EngineTypes.SHADOW_JITTERING;
    int shadowCascadesCount = EngineTypes.SHADOW_CASCADES_COUNT;
    final Vector2f shadowTiles = new Vector2f(EngineTypes.SHADOW_TILES_X, EngineTypes.SHADOW_TILES_Y);
    StackFloat shadowCascadesClips;
    List<Matrix4f> shadowViewProj; // shadow proj * view, used: mesh render - shadow cascade draw (mpv), out mesh draw (mpvsb[])
//    std::vector<SFrustum> shadowFrustum;

    int geometryTextureSize = EngineTypes.GEOMETRY_TEXTURE_SIZE;
    final Vector2f geometryTiles = new Vector2f(EngineTypes.LPV_SUN_SKY_DIRS_COUNT + EngineTypes.LPV_SPECIAL_DIRS_COUNT, EngineTypes.LPV_CASCADES_COUNT);
    StackFloat geometryPos; // sky only
    StackFloat geometryCascadesClips;
    StackFloat geometryCamCascadesClips;
    List<Matrix4f> geometryViewProj; // geometry proj * view, used: mesh render - geom cascade draw (mvp)
//    std::vector<SFrustum> geometryFrustum;

    int sunRaysTextureSize = EngineTypes.SUN_RAYS_TEXTURE_SIZE;

    int lpvSpecialDirsCount = EngineTypes.LPV_SPECIAL_DIRS_COUNT;
    int lpvSunSkyDirsCount = EngineTypes.LPV_SUN_SKY_DIRS_COUNT;
    int lpvSkyDirsCount = EngineTypes.LPV_SKY_DIRS_COUNT;
    int lpvDirsReservedCount = EngineTypes.LPV_DIRS_RESERVED_COUNT;
    int lpvSunSkySpecDirsCount = lpvSpecialDirsCount + lpvSunSkyDirsCount;
    StackByte sunSkyUsed;
    StackFloat sunSkyPoses; // lpv injection, light data
    StackFloat sunSkyRots;
    StackFloat sunSkyColors;

    final Vector3f lpvTextureSize = new Vector3f(EngineTypes.LPV_TEXTURE_SIZE_X, EngineTypes.LPV_TEXTURE_SIZE_Y, EngineTypes.LPV_TEXTURE_SIZE_Z);
    int lpvCascadesCount = EngineTypes.LPV_CASCADES_COUNT;
    int lpvPropagationSteps = EngineTypes.LPV_PROPAGATION_STEPS;
    boolean lpvPropagationSwap = false;
    float lpvIntensity = EngineTypes.LPV_INTENSITY;
    float lpvReflIntensity = EngineTypes.LPV_REFL_INTENSITY;
    StackFloat lpvCellSizes;
    StackFloat lpvPoses; // lpv inject, propagate, out mesh draw

    boolean waitForFlushTimers;

    SEngine(){
        shadowCascadesClips.resize(shadowCascadesCount * 2);
        /*shadowViewProj.resize(shadowCascadesCount);
        shadowFrustum.resize(shadowCascadesCount);
        memcpy(&shadowCascadesClips[0], NEngine::SHADOW_CASCADES_CLIPS, sizeof(float) * shadowCascadesClips.size());
        for(uint32 i = 0; i < shadowCascadesCount; i++)
            shadowViewProj[i] = glm::mat4();

        geometryPos.resize(lpvCascadesCount * NMath::VEC3);
        geometryCascadesClips.resize(lpvCascadesCount * NMath::VEC2);
        geometryCamCascadesClips.resize(lpvCascadesCount * NMath::VEC2);
        geometryViewProj.resize(lpvCascadesCount * lpvSunSkySpecDirsCount);
        geometryFrustum.resize(lpvCascadesCount * lpvSunSkySpecDirsCount);
        memset(&geometryPos[0], 0, sizeof(float) * lpvCascadesCount * NMath::VEC3);
        memcpy(&geometryCamCascadesClips[0], NEngine::GEOMETRY_CAM_CASCADES_DEPTHS, sizeof(float) * geometryCamCascadesClips.size());
        for(uint32 i = 0; i < lpvCascadesCount; i++)
        {
            geometryCascadesClips[i * 2 + 0] = NEngine::LPV_CELL_SIZES[i * NMath::VEC3] * lpvTextureSize.x * 0.5f;
            geometryCascadesClips[i * 2 + 1] = NEngine::GEOMETRY_CASCADES_CLIPS[i];
        }
        for(int i = 0; i < lpvCascadesCount * lpvSunSkySpecDirsCount; i++)
            geometryViewProj[i] = glm::mat4();*/

        sunSkyUsed.resize(lpvSunSkySpecDirsCount);
        sunSkyPoses.resize(lpvSunSkyDirsCount * 3);
        sunSkyRots.resize(lpvSunSkyDirsCount * 3);
        sunSkyColors.resize(lpvSunSkyDirsCount * 3);
//        memcpy(&sunSkyUsed[0], NEngine::SUN_SKY_USED, sizeof(uint8) * lpvSunSkySpecDirsCount);
//        memcpy(&sunSkyPoses[0], NEngine::SUN_SKY_POSES, sizeof(float) * lpvSunSkyDirsCount * NMath::VEC3);
//        memcpy(&sunSkyRots[0], NEngine::SUN_SKY_ROTS, sizeof(float) * lpvSunSkyDirsCount * NMath::VEC2);
//        memcpy(&sunSkyColors[0], NEngine::SUN_SKY_COLORS, sizeof(float) * lpvSunSkyDirsCount * NMath::VEC3);

        lpvCellSizes.resize(lpvCascadesCount * 3);
        lpvPoses.resize(lpvCascadesCount * 3);
//        memcpy(&lpvCellSizes[0], NEngine::LPV_CELL_SIZES, sizeof(float) * lpvCellSizes.size());
    }
}
