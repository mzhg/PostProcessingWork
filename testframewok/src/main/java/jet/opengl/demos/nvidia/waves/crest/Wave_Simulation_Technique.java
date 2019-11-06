package jet.opengl.demos.nvidia.waves.crest;

import jet.opengl.demos.nvidia.waves.ocean.Technique;
import jet.opengl.demos.nvidia.waves.ocean.TechniqueParams;
import jet.opengl.postprocessing.common.GLenum;
import jet.opengl.postprocessing.texture.SamplerUtils;
import jet.opengl.postprocessing.texture.TextureGL;
import jet.opengl.postprocessing.util.CacheBuffer;

class Wave_Simulation_Technique extends Technique {

    private int ldParamsIndex;
    private int ldPosScaleIndex;
    private int ldSliceIndex;

    private int amplitudesIndex;
    private int attenuationInShallowsIndex;
    private int chopAmpsIndex;
    private int numWaveVecsIndex;
    private int phasesIndex;
    private int targetPointIndex;
    private int twoPiWaveLenthIndex;
    private int waveDirXIndex;
    private int waveDirYIndex;
    private int weightIndex;

    private boolean mInitlized;

    private static int g_DefualtSampler;

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

            if(g_DefualtSampler == 0)
                g_DefualtSampler = SamplerUtils.getDefaultSampler();

            ldParamsIndex = gl.glGetUniformLocation(m_program, "_LD_Params");
            ldPosScaleIndex = gl.glGetUniformLocation(m_program, "_LD_Pos_Scale");
            ldSliceIndex = gl.glGetUniformLocation(m_program, "_LD_SliceIndex");

            amplitudesIndex = gl.glGetUniformLocation(m_program, "_Amplitudes");
            attenuationInShallowsIndex = gl.glGetUniformLocation(m_program, "_AttenuationInShallows");
            chopAmpsIndex = gl.glGetUniformLocation(m_program, "_ChopAmps");
            numWaveVecsIndex = gl.glGetUniformLocation(m_program, "_NumWaveVecs");
            phasesIndex = gl.glGetUniformLocation(m_program, "_Phases");
            targetPointIndex = gl.glGetUniformLocation(m_program, "_TargetPointData");
            twoPiWaveLenthIndex = gl.glGetUniformLocation(m_program, "_TwoPiOverWavelengths");
            waveDirXIndex = gl.glGetUniformLocation(m_program, "_WaveDirX");
            waveDirYIndex = gl.glGetUniformLocation(m_program, "_WaveDirZ");
            weightIndex = gl.glGetUniformLocation(m_program, "_Weight");
        }

        if(amplitudesIndex >=0) gl.glUniform4fv(amplitudesIndex, CacheBuffer.wrapNotNull(shaderData._Amplitudes));
        if(attenuationInShallowsIndex >=0) gl.glUniform1f(attenuationInShallowsIndex, shaderData._AttenuationInShallows);
        if(chopAmpsIndex >= 0) gl.glUniform4fv(chopAmpsIndex, CacheBuffer.wrapNotNull(shaderData._ChopAmps));
        if(ldParamsIndex >= 0) gl.glUniform4fv(ldParamsIndex, CacheBuffer.wrapNotNull(shaderData._LD_Params));
        if(ldPosScaleIndex >= 0) gl.glUniform4fv(ldPosScaleIndex, CacheBuffer.wrapNotNull(shaderData._LD_Pos_Scale));
        if(ldSliceIndex >= 0) gl.glUniform1i(ldSliceIndex, shaderData._LD_SliceIndex);
        if(numWaveVecsIndex >= 0) gl.glUniform1ui(numWaveVecsIndex, shaderData._NumWaveVecs);
        if(phasesIndex >= 0) gl.glUniform4fv(phasesIndex, CacheBuffer.wrapNotNull(shaderData._Phases));
        if(targetPointIndex >= 0) gl.glUniform4fv(targetPointIndex, CacheBuffer.wrap(shaderData._TargetPointData));
        if(twoPiWaveLenthIndex >= 0) gl.glUniform4fv(twoPiWaveLenthIndex, CacheBuffer.wrapNotNull(shaderData._TwoPiOverWavelengths));
        if(waveDirXIndex >= 0) gl.glUniform4fv(waveDirXIndex, CacheBuffer.wrapNotNull(shaderData._WaveDirX));
        if(waveDirYIndex >= 0) gl.glUniform4fv(waveDirYIndex, CacheBuffer.wrapNotNull(shaderData._WaveDirZ));
        if(weightIndex >=0) gl.glUniform1f(weightIndex, shaderData._Weight);

        bindTexture(0, shaderData._LD_TexArray_AnimatedWaves);
        bindTexture(1, shaderData._LD_TexArray_WaveBuffer);
        bindTexture(2, shaderData._LD_TexArray_SeaFloorDepth);

        bindImage(0, shaderData._LD_TexArray_AnimatedWaves_Compute, true);
    }

    private void bindTexture(int unit, TextureGL texture){
        if(texture != null){
            gl.glBindTextureUnit(unit, texture.getTexture());
            gl.glBindSampler(unit, g_DefualtSampler);
        }else{
            gl.glBindTextureUnit(unit, 0);
        }
    }

    private void bindImage(int unit, TextureGL texture, boolean read){
        if(texture != null){
            gl.glBindImageTexture(unit, texture.getTexture(), 0, true, 0, read? GLenum.GL_READ_WRITE:GLenum.GL_WRITE_ONLY, texture.getFormat());
        }else{
            gl.glBindImageTexture(unit, 0, 0, true, 0, read? GLenum.GL_READ_WRITE:GLenum.GL_WRITE_ONLY, GLenum.GL_RGBA8);
        }
    }
}
