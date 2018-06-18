package jet.opengl.demos.intel.fluid.utils;

import org.lwjgl.util.vector.ReadableVector3f;
import org.lwjgl.util.vector.Vector3f;

import java.util.ArrayList;

import jet.opengl.postprocessing.util.Numeric;
import jet.opengl.postprocessing.util.RecycledPool;
import jet.opengl.postprocessing.util.StackFloat;

/**
 * Templated container for fast spatial lookups and insertions.<p></p>
 * Created by Administrator on 2018/6/2 0002.
 */

public class UniformGridFloat extends UniformGridGeometry{

    private final StackFloat mContents = new StackFloat(8);   ///< 3D array of items.

    /** Construct an empty UniformGrid.
     \see Initialize
     */
    public UniformGridFloat() { }


    /** Construct a uniform grid container that fits the given geometry.
     \see Initialize
     */
    public UniformGridFloat(int uNumElements , ReadableVector3f vMin , ReadableVector3f vMax , boolean bPowerOf2 )
    {
        super( uNumElements , vMin , vMax , bPowerOf2 );
    }


    /** Copy shape from given uniform grid.
     */
    public UniformGridFloat(UniformGridGeometry that )
    {
        super( that );
    }


    /** Copy constructor for empty uniform grids.

     This copy constructor does not copy contained contents.  A proper
     copy ctor should deep-copy all of its data in order to operate with
     containers, in particular STL vector) when it reallocates arrays.
     But reallocating these containers would entail massive memory moves,
     which although possible, would be inefficient.  Meanwhile, NestedGrid
     also pushes UniformGrids onto a vector, which requires using a copy
     ctor to initialize the new element.  So that code makes empty elements
     to push onto the vector, and arrange for this copy ctor to handle those
     empties properly.

     Meanwhile we want to catch any unintentional copies of actual data,
     so this method catches any attempt to copy populated UniformGrid objects.
     */
    public UniformGridFloat(UniformGridFloat that )
//            : UniformGridGeometry( that )
    {
        super(that);
        //ASSERT( that.Empty() ) ; // Maybe you meant to use UniformGrid( const UniformGridGeometry & ) instead.
//        this->operator=( that ) ;
        set(that);
    }

    public UniformGridFloat set(UniformGridFloat that )
    {
        if( this != that )
        {
            super.set( that ) ;
            // Note, in MSVC 7.1 (.NET) for vector-of-vectors, this seems to corrupt the original vector.
//            mContents = that.mContents ;
            mContents.clear();
            mContents.addAll(that.mContents);
        }
        return this ;
    }

    /// Return contents

/*#   if _MSC_VER && ( _MSC_VER < 1700 ) // Older compiler...
    ItemT *       Data()       { return *//* mContents.Empty() ? 0 : *//* & mContents[ 0 ] ; }
        const ItemT *       Data() const { return *//* mContents.Empty() ? 0 : *//* & mContents[ 0 ] ; }
#   else
    ItemT *       Data()       { return mContents.Data() ; }
        const ItemT *       Data() const { return mContents.Data() ; }
#   endif*/


    /// Return item at given offset, where offset should come from OffsetOfPosition or OffsetFromIndices.
    /*ItemT &       operator[]( final int & offset )       { ASSERT( offset < Size() ) ; return mContents[ offset ] ; }
        const ItemT &       operator[]( final int & offset ) const { ASSERT( offset < Size() ) ; return mContents[ offset ] ; }*/
    public float get(int offset) { return mContents.get(offset);}
    public float set(int offset, float value) { return mContents.set(offset, value);}

    /// Return item at given indices.
//    ItemT &       operator[]( final int indices[] )    { return mContents[ OffsetFromIndices( indices ) ] ; }
    public float get(int[] indices) { return mContents.get(offsetFromIndices( indices ));}

    /// Return item at given indices.
    public float       get( int ix , int iy , int iz )       { return mContents.get(offsetFromIndices( ix , iy , iz )) ; }
//        const ItemT &       Get( size_t ix , size_t iy , size_t iz ) const { return mContents[ OffsetFromIndices( ix , iy , iz ) ] ; }

    /// Return item at given position.
    public float        get( ReadableVector3f vPosition )        { return mContents.get(offsetOfPosition( vPosition )) ; }
//        const ItemT &       operator[]( const Vec3 & vPosition ) const  { return mContents[ OffsetOfPosition( vPosition ) ] ; }

//    bool operator==( const UniformGrid & that ) const
    @Override
    public boolean equals(Object obj)
    {
        if( !super.equals(obj) )
        {   // Geometries do not match, so these grids are not equal.
            return false ;
        }

        if(obj instanceof UniformGridFloat) {
            UniformGridFloat that = (UniformGridFloat)obj;
            return mContents.equals(that.mContents);
        }

        return false;
    }




    /** Scale each value in this grid by the given scalar.

     \param scale - amount by which to scale each value in this grid

     */
    public void scale( float scale )
    {
        final int numCells = getGridCapacity() ;
        float[] data = mContents.getData();
        for( int offset = 0 ; offset < numCells ; ++ offset )
        {
            data[offset] *= scale;

            /*float rVal = get(offset) ;
            rVal *= scale ;

            set(offset, rVal);*/
        }
    }

    /** Initialize contents to whatever default ctor provides.
     */
    public void init(float initialValue)
    {
//        PERF_BLOCK( UniformGrid__Init ) ;

        // First clear because Resize only assigns values to new items.
        mContents.clear() ;
        mContents.resize( getGridCapacity() , initialValue ); ;
    }

    public final void init(){ init(0.0f);}

    @Override
    public void defineShape( int uNumElements , ReadableVector3f vMin , ReadableVector3f vMax , boolean bPowerOf2 )
    {
        mContents.clear() ;
        super.defineShape( uNumElements , vMin , vMax , bPowerOf2 ) ;
    }


