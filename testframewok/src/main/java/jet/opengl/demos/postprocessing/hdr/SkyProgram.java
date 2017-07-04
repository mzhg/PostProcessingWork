package jet.opengl.demos.postprocessing.hdr;

import org.lwjgl.util.vector.Matrix4f;

import java.io.IOException;

import jet.opengl.postprocessing.shader.GLSLProgram;
import jet.opengl.postprocessing.util.CacheBuffer;

/**
 * Created by mazhen'gui on 2017/3/16.
 */

final class SkyProgram extends GLSLProgram{
    private int viewMatIndex;
    private int projMatIndex;
    private int attribPos;

    public SkyProgram() throws IOException {
//        CharSequence vert = NvAssetLoader.readText("hdr_shaders/skyBox.vert");
//        CharSequence frag = NvAssetLoader.readText("hdr_shaders/skyBox.frag");
//
//        NvGLSLProgram program = new NvGLSLProgram();
//        setSourceFromStrings(vert, frag, true);
//        programID = getProgram();
        setSourceFromFiles("HDR/shaders/skyBox.vert", "HDR/shaders/skyBox.frag");

        int envMap      = getUniformLocation("envMap");
        viewMatIndex    = getUniformLocation("viewMatrix");
        projMatIndex    = getUniformLocation("ProjMatrix");
        attribPos = getAttribLocation("PosAttribute");

        enable();
        gl.glUniform1i(envMap, 0);
        gl.glUseProgram(0);
    }

    public void applyViewMat(Matrix4f mat){gl.glUniformMatrix4fv(viewMatIndex, false, CacheBuffer.wrap(mat)); }
    public void applyProjMat(Matrix4f mat){gl.glUniformMatrix4fv(projMatIndex, false, CacheBuffer.wrap(mat)); }

    public int getAttribPosition() {
        return attribPos;
    }
}
