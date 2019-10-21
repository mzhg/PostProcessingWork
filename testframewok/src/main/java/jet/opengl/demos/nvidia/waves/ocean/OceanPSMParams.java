package jet.opengl.demos.nvidia.waves.ocean;

import jet.opengl.postprocessing.common.GLFuncProviderFactory;
import jet.opengl.postprocessing.shader.GLSLProgram;

final class OceanPSMParams {
    int m_pPSMMapVariable;
    int m_pPSMSlicesVariable;
    int m_pPSMTintVariable;

    GLSLProgram m_prog;

    OceanPSMParams(GLSLProgram prog){
        m_prog = prog;

//        m_pPSMMapVariable = pFX->GetVariableByName("g_PSMMap")->AsShaderResource();
        m_pPSMSlicesVariable = prog.getUniformLocation("g_PSMSlices");
        m_pPSMTintVariable = prog.getUniformLocation("g_PSMTint");
        int location = prog.getUniformLocation("g_PSMMap");
        m_pPSMMapVariable = GLFuncProviderFactory.getGLFuncProvider().glGetUniformi(prog.getProgram(), location);
    }

}