    /** Return the number of cells in this grid which have been assigned values.
     */
    public int size() { return mContents.size() ; }


    /** Return whether this container contains any items.
     */
    public boolean empty()  { return mContents.isEmpty() ; }


    /** Create an empty container based on a given uniform grid container, but with lower resolution.

     \param src - Source UniformGridGeometry upon which to base dimensions of this container

     \param iDecimation - amount by which to reduce resolution (number of grid cells in each dimension).
     Typically this would be 2.

     */
    @Override
    public void decimate( UniformGridGeometry src , int iDecimation )
    {
        super.decimate( src , iDecimation ) ;
    }


//    static void UnitTest() ;


    /** Compute statistics of data in a uniform grid.

     \param min - minimum of all values in grid.  Caller must initialize to large values before calling this routine.

     \param max - maximum of all values in grid.  Caller must initialize to smale values before calling this routine.

     */
    public long computeStatistics( /*ItemT & min , ItemT & max*/ )
    {
        float max, min;
        max = min = get(0) ;
        final int numCells = getGridCapacity() ;
        for( int offset = 0 ; offset < numCells ; ++ offset )
        {
           final float rVal = get( offset ) ;
            min = Math.min( min , rVal ) ;
            max = Math.max( max , rVal ) ;
        }

        return Numeric.encode(Float.floatToRawIntBits(min), Float.floatToRawIntBits(max));
    }



    /** Interpolate values from grid to get value at given position.

     \param vResult      Interpolated value corresponding to value of grid contents at vPosition.

     \param vPosition    Position to sample.
     */
    public float interpolate( ReadableVector3f vPosition )
    {
        float  vResult;
        int[]  indices = new int[4] ; // Indices of grid cell containing position.
//        DEBUG_ONLY( UniformGridGeometry::sInterpolating = true ) ;
        indicesOfPosition( indices , vPosition ) ;
//        DEBUG_ONLY( UniformGridGeometry::sInterpolating = false ) ;
        assert ( indices[0] < getNumCells( 0 ) ) ;
        assert ( indices[1] < getNumCells( 1 ) ) ;
        assert ( indices[2] < getNumCells( 2 ) ) ;
        Vector3f vMinCorner = new Vector3f();
        positionFromIndices( vMinCorner , indices ) ;
        final Vector3f      vDiff         = Vector3f.sub(vPosition, vMinCorner, null) ; // Relative location of position within its containing grid cell.
        final Vector3f      tween         = new Vector3f( vDiff.x * getCellsPerExtent().x , vDiff.y * getCellsPerExtent().y , vDiff.z * getCellsPerExtent().z ) ;
        final Vector3f      oneMinusTween = new Vector3f( 1.0f - tween.x, 1.0f - tween.y, 1.0f - tween.z)  ;
        final int     numXY         = getNumPoints( 0 ) * getNumPoints( 1 ) ;
        final int    offsetX0Y0Z0  = offsetFromIndices( indices ) ;
        final int    offsetX1Y0Z0  = offsetX0Y0Z0 + 1 ;
        final int    offsetX0Y1Z0  = offsetX0Y0Z0 + getNumPoints(0) ;
        final int    offsetX1Y1Z0  = offsetX0Y0Z0 + getNumPoints(0) + 1 ;
        final int    offsetX0Y0Z1  = offsetX0Y0Z0 + numXY ;
        final int    offsetX1Y0Z1  = offsetX0Y0Z0 + numXY + 1 ;
        final int    offsetX0Y1Z1  = offsetX0Y0Z0 + numXY + getNumPoints(0) ;
        final int    offsetX1Y1Z1  = offsetX0Y0Z0 + numXY + getNumPoints(0) + 1 ;
        vResult =     ( ( oneMinusTween.x * get(offsetX0Y0Z0)
                +         tween.x * get(offsetX1Y0Z0) ) * oneMinusTween.y
            + ( oneMinusTween.x * get(offsetX0Y1Z0)
            +         tween.x * get(offsetX1Y1Z0) ) * tween.y        ) * oneMinusTween.z
            + ( ( oneMinusTween.x * get(offsetX0Y0Z1)
            +         tween.x * get(offsetX1Y0Z1) ) * oneMinusTween.y
            + ( oneMinusTween.x * get(offsetX0Y1Z1)
            +         tween.x * get(offsetX1Y1Z1) ) * tween.y        ) * tween.z ;

        return vResult;
    }



