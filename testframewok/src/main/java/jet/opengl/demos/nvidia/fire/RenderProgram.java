package jet.opengl.demos.nvidia.fire;

import java.io.IOException;

import jet.opengl.postprocessing.shader.GLSLProgram;
import jet.opengl.postprocessing.util.CacheBuffer;

/**
 * Created by mazhen'gui on 2017/9/1.
 */

final class RenderProgram extends GLSLProgram{
    private int g_pmCubeViewMatrixVariable = -1;
    private int g_pmCubeProjMatrixVariable = -1;
    private int g_pmWorldViewProj = -1;
    private int g_pvEyePos = -1;
    private int g_pvLightPos = -1;
    private int g_pfLightIntensity = -1;
    private int g_pfStepSize = -1;
    private int g_pfTime = -1;
    private int g_pfNoiseScale = -1;
    private int g_pfRoughness = -1;
    private int g_pfFrequencyWeights = -1;
    private int g_pbJitter = -1;
    private int g_piCubeMapFace = -1;

    public RenderProgram(String vert, String frag){
        final String path = "nvidia/PerlinFire/shaders/";
        try {
            setSourceFromFiles(path + vert, path + frag);
        } catch (IOException e) {
            e.printStackTrace();
        }

        initUniforms();
    }

    public RenderProgram(String vert){
        final String path = "nvidia/PerlinFire/shaders/";
        try {
            setSourceFromFiles(path + vert, "Scenes/Cube16/shaders/Dummy_PS.frag");
        } catch (IOException e) {
            e.printStackTrace();
        }

        initUniforms();
    }

    private void initUniforms(){
        // Obtain miscellaneous variables
        g_pmCubeViewMatrixVariable = getUniformLocation("CubeViewMatrices");
        g_pmCubeProjMatrixVariable = getUniformLocation("CubeProjectionMatrix");
        g_pmWorldViewProj = getUniformLocation("WorldViewProj");
        g_pvEyePos = getUniformLocation("EyePos");
        g_pvLightPos = getUniformLocation("LightPos");
        g_pfLightIntensity = getUniformLocation( "LightIntensity" );
        g_pfStepSize = getUniformLocation("StepSize");
        g_pfTime = getUniformLocation("Time");
        g_pfNoiseScale = getUniformLocation("NoiseScale");
        g_pfRoughness = getUniformLocation("Roughness");
        g_pfFrequencyWeights = getUniformLocation("FrequencyWeights");
        g_pbJitter = getUniformLocation("bJitter");
        g_piCubeMapFace = getUniformLocation("CubeMapFace");
    }

    public void setUniforms(UniformData data){
        if(g_pmCubeViewMatrixVariable >=0){
            gl.glUniformMatrix4fv(g_pmCubeViewMatrixVariable, false, CacheBuffer.wrap(data.mCubeViewMatrixs));
        }

        if(g_pmCubeProjMatrixVariable>=0){
            gl.glUniformMatrix4fv(g_pmCubeProjMatrixVariable, false, CacheBuffer.wrap(data.mCubeProjMatrix));
        }

        if(g_pmWorldViewProj >=0){
            gl.glUniformMatrix4fv(g_pmWorldViewProj, false, CacheBuffer.wrap(data.mWorldViewProj));
        }

        if(g_pvEyePos >=0){
            gl.glUniform3f(g_pvEyePos, data.vEyePos.x, data.vEyePos.y, data.vEyePos.z);
        }

        if(g_pvLightPos >=0){
            gl.glUniform3f(g_pvLightPos, data.vLightPos.x, data.vLightPos.y, data.vLightPos.z);
        }

        if(g_pfLightIntensity >=0){
            gl.glUniform1f(g_pfLightIntensity, data.fLightIntensity);
        }

        if(g_pfStepSize >=0){
            gl.glUniform1f(g_pfStepSize, data.fStepSize);
        }

        if(g_pfTime >=0){
            gl.glUniform1f(g_pfTime, data.fTime);
        }

        if(g_pfNoiseScale >=0){
            gl.glUniform1f(g_pfNoiseScale, data.fNoiseScale);
        }

        if(g_pfRoughness >=0){
            gl.glUniform1f(g_pfRoughness, data.fRoughness);
        }

        if(g_pfFrequencyWeights >=0){
            gl.glUniform1fv(g_pfFrequencyWeights, CacheBuffer.wrap(data.fFrequencyWeights));
        }

        if(g_pbJitter>=0){
            gl.glUniform1i(g_pbJitter, data.bJitter?1:0);
        }

        if(g_piCubeMapFace>=0){
            gl.glUniform1i(g_piCubeMapFace, data.iCubeMapFace);
        }
    }
}
