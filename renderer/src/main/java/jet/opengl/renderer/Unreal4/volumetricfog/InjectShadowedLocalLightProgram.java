package jet.opengl.renderer.Unreal4.volumetricfog;

import java.io.IOException;

import jet.opengl.postprocessing.shader.GLSLProgram;
import jet.opengl.postprocessing.shader.GLSLUtil;
import jet.opengl.postprocessing.shader.Macro;
import jet.opengl.postprocessing.shader.ShaderLoader;
import jet.opengl.postprocessing.shader.ShaderSourceItem;
import jet.opengl.postprocessing.shader.ShaderType;

final class InjectShadowedLocalLightProgram extends GLSLProgram {
    InjectShadowedLocalLightProgram(String prefx, boolean bDynamicallyShadowed, boolean bInverseSquared, boolean bTemporalReprojection){
        CharSequence vert_source = null;
        CharSequence gemo_source = null;
        CharSequence frag_source = null;

        try {
            vert_source = ShaderLoader.loadShaderFile(prefx + "WriteToBoundingSphereVS.vert", false);
            gemo_source = ShaderLoader.loadShaderFile(prefx + "WriteToSliceMainGS.gemo", false);
            frag_source = ShaderLoader.loadShaderFile(prefx + "InjectShadowedLocalLightPS.frag", false);
        } catch (IOException e) {
            e.printStackTrace();
        }

        Macro[] macros = {
                new Macro("DYNAMICALLY_SHADOWED", bDynamicallyShadowed?1:0),
                new Macro("INVERSE_SQUARED_FALLOFF", bInverseSquared?1:0),
                new Macro("USE_TEMPORAL_REPROJECTION", bTemporalReprojection?1:0),
        };

        ShaderSourceItem vs_item = new ShaderSourceItem(vert_source, ShaderType.VERTEX);
        ShaderSourceItem gemo_item = new ShaderSourceItem(gemo_source, ShaderType.GEOMETRY);
        ShaderSourceItem frag_item = new ShaderSourceItem(frag_source, ShaderType.FRAGMENT);
        frag_item.macros = macros;

        setSourceFromStrings(vs_item, gemo_item, frag_item);
    }

    void applyUniforms(InjectLocalLightParameters params){
        GLSLUtil.setFloat2(this, "DepthBiasParameters", params.DepthBiasParameters);
        GLSLUtil.setFloat4(this, "FrameJitterOffsets", params.FrameJitterOffsets);
        GLSLUtil.setUInt(this, "HistoryMissSuperSampleCount", params.HistoryMissSuperSampleCount);
        GLSLUtil.setFloat(this, "HistoryWeight", params.HistoryWeight);
        GLSLUtil.setFloat(this, "InvShadowmapResolution", params.InvShadowmapResolution);
        GLSLUtil.setFloat(this, "InverseSquaredLightDistanceBiasScale", params.InverseSquaredLightDistanceBiasScale);

        GLSLUtil.setInt(this, "MinZ", params.MinZ);
        GLSLUtil.setFloat(this, "PhaseG", params.PhaseG);

        GLSLUtil.setFloat4(this, "ShadowInjectParams", params.ShadowInjectParams);
        GLSLUtil.setFloat4(this, "ShadowmapMinMax", params.ShadowmapMinMax);
        GLSLUtil.setFloat4(this, "StaticShadowBufferSize", params.StaticShadowBufferSize);
        GLSLUtil.setMat4(this, "ShadowViewProjectionMatrices", params.ShadowViewProjectionMatrices);

        GLSLUtil.setMat4(this, "UnjitteredClipToTranslatedWorld", params.UnjitteredClipToTranslatedWorld);
        GLSLUtil.setMat4(this, "UnjitteredPrevWorldToClip", params.UnjitteredPrevWorldToClip);
        GLSLUtil.setMat4(this, "ViewToVolumeClip", params.ViewToVolumeClip);
        GLSLUtil.setMat4(this, "WorldToShadowMatrix", params.WorldToShadowMatrix);
        GLSLUtil.setMat4(this, "WorldToStaticShadowMatrix", params.WorldToStaticShadowMatrix);
        GLSLUtil.setMat4(this, "g_ViewProj", params.g_ViewProj);
        GLSLUtil.setFloat3(this, "ViewForward", params.ViewForward);
        GLSLUtil.setFloat3(this, "View_PreViewTranslation", params.View_PreViewTranslation);
        GLSLUtil.setFloat4(this, "ViewSpaceBoundingSphere", params.ViewSpaceBoundingSphere);
        GLSLUtil.setFloat3(this, "VolumetricFog_GridSize", params.VolumetricFog_GridSize);
        GLSLUtil.setFloat3(this, "VolumetricFog_GridZParams", params.VolumetricFog_GridZParams);
        GLSLUtil.setFloat3(this, "WorldCameraOrigin", params.WorldCameraOrigin);
        GLSLUtil.setBool(this, "bStaticallyShadowed", params.bStaticallyShadowed);
        GLSLUtil.setFloat(this, "g_CameraFar", params.cameraFar);
        GLSLUtil.setFloat(this, "g_CameraNear", params.cameraNear);
    }
}
