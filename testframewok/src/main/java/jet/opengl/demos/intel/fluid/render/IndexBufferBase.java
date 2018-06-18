package jet.opengl.demos.intel.fluid.render;

import java.nio.IntBuffer;
import java.nio.ShortBuffer;

/**
 * Index buffer base class.<p></p>

 Each platform should specialize this class<p></p>
 * Created by Administrator on 2018/3/13 0013.
 */
public abstract class IndexBufferBase {
    /*enum IndexTypeE
    {*/
    public static final int
        INDEX_TYPE_NONE=0 ///< Unassigned
        ,   INDEX_TYPE_16=1   ///< 16-bit integer (a.k.a "word" or "short")
        ,   INDEX_TYPE_32=2;   ///< 32-bit integer
//    } ;

    private int      mTypeId         ;   ///< Type identifier
    protected static final int sTypeId = 1230520930 ;    ///< Type identifier for index buffer

    protected int  mIndexType = INDEX_TYPE_NONE;   ///< Type of index data
    protected int  mNumIndices = 0;   ///< Number of indices, i.e. number of elements in index buffer.

    public IndexBufferBase( int typeId ) { mTypeId = typeId;}

    /// Return type of the underlying implementation for this index buffer object.
    public int getTypeId() { return mTypeId ; }

    /// Return type of index values.
    public int getIndexType() { return mIndexType ; }

    /// Return address of index data, as short integers.  Must call UnlockIndices afterwards.
    public abstract ShortBuffer getIndicesWord();

    /// Return address of index data, as integers.  Must call UnlockIndices afterwards.
    public abstract IntBuffer getIndicesInt();

    /// Unlock indices obtained by GetIndices*()
    public abstract void unlock() ;

    /// Return number of elements this index buffer represents.
    public int getNumIndices() { return mNumIndices ; }

    /// Platform-specific routine to allocate index buffer.
    public abstract void allocate( int numIndices , int indexType );

    /// Platform-specific routine to empty and release index buffer.
    public abstract void clear();
}
