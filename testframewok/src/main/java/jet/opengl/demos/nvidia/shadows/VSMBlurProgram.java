package jet.opengl.demos.nvidia.shadows;

import java.io.IOException;

import jet.opengl.postprocessing.shader.GLSLProgram;

/**
 * Created by mazhen'gui on 2017/11/9.
 */

final class VSMBlurProgram extends GLSLProgram {

//    uniform bool bVertical = false;
//    uniform float fStepSize = 1.0f;
//    uniform float fFilterWidth = 8;
    private int bVertical;
    private int fStepSize;
    private int fFilterWidth;

    VSMBlurProgram(){
        final String path = "nvidia/ShadowWorks/shaders/";
        try {
            setSourceFromFiles("shader_libs/PostProcessingDefaultScreenSpaceVS.vert", path+"SeparableBlurPS.frag");
        } catch (IOException e) {
            e.printStackTrace();
        }

        bVertical = getUniformLocation("bVertical");
        fStepSize = getUniformLocation("fStepSize");
        fFilterWidth = getUniformLocation("fFilterWidth");
    }

    void setVertical(boolean flag) { if(bVertical >=0) gl.glUniform1i(bVertical, flag?1:0);}
    void setStepSize(float stepSize) { if(fStepSize >=0) gl.glUniform1f(fStepSize, stepSize);}
    void setFilterWidth(float filterWidth) { if(fFilterWidth >=0) gl.glUniform1f(fFilterWidth, filterWidth);}
}
