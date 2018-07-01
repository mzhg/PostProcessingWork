package nv.samples.smoke;

import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector3f;

/**
 * Created by Administrator on 2018/7/1 0001.
 */

final class VolumeConstants {
    float       RTWidth;
    float       RTHeight;

    final Matrix4f WorldViewProjection = new Matrix4f();
    final Matrix4f    InvWorldViewProjection = new Matrix4f();
    final Matrix4f    WorldView = new Matrix4f();

    final Matrix4f    Grid2World = new Matrix4f();

    float       ZNear = 0.05f;
    float       ZFar = 1000.0f;

    final Vector3f gridDim = new Vector3f();
    final Vector3f recGridDim= new Vector3f();
    float       maxGridDim;
    float       gridScaleFactor = 1.0f;
    final Vector3f      eyeOnGrid = new Vector3f();

    float       edgeThreshold = 0.2f;

    float       tan_FovXhalf;
    float       tan_FovYhalf;

    boolean useGlow               = true;
    float glowContribution     = 0.81f;
    float finalIntensityScale  = 22.0f;
    float finalAlphaScale      = 0.95f;
    float smokeColorMultiplier = 2.0f;
    float smokeAlphaMultiplier = 0.1f;
    float fireAlphaMultiplier  = 0.4f;
    int   rednessFactor        = 5;

    boolean        g_bRaycastBisection      = true; // true: compute more accurate ray-surface intersection; false: use first hit position
    boolean        g_bRaycastFilterTricubic = true; // true: tricubic; false: trilinear
    boolean        g_bRaycastShadeAsWater   = true; // true: shade using reflection+refraction from environment map; false: output gradient

}
