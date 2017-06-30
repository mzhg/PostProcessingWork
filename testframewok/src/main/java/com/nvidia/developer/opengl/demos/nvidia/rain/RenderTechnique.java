package com.nvidia.developer.opengl.demos.nvidia.rain;

import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector3f;
import org.lwjgl.util.vector.Vector4f;

import java.io.IOException;

import jet.opengl.postprocessing.shader.GLSLProgram;
import jet.opengl.postprocessing.shader.ShaderLoader;
import jet.opengl.postprocessing.shader.ShaderSourceItem;
import jet.opengl.postprocessing.shader.ShaderType;
import jet.opengl.postprocessing.util.CacheBuffer;

/**
 * Created by mazhen'gui on 2017/6/30.
 */

final class RenderTechnique extends GLSLProgram{
    private int m_eyePosLoc = -1;
    private int mNearLoc = -1;
    private int m_veParticlesLoc = -1;
    private int mResponseDirLightLoc = -1;
    private int m_splashYDisplaceLoc = -1;
    private int m_cosSpotlightAngleLoc = -1;
    private int mTotalVelLoc = -1;
    private int m_lashBumpTextureLoc = -1;
    private int mKsDirLoc = -1;
    private int mScreenWidthMultiplierLoc = -1;
    private int mVecPointLightEye2Loc = -1;
    private int mBgAirLightLoc = -1;
    private int mVecPointLightEye3Loc = -1;
    private int m_eneTextureDiffuseLoc = -1;
    private int mDSVPointLightLoc = -1;
    private int m_20tableLoc = -1;
    private int m_heightRangeLoc = -1;
    private int m_eneTextureSpecularLoc = -1;
    private int m_lightPosLoc = -1;
    private int mPointLightIntensityLoc = -1;
    private int m_timeCycleLoc = -1;
    private int mResponsePointLightLoc = -1;
    private int mSpriteSizeLoc = -1;
    private int mProjectionLoc = -1;
    private int mInvViewLoc = -1;
    private int mViewSpaceLightVec2Loc = -1;
    private int mWorldViewLoc = -1;
    private int m_lashDiffuseTextureLoc = -1;
    private int mViewProjectionInverseLoc = -1;
    private int mScreenWidthLoc = -1;
    private int m_ngleTextureLoc = -1;
    private int mFrameRateLoc = -1;
    private int mFarLoc = -1;
//    int m_ableLoc = -1;
    private int m_splashXDisplaceLoc = -1;
    private int mVecPointLightEyeLoc = -1;
    private int mScreenHeightLoc = -1;
    private int m_inTextureArrayLoc = -1;
    private int m_specPowerLoc = -1;
    private int m_xHeightLoc = -1;
    private int m_heightMinLoc = -1;
    private int m_radiusMinLoc = -1;
    private int mViewSpaceLightVecLoc = -1;
    private int m_useSpotLightLoc = -1;
    private int m_ableLoc = -1;
    private int mScreenHeightMultiplierLoc = -1;
    private int mKdLoc = -1;
    private int m_betaLoc = -1;
    private int mSpotLightDirLoc = -1;
    private int m_deLoc = -1;
    private int mDiffuseColorLoc = -1;
    private int mDSVPointLight2Loc = -1;
    private int mDSVPointLight3Loc = -1;
    private int m_rLightIntensityLoc = -1;
    private int mKsPointLoc = -1;
    private int m_ckgroundTextureLoc = -1;
    private int mInverseProjectionLoc = -1;
    private int mWorldViewProjLoc = -1;
    private int m_eneTextureNormalLoc = -1;
    private int mWorldLoc = -1;
    private int m_nderBgLoc = -1;
    private int m_radiusRangeLoc = -1;

    RenderTechnique(String vertfile, String gemofile, String fragfile){
        ShaderSourceItem vs_item = new ShaderSourceItem();
        ShaderSourceItem gs_item = null;
        ShaderSourceItem ps_item = new ShaderSourceItem();

        try {
            final String folder = "nvidia/Rain/shaders/";
            vs_item.source = ShaderLoader.loadShaderFile(folder + vertfile, false);
            vs_item.type = ShaderType.VERTEX;

            if(gemofile != null){
                gs_item = new ShaderSourceItem();
                gs_item.source = ShaderLoader.loadShaderFile(folder + gemofile, false);
                gs_item.type = ShaderType.GEOMETRY;
            }

            ps_item.source = ShaderLoader.loadShaderFile(folder + fragfile, false);
            ps_item.type = ShaderType.FRAGMENT;
        } catch (IOException e) {
            e.printStackTrace();
        }

        setSourceFromStrings(vs_item, gs_item, ps_item);
        initUniforms();
    }

