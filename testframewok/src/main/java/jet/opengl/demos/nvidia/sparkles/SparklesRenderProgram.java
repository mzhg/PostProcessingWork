package jet.opengl.demos.nvidia.sparkles;

import java.io.IOException;

import jet.opengl.postprocessing.shader.GLSLProgram;
import jet.opengl.postprocessing.shader.ShaderLoader;
import jet.opengl.postprocessing.shader.ShaderSourceItem;
import jet.opengl.postprocessing.shader.ShaderType;

/**
 * Created by mazhen'gui on 2017/10/21.
 */

public final class SparklesRenderProgram extends GLSLProgram {

    public SparklesRenderProgram(){
        final String path = "nvidia/sparkles/";
        ShaderSourceItem vs_item = new ShaderSourceItem();
        ShaderSourceItem gs_item = null;
        ShaderSourceItem ps_item = null;

        try {
            vs_item.source = ShaderLoader.loadShaderFile(path + "SparklesVS.vert", false);
            vs_item.type = ShaderType.VERTEX;

            gs_item = new ShaderSourceItem();
            gs_item.source = ShaderLoader.loadShaderFile(path + "SparklesGS.gemo", false);
            gs_item.type = ShaderType.GEOMETRY;

            ps_item = new ShaderSourceItem();
            ps_item.source = ShaderLoader.loadShaderFile(path + "SparklesPS.frag", false);
            ps_item.type = ShaderType.FRAGMENT;
        } catch (IOException e) {
            e.printStackTrace();
        }
        setSourceFromStrings(vs_item, gs_item, ps_item);
    }
}
