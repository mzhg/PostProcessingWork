package jet.opengl.demos.gpupro.vct;

import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Transform;

import jet.opengl.postprocessing.shader.GLSLProgram;

/** A renderer that can be used to render a mesh. */
final class MeshRenderer {

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

        // TODO setup the model matrix.
        if(materialSetting != null)
            materialSetting.Upload(program, false);

        mesh.draw();
    }
}
