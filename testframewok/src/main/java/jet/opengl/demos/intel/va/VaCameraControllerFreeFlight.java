package jet.opengl.demos.intel.va;

import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector3f;

import jet.opengl.postprocessing.util.CacheBuffer;

/**
 * Created by mazhen'gui on 2017/11/18.
 */

public class VaCameraControllerFreeFlight implements VaCameraControllerBase {
    protected float                   m_yaw;
    protected float                   m_pitch;
    protected float                   m_roll;

    // a reference for yaw pitch roll calculations: default is X is forward, Z is up, Y is right
    protected final Matrix4f          m_baseOrientation = new Matrix4f();

    protected float                   m_accumMouseDeltaX;
    protected float                   m_accumMouseDeltaY;
    protected final Vector3f          m_accumMove = new Vector3f();
    protected float                   m_rotationSpeed;
    protected float                   m_movementSpeed;
    protected float                   m_inputSmoothingLerpK;

    protected float                   m_movementSpeedAccelerationModifier;

    protected boolean                 m_moveWhileNotCaptured;

    @Override
    public void CameraAttached(VaCameraBase camera) {
        /*vaMatrix4x4 debasedOrientation = m_baseOrientation.Inverse() * vaMatrix4x4::FromQuaternion( camera.GetOrientation() );
        debasedOrientation.DecomposeRotationYawPitchRoll( m_yaw, m_pitch, m_roll );*/
        m_roll = 0;

        Matrix4f inverse = CacheBuffer.getCachedMatrix();
        Matrix4f orientation = CacheBuffer.getCachedMatrix();
        Vector3f rotation = CacheBuffer.getCachedVec3();
        try {
            Matrix4f.invert(m_baseOrientation, inverse);
            camera.GetOrientation().toMatrix(orientation);
            Matrix4f debasedOrientation = Matrix4f.mul(orientation, inverse, inverse);
            Matrix4f.decomposeRotationYawPitchRoll(debasedOrientation, rotation);

            m_yaw = rotation.x;
            m_pitch = rotation.y;
            m_roll = rotation.z;
        }finally {
            CacheBuffer.free(inverse);
            CacheBuffer.free(orientation);
            CacheBuffer.free(rotation);
        }
    }

    @Override
    public void CameraTick(float deltaTime, VaCameraBase camera, boolean hasFocus) {
        throw new UnsupportedOperationException();
    }

    public void       SetMoveWhileNotCaptured( boolean moveWhileNotCaptured )    { m_moveWhileNotCaptured = moveWhileNotCaptured; }
    public boolean    GetMoveWhileNotCaptured( )                              { return m_moveWhileNotCaptured; }
}