    /** Interpolate values from grid to get value at given position.

     \param vResult      Interpolated value corresponding to value of grid contents at vPosition.

     \param vPosition    Position to sample.
     */
    public float interpolateConditionally(float result, ReadableVector3f vPosition )
    {
        int[]        indices = new int[4] ; // Indices of grid cell containing position.
//        DEBUG_ONLY( UniformGridGeometry::sInterpolating = true ) ;
        indicesOfPosition( indices , vPosition ) ;
//        DEBUG_ONLY( UniformGridGeometry::sInterpolating = false ) ;
        assert ( indices[0] < getNumCells( 0 ) ) ;
        assert ( indices[1] < getNumCells( 1 ) ) ;
        assert ( indices[2] < getNumCells( 2 ) ) ;
        Vector3f            vMinCorner = new Vector3f();
        positionFromIndices( vMinCorner , indices ) ;
        final Vector3f      vDiff         = Vector3f.sub(vPosition ,vMinCorner, null) ; // Relative location of position within its containing grid cell.
        final Vector3f      tween         = new Vector3f( vDiff.x * getCellsPerExtent().x , vDiff.y * getCellsPerExtent().y , vDiff.z * getCellsPerExtent().z ) ;
        final Vector3f      oneMinusTween = new Vector3f( 1.0f - tween.x, 1.0f - tween.y, 1.0f - tween.z) ;
        final int    numXY         = getNumCells( 0 ) * getNumCells( 1 ) ;
        final int    offsetX0Y0Z0  = offsetFromIndices( indices ) ;
        final int    offsetX1Y0Z0  = offsetX0Y0Z0 + 1 ;
        final int    offsetX0Y1Z0  = offsetX0Y0Z0 + getNumCells(0) ;
        final int    offsetX1Y1Z0  = offsetX0Y0Z0 + getNumCells(0) + 1 ;
        final int    offsetX0Y0Z1  = offsetX0Y0Z0 + numXY ;
        final int    offsetX1Y0Z1  = offsetX0Y0Z0 + numXY + 1 ;
        final int    offsetX0Y1Z1  = offsetX0Y0Z0 + numXY + getNumCells(0) ;
        final int    offsetX1Y1Z1  = offsetX0Y0Z0 + numXY + getNumCells(0) + 1 ;
        float weightSum = 0.0f ;
        final float[] data = mContents.getData();
        if( ! Float.isNaN( data[offsetX0Y0Z0] ) ) { result += oneMinusTween.x * oneMinusTween.y * oneMinusTween.z * data[ offsetX0Y0Z0 ] ; weightSum += oneMinusTween.x * oneMinusTween.y * oneMinusTween.z ; }
        if( ! Float.isNaN( data[ offsetX1Y0Z0 ] ) ) { result +=         tween.x * oneMinusTween.y * oneMinusTween.z * data[ offsetX1Y0Z0 ] ; weightSum +=         tween.x * oneMinusTween.y * oneMinusTween.z ; }
        if( ! Float.isNaN( data[ offsetX0Y1Z0 ] ) ) { result += oneMinusTween.x *         tween.y * oneMinusTween.z * data[ offsetX0Y1Z0 ] ; weightSum += oneMinusTween.x *         tween.y * oneMinusTween.z ; }
        if( ! Float.isNaN( data[ offsetX1Y1Z0 ] ) ) { result +=         tween.x *         tween.y * oneMinusTween.z * data[ offsetX1Y1Z0 ] ; weightSum +=         tween.x *         tween.y * oneMinusTween.z ; }
        if( ! Float.isNaN( data[ offsetX0Y0Z1 ] ) ) { result += oneMinusTween.x * oneMinusTween.y *         tween.z * data[ offsetX0Y0Z1 ] ; weightSum += oneMinusTween.x * oneMinusTween.y * oneMinusTween.z ; }
        if( ! Float.isNaN( data[ offsetX1Y0Z1 ] ) ) { result +=         tween.x * oneMinusTween.y *         tween.z * data[ offsetX1Y0Z1 ] ; weightSum +=         tween.x * oneMinusTween.y *         tween.z ; }
        if( ! Float.isNaN( data[ offsetX0Y1Z1 ] ) ) { result += oneMinusTween.x *         tween.y *         tween.z * data[ offsetX0Y1Z1 ] ; weightSum += oneMinusTween.x *         tween.y *         tween.z ; }
        if( ! Float.isNaN( data[ offsetX1Y1Z1 ] ) ) { result +=         tween.x *         tween.y *         tween.z * data[ offsetX1Y1Z1 ] ; weightSum +=         tween.x *         tween.y *         tween.z ; }
        if( 0.0f == weightSum )
        {
//            (float&) result = UNIFORM_GRID_INVALID_VALUE ;
            result = Float.NaN;
        }
        else
        {
            assert ( ( weightSum > 0.0f ) && ( weightSum <= 1.0f ) ) ;
            result /= weightSum ;
            assert ( ! Float.isNaN( result ) && ! Float.isInfinite( result ) ) ;
        }

        return result;
    }



    /** Interpolate values from grid to get value at given position.

     \param vResult  Interpolated value corresponding to value of grid contents at vPosition.

     \param vPosition Position to sample.

     \note   This version assumes the floating-point-control-word is set
     to "truncate" -- which the caller must do by calling
     Changex87FloatingPointToTruncate or equivalent, and
     afterwords, typically the caller should call
     SetFloatingPointControlWord to return the FPCW back to "rounding" (the
     default) afterwards.  This optimization is specific to
     Intel chipsets, and dramatically improves speed.  Since
     this interpolation is one of the hotest spots for particle
     advection, which in turn is one of the hotest spots in a
     particle simulation, this kind of manually tuning is
     worthwhile.

     \see    Changex87FloatingPointToTruncate, IndicesOfPosition_AssumesFpcwSetToTruncate.

     */
    public float interpolate_AssumesFpcwSetToTruncate( ReadableVector3f vPosition )
    {
        float vResult;
        final int[]        indices = new int[4] ; // Indices of grid cell containing position.

//        DEBUG_ONLY( UniformGridGeometry::sInterpolating = true ) ;
        indicesOfPosition_AssumesFpcwSetToTruncate( indices , vPosition ) ;
//        DEBUG_ONLY( UniformGridGeometry::sInterpolating = false ) ;

        assert ( indices[0] < getNumCells( 0 ) ) ;
        assert ( indices[1] < getNumCells( 1 ) ) ;
        assert ( indices[2] < getNumCells( 2 ) ) ;
        final Vector3f vMinCorner = new Vector3f();
        positionFromIndices( vMinCorner , indices ) ;
        final int    offsetX0Y0Z0  = offsetFromIndices( indices ) ;
        final Vector3f      vDiff         = Vector3f.sub(vPosition, vMinCorner, null) ; // Relative location of position within its containing grid cell.
        final Vector3f      tween         = new Vector3f( vDiff.x * getCellsPerExtent().x , vDiff.y * getCellsPerExtent().y , vDiff.z * getCellsPerExtent().z ) ;
        final Vector3f      oneMinusTween = new Vector3f( 1.0f - tween.x, 1.0f - tween.y, 1.0f - tween.z);
        final int    numXY         = getNumCells( 0 ) * getNumCells( 1 ) ;
        final int    offsetX1Y0Z0  = offsetX0Y0Z0 + 1 ;
        final int    offsetX0Y1Z0  = offsetX0Y0Z0 + getNumCells(0) ;
        final int    offsetX1Y1Z0  = offsetX0Y1Z0 + 1 ;
        final int    offsetX0Y0Z1  = offsetX0Y0Z0 + numXY ;
        final int    offsetX1Y0Z1  = offsetX0Y0Z1 + 1 ;
        final int    offsetX0Y1Z1  = offsetX0Y0Z0 + numXY + getNumCells(0) ;
        final int    offsetX1Y1Z1  = offsetX0Y1Z1 + 1 ;
        final float[] data = mContents.getData();
        vResult =     ( ( oneMinusTween.x * data[ offsetX0Y0Z0 ]
                +         tween.x * data[ offsetX1Y0Z0 ] ) * oneMinusTween.y
            + ( oneMinusTween.x * data[ offsetX0Y1Z0 ]
            +         tween.x * data[ offsetX1Y1Z0 ] ) * tween.y        ) * oneMinusTween.z
            + ( ( oneMinusTween.x * data[ offsetX0Y0Z1 ]
            +         tween.x * data[ offsetX1Y0Z1 ] ) * oneMinusTween.y
            + ( oneMinusTween.x * data[ offsetX0Y1Z1 ]
            +         tween.x * data[ offsetX1Y1Z1 ] ) * tween.y        ) * tween.z ;

        return vResult;
    }