    private void initUniforms(){
        m_eyePosLoc = gl.glGetUniformLocation(m_program, "g_eyePos");
        mNearLoc = gl.glGetUniformLocation(m_program, "g_Near");
        m_veParticlesLoc = gl.glGetUniformLocation(m_program, "moveParticles");
        mResponseDirLightLoc = gl.glGetUniformLocation(m_program, "g_ResponseDirLight");
        m_splashYDisplaceLoc = gl.glGetUniformLocation(m_program, "g_splashYDisplace");
        m_cosSpotlightAngleLoc = gl.glGetUniformLocation(m_program, "g_cosSpotlightAngle");
        mTotalVelLoc = gl.glGetUniformLocation(m_program, "g_TotalVel");
        m_lashBumpTextureLoc = gl.glGetUniformLocation(m_program, "SplashBumpTexture");
        mKsDirLoc = gl.glGetUniformLocation(m_program, "g_KsDir");
        mScreenWidthMultiplierLoc = gl.glGetUniformLocation(m_program, "g_ScreenWidthMultiplier");
        mVecPointLightEye2Loc = gl.glGetUniformLocation(m_program, "g_VecPointLightEye2");
        mBgAirLightLoc = gl.glGetUniformLocation(m_program, "g_BgAirLight");
        mVecPointLightEye3Loc = gl.glGetUniformLocation(m_program, "g_VecPointLightEye3");
        m_eneTextureDiffuseLoc = gl.glGetUniformLocation(m_program, "SceneTextureDiffuse");
        mDSVPointLightLoc = gl.glGetUniformLocation(m_program, "g_DSVPointLight");
        m_20tableLoc = gl.glGetUniformLocation(m_program, "G_20table");
        m_heightRangeLoc = gl.glGetUniformLocation(m_program, "g_heightRange");
        m_eneTextureSpecularLoc = gl.glGetUniformLocation(m_program, "SceneTextureSpecular");
        m_lightPosLoc = gl.glGetUniformLocation(m_program, "g_lightPos");
        mPointLightIntensityLoc = gl.glGetUniformLocation(m_program, "g_PointLightIntensity");
        m_timeCycleLoc = gl.glGetUniformLocation(m_program, "g_timeCycle");
        mResponsePointLightLoc = gl.glGetUniformLocation(m_program, "g_ResponsePointLight");
        mSpriteSizeLoc = gl.glGetUniformLocation(m_program, "g_SpriteSize");
        mProjectionLoc = gl.glGetUniformLocation(m_program, "g_mProjection");
        mInvViewLoc = gl.glGetUniformLocation(m_program, "g_mInvView");
        mViewSpaceLightVec2Loc = gl.glGetUniformLocation(m_program, "g_ViewSpaceLightVec2");
        mWorldViewLoc = gl.glGetUniformLocation(m_program, "g_mWorldView");
        m_lashDiffuseTextureLoc = gl.glGetUniformLocation(m_program, "SplashDiffuseTexture");
        mViewProjectionInverseLoc = gl.glGetUniformLocation(m_program, "g_mViewProjectionInverse");
        mScreenWidthLoc = gl.glGetUniformLocation(m_program, "g_ScreenWidth");
        m_ngleTextureLoc = gl.glGetUniformLocation(m_program, "singleTexture");
        mFrameRateLoc = gl.glGetUniformLocation(m_program, "g_FrameRate");
        mFarLoc = gl.glGetUniformLocation(m_program, "g_Far");
        m_ableLoc = gl.glGetUniformLocation(m_program, "Ftable");
        m_splashXDisplaceLoc = gl.glGetUniformLocation(m_program, "g_splashXDisplace");
        mVecPointLightEyeLoc = gl.glGetUniformLocation(m_program, "g_VecPointLightEye");
        mScreenHeightLoc = gl.glGetUniformLocation(m_program, "g_ScreenHeight");
        m_inTextureArrayLoc = gl.glGetUniformLocation(m_program, "rainTextureArray");
        m_specPowerLoc = gl.glGetUniformLocation(m_program, "g_specPower");
        m_xHeightLoc = gl.glGetUniformLocation(m_program, "maxHeight");
        m_heightMinLoc = gl.glGetUniformLocation(m_program, "g_heightMin");
        m_radiusMinLoc = gl.glGetUniformLocation(m_program, "g_radiusMin");
        mViewSpaceLightVecLoc = gl.glGetUniformLocation(m_program, "g_ViewSpaceLightVec");
        m_useSpotLightLoc = gl.glGetUniformLocation(m_program, "g_useSpotLight");
        m_ableLoc = gl.glGetUniformLocation(m_program, "Gtable");
        mScreenHeightMultiplierLoc = gl.glGetUniformLocation(m_program, "g_ScreenHeightMultiplier");
        mKdLoc = gl.glGetUniformLocation(m_program, "g_Kd");
        m_betaLoc = gl.glGetUniformLocation(m_program, "g_beta");
        mSpotLightDirLoc = gl.glGetUniformLocation(m_program, "g_SpotLightDir");
        m_deLoc = gl.glGetUniformLocation(m_program, "g_de");
        mDiffuseColorLoc = gl.glGetUniformLocation(m_program, "g_DiffuseColor");
        mDSVPointLight2Loc = gl.glGetUniformLocation(m_program, "g_DSVPointLight2");
        mDSVPointLight3Loc = gl.glGetUniformLocation(m_program, "g_DSVPointLight3");
        m_rLightIntensityLoc = gl.glGetUniformLocation(m_program, "dirLightIntensity");
        mKsPointLoc = gl.glGetUniformLocation(m_program, "g_KsPoint");
        m_ckgroundTextureLoc = gl.glGetUniformLocation(m_program, "backgroundTexture");
        mInverseProjectionLoc = gl.glGetUniformLocation(m_program, "g_mInverseProjection");
        mWorldViewProjLoc = gl.glGetUniformLocation(m_program, "g_mWorldViewProj");
        m_eneTextureNormalLoc = gl.glGetUniformLocation(m_program, "SceneTextureNormal");
        mWorldLoc = gl.glGetUniformLocation(m_program, "g_mWorld");
        m_nderBgLoc = gl.glGetUniformLocation(m_program, "renderBg");
        m_radiusRangeLoc = gl.glGetUniformLocation(m_program, "g_radiusRange");
    }

