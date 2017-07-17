package jet.opengl.demos.demos.amdfx.dof;

import org.lwjgl.util.vector.Vector4f;

/**
 * Created by mazhen'gui on 2017/6/29.
 */

final class CameraParameters {
    final Vector4f vecEye = new Vector4f();
    final Vector4f vecAt = new Vector4f();
    float  focalLength;
    float  focalDistance;
    float  sensorWidth;
    float  fStop;
    CameraParameters() {}

    CameraParameters(float eyeX, float eyeY, float eyeZ, float atX, float atY, float atZ, float focalLength, float focalDistance, float sensorWidth, float fStop){
        vecEye.set(eyeX,eyeY,eyeZ);
        vecAt.set(atX,atY,atZ);

        this.focalLength = focalLength;
        this.focalDistance = focalDistance;
        this.sensorWidth = sensorWidth;
        this.fStop = fStop;
    }
}
