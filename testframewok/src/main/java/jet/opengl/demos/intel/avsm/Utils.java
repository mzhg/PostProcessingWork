package jet.opengl.demos.intel.avsm;

import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector3f;

/**
 * Created by mazhen'gui on 2017/10/9.
 */

final class Utils {

    static void ComputeFrustumExtents(Matrix4f cameraViewInv,
                                      Matrix4f cameraProj,
                                      float nearZ, float farZ,
                                      Matrix4f lightViewProj,
                                      Vector3f outMin,
                                      Vector3f outMax)
    {
        // Extract frustum points
        float scaleXInv = 1.0f / cameraProj.m00;
        float scaleYInv = 1.0f / cameraProj.m11;

        // Transform frustum corners into light view space
//        D3DXMATRIXA16 cameraViewToLightProj = cameraViewInv * lightViewProj;
        Matrix4f cameraViewToLightProj = Matrix4f.mul(lightViewProj, cameraViewInv, null);

        Vector3f[] corners = new Vector3f[8];
        // Near corners (in view space)
        float nearX = scaleXInv * nearZ;
        float nearY = scaleYInv * nearZ;
        corners[0] = new Vector3f(-nearX,  nearY, -nearZ);
        corners[1] = new Vector3f( nearX,  nearY, -nearZ);
        corners[2] = new Vector3f(-nearX, -nearY, -nearZ);
        corners[3] = new Vector3f( nearX, -nearY, -nearZ);
        // Far corners (in view space)
        float farX = scaleXInv * farZ;
        float farY = scaleYInv * farZ;
        corners[4] = new Vector3f(-farX,  farY, -farZ);
        corners[5] = new Vector3f( farX,  farY, -farZ);
        corners[6] = new Vector3f(-farX, -farY, -farZ);
        corners[7] = new Vector3f( farX, -farY, -farZ);

        Vector3f[] cornersLightView = corners;
//        D3DXVec3TransformArray(cornersLightView, sizeof(D3DXVECTOR4),
//                corners, sizeof(D3DXVECTOR3),
//                &cameraViewToLightProj, 8);
        for(int i = 0; i < corners.length; i++){
            Matrix4f.transformCoord(cameraViewToLightProj, corners[i], cornersLightView[i]);
        }

        // NOTE: we don't do a perspective divide here since we actually rely on it being an ortho
        // projection in several places, including not doing any proper near plane clipping here.

        // Work out AABB of frustum in light view space
        /*D3DXVECTOR4 minCorner(cornersLightView[0]);
        D3DXVECTOR4 maxCorner(cornersLightView[0]);
        for (unsigned int i = 1; i < 8; ++i) {
        D3DXVec4Minimize(&minCorner, &minCorner, &cornersLightView[i]);
        D3DXVec4Maximize(&maxCorner, &maxCorner, &cornersLightView[i]);
    }
        outMin->x = minCorner.x;
        outMin->y = minCorner.y;
        outMin->z = minCorner.z;
        outMax->x = maxCorner.x;
        outMax->y = maxCorner.y;
        outMax->z = maxCorner.z;*/

        outMin.set(cornersLightView[0]);
        outMax.set(cornersLightView[0]);

        for(int i = 1; i < 8; i++){
            Vector3f.min(outMin, cornersLightView[i], outMin);
            Vector3f.max(outMax, cornersLightView[i], outMax);
        }
    }

    static void TransformBBox(Vector3f min,
                              Vector3f max, Matrix4f m)
    {
        Vector3f minCorner = min;
        Vector3f maxCorner = max;
        Vector3f[] corners = new Vector3f[8];
        // Bottom corners
        corners[0] = new Vector3f(minCorner.x, minCorner.y, minCorner.z);
        corners[1] = new Vector3f(maxCorner.x, minCorner.y, minCorner.z);
        corners[2] = new Vector3f(maxCorner.x, minCorner.y, maxCorner.z);
        corners[3] = new Vector3f(minCorner.x, minCorner.y, maxCorner.z);
        // Top corners
        corners[4] = new Vector3f(minCorner.x, maxCorner.y, minCorner.z);
        corners[5] = new Vector3f(maxCorner.x, maxCorner.y, minCorner.z);
        corners[6] = new Vector3f(maxCorner.x, maxCorner.y, maxCorner.z);
        corners[7] = new Vector3f(minCorner.x, maxCorner.y, maxCorner.z);

        Vector3f[] newCorners = corners;
        for (int i = 0; i < 8; i++) {
//            D3DXVec3TransformCoord(&newCorners[i], &corners[i], &m);
            Matrix4f.transformCoord(m, corners[i], newCorners[i]);
        }

        Vector3f newMin = min;
        Vector3f newMax = max;

        newMin.set(Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE);
        newMax.set(-Float.MAX_VALUE, -Float.MAX_VALUE, -Float.MAX_VALUE);

        // Initialize
//        for (int i = 0; i < 3; ++i) {
//            newMin[i] = std::numeric_limits<float>::max();
//            newMax[i] = std::numeric_limits<float>::min();
//        }

        // Find new min and max corners
        for (int i = 0; i < 8; i++) {
//            D3DXVec3Minimize(&newMin, &newMin, &newCorners[i]);
//            D3DXVec3Maximize(&newMax, &newMax, &newCorners[i]);
            Vector3f.min(newMin, newCorners[i], newMin);
            Vector3f.max(newMax, newCorners[i], newMax);
        }

//        min = newMin;
//        max = newMax;
    }
}
