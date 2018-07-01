package nv.samples.smoke;

import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector3f;

import java.io.IOException;

import jet.opengl.postprocessing.shader.GLSLProgram;
import jet.opengl.postprocessing.util.CacheBuffer;

/**
 * Created by Administrator on 2018/7/1 0001.
 */

final class VolumeRendererProgram extends GLSLProgram{
    private int mTHeightLoc = -1;
    private int m_gridDimLoc = -1;
    private int m_g_bRaycastFilterTricubicLoc = -1;
    private int maxGridDimLoc = -1;
    private int m_rayDataTexSmallLoc = -1;
    private int m_volumeTexLoc = -1;
    private int m_edgeThresholdLoc = -1;
    private int m_recGridDimLoc = -1;
    private int m_useGlowLoc = -1;
    private int mGrid2WorldLoc = -1;
    private int m_eyeOnGridLoc = -1;
    private int m_smokeColorMultiplierLoc = -1;
    private int m_glowTexLoc = -1;
    private int m_rayDataTexLoc = -1;
    private int m_glowContributionLoc = -1;
    private int m_jitterTexLoc = -1;
    private int m_smokeAlphaMultiplierLoc = -1;
    private int m_gridScaleFactorLoc = -1;
    private int m_envMapTexLoc = -1;
    private int mWorldViewLoc = -1;
    private int m_finalIntensityScaleLoc = -1;
    private int m_g_bRaycastBisectionLoc = -1;
    private int m_tan_FovYhalfLoc = -1;
    private int m_finalAlphaScaleLoc = -1;
    private int m_g_bRaycastShadeAsWaterLoc = -1;
    private int mWorldViewProjectionLoc = -1;
    private int m_fireTransferFunctionLoc = -1;
    private int mFarLoc = -1;
    private int mTWidthLoc = -1;
    private int m_rednessFactorLoc = -1;
    private int mInvWorldViewProjectionLoc = -1;
    private int m_tan_FovXhalfLoc = -1;
    private int m_fireAlphaMultiplierLoc = -1;
    private int mNearLoc = -1;
    private int m_rayCastTexLoc = -1;
    private int m_edgeTexLoc = -1;
    private int m_sceneDepthTexLoc = -1;

    private Runnable m_BlendState;
    private Runnable m_RasterizerState;
    private Runnable m_DepthStencilState;

    public void setBlendState(Runnable blendState) { m_BlendState = blendState;}
    public void setRasterizerState(Runnable rasterizerState) { m_RasterizerState = rasterizerState;}
    public void setDepthStencilState(Runnable depthStencilState) {m_DepthStencilState = depthStencilState;}

    @Override
    public void enable() {
        super.enable();

        m_BlendState.run();
        m_RasterizerState.run();
        m_DepthStencilState.run();
    }

    public VolumeRendererProgram(String vert, String frag){
        final String path = "nvidia/Smoke/shaders/";
        try {
            setSourceFromFiles(path + vert, path + frag);
        } catch (IOException e) {
            e.printStackTrace();
        }

        initUniforms();
    }

