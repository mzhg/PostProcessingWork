package jet.opengl.demos.intel.fluid.utils;

import org.lwjgl.util.vector.ReadableVector3f;

import java.util.ArrayList;

import jet.opengl.postprocessing.util.RecycledPool;

/**
 * Templated container for fast spatial lookups and insertions.<p></p>
 * Created by Administrator on 2018/6/2 0002.
 */

public class UniformGrid<ItemT> extends UniformGridGeometry{

    private final ArrayList<ItemT> mContents = new ArrayList<ItemT>();   ///< 3D array of items.

    /** Construct an empty UniformGrid.
     \see Initialize
     */
    public UniformGrid() { }


    /** Construct a uniform grid container that fits the given geometry.
     \see Initialize
     */
    public UniformGrid(int uNumElements , ReadableVector3f vMin , ReadableVector3f vMax , boolean bPowerOf2 )
    {
        super( uNumElements , vMin , vMax , bPowerOf2 );
    }


    /** Copy shape from given uniform grid.
     */
    public UniformGrid( UniformGridGeometry that )
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
    public UniformGrid( UniformGrid that )
//            : UniformGridGeometry( that )
    {
        super(that);
        //ASSERT( that.Empty() ) ; // Maybe you meant to use UniformGrid( const UniformGridGeometry & ) instead.
//        this->operator=( that ) ;
        set(that);
    }