    /** Accumulate given value into grid at given position.

     \param vPosition - position of a "source" whose contents this routine stores in a grid cell.

     \param item - value of "source" to store into the grid cell that contains vPosition.

     \note "Accumulate" accumulates values; it does not overwrite them.
     This routine does not populate the grid according to a function whose values
     are given by "item".  Instead, this routine treats each insertion as though
     the cell contains a specified "source", in addition to other sources that
     might have already been inserted.

     \note Since this routine accumulates values, it is likely prudent to initialize
     the values of each gridpoints to zero before calling this routine.

     */
    public void accumulate( ReadableVector3f vPosition , float item )
    {
        final int[]        indices = new int[4] ; // Indices of grid cell containing position.
        indicesOfPosition( indices , vPosition ) ;
        final Vector3f vMinCorner = new Vector3f();
        positionFromIndices( vMinCorner , indices ) ;
        //ASSERT( vPosition >= vMinCorner ) ;
        final int    offsetX0Y0Z0  = offsetFromIndices( indices ) ;
//        const Vec3      vDiff         = vPosition - vMinCorner ; // Relative location of position within its containing grid cell.
//        const Vec3      tween         = Clamp0to1( Vec3( vDiff.x * GetCellsPerExtent().x , vDiff.y * GetCellsPerExtent().y , vDiff.z * GetCellsPerExtent().z ) ) ;
//        const Vec3      oneMinusTween = Vec3( 1.0f , 1.0f , 1.0f ) - tween ;
        final Vector3f      vDiff         = Vector3f.sub(vPosition, vMinCorner, null) ; // Relative location of position within its containing grid cell.
        final Vector3f      tween         = new Vector3f( vDiff.x * getCellsPerExtent().x , vDiff.y * getCellsPerExtent().y , vDiff.z * getCellsPerExtent().z ) ;
        Vector3f.saturation(tween);
        final Vector3f      oneMinusTween = new Vector3f( 1.0f - tween.x, 1.0f - tween.y, 1.0f - tween.z);

        //if(     ( tween.x         < 0.0f ) || ( tween.y         < 0.0f ) || ( tween.z         < 0.0f )
        //    ||  ( oneMinusTween.x < 0.0f ) || ( oneMinusTween.y < 0.0f ) || ( oneMinusTween.z < 0.0f )
        //    ||  ( tween.x         > 1.0f ) || ( tween.y         > 1.0f ) || ( tween.z         > 1.0f )
        //    ||  ( oneMinusTween.x > 1.0f ) || ( oneMinusTween.y > 1.0f ) || ( oneMinusTween.z > 1.0f ) )
        //{
        //    printf( "position= %.15g %.15g %.15g   vMinCorner= %.15g %.15g %.15g\n", vPosition.x , vPosition.y , vPosition.z , vMinCorner.x , vMinCorner.y , vMinCorner.z ) ;
        //    printf( "diff= %g %g %g\n", vDiff.x , vDiff.y , vDiff.z ) ;
        //    printf( "tween= %g %g %g   oneMinusTween= %g %g %g\n", tween.x , tween.y , tween.z , oneMinusTween.x , oneMinusTween.y , oneMinusTween.z ) ;
        //    DEBUG_BREAK() ;
        //}
        //ASSERT( ( tween.x         <= 1.0f ) && ( tween.y         <= 1.0f ) && ( tween.z         <= 1.0f ) ) ;
        //ASSERT( ( tween.x         >= 0.0f ) && ( tween.y         >= 0.0f ) && ( tween.z         >= 0.0f ) ) ;
        //ASSERT( ( oneMinusTween.x <= 1.0f ) && ( oneMinusTween.y <= 1.0f ) && ( oneMinusTween.z <= 1.0f ) ) ;
        //ASSERT( ( oneMinusTween.x >= 0.0f ) && ( oneMinusTween.y >= 0.0f ) && ( oneMinusTween.z >= 0.0f ) ) ;
        final int    numXY         = getNumPoints( 0 ) * getNumPoints( 1 ) ;
        final int    offsetX1Y0Z0  = offsetX0Y0Z0 + 1 ;
        final int    offsetX0Y1Z0  = offsetX0Y0Z0 + getNumPoints(0) ;
        final int    offsetX1Y1Z0  = offsetX0Y0Z0 + getNumPoints(0) + 1 ;
        final int    offsetX0Y0Z1  = offsetX0Y0Z0 + numXY ;
        final int    offsetX1Y0Z1  = offsetX0Y0Z0 + numXY + 1 ;
        final int    offsetX0Y1Z1  = offsetX0Y0Z0 + numXY + getNumPoints(0) ;
        final int    offsetX1Y1Z1  = offsetX0Y0Z0 + numXY + getNumPoints(0) + 1 ;
        final float[] data = mContents.getData();

        data[ offsetX0Y0Z0 ] += oneMinusTween.x * oneMinusTween.y * oneMinusTween.z * item ;
        data[ offsetX1Y0Z0 ] +=         tween.x * oneMinusTween.y * oneMinusTween.z * item ;
        data[ offsetX0Y1Z0 ] += oneMinusTween.x *         tween.y * oneMinusTween.z * item ;
        data[ offsetX1Y1Z0 ] +=         tween.x *         tween.y * oneMinusTween.z * item ;
        data[ offsetX0Y0Z1 ] += oneMinusTween.x * oneMinusTween.y *         tween.z * item ;
        data[ offsetX1Y0Z1 ] +=         tween.x * oneMinusTween.y *         tween.z * item ;
        data[ offsetX0Y1Z1 ] += oneMinusTween.x *         tween.y *         tween.z * item ;
        data[ offsetX1Y1Z1 ] +=         tween.x *         tween.y *         tween.z * item ;
        /*#if defined( _DEBUG ) && 0
        {
                const float weightSum = oneMinusTween.x * oneMinusTween.y * oneMinusTween.z +
                tween.x * oneMinusTween.y * oneMinusTween.z +
                oneMinusTween.x *         tween.y * oneMinusTween.z +
                tween.x *         tween.y * oneMinusTween.z +
                oneMinusTween.x * oneMinusTween.y *         tween.z +
                tween.x * oneMinusTween.y *         tween.z +
                oneMinusTween.x *         tween.y *         tween.z +
                tween.x *         tween.y *         tween.z ;
            ASSERT( Math::Resembles( weightSum , 1.0f ) ) ;
                const ItemT itemSum = oneMinusTween.x * oneMinusTween.y * oneMinusTween.z * item
                +         tween.x * oneMinusTween.y * oneMinusTween.z * item
                + oneMinusTween.x *         tween.y * oneMinusTween.z * item
                +         tween.x *         tween.y * oneMinusTween.z * item
                + oneMinusTween.x * oneMinusTween.y *         tween.z * item
                +         tween.x * oneMinusTween.y *         tween.z * item
                + oneMinusTween.x *         tween.y *         tween.z * item
                +         tween.x *         tween.y *         tween.z * item ;
            using namespace Math ;
            ASSERT( Resembles( itemSum , item ) ) ;
        }
        #endif*/
    }




