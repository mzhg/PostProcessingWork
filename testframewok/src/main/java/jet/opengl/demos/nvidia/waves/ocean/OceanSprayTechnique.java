package jet.opengl.demos.nvidia.waves.ocean;

import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector2f;
import org.lwjgl.util.vector.Vector3f;
import org.lwjgl.util.vector.Vector4f;

import jet.opengl.postprocessing.util.CacheBuffer;

final class OceanSprayTechnique extends Technique implements OceanConst{

    @Override
    public void enable(TechniqueParams params) {
        super.enable(params);

        init();

        OceanSprayParams sprayParams = (OceanSprayParams)params;
        setSimulationTime(sprayParams.g_SimulationTime);
        setLightningPosition(sprayParams.g_LightningPosition);
        setLightningColor(sprayParams.g_LightningColor);
        setAmbientColor(sprayParams.g_AmbientColor);
        setworldToHeightLookupScale(sprayParams.g_worldToHeightLookupScale);
        setAudioVisualizationLevel(sprayParams.g_AudioVisualizationLevel);
        setLightColor(sprayParams.g_LightColor);
        setSimpleParticles(sprayParams.g_SimpleParticles);
        setPSMOpacityMultiplier(sprayParams.g_PSMOpacityMultiplier);
        setFogExponent(sprayParams.g_FogExponent);
        setworldToHeightLookupRot(sprayParams.g_worldToHeightLookupRot);
        setAudioVisualizationMargin(sprayParams.g_AudioVisualizationMargin);
        setAudioVisualizationRect(sprayParams.g_AudioVisualizationRect);
        setLightsNum(sprayParams.g_LightsNum);
        setvesselToWorld(sprayParams.g_vesselToWorld);
        setSpotlightPosition(sprayParams.g_SpotlightPosition);
        setiDepthSortLevel(sprayParams.g_iDepthSortLevel);
        setWindSpeed(sprayParams.g_WindSpeed);
        setSpotlightMatrix(sprayParams.g_SpotlightMatrix);
        setParticlesNum(sprayParams.g_ParticlesNum);
        setworldToVessel(sprayParams.g_worldToVessel);
        setmatWorldToFoam(sprayParams.g_matWorldToFoam);
        setmatProj(sprayParams.g_matProj);
        setLightDirection(sprayParams.g_LightDirection);
        setiDepthSortLevelMask(sprayParams.g_iDepthSortLevelMask);
        setworldToHeightLookupOffset(sprayParams.g_worldToHeightLookupOffset);
        setSpotLightAxisAndCosAngle(sprayParams.g_SpotLightAxisAndCosAngle);
        setiDepthSortHeight(sprayParams.g_iDepthSortHeight);
        setInvParticleLifeTime(sprayParams.g_InvParticleLifeTime);
        setmatProjInv(sprayParams.g_matProjInv);
        setSpotlightColor(sprayParams.g_SpotlightColor);
        setmatView(sprayParams.g_matView);
        setiDepthSortWidth(sprayParams.g_iDepthSortWidth);

        if(_PSMSlices >= 0) gl.glUniform1f(_PSMSlices, sprayParams.m_pPSMParams.g_PSMSlices);
    }

