package jet.opengl.demos.intel.avsm;

import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.ReadableVector3f;
import org.lwjgl.util.vector.Vector3f;

/**
 * Created by mazhen'gui on 2017/9/29.
 */
final class CPUTCamera {
    private float fov;
    private float aspect;
    private float near;
    private float far;

    private final Vector3f position = new Vector3f();
    private final Vector3f lookAt = new Vector3f();
    private final Vector3f up = new Vector3f();
    private final Vector3f right = new Vector3f();

    private final Matrix4f world = new Matrix4f();
    private final Matrix4f projection = new Matrix4f();
    private final Matrix4f worldProj = new Matrix4f();

    public float GetFov() { return fov;}
    public float GetAspectRatio() { return aspect;}
    public float GetNearPlaneDistance() { return near;}
    public float GetFarPlaneDistance() { return far;}
    public ReadableVector3f GetPosition() { return position;}
    public ReadableVector3f GetLook() { return lookAt;}
    public ReadableVector3f GetUp() { return up;}
    public ReadableVector3f GetRight() { return right;}
    public Matrix4f GetProjectionMatrix() { return projection;}
    public Matrix4f GetViewMatrix() { return world;}


    public Matrix4f GetWorldMatrix() { return world;}
    public void SetPositionAndOrientation(float fov, float aspectRatio, float nearClipDistance, float farClipDistance,
                                          ReadableVector3f position, ReadableVector3f look, ReadableVector3f up){
        this.fov = fov;
        this.aspect = aspectRatio;
        this.near = nearClipDistance;
        this.far = farClipDistance;

        // Setup Frustrum
        Matrix4f.perspective(fov, aspectRatio, nearClipDistance, farClipDistance, projection);
        Matrix4f.lookAt(position, look, up, world);

        Matrix4f.decompseRigidMatrix(world, this.position, right, this.up, this.lookAt);
        this.lookAt.scale(-1);

        Matrix4f.mul(projection, world, worldProj);
    }
}
