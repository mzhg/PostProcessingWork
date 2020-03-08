package jet.opengl.demos.gpupro.culling;

import org.lwjgl.util.vector.Vector4i;

import jet.opengl.postprocessing.shader.GLSLProgram;

final class RenderInput {

    /** Weather clearing the framebuffer in the current rendering */
    boolean clearFBO;

    PickType pickType = PickType.None;
    PickedRenderType pickedRenderType = PickedRenderType.None;
    final Vector4i pickRect = new Vector4i();
    /** Weather writing color to the color buffer. */
    boolean writeFBO;

    /**  */
    GLSLProgram transparencyRenderProg;


}
