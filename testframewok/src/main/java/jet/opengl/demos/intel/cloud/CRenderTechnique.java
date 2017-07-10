package jet.opengl.demos.intel.cloud;

import java.io.IOException;

import jet.opengl.postprocessing.shader.GLSLProgram;
import jet.opengl.postprocessing.shader.Macro;

/**
 * Created by mazhen'gui on 2017/7/7.
 */

final class CRenderTechnique extends GLSLProgram{

    public static final int START_TEXTURE_UNIT = 23;
    public static final int TEX2D_LIGHT_SPACE_DEPTH = START_TEXTURE_UNIT+0;
    public static final int TEX2D_CLOUD_TRANSPARENCY = START_TEXTURE_UNIT+1;
    public static final int TEX2D_CLOUD_MIN_MAX_DEPTH = START_TEXTURE_UNIT+2;
    public static final int TEX2D_CLOUD_DENSITY = START_TEXTURE_UNIT+3;
    public static final int TEX2D_WHITE_NOISE = START_TEXTURE_UNIT+4;
    public static final int TEX3D_NOISE = START_TEXTURE_UNIT+5;
    public static final int TEX2D_MAX_DENSITY = START_TEXTURE_UNIT+6;
    public static final int TEX3D_LIGHT_ATTEN_MASS = START_TEXTURE_UNIT+7;
    public static final int TEX3D_CELL_DENSITY = START_TEXTURE_UNIT+8;
    public static final int TEX2D_AMB_SKY_LIGHT = START_TEXTURE_UNIT+9;
    public static final int TEX3D_LIGHT_CLOUD_TRANSPARENCY = START_TEXTURE_UNIT+10;
    public static final int TEX3D_LIGHT_CLOUD_MIN_MAX_DEPTH = START_TEXTURE_UNIT+11;
    public static final int TEX3D_PARTICLE_DENSITY_LUT = START_TEXTURE_UNIT+12;
    public static final int TEX3D_SINGLE_SCATT_IN_PART_LUT = START_TEXTURE_UNIT+13;
    public static final int TEX3D_MULTIL_SCATT_IN_PART_LUT = START_TEXTURE_UNIT+14;

    CRenderTechnique(String filename, Macro[] macros){
        try {
            setSourceFromFiles("shader_libs/PostProcessingDefaultScreenSpaceVS.vert", "shader_libs/OutdoorSctr/" + filename, macros);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
