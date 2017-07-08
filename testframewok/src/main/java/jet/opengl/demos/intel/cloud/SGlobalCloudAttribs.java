package jet.opengl.demos.intel.cloud;

import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector2f;
import org.lwjgl.util.vector.Vector4f;

/**
 * Created by mazhen'gui on 2017/7/7.
 */

final class SGlobalCloudAttribs {

    int uiInnerRingDim = 128;
    int uiRingExtension = 4;
    int uiRingDimension = uiRingExtension + uiInnerRingDim + uiRingExtension;
    int uiNumRings = 5;

    int uiMaxLayers = 4;
    int uiNumCells = 0;
    int uiMaxParticles = 0;
    int uiDownscaleFactor = 2;

    float fCloudDensityThreshold = 0.35f;
    float fCloudThickness = 700.f;
    float fCloudAltitude = 3000.f;
    float fParticleCutOffDist = 2e+5f;

    float fTime;
    float fCloudVolumeDensity = 5e-3f;
    final Vector2f f2LiSpCloudDensityDim = new Vector2f(512,512);

    int uiBackBufferWidth = 1024;
    int uiBackBufferHeight = 768;
    int uiDownscaledBackBufferWidth = uiBackBufferWidth / uiDownscaleFactor;
    int uiDownscaledBackBufferHeight = uiBackBufferHeight/uiDownscaleFactor;

    float fTileTexWidth=32;
    float fTileTexHeight=32;
    int uiLiSpFirstListIndTexDim = 128;
    int uiNumCascades;

    final Vector4f f4Parameter = new Vector4f();

    float fScatteringCoeff = 0.07f;
    float fAttenuationCoeff = fScatteringCoeff;
    int uiNumParticleLayers = 1;
    int uiDensityGenerationMethod = 0;

    boolean bVolumetricBlending = true;
    int uiParameter;
    int uiDensityBufferScale = 2;
    float fReferenceParticleRadius = 200.f;

    final Vector4f[] f4TilingFrustumPlanes = new Vector4f[6];
    // Transform from view space to light projection space
    final Matrix4f mParticleTiling = new Matrix4f();

    SGlobalCloudAttribs(){
        for(int i = 0; i < f4TilingFrustumPlanes.length; i++){
            f4TilingFrustumPlanes[i] = new Vector4f();
        }
    }
}
