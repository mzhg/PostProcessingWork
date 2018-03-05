package jet.opengl.demos.nvidia.shadows;

import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.ReadableVector3f;

import java.io.IOException;

import jet.opengl.postprocessing.shader.GLSLProgram;
import jet.opengl.postprocessing.util.CacheBuffer;

/**
 * Created by mazhen'gui on 2017/11/9.
 */

final class VSMSceneRenderProgram extends GLSLProgram {

    private int mWorld;
    private int wLightViewProj;
    private int wLightPos;
    private int wLightDir;
    private int mViewProj;
    private int wLightView;

    private int m_podiumCenterWorldIdx;
    private int m_useTextureIdx;
    private int m_useDiffuseIdx;

    private int bShowVariance;
    private int bShowMD;
    private int bShowCheb;
    private int bVSM;
    private int fFilterWidth;

    VSMSceneRenderProgram(){
        final String path = "nvidia/ShadowWorks/shaders/";
        try {
            setSourceFromFiles(path + "SceneRenderVSM_VS.vert", path + "SceneRenderVSM_PS.frag");
        } catch (IOException e) {
            e.printStackTrace();
        }

        mWorld = getUniformLocation("g_world");
        wLightViewProj = getUniformLocation("gLightViewProj");
        wLightPos = getUniformLocation("gLightPos");
        wLightDir = getUniformLocation("gLightDir");
        mViewProj = getUniformLocation("g_viewProj");

        m_podiumCenterWorldIdx = getUniformLocation("g_podiumCenterWorld");
        m_useTextureIdx = getUniformLocation("g_useTexture");
        m_useDiffuseIdx = getUniformLocation("g_useDiffuse");

        bShowVariance = getUniformLocation("bShowVariance");
        bShowMD = getUniformLocation("bShowMD");
        bShowCheb = getUniformLocation("bShowCheb");
        bVSM = getUniformLocation("bVSM");
        fFilterWidth = getUniformLocation("fFilterWidth");

        wLightView = getUniformLocation("gLightView");
    }

    void setShowVariance(boolean flag) { if(bShowVariance>=0) gl.glUniform1i(bShowVariance, flag?1:0);}
    void setShowMD(boolean flag) { if(bShowMD>=0) gl.glUniform1i(bShowMD, flag?1:0);}
    void setShowCheb(boolean flag) { if(bShowCheb>=0) gl.glUniform1i(bShowCheb, flag?1:0);}
    void setEnableVSM(boolean flag) { if(bVSM>=0) gl.glUniform1i(bVSM, flag?1:0);}
    void setFilterWidth(float filterWidth) { if(fFilterWidth>=0) gl.glUniform1f(fFilterWidth, filterWidth);}
    void setWorld(Matrix4f mat) { if(mWorld >=0) gl.glUniformMatrix4fv(mWorld, false, CacheBuffer.wrap(mat));}
    void setLightViewProj(Matrix4f mat) { if(wLightViewProj >=0) gl.glUniformMatrix4fv(wLightViewProj, false, CacheBuffer.wrap(mat));}
    void setViewProj(Matrix4f mat) { if(mViewProj >=0) gl.glUniformMatrix4fv(mViewProj, false, CacheBuffer.wrap(mat));}
    void setLightPos(ReadableVector3f pos) { if(wLightPos >=0) gl.glUniform3f(wLightPos, pos.getX(), pos.getY(), pos.getZ());}
    void setLightDir(ReadableVector3f pos) { if(wLightDir >=0) gl.glUniform3f(wLightDir, pos.getX(), pos.getY(), pos.getZ());}
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

    void setLightView(Matrix4f mat) { if(wLightView >=0) gl.glUniformMatrix4fv(wLightView, false, CacheBuffer.wrap(mat));}
}
