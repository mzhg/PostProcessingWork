package jet.opengl.demos.nvidia.waves.samples;

import java.io.IOException;

import jet.opengl.postprocessing.shader.GLSLProgram;
import jet.opengl.postprocessing.shader.Macro;
import jet.opengl.postprocessing.shader.ShaderLoader;
import jet.opengl.postprocessing.shader.ShaderSourceItem;
import jet.opengl.postprocessing.shader.ShaderType;

/**
 * Created by mazhen'gui on 2017/8/16.
 */

final class OceanSurfaceShadedProgram extends GLSLProgram{
    private IsUniformData uniformData;
    OceanSurfaceShadedProgram(){

        final String shader_path = "nvidia/WaveWorks/shaders/";
//        m_pRenderSurfaceShadedWithShorelinePass =GLSLProgram.createProgram(shader_path + "OceanWaveVS.vert", shader_path + "OceanWaveHS.gltc",
//                shader_path+"OceanWaveDS.glte", shader_path+"SolidWireGS.gemo", shader_path+"OceanWaveShorePS.frag",
//                CommonUtil.toArray(new Macro("GFSDK_WAVEWORKS_USE_TESSELLATION", 1)));

        CharSequence vert = null;
        CharSequence ctrl = null;
        CharSequence tess = null;
        CharSequence gemo = null;
        CharSequence frag = null;

        try {
            vert = ShaderLoader.loadShaderFile(shader_path + "OceanWaveVS.vert", false);
            ctrl = ShaderLoader.loadShaderFile(shader_path + "OceanWaveHS.gltc", false);
            tess = ShaderLoader.loadShaderFile(shader_path + "OceanWaveDS.glte", false);
            gemo = ShaderLoader.loadShaderFile(shader_path + "SolidWireGS.gemo", false);
            frag = ShaderLoader.loadShaderFile(shader_path + "OceanWaveShorePS.frag", false);
        } catch (IOException e) {
            e.printStackTrace();
        }

        final Macro[] macros = {new Macro("GFSDK_WAVEWORKS_USE_TESSELLATION", 1)};
        ShaderSourceItem items[] = new ShaderSourceItem[5];
        items[0] = new ShaderSourceItem(vert, ShaderType.VERTEX);
        items[1] = new ShaderSourceItem(ctrl, ShaderType.TESS_CONTROL);
        items[2] = new ShaderSourceItem(tess, ShaderType.TESS_EVAL);
        items[3] = new ShaderSourceItem(gemo, ShaderType.GEOMETRY);
        items[4] = new ShaderSourceItem(frag, ShaderType.FRAGMENT);

        for(ShaderSourceItem item : items){
            item.macros = macros;
        }

        setSourceFromStrings(items);
        uniformData = new IsUniformData(getName(), getProgram());
    }

    void setUniforms(IsParameters params){
        uniformData.setParameters(params);
    }
}
