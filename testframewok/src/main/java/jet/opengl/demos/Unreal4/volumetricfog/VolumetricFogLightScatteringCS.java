package jet.opengl.demos.Unreal4.volumetricfog;

import java.io.IOException;

import jet.opengl.postprocessing.shader.GLSLProgram;
import jet.opengl.postprocessing.shader.GLSLUtil;
import jet.opengl.postprocessing.shader.Macro;
import jet.opengl.postprocessing.shader.ShaderLoader;
import jet.opengl.postprocessing.shader.ShaderSourceItem;
import jet.opengl.postprocessing.shader.ShaderType;

final class VolumetricFogLightScatteringCS extends GLSLProgram {

    VolumetricFogLightScatteringCS(String prefix, int threadSize, IntegratedKey key){
        CharSequence source = null;

        try {
            source = ShaderLoader.loadShaderFile(prefix + "LightScatteringCS.comp", false);
        } catch (IOException e) {
            e.printStackTrace();
        }

        ShaderSourceItem item = new ShaderSourceItem(source, ShaderType.COMPUTE);
        item.macros = new Macro[]{
                new Macro("THREADGROUP_SIZE", threadSize),
                new Macro("USE_TEMPORAL_REPROJECTION", key.bUseGlobalDistanceField?1:0),
                new Macro("DISTANCE_FIELD_SKY_OCCLUSION", key.bUseDistanceFieldSkyOcclusion?1:0),
        };
        setSourceFromStrings(item);
    }

    void apply(LightScatteringParameters params){
        GLSLUtil.setMat4(this, "DirectionalLightFunctionWorldToShadow", params.DirectionalLightFunctionWorldToShadow);
        GLSLUtil.setFloat4(this, "FrameJitterOffsets", params.FrameJitterOffsets);
        GLSLUtil.setFloat3(this, "HeightFogDirectionalLightInscatteringColor", params.HeightFogDirectionalLightInscatteringColor);
        GLSLUtil.setUInt(this, "HistoryMissSuperSampleCount", params.HistoryMissSuperSampleCount);
        GLSLUtil.setFloat(this, "HistoryWeight", params.HistoryWeight);
        GLSLUtil.setFloat(this, "InverseSquaredLightDistanceBiasScale", params.InverseSquaredLightDistanceBiasScale);
        GLSLUtil.setFloat(this, "PhaseG", params.PhaseG);

        GLSLUtil.setMat4(this, "UnjitteredClipToTranslatedWorld", params.UnjitteredClipToTranslatedWorld);
        GLSLUtil.setMat4(this, "UnjitteredPrevWorldToClip", params.UnjitteredPrevWorldToClip);
        GLSLUtil.setFloat(this, "PhaseG", params.PhaseG);
        GLSLUtil.setBool(this, "UseDirectionalLightShadowing", params.UseDirectionalLightShadowing);
        GLSLUtil.setBool(this, "UseHeightFogColors", params.UseHeightFogColors);
        GLSLUtil.setFloat3(this, "View_PreViewTranslation", params.View_PreViewTranslation);
        GLSLUtil.setFloat3(this, "VolumetricFog_GridSize", params.VolumetricFog_GridSize);
        GLSLUtil.setFloat3(this, "VolumetricFog_GridZParams", params.VolumetricFog_GridZParams);
        GLSLUtil.setFloat3(this, "WorldCameraOrigin", params.WorldCameraOrigin);
        GLSLUtil.setMat4(this, "g_ViewProj", params.g_ViewProj);
    }
}