    private void setSimulationTime(float f) { if(m_g_SimulationTimeLoc >=0)gl.glUniform1f(m_g_SimulationTimeLoc, f);}
    private void setLightningPosition(Vector3f v) { if(m_g_LightningPositionLoc >=0)gl.glUniform3f(m_g_LightningPositionLoc, v.x, v.y, v.z);}
    private void setLightningColor(Vector3f v) { if(m_g_LightningColorLoc >=0)gl.glUniform3f(m_g_LightningColorLoc, v.x, v.y, v.z);}
    private void setAmbientColor(Vector3f v) { if(m_g_AmbientColorLoc >=0)gl.glUniform3f(m_g_AmbientColorLoc, v.x, v.y, v.z);}
    private void setworldToHeightLookupScale(Vector2f v) { if(m_g_worldToHeightLookupScaleLoc >=0)gl.glUniform2f(m_g_worldToHeightLookupScaleLoc, v.x, v.y);}
    private void setAudioVisualizationLevel(float f) { if(m_g_AudioVisualizationLevelLoc >=0)gl.glUniform1f(m_g_AudioVisualizationLevelLoc, f);}
    private void setLightColor(Vector3f v) { if(m_g_LightColorLoc >=0)gl.glUniform3f(m_g_LightColorLoc, v.x, v.y, v.z);}
    private void setSimpleParticles(float f) { if(m_g_SimpleParticlesLoc >=0)gl.glUniform1f(m_g_SimpleParticlesLoc, f);}
    private void setPSMOpacityMultiplier(float f) { if(m_g_PSMOpacityMultiplierLoc >=0)gl.glUniform1f(m_g_PSMOpacityMultiplierLoc, f);}
    private void setFogExponent(float f) { if(m_g_FogExponentLoc >=0)gl.glUniform1f(m_g_FogExponentLoc, f);}
    private void setworldToHeightLookupRot(Vector2f v) { if(m_g_worldToHeightLookupRotLoc >=0)gl.glUniform2f(m_g_worldToHeightLookupRotLoc, v.x, v.y);}
    private void setAudioVisualizationMargin(Vector2f v) { if(m_g_AudioVisualizationMarginLoc >=0)gl.glUniform2f(m_g_AudioVisualizationMarginLoc, v.x, v.y);}
    private void setAudioVisualizationRect(Vector4f v) { if(m_g_AudioVisualizationRectLoc >=0)gl.glUniform4f(m_g_AudioVisualizationRectLoc, v.x, v.y, v.z, v.w);}
    private void setLightsNum(int i) { if(m_g_LightsNumLoc >=0)gl.glUniform1i(m_g_LightsNumLoc, i);}
    private void setvesselToWorld(Matrix4f mat) { if(m_g_vesselToWorldLoc >=0)gl.glUniformMatrix4fv(m_g_vesselToWorldLoc, false, CacheBuffer.wrap(mat));}
    private void setSpotlightPosition(Vector4f[] v) { if(m_g_SpotlightPosition >=0)gl.glUniform4fv(m_g_SpotlightPosition, CacheBuffer.wrap(v));}
    private void setiDepthSortLevel(int i) { if(m_g_iDepthSortLevelLoc >=0)gl.glUniform1i(m_g_iDepthSortLevelLoc, i);}
    private void setWindSpeed(Vector3f v) { if(m_g_WindSpeedLoc >=0)gl.glUniform3f(m_g_WindSpeedLoc, v.x, v.y, v.z);}
    private void setSpotlightMatrix(Matrix4f[] mat) { if(m_g_SpotlightMatrix >=0)gl.glUniformMatrix4fv(m_g_SpotlightMatrix, false, CacheBuffer.wrap(mat));}
    private void setParticlesNum(int i) { if(m_g_ParticlesNumLoc >=0)gl.glUniform1i(m_g_ParticlesNumLoc, i);}
    private void setworldToVessel(Matrix4f mat) { if(m_g_worldToVesselLoc >=0)gl.glUniformMatrix4fv(m_g_worldToVesselLoc, false, CacheBuffer.wrap(mat));}
    private void setmatWorldToFoam(Matrix4f mat) { if(m_g_matWorldToFoamLoc >=0)gl.glUniformMatrix4fv(m_g_matWorldToFoamLoc, false, CacheBuffer.wrap(mat));}
    private void setmatProj(Matrix4f mat) { if(m_g_matProjLoc >=0)gl.glUniformMatrix4fv(m_g_matProjLoc, false, CacheBuffer.wrap(mat));}
    private void setLightDirection(Vector3f v) { if(m_g_LightDirectionLoc >=0)gl.glUniform3f(m_g_LightDirectionLoc, v.x, v.y, v.z);}
    private void setiDepthSortLevelMask(int i) { if(m_g_iDepthSortLevelMaskLoc >=0)gl.glUniform1i(m_g_iDepthSortLevelMaskLoc, i);}
    private void setworldToHeightLookupOffset(Vector2f v) { if(m_g_worldToHeightLookupOffsetLoc >=0)gl.glUniform2f(m_g_worldToHeightLookupOffsetLoc, v.x, v.y);}
    private void setSpotLightAxisAndCosAngle(Vector4f[] v) { if(m_g_SpotLightAxisAndCosAngle >=0)gl.glUniform4fv(m_g_SpotLightAxisAndCosAngle, CacheBuffer.wrap(v));}
    private void setiDepthSortHeight(int i) { if(m_g_iDepthSortHeightLoc >=0)gl.glUniform1i(m_g_iDepthSortHeightLoc, i);}
    private void setInvParticleLifeTime(float f) { if(m_g_InvParticleLifeTimeLoc >=0)gl.glUniform1f(m_g_InvParticleLifeTimeLoc, f);}
    private void setmatProjInv(Matrix4f mat) { if(m_g_matProjInvLoc >=0)gl.glUniformMatrix4fv(m_g_matProjInvLoc, false, CacheBuffer.wrap(mat));}
    private void setSpotlightColor(Vector4f[] v) { if(m_g_SpotlightColor >=0)gl.glUniform4fv(m_g_SpotlightColor, CacheBuffer.wrap(v));}
    private void setmatView(Matrix4f mat) { if(m_g_matViewLoc >=0)gl.glUniformMatrix4fv(m_g_matViewLoc, false, CacheBuffer.wrap(mat));}
    private void setiDepthSortWidth(int i) { if(m_g_iDepthSortWidthLoc >=0)gl.glUniform1i(m_g_iDepthSortWidthLoc, i);}

