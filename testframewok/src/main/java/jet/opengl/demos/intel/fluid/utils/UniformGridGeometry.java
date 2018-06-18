package jet.opengl.demos.intel.fluid.utils;

import org.lwjgl.util.vector.ReadableVector3f;
import org.lwjgl.util.vector.Vector3f;

import jet.opengl.postprocessing.util.Numeric;

/**
 * Created by Administrator on 2018/5/10 0010.
 */

public class UniformGridGeometry {
    static final float FLT_EPSILON   =  1.192092896e-07F;

    protected final Vector3f mMinCorner = new Vector3f()      ;   ///< Minimum position (in world units) of grid in X, Y and Z directions.
    protected final Vector3f mGridExtent = new Vector3f()     ;   ///< Size (in world units) of grid in X, Y and Z directions.
    protected final Vector3f mCellExtent = new Vector3f()    ;   ///< Size (in world units) of a cell.
    protected final Vector3f mCellsPerExtent = new Vector3f() ;   ///< Reciprocal of cell size (precomputed once to avoid excess divides).
    protected final int[]  mNumPoints = new int[ 3 ] ;   ///< Number of gridpoints along X, Y and Z directions.

    /** Construct a uniform grid that fits the given geometry.

     \see Clear, DefineShape
     */
    public UniformGridGeometry(int uNumElements , ReadableVector3f vMin ,ReadableVector3f vMax , boolean bPowerOf2 )
    {
        defineShape( uNumElements , vMin , vMax , bPowerOf2 ) ;
    }

    public UniformGridGeometry(){}

    public UniformGridGeometry(UniformGridGeometry ohs){
        set(ohs);
    }

    public UniformGridGeometry set(UniformGridGeometry ohs){
        mMinCorner.set(ohs.mMinCorner);
        mGridExtent.set(ohs.mGridExtent);
        mCellExtent.set(ohs.mCellExtent);
        mCellsPerExtent.set(ohs.mCellsPerExtent);
        for(int i = 0; i < 3; i++){
            mNumPoints[i] = ohs.mNumPoints[i];
        }

        return this;
    }

    /// Return minimal corner of this UniformGrid.
    public ReadableVector3f getMinCorner() { return mMinCorner ; }


    /// Return maximal corner of this UniformGrid.
    public ReadableVector3f getMaxCorner()  { return Vector3f.add(getMinCorner(), getExtent(), null) ; }


    /** Return whether the given query point lies inside the boundaries of this grid.
     */
    public boolean  encompasses( ReadableVector3f queryPoint )
    {
//        return ( queryPoint >= GetMinCorner() ) && ( queryPoint <= GetMaxCorner() ) ;  TODO
        ReadableVector3f minCorner = getMinCorner();
        ReadableVector3f maxCorner = getMaxCorner();
        boolean result = true;
        for(int i = 0; i < 3; i++){
            result &=(queryPoint.get(i) >= minCorner.get(i)) && (queryPoint.get(i) <= maxCorner.get(i));
        }

        return result;
    }


    /// Return center position that this UniformGrid contains.
    public ReadableVector3f getCenter()
    {
//        return ( GetMinCorner() + GetMaxCorner() ) * 0.5f ;
        return Vector3f.add(getMinCorner(), getMaxCorner(), null).scale(0.5f);
    }


    /** Return whether the shape of this object matches that of the given.

     \param that - Other UniformGridGeometry object

     \return true if the shape of this object matches that of that.

     */
    public boolean shapeMatches( UniformGridGeometry that )
    {
        return      ( getNumPoints( 0 ) == that.getNumPoints( 0 ) )
                &&  ( getNumPoints( 1 ) == that.getNumPoints( 1 ) )
                &&  ( getNumPoints( 2 ) == that.getNumPoints( 2 ) )
                &&  ( getMinCorner()    == that.getMinCorner()    )
                &&  ( getExtent()       == that.getExtent()       ) ;
    }



   /* bool operator==( const UniformGridGeometry & that ) const
    {
        return ShapeMatches( that )
                && mGridExtent      == that.mGridExtent
                && mCellExtent      == that.mCellExtent
                && mCellsPerExtent  == that.mCellsPerExtent ;
    }*/

