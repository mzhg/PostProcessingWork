package jet.opengl.demos.intel.fluid.render;

import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.ReadableVector3f;

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

    void        applyRenderState(RenderStateS renderState );
    void        getRenderState( RenderStateS renderState );
    void        disableTexturing();

    /*VertexBufferBase    NewVertexBuffer() = 0 ;
    IndexBufferBase     NewIndexBuffer() = 0 ;
    void                DeleteIndexBuffer( IndexBufferBase * indexBuffer ) = 0 ;
    MeshBase            NewMesh( ModelData * owningModelData ) = 0 ;
    TextureBase         NewTexture() = 0 ;*/
}
