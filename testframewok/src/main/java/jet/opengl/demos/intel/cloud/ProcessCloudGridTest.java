package jet.opengl.demos.intel.cloud;

import org.lwjgl.util.vector.Vector2f;
import org.lwjgl.util.vector.Vector3f;
import org.lwjgl.util.vector.Vector3i;
import org.lwjgl.util.vector.Vector4f;

import jet.opengl.postprocessing.util.Numeric;
import jet.opengl.postprocessing.util.StackInt;

/**
 * Created by Administrator on 2017/7/18 0018.
 */

 class ProcessCloudGridTest {
    static final float g_fEarthRadius = 6360000.0f;
    static final float g_fTimeScale = 1.0f;

    static final int THREAD_GROUP_SIZE = 128;

    SRenderAttribs      g_RenderAttribs;
    SGlobalCloudAttribs g_GlobalCloudAttribs;

    int[] g_PackedCellLocations;

    SCloudCellAttribs[] g_CloudCellsRW;
    StackInt g_ValidCellsAppendBuf;
    StackInt g_VisibleCellsAppendBuf;

    private int uiCellI, uiCellJ, uiRing, uiLayer;

    ProcessCloudGridTest(SGlobalCloudAttribs globalCloudAttribs, SRenderAttribs renderAttribs, int[] packedCellLocations){
        g_GlobalCloudAttribs = globalCloudAttribs;
        g_RenderAttribs = renderAttribs;
        g_PackedCellLocations = packedCellLocations;

        g_CloudCellsRW = new SCloudCellAttribs[globalCloudAttribs.uiNumCells];
        for(int i = 0; i < g_CloudCellsRW.length; i++){
            g_CloudCellsRW[i] = new SCloudCellAttribs();
        }
    }

    void process(Vector3i gl_WorkGroupID, Vector3i gl_LocalInvocationID){
        Vector3i Gid = gl_WorkGroupID;
        Vector3i GTid = gl_LocalInvocationID;

        int uiCellId = Gid.x * THREAD_GROUP_SIZE + GTid.x;
        if( uiCellId >= g_GlobalCloudAttribs.uiNumCells )
            return;

        // Get cell location in the grid
        int uiPackedLocation = g_PackedCellLocations[uiCellId];

        UnPackParticleIJRing(uiPackedLocation/*, uiCellI, uiCellJ, uiRing, uiLayer*/);

        // Compute cell center world space coordinates
        int uiRingDimension = g_GlobalCloudAttribs.uiRingDimension;
        final float fRingWorldStep = GetCloudRingWorldStep(uiRing/*, g_GlobalCloudAttribs*/);

        //
        //
        //                                 Camera
        //                               |<----->|
        //   |   X   |   X   |   X   |   X   |   X   |   X   |   X   |   X   |       CameraI == 4
        //   0  0.5     1.5     2.5     3.5  4  4.5     5.5     6.5     7.5  8       uiRingDimension == 8
        //                                   |
        //                                CameraI
        float fCameraI = (float) Math.floor(g_RenderAttribs.f3CameraPos.x/fRingWorldStep + 0.5);
        float fCameraJ = (float) Math.floor(g_RenderAttribs.f3CameraPos.z/fRingWorldStep + 0.5);

        Vector3f f3CellCenter = new Vector3f();
        f3CellCenter.x = (fCameraI + (uiCellI) - (uiRingDimension/2) + 0.5f) * fRingWorldStep;
        f3CellCenter.z = (fCameraJ + (uiCellJ) - (uiRingDimension/2) + 0.5f) * fRingWorldStep;
        f3CellCenter.y = 0;

        Vector3f f3EarthCentre = new Vector3f(0, -g_fEarthRadius, 0);
        Vector3f f3DirFromEarthCenter = Vector3f.sub(f3CellCenter, f3EarthCentre, null);
        float fDistFromCenter = Vector3f.length(f3DirFromEarthCenter);
//        f3CellCenter = f3EarthCentre + f3DirFromEarthCenter * ((g_fEarthRadius + g_GlobalCloudAttribs.fCloudAltitude)/fDistFromCenter);
        f3CellCenter.x = f3EarthCentre.x + f3DirFromEarthCenter.x * ((g_fEarthRadius + g_GlobalCloudAttribs.fCloudAltitude)/fDistFromCenter);
        f3CellCenter.y = f3EarthCentre.y + f3DirFromEarthCenter.y * ((g_fEarthRadius + g_GlobalCloudAttribs.fCloudAltitude)/fDistFromCenter);
        f3CellCenter.z = f3EarthCentre.z + f3DirFromEarthCenter.z * ((g_fEarthRadius + g_GlobalCloudAttribs.fCloudAltitude)/fDistFromCenter);

        int uiNumActiveLayers = GetNumActiveLayers(g_GlobalCloudAttribs.uiMaxLayers, uiRing);

        float fParticleSize = GetParticleSize(fRingWorldStep);
        Vector3f f3Size = GetParticleScales(fParticleSize, uiNumActiveLayers);

        // Construct local frame
        Vector3f f3Normal = Vector3f.sub(f3CellCenter,f3EarthCentre, null);  f3Normal.normalise();
        Vector3f f3Tangent = Vector3f.cross(f3Normal, new Vector3f(0,0,1), null);  f3Tangent.normalise();
        Vector3f f3Bitangent = Vector3f.cross(f3Tangent, f3Normal, null);f3Bitangent.normalise();

        float fTime = g_fTimeScale*g_GlobalCloudAttribs.fTime;

        // Get cloud density in the cell
        Vector4f f4UV01 = GetCloudDensityUV(f3CellCenter, fTime);
        Vector2f f2LODs = ComputeDensityTexLODsFromStep(f3Size.x*2);
        float fMaxDensity = GetMaxDensity( f4UV01, f2LODs );

        boolean bIsValid = true;
        if( fMaxDensity < 1e-5 )
            bIsValid = false;

        float fDensity = 0;
        float fMorphFadeout = 1;
        if( bIsValid )
        {
            fDensity = saturate(GetCloudDensity( f4UV01, f2LODs ));

            // Compute morph weights for outer and inner boundaries
            {
//                float4 f4OuterBoundary = g_RenderAttribs.f3CameraPos.xzxz + float4(-1,-1,+1,+1) * float(uiRingDimension/2) * fRingWorldStep;
                Vector4f f4OuterBoundary = new Vector4f();
                f4OuterBoundary.x = g_RenderAttribs.f3CameraPos.x + (-1) * (uiRingDimension/2) * fRingWorldStep;
                f4OuterBoundary.y = g_RenderAttribs.f3CameraPos.z + (-1) * (uiRingDimension/2) * fRingWorldStep;
                f4OuterBoundary.z = g_RenderAttribs.f3CameraPos.x + (+1) * (uiRingDimension/2) * fRingWorldStep;
                f4OuterBoundary.w = g_RenderAttribs.f3CameraPos.z + (+1) * (uiRingDimension/2) * fRingWorldStep;

                //f4OuterBoundary.x                                                  f4OuterBoundary.z
                //      |                                                               |
                //      |       uiRingDimension/2              uiRingDimension/2        |
                //      |<----------------------------->C<----------------------------->|
                //                               |       |
                //   |   X   |   X   |   X   |   X   |   X   |   X   |   X   |   X   |

//                float4 f4DistToOuterBnd = float4(1,1,-1,-1)*(f3CellCenter.xzxz  - f4OuterBoundary.xyzw);
                Vector4f f4DistToOuterBnd = new Vector4f();
                f4DistToOuterBnd.x = 1 * (f3CellCenter.x - f4OuterBoundary.x);
                f4DistToOuterBnd.y = 1 * (f3CellCenter.z - f4OuterBoundary.y);
                f4DistToOuterBnd.z = -1 * (f3CellCenter.x - f4OuterBoundary.z);
                f4DistToOuterBnd.w = -1 * (f3CellCenter.z - f4OuterBoundary.w);

                float fMinDist = Math.min(f4DistToOuterBnd.x, f4DistToOuterBnd.y);
                fMinDist = Math.min(fMinDist, f4DistToOuterBnd.z);
                fMinDist = Math.min(fMinDist, f4DistToOuterBnd.w);
                float fOuterMorphRange = g_GlobalCloudAttribs.uiRingExtension * fRingWorldStep;
                float fOuterMorphWeight = saturate( fMinDist / fOuterMorphRange);
                fMorphFadeout *= fOuterMorphWeight;
            }

            if(uiRing > 0)
            {
//                float4 f4InnerBoundary = g_RenderAttribs.f3CameraPos.xzxz + float4(-1,-1,+1,+1) * float(g_GlobalCloudAttribs.uiInnerRingDim/4 + g_GlobalCloudAttribs.uiRingExtension/2) * fRingWorldStep;
                Vector4f f4InnerBoundary = new Vector4f();
                f4InnerBoundary.x = g_RenderAttribs.f3CameraPos.x + (-1) * (g_GlobalCloudAttribs.uiInnerRingDim/4 + g_GlobalCloudAttribs.uiRingExtension/2) * fRingWorldStep;
                f4InnerBoundary.y = g_RenderAttribs.f3CameraPos.z + (-1) * (g_GlobalCloudAttribs.uiInnerRingDim/4 + g_GlobalCloudAttribs.uiRingExtension/2) * fRingWorldStep;
                f4InnerBoundary.z = g_RenderAttribs.f3CameraPos.x + (+1) * (g_GlobalCloudAttribs.uiInnerRingDim/4 + g_GlobalCloudAttribs.uiRingExtension/2) * fRingWorldStep;
                f4InnerBoundary.w = g_RenderAttribs.f3CameraPos.z + (+1) * (g_GlobalCloudAttribs.uiInnerRingDim/4 + g_GlobalCloudAttribs.uiRingExtension/2) * fRingWorldStep;

                //               f4InnerBoundary.x                f4InnerBoundary.z
                //                        |                             |
                //                        |                             |
                //                        |<----------->C<------------->|
                //                               |       |
                //   |   X   |   X   |   X   |   X   |   X   |   X   |   X   |   X   |

//                float4 f4DistToInnerBnd = float4(1,1,-1,-1)*(f3CellCenter.xzxz - f4InnerBoundary.xyzw);
                Vector4f f4DistToInnerBnd = new Vector4f();
                f4DistToInnerBnd.x = (+1) * (f3CellCenter.x - f4InnerBoundary.x);
                f4DistToInnerBnd.y = (+1) * (f3CellCenter.z - f4InnerBoundary.y);
                f4DistToInnerBnd.z = (-1) * (f3CellCenter.x - f4InnerBoundary.z);
                f4DistToInnerBnd.w = (-1) * (f3CellCenter.z - f4InnerBoundary.w);

                float fMinDist = Math.min(f4DistToInnerBnd.x, f4DistToInnerBnd.y);
                fMinDist = Math.min(fMinDist, f4DistToInnerBnd.z);
                fMinDist = Math.min(fMinDist, f4DistToInnerBnd.w);
                float fInnerMorphRange = g_GlobalCloudAttribs.uiRingExtension/2 * fRingWorldStep;
                float fInnerMorphWeight = 1-saturate( fMinDist / fInnerMorphRange);
                fMorphFadeout *= fInnerMorphWeight;
            }

            if( fDensity < 1e-5 )
                bIsValid = false;

            // TODO: perform this check for each particle, not cell:
            float fParticleBoundSphereRadius = Vector3f.length(f3Size);
            if( Vector3f.distance(f3CellCenter, g_RenderAttribs.f3CameraPos) > g_GlobalCloudAttribs.fParticleCutOffDist + fParticleBoundSphereRadius )
                bIsValid = false;
        }

        if( bIsValid )
        {
            // If the cell is valid, store the data in the buffer
            g_CloudCellsRW[uiCellId].f3Center.set(f3CellCenter);
            g_CloudCellsRW[uiCellId].fSize = f3Size.x;

            g_CloudCellsRW[uiCellId].f3Normal.set(f3Normal);
            g_CloudCellsRW[uiCellId].f3Tangent.set(f3Tangent);
            g_CloudCellsRW[uiCellId].f3Bitangent.set(f3Bitangent);

            g_CloudCellsRW[uiCellId].uiNumActiveLayers = uiNumActiveLayers;

            g_CloudCellsRW[uiCellId].fDensity = fDensity;
            g_CloudCellsRW[uiCellId].fMorphFadeout = fMorphFadeout;

            g_CloudCellsRW[uiCellId].uiPackedLocation = uiPackedLocation;

            // Append the cell ID to the list
            g_ValidCellsAppendBuf.push(uiCellId);

            // Perform view frustum culling
            boolean bIsVisible = IsParticleVisibile(f3CellCenter, new Vector3f(f3Size.x, g_GlobalCloudAttribs.fCloudThickness/2.f, f3Size.z), g_RenderAttribs.f4ViewFrustumPlanes);
            if( bIsVisible )
            {
                g_VisibleCellsAppendBuf.push(uiCellId);
            }
        }
    }

//    uiCellI, uiCellJ, uiRing, uiLayer;
    void UnPackParticleIJRing(int ID /*,out uint i, out uint j, out uint ring, out uint layer*/)
    {
        uiCellI=(ID)&((1<<12)-1);
        uiCellJ=((ID)>>12)&((1<<12)-1);
        uiRing = ((ID)>>24) & ((1<<4)-1);
        uiLayer = (ID)>>28;
    }

    float GetCloudRingWorldStep(int uiRing/*, SGlobalCloudAttribs g_GlobalCloudAttribs*/)
    {
        final float fLargestRingSize = g_GlobalCloudAttribs.fParticleCutOffDist * 2;
        int uiRingDimension = g_GlobalCloudAttribs.uiRingDimension;
        int uiNumRings = g_GlobalCloudAttribs.uiNumRings;
        float fRingWorldStep = fLargestRingSize /((uiRingDimension) << ((uiNumRings-1) - uiRing));
        return fRingWorldStep;
    }

    int GetNumActiveLayers(int iMaxLayers, int iRing)
    {
        return iMaxLayers;//(iMaxLayers + (1<<iRing)-1) >> iRing;
    }

    float GetParticleSize( float fRingWorldStep)
    {
        return fRingWorldStep;
    }

    Vector3f GetParticleScales(float fSize, float fNumActiveLayers)
    {
        Vector3f f3Scales = new Vector3f(fSize, fSize, fSize);
        //if( fNumActiveLayers > 1 )
        //    f3Scales.y = max(f3Scales.y, g_GlobalCloudAttribs.fCloudThickness/fNumActiveLayers);
        f3Scales.y = Math.min(f3Scales.y, g_GlobalCloudAttribs.fCloudThickness/2.f);
        return f3Scales;
    }

    final static Vector2f g_f2CloudDensitySamplingScale = new Vector2f(1.f / 150000.f, 1.f / 19000.f);
    final static Vector2f CLOUD_DENSITY_TEX_DIM = new Vector2f(1024,1024);

    Vector4f GetCloudDensityUV(Vector3f CloudPosition, float fTime)
    {
//        const float4 f2Offset01 = float4( 0.1*float2(-0.04, +0.01) * fTime, 0.2*float2( 0.01,  0.04) * fTime );
        Vector4f f2Offset01 = new Vector4f(0.1f * -0.04f * fTime, 0.1f * 0.01f * fTime, 0.2f * 0.01f * fTime, 0.2f * 0.04f * fTime);
//        float4 f2UV01 = CloudPosition.xzxz * g_f2CloudDensitySamplingScale.xxyy + f2Offset01;
        Vector4f f2UV01 = new Vector4f();
        f2UV01.x = CloudPosition.x * g_f2CloudDensitySamplingScale.x + f2Offset01.x;
        f2UV01.y = CloudPosition.z * g_f2CloudDensitySamplingScale.x + f2Offset01.y;
        f2UV01.z = CloudPosition.x * g_f2CloudDensitySamplingScale.y + f2Offset01.z;
        f2UV01.w = CloudPosition.z * g_f2CloudDensitySamplingScale.y + f2Offset01.w;
        return f2UV01;
    }

    Vector2f ComputeDensityTexLODsFromStep(float fSamplingStep)
    {
        Vector2f f2dU = new Vector2f();
        f2dU.x = fSamplingStep * g_f2CloudDensitySamplingScale.x * CLOUD_DENSITY_TEX_DIM.x;
        f2dU.y = fSamplingStep * g_f2CloudDensitySamplingScale.y * CLOUD_DENSITY_TEX_DIM.x;

        f2dU.x = Math.max(1, f2dU.x);
        f2dU.y = Math.max(1, f2dU.y);

        f2dU.x = (float) (Math.log(f2dU.x)/Math.log(2));
        f2dU.y = (float) (Math.log(f2dU.y)/Math.log(2));
//        float2 f2LODs = log2(max(f2dU, float2(1)));
        return f2dU;
    }

    float GetMaxDensity(Vector4f f4UV01, Vector2f f2LODs /*= float2(0,0)*/)
    {
//        float fDensity =
////        g_tex2MaxDensityMip.SampleLevel(samPointWrap, f4UV01.xy, f2LODs.x) *
////        g_tex2MaxDensityMip.SampleLevel(samPointWrap, f4UV01.zw, f2LODs.y);
//                textureLod(g_tex2MaxDensityMip, f4UV01.xy, f2LODs.x).x *
//                        textureLod(g_tex2MaxDensityMip, f4UV01.zw, f2LODs.y).x;
//
//        fDensity = saturate((fDensity-g_GlobalCloudAttribs.fCloudDensityThreshold)/(1-g_GlobalCloudAttribs.fCloudDensityThreshold));

        return 0.03f;// fDensity;
    }

    float GetCloudDensity(Vector4f f4UV01, Vector2f f2LODs /*= float2(0,0)*/)
    {
//        float fDensity =
////        g_tex2DCloudDensity.SampleLevel(samLinearWrap, f4UV01.xy, f2LODs.x) *
////        g_tex2DCloudDensity.SampleLevel(samLinearWrap, f4UV01.zw, f2LODs.y);
//                textureLod(g_tex2DCloudDensity, f4UV01.xy, f2LODs.x).x *
//                        textureLod(g_tex2DCloudDensity, f4UV01.zw, f2LODs.y).x;
//
//        fDensity = saturate((fDensity-g_GlobalCloudAttribs.fCloudDensityThreshold)/(1.0-g_GlobalCloudAttribs.fCloudDensityThreshold));

        return 0.01f; //fDensity;
    }

    float saturate(float f){
        return Numeric.clamp(f, 0.0f, 1.0f);
    }

    // This function computes visibility for the particle
    boolean IsParticleVisibile(Vector3f f3Center, Vector3f f3Scales, Vector4f[] f4ViewFrustumPlanes)
    {
        float fParticleBoundSphereRadius = Vector3f.length(f3Scales);
        boolean bIsVisible = true;
        for(int iPlane = 0; iPlane < 6; ++iPlane)
        {
            Vector4f f4CurrPlane = f4ViewFrustumPlanes[iPlane];
            // Note that the plane normal is not normalized to 1
            float DMax = Vector3f.dot(f3Center, f4CurrPlane) + f4CurrPlane.w + fParticleBoundSphereRadius*Vector3f.length(f4CurrPlane);
            if( DMax < 0 )
            {
                bIsVisible = false;
//            return false;
            }
        }
        return bIsVisible;
    }
}
