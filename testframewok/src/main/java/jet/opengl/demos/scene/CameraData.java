package jet.opengl.demos.scene;

import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.ReadableVector3f;
import org.lwjgl.util.vector.Vector3f;

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
    }

    public void setViewAndUpdateCamera(Matrix4f viewMat){
        view.load(viewMat);
        Matrix4f.decompseRigidMatrix(view, this.position, right, this.up, this.lookAt);
        this.lookAt.scale(-1);

        Matrix4f.mul(projection, view, viewProj);
    }

    public void setPositionAndOrientation(float fov, float aspectRatio, float nearClipDistance, float farClipDistance,
                                          ReadableVector3f position, ReadableVector3f look, ReadableVector3f up){
        setProjection(fov, aspectRatio, nearClipDistance, farClipDistance);
        Matrix4f.lookAt(position, look, up, view);

        Matrix4f.decompseRigidMatrix(view, this.position, right, this.up, this.lookAt);
        this.lookAt.scale(-1);

        Matrix4f.mul(projection, view, viewProj);
    }
}
