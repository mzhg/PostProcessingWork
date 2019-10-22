package jet.opengl.demos.nvidia.waves.ocean;

import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.ReadableVector2f;
import org.lwjgl.util.vector.Vector2f;

final class OceanVesselDynamicState {
    OceanVesselDynamicState(){
        m_bFirstUpdate = true;
    }

    void setPosition(ReadableVector2f pos){
        m_Position.set(pos);
        ResetDynamicState();
    }
    void setHeading(ReadableVector2f heading, float speed){
        m_NominalHeading.set(heading);
        m_Speed = speed;
        m_NominalHeading.normalise();
        ResetDynamicState();
    }

    final Vector2f m_Position = new Vector2f();
    final Vector2f m_NominalHeading = new Vector2f();
    float m_Speed;

    float m_Pitch;
    float m_PitchRate;
    float m_Roll;
    float m_RollRate;
    float m_Yaw;
    float m_YawRate;
    float m_PrevSeaYaw;
    float m_Height;
    float m_HeightRate;

    int m_DynamicsCountdown;

    boolean m_bFirstUpdate;

    final Matrix4f m_CameraToWorld = new Matrix4f();
    final Matrix4f m_LocalToWorld = new Matrix4f();
    final Matrix4f m_WakeToWorld = new Matrix4f();

    private void ResetDynamicState(){
        m_Pitch = 0.f;
        m_PitchRate = 0.f;
        m_Roll = 0.f;
        m_RollRate = 0.f;
        m_Yaw = 0.f;
        m_YawRate = 0.f;
        m_PrevSeaYaw = 0.f;
        m_Height = 0.f;
        m_HeightRate = 0.f;
        m_DynamicsCountdown = 3;
        m_bFirstUpdate = true;
    }
}
