package jet.opengl.demos.intel.fluid.ogl;

import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.ReadableVector3f;

import jet.opengl.demos.intel.fluid.render.ApiBase;
import jet.opengl.demos.intel.fluid.render.IndexBufferBase;
import jet.opengl.demos.intel.fluid.render.MeshBase;
import jet.opengl.demos.intel.fluid.render.RenderState;
import jet.opengl.demos.intel.fluid.render.TextureBase;
import jet.opengl.demos.intel.fluid.render.VertexBufferBase;
import jet.opengl.demos.intel.fluid.scene.ModelData;
import jet.opengl.demos.intel.fluid.scene.ModelNode;
import jet.opengl.demos.intel.fluid.utils.Camera;
import jet.opengl.demos.intel.fluid.utils.Viewport;

/**
 * Created by Administrator on 2018/4/5 0005.
 */

public class OpenGL_Api implements ApiBase {
    @Override
    public void setViewport(Viewport viewport) {

    }

    @Override
    public void setCamera(Camera xView) {

    }

    @Override
    public void setLights(ModelNode lightReceiver) {

    }

    @Override
    public void setLocalToWorld(Matrix4f localToWorld) {

    }

    @Override
    public void renderSimpleText(CharSequence text, ReadableVector3f position, boolean useScreenSpace) {

    }

    @Override
    public void applyRenderState(RenderState renderState) {

    }

    @Override
    public void getRenderState(RenderState renderState) {

    }

    @Override
    public void disableTexturing() {

    }

    @Override
    public VertexBufferBase newVertexBuffer() {
        return null;
    }

    @Override
    public IndexBufferBase newIndexBuffer() {
        return null;
    }

    @Override
    public void deleteIndexBuffer(IndexBufferBase indexBuffer) {

    }

    @Override
    public MeshBase newMesh(ModelData owningModelData) {
        return null;
    }

    @Override
    public TextureBase newTexture() {
        return null;
    }
}
