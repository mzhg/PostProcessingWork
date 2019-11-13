package jet.opengl.demos.nvidia.waves.crest;

import org.lwjgl.util.vector.ReadableVector4f;
import org.lwjgl.util.vector.Vector3f;

final class Wave_Shading_ShaderData extends Wave_Simulation_ShaderData {

    float _ForceUnderwater;
    ReadableVector4f _InstanceData;
    ReadableVector4f _GeomData;

    final Vector3f _OceanCenterPosWorld = new Vector3f();
}