    private int m_g_SimulationTimeLoc = -1;
    private int m_g_LightningPositionLoc = -1;
    private int m_g_LightningColorLoc = -1;
    private int m_g_AmbientColorLoc = -1;
    private int m_g_worldToHeightLookupScaleLoc = -1;
    private int m_g_AudioVisualizationLevelLoc = -1;
    private int m_g_texHeightLookupLoc = -1;
    private int m_g_RenderOrientationAndDecimationDataLoc = -1;
    private int m_g_LightColorLoc = -1;
    private int m_g_RenderVelocityAndTimeDataLoc = -1;
    private int m_g_SimpleParticlesLoc = -1;
    private int m_g_PSMOpacityMultiplierLoc = -1;
    private int m_g_FogExponentLoc = -1;
    private int m_g_worldToHeightLookupRotLoc = -1;
    private int m_g_AudioVisualizationMarginLoc = -1;
    private int m_g_AudioVisualizationRectLoc = -1;
    private int m_g_LightsNumLoc = -1;
    private int m_g_RenderInstanceDataLoc = -1;
    private int m_g_vesselToWorldLoc = -1;
    private int m_g_SpotlightPosition = -1;
    private int m_g_iDepthSortLevelLoc = -1;
    private int m_g_WindSpeedLoc = -1;
    private int m_g_texSplashLoc = -1;
    private int m_g_SpotlightMatrix = -1;
    private int m_g_ParticlesNumLoc = -1;
    private int m_g_worldToVesselLoc = -1;
    private int m_g_matWorldToFoamLoc = -1;
    private int m_g_matProjLoc = -1;
    private int m_g_LightDirectionLoc = -1;
    private int m_g_iDepthSortLevelMaskLoc = -1;
    private int m_g_worldToHeightLookupOffsetLoc = -1;
    private int m_g_SpotLightAxisAndCosAngle = -1;
    private int m_g_SprayParticleCountLoc = -1;
    private int m_g_iDepthSortHeightLoc = -1;
    private int m_g_InvParticleLifeTimeLoc = -1;
    private int m_g_matProjInvLoc = -1;
    private int m_g_SpotlightResource = -1;
    private int m_g_SpotlightColor = -1;
    private int m_g_matViewLoc = -1;
    private int m_g_iDepthSortWidthLoc = -1;

    private int _PSMSlices;

    private boolean m_init = false;

