package jet.opengl.demos.nvidia.volumelight;

import jet.opengl.postprocessing.shader.Macro;
import jet.opengl.postprocessing.util.Pair;

final class PrecomputedLightLUT_IntelProgram extends BaseVLProgram{

    public PrecomputedLightLUT_IntelProgram(ContextImp_OpenGL context) {
        super(context);

        compileProgram();
        initUniformData();
    }

    @Override
    protected Object getParameter() {
        return "LightLUT_Intel";
    }

    @Override
    protected Pair<String, Macro[]> getPSShader() {
        return new Pair<>("Intel_LightLUT_PS.frag", null);
    }
}
