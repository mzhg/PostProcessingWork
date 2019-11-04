package jet.opengl.demos.nvidia.waves.crest;

import jet.opengl.demos.nvidia.waves.ocean.Technique;
import jet.opengl.demos.nvidia.waves.ocean.TechniqueParams;

class Wave_Simulation_Technique extends Technique {

    @Override
    public final void enable() {
        throw new UnsupportedOperationException("Don't use this function.");
    }

    @Override
    public void enable(TechniqueParams params) {
        super.enable(params);

        Wave_Simulation_ShaderData shaderData = (Wave_Simulation_ShaderData)params;
        // todo

    }
}
