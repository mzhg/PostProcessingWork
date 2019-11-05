package jet.opengl.demos.nvidia.waves.crest;

import jet.opengl.demos.nvidia.waves.ocean.TechniqueParams;
import jet.opengl.postprocessing.util.CacheBuffer;

final class Wave_Shading_Technique extends Wave_Simulation_Technique {

    private int vpIndex;
    private int geomDataIndex;
    private int instanceDataIndex;

    private int oceanCenterIndex;
    private int worldIndex;

    private boolean mInitlized = false;

    @Override
    public void enable(TechniqueParams params) {
        super.enable(params);

        Wave_Shading_ShaderData shaderData = (Wave_Shading_ShaderData)params;

        if(!mInitlized){
            mInitlized = true;
            vpIndex = gl.glGetUniformLocation(m_program, "UNITY_MATRIX_VP");
            geomDataIndex = gl.glGetUniformLocation(m_program, "_GeomData");
            instanceDataIndex = gl.glGetUniformLocation(m_program, "_InstanceData");

            oceanCenterIndex = gl.glGetUniformLocation(m_program, "_OceanCenterPosWorld");
            worldIndex = gl.glGetUniformLocation(m_program, "unity_ObjectToWorld");
        }

        if(vpIndex >= 0) gl.glUniformMatrix4fv(vpIndex, false, CacheBuffer.wrap(shaderData.UNITY_MATRIX_VP));
        if(worldIndex >= 0) gl.glUniformMatrix4fv(worldIndex, false, CacheBuffer.wrap(shaderData.unity_ObjectToWorld));
        if(geomDataIndex >= 0) gl.glUniform4f(geomDataIndex, shaderData._GeomData.getX(),shaderData._GeomData.getY(),shaderData._GeomData.getZ(),shaderData._GeomData.getW());
        if(instanceDataIndex >= 0) gl.glUniform4f(instanceDataIndex, shaderData._InstanceData.getX(),shaderData._InstanceData.getY(),shaderData._InstanceData.getZ(),shaderData._InstanceData.getW());
        if(oceanCenterIndex >= 0) gl.glUniform3f(oceanCenterIndex, shaderData._OceanCenterPosWorld.x,shaderData._OceanCenterPosWorld.y,shaderData._OceanCenterPosWorld.z);
    }
}