    @Override
    public boolean equals(Object obj) {
        if(this == obj) return true;

        if(obj instanceof UniformGridGeometry){
            UniformGridGeometry that = (UniformGridGeometry)obj;
            return shapeMatches( that )
                    && mGridExtent      .equals(that.mGridExtent)
                    && mCellExtent      .equals(that.mCellExtent)
                    && mCellsPerExtent  .equals(that.mCellsPerExtent) ;
        }

        return false;
    }

    /** Define the shape a uniform grid such that it fits the given geometry.

     \param uNumElements - number of elements this container will contain.

     \param vMin - minimal coordinate of axis-aligned bounding box.

     \param vMax - maximal coordinate of axis-aligned bounding box.

     \param bPowerOf2 - whether to make each grid dimension a power of 2.
     Doing so simplifies grid subdivision, if this grid will be used
     in a hierarchical grid.

     This makes a uniform grid of cells, where each cell is the same size
     and the side of each cell is nearly the same size.  If the cells are
     3-dimensional then that means each cell is a box, nearly a cube.
     The number of dimensions of the region depends on the actual size of
     the region.  If any size component is smaller than a small threshold
     then this class considers that component to be zero, and reduces the
     dimensionality of the region.  For example, if the region size is
     (2,3,0) then this class considers the region to have 2 dimensions
     (x and y) since the z size is zero.  In this example, the cells
     would be nearly square rectangles (instead of boxes).

     */
    public void defineShape(int uNumElements , ReadableVector3f vMin , ReadableVector3f vMax , boolean bPowerOf2 )
    {
        if( uNumElements > 0 )
        {   // This grid contains elements.
            mMinCorner.set(vMin) ;
            final float Nudge = 1.0f + FLT_EPSILON ;  // slightly expand size to ensure robust containment even with roundoff
//            mGridExtent     = ( vMax - vMin ) * Nudge ;
            Vector3f.sub(vMax, vMin, mGridExtent).scale(Nudge);

            Vector3f vSizeEffective = new Vector3f( getExtent() ) ;
            int numDims = 3 ;   // Number of dimensions to region.
            if( 0.0f == vSizeEffective.x )
            {   // X size is zero so reduce dimensionality
                vSizeEffective.x = 1.0f ; // This component will not contribute to the total region volume/area/length.
                mGridExtent.x = 0.0f ;
                -- numDims ;
            }
            if( 0.0f == vSizeEffective.y )
            {   // Y size is zero so reduce dimensionality
                vSizeEffective.y = 1.0f ; // This component will not contribute to the total region volume/area/length.
                mGridExtent.y = 0.0f ;
                -- numDims ;
            }
            if( 0.0f == vSizeEffective.z )
            {   // Z size is zero so reduce dimensionality
                vSizeEffective.z = 1.0f ; // This component will not contribute to the total region volume/area/length.
                mGridExtent.z = 0.0f ;
                -- numDims ;
            }
            assert ( numDims > 0 ) ;

            // Compute region volume, area or length (depending on dimensionality).
            final float volume                  = vSizeEffective.x * vSizeEffective.y * vSizeEffective.z ;
            final float invCellVolumeCubeRoot   = (float) Math.pow( volume / uNumElements , -1.0f / numDims ); // Approximate size of each cell in grid.
            // Compute number of cells in each direction of uniform grid.
            // Choose grid dimensions to fit as well as possible, so that the total number
            // of grid cells is nearly the total number of elements in the contents.
            int numCells[/*3*/] = { Math.max( 1 , (int)( getExtent().x * invCellVolumeCubeRoot + 0.5f ) ) ,
                    Math.max( ( 1 ) , (int)( getExtent().y * invCellVolumeCubeRoot + 0.5f ) ) ,
                    Math.max( ( 1 ) , (int)( getExtent().z * invCellVolumeCubeRoot + 0.5f ) ) } ;
            if( bPowerOf2 )
            {   // Choose number of gridcells to be powers of 2.
                // This will simplify subdivision in a NestedGrid.
                numCells[ 0 ] = Numeric.nearestPowerOfTwo( numCells[ 0 ] ) ;
                numCells[ 1 ] = Numeric.nearestPowerOfTwo( numCells[ 1 ] ) ;
                numCells[ 2 ] = Numeric.nearestPowerOfTwo( numCells[ 2 ] ) ;
            }
            while( numCells[ 0 ] * numCells[ 1 ] * numCells[ 2 ] >= uNumElements * 8 )
            {   // Grid capacity is excessive.
                // This can occur when the trial numCells is below 0.5 in which case the integer arithmetic loses the subtlety.
                numCells[ 0 ] = Math.max( 1 , numCells[0] / 2 ) ;
                numCells[ 1 ] = Math.max( 1 , numCells[1] / 2 ) ;
                numCells[ 2 ] = Math.max( 1 , numCells[2] / 2 ) ;
            }
            mNumPoints[ 0 ] = numCells[ 0 ] + 1 ; // Increment to obtain number of points.
            mNumPoints[ 1 ] = numCells[ 1 ] + 1 ; // Increment to obtain number of points.
            mNumPoints[ 2 ] = numCells[ 2 ] + 1 ; // Increment to obtain number of points.

            precomputeSpacing() ;
        }
        else
        {   // This grid contains no elements.
            clear() ;
        }
    }