    /** Set all points surrounding the given cell to the specified value.*/
    public void setCell(ReadableVector3f vPosition , float item )
    {
        final int[] indices = new int[4] ; // Indices of grid cell containing position.
        indicesOfPosition( indices , vPosition ) ;
        Vector3f vMinCorner = new Vector3f();
        positionFromIndices( vMinCorner , indices ) ;
        final int  numXY        = getNumPoints( 0 ) * getNumPoints( 1 ) ;
        final int  offsetX0Y0Z0 = offsetFromIndices( indices ) ;
        final int  offsetX1Y0Z0 = offsetX0Y0Z0 + 1 ;
        final int  offsetX0Y1Z0 = offsetX0Y0Z0 + getNumPoints(0) ;
        final int  offsetX1Y1Z0 = offsetX0Y0Z0 + getNumPoints(0) + 1 ;
        final int  offsetX0Y0Z1 = offsetX0Y0Z0 + numXY ;
        final int  offsetX1Y0Z1 = offsetX0Y0Z0 + numXY + 1 ;
        final int  offsetX0Y1Z1 = offsetX0Y0Z0 + numXY + getNumPoints(0) ;
        final int  offsetX1Y1Z1 = offsetX0Y0Z0 + numXY + getNumPoints(0) + 1 ;
        final float[] data = mContents.getData();

        data[ offsetX0Y0Z0 ] = item ;
        data[ offsetX1Y0Z0 ] = item ;
        data[ offsetX0Y1Z0 ] = item ;
        data[ offsetX1Y1Z0 ] = item ;
        data[ offsetX0Y0Z1 ] = item ;
        data[ offsetX1Y0Z1 ] = item ;
        data[ offsetX0Y1Z1 ] = item ;
        data[ offsetX1Y1Z1 ] = item ;
    }

    /** Remove given component from all points surrounding the given cell.
     */
    public void removeComponent( ReadableVector3f vPosition , float component )
    {
        int[]          indices = new int[4] ; // Indices of grid cell containing position.
        indicesOfPosition( indices , vPosition ) ;
        final Vector3f  vMinCorner = new Vector3f();
        positionFromIndices( vMinCorner , indices ) ;
        final int    numXY        = getNumPoints( 0 ) * getNumPoints( 1 ) ;
        final int    offsetX0Y0Z0 = offsetFromIndices( indices ) ;
        final int    offsetX1Y0Z0 = offsetX0Y0Z0 + 1 ;
        final int    offsetX0Y1Z0 = offsetX0Y0Z0 + getNumPoints(0) ;
        final int    offsetX1Y1Z0 = offsetX0Y0Z0 + getNumPoints(0) + 1 ;
        final int    offsetX0Y0Z1 = offsetX0Y0Z0 + numXY ;
        final int    offsetX1Y0Z1 = offsetX0Y0Z0 + numXY + 1 ;
        final int    offsetX0Y1Z1 = offsetX0Y0Z0 + numXY + getNumPoints(0) ;
        final int    offsetX1Y1Z1 = offsetX0Y0Z0 + numXY + getNumPoints(0) + 1 ;
        final float[] data = mContents.getData();
        
        data[ offsetX0Y0Z0 ] -= data[ offsetX0Y0Z0 ] * component * component ;
        data[ offsetX1Y0Z0 ] -= data[ offsetX1Y0Z0 ] * component * component  ;
        data[ offsetX0Y1Z0 ] -= data[ offsetX0Y1Z0 ] * component * component  ;
        data[ offsetX1Y1Z0 ] -= data[ offsetX1Y1Z0 ] * component * component  ;
        data[ offsetX0Y0Z1 ] -= data[ offsetX0Y0Z1 ] * component * component  ;
        data[ offsetX1Y0Z1 ] -= data[ offsetX1Y0Z1 ] * component * component  ;
        data[ offsetX0Y1Z1 ] -= data[ offsetX0Y1Z1 ] * component * component  ;
        data[ offsetX1Y1Z1 ] -= data[ offsetX1Y1Z1 ] * component * component  ;
    }




