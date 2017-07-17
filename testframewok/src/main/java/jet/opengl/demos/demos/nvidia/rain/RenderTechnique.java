package jet.opengl.demos.demos.nvidia.rain;

import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector3f;
import org.lwjgl.util.vector.Vector4f;

import java.io.IOException;

import jet.opengl.postprocessing.shader.GLSLProgram;
import jet.opengl.postprocessing.shader.GLSLUtil;
import jet.opengl.postprocessing.shader.ProgramLinkTask;
import jet.opengl.postprocessing.shader.ProgramProperties;
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
        this(vertfile, gemofile, fragfile, null);
    }

    RenderTechnique(String vertfile, String gemofile, String fragfile, ProgramLinkTask task){
        ShaderSourceItem vs_item = new ShaderSourceItem();
        ShaderSourceItem gs_item = null;
        ShaderSourceItem ps_item = null;

        try {
            final String folder = "nvidia/Rain/shaders/";
            vs_item.source = ShaderLoader.loadShaderFile(folder + vertfile, false);
            vs_item.type = ShaderType.VERTEX;

            if(gemofile != null){
                gs_item = new ShaderSourceItem();
                gs_item.source = ShaderLoader.loadShaderFile(folder + gemofile, false);
                gs_item.type = ShaderType.GEOMETRY;
            }

            if(fragfile != null){
                ps_item = new ShaderSourceItem();
                ps_item.source = ShaderLoader.loadShaderFile(folder + fragfile, false);
                ps_item.type = ShaderType.FRAGMENT;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        if(task != null){
            addLinkTask(task);
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

    void setUniform(RainParams params){
//        final Matrix4f g_mInverseProjection = new Matrix4f();
//        final Matrix4f g_mWorld = new Matrix4f();
//        final Matrix4f g_mWorldViewProj = new Matrix4f();
//        final Matrix4f g_mWorldView = new Matrix4f();
//        final Matrix4f g_mProjection = new Matrix4f();
//        final Matrix4f g_mViewProjectionInverse = new Matrix4f();
//        final Matrix4f g_mInvView = new Matrix4f();

        setInverseProjection(params.g_mInverseProjection);
        setWorld(params.g_mWorld);
        setWorldViewProj(params.g_mWorldViewProj);
        setWorldView(params.g_mWorldView);
        setProjection(params.g_mProjection);
        setViewProjectionInverse(params.g_mViewProjectionInverse);
        setInvView(params.g_mInvView);

//        final Vector3f g_eyePos = new Vector3f();   //eye in world space
//        final Vector3f g_lightPos = new Vector3f(10,10,0); //the directional light in world space
//        float g_de;
//        final Vector3f g_ViewSpaceLightVec = new Vector3f();
//        final Vector3f g_ViewSpaceLightVec2 = new Vector3f();
//        float g_DSVPointLight;
//        float g_DSVPointLight2;
//        float g_DSVPointLight3;
//        final Vector3f g_VecPointLightEye = new Vector3f();
//        final Vector3f g_VecPointLightEye2 = new Vector3f();
//        final Vector3f g_VecPointLightEye3 = new Vector3f();
//        boolean g_useSpotLight = true;
//        float g_cosSpotlightAngle = 0.8f;
//        final Vector3f g_SpotLightDir = new Vector3f(0,-1,0);
//        float g_FrameRate;
//        float g_timeCycle;
//        float g_splashXDisplace;
//        float g_splashYDisplace;

        setEyePos(params.g_eyePos);
        setLightPos(params.g_lightPos);
        setDe(params.g_de);
        setViewSpaceLightVec(params.g_ViewSpaceLightVec);
        setViewSpaceLightVec2(params.g_ViewSpaceLightVec2);
        setDSVPointLight(params.g_DSVPointLight);
        setDSVPointLight2(params.g_DSVPointLight2);
        setDSVPointLight3(params.g_DSVPointLight3);
        setVecPointLightEye(params.g_VecPointLightEye);
        setVecPointLightEye2(params.g_VecPointLightEye2);
        setVecPointLightEye3(params.g_VecPointLightEye3);
        setUseSpotLight(params.g_useSpotLight);
        setCosSpotlightAngle(params.g_cosSpotlightAngle);
        setSpotLightDir(params.g_SpotLightDir);
        setFrameRate(params.g_FrameRate);
        setTimeCycle(params.g_timeCycle);
        setSplashXDisplace(params.g_splashXDisplace);
        setSplashYDisplace(params.g_splashYDisplace);

//changesOften
//        float g_ResponseDirLight = 1.0f;
//        float g_ResponsePointLight = 1.0f;
//        float dirLightIntensity = 1.0f;
//        boolean renderBg = false;
//        boolean moveParticles = false;
//        final Vector3f g_TotalVel = new Vector3f(0,-0.25f,0);
//        final Vector4f g_DiffuseColor = new Vector4f();
//        float g_PointLightIntensity = 2.0f;
//        float g_SpriteSize = 1.0f;
//        final Vector3f g_beta = new Vector3f(0.04f,0.04f,0.04f);
//        float g_BgAirLight = 0.0f;
//        float g_Kd = 0.1f;
//        float g_KsPoint = 20;
//        float g_KsDir = 10;
//        float g_specPower = 20;
        setResponseDirLight(params.g_ResponseDirLight);
        setResponsePointLight(params.g_ResponsePointLight);
        setLightIntensity(params.dirLightIntensity);
        setRenderBg(params.renderBg);
        setVeParticles(params.moveParticles);
        setTotalVel(params.g_TotalVel);
        setDiffuseColor(params.g_DiffuseColor);
        setPointLightIntensity(params.g_PointLightIntensity);
        setSpriteSize(params.g_SpriteSize);
        setBeta(params.g_beta);
        setBgAirLight(params.g_BgAirLight);
        setKd(params.g_Kd);
        setKsPoint(params.g_KsPoint);
        setKsDir(params.g_KsDir);
        setSpecPower(params.g_specPower);

//        float g_ScreenWidth = 640.0f;
//        float g_ScreenHeight = 480.0f;
//        float g_ScreenWidthMultiplier =  0.0031299f;
//        float g_ScreenHeightMultiplier = 0.0041754f;
//        float g_heightMin = 0.0f;
//        float g_radiusMin = 1.0f;
//        float g_heightRange = 30.0f;
//        float g_radiusRange = 30.0f;
//        float maxHeight;
//        float g_Near;
//        float g_Far;

        setScreenWidth(params.g_ScreenWidth);
        setScreenHeight(params.g_ScreenHeight);
        setScreenWidthMultiplier(params.g_ScreenWidthMultiplier);
        setScreenHeightMultiplier(params.g_ScreenHeightMultiplier);
        setHeightMin(params.g_heightMin);
        setRadiusMin(params.g_radiusMin);
        setHeightRange(params.g_heightRange);
        setRadiusRange(params.g_radiusRange);
        setHeight(params.maxHeight);
        setNear(params.g_Near);
        setFar(params.g_Far);
    }

    public void printPrograminfo(){
        System.out.println("----------------------------"+getName() +"-----------------------------------------" );
        ProgramProperties props = GLSLUtil.getProperties(getProgram());
        System.out.println(props);
    }

    private void setEyePos(Vector3f v) { if(m_eyePosLoc >=0)gl.glUniform3f(m_eyePosLoc, v.x, v.y, v.z);}
    private void setNear(float f) { if(mNearLoc >=0)gl.glUniform1f(mNearLoc, f);}
    private void setVeParticles(boolean b) { if(m_veParticlesLoc >=0)gl.glUniform1i(m_veParticlesLoc, b ? 1 : 0);}
    private void setResponseDirLight(float f) { if(mResponseDirLightLoc >=0)gl.glUniform1f(mResponseDirLightLoc, f);}
    private void setSplashYDisplace(float f) { if(m_splashYDisplaceLoc >=0)gl.glUniform1f(m_splashYDisplaceLoc, f);}
    private void setCosSpotlightAngle(float f) { if(m_cosSpotlightAngleLoc >=0)gl.glUniform1f(m_cosSpotlightAngleLoc, f);}
    private void setTotalVel(Vector3f v) { if(mTotalVelLoc >=0)gl.glUniform3f(mTotalVelLoc, v.x, v.y, v.z);}
//    public void setSashBumpTexture(int texture) {
//        GL13.glActiveTexture(GL13.GL_TEXTURE0 + 9);
//        GL11.glBindTexture(GL11.GL_TEXTURE_2D, texture);
//    }
    private void setKsDir(float f) { if(mKsDirLoc >=0)gl.glUniform1f(mKsDirLoc, f);}
    private void setScreenWidthMultiplier(float f) { if(mScreenWidthMultiplierLoc >=0)gl.glUniform1f(mScreenWidthMultiplierLoc, f);}
    private void setVecPointLightEye2(Vector3f v) { if(mVecPointLightEye2Loc >=0)gl.glUniform3f(mVecPointLightEye2Loc, v.x, v.y, v.z);}
    private void setBgAirLight(float f) { if(mBgAirLightLoc >=0)gl.glUniform1f(mBgAirLightLoc, f);}
    private void setVecPointLightEye3(Vector3f v) { if(mVecPointLightEye3Loc >=0)gl.glUniform3f(mVecPointLightEye3Loc, v.x, v.y, v.z);}
//    public void setM_eneTextureDiffuse(int texture) {
//        GL13.glActiveTexture(GL13.GL_TEXTURE0 + 3);
//        GL11.glBindTexture(GL11.GL_TEXTURE_2D, texture);
//    }
    private void setDSVPointLight(float f) { if(mDSVPointLightLoc >=0)gl.glUniform1f(mDSVPointLightLoc, f);}
//    public void setM_20table(int texture) {
//        GL13.glActiveTexture(GL13.GL_TEXTURE0 + 8);
//        GL11.glBindTexture(GL11.GL_TEXTURE_2D, texture);
//    }
    private void setHeightRange(float f) { if(m_heightRangeLoc >=0)gl.glUniform1f(m_heightRangeLoc, f);}
//    public void setM_eneTextureSpecular(int texture) {
//        GL13.glActiveTexture(GL13.GL_TEXTURE0 + 4);
//        GL11.glBindTexture(GL11.GL_TEXTURE_2D, texture);
//    }
    private void setLightPos(Vector3f v) { if(m_lightPosLoc >=0)gl.glUniform3f(m_lightPosLoc, v.x, v.y, v.z);}
    private void setPointLightIntensity(float f) { if(mPointLightIntensityLoc >=0)gl.glUniform1f(mPointLightIntensityLoc, f);}
    private void setTimeCycle(float f) { if(m_timeCycleLoc >=0)gl.glUniform1f(m_timeCycleLoc, f);}
    private void setResponsePointLight(float f) { if(mResponsePointLightLoc >=0)gl.glUniform1f(mResponsePointLightLoc, f);}
    private void setSpriteSize(float f) { if(mSpriteSizeLoc >=0)gl.glUniform1f(mSpriteSizeLoc, f);}
    private void setProjection(Matrix4f mat) { if(mProjectionLoc >=0)gl.glUniformMatrix4fv(mProjectionLoc, false, CacheBuffer.wrap(mat));}
    private void setInvView(Matrix4f mat) { if(mInvViewLoc >=0)gl.glUniformMatrix4fv(mInvViewLoc, false, CacheBuffer.wrap(mat));}
    private void setViewSpaceLightVec2(Vector3f v) { if(mViewSpaceLightVec2Loc >=0)gl.glUniform3f(mViewSpaceLightVec2Loc, v.x, v.y, v.z);}
    private void setWorldView(Matrix4f mat) { if(mWorldViewLoc >=0)gl.glUniformMatrix4fv(mWorldViewLoc, false, CacheBuffer.wrap(mat));}
//    public void setM_lashDiffuseTexture(int texture) {
//        GL13.glActiveTexture(GL13.GL_TEXTURE0 + 10);
//        GL11.glBindTexture(GL11.GL_TEXTURE_2D, texture);
//    }
    private void setViewProjectionInverse(Matrix4f mat) { if(mViewProjectionInverseLoc >=0)gl.glUniformMatrix4fv(mViewProjectionInverseLoc, false, CacheBuffer.wrap(mat));}
    private void setScreenWidth(float f) { if(mScreenWidthLoc >=0)gl.glUniform1f(mScreenWidthLoc, f);}
//    public void setM_ngleTexture(int texture) {
//        GL13.glActiveTexture(GL13.GL_TEXTURE0 + 1);
//        GL11.glBindTexture(GL11.GL_TEXTURE_2D, texture);
//    }
    private void setFrameRate(float f) { if(mFrameRateLoc >=0)gl.glUniform1f(mFrameRateLoc, f);}
    private void setFar(float f) { if(mFarLoc >=0)gl.glUniform1f(mFarLoc, f);}
//    public void setM_able(int texture) {
//        GL13.glActiveTexture(GL13.GL_TEXTURE0 + 6);
//        GL11.glBindTexture(GL11.GL_TEXTURE_2D, texture);
//    }
    private void setSplashXDisplace(float f) { if(m_splashXDisplaceLoc >=0)gl.glUniform1f(m_splashXDisplaceLoc, f);}
    private void setVecPointLightEye(Vector3f v) { if(mVecPointLightEyeLoc >=0)gl.glUniform3f(mVecPointLightEyeLoc, v.x, v.y, v.z);}
    private void setScreenHeight(float f) { if(mScreenHeightLoc >=0)gl.glUniform1f(mScreenHeightLoc, f);}
    private void setSpecPower(float f) { if(m_specPowerLoc >=0)gl.glUniform1f(m_specPowerLoc, f);}
    private void setHeight(float f) { if(m_xHeightLoc >=0)gl.glUniform1f(m_xHeightLoc, f);}
    private void setHeightMin(float f) { if(m_heightMinLoc >=0)gl.glUniform1f(m_heightMinLoc, f);}
    private void setRadiusMin(float f) { if(m_radiusMinLoc >=0)gl.glUniform1f(m_radiusMinLoc, f);}
    private void setViewSpaceLightVec(Vector3f v) { if(mViewSpaceLightVecLoc >=0)gl.glUniform3f(mViewSpaceLightVecLoc, v.x, v.y, v.z);}
    private void setUseSpotLight(boolean b) { if(m_useSpotLightLoc >=0)gl.glUniform1i(m_useSpotLightLoc, b ? 1 : 0);}
//    public void setM_able(int texture) {
//        GL13.glActiveTexture(GL13.GL_TEXTURE0 + 7);
//        GL11.glBindTexture(GL11.GL_TEXTURE_2D, texture);
//    }
    private void setScreenHeightMultiplier(float f) { if(mScreenHeightMultiplierLoc >=0)gl.glUniform1f(mScreenHeightMultiplierLoc, f);}
    private void setKd(float f) { if(mKdLoc >=0)gl.glUniform1f(mKdLoc, f);}
    private void setBeta(Vector3f v) { if(m_betaLoc >=0)gl.glUniform3f(m_betaLoc, v.x, v.y, v.z);}
    private void setSpotLightDir(Vector3f v) { if(mSpotLightDirLoc >=0)gl.glUniform3f(mSpotLightDirLoc, v.x, v.y, v.z);}
    private void setDe(float f) { if(m_deLoc >=0)gl.glUniform1f(m_deLoc, f);}
    private void setDiffuseColor(Vector4f v) { if(mDiffuseColorLoc >=0)gl.glUniform4f(mDiffuseColorLoc, v.x, v.y, v.z, v.w);}
    private void setDSVPointLight2(float f) { if(mDSVPointLight2Loc >=0)gl.glUniform1f(mDSVPointLight2Loc, f);}
    private void setDSVPointLight3(float f) { if(mDSVPointLight3Loc >=0)gl.glUniform1f(mDSVPointLight3Loc, f);}
    private void setLightIntensity(float f) { if(m_rLightIntensityLoc >=0)gl.glUniform1f(m_rLightIntensityLoc, f);}
    private void setKsPoint(float f) { if(mKsPointLoc >=0)gl.glUniform1f(mKsPointLoc, f);}
//    public void setM_ckgroundTexture(int texture) {
//        GL13.glActiveTexture(GL13.GL_TEXTURE0 + 2);
//        GL11.glBindTexture(GL11.GL_TEXTURE_2D, texture);
//    }
    private void setInverseProjection(Matrix4f mat) { if(mInverseProjectionLoc >=0)gl.glUniformMatrix4fv(mInverseProjectionLoc, false, CacheBuffer.wrap(mat));}
    private void setWorldViewProj(Matrix4f mat) { if(mWorldViewProjLoc >=0)gl.glUniformMatrix4fv(mWorldViewProjLoc, false, CacheBuffer.wrap(mat));}
//    public void setM_eneTextureNormal(int texture) {
//        GL13.glActiveTexture(GL13.GL_TEXTURE0 + 5);
//        GL11.glBindTexture(GL11.GL_TEXTURE_2D, texture);
//    }
    private void setWorld(Matrix4f mat) { if(mWorldLoc >=0)gl.glUniformMatrix4fv(mWorldLoc, false, CacheBuffer.wrap(mat));}
    private void setRenderBg(boolean b) { if(m_nderBgLoc >=0)gl.glUniform1i(m_nderBgLoc, b ? 1 : 0);}
    private void setRadiusRange(float f) { if(m_radiusRangeLoc >=0)gl.glUniform1f(m_radiusRangeLoc, f);}
}