    /** Scale given grid.

     \param src - Source uniform grid upon which to base dimensions of this one

     \param scale - amount by which to scale each axis of grid

     \note The number of cells is also scaled.

     */
    public void scale(UniformGridGeometry src , ReadableVector3f scale )
    {
        mGridExtent     .set( src.mGridExtent.x * scale.getX() , src.mGridExtent.y * scale.getY() , src.mGridExtent.z * scale.getZ() ) ;
        mMinCorner      .set( src.mMinCorner.x  * scale.getX() , src.mMinCorner.y  * scale.getY() , src.mMinCorner.z  * scale.getZ() ) ;
        mNumPoints[ 0 ] = (int)(  src.getNumCells( 0 ) * scale.getX() ) + 1 ;
        mNumPoints[ 1 ] = (int)(  src.getNumCells( 1 ) * scale.getY() ) + 1 ;
        mNumPoints[ 2 ] = (int)(  src.getNumCells( 2 ) * scale.getZ() ) + 1 ;
        precomputeSpacing() ;
    }



    /** Create a higher-resolution uniform grid based on another.

     \param src  Source uniform grid upon which to base dimensions of this one.

     \param iExpansion  Amount by which to increase the number of grid cells in each dimension.
     Typically this would be a power of 2.

     \note The number of cells is increased.  The number of points is different.

     */
    public void expand( UniformGridGeometry src , int iExpansions[ ] )
    {
        mGridExtent         .set(src.mGridExtent) ;
        mMinCorner          .set(src.mMinCorner) ;
        mNumPoints[ 0 ]     = src.getNumCells( 0 ) * iExpansions[ 0 ] + 1 ;
        mNumPoints[ 1 ]     = src.getNumCells( 1 ) * iExpansions[ 1 ] + 1 ;
        mNumPoints[ 2 ]     = src.getNumCells( 2 ) * iExpansions[ 2 ] + 1 ;
        precomputeSpacing() ;
    }




    /** Create a higher-resolution uniform grid based on another.

     \param src  Source uniform grid upon which to base dimensions of this one.

     \param iExpansion  Amount by which to increase the number of grid cells in each dimension.
     Typically this would be a power of 2.

     \note The number of cells is increased.  The number of points is different.

     */
    public void expand( UniformGridGeometry src , int expansion )
    {
        int expansions[  ] = { expansion , expansion , expansion } ;
        expand( src , expansions ) ;
    }




    /** Make this a lower-resolution uniform grid based the given source grid.

     \param src  Source uniform grid upon which to base dimensions of this one.

     \param iDecimation  Amount by which to reduce the number of grid cells in each dimension.
     Typically this would be 2.

     \note The number of cells is decimated.  The number of points is different.

     */
    public void decimate( UniformGridGeometry src , int iDecimation )
    {
        mGridExtent         .set(src.mGridExtent) ;
        mMinCorner          .set(src.mMinCorner) ;
        mNumPoints[ 0 ]     = src.getNumCells( 0 ) / iDecimation + 1 ;
        mNumPoints[ 1 ]     = src.getNumCells( 1 ) / iDecimation + 1 ;
        mNumPoints[ 2 ]     = src.getNumCells( 2 ) / iDecimation + 1 ;
        if( iDecimation > 1 )
        {   // Decimation could reduce dimension and integer arithmetic could make value be 0, which is useless if src contained any data.
            mNumPoints[ 0 ] = Math.max( 2 , getNumPoints( 0 ) ) ;
            mNumPoints[ 1 ] = Math.max( 2 , getNumPoints( 1 ) ) ;
            mNumPoints[ 2 ] = Math.max( 2 , getNumPoints( 2 ) ) ;
        }
        precomputeSpacing() ;
    }



