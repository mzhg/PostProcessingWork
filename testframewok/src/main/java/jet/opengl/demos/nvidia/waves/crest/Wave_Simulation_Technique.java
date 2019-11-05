package jet.opengl.demos.nvidia.waves.crest;

import jet.opengl.demos.nvidia.waves.ocean.Technique;
import jet.opengl.demos.nvidia.waves.ocean.TechniqueParams;
import jet.opengl.postprocessing.util.CacheBuffer;

class Wave_Simulation_Technique extends Technique {

    private int ldParamsIndex;
    private int ldPosScaleIndex;
    private int ldSliceIndex;

    private boolean mInitlized;

    @Override
    public final void enable() {
        throw new UnsupportedOperationException("Don't use this function.");
    }

    @Override
    public void enable(TechniqueParams params) {
        super.enable(params);

        Wave_Simulation_ShaderData shaderData = (Wave_Simulation_ShaderData)params;
        // todo
        if(!mInitlized){
            mInitlized = true;

            ldParamsIndex = gl.glGetUniformLocation(m_program, "_LD_Params");
            ldPosScaleIndex = gl.glGetUniformLocation(m_program, "_LD_Pos_Scale");
            ldSliceIndex = gl.glGetUniformLocation(m_program, "_LD_SliceIndex");
        }

        if(ldParamsIndex >= 0) gl.glUniform4fv(ldParamsIndex, CacheBuffer.wrap(shaderData._LD_Params));
        if(ldPosScaleIndex >= 0) gl.glUniform4fv(ldPosScaleIndex, CacheBuffer.wrap(shaderData._LD_Pos_Scale));
        if(ldSliceIndex >= 0) gl.glUniform1i(ldSliceIndex, shaderData._LD_SliceIndex);

        if(shaderData._LD_TexArray_AnimatedWaves != null){
            gl.glBindTextureUnit(0, shaderData._LD_TexArray_AnimatedWaves.getTexture());
        }else{
            gl.glBindTextureUnit(0, 0);
        }
    }
}
