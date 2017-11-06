package jet.opengl.demos.nvidia.shadows;

import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.ReadableVector3f;

import java.io.IOException;

import jet.opengl.postprocessing.util.CacheBuffer;

/**
 * Created by mazhen'gui on 2017/11/6.
 */

final class SoftShadowSceneRenderProgram extends ShadowShadingProgram{
    private int m_worldIdx;
    private int m_viewProjIdx;
    private int m_lightPosIdx;
    private int m_podiumCenterWorldIdx;
    private int m_useTextureIdx;
    private int m_useDiffuseIdx;

    SoftShadowSceneRenderProgram(){
        try {
            setSourceFromFiles("nvidia/ShadowWorks/shaders/eyerender.vert", "nvidia/ShadowWorks/shaders/sceneShading.frag");
        } catch (IOException e) {
            e.printStackTrace();
        }

        initShadow();

//        uniform mat4 g_world;
//        uniform mat4 g_viewProj;
//        uniform mat4 g_lightViewProjClip2Tex;
//        uniform vec3 g_lightPos;
//        uniform vec3 g_podiumCenterWorld;
//        uniform int g_useTexture;
//        uniform bool g_useDiffuse;

        m_worldIdx = getUniformLocation("g_world");
        m_viewProjIdx = getUniformLocation("g_viewProj");
        m_lightPosIdx = getUniformLocation("g_lightPos");
        m_podiumCenterWorldIdx = getUniformLocation("g_podiumCenterWorld");
        m_useTextureIdx = getUniformLocation("g_useTexture");
        m_useDiffuseIdx = getUniformLocation("g_useDiffuse");
    }

    void setWorld(Matrix4f mat){
        if(m_worldIdx >= 0)
            gl.glUniformMatrix4fv(m_worldIdx, false, CacheBuffer.wrap(mat));
    }

    void setViewProj(Matrix4f mat){
        if(m_viewProjIdx >= 0)
            gl.glUniformMatrix4fv(m_viewProjIdx, false, CacheBuffer.wrap(mat));
    }

    void setLightPos(ReadableVector3f pos){
        if(m_lightPosIdx >= 0)
            gl.glUniform3f(m_lightPosIdx, pos.getX(), pos.getY(), pos.getZ());
    }

    void setPodiumCenterWorldPos(ReadableVector3f pos){
        if(m_podiumCenterWorldIdx >= 0)
            gl.glUniform3f(m_podiumCenterWorldIdx, pos.getX(), pos.getY(), pos.getZ());
    }

    void setUseTexture(int  id){
        if(m_useTextureIdx >= 0)
            gl.glUniform1i(m_useTextureIdx, id);
    }

    void setUseDiffuseTex(boolean flag){
        if(m_useDiffuseIdx >= 0)
            gl.glUniform1i(m_useDiffuseIdx, flag?1:0);
    }
}