    /** Copy shape information from another UniformGrid into this one.
     */
    public void copyShape( UniformGridGeometry src )
    {
        decimate( src , 1 ) ;
    }

    /** Use shape information from another UniformGrid and contraints to determine shape of this grid.
     */
    public void fitShape( UniformGridGeometry src , float minCellSpacing )
    {
        copyShape( src ) ; // Kick-start geometry using given shape.

        Vector3f cellSpacings = getCellSpacing() ;
        for( int axis = 0 ; axis < 3 ; ++ axis )
        {
            int decimation = 1 ;
            while(      ( cellSpacings.get(axis) < minCellSpacing )
                    &&  ( getNumCells( axis ) > 2 ) )
            {
                decimation *= 2 ;
                mNumPoints[ axis ] = Math.max( 2, src.getNumCells( axis ) / decimation + 1 ) ;
                precomputeSpacing() ;
            }
        }
    }


    /** Get world-space dimensions of UniformGridGeometry.
     */
    public Vector3f getExtent() { return mGridExtent ; }


    /** Whether this geometry has zero extent.
     */
    public boolean hasZeroExtent() { return /*GetExtent() == Vec3( 0.0f , 0.0f , 0.0f )*/ getExtent().isZero() ; }


    /** Get reciprocal of cell spacing.
     */
    public Vector3f getCellsPerExtent() { return mCellsPerExtent ; }


    /** Get number of grid points along the given dimension.

     \param index - dimension queried, where 0 means x, 1 means y and 2 means z.

     \note The number of cells in each direction i is GetNumPoints(i) - 1.

     */
     public int getNumPoints(int index ) { return mNumPoints[ index ] ; }


    /** Get number of grid cells along the given dimension.

     \param index - dimension queried, where 0 means x, 1 means y and 2 means z.

     \see GetNumPoints

     */
    public int getNumCells( int index )
    {
        assert ( getNumPoints( index ) >= 1 ) ;
        return getNumPoints( index ) - 1 ;
    }


    /** Get total number of gridpoints.

     \note This returns the number of gridpoints defined by the geometry, which is separate
     from the capacity of the container.  When the container has been initialized,
     these should be the same, but prior to initializing the container,
     GetGridCapacity can be non-zero even when the container size is zero.

     */
    public int getGridCapacity() { return getNumPoints( 0 ) * getNumPoints( 1 ) * getNumPoints( 2 ) ; }


    /** Return extent (in world units) of a grid cell.
     */
    public Vector3f getCellSpacing()  { return mCellExtent ; }


    /** Return volume (in world units) of entire grid.
     */
    public float getVolume()   { return getExtent().x * getExtent().y * getExtent().z ; }


    /** Return volume (in world units) of a grid cell.
     */
    public float getCellVolume()  { return mCellExtent.x * mCellExtent.y * mCellExtent.z ; }


    /** Return extent (in world units) of a grid cell.
     */
    public Vector3f getCellCenter( int ix , int iy , int iz )
    {
        final Vector3f vOffset = new Vector3f
                          (   ix * getCellSpacing().x
                            , iy * getCellSpacing().y
                            , iz * getCellSpacing().z ) ;
        final Vector3f vMin = Vector3f.add( getMinCorner() , vOffset, vOffset ) ;
        /*const Vec3 vCenter( vMin + GetCellSpacing() * 0.5f ) ;
        return vCenter ;*/
        return Vector3f.linear(vMin, getCellSpacing(), 0.5f, vMin);
    }