    private void initUniforms(){
        mTHeightLoc = getUniformLocation( "RTHeight");
        m_gridDimLoc = getUniformLocation( "gridDim");
        m_g_bRaycastFilterTricubicLoc = getUniformLocation( "g_bRaycastFilterTricubic");
        maxGridDimLoc = getUniformLocation( "maxGridDim");
        m_volumeTexLoc = getUniformLocation( "volumeTex");
        m_edgeThresholdLoc = getUniformLocation( "edgeThreshold");
        m_recGridDimLoc = getUniformLocation( "recGridDim");
        m_useGlowLoc = getUniformLocation( "useGlow");
        mGrid2WorldLoc = getUniformLocation( "Grid2World");
        m_eyeOnGridLoc = getUniformLocation( "eyeOnGrid");
        m_smokeColorMultiplierLoc = getUniformLocation( "smokeColorMultiplier");
        m_glowContributionLoc = getUniformLocation( "glowContribution");
        m_smokeAlphaMultiplierLoc = getUniformLocation( "smokeAlphaMultiplier");
        m_gridScaleFactorLoc = getUniformLocation( "gridScaleFactor");
        mWorldViewLoc = getUniformLocation( "WorldView");
        m_finalIntensityScaleLoc = getUniformLocation( "finalIntensityScale");
        m_g_bRaycastBisectionLoc = getUniformLocation( "g_bRaycastBisection");
        m_tan_FovYhalfLoc = getUniformLocation( "tan_FovYhalf");
        m_finalAlphaScaleLoc = getUniformLocation( "finalAlphaScale");
        m_g_bRaycastShadeAsWaterLoc = getUniformLocation( "g_bRaycastShadeAsWater");
        mWorldViewProjectionLoc = getUniformLocation( "WorldViewProjection");
        mFarLoc = getUniformLocation( "ZFar");
        mTWidthLoc = getUniformLocation( "RTWidth");
        m_rednessFactorLoc = getUniformLocation( "rednessFactor");
        mInvWorldViewProjectionLoc = getUniformLocation( "InvWorldViewProjection");
        m_tan_FovXhalfLoc = getUniformLocation( "tan_FovXhalf");
        m_fireAlphaMultiplierLoc = getUniformLocation( "fireAlphaMultiplier");
        mNearLoc = getUniformLocation( "ZNear");
    }

    public void setVolumeConstants(VolumeConstants constants){
        setRTHeight(constants.RTHeight);
        setGridDim(constants.gridDim);
        setRaycastFilterTricubic(constants.g_bRaycastBisection);
        setMaxGridDim(constants.maxGridDim);
        setEdgeThreshold(constants.edgeThreshold);
        setRecGridDim(constants.recGridDim);
        setUseGlow(constants.useGlow);
        setGrid2World(constants.Grid2World);
        setEyeOnGrid(constants.eyeOnGrid);
        setSmokeColorMultiplier(constants.smokeColorMultiplier);
        setGlowContribution(constants.glowContribution);
        setSmokeAlphaMultiplier(constants.smokeAlphaMultiplier);
        setGridScaleFactor(constants.gridScaleFactor);
        setWorldView(constants.WorldView);
        setFinalIntensityScale(constants.finalIntensityScale);
        setRaycastBisection(constants.g_bRaycastBisection);
        setTanFovYhalf(constants.tan_FovYhalf);
        setFinalAlphaScale(constants.finalAlphaScale);
        setRaycastShadeAsWater(constants.g_bRaycastShadeAsWater);
        setWorldViewProjection(constants.WorldViewProjection);
        setFar(constants.ZFar);
        setRTWidth(constants.RTWidth);
        setRednessFactor(constants.rednessFactor);
        setInvWorldViewProjection(constants.InvWorldViewProjection);
        setTanFovXhalf(constants.tan_FovXhalf);
        setFireAlphaMultiplier(constants.fireAlphaMultiplier);
        setNear(constants.ZNear);
    }

