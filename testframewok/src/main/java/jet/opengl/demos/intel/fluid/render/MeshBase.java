package jet.opengl.demos.intel.fluid.render;

import org.lwjgl.util.vector.ReadableVector3f;

import jet.opengl.demos.intel.fluid.scene.ModelData;

/**
 * Created by Administrator on 2018/3/13 0013.
 */

public abstract class MeshBase {

    private  static final int
            PRIMITIVE_NONE              = 0,
            PRIMITIVE_POINTS            = 1,
            PRIMITIVE_LINES             = 2,
            PRIMITIVE_TRIANGLES         = 3,
            PRIMITIVE_TRIANGLE_STRIP    = 4,
            PRIMITIVE_QUADS             = 5,
            PRIMITIVE_NUM               = 6;

    private ModelData           mOwningModelData    ;   ///< ModelData object that owns this mesh.
    private Technique           mTechnique          ;   ///< Technique used to render this mesh.
    private VertexBufferBase    mVertexBuffer       ;   ///< Vertex data.
    private IndexBufferBase     mIndexBuffer        ;   ///< Index data.
    private int              mPrimitiveType   = PRIMITIVE_NONE;   ///< Type of primitive shape this mesh uses.

    public MeshBase( ModelData owningModelData ){
        mOwningModelData = owningModelData;
    }

    /// Platform-specific method to render geometry mesh.
    public abstract void render();

    /// Set render technique (material) used to render this mesh.
    public void setTechnique( Technique technique )
    {
        mTechnique = technique ;
    }

    public Technique getTechnique() { return mTechnique; }

    public void setPrimitiveType( int primitiveType ){
        assert ( ( PRIMITIVE_NONE == mPrimitiveType ) || ( primitiveType == mPrimitiveType ) ) ; // Not allowed to change format once set.
        mPrimitiveType = primitiveType ;
    }
    public int getPrimitiveType() { return mPrimitiveType ; }

    public VertexBufferBase newVertexBuffer( ApiBase renderApi ){
        assert ( renderApi != null) ;
        assert ( null == mVertexBuffer ) ;

        // Create vertex buffer
        mVertexBuffer = renderApi.newVertexBuffer() ;
        return mVertexBuffer ;
    }

    /// Return address of buffer for vertices that define this mesh.
    public VertexBufferBase getVertexBuffer()       { return mVertexBuffer ; }

    /// Return address of buffer for indices indicating edges between vertices in this mesh.
    public IndexBufferBase getIndexBuffer()        { return mIndexBuffer  ; }

    public void makeBox(ApiBase renderApi , ReadableVector3f dimensions , VertexDeclaration.VertexFormatE vertexFormat , int primitiveType /*= PRIMITIVE_TRIANGLES*/ ,
                        boolean useIndices /*= false*/ ){
        throw new UnsupportedOperationException();
    }

    public void makeSphere( ApiBase renderApi , float radius , int numLatitudinalSegment , int numLongitudinalSegments , VertexDeclaration.VertexFormatE vertexFormat ){
        throw new UnsupportedOperationException();
    }
}