    /** Compute indices into contents array of a point at a given position.

     \param vPosition - position of a point.  It must be within the region of this container.

     \param indices - Indices into contents array of a point at vPosition.

     \see IndicesFromOffset, PositionFromOffset, OffsetOfPosition, PositionFromIndices

     \note Derived class defines the actual contents array.

     */
    public void indicesOfPosition( int indices[/*4*/] , ReadableVector3f vPosition )
    {
        // Notice the pecular test here.  vPosition may lie slightly outside of the extent give by vMax.
        // Review the geometry described in the class header comment.
        Vector3f vPosRel = Vector3f.sub( vPosition,getMinCorner(), null ) ;   // position of given point relative to container region
        Vector3f vIdx = new Vector3f( vPosRel.x * getCellsPerExtent().x , vPosRel.y * getCellsPerExtent().y , vPosRel.z * getCellsPerExtent().z ) ;
        /*#if 0   // Original code.
        indices[0] = unsigned( vIdx.x ) ;
        indices[1] = unsigned( vIdx.y ) ;
        indices[2] = unsigned( vIdx.z ) ;
        #elif 1 // Optimization 1: change control word once for all 3 conversions

        #elif 0 // Optimization 2: Use SSE instructions that do not require 16-byte alignment
        _asm {
        movdqu      xmm0    , vIdx
        cvttps2dq   xmm0    , xmm0
        movdqu      indices , xmm0
    }
        #elif 0 // Optimization 3: Use SSE instructions that assume 16-byte alignment
        _asm {
        cvttps2dq   xmm0    , vIdx
        movdqa      indices , xmm0
    }
        #elif 0 // Optimization 4: use approximation.  Faster, but gives incorrect results sometimes.
        indices[0] = FastIntFromFloatApproximate( vIdx.x ) ;
        indices[1] = FastIntFromFloatApproximate( vIdx.y ) ;
        indices[2] = FastIntFromFloatApproximate( vIdx.z ) ;
        #endif*/

//        const WORD OldCtrlWord = Changex87FloatingPointToTruncate() ;
        indices[0] = Float.floatToRawIntBits( vIdx.x ) ;
        indices[1] = Float.floatToRawIntBits( vIdx.y ) ;
        indices[2] = Float.floatToRawIntBits( vIdx.z ) ;
//        SetFloatingPointControlWord( OldCtrlWord ) ;

        assert ( indices[0] < getNumPoints( 0 ) ) ;
        assert ( indices[1] < getNumPoints( 1 ) ) ;
        assert ( indices[2] < getNumPoints( 2 ) ) ;
        assert ( /*! sInterpolating ||*/ ( indices[0] < getNumCells( 0 ) ) ) ; // DO NOT SUBMIT
        assert ( /*! sInterpolating ||*/ ( indices[1] < getNumCells( 1 ) ) ) ; // DO NOT SUBMIT
        assert ( /*! sInterpolating ||*/ ( indices[2] < getNumCells( 2 ) ) ) ; // DO NOT SUBMIT
    }


    /** Get indices of nearest grid point.

     \param indices      (out) Indices of nearest grid point.

     \param vPosition    (in) Position being queried.

     This contrasts with IndicesOfPosition, which gets the indices of the nearest grid *cell*,
     which is tantamount to getting the minimal corner gridpoint of that cell.
     IndicesOfPosition is like rounding down whereas this routine is like
     rounding to nearest.
     */
    public void indicesOfNearestGridPoint( int indices[/*4*/] , ReadableVector3f vPosition )
    {
//        DEBUG_ONLY( UniformGridGeometry::sInterpolating = true ) ;
        indicesOfPosition( indices , vPosition ) ;
//        DEBUG_ONLY( UniformGridGeometry::sInterpolating = false ) ;
        assert ( indices[0] < getNumCells( 0 ) ) ;
        assert ( indices[1] < getNumCells( 1 ) ) ;
        assert ( indices[2] < getNumCells( 2 ) ) ;
        Vector3f vMinCorner = new Vector3f();
        positionFromIndices( vMinCorner , indices ) ;
        final Vector3f  vDiff   = Vector3f.sub(vPosition, vMinCorner, null) ; // Relative location of position within its containing grid cell.
        final Vector3f  tween   = new Vector3f( vDiff.x * getCellsPerExtent().x , vDiff.y * getCellsPerExtent().y , vDiff.z * getCellsPerExtent().z ) ;
        if( tween.x >= 0.5f ) ++ indices[ 0 ] ;
        if( tween.y >= 0.5f ) ++ indices[ 1 ] ;
        if( tween.z >= 0.5f ) ++ indices[ 2 ] ;
    }