    public UniformGrid set( UniformGrid that )
    {
        if( this != that )
        {
            super.set( that ) ;
            // Note, in MSVC 7.1 (.NET) for vector-of-vectors, this seems to corrupt the original vector.
//            mContents = that.mContents ;
            mContents.clear();
            mContents.addAll(that.mContents); // TODO reference copy.
//            for(int i = 0; i < that.mContents.size(); i++){
//                mContents.add(that.mContents.get(i).clone());
//            }
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
    /*ItemT &       operator[]( const size_t & offset )       { ASSERT( offset < Size() ) ; return mContents[ offset ] ; }
        const ItemT &       operator[]( const size_t & offset ) const { ASSERT( offset < Size() ) ; return mContents[ offset ] ; }*/
    public ItemT get(int offset) { return mContents.get(offset);}

    /// Return item at given indices.
//    ItemT &       operator[]( const size_t indices[] )    { return mContents[ OffsetFromIndices( indices ) ] ; }
    public ItemT get(int[] indices) { return mContents.get(offsetFromIndices( indices ));}

    /// Return item at given indices.
    public ItemT       get( int ix , int iy , int iz )       { return mContents.get(offsetFromIndices( ix , iy , iz )) ; }
//        const ItemT &       Get( size_t ix , size_t iy , size_t iz ) const { return mContents[ OffsetFromIndices( ix , iy , iz ) ] ; }

    /// Return item at given position.
    public ItemT        get( ReadableVector3f vPosition )        { return mContents.get(offsetOfPosition( vPosition )) ; }
//        const ItemT &       operator[]( const Vec3 & vPosition ) const  { return mContents[ OffsetOfPosition( vPosition ) ] ; }

//    bool operator==( const UniformGrid & that ) const
    @Override
    public boolean equals(Object obj)
    {
        if( !super.equals(obj) )
        {   // Geometries do not match, so these grids are not equal.
            return false ;
        }

        if(obj instanceof UniformGrid) {
            UniformGrid that = (UniformGrid)obj;
            return mContents.equals(that.mContents);
        }

        return false;
    }




    /** Scale each value in this grid by the given scalar.

     \param scale - amount by which to scale each value in this grid

     */
    /*public void scale( float scale )
    {
        final int numCells = getGridCapacity() ;
        for( int offset = 0 ; offset < numCells ; ++ offset )
        {
            ItemT rVal = get(offset) ;
            rVal *= scale ;
        }
    }*/


    /** Initialize contents to whatever default ctor provides.
     */
    public void init(RecycledPool.ObjectCreater<ItemT> creater)
    {
//        PERF_BLOCK( UniformGrid__Init ) ;

        // First clear because Resize only assigns values to new items.
        mContents.clear() ;
//        mContents.Resize( GetGridCapacity() , initialValue ) ;
        for(int i = 0; i < getGridCapacity(); i++){
            mContents.add(creater.newObject());
        }
    }

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
    /*public void computeStatistics( ItemT & min , ItemT & max ) const
    {
        max = min = (*this)[ 0 ] ;
            const unsigned numCells = GetGridCapacity() ;
        for( unsigned offset = 0 ; offset < numCells ; ++ offset )
        {
                const ItemT & rVal = (*this)[ offset ] ;
            min = Min2( min , rVal ) ;
            max = Max2( max , rVal ) ;
        }
    }*/



    /** Interpolate values from grid to get value at given position.

     \param vResult      Interpolated value corresponding to value of grid contents at vPosition.

     \param vPosition    Position to sample.
     */
    /*void Interpolate( ItemT & vResult , const Vec3 & vPosition ) const
    {
        unsigned        indices[4] ; // Indices of grid cell containing position.
        DEBUG_ONLY( UniformGridGeometry::sInterpolating = true ) ;
        IndicesOfPosition( indices , vPosition ) ;
        DEBUG_ONLY( UniformGridGeometry::sInterpolating = false ) ;
        ASSERT( indices[0] < GetNumCells( 0 ) ) ;
        ASSERT( indices[1] < GetNumCells( 1 ) ) ;
        ASSERT( indices[2] < GetNumCells( 2 ) ) ;
        Vec3            vMinCorner ;
        PositionFromIndices( vMinCorner , indices ) ;
            const Vec3      vDiff         = vPosition - vMinCorner ; // Relative location of position within its containing grid cell.
            const Vec3      tween         = Vec3( vDiff.x * GetCellsPerExtent().x , vDiff.y * GetCellsPerExtent().y , vDiff.z * GetCellsPerExtent().z ) ;
            const Vec3      oneMinusTween = Vec3( 1.0f , 1.0f , 1.0f ) - tween ;
            const size_t    numXY         = GetNumPoints( 0 ) * GetNumPoints( 1 ) ;
            const size_t    offsetX0Y0Z0  = OffsetFromIndices( indices ) ;
            const size_t    offsetX1Y0Z0  = offsetX0Y0Z0 + 1 ;
            const size_t    offsetX0Y1Z0  = offsetX0Y0Z0 + GetNumPoints(0) ;
            const size_t    offsetX1Y1Z0  = offsetX0Y0Z0 + GetNumPoints(0) + 1 ;
            const size_t    offsetX0Y0Z1  = offsetX0Y0Z0 + numXY ;
            const size_t    offsetX1Y0Z1  = offsetX0Y0Z0 + numXY + 1 ;
            const size_t    offsetX0Y1Z1  = offsetX0Y0Z0 + numXY + GetNumPoints(0) ;
            const size_t    offsetX1Y1Z1  = offsetX0Y0Z0 + numXY + GetNumPoints(0) + 1 ;
        vResult =     ( ( oneMinusTween.x * (*this)[ offsetX0Y0Z0 ]
                +         tween.x * (*this)[ offsetX1Y0Z0 ] ) * oneMinusTween.y
            + ( oneMinusTween.x * (*this)[ offsetX0Y1Z0 ]
            +         tween.x * (*this)[ offsetX1Y1Z0 ] ) * tween.y        ) * oneMinusTween.z
            + ( ( oneMinusTween.x * (*this)[ offsetX0Y0Z1 ]
            +         tween.x * (*this)[ offsetX1Y0Z1 ] ) * oneMinusTween.y
            + ( oneMinusTween.x * (*this)[ offsetX0Y1Z1 ]
            +         tween.x * (*this)[ offsetX1Y1Z1 ] ) * tween.y        ) * tween.z ;
    }*/



    /** Interpolate values from grid to get value at given position.

     \param vResult      Interpolated value corresponding to value of grid contents at vPosition.

     \param vPosition    Position to sample.
     */
    /*void InterpolateConditionally( ItemT & result , const Vec3 & vPosition ) const
    {
        unsigned        indices[4] ; // Indices of grid cell containing position.
        DEBUG_ONLY( UniformGridGeometry::sInterpolating = true ) ;
        IndicesOfPosition( indices , vPosition ) ;
        DEBUG_ONLY( UniformGridGeometry::sInterpolating = false ) ;
        ASSERT( indices[0] < GetNumCells( 0 ) ) ;
        ASSERT( indices[1] < GetNumCells( 1 ) ) ;
        ASSERT( indices[2] < GetNumCells( 2 ) ) ;
        Vec3            vMinCorner ;
        PositionFromIndices( vMinCorner , indices ) ;
            const Vec3      vDiff         = vPosition - vMinCorner ; // Relative location of position within its containing grid cell.
            const Vec3      tween         = Vec3( vDiff.x * GetCellsPerExtent().x , vDiff.y * GetCellsPerExtent().y , vDiff.z * GetCellsPerExtent().z ) ;
            const Vec3      oneMinusTween = Vec3( 1.0f , 1.0f , 1.0f ) - tween ;
            const size_t    numXY         = GetNumPoints( 0 ) * GetNumPoints( 1 ) ;
            const size_t    offsetX0Y0Z0  = OffsetFromIndices( indices ) ;
            const size_t    offsetX1Y0Z0  = offsetX0Y0Z0 + 1 ;
            const size_t    offsetX0Y1Z0  = offsetX0Y0Z0 + GetNumPoints(0) ;
            const size_t    offsetX1Y1Z0  = offsetX0Y0Z0 + GetNumPoints(0) + 1 ;
            const size_t    offsetX0Y0Z1  = offsetX0Y0Z0 + numXY ;
            const size_t    offsetX1Y0Z1  = offsetX0Y0Z0 + numXY + 1 ;
            const size_t    offsetX0Y1Z1  = offsetX0Y0Z0 + numXY + GetNumPoints(0) ;
            const size_t    offsetX1Y1Z1  = offsetX0Y0Z0 + numXY + GetNumPoints(0) + 1 ;
        float weightSum = 0.0f ;
        if( ! IsNan( (*this)[ offsetX0Y0Z0 ] ) ) { result += oneMinusTween.x * oneMinusTween.y * oneMinusTween.z * (*this)[ offsetX0Y0Z0 ] ; weightSum += oneMinusTween.x * oneMinusTween.y * oneMinusTween.z ; }
        if( ! IsNan( (*this)[ offsetX1Y0Z0 ] ) ) { result +=         tween.x * oneMinusTween.y * oneMinusTween.z * (*this)[ offsetX1Y0Z0 ] ; weightSum +=         tween.x * oneMinusTween.y * oneMinusTween.z ; }
        if( ! IsNan( (*this)[ offsetX0Y1Z0 ] ) ) { result += oneMinusTween.x *         tween.y * oneMinusTween.z * (*this)[ offsetX0Y1Z0 ] ; weightSum += oneMinusTween.x *         tween.y * oneMinusTween.z ; }
        if( ! IsNan( (*this)[ offsetX1Y1Z0 ] ) ) { result +=         tween.x *         tween.y * oneMinusTween.z * (*this)[ offsetX1Y1Z0 ] ; weightSum +=         tween.x *         tween.y * oneMinusTween.z ; }
        if( ! IsNan( (*this)[ offsetX0Y0Z1 ] ) ) { result += oneMinusTween.x * oneMinusTween.y *         tween.z * (*this)[ offsetX0Y0Z1 ] ; weightSum += oneMinusTween.x * oneMinusTween.y * oneMinusTween.z ; }
        if( ! IsNan( (*this)[ offsetX1Y0Z1 ] ) ) { result +=         tween.x * oneMinusTween.y *         tween.z * (*this)[ offsetX1Y0Z1 ] ; weightSum +=         tween.x * oneMinusTween.y *         tween.z ; }
        if( ! IsNan( (*this)[ offsetX0Y1Z1 ] ) ) { result += oneMinusTween.x *         tween.y *         tween.z * (*this)[ offsetX0Y1Z1 ] ; weightSum += oneMinusTween.x *         tween.y *         tween.z ; }
        if( ! IsNan( (*this)[ offsetX1Y1Z1 ] ) ) { result +=         tween.x *         tween.y *         tween.z * (*this)[ offsetX1Y1Z1 ] ; weightSum +=         tween.x *         tween.y *         tween.z ; }
        if( 0.0f == weightSum )
        {
            (float&) result = UNIFORM_GRID_INVALID_VALUE ;
        }
        else
        {
            ASSERT( ( weightSum > 0.0f ) && ( weightSum <= 1.0f ) ) ;
            result /= weightSum ;
            ASSERT( ! IsNan( result ) && ! IsInf( result ) ) ;
        }
    }*/



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
    /*void Interpolate_AssumesFpcwSetToTruncate( ItemT & vResult , const Vec3 & vPosition ) const
    {
        unsigned        indices[4] ; // Indices of grid cell containing position.

        DEBUG_ONLY( UniformGridGeometry::sInterpolating = true ) ;
        IndicesOfPosition_AssumesFpcwSetToTruncate( indices , vPosition ) ;
        DEBUG_ONLY( UniformGridGeometry::sInterpolating = false ) ;

        ASSERT( indices[0] < GetNumCells( 0 ) ) ;
        ASSERT( indices[1] < GetNumCells( 1 ) ) ;
        ASSERT( indices[2] < GetNumCells( 2 ) ) ;
        Vec3            vMinCorner ;
        PositionFromIndices( vMinCorner , indices ) ;
            const size_t    offsetX0Y0Z0  = OffsetFromIndices( indices ) ;
            const Vec3      vDiff         = vPosition - vMinCorner ; // Relative location of position within its containing grid cell.
            const Vec3      tween         = Vec3( vDiff.x * GetCellsPerExtent().x , vDiff.y * GetCellsPerExtent().y , vDiff.z * GetCellsPerExtent().z ) ;
            const Vec3      oneMinusTween = Vec3( 1.0f , 1.0f , 1.0f ) - tween ;
            const size_t    numXY         = GetNumPoints( 0 ) * GetNumPoints( 1 ) ;
            const size_t    offsetX1Y0Z0  = offsetX0Y0Z0 + 1 ;
            const size_t    offsetX0Y1Z0  = offsetX0Y0Z0 + GetNumPoints(0) ;
            const size_t    offsetX1Y1Z0  = offsetX0Y1Z0 + 1 ;
            const size_t    offsetX0Y0Z1  = offsetX0Y0Z0 + numXY ;
            const size_t    offsetX1Y0Z1  = offsetX0Y0Z1 + 1 ;
            const size_t    offsetX0Y1Z1  = offsetX0Y0Z0 + numXY + GetNumPoints(0) ;
            const size_t    offsetX1Y1Z1  = offsetX0Y1Z1 + 1 ;
        vResult =     ( ( oneMinusTween.x * (*this)[ offsetX0Y0Z0 ]
                +         tween.x * (*this)[ offsetX1Y0Z0 ] ) * oneMinusTween.y
            + ( oneMinusTween.x * (*this)[ offsetX0Y1Z0 ]
            +         tween.x * (*this)[ offsetX1Y1Z0 ] ) * tween.y        ) * oneMinusTween.z
            + ( ( oneMinusTween.x * (*this)[ offsetX0Y0Z1 ]
            +         tween.x * (*this)[ offsetX1Y0Z1 ] ) * oneMinusTween.y
            + ( oneMinusTween.x * (*this)[ offsetX0Y1Z1 ]
            +         tween.x * (*this)[ offsetX1Y1Z1 ] ) * tween.y        ) * tween.z ;
    }*/



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
    /*void Accumulate( const Vec3 & vPosition , const ItemT & item )
    {
        unsigned        indices[4] ; // Indices of grid cell containing position.
        IndicesOfPosition( indices , vPosition ) ;
        Vec3            vMinCorner ;
        PositionFromIndices( vMinCorner , indices ) ;
        //ASSERT( vPosition >= vMinCorner ) ;
            const size_t    offsetX0Y0Z0  = OffsetFromIndices( indices ) ;
            const Vec3      vDiff         = vPosition - vMinCorner ; // Relative location of position within its containing grid cell.
            const Vec3      tween         = Clamp0to1( Vec3( vDiff.x * GetCellsPerExtent().x , vDiff.y * GetCellsPerExtent().y , vDiff.z * GetCellsPerExtent().z ) ) ;
            const Vec3      oneMinusTween = Vec3( 1.0f , 1.0f , 1.0f ) - tween ;
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
            const size_t    numXY         = GetNumPoints( 0 ) * GetNumPoints( 1 ) ;
            const size_t    offsetX1Y0Z0  = offsetX0Y0Z0 + 1 ;
            const size_t    offsetX0Y1Z0  = offsetX0Y0Z0 + GetNumPoints(0) ;
            const size_t    offsetX1Y1Z0  = offsetX0Y0Z0 + GetNumPoints(0) + 1 ;
            const size_t    offsetX0Y0Z1  = offsetX0Y0Z0 + numXY ;
            const size_t    offsetX1Y0Z1  = offsetX0Y0Z0 + numXY + 1 ;
            const size_t    offsetX0Y1Z1  = offsetX0Y0Z0 + numXY + GetNumPoints(0) ;
            const size_t    offsetX1Y1Z1  = offsetX0Y0Z0 + numXY + GetNumPoints(0) + 1 ;
        (*this)[ offsetX0Y0Z0 ] += oneMinusTween.x * oneMinusTween.y * oneMinusTween.z * item ;
        (*this)[ offsetX1Y0Z0 ] +=         tween.x * oneMinusTween.y * oneMinusTween.z * item ;
        (*this)[ offsetX0Y1Z0 ] += oneMinusTween.x *         tween.y * oneMinusTween.z * item ;
        (*this)[ offsetX1Y1Z0 ] +=         tween.x *         tween.y * oneMinusTween.z * item ;
        (*this)[ offsetX0Y0Z1 ] += oneMinusTween.x * oneMinusTween.y *         tween.z * item ;
        (*this)[ offsetX1Y0Z1 ] +=         tween.x * oneMinusTween.y *         tween.z * item ;
        (*this)[ offsetX0Y1Z1 ] += oneMinusTween.x *         tween.y *         tween.z * item ;
        (*this)[ offsetX1Y1Z1 ] +=         tween.x *         tween.y *         tween.z * item ;
        #if defined( _DEBUG ) && 0
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
        #endif
    }*/




    /** Set all points surrounding the given cell to the specified value.
     */
    /*void SetCell( const Vec3 & vPosition , const ItemT & item )
    {
        unsigned        indices[4] ; // Indices of grid cell containing position.
        IndicesOfPosition( indices , vPosition ) ;
        Vec3            vMinCorner ;
        PositionFromIndices( vMinCorner , indices ) ;
            const unsigned  numXY        = GetNumPoints( 0 ) * GetNumPoints( 1 ) ;
            const unsigned  offsetX0Y0Z0 = OffsetFromIndices( indices ) ;
            const unsigned  offsetX1Y0Z0 = offsetX0Y0Z0 + 1 ;
            const unsigned  offsetX0Y1Z0 = offsetX0Y0Z0 + GetNumPoints(0) ;
            const unsigned  offsetX1Y1Z0 = offsetX0Y0Z0 + GetNumPoints(0) + 1 ;
            const unsigned  offsetX0Y0Z1 = offsetX0Y0Z0 + numXY ;
            const unsigned  offsetX1Y0Z1 = offsetX0Y0Z0 + numXY + 1 ;
            const unsigned  offsetX0Y1Z1 = offsetX0Y0Z0 + numXY + GetNumPoints(0) ;
            const unsigned  offsetX1Y1Z1 = offsetX0Y0Z0 + numXY + GetNumPoints(0) + 1 ;
        (*this)[ offsetX0Y0Z0 ] = item ;
        (*this)[ offsetX1Y0Z0 ] = item ;
        (*this)[ offsetX0Y1Z0 ] = item ;
        (*this)[ offsetX1Y1Z0 ] = item ;
        (*this)[ offsetX0Y0Z1 ] = item ;
        (*this)[ offsetX1Y0Z1 ] = item ;
        (*this)[ offsetX0Y1Z1 ] = item ;
        (*this)[ offsetX1Y1Z1 ] = item ;
    }*/




    /** Remove given component from all points surrounding the given cell.
     */
    /*void RemoveComponent( const Vec3 & vPosition , const ItemT & component )
    {
        size_t          indices[4] ; // Indices of grid cell containing position.
        IndicesOfPosition( indices , vPosition ) ;
        Vec3            vMinCorner ;
        PositionFromIndices( vMinCorner , indices ) ;
            const size_t    numXY        = GetNumPoints( 0 ) * GetNumPoints( 1 ) ;
            const size_t    offsetX0Y0Z0 = OffsetFromIndices( indices ) ;
            const size_t    offsetX1Y0Z0 = offsetX0Y0Z0 + 1 ;
            const size_t    offsetX0Y1Z0 = offsetX0Y0Z0 + GetNumPoints(0) ;
            const size_t    offsetX1Y1Z0 = offsetX0Y0Z0 + GetNumPoints(0) + 1 ;
            const size_t    offsetX0Y0Z1 = offsetX0Y0Z0 + numXY ;
            const size_t    offsetX1Y0Z1 = offsetX0Y0Z0 + numXY + 1 ;
            const size_t    offsetX0Y1Z1 = offsetX0Y0Z0 + numXY + GetNumPoints(0) ;
            const size_t    offsetX1Y1Z1 = offsetX0Y0Z0 + numXY + GetNumPoints(0) + 1 ;
        (*this)[ offsetX0Y0Z0 ] -= (*this)[ offsetX0Y0Z0 ] * component * component ;
        (*this)[ offsetX1Y0Z0 ] -= (*this)[ offsetX1Y0Z0 ] * component * component  ;
        (*this)[ offsetX0Y1Z0 ] -= (*this)[ offsetX0Y1Z0 ] * component * component  ;
        (*this)[ offsetX1Y1Z0 ] -= (*this)[ offsetX1Y1Z0 ] * component * component  ;
        (*this)[ offsetX0Y0Z1 ] -= (*this)[ offsetX0Y0Z1 ] * component * component  ;
        (*this)[ offsetX1Y0Z1 ] -= (*this)[ offsetX1Y0Z1 ] * component * component  ;
        (*this)[ offsetX0Y1Z1 ] -= (*this)[ offsetX0Y1Z1 ] * component * component  ;
        (*this)[ offsetX1Y1Z1 ] -= (*this)[ offsetX1Y1Z1 ] * component * component  ;
    }*/




    /** Thread-safe version of RemoveComponent.
     */
    /*void RemoveComponent_ThreadSafe( const Vec3 & vPosition , const ItemT & component )
    {
        using namespace Math ;
        unsigned        indices[4] ; // Indices of grid cell containing position.
        IndicesOfPosition( indices , vPosition ) ;
        Vec3            vMinCorner ;
        PositionFromIndices( vMinCorner , indices ) ;
            const unsigned  numXY        = GetNumPoints( 0 ) * GetNumPoints( 1 ) ;
            const unsigned  offsetX0Y0Z0 = OffsetFromIndices( indices ) ;
            const unsigned  offsetX1Y0Z0 = offsetX0Y0Z0 + 1 ;
            const unsigned  offsetX0Y1Z0 = offsetX0Y0Z0 + GetNumPoints(0) ;
            const unsigned  offsetX1Y1Z0 = offsetX0Y0Z0 + GetNumPoints(0) + 1 ;
            const unsigned  offsetX0Y0Z1 = offsetX0Y0Z0 + numXY ;
            const unsigned  offsetX1Y0Z1 = offsetX0Y0Z0 + numXY + 1 ;
            const unsigned  offsetX0Y1Z1 = offsetX0Y0Z0 + numXY + GetNumPoints(0) ;
            const unsigned  offsetX1Y1Z1 = offsetX0Y0Z0 + numXY + GetNumPoints(0) + 1 ;
        Vec3_FetchAndAdd( (*this)[ offsetX0Y0Z0 ] , - (*this)[ offsetX0Y0Z0 ] * component * component ) ;
        Vec3_FetchAndAdd( (*this)[ offsetX1Y0Z0 ] , - (*this)[ offsetX1Y0Z0 ] * component * component ) ;
        Vec3_FetchAndAdd( (*this)[ offsetX0Y1Z0 ] , - (*this)[ offsetX0Y1Z0 ] * component * component ) ;
        Vec3_FetchAndAdd( (*this)[ offsetX1Y1Z0 ] , - (*this)[ offsetX1Y1Z0 ] * component * component ) ;
        Vec3_FetchAndAdd( (*this)[ offsetX0Y0Z1 ] , - (*this)[ offsetX0Y0Z1 ] * component * component ) ;
        Vec3_FetchAndAdd( (*this)[ offsetX1Y0Z1 ] , - (*this)[ offsetX1Y0Z1 ] * component * component ) ;
        Vec3_FetchAndAdd( (*this)[ offsetX0Y1Z1 ] , - (*this)[ offsetX0Y1Z1 ] * component * component ) ;
        Vec3_FetchAndAdd( (*this)[ offsetX1Y1Z1 ] , - (*this)[ offsetX1Y1Z1 ] * component * component ) ;
    }*/




    /** Restrict values from a high-resolution grid into a low-resolution grid.

     In the vernacular of multi-grid solvers, the "restrict" operation reduces
     the resolution of a grid, so this operation is tantamount to down-sampling.
     It contrasts with the "interpolate" operation, which up-samples.

     This routine assumes this loRes grid is empty and the given hiRes layer is populated.

     \param hiRes - hiRes grid from which information will be aggregated.

     \note This loRes grid must have 3 or greater points in at least one of its dimensions.

     \see SolvePoissonMultiGrid UpSampleFrom

     */
    /*void DownSample( const UniformGrid< ItemT > & hiRes )
    {
        ASSERT( hiRes.Size() == hiRes.GetGridCapacity() ) ;
        UniformGrid< ItemT > &  loRes        = * this ;
        ASSERT( ( loRes.GetNumPoints( 0 ) > 2 ) || ( loRes.GetNumPoints( 1 ) > 2 ) || ( loRes.GetNumPoints( 2 ) > 2 ) ) ;
            const unsigned          numPointsHiRes[3]   = { hiRes.GetNumPoints( 0 ) , hiRes.GetNumPoints( 1 ) , hiRes.GetNumPoints( 2 ) } ;
            const unsigned  &       numXhiRes           = hiRes.GetNumPoints( 0 ) ;
            const unsigned          numXYhiRes          = numXhiRes * hiRes.GetNumPoints( 1 ) ;
        static const float      fMultiplierTable[]  = { 8.0f , 4.0f , 2.0f , 1.0f } ;

        // number of cells in each grid cluster
            const unsigned pClusterDims[] = {   hiRes.GetNumCells( 0 ) / loRes.GetNumCells( 0 )
            ,   hiRes.GetNumCells( 1 ) / loRes.GetNumCells( 1 )
            ,   hiRes.GetNumCells( 2 ) / loRes.GetNumCells( 2 ) } ;
        ASSERT( pClusterDims[0] > 1 ) ;
        ASSERT( pClusterDims[0] > 1 ) ;
        ASSERT( pClusterDims[0] > 1 ) ;

            const unsigned  numPointsLoRes[3]   = { loRes.GetNumPoints( 0 ) , loRes.GetNumPoints( 1 ) , loRes.GetNumPoints( 2 ) } ;
            const unsigned  numXYLoRes          = loRes.GetNumPoints( 0 ) * loRes.GetNumPoints( 1 ) ;
        #if USE_ALL_NEIGHBORS
            const unsigned  idxShifts[3]        = { pClusterDims[0] / 2 , pClusterDims[1] / 2 , pClusterDims[2] / 2 } ;
        #endif
        // Since this loop iterates over each destination cell,
        // it should readily parallelize without contention.
        //
        // Note that the if-statements inside this loop could (and perhaps should) be
        // moved outside the loop.  Accomplish this by limiting the loRes indices to
        // be in [1,N-2] and then creating 6 2D loops below, for the boundary planes.
        unsigned idxLoRes[3] ;
        for( idxLoRes[2] = 0 ; idxLoRes[2] < numPointsLoRes[2] ; ++ idxLoRes[2] )
        {
                const unsigned offsetZ = idxLoRes[2] * numXYLoRes ;
            for( idxLoRes[1] = 0 ; idxLoRes[1] < numPointsLoRes[1] ; ++ idxLoRes[1] )
            {
                    const unsigned offsetYZ = idxLoRes[1] * loRes.GetNumPoints( 0 ) + offsetZ ;
                for( idxLoRes[0] = 0 ; idxLoRes[0] < numPointsLoRes[0] ; ++ idxLoRes[0] )
                {   // For each cell in the loRes layer...
                        const unsigned  offsetXYZ   = idxLoRes[0] + offsetYZ ;
                    ItemT        &  rValLoRes  = loRes[ offsetXYZ ] ;
                    float           multiplier  = 0.0f ;
                    unsigned clusterMinIndices[ 3 ] ;
                    NestedGrid<Vec3>::GetChildClusterMinCornerIndex( clusterMinIndices , pClusterDims , idxLoRes ) ;
                    unsigned increment[3] ;
                    int      shiftHiRes[3] ;
                    unsigned idxHiRes[3] ;

                    // Code below accumulates values so destination needs to start at zero.
                    memset( & rValLoRes , 0 , sizeof( rValLoRes ) ) ;

                    // For each cell of hiRes layer in this grid cluster...
                    for( increment[2] = 0 ; increment[2] < pClusterDims[2] IF_USE_ALL_NEIGHBORS( + idxShifts[2] ) ; ++ increment[2] )
                    {
                        shiftHiRes[2] = increment[2] IF_USE_ALL_NEIGHBORS( - idxShifts[2] ) ;
                        idxHiRes[2] = clusterMinIndices[2] + shiftHiRes[2] ;
                        if( idxHiRes[2] < numPointsHiRes[2] )
                        {
                                const unsigned offsetZ  = idxHiRes[2] * numXYhiRes ;
                            for( increment[1] = 0 ; increment[1] < pClusterDims[1] IF_USE_ALL_NEIGHBORS( + idxShifts[1] ) ; ++ increment[1] )
                            {
                                shiftHiRes[1] = increment[1] IF_USE_ALL_NEIGHBORS( - idxShifts[1] ) ;
                                idxHiRes[1] = clusterMinIndices[1] + shiftHiRes[1] ;
                                if( idxHiRes[1] < numPointsHiRes[1] )
                                {
                                        const unsigned offsetYZ = idxHiRes[1] * numXhiRes + offsetZ ;
                                    for( increment[0] = 0 ; increment[0] < pClusterDims[0] IF_USE_ALL_NEIGHBORS( + idxShifts[0] ) ; ++ increment[0] )
                                    {
                                        shiftHiRes[0] = increment[0] IF_USE_ALL_NEIGHBORS( - idxShifts[0] ) ;
                                        idxHiRes[0] = clusterMinIndices[0] + shiftHiRes[0] ;
                                        if( idxHiRes[0]  < numPointsHiRes[0] )
                                        {
                                                const unsigned  offsetXYZ       = idxHiRes[0]  + offsetYZ ;
                                                const unsigned  manhattanDist   = ABS( shiftHiRes[0] ) + ABS( shiftHiRes[1] ) + ABS( shiftHiRes[2] ) ;
                                            ASSERT( ( manhattanDist >= 0 ) && ( manhattanDist < 4 ) ) ;
                                                const ItemT &   rValHiRes       = hiRes[ offsetXYZ ] ;
                                            ASSERT( ! IsInf( rValHiRes ) && ! IsNan( rValHiRes ) ) ;
                                            #if USE_ALL_NEIGHBORS
                                            multiplier += fMultiplierTable[ manhattanDist ] ;
                                            rValLoRes  += fMultiplierTable[ manhattanDist ] * rValHiRes ;
                                            ASSERT( multiplier <= 64.0 ) ;
                                            #else
                                            multiplier += 1.0f ;
                                            rValLoRes  += rValHiRes ;
                                            #endif
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Normalize sum to account for number of gridpoints that contributed to this gridpoint.
                    ASSERT( multiplier > 0.0f ) ;
                    #if USE_ALL_NEIGHBORS
                    rValLoRes /= 64.0f ;
                    #else
                    rValLoRes /= multiplier ;
                    #endif
                }
            }
        }

            #if defined( _DEBUG )
        {
                const ItemT zerothMomentLoRes = loRes.Sum() * loRes.GetCellVolume() ;
                const ItemT zerothMomentHiRes = hiRes.Sum() * hiRes.GetCellVolume() ;
            ASSERT( zerothMomentLoRes.Resembles( zerothMomentHiRes , 1.0e-4f ) ) ;
        }
            #endif
    }*/


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
    /*void UpSample( const UniformGrid< ItemT > & loRes )
    {
        UniformGrid< ItemT > &  hiRes         = * this ;
        ASSERT( loRes.Size() == loRes.GetGridCapacity() ) ;
            const unsigned          numPointsHiRes[3]   = { hiRes.GetNumPoints( 0 ) , hiRes.GetNumPoints( 1 ) , hiRes.GetNumPoints( 2 ) } ;
            const unsigned  &       numXhiRes           = hiRes.GetNumPoints( 0 ) ;
            const unsigned          numXYhiRes          = numXhiRes * hiRes.GetNumPoints( 1 ) ;
        // Nudge sampling positions so they lie strictly within the grid.
            const Vec3              vPosMinAdjust       = Vec3( fabsf( hiRes.GetMinCorner().x )
            , fabsf( hiRes.GetMinCorner().y )
            , fabsf( hiRes.GetMinCorner().z ) ) * 2.0f * FLT_EPSILON ;
            const Vec3              vPosMin             = hiRes.GetMinCorner() + vPosMinAdjust ;
        ASSERT( ( hiRes.GetCellSpacing().x >= 0.0f ) && ( hiRes.GetCellSpacing().y >= 0.0f ) && ( hiRes.GetCellSpacing().z >= 0.0f ) ) ;
            const Vec3              vSpacing            = hiRes.GetCellSpacing() * ( 1.0f - 4.0f * FLT_EPSILON ) ;

        // Since this loop iterates over each destination cell,
        // it should readily parallelize without contention.
        unsigned idxHiRes[3] ;
        for( idxHiRes[2] = 0 ; idxHiRes[2] < numPointsHiRes[2] ; ++ idxHiRes[2] )
        {
                const unsigned  offsetZ     = idxHiRes[2] * numXYhiRes ;
            Vec3            vPosition   ;
            vPosition.z = vPosMin.z + float( idxHiRes[2] ) * vSpacing.z ;

            for( idxHiRes[1] = 0 ; idxHiRes[1] < numPointsHiRes[1] ; ++ idxHiRes[1] )
            {
                    const unsigned offsetYZ = idxHiRes[1] * hiRes.GetNumPoints( 0 ) + offsetZ ;
                vPosition.y = vPosMin.y + float( idxHiRes[1] ) * vSpacing.y ;

                for( idxHiRes[0] = 0 ; idxHiRes[0] < numPointsHiRes[0] ; ++ idxHiRes[0] )
                {   // For each cell in the loRes layer...
                        const unsigned  offsetXYZ   = idxHiRes[0] + offsetYZ ;
                    ItemT        &  rValHiRes   = hiRes[ offsetXYZ ] ;
                    vPosition.x = vPosMin.x + float( idxHiRes[0] ) * vSpacing.x ;
                    loRes.Interpolate( rValHiRes , vPosition ) ;
                    ASSERT( ! IsNan( rValHiRes ) && ! IsInf( rValHiRes ) ) ;
                }
            }
        }

            #if defined( _DEBUG )
        {
                const ItemT zerothMomentLoRes = loRes.ComputeZerothMoment() ;
                const ItemT zerothMomentHiRes = hiRes.ComputeZerothMoment() ;
            ASSERT( zerothMomentLoRes.Resembles( zerothMomentHiRes ) ) ;
        }
            #endif
    }*/



    public void clear()
    {
        mContents.clear() ;
        super.clear();
    }


//    void GenerateBrickOfBytes( const char * strFilenameBase , unsigned uFrame ) const ;



    /** Compute sum of contents of this container.
     */
    /*ItemT Sum() const
    {
        ItemT     sum ;
        memset( & sum , 0 , sizeof( sum ) ) ;
        for( unsigned offsetP = 0 ; offsetP < GetGridCapacity() ; ++ offsetP )
        {
            sum += operator[]( offsetP ) ;
        }
        return sum ;
    }*/


    /** Compute zeroth moment of contents of this container.
     */
    /*ItemT ComputeZerothMoment() const
    {
            const unsigned numCells[ 3 ] = { GetNumCells( 0 ) , GetNumCells( 1 ) , GetNumCells( 2 ) } ;
            const unsigned strides[ 3 ] = { 1 , GetNumPoints( 0 ) , GetNumPoints( 0 ) * GetNumPoints( 1 ) } ;
        ItemT     firstMoment ;
        memset( & firstMoment , 0 , sizeof( firstMoment ) ) ;
        unsigned idxCell[3] ;
        for( idxCell[2] = 0 ; idxCell[2] < numCells[ 2 ] ; ++ idxCell[2] )
        {
            for( idxCell[1] = 0 ; idxCell[1] < numCells[ 1 ] ; ++ idxCell[1] )
            {
                for( idxCell[0] = 0 ; idxCell[0] < numCells[ 0 ] ; ++ idxCell[0] )
                {   // For each cell...
                        const size_t offset = OffsetFromIndices( idxCell ) ;
                    firstMoment +=
                            (   operator[]( offset                                              )
                        +   operator[]( offset + strides[ 0 ]                               )
                        +   operator[]( offset +                strides[ 1 ]                )
                        +   operator[]( offset + strides[ 0 ] + strides[ 1 ]                )
                        +   operator[]( offset +                               strides[ 2 ] )
                        +   operator[]( offset + strides[ 0 ] +                strides[ 2 ] )
                        +   operator[]( offset +                strides[ 1 ] + strides[ 2 ] )
                        +   operator[]( offset + strides[ 0 ] + strides[ 1 ] + strides[ 2 ] )
                            ) ;
                }
            }
        }
        static const float oneOverPointsPerCell = 0.125f ; // Each gridcell has 8 gridpoints.
        return firstMoment * GetCellVolume() * oneOverPointsPerCell ;
    }*/
}
