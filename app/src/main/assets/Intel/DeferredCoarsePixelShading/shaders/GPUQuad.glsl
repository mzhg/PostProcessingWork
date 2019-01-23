#include "GBuffer.glsl"

//--------------------------------------------------------------------------------------
// Bounds computation utilities, similar to PointLightBounds.cpp
void UpdateClipRegionRoot(float nc,          // Tangent plane x/y normal coordinate (view space)
                          float lc,          // Light x/y coordinate (view space)
                          float lz,          // Light z coordinate (view space)
                          float lightRadius,
                          float cameraScale, // Project scale for coordinate (_11 or _22 for x/y respectively)
                          inout float clipMin,
                          inout float clipMax)
{
    float nz = (lightRadius - nc * lc) / lz;
    float pz = (lc * lc + lz * lz - lightRadius * lightRadius) /
               (lz - (nz / nc) * lc);

    if (pz > 0.0f) {
        float c = -nz * cameraScale / nc;
        if (nc > 0.0f) {        // Left side boundary
            clipMin = max(clipMin, c);
        } else {                          // Right side boundary
            clipMax = min(clipMax, c);
        }
    }
}

void UpdateClipRegion(float lc,          // Light x/y coordinate (view space)
                      float lz,          // Light z coordinate (view space)
                      float lightRadius,
                      float cameraScale, // Project scale for coordinate (_11 or _22 for x/y respectively)
                      inout float clipMin,
                      inout float clipMax)
{
    float rSq = lightRadius * lightRadius;
    float lcSqPluslzSq = lc * lc + lz * lz;
	float d = rSq * lc * lc - lcSqPluslzSq * (rSq - lz * lz);

    if (d > 0) {
        float a = lightRadius * lc;
        float b = sqrt(d);
        float nx0 = (a + b) / lcSqPluslzSq;
        float nx1 = (a - b) / lcSqPluslzSq;

        UpdateClipRegionRoot(nx0, lc, lz, lightRadius, cameraScale, clipMin, clipMax);
        UpdateClipRegionRoot(nx1, lc, lz, lightRadius, cameraScale, clipMin, clipMax);
    }
}

// Returns bounding box [min.xy, max.xy] in clip [-1, 1] space.
float4 ComputeClipRegion(float3 lightPosView, float lightRadius)
{
    // Early out with empty rectangle if the light is too far behind the view frustum
    float4 clipRegion = float4(1, 1, 0, 0);
    if (lightPosView.z + lightRadius >= mCameraNearFar.x) {
        float2 clipMin = float2(-1.0f, -1.0f);
        float2 clipMax = float2( 1.0f,  1.0f);

        UpdateClipRegion(lightPosView.x, lightPosView.z, lightRadius, mCameraProj._11, clipMin.x, clipMax.x);
        UpdateClipRegion(lightPosView.y, lightPosView.z, lightRadius, mCameraProj._22, clipMin.y, clipMax.y);

        clipRegion = float4(clipMin, clipMax);
    }

    return clipRegion;
}

float4 GPUQuad(vec2 positionViewport, int lightIndex, int sampleIndex)
{
    float3 lit = float3(0.0f, 0.0f, 0.0f);

    if (mUI.visualizeLightCount) {
        lit = rcp(255.0f).xxx;
    } else {
        SurfaceData surface = ComputeSurfaceDataFromGBufferSample(int2(positionViewport.xy), sampleIndex);

        // Avoid shading skybox/background pixels
        // NOTE: Compiler doesn't quite seem to move all the unrelated surface computations inside here
        // We could force it to by restructuring the code a bit, but the "all skybox" case isn't useful for
        // our benchmarking anyways.
        if (surface.positionView.z < mCameraNearFar.y) {
            PointLight light = gLight[lightIndex.x];
            AccumulateBRDF(surface, light, lit);
        }
    }

    return float4(lit, 1.0f);
}