package jet.opengl.demos.nvidia.shadows;

import org.lwjgl.util.vector.Matrix4f;

import jet.opengl.postprocessing.shader.GLSLProgram;
import jet.opengl.postprocessing.util.CacheBuffer;

/**
 * Created by mazhen'gui on 2017/11/4.
 */

public abstract class ShadowShadingProgram extends GLSLProgram {

//    uniform int g_shadowTechnique; // 0 = none, 1 = pcss, 2 = pcf
//    uniform int g_samplePattern;   // 0 = POISSON_25_25, 1 = POISSON_32_64, 2 = POISSON_100_100, 3 = POISSON_64_128, 4 = REGULAR_49_225
//    uniform vec2 g_lightRadiusUV;
//    uniform float g_lightZNear;
//    uniform float g_lightZFar;
//    uniform mat4 g_lightView;
//    uniform mat4 g_lightProj;

    private int m_shadowTechnique;
    private int m_samplePattern;
    private int m_lightRadiusUV;
    private int m_lightZNear;
    private int m_lightZFar;
    private int m_lightView;
    private int m_lightProj;

    /**
     * Subclass must call this method to initlize the resources.
     */
    protected final void initShadow(){
        m_shadowTechnique = getUniformLocation("g_shadowTechnique");
        m_samplePattern = getUniformLocation("g_samplePattern");
        m_lightRadiusUV = getUniformLocation("g_lightRadiusUV");
        m_lightZNear = getUniformLocation("g_lightZNear");
        m_lightZFar = getUniformLocation("g_lightZFar");
        m_lightView = getUniformLocation("g_lightView");
        m_lightProj = getUniformLocation("g_lightProj");
    }

    public void setShadowUniforms(ShadowConfig data, ShadowMapParams params){
        setShadowUniforms(data, params, 0.5f);
    }

    public void setShadowUniforms(ShadowConfig data, ShadowMapParams params, float lightRadiusWorld){
        setShadowTechnique(data.shadowMapFiltering != null ? data.shadowMapFiltering.ordinal() : 0);
        setSamplePattern(data.shadowMapPattern != null ? data.shadowMapPattern.ordinal() : 0);
        setLightProj(params.m_LightProj);
        setLightView(params.m_LightView);
        setLightRange(params.m_LightNear, params.m_LightFar);

        // Calculate the LightRadius UV
        float width, height;
        if(params.m_perspective) {
            double r = Math.toRadians(params.m_LightFov / 2);
            double ymax = (params.m_LightFar * Math.tan(r));

            height = (float) (ymax * 2.0);
            width = height * params.m_LightRatio;
        }else{
            width = params.m_lightRight - params.m_lightLeft;
            height = params.m_LightTop - params.m_lightBottom;
        }

        setLightRadiusUV(lightRadiusWorld/ width, lightRadiusWorld/height);
//        setLightRadiusUV(0.5767753f, 0.40444714f);
    }

    private void setShadowTechnique(int technique){
        if(m_shadowTechnique >= 0){
            gl.glUniform1i(m_shadowTechnique, technique);
        }
    }

    private void setSamplePattern(int pattern){
        if(m_samplePattern >= 0){
            gl.glUniform1i(m_samplePattern, pattern);
        }
    }

    private void setLightRadiusUV(float u, float v){
        if(m_lightRadiusUV >= 0){
            gl.glUniform2f(m_lightRadiusUV, u,v);
        }
    }

    private void setLightRange(float near, float far){
        if(m_lightZNear >=0){
            gl.glUniform1f(m_lightZNear, near);
        }

        if(m_lightZFar >=0){
            gl.glUniform1f(m_lightZFar, far);
        }
    }

    private void setLightView(Matrix4f mat){
        if(m_lightView >= 0){
            gl.glUniformMatrix4fv(m_lightView, false, CacheBuffer.wrap(mat));
        }
    }

    private void setLightProj(Matrix4f proj){
        if(m_lightProj >= 0){
            gl.glUniformMatrix4fv(m_lightProj, false, CacheBuffer.wrap(proj));
        }
    }

}