    public void setEyePos(Vector3f v) { if(m_eyePosLoc >=0)gl.glUniform3f(m_eyePosLoc, v.x, v.y, v.z);}
    public void setNear(float f) { if(mNearLoc >=0)gl.glUniform1f(mNearLoc, f);}
    public void setVeParticles(boolean b) { if(m_veParticlesLoc >=0)gl.glUniform1i(m_veParticlesLoc, b ? 1 : 0);}
    public void setResponseDirLight(float f) { if(mResponseDirLightLoc >=0)gl.glUniform1f(mResponseDirLightLoc, f);}
    public void setSplashYDisplace(float f) { if(m_splashYDisplaceLoc >=0)gl.glUniform1f(m_splashYDisplaceLoc, f);}
    public void setCosSpotlightAngle(float f) { if(m_cosSpotlightAngleLoc >=0)gl.glUniform1f(m_cosSpotlightAngleLoc, f);}
    public void setTotalVel(Vector3f v) { if(mTotalVelLoc >=0)gl.glUniform3f(mTotalVelLoc, v.x, v.y, v.z);}
//    public void setSashBumpTexture(int texture) {
//        GL13.glActiveTexture(GL13.GL_TEXTURE0 + 9);
//        GL11.glBindTexture(GL11.GL_TEXTURE_2D, texture);
//    }
    public void setKsDir(float f) { if(mKsDirLoc >=0)gl.glUniform1f(mKsDirLoc, f);}
    public void setScreenWidthMultiplier(float f) { if(mScreenWidthMultiplierLoc >=0)gl.glUniform1f(mScreenWidthMultiplierLoc, f);}
    public void setVecPointLightEye2(Vector3f v) { if(mVecPointLightEye2Loc >=0)gl.glUniform3f(mVecPointLightEye2Loc, v.x, v.y, v.z);}
    public void setBgAirLight(float f) { if(mBgAirLightLoc >=0)gl.glUniform1f(mBgAirLightLoc, f);}
    public void setVecPointLightEye3(Vector3f v) { if(mVecPointLightEye3Loc >=0)gl.glUniform3f(mVecPointLightEye3Loc, v.x, v.y, v.z);}
//    public void setM_eneTextureDiffuse(int texture) {
//        GL13.glActiveTexture(GL13.GL_TEXTURE0 + 3);
//        GL11.glBindTexture(GL11.GL_TEXTURE_2D, texture);
//    }
    public void setDSVPointLight(float f) { if(mDSVPointLightLoc >=0)gl.glUniform1f(mDSVPointLightLoc, f);}
//    public void setM_20table(int texture) {
//        GL13.glActiveTexture(GL13.GL_TEXTURE0 + 8);
//        GL11.glBindTexture(GL11.GL_TEXTURE_2D, texture);
//    }
    public void setHeightRange(float f) { if(m_heightRangeLoc >=0)gl.glUniform1f(m_heightRangeLoc, f);}
//    public void setM_eneTextureSpecular(int texture) {
//        GL13.glActiveTexture(GL13.GL_TEXTURE0 + 4);
//        GL11.glBindTexture(GL11.GL_TEXTURE_2D, texture);
//    }
    public void setLightPos(Vector3f v) { if(m_lightPosLoc >=0)gl.glUniform3f(m_lightPosLoc, v.x, v.y, v.z);}
    public void setPointLightIntensity(float f) { if(mPointLightIntensityLoc >=0)gl.glUniform1f(mPointLightIntensityLoc, f);}
    public void setTimeCycle(float f) { if(m_timeCycleLoc >=0)gl.glUniform1f(m_timeCycleLoc, f);}
    public void setResponsePointLight(float f) { if(mResponsePointLightLoc >=0)gl.glUniform1f(mResponsePointLightLoc, f);}
    public void setSpriteSize(float f) { if(mSpriteSizeLoc >=0)gl.glUniform1f(mSpriteSizeLoc, f);}
    public void setProjection(Matrix4f mat) { if(mProjectionLoc >=0)gl.glUniformMatrix4fv(mProjectionLoc, false, CacheBuffer.wrap(mat));}
    public void setInvView(Matrix4f mat) { if(mInvViewLoc >=0)gl.glUniformMatrix4fv(mInvViewLoc, false, CacheBuffer.wrap(mat));}
    public void setViewSpaceLightVec2(Vector3f v) { if(mViewSpaceLightVec2Loc >=0)gl.glUniform3f(mViewSpaceLightVec2Loc, v.x, v.y, v.z);}
    public void setmWorldView(Matrix4f mat) { if(mWorldViewLoc >=0)gl.glUniformMatrix4fv(mWorldViewLoc, false, CacheBuffer.wrap(mat));}
//    public void setM_lashDiffuseTexture(int texture) {
//        GL13.glActiveTexture(GL13.GL_TEXTURE0 + 10);
//        GL11.glBindTexture(GL11.GL_TEXTURE_2D, texture);
//    }
    public void setViewProjectionInverse(Matrix4f mat) { if(mViewProjectionInverseLoc >=0)gl.glUniformMatrix4fv(mViewProjectionInverseLoc, false, CacheBuffer.wrap(mat));}
    public void setScreenWidth(float f) { if(mScreenWidthLoc >=0)gl.glUniform1f(mScreenWidthLoc, f);}
//    public void setM_ngleTexture(int texture) {
//        GL13.glActiveTexture(GL13.GL_TEXTURE0 + 1);
//        GL11.glBindTexture(GL11.GL_TEXTURE_2D, texture);
//    }
    public void setFrameRate(float f) { if(mFrameRateLoc >=0)gl.glUniform1f(mFrameRateLoc, f);}
    public void setFar(float f) { if(mFarLoc >=0)gl.glUniform1f(mFarLoc, f);}
//    public void setM_able(int texture) {
//        GL13.glActiveTexture(GL13.GL_TEXTURE0 + 6);
//        GL11.glBindTexture(GL11.GL_TEXTURE_2D, texture);
//    }
    public void setSplashXDisplace(float f) { if(m_splashXDisplaceLoc >=0)gl.glUniform1f(m_splashXDisplaceLoc, f);}
    public void setVecPointLightEye(Vector3f v) { if(mVecPointLightEyeLoc >=0)gl.glUniform3f(mVecPointLightEyeLoc, v.x, v.y, v.z);}
    public void setScreenHeight(float f) { if(mScreenHeightLoc >=0)gl.glUniform1f(mScreenHeightLoc, f);}
    public void setSpecPower(float f) { if(m_specPowerLoc >=0)gl.glUniform1f(m_specPowerLoc, f);}
    public void setHeight(float f) { if(m_xHeightLoc >=0)gl.glUniform1f(m_xHeightLoc, f);}
    public void setHeightMin(float f) { if(m_heightMinLoc >=0)gl.glUniform1f(m_heightMinLoc, f);}
    public void setRadiusMin(float f) { if(m_radiusMinLoc >=0)gl.glUniform1f(m_radiusMinLoc, f);}
    public void setViewSpaceLightVec(Vector3f v) { if(mViewSpaceLightVecLoc >=0)gl.glUniform3f(mViewSpaceLightVecLoc, v.x, v.y, v.z);}
    public void setUseSpotLight(boolean b) { if(m_useSpotLightLoc >=0)gl.glUniform1i(m_useSpotLightLoc, b ? 1 : 0);}
//    public void setM_able(int texture) {
//        GL13.glActiveTexture(GL13.GL_TEXTURE0 + 7);
//        GL11.glBindTexture(GL11.GL_TEXTURE_2D, texture);
//    }
    public void setScreenHeightMultiplier(float f) { if(mScreenHeightMultiplierLoc >=0)gl.glUniform1f(mScreenHeightMultiplierLoc, f);}
    public void setKd(float f) { if(mKdLoc >=0)gl.glUniform1f(mKdLoc, f);}
    public void setBeta(Vector3f v) { if(m_betaLoc >=0)gl.glUniform3f(m_betaLoc, v.x, v.y, v.z);}
    public void setSpotLightDir(Vector3f v) { if(mSpotLightDirLoc >=0)gl.glUniform3f(mSpotLightDirLoc, v.x, v.y, v.z);}
    public void setDe(float f) { if(m_deLoc >=0)gl.glUniform1f(m_deLoc, f);}
    public void setDiffuseColor(Vector4f v) { if(mDiffuseColorLoc >=0)gl.glUniform4f(mDiffuseColorLoc, v.x, v.y, v.z, v.w);}
    public void setDSVPointLight2(float f) { if(mDSVPointLight2Loc >=0)gl.glUniform1f(mDSVPointLight2Loc, f);}
    public void setDSVPointLight3(float f) { if(mDSVPointLight3Loc >=0)gl.glUniform1f(mDSVPointLight3Loc, f);}
    public void setLightIntensity(float f) { if(m_rLightIntensityLoc >=0)gl.glUniform1f(m_rLightIntensityLoc, f);}
    public void setKsPoint(float f) { if(mKsPointLoc >=0)gl.glUniform1f(mKsPointLoc, f);}
//    public void setM_ckgroundTexture(int texture) {
//        GL13.glActiveTexture(GL13.GL_TEXTURE0 + 2);
//        GL11.glBindTexture(GL11.GL_TEXTURE_2D, texture);
//    }
    public void setInverseProjection(Matrix4f mat) { if(mInverseProjectionLoc >=0)gl.glUniformMatrix4fv(mInverseProjectionLoc, false, CacheBuffer.wrap(mat));}
    public void setWorldViewProj(Matrix4f mat) { if(mWorldViewProjLoc >=0)gl.glUniformMatrix4fv(mWorldViewProjLoc, false, CacheBuffer.wrap(mat));}
//    public void setM_eneTextureNormal(int texture) {
//        GL13.glActiveTexture(GL13.GL_TEXTURE0 + 5);
//        GL11.glBindTexture(GL11.GL_TEXTURE_2D, texture);
//    }
    public void setWorld(Matrix4f mat) { if(mWorldLoc >=0)gl.glUniformMatrix4fv(mWorldLoc, false, CacheBuffer.wrap(mat));}
    public void setDerBg(boolean b) { if(m_nderBgLoc >=0)gl.glUniform1i(m_nderBgLoc, b ? 1 : 0);}
    public void setRadiusRange(float f) { if(m_radiusRangeLoc >=0)gl.glUniform1f(m_radiusRangeLoc, f);}
}