    /** Thread-safe version of RemoveComponent.
     */
    public void removeComponent_ThreadSafe( ReadableVector3f vPosition , float component )
    {
        final int[]        indices = new int[4] ; // Indices of grid cell containing position.
        indicesOfPosition( indices , vPosition ) ;
        final Vector3f vMinCorner = new Vector3f();
        positionFromIndices( vMinCorner , indices ) ;
        final int  numXY        = getNumPoints( 0 ) * getNumPoints( 1 ) ;
        final int  offsetX0Y0Z0 = offsetFromIndices( indices ) ;
        final int  offsetX1Y0Z0 = offsetX0Y0Z0 + 1 ;
        final int  offsetX0Y1Z0 = offsetX0Y0Z0 + getNumPoints(0) ;
        final int  offsetX1Y1Z0 = offsetX0Y0Z0 + getNumPoints(0) + 1 ;
        final int  offsetX0Y0Z1 = offsetX0Y0Z0 + numXY ;
        final int  offsetX1Y0Z1 = offsetX0Y0Z0 + numXY + 1 ;
        final int  offsetX0Y1Z1 = offsetX0Y0Z0 + numXY + getNumPoints(0) ;
        final int  offsetX1Y1Z1 = offsetX0Y0Z0 + numXY + getNumPoints(0) + 1 ;
        final float[] data = mContents.getData();

        data[ offsetX0Y0Z0 ] -= data[ offsetX0Y0Z0 ] * component * component;
        data[ offsetX1Y0Z0 ] -= data[ offsetX1Y0Z0 ] * component * component;
        data[ offsetX0Y1Z0 ] -= data[ offsetX0Y1Z0 ] * component * component;
        data[ offsetX1Y1Z0 ] -= data[ offsetX1Y1Z0 ] * component * component;
        data[ offsetX0Y0Z1 ] -= data[ offsetX0Y0Z1 ] * component * component;
        data[ offsetX1Y0Z1 ] -= data[ offsetX1Y0Z1 ] * component * component;
        data[ offsetX0Y1Z1 ] -= data[ offsetX0Y1Z1 ] * component * component;
        data[ offsetX1Y1Z1 ] -= data[ offsetX1Y1Z1 ] * component * component;
    }