    /** Compute indices into contents array of a point at a given position.

     Assumes floating point control word is set to use truncation instead of nearest, for rounding.

     \see IndicesOfPosition, StoreFloatAsInt, Changex87FloatingPointToTruncate.

     */
    public void indicesOfPosition_AssumesFpcwSetToTruncate( int indices[/*4*/] , ReadableVector3f vPosition )
    {
//        ASSERT( FpcwTruncates() ) ;
        // Notice the pecular test here.  vPosition may lie slightly outside of the extent give by vMax.
        // Review the geometry described in the class header comment.
        Vector3f vPosRel = Vector3f.sub( vPosition, getMinCorner(), null ) ;   // position of given point relative to container region
        Vector3f vIdx = new Vector3f( vPosRel.x * getCellsPerExtent().x , vPosRel.y * getCellsPerExtent().y , vPosRel.z * getCellsPerExtent().z ) ;
        // The following 3 float-to-int conversions assume the
        // floating-point-control-word is set to truncate, by the outer caller.
        indices[0] = Float.floatToRawIntBits( vIdx.x ) ;
        indices[1] = Float.floatToRawIntBits( vIdx.y ) ;
        indices[2] = Float.floatToRawIntBits( vIdx.z ) ;
        assert ( indices[0] < getNumPoints( 0 ) ) ;
        assert ( indices[1] < getNumPoints( 1 ) ) ;
        assert ( indices[2] < getNumPoints( 2 ) ) ;
        assert ( /*! sInterpolating ||*/ ( indices[0] < getNumCells( 0 ) ) ) ; // DO NOT SUBMIT
        assert ( /*! sInterpolating ||*/ ( indices[1] < getNumCells( 1 ) ) ) ; // DO NOT SUBMIT
        assert ( /*! sInterpolating ||*/ ( indices[2] < getNumCells( 2 ) ) ) ; // DO NOT SUBMIT
    }


    /** Compute offset into contents array of a point at a given position.

     \param vPosition - position of a point.  It must be within the region of this container.

     \return Offset into contents array of a point at vPosition.

     \see IndicesFromOffset, PositionFromOffset, PositionFromIndices

     \note Derived class defines the actual contents array.

     */
    public int offsetOfPosition( ReadableVector3f vPosition )
    {
        int indices[/*4*/]  = new int[4];
        indicesOfPosition( indices , vPosition ) ;
        final int offset = indices[0] + getNumPoints( 0 ) * ( indices[1] + getNumPoints( 1 ) * indices[2] ) ;
        return offset ;
    }


    /** Compute position of minimal corner of grid cell with given indices.

     \param vPosition    (out) Position of minimal corner of grid cell.

     \param indices      (in) Grid cell indices.

     \note Rarely would you want to compute position from indices in this
     way. Typically, this kind of computation occurs inside a
     triply-nested loop, in which case the procedure should
     compute each component separately.  Furthermore, such a
     routine would cache GetCellSpacing instead of computing it
     each iteration.

     */
    public void positionFromIndices( Vector3f vPosition , int indices[/*3*/] )
    {
        vPosition.x = getMinCorner().getX() + ( indices[0] ) * getCellSpacing().x ;
        vPosition.y = getMinCorner().getY() + ( indices[1] ) * getCellSpacing().y ;
        vPosition.z = getMinCorner().getZ() + ( indices[2] ) * getCellSpacing().z ;
    }


    /** Compute position of minimal corner of grid cell with given indices.

     \param ix   X index of grid cell.
     \param iy   Y index of grid cell.
     \param iz   Z index of grid cell.

     \return Position of minimal corner of grid cell.
     */
    public Vector3f positionFromIndices( int ix , int iy , int iz )
    {
        Vector3f vPosition = new Vector3f(
                getMinCorner().getX() + ix * getCellSpacing().x
            , getMinCorner().getY() + iy * getCellSpacing().y
            , getMinCorner().getZ() + iz * getCellSpacing().z ) ;
        return vPosition ;
    }


    /** Compute position of minimal corner of grid cell with given indices.

     \param indices      (in) Grid cell indices.

     \return Position of minimal corner of grid cell.

     */
    public Vector3f  positionFromIndices( int indices[/*3*/] )
    {
        Vector3f vPosition = new Vector3f( getMinCorner().getX() + ( indices[0] ) * getCellSpacing().x
            , getMinCorner().getY() + ( indices[1] ) * getCellSpacing().y
            , getMinCorner().getZ() + ( indices[2] ) * getCellSpacing().z ) ;
        return vPosition ;
    }


