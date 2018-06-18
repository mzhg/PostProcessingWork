package jet.opengl.demos.intel.fluid.render;

import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.ReadableVector3f;

import jet.opengl.demos.intel.fluid.scene.ModelData;
import jet.opengl.demos.intel.fluid.scene.ModelNode;
import jet.opengl.demos.intel.fluid.utils.Camera;
import jet.opengl.demos.intel.fluid.utils.Viewport;

/**
 * Created by mazhen'gui on 2018/3/12.
 */

public interface ApiBase {
    public static final int sType = 1380012105 ; ///< Type identifier for this class

    // enum parameters
    public static final int sName = 0,
                            xView = 1,   ///< View matrix
                            NUM_PARAMETERS = 2;

//    Window *    NewWindow( System * renderSystem ) = 0 ;
    void        setViewport( Viewport viewport );
    void        setCamera( Camera xView );
    void        setLights( ModelNode lightReceiver );
    void        setLocalToWorld(Matrix4f localToWorld );

    void        renderSimpleText(CharSequence text , ReadableVector3f position , boolean useScreenSpace );

    void        applyRenderState(RenderState renderState );
    void        getRenderState( RenderState renderState );
    void        disableTexturing();

    VertexBufferBase    newVertexBuffer();
    IndexBufferBase     newIndexBuffer();
    void                deleteIndexBuffer( IndexBufferBase indexBuffer );
    MeshBase            newMesh( ModelData owningModelData );
    TextureBase         newTexture();
}