    /** Restrict values from a high-resolution grid into a low-resolution grid.

     In the vernacular of multi-grid solvers, the "restrict" operation reduces
     the resolution of a grid, so this operation is tantamount to down-sampling.
     It contrasts with the "interpolate" operation, which up-samples.

     This routine assumes this loRes grid is empty and the given hiRes layer is populated.

     \param hiRes - hiRes grid from which information will be aggregated.

     \note This loRes grid must have 3 or greater points in at least one of its dimensions.

     \see SolvePoissonMultiGrid UpSampleFrom

     */
    public void downSample( UniformGridFloat hiRes )
    {
        assert ( hiRes.size() == hiRes.getGridCapacity() ) ;
        UniformGridFloat  loRes        = this ;
        assert ( ( loRes.getNumPoints( 0 ) > 2 ) || ( loRes.getNumPoints( 1 ) > 2 ) || ( loRes.getNumPoints( 2 ) > 2 ) ) ;
        final int          numPointsHiRes[]   = { hiRes.getNumPoints( 0 ) , hiRes.getNumPoints( 1 ) , hiRes.getNumPoints( 2 ) } ;
        final int          numXhiRes           = hiRes.getNumPoints( 0 ) ;
        final int          numXYhiRes          = numXhiRes * hiRes.getNumPoints( 1 ) ;
        final float      fMultiplierTable[]  = { 8.0f , 4.0f , 2.0f , 1.0f } ;

        // number of cells in each grid cluster
        final int pClusterDims[] = {   hiRes.getNumCells( 0 ) / loRes.getNumCells( 0 )
            ,   hiRes.getNumCells( 1 ) / loRes.getNumCells( 1 )
            ,   hiRes.getNumCells( 2 ) / loRes.getNumCells( 2 ) } ;
        assert ( pClusterDims[0] > 1 ) ;
        assert ( pClusterDims[0] > 1 ) ;
        assert ( pClusterDims[0] > 1 ) ;

        final int  numPointsLoRes[]   = { loRes.getNumPoints( 0 ) , loRes.getNumPoints( 1 ) , loRes.getNumPoints( 2 ) } ;
        final int  numXYLoRes          = loRes.getNumPoints( 0 ) * loRes.getNumPoints( 1 ) ;
//        #if USE_ALL_NEIGHBORS
        final int  idxShifts[]        = { pClusterDims[0] / 2 , pClusterDims[1] / 2 , pClusterDims[2] / 2 } ;
//        #endif
        // Since this loop iterates over each destination cell,
        // it should readily parallelize without contention.
        //
        // Note that the if-statements inside this loop could (and perhaps should) be
        // moved outside the loop.  Accomplish this by limiting the loRes indices to
        // be in [1,N-2] and then creating 6 2D loops below, for the boundary planes.
        final int[] idxLoRes = new int[3] ;
        for( idxLoRes[2] = 0 ; idxLoRes[2] < numPointsLoRes[2] ; ++ idxLoRes[2] )
        {
            int offsetZ = idxLoRes[2] * numXYLoRes ;
            for( idxLoRes[1] = 0 ; idxLoRes[1] < numPointsLoRes[1] ; ++ idxLoRes[1] )
            {
                int offsetYZ = idxLoRes[1] * loRes.getNumPoints( 0 ) + offsetZ ;
                for( idxLoRes[0] = 0 ; idxLoRes[0] < numPointsLoRes[0] ; ++ idxLoRes[0] )
                {   // For each cell in the loRes layer...
                    int  offsetXYZ   = idxLoRes[0] + offsetYZ ;
                    float           rValLoRes  = loRes.get(offsetXYZ) ;
                    float           multiplier  = 0.0f ;
                    int[] clusterMinIndices = new int[ 3 ] ;
                    NestedGrid.getChildClusterMinCornerIndex( clusterMinIndices , pClusterDims , idxLoRes ) ;
                    int[] increment = new int[3] ;
                    int[]      shiftHiRes = new int[3] ;
                    int[] idxHiRes = new int[3] ;

                    // Code below accumulates values so destination needs to start at zero.
//                    memset( & rValLoRes , 0 , sizeof( rValLoRes ) ) ;

                    // For each cell of hiRes layer in this grid cluster...
                    for( increment[2] = 0 ; increment[2] < pClusterDims[2]  + idxShifts[2]  ; ++ increment[2] )
                    {
                        shiftHiRes[2] = increment[2]  - idxShifts[2]  ;
                        idxHiRes[2] = clusterMinIndices[2] + shiftHiRes[2] ;
                        if( idxHiRes[2] < numPointsHiRes[2] )
                        {
                            offsetZ  = idxHiRes[2] * numXYhiRes ;
                            for( increment[1] = 0 ; increment[1] < pClusterDims[1]  + idxShifts[1]  ; ++ increment[1] )
                            {
                                shiftHiRes[1] = increment[1]  - idxShifts[1] ;
                                idxHiRes[1] = clusterMinIndices[1] + shiftHiRes[1] ;
                                if( idxHiRes[1] < numPointsHiRes[1] )
                                {
                                    offsetYZ = idxHiRes[1] * numXhiRes + offsetZ ;
                                    for( increment[0] = 0 ; increment[0] < pClusterDims[0]+ idxShifts[0] ; ++ increment[0] )
                                    {
                                        shiftHiRes[0] = increment[0]- idxShifts[0] ;
                                        idxHiRes[0] = clusterMinIndices[0] + shiftHiRes[0] ;
                                        if( idxHiRes[0]  < numPointsHiRes[0] )
                                        {
                                            offsetXYZ       = idxHiRes[0]  + offsetYZ ;
                                            final int  manhattanDist   = Math.abs( shiftHiRes[0] ) + Math.abs( shiftHiRes[1] ) + Math.abs( shiftHiRes[2] ) ;
                                            assert ( ( manhattanDist >= 0 ) && ( manhattanDist < 4 ) ) ;
                                            float   rValHiRes       = hiRes.get(offsetXYZ);
                                            assert ( ! Float.isInfinite( rValHiRes ) && ! Float.isNaN( rValHiRes ) ) ;
//                                            #if USE_ALL_NEIGHBORS
                                            multiplier += fMultiplierTable[ manhattanDist ] ;
                                            rValLoRes  += fMultiplierTable[ manhattanDist ] * rValHiRes ;
                                            assert ( multiplier <= 64.0 ) ;
//                                            #else
//                                            multiplier += 1.0f ;
//                                            rValLoRes  += rValHiRes ;
//                                            #endif
                                        }
                                    }
                                }
                            }
                        }
                    }

                    loRes.set(offsetXYZ, rValLoRes);

                    // Normalize sum to account for number of gridpoints that contributed to this gridpoint.
                    assert ( multiplier > 0.0f ) ;
//                    #if USE_ALL_NEIGHBORS
                    rValLoRes /= 64.0f ;
//                    #else
//                    rValLoRes /= multiplier ;
//                    #endif
                }
            }
        }

//            #if defined( _DEBUG )
        {
//            final float zerothMomentLoRes = loRes.sum() * loRes.getCellVolume() ;
//            final float zerothMomentHiRes = hiRes.sum() * hiRes.getCellVolume() ;
//            assert ( zerothMomentLoRes.resembles( zerothMomentHiRes , 1.0e-4f ) ) ;
        }
//            #endif
    }