    /** Compute X,Y,Z grid cell indices from offset into contents array.

     \param indices - Individual X,Y,Z component grid cell indices.

     \param offset - Offset into mContents.
     */
    public void indicesFromOffset(int indices[/*3*/] , int offset )
    {
        indices[2] = offset / ( getNumPoints(0) * getNumPoints(1) ) ;
        indices[1] = ( offset - indices[2] * getNumPoints(0) * getNumPoints(1) ) / getNumPoints(0) ;
        indices[0] = offset - getNumPoints(0) * ( indices[1] + getNumPoints(1) * indices[2] ) ;
    }


    /** Get position of grid cell minimum corner.

     \param vPos     Position of grid cell minimum corner.

     \param offset   Offset into contents array.

     Each grid cell spans a region (whose size is given by GetCellSpacing)
     starting at a location which this routine returns.  So the grid cell
     with the given offset spans the region from vPos (as this routine
     assigns) to vPos + GetCellSpacing().

     \note Derived class provides actual contents array.

     */
    public void positionFromOffset( Vector3f vPos , int offset )
    {
        int indices[]  = new int[3];
        indicesFromOffset( indices , offset ) ;
        vPos.x = getMinCorner().getX() + ( indices[0] ) * getCellSpacing().x ;
        vPos.y = getMinCorner().getY() + ( indices[1] ) * getCellSpacing().y ;
        vPos.z = getMinCorner().getZ() + ( indices[2] ) * getCellSpacing().z ;
    }


    /** Get offset into contents array given indices.

     \param ix   X index of grid point.
     \param iy   Y index of grid point.
     \param iz   Z index of grid point.

     \return offset into contents array

     */
    public int offsetFromIndices( int ix , int iy , int iz )
    {
        return ix + getNumPoints(0) * ( iy + getNumPoints(1) * iz ) ;
    }


    /** Get offset into contents array given indices.

     \param indices - indices specifying a grid cell

     \return offset into contents array

     \note Typically this routine would not be efficient to use, except for special cases.
     Often, one writes a triple-nested loop iterating over each
     component of indices, in which case it is more efficient
     to compute the z and y terms of the offset separately and
     combine them with the x term in the inner-most loop.
     This routine is useful primarily when there is no coherence
     between the indices of this iteration and the previous or next.

     \note Derived class provides actual contents array.

     */
    public int offsetFromIndices( int indices[/*3*/] )
    {
        return offsetFromIndices( indices[ 0 ] , indices[ 1 ] , indices[ 2 ] ) ;
    }

    /** Precompute grid spacing, to optimize OffsetOfPosition and other utility routines.
     */
    protected void precomputeSpacing()
    {
        mCellExtent.x       = getExtent().x / (float)( getNumCells( 0 ) ) ;
        mCellExtent.y       = getExtent().y / (float)( getNumCells( 1 ) ) ;
        mCellExtent.z       = getExtent().z / (float)( getNumCells( 2 ) ) ;
        mCellsPerExtent.x   = (float)( getNumCells( 0 ) ) / getExtent().x ;
        mCellsPerExtent.y   = (float)( getNumCells( 1 ) ) / getExtent().y ;
        if( 0.0f == getExtent().z )
        {   // Avoid divide-by-zero for 2D domains that lie in the XY plane.
            mCellsPerExtent.z   = 1.0f / Float.MIN_NORMAL ;
        }
        else
        {
            mCellsPerExtent.z   = getNumCells( 2 ) / getExtent().z ;
        }
    }

    /** Return the offset associated with the cell whose indices are second-to-last in each direction.

     Same as nx * ( ny * ( nz - 1 ) - 1 ) - 2.
     */
    protected int getOffsetOfPenultimateCell()
    {
        return (getNumPoints(0)-2) + getNumPoints(0) * ( (getNumPoints(1)-2) + getNumPoints(1) * (getNumPoints(2)-2) ) ;
    }


    /** Clear out any existing shape information.
     */
    protected void clear()
    {
        mMinCorner.set(0,0,0);
                mGridExtent.set(0,0,0);
                        mCellExtent.set(0,0,0);
                                mCellsPerExtent.set(0,0,0);
        mNumPoints[ 0 ] = mNumPoints[ 1 ] = mNumPoints[ 2 ] = 0 ;
    }
}
