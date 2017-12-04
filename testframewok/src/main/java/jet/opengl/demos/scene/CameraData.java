package jet.opengl.demos.scene;

import com.nvidia.developer.opengl.utils.BoundingBox;

import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.ReadableVector3f;
import org.lwjgl.util.vector.Vector3f;
import org.lwjgl.util.vector.Vector4f;

/**
 * Created by mazhen'gui on 2017/10/9.
 */

public class CameraData {
    public final Matrix4f projection = new Matrix4f();
    public final Matrix4f view = new Matrix4f();
    public float fov = 60;
    public float aspect;
    public float near = 0.1f;
    public float far = 100.f;

    public final Vector3f position = new Vector3f();
    public final Vector3f lookAt = new Vector3f();
    public final Vector3f up = new Vector3f();
    public final Vector3f right = new Vector3f();
    public final Matrix4f viewProj = new Matrix4f();

    private final Vector4f[] frustumePlanes = new Vector4f[6];

    public float getFov() { return fov;}
    public float getAspectRatio() { return aspect;}
    public float getNearPlaneDistance() { return near;}
    public float getFarPlaneDistance() { return far;}
    public ReadableVector3f getPosition() { return position;}
    public ReadableVector3f getLookAt() { return lookAt;}
    public ReadableVector3f getUp() { return up;}
    public ReadableVector3f getRight() { return right;}

    public Matrix4f getViewMatrix() { return view;}
    public Matrix4f getViewProjMatrix() { return viewProj;}
    public Matrix4f getProjMatrix() { return projection;}

    public void setProjection(float fov, float aspectRatio, float nearClipDistance, float farClipDistance){
        this.fov = fov;
        this.aspect = aspectRatio;
        this.near = nearClipDistance;
        this.far = farClipDistance;

        // Setup Frustrum
        Matrix4f.perspective(fov, aspectRatio, nearClipDistance, farClipDistance, projection);
    }

    public void setViewAndUpdateCamera(ReadableVector3f position, ReadableVector3f look, ReadableVector3f up){
        Matrix4f.lookAt(position, look, up, view);

        Matrix4f.decompseRigidMatrix(view, this.position, right, this.up, this.lookAt);
        this.lookAt.scale(-1);

        Matrix4f.mul(projection, view, viewProj);
        Matrix4f.extractFrustumPlanes(viewProj, frustumePlanes);
    }

    public void setViewAndUpdateCamera(Matrix4f viewMat){
        view.load(viewMat);
        Matrix4f.decompseRigidMatrix(view, this.position, right, this.up, this.lookAt);
        this.lookAt.scale(-1);

        Matrix4f.mul(projection, view, viewProj);
        Matrix4f.extractFrustumPlanes(viewProj, frustumePlanes);
    }

    public void setPositionAndOrientation(float fov, float aspectRatio, float nearClipDistance, float farClipDistance,
                                          ReadableVector3f position, ReadableVector3f look, ReadableVector3f up){
        setProjection(fov, aspectRatio, nearClipDistance, farClipDistance);
        Matrix4f.lookAt(position, look, up, view);

        Matrix4f.decompseRigidMatrix(view, this.position, right, this.up, this.lookAt);
        this.lookAt.scale(-1);

        Matrix4f.mul(projection, view, viewProj);
        Matrix4f.extractFrustumPlanes(viewProj, frustumePlanes);
    }

    public boolean isCenterExtentVisible(ReadableVector3f center, ReadableVector3f extent){
        // TODO
        return true;
    }

    // TODO need valid
    public boolean isBoxVisible(BoundingBox box){
        // If bounding box is "behind" some plane, then it is invisible
        // Otherwise it is treated as visible
        for(int iViewFrustumPlane = 0; iViewFrustumPlane < 6; iViewFrustumPlane++)
        {
//	        SPlane3D *pCurrPlane = pPlanes + iViewFrustumPlane;
            Vector4f pCurrPlane = frustumePlanes[iViewFrustumPlane];
            Vector4f pCurrNormal = pCurrPlane;

            float MaxPointX = (pCurrNormal.x > 0) ? box.xMax() : box.xMin();
            float MaxPointY = (pCurrNormal.y > 0) ? box.yMax() : box.yMin();
            float MaxPointZ = (pCurrNormal.z > 0) ? box.zMax() : box.zMin();

//	        float DMax = D3DXVec3Dot( &MaxPoint, pCurrNormal ) + pCurrPlane->Distance;
            float DMax = MaxPointX * pCurrNormal.x + MaxPointY * pCurrNormal.y + MaxPointZ * pCurrNormal.z + pCurrPlane.w;

            if( DMax < 0 )
                return false;
        }

        return true;
    }
}