    private void setRTHeight(float f) { if(mTHeightLoc >=0)gl.glUniform1f(mTHeightLoc, f);}
    private void setGridDim(Vector3f v) { if(m_gridDimLoc >=0)gl.glUniform3f(m_gridDimLoc, v.x, v.y, v.z);}
    private void setRaycastFilterTricubic(boolean b) { if(m_g_bRaycastFilterTricubicLoc >=0)gl.glUniform1i(m_g_bRaycastFilterTricubicLoc, b ? 1 : 0);}
    private void setMaxGridDim(float f) { if(maxGridDimLoc >=0)gl.glUniform1f(maxGridDimLoc, f);}
    private void setEdgeThreshold(float f) { if(m_edgeThresholdLoc >=0)gl.glUniform1f(m_edgeThresholdLoc, f);}
    private void setRecGridDim(Vector3f v) { if(m_recGridDimLoc >=0)gl.glUniform3f(m_recGridDimLoc, v.x, v.y, v.z);}
    private void setUseGlow(boolean b) { if(m_useGlowLoc >=0)gl.glUniform1i(m_useGlowLoc, b ? 1 : 0);}
    private void setGrid2World(Matrix4f mat) { if(mGrid2WorldLoc >=0)gl.glUniformMatrix4fv(mGrid2WorldLoc, false, CacheBuffer.wrap(mat));}
    private void setEyeOnGrid(Vector3f v) { if(m_eyeOnGridLoc >=0)gl.glUniform3f(m_eyeOnGridLoc, v.x, v.y, v.z);}
    private void setSmokeColorMultiplier(float f) { if(m_smokeColorMultiplierLoc >=0)gl.glUniform1f(m_smokeColorMultiplierLoc, f);}
    private void setGlowContribution(float f) { if(m_glowContributionLoc >=0)gl.glUniform1f(m_glowContributionLoc, f);}
    private void setSmokeAlphaMultiplier(float f) { if(m_smokeAlphaMultiplierLoc >=0)gl.glUniform1f(m_smokeAlphaMultiplierLoc, f);}
    private void setGridScaleFactor(float f) { if(m_gridScaleFactorLoc >=0)gl.glUniform1f(m_gridScaleFactorLoc, f);}
    private void setWorldView(Matrix4f mat) { if(mWorldViewLoc >=0)gl.glUniformMatrix4fv(mWorldViewLoc, false, CacheBuffer.wrap(mat));}
    private void setFinalIntensityScale(float f) { if(m_finalIntensityScaleLoc >=0)gl.glUniform1f(m_finalIntensityScaleLoc, f);}
    private void setRaycastBisection(boolean b) { if(m_g_bRaycastBisectionLoc >=0)gl.glUniform1i(m_g_bRaycastBisectionLoc, b ? 1 : 0);}
    private void setTanFovYhalf(float f) { if(m_tan_FovYhalfLoc >=0)gl.glUniform1f(m_tan_FovYhalfLoc, f);}
    private void setFinalAlphaScale(float f) { if(m_finalAlphaScaleLoc >=0)gl.glUniform1f(m_finalAlphaScaleLoc, f);}
    private void setRaycastShadeAsWater(boolean b) { if(m_g_bRaycastShadeAsWaterLoc >=0)gl.glUniform1i(m_g_bRaycastShadeAsWaterLoc, b ? 1 : 0);}
    private void setWorldViewProjection(Matrix4f mat) { if(mWorldViewProjectionLoc >=0)gl.glUniformMatrix4fv(mWorldViewProjectionLoc, false, CacheBuffer.wrap(mat));}
    private void setFar(float f) { if(mFarLoc >=0)gl.glUniform1f(mFarLoc, f);}
    private void setRTWidth(float f) { if(mTWidthLoc >=0)gl.glUniform1f(mTWidthLoc, f);}
    private void setRednessFactor(int i) { if(m_rednessFactorLoc >=0)gl.glUniform1i(m_rednessFactorLoc, i);}
    private void setInvWorldViewProjection(Matrix4f mat) { if(mInvWorldViewProjectionLoc >=0)gl.glUniformMatrix4fv(mInvWorldViewProjectionLoc, false, CacheBuffer.wrap(mat));}
    private void setTanFovXhalf(float f) { if(m_tan_FovXhalfLoc >=0)gl.glUniform1f(m_tan_FovXhalfLoc, f);}
    private void setFireAlphaMultiplier(float f) { if(m_fireAlphaMultiplierLoc >=0)gl.glUniform1f(m_fireAlphaMultiplierLoc, f);}
    private void setNear(float f) { if(mNearLoc >=0)gl.glUniform1f(mNearLoc, f);}
}
