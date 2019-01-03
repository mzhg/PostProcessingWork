package jet.opengl.demos.gpupro.rvi;

import org.lwjgl.util.vector.ReadableVector3f;
import org.lwjgl.util.vector.ReadableVector4f;
import org.lwjgl.util.vector.Vector3f;

/** Point-light that follows a simple rectangular path on the XZ-plane. */
final class PATH_POINT_LIGHT {
    static final float PATH_POINTLIGHT_MOVE_SPEED = 0.1f;
    POINT_LIGHT pointLight;
    final Vector3f direction = new Vector3f();
    boolean paused;
    final static float[] controlPoints = new float[4];

    boolean Init(ReadableVector3f position, float radius, ReadableVector4f color, float multiplier,ReadableVector3f direction){
        pointLight = DX11_RENDERER.getInstance().CreatePointLight(position,radius,color,multiplier);
        if(pointLight == null)
            return false;
        this.direction.set(direction);
        return true;
    }

    void Update(float frameInterval){
        if((!pointLight.IsActive())||(paused))
            return;

        Vector3f position = pointLight.GetPosition();
        if(position.x > controlPoints[1])
        {
            position.x = controlPoints[1];
            direction.set(0.0f,0.0f,-1.0f);
        }
        if(position.x < controlPoints[0])
        {
            position.x = controlPoints[0];
            direction.set(0.0f,0.0f,1.0f);
        }
        if(position.z > controlPoints[3])
        {
            position.z = controlPoints[3];
            direction.set(1.0f,0.0f,0.0f);
        }
        if(position.z < controlPoints[2])
        {
            position.z = controlPoints[2];
            direction.set(-1.0f,0.0f,0.0f);
        }

        // prevent large values at beginning of application
//        float frameInterval = (float)DEMO::timeManager->GetFrameInterval();
        if(frameInterval>1000.0f)
            return;

//        position += direction*frameInterval*PATH_POINTLIGHT_MOVE_SPEED;
        Vector3f.linear(position, direction, frameInterval*PATH_POINTLIGHT_MOVE_SPEED, position);
        pointLight.SetPosition(position);
    }

    void SetActive(boolean active)
    {
        pointLight.SetActive(active);
    }

    void SetPaused(boolean paused)
    {
        this.paused = paused;
    }

    static void SetControlPoints(float minX,float maxX,float minZ,float maxZ)
    {
        controlPoints[0] = minX;
        controlPoints[1] = maxX;
        controlPoints[2] = minZ;
        controlPoints[3] = maxZ;
    }
}