    /** Interpolate value from the given low-resolution grid into this high-resolution grid.

     In the vernacular of multi-grid solvers, the "interpolate" operation increases
     the resolution of a grid, so this operation is tantamount to up-sampling.
     It contrasts with the "restrict" operation, which down-samples.

     This routine assumes the given high-resolution grid (this)
     is empty and the given loRes grid is populated.

     \param loRes - low-resolution grid from which information will be read.

     \see SolvePoissonMultiGrid DownSample

     \note As of 2009nov19, this routine does NOT properly "undo" the operations of DownSampleInto.
     In multi-grid vernacular, this operation does NOT apply the transpose
     of the Restriction operation.  This is evident by virtue of the fact
     that after this operation, zeroth moment between the low-res and high-res
     grids DO NOT MATCH.
     A proper implementation would depend on the USE_ALL_NEIGHBORS flag,
     and would preserve zeroth moment.
     When USE_ALL_NEIGHBORS is DISabled, then this routine could simply
     copy values from the low-res to high-res grids, without interpolation.
     When USE_ALL_NEIGHBORS is ENabled, then this routine would need to use
     a different interpolation algorithm -- not tri-linear, but something like
     quadratic, where values come from all 27 neighbors of a position.

     */
    void UpSample( UniformGridFloat loRes )
    {
        UniformGridFloat  hiRes         = this ;
        assert ( loRes.size() == loRes.getGridCapacity() ) ;
        final int          numPointsHiRes[]   = { hiRes.getNumPoints( 0 ) , hiRes.getNumPoints( 1 ) , hiRes.getNumPoints( 2 ) } ;
        final int          numXhiRes           = hiRes.getNumPoints( 0 ) ;
        final int          numXYhiRes          = numXhiRes * hiRes.getNumPoints( 1 ) ;
        // Nudge sampling positions so they lie strictly within the grid.
        final Vector3f              vPosMinAdjust       = new Vector3f( Math.abs( hiRes.getMinCorner().getX() )
        , Math.abs( hiRes.getMinCorner().getY() )
        , Math.abs( hiRes.getMinCorner().getZ() ) ) /** 2.0f * FLT_EPSILON*/ ; vPosMinAdjust.scale(2.0f * FLT_EPSILON);

        final Vector3f              vPosMin             = Vector3f.add(hiRes.getMinCorner(), vPosMinAdjust, null) ;
        assert ( ( hiRes.getCellSpacing().x >= 0.0f ) && ( hiRes.getCellSpacing().y >= 0.0f ) && ( hiRes.getCellSpacing().z >= 0.0f ) ) ;
        final Vector3f              vSpacing            = Vector3f.scale(hiRes.getCellSpacing() , ( 1.0f - 4.0f * FLT_EPSILON ), null) ;

        // Since this loop iterates over each destination cell,
        // it should readily parallelize without contention.
        int[] idxHiRes = new int[3] ;
        for( idxHiRes[2] = 0 ; idxHiRes[2] < numPointsHiRes[2] ; ++ idxHiRes[2] )
        {
            final int  offsetZ     = idxHiRes[2] * numXYhiRes ;
            Vector3f            vPosition =new Vector3f()  ;
            vPosition.z = vPosMin.z + ( idxHiRes[2] ) * vSpacing.z ;

            for( idxHiRes[1] = 0 ; idxHiRes[1] < numPointsHiRes[1] ; ++ idxHiRes[1] )
            {
                    final int offsetYZ = idxHiRes[1] * hiRes.getNumPoints( 0 ) + offsetZ ;
                vPosition.y = vPosMin.y + ( idxHiRes[1] ) * vSpacing.y ;

                for( idxHiRes[0] = 0 ; idxHiRes[0] < numPointsHiRes[0] ; ++ idxHiRes[0] )
                {   // For each cell in the loRes layer...
                    final int  offsetXYZ   = idxHiRes[0] + offsetYZ ;
                    float        rValHiRes   = hiRes.get(offsetXYZ) ;
                    vPosition.x = vPosMin.x + idxHiRes[0] * vSpacing.x ;
                    rValHiRes = loRes.interpolate( vPosition ) ;
                    assert ( ! Float.isNaN( rValHiRes ) && ! Float.isInfinite( rValHiRes ) ) ;
                    hiRes.set(offsetXYZ, rValHiRes);
                }
            }
        }

//            #if defined( _DEBUG )
        {
//                final float zerothMomentLoRes = loRes.computeZerothMoment() ;
//            final float zerothMomentHiRes = hiRes.computeZerothMoment() ;
//            assert ( zerothMomentLoRes.resembles( zerothMomentHiRes ) ) ;
        }
//            #endif
    }



    public void clear()
    {
        mContents.clear() ;
        super.clear();
    }


//    void GenerateBrickOfBytes( const char * strFilenameBase , unsigned uFrame ) const ;



    /** Compute sum of contents of this container.
     */
    public float sum()
    {
        float     sum  = 0;
//        memset( & sum , 0 , sizeof( sum ) ) ;
        for( int offsetP = 0 ; offsetP < getGridCapacity() ; ++ offsetP )
        {
            sum +=get(offsetP) ;
        }
        return sum ;
    }


    /** Compute zeroth moment of contents of this container.
     */
    public float computeZerothMoment()
    {
            final int numCells[  ] = { getNumCells( 0 ) , getNumCells( 1 ) , getNumCells( 2 ) } ;
            final int strides[  ] = { 1 , getNumPoints( 0 ) , getNumPoints( 0 ) * getNumPoints( 1 ) } ;
        float     firstMoment  = 0;
//        memset( & firstMoment , 0 , sizeof( firstMoment ) ) ;
        int[] idxCell = new int[3] ;
        for( idxCell[2] = 0 ; idxCell[2] < numCells[ 2 ] ; ++ idxCell[2] )
        {
            for( idxCell[1] = 0 ; idxCell[1] < numCells[ 1 ] ; ++ idxCell[1] )
            {
                for( idxCell[0] = 0 ; idxCell[0] < numCells[ 0 ] ; ++ idxCell[0] )
                {   // For each cell...
                        final int offset = offsetFromIndices( idxCell ) ;
                    firstMoment +=
                            (   get( offset                                              )
                        +   get( offset + strides[ 0 ]                               )
                        +   get( offset +                strides[ 1 ]                )
                        +   get( offset + strides[ 0 ] + strides[ 1 ]                )
                        +   get( offset +                               strides[ 2 ] )
                        +   get( offset + strides[ 0 ] +                strides[ 2 ] )
                        +   get( offset +                strides[ 1 ] + strides[ 2 ] )
                        +   get( offset + strides[ 0 ] + strides[ 1 ] + strides[ 2 ] )
                            ) ;
                }
            }
        }
        final float oneOverPointsPerCell = 0.125f ; // Each gridcell has 8 gridpoints.
        return firstMoment * getCellVolume() * oneOverPointsPerCell ;
    }
}
