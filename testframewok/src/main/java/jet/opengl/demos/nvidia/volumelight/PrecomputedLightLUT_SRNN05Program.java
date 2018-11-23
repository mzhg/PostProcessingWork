package jet.opengl.demos.nvidia.volumelight;

import jet.opengl.postprocessing.shader.Macro;
import jet.opengl.postprocessing.util.Pair;

final class PrecomputedLightLUT_SRNN05Program extends BaseVLProgram{

    public PrecomputedLightLUT_SRNN05Program(ContextImp_OpenGL context) {
        super(context);

        compileProgram();
        initUniformData();
    }

    @Override
    protected Object getParameter() {
        return "LightLUT_SRNN05";
    }

    @Override
    protected Pair<String, Macro[]> getPSShader() {
        return new Pair<>("SRNN05_LightLUT_PS.frag", null);
    }
}
