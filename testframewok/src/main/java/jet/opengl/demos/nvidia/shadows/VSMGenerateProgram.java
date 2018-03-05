package jet.opengl.demos.nvidia.shadows;

import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.ReadableVector3f;

import java.io.IOException;

import jet.opengl.postprocessing.shader.GLSLProgram;
import jet.opengl.postprocessing.util.CacheBuffer;

/**
 * Created by mazhen'gui on 2017/11/9.
 */

final class VSMGenerateProgram extends GLSLProgram{

//    uniform mat4 gWorld;
//    uniform mat4 gLightViewProj;
//    uniform vec3 gLightPos;
    private int mWorld;
    private int wLightViewProj;
    private int wLightPos;
    private int wLightView;

    VSMGenerateProgram(){
        final String path = "nvidia/ShadowWorks/shaders/";
        try {
            setSourceFromFiles(path + "GenerateVSM_VS.vert", path + "GenerateVSM_PS.frag");
        } catch (IOException e) {
            e.printStackTrace();
        }

        mWorld = getUniformLocation("gWorld");
        wLightViewProj = getUniformLocation("gLightViewProj");
        wLightView = getUniformLocation("gLightView");
        wLightPos = getUniformLocation("gLightPos");
    }

    void setWorld(Matrix4f mat) { if(mWorld >=0) gl.glUniformMatrix4fv(mWorld, false, CacheBuffer.wrap(mat));}
    void setLightViewProj(Matrix4f mat) { if(wLightViewProj >=0) gl.glUniformMatrix4fv(wLightViewProj, false, CacheBuffer.wrap(mat));}
    void setLightView(Matrix4f mat) { if(wLightView >=0) gl.glUniformMatrix4fv(wLightView, false, CacheBuffer.wrap(mat));}
    void setLightPos(ReadableVector3f pos) { if(wLightPos >=0) gl.glUniform3f(wLightPos, pos.getX(), pos.getY(), pos.getZ());}
}
