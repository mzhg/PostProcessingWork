package jet.opengl.demos.gpupro.vct;

import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Transform;

import jet.opengl.postprocessing.shader.GLSLProgram;
import jet.opengl.postprocessing.shader.GLSLUtil;

/** A renderer that can be used to render a mesh. */
final class MeshRenderer {
    private static final String MODEL_MATRIX_NAME = "M";

    boolean enabled = true;
    boolean tweakable = false; // Automatically adds a window for this mesh renderer.
    String name = "Mesh renderer"; // Is displayed in the tweak bar.

    final Matrix4f combinedModel = new Matrix4f();
    final Transform transform = new Transform();
    Mesh mesh;
    MaterialSetting materialSetting;

    MeshRenderer(Mesh mesh){
        this.mesh = mesh;
    }

    MeshRenderer(Mesh mesh, MaterialSetting materialSetting){
        this.mesh = mesh;
        this.materialSetting = materialSetting;
    }

    void render(GLSLProgram program){
        transform.getMatrix(combinedModel);

        GLSLUtil.setMat4(program, MODEL_MATRIX_NAME, combinedModel);

        if(materialSetting != null)
            materialSetting.Upload(program, false);

        mesh.draw();
    }
}
