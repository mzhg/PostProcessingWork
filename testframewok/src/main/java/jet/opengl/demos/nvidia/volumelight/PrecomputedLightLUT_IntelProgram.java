package jet.opengl.demos.nvidia.volumelight;

import jet.opengl.postprocessing.shader.Macro;
import jet.opengl.postprocessing.util.CommonUtil;
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
        Macro[] macros = {
                new Macro("ATTENUATIONMODE_NONE", RenderVolumeDesc.ATTENUATIONMODE_NONE),
                new Macro("ATTENUATIONMODE_POLYNOMIAL", RenderVolumeDesc.ATTENUATIONMODE_POLYNOMIAL),
                new Macro("ATTENUATIONMODE_INV_POLYNOMIAL", RenderVolumeDesc.ATTENUATIONMODE_INV_POLYNOMIAL),
                new Macro("ATTENUATIONMODE", "ATTENUATIONMODE_INV_POLYNOMIAL"),
        };
        return new Pair<>("Intel_LightLUT_PS.frag", macros);
    }
}
