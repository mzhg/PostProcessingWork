package jet.opengl.demos.intel.cloud;

import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector3f;
import org.lwjgl.util.vector.Vector4f;

import jet.opengl.postprocessing.texture.Texture2D;

/**
 * Created by mazhen'gui on 2017/7/7.
 */

final class SRenderAttribs {
    Matrix4f ViewProjMatr;
    final Matrix4f viewProjInv = new Matrix4f();
    int pcbCameraAttribs;
    int pcbLightAttribs;
    int pcMediaScatteringParams;
    Texture2D pPrecomputedNetDensitySRV;
    Texture2D pAmbientSkylightSRV;
    Texture2D pDepthBufferSRV;
    Texture2D pShadowMapDSV;
    Texture2D pLiSpCloudTransparencySRV;
    Texture2D pLiSpCloudMinMaxDepthSRV;
    final Vector3f f3CameraPos = new Vector3f();
    final Vector3f f3ViewDir = new Vector3f();
    Vector4f[] f4ViewFrustumPlanes;
    Vector3f  f4DirOnLight;
    int iCascadeIndex;
    float fCurrTime;
    int uiLiSpCloudDensityDim;
    int bLightSpacePass;
//    const SCameraAttribs *m_pCameraAttribs;
//    const SShadowMapAttribs *m_pSMAttribs;
}
