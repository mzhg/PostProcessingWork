package jet.opengl.demos.intel.fluid.render;

import java.nio.ByteBuffer;

/**
 * Created by Administrator on 2018/3/13 0013.
 */

public abstract class VertexBufferBase {

    protected static final int sTypeId = 1448624710; // 'VXBF' ;    ///< Type identifier for VertexBufferBase

    protected int mTypeId             ;   ///< Type identifier, for run-time type checking

    private GenericVertex[]   mGenericVertexData  ;   ///< Address of generic vertex data.  Mutually exclusive with any platform-specific vertex data.
    private final VertexDeclaration   mVertexDeclaration = new VertexDeclaration();   ///< Declaration of vertex format
    private int          mVertexSize         ;   ///< Size, in bytes, of a single vertex -- Stride between elements in mVertexData.
    private int          mPopulation         ;   ///< Number of vertices in buffer (i.e. actual population).
    private int          mCapacity           ;   ///< Number of vertices this buffer can hold.

    public VertexBufferBase(){}

    /// Return type identifier for this object.
    public int getTypeId() { return mTypeId ; }

    /// Platform-specific routine to format vertex buffer.
    public abstract void declareVertexFormat(VertexDeclaration vertexDeclaration );

    /// Return reference to object that declares format for vertices in this buffer.
    public VertexDeclaration hetVertexDeclaration() { return mVertexDeclaration ; }

    /// Return size, in bytes, of each vertex occupies.
    public int getVertexSizeInBytes() { return mVertexSize ; }

    public GenericVertex[]  allocateGeneric( int numVertices ){
        assert ( null == mGenericVertexData ) ;
        assert ( VertexDeclaration.VertexFormatE.VERTEX_FORMAT_NONE == mVertexDeclaration.mVertexFormat ) ;
        assert ( 0 == getVertexSizeInBytes() ) ;
        assert ( 0 == getPopulation() ) ;
        assert ( 0 == getCapacity() ) ;
        assert ( numVertices > 0 ) ;

        setCapacity( numVertices ) ;

        mGenericVertexData = new GenericVertex[ numVertices ] ;

        setVertexDeclaration( new VertexDeclaration(VertexDeclaration.VertexFormatE.GENERIC ) ) ;
        setVertexSize( GenericVertex.SIZE ) ;

        return mGenericVertexData ;
    }

    /// Platform-specific routine to allocate vertex buffer.
    public abstract boolean  allocate( int numVertices );

    /// Platform-specific routine to acquire a lock on vertex data and return its starting address.
    public abstract ByteBuffer  lockVertexData();

    /// Platform-specific routine to unlock vertex data previously locked by LockVertexData.
    public abstract void    unlockVertexData();

    /// Platform-specific routine to obtain address of first of a given element.
    /// If the vertex format has more than one element with the given semantic, which indicates which one to obtain.
    public abstract ByteBuffer getElementStart(ByteBuffer vertexData , VertexDeclaration.SemanticE semantic , int which );

    /// Platform-specific routine to translate vertices from generic intermediate format to one ready to render.
    public abstract void translateFromGeneric(VertexDeclaration targetVertexDeclaration );

    /// Platform-specific routine to empty and release vertex buffer.
    public abstract void clear();

    /** Change capacity of this vertex buffer.<p></p>

     This empties the current contents of the vertex buffer and reallocates an empty buffer of the given capacity.<p></p>

     <b>Note:</b>   This can block if the GPU currently holds a lock on the resources associated with this vertex buffer.
     */
    public boolean changeCapacityAndReallocate( int numVertices ){
        assert ( numVertices > getCapacity() ) ; // No good reason to call this if capacity is not increasing.  But technically this is not a required precondition.
        deallocate() ;
        return allocate( numVertices ) ;
    }

    public void setPopulation( int numVertices ){
        assert ( numVertices <= getCapacity() ) ;
        mPopulation = numVertices ;
    }

    /// Return number of vertices in this buffer (i.e. actual population).
    public int  getPopulation() { return mPopulation       ; }

    /// Return number of vertices this buffer can hold.
    public int  getCapacity() { return mCapacity ; }

    protected abstract void deallocate();

    protected GenericVertex[] getGenericVertexData()        { return mGenericVertexData ; }

    protected void    setVertexDeclaration(VertexDeclaration vertexDeclaration ){
        assert ( mVertexDeclaration.isInvalid() ) ;  // Not allowed to change vertex format after it was set.
        mVertexDeclaration.set(vertexDeclaration);
    }

    protected void    setVertexSize( int vertexSize ){
        assert ( ( 0 == mVertexSize ) || ( 0 == vertexSize ) ) ;    // Not allowed to change vertex size; it is determined by vertex declaration anyway.
        mVertexSize = vertexSize ;
    }
    protected void    setCapacity( int capacity ){
        assert  ( ( 0 == mCapacity ) || ( 0 == capacity ) ) ;  // Not allowed to change capacity without reallocating vertex buffer.
        mCapacity = capacity ;
    }
}