    private void init(){
        if(m_init) return;
        m_init = true;

        _PSMSlices = gl.glGetUniformLocation(m_program, "g_PSMSlices");
        m_g_SimulationTimeLoc = gl.glGetUniformLocation(m_program, "g_SimulationTime");
        m_g_LightningPositionLoc = gl.glGetUniformLocation(m_program, "g_LightningPosition");
        m_g_LightningColorLoc = gl.glGetUniformLocation(m_program, "g_LightningColor");
        m_g_AmbientColorLoc = gl.glGetUniformLocation(m_program, "g_AmbientColor");
        m_g_worldToHeightLookupScaleLoc = gl.glGetUniformLocation(m_program, "g_worldToHeightLookupScale");
        m_g_AudioVisualizationLevelLoc = gl.glGetUniformLocation(m_program, "g_AudioVisualizationLevel");
        m_g_texHeightLookupLoc = gl.glGetUniformLocation(m_program, "g_texHeightLookup");
        m_g_RenderOrientationAndDecimationDataLoc = gl.glGetUniformLocation(m_program, "g_RenderOrientationAndDecimationData");
        m_g_LightColorLoc = gl.glGetUniformLocation(m_program, "g_LightColor");
        m_g_RenderVelocityAndTimeDataLoc = gl.glGetUniformLocation(m_program, "g_RenderVelocityAndTimeData");
        m_g_SimpleParticlesLoc = gl.glGetUniformLocation(m_program, "g_SimpleParticles");
        m_g_PSMOpacityMultiplierLoc = gl.glGetUniformLocation(m_program, "g_PSMOpacityMultiplier");
        m_g_FogExponentLoc = gl.glGetUniformLocation(m_program, "g_FogExponent");
        m_g_worldToHeightLookupRotLoc = gl.glGetUniformLocation(m_program, "g_worldToHeightLookupRot");
        m_g_AudioVisualizationMarginLoc = gl.glGetUniformLocation(m_program, "g_AudioVisualizationMargin");
        m_g_AudioVisualizationRectLoc = gl.glGetUniformLocation(m_program, "g_AudioVisualizationRect");
        m_g_LightsNumLoc = gl.glGetUniformLocation(m_program, "g_LightsNum");
        m_g_RenderInstanceDataLoc = gl.glGetUniformLocation(m_program, "g_RenderInstanceData");
        m_g_vesselToWorldLoc = gl.glGetUniformLocation(m_program, "g_vesselToWorld");
        m_g_SpotlightPosition = gl.glGetUniformLocation(m_program, "g_SpotlightPosition");
        m_g_iDepthSortLevelLoc = gl.glGetUniformLocation(m_program, "g_iDepthSortLevel");
        m_g_WindSpeedLoc = gl.glGetUniformLocation(m_program, "g_WindSpeed");
        m_g_texSplashLoc = gl.glGetUniformLocation(m_program, "g_texSplash");
        m_g_SpotlightMatrix = gl.glGetUniformLocation(m_program, "g_SpotlightMatrix");
        m_g_ParticlesNumLoc = gl.glGetUniformLocation(m_program, "g_ParticlesNum");
        m_g_worldToVesselLoc = gl.glGetUniformLocation(m_program, "g_worldToVessel");
        m_g_matWorldToFoamLoc = gl.glGetUniformLocation(m_program, "g_matWorldToFoam");
        m_g_matProjLoc = gl.glGetUniformLocation(m_program, "g_matProj");
        m_g_LightDirectionLoc = gl.glGetUniformLocation(m_program, "g_LightDirection");
        m_g_iDepthSortLevelMaskLoc = gl.glGetUniformLocation(m_program, "g_iDepthSortLevelMask");
        m_g_worldToHeightLookupOffsetLoc = gl.glGetUniformLocation(m_program, "g_worldToHeightLookupOffset");
        m_g_SpotLightAxisAndCosAngle = gl.glGetUniformLocation(m_program, "g_SpotLightAxisAndCosAngle");
        m_g_SprayParticleCountLoc = gl.glGetUniformLocation(m_program, "g_SprayParticleCount");
        m_g_iDepthSortHeightLoc = gl.glGetUniformLocation(m_program, "g_iDepthSortHeight");
        m_g_InvParticleLifeTimeLoc = gl.glGetUniformLocation(m_program, "g_InvParticleLifeTime");
        m_g_matProjInvLoc = gl.glGetUniformLocation(m_program, "g_matProjInv");
        m_g_SpotlightResource = gl.glGetUniformLocation(m_program, "g_SpotlightResource");
        m_g_SpotlightColor = gl.glGetUniformLocation(m_program, "g_SpotlightColor");
        m_g_matViewLoc = gl.glGetUniformLocation(m_program, "g_matView");
        m_g_iDepthSortWidthLoc = gl.glGetUniformLocation(m_program, "g_iDepthSortWidth");
    }

}
